package com.ideasinc.followthrough.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * A barrier for a goal ("what's getting in the way", handoff §3 / goal detail).
 * Goal-scoped chips; feed the builder's "draw from yourself" step. Never scored.
 */
@Entity(
    tableName = "barriers",
    foreignKeys = [ForeignKey(
        entity = Goal::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Barrier(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val goalId: String,
    val text: String,
    val createdAt: Long
)

/**
 * A dated progress note for a goal (handoff §3). Gentle "what went well / what
 * happened" — never counted, never a streak. One-line entries on goal detail.
 */
@Entity(
    tableName = "progress_notes",
    foreignKeys = [ForeignKey(
        entity = Goal::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ProgressNote(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val goalId: String,
    val text: String,
    val createdAt: Long
)
