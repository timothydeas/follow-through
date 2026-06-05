package com.ideasinc.followthrough.ui.goals

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily

// Two-field <-> single-string mapping for a goal's implementation intention. The
// stored sentence is the single source of truth (read by the reminder
// notification and the check-in record); the "When… / I will…" fields are a
// presentation convenience. Shared by the New Goal action-plan step and the Goal
// Detail intention editor so both speak the exact same format.
internal const val WHEN_PREFIX = "When "
internal const val I_WILL_PREFIX = "I will "
internal const val I_WILL_SEP = ", I will "

internal fun joinIntention(whenText: String, actionText: String): String {
    val w = whenText.trim()
    val a = actionText.trim()
    return when {
        w.isEmpty() && a.isEmpty() -> ""
        w.isEmpty() -> "$I_WILL_PREFIX$a"
        a.isEmpty() -> "$WHEN_PREFIX$w"
        else -> "$WHEN_PREFIX$w$I_WILL_SEP$a"
    }
}

internal fun splitIntention(value: String): Pair<String, String> {
    val v = value.trim()
    if (v.isEmpty()) return "" to ""
    val sepIdx = v.indexOf(I_WILL_SEP)
    if (v.startsWith(WHEN_PREFIX) && sepIdx >= 0) {
        val whenPart = v.substring(WHEN_PREFIX.length, sepIdx)
        val actionPart = v.substring(sepIdx + I_WILL_SEP.length)
        return whenPart to actionPart
    }
    if (v.startsWith(WHEN_PREFIX)) return v.substring(WHEN_PREFIX.length) to ""
    if (v.startsWith(I_WILL_PREFIX)) return "" to v.substring(I_WILL_PREFIX.length)
    // Free-text that doesn't fit the template — keep it all in the "When" field
    // so nothing is lost on edit (used by the New Goal step's two-field layout).
    return v to ""
}

/**
 * True when [value] cleanly decomposes into the "When … / I will …" template, so
 * the two-field editor fits. False for legacy/free-form plans, which the editor
 * loads into a single field instead so the text stays fully editable with no
 * data loss.
 */
internal fun isStructuredIntention(value: String): Boolean {
    val v = value.trim()
    return v.startsWith(WHEN_PREFIX) || v.startsWith(I_WILL_PREFIX)
}

/** A single labelled plan input, styled to match the New Goal action-plan step. */
@Composable
internal fun PlanField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontFamily = DmSansFontFamily),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = DmSansFontFamily),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * The shared intention editor body. When [structured] it shows the two
 * "When… / I will…" inputs; otherwise a single field holds the legacy/free-form
 * plan verbatim. The caller owns the state and decides which mode to use (see
 * [isStructuredIntention]).
 */
@Composable
internal fun IntentionEditorFields(
    structured: Boolean,
    whenText: String,
    actionText: String,
    freeText: String,
    onWhenChange: (String) -> Unit,
    onActionChange: (String) -> Unit,
    onFreeTextChange: (String) -> Unit
) {
    if (structured) {
        PlanField(
            label = "When… (the moment or situation)",
            value = whenText,
            placeholder = "e.g., I pour my morning coffee",
            onValueChange = onWhenChange
        )
        Spacer(modifier = Modifier.height(16.dp))
        PlanField(
            label = "I will… (what you'll do)",
            value = actionText,
            placeholder = "e.g., make a breakfast I look forward to",
            onValueChange = onActionChange
        )
    } else {
        PlanField(
            label = "Your plan",
            value = freeText,
            placeholder = "When …, I will …",
            onValueChange = onFreeTextChange
        )
    }
}
