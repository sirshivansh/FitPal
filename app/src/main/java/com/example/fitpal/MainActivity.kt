package com.example.fitpal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fitpal.ui.dashboard.DashboardScreen
import com.example.fitpal.ui.dashboard.DashboardViewModel
import com.example.fitpal.ui.dashboard.HabitsScreen
import com.example.fitpal.ui.diary.DiaryScreen
import com.example.fitpal.ui.diary.DiaryViewModel
import com.example.fitpal.ui.profile.ProfileScreen
import com.example.fitpal.ui.profile.ProfileViewModel
import com.example.fitpal.ui.profile.SettingsScreen
import com.example.fitpal.ui.components.PinLockScreen
import com.example.fitpal.ui.components.FitPalFloatingBottomBar
import com.example.fitpal.ui.components.SpeedDialSheet
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.platform.testTag
import com.example.fitpal.util.SecurityUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val themePrefs = getSharedPreferences("fitpal_settings", android.content.Context.MODE_PRIVATE)
        if (themePrefs.contains("is_dark_theme")) {
            com.example.fitpal.ui.theme.isDarkThemeGlobal = themePrefs.getBoolean("is_dark_theme", true)
        }

        setContent {
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDark = com.example.fitpal.ui.theme.isDarkThemeGlobal ?: systemDark
            com.example.fitpal.ui.theme.FitPalTheme(darkTheme = isDark) {
                com.example.fitpal.ui.components.GlassAtmosphericBackground(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val context = LocalContext.current
                    val app = context.applicationContext as FitPalApplication
                
                val factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return when {
                            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> 
                                DashboardViewModel(app.profileRepository, app.foodRepository, app.exerciseRepository) as T
                            modelClass.isAssignableFrom(DiaryViewModel::class.java) -> 
                                DiaryViewModel(app.profileRepository, app.foodRepository, app.exerciseRepository) as T
                            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> 
                                ProfileViewModel(app.profileRepository, app.cloudSyncManager) as T
                            else -> throw IllegalArgumentException("Unknown ViewModel class")
                        }
                    }
                }

                val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
                val diaryViewModel: DiaryViewModel = viewModel(factory = factory)
                val profileViewModel: ProfileViewModel = viewModel(factory = factory)

                var isUnlocked by remember { mutableStateOf(!SecurityUtils.isPinSet(context)) }
                
                if (!isUnlocked) {
                    PinLockScreen(
                        onUnlockSuccess = { isUnlocked = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val prefs = remember { context.getSharedPreferences("fitpal_prefs", android.content.Context.MODE_PRIVATE) }
                    var hasCachedProfile by remember { mutableStateOf(prefs.getBoolean("has_active_profile", false)) }

                    val profileState by profileViewModel.userProfile.collectAsState()
                    var isProfileLoaded by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(profileState) {
                        if (profileState != null) {
                            hasCachedProfile = true
                            prefs.edit().putBoolean("has_active_profile", true).apply()
                        }
                        isProfileLoaded = true
                    }
                    
                    if (!isProfileLoaded && !hasCachedProfile) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (isProfileLoaded && profileState == null && !hasCachedProfile) {
                        // Display Setup flow directly without bottom bar navigation
                        ProfileScreen(
                            viewModel = profileViewModel,
                            onProfileSaved = {
                                prefs.edit().putBoolean("has_active_profile", true).apply()
                                hasCachedProfile = true
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val navController = rememberNavController()
                        var currentTab by remember { mutableStateOf("dashboard") }
                        var isSpeedDialOpen by remember { mutableStateOf(false) }

                        // Quick logging handlers for Speed Dial actions
                        val onLogFoodManual = {
                            currentTab = "diary"
                            navController.navigate("diary") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        val onLogFoodAi = {
                            currentTab = "diary"
                            navController.navigate("diary") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        val onLogWater = {
                            dashboardViewModel.incrementWater()
                        }

                        val onStartWorkout = {
                            currentTab = "habits"
                            navController.navigate("habits") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        val onAiWorkoutNotebook = {
                            currentTab = "dashboard"
                            navController.navigate("dashboard") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        val onRecordWeight: (Float) -> Unit = { weight ->
                            dashboardViewModel.logWeight(weight)
                        }

                        val onCheckinHabits = {
                            currentTab = "habits"
                            navController.navigate("habits") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                        // Speed Dial Sheet (Adaptive: Modal Dialog on Tablet, Bottom Sheet on Phone)
                        SpeedDialSheet(
                            isOpen = isSpeedDialOpen,
                            onDismiss = { isSpeedDialOpen = false },
                            onLogFoodManual = onLogFoodManual,
                            onLogFoodAi = onLogFoodAi,
                            onLogWater = onLogWater,
                            onStartWorkout = onStartWorkout,
                            onAiWorkoutNotebook = onAiWorkoutNotebook,
                            onRecordWeight = onRecordWeight,
                            onCheckinHabits = onCheckinHabits
                        )

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val isExpanded = maxWidth >= 600.dp

                            if (isExpanded) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    NavigationRail(
                                        modifier = Modifier.fillMaxHeight(),
                                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        header = {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            com.example.fitpal.ui.components.BrandLogo(size = 36.dp)
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    ) {
                                        NavigationRailItem(
                                            selected = currentTab == "dashboard",
                                            onClick = {
                                                currentTab = "dashboard"
                                                navController.navigate("dashboard") {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                            label = { Text("Dashboard") },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag("nav_dashboard")
                                        )
                                        NavigationRailItem(
                                            selected = currentTab == "diary",
                                            onClick = {
                                                currentTab = "diary"
                                                navController.navigate("diary") {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(Icons.Default.Book, contentDescription = "Diary") },
                                            label = { Text("Diary") },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag("nav_diary")
                                        )
                                        NavigationRailItem(
                                            selected = currentTab == "habits",
                                            onClick = {
                                                currentTab = "habits"
                                                navController.navigate("habits") {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Habits") },
                                            label = { Text("Habits") },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag("nav_habits")
                                        )
                                        NavigationRailItem(
                                            selected = currentTab == "profile",
                                            onClick = {
                                                currentTab = "profile"
                                                navController.navigate("profile") {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                            label = { Text("Profile") },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag("nav_profile")
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        FloatingActionButton(
                                            onClick = { isSpeedDialOpen = true },
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier
                                                .padding(bottom = 24.dp)
                                                .size(48.dp)
                                                .testTag("rail_speed_dial_fab")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Quick Log Actions")
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        NavHost(
                                            navController = navController,
                                            startDestination = "dashboard",
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            composable("dashboard") {
                                                DashboardScreen(
                                                    viewModel = dashboardViewModel,
                                                    onNavigateToSettings = { navController.navigate("settings") },
                                                    onQuickAddFood = {
                                                        currentTab = "diary"
                                                        navController.navigate("diary") {
                                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    },
                                                    onNavigateToHabits = { navController.navigate("habits") }
                                                )
                                            }
                                            composable("habits") {
                                                HabitsScreen(
                                                    profileRepository = app.profileRepository,
                                                    foodRepository = app.foodRepository,
                                                    exerciseRepository = app.exerciseRepository,
                                                    onBack = { navController.popBackStack() },
                                                    onNavigateToDiary = { targetDate ->
                                                        diaryViewModel.setDate(targetDate)
                                                        currentTab = "diary"
                                                        navController.navigate("diary") {
                                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    },
                                                    onNavigateToWorkouts = { targetDate ->
                                                        diaryViewModel.setDate(targetDate)
                                                        currentTab = "diary"
                                                        navController.navigate("diary") {
                                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                )
                                            }
                                            composable("diary") {
                                                DiaryScreen(
                                                    viewModel = diaryViewModel,
                                                    onAddFoodForMeal = { _, _ -> },
                                                    onAddExercise = { }
                                                )
                                            }
                                            composable("profile") {
                                                ProfileScreen(
                                                    viewModel = profileViewModel,
                                                    onProfileSaved = { navController.navigate("dashboard") }
                                                )
                                            }
                                            composable("settings") {
                                                SettingsScreen(
                                                    profileViewModel = profileViewModel,
                                                    onBack = { navController.popBackStack() }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Scaffold(
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    bottomBar = {
                                        FitPalFloatingBottomBar(
                                            currentTab = currentTab,
                                            onTabSelected = { selectedRoute ->
                                                currentTab = selectedRoute
                                                navController.navigate(selectedRoute) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            onCenterActionClick = {
                                                // Center glowing neon green button triggers multi-action speed dial
                                                isSpeedDialOpen = true
                                            }
                                        )
                                    }
                                ) { innerPadding ->
                                    NavHost(
                                        navController = navController,
                                        startDestination = "dashboard",
                                        modifier = Modifier.padding(innerPadding)
                                    ) {
                                        composable("dashboard") {
                                            DashboardScreen(
                                                viewModel = dashboardViewModel,
                                                onNavigateToSettings = { navController.navigate("settings") },
                                                onQuickAddFood = {
                                                    currentTab = "diary"
                                                    navController.navigate("diary") {
                                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                onNavigateToHabits = { navController.navigate("habits") }
                                            )
                                        }
                                        composable("habits") {
                                            HabitsScreen(
                                                profileRepository = app.profileRepository,
                                                foodRepository = app.foodRepository,
                                                exerciseRepository = app.exerciseRepository,
                                                onBack = { navController.popBackStack() },
                                                onNavigateToDiary = { targetDate ->
                                                    diaryViewModel.setDate(targetDate)
                                                    currentTab = "diary"
                                                    navController.navigate("diary") {
                                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                onNavigateToWorkouts = { targetDate ->
                                                    diaryViewModel.setDate(targetDate)
                                                    currentTab = "diary"
                                                    navController.navigate("diary") {
                                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            )
                                        }
                                        composable("diary") {
                                            DiaryScreen(
                                                viewModel = diaryViewModel,
                                                onAddFoodForMeal = { _, _ -> },
                                                onAddExercise = { }
                                            )
                                        }
                                        composable("profile") {
                                            ProfileScreen(
                                                viewModel = profileViewModel,
                                                onProfileSaved = { navController.navigate("dashboard") }
                                            )
                                        }
                                        composable("settings") {
                                            SettingsScreen(
                                                profileViewModel = profileViewModel,
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
