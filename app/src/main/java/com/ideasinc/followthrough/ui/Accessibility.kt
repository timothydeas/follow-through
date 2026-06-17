package com.ideasinc.followthrough.ui

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/**
 * Returns true while ANY accessibility service is active — TalkBack, Switch
 * Access, keyboard/external-navigation services, etc. — not just touch
 * exploration. Used to surface alternatives to gesture-only interactions
 * (e.g. drag-and-drop reordering) for every assistive-tech user.
 */
@Composable
fun rememberIsTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    } ?: return false

    fun anyServiceActive(): Boolean = manager.isEnabled || manager.isTouchExplorationEnabled

    var enabled by remember { mutableStateOf(anyServiceActive()) }
    DisposableEffect(manager) {
        val touchListener = AccessibilityManager.TouchExplorationStateChangeListener {
            enabled = anyServiceActive()
        }
        val stateListener = AccessibilityManager.AccessibilityStateChangeListener {
            enabled = anyServiceActive()
        }
        manager.addTouchExplorationStateChangeListener(touchListener)
        manager.addAccessibilityStateChangeListener(stateListener)
        enabled = anyServiceActive()
        onDispose {
            manager.removeTouchExplorationStateChangeListener(touchListener)
            manager.removeAccessibilityStateChangeListener(stateListener)
        }
    }
    return enabled
}

/**
 * Returns a function that posts a polite announcement to the accessibility service.
 * No-ops on hosts without a window/view.
 */
@Composable
fun rememberA11yAnnouncer(): (String) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { message: String ->
            if (message.isNotBlank()) {
                view.announceForAccessibility(message)
            }
        }
    }
}

/**
 * Up/down arrow controls that supply an accessible alternative to drag-and-drop
 * reordering. Visible only when a touch-exploration accessibility service is on.
 */
@Composable
fun AccessibilityReorderArrows(
    visible: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMoveUp,
            enabled = canMoveUp,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Move goal up",
                tint = if (canMoveUp) LocalContentColor.current
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = onMoveDown,
            enabled = canMoveDown,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Move goal down",
                tint = if (canMoveDown) LocalContentColor.current
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
