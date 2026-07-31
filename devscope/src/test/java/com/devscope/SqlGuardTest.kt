package com.devscope

import com.devscope.db.SqlGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlGuardTest {

    @Test
    fun `select is not destructive`() {
        assertFalse(SqlGuard.isDestructive("SELECT * FROM users"))
        assertFalse(SqlGuard.isDestructive("  select id from users where name = 'delete'"))
    }

    @Test
    fun `mutating statements are destructive regardless of case and spacing`() {
        assertTrue(SqlGuard.isDestructive("DELETE FROM users"))
        assertTrue(SqlGuard.isDestructive("   drop table users"))
        assertTrue(SqlGuard.isDestructive("Update users SET name='x'"))
        assertTrue(SqlGuard.isDestructive("insert into users values (1)"))
        assertTrue(SqlGuard.isDestructive("ALTER TABLE users ADD COLUMN x TEXT"))
    }

    @Test
    fun `row-returning statements are detected`() {
        assertTrue(SqlGuard.returnsRows("SELECT 1"))
        assertTrue(SqlGuard.returnsRows("PRAGMA table_info(users)"))
        assertTrue(SqlGuard.returnsRows("WITH t AS (SELECT 1) SELECT * FROM t"))
        assertFalse(SqlGuard.returnsRows("DELETE FROM users"))
    }
}
