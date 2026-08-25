package com.devscope.crash

/**
 * Destination for crash reports beyond the device — a backend, a log service,
 * or Firebase in the demo app.
 *
 * DevScope deliberately does NOT depend on any cloud SDK: the host app decides
 * where crashes go and implements this interface, so the library stays free of
 * forced dependencies (see the demo's FirestoreCrashSink).
 */
interface CrashSink {
    /** Called off the main thread. Throw to signal failure — the report is kept and retried next launch. */
    fun upload(report: CrashReport)
}
