package com.devscope.perf

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import androidx.compose.runtime.Composable
import com.devscope.core.DevScopeModule
import com.devscope.core.ModuleRegistry
import com.devscope.core.RingBuffer
import com.devscope.ui.PerfTab
import kotlinx.coroutines.flow.MutableStateFlow

/** One second of rendering and memory statistics. */
data class PerfSample(
    val fps: Int,
    val jankFrames: Int,
    val worstFrameMs: Int,
    val usedMemoryMb: Int,
)

/**
 * Live rendering health: frames per second, dropped frames, and heap usage.
 *
 * Frame timing comes from [Choreographer] — the same clock the system uses to
 * schedule drawing — so the numbers reflect what the user actually sees, not a
 * synthetic benchmark. Everything is derived from frame *intervals*: no extra
 * work is scheduled and no other thread is involved.
 */
internal class PerformanceModule(private val registry: ModuleRegistry) : DevScopeModule {

    private companion object {
        /** One minute of one-second samples. */
        const val CAPACITY = 60

        /** A frame this late means at least one dropped frame at 60 Hz. */
        const val JANK_THRESHOLD_MS = 32

        const val NANOS_PER_MS = 1_000_000L
        const val WINDOW_MS = 1_000L
        const val BYTES_PER_MB = 1024 * 1024
    }

    override val id = "perf"
    override val title = "Perf"

    val samples = RingBuffer<PerfSample>(CAPACITY)
    val current = MutableStateFlow(PerfSample(fps = 0, jankFrames = 0, worstFrameMs = 0, usedMemoryMb = 0))

    private var lastFrameNanos = 0L
    private var windowStartNanos = 0L
    private var framesInWindow = 0
    private var jankInWindow = 0
    private var worstFrameInWindow = 0

    fun install() {
        // Choreographer is per-thread: the callback must be registered on main.
        Handler(Looper.getMainLooper()).post {
            registry.guard(id) { Choreographer.getInstance().postFrameCallback(frameCallback) }
        }
    }

    override fun onClear() {
        samples.clear()
    }

    @Composable
    override fun Content() = PerfTab(this)

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            registry.guard(id) { measure(frameTimeNanos) }
            // Re-arm: callbacks are one-shot, so the next frame needs a new one.
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun measure(frameTimeNanos: Long) {
        if (windowStartNanos == 0L) windowStartNanos = frameTimeNanos
        if (lastFrameNanos != 0L) {
            val frameMs = ((frameTimeNanos - lastFrameNanos) / NANOS_PER_MS).toInt()
            framesInWindow++
            if (frameMs >= JANK_THRESHOLD_MS) jankInWindow++
            if (frameMs > worstFrameInWindow) worstFrameInWindow = frameMs
        }
        lastFrameNanos = frameTimeNanos

        val elapsedMs = (frameTimeNanos - windowStartNanos) / NANOS_PER_MS
        if (elapsedMs < WINDOW_MS) return

        val runtime = Runtime.getRuntime()
        val sample = PerfSample(
            fps = (framesInWindow * WINDOW_MS / elapsedMs).toInt(),
            jankFrames = jankInWindow,
            worstFrameMs = worstFrameInWindow,
            usedMemoryMb = ((runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB).toInt(),
        )
        samples.add(sample)
        current.value = sample

        framesInWindow = 0
        jankInWindow = 0
        worstFrameInWindow = 0
        windowStartNanos = frameTimeNanos
    }
}
