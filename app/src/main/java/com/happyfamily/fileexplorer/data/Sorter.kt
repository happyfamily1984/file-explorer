package com.happyfamily.fileexplorer.data

import java.text.Collator
import java.util.Calendar
import java.util.Locale

data class Section(val title: String?, val items: List<FileItem>)

/** 정렬 + 묶음(날짜별/형식별) 처리. 순수 함수라 테스트하기 쉽다. */
object Sorter {
    private val collator: Collator = Collator.getInstance(Locale.KOREAN).apply { strength = Collator.PRIMARY }

    private val nameComparator = Comparator<FileItem> { a, b -> naturalCompare(a.name, b.name) }

    fun sort(items: List<FileItem>, prefs: SortPrefs, now: Long = System.currentTimeMillis()): List<Section> {
        val asc = prefs.isAscending
        val base: Comparator<FileItem> = when (prefs.sortBy) {
            SortBy.DATE -> compareBy<FileItem> { it.modified }.then(nameComparator)
            SortBy.NAME -> nameComparator
            SortBy.TYPE -> compareBy<FileItem> { it.kind.ordinal }
                .thenBy { it.extension }
                .then(nameComparator)
            SortBy.SIZE -> compareBy<FileItem> { if (it.isDir) -1L else it.size }.then(nameComparator)
        }
        var cmp = if (asc) base else base.reversed()
        if (prefs.foldersFirst) cmp = compareBy<FileItem> { !it.isDir }.then(cmp)
        val sorted = items.sortedWith(cmp)

        return when (prefs.sortBy) {
            SortBy.DATE -> groupConsecutive(sorted, prefs.foldersFirst) { dateBucket(it.modified, now) }
            SortBy.TYPE -> groupConsecutive(sorted, false) { it.kind.label }
            else -> if (sorted.isEmpty()) emptyList() else listOf(Section(null, sorted))
        }
    }

    private fun groupConsecutive(
        sorted: List<FileItem>,
        foldersSeparate: Boolean,
        key: (FileItem) -> String,
    ): List<Section> {
        val result = mutableListOf<Section>()
        var title: String? = null
        var bucket = mutableListOf<FileItem>()
        for (item in sorted) {
            val k = if (foldersSeparate && item.isDir) "폴더" else key(item)
            if (k != title) {
                if (bucket.isNotEmpty()) result += Section(title, bucket)
                title = k
                bucket = mutableListOf()
            }
            bucket += item
        }
        if (bucket.isNotEmpty()) result += Section(title, bucket)
        return result
    }

    /** 오늘 / 어제 / 이번 주 / 지난 주 / 이번 달 / N월 / YYYY년 */
    fun dateBucket(time: Long, now: Long): String {
        val today = startOfDay(now)
        val day = 24L * 60 * 60 * 1000
        if (time >= today) return "오늘"
        if (time >= today - day) return "어제"
        val c = Calendar.getInstance().apply { timeInMillis = today; firstDayOfWeek = Calendar.MONDAY }
        val dow = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 월=0 … 일=6
        val weekStart = today - dow * day
        if (time >= weekStart) return "이번 주"
        if (time >= weekStart - 7 * day) return "지난 주"
        val t = Calendar.getInstance().apply { timeInMillis = time }
        val n = Calendar.getInstance().apply { timeInMillis = now }
        if (t.get(Calendar.YEAR) == n.get(Calendar.YEAR)) {
            return if (t.get(Calendar.MONTH) == n.get(Calendar.MONTH)) "이번 달"
            else "${t.get(Calendar.MONTH) + 1}월"
        }
        return "${t.get(Calendar.YEAR)}년"
    }

    private fun startOfDay(t: Long): Long = Calendar.getInstance().apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** 숫자를 숫자로 비교하는 자연 정렬 (file2 < file10), 한글은 가나다 순. */
    fun naturalCompare(a: String, b: String): Int {
        var i = 0; var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]; val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                var ei = i; while (ei < a.length && a[ei].isDigit()) ei++
                var ej = j; while (ej < b.length && b[ej].isDigit()) ej++
                val na = a.substring(i, ei).trimStart('0'); val nb = b.substring(j, ej).trimStart('0')
                if (na.length != nb.length) return na.length - nb.length
                val c = na.compareTo(nb); if (c != 0) return c
                i = ei; j = ej
            } else {
                var ei = i; while (ei < a.length && !a[ei].isDigit()) ei++
                var ej = j; while (ej < b.length && !b[ej].isDigit()) ej++
                val c = collator.compare(a.substring(i, ei), b.substring(j, ej))
                if (c != 0) return c
                i = ei; j = ej
            }
        }
        return (a.length - i) - (b.length - j)
    }
}
