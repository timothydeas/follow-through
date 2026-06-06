package com.ideasinc.followthrough.ui.checkin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ideasinc.followthrough.data.QuestionConfig
import com.ideasinc.followthrough.data.QuestionKeys
import com.ideasinc.followthrough.ui.ConfidenceSlider
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily

// Lead-light reflection building blocks shared by the check-in flow and the
// New Goal flow so both speak the same voice and reuse one implementation
// rather than maintaining parallel widgets.

/**
 * The one text input used everywhere in both flows — a bordered field with a
 * persistent visible border. Routing every input through this guarantees
 * identical keyboard behaviour (the caller's `imePadding()` + verticalScroll is
 * what keeps the focused field above the keyboard) across check-in and goal
 * creation. By default ([compact] = true) the field starts at a single-line
 * height and grows as the user types; the deeper reflection prompts pass
 * `compact = false` for a roomy ~120dp box that invites a longer answer.
 */
@Composable
internal fun ReflectionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    description: String,
    modifier: Modifier = Modifier,
    compact: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = DmSansFontFamily),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (compact) Modifier else Modifier.heightIn(min = 120.dp))
            .semantics { contentDescription = description }
    )
}

/**
 * One reflection question: a heading label plus its input. Confidence renders as
 * the 0–100 [ConfidenceSlider] (its placeholder doubles as the helper line);
 * every other question is a [ReflectionTextField]. The label carries heading
 * semantics and the field a matching content description for screen readers.
 */
@Composable
internal fun QuestionField(
    config: QuestionConfig,
    value: String,
    onValueChange: (String) -> Unit,
    compact: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = config.label,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = DmSansFontFamily),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        if (config.key == QuestionKeys.CONFIDENCE) {
            // Confidence is a 0–100 slider, not a text field (ported prototype).
            // The question's placeholder doubles as the slider's helper line.
            Text(
                text = placeholderFor(config),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ConfidenceSlider(value = value, onValueChange = onValueChange)
        } else {
            ReflectionTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholderFor(config),
                description = config.label,
                compact = compact
            )
        }
    }
}

/**
 * The expand/collapse affordance that gates the optional "deeper" prompts. The
 * label is caller-supplied so the check-in can invite "Reflect more" while goal
 * creation invites "Strengthen your plan", both collapsing to [expandedLabel].
 * Accessible: a ≥48dp button row whose click label tracks the current action.
 */
@Composable
internal fun ReflectMoreToggle(
    expanded: Boolean,
    onToggle: () -> Unit,
    collapsedLabel: String = "Reflect more",
    expandedLabel: String = "Show less"
) {
    val label = if (expanded) expandedLabel else collapsedLabel
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onToggle)
            .heightIn(min = 48.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.BrandAccentText,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = AppColors.BrandAccentText
        )
    }
}
