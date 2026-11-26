package com.happyfamily.fileexplorer.ui

import java.util.Calendar
import java.util.Locale

object Format {
    fun size(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = listOf("KB", "MB", "GB", "TB")
        var v = bytes / 1024.0
        var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        return String.format(Locale.US, "%.2f %s", v, units[i])
    }

    fun sizeShort(bytes: Long): String {
        val gb = bytes / 1_000_000_000.0
        return String.format(Locale.US, if (gb >= 100) "%.0f GB" else "%.1f GB", gb)
    }

    /** 11월 27일 오전 12:06 (다른 해면 2025년 3월 2일 …) */
    fun date(time: Long, now: Long = System.currentTimeMillis()): String {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        val n = Calendar.getInstance().apply { timeInMillis = now }
        val h = c.get(Calendar.HOUR_OF_DAY)
        val ampm = if (h < 12) "오전" else "오후"
        val h12 = if (h % 12 == 0) 12 else h % 12
        val min = String.format(Locale.US, "%02d", c.get(Calendar.MINUTE))
        val y = if (c.get(Calendar.YEAR) != n.get(Calendar.YEAR)) "${c.get(Calendar.YEAR)}년 " else ""
        return "$y${c.get(Calendar.MONTH) + 1}월 ${c.get(Calendar.DAY_OF_MONTH)}일 $ampm $h12:$min"
    }

    fun relative(time: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - time
        val m = diff / 60_000
        return when {
            m < 5 -> "방금 전"
            m < 60 -> "${m}분 전"
            m < 60 * 24 -> "${m / 60}시간 전"
            m < 60 * 24 * 7 -> "${m / (60 * 24)}일 전"
            else -> {
                val c = Calendar.getInstance().apply { timeInMillis = time }
                "${c.get(Calendar.MONTH) + 1}월 ${c.get(Calendar.DAY_OF_MONTH)}일"
            }
        }
    }

    /** 가운데 말줄임: "baemin…8.png" */
    fun middleEllipsis(name: String, max: Int = 14): String {
        if (name.length <= max) return name
        val tail = name.substringAfterLast('.', "").let { if (it.isEmpty() || it.length > 5) name.takeLast(5) else ".$it" }
        val tailWithChar = name.substring(name.length - tail.length - 1)
        val headLen = (max - tailWithChar.length - 1).coerceAtLeast(3)
        return name.take(headLen) + "…" + tailWithChar
    }
}
