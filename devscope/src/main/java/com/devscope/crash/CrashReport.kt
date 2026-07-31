package com.devscope.crash

/** One captured crash, backed by a plain-text file on disk. */
data class CrashReport(
    val fileName: String,
    val timeMs: Long,
    val threadName: String,
    val exceptionClass: String,
    val message: String,
    val stackTrace: String,
)
