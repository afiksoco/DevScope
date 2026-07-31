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

    private companion object {
        const val HEADER_LINES = 4
        const val MAX_KEPT = 20
    }

    fun save(timeMs: Long, threadName: String, t: Throwable, stackTrace: String) {
        dir.mkdirs()
        File(dir, "crash_$timeMs.txt").writeText(
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
        dir.listFiles { f -> f.name.startsWith("crash_") }
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
