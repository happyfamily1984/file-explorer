package com.happyfamily.fileexplorer

import com.happyfamily.fileexplorer.data.*
import org.junit.Assert.assertEquals
import org.junit.Test

class SorterTest {
    private val now = 1_800_000_000_000L
    private fun f(n: String, size: Long, t: Long, dir: Boolean = false) = FileItem("/x/$n", n, dir, size, t)

    private val items = listOf(
        f("b10.txt", 5, now - 1000), f("b2.txt", 50, now - 5000), f("사진.jpg", 500, now - 3000),
        f("A폴더", 0, now - 9_000_000_000, dir = true),
    )

    @Test fun defaultIsNewestFirst() {
        val names = Sorter.sort(items, SortPrefs(), now).flatMap { it.items }.map { it.name }
        assertEquals(listOf("b10.txt", "사진.jpg", "b2.txt", "A폴더"), names)
    }

    @Test fun naturalNameOrder() {
        val names = Sorter.sort(items, SortPrefs(sortBy = SortBy.NAME), now).flatMap { it.items }.map { it.name }
        assertEquals(listOf("A폴더", "b2.txt", "b10.txt", "사진.jpg"), names)
    }

    @Test fun typeGroups() {
        val titles = Sorter.sort(items, SortPrefs(sortBy = SortBy.TYPE), now).map { it.title }
        assertEquals(listOf("폴더", "이미지", "문서"), titles)
    }

    @Test fun foldersFirst() {
        val names = Sorter.sort(items, SortPrefs(foldersFirst = true), now).flatMap { it.items }.map { it.name }
        assertEquals("A폴더", names.first())
    }
}
