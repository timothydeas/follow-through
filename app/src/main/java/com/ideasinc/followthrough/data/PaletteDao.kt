package com.ideasinc.followthrough.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** The self-knowledge palette: person-level passions/interests and learnings. */
@Dao
interface PaletteDao {
    @Query("SELECT * FROM passions_interests ORDER BY createdAt ASC")
    fun getPassions(): Flow<List<PassionInterest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPassion(passion: PassionInterest)

    @Update
    suspend fun updatePassion(passion: PassionInterest)

    @Query("DELETE FROM passions_interests WHERE id = :id")
    suspend fun deletePassion(id: String)

    @Query("SELECT * FROM learnings ORDER BY createdAt ASC")
    fun getLearnings(): Flow<List<Learning>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearning(learning: Learning)

    @Update
    suspend fun updateLearning(learning: Learning)

    @Query("DELETE FROM learnings WHERE id = :id")
    suspend fun deleteLearning(id: String)
}
