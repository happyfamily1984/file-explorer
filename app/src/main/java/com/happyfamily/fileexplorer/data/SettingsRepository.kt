package com.happyfamily.fileexplorer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 정렬 설정을 디스크(DataStore)에 저장한다.
 * 변경 즉시 파일로 기록되므로 앱을 종료하거나 강제 종료돼도
 * 다음 실행 때 마지막으로 사용한 정렬 방식이 그대로 복원된다.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val SORT_BY = stringPreferencesKey("sort_by")
        val FOLDERS_FIRST = booleanPreferencesKey("folders_first")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val FAVORITES = stringSetPreferencesKey("favorites")
        fun asc(by: SortBy) = booleanPreferencesKey("asc_${by.name}")
    }

    val sortPrefs: Flow<SortPrefs> = context.dataStore.data.map { p ->
        SortPrefs(
            sortBy = p[Keys.SORT_BY]?.let { runCatching { SortBy.valueOf(it) }.getOrNull() } ?: SortBy.DATE,
            ascending = SortBy.entries.associateWith { p[Keys.asc(it)] ?: it.defaultAscending },
            foldersFirst = p[Keys.FOLDERS_FIRST] ?: false,
            showHidden = p[Keys.SHOW_HIDDEN] ?: false,
        )
    }

    suspend fun setSortBy(by: SortBy) = context.dataStore.edit { it[Keys.SORT_BY] = by.name }

    suspend fun setAscending(by: SortBy, asc: Boolean) = context.dataStore.edit { it[Keys.asc(by)] = asc }

    suspend fun setFoldersFirst(v: Boolean) = context.dataStore.edit { it[Keys.FOLDERS_FIRST] = v }

    suspend fun setShowHidden(v: Boolean) = context.dataStore.edit { it[Keys.SHOW_HIDDEN] = v }

    /** 즐겨찾기한 파일·폴더 경로 */
    val favorites: Flow<Set<String>> = context.dataStore.data.map { it[Keys.FAVORITES] ?: emptySet() }

    suspend fun setFavorite(paths: Collection<String>, add: Boolean) = context.dataStore.edit {
        val cur = it[Keys.FAVORITES] ?: emptySet()
        it[Keys.FAVORITES] = if (add) cur + paths else cur - paths.toSet()
    }
}
