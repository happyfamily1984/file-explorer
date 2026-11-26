package com.happyfamily.fileexplorer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happyfamily.fileexplorer.data.Category
import com.happyfamily.fileexplorer.data.FileItem
import com.happyfamily.fileexplorer.data.StorageInfo

@Composable
fun HomeContent(
    recent: List<FileItem>,
    storage: StorageInfo,
    onSearch: () -> Unit,
    onMenuSort: () -> Unit,
    onMenuHidden: () -> Unit,
    showHidden: Boolean,
    onRecentClick: (FileItem) -> Unit,
    onCategory: (Category) -> Unit,
    onInternal: () -> Unit,
    onPrivacy: () -> Unit,
    now: Long = System.currentTimeMillis(),
) {
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(C.Bg).verticalScroll(rememberScrollState())
            .statusBarsPadding().navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, end = 12.dp), horizontalArrangement = Arrangement.End) {
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

        if (recent.isNotEmpty()) {
            Row(Modifier.padding(start = 28.dp, top = 40.dp)) {
                Text("최근에 추가된 파일 ${recent.size}개", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = C.Text)
                Box(Modifier.padding(start = 3.dp, top = 2.dp).size(7.dp).clip(CircleShape).background(Color(0xFFE2663A)))
            }
            Spacer(Modifier.height(22.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(recent, key = { it.path }) { f ->
                    Column(Modifier.width(114.dp).clickable { onRecentClick(f) }) {
                        Box(
                            Modifier.size(114.dp).clip(RoundedCornerShape(22.dp))
                                .background(Color.White)
                                .border(BorderStroke(1.dp, C.Divider), RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) { FileThumb(f, if (f.kind.name == "IMAGE" || f.kind.name == "VIDEO") 114.dp else 64.dp, 22.dp) }
                        Spacer(Modifier.height(10.dp))
                        Text(Format.relative(f.modified, now), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = C.Text,
                            modifier = Modifier.padding(start = 4.dp))
                        Text(Format.middleEllipsis(f.name), fontSize = 14.sp, color = C.Sub, maxLines = 1,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                    }
                }
            }
        }

        SectionTitle("카테고리", top = if (recent.isEmpty()) 24 else 40)
        val cats = listOf(
            Triple(Category.IMAGE, Icons.Outlined.Image, C.Image),
            Triple(Category.VIDEO, Icons.Outlined.SmartDisplay, C.Video),
            Triple(Category.AUDIO, Icons.Outlined.MusicNote, C.Audio),
            Triple(Category.DOCUMENT, Icons.Outlined.Description, C.Doc),
            Triple(Category.DOWNLOAD, Icons.Outlined.Download, C.Download),
            Triple(Category.APK, null, C.Apk),
        )
        Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            cats.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { (cat, icon, color) ->
                        CategoryTile(cat.label, icon, color, Modifier.weight(1f)) { onCategory(cat) }
                    }
                }
            }
        }

        SectionTitle("저장공간", top = 36)
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onInternal).padding(start = 28.dp, end = 22.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.PhoneAndroid, null, tint = C.Text, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(26.dp))
            Text("내장 저장공간", fontSize = 19.sp, color = C.Text, modifier = Modifier.weight(1f))
            PillProgress(storage.used, storage.total, Modifier.width(150.dp))
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(text: String, top: Int) {
    Text(text, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = C.Text,
        modifier = Modifier.padding(start = 28.dp, top = top.dp, bottom = 20.dp))
}

@Composable
private fun CategoryTile(label: String, icon: ImageVector?, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(112.dp).clip(RoundedCornerShape(20.dp)).background(C.Tile).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        else Text("APK", color = color, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        Text(label, fontSize = 16.sp, color = C.Text)
    }
}
