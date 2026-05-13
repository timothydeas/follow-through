package com.ideasinc.followthrough.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NoteType {
    REFLECTION,
    FOLLOW_THROUGH,
    FOLLOWED_THROUGH
}

@Entity(tableName = "notes")
data class GroundedNote(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val tag: String?,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val type: NoteType = NoteType.REFLECTION,
    val isDraft: Boolean = false,
    val reflection: String = "",
    val whatStoppedYou: String = "",
    val whatYouLearned: String = "",
    val nextSteps: String = "",
    val whenField: String = "",
    val willField: String = "",
    val followedThrough: Boolean = false,
    val followedThroughAt: Long? = null,
    val implementationIntention: String = ""
)
