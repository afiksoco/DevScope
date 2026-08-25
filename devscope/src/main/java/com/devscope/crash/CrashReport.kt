package com.devscope.crash

/** One captured crash, backed by a plain-text file on disk. */
data class CrashReport(
    val fileName: String,
    val timeMs: Long,
    val threadName: String,
    val exceptionClass: String,
    val message: String,
    val stackTrace: String,
) {
    /** True once the report was accepted by a [CrashSink] (e.g. uploaded to Firebase). */
    val isUploaded: Boolean get() = fileName.startsWith(CrashStore.SENT_PREFIX)
}
