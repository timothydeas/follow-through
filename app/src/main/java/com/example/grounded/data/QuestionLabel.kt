package com.example.grounded.data

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
    const val TEMPTATION_AND_SELF_TALK = "temptationAndSelfTalk"
    const val COMPETING_PRIORITY = "competingPriority"
    const val IMPLEMENTATION_INTENTION = "implementationIntention"
    const val ACCOUNTABILITY = "accountability"

    val ALL_KEYS = listOf(
        GOAL_OR_CHANGE,
        MADE_PROGRESS,
        AVOIDING,
        CONFIDENCE,
        TEMPTATION_AND_SELF_TALK,
        COMPETING_PRIORITY,
        IMPLEMENTATION_INTENTION,
        ACCOUNTABILITY
    )

    val DEFAULT_LABELS = mapOf(
        GOAL_OR_CHANGE to "What goal or change are you working toward — or struggling with?",
        MADE_PROGRESS to "Do you feel you've made progress?",
        AVOIDING to "Is there something you already know would help you here, but you've been avoiding finding out or facing?",
        CONFIDENCE to "How confident do you feel you'll figure this out?",
        TEMPTATION_AND_SELF_TALK to "What pulls you away most — and what can you tell yourself in that moment?",
        COMPETING_PRIORITY to "What's getting in the way of this goal right now, and does it matter more than the goal itself?",
        IMPLEMENTATION_INTENTION to "When this moment comes, I will —",
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
