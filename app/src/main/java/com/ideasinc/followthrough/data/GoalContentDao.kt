package com.ideasinc.followthrough.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Goal-scoped barriers and progress notes (goal detail). */
@Dao
interface GoalContentDao {
    @Query("SELECT * FROM barriers WHERE goalId = :goalId ORDER BY createdAt ASC")
    fun getBarriers(goalId: String): Flow<List<Barrier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBarrier(barrier: Barrier)

    @Update
    suspend fun updateBarrier(barrier: Barrier)

    @Query("DELETE FROM barriers WHERE id = :id")
    suspend fun deleteBarrier(id: String)

    @Query("SELECT * FROM progress_notes WHERE goalId = :goalId ORDER BY createdAt DESC")
    fun getProgressNotes(goalId: String): Flow<List<ProgressNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgressNote(note: ProgressNote)

    @Query("DELETE FROM progress_notes WHERE id = :id")
    suspend fun deleteProgressNote(id: String)
}
