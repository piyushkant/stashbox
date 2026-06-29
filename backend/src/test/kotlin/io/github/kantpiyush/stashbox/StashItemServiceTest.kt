package io.github.kantpiyush.stashbox

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.Optional

class StashItemServiceTest {

    private val repository: StashItemRepository = mock()
    private val service = StashItemServiceImpl(repository)

    @Test
    fun `findAll returns items from repository`() {
        val items = listOf(StashItem(text = "first"), StashItem(text = "second"))
        whenever(repository.findAllByOrderByCreatedAtDesc()).thenReturn(items)

        val result = service.findAll()

        assertEquals(2, result.size)
        assertEquals("first", result[0].text)
        assertEquals("second", result[1].text)
    }

    @Test
    fun `findAll returns empty list when no items`() {
        whenever(repository.findAllByOrderByCreatedAtDesc()).thenReturn(emptyList())

        val result = service.findAll()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findById returns item when found`() {
        val item = StashItem(id = "abc", text = "test item")
        whenever(repository.findById("abc")).thenReturn(Optional.of(item))

        val result = service.findById("abc")

        assertNotNull(result)
        assertEquals("abc", result?.id)
        assertEquals("test item", result?.text)
    }

    @Test
    fun `findById returns null when not found`() {
        whenever(repository.findById("missing")).thenReturn(Optional.empty())

        val result = service.findById("missing")

        assertNull(result)
    }

    @Test
    fun `save delegates to repository and returns saved item`() {
        val item = StashItem(text = "save me")
        whenever(repository.save(item)).thenReturn(item)

        val result = service.save(item)

        assertEquals(item, result)
        verify(repository).save(item)
    }

    @Test
    fun `deleteById returns true and deletes when item exists`() {
        whenever(repository.existsById("abc")).thenReturn(true)

        val result = service.deleteById("abc")

        assertTrue(result)
        verify(repository).deleteById("abc")
    }

    @Test
    fun `deleteById returns false and never deletes when item does not exist`() {
        whenever(repository.existsById("missing")).thenReturn(false)

        val result = service.deleteById("missing")

        assertFalse(result)
        verify(repository, never()).deleteById(any())
    }
}
