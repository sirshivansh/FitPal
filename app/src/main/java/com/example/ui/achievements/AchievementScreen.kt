package com.example.ui.achievements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(
    viewModel: AchievementViewModel,
    modifier: Modifier = Modifier
) {
    val achievements by viewModel.achievements.collectAsState()
    
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }
    
    val filteredAchievements = remember(achievements, selectedCategory) {
        if (selectedCategory == null) achievements
        else achievements.filter { it.category == selectedCategory }
    }
    
    val totalUnlocked = remember(achievements) { achievements.count { it.isUnlocked } }
    val totalCount = achievements.size
    val unlockRatio = if (totalCount > 0) totalUnlocked.toFloat() / totalCount else 0f
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Earned Badges", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
        ) {
            // HERO ACHIEVEMENTS SUMMARY CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("achievements_summary_card"),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Your Progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$totalUnlocked of $totalCount Unlocked",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MilitaryTech,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Sleek Progress Indicator
                        val animatedRatio by animateFloatAsState(targetValue = unlockRatio, animationSpec = spring())
                        LinearProgressIndicator(
                            progress = animatedRatio,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .testTag("achievements_overall_progress"),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "${(unlockRatio * 100).roundToInt()}% completed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
            
            // CATEGORY TABS SELECTOR (HORIZONTAL FLOW)
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    item {
                        CategoryChip(
                            label = "All",
                            isSelected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            modifier = Modifier.testTag("category_chip_all")
                        )
                    }
                    items(AchievementCategory.values()) { category ->
                        CategoryChip(
                            label = category.displayName,
                            isSelected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            modifier = Modifier.testTag("category_chip_${category.name.lowercase()}")
                        )
                    }
                }
            }
            
            // INDIVIDUAL ACHIEVEMENTS
            items(filteredAchievements, key = { it.id }) { achievement ->
                AchievementRow(
                    achievement = achievement,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("achievement_row_${achievement.id}")
                )
            }
            
            // Empty state if no achievements found
            if (filteredAchievements.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No achievements found in this category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
    val contentColor by animateColorAsState(targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
    val borderStroke = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val view = androidx.compose.ui.platform.LocalView.current
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                com.example.util.HapticFeedbackHelper.triggerLightTap(view)
                onClick()
            },
        color = bgColor,
        border = borderStroke,
        shape = RoundedCornerShape(24.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = contentColor
        )
    }
}

@Composable
fun AchievementRow(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Render custom vector badge icon
            BadgeIcon(
                achievementId = achievement.id,
                category = achievement.category,
                isUnlocked = achievement.isUnlocked,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("badge_icon_${achievement.id}")
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (achievement.isUnlocked) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Unlocked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Progress Bar
                val progressPercent = (achievement.currentProgress / achievement.maxProgress).coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(targetValue = progressPercent, animationSpec = spring())
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (achievement.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${achievement.currentProgress.roundToInt()} / ${achievement.maxProgress.roundToInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = achievement.rewardText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (achievement.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun BadgeIcon(
    achievementId: String,
    category: AchievementCategory,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    // Elegant dynamic color selections
    val badgeBaseColor = when (category) {
        AchievementCategory.STREAKS -> Color(0xFFF4A261) // Sand Orange
        AchievementCategory.WEIGHT -> MaterialTheme.colorScheme.primary
        AchievementCategory.NUTRITION -> Color(0xFF3F8EFC) // Water Blue
        AchievementCategory.EXERCISE -> Color(0xFFE76F51) // Warm Coral Red
    }
    
    val displayColor = if (isUnlocked) badgeBaseColor else Color(0xFFC1C9C1) // Grayscale look if locked
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.width / 2
            val radius = size.width / 2
            
            // Outer Shield/Ring with dashed style or solid line
            drawCircle(
                color = displayColor.copy(alpha = 0.15f),
                radius = radius
            )
            
            drawCircle(
                color = displayColor,
                radius = radius * 0.85f,
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Core colored circle
            drawCircle(
                color = displayColor.copy(alpha = 0.08f),
                radius = radius * 0.7f
            )
        }
        
        // Inner Badge Icon Graphic using our 5 custom vector drawables
        val drawableRes = when (achievementId) {
            "streak_3", "streak_7" -> com.example.fitpal.R.drawable.ic_ach_flame
            "weight_logs_3" -> com.example.fitpal.R.drawable.ic_ach_scale
            "weight_goal_met" -> com.example.fitpal.R.drawable.ic_ach_trophy
            "water_8", "calorie_control" -> com.example.fitpal.R.drawable.ic_ach_apple
            "macro_master" -> com.example.fitpal.R.drawable.ic_ach_trophy
            "exercise_workouts_3", "exercise_warrior_150", "exercise_burner_500" -> com.example.fitpal.R.drawable.ic_ach_dumbbell
            else -> com.example.fitpal.R.drawable.ic_ach_trophy
        }
        
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = drawableRes),
            contentDescription = null,
            tint = displayColor,
            modifier = Modifier.size(30.dp)
        )
    }
}
