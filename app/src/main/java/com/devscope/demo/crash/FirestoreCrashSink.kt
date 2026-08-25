package com.devscope.demo.crash

import com.devscope.crash.CrashReport
import com.devscope.crash.CrashSink
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

/**
 * Sends DevScope crash reports to Firebase Firestore, into the `crashes` collection.
 *
 * This lives in the demo app, not in the library: DevScope defines the
 * [CrashSink] contract and stays free of any cloud SDK, so apps that don't want
 * Firebase never pull it in. Swapping Firestore for another backend means
 * writing a different CrashSink — nothing in the library changes.
 */
class FirestoreCrashSink(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : CrashSink {

    private companion object {
        const val COLLECTION = "crashes"
        const val TIMEOUT_SECONDS = 20L

        /** Firestore rejects documents over 1 MB — keep the trace well under it. */
        const val MAX_TRACE_CHARS = 100_000
    }

    /**
     * Called off the main thread by DevScope. Blocking here is intentional and
     * safe: it lets a failed upload throw, which keeps the report pending for
     * the next launch instead of silently losing it.
     */
    override fun upload(report: CrashReport) {
        val document = mapOf(
            "timeMs" to report.timeMs,
            "thread" to report.threadName,
            "exception" to report.exceptionClass,
            "message" to report.message,
            "stackTrace" to report.stackTrace.take(MAX_TRACE_CHARS),
        )
        Tasks.await(
            firestore.collection(COLLECTION).add(document),
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS,
        )
    }
}
