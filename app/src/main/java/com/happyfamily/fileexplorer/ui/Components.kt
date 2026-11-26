package com.happyfamily.fileexplorer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
    FileKind.DOCUMENT -> C.Accent
    FileKind.ARCHIVE -> C.Archive
    FileKind.APK -> C.Apk
    FileKind.OTHER -> C.Other
}

fun kindIcon(k: FileKind): ImageVector = when (k) {
    FileKind.FOLDER -> Icons.Filled.FolderZip
    FileKind.IMAGE -> Icons.Filled.Image
    FileKind.VIDEO -> Icons.Filled.PlayArrow
    FileKind.AUDIO -> Icons.Filled.MusicNote
    FileKind.DOCUMENT -> Icons.Filled.Description
    FileKind.ARCHIVE -> Icons.Filled.FolderZip
    FileKind.APK -> Icons.Filled.Android
    FileKind.OTHER -> Icons.Filled.InsertDriveFile
}

fun Modifier.card(radius: Dp = 22.dp): Modifier = this
    .shadow(10.dp, RoundedCornerShape(radius), ambientColor = Color(0x223B6EF0), spotColor = Color(0x223B6EF0))
    .clip(RoundedCornerShape(radius))
    .background(C.Card)
    .border(1.dp, C.CardBorder, RoundedCornerShape(radius))

/** 그라데이션이 들어간 파란 폴더 (뒤판 + 앞판). badge가 있으면 앞판 가운데에 흰 아이콘. */
@Composable
fun BlueFolder(size: Dp, badge: ImageVector? = null, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width; val h = this.size.height
            val top = h * 0.16f; val bottom = h * 0.86f
            val r = w * 0.09f
            // 뒤판 + 탭
            val back = Path().apply {
                moveTo(w * 0.06f + r, top)
                lineTo(w * 0.38f, top)
                lineTo(w * 0.46f, top + h * 0.09f)
                lineTo(w * 0.94f - r, top + h * 0.09f)
                quadraticTo(w * 0.94f, top + h * 0.09f, w * 0.94f, top + h * 0.09f + r)
                lineTo(w * 0.94f, bottom - r)
                lineTo(w * 0.06f, bottom - r)
                lineTo(w * 0.06f, top + r)
                quadraticTo(w * 0.06f, top, w * 0.06f + r, top)
                close()
            }
            drawPath(back, Brush.verticalGradient(listOf(C.FolderBack, Color(0xFF2558C9)), startY = top, endY = bottom))
            // 종이
            drawRoundRect(
                Color(0xFFF4F8FF), topLeft = Offset(w * 0.16f, top + h * 0.12f),
                size = Size(w * 0.68f, h * 0.4f), cornerRadius = CornerRadius(r * 0.6f)
            )
            // 앞판
            drawRoundRect(
                Brush.verticalGradient(listOf(C.FolderTop, C.FolderBottom), startY = top + h * 0.22f, endY = bottom),
                topLeft = Offset(w * 0.04f, top + h * 0.22f), size = Size(w * 0.92f, bottom - top - h * 0.22f),
                cornerRadius = CornerRadius(r),
            )
        }
        if (badge != null) Icon(
            badge, null, tint = Color.White,
            modifier = Modifier.padding(top = size * 0.2f).size(size * 0.34f)
        )
    }
}

/** 색 그라데이션 사각형 + 흰 아이콘 */
@Composable
fun ColorGlyph(icon: ImageVector, color: Color, size: Dp, text: String? = null) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.78f), color))),
        contentAlignment = Alignment.Center
    ) {
        if (text != null) Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.27f).sp)
        else Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.56f))
    }
}

@Composable
fun KindGlyph(item: FileItem, size: Dp) {
    when (val k = item.kind) {
        FileKind.FOLDER -> BlueFolder(size)
        FileKind.ARCHIVE -> Icon(Icons.Filled.FolderZip, null, tint = C.Archive, modifier = Modifier.size(size * 0.92f))
        else -> ColorGlyph(kindIcon(k), kindColor(k), size * 0.86f)
    }
}

@Composable
fun FileThumb(item: FileItem, size: Dp, corner: Dp = 12.dp, fill: Boolean = false) {
    val preview = LocalInspectionMode.current
    Box(if (fill) Modifier.fillMaxSize() else Modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            !preview && (item.kind == FileKind.IMAGE || item.kind == FileKind.VIDEO) -> SubcomposeAsyncImage(
                model = item.file,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = (if (fill) Modifier.fillMaxSize() else Modifier.size(size)).clip(RoundedCornerShape(corner)),
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
    if (b != null) Image(b.asImageBitmap(), null, Modifier.size(size))
    else KindGlyph(item, size)
}

/** 파란 세로 막대 + 굵은 제목 */
@Composable
fun BarTitle(title: String, modifier: Modifier = Modifier, fontSize: Int = 18) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(4.dp, (fontSize + 2).dp).clip(CircleShape).background(C.Accent))
        Spacer(Modifier.width(10.dp))
        Text(title, color = C.Text, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String) {
    BarTitle(title, Modifier.fillMaxWidth().padding(start = 20.dp, end = 24.dp, top = 18.dp, bottom = 4.dp), 17)
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
            .background(if (selected) C.Accent.copy(alpha = 0.07f) else Color.Transparent)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Icon(
                    if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    null, tint = if (selected) C.Accent else Color(0xFFB8BECC), modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            FileThumb(item, 48.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, color = C.Text, fontSize = 16.sp, maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(3.dp))
                Text(Format.date(item.modified, now), color = C.Sub, fontSize = 13.sp)
            }
            Spacer(Modifier.width(8.dp))
            if (item.isDir) Box(
                Modifier.clip(CircleShape).background(C.Chip).padding(horizontal = 14.dp, vertical = 6.dp)
            ) { Text("${item.childCount}개", color = C.SubStrong, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            else Text(Format.size(item.size), color = C.SubStrong, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (!selectionMode) Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFF3A3F52),
                modifier = Modifier.padding(start = 4.dp).size(24.dp)
            )
        }
        if (showDivider) Box(
            Modifier.padding(start = if (selectionMode) 116.dp else 82.dp, end = 18.dp)
                .fillMaxWidth().height(1.dp).background(C.Divider)
        )
    }
}

@Composable
fun IconBtn(icon: ImageVector, desc: String, tint: Color = C.Text, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(26.dp)) }
}
