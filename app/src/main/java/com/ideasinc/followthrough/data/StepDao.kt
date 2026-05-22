package com.ideasinc.followthrough.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Query("SELECT * FROM steps WHERE goalId = :goalId ORDER BY createdAt ASC")
    fun getStepsForGoal(goalId: String): Flow<List<Step>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: Step)

    @Update
    suspend fun updateStep(step: Step)

    @Query("DELETE FROM steps WHERE id = :id")
    suspend fun deleteById(id: String)
}
