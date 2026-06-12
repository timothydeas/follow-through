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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.ui.checkin.CheckInEditorScreen
import com.ideasinc.followthrough.ui.checkin.CheckInEditorViewModel
import com.ideasinc.followthrough.ui.checkin.CheckInFlowScreen
import com.ideasinc.followthrough.ui.checkin.CheckInFlowViewModel
import com.ideasinc.followthrough.ui.followthrough.FollowThrusScreen
import com.ideasinc.followthrough.ui.goals.GoalDetailScreen
import com.ideasinc.followthrough.ui.goals.GoalDetailViewModel
import com.ideasinc.followthrough.ui.goals.NewGoalFlowScreen
import com.ideasinc.followthrough.ui.goals.NewGoalFlowViewModel
import com.ideasinc.followthrough.ui.launch.LaunchInsightScreen
import com.ideasinc.followthrough.ui.launch.pickLaunchInsight
import com.ideasinc.followthrough.ui.list.ListScreen
import com.ideasinc.followthrough.ui.list.ListViewModel
import com.ideasinc.followthrough.ui.onboarding.OnboardingScreen
import com.ideasinc.followthrough.ui.settings.ScienceScreen
import com.ideasinc.followthrough.ui.settings.SettingsScreen
import com.ideasinc.followthrough.ui.stats.StatsScreen
import com.ideasinc.followthrough.ui.theme.AppColors

private const val ROUTE_LIST = "list"
private const val ROUTE_LAUNCH_INSIGHT = "launch_insight"
private const val ROUTE_NEW_GOAL = "new_goal"
private const val ROUTE_GOAL_DETAIL = "goal_detail/{goalId}"
private const val ROUTE_CHECKIN_FLOW = "checkin_flow/{goalId}"
private const val ROUTE_CHECKIN_EDITOR = "checkin_editor/{checkInId}"
private const val ARG_GOAL_ID = "goalId"
private const val ARG_CHECKIN_ID = "checkInId"
private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_SCIENCE = "science"
private const val ROUTE_STATS = "stats"
private const val ROUTE_FOLLOWTHRUS = "followthrus"

internal const val PREFS_NAME = "grounded_prefs"
internal const val KEY_ONBOARDING_VERSION = "onboarding_version"
internal const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
// Bumped 93 → 94 for the MVP onboarding redesign so existing closed-testing
// users re-see onboarding once after upgrading in place (no data is reset).
// Bumped 94 → 95 so the refreshed "See an example" (health example) re-shows
// once for already-onboarded installs after this upgrade (no data is reset).
// Bumped 95 → 96 for the coral/cream UI port (redesigned onboarding) so it
// re-shows once for already-onboarded installs after this upgrade (no reset).
// Bumped 96 → 97 to re-show onboarding once for already-onboarded installs
// after this upgrade (no data is reset).
// Bumped 97 → 98 for the reworded slide 1 ("Made for the moment.") so the
// refreshed copy re-shows once for already-onboarded installs (no data reset).
// Bumped 99 → 100 to re-show onboarding once for a demo (no data reset).
// Bumped 100 → 101 to re-show onboarding once (no data reset).
// Bumped 101 → 102 to re-show onboarding once for onboarding screenshots (no data reset).
// Bumped 102 → 103 to re-show the full first-run for retesting the barriers/intentions/
// progress restructure on-device (no data reset).
// Bumped 103 → 104 to re-show the full first-run for retesting the check-in–centered
// rework (creation → first check-in, typed check-ins, Stats restored) (no data reset).
// Bumped 104 → 105 to re-show the full first-run for retesting distinctive reminder
// cues (emoji/label/image/sound per goal) (no data reset).
// Bumped 105 → 106 to re-show the full first-run for retesting per-plan model
// (multiple plans per goal, each with its own cue + reminder) (no data reset).
// Bumped 106 → 107 for the reverted per-check-in model + rewritten "How it works"
// cards / example, so the refreshed onboarding re-shows (no data reset).
// Bumped 107 → 108 for the test build adding the reminder+cue step to the check-in
// flow and the check-in streak headline (no data reset).
// Bumped 108 → 109 for the single-select cue rework + fixed cue controls and
// reminder deep-link (no data reset).
// Bumped 109 → 110 for the test build that removes cue authoring; reminders now
// show only the implementation intention (no data reset).
internal const val CURRENT_ONBOARDING_VERSION = 110

@Composable
fun AppNavigation(
    container: AppContainer,
    pendingCheckInId: String? = null,
    onCheckInConsumed: () -> Unit = {},
    isExpandedWidth: Boolean = false
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    // The goal selected in the two-pane (expanded-width) Home. Hoisted here so it
    // survives while modal flows (new goal, check-in) cover the NavHost and is
    // restored after process death. Ignored on compact widths, where Home is a
    // single pane and the goal opens as its own full-screen destination.
    var selectedGoalId by rememberSaveable { mutableStateOf<String?>(null) }

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val savedVersion = prefs.getInt(KEY_ONBOARDING_VERSION, 0)
    val startDestination = if (savedVersion >= CURRENT_ONBOARDING_VERSION) ROUTE_LAUNCH_INSIGHT else ROUTE_ONBOARDING

    val listViewModel: ListViewModel = viewModel(
        factory = ListViewModel.Factory(container.goalDao, container.checkInDao)
    )

    // A tapped reminder deep-links to THAT specific check-in's editor (showing its
    // implementation intention), not a new check-in. Reset the back stack to Home
    // and push the editor on top, so Back returns to Home rather than dropping out
    // of the app. If the check-in was deleted, just land on Home. (The launch-
    // insight flash is suppressed by LaunchInsightGate.)
    LaunchedEffect(pendingCheckInId) {
        val checkInId = pendingCheckInId ?: return@LaunchedEffect
        val exists = container.checkInDao.getCheckInById(checkInId) != null
        navController.navigate(ROUTE_LIST) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
        if (exists) {
            navController.navigate("checkin_editor/$checkInId")
        }
        onCheckInConsumed()
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_ONBOARDING) {
            CenteredPane {
                OnboardingScreen(
                    onBiometricPersist = { biometricEnabled ->
                        prefs.edit()
                            .putBoolean(KEY_BIOMETRIC_ENABLED, biometricEnabled)
                            .apply()
                    },
                    onComplete = {
                        prefs.edit()
                            .putInt(KEY_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION)
                            .apply()
                        navController.navigate(ROUTE_LAUNCH_INSIGHT) {
                            popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(ROUTE_LAUNCH_INSIGHT) {
            // Picked once per destination instance; pickLaunchInsight also
            // persists the chosen index so the next launch avoids it.
            val insight = remember { pickLaunchInsight(prefs) }
            CenteredPane {
                LaunchInsightScreen(
                    text = insight,
                    onDismiss = {
                        navController.navigate(ROUTE_LIST) {
                            popUpTo(ROUTE_LAUNCH_INSIGHT) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(ROUTE_LIST) {
            if (isExpandedWidth) {
                TwoPaneHome(
                    container = container,
                    listViewModel = listViewModel,
                    selectedGoalId = selectedGoalId,
                    onSelectGoal = { selectedGoalId = it },
                    onNewGoal = { navController.navigate(ROUTE_NEW_GOAL) },
                    onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
                    onStatsClick = { navController.navigate(ROUTE_STATS) },
                    onAddCheckIn = { id -> navController.navigate("checkin_flow/$id") },
                    onOpenCheckIn = { checkInId -> navController.navigate("checkin_editor/$checkInId") }
                )
            } else {
                CenteredPane {
                    ListScreen(
                        viewModel = listViewModel,
                        onGoalClick = { id -> navController.navigate("goal_detail/$id") },
                        onNewGoal = { navController.navigate(ROUTE_NEW_GOAL) },
                        onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
                        onStatsClick = { navController.navigate(ROUTE_STATS) }
                    )
                }
            }
        }

        composable(ROUTE_STATS) {
            CenteredPane {
                StatsScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
                    onOpenFollowThrus = { navController.navigate(ROUTE_FOLLOWTHRUS) }
                )
            }
        }

        composable(ROUTE_SETTINGS) {
            CenteredPane {
                SettingsScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onScience = { navController.navigate(ROUTE_SCIENCE) }
                )
            }
        }

        composable(ROUTE_SCIENCE) {
            CenteredPane {
                ScienceScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(ROUTE_FOLLOWTHRUS) {
            CenteredPane {
                FollowThrusScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
                    onGoalClick = { id -> navController.navigate("goal_detail/$id") }
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
                        // Creation flows straight into the new goal's first check-in.
                        // popUpTo(new_goal, inclusive) drops the name screen so Back
                        // from the check-in returns to Home.
                        navController.navigate("checkin_flow/$goalId") {
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
                    container.goalDao,
                    container.checkInDao,
                    goalId
                )
            )
            CenteredPane {
                GoalDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onNavigateToList = {
                        navController.navigate(ROUTE_LIST) {
                            popUpTo(ROUTE_LIST) { inclusive = true }
                        }
                    },
                    onAddCheckIn = { navController.navigate("checkin_flow/$goalId") },
                    onOpenCheckIn = { checkInId -> navController.navigate("checkin_editor/$checkInId") }
                )
            }
        }

        composable(
            route = ROUTE_CHECKIN_EDITOR,
            arguments = listOf(navArgument(ARG_CHECKIN_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val checkInId = backStackEntry.arguments?.getString(ARG_CHECKIN_ID) ?: return@composable
            val vm: CheckInEditorViewModel = viewModel(
                factory = CheckInEditorViewModel.Factory(container.checkInDao, container.goalDao, checkInId)
            )
            CenteredPane {
                CheckInEditorScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
        }

        composable(
            route = ROUTE_CHECKIN_FLOW,
            arguments = listOf(navArgument(ARG_GOAL_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString(ARG_GOAL_ID) ?: return@composable
            val vm: CheckInFlowViewModel = viewModel(
                factory = CheckInFlowViewModel.Factory(
                    container.checkInDao,
                    container.goalDao,
                    goalId
                )
            )
            CenteredPane {
                CheckInFlowScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = { id ->
                        if (isExpandedWidth) {
                            // Two-pane Home: drop the flow and return to Home with
                            // this goal selected, so its refreshed detail shows in
                            // the right pane.
                            selectedGoalId = id
                            navController.navigate(ROUTE_LIST) {
                                popUpTo(ROUTE_CHECKIN_FLOW) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            // Land on the goal's detail. popUpTo(checkin_flow,
                            // inclusive) drops the flow; launchSingleTop reuses an
                            // existing detail underneath rather than stacking a
                            // second one. From creation or the reminder deep-link
                            // there is none, so a fresh detail is pushed over Home.
                            navController.navigate("goal_detail/$id") {
                                popUpTo(ROUTE_CHECKIN_FLOW) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Caps single-column content at a comfortable reading width and centres it against
 * the cream page. On phones (< 600dp) the cap is a no-op, so the design is
 * unchanged; on tablets it keeps forms and lists from stretching edge-to-edge.
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

/**
 * Expanded-width (tablet/landscape) Home: a fixed goals-list pane on the left and
 * the selected goal's detail on the right, so the list stays visible while a goal
 * is open. Tapping a goal updates [selectedGoalId] instead of pushing a new
 * destination; the right pane shows a placeholder until one is chosen. The whole
 * pair is capped and centred so it doesn't sprawl on very wide displays.
 */
@Composable
private fun TwoPaneHome(
    container: AppContainer,
    listViewModel: ListViewModel,
    selectedGoalId: String?,
    onSelectGoal: (String?) -> Unit,
    onNewGoal: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onAddCheckIn: (String) -> Unit,
    onOpenCheckIn: (String) -> Unit
) {
    CenteredPane(maxWidth = 1200.dp) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.width(360.dp).fillMaxHeight()) {
                ListScreen(
                    viewModel = listViewModel,
                    onGoalClick = { onSelectGoal(it) },
                    onNewGoal = onNewGoal,
                    onSettingsClick = onSettingsClick,
                    onStatsClick = onStatsClick
                )
            }
            VerticalDivider(color = AppColors.Border)
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (selectedGoalId != null) {
                    // key() so switching goals builds a fresh detail subtree (and
                    // view-model), resetting its dialogs/scroll/focus per goal.
                    key(selectedGoalId) {
                        val vm: GoalDetailViewModel = viewModel(
                            key = "detail_$selectedGoalId",
                            factory = GoalDetailViewModel.Factory(
                                container.goalDao,
                                container.checkInDao,
                                selectedGoalId
                            )
                        )
                        GoalDetailScreen(
                            viewModel = vm,
                            onBack = { onSelectGoal(null) },
                            onNavigateToList = { onSelectGoal(null) },
                            onAddCheckIn = { onAddCheckIn(selectedGoalId) },
                            onOpenCheckIn = onOpenCheckIn
                        )
                    }
                } else {
                    DetailPlaceholder()
                }
            }
        }
    }
}

/** Right-pane resting state in two-pane Home, before a goal is selected. */
@Composable
private fun DetailPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Select a goal to see its check-ins and reminders.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}
