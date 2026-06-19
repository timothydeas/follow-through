package com.ideasinc.followthrough.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * The cue type of a reminder's single trigger. Stored as the lowercase string in
 * [Reminder.cueType]. The one-cue principle holds: a reminder carries exactly one
 * cue object (one type + value), never a list.
 *
 * Launch scope (lean-prd Constraints): only [EMOJI] and [PHRASE] are enabled.
 * [PHOTO] and [SOUND] exist in the model as the documented target state but are
 * feature-flagged OFF until the picker/permission crashes are fixed — see
 * [LAUNCH_ENABLED] / [isEnabledAtLaunch].
 */
object CueType {
    const val EMOJI = "emoji"
    const val PHRASE = "phrase"
    const val PHOTO = "photo"
    const val SOUND = "sound"

    /** The cue types a user may choose at launch. Photo/sound stay off. */
    val LAUNCH_ENABLED: List<String> = listOf(EMOJI, PHRASE)

    fun isEnabledAtLaunch(type: String): Boolean = type in LAUNCH_ENABLED
    fun isValid(type: String): Boolean = type in listOf(EMOJI, PHRASE, PHOTO, SOUND)
}

/** How a reminder's schedule repeats. Stored as the lowercase string in [Reminder.scheduleMode]. */
object ScheduleMode {
    const val DAILY = "daily"
    const val WEEKLY = "weekly"
    const val ONCE = "once"

    /**
     * No reminder — the user kept the intention but didn't want a notification (autonomy-first;
     * not everyone wants alarms on their phone). It shows on the Intentions list, "Did it" still
     * works, but nothing is ever scheduled or fired. A reminder can be added later by editing.
     */
    const val NONE = "none"
}

/** Lifecycle of a reminder. Stored as the lowercase string in [Reminder.status]. */
object ReminderStatus {
    const val ACTIVE = "active"
    const val ARCHIVED = "archived"
}

/**
 * Day-of-week tokens. Persisted inside [Reminder.scheduleDays] as a comma-separated
 * string (e.g. "MON,WED,FRI") to keep Room free of TypeConverters.
 */
enum class WeekDay {
    MON, TUE, WED, THU, FRI, SAT, SUN;

    companion object {
        val ALL: List<WeekDay> = listOf(MON, TUE, WED, THU, FRI, SAT, SUN)

        fun parseCsv(csv: String): List<WeekDay> =
            csv.split(",")
                .mapNotNull { token -> entries.firstOrNull { it.name == token.trim() } }

        fun toCsv(days: List<WeekDay>): String = days.joinToString(",") { it.name }
    }
}

/**
 * A reminder — the saved intention + one cue + schedule unit (handoff §3). The
 * spine's destination object: each reminder belongs to a goal, pairs a "When …,
 * I will …" intention with exactly one distinctive cue, and surfaces on a daily
 * or weekly schedule. Delivery + the user's Done/Snooze/Not-today responses are
 * recorded as [ReminderEvent]s; nothing here is mutated by a delivery.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(
        entity = Goal::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Reminder(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val goalId: String,
    // Intention — the When/I-will pairing. Both halves always present in text.
    val whenMoment: String,
    val iWill: String,
    // The single cue.
    val cueType: String,
    val cueValue: String,
    val cueAltText: String? = null,
    val cueSourcePaletteId: String? = null,
    val cueIsPaletteDrawn: Boolean = false,
    // Schedule.
    val scheduleMode: String,
    val scheduleDays: String,        // CSV of WeekDay names; daily => all 7
    val scheduleTimeLocal: String,   // "HH:mm" 24h
    val scheduleTimezone: String,
    val scheduleDate: String? = null, // "yyyy-MM-dd" for a one-off (ScheduleMode.ONCE); null otherwise
    val fullTextAlwaysShown: Boolean = true,
    val status: String = ReminderStatus.ACTIVE,
    val createdAt: Long,
    val updatedAt: Long
) {
    /** The full intention sentence that always travels with the cue. */
    val intentionText: String
        get() = "When ${whenMoment.trim()}, I'll ${iWill.trim()}"

    val days: List<WeekDay> get() = WeekDay.parseCsv(scheduleDays)
}
