package com.example.fitpal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitpal.ui.theme.*

@Composable
fun FitPalFloatingBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    onCenterActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkThemeGlobal ?: androidx.compose.foundation.isSystemInDarkTheme()
    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Frosted Glass Floating Dock Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(shape)
                .background(
                    if (isDark) {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xE6141D2D), // 90% opacity frosted slate-glass
                                Color(0xBF0D1421)  // 75% opacity deep backdrop
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xF2FFFFFF), // 95% frosted white
                                Color(0xD9F1F5F9)  // 85% frosted light slate
                            )
                        )
                    }
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(
                                Color.White.copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.22f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color(0x55CBD5E1),
                                Color.White.copy(alpha = 0.70f)
                            )
                        }
                    ),
                    shape = shape
                )
        ) {
            // Delicate top specular reflection line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 24.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                if (isDark) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.75f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Home / Dashboard
                DockNavItem(
                    selected = currentTab == "dashboard",
                    label = "Home",
                    icon = Icons.Default.Home,
                    testTag = "nav_dashboard",
                    onClick = { onTabSelected("dashboard") }
                )

                // Tab 2: Workout / Habits
                DockNavItem(
                    selected = currentTab == "habits",
                    label = "Workout",
                    icon = Icons.Default.FitnessCenter,
                    testTag = "nav_habits",
                    onClick = { onTabSelected("habits") }
                )

                // Center glowing athletic glass action button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.60f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                                    Color.White.copy(alpha = 0.35f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable { onCenterActionClick() }
                        .testTag("nav_center_action"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick Action",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Tab 3: Meals / Diary
                DockNavItem(
                    selected = currentTab == "diary",
                    label = "Meals",
                    icon = Icons.Default.Restaurant,
                    testTag = "nav_diary",
                    onClick = { onTabSelected("diary") }
                )

                // Tab 4: Profile
                DockNavItem(
                    selected = currentTab == "profile",
                    label = "Profile",
                    icon = Icons.Default.Person,
                    testTag = "nav_profile",
                    onClick = { onTabSelected("profile") }
                )
            }
        }
    }
}

@Composable
private fun DockNavItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val primaryColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        // Highlight capsule behind active tab
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) primaryColor.copy(alpha = 0.20f) else Color.Transparent)
                .then(
                    if (selected) {
                        Modifier.border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    primaryColor.copy(alpha = 0.50f),
                                    Color.White.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.25f)
                                )
                            ),
                            shape = RoundedCornerShape(50)
                        )
                    } else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) primaryColor else mutedColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(1.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) primaryColor else mutedColor
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
