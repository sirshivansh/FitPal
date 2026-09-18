package com.example.fitpal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
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
import com.example.fitpal.util.ObservedMaintenanceResult

@Composable
fun ObservedMaintenanceCard(
    result: ObservedMaintenanceResult,
    modifier: Modifier = Modifier
) {
    val electricLime = Color(0xFFCEFD1A)
    val cardBg = Color(0xFF111317)
    val elevatedBg = Color(0xFF181B20)
    val crispWhite = Color(0xFFF0F3F6)
    val mutedGrey = Color(0xFF717886)
    val bronzeAccent = Color(0xFFE5A93C)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("observed_maintenance_card"),
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
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = bronzeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Real-World Maintenance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = crispWhite
                        )
                        Text(
                            text = "ADAPTIVE CALIBRATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = mutedGrey
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (result.isReady) electricLime.copy(alpha = 0.15f) else Color(0xFF2E462E),
                    border = BorderStroke(
                        1.dp,
                        if (result.isReady) electricLime.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)
                    )
                ) {
                    Text(
                        text = if (result.isReady) "Calibrated" else "${result.daysLogged}/${result.minDaysRequired} Days",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (result.isReady) electricLime else mutedGrey,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!result.isReady) {
                // Progress towards calibration
                val progress = (result.daysLogged.toFloat() / result.minDaysRequired.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = electricLime,
                    trackColor = elevatedBg
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = result.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedGrey,
                    lineHeight = 16.sp
                )
            } else {
                // Calibrated Result
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "True Maintenance",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedGrey
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${result.observedMaintenanceKcal}",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp
                                ),
                                color = electricLime
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "kcal / day",
                                style = MaterialTheme.typography.labelSmall,
                                color = mutedGrey,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Avg Intake: ${result.averageDailyIntakeKcal} kcal",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = crispWhite
                        )
                        Text(
                            text = "Rate: ${if (result.weeklyChangeRateKg > 0) "+" else ""}${result.weeklyChangeRateKg} kg/wk",
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedGrey
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = result.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedGrey,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
