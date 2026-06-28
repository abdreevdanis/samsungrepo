package com.rassvet.essential.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val testDb = "migration-test"

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migration4To5_createsFtsTable() {
        helper.createDatabase(testDb, 4).close()

        helper.runMigrationsAndValidate(testDb, 5, true, AppDatabaseMigrations.MIGRATION_4_5).use { db ->
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='note_fts'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }
    }

    @Test
    fun freshDatabase_hasFtsTable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.build(context)
        db.openHelper.writableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='note_fts'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        db.close()
    }
}
