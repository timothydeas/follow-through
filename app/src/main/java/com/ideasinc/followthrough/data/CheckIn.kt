package com.ideasinc.followthrough.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** The two kinds of check-in. Stored as the lowercase string in [CheckIn.type]. */
object CheckInType {
    const val BARRIER = "barrier"
    const val PROGRESS = "progress"

    fun isValid(value: String): Boolean = value == BARRIER || value == PROGRESS
}

/**
 * A single check-in — the unit everything hangs off of. Each carries its type
 * (barrier/progress), a free-text note, an implementation intention ("when [cue],
 * I will [action]"), and its OWN distinctive cue (emoji, label, local image,
 * sound) and reminder (the reminder schedule lives in SharedPreferences keyed by
 * this check-in's id). Intention, cue, and reminder are all optional per check-in.
 *
 * A goal has many check-ins, all coexisting; each is openable and editable, and
 * each reminder fires with that check-in's own cue + intention.
 */
@Entity(
    tableName = "check_ins",
    foreignKeys = [ForeignKey(
        entity = Goal::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class CheckIn(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val goalId: String,
    val type: String,
    val note: String,
    val intention: String = "",
    val cueEmoji: String? = null,
    val cueLabel: String? = null,
    val cueImagePath: String? = null,
    val cueSound: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
