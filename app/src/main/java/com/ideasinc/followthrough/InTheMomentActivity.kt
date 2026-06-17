package com.ideasinc.followthrough

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.ideasinc.followthrough.data.CueType
import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.notifications.EXTRA_REMINDER_ID
import com.ideasinc.followthrough.notifications.ReminderAlarmScheduler
import com.ideasinc.followthrough.notifications.recordReminderEvent
import com.ideasinc.followthrough.notifications.undoReminderEvent
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.GroundedTheme
import kotlinx.coroutines.launch

/**
 * The In-the-moment screen (MVP_User_Flow_IA.md) — the highest-value surface. Reached by
 * tapping the cue-fire notification. A focused full-screen showing the distinctive cue, the
 * full intention text, and the single response: **Did it** (undoable). Nothing else. No
 * response simply means not done — logged neutrally, never held against the user.
 */
class InTheMomentActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID)
        if (reminderId == null) { finish(); return }
        val container = (application as GroundedApplication).container

        setContent {
            GroundedTheme {
                InTheMomentScreen(
                    container = container,
                    reminderId = reminderId,
                    onClose = { finish() },
                    onRemindLater = {
                        // A neutral defer (not a Did-it / Not-done response): re-fire ~1h out.
                        ReminderAlarmScheduler.snooze(applicationContext, reminderId)
                        android.widget.Toast.makeText(applicationContext, "We'll remind you in about an hour.", android.widget.Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun InTheMomentScreen(
    container: AppContainer,
    reminderId: String,
    onClose: () -> Unit,
    onRemindLater: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var reminder by remember { mutableStateOf<Reminder?>(null) }
    var doneEventId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reminderId) {
        val r = container.reminderDao.getReminderById(reminderId)
        if (r == null) onClose() else reminder = r
    }

    val r = reminder ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // A discoverable way to leave WITHOUT responding — no response simply means "not
        // done," logged neutrally. This is navigation (an exit), not a Snooze/Not-yet
        // response, so the single-response design holds. Shown before a response is given.
        if (doneEventId == null) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding()
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (doneEventId == null) {
                // The cue — the hero. Emoji shown large; a phrase shown as a vivid line.
                if (r.cueValue.isNotBlank()) {
                    if (r.cueType == CueType.EMOJI) {
                        Text(r.cueValue, fontSize = 84.sp, textAlign = TextAlign.Center)
                    } else {
                        Text(
                            "“${r.cueValue}”",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AppColors.BrandAccentText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Text(
                    r.intentionText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = {
                        scope.launch {
                            doneEventId = recordReminderEvent(
                                container.reminderEventDao, r.id, EventAction.DONE, System.currentTimeMillis()
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) {
                    Text("Did it", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                }
                // Neutral defer — re-surfaces in about an hour. Understated so "Did it" stays
                // the primary action; not a response (no event is logged).
                TextButton(
                    onClick = onRemindLater,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Remind me later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // Follow-through confirmation — warm, brief, undoable. Gold reads as a
                // celebratory accent on its own surface (AA-safe; gold is never body text on cream).
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.GoldSurface)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        "Followed through. Nice.",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.OnGoldSurface,
                        textAlign = TextAlign.Center
                    )
                }
                OutlinedButton(
                    onClick = {
                        val id = doneEventId
                        scope.launch {
                            if (id != null) undoReminderEvent(container.reminderEventDao, id)
                            onClose()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Undo", color = AppColors.BrandAccentText)
                }
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Done")
                }
            }
        }
    }
}
