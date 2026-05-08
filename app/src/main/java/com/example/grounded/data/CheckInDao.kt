package com.example.grounded.data

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
}
