package com.ideasinc.followthrough.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE status = 'active' ORDER BY scheduleTimeLocal ASC, createdAt DESC")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE goalId = :goalId ORDER BY createdAt DESC")
    fun getRemindersForGoal(goalId: String): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): Reminder?

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getReminderByIdFlow(id: String): Flow<Reminder?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: Reminder)

    @Update
    suspend fun update(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    // local_metrics inputs (recomputed on read; never transmitted).
    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM reminders WHERE cueIsPaletteDrawn = 1")
    suspend fun countPaletteDrawn(): Int
}
