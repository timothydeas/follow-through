package com.ideasinc.followthrough.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderEventDao {
    @Query("SELECT * FROM reminder_events ORDER BY deliveredAt DESC")
    fun getAllEvents(): Flow<List<ReminderEvent>>

    @Query("SELECT * FROM reminder_events WHERE undone = 0 ORDER BY deliveredAt DESC")
    fun getLiveEvents(): Flow<List<ReminderEvent>>

    @Query("SELECT * FROM reminder_events WHERE reminderId = :reminderId ORDER BY deliveredAt DESC")
    fun getEventsForReminder(reminderId: String): Flow<List<ReminderEvent>>

    @Query("SELECT * FROM reminder_events WHERE id = :id")
    suspend fun getEventById(id: String): ReminderEvent?

    /**
     * The existing non-undone "Did it" for [reminderId] within [dayStart, dayEnd), or null. Used to
     * keep a follow-through idempotent per (intention, day): the notification "Did it" and the
     * in-app card are two paths to the same opportunity, and a recurring intention stays on the list
     * after one — so without this they'd log two DONE rows for one day and inflate the counts.
     */
    @Query(
        "SELECT * FROM reminder_events WHERE reminderId = :reminderId AND action = 'done' " +
            "AND undone = 0 AND deliveredAt >= :dayStart AND deliveredAt < :dayEnd " +
            "ORDER BY deliveredAt DESC LIMIT 1"
    )
    suspend fun findDoneOnDay(reminderId: String, dayStart: Long, dayEnd: Long): ReminderEvent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: ReminderEvent)

    @Update
    suspend fun update(event: ReminderEvent)

    /** Undo is never a hard delete — flag it and keep the row (handoff §4). */
    @Query("UPDATE reminder_events SET undone = 1, undoReason = :reason WHERE id = :id")
    suspend fun markUndone(id: String, reason: String)

    /** Follow-through opportunities = cue fires logged as 'delivered' markers (not responses). */
    @Query("SELECT COUNT(*) FROM reminder_events WHERE undone = 0 AND action = 'delivered'")
    suspend fun deliveredTotal(): Int

    @Query("SELECT COUNT(*) FROM reminder_events WHERE undone = 0 AND action = 'done'")
    suspend fun doneTotal(): Int
}
