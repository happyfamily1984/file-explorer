package com.happyfamily.fileexplorer.ui

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.happyfamily.fileexplorer.data.FileItem
import java.io.File

/** 파일을 열 수 있는 앱 하나 */
data class AppChoice(val pkg: String, val cls: String, val label: String, val info: ResolveInfo?) {
    val component: String get() = "$pkg/$cls"
}

object FileActions {
    private fun uri(ctx: Context, f: File): Uri =
        FileProvider.getUriForFile(ctx, ctx.packageName + ".provider", f)

    fun mime(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext == "apk") return "application/vnd.android.package-archive"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    private fun viewIntent(ctx: Context, item: FileItem) = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri(ctx, item.file), mime(item.name))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    /** 이 파일을 열 수 있는 앱 목록 (이름순) */
    fun candidates(ctx: Context, item: FileItem): List<AppChoice> {
        val pm = ctx.packageManager
        return pm.queryIntentActivities(viewIntent(ctx, item), PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .distinctBy { it.activityInfo.packageName + "/" + it.activityInfo.name }
            .map { AppChoice(it.activityInfo.packageName, it.activityInfo.name, it.loadLabel(pm).toString(), it) }
            .sortedBy { it.label }
    }

    /** 저장된 "패키지/액티비티" 문자열을 앱 이름으로 (설정 화면 표시용) */
    fun labelOf(ctx: Context, component: String): String? = runCatching {
        val (pkg, cls) = component.split('/', limit = 2)
        ctx.packageManager.getActivityInfo(ComponentName(pkg, cls), 0).loadLabel(ctx.packageManager).toString()
    }.getOrNull()

    /** 지정한 앱으로 바로 연다. 앱이 지워졌거나 못 열면 false */
    fun launchWith(ctx: Context, item: FileItem, component: String): Boolean {
        val (pkg, cls) = component.split('/', limit = 2).let { if (it.size == 2) it else return false }
        val i = viewIntent(ctx, item).setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try { ctx.startActivity(i); true } catch (e: Exception) { false }
    }

    fun share(ctx: Context, items: List<FileItem>) {
        val files = items.filter { !it.isDir }
        if (files.isEmpty()) {
            Toast.makeText(ctx, "폴더는 공유할 수 없습니다", Toast.LENGTH_SHORT).show(); return
        }
        val uris = ArrayList(files.map { uri(ctx, it.file) })
        val i = if (uris.size == 1) Intent(Intent.ACTION_SEND).apply {
            type = mime(files[0].name); putExtra(Intent.EXTRA_STREAM, uris[0])
        } else Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            ctx.startActivity(Intent.createChooser(i, "공유").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, "공유할 수 있는 앱이 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
}
