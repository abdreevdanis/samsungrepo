package com.rassvet.essential.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {
    val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS chat_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        last_updated_at INTEGER NOT NULL
                    )""",
                )
                db.execSQL(
                    """CREATE INDEX IF NOT EXISTS index_chat_sessions_last_updated_at
                       ON chat_sessions(last_updated_at)""",
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS chat_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        session_id INTEGER NOT NULL,
                        is_user INTEGER NOT NULL,
                        text TEXT NOT NULL,
                        attachments_json TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )""",
                )
                db.execSQL(
                    """CREATE INDEX IF NOT EXISTS index_chat_messages_session_id
                       ON chat_messages(session_id)""",
                )
            }
        }

    val MIGRATION_4_5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppDatabaseFts.ensureNoteFtsTable(db)
            }
        }

    val MIGRATION_5_6 =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """ALTER TABLE chat_messages ADD COLUMN sources_json TEXT NOT NULL DEFAULT '[]'""",
                )
            }
        }

    val ALL = arrayOf(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}
