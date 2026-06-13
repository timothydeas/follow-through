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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: ReminderEvent)

    @Update
    suspend fun update(event: ReminderEvent)

    /** Undo is never a hard delete — flag it and keep the row (handoff §4). */
    @Query("UPDATE reminder_events SET undone = 1, undoReason = :reason WHERE id = :id")
    suspend fun markUndone(id: String, reason: String)

    @Query("SELECT COUNT(*) FROM reminder_events WHERE undone = 0")
    suspend fun deliveredTotal(): Int

    @Query("SELECT COUNT(*) FROM reminder_events WHERE undone = 0 AND action = 'done'")
    suspend fun doneTotal(): Int
}
