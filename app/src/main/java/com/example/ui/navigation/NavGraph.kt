package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.FitPalApplication
import com.example.ui.ViewModelFactory
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.diary.DiaryScreen
import com.example.ui.diary.DiaryViewModel
import com.example.ui.exercise.ExerciseLogScreen
import com.example.ui.exercise.ExerciseViewModel
import com.example.ui.foodlogging.FoodLogScreen
import com.example.ui.foodlogging.FoodLogViewModel
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.ProfileViewModel
import com.example.ui.profile.SettingsScreen
import com.example.ui.progress.ProgressScreen
import com.example.ui.progress.ProgressViewModel
import com.example.ui.achievements.AchievementScreen
import com.example.ui.achievements.AchievementViewModel

@Composable
fun FitPalAppContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as FitPalApplication
    val factory = remember { ViewModelFactory(app) }

    val profileViewModel: ProfileViewModel = viewModel(factory = factory)
    val userProfile by profileViewModel.userProfile.collectAsState()

    // If profile has not been created yet, force Onboarding Screen
    if (userProfile == null) {
        ProfileScreen(
            viewModel = profileViewModel,
            onProfileSaved = { /* Will naturally trigger state update & load main scaffold */ }
        )
    } else {
        MainAppScaffold(factory = factory, profileViewModel = profileViewModel)
    }
}

@Composable
fun MainAppScaffold(
    factory: ViewModelFactory,
    profileViewModel: ProfileViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Diary,
        BottomNavItem.Exercise,
        BottomNavItem.Progress,
        BottomNavItem.Achievements
    )

    // Hide bottom bar on food log and settings screens to maintain focused touch boundaries
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("app_bottom_bar")
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        val view = androidx.compose.ui.platform.LocalView.current
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    com.example.util.HapticFeedbackHelper.triggerLightTap(view)
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            modifier = Modifier.testTag("nav_item_${item.route}")
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Dashboard.route) {
                val dashboardVm: DashboardViewModel = viewModel(factory = factory)
                DashboardScreen(
                    viewModel = dashboardVm,
                    onNavigateToSettings = { navController.navigate("settings") },
                    onQuickAddFood = { navController.navigate("food_log/Breakfast") },
                    onNavigateToHabits = { navController.navigate("habits") }
                )
            }

            composable(BottomNavItem.Diary.route) {
                val diaryVm: DiaryViewModel = viewModel(factory = factory)
                DiaryScreen(
                    viewModel = diaryVm,
                    onAddFoodForMeal = { mealType -> navController.navigate("food_log/$mealType") },
                    onAddExercise = { navController.navigate("exercise_log") }
                )
            }

            composable(BottomNavItem.Exercise.route) {
                val exerciseVm: ExerciseViewModel = viewModel(factory = factory)
                // Re-use search/adding panel as part of primary layout tab!
                ExerciseLogScreen(
                    viewModel = exerciseVm,
                    onBack = { navController.navigate(BottomNavItem.Dashboard.route) }
                )
            }

            composable(BottomNavItem.Progress.route) {
                val progressVm: ProgressViewModel = viewModel(factory = factory)
                ProgressScreen(viewModel = progressVm)
            }

            composable(BottomNavItem.Achievements.route) {
                val achievementVm: AchievementViewModel = viewModel(factory = factory)
                AchievementScreen(viewModel = achievementVm)
            }

            composable("settings") {
                SettingsScreen(
                    profileViewModel = profileViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("habits") {
                val habitVm: com.example.ui.habit.HabitViewModel = viewModel(factory = factory)
                com.example.ui.habit.HabitScreen(
                    viewModel = habitVm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "food_log/{mealType}",
                arguments = listOf(navArgument("mealType") { type = NavType.StringType })
            ) { backStackEntry ->
                val mealType = backStackEntry.arguments?.getString("mealType") ?: "Breakfast"
                val foodLogVm: FoodLogViewModel = viewModel(factory = factory)
                FoodLogScreen(
                    viewModel = foodLogVm,
                    mealType = mealType,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("exercise_log") {
                val exerciseVm: ExerciseViewModel = viewModel(factory = factory)
                ExerciseLogScreen(
                    viewModel = exerciseVm,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
