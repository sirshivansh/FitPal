package com.example.fitpal.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

data class WindowDimensions(
    val widthClass: WindowSizeClass,
    val heightClass: WindowSizeClass,
    val widthDp: Dp,
    val heightDp: Dp,
    val isTablet: Boolean
)

@Composable
fun rememberWindowDimensions(): WindowDimensions {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val widthClass = when {
        screenWidth < 600.dp -> WindowSizeClass.COMPACT
        screenWidth < 840.dp -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }

    val heightClass = when {
        screenHeight < 480.dp -> WindowSizeClass.COMPACT
        screenHeight < 900.dp -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }

    val isTablet = screenWidth >= 600.dp

    return WindowDimensions(
        widthClass = widthClass,
        heightClass = heightClass,
        widthDp = screenWidth,
        heightDp = screenHeight,
        isTablet = isTablet
    )
}
