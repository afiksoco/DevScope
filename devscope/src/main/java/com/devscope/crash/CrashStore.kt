package com.devscope.crash

import android.content.Context
import android.content.Intent
import java.io.File

/**
 * Persists crashes as plain-text files under the app's private storage.
 * Files survive process death (that's the whole point — the report is written
 * *before* the process dies and read on the next launch).
 *
 * File format: 4 header lines (time / thread / class / message), then the
 * full stack trace.
 */
internal class CrashStore(private val dir: File) {

    companion object {
        private const val HEADER_LINES = 4
        private const val MAX_KEPT = 20

        /** Pending reports; renamed to [SENT_PREFIX] once a CrashSink accepted them. */
        const val PENDING_PREFIX = "crash_"
        const val SENT_PREFIX = "sent_"
    }

    fun save(timeMs: Long, threadName: String, t: Throwable, stackTrace: String) {
        dir.mkdirs()
        File(dir, "$PENDING_PREFIX$timeMs.txt").writeText(
            listOf(
                timeMs.toString(),
                threadName,
                t.javaClass.name,
                (t.message ?: "").replace('\n', ' '),
                stackTrace,
            ).joinToString("\n")
        )
        // Bound disk usage: keep only the newest MAX_KEPT reports.
        crashFiles().drop(MAX_KEPT).forEach { it.delete() }
    }

    fun list(): List<CrashReport> = crashFiles().mapNotNull(::parse)

    /** Reports not yet accepted by a [CrashSink]. */
    fun pending(): List<CrashReport> = list().filterNot { it.isUploaded }

    /** Marks [report] as uploaded by renaming its file; survives restarts, so it is never sent twice. */
    fun markUploaded(report: CrashReport) {
        val from = File(dir, report.fileName)
        val to = File(dir, report.fileName.replaceFirst(PENDING_PREFIX, SENT_PREFIX))
        from.renameTo(to)
    }

    fun delete(report: CrashReport) {
        File(dir, report.fileName).delete()
    }

    fun clear() = crashFiles().forEach { it.delete() }

    /** Share a report through the standard Android share sheet. */
    fun share(context: Context, report: CrashReport) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Crash: ${report.exceptionClass}")
            putExtra(Intent.EXTRA_TEXT, "${report.exceptionClass}: ${report.message}\n\n${report.stackTrace}")
        }
        context.startActivity(Intent.createChooser(intent, "Share crash report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun crashFiles(): List<File> =
        dir.listFiles { f -> f.name.startsWith(PENDING_PREFIX) || f.name.startsWith(SENT_PREFIX) }
            ?.sortedByDescending { it.name }
            ?: emptyList()

    private fun parse(file: File): CrashReport? = runCatching {
        val lines = file.readText().lines()
        CrashReport(
            fileName = file.name,
            timeMs = lines[0].toLong(),
            threadName = lines[1],
            exceptionClass = lines[2],
            message = lines[3],
            stackTrace = lines.drop(HEADER_LINES).joinToString("\n"),
        )
    }.getOrNull() // A corrupt file is skipped, never crashes the panel.
}
