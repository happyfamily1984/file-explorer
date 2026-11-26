package com.happyfamily.fileexplorer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.happyfamily.fileexplorer.data.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun extLabel(ext: String) = ".$ext"

/** "어떤 앱으로 열까요?" — 앱 선택 + 항상 이 앱으로 열기 */
@Composable
fun OpenWithContent(
    item: FileItem,
    apps: List<AppChoice>,
    always: Boolean,
    onAlwaysChange: (Boolean) -> Unit,
    onPick: (AppChoice) -> Unit,
) {
    val ext = item.extension
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 24.dp)
    ) {
        Text("어떤 앱으로 열까요?", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = C.Text)
        Spacer(Modifier.height(4.dp))
        Text(item.name, fontSize = 14.sp, color = C.Sub, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)

        if (ext.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(if (always) C.AccentSoft else C.Tile)
                    .border(1.dp, if (always) C.Accent.copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(18.dp))
                    .clickable { onAlwaysChange(!always) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (always) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank, null,
                    tint = if (always) C.Accent else C.Sub, modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("${extLabel(ext)} 파일은 항상 이 앱으로 열기", fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, color = C.Text)
                    Text(
                        "체크하고 앱을 고르면 다음부터 묻지 않고 열려요.\n설정 > 기본으로 열 앱에서 언제든 바꿀 수 있어요.",
                        fontSize = 12.5.sp, color = C.SubStrong, lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        apps.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { app -> AppCell(app, Modifier.weight(1f)) { onPick(app) } }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AppCell(app: AppChoice, modifier: Modifier, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val icon by produceState<Bitmap?>(null, app.component) {
        value = withContext(Dispatchers.IO) {
            runCatching { app.info?.loadIcon(ctx.packageManager)?.toBitmap(144, 144) }.getOrNull()
        }
    }
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val b = icon
        if (b != null) Image(b.asImageBitmap(), app.label, Modifier.size(54.dp))
        else ColorGlyph(Icons.Outlined.Apps, C.Accent, 54.dp)
        Spacer(Modifier.height(8.dp))
        Text(app.label, fontSize = 12.5.sp, color = C.Text, textAlign = TextAlign.Center, maxLines = 2,
            lineHeight = 16.sp, overflow = TextOverflow.Ellipsis)
    }
}
