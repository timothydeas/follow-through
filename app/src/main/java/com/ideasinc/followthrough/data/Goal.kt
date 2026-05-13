package com.ideasinc.followthrough.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String,
    val title: String,
    val accountableTo: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val priority: Int? = null,
    val followedThrough: Boolean = false,
    val followedThroughAt: Long? = null
)
