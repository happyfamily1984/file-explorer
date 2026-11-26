package com.happyfamily.fileexplorer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happyfamily.fileexplorer.data.*
import com.happyfamily.fileexplorer.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class Route {
    data object Home : Route()
    data class Folder(val path: String) : Route()
    data class Cat(val category: Category) : Route()
    data object Search : Route()
    data object Recent : Route()
    data object Favorites : Route()
    data object Settings : Route()

    fun encode(): String = when (this) {
        Home -> "H"
        Search -> "S"
        Recent -> "R"
        Favorites -> "V"
        Settings -> "T"
        is Folder -> "F:$path"
        is Cat -> "C:${category.name}"
    }

    companion object {
        fun decode(s: String): Route = when {
            s == "S" -> Search
            s == "R" -> Recent
            s == "V" -> Favorites
            s == "T" -> Settings
            s.startsWith("F:") -> Folder(s.removePrefix("F:"))
            s.startsWith("C:") -> Cat(Category.valueOf(s.removePrefix("C:")))
            else -> Home
        }
    }
}

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { App(vm, onExit = { finish() }) } }
    }
}

fun hasStoragePermission(ctx: android.content.Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
    else ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

@Composable
fun App(vm: MainViewModel, onExit: () -> Unit) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(hasStoragePermission(ctx)) }
    LifecycleResumeEffect(Unit) { granted = hasStoragePermission(ctx); onPauseOrDispose { } }

    val prefs = vm.prefs.collectAsStateWithLifecycle().value
    if (prefs == null) { Box(Modifier.fillMaxSize().background(C.Bg)); return }

    if (!granted) {
        PermissionScreen(onGranted = { granted = hasStoragePermission(ctx) }, onExit = onExit)
        return
    }

    val stack = rememberSaveable(
        saver = listSaver<SnapshotStateList<Route>, String>(
            save = { it.map(Route::encode) },
            restore = { mutableStateListOf<Route>().apply { addAll(it.map(Route::decode)) } },
        )
    ) { mutableStateListOf<Route>(Route.Home) }
    val push: (Route) -> Unit = { stack.add(it) }
    val pop: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    val goHome: () -> Unit = { while (stack.size > 1) stack.removeAt(stack.lastIndex) }

    var showSort by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    val rootPath = vm.repo.root.absolutePath
    val openTab: (Tab) -> Unit = { t ->
        goHome()
        when (t) {
            Tab.HOME -> {}
            Tab.FILES -> stack.add(Route.Folder(rootPath))
            Tab.RECENT -> stack.add(Route.Recent)
            Tab.FAVORITES -> stack.add(Route.Favorites)
            Tab.SETTINGS -> stack.add(Route.Settings)
        }
    }
    val currentTab = when (val first = stack.getOrNull(1)) {
        null -> Tab.HOME
        Route.Recent -> Tab.RECENT
        Route.Favorites -> Tab.FAVORITES
        Route.Settings -> Tab.SETTINGS
        is Route.Folder -> if (first.path == rootPath) Tab.FILES else Tab.HOME
        else -> Tab.HOME
    }

    AppBackground {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (val r = stack.last()) {
                    Route.Home -> HomeRoute(vm, prefs, push, onSort = { showSort = true }, onPrivacy = { showPrivacy = true },
                        onRecentMore = { openTab(Tab.RECENT) }, onExit = onExit)
                    Route.Settings -> SettingsRoute(vm, prefs, onBack = pop, onSort = { showSort = true }, onPrivacy = { showPrivacy = true })
                    else -> key(stack.size, r) {
                        ListRoute(vm, r, prefs, push = push, pop = pop, goHome = goHome, replaceTop = { stack[stack.lastIndex] = it },
                            onSort = { showSort = true }, onSelecting = { selecting = it })
                    }
                }
            }
            if (!selecting) BottomNav(currentTab, openTab)
        }
    }

    if (showSort) SortBottomSheet(vm, prefs) { showSort = false }
    if (showPrivacy) PrivacyDialog { showPrivacy = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(vm: MainViewModel, prefs: SortPrefs, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Color.White) {
        SortSheetContent(
            prefs = prefs,
            onSortBy = { vm.setSortBy(it) },
            onAscending = { vm.setAscending(prefs.sortBy, it) },
            onFoldersFirst = { vm.setFoldersFirst(it) },
            onDone = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } },
        )
    }
}

@Composable
private fun HomeRoute(
    vm: MainViewModel, prefs: SortPrefs, push: (Route) -> Unit,
    onSort: () -> Unit, onPrivacy: () -> Unit, onRecentMore: () -> Unit, onExit: () -> Unit,
) {
    val ctx = LocalContext.current
    var recent by remember { mutableStateOf(emptyList<FileItem>()) }
    var storage by remember { mutableStateOf(StorageInfo(0, 0)) }
    var askExit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(Unit) {
        val job = scope.launch {
            recent = vm.repo.recent()
            storage = withContext(Dispatchers.IO) { vm.repo.storage() }
        }
        onPauseOrDispose { job.cancel() }
    }
    BackHandler { askExit = true }

    HomeContent(
        recent = recent, storage = storage,
        onSearch = { push(Route.Search) },
        onMenuSort = onSort,
        onMenuHidden = { vm.setShowHidden(!prefs.showHidden) },
        showHidden = prefs.showHidden,
        onRecentClick = { FileActions.open(ctx, it) },
        onRecentMore = onRecentMore,
        onCategory = {
            if (it == Category.DOWNLOAD) push(Route.Folder(vm.repo.downloadDir.absolutePath)) else push(Route.Cat(it))
        },
        onInternal = { push(Route.Folder(vm.repo.root.absolutePath)) },
        onPrivacy = onPrivacy,
    )

    if (askExit) AlertDialog(
        onDismissRequest = { askExit = false },
        containerColor = Color.White,
        title = { Text("앱 종료", fontWeight = FontWeight.Bold) },
        text = { Text("앱을 종료하겠습니까?") },
        confirmButton = { TextButton({ askExit = false; onExit() }) { Text("예", color = C.Accent, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton({ askExit = false }) { Text("아니오", color = C.SubStrong) } },
    )
}

@Composable
private fun ListRoute(
    vm: MainViewModel, route: Route, prefs: SortPrefs,
    push: (Route) -> Unit, pop: () -> Unit, goHome: () -> Unit, replaceTop: (Route) -> Unit,
    onSort: () -> Unit, onSelecting: (Boolean) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val repo = vm.repo
    val rootPath = repo.root.absolutePath
    val downloadPath = repo.downloadDir.absolutePath

    var items by remember { mutableStateOf(emptyList<FileItem>()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var selection by remember { mutableStateOf(emptySet<String>()) }
    var query by rememberSaveable { mutableStateOf(if (route == Route.Search) "" else null as String?) }
    var folderSize by remember { mutableStateOf<Long?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var newFolder by remember { mutableStateOf(false) }

    // 데이터 불러오기 — 화면에 돌아올 때마다 새로고침
    LifecycleResumeEffect(route, prefs.showHidden) { reload++; onPauseOrDispose { } }
    if (route == Route.Search) {
        LaunchedEffect(query, prefs.showHidden) {
            val q = query.orEmpty()
            if (q.isBlank()) { items = emptyList(); loading = false; return@LaunchedEffect }
            delay(300); loading = true
            items = repo.search(q, prefs.showHidden); loading = false
        }
    } else {
        LaunchedEffect(route, prefs.showHidden, reload, if (route == Route.Favorites) favorites else null) {
            if (reload == 0) return@LaunchedEffect
            if (items.isEmpty()) loading = true
            items = when (route) {
                is Route.Folder -> repo.list(route.path, prefs.showHidden)
                is Route.Cat -> repo.category(route.category, prefs.showHidden)
                Route.Recent -> repo.recent(300)
                Route.Favorites -> repo.paths(favorites, prefs.showHidden)
                else -> emptyList()
            }
            loading = false
            selection = selection.filter { p -> items.any { it.path == p } }.toSet()
        }
    }
    if (route is Route.Folder) LaunchedEffect(route.path, reload) {
        folderSize = withContext(Dispatchers.IO) { dirSize(File(route.path)) }
    }

    val shown = remember(items, query, route) {
        val q = query
        if (route != Route.Search && !q.isNullOrBlank()) items.filter { it.name.contains(q, ignoreCase = true) } else items
    }
    val sections = remember(shown, prefs) { Sorter.sort(shown, prefs) }

    val crumbs = when (route) {
        is Route.Folder -> folderCrumbs(route.path, rootPath, downloadPath)
        is Route.Cat -> listOf(Crumb(route.category.label, null))
        Route.Recent -> listOf(Crumb("최근 항목", null))
        Route.Favorites -> listOf(Crumb("즐겨찾기", null))
        else -> listOf(Crumb("검색", null))
    }
    val headerSize = when (route) {
        is Route.Folder -> folderSize?.let { Format.size(it) }
        else -> Format.size(shown.sumOf { it.size })
    }
    val headerBadge = when (route) {
        is Route.Folder -> if (route.path.startsWith(downloadPath)) Icons.Filled.Download else null
        is Route.Cat -> categoryIcon(route.category).first
        Route.Recent -> Icons.Filled.Schedule
        Route.Favorites -> Icons.Filled.Star
        else -> Icons.Filled.Search
    }
    val selectingNow = selection.isNotEmpty()
    LaunchedEffect(selectingNow) { onSelecting(selectingNow) }
    DisposableEffect(Unit) { onDispose { onSelecting(false) } }

    // 뒤로가기: 선택 모드 해제 → 검색창 닫기 → 이전 화면
    BackHandler(enabled = selection.isNotEmpty() || (route != Route.Search && query != null)) {
        if (selection.isNotEmpty()) selection = emptySet() else query = null
    }
    BackHandler(enabled = selection.isEmpty() && (route == Route.Search || query == null)) { pop() }

    val selectedItems = items.filter { it.path in selection }
    FileListContent(
        crumbs = crumbs, headerSize = headerSize, headerBadge = headerBadge, sections = sections, prefs = prefs,
        loading = loading, selection = selection, searchQuery = query,
        selectionAllFavorite = selection.isNotEmpty() && selection.all { it in favorites },
        a = ListActions(
            onBack = { if (route != Route.Search && query != null) query = null else pop() },
            onSearchOpen = { query = "" },
            onSearchChange = { query = it },
            onCrumb = { c -> c.path?.let { replaceTop(Route.Folder(it)) } },
            onHome = goHome,
            onItemClick = { f ->
                when {
                    selection.isNotEmpty() -> selection = if (f.path in selection) selection - f.path else selection + f.path
                    f.isDir -> push(Route.Folder(f.path))
                    else -> FileActions.open(ctx, f)
                }
            },
            onItemLongClick = { f -> selection = selection + f.path },
            onSortClick = onSort,
            onToggleDirection = { vm.setAscending(prefs.sortBy, !prefs.isAscending) },
            onToggleHidden = { vm.setShowHidden(!prefs.showHidden) },
            onNewFolder = if (route is Route.Folder) ({ newFolder = true }) else null,
            onSelectAll = {
                val all = shown.map { it.path }.toSet()
                selection = if (selection.size == all.size && all.isNotEmpty()) emptySet() else all
            },
            onShare = { FileActions.share(ctx, selectedItems) },
            onFavorite = {
                val add = !selection.all { it in favorites }
                vm.setFavorite(selection, add)
                Toast.makeText(ctx, if (add) "즐겨찾기에 추가했습니다" else "즐겨찾기에서 뺐습니다", Toast.LENGTH_SHORT).show()
                selection = emptySet()
            },
            onRename = { renameTarget = selectedItems.firstOrNull() },
            onDelete = { confirmDelete = true },
        ),
    )

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        containerColor = Color.White,
        title = { Text("삭제", fontWeight = FontWeight.Bold) },
        text = { Text("${selection.size}개 항목을 삭제할까요?\n삭제한 파일은 되돌릴 수 없습니다.") },
        confirmButton = {
            TextButton({
                confirmDelete = false
                val paths = selection.toList()
                scope.launch {
                    val n = repo.delete(paths)
                    Toast.makeText(ctx, "${n}개 항목을 삭제했습니다", Toast.LENGTH_SHORT).show()
                    selection = emptySet(); reload++
                }
            }) { Text("삭제", color = C.Red, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("취소", color = C.SubStrong) } },
    )

    renameTarget?.let { t ->
        NameDialog("이름 변경", t.name, onDismiss = { renameTarget = null }) { name ->
            renameTarget = null
            scope.launch {
                if (repo.rename(t.path, name)) { selection = emptySet(); reload++ }
                else Toast.makeText(ctx, "같은 이름이 이미 있거나 변경할 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
    if (newFolder && route is Route.Folder) NameDialog("새 폴더", "", onDismiss = { newFolder = false }) { name ->
        newFolder = false
        scope.launch {
            if (repo.createFolder(route.path, name)) reload++
            else Toast.makeText(ctx, "폴더를 만들 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun folderCrumbs(path: String, root: String, download: String): List<Crumb> {
    val (base, baseLabel) = if (path.startsWith(download)) download to "다운로드" else root to "내장 저장공간"
    val out = mutableListOf(Crumb(baseLabel, base))
    path.removePrefix(base).split('/').filter { it.isNotEmpty() }.fold(base) { acc, seg ->
        val p = "$acc/$seg"; out += Crumb(seg, p); p
    }
    return out
}

private fun dirSize(dir: File): Long {
    var total = 0L; var count = 0
    val stack = ArrayDeque<File>().apply { add(dir) }
    while (stack.isNotEmpty() && count < 200_000) {
        val d = stack.removeLast()
        d.listFiles()?.forEach { f -> count++; if (f.isDirectory) stack.add(f) else total += f.length() }
    }
    return total
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val valid = text.isNotBlank() && !text.contains('/')
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color.White,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { OutlinedTextField(text, { text = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton({ onOk(text.trim()) }, enabled = valid) { Text("확인", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("취소", color = C.SubStrong) } },
    )
}

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color.White,
        title = { Text("개인정보 처리방침", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "이 앱은 기기 안의 파일을 보여주고 정리하기 위해서만 저장공간 접근 권한을 사용합니다.\n\n" +
                    "• 어떤 개인정보나 파일도 외부 서버로 전송·수집하지 않습니다.\n" +
                    "• 정렬 방식 등 설정은 기기 안에만 저장됩니다.\n\n" +
                    "문의: happyfamily1984.3@gmail.com",
                lineHeight = 21.sp
            )
        },
        confirmButton = { TextButton(onDismiss) { Text("확인", fontWeight = FontWeight.Bold) } },
    )
}

@Composable
fun PermissionScreen(onGranted: () -> Unit, onExit: () -> Unit) {
    val ctx = LocalContext.current
    var askExit by remember { mutableStateOf(false) }
    BackHandler { askExit = true }
    val legacy = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onGranted() }
    Column(
        Modifier.fillMaxSize().background(C.Bg).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.FolderOpen, null, tint = C.Accent, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text("파일 접근 권한이 필요해요", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = C.Text)
        Spacer(Modifier.height(12.dp))
        Text(
            "기기의 사진·동영상·문서·다운로드 파일을 보여주고\n정리하려면 '모든 파일 접근' 권한을 허용해 주세요.\n파일은 외부로 전송되지 않습니다.",
            textAlign = TextAlign.Center, color = C.SubStrong, fontSize = 15.sp, lineHeight = 22.sp
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + ctx.packageName))
                    runCatching { ctx.startActivity(i) }.onFailure {
                        ctx.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                } else legacy.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            },
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = C.Accent)
        ) { Text("권한 허용하기", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
    if (askExit) AlertDialog(
        onDismissRequest = { askExit = false }, containerColor = Color.White,
        title = { Text("앱 종료", fontWeight = FontWeight.Bold) },
        text = { Text("앱을 종료하겠습니까?") },
        confirmButton = { TextButton({ askExit = false; onExit() }) { Text("예", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton({ askExit = false }) { Text("아니오", color = C.SubStrong) } },
    )
}

enum class Tab(val label: String) { HOME("홈"), FILES("파일"), RECENT("최근 항목"), FAVORITES("즐겨찾기"), SETTINGS("설정") }
