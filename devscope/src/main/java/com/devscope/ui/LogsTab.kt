package com.devscope.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devscope.core.RingBuffer
import com.devscope.log.LogEntry

private val LEVELS = listOf('V', 'D', 'I', 'W', 'E')

/** Live log stream with text search and level filtering. */
@Composable
internal fun LogsTab(buffer: RingBuffer<LogEntry>) {
    val entries by buffer.items.collectAsState()
    var query by remember { mutableStateOf("") }
    var minLevel by remember { mutableStateOf('V') }

    val visible = entries.asReversed().filter { entry ->
        LEVELS.indexOf(entry.level) >= LEVELS.indexOf(minLevel) &&
            (query.isBlank() ||
                entry.message.contains(query, ignoreCase = true) ||
                entry.tag?.contains(query, ignoreCase = true) == true)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DsTextField(query, { query = it }, placeholder = "search…", Modifier.weight(1f))
        }
        Row(
            Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LEVELS.forEach { level ->
                DsChip("$level+", selected = minLevel == level, onClick = { minLevel = level })
            }
        }
        if (visible.isEmpty()) {
            DsEmpty(if (entries.isEmpty()) "no logs yet" else "no logs match the filter")
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(top = 6.dp)) {
                items(visible) { entry -> LogRow(entry) }
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(formatTime(entry.timeMs), color = DsColors.faint, fontSize = 11.sp, fontFamily = MonoFont)
        Text(
            entry.level.toString(),
            color = DsColors.forLevel(entry.level),
            fontSize = 11.sp,
            fontFamily = MonoFont,
            fontWeight = FontWeight.Bold,
        )
        Text(
            buildString {
                entry.tag?.let { append("$it: ") }
                append(entry.message)
            },
            color = DsColors.text,
            fontSize = 12.sp,
            fontFamily = MonoFont,
        )
    }
}
