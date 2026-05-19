package com.ideasinc.followthrough.navigation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.ideasinc.followthrough.ui.editor.EditorScreen
import com.ideasinc.followthrough.ui.editor.EditorViewModel
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
import com.ideasinc.followthrough.ui.stepflow.StepFlowScreen
import com.ideasinc.followthrough.ui.stepflow.StepFlowViewModel

private const val ROUTE_LIST = "list"
private const val ROUTE_LAUNCH_INSIGHT = "launch_insight"
private const val ROUTE_EDITOR = "editor/{noteId}"
private const val ROUTE_STEP_FLOW = "step_flow/{noteId}"
private const val ROUTE_NEW_GOAL = "new_goal"
private const val ROUTE_GOAL_DETAIL = "goal_detail/{goalId}"
private const val ROUTE_CHECKIN_FLOW = "checkin_flow/{goalId}"
private const val ROUTE_CHECKIN_READ = "checkin_read/{checkInId}"
private const val ARG_NOTE_ID = "noteId"
private const val ARG_GOAL_ID = "goalId"
private const val ARG_CHECKIN_ID = "checkInId"
private const val NEW_NOTE_SENTINEL = "new"
private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_CUSTOMIZE_QUESTIONS = "customize_questions"
private const val ROUTE_STATS = "stats"

internal const val PREFS_NAME = "grounded_prefs"
internal const val KEY_ONBOARDING_VERSION = "onboarding_version"
internal const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
internal const val CURRENT_ONBOARDING_VERSION = 63

@Composable
fun AppNavigation(
    container: AppContainer,
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val savedVersion = prefs.getInt(KEY_ONBOARDING_VERSION, 0)
    val startDestination = if (savedVersion >= CURRENT_ONBOARDING_VERSION) ROUTE_LAUNCH_INSIGHT else ROUTE_ONBOARDING

    val isTablet = windowWidthSizeClass != WindowWidthSizeClass.Compact

    val listViewModel: ListViewModel = viewModel(
        factory = ListViewModel.Factory(
            container.goalDao,
            container.checkInDao
        )
    )

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
            if (isTablet) {
                TabletLayout(
                    container = container,
                    listViewModel = listViewModel,
                    navController = navController,
                    onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
                    onStatsClick = { navController.navigate(ROUTE_STATS) }
                )
            } else {
                ListScreen(
                    viewModel = listViewModel,
                    onGoalClick = { id -> navController.navigate("goal_detail/$id") },
                    onNewGoal = { navController.navigate(ROUTE_NEW_GOAL) },
                    onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
                    onStatsClick = { navController.navigate(ROUTE_STATS) }
                )
            }
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
                onBack = { navController.popBackStack() }
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
                factory = GoalDetailViewModel.Factory(container.goalDao, container.checkInDao, goalId)
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

        // Legacy routes kept for backward compatibility
        composable(
            route = ROUTE_EDITOR,
            arguments = listOf(navArgument(ARG_NOTE_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString(ARG_NOTE_ID)
            val noteId = if (rawId == null || rawId == NEW_NOTE_SENTINEL) null else rawId
            val editorVm: EditorViewModel = viewModel(
                factory = EditorViewModel.Factory(container.noteDao, noteId)
            )
            EditorScreen(
                viewModel = editorVm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = ROUTE_STEP_FLOW,
            arguments = listOf(navArgument(ARG_NOTE_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString(ARG_NOTE_ID)
            val noteId = if (rawId == null || rawId == NEW_NOTE_SENTINEL) null else rawId
            val stepFlowVm: StepFlowViewModel = viewModel(
                factory = StepFlowViewModel.Factory(container.noteDao, noteId)
            )
            StepFlowScreen(
                viewModel = stepFlowVm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun TabletLayout(
    container: AppContainer,
    listViewModel: ListViewModel,
    navController: androidx.navigation.NavHostController,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    var selectedGoalId by remember { mutableStateOf<String?>(null) }
    var selectionKey by remember { mutableStateOf(0) }

    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
        ) {
            ListScreen(
                viewModel = listViewModel,
                onGoalClick = { id ->
                    selectedGoalId = id
                    selectionKey++
                },
                onNewGoal = { navController.navigate(ROUTE_NEW_GOAL) },
                onSettingsClick = onSettingsClick,
                onStatsClick = onStatsClick
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val goalId = selectedGoalId
            if (goalId == null) {
                Text(
                    text = "Select a goal or tap + to create one",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val vm: GoalDetailViewModel = viewModel(
                    key = "${goalId}_$selectionKey",
                    factory = GoalDetailViewModel.Factory(container.goalDao, container.checkInDao, goalId)
                )
                GoalDetailScreen(
                    viewModel = vm,
                    onBack = { selectedGoalId = null },
                    onNavigateToList = { selectedGoalId = null },
                    onAddCheckIn = { navController.navigate("checkin_flow/$goalId") },
                    onCheckInClick = { checkInId -> navController.navigate("checkin_read/$checkInId") }
                )
            }
        }
    }
}
