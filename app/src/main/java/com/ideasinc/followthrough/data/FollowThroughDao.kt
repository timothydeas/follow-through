package com.ideasinc.followthrough.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowThroughDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FollowThroughEntry)

    @Query("SELECT DISTINCT noteId FROM follow_throughs")
    fun getNoteIdsWithFollowThroughs(): Flow<List<String>>
}
