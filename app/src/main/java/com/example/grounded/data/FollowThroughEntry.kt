package com.example.grounded.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "follow_throughs",
    foreignKeys = [ForeignKey(
        entity = GroundedNote::class,
        parentColumns = ["id"],
        childColumns = ["noteId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class FollowThroughEntry(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val noteId: String,
    val body: String,
    val createdAt: Long
)
