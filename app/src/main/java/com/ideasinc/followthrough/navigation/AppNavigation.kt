package com.ideasinc.followthrough.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.ui.builder.ReminderBuilderScreen
import com.ideasinc.followthrough.ui.onboarding.OnboardingScreen
import com.ideasinc.followthrough.ui.settings.SettingsScreen
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.today.TodayScreen
import com.ideasinc.followthrough.ui.worked.WhatWorkedScreen

// Primary destinations — the three MVP tabs (Intentions · Progress · Settings). The Progress
// tab keeps the internal route id "what_worked": it still hosts the what's-working cues, now
// led by the streak/stats block (see WhatWorkedScreen). User-facing label is "Progress".
private const val ROUTE_INTENTIONS = "intentions"
private const val ROUTE_WHAT_WORKED = "what_worked"
private const val ROUTE_SETTINGS = "settings"

// Full-screen routes (no bar/rail chrome).
private const val ROUTE_CREATE = "create"
private const val ROUTE_CREATE_EDIT = "create_edit/{reminderId}"
private const val ARG_REMINDER_ID = "reminderId"
// Create a new intention pre-seeded from a learning ("make this an intention"). Query arg so
// free-text (with spaces/slashes) round-trips safely; Navigation decodes it for us.
private const val ROUTE_CREATE_SEEDED = "create_seeded?seed={seed}&dir={dir}"
private const val ARG_SEED = "seed"
// The parent direction a learning came from, so a learning→intention pre-fills it (not blank).
private const val ARG_DIR = "dir"

internal const val PREFS_NAME = "grounded_prefs"
internal const val KEY_ONBOARDING_VERSION = "onboarding_version"
internal const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
internal const val KEY_REMINDERS_PAUSED = "reminders_paused"
// Bumped to 200 for the MVP information-architecture replacement (MVP_User_Flow_IA.md):
// new Intentions / What worked / Settings navigation, the create-cue flow, the in-the-
// moment screen, and the new vocabulary throughout. Re-show the refreshed first-run on the
// next build (no data reset).
// v201: onboarding example slide re-aligned to the new MVP (intention → cue → moment + Did
// it), dropping the retired "goal / make it appealing" framing. Re-show once (no data reset).
// v202: pane-1 headline "Progress, not perfection." → "Remember what you meant to do." (the
// old headline was a progress/streak-era frame the MVP no longer has). Re-show once.
// v203: example pane retitled "Here's an example" → "How it works" and gained a final "You
// did it" beat (tap Did it) so it walks the full loop. Re-show once (no data reset).
// v204: example beats re-aligned to mirror the create-cue flow's step order (what → when →
// cue → Did it), so the first step ("What do you want to remember to do?") isn't a surprise.
internal const val CURRENT_ONBOARDING_VERSION = 204

/** A primary destination, rendered in both the bottom bar and the nav rail. */
private data class PrimaryDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val PRIMARY_DESTINATIONS = listOf(
    PrimaryDestination(ROUTE_INTENTIONS, "Intentions", Icons.Rounded.Bolt),
    PrimaryDestination(ROUTE_WHAT_WORKED, "Progress", Icons.Rounded.Insights),
    PrimaryDestination(ROUTE_SETTINGS, "Settings", Icons.Rounded.Settings)
)

private val PRIMARY_ROUTES = PRIMARY_DESTINATIONS.map { it.route }.toSet()

@Composable
fun AppNavigation(
    container: AppContainer,
    isExpandedWidth: Boolean = false,
    useNavRail: Boolean = false
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // Onboarding is a GATE rendered OUTSIDE the nav graph (not a destination), so Intentions
    // is always the graph's stable start destination and the back stack bottoms out there.
    var showOnboarding by rememberSaveable {
        mutableStateOf(prefs.getInt(KEY_ONBOARDING_VERSION, 0) < CURRENT_ONBOARDING_VERSION)
    }
    // One-shot: onboarding's "Set my first intention" asks the app to open the create-cue
    // flow once the main UI (NavHost) is up.
    var pendingOpenCreate by rememberSaveable { mutableStateOf(false) }

    if (showOnboarding) {
        CenteredPane {
            OnboardingScreen(
                onComplete = {
                    prefs.edit().putInt(KEY_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION).apply()
                    showOnboarding = false
                },
                onCreateFirstReminder = {
                    prefs.edit().putInt(KEY_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION).apply()
                    pendingOpenCreate = true
                    showOnboarding = false
                }
            )
        }
        return
    }

    val navController = rememberNavController()

    // After onboarding's "Set my first intention", open the create-cue flow on top of
    // Intentions (so closing it returns there). `awaitingCreate` covers the one-frame
    // Intentions render during the handoff with the app background for a clean transition.
    var awaitingCreate by remember { mutableStateOf(pendingOpenCreate) }
    LaunchedEffect(Unit) {
        if (pendingOpenCreate) {
            pendingOpenCreate = false
            navController.navigate(ROUTE_CREATE)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showChrome = currentRoute in PRIMARY_ROUTES

    LaunchedEffect(currentRoute) {
        if (currentRoute == ROUTE_CREATE) awaitingCreate = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail && showChrome) {
                AppNavigationRail(currentRoute = currentRoute, onSelect = { navController.navigateToTab(it) })
                VerticalDivider(color = AppColors.Border)
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Scaffold(
                    bottomBar = {
                        if (!useNavRail && showChrome) {
                            AppBottomBar(currentRoute = currentRoute, onSelect = { navController.navigateToTab(it) })
                        }
                    }
                ) { padding ->
                    AppNavHost(
                        navController = navController,
                        container = container,
                        onReplayIntro = {
                            prefs.edit().putInt(KEY_ONBOARDING_VERSION, 0).apply()
                            showOnboarding = true
                        },
                        isExpandedWidth = isExpandedWidth,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
        if (awaitingCreate) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }
    }
}

/**
 * Pop only when there's a screen underneath. Guards against a double-tap (or a stray tap during the
 * close transition, where the outgoing screen is briefly still touchable) popping past the start
 * destination and leaving an empty NavHost — which would render as a blank cream screen.
 */
private fun NavHostController.popBackStackSafely() {
    if (previousBackStackEntry != null) popBackStack()
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar {
        PRIMARY_DESTINATIONS.forEach { dest ->
            NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = { if (currentRoute != dest.route) onSelect(dest.route) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}

@Composable
private fun AppNavigationRail(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationRail {
        PRIMARY_DESTINATIONS.forEach { dest ->
            NavigationRailItem(
                selected = currentRoute == dest.route,
                onClick = { if (currentRoute != dest.route) onSelect(dest.route) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    container: AppContainer,
    onReplayIntro: () -> Unit,
    isExpandedWidth: Boolean,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = ROUTE_INTENTIONS, modifier = modifier) {
        composable(ROUTE_INTENTIONS) {
            if (isExpandedWidth) {
                // Tablet two-pane: intention list (left) ↔ create/edit detail (right).
                // detail: null = nothing selected, "" = new intention, otherwise a reminderId.
                // session bumps on every open so the entry-scoped builder VM is fresh each time
                // (right pane otherwise reuses one stale VM across intentions / after a save).
                var detail by rememberSaveable { mutableStateOf<String?>(null) }
                var session by rememberSaveable { mutableStateOf(0) }
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        TodayScreen(
                            container = container,
                            onNewReminder = { detail = ""; session++ },
                            onEditReminder = { detail = it; session++ },
                            onOpenProgress = { navController.navigateToTab(ROUTE_WHAT_WORKED) },
                            onOpenSettings = { navController.navigateToTab(ROUTE_SETTINGS) }
                        )
                    }
                    VerticalDivider(color = AppColors.Border)
                    Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                        val d = detail
                        if (d == null) {
                            DetailPlaceholder("Select an intention, or tap + to add one.")
                        } else {
                            ReminderBuilderScreen(
                                container = container,
                                reminderId = d.ifEmpty { null },
                                onClose = { detail = null },
                                onSaved = { detail = null },
                                viewModelKey = "builder-intentions-$session"
                            )
                        }
                    }
                }
            } else {
                CenteredPane(maxWidth = 600.dp) {
                    TodayScreen(
                        container = container,
                        onNewReminder = { navController.navigate(ROUTE_CREATE) },
                        onEditReminder = { reminderId -> navController.navigate("create_edit/$reminderId") },
                        onOpenProgress = { navController.navigateToTab(ROUTE_WHAT_WORKED) },
                        onOpenSettings = { navController.navigateToTab(ROUTE_SETTINGS) }
                    )
                }
            }
        }

        composable(ROUTE_WHAT_WORKED) {
            if (isExpandedWidth) {
                // Tablet two-pane: worked-cues list (left) ↔ reuse/refine detail (right).
                // session bumps on every open so the entry-scoped builder VM is fresh each time.
                var detail by rememberSaveable { mutableStateOf<String?>(null) }
                var session by rememberSaveable { mutableStateOf(0) }
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        WhatWorkedScreen(
                            container = container,
                            onReuse = { detail = it; session++ },
                            onMakeIntention = { seed, dir -> navController.navigate("create_seeded?seed=" + Uri.encode(seed) + "&dir=" + Uri.encode(dir)) }
                        )
                    }
                    VerticalDivider(color = AppColors.Border)
                    Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                        val d = detail
                        if (d == null) {
                            DetailPlaceholder("Pick a cue to reuse or refine.")
                        } else {
                            ReminderBuilderScreen(
                                container = container,
                                reminderId = d,
                                onClose = { detail = null },
                                onSaved = { detail = null },
                                viewModelKey = "builder-worked-$session"
                            )
                        }
                    }
                }
            } else {
                CenteredPane(maxWidth = 600.dp) {
                    WhatWorkedScreen(
                        container = container,
                        onReuse = { reminderId -> navController.navigate("create_edit/$reminderId") },
                        onMakeIntention = { seed, dir -> navController.navigate("create_seeded?seed=" + Uri.encode(seed) + "&dir=" + Uri.encode(dir)) }
                    )
                }
            }
        }

        composable(ROUTE_SETTINGS) {
            CenteredPane {
                SettingsScreen(
                    container = container,
                    onReplayIntro = onReplayIntro
                )
            }
        }

        // Create a new intention (the create-cue flow). Lands back on Intentions.
        composable(ROUTE_CREATE) {
            CenteredPane {
                ReminderBuilderScreen(
                    container = container,
                    reminderId = null,
                    onClose = { navController.popBackStackSafely() },
                    onSaved = {
                        navController.navigate(ROUTE_INTENTIONS) {
                            popUpTo(ROUTE_CREATE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        // Create a new intention pre-seeded from a learning. Lands back on Intentions.
        composable(
            route = ROUTE_CREATE_SEEDED,
            arguments = listOf(
                navArgument(ARG_SEED) { type = NavType.StringType; defaultValue = "" },
                navArgument(ARG_DIR) { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val seed = backStackEntry.arguments?.getString(ARG_SEED).orEmpty()
            val dir = backStackEntry.arguments?.getString(ARG_DIR).orEmpty()
            CenteredPane {
                ReminderBuilderScreen(
                    container = container,
                    reminderId = null,
                    seedIWill = seed,
                    seedDirection = dir,
                    onClose = { navController.popBackStackSafely() },
                    onSaved = {
                        navController.navigate(ROUTE_INTENTIONS) {
                            popUpTo(ROUTE_CREATE_SEEDED) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        // Edit an existing intention.
        composable(
            route = ROUTE_CREATE_EDIT,
            arguments = listOf(navArgument(ARG_REMINDER_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getString(ARG_REMINDER_ID) ?: return@composable
            CenteredPane {
                ReminderBuilderScreen(
                    container = container,
                    reminderId = reminderId,
                    onClose = { navController.popBackStackSafely() },
                    onSaved = { navController.popBackStackSafely() }
                )
            }
        }
    }
}

@Composable
private fun DetailPlaceholder(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Caps single-column content at a comfortable reading width and centres it against the
 * cream page. On phones (< 600dp) the cap is a no-op; on tablets it keeps forms and lists
 * from stretching edge-to-edge.
 */
@Composable
private fun CenteredPane(
    maxWidth: Dp = 600.dp,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = maxWidth).fillMaxSize()) {
            content()
        }
    }
}
