package com.ideasinc.followthrough.data.backup

import com.ideasinc.followthrough.data.CheckIn
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.QuestionLabel
import com.ideasinc.followthrough.notifications.GoalReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Round-trip coverage for [BackupSerializer]. Builds a [BackupData] exercising
 * every nullable column (both present and null), encodes it to JSON, decodes it
 * back, and asserts structural equality — so any field dropped or mistyped in
 * the encode/decode pair fails the test.
 *
 * Runs under Robolectric so the real `org.json` implementation is on the
 * classpath (the stubbed android.jar one returns defaults and would mask bugs).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BackupSerializerTest {

    private fun sampleData(): BackupData = BackupData(
        schemaVersion = BACKUP_SCHEMA_VERSION,
        appVersionName = "1.1",
        appVersionCode = 2,
        dbVersion = BACKUP_DB_VERSION,
        exportedAt = 1_733_000_000_000L,
        goals = listOf(
            // Every optional field populated.
            Goal(
                id = "goal-1",
                title = "Take care of my health",
                accountableTo = "My partner",
                createdAt = 1_700_000_000_000L,
                updatedAt = 1_700_000_500_000L,
                priority = 1,
                followedThrough = true,
                followedThroughAt = 1_700_000_900_000L
            ),
            // Every optional field null/false — exercises the JSON-null path.
            Goal(
                id = "goal-2",
                title = "Be heard at work",
                accountableTo = null,
                createdAt = 1_700_001_000_000L,
                updatedAt = 1_700_001_500_000L,
                priority = null,
                followedThrough = false,
                followedThroughAt = null
            )
        ),
        checkIns = listOf(
            CheckIn(
                id = "ci-1",
                goalId = "goal-1",
                goalOrChange = "Eat a real breakfast",
                madeProgress = "Yes",
                avoiding = null,
                confidence = "70",
                competingPriority = "Running late",
                implementationIntention = "When I pour my coffee, I will make breakfast.",
                accountability = "My partner",
                createdAt = 1_700_002_000_000L,
                updatedAt = 1_700_002_500_000L
            ),
            // All-nullable check-in fields left null.
            CheckIn(
                id = "ci-2",
                goalId = "goal-2",
                goalOrChange = "",
                madeProgress = null,
                avoiding = null,
                confidence = null,
                competingPriority = null,
                implementationIntention = null,
                accountability = null,
                createdAt = 1_700_003_000_000L,
                updatedAt = 1_700_003_500_000L
            )
        ),
        questionLabels = listOf(
            QuestionLabel(
                id = "ql-1",
                questionKey = "confidence",
                customLabel = "How sure are you?",
                customPlaceholder = "Trust your gut",
                isEnabled = true
            ),
            QuestionLabel(
                id = "ql-2",
                questionKey = "accountability",
                customLabel = "Who has your back?",
                customPlaceholder = null,
                isEnabled = false
            )
        ),
        goalReminders = listOf(
            GoalReminderEntry(
                goalId = "goal-1",
                reminder = GoalReminder(
                    enabled = true,
                    hour = 8,
                    minute = 30,
                    days = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY),
                    body = "When I pour my coffee, I will make breakfast."
                )
            ),
            GoalReminderEntry(
                goalId = "goal-2",
                reminder = GoalReminder(
                    enabled = false,
                    hour = 9,
                    minute = 0,
                    days = emptySet(),
                    body = ""
                )
            )
        ),
        settings = BackupSettings(
            globalReminderEnabled = true,
            globalReminderHour = 19,
            globalReminderMinute = 15,
            globalReminderDays = setOf(Calendar.SUNDAY, Calendar.SATURDAY),
            themeMode = "DARK",
            biometricEnabled = true
        )
    )

    @Test
    fun encodeThenDecode_roundTrips() {
        val original = sampleData()
        val decoded = BackupSerializer.decode(BackupSerializer.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun encodeThenDecode_emptyCollections_roundTrip() {
        val original = sampleData().copy(
            goals = emptyList(),
            checkIns = emptyList(),
            questionLabels = emptyList(),
            goalReminders = emptyList()
        )
        val decoded = BackupSerializer.decode(BackupSerializer.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun decode_rejectsNonBackupJson() {
        assertThrows(BackupFormatException::class.java) {
            BackupSerializer.decode("""{"hello":"world"}""")
        }
    }

    @Test
    fun decode_rejectsUnparseableInput() {
        assertThrows(BackupFormatException::class.java) {
            BackupSerializer.decode("not json at all")
        }
    }
}
