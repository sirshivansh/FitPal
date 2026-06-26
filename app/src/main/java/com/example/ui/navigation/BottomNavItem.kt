package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem("dashboard", "Dashboard", Icons.Default.Home)
    object Diary : BottomNavItem("diary", "Diary", Icons.Default.Book)
    object Exercise : BottomNavItem("exercise", "Exercise", Icons.Default.DirectionsRun)
    object Progress : BottomNavItem("progress", "Progress", Icons.Default.ShowChart)
    object Achievements : BottomNavItem("achievements", "Badges", Icons.Default.EmojiEvents)
}
