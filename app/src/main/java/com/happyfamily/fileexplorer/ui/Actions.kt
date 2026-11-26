package com.happyfamily.fileexplorer.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.happyfamily.fileexplorer.data.FileItem
import java.io.File

object FileActions {
    private fun uri(ctx: Context, f: File): Uri =
        FileProvider.getUriForFile(ctx, ctx.packageName + ".provider", f)

    fun mime(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext == "apk") return "application/vnd.android.package-archive"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    fun open(ctx: Context, item: FileItem) {
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri(ctx, item.file), mime(item.name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            ctx.startActivity(Intent.createChooser(i, item.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, "이 파일을 열 수 있는 앱이 없습니다", Toast.LENGTH_SHORT).show()
        }
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
        ctx.startActivity(Intent.createChooser(i, "공유").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
