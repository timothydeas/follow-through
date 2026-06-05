package com.ideasinc.followthrough.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.ui.checkin.CheckInFlowScreen
import com.ideasinc.followthrough.ui.checkin.CheckInFlowViewModel
import com.ideasinc.followthrough.ui.checkin.CheckInReadScreen
import com.ideasinc.followthrough.ui.checkin.CheckInReadViewModel
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
import com.ideasinc.followthrough.ui.settings.CustomizeQuestionsScreen
import com.ideasinc.followthrough.ui.settings.SettingsScreen
import com.ideasinc.followthrough.ui.stats.StatsScreen

private const val ROUTE_LIST = "list"
private const val ROUTE_LAUNCH_INSIGHT = "launch_insight"
private const val ROUTE_NEW_GOAL = "new_goal"
private const val ROUTE_GOAL_DETAIL = "goal_detail/{goalId}"
private const val ROUTE_CHECKIN_FLOW = "checkin_flow/{goalId}"
private const val ROUTE_CHECKIN_READ = "checkin_read/{checkInId}"
private const val ARG_GOAL_ID = "goalId"
private const val ARG_CHECKIN_ID = "checkInId"
private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_CUSTOMIZE_QUESTIONS = "customize_questions"
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
internal const val CURRENT_ONBOARDING_VERSION = 98

@Composable
fun AppNavigation(
    container: AppContainer,
    pendingGoalId: String? = null,
    onGoalConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val savedVersion = prefs.getInt(KEY_ONBOARDING_VERSION, 0)
    val startDestination = if (savedVersion >= CURRENT_ONBOARDING_VERSION) ROUTE_LAUNCH_INSIGHT else ROUTE_ONBOARDING

    val listViewModel: ListViewModel = viewModel(
        factory = ListViewModel.Factory(
            container.goalDao,
            container.checkInDao
        )
    )

    // A tapped per-goal reminder deep-links here. Reset the back stack to Home
    // and push that goal's detail on top, so Back returns to Home rather than
    // dropping out of the app. If the goal was deleted, just land on Home.
    LaunchedEffect(pendingGoalId) {
        val goalId = pendingGoalId ?: return@LaunchedEffect
        val exists = container.goalDao.getGoalById(goalId) != null
        navController.navigate(ROUTE_LIST) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
        if (exists) {
            navController.navigate("goal_detail/$goalId")
        }
        onGoalConsumed()
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_ONBOARDING) {
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

        composable(ROUTE_LAUNCH_INSIGHT) {
            // Picked once per destination instance; pickLaunchInsight also
            // persists the chosen index so the next launch avoids it.
            val insight = remember { pickLaunchInsight(prefs) }
            LaunchInsightScreen(
                text = insight,
                onDismiss = {
                    navController.navigate(ROUTE_LIST) {
                        popUpTo(ROUTE_LAUNCH_INSIGHT) { inclusive = true }
                    }
                }
            )
        }

        composable(ROUTE_LIST) {
            ListScreen(
                viewModel = listViewModel,
                container = container,
                onGoalClick = { id -> navController.navigate("goal_detail/$id") },
                onNewGoal = { navController.navigate(ROUTE_NEW_GOAL) },
                onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
                onStatsClick = { navController.navigate(ROUTE_STATS) }
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onCustomizeQuestions = { navController.navigate(ROUTE_CUSTOMIZE_QUESTIONS) }
            )
        }

        composable(ROUTE_CUSTOMIZE_QUESTIONS) {
            CustomizeQuestionsScreen(
                container = container,
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_STATS) {
            StatsScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
                onOpenFollowThrus = { navController.navigate(ROUTE_FOLLOWTHRUS) }
            )
        }

        composable(ROUTE_FOLLOWTHRUS) {
            FollowThrusScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
                onGoalClick = { id -> navController.navigate("goal_detail/$id") }
            )
        }

        composable(ROUTE_NEW_GOAL) {
            val vm: NewGoalFlowViewModel = viewModel(
                factory = NewGoalFlowViewModel.Factory(
                    container.goalDao,
                    container.checkInDao,
                    container.questionLabelDao
                )
            )
            NewGoalFlowScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onGoalCreated = { goalId ->
                    navController.navigate("goal_detail/$goalId") {
                        popUpTo(ROUTE_LIST)
                    }
                }
            )
        }

        composable(
            route = ROUTE_GOAL_DETAIL,
            arguments = listOf(navArgument(ARG_GOAL_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString(ARG_GOAL_ID) ?: return@composable
            val vm: GoalDetailViewModel = viewModel(
                factory = GoalDetailViewModel.Factory(container.goalDao, container.checkInDao, container.questionLabelDao, goalId)
            )
            GoalDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onNavigateToList = {
                    navController.navigate(ROUTE_LIST) {
                        popUpTo(ROUTE_LIST) { inclusive = true }
                    }
                },
                onAddCheckIn = { navController.navigate("checkin_flow/$goalId") },
                onCheckInClick = { checkInId -> navController.navigate("checkin_read/$checkInId") }
            )
        }

        composable(
            route = ROUTE_CHECKIN_FLOW,
            arguments = listOf(navArgument(ARG_GOAL_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString(ARG_GOAL_ID) ?: return@composable
            val vm: CheckInFlowViewModel = viewModel(
                factory = CheckInFlowViewModel.Factory(
                    container.checkInDao,
                    container.questionLabelDao,
                    container.goalDao,
                    goalId
                )
            )
            CheckInFlowScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = ROUTE_CHECKIN_READ,
            arguments = listOf(navArgument(ARG_CHECKIN_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val checkInId = backStackEntry.arguments?.getString(ARG_CHECKIN_ID) ?: return@composable
            val vm: CheckInReadViewModel = viewModel(
                factory = CheckInReadViewModel.Factory(
                    container.checkInDao,
                    container.goalDao,
                    container.questionLabelDao,
                    checkInId
                )
            )
            CheckInReadScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
