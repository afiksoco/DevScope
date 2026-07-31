package com.devscope.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devscope.db.DatabaseModule
import com.devscope.db.QueryResult
import com.devscope.db.SqlGuard
import kotlinx.coroutines.launch

/**
 * Room browser: table chips, paginated rows, and a free-SQL box.
 * Destructive SQL pops a confirmation dialog before running (DB-safety edge case).
 */
@Composable
internal fun DbTab(module: DatabaseModule) {
    val scope = rememberCoroutineScope()
    var tables by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTable by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<QueryResult?>(null) }
    var sql by remember { mutableStateOf("") }
    var pendingSql by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { tables = module.listTables() }
    LaunchedEffect(selectedTable, page) {
        selectedTable?.let { result = module.browse(it, page) }
    }

    fun run(statement: String) {
        scope.launch {
            result = module.runSql(statement)
            selectedTable = null
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("TABLES · ${module.displayName}", color = DsColors.warn, fontSize = 11.sp, fontFamily = MonoFont)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tables.forEach { table ->
                DsChip(table, selected = table == selectedTable) {
                    selectedTable = table
                    page = 0
                }
            }
            if (tables.isEmpty()) {
                Text("no tables", color = DsColors.faint, fontSize = 12.sp, fontFamily = MonoFont)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DsTextField(sql, { sql = it }, placeholder = "SELECT * FROM …", Modifier.weight(1f))
            DsChip("run", selected = sql.isNotBlank()) {
                if (sql.isBlank()) return@DsChip
                // Destructive statements need explicit confirmation first.
                if (SqlGuard.isDestructive(sql)) pendingSql = sql else run(sql)
            }
        }

        // Pagination for table browsing (50 rows per page).
        if (selectedTable != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DsChip("‹ prev", selected = false) { if (page > 0) page-- }
                Text("page ${page + 1}", color = DsColors.muted, fontSize = 12.sp, fontFamily = MonoFont)
                DsChip("next ›", selected = false) { if (result?.hasMore == true) page++ }
            }
        }

        result?.let { ResultTable(it, Modifier.weight(1f)) }
    }

    pendingSql?.let { statement ->
        AlertDialog(
            onDismissRequest = { pendingSql = null },
            containerColor = DsColors.panel2,
            title = { Text("Destructive SQL", color = DsColors.warn, fontFamily = MonoFont) },
            text = {
                Text(
                    "This statement changes data or schema:\n\n$statement",
                    color = DsColors.text,
                    fontSize = 13.sp,
                    fontFamily = MonoFont,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingSql = null
                    run(statement)
                }) { Text("Run anyway", color = DsColors.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingSql = null }) { Text("Cancel", color = DsColors.muted) }
            },
        )
    }
}

@Composable
private fun ResultTable(result: QueryResult, modifier: Modifier = Modifier) {
    result.error?.let {
        Text("error: $it", color = DsColors.error, fontSize = 12.sp, fontFamily = MonoFont)
        return
    }
    if (result.rows.isEmpty()) {
        DsEmpty("no rows")
        return
    }
    val scroll = rememberScrollState()
    Column(
        modifier
            .fillMaxWidth()
            .background(DsColors.ink, RoundedCornerShape(8.dp))
            .padding(8.dp),
    ) {
        Row(Modifier.horizontalScroll(scroll)) {
            result.columns.forEach { column ->
                Text(
                    column,
                    color = DsColors.info,
                    fontSize = 11.sp,
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.width(120.dp),
                )
            }
        }
        LazyColumn {
            items(result.rows) { row ->
                Row(Modifier.horizontalScroll(scroll).padding(vertical = 2.dp)) {
                    row.forEach { cell ->
                        Text(
                            cell,
                            color = DsColors.text,
                            fontSize = 11.sp,
                            fontFamily = MonoFont,
                            maxLines = 1,
                            modifier = Modifier.width(120.dp),
                        )
                    }
                }
            }
        }
    }
}
