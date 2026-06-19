package com.ideasinc.followthrough.notifications

import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.ReminderEvent
import com.ideasinc.followthrough.data.ReminderEventDao
import com.ideasinc.followthrough.data.ReminderDao
import com.ideasinc.followthrough.data.UNDO_REASON_ACCIDENTAL
import java.util.Calendar
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
    // A follow-through is one-per-(intention, day): if this intention already has a non-undone
    // "Did it" logged today, return that event instead of appending a duplicate (the shade action
    // and the in-app card, or a fast double-tap, are otherwise two rows for one opportunity). Undo
    // still works — the caller gets the existing event id back.
    if (action == EventAction.DONE) {
        val dayStart = startOfDayMs(deliveredAt)
        eventDao.findDoneOnDay(reminderId, dayStart, dayStart + DAY_MS)?.let { return it.id }
    }
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

/**
 * Logs a [EventAction.DELIVERED] marker at fire time — a follow-through *opportunity*, not a
 * response. Reconciled against Done rows to detect misses for the forgiving streak; never
 * surfaced in What worked (which filters to Done). Stays on-device (no telemetry, rule #6).
 */
suspend fun recordDelivered(eventDao: ReminderEventDao, reminderId: String, deliveredAt: Long) {
    eventDao.upsert(
        ReminderEvent(
            id = UUID.randomUUID().toString(),
            reminderId = reminderId,
            deliveredAt = deliveredAt,
            action = EventAction.DELIVERED,
            actedAt = 0L,
            undone = false,
            undoReason = null,
            reflectionText = null
        )
    )
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

private const val DAY_MS = 86_400_000L

/** Device-local start-of-day for [ts] — the (intention, day) bucket boundary for idempotency. */
private fun startOfDayMs(ts: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}
