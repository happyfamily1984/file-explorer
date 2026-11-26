package com.happyfamily.fileexplorer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happyfamily.fileexplorer.data.SortBy
import com.happyfamily.fileexplorer.data.SortPrefs

fun directionLabels(by: SortBy): Pair<String, String> = when (by) { // (내림, 오름)
    SortBy.DATE -> "최신 순" to "오래된 순"
    SortBy.NAME -> "ㅎ→ㄱ, Z→A" to "ㄱ→ㅎ, A→Z"
    SortBy.TYPE -> "역순" to "이미지→문서→기타"
    SortBy.SIZE -> "큰 파일 먼저" to "작은 파일 먼저"
}

private fun description(by: SortBy) = when (by) {
    SortBy.DATE -> "최근 수정된 파일이 먼저 · 오늘/어제/이번 주로 묶기"
    SortBy.NAME -> "숫자는 1, 2, 10 순서로 정렬"
    SortBy.TYPE -> "이미지·동영상·문서 등 종류별로 묶기"
    SortBy.SIZE -> "용량 순서대로 보기"
}

/** 헤더 카드 안의 정렬 알약 버튼: 글자 → 정렬 시트, 화살표 → 방향 전환 */
@Composable
fun SortPill(prefs: SortPrefs, onClick: () -> Unit, onToggleDirection: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(Color.White)
            .border(1.dp, C.CardBorder, CircleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.clickable(onClick = onClick).padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Outlined.Sort, null, tint = C.Accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(prefs.sortBy.label, color = C.Accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier.clickable(onClick = onToggleDirection).padding(start = 2.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (prefs.isAscending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                "정렬 방향", tint = C.Accent, modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SortSheetContent(
    prefs: SortPrefs,
    onSortBy: (SortBy) -> Unit,
    onAscending: (Boolean) -> Unit,
    onFoldersFirst: (Boolean) -> Unit,
    onDone: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp)) {
        Text("정렬", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.Text)
        Spacer(Modifier.height(12.dp))
        SortBy.entries.forEach { by ->
            val sel = prefs.sortBy == by
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .clickable { onSortBy(by) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = sel, onClick = { onSortBy(by) },
                    colors = RadioButtonDefaults.colors(selectedColor = C.Accent))
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        by.label + if (by == SortBy.DATE) " (기본)" else "",
                        fontSize = 17.sp, color = C.Text,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(description(by), fontSize = 13.sp, color = C.Sub)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("순서", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = C.SubStrong)
        Spacer(Modifier.height(8.dp))
        val (descL, ascL) = directionLabels(prefs.sortBy)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Tile).padding(4.dp)
        ) {
            Segment(descL, !prefs.isAscending, Modifier.weight(1f)) { onAscending(false) }
            Segment(ascL, prefs.isAscending, Modifier.weight(1f)) { onAscending(true) }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().clickable { onFoldersFirst(!prefs.foldersFirst) }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("폴더를 위에 먼저 표시", fontSize = 16.sp, color = C.Text, modifier = Modifier.weight(1f))
            Switch(
                checked = prefs.foldersFirst, onCheckedChange = onFoldersFirst,
                colors = SwitchDefaults.colors(checkedTrackColor = C.Accent)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "선택한 정렬 방식은 바로 저장되어, 앱을 종료했다가 다시 열어도 그대로 유지됩니다.",
            fontSize = 13.sp, color = C.Sub, lineHeight = 18.sp
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = C.Accent)
        ) { Text("완료", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun Segment(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(11.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 14.sp,
            color = if (selected) C.Accent else C.SubStrong,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
