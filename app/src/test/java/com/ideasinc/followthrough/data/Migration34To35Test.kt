package com.ideasinc.followthrough.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * BLOCKING migration gate for the v34 → v35 prototype-alignment foundation. The
 * step is purely ADDITIVE: a new `goals.whyItMatters` column plus six new tables
 * (`reminders`, `reminder_events`, `passions_interests`, `learnings`, `barriers`,
 * `progress_notes`). Existing goals and check-ins must survive untouched.
 *
 * Builds a populated v34 database by hand, opens it through Room with
 * [MIGRATION_34_35], and asserts:
 *  - the migration runs and Room's schema validation passes (proves the hand-written
 *    CREATE TABLE SQL matches the v35 entities exactly — the real reason this test
 *    exists, since we cannot device-test here),
 *  - the pre-existing goal + check-in survive, with whyItMatters defaulted to '',
 *  - a fully-specified reminder, event, palette entry, barrier, and progress note
 *    each round-trip.
 *
 * Runs on the JVM via Robolectric — no emulator/device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class Migration34To35Test {

    private val dbName = "migration-34-35-test.db"
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clean() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createV34Schema(db: SupportSQLiteDatabase) {
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
                type TEXT NOT NULL,
                note TEXT NOT NULL,
                intention TEXT NOT NULL,
                cueEmoji TEXT,
                cueLabel TEXT,
                cueImagePath TEXT,
                cueSound TEXT,
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
    }

    private fun seedV34(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO goals (id, title, accountableTo, createdAt, updatedAt, priority, followedThrough, followedThroughAt) " +
                "VALUES ('g1', 'Run a 5K without stopping by August', NULL, 1000, 1000, 0, 0, NULL)"
        )
        db.execSQL(
            "INSERT INTO check_ins (id, goalId, type, note, intention, createdAt, updatedAt) " +
                "VALUES ('c1', 'g1', 'progress', 'Ran 1.5 miles', '', 1100, 1100)"
        )
    }

    private fun buildPopulatedV34() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(34) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    createV34Schema(db)
                    seedV34(db)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        helper.writableDatabase // forces onCreate at version 34
        helper.close()
    }

    private fun openMigratedDb(): GroundedDatabase =
        Room.databaseBuilder(context, GroundedDatabase::class.java, dbName)
            .addMigrations(MIGRATION_34_35)
            .build()

    private fun tableExists(db: SupportSQLiteDatabase, name: String): Boolean =
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name = ?", arrayOf<Any>(name))
            .use { it.count > 0 }

    private fun rowCount(db: SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun migrate34To35_addsTables_preservesData_andValidates() {
        buildPopulatedV34()

        val room = openMigratedDb()
        val db = room.openHelper.writableDatabase // triggers 34→35 + Room validation

        // Existing data survives untouched.
        assertEquals("goal survives", 1, rowCount(db, "goals"))
        assertEquals("check-in survives", 1, rowCount(db, "check_ins"))
        db.query("SELECT whyItMatters FROM goals WHERE id = 'g1'").use {
            assertTrue(it.moveToFirst())
            assertEquals("whyItMatters defaults to empty", "", it.getString(0))
        }

        // New tables exist and are empty.
        for (t in listOf("reminders", "reminder_events", "passions_interests", "learnings", "barriers", "progress_notes")) {
            assertTrue("$t exists", tableExists(db, t))
            assertEquals("$t empty", 0, rowCount(db, t))
        }

        // A fully-specified reminder + its event round-trip.
        db.execSQL(
            "INSERT INTO reminders (id, goalId, whenMoment, iWill, cueType, cueValue, cueAltText, " +
                "cueSourcePaletteId, cueIsPaletteDrawn, scheduleMode, scheduleDays, scheduleTimeLocal, " +
                "scheduleTimezone, fullTextAlwaysShown, status, createdAt, updatedAt) " +
                "VALUES ('rem1', 'g1', 'I change out of work clothes', 'put on running shoes', 'phrase', " +
                "'Biscuit''s leash is the starting line', NULL, 'pi_002', 1, 'weekly', 'MON,WED,FRI', " +
                "'17:30', 'America/Los_Angeles', 1, 'active', 1500, 1500)"
        )
        db.execSQL(
            "INSERT INTO reminder_events (id, reminderId, deliveredAt, action, actedAt, undone, undoReason, reflectionText) " +
                "VALUES ('ev1', 'rem1', 1600, 'done', 1601, 0, NULL, NULL)"
        )
        db.execSQL("INSERT INTO passions_interests (id, label, emoji, createdAt) VALUES ('pi_002', 'My dog Biscuit', '🐕', 1700)")
        db.execSQL("INSERT INTO learnings (id, text, createdAt) VALUES ('ln1', 'Morning me keeps promises', 1700)")
        db.execSQL("INSERT INTO barriers (id, goalId, text, createdAt) VALUES ('br1', 'g1', 'After-work slump', 1700)")
        db.execSQL("INSERT INTO progress_notes (id, goalId, text, createdAt) VALUES ('pr1', 'g1', 'Knees fine', 1700)")

        assertEquals(1, rowCount(db, "reminders"))
        assertEquals(1, rowCount(db, "reminder_events"))
        assertEquals(1, rowCount(db, "passions_interests"))
        assertEquals(1, rowCount(db, "learnings"))
        assertEquals(1, rowCount(db, "barriers"))
        assertEquals(1, rowCount(db, "progress_notes"))

        room.close()
    }
}
