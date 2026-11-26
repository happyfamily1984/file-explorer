package com.happyfamily.fileexplorer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object C {
    val Bg = Color(0xFFFAFAFC)
    val Tile = Color(0xFFF2F2F5)
    val Text = Color(0xFF1B1B1D)
    val Sub = Color(0xFF8B8B90)
    val SubStrong = Color(0xFF6E6E73)
    val Divider = Color(0xFFE3E3E7)
    val Accent = Color(0xFF3E6AE1)
    val Folder = Color(0xFF9AAACB)
    val PillFill = Color(0xFF7D9DF6)
    val PillTrack = Color(0xFFE4EAFC)
    val Image = Color(0xFFE0577A)
    val Video = Color(0xFF8A3FD1)
    val Audio = Color(0xFF6B72E0)
    val Doc = Color(0xFFE0A85A)
    val Download = Color(0xFF5CC0A6)
    val Apk = Color(0xFF8CC63F)
    val Archive = Color(0xFF9AAACB)
    val Other = Color(0xFF9E9EA6)
    val Red = Color(0xFFE5484D)
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
