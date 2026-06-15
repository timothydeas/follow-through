package com.ideasinc.followthrough.navigation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.ui.aboutyou.AboutYouScreen
import com.ideasinc.followthrough.ui.builder.ReminderBuilderScreen
import com.ideasinc.followthrough.ui.goals.GoalDetailScreen
import com.ideasinc.followthrough.ui.goals.GoalDetailViewModel
import com.ideasinc.followthrough.ui.goals.NewGoalFlowScreen
import com.ideasinc.followthrough.ui.goals.NewGoalFlowViewModel
import com.ideasinc.followthrough.ui.list.ListScreen
import com.ideasinc.followthrough.ui.list.ListViewModel
import com.ideasinc.followthrough.ui.onboarding.OnboardingScreen
import com.ideasinc.followthrough.ui.settings.SettingsScreen
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.today.TodayScreen

// Primary destinations — the four spine tabs (Today · Goals · About You · Settings).
private const val ROUTE_TODAY = "today"
private const val ROUTE_GOALS = "goals"
private const val ROUTE_ABOUT_YOU = "about_you"
private const val ROUTE_SETTINGS = "settings"

// Full-screen routes reached from a tab (no bar/rail chrome).
private const val ROUTE_NEW_GOAL = "new_goal"
private const val ROUTE_GOAL_DETAIL = "goal_detail/{goalId}"
private const val ROUTE_BUILDER = "builder"
private const val ROUTE_BUILDER_NEW = "builder_new/{goalId}"
private const val ROUTE_BUILDER_EDIT = "builder_edit/{reminderId}"
private const val ARG_GOAL_ID = "goalId"
private const val ARG_REMINDER_ID = "reminderId"

internal const val PREFS_NAME = "grounded_prefs"
internal const val KEY_ONBOARDING_VERSION = "onboarding_version"
internal const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
// Bumped 110 → 111 for the prototype-alignment navigation spine (four primary
// destinations Today · Goals · About You · Settings with an adaptive bottom bar /
// nav rail), so the full first-run re-shows on the next test build (no data reset).
// Bumped 111 → 112 for the rewritten 3-pane Welcome (handoff §8 verbatim copy,
// "Progress, not perfection." → "Create my first reminder"), so the refreshed
// onboarding re-shows once (no data reset).
// Bumped 112 → 113 for the full CheckIn retirement: goal creation now flows into
// the Reminder Builder (no legacy check-in flow), Stats/FollowThrus folded away,
// DB v36. Re-show the full first-run on the next test build (no data reset).
// Bumped 113 → 114 for the device-test polish: LaunchInsight + Science removed,
// onboarding tagline dropped + step-2 restyle + button-layout fix, Goals card
// redesign. Re-show the full first-run on the next test build (no data reset).
internal const val CURRENT_ONBOARDING_VERSION = 114

/** A primary spine destination, rendered in both the bottom bar and the nav rail. */
private data class PrimaryDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val PRIMARY_DESTINATIONS = listOf(
    PrimaryDestination(ROUTE_TODAY, "Today", Icons.Outlined.Home),
    PrimaryDestination(ROUTE_GOALS, "Goals", Icons.Outlined.Flag),
    PrimaryDestination(ROUTE_ABOUT_YOU, "About You", Icons.Outlined.Person),
    PrimaryDestination(ROUTE_SETTINGS, "Settings", Icons.Outlined.Settings)
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

    // Onboarding is a GATE rendered OUTSIDE the nav graph (not a destination), so Today
    // is always the graph's stable start destination. Previously onboarding WAS the start
    // destination and got popped on completion, which left tab-switching and system-back
    // popping toward a destination no longer on the back stack — and the stack could empty
    // to a blank screen. As a gate, the back stack always bottoms out at Today, and back
    // from Today exits the app cleanly.
    var showOnboarding by rememberSaveable {
        mutableStateOf(prefs.getInt(KEY_ONBOARDING_VERSION, 0) < CURRENT_ONBOARDING_VERSION)
    }
    // One-shot: onboarding's "Create my first reminder" asks the app to open the builder
    // once the main UI (NavHost) is up.
    var pendingOpenBuilder by rememberSaveable { mutableStateOf(false) }

    if (showOnboarding) {
        CenteredPane {
            OnboardingScreen(
                onComplete = {
                    prefs.edit().putInt(KEY_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION).apply()
                    showOnboarding = false
                },
                onCreateFirstReminder = {
                    prefs.edit().putInt(KEY_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION).apply()
                    pendingOpenBuilder = true
                    showOnboarding = false
                }
            )
        }
        return
    }

    val navController = rememberNavController()
    val listViewModel: ListViewModel = viewModel(
        factory = ListViewModel.Factory(container.goalDao, container.reminderDao)
    )

    // After onboarding's "Create my first reminder", open the builder on top of Today
    // (so closing the first reminder returns to Today). `awaitingBuilder` covers the
    // one-frame Today render during this handoff with the app background, so the user
    // sees a clean onboarding→builder transition instead of a flash of Today. Captured
    // synchronously so the cover is up on the very first frame; latches off once the
    // builder is on screen.
    var awaitingBuilder by remember { mutableStateOf(pendingOpenBuilder) }
    LaunchedEffect(Unit) {
        if (pendingOpenBuilder) {
            pendingOpenBuilder = false
            navController.navigate(ROUTE_BUILDER)
        }
    }

    // Chrome (bottom bar / rail) shows only on the four primary destinations; every
    // full-screen route (builder, goal detail) hides it.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showChrome = currentRoute in PRIMARY_ROUTES

    LaunchedEffect(currentRoute) {
        if (currentRoute == ROUTE_BUILDER) awaitingBuilder = false
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
                        listViewModel = listViewModel,
                        onReplayIntro = {
                            // Re-show the Welcome gate; reset the version so a normal relaunch
                            // also re-shows until completed. No user data is touched.
                            prefs.edit().putInt(KEY_ONBOARDING_VERSION, 0).apply()
                            showOnboarding = true
                        },
                        isExpandedWidth = isExpandedWidth,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
        if (awaitingBuilder) {
            // Same-color cover over the one-frame Today render during the handoff.
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        // Standard bottom-nav behavior: pop to start saving state, single instance,
        // restore the tab's prior state when returning.
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
    listViewModel: ListViewModel,
    onReplayIntro: () -> Unit,
    isExpandedWidth: Boolean,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = ROUTE_TODAY, modifier = modifier) {
        composable(ROUTE_TODAY) {
            CenteredPane {
                TodayScreen(
                    container = container,
                    onNewReminder = { navController.navigate(ROUTE_BUILDER) }
                )
            }
        }

        composable(ROUTE_BUILDER) {
            CenteredPane {
                ReminderBuilderScreen(
                    container = container,
                    goalId = null,
                    reminderId = null,
                    onClose = { navController.popBackStack() },
                    onSaved = { savedGoalId ->
                        navController.navigate("goal_detail/$savedGoalId") {
                            popUpTo(ROUTE_BUILDER) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(ROUTE_GOALS) {
            CenteredPane(maxWidth = if (isExpandedWidth) 900.dp else 600.dp) {
                ListScreen(
                    viewModel = listViewModel,
                    onGoalClick = { id -> navController.navigate("goal_detail/$id") },
                    onNewGoal = { navController.navigate(ROUTE_NEW_GOAL) },
                    isExpandedWidth = isExpandedWidth
                )
            }
        }

        composable(ROUTE_ABOUT_YOU) {
            CenteredPane { AboutYouScreen(container = container) }
        }

        composable(ROUTE_SETTINGS) {
            CenteredPane {
                SettingsScreen(
                    onBack = { navController.navigateToTab(ROUTE_TODAY) },
                    onReplayIntro = onReplayIntro
                )
            }
        }

        composable(ROUTE_NEW_GOAL) {
            val vm: NewGoalFlowViewModel = viewModel(
                factory = NewGoalFlowViewModel.Factory(container.goalDao)
            )
            CenteredPane {
                NewGoalFlowScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onGoalCreated = { goalId ->
                        // Creation flows straight into the new reminder builder for
                        // this goal.
                        navController.navigate("builder_new/$goalId") {
                            popUpTo(ROUTE_NEW_GOAL) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(
            route = ROUTE_GOAL_DETAIL,
            arguments = listOf(navArgument(ARG_GOAL_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString(ARG_GOAL_ID) ?: return@composable
            val vm: GoalDetailViewModel = viewModel(
                factory = GoalDetailViewModel.Factory(
                    container.goalDao, container.goalContentDao, container.reminderDao, goalId
                )
            )
            CenteredPane {
                GoalDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onNavigateToList = {
                        navController.navigate(ROUTE_GOALS) {
                            popUpTo(ROUTE_GOALS) { inclusive = true }
                        }
                    },
                    onAddReminder = { navController.navigate("builder_new/$goalId") },
                    onOpenReminder = { reminderId -> navController.navigate("builder_edit/$reminderId") }
                )
            }
        }

        composable(
            route = ROUTE_BUILDER_NEW,
            arguments = listOf(navArgument(ARG_GOAL_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString(ARG_GOAL_ID) ?: return@composable
            CenteredPane {
                ReminderBuilderScreen(
                    container = container,
                    goalId = goalId,
                    reminderId = null,
                    onClose = { navController.popBackStack() },
                    onSaved = { savedGoalId ->
                        navController.navigate("goal_detail/$savedGoalId") {
                            popUpTo(ROUTE_BUILDER_NEW) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(
            route = ROUTE_BUILDER_EDIT,
            arguments = listOf(navArgument(ARG_REMINDER_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getString(ARG_REMINDER_ID) ?: return@composable
            CenteredPane {
                ReminderBuilderScreen(
                    container = container,
                    goalId = null,
                    reminderId = reminderId,
                    onClose = { navController.popBackStack() },
                    onSaved = { savedGoalId ->
                        navController.navigate("goal_detail/$savedGoalId") {
                            popUpTo(ROUTE_BUILDER_EDIT) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

    }
}

/**
 * Caps single-column content at a comfortable reading width and centres it against
 * the cream page. On phones (< 600dp) the cap is a no-op; on tablets it keeps forms
 * and lists from stretching edge-to-edge.
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

