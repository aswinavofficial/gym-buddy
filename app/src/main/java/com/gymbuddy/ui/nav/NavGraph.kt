package com.gymbuddy.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymbuddy.ui.browse.BrowseScreen
import com.gymbuddy.ui.browse.BrowseViewModel
import com.gymbuddy.ui.browse.ExercisePickerScreen
import com.gymbuddy.ui.containerViewModel
import com.gymbuddy.ui.detail.DetailScreen
import com.gymbuddy.ui.detail.DetailViewModel
import com.gymbuddy.ui.home.HomeScreen
import com.gymbuddy.ui.home.HomeViewModel
import com.gymbuddy.ui.onboarding.OnboardingScreen
import com.gymbuddy.ui.plan.PlanEditorScreen
import com.gymbuddy.ui.plan.PlanEditorViewModel
import com.gymbuddy.ui.plan.PlanViewModel
import com.gymbuddy.ui.plan.PlansScreen
import com.gymbuddy.ui.profile.ProfileScreen
import com.gymbuddy.ui.profile.ProfileViewModel
import com.gymbuddy.ui.progress.ProgressScreen
import com.gymbuddy.ui.progress.ProgressViewModel
import com.gymbuddy.ui.workout.HistoryScreen
import com.gymbuddy.ui.workout.HistoryViewModel
import com.gymbuddy.ui.workout.WorkoutSessionScreen
import com.gymbuddy.ui.workout.WorkoutViewModel

@Composable
fun NavGraph(startMain: Boolean) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = TopDestination.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    TopDestination.entries.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(dest.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(iconFor(dest, selected), null) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (startMain) Routes.HOME else Routes.ONBOARDING,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                })
            }

            composable(Routes.HOME) {
                val vm = containerViewModel { HomeViewModel(it) }
                HomeScreen(
                    vm = vm,
                    onStartWorkout = { id -> navController.navigate(Routes.workout(id)) },
                    onOpenSession = { navController.navigate(Routes.HISTORY) },
                    onBrowse = { navController.navigate(Routes.BROWSE) { launchSingleTop = true } },
                    onSeeHistory = { navController.navigate(Routes.HISTORY) },
                )
            }

            composable(Routes.BROWSE) {
                val vm = containerViewModel { BrowseViewModel(it.exerciseRepository) }
                BrowseScreen(vm = vm, onExerciseClick = { navController.navigate(Routes.detail(it)) })
            }

            composable(Routes.PLANS) {
                val vm = containerViewModel { PlanViewModel(it) }
                PlansScreen(vm = vm, onOpenPlan = { navController.navigate(Routes.planEditor(it)) })
            }

            composable(Routes.PROGRESS) {
                val vm = containerViewModel { ProgressViewModel(it) }
                ProgressScreen(vm = vm)
            }

            composable(Routes.PROFILE) {
                val vm = containerViewModel { ProfileViewModel(it) }
                ProfileScreen(vm = vm)
            }

            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                val vm = containerViewModel(key = "detail-$id") {
                    DetailViewModel(id, it.exerciseRepository, it.planRepository, it.settingsRepository, it)
                }
                DetailScreen(vm = vm, onBack = { navController.popBackStack() })
            }

            composable(
                Routes.PLAN_EDITOR,
                arguments = listOf(navArgument("planId") { type = NavType.LongType }),
            ) { entry ->
                val planId = entry.arguments?.getLong("planId") ?: 0L
                val vm = containerViewModel(key = "editor-$planId") { PlanEditorViewModel(planId, it) }
                ConsumePickResult(entry) { vm.completePick(it) }
                PlanEditorScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onPickExercise = { navController.navigate(Routes.PICKER) },
                    onStartDay = { sessionId -> navController.navigate(Routes.workout(sessionId)) },
                    onOpenExercise = { navController.navigate(Routes.detail(it)) },
                )
            }

            composable(Routes.PICKER) {
                ExercisePickerScreen(
                    onPicked = { id ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(Routes.RESULT_PICKED_EXERCISE, id)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                Routes.WORKOUT,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
            ) { entry ->
                val sessionId = entry.arguments?.getLong("sessionId") ?: 0L
                val vm = containerViewModel(key = "workout-$sessionId") { WorkoutViewModel(sessionId, it) }
                ConsumePickResult(entry) { vm.completePick(it) }
                WorkoutSessionScreen(
                    vm = vm,
                    onPickExercise = { navController.navigate(Routes.PICKER) },
                    onFinished = { navController.popBackStack(Routes.HOME, inclusive = false) },
                )
            }

            composable(Routes.HISTORY) {
                val vm = containerViewModel { HistoryViewModel(it) }
                HistoryScreen(vm = vm, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun ConsumePickResult(
    entry: androidx.navigation.NavBackStackEntry,
    onResult: (String) -> Unit,
) {
    val handle = entry.savedStateHandle
    val picked by handle.getStateFlow<String?>(Routes.RESULT_PICKED_EXERCISE, null)
        .collectAsStateWithLifecycle()
    LaunchedEffect(picked) {
        picked?.let {
            onResult(it)
            handle[Routes.RESULT_PICKED_EXERCISE] = null
        }
    }
}

private fun iconFor(dest: TopDestination, selected: Boolean): ImageVector = when (dest) {
    TopDestination.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    TopDestination.BROWSE -> if (selected) Icons.Filled.FitnessCenter else Icons.Outlined.FitnessCenter
    TopDestination.PLANS -> if (selected) Icons.Filled.ListAlt else Icons.Outlined.ListAlt
    TopDestination.PROGRESS -> if (selected) Icons.Filled.BarChart else Icons.Outlined.BarChart
    TopDestination.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
}
