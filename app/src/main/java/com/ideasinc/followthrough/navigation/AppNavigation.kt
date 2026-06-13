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
import com.ideasinc.followthrough.ui.today.TodayScreen

// Pre-spine full-screen routes (outside the bottom-bar / rail chrome).
private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_LAUNCH_INSIGHT = "launch_insight"

// Primary destinations — the four spine tabs (Today · Goals · About You · Settings).
private const val ROUTE_TODAY = "today"
private const val ROUTE_GOALS = "goals"
private const val ROUTE_ABOUT_YOU = "about_you"
private const val ROUTE_SETTINGS = "settings"

// Full-screen routes reached from a tab (no bar/rail chrome).
private const val ROUTE_NEW_GOAL = "new_goal"
private const val ROUTE_GOAL_DETAIL = "goal_detail/{goalId}"
private const val ROUTE_CHECKIN_FLOW = "checkin_flow/{goalId}"
private const val ROUTE_CHECKIN_EDITOR = "checkin_editor/{checkInId}"
private const val ROUTE_BUILDER = "builder"
private const val ROUTE_BUILDER_NEW = "builder_new/{goalId}"
private const val ROUTE_BUILDER_EDIT = "builder_edit/{reminderId}"
private const val ROUTE_SCIENCE = "science"
private const val ROUTE_STATS = "stats"
private const val ROUTE_FOLLOWTHRUS = "followthrus"
private const val ARG_GOAL_ID = "goalId"
private const val ARG_CHECKIN_ID = "checkInId"
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
internal const val CURRENT_ONBOARDING_VERSION = 112

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
    pendingCheckInId: String? = null,
    onCheckInConsumed: () -> Unit = {},
    isExpandedWidth: Boolean = false,
    useNavRail: Boolean = false
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    // The goal selected in the two-pane (expanded-width) Goals tab. Hoisted here so
    // it survives while modal flows cover the NavHost and is restored after process
    // death. Ignored on compact widths, where a goal opens as its own destination.
    var selectedGoalId by rememberSaveable { mutableStateOf<String?>(null) }

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val savedVersion = prefs.getInt(KEY_ONBOARDING_VERSION, 0)
    val startDestination = if (savedVersion >= CURRENT_ONBOARDING_VERSION) ROUTE_LAUNCH_INSIGHT else ROUTE_ONBOARDING

    val listViewModel: ListViewModel = viewModel(
        factory = ListViewModel.Factory(container.goalDao, container.checkInDao)
    )

    // A tapped reminder deep-links to THAT specific check-in's editor. Reset the back
    // stack to Today and push the editor on top, so Back returns to Today rather than
    // dropping out of the app. If the check-in was deleted, just land on Today.
    LaunchedEffect(pendingCheckInId) {
        val checkInId = pendingCheckInId ?: return@LaunchedEffect
        val exists = container.checkInDao.getCheckInById(checkInId) != null
        navController.navigate(ROUTE_TODAY) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
        if (exists) {
            navController.navigate("checkin_editor/$checkInId")
        }
        onCheckInConsumed()
    }

    // Chrome (bottom bar / rail) shows only on the four primary destinations; every
    // full-screen route (onboarding, launch, builder, goal detail, check-in) hides it.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showChrome = currentRoute in PRIMARY_ROUTES

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
                    startDestination = startDestination,
                    container = container,
                    listViewModel = listViewModel,
                    prefs = prefs,
                    isExpandedWidth = isExpandedWidth,
                    selectedGoalId = selectedGoalId,
                    onSelectGoal = { selectedGoalId = it },
                    modifier = Modifier.padding(padding)
                )
            }
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
    startDestination: String,
    container: AppContainer,
    listViewModel: ListViewModel,
    prefs: android.content.SharedPreferences,
    isExpandedWidth: Boolean,
    selectedGoalId: String?,
    onSelectGoal: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(ROUTE_ONBOARDING) {
            CenteredPane {
                OnboardingScreen(
                    onComplete = {
                        prefs.edit().putInt(KEY_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION).apply()
                        navController.navigate(ROUTE_LAUNCH_INSIGHT) {
                            popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                        }
                    },
                    onCreateFirstReminder = {
                        prefs.edit().putInt(KEY_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION).apply()
                        navController.navigate(ROUTE_TODAY) {
                            popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                        }
                        navController.navigate(ROUTE_BUILDER)
                    }
                )
            }
        }

        composable(ROUTE_LAUNCH_INSIGHT) {
            val insight = remember { pickLaunchInsight(prefs) }
            CenteredPane {
                LaunchInsightScreen(
                    text = insight,
                    onDismiss = {
                        navController.navigate(ROUTE_TODAY) {
                            popUpTo(ROUTE_LAUNCH_INSIGHT) { inclusive = true }
                        }
                    }
                )
            }
        }

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
            if (isExpandedWidth) {
                TwoPaneGoals(
                    container = container,
                    listViewModel = listViewModel,
                    selectedGoalId = selectedGoalId,
                    onSelectGoal = onSelectGoal,
                    onNewGoal = { navController.navigate(ROUTE_NEW_GOAL) },
                    onSettingsClick = { navController.navigateToTab(ROUTE_SETTINGS) },
                    onStatsClick = { navController.navigate(ROUTE_STATS) },
                    onAddReminder = { id -> navController.navigate("builder_new/$id") },
                    onOpenReminder = { reminderId -> navController.navigate("builder_edit/$reminderId") }
                )
            } else {
                CenteredPane {
                    ListScreen(
                        viewModel = listViewModel,
                        onGoalClick = { id -> navController.navigate("goal_detail/$id") },
                        onNewGoal = { navController.navigate(ROUTE_NEW_GOAL) },
                        onSettingsClick = { navController.navigateToTab(ROUTE_SETTINGS) },
                        onStatsClick = { navController.navigate(ROUTE_STATS) }
                    )
                }
            }
        }

        composable(ROUTE_ABOUT_YOU) {
            CenteredPane { AboutYouScreen(container = container) }
        }

        composable(ROUTE_SETTINGS) {
            CenteredPane {
                SettingsScreen(
                    container = container,
                    onBack = { navController.navigateToTab(ROUTE_TODAY) },
                    onScience = { navController.navigate(ROUTE_SCIENCE) }
                )
            }
        }

        composable(ROUTE_STATS) {
            CenteredPane {
                StatsScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigateToTab(ROUTE_SETTINGS) },
                    onOpenFollowThrus = { navController.navigate(ROUTE_FOLLOWTHRUS) }
                )
            }
        }

        composable(ROUTE_SCIENCE) {
            CenteredPane { ScienceScreen(onBack = { navController.popBackStack() }) }
        }

        composable(ROUTE_FOLLOWTHRUS) {
            CenteredPane {
                FollowThrusScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigateToTab(ROUTE_SETTINGS) },
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
                    container.goalDao, container.checkInDao, container.goalContentDao, container.reminderDao, goalId
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
                factory = CheckInFlowViewModel.Factory(container.checkInDao, container.goalDao, goalId)
            )
            CenteredPane {
                CheckInFlowScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = { id ->
                        if (isExpandedWidth) {
                            onSelectGoal(id)
                            navController.navigate(ROUTE_GOALS) {
                                popUpTo(ROUTE_CHECKIN_FLOW) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
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

/**
 * Expanded-width (tablet/landscape) Goals tab: a fixed goals-list pane on the left
 * and the selected goal's detail on the right, so the list stays visible while a
 * goal is open. Tapping a goal updates [selectedGoalId] instead of pushing a new
 * destination; the right pane shows a placeholder until one is chosen.
 */
@Composable
private fun TwoPaneGoals(
    container: AppContainer,
    listViewModel: ListViewModel,
    selectedGoalId: String?,
    onSelectGoal: (String?) -> Unit,
    onNewGoal: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onAddReminder: (String) -> Unit,
    onOpenReminder: (String) -> Unit
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
                    key(selectedGoalId) {
                        val vm: GoalDetailViewModel = viewModel(
                            key = "detail_$selectedGoalId",
                            factory = GoalDetailViewModel.Factory(
                                container.goalDao,
                                container.checkInDao,
                                container.goalContentDao,
                                container.reminderDao,
                                selectedGoalId
                            )
                        )
                        GoalDetailScreen(
                            viewModel = vm,
                            onBack = { onSelectGoal(null) },
                            onNavigateToList = { onSelectGoal(null) },
                            onAddReminder = { onAddReminder(selectedGoalId) },
                            onOpenReminder = onOpenReminder
                        )
                    }
                } else {
                    DetailPlaceholder()
                }
            }
        }
    }
}

/** Right-pane resting state in the two-pane Goals tab, before a goal is selected. */
@Composable
private fun DetailPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Select a goal to see its reminders and progress.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}
