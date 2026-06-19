package com.ideasinc.followthrough.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

private val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The Step 5 and Step 6 default question labels were reworded. The
        // check_ins schema is unchanged (column names and types remain), so no
        // ALTER TABLE is needed and all stored answers are preserved.
        //
        // For question_labels rows that still hold the prior default text
        // (which happens when a user toggled the question without editing the
        // label), rewrite them to the new defaults so the UI reflects the new
        // wording. Rows holding genuinely custom text are left untouched.
        db.execSQL(
            "UPDATE question_labels SET customLabel = " +
                "'What''s getting in your way right now — is it the situation itself, " +
                "or how you''re seeing it or expecting it to go? " +
                "If nothing is, what might get in the way later?' " +
                "WHERE questionKey = 'competingPriority' AND customLabel = " +
                "'What''s getting in your way right now — is it the situation itself, " +
                "or how you''re seeing it or expecting it to go?'"
        )
        db.execSQL(
            "UPDATE question_labels SET customLabel = " +
                "'I will [what I''ll do] when [moment or situation] occurs.' " +
                "WHERE questionKey = 'implementationIntention' AND customLabel = " +
                "'When this moment comes, I will —'"
        )
    }
}

private val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The implementation intention default label was reworded from a
        // bracketed-template form to a fill-in-the-blanks form. Rewrite any
        // question_labels row that still holds the prior default text so the
        // UI reflects the new wording. Rows with genuinely custom text are
        // left untouched.
        db.execSQL(
            "UPDATE question_labels SET customLabel = " +
                "'I will _____ when _____.' " +
                "WHERE questionKey = 'implementationIntention' AND customLabel = " +
                "'I will [what I''ll do] when [moment or situation] occurs.'"
        )
    }
}

private val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The implementation intention default label was reverted to the
        // earlier "When this moment comes, I will —" wording. Rewrite any
        // question_labels row still holding either of the two prior default
        // texts so the UI reflects the new wording. Rows with genuinely
        // custom text are left untouched.
        db.execSQL(
            "UPDATE question_labels SET customLabel = " +
                "'When this moment comes, I will —' " +
                "WHERE questionKey = 'implementationIntention' AND customLabel IN (" +
                "'I will _____ when _____.', " +
                "'I will [what I''ll do] when [moment or situation] occurs.'" +
                ")"
        )
    }
}

private val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Force every implementation intention question_labels row to the new
        // example-bracketed default. Unlike the prior migrations this does
        // not filter by customLabel — it overwrites any existing value for
        // this key, including user-customized ones.
        db.execSQL(
            "UPDATE question_labels SET customLabel = " +
                "'I will [e.g., what I''ll do] when [e.g., moment or situation] occurs.' " +
                "WHERE questionKey = 'implementationIntention'"
        )
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

private val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The implementation intention default label was reworded to lead with
        // the situation cue. Overwrite any existing value for this key so every
        // user — including those with customized text — sees the new wording.
        db.execSQL(
            "UPDATE question_labels SET customLabel = " +
                "'When [e.g., moment or situation], I will [e.g., what I''ll do].' " +
                "WHERE questionKey = 'implementationIntention'"
        )
    }
}

private val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add the steps table for sub-goals. Cascades on goal deletion and is
        // indexed by goalId, matching the Step entity Room generates.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS steps (
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
        db.execSQL("CREATE INDEX IF NOT EXISTS index_steps_goalId ON steps(goalId)")
    }
}

private val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add the per-question custom placeholder column. Nullable — a null
        // value means "fall back to the built-in default placeholder".
        db.execSQL("ALTER TABLE question_labels ADD COLUMN customPlaceholder TEXT")
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

internal val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // MVP cleanup: the sub-goal "steps" feature and the two legacy
        // pre-Goal tables ("notes" and its child "follow_throughs") are removed.
        // Goal / CheckIn / QuestionLabel are untouched, so all user goals,
        // check-ins, and question customizations survive the upgrade intact.
        //
        // follow_throughs is dropped first because it holds a foreign key into
        // notes (ON DELETE CASCADE); dropping the child before the parent keeps
        // SQLite happy regardless of foreign_keys pragma state.
        db.execSQL("DROP TABLE IF EXISTS follow_throughs")
        db.execSQL("DROP TABLE IF EXISTS notes")
        db.execSQL("DROP TABLE IF EXISTS steps")
    }
}

internal val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The "avoiding" and "competingPriority" default question labels were
        // reworded. The check_ins schema is unchanged, so every stored answer is
        // preserved. For question_labels rows that still hold a prior default
        // (which happens when a user toggled the question without editing its
        // text), rewrite them to the new wording. Rows with genuinely custom
        // text are left untouched.
        db.execSQL(
            "UPDATE question_labels SET customLabel = " +
                "'Is there something you''ve been avoiding facing — even though you know it would help?' " +
                "WHERE questionKey = 'avoiding' AND customLabel = " +
                "'Is there something you already know would help you here, " +
                "but you''ve been avoiding finding out or facing?'"
        )
        // competingPriority's short default shipped without a migration, so an
        // un-customized row may still hold either that short form or the older
        // long form. Catch both.
        db.execSQL(
            "UPDATE question_labels SET customLabel = " +
                "'What''s getting in your way — the situation itself, or how you''re perceiving it?' " +
                "WHERE questionKey = 'competingPriority' AND customLabel IN (" +
                "'What''s getting in your way — the situation itself, or how you''re seeing it?', " +
                "'What''s getting in your way right now — is it the situation itself, " +
                "or how you''re seeing it or expecting it to go? " +
                "If nothing is, what might get in the way later?'" +
                ")"
        )
    }
}

internal val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The implementation intention moves off check_ins and onto a goal-owned
        // list. Create the new table, then back-fill each goal's newest non-blank
        // check-in intention as a single starter row. The old
        // check_ins.implementationIntention column is intentionally kept (read-only
        // history in the Check-in Read screen); nothing is dropped.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS implementation_intentions (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                text TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_implementation_intentions_goalId " +
                "ON implementation_intentions(goalId)"
        )
        // One row per goal: the most recent check-in that carries a non-blank
        // intention. Simple back-fill — on-device data preservation isn't critical.
        db.execSQL(
            """
            INSERT INTO implementation_intentions (id, goalId, text, createdAt)
            SELECT 'ii_' || ci.id, ci.goalId, ci.implementationIntention, ci.createdAt
            FROM check_ins ci
            WHERE ci.implementationIntention IS NOT NULL
              AND ci.implementationIntention != ''
              AND ci.createdAt = (
                  SELECT MAX(c2.createdAt) FROM check_ins c2
                  WHERE c2.goalId = ci.goalId
                    AND c2.implementationIntention IS NOT NULL
                    AND c2.implementationIntention != ''
              )
            """.trimIndent()
        )
    }
}

internal val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // A goal now owns two new free-text lists, both mirroring
        // implementation_intentions: barriers ("what's getting in the way",
        // captured by the recurring entry that replaces the check-in flow) and
        // progress_notes (gentle "what went well" — never counted or scored).
        // No backfill — these start empty; nothing is dropped.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS barriers (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                text TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_barriers_goalId ON barriers(goalId)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS progress_notes (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                text TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_progress_notes_goalId ON progress_notes(goalId)"
        )
    }
}

internal val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The model consolidates back onto check-ins: a check-in now carries its
        // own type (barrier/progress), note, and implementation intention, and a
        // goal's "current plan" is derived as its most recent check-in's
        // intention. The three short-lived v29/v30 helper tables are therefore
        // redundant and dropped.
        db.execSQL("DROP TABLE IF EXISTS implementation_intentions")
        db.execSQL("DROP TABLE IF EXISTS barriers")
        db.execSQL("DROP TABLE IF EXISTS progress_notes")

        // check_ins is reshaped from the old reflection schema (goalOrChange,
        // madeProgress, avoiding, confidence, competingPriority,
        // implementationIntention, accountability) to the new typed schema
        // (type, note, intention). The new `type` column is NOT NULL with no
        // sensible value for legacy untyped reflection rows, and the model
        // changed fundamentally, so legacy check-in rows are not carried over —
        // the table is recreated empty. Goals and their follow-through state are
        // untouched (follow-through lives on `goals`). Pre-release; acceptable.
        db.execSQL("DROP TABLE IF EXISTS check_ins")
        db.execSQL(
            """
            CREATE TABLE check_ins (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                type TEXT NOT NULL,
                note TEXT NOT NULL,
                intention TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_check_ins_goalId ON check_ins(goalId)")
    }
}

internal val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // A goal gains an optional, local-only "distinctive cue" for its reminder:
        // an emoji, a short vivid label, a copied-local image path, and a
        // notification-sound URI. All nullable — existing goals get NULLs and keep
        // the default reminder presentation. No data is dropped.
        db.execSQL("ALTER TABLE goals ADD COLUMN cueEmoji TEXT")
        db.execSQL("ALTER TABLE goals ADD COLUMN cueLabel TEXT")
        db.execSQL("ALTER TABLE goals ADD COLUMN cueImagePath TEXT")
        db.execSQL("ALTER TABLE goals ADD COLUMN cueSound TEXT")
    }
}

internal val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The intended model: a goal holds MANY plans that coexist. Each plan is an
        // implementation intention + its own cue (emoji/label/image/sound) + its
        // own reminder. The per-goal cue columns and the per-check-in intention
        // move DOWN to the new per-plan `plans` table.

        // 1) New plans table.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS plans (
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
        db.execSQL("CREATE INDEX IF NOT EXISTS index_plans_goalId ON plans(goalId)")

        // 2) Back-fill: each existing goal becomes one "first plan" with a
        //    deterministic id (plan_<goalId>) so the reminder prefs can be re-keyed
        //    in code. Its intention = the goal's most recent non-blank check-in
        //    intention; its cue = the goal's cue columns. Done BEFORE those columns
        //    are dropped below.
        db.execSQL(
            """
            INSERT INTO plans (id, goalId, intention, cueEmoji, cueLabel, cueImagePath, cueSound, createdAt, updatedAt)
            SELECT
                'plan_' || g.id,
                g.id,
                COALESCE((
                    SELECT c.intention FROM check_ins c
                    WHERE c.goalId = g.id AND c.intention != ''
                    ORDER BY c.createdAt DESC LIMIT 1
                ), ''),
                g.cueEmoji, g.cueLabel, g.cueImagePath, g.cueSound,
                g.createdAt, g.updatedAt
            FROM goals g
            """.trimIndent()
        )

        // 3) Recreate goals without the cue columns (they're per-plan now). Foreign
        //    keys are disabled during Room migrations, so recreating this parent
        //    table is safe; check_ins / plans FKs re-target the renamed table.
        db.execSQL(
            """
            CREATE TABLE goals_new (
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
            INSERT INTO goals_new (id, title, accountableTo, createdAt, updatedAt, priority, followedThrough, followedThroughAt)
            SELECT id, title, accountableTo, createdAt, updatedAt, priority, followedThrough, followedThroughAt FROM goals
            """.trimIndent()
        )
        db.execSQL("DROP TABLE goals")
        db.execSQL("ALTER TABLE goals_new RENAME TO goals")

        // 4) Recreate check_ins without the intention column (intentions are on
        //    plans now). Goal-scoped reflection only: type + note.
        db.execSQL(
            """
            CREATE TABLE check_ins_new (
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
        db.execSQL(
            """
            INSERT INTO check_ins_new (id, goalId, type, note, createdAt, updatedAt)
            SELECT id, goalId, type, note, createdAt, updatedAt FROM check_ins
            """.trimIndent()
        )
        db.execSQL("DROP TABLE check_ins")
        db.execSQL("ALTER TABLE check_ins_new RENAME TO check_ins")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_check_ins_goalId ON check_ins(goalId)")
    }
}

internal val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Reverting the v33 split: the implementation intention belongs ON the
        // check-in, not in a separate `plans` table. Each check-in now carries its
        // own intention + cue (emoji/label/image/sound); reminders re-key to the
        // check-in. There is no data to preserve (pre-release, fresh testing), so
        // drop `plans` and recreate `check_ins` empty with the restored shape.
        db.execSQL("DROP TABLE IF EXISTS plans")
        db.execSQL("DROP TABLE IF EXISTS check_ins")
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
        db.execSQL("CREATE INDEX IF NOT EXISTS index_check_ins_goalId ON check_ins(goalId)")
    }
}

internal val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Prototype-alignment foundation (slice 3). Purely ADDITIVE: nothing is
        // dropped. CheckIn, QuestionLabel, and the rest of `goals` are untouched
        // beyond one new nullable-defaulted column, so all existing user data
        // (goals, check-ins, follow-through state, question customizations)
        // survives the upgrade intact. The CheckIn-era model coexists with the new
        // Reminder model until CheckIn is retired in a later slice.

        // 1) Goal gains "why it matters".
        db.execSQL("ALTER TABLE goals ADD COLUMN whyItMatters TEXT NOT NULL DEFAULT ''")

        // 2) Reminder — intention + one cue + schedule, first-class and goal-owned.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS reminders (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                whenMoment TEXT NOT NULL,
                iWill TEXT NOT NULL,
                cueType TEXT NOT NULL,
                cueValue TEXT NOT NULL,
                cueAltText TEXT,
                cueSourcePaletteId TEXT,
                cueIsPaletteDrawn INTEGER NOT NULL,
                scheduleMode TEXT NOT NULL,
                scheduleDays TEXT NOT NULL,
                scheduleTimeLocal TEXT NOT NULL,
                scheduleTimezone TEXT NOT NULL,
                fullTextAlwaysShown INTEGER NOT NULL,
                status TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_goalId ON reminders(goalId)")

        // 3) ReminderEvent — the delivery + response log (done/snoozed/not_today).
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS reminder_events (
                id TEXT NOT NULL PRIMARY KEY,
                reminderId TEXT NOT NULL,
                deliveredAt INTEGER NOT NULL,
                action TEXT NOT NULL,
                actedAt INTEGER NOT NULL,
                undone INTEGER NOT NULL,
                undoReason TEXT,
                reflectionText TEXT,
                FOREIGN KEY (reminderId) REFERENCES reminders(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_reminder_events_reminderId ON reminder_events(reminderId)"
        )

        // 4) Self-knowledge palette (person-level).
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS passions_interests (
                id TEXT NOT NULL PRIMARY KEY,
                label TEXT NOT NULL,
                emoji TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS learnings (
                id TEXT NOT NULL PRIMARY KEY,
                text TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 5) Goal-scoped barriers + progress notes (re-introduced; were dropped at v31).
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS barriers (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                text TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_barriers_goalId ON barriers(goalId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS progress_notes (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                text TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_progress_notes_goalId ON progress_notes(goalId)")
    }
}

internal val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The legacy CheckIn model is fully retired (entity, DAO, screens, and the
        // per-check-in reminder path all removed). The `check_ins` table is no longer
        // a declared entity; it is left dormant on disk rather than dropped so the
        // historical migration tests (which still walk through the check_ins shape on
        // their way to the current version) remain valid. Room ignores undeclared
        // tables. `question_labels` is still declared and untouched.
    }
}

internal val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Intrinsic-motivation goal-design fields — goal-level only, never touch the
        // reminder. Additive; existing goals default to ''.
        db.execSQL("ALTER TABLE goals ADD COLUMN motivationType TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE goals ADD COLUMN wantToFraming TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE goals ADD COLUMN linkedPassionIds TEXT NOT NULL DEFAULT ''")
    }
}

internal val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Learnings move from person-level to goal-scoped (FK to goals, CASCADE),
        // matching barriers / progress notes. Old rows had no goal to attach to, so the
        // table is recreated empty. Column order + FK + index mirror the goal-scoped
        // tables above so Room's schema validation matches.
        db.execSQL("DROP TABLE IF EXISTS learnings")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS learnings (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                text TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_learnings_goalId ON learnings(goalId)")
    }
}

internal val MIGRATION_38_39 = object : Migration(38, 39) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // One-off intentions (ScheduleMode.ONCE): add a nullable target date (yyyy-MM-dd).
        // Additive + nullable, so existing reminders are untouched.
        db.execSQL("ALTER TABLE reminders ADD COLUMN scheduleDate TEXT")
    }
}

internal val MIGRATION_39_40 = object : Migration(39, 40) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Schema cleanup: drop the tables for features removed in the MVP IA replacement —
        // the interests palette, per-goal barriers / progress notes / learnings, and the
        // old check-in question labels. Nothing reads or writes them anymore. The goals,
        // reminders, and reminder_events tables are unchanged.
        db.execSQL("DROP TABLE IF EXISTS passions_interests")
        db.execSQL("DROP TABLE IF EXISTS learnings")
        db.execSQL("DROP TABLE IF EXISTS barriers")
        db.execSQL("DROP TABLE IF EXISTS progress_notes")
        db.execSQL("DROP TABLE IF EXISTS question_labels")
    }
}

internal val MIGRATION_40_41 = object : Migration(40, 41) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Direction check-ins: the periodic "is this direction working?" log for an intention's
        // goal (direction text reuses the existing goals.whyItMatters column). Additive; nothing
        // is dropped. Local-only.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS direction_check_ins (
                id TEXT NOT NULL PRIMARY KEY,
                goalId TEXT NOT NULL,
                askedAt INTEGER NOT NULL,
                answeredAt INTEGER NOT NULL,
                feeling TEXT NOT NULL,
                noteText TEXT,
                FOREIGN KEY (goalId) REFERENCES goals(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_direction_check_ins_goalId ON direction_check_ins(goalId)")
    }
}

@Database(
    entities = [
        Goal::class,
        Reminder::class,
        ReminderEvent::class,
        DirectionCheckIn::class
    ],
    version = 41,
    exportSchema = true
)
abstract class GroundedDatabase : RoomDatabase() {

    abstract fun goalDao(): GoalDao
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderEventDao(): ReminderEventDao
    abstract fun directionCheckInDao(): DirectionCheckInDao

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
                        MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19,
                        MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22,
                        MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25,
                        MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28,
                        MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31,
                        MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34,
                        MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37,
                        MIGRATION_37_38, MIGRATION_38_39, MIGRATION_39_40,
                        MIGRATION_40_41
                    )
                    .build().also { INSTANCE = it }
            }
    }
}
