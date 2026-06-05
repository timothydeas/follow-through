package com.ideasinc.followthrough.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins ORDER BY createdAt DESC")
    fun getAllCheckIns(): Flow<List<CheckIn>>

    @Query("SELECT * FROM check_ins WHERE goalId = :goalId ORDER BY createdAt DESC")
    fun getCheckInsForGoal(goalId: String): Flow<List<CheckIn>>

    @Query("SELECT * FROM check_ins WHERE id = :id")
    suspend fun getCheckInById(id: String): CheckIn?

    @Query("SELECT * FROM check_ins WHERE id = :id")
    fun getCheckInByIdAsFlow(id: String): Flow<CheckIn?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: CheckIn)

    @Query("DELETE FROM check_ins WHERE id = :id")
    suspend fun deleteById(id: String)

    // Wipes every check-in. Used when a data import replaces all existing data
    // (goals cascade-delete their check-ins, but this is called explicitly first
    // so the clear is independent of foreign-key pragma state).
    @Query("DELETE FROM check_ins")
    suspend fun deleteAll()
}
