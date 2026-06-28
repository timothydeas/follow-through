package com.ideasinc.followthrough

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.semantics
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
class InTheMomentActivity : FragmentActivity() {

    private var authCleared by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // With the app lock on, keep the cue + intention out of screenshots and the recents preview.
        if (AppLock.isEnabled(this)) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID)
        if (reminderId == null) { finish(); return }
        val container = (application as GroundedApplication).container

        // The same lock that guards MainActivity must guard this surface too — it's reached by
        // tapping a notification, which would otherwise reveal the most sensitive content (the cue
        // and full intention) with no authentication. shouldGate() is false when no credential is
        // enrolled, so a lock-on-but-credential-removed user is never locked out.
        val alreadyAuthed = savedInstanceState?.getBoolean(KEY_AUTH_DONE, false) == true
        authCleared = alreadyAuthed || !AppLock.shouldGate(this)

        // Back / gesture-back from the in-the-moment screen must land in the app, not the launcher
        // (same reason as closeAndReturnHome below).
        onBackPressedDispatcher.addCallback(this) { closeAndReturnHome() }

        setContent {
            GroundedTheme {
                if (authCleared) {
                    InTheMomentScreen(
                        container = container,
                        reminderId = reminderId,
                        onClose = { closeAndReturnHome() }
                    )
                }
            }
        }

        if (!authCleared) {
            AppLock.prompt(this) { ok -> if (ok) authCleared = true else finish() }
        }
    }

    /**
     * Leave the in-the-moment screen and make sure the user lands back in the app — not on the
     * device launcher. The cue notification launches this activity with FLAG_ACTIVITY_NEW_TASK, so
     * when the app wasn't already open this activity is the sole member of a fresh task: a bare
     * finish() would empty that task and drop the user to the home screen (the reported bug). Only
     * in that case (isTaskRoot) do we first bring up MainActivity (the Intentions home). When the
     * app was already open, this activity is stacked on the existing task (isTaskRoot is false) and
     * a plain finish() returns the user exactly where they were — behavior unchanged.
     */
    private fun closeAndReturnHome() {
        if (isTaskRoot) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_AUTH_DONE, authCleared)
    }

    companion object {
        private const val KEY_AUTH_DONE = "auth_done"
    }
}

@Composable
private fun InTheMomentScreen(
    container: AppContainer,
    reminderId: String,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var reminder by remember { mutableStateOf<Reminder?>(null) }
    var doneEventId by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

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
                        // Guard the in-flight gap so a fast double-tap can't fire two writes.
                        if (submitting) return@Button
                        submitting = true
                        scope.launch {
                            doneEventId = recordReminderEvent(
                                container.reminderEventDao, r.id, EventAction.DONE, System.currentTimeMillis()
                            )
                        }
                    },
                    enabled = !submitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) {
                    Text("Did it", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                }
            } else {
                // Follow-through confirmation — warm, brief, undoable. A centered celebration card
                // in the app's own card language (gold surface, hairline border, rounded 20dp, a
                // rounded check, proportional type) so it reads as part of the app, not a bare
                // banner. Gold is a celebratory accent on its own surface (AA-safe; gold is never
                // body text on cream). Merged semantics so TalkBack reads it as one announcement.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppColors.GoldSurface)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                        .semantics(mergeDescendants = true) {},
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Decorative — the text below conveys the result; null keeps TalkBack from
                    // announcing the icon separately.
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = AppColors.OnGoldSurface,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        "Followed through",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.OnGoldSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Nice.",
                        style = MaterialTheme.typography.bodyLarge,
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
