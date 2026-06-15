package com.ideasinc.followthrough.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A goal. Its intentions, cues, and schedules live on its [Reminder]s (a goal has
 * many); its barriers and progress notes live on [Barrier]/[ProgressNote]. The goal
 * itself carries its identity, "why it matters", ordering, and follow-through state.
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
    val whyItMatters: String = "",
    /**
     * Intrinsic motivation lives on the GOAL (never the reminder/cue/intention). Whether
     * the user wants to do this or feels they have to — see [MotivationType]. Used to
     * harness the want-to (a have-to is reframed, not dismissed) and to lightly prioritize
     * want-to goals. Empty = unset.
     */
    val motivationType: String = "",
    /**
     * The user's own words for the version of this they'd actually enjoy / want — the
     * "want-to" angle, captured at goal creation and shown on goal detail. NEVER seeded
     * into the implementation intention or cue (that was the reverted `enjoyableWay`
     * mistake — intrinsic motivation is goal-design, not a reminder input).
     */
    val wantToFraming: String = "",
    /**
     * Comma-separated [PassionInterest] ids this goal draws on — the About You palette
     * connection ("what you love" feeding the goal's want-to). Goal-level; resolved to
     * emoji + label for display. CSV is safe because ids are UUIDs (no commas).
     */
    val linkedPassionIds: String = ""
)

/** Whether a goal is intrinsically wanted or felt as an obligation. Stored on [Goal.motivationType]. */
object MotivationType {
    const val WANT_TO = "want_to"
    const val HAVE_TO = "have_to"
}
