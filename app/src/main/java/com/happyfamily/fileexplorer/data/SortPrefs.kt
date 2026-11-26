package com.happyfamily.fileexplorer.data

enum class SortBy(val label: String, val defaultAscending: Boolean) {
    DATE("수정한 날짜", false),
    NAME("이름", true),
    TYPE("형식", true),
    SIZE("크기", false),
}

data class SortPrefs(
    val sortBy: SortBy = SortBy.DATE,
    /** 기준별로 마지막에 선택한 정렬 방향을 따로 기억한다. */
    val ascending: Map<SortBy, Boolean> = SortBy.entries.associateWith { it.defaultAscending },
    val foldersFirst: Boolean = false,
    val showHidden: Boolean = false,
) {
    val isAscending: Boolean get() = ascending[sortBy] ?: sortBy.defaultAscending

    fun directionLabel(): String = when (sortBy) {
        SortBy.DATE -> if (isAscending) "오래된 순" else "최신 순"
        SortBy.NAME -> if (isAscending) "가나다 순" else "역순"
        SortBy.TYPE -> if (isAscending) "오름차순" else "내림차순"
        SortBy.SIZE -> if (isAscending) "작은 순" else "큰 순"
    }
}
