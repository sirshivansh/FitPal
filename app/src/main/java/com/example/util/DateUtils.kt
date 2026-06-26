package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US)
    private val shortDateFormat = SimpleDateFormat("MMM dd", Locale.US)

    fun getTodayDateString(): String {
        return dbDateFormat.format(Date())
    }

    fun formatDateForDb(date: Date): String {
        return dbDateFormat.format(date)
    }

    fun formatDateForDisplay(dateString: String): String {
        return try {
            val date = dbDateFormat.parse(dateString)
            if (date != null) {
                displayDateFormat.format(date)
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }

    fun formatDateForDisplayShort(dateString: String): String {
        return try {
            val date = dbDateFormat.parse(dateString)
            if (date != null) {
                shortDateFormat.format(date)
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }

    fun getPreviousDay(dateString: String): String {
        return addDays(dateString, -1)
    }

    fun getNextDay(dateString: String): String {
        return addDays(dateString, 1)
    }

    fun addDays(dateString: String, days: Int): String {
        return try {
            val date = dbDateFormat.parse(dateString) ?: Date()
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.DAY_OF_YEAR, days)
            dbDateFormat.format(calendar.time)
        } catch (e: Exception) {
            dateString
        }
    }

    fun getDaysRange(days: Int): List<String> {
        val list = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1))
        for (i in 0 until days) {
            list.add(dbDateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }
}
