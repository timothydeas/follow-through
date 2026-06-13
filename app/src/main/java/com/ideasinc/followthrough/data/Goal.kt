package com.ideasinc.followthrough.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A goal. Its implementation intentions, cues, and reminders live on its
 * [CheckIn]s (a goal has many), so the goal itself carries only its identity,
 * ordering, and follow-through state.
 */
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String,
    val title: String,
    val accountableTo: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val priority: Int? = null,
    val followedThrough: Boolean = false,
    val followedThroughAt: Long? = null,
    /** "Why this matters" — the reason the goal exists. Shown on goal detail. */
    val whyItMatters: String = ""
)
