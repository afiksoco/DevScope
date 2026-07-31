package com.devscope.core

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the installed modules and implements the fail-safe edge case:
 * any module callback runs through [guard], and a module that throws is
 * disabled (its tab shows "unavailable") instead of crashing the host app.
 */
internal class ModuleRegistry {

    private val _modules = MutableStateFlow<List<DevScopeModule>>(emptyList())
    val modules: StateFlow<List<DevScopeModule>> = _modules

    /** Module id -> failure message, for modules that threw and were disabled. */
    private val _failures = MutableStateFlow<Map<String, String>>(emptyMap())
    val failures: StateFlow<Map<String, String>> = _failures

    fun register(module: DevScopeModule) {
        // Re-registering the same id is a no-op (multiple install() calls edge case).
        if (_modules.value.any { it.id == module.id }) return
        _modules.value = _modules.value + module
    }

    fun isFailed(id: String): Boolean = _failures.value.containsKey(id)

    /**
     * Runs [block] for the module with [id]; if it throws, the module is
     * disabled and the host app keeps running.
     */
    fun <T> guard(id: String, fallback: T, block: () -> T): T =
        try {
            block()
        } catch (t: Throwable) {
            markFailed(id, t)
            fallback
        }

    fun guard(id: String, block: () -> Unit) = guard(id, Unit) { block() }

    fun markFailed(id: String, t: Throwable) {
        Log.w("DevScope", "Module '$id' failed and was disabled", t)
        _failures.value = _failures.value + (id to (t.message ?: t.javaClass.simpleName))
    }
}
