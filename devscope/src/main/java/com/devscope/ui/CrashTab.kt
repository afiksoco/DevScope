package com.devscope.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devscope.crash.CrashModule
import com.devscope.crash.CrashReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Saved crash reports: expand for the stack trace, share or delete. */
@Composable
internal fun CrashTab(module: CrashModule) {
    val context = LocalContext.current
    var reports by remember { mutableStateOf<List<CrashReport>?>(null) }
    var expandedIndex by remember { mutableStateOf(-1) }
    var reloadKey by remember { mutableIntStateOf(0) }

    // Reports are read from disk off the main thread.
    LaunchedEffect(reloadKey) {
        reports = withContext(Dispatchers.IO) { module.store.list() }
    }

    val loaded = reports ?: return
    if (loaded.isEmpty()) {
        DsEmpty("no crashes recorded — that's a good thing")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(loaded) { index, report ->
            DsExpandableRow(
                expanded = expandedIndex == index,
                onToggle = { expandedIndex = if (expandedIndex == index) -1 else index },
                header = {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(formatTime(report.timeMs), color = DsColors.faint, fontSize = 11.sp, fontFamily = MonoFont)
                            Text("thread: ${report.threadName}", color = DsColors.muted, fontSize = 11.sp, fontFamily = MonoFont)
                        }
                        Text(
                            report.exceptionClass.substringAfterLast('.'),
                            color = DsColors.error,
                            fontSize = 13.sp,
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Bold,
                        )
                        if (report.message.isNotBlank()) {
                            Text(report.message, color = DsColors.text, fontSize = 12.sp, fontFamily = MonoFont)
                        }
                    }
                },
                details = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            report.stackTrace,
                            color = DsColors.muted,
                            fontSize = 10.sp,
                            fontFamily = MonoFont,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DsColors.ink, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                                .horizontalScroll(rememberScrollState()),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DsChip("share", selected = false) { module.store.share(context, report) }
                            DsChip("delete", selected = false) {
                                module.store.delete(report)
                                expandedIndex = -1
                                reloadKey++
                            }
                        }
                    }
                },
            )
        }
    }
}
