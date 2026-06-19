package com.ideasinc.followthrough.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Actions stored as the lowercase string in [ReminderEvent.action]. The user-facing
 * response is only [DONE] ("Did it"); [SNOOZED] / [NOT_TODAY] are retained for data and
 * history compatibility. [DELIVERED] is a non-response marker written when a cue fires,
 * so a follow-through opportunity that was never acted on can be reconciled into a miss
 * for the forgiving streak (CLAUDE.md rule #5). A miss is logged neutrally — never punitive.
 */
object EventAction {
    const val DONE = "done"
    const val SNOOZED = "snoozed"
    const val NOT_TODAY = "not_today"
    /** Written at fire time, not a response. Powers local miss detection; never shown in What worked. */
    const val DELIVERED = "delivered"
}

/** Default reason recorded when a user undoes an action via the 8s snackbar. */
const val UNDO_REASON_ACCIDENTAL = "accidental_tap"

/**
 * One delivery-and-response record (handoff §3/§4). Every Done / Snooze / Not-today
 * appends a row; nothing is ever hard-deleted. Undo sets [undone] = true with an
 * [undoReason] so counts can exclude it while history is preserved. These rows power
 * the Today progress line, the weekly progress indicator (§4a), and local_metrics.
 */
@Entity(
    tableName = "reminder_events",
    foreignKeys = [ForeignKey(
        entity = Reminder::class,
        parentColumns = ["id"],
        childColumns = ["reminderId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ReminderEvent(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val reminderId: String,
    val deliveredAt: Long,
    val action: String,
    val actedAt: Long,
    val undone: Boolean = false,
    val undoReason: String? = null,
    val reflectionText: String? = null
)
