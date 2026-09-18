package com.example.fitpal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitpal.data.local.entity.WorkoutEntry
import kotlin.math.roundToInt

@Composable
fun WorkoutHistoryCard(
    workouts: List<WorkoutEntry>,
    onAddWorkoutClick: () -> Unit,
    onDeleteWorkout: (WorkoutEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val electricLime = Color(0xFFCEFD1A)
    val cardBg = Color(0xFF111317)
    val elevatedBg = Color(0xFF181B20)
    val crispWhite = Color(0xFFF0F3F6)
    val mutedGrey = Color(0xFF717886)
    val earthyBronze = Color(0xFFE5A93C)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("workout_history_card"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = elevatedBg,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = "Workouts",
                                tint = earthyBronze,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Logged Workouts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = crispWhite
                        )
                        Text(
                            text = "ACTIVE SESSIONS & LOGS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = mutedGrey
                        )
                    }
                }

                Button(
                    onClick = onAddWorkoutClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = electricLime,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_workout_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI Workout",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (workouts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(elevatedBg)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No workouts recorded for this day",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = crispWhite.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Log lifting, running, or walking naturally with the AI Notebook",
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedGrey
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    workouts.forEach { workout ->
                        WorkoutItemView(
                            workout = workout,
                            onDelete = { onDeleteWorkout(workout) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutItemView(
    workout: WorkoutEntry,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elevatedBg = Color(0xFF1E251E)
    val crispWhite = Color(0xFFF5F5F7)
    val mutedGrey = Color(0xFF9CA3AF)
    val electricLime = Color(0xFF20AA1D)
    val coralBadge = Color(0xFFAE422A)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("workout_item_${workout.id}"),
        shape = RoundedCornerShape(16.dp),
        color = elevatedBg,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = workout.title.ifBlank { workout.workoutType },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = crispWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF2E462E),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Text(
                                text = workout.workoutType,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = electricLime,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${workout.durationMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedGrey
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedGrey
                        )
                        Text(
                            text = "${workout.intensity} effort",
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedGrey
                        )
                        if (workout.distanceKm != null && workout.distanceKm > 0f) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedGrey
                            )
                            Text(
                                text = "${workout.distanceKm} km",
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedGrey
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${workout.estimatedCalories} kcal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = electricLime
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (workout.confidence == "High")
                                electricLime.copy(alpha = 0.15f)
                            else Color(0xFF2E462E)
                        ) {
                            Text(
                                text = "${workout.confidence} Conf",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (workout.confidence == "High") electricLime else mutedGrey,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_workout_${workout.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete workout",
                            tint = coralBadge,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Exercises breakdown if available
            val exercises = workout.getExercises()
            if (exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    exercises.forEach { ex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = ex.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = crispWhite
                            )
                            val details = buildString {
                                if (ex.sets != null && ex.reps != null) {
                                    append("${ex.sets} × ${ex.reps}")
                                } else if (ex.sets != null) {
                                    append("${ex.sets} sets")
                                }
                                if (ex.weightKg != null && ex.weightKg > 0f) {
                                    append(" @ ${ex.weightKg.roundToInt()}kg")
                                }
                            }
                            if (details.isNotBlank()) {
                                Text(
                                    text = details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = mutedGrey
                                )
                            }
                        }
                    }
                }
            }

            // Historical Weight Note
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Preserved with ${workout.userWeightKgAtTime.roundToInt()}kg body weight at time of logging",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = mutedGrey.copy(alpha = 0.7f)
            )
        }
    }
}
