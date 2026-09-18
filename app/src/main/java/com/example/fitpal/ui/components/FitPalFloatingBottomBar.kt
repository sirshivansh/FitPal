package com.example.fitpal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Elevated dark pill floating dock bar with subtle outline
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF111317),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
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

                // Center glowing neon green industrial button (#CEFD1A with subtle mechanical border)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(VividElectricLime)
                        .clickable { onCenterActionClick() }
                        .testTag("nav_center_action"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick Action",
                        tint = Color(0xFF090A0C),
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
        // Glowing neon green highlight capsule behind active tab
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) VividElectricLime.copy(alpha = 0.18f) else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) VividElectricLime else MutedGrey,
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
            color = if (selected) VividElectricLime else MutedGrey
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(VividElectricLime)
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
