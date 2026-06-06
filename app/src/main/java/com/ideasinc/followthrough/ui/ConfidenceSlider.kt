package com.ideasinc.followthrough.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily
import kotlin.math.roundToInt

/**
 * The confidence rating control. The ported prototype renders "How confident do
 * you feel you'll figure this out?" as a 0–100 slider, not a free-text field.
 *
 * Storage stays a [String] — the `check_ins.confidence` column is TEXT — so no
 * DB migration is needed: a set value is the integer as a decimal string
 * ("0".."100"); an untouched control is the empty string, which the flows turn
 * into `null` via `ifBlank { null }` exactly like a blank text answer used to.
 * Legacy free-text answers written before the slider existed parse to no number,
 * so the thumb rests at the midpoint and the old note is surfaced beneath the
 * track — neither shown as a bogus score nor silently erased when the entry is
 * re-opened for editing (the value only changes once the user moves the slider).
 */
@Composable
fun ConfidenceSlider(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val parsed = value.toIntOrNull()?.coerceIn(0, 100)
    val isSet = parsed != null
    val legacyNote = if (!isSet && value.isNotBlank()) value else null
    // Thumb position: the set score, or the midpoint as a neutral resting point.
    val position = (parsed ?: 50).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        // Fixed-height readout so the row doesn't jump when the first value is
        // set. Until then it reads as a clear prompt rather than a bare dash
        // (which looked like a rendering glitch); the slider carries the value
        // for TalkBack, so this is decorative.
        Box(
            modifier = Modifier
                .height(36.dp)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.CenterStart
        ) {
            if (isSet) {
                Text(
                    text = parsed.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DmSansFontFamily),
                    color = AppColors.BrandAccentText
                )
            } else {
                Text(
                    text = "Not yet rated",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Slider(
            value = position,
            onValueChange = { onValueChange(it.roundToInt().toString()) },
            valueRange = 0f..100f,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (isSet) {
                        "Confidence: $parsed out of 100"
                    } else {
                        "Confidence not set. Slide to rate from 0 to 100."
                    }
                }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Not at all",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Completely",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        legacyNote?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your earlier note: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * How a stored confidence value reads in read-only summaries: a slider score
 * becomes "75 / 100"; a legacy free-text answer passes through unchanged; blank
 * stays blank so callers can skip the field.
 */
fun confidenceDisplay(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val n = raw.toIntOrNull()
    return if (n != null) "$n / 100" else raw
}
