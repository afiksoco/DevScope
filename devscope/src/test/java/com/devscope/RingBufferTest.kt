package com.devscope

import com.devscope.core.RingBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RingBufferTest {

    @Test
    fun `keeps items in insertion order`() {
        val buffer = RingBuffer<Int>(5)
        (1..3).forEach(buffer::add)
        assertEquals(listOf(1, 2, 3), buffer.items.value)
    }

    @Test
    fun `drops oldest item beyond capacity`() {
        val buffer = RingBuffer<Int>(3)
        (1..5).forEach(buffer::add)
        assertEquals(listOf(3, 4, 5), buffer.items.value)
    }

    @Test
    fun `clear empties the buffer`() {
        val buffer = RingBuffer<Int>(3)
        (1..3).forEach(buffer::add)
        buffer.clear()
        assertTrue(buffer.items.value.isEmpty())
        assertEquals(0, buffer.size)
    }

    @Test
    fun `concurrent adds never exceed capacity`() {
        val buffer = RingBuffer<Int>(100)
        val threads = (1..8).map { t ->
            Thread { repeat(1_000) { buffer.add(t * 10_000 + it) } }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertEquals(100, buffer.size)
    }
}
