package com.ideasinc.followthrough.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_labels")
data class QuestionLabel(
    @PrimaryKey val id: String,
    val questionKey: String,
    val customLabel: String,
    val customPlaceholder: String? = null,
    val isEnabled: Boolean = true
)

object QuestionKeys {
    const val GOAL_OR_CHANGE = "goalOrChange"
    const val AVOIDING = "avoiding"
    const val CONFIDENCE = "confidence"
    const val MADE_PROGRESS = "madeProgress"
    const val COMPETING_PRIORITY = "competingPriority"
    const val IMPLEMENTATION_INTENTION = "implementationIntention"
    const val ACCOUNTABILITY = "accountability"

    val ALL_KEYS = listOf(
        GOAL_OR_CHANGE,
        AVOIDING,
        CONFIDENCE,
        MADE_PROGRESS,
        COMPETING_PRIORITY,
        IMPLEMENTATION_INTENTION,
        ACCOUNTABILITY
    )

    val DEFAULT_LABELS = mapOf(
        GOAL_OR_CHANGE to "What goal or change are you working toward — or struggling with?",
        MADE_PROGRESS to "Do you feel you've made progress?",
        AVOIDING to "Is there something you've been avoiding facing — even though you know it would help?",
        CONFIDENCE to "How confident do you feel you'll figure this out?",
        COMPETING_PRIORITY to "What's getting in your way — the situation itself, or how you're perceiving it?",
        IMPLEMENTATION_INTENTION to "When [e.g., moment or situation], I will [e.g., what I'll do].",
        ACCOUNTABILITY to "Who holds you accountable?"
    )

    // Default placeholder (example-answer) text per question. Mirrors the
    // values the check-in flow used before placeholders became customizable.
    val DEFAULT_PLACEHOLDERS = mapOf(
        GOAL_OR_CHANGE to "e.g., take care of my health",
        AVOIDING to "Something you've been putting off looking at, even though part of you knows it matters",
        CONFIDENCE to "You don't need proof you can do this before you start. What does your gut say?",
        MADE_PROGRESS to "Yes, no, or a quick note.",
        COMPETING_PRIORITY to "Be honest — sometimes our perception or anticipation of a situation matters more than the situation itself. And if nothing is in your way right now, think ahead.",
        IMPLEMENTATION_INTENTION to "I will go for a walk when I finish my morning coffee.",
        ACCOUNTABILITY to "A person, a memory, a strategy — whatever keeps you going"
    )
}

data class QuestionConfig(
    val key: String,
    val label: String,
    val placeholder: String,
    val isEnabled: Boolean
)

fun List<QuestionLabel>.toConfigMap(): Map<String, QuestionLabel> = associateBy { it.questionKey }

fun resolveConfigs(labels: List<QuestionLabel>): List<QuestionConfig> {
    val map = labels.toConfigMap()
    return QuestionKeys.ALL_KEYS.map { key ->
        val label = map[key]
        QuestionConfig(
            key = key,
            label = label?.customLabel ?: QuestionKeys.DEFAULT_LABELS[key] ?: key,
            placeholder = label?.customPlaceholder
                ?: QuestionKeys.DEFAULT_PLACEHOLDERS[key] ?: "",
            isEnabled = label?.isEnabled ?: true
        )
    }
}
