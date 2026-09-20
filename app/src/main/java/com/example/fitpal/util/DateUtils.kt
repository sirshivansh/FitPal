package com.example.fitpal.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getTodayDateString(): String {
        return LocalDate.now().format(formatter)
    }

    fun getPreviousDay(dateStr: String): String {
        val date = LocalDate.parse(dateStr, formatter)
        return date.minusDays(1).format(formatter)
    }

    fun getNextDay(dateStr: String): String {
        val date = LocalDate.parse(dateStr, formatter)
        return date.plusDays(1).format(formatter)
    }

    fun formatDateForDisplay(dateStr: String): String {
        val date = LocalDate.parse(dateStr, formatter)
        return date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US))
    }

    fun formatDateForDisplayShort(dateStr: String): String {
        val date = LocalDate.parse(dateStr, formatter)
        return date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
    }

    fun getWeekStripDays(dateStr: String): List<String> {
        val date = LocalDate.parse(dateStr, formatter)
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        return (0..6).map { monday.plusDays(it.toLong()).format(formatter) }
    }

    fun getDayOfWeekLetter(dateStr: String): String {
        val date = LocalDate.parse(dateStr, formatter)
        return date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.US)
    }

    fun getDayOfMonth(dateStr: String): String {
        val date = LocalDate.parse(dateStr, formatter)
        return date.dayOfMonth.toString()
    }

    fun getLast30Days(): List<String> {
        val today = LocalDate.now()
        return (0..29).map { today.minusDays(it.toLong()).format(formatter) }.reversed()
    }

    fun getLast7Days(endDateStr: String = getTodayDateString()): List<String> {
        val end = LocalDate.parse(endDateStr, formatter)
        return (0..6).map { end.minusDays(it.toLong()).format(formatter) }.reversed()
    }

    fun getDayOfWeekShort(dateStr: String): String {
        val date = LocalDate.parse(dateStr, formatter)
        return date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
    }

    fun formatRangeForDisplay(startDateStr: String, endDateStr: String): String {
        val start = LocalDate.parse(startDateStr, formatter)
        val end = LocalDate.parse(endDateStr, formatter)
        return if (start.year == end.year) {
            "${start.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))} - ${end.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))}"
        } else {
            "${start.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))} - ${end.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))}"
        }
    }
}
