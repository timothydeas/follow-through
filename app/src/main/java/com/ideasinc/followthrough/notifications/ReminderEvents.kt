package com.ideasinc.followthrough.notifications

import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.ReminderEvent
import com.ideasinc.followthrough.data.ReminderEventDao
import com.ideasinc.followthrough.data.ReminderDao
import com.ideasinc.followthrough.data.UNDO_REASON_ACCIDENTAL
import java.util.UUID

/**
 * On-device local_metrics — recomputed from reminders + non-undone events, never
 * transmitted (handoff §4). Powers the user's own progress surfaces.
 */
data class LocalMetrics(
    val remindersCreatedTotal: Int,
    val remindersWithPaletteCue: Int,
    val deliveredTotal: Int,
    val doneTotal: Int
) {
    /** done / delivered, 2dp; 0.0 when nothing delivered. */
    val followThroughRate: Double
        get() = if (deliveredTotal == 0) 0.0 else Math.round(doneTotal * 100.0 / deliveredTotal) / 100.0
}

/**
 * Shared write path for a reminder response (Done / Snooze / Not today), used by
 * both the notification action buttons and the in-app Today card. Appends an event;
 * nothing is ever hard-deleted. Returns the new event id so the caller can offer an
 * 8-second Undo.
 */
suspend fun recordReminderEvent(
    eventDao: ReminderEventDao,
    reminderId: String,
    action: String,
    deliveredAt: Long,
    reflectionText: String? = null
): String {
    val id = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()
    eventDao.upsert(
        ReminderEvent(
            id = id,
            reminderId = reminderId,
            deliveredAt = deliveredAt,
            action = action,
            actedAt = now,
            undone = false,
            undoReason = null,
            reflectionText = reflectionText
        )
    )
    return id
}

/** Undo an action within the 8s window. Flags the event, never deletes it. */
suspend fun undoReminderEvent(eventDao: ReminderEventDao, eventId: String) {
    eventDao.markUndone(eventId, UNDO_REASON_ACCIDENTAL)
}

/** Recomputes the on-device counters from current reminders + live events. */
suspend fun computeLocalMetrics(reminderDao: ReminderDao, eventDao: ReminderEventDao): LocalMetrics =
    LocalMetrics(
        remindersCreatedTotal = reminderDao.countAll(),
        remindersWithPaletteCue = reminderDao.countPaletteDrawn(),
        deliveredTotal = eventDao.deliveredTotal(),
        doneTotal = eventDao.doneTotal()
    )

/** True for a recognised response action. */
internal fun isValidResponse(action: String?): Boolean =
    action == EventAction.DONE || action == EventAction.SNOOZED || action == EventAction.NOT_TODAY
