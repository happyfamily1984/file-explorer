package com.happyfamily.fileexplorer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.happyfamily.fileexplorer.data.FileItem
import com.happyfamily.fileexplorer.data.SettingsRepository
import com.happyfamily.fileexplorer.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 다른 앱에서 "열기"를 누르면 거치는 가벼운 화면.
 * - 그 형식에 "항상 이 앱으로 열기"가 저장돼 있으면 화면 없이 바로 넘겨준다.
 * - 아니면 앱 선택 시트 + "파일 탐색기에서 위치 보기"를 보여준다.
 * 투명 화면이라 원래 보던 앱 위에 시트만 뜨고, 끝나면 바로 원래 앱으로 돌아간다.
 */
class OpenActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) { IncomingFile.from(this@OpenActivity, intent) }
            if (file == null) { finish(); return@launch }
            val saved = SettingsRepository(this@OpenActivity).openWith.first()[file.extension]
            if (saved != null && file.extension.isNotEmpty()) {
                if (file.launchWith(this@OpenActivity, saved)) { finish(); return@launch }
                vm.setOpenWith(file.extension, null)
            }
            val apps = withContext(Dispatchers.IO) { file.candidates(this@OpenActivity) }
            setContent { AppTheme { Sheet(file, apps) } }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Sheet(file: IncomingFile, apps: List<AppChoice>) {
        var always by remember { mutableStateOf(false) }
        val pseudo = remember { FileItem(file.uri.toString(), file.name, false, file.size, 0) }
        ModalBottomSheet(
            onDismissRequest = { finish() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
        ) {
            if (file.localPath != null) {
                OutlinedButton(
                    onClick = { reveal(file.localPath) },
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp).fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Outlined.FolderOpen, null, tint = C.Accent)
                    Spacer(Modifier.width(8.dp))
                    Text("파일 탐색기에서 위치 보기", color = C.Accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            if (apps.isEmpty()) Text(
                "이 파일을 열 수 있는 다른 앱이 없어요.", color = C.Sub, fontSize = 14.sp,
                modifier = Modifier.padding(20.dp)
            ) else OpenWithContent(pseudo, apps, always, { always = it }) { app ->
                if (always && file.extension.isNotEmpty()) {
                    vm.setOpenWith(file.extension, app.component)
                    Toast.makeText(this@OpenActivity, "이제 ${extLabel(file.extension)} 파일은 '${app.label}'(으)로 바로 열려요", Toast.LENGTH_SHORT).show()
                }
                if (!file.launchWith(this@OpenActivity, app.component))
                    Toast.makeText(this@OpenActivity, "'${app.label}'(으)로 열 수 없어요", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun reveal(path: String) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_FOLDER, File(path).parent)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }
}
