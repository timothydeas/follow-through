package com.example.grounded.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionLabelDao {
    @Query("SELECT * FROM question_labels")
    fun getAllLabels(): Flow<List<QuestionLabel>>

    @Query("SELECT * FROM question_labels WHERE questionKey = :key LIMIT 1")
    suspend fun getLabelForKey(key: String): QuestionLabel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: QuestionLabel)

    @Query("DELETE FROM question_labels WHERE questionKey = :key")
    suspend fun deleteByKey(key: String)
}
