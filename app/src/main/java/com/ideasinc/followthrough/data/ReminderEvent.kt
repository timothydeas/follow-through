package com.ideasinc.followthrough.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * The three forgiving responses to a delivered reminder. Stored as the lowercase
 * string in [ReminderEvent.action]. "Not today" is never punitive.
 */
object EventAction {
    const val DONE = "done"
    const val SNOOZED = "snoozed"
    const val NOT_TODAY = "not_today"
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
