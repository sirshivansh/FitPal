package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareHelper {

    fun shareDailyNutritionImage(
        context: Context,
        dateStr: String,
        isDarkTheme: Boolean,
        totalFoodCalories: Int,
        totalExerciseCalories: Int,
        calorieGoal: Int,
        proteinGrams: Float,
        proteinGoal: Int,
        carbsGrams: Float,
        carbsGoal: Int,
        fatGrams: Float,
        fatGoal: Int
    ) {
        try {
            // Create a high-res bitmap
            val width = 1000
            val height = 1200
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Paint for Background Gradient
            val bgPaint = Paint().apply { isAntiAlias = true }
            val bgShader = if (isDarkTheme) {
                LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.parseColor("#121214"), Color.parseColor("#1C1C24"),
                    Shader.TileMode.CLAMP
                )
            } else {
                LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.parseColor("#F9F9FB"), Color.parseColor("#EAEAF2"),
                    Shader.TileMode.CLAMP
                )
            }
            bgPaint.shader = bgShader
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Paint definitions
            val textPaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            // Draw Header Card background
            val cardPaint = Paint().apply {
                isAntiAlias = true
                color = if (isDarkTheme) Color.parseColor("#25252E") else Color.WHITE
                style = Paint.Style.FILL
            }
            val shadowPaint = Paint().apply {
                isAntiAlias = true
                color = if (isDarkTheme) Color.argb(40, 0, 0, 0) else Color.argb(30, 0, 0, 0)
                style = Paint.Style.FILL
            }

            // Draw shadow then card for Header
            canvas.drawRoundRect(80f, 85f, width - 80f, 245f, 32f, 32f, shadowPaint)
            canvas.drawRoundRect(80f, 80f, width - 80f, 240f, 32f, 32f, cardPaint)

            // Header Texts
            textPaint.color = if (isDarkTheme) Color.parseColor("#00E676") else Color.parseColor("#4CAF50")
            textPaint.textSize = 48f
            canvas.drawText("FitPal Nutrition Report", (width / 2).toFloat(), 150f, textPaint)

            textPaint.color = if (isDarkTheme) Color.WHITE else Color.BLACK
            textPaint.textSize = 34f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Date: $dateStr", (width / 2).toFloat(), 205f, textPaint)

            // DRAW MAIN CALORIE CONTAINER CARD
            canvas.drawRoundRect(80f, 295f, width - 80f, 655f, 32f, 32f, shadowPaint)
            canvas.drawRoundRect(80f, 290f, width - 80f, 650f, 32f, 32f, cardPaint)

            // Card title
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 36f
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = if (isDarkTheme) Color.WHITE else Color.BLACK
            canvas.drawText("Calorie Summary", 130f, 355f, textPaint)

            // Draw visual progress track & indicator
            val ringX = 260f
            val ringY = 490f
            val ringRadius = 100f
            val ringPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 24f
                strokeCap = Paint.Cap.ROUND
            }
            
            // Base track
            ringPaint.color = if (isDarkTheme) Color.parseColor("#3A3A4A") else Color.parseColor("#E0E0E0")
            canvas.drawCircle(ringX, ringY, ringRadius, ringPaint)

            // Progress Arc
            val netCalories = totalFoodCalories - totalExerciseCalories
            val progressPercent = if (calorieGoal > 0) netCalories.toFloat() / calorieGoal.toFloat() else 0f
            val progressAngle = (progressPercent.coerceIn(0f, 1f) * 360f)
            ringPaint.color = Color.parseColor("#4CAF50") // Premium Green
            val rectF = RectF(ringX - ringRadius, ringY - ringRadius, ringX + ringRadius, ringY + ringRadius)
            canvas.drawArc(rectF, -90f, progressAngle, false, ringPaint)

            // Inside Ring Texts
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 44f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = if (isDarkTheme) Color.WHITE else Color.BLACK
            canvas.drawText("$netCalories", ringX, ringY + 10f, textPaint)

            textPaint.textSize = 20f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = if (isDarkTheme) Color.parseColor("#A0A0B0") else Color.parseColor("#666666")
            canvas.drawText("kcal net", ringX, ringY + 40f, textPaint)

            // Calorie breakdown column labels on the right
            textPaint.textAlign = Paint.Align.LEFT
            val infoX = 420f
            
            fun drawCalorieItem(label: String, value: String, y: Float, colorCode: String) {
                val dotPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor(colorCode)
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(infoX, y - 10f, 10f, dotPaint)
                
                textPaint.color = if (isDarkTheme) Color.parseColor("#D0D0E0") else Color.parseColor("#555555")
                textPaint.textSize = 28f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(label, infoX + 25f, y, textPaint)

                textPaint.color = if (isDarkTheme) Color.WHITE else Color.BLACK
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(value, infoX + 260f, y, textPaint)
            }

            drawCalorieItem("Goal Budget:", "$calorieGoal kcal", 420f, if (isDarkTheme) "#00E676" else "#4CAF50")
            drawCalorieItem("Food Eaten:", "$totalFoodCalories kcal", 480f, "#2196F3")
            drawCalorieItem("Exercise Burned:", "$totalExerciseCalories kcal", 540f, "#FF9800")
            drawCalorieItem("Remaining:", "${(calorieGoal - netCalories).coerceAtLeast(0)} kcal", 600f, "#9C27B0")

            // DRAW MACRONUTRIENT CARD
            canvas.drawRoundRect(80f, 705f, width - 80f, 1085f, 32f, 32f, shadowPaint)
            canvas.drawRoundRect(80f, 700f, width - 80f, 1080f, 32f, 32f, cardPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 36f
            textPaint.color = if (isDarkTheme) Color.WHITE else Color.BLACK
            canvas.drawText("Macronutrient Targets", 130f, 765f, textPaint)

            // Draw three horizontal modern progress bars
            fun drawMacroProgressBar(
                label: String,
                current: Float,
                goal: Int,
                y: Float,
                trackColor: String,
                fillColor: String
            ) {
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textSize = 28f
                textPaint.color = if (isDarkTheme) Color.WHITE else Color.BLACK
                canvas.drawText(label, 130f, y, textPaint)

                val ratio = if (goal > 0) current / goal.toFloat() else 0f
                val isExcess = ratio > 1f
                val statusText = if (isExcess) "${current.toInt()}g / ${goal}g (Over)" else "${current.toInt()}g / ${goal}g"
                
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.color = if (isExcess) Color.RED else (if (isDarkTheme) Color.parseColor("#A0A0B0") else Color.parseColor("#666666"))
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(statusText, width - 130f, y, textPaint)

                // Progress Bar Background track
                val barPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                }
                barPaint.color = Color.parseColor(trackColor)
                val barLeft = 130f
                val barRight = width - 130f
                val barHeight = 24f
                val barTop = y + 20f
                val barBottom = barTop + barHeight
                canvas.drawRoundRect(barLeft, barTop, barRight, barBottom, 12f, 12f, barPaint)

                // Fill progress
                barPaint.color = if (isExcess) Color.parseColor("#E74C3C") else Color.parseColor(fillColor)
                val fillRight = barLeft + (barRight - barLeft) * ratio.coerceIn(0f, 1f)
                canvas.drawRoundRect(barLeft, barTop, fillRight, barBottom, 12f, 12f, barPaint)
                
                textPaint.textAlign = Paint.Align.LEFT
            }

            val pTrackColor = if (isDarkTheme) "#2A3A4E" else "#E3F2FD"
            val cTrackColor = if (isDarkTheme) "#3E2723" else "#EFEBE9"
            val fTrackColor = if (isDarkTheme) "#4A148C" else "#F3E5F5"

            drawMacroProgressBar("Protein", proteinGrams, proteinGoal, 830f, pTrackColor, "#3F8EFC")
            drawMacroProgressBar("Carbohydrates", carbsGrams, carbsGoal, 920f, cTrackColor, "#FF9F43")
            drawMacroProgressBar("Fat", fatGrams, fatGoal, 1010f, fTrackColor, "#10AC84")

            // Draw Footer info
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 24f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textPaint.color = if (isDarkTheme) Color.parseColor("#A0A0B0") else Color.parseColor("#777777")
            canvas.drawText("Empower your lifestyle with FitPal • Daily Nutrition Goals Tracker", (width / 2).toFloat(), 1140f, textPaint)

            // Save Bitmap to Cache Directory
            val imagesFolder = File(context.cacheDir, "shared_images")
            if (!imagesFolder.exists()) {
                imagesFolder.mkdirs()
            }
            val file = File(imagesFolder, "fitpal_nutrition_report.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            // Get Content Uri via FileProvider
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Create Share Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "FitPal Nutrition Report")
                putExtra(Intent.EXTRA_TEXT, "Here is my daily FitPal Nutrition Report! 🍏\nTarget: $calorieGoal kcal\nProtein: ${proteinGrams.toInt()}g / ${proteinGoal}g\nCarbs: ${carbsGrams.toInt()}g / ${carbsGoal}g\nFat: ${fatGrams.toInt()}g / ${fatGoal}g")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Daily Report"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing report: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
