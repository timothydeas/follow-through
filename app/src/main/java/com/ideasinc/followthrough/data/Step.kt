package com.ideasinc.followthrough.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * A sub-goal / step belonging to a [Goal]. Deleting the parent goal cascades
 * to its steps. Steps are ordered by [createdAt] so they read in the order the
 * user added them.
 */
@Entity(
    tableName = "steps",
    foreignKeys = [ForeignKey(
        entity = Goal::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Step(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val goalId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
