package com.happyfamily.fileexplorer.data

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileRepository(private val context: Context) {

    val root: File get() = Environment.getExternalStorageDirectory()
    val downloadDir: File get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    suspend fun list(path: String, showHidden: Boolean): List<FileItem> = withContext(Dispatchers.IO) {
        File(path).listFiles()
            ?.filter { showHidden || !it.name.startsWith(".") }
            ?.map { FileItem.from(it, showHidden) }
            ?: emptyList()
    }

    suspend fun category(cat: Category, showHidden: Boolean): List<FileItem> = withContext(Dispatchers.IO) {
        val (selection, args) = when (cat) {
            Category.IMAGE -> "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?" to
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            Category.VIDEO -> "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?" to
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
            Category.AUDIO -> "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?" to
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO.toString())
            Category.APK -> "${MediaStore.Files.FileColumns.DATA} LIKE ?" to arrayOf("%.apk")
            Category.DOCUMENT -> null to null
            Category.DOWNLOAD -> return@withContext list(downloadDir.absolutePath, showHidden)
        }
        val items = query(selection, args, limit = null)
        val filtered = if (cat == Category.DOCUMENT) items.filter { it.kind == FileKind.DOCUMENT } else items
        filtered.filter { showHidden || !isHiddenPath(it.path) }
    }

    /** 최근에 추가·수정된 파일 (폴더 제외) */
    suspend fun recent(limit: Int = 12): List<FileItem> = withContext(Dispatchers.IO) {
        query(
            "${MediaStore.Files.FileColumns.MIME_TYPE} IS NOT NULL AND " +
                "${MediaStore.Files.FileColumns.DATA} NOT LIKE ?",
            arrayOf("%/Android/%"),
            limit = limit * 4,
        ).filter { !isHiddenPath(it.path) }.take(limit)
    }

    private fun query(selection: String?, args: Array<String>?, limit: Int?): List<FileItem> {
        val uri = MediaStore.Files.getContentUri("external")
        val proj = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
        )
        val out = ArrayList<FileItem>()
        runCatching {
            context.contentResolver.query(
                uri, proj, selection, args,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
            )?.use { c ->
                val iData = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val iSize = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val iDate = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                while (c.moveToNext()) {
                    val path = c.getString(iData) ?: continue
                    val f = File(path)
                    if (f.isDirectory || !f.exists()) continue
                    out += FileItem(
                        path = path,
                        name = f.name,
                        isDir = false,
                        size = c.getLong(iSize).takeIf { it > 0 } ?: f.length(),
                        modified = c.getLong(iDate) * 1000L,
                    )
                    if (limit != null && out.size >= limit) break
                }
            }
        }
        return out
    }

    private fun isHiddenPath(p: String) = p.split('/').any { it.startsWith(".") }

    /** 내장 저장공간 전체에서 이름으로 검색 */
    suspend fun search(query: String, showHidden: Boolean, max: Int = 500): List<FileItem> =
        withContext(Dispatchers.IO) {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return@withContext emptyList()
            val out = ArrayList<FileItem>()
            val stack = ArrayDeque<File>().apply { add(root) }
            val skip = File(root, "Android").absolutePath
            while (stack.isNotEmpty() && out.size < max) {
                val dir = stack.removeLast()
                val children = dir.listFiles() ?: continue
                for (f in children) {
                    if (!showHidden && f.name.startsWith(".")) continue
                    if (f.name.lowercase().contains(q)) out += FileItem.from(f, showHidden)
                    if (f.isDirectory && f.absolutePath != skip) stack.add(f)
                    if (out.size >= max) break
                }
            }
            out
        }

    fun storage(): StorageInfo = runCatching {
        val s = StatFs(root.absolutePath)
        StorageInfo(used = s.totalBytes - s.availableBytes, total = s.totalBytes)
    }.getOrDefault(StorageInfo(0, 0))

    suspend fun delete(paths: List<String>): Int = withContext(Dispatchers.IO) {
        paths.count { File(it).deleteRecursively() }.also { scan(paths) }
    }

    suspend fun rename(path: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val f = File(path)
        val target = File(f.parentFile, newName)
        if (target.exists()) return@withContext false
        f.renameTo(target).also { if (it) scan(listOf(path, target.absolutePath)) }
    }

    suspend fun createFolder(parent: String, name: String): Boolean = withContext(Dispatchers.IO) {
        val d = File(parent, name)
        !d.exists() && d.mkdirs()
    }

    private fun scan(paths: List<String>) {
        runCatching { android.media.MediaScannerConnection.scanFile(context, paths.toTypedArray(), null, null) }
    }
}
