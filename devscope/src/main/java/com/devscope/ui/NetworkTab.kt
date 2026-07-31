package com.devscope.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.devscope.network.NetworkEntry

/** HTTP call list; tap a row to expand the response body preview. */
@Composable
internal fun NetworkTab(buffer: RingBuffer<NetworkEntry>) {
    val entries by buffer.items.collectAsState()
    var expandedIndex by remember { mutableStateOf(-1) }

    if (entries.isEmpty()) {
        DsEmpty("no HTTP calls yet — add DevScope.networkInterceptor to your OkHttpClient")
        return
    }
    val newestFirst = entries.asReversed()
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(newestFirst) { index, entry ->
            DsExpandableRow(
                expanded = expandedIndex == index,
                onToggle = { expandedIndex = if (expandedIndex == index) -1 else index },
                header = { NetworkHeader(entry) },
                details = { NetworkDetails(entry) },
            )
        }
    }
}

@Composable
private fun NetworkHeader(entry: NetworkEntry) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(formatTime(entry.timeMs), color = DsColors.faint, fontSize = 11.sp, fontFamily = MonoFont)
        Text(entry.method, color = DsColors.info, fontSize = 12.sp, fontFamily = MonoFont, fontWeight = FontWeight.Bold)
        Text(
            entry.code?.toString() ?: "FAIL",
            color = DsColors.forStatus(entry.code),
            fontSize = 12.sp,
            fontFamily = MonoFont,
            fontWeight = FontWeight.Bold,
        )
        Text("${entry.durationMs}ms", color = DsColors.muted, fontSize = 11.sp, fontFamily = MonoFont)
        Text(
            entry.url,
            color = DsColors.text,
            fontSize = 12.sp,
            fontFamily = MonoFont,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NetworkDetails(entry: NetworkEntry) {
    val body = entry.error ?: entry.responseBody.ifBlank { "(empty body)" }
    Text(
        body,
        color = if (entry.isError) DsColors.error else DsColors.muted,
        fontSize = 11.sp,
        fontFamily = MonoFont,
        modifier = Modifier
            .fillMaxWidth()
            .background(DsColors.ink, RoundedCornerShape(8.dp))
            .padding(10.dp)
            .horizontalScroll(rememberScrollState()),
    )
}
