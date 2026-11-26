package com.happyfamily.fileexplorer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object C {
    val Bg = Color(0xFFF4F7FD)
    val Card = Color(0xFFFFFFFF)
    val CardBorder = Color(0xFFE4EAF6)
    val Tile = Color(0xFFF0F3FA)
    val Text = Color(0xFF1A1D29)
    val Sub = Color(0xFF7A8094)
    val SubStrong = Color(0xFF4A5064)
    val Divider = Color(0xFFE6EAF3)
    val Accent = Color(0xFF2F6BF0)
    val AccentSoft = Color(0xFFE4ECFD)
    val Chip = Color(0xFFECF0F9)
    val Folder = Color(0xFF5B9BF8)
    val FolderTop = Color(0xFF7DB8FF)
    val FolderBottom = Color(0xFF3B7CF4)
    val FolderBack = Color(0xFF2E68E3)
    val Image = Color(0xFFF0577A)
    val Video = Color(0xFF8B5CF6)
    val Audio = Color(0xFF3B6EF5)
    val Doc = Color(0xFFF7B32B)
    val Download = Color(0xFF22B573)
    val Apk = Color(0xFF7CC531)
    val Archive = Color(0xFFA9B6D3)
    val Other = Color(0xFF9AA3B8)
    val Red = Color(0xFFE5484D)
    // 호환용 별칭
    val PillFill = Accent
    val PillTrack = AccentSoft
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.Accent,
            background = C.Bg,
            surface = C.Bg,
            onBackground = C.Text,
            onSurface = C.Text,
            surfaceContainerLow = C.Bg,
            surfaceContainer = C.Bg,
            surfaceContainerHigh = C.Bg,
        ),
        content = content,
    )
}

/** 시안의 은은한 파란 물결 배경 */
@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(C.Bg).drawBehind {
            drawRect(
                Brush.verticalGradient(
                    0f to Color(0xFFDCE7FD), 0.28f to Color(0xFFEEF3FE), 0.45f to C.Bg,
                )
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0x5591B8FA), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.02f), radius = size.width * 0.75f,
                ),
                radius = size.width * 0.75f, center = Offset(size.width * 0.95f, size.height * 0.02f),
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0x3391B8FA), Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 0.12f), radius = size.width * 0.55f,
                ),
                radius = size.width * 0.55f, center = Offset(size.width * 0.05f, size.height * 0.12f),
            )
        }
    ) { content() }
}
