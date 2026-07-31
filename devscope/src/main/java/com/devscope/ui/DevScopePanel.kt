package com.devscope.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import com.devscope.core.ModuleRegistry
import com.devscope.core.PanelState
import kotlin.math.roundToInt

/**
 * Root of everything DevScope draws over the host app:
 * a draggable bubble when closed (if enabled), the tabbed panel when open.
 */
@Composable
internal fun DevScopeRoot(registry: ModuleRegistry, showBubble: Boolean) {
    val isOpen by PanelState.isOpen.collectAsState()
    if (isOpen) {
        Panel(registry)
    } else if (showBubble) {
        Bubble()
    }
}

/** Small draggable "◆" that reopens the panel — the no-sensor fallback trigger. */
@Composable
private fun Bubble() {
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .size(46.dp)
                .background(DsColors.panel, CircleShape)
                .border(1.dp, DsColors.warn, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        offset += drag
                    }
                }
                .clickable { PanelState.isOpen.value = true },
            contentAlignment = Alignment.Center,
        ) {
            Text("◆", color = DsColors.warn, fontSize = 18.sp)
        }
    }
}

@Composable
private fun Panel(registry: ModuleRegistry) {
    val modules by registry.modules.collectAsState()
    val failures by registry.failures.collectAsState()
    val selectedTab by PanelState.selectedTab.collectAsState()
    val safeIndex = selectedTab.coerceIn(0, (modules.size - 1).coerceAtLeast(0))

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .background(DsColors.panel, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .border(1.dp, DsColors.line, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        ) {
            Header(onClear = {
                modules.getOrNull(safeIndex)?.let { module ->
                    registry.guard(module.id) { module.onClear() }
                }
            })
            if (modules.isEmpty()) {
                DsEmpty("no modules installed")
                return@Column
            }
            ScrollableTabRow(
                selectedTabIndex = safeIndex,
                containerColor = DsColors.panel,
                edgePadding = 8.dp,
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(positions[safeIndex]),
                        color = DsColors.warn,
                    )
                },
                divider = {},
            ) {
                modules.forEachIndexed { index, module ->
                    Tab(
                        selected = index == safeIndex,
                        onClick = { PanelState.selectedTab.value = index },
                        text = {
                            Text(
                                module.title,
                                fontFamily = MonoFont,
                                fontSize = 13.sp,
                                color = when {
                                    failures.containsKey(module.id) -> DsColors.faint
                                    index == safeIndex -> DsColors.text
                                    else -> DsColors.muted
                                },
                            )
                        },
                    )
                }
            }
            val module = modules[safeIndex]
            val failure = failures[module.id]
            if (failure != null) {
                // Fail-safe edge case, user-visible side: the tab stays, the
                // reason is shown, the rest of the panel keeps working.
                DsEmpty("module unavailable: $failure")
            } else {
                module.Content()
            }
        }
    }
}

@Composable
private fun Header(onClear: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("◆", color = DsColors.warn, fontSize = 14.sp)
        Text(
            " DevScope",
            color = DsColors.text,
            fontSize = 14.sp,
            fontFamily = MonoFont,
            fontWeight = FontWeight.Bold,
        )
        Box(Modifier.weight(1f))
        Text(
            "clear",
            color = DsColors.muted,
            fontSize = 12.sp,
            fontFamily = MonoFont,
            modifier = Modifier.clickable(onClick = onClear).padding(8.dp),
        )
        Text(
            "close ✕",
            color = DsColors.muted,
            fontSize = 12.sp,
            fontFamily = MonoFont,
            modifier = Modifier
                .clickable { PanelState.isOpen.value = false }
                .padding(8.dp),
        )
    }
}
