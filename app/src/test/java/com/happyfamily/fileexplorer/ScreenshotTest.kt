package com.happyfamily.fileexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.happyfamily.fileexplorer.data.*
import com.happyfamily.fileexplorer.ui.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

@RunWith(org.robolectric.RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h866dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private val now: Long = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 27, 0, 20, 0)
    }.timeInMillis

    private fun t(daysAgo: Int, h: Int, m: Int): Long = Calendar.getInstance().apply {
        timeInMillis = now; add(Calendar.DAY_OF_MONTH, -daysAgo)
        set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
    }.timeInMillis

    private val base = "/storage/emulated/0/Download"
    private fun f(name: String, size: Long, time: Long) = FileItem("$base/$name", name, false, size, time)
    private fun d(name: String, n: Int, time: Long) = FileItem("$base/$name", name, true, 0, time, n)

    private val download = listOf(
        d("ShortDramaMaker", 12, t(0, 0, 6)),
        f("rider-toiletmap-store (1).zip", 19_493_000, t(0, 0, 14)),
        f("ShortDramaMaker-2.4.9.apk", 8_126_000, t(0, 0, 11)),
        f("ShortDramaMaker-2.4.8.apk", 8_126_000, t(0, 0, 4)),
        f("baemin-store-guide-8.png", 1_204_000, t(0, 0, 2)),
        d("logs", 32, t(1, 17, 13)),
        d("Telegram", 36, t(2, 21, 49)),
        d("DaumMail", 38, t(2, 13, 5)),
        f("2026 가계부.xlsx", 84_000, t(3, 9, 30)),
        f("여행 일정표.pdf", 2_310_000, t(9, 14, 2)),
        f("회의록_11월.hwp", 152_000, t(12, 10, 41)),
        f("bgm_summer.mp3", 4_820_000, t(30, 20, 11)),
        f("가족사진_001.jpg", 3_402_000, t(40, 12, 0)),
        f("영상편집_최종.mp4", 182_400_000, t(45, 19, 22)),
    )

    private val noop = ListActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})

    @Composable
    private fun List(prefs: SortPrefs, selection: Set<String> = emptySet()) {
        FileListContent(
            crumbs = listOf(Crumb("다운로드", base)),
            headerSize = "10.09 GB",
            sections = Sorter.sort(download, prefs, now),
            prefs = prefs, loading = false, selection = selection, searchQuery = null,
            a = noop, now = now,
        )
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.setContent { AppTheme { content() } }
        rule.onRoot().captureRoboImage("../screenshots/$name.png")
    }

    @Test fun s1_home() = shot("01_home") {
        HomeContent(
            recent = download.filter { !it.isDir }.sortedByDescending { it.modified }.take(4),
            storage = StorageInfo(152_000_000_000, 256_000_000_000),
            onSearch = {}, onMenuSort = {}, onMenuHidden = {}, showHidden = false,
            onRecentClick = {}, onCategory = {}, onInternal = {}, onPrivacy = {}, now = now,
        )
    }

    @Test fun s2_date() = shot("02_list_date") { List(SortPrefs()) }

    @Test fun s3_sheet() = shot("03_sort_sheet") {
        Box(Modifier.fillMaxSize()) {
            List(SortPrefs())
            Box(Modifier.fillMaxSize().background(Color(0x66000000)))
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Color.White)
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(vertical = 14.dp)
                    .size(36.dp, 4.dp).clip(CircleShape).background(Color(0xFFD0D0D5)))
                SortSheetContent(SortPrefs(), {}, {}, {}, {})
            }
        }
    }

    @Test fun s4_type() = shot("04_list_type") {
        List(SortPrefs(sortBy = SortBy.TYPE))
    }

    @Test fun s5_name_select() = shot("05_list_name_select") {
        List(SortPrefs(sortBy = SortBy.NAME, foldersFirst = true),
            selection = setOf("$base/logs", "$base/Telegram"))
    }
}
