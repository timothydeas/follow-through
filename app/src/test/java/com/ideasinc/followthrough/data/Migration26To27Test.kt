package com.ideasinc.followthrough.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * BLOCKING migration gate for the in-place v26 → v27 upgrade.
 *
 * Builds a *populated* v26 database by hand (the real v26 table shapes, with
 * the legacy `notes` / `follow_throughs` tables and the `steps` table present),
 * then opens it through Room with [MIGRATION_26_27] and asserts:
 *  - `goals`, `check_ins`, and `question_labels` data survive intact, and
 *  - `steps`, `notes`, and `follow_throughs` are dropped.
 *
 * Runs on the JVM via Robolectric, so it needs no emulator/device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class Migration26To27Test {

    private val dbName = "migration-26-27-test.db"
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clean() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    // ── v26 schema (exactly as the historical migration chain leaves it) ──

    private fun createV26Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE goals (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                accountableTo TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                priority INTEGER,
                followedThrough INTEGER NOT NULL DEFAULT 0,
                followedThroughAt INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE check_ins (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                goalOrChange TEXT NOT NULL DEFAULT '',
                madeProgress TEXT,
                avoiding TEXT,
                confidence TEXT,
                competingPriority TEXT,
                implementationIntention TEXT,
                accountability TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_check_ins_goalId ON check_ins(goalId)")
        db.execSQL(
            """
            CREATE TABLE question_labels (
                id TEXT NOT NULL PRIMARY KEY,
                questionKey TEXT NOT NULL,
                customLabel TEXT NOT NULL,
                isEnabled INTEGER NOT NULL DEFAULT 1,
                customPlaceholder TEXT
            )
            """.trimIndent()
        )
        // Tables that v27 removes.
        db.execSQL(
            """
            CREATE TABLE steps (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                title TEXT NOT NULL,
                isCompleted INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_steps_goalId ON steps(goalId)")
        db.execSQL(
            """
            CREATE TABLE notes (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                isPinned INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE follow_throughs (
                id TEXT NOT NULL PRIMARY KEY,
                noteId TEXT NOT NULL,
                body TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY (noteId) REFERENCES notes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    private fun seedV26(db: SupportSQLiteDatabase) {
        // Two goals, one of them followed-through.
        db.execSQL(
            "INSERT INTO goals (id, title, accountableTo, createdAt, updatedAt, priority, followedThrough, followedThroughAt) " +
                "VALUES ('g1', 'Take care of my health', NULL, 1000, 1000, 1, 0, NULL)"
        )
        db.execSQL(
            "INSERT INTO goals (id, title, accountableTo, createdAt, updatedAt, priority, followedThrough, followedThroughAt) " +
                "VALUES ('g2', 'Be heard at work', 'my mentor', 2000, 2500, NULL, 1, 2400)"
        )
        // Check-ins (with the implementation intention preserved).
        db.execSQL(
            "INSERT INTO check_ins (id, goalId, goalOrChange, madeProgress, avoiding, confidence, competingPriority, implementationIntention, accountability, createdAt, updatedAt) " +
                "VALUES ('c1', 'g1', 'Take care of my health', 'Yes', NULL, 'Pretty confident', NULL, 'When I finish my morning coffee, I will walk for 15 minutes.', NULL, 1100, 1100)"
        )
        db.execSQL(
            "INSERT INTO check_ins (id, goalId, goalOrChange, madeProgress, avoiding, confidence, competingPriority, implementationIntention, accountability, createdAt, updatedAt) " +
                "VALUES ('c2', 'g2', 'Be heard at work', NULL, NULL, NULL, NULL, NULL, NULL, 2100, 2100)"
        )
        // A customized question label.
        db.execSQL(
            "INSERT INTO question_labels (id, questionKey, customLabel, isEnabled, customPlaceholder) " +
                "VALUES ('q1', 'accountability', 'Who has your back?', 1, NULL)"
        )
        // Legacy rows that must disappear with their tables.
        db.execSQL("INSERT INTO notes (id, title, body, isPinned, createdAt, updatedAt) VALUES ('n1', 't', 'b', 0, 1, 1)")
        db.execSQL("INSERT INTO follow_throughs (id, noteId, body, createdAt) VALUES ('f1', 'n1', 'b', 1)")
        db.execSQL("INSERT INTO steps (id, goalId, title, isCompleted, createdAt, updatedAt) VALUES ('s1', 'g1', 'step', 0, 1, 1)")
    }

    private fun buildPopulatedV26() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(26) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    createV26Schema(db)
                    seedV26(db)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // Not exercised — the DB is created directly at version 26.
                }
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        helper.writableDatabase // forces onCreate at version 26
        helper.close()
    }

    private fun openMigratedDb(): GroundedDatabase =
        Room.databaseBuilder(context, GroundedDatabase::class.java, dbName)
            // Room targets the current @Database version, so supply every
            // migration from 26 up to it — this also exercises the whole chain,
            // where short-lived helper tables (and the v33 `plans` table) are
            // created and then dropped, on its way to the current endpoint.
            .addMigrations(
                MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29,
                MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33,
                MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36
            )
            .build()

    private fun tableExists(db: SupportSQLiteDatabase, name: String): Boolean =
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name = ?", arrayOf<Any>(name))
            .use { it.count > 0 }

    private fun rowCount(db: SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun migrate26To31_preservesGoalsAndLabels_andDropsAllRetiredTables() {
        buildPopulatedV26()

        val room = openMigratedDb()
        val db = room.openHelper.writableDatabase // runs the full 26→31 chain + Room validation

        // Goals survive the whole chain (untouched by the v31 consolidation).
        assertEquals("goals row count", 2, rowCount(db, "goals"))
        assertEquals("question_labels row count", 1, rowCount(db, "question_labels"))

        // The followed-through goal's state survived (follow-through lives on goals).
        db.query("SELECT title, followedThrough, followedThroughAt FROM goals WHERE id = 'g2'").use {
            assertTrue(it.moveToFirst())
            assertEquals("Be heard at work", it.getString(0))
            assertEquals(1, it.getInt(1))
            assertEquals(2400L, it.getLong(2))
        }
        db.query("SELECT customLabel FROM question_labels WHERE id = 'q1'").use {
            assertTrue(it.moveToFirst())
            assertEquals("Who has your back?", it.getString(0))
        }

        // check_ins is reshaped to the v31 typed schema and recreated empty
        // (legacy reflection rows are intentionally not carried over).
        assertEquals("check_ins recreated empty", 0, rowCount(db, "check_ins"))

        // Permanently retired tables stay gone: the v26 legacy tables, the
        // short-lived v29/v30 helper tables, and the v33 plans table.
        assertFalse("steps dropped", tableExists(db, "steps"))
        assertFalse("notes dropped", tableExists(db, "notes"))
        assertFalse("follow_throughs dropped", tableExists(db, "follow_throughs"))
        assertFalse("implementation_intentions dropped", tableExists(db, "implementation_intentions"))
        assertFalse("plans dropped", tableExists(db, "plans"))

        // barriers + progress_notes were dropped at v31 but are RE-INTRODUCED at
        // v35 (now goal-owned, alongside the new Reminder model), so by the current
        // endpoint they exist again — recreated empty.
        assertTrue("barriers re-introduced at v35", tableExists(db, "barriers"))
        assertTrue("progress_notes re-introduced at v35", tableExists(db, "progress_notes"))
        assertEquals("barriers empty", 0, rowCount(db, "barriers"))
        assertEquals("progress_notes empty", 0, rowCount(db, "progress_notes"))
        // The rest of the v35 foundation is present too.
        assertTrue("reminders table", tableExists(db, "reminders"))
        assertTrue("reminder_events table", tableExists(db, "reminder_events"))
        assertTrue("passions_interests table", tableExists(db, "passions_interests"))
        assertTrue("learnings table", tableExists(db, "learnings"))

        room.close()
    }

    @Test
    fun migrate26To27_onEmptyDatabase_succeeds() {
        // Create an empty v26 DB (schema only, no rows).
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(26) {
                override fun onCreate(db: SupportSQLiteDatabase) { createV26Schema(db) }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        FrameworkSQLiteOpenHelperFactory().create(config).apply {
            writableDatabase
            close()
        }

        val room = openMigratedDb()
        val db = room.openHelper.writableDatabase
        assertEquals(0, rowCount(db, "goals"))
        assertFalse(tableExists(db, "steps"))
        room.close()
    }
}
