package com.devscope.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Thread-safe bounded buffer: when full, the oldest item is dropped.
 *
 * This is the memory edge case in practice — a long debug session can produce
 * endless logs / network calls, and an unbounded list would eventually OOM the
 * host app. Exposes an immutable snapshot [items] that Compose can collect.
 */
class RingBuffer<T>(private val capacity: Int) {

    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    private val lock = Any()
    private val buffer = ArrayDeque<T>(capacity)

    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items

    val size: Int get() = _items.value.size

    fun add(item: T) {
        synchronized(lock) {
            buffer.addLast(item)
            if (buffer.size > capacity) buffer.removeFirst()
            _items.value = buffer.toList()
        }
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            _items.value = emptyList()
        }
    }
}
