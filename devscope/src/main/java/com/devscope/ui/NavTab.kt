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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devscope.nav.NavigationModule

/** Current route + a timeline of recent destination changes. */
@Composable
internal fun NavTab(module: NavigationModule) {
    val current by module.currentRoute.collectAsState()
    val history by module.history.items.collectAsState()

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("CURRENT", color = DsColors.warn, fontSize = 11.sp, fontFamily = MonoFont)
        Text(current, color = DsColors.text, fontSize = 16.sp, fontFamily = MonoFont, fontWeight = FontWeight.Bold)
        Text("HISTORY", color = DsColors.warn, fontSize = 11.sp, fontFamily = MonoFont)
        if (history.isEmpty()) {
            DsEmpty("no navigation events yet")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(history.asReversed()) { event ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(formatTime(event.timeMs), color = DsColors.faint, fontSize = 11.sp, fontFamily = MonoFont)
                        Text("›", color = DsColors.warn, fontSize = 12.sp, fontFamily = MonoFont)
                        Column {
                            Text(event.route, color = DsColors.text, fontSize = 12.sp, fontFamily = MonoFont)
                            event.args?.let {
                                Text(it, color = DsColors.muted, fontSize = 11.sp, fontFamily = MonoFont)
                            }
                        }
                    }
                }
            }
        }
    }
}
