package com.happyfamily.fileexplorer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happyfamily.fileexplorer.R
import com.happyfamily.fileexplorer.data.Category
import com.happyfamily.fileexplorer.data.FileItem
import com.happyfamily.fileexplorer.data.FileKind
import com.happyfamily.fileexplorer.data.StorageInfo

fun categoryIcon(c: Category): Pair<ImageVector, Color> = when (c) {
    Category.IMAGE -> Icons.Filled.Image to C.Image
    Category.VIDEO -> Icons.Filled.PlayArrow to C.Video
    Category.AUDIO -> Icons.Filled.MusicNote to C.Audio
    Category.DOCUMENT -> Icons.Filled.Description to C.Doc
    Category.DOWNLOAD -> Icons.Filled.Download to C.Download
    Category.APK -> Icons.Filled.Android to C.Apk
}

@Composable
fun HomeContent(
    recent: List<FileItem>,
    storage: StorageInfo,
    onSearch: () -> Unit,
    onMenuSort: () -> Unit,
    onMenuHidden: () -> Unit,
    showHidden: Boolean,
    onRecentClick: (FileItem) -> Unit,
    onRecentMore: () -> Unit,
    onCategory: (Category) -> Unit,
    onInternal: () -> Unit,
    onPrivacy: () -> Unit,
    now: Long = System.currentTimeMillis(),
) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, start = 18.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.app_logo), "파일 탐색기", Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
            Spacer(Modifier.width(10.dp))
            Text("파일 탐색기", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.Text)
            Spacer(Modifier.weight(1f))
            IconBtn(Icons.Outlined.Search, "검색", onClick = onSearch)
            Box {
                IconBtn(Icons.Outlined.MoreVert, "더보기") { menu = true }
                DropdownMenu(menu, { menu = false }, containerColor = Color.White) {
                    DropdownMenuItem({ Text("정렬 방식") }, { menu = false; onMenuSort() })
                    DropdownMenuItem(
                        { Text(if (showHidden) "숨김 파일 숨기기" else "숨김 파일 표시") },
                        { menu = false; onMenuHidden() })
                    DropdownMenuItem({ Text("개인정보 처리방침") }, { menu = false; onPrivacy() })
                }
            }
        }

        // 저장공간 카드
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp).fillMaxWidth().card(26.dp)
                .clickable(onClick = onInternal).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StorageRing(storage, 64.dp)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text("내장 저장공간", fontSize = 16.sp, color = C.Text, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = C.Accent, fontWeight = FontWeight.Bold, fontSize = 23.sp)) {
                        append(Format.sizeShort(storage.used))
                    }
                    withStyle(SpanStyle(color = C.Sub, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)) {
                        append(" / " + Format.sizeShort(storage.total))
                    }
                })
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.SubStrong, modifier = Modifier.size(28.dp))
        }

        if (recent.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                BarTitle("최근에 추가된 파일", Modifier.weight(1f))
                Row(
                    Modifier.clip(CircleShape).clickable(onClick = onRecentMore).padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("더보기", color = C.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.Accent, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(recent, key = { it.path }) { f ->
                    val media = f.kind == FileKind.IMAGE || f.kind == FileKind.VIDEO
                    Column(Modifier.width(124.dp).clip(RoundedCornerShape(16.dp)).clickable { onRecentClick(f) }) {
                        Box(
                            Modifier.padding(2.dp).fillMaxWidth().height(96.dp).card(20.dp),
                            contentAlignment = Alignment.Center
                        ) { FileThumb(f, if (media) 124.dp else 58.dp, 20.dp, fill = media) }
                        Spacer(Modifier.height(8.dp))
                        Text(Format.relative(f.modified, now), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = C.Text,
                            modifier = Modifier.padding(start = 6.dp))
                        Text(Format.middleEllipsis(f.name, 13), fontSize = 13.sp, color = C.Sub, maxLines = 1,
                            modifier = Modifier.padding(start = 6.dp, top = 1.dp, bottom = 4.dp))
                    }
                }
            }
        }

        BarTitle("카테고리", Modifier.padding(start = 20.dp, top = 22.dp, bottom = 12.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Category.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { cat ->
                        val (icon, color) = categoryIcon(cat)
                        Row(
                            Modifier.weight(1f).height(64.dp).card(18.dp).clickable { onCategory(cat) }
                                .padding(start = 9.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ColorGlyph(icon, color, 34.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(cat.label, fontSize = 14.sp, color = C.Text, fontWeight = FontWeight.SemiBold,
                                maxLines = 2, lineHeight = 17.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        BarTitle("저장공간", Modifier.padding(start = 20.dp, top = 24.dp, bottom = 4.dp))
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onInternal).padding(start = 22.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.PhoneAndroid, null, tint = C.Text, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Text("내장 저장공간", fontSize = 15.sp, color = C.Text, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(12.dp))
            val ratio = if (storage.total > 0) storage.used.toFloat() / storage.total else 0f
            Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(C.AccentSoft)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF4F8BFA), C.Accent))))
            }
            Spacer(Modifier.width(10.dp))
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = C.Accent, fontWeight = FontWeight.Bold)) { append(Format.sizeShort(storage.used)) }
                withStyle(SpanStyle(color = C.Sub)) { append(" / " + Format.sizeShort(storage.total)) }
            }, fontSize = 13.sp)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.SubStrong, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StorageRing(storage: StorageInfo, size: androidx.compose.ui.unit.Dp) {
    val ratio = if (storage.total > 0) (storage.used.toFloat() / storage.total).coerceIn(0f, 1f) else 0f
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = this.size.width * 0.12f
            val inset = sw / 2
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(C.AccentSoft, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
            drawArc(
                Brush.sweepGradient(listOf(Color(0xFF7FB0FF), C.Accent, Color(0xFF7FB0FF))),
                -90f, 360f * ratio, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round)
            )
        }
        Icon(Icons.Outlined.Storage, null, tint = C.Text, modifier = Modifier.size(size * 0.38f))
    }
}
