package com.happyfamily.fileexplorer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.happyfamily.fileexplorer.data.FileRepository
import com.happyfamily.fileexplorer.data.SettingsRepository
import com.happyfamily.fileexplorer.data.SortBy
import com.happyfamily.fileexplorer.data.SortPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val repo = FileRepository(app)
    private val settings = SettingsRepository(app)

    /** null = 아직 디스크에서 읽는 중 (기본값으로 잠깐 깜빡이지 않도록) */
    val prefs: StateFlow<SortPrefs?> = settings.sortPrefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val favorites: StateFlow<Set<String>> = settings.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun setFavorite(paths: Collection<String>, add: Boolean) = viewModelScope.launch { settings.setFavorite(paths, add) }
    val openWith: StateFlow<Map<String, String>> = settings.openWith
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun setOpenWith(ext: String, component: String?) = viewModelScope.launch { settings.setOpenWith(ext, component) }
    fun clearOpenWith() = viewModelScope.launch { settings.clearOpenWith() }
    private val _pendingFolder = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val pendingFolder: StateFlow<String?> = _pendingFolder
    fun requestFolder(path: String?) { _pendingFolder.value = path }

    fun setSortBy(by: SortBy) = viewModelScope.launch { settings.setSortBy(by) }
    fun setAscending(by: SortBy, asc: Boolean) = viewModelScope.launch { settings.setAscending(by, asc) }
    fun setFoldersFirst(v: Boolean) = viewModelScope.launch { settings.setFoldersFirst(v) }
    fun setShowHidden(v: Boolean) = viewModelScope.launch { settings.setShowHidden(v) }
}
