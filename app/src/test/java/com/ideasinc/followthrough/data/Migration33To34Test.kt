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
 * BLOCKING migration gate for the v33 → v34 revert: the implementation intention
 * and the cue move back ONTO `check_ins`, and the separate `plans` table is
 * dropped. No data is preserved (pre-release), so `check_ins` is recreated empty.
 *
 * Builds a populated v33 database by hand (a `plans` table + intention-less
 * `check_ins`), opens it through Room with [MIGRATION_33_34], and asserts:
 *  - the migration runs and Room validation passes,
 *  - the `plans` table is gone,
 *  - `check_ins` is empty and carries the restored columns (intention + cue), so a
 *    fully-specified check-in can be inserted and read back,
 *  - goals survive untouched.
 *
 * Runs on the JVM via Robolectric — no emulator/device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class Migration33To34Test {

    private val dbName = "migration-33-34-test.db"
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clean() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createV33Schema(db: SupportSQLiteDatabase) {
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
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_check_ins_goalId ON check_ins(goalId)")
        db.execSQL(
            """
            CREATE TABLE plans (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
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
        db.execSQL("CREATE INDEX index_plans_goalId ON plans(goalId)")
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

    private fun seedV33(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO goals (id, title, accountableTo, createdAt, updatedAt, priority, followedThrough, followedThroughAt) " +
                "VALUES ('g1', 'Stay close to the people I care about', NULL, 1000, 1000, 1, 0, NULL)"
        )
        db.execSQL("INSERT INTO check_ins (id, goalId, type, note, createdAt, updatedAt) VALUES ('c1', 'g1', 'barrier', 'weeks slip by', 1100, 1100)")
        db.execSQL("INSERT INTO plans (id, goalId, intention, createdAt, updatedAt) VALUES ('plan_g1', 'g1', 'old plan', 1200, 1200)")
    }

    private fun buildPopulatedV33() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(33) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    createV33Schema(db)
                    seedV33(db)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        helper.writableDatabase // forces onCreate at version 33
        helper.close()
    }

    private fun openMigratedDb(): GroundedDatabase =
        Room.databaseBuilder(context, GroundedDatabase::class.java, dbName)
            .addMigrations(MIGRATION_33_34)
            .build()

    private fun tableExists(db: SupportSQLiteDatabase, name: String): Boolean =
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name = ?", arrayOf<Any>(name))
            .use { it.count > 0 }

    private fun rowCount(db: SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun migrate33To34_dropsPlans_restoresCheckInColumns_empty() {
        buildPopulatedV33()

        val room = openMigratedDb()
        val db = room.openHelper.writableDatabase // triggers 33→34 + Room validation

        assertFalse("plans dropped", tableExists(db, "plans"))
        assertTrue("check_ins exists", tableExists(db, "check_ins"))
        assertEquals("check_ins recreated empty", 0, rowCount(db, "check_ins"))
        assertEquals("goals survive", 1, rowCount(db, "goals"))

        // A fully-specified check-in (intention + cue) round-trips.
        db.execSQL(
            "INSERT INTO check_ins (id, goalId, type, note, intention, cueEmoji, cueLabel, cueImagePath, cueSound, createdAt, updatedAt) " +
                "VALUES ('c1', 'g1', 'barrier', 'weeks slip by', 'When I pour my morning coffee, I''ll text one person', " +
                "'🛸', 'coffee text', '/data/x.jpg', 'content://media/1', 1500, 1500)"
        )
        db.query("SELECT intention, cueEmoji, cueSound FROM check_ins WHERE id = 'c1'").use {
            assertTrue(it.moveToFirst())
            assertEquals("When I pour my morning coffee, I'll text one person", it.getString(0))
            assertEquals("🛸", it.getString(1))
            assertEquals("content://media/1", it.getString(2))
        }

        room.close()
    }
}
