package com.happyfamily.fileexplorer

import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.happyfamily.fileexplorer.ui.AppChoice
import com.happyfamily.fileexplorer.ui.FileActions
import java.io.File

/** 다른 앱(클로드, 브라우저, 메신저 등)이 "열기"로 넘겨준 파일 */
data class IncomingFile(
    val uri: Uri,
    val mime: String,
    val name: String,
    val size: Long,
    /** 기기 안의 실제 위치를 알아냈으면 그 경로 (폴더에서 보기용) */
    val localPath: String?,
) {
    val extension: String get() = name.substringAfterLast('.', "").lowercase()

    companion object {
        fun from(ctx: Context, intent: Intent): IncomingFile? {
            val uri = intent.data ?: return null
            val cr = ctx.contentResolver
            var name: String? = null
            var size = -1L
            if (uri.scheme == "content") runCatching {
                cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        name = c.getString(0)
                        if (!c.isNull(1)) size = c.getLong(1)
                    }
                }
            }
            val path = resolvePath(ctx, uri, name, size)
            val finalName = name ?: path?.let { File(it).name } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "파일"
            val mime = intent.type?.takeIf { it != "*/*" }
                ?: runCatching { cr.getType(uri) }.getOrNull()
                ?: FileActions.mime(finalName)
            return IncomingFile(uri, mime, finalName, size, path)
        }

        /** content:// 주소를 실제 파일 경로로 바꿔 본다. 못 찾으면 다운로드 폴더에서 같은 이름·크기 파일을 찾는다. */
        private fun resolvePath(ctx: Context, uri: Uri, name: String?, size: Long): String? {
            if (uri.scheme == "file") return uri.path
            runCatching {
                if (DocumentsContract.isDocumentUri(ctx, uri)) {
                    val id = DocumentsContract.getDocumentId(uri)
                    if (id.startsWith("raw:")) return id.removePrefix("raw:")
                    if (id.startsWith("primary:")) return Environment.getExternalStorageDirectory().absolutePath + "/" + id.removePrefix("primary:")
                }
            }
            runCatching {
                ctx.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0)?.let { if (File(it).exists()) return it }
                }
            }
            if (name != null) {
                val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val hit = File(dl, name)
                if (hit.isFile && (size < 0 || hit.length() == size)) return hit.absolutePath
                dl.walkTopDown().maxDepth(3).firstOrNull { it.isFile && it.name == name && (size < 0 || it.length() == size) }
                    ?.let { return it.absolutePath }
            }
            return null
        }
    }

    private fun viewIntent() = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        clipData = ClipData.newRawUri(name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** 이 파일을 열 수 있는 다른 앱들 (파일 탐색기 자신은 제외) */
    fun candidates(ctx: Context): List<AppChoice> {
        val pm = ctx.packageManager
        return pm.queryIntentActivities(viewIntent(), PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .distinctBy { it.activityInfo.packageName + "/" + it.activityInfo.name }
            .map { AppChoice(it.activityInfo.packageName, it.activityInfo.name, it.loadLabel(pm).toString(), it) }
            .sortedBy { it.label }
    }

    fun launchWith(ctx: Context, component: String): Boolean {
        val parts = component.split('/', limit = 2)
        if (parts.size != 2 || parts[0] == ctx.packageName) return false
        return try {
            ctx.startActivity(viewIntent().setComponent(ComponentName(parts[0], parts[1]))); true
        } catch (e: Exception) { false }
    }
}
