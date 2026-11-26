package com.happyfamily.fileexplorer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.SubcomposeAsyncImage
import com.happyfamily.fileexplorer.data.FileItem
import com.happyfamily.fileexplorer.data.FileKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun kindColor(k: FileKind): Color = when (k) {
    FileKind.FOLDER -> C.Folder
    FileKind.IMAGE -> C.Image
    FileKind.VIDEO -> C.Video
    FileKind.AUDIO -> C.Audio
    FileKind.DOCUMENT -> C.Doc
    FileKind.ARCHIVE -> C.Archive
    FileKind.APK -> C.Apk
    FileKind.OTHER -> C.Other
}

fun kindIcon(k: FileKind): ImageVector = when (k) {
    FileKind.FOLDER -> Icons.Filled.Folder
    FileKind.IMAGE -> Icons.Outlined.Image
    FileKind.VIDEO -> Icons.Outlined.SmartDisplay
    FileKind.AUDIO -> Icons.Outlined.MusicNote
    FileKind.DOCUMENT -> Icons.Outlined.Description
    FileKind.ARCHIVE -> Icons.Filled.FolderZip
    FileKind.APK -> Icons.Outlined.Download
    FileKind.OTHER -> Icons.Outlined.InsertDriveFile
}

/** 파일 종류에 맞는 기본 아이콘 (썸네일이 없을 때) */
@Composable
fun KindGlyph(item: FileItem, size: Dp) {
    val k = item.kind
    when (k) {
        FileKind.FOLDER, FileKind.ARCHIVE -> Icon(
            kindIcon(k), null, tint = C.Folder, modifier = Modifier.size(size)
        )
        FileKind.APK -> Box(
            Modifier.size(size * 0.86f).clip(RoundedCornerShape(size * 0.24f)).background(Color(0xFFEFF7E3)),
            contentAlignment = Alignment.Center
        ) { Text("APK", color = C.Apk, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.27f).sp) }
        else -> Box(
            Modifier.size(size * 0.86f).clip(RoundedCornerShape(size * 0.24f))
                .background(kindColor(k).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(kindIcon(k), null, tint = kindColor(k), modifier = Modifier.size(size * 0.5f)) }
    }
}

/** 이미지·동영상 썸네일, APK 아이콘, 그 외 종류 아이콘 */
@Composable
fun FileThumb(item: FileItem, size: Dp, corner: Dp = 12.dp) {
    val preview = LocalInspectionMode.current
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            !preview && (item.kind == FileKind.IMAGE || item.kind == FileKind.VIDEO) -> SubcomposeAsyncImage(
                model = item.file,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(RoundedCornerShape(corner)),
                loading = { KindGlyph(item, size) },
                error = { KindGlyph(item, size) },
            )
            !preview && item.kind == FileKind.APK && item.extension == "apk" -> ApkIcon(item, size)
            else -> KindGlyph(item, size)
        }
    }
}

@Composable
private fun ApkIcon(item: FileItem, size: Dp) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(null, item.path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val pm = ctx.packageManager
                val info = pm.getPackageArchiveInfo(item.path, 0) ?: return@runCatching null
                val ai = info.applicationInfo ?: return@runCatching null
                ai.sourceDir = item.path; ai.publicSourceDir = item.path
                ai.loadIcon(pm).toBitmap(144, 144)
            }.getOrNull()
        }
    }
    val b = bmp
    if (b != null) Image(b.asImageBitmap(), null, Modifier.size(size * 0.92f))
    else KindGlyph(item, size)
}

@Composable
fun SectionHeader(title: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 24.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = C.SubStrong, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(14.dp))
        Canvas(Modifier.weight(1f).height(2.dp)) {
            drawLine(
                color = Color(0xFFCFCFD4),
                start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2),
                strokeWidth = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 7f)),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    item: FileItem,
    selectionMode: Boolean,
    selected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    now: Long = System.currentTimeMillis(),
) {
    Column(
        Modifier.fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (selected) C.Accent.copy(alpha = 0.06f) else Color.Transparent)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Icon(
                    if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    null,
                    tint = if (selected) C.Accent else Color(0xFFB8B8BE),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            FileThumb(item, 48.dp)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name, color = C.Text, fontSize = 17.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Format.date(item.modified, now), color = C.Sub, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(
                        if (item.isDir) "${item.childCount}개" else Format.size(item.size),
                        color = C.SubStrong, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (showDivider) Box(
            Modifier.padding(start = if (selectionMode) 116.dp else 82.dp, end = 20.dp)
                .fillMaxWidth().height(1.dp).background(C.Divider)
        )
    }
}

@Composable
fun PillProgress(used: Long, total: Long, modifier: Modifier = Modifier) {
    val ratio = if (total > 0) (used.toFloat() / total).coerceIn(0f, 1f) else 0f
    Box(modifier.height(34.dp).clip(CircleShape).background(C.PillTrack)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(ratio).background(C.PillFill))
        Row(
            Modifier.align(Alignment.Center).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Format.sizeShort(used), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1C2340))
            Text(" / ", fontSize = 13.sp, color = Color(0xFF1C2340))
            Text(Format.sizeShort(total), fontSize = 13.sp, color = Color(0xFF3B4260))
        }
    }
}

@Composable
fun IconBtn(icon: ImageVector, desc: String, tint: Color = C.Text, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(26.dp)) }
}
