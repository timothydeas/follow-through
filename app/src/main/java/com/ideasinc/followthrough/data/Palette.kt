package com.ideasinc.followthrough.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A passion or interest in the self-knowledge palette ("About You", handoff §3).
 * Lives on the person (there is a single local profile), reusable across goals as
 * the raw material for distinctive cues. An emoji + a short label.
 */
@Entity(tableName = "passions_interests")
data class PassionInterest(
    @PrimaryKey val id: String,
    val label: String,
    val emoji: String,
    val createdAt: Long
)

/**
 * A learning ("what's worked before") in the palette. Short free text, person-level,
 * reusable across goals. Captured as a chip, never an interrogation.
 */
@Entity(tableName = "learnings")
data class Learning(
    @PrimaryKey val id: String,
    val text: String,
    val createdAt: Long
)
