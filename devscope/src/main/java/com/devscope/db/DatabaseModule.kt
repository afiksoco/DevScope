package com.devscope.db

import androidx.compose.runtime.Composable
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import com.devscope.core.DevScopeModule
import com.devscope.ui.DbTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A tabular query result: column names + stringified rows. */
data class QueryResult(
    val columns: List<String>,
    val rows: List<List<String>>,
    val hasMore: Boolean = false,
    val error: String? = null,
)

/**
 * Room inspector: browse tables page by page and run free SQL.
 *
 * DB-safety edge cases, all here:
 *  - every query runs on Dispatchers.IO — the main thread is never blocked;
 *  - table browsing is paginated ([PAGE_SIZE] rows), so a huge table can't
 *    freeze the panel or exhaust memory;
 *  - destructive SQL is detected by [SqlGuard] and the UI demands explicit
 *    confirmation before running it;
 *  - any SQLite error comes back as a [QueryResult.error] string, never as an
 *    exception that could crash the host.
 *
 * Room is a compileOnly dependency: apps without Room never register this
 * module (missing-dependency edge case).
 */
internal class DatabaseModule(
    private val db: RoomDatabase,
    private val dbName: String,
) : DevScopeModule {

    companion object {
        const val PAGE_SIZE = 50
    }

    override val id = "db"
    override val title = "DB"

    val displayName: String get() = dbName

    suspend fun listTables(): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            readAll(
                "SELECT name FROM sqlite_master WHERE type='table' " +
                    "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%' " +
                    "AND name != 'android_metadata' ORDER BY name"
            ).rows.map { it.first() }
        }.getOrDefault(emptyList())
    }

    suspend fun browse(table: String, page: Int): QueryResult = withContext(Dispatchers.IO) {
        // Table names come from sqlite_master (not free text), quoting is enough.
        val offset = page * PAGE_SIZE
        safeQuery("SELECT * FROM \"$table\" LIMIT ${PAGE_SIZE + 1} OFFSET $offset", pageProbe = true)
    }

    suspend fun runSql(sql: String): QueryResult = withContext(Dispatchers.IO) {
        if (SqlGuard.returnsRows(sql)) {
            safeQuery(sql)
        } else {
            runCatching {
                db.openHelper.writableDatabase.execSQL(sql)
                QueryResult(columns = listOf("result"), rows = listOf(listOf("OK")))
            }.getOrElse { QueryResult(emptyList(), emptyList(), error = it.message) }
        }
    }

    @Composable
    override fun Content() = DbTab(this)

    private fun safeQuery(sql: String, pageProbe: Boolean = false): QueryResult =
        runCatching { readAll(sql, pageProbe) }
            .getOrElse { QueryResult(emptyList(), emptyList(), error = it.message) }

    /** Runs [sql] and stringifies every cell; NULLs shown as "NULL". */
    private fun readAll(sql: String, pageProbe: Boolean = false): QueryResult {
        db.query(SimpleSQLiteQuery(sql)).use { cursor ->
            val columns = cursor.columnNames.toList()
            val rows = mutableListOf<List<String>>()
            while (cursor.moveToNext()) {
                rows += columns.indices.map { i ->
                    when {
                        cursor.isNull(i) -> "NULL"
                        // BLOB columns can't be read as text — show size instead.
                        cursor.getType(i) == android.database.Cursor.FIELD_TYPE_BLOB ->
                            "(blob ${cursor.getBlob(i).size} bytes)"
                        else -> cursor.getString(i) ?: "NULL"
                    }
                }
            }
            // pageProbe: we asked for PAGE_SIZE+1 rows just to learn if another
            // page exists; only PAGE_SIZE are shown.
            val hasMore = pageProbe && rows.size > PAGE_SIZE
            return QueryResult(columns, if (hasMore) rows.dropLast(1) else rows, hasMore)
        }
    }
}
