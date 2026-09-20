package com.example.fitpal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Precision Sports Instrumentation: Calorie Balance Differential
 * Compares consumed calories with total estimated expenditure (BMR + Active Burn + Steps).
 * Formula: Net Balance = Calories Consumed - Total Estimated Expenditure.
 * Negative = Deficit (Weight Loss zone), Positive = Surplus (Anabolic / Mass Gain zone).
 */
@Composable
fun CalorieBalanceDial(
    consumedCalories: Int,
    estimatedExpenditure: Int,
    activeBurn: Int,
    goalAdjustment: Int = 0, // e.g. -500 for deficit goal, +300 for surplus
    modifier: Modifier = Modifier
) {
    val netBalance = consumedCalories - estimatedExpenditure
    val isDeficit = netBalance < 0
    val isNearMaintenance = kotlin.math.abs(netBalance) <= 75

    // Visual colors
    val acidLime = Color(0xFFCEFD1A)
    val flameCoral = Color(0xFFFF5722)
    val electricAzure = Color(0xFF0EA5E9)
    val neutralGrey = Color(0xFF717886)
    val cardBg = Color(0xFF111317)
    val hairlineBorder = Color(255, 255, 255, 20)

    val statusColor = when {
        isNearMaintenance -> Color(0xFF10B981) // Muted emerald maintenance
        isDeficit -> acidLime                   // Acid lime deficit
        else -> flameCoral                     // Flame coral surplus
    }

    val statusLabel = when {
        isNearMaintenance -> "NEUTRAL / MAINTENANCE"
        isDeficit -> "NET DEFICIT (${kotlin.math.abs(netBalance)} KCAL)"
        else -> "NET SURPLUS (+${netBalance} KCAL)"
    }

    // Dial ratio calculation (-1000 deficit to +1000 surplus clamped)
    val balanceClamped = netBalance.coerceIn(-1200, 1200)
    // 0 = full deficit (-1200), 0.5 = neutral (0), 1.0 = full surplus (+1200)
    val dialFraction = ((balanceClamped + 1200f) / 2400f).coerceIn(0f, 1f)

    val animatedFraction by animateFloatAsState(
        targetValue = dialFraction,
        animationSpec = tween(durationMillis = 900),
        label = "DialNeedle"
    )

    var showInfoDialog by remember { mutableStateOf(false) }

    GlassCard(
        modifier = modifier
            .testTag("calorie_balance_dial_card")
            .semantics {
                contentDescription = "Calorie Balance Differential: Consumed $consumedCalories, Expenditure $estimatedExpenditure. Net balance is $netBalance kcal."
            },
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header with sports instrumentation title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(acidLime.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = acidLime,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "CALORIE BALANCE DIFFERENTIAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = acidLime
                        )
                        Text(
                            text = "Energy Ingested vs. Total Metabolic Burn",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = neutralGrey
                        )
                    }
                }

                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.size(28.dp).testTag("balance_dial_info_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Calculation info",
                        tint = neutralGrey,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Precision Needle Arc Gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(130.dp)
                ) {
                    val strokeWidth = 10.dp.toPx()
                    val arcRadius = (size.width - strokeWidth) / 2
                    val arcCenter = Offset(size.width / 2, size.height - 10f)

                    // 180-degree semicircular gauge arc: starts at 180° (left), sweeps 180° to 0° (right)
                    val arcTopLeft = Offset(arcCenter.x - arcRadius, arcCenter.y - arcRadius)
                    val arcSize = Size(arcRadius * 2, arcRadius * 2)

                    // Track background
                    drawArc(
                        color = Color(0xFF1F242D),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Gradient active arc
                    val arcGradient = Brush.horizontalGradient(
                        0.0f to acidLime,       // Deficit zone (left)
                        0.5f to Color(0xFF10B981), // Neutral zone (mid)
                        1.0f to flameCoral      // Surplus zone (right)
                    )

                    drawArc(
                        brush = arcGradient,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Neutral midpoint indicator
                    val midAngleRad = Math.toRadians(270.0)
                    val midMarkerStart = Offset(
                        arcCenter.x + ((arcRadius - 8.dp.toPx()) * cos(midAngleRad)).toFloat(),
                        arcCenter.y + ((arcRadius - 8.dp.toPx()) * sin(midAngleRad)).toFloat()
                    )
                    val midMarkerEnd = Offset(
                        arcCenter.x + ((arcRadius + 8.dp.toPx()) * cos(midAngleRad)).toFloat(),
                        arcCenter.y + ((arcRadius + 8.dp.toPx()) * sin(midAngleRad)).toFloat()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = midMarkerStart,
                        end = midMarkerEnd,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Needle calculation based on animatedFraction
                    // angle ranges from 180° (left) to 360° (right)
                    val needleAngleDeg = 180f + (animatedFraction * 180f)
                    val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
                    val needleLength = arcRadius - 12.dp.toPx()
                    val needleTip = Offset(
                        arcCenter.x + (needleLength * cos(needleAngleRad)).toFloat(),
                        arcCenter.y + (needleLength * sin(needleAngleRad)).toFloat()
                    )

                    // Draw needle arm
                    drawLine(
                        color = Color.White,
                        start = arcCenter,
                        end = needleTip,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Center pivot hub
                    drawCircle(
                        color = statusColor,
                        radius = 7.dp.toPx(),
                        center = arcCenter
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = arcCenter
                    )
                }

                // Gauge readout at bottom center
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-4).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val signPrefix = if (netBalance > 0) "+" else ""
                    Text(
                        text = "$signPrefix$netBalance",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 28.sp
                        ),
                        color = statusColor,
                        modifier = Modifier.testTag("net_balance_value")
                    )
                    Text(
                        text = "NET KCAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = neutralGrey
                    )
                }
            }

            // Status Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isDeficit) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-metrics Bar (Energy In vs Energy Out)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161920))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ingested
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                        Text("Ingested", style = MaterialTheme.typography.labelSmall, color = neutralGrey)
                    }
                    Text(
                        text = "$consumedCalories kcal",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                }

                Text(
                    text = "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = neutralGrey
                )

                // Burned / Expenditure
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(flameCoral))
                        Text("Total Out", style = MaterialTheme.typography.labelSmall, color = neutralGrey)
                    }
                    Text(
                        text = "$estimatedExpenditure kcal",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                }

                Text(
                    text = "=",
                    style = MaterialTheme.typography.titleMedium,
                    color = neutralGrey
                )

                // Active Burn contribution
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(acidLime))
                        Text("Active Burn", style = MaterialTheme.typography.labelSmall, color = neutralGrey)
                    }
                    Text(
                        text = "$activeBurn kcal",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = acidLime
                    )
                }
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text("Energy Balance Formula", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Net Balance = Ingested Calories - Estimated Expenditure",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = acidLime
                    )
                    Text(
                        text = "• Ingested Calories: Sum of all logged foods and beverages for today.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Estimated Expenditure: Dynamic base TDEE (BMR adjusted for basal metabolic load) + active exercise expenditure + step burn.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Negative Net Balance: An energy deficit. Your body utilizes stored glycogen and adipose tissue to meet metabolic demands.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Positive Net Balance: An energy surplus. Energy is directed toward recovery, muscle protein synthesis, or glycogen storage.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
