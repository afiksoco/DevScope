package com.devscope.log

import android.util.Log
import androidx.compose.runtime.Composable
import com.devscope.core.DevScopeModule
import com.devscope.core.ModuleRegistry
import com.devscope.core.RingBuffer
import com.devscope.ui.LogsTab
import timber.log.Timber

/**
 * Live log stream. Captures everything the app logs through Timber by planting
 * a [DevScopeTree], and keeps the last [CAPACITY] lines in a ring buffer
 * (memory edge case: old lines are dropped, a long session can't OOM).
 */
internal class LogsModule(private val registry: ModuleRegistry) : DevScopeModule {

    private companion object {
        const val CAPACITY = 2_000
    }

    override val id = "logs"
    override val title = "Logs"

    val entries = RingBuffer<LogEntry>(CAPACITY)

    fun install() {
        Timber.plant(DevScopeTree())
    }

    override fun onClear() = entries.clear()

    @Composable
    override fun Content() = LogsTab(entries)

    /**
     * A capture-only tree: it feeds the panel but never writes to Logcat, so
     * it composes with whatever trees the host app already planted.
     */
    private inner class DevScopeTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Fail-safe edge case: a log call must never crash the host.
            registry.guard(id) {
                val text = if (t != null) "$message\n${Log.getStackTraceString(t)}" else message
                entries.add(LogEntry(System.currentTimeMillis(), priority, tag, text))
            }
        }
    }
}
