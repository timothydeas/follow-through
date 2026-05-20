package com.ideasinc.followthrough.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_labels")
data class QuestionLabel(
    @PrimaryKey val id: String,
    val questionKey: String,
    val customLabel: String,
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
        AVOIDING to "Is there something you already know would help you here, but you've been avoiding finding out or facing?",
        CONFIDENCE to "How confident do you feel you'll figure this out?",
        COMPETING_PRIORITY to "What's getting in your way right now — is it the situation itself, or how you're seeing it or expecting it to go? If nothing is, what might get in the way later?",
        IMPLEMENTATION_INTENTION to "I will [e.g., what I'll do] when [e.g., moment or situation] occurs.",
        ACCOUNTABILITY to "Who holds you accountable?"
    )
}

data class QuestionConfig(
    val key: String,
    val label: String,
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
            isEnabled = label?.isEnabled ?: true
        )
    }
}
