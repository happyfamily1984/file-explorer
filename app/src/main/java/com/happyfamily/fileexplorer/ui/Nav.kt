package com.happyfamily.fileexplorer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happyfamily.fileexplorer.MainViewModel
import com.happyfamily.fileexplorer.Tab
import com.happyfamily.fileexplorer.data.SortPrefs

private fun tabIcons(t: Tab): Pair<ImageVector, ImageVector> = when (t) {
    Tab.HOME -> Icons.Filled.Home to Icons.Outlined.Home
    Tab.FILES -> Icons.Filled.Folder to Icons.Outlined.Folder
    Tab.RECENT -> Icons.Filled.Schedule to Icons.Outlined.Schedule
    Tab.FAVORITES -> Icons.Filled.Star to Icons.Outlined.StarBorder
    Tab.SETTINGS -> Icons.Filled.Settings to Icons.Outlined.Settings
}

/** 떠 있는 둥근 하단 탭 바 */
@Composable
fun BottomNav(current: Tab, onTab: (Tab) -> Unit) {
    Row(
        Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp).navigationBarsPadding()
            .fillMaxWidth().card(30.dp).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Tab.entries.forEach { t ->
            val sel = t == current
            val (filled, outlined) = tabIcons(t)
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(24.dp))
                    .background(if (sel) C.AccentSoft else Color.Transparent)
                    .clickable { onTab(t) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(if (sel) filled else outlined, t.label, tint = if (sel) C.Accent else C.SubStrong, modifier = Modifier.size(25.dp))
                Spacer(Modifier.height(3.dp))
                Text(t.label, fontSize = 11.sp, maxLines = 1,
                    color = if (sel) C.Accent else C.SubStrong, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
fun SettingsRoute(vm: MainViewModel, prefs: SortPrefs, onBack: () -> Unit, onSort: () -> Unit, onPrivacy: () -> Unit) {
    BackHandler(onBack = onBack)
    SettingsContent(
        prefs, onBack, onSort,
        onFoldersFirst = { vm.setFoldersFirst(it) },
        onShowHidden = { vm.setShowHidden(it) },
        onPrivacy = onPrivacy,
    )
}

@Composable
fun SettingsContent(
    prefs: SortPrefs, onBack: () -> Unit, onSort: () -> Unit,
    onFoldersFirst: (Boolean) -> Unit, onShowHidden: (Boolean) -> Unit, onPrivacy: () -> Unit,
) {
    val (descL, ascL) = directionLabels(prefs.sortBy)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()) {
        Row(Modifier.padding(top = 10.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.AutoMirrored.Outlined.ArrowBackIos, "뒤로", onClick = onBack)
            Text("설정", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.Text)
        }
        BarTitle("정렬", Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp), 16)
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().card(24.dp)) {
            SettingRow(Icons.AutoMirrored.Outlined.Sort, "정렬 방식",
                "${prefs.sortBy.label} · ${if (prefs.isAscending) ascL else descL}", onClick = onSort)
            SettingDivider()
            SettingSwitch(Icons.Outlined.CreateNewFolder, "폴더를 위에 먼저 표시", prefs.foldersFirst, onFoldersFirst)
            SettingDivider()
            SettingSwitch(Icons.Outlined.VisibilityOff, "숨김 파일 표시", prefs.showHidden, onShowHidden)
        }
        Text(
            "정렬 방식은 바꾸는 즉시 저장되어 앱을 종료했다가 다시 열어도 유지됩니다.",
            fontSize = 13.sp, color = C.Sub, lineHeight = 18.sp, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp)
        )
        BarTitle("정보", Modifier.padding(start = 20.dp, top = 24.dp, bottom = 10.dp), 16)
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().card(24.dp)) {
            SettingRow(Icons.Outlined.Shield, "개인정보 처리방침", null, onClick = onPrivacy)
            SettingDivider()
            SettingRow(Icons.Outlined.Info, "앱 버전", "1.1.0", onClick = null)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SettingDivider() = Box(Modifier.padding(start = 62.dp).fillMaxWidth().height(1.dp).background(C.Divider))

@Composable
private fun SettingRow(icon: ImageVector, title: String, sub: String?, onClick: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = C.Accent, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = C.Text, fontWeight = FontWeight.Medium)
            if (sub != null) Text(sub, fontSize = 13.sp, color = C.Sub)
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.SubStrong)
    }
}

@Composable
private fun SettingSwitch(icon: ImageVector, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = C.Accent, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(18.dp))
        Text(title, fontSize = 16.sp, color = C.Text, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = C.Accent))
    }
}
