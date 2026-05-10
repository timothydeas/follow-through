package com.example.grounded.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun fromNoteType(type: NoteType): String = type.name

    @TypeConverter
    fun toNoteType(value: String): NoteType = NoteType.valueOf(value)
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN type TEXT NOT NULL DEFAULT 'NOTE'")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN followedThrough INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS notes_new (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                tag TEXT,
                isPinned INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                type TEXT NOT NULL DEFAULT 'REFLECTION'
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO notes_new (id, title, body, tag, isPinned, createdAt, updatedAt, type)
            SELECT id, title, body, tag, isPinned, createdAt, updatedAt,
                CASE WHEN type = 'NOTE' THEN 'REFLECTION' ELSE type END
            FROM notes
            """.trimIndent()
        )
        db.execSQL("DROP TABLE notes")
        db.execSQL("ALTER TABLE notes_new RENAME TO notes")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS follow_throughs (
                id TEXT NOT NULL PRIMARY KEY,
                noteId TEXT NOT NULL,
                body TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY (noteId) REFERENCES notes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_follow_throughs_noteId ON follow_throughs(noteId)"
        )
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE notes SET
                type = 'FOLLOW_THROUGH',
                body = CASE
                    WHEN type = 'FOLLOW_THROUGH' THEN body
                    ELSE char(9) || char(9) || body
                END
            """.trimIndent()
        )
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE notes SET
                body = title || char(9) ||
                       CASE
                           WHEN instr(body, char(9)) > 0
                           THEN substr(body, instr(body, char(9)) + 1)
                           ELSE ''
                       END,
                tag = NULL
            """.trimIndent()
        )
    }
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS notes_new (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                tag TEXT,
                isPinned INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                type TEXT NOT NULL DEFAULT 'REFLECTION'
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO notes_new (id, title, body, tag, isPinned, createdAt, updatedAt, type)
            SELECT
                id,
                CASE
                    WHEN instr(body, char(9)) > 0
                         AND instr(substr(body, instr(body, char(9)) + 1), char(9)) > 0
                    THEN substr(
                             substr(body, instr(body, char(9)) + 1),
                             instr(substr(body, instr(body, char(9)) + 1), char(9)) + 1
                         )
                    ELSE ''
                END AS title,
                CASE
                    WHEN instr(body, char(9)) > 0
                         AND instr(substr(body, instr(body, char(9)) + 1), char(9)) > 0
                    THEN substr(
                             substr(body, instr(body, char(9)) + 1),
                             instr(substr(body, instr(body, char(9)) + 1), char(9)) + 1
                         )
                    ELSE ''
                END || char(9) ||
                CASE
                    WHEN instr(body, char(9)) > 0
                         AND instr(substr(body, instr(body, char(9)) + 1), char(9)) > 0
                    THEN substr(
                             substr(body, instr(body, char(9)) + 1),
                             instr(substr(body, instr(body, char(9)) + 1), char(9)) + 1
                         )
                    ELSE ''
                END || char(9) ||
                '' || char(9) ||
                '' || char(9) ||
                CASE
                    WHEN instr(body, char(9)) > 0
                    THEN substr(body, 1, instr(body, char(9)) - 1)
                    ELSE body
                END || char(9) ||
                CASE
                    WHEN instr(body, char(9)) > 0
                    THEN CASE
                             WHEN instr(substr(body, instr(body, char(9)) + 1), char(9)) > 0
                             THEN substr(
                                      substr(body, instr(body, char(9)) + 1),
                                      1,
                                      instr(substr(body, instr(body, char(9)) + 1), char(9)) - 1
                                  )
                             ELSE substr(body, instr(body, char(9)) + 1)
                         END
                    ELSE ''
                END AS body,
                NULL AS tag,
                isPinned,
                createdAt,
                updatedAt,
                'REFLECTION' AS type
            FROM notes
            """.trimIndent()
        )
        db.execSQL("DROP TABLE notes")
        db.execSQL("ALTER TABLE notes_new RENAME TO notes")
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            DELETE FROM notes
            WHERE TRIM(title) IN ('a', 'j', 'k')
               OR (isDraft = 1 AND createdAt >= 1744502400000 AND createdAt < 1744848000000)
            """.trimIndent()
        )
    }
}

private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN reflection TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE notes ADD COLUMN whatStoppedYou TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE notes ADD COLUMN whatYouLearned TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE notes ADD COLUMN nextSteps TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE notes ADD COLUMN whenField TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE notes ADD COLUMN willField TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE notes SET reflection = body WHERE body != ''")
    }
}

private val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN followedThrough INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE notes ADD COLUMN followedThroughAt INTEGER")
        db.execSQL("ALTER TABLE notes ADD COLUMN implementationIntention TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            """
            UPDATE notes SET implementationIntention =
            CASE
                WHEN whenField != '' AND willField != '' THEN 'When ' || whenField || ', I will ' || willField
                WHEN whenField != '' THEN 'When ' || whenField
                WHEN willField != '' THEN 'I will ' || willField
                ELSE ''
            END
            WHERE whenField != '' OR willField != ''
            """.trimIndent()
        )
    }
}

private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE notes SET
                body = CASE
                    WHEN instr(body, char(9)) > 0
                    THEN substr(body, 1, instr(body, char(9)) - 1)
                    ELSE body
                END
            """.trimIndent()
        )
        db.execSQL("DELETE FROM notes WHERE isDraft = 1")
    }
}

private val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add followedThrough columns to goals
        db.execSQL("ALTER TABLE goals ADD COLUMN followedThrough INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE goals ADD COLUMN followedThroughAt INTEGER")

        // Backfill goals from existing check_ins: a goal is followed-through if any
        // of its check-ins was marked followed-through. Use the most recent
        // followedThroughAt timestamp.
        db.execSQL(
            """
            UPDATE goals SET
                followedThrough = 1,
                followedThroughAt = (
                    SELECT MAX(followedThroughAt)
                    FROM check_ins
                    WHERE check_ins.goalId = goals.id AND check_ins.followedThrough = 1
                )
            WHERE id IN (
                SELECT goalId FROM check_ins WHERE followedThrough = 1
            )
            """.trimIndent()
        )

        // Recreate check_ins without followedThrough / followedThroughAt columns,
        // preserving every other column and the foreign key + index.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS check_ins_new (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                reflection TEXT NOT NULL DEFAULT '',
                whatDidYouLearn TEXT NOT NULL DEFAULT '',
                madeProgress TEXT NOT NULL DEFAULT '',
                accountableWhenBackingOut TEXT,
                implementationIntention TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO check_ins_new (
                id, goalId, reflection, whatDidYouLearn, madeProgress,
                accountableWhenBackingOut, implementationIntention, createdAt, updatedAt
            )
            SELECT
                id, goalId, reflection, whatDidYouLearn, madeProgress,
                accountableWhenBackingOut, implementationIntention, createdAt, updatedAt
            FROM check_ins
            """.trimIndent()
        )
        db.execSQL("DROP TABLE check_ins")
        db.execSQL("ALTER TABLE check_ins_new RENAME TO check_ins")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_check_ins_goalId ON check_ins(goalId)")
    }
}

private val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Replace check_ins schema with the new seven-question shape:
        //   goalOrChange (NOT NULL, default ''),
        //   avoiding, confidence, madeProgress, temptationAndSelfTalk,
        //   implementationIntention, accountability (all nullable).
        // Mapping from old → new:
        //   reflection                  → goalOrChange
        //   accountableWhenBackingOut   → accountability
        //   madeProgress                → madeProgress (NOT NULL '' becomes NULL when blank)
        //   implementationIntention     → implementationIntention (unchanged)
        //   whatDidYouLearn             → dropped (no semantic match)
        // Question label customizations are best-effort migrated.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS check_ins_new (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                goalOrChange TEXT NOT NULL DEFAULT '',
                avoiding TEXT,
                confidence TEXT,
                madeProgress TEXT,
                temptationAndSelfTalk TEXT,
                implementationIntention TEXT,
                accountability TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO check_ins_new (
                id, goalId, goalOrChange, avoiding, confidence, madeProgress,
                temptationAndSelfTalk, implementationIntention, accountability,
                createdAt, updatedAt
            )
            SELECT
                id,
                goalId,
                COALESCE(reflection, ''),
                NULL,
                NULL,
                CASE WHEN madeProgress IS NULL OR madeProgress = '' THEN NULL ELSE madeProgress END,
                NULL,
                implementationIntention,
                accountableWhenBackingOut,
                createdAt,
                updatedAt
            FROM check_ins
            """.trimIndent()
        )
        db.execSQL("DROP TABLE check_ins")
        db.execSQL("ALTER TABLE check_ins_new RENAME TO check_ins")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_check_ins_goalId ON check_ins(goalId)")

        // Migrate question_labels customizations to the new key set.
        // Old key "accountableWhenBackingOut" → new key "accountability".
        // Drop old keys that have no semantic counterpart so resolveConfigs
        // (which iterates only the new ALL_KEYS list) does not see stale rows.
        db.execSQL(
            """
            UPDATE question_labels
            SET questionKey = 'accountability'
            WHERE questionKey = 'accountableWhenBackingOut'
            """.trimIndent()
        )
        db.execSQL(
            """
            DELETE FROM question_labels
            WHERE questionKey IN ('reflection', 'whatDidYouLearn')
            """.trimIndent()
        )
    }
}

private val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No schema changes — only entity field order and Kotlin default labels
        // were updated to reflect the new question order. Column names, types,
        // nullability, defaults, indices, and foreign keys all remain identical
        // to v16, so no SQL is required and existing data is preserved as-is.
    }
}

private val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE check_ins ADD COLUMN competingPriority TEXT")
    }
}

private val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Drop the temptationAndSelfTalk column. SQLite ALTER TABLE DROP COLUMN
        // is unavailable on older Android versions, so recreate the table
        // preserving every other column, the foreign key, and the index.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS check_ins_new (
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
        db.execSQL(
            """
            INSERT INTO check_ins_new (
                id, goalId, goalOrChange, madeProgress, avoiding, confidence,
                competingPriority, implementationIntention, accountability,
                createdAt, updatedAt
            )
            SELECT
                id, goalId, goalOrChange, madeProgress, avoiding, confidence,
                competingPriority, implementationIntention, accountability,
                createdAt, updatedAt
            FROM check_ins
            """.trimIndent()
        )
        db.execSQL("DROP TABLE check_ins")
        db.execSQL("ALTER TABLE check_ins_new RENAME TO check_ins")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_check_ins_goalId ON check_ins(goalId)")

        // Remove the now-defunct question_labels row for the dropped key so
        // resolveConfigs (which iterates only the new ALL_KEYS list) does not
        // see stale data.
        db.execSQL("DELETE FROM question_labels WHERE questionKey = 'temptationAndSelfTalk'")
    }
}

private val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create goals table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS goals (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                accountableTo TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                priority INTEGER
            )
            """.trimIndent()
        )

        // Create check_ins table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS check_ins (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                reflection TEXT NOT NULL DEFAULT '',
                whatDidYouLearn TEXT NOT NULL DEFAULT '',
                madeProgress TEXT NOT NULL DEFAULT '',
                accountableWhenBackingOut TEXT,
                implementationIntention TEXT,
                followedThrough INTEGER NOT NULL DEFAULT 0,
                followedThroughAt INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_check_ins_goalId ON check_ins(goalId)")

        // Create question_labels table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS question_labels (
                id TEXT NOT NULL PRIMARY KEY,
                questionKey TEXT NOT NULL,
                customLabel TEXT NOT NULL,
                isEnabled INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        // Migrate existing notes → goals (reuse note id as goal id)
        db.execSQL(
            """
            INSERT INTO goals (id, title, accountableTo, createdAt, updatedAt, priority)
            SELECT
                id,
                CASE
                    WHEN reflection != '' THEN
                        TRIM(CASE
                            WHEN instr(reflection, char(10)) > 0
                            THEN substr(reflection, 1, instr(reflection, char(10)) - 1)
                            ELSE reflection
                        END)
                    WHEN title != '' THEN title
                    ELSE 'Untitled goal'
                END,
                NULL,
                createdAt,
                updatedAt,
                NULL
            FROM notes
            """.trimIndent()
        )

        // Migrate existing notes → check_ins (id = 'ci_' + note id)
        db.execSQL(
            """
            INSERT INTO check_ins (id, goalId, reflection, whatDidYouLearn, madeProgress,
                accountableWhenBackingOut, implementationIntention, followedThrough,
                followedThroughAt, createdAt, updatedAt)
            SELECT
                'ci_' || id,
                id,
                CASE WHEN reflection != '' THEN reflection ELSE body END,
                whatYouLearned,
                nextSteps,
                NULL,
                CASE WHEN implementationIntention != '' THEN implementationIntention ELSE NULL END,
                followedThrough,
                followedThroughAt,
                createdAt,
                updatedAt
            FROM notes
            """.trimIndent()
        )
    }
}

@TypeConverters(Converters::class)
@Database(
    entities = [
        GroundedNote::class,
        FollowThroughEntry::class,
        Goal::class,
        CheckIn::class,
        QuestionLabel::class
    ],
    version = 19,
    exportSchema = false
)
abstract class GroundedDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun followThroughDao(): FollowThroughDao
    abstract fun goalDao(): GoalDao
    abstract fun checkInDao(): CheckInDao
    abstract fun questionLabelDao(): QuestionLabelDao

    companion object {
        @Volatile private var INSTANCE: GroundedDatabase? = null

        fun getInstance(context: Context): GroundedDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GroundedDatabase::class.java,
                    "grounded.db"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                        MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
                        MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19
                    )
                    .build().also { INSTANCE = it }
            }
    }
}
