package com.devscope.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devscope.perf.PerfSample
import com.devscope.perf.PerformanceModule
import kotlinx.coroutines.delay

private const val TARGET_FPS = 60f

/** Live FPS, dropped frames and heap usage, with a one-minute history graph. */
@Composable
internal fun PerfTab(module: PerformanceModule) {
    val sample by module.current.collectAsState()
    val history by module.samples.items.collectAsState()

    // Frame callbacks only fire while something is drawing. Ticking here keeps
    // the panel repainting, so the graph stays live while this tab is open —
    // and stops the moment the user leaves it.
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            tick++
        }
    }

    Column(
        Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${sample.fps}",
                color = fpsColor(sample.fps),
                fontSize = 54.sp,
                fontFamily = MonoFont,
                fontWeight = FontWeight.Bold,
            )
            Text("fps", color = DsColors.muted, fontSize = 16.sp, fontFamily = MonoFont, modifier = Modifier.padding(bottom = 8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("dropped", "${sample.jankFrames}", if (sample.jankFrames > 0) DsColors.warn else DsColors.live)
            Stat("worst frame", "${sample.worstFrameMs}ms", if (sample.worstFrameMs >= 32) DsColors.warn else DsColors.text)
            Stat("heap", "${sample.usedMemoryMb}MB", DsColors.info)
        }

        Text("LAST 60 SECONDS", color = DsColors.warn, fontSize = 11.sp, fontFamily = MonoFont)
        Graph(history, Modifier.fillMaxWidth().height(140.dp))

        Text(
            "Frame timings come from Choreographer — the same clock the system draws on. " +
                "A frame over 32ms counts as dropped.",
            color = DsColors.faint,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color) {
    Column(
        Modifier
            .background(DsColors.panel2, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, color = DsColors.faint, fontSize = 10.sp, fontFamily = MonoFont)
        Text(value, color = color, fontSize = 16.sp, fontFamily = MonoFont, fontWeight = FontWeight.Bold)
    }
}

/** FPS as a filled line against the 60fps target, with heap drawn behind it. */
@Composable
private fun Graph(history: List<PerfSample>, modifier: Modifier) {
    Canvas(modifier.background(DsColors.ink, RoundedCornerShape(10.dp)).padding(8.dp)) {
        // Target line at 60fps.
        drawLine(DsColors.line, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f)
        if (history.size < 2) return@Canvas

        val stepX = size.width / (history.size - 1).coerceAtLeast(1)
        val maxHeap = history.maxOf { it.usedMemoryMb }.coerceAtLeast(1)

        history.forEachIndexed { i, s ->
            if (i == 0) return@forEachIndexed
            val prev = history[i - 1]
            val x0 = (i - 1) * stepX
            val x1 = i * stepX

            // Heap, faint, scaled to its own peak.
            drawLine(
                DsColors.faint,
                Offset(x0, size.height - size.height * prev.usedMemoryMb / maxHeap),
                Offset(x1, size.height - size.height * s.usedMemoryMb / maxHeap),
                strokeWidth = 2f,
            )
            // FPS, colored by how healthy the frame rate was.
            drawLine(
                fpsColor(s.fps),
                Offset(x0, size.height - size.height * (prev.fps / TARGET_FPS).coerceAtMost(1f)),
                Offset(x1, size.height - size.height * (s.fps / TARGET_FPS).coerceAtMost(1f)),
                strokeWidth = 4f,
            )
        }
    }
}

private fun fpsColor(fps: Int): Color = when {
    fps >= 50 -> DsColors.live
    fps >= 30 -> DsColors.warn
    else -> DsColors.error
}
