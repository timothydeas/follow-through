package com.ideasinc.followthrough.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<GroundedNote>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): GroundedNote?

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteByIdAsFlow(id: String): Flow<GroundedNote?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: GroundedNote)

    @Update
    suspend fun updateNote(note: GroundedNote)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)
}
