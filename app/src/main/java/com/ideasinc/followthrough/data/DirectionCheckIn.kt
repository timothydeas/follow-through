package com.ideasinc.followthrough.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * A periodic "is this direction working?" check-in for an intention's goal (the direction text
 * lives on [Goal.whyItMatters]). Records the user's felt progress and any own-words learning, so
 * the Progress tab can ask the right thing at the right time and surface past learnings. A
 * learning ([noteText]) can seed a new intention. Local-only — never transmitted (rule #6); a
 * longitudinal signal the future AI can learn from (see future-ai-notes.md).
 */
@Entity(
    tableName = "direction_check_ins",
    foreignKeys = [ForeignKey(
        entity = Goal::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DirectionCheckIn(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val goalId: String,
    val askedAt: Long,
    val answeredAt: Long,
    val feeling: String,
    val noteText: String? = null
)

/**
 * The user's response to a direction check-in. The first three are progress answers: [GETTING_THERE]
 * and [LEARNED] reset the cadence to the longer interval, while [NOT_REALLY] (the direction isn't
 * working) earns a sooner re-check. [DISMISSED] is "ask me later" — not a progress answer, it just
 * snoozes the prompt for the shorter interval.
 */
object DirectionFeeling {
    const val GETTING_THERE = "getting_there"
    const val NOT_REALLY = "not_really"
    const val LEARNED = "learned"
    const val DISMISSED = "dismissed"

    /** Progress answers (exclude the "ask me later" snooze). */
    fun isProgressAnswer(feeling: String): Boolean =
        feeling == GETTING_THERE || feeling == NOT_REALLY || feeling == LEARNED
}
