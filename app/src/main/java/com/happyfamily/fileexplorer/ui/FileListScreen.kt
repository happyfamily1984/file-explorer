package com.happyfamily.fileexplorer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happyfamily.fileexplorer.data.FileItem
import com.happyfamily.fileexplorer.data.Section
import com.happyfamily.fileexplorer.data.SortPrefs

data class Crumb(val label: String, val path: String?)

class ListActions(
    val onBack: () -> Unit,
    val onSearchOpen: () -> Unit,
    val onSearchChange: (String) -> Unit,
    val onCrumb: (Crumb) -> Unit,
    val onHome: () -> Unit,
    val onItemClick: (FileItem) -> Unit,
    val onItemLongClick: (FileItem) -> Unit,
    val onSortClick: () -> Unit,
    val onToggleDirection: () -> Unit,
    val onToggleHidden: () -> Unit,
    val onNewFolder: (() -> Unit)?,
    val onSelectAll: () -> Unit,
    val onShare: () -> Unit,
    val onRename: () -> Unit,
    val onDelete: () -> Unit,
)

@Composable
fun FileListContent(
    crumbs: List<Crumb>,
    headerSize: String?,
    sections: List<Section>,
    prefs: SortPrefs,
    loading: Boolean,
    selection: Set<String>,
    searchQuery: String?,
    a: ListActions,
    listState: LazyListState = rememberLazyListState(),
    now: Long = System.currentTimeMillis(),
) {
    val total = sections.sumOf { it.items.size }
    val selecting = selection.isNotEmpty()
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        // ── 상단 바
        when {
            selecting -> SelectionTopBar(selection.size, total, a.onSelectAll)
            searchQuery != null -> SearchTopBar(searchQuery, a.onBack, a.onSearchChange)
            else -> {
                var menu by remember { mutableStateOf(false) }
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp, start = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBtn(Icons.AutoMirrored.Outlined.ArrowBackIos, "뒤로", onClick = a.onBack)
                    Spacer(Modifier.weight(1f))
                    IconBtn(Icons.Outlined.Search, "검색", onClick = a.onSearchOpen)
                    Box {
                        IconBtn(Icons.Outlined.MoreVert, "더보기") { menu = true }
                        DropdownMenu(menu, { menu = false }, containerColor = Color.White) {
                            DropdownMenuItem({ Text("정렬 방식") }, { menu = false; a.onSortClick() })
                            DropdownMenuItem({ Text("편집 (선택)") }, { menu = false; a.onSelectAll() })
                            a.onNewFolder?.let { nf -> DropdownMenuItem({ Text("새 폴더") }, { menu = false; nf() }) }
                            DropdownMenuItem(
                                { Text(if (prefs.showHidden) "숨김 파일 숨기기" else "숨김 파일 표시") },
                                { menu = false; a.onToggleHidden() })
                        }
                    }
                }
            }
        }

        // ── 경로 표시
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 24.dp, top = 18.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(width = 38.dp, height = 32.dp).clip(RoundedCornerShape(9.dp))
                    .border(1.5.dp, Color(0xFF8E8E94), RoundedCornerShape(9.dp)).clickable(onClick = a.onHome),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Home, "홈", tint = Color(0xFF6E6E73), modifier = Modifier.size(20.dp)) }
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState(Int.MAX_VALUE)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                crumbs.forEachIndexed { i, c ->
                    Icon(Icons.Filled.ArrowRight, null, tint = Color(0xFFB5B5BB), modifier = Modifier.size(26.dp))
                    val last = i == crumbs.lastIndex
                    Text(
                        c.label, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        color = if (last) C.Accent else C.Sub,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = !last && c.path != null) { a.onCrumb(c) }
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    )
                }
            }
            if (headerSize != null) Text(headerSize, color = C.SubStrong, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        SortBar(prefs, total, a.onSortClick, a.onToggleDirection)

        Box(Modifier.weight(1f)) {
            if (!loading && total == 0) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.FolderOff, null, tint = Color(0xFFC6C6CC), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(if (searchQuery.isNullOrEmpty()) "파일이 없습니다" else "검색 결과가 없습니다", color = C.Sub, fontSize = 15.sp)
                }
            }
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = C.Accent)
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                sections.forEach { sec ->
                    if (sec.title != null) item(key = "h_" + sec.title) { SectionHeader(sec.title) }
                    items(sec.items, key = { it.path }) { f ->
                        FileRow(
                            item = f, selectionMode = selecting, selected = f.path in selection,
                            showDivider = f !== sec.items.last(),
                            onClick = { a.onItemClick(f) }, onLongClick = { a.onItemLongClick(f) }, now = now,
                        )
                    }
                }
            }
        }

        if (selecting) SelectionBottomBar(selection.size == 1, a)
    }
}

@Composable
private fun SelectionTopBar(count: Int, total: Int, onSelectAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, start = 12.dp, end = 12.dp).height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onSelectAll).padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (count == total) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                "전체 선택", tint = if (count == total) C.Accent else Color(0xFFB8B8BE), modifier = Modifier.size(24.dp)
            )
            Text("전체", fontSize = 11.sp, color = C.SubStrong)
        }
        Spacer(Modifier.width(12.dp))
        Text("${count}개 선택됨", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.Text)
    }
}

@Composable
private fun SearchTopBar(query: String, onBack: () -> Unit, onChange: (String) -> Unit) {
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, start = 6.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBtn(Icons.AutoMirrored.Outlined.ArrowBackIos, "뒤로", onClick = onBack)
        Row(
            Modifier.weight(1f).height(46.dp).clip(CircleShape).background(C.Tile).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, null, tint = C.Sub, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("파일 이름 검색", color = C.Sub, fontSize = 16.sp)
                BasicTextField(
                    query, onChange, singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp, color = C.Text),
                    cursorBrush = SolidColor(C.Accent),
                    modifier = Modifier.fillMaxWidth().focusRequester(fr)
                )
            }
            if (query.isNotEmpty()) Icon(
                Icons.Outlined.Close, "지우기", tint = C.Sub,
                modifier = Modifier.size(20.dp).clickable { onChange("") }
            )
        }
    }
}

@Composable
private fun SelectionBottomBar(single: Boolean, a: ListActions) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomAction(Icons.Outlined.Share, "공유", true, a.onShare)
        BottomAction(Icons.Outlined.DriveFileRenameOutline, "이름 변경", single, a.onRename)
        BottomAction(Icons.Outlined.Delete, "삭제", true, a.onDelete)
    }
}

@Composable
private fun BottomAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) C.Text else Color(0xFFC6C6CC)
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = tint)
    }
}
