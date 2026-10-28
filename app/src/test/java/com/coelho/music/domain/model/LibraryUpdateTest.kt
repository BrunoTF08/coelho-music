package com.coelho.music.domain.model

import org.junit.Assert.*
import org.junit.Test

class LibraryUpdateTest {
    private data class Entry(val id: Long, val added: Long, val title: String = "Música")

    @Test fun ordersByIndexDateDescendingWithDeterministicTies() {
        val rows = listOf(Entry(1, 100), Entry(3, 200), Entry(2, 200), Entry(4, 50))
        assertEquals(listOf(3L, 2L, 1L, 4L), recentlyAdded(rows, { it.added }, { it.id }).map { it.id })
    }
    @Test fun previewIsTenNewestAndFullListPreservesAll() {
        val rows = (1L..2000L).map { Entry(it, it * 100) }
        val result = recentlyAdded(rows.shuffled(), { it.added }, { it.id })
        assertEquals(2000, result.size)
        assertEquals((2000L downTo 1991L).toList(), result.take(10).map { it.id })
        assertTrue(recentlyAdded(emptyList<Entry>(), { it.added }, { it.id }).isEmpty())
    }
    @Test fun refreshReusesOnlyUnchangedObjectsAndHandlesRemoval() {
        val old = Entry(1, 20)
        val changed = Entry(2, 30, "Antes")
        val removed = Entry(3, 40)
        val incoming = listOf(old.copy(), changed.copy(title = "Depois"), Entry(4, 50))
        val merged = reuseUnchanged(listOf(old, changed, removed).associateBy { it.id }, incoming) { it.id }
        assertSame(old, merged[0])
        assertNotSame(changed, merged[1])
        assertEquals("Depois", merged[1].title)
        assertEquals(listOf(1L, 2L, 4L), merged.map { it.id })
    }
    @Test fun incrementalRefreshAddsEditsAndDeletesWithoutReplacingOtherRows() {
        val untouched = Entry(1, 10)
        val old = mapOf(1L to untouched, 2L to Entry(2, 20), 3L to Entry(3, 30))
        val next = applyLibraryDelta(old, listOf(Entry(2, 20, "Atualizada"), Entry(4, 40)), setOf(2L, 3L, 4L)) { it.id }
        assertSame(untouched, next[1L])
        assertEquals("Atualizada", next[2L]?.title)
        assertFalse(next.containsKey(3L))
        assertEquals(40L, next[4L]?.added)
        assertEquals(3, next.size)
        assertTrue(old.containsKey(3L))
    }
}
