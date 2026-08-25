package com.devscope.crash

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import com.devscope.core.DevScopeModule
import com.devscope.ui.CrashTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

/**
 * Catches uncaught exceptions, writes them to disk, then hands the exception
 * on to whatever handler was installed before us.
 */
internal class CrashModule(context: Context) : DevScopeModule {

    private companion object {
        const val TAG = "DevScope"
    }

    override val id = "crash"
    override val title = "Crash"

    internal val store = CrashStore(File(context.filesDir, "devscope_crashes"))

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        // Never wrap ourselves twice (multiple install() calls edge case).
        if (previous is DevScopeExceptionHandler) return
        Thread.setDefaultUncaughtExceptionHandler(DevScopeExceptionHandler(store, previous))
    }

    /**
     * Sends every crash that hasn't been uploaded yet through [sink], then marks
     * it so it is never sent twice — including across restarts.
     *
     * Uploading happens on the next launch rather than at crash time: the process
     * is dying then, and a network call would not survive it.
     */
    fun attachSink(sink: CrashSink) {
        scope.launch {
            store.pending().forEach { report ->
                // One failed upload (offline, quota) must not skip the rest or
                // crash the app — the report simply stays pending for next time.
                runCatching { sink.upload(report) }
                    .onSuccess {
                        store.markUploaded(report)
                        Log.i(TAG, "Crash report uploaded: ${report.exceptionClass}")
                    }
                    .onFailure { Log.w(TAG, "Crash upload failed, will retry next launch", it) }
            }
        }
    }

    override fun onClear() = store.clear()

    @Composable
    override fun Content() = CrashTab(this)

    /**
     * The crash-loop edge case lives here: saving the report is wrapped in its
     * own try/catch, and no matter what happens we always delegate to the
     * previous handler — so a bug in DevScope's own crash handling can never
     * swallow the crash or loop forever.
     */
    private class DevScopeExceptionHandler(
        private val store: CrashStore,
        private val previous: Thread.UncaughtExceptionHandler?,
    ) : Thread.UncaughtExceptionHandler {

        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            try {
                store.save(
                    timeMs = System.currentTimeMillis(),
                    threadName = thread.name,
                    t = throwable,
                    stackTrace = Log.getStackTraceString(throwable),
                )
            } catch (t: Throwable) {
                // Persisting failed (disk full, etc.) — give up on the report,
                // never on delegating the crash itself.
                Log.w("DevScope", "Failed to persist crash report", t)
            }
            previous?.uncaughtException(thread, throwable) ?: exitProcess(10)
        }
    }
}
