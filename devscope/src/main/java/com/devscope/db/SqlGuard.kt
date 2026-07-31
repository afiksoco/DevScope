package com.devscope.db

/**
 * Classifies free-form SQL typed into the DB tab.
 * Pure logic, covered by unit tests (see SqlGuardTest).
 */
object SqlGuard {

    private val DESTRUCTIVE = Regex(
        """^\s*(drop|delete|update|insert|replace|alter|create|truncate|vacuum|reindex)\b""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * DB-safety edge case: anything that can change the schema or the data
     * requires an explicit confirmation in the UI before it runs.
     */
    fun isDestructive(sql: String): Boolean = DESTRUCTIVE.containsMatchIn(sql)

    /** SELECT-like statements return rows; everything else just executes. */
    fun returnsRows(sql: String): Boolean =
        Regex("""^\s*(select|pragma|explain|with)\b""", RegexOption.IGNORE_CASE)
            .containsMatchIn(sql)
}
