package com.ideasinc.followthrough.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectionCheckInDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkIn: DirectionCheckIn)

    /** All check-ins, newest first — powers the "due" computation (last answered / dismissed per goal). */
    @Query("SELECT * FROM direction_check_ins ORDER BY answeredAt DESC")
    fun getAll(): Flow<List<DirectionCheckIn>>

    /** Own-words learnings, newest first — the reusable "things you've learned" list. */
    @Query("SELECT * FROM direction_check_ins WHERE noteText IS NOT NULL AND noteText != '' ORDER BY answeredAt DESC")
    fun getLearnings(): Flow<List<DirectionCheckIn>>
}
