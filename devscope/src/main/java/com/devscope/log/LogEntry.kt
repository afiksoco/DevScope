package com.devscope.log

import android.util.Log

/** One captured log line. */
data class LogEntry(
    val timeMs: Long,
    val priority: Int,
    val tag: String?,
    val message: String,
) {
    val level: Char
        get() = when (priority) {
            Log.VERBOSE -> 'V'
            Log.DEBUG -> 'D'
            Log.INFO -> 'I'
            Log.WARN -> 'W'
            Log.ERROR -> 'E'
            Log.ASSERT -> 'A'
            else -> '?'
        }
}
