package com.akitech.gallery.ui.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object Formatters {
    fun formatPhotoDate(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return "Unknown"
        val date = Date(epochSeconds * 1000L)
        return DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            Locale.getDefault(),
        ).format(date)
    }

    fun formatCompactDate(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return ""
        val date = Date(epochSeconds * 1000L)
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
    }

    fun formatSectionDate(epochSeconds: Long, nowMillis: Long = System.currentTimeMillis()): String {
        if (epochSeconds <= 0L) return "Earlier"
        val photoCal = Calendar.getInstance().apply { timeInMillis = epochSeconds * 1000L }
        val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }

        val sameYear = photoCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
        val todayDay = todayCal.get(Calendar.DAY_OF_YEAR)
        val photoDay = photoCal.get(Calendar.DAY_OF_YEAR)

        return when {
            sameYear && photoDay == todayDay -> "Today"
            sameYear && photoDay == todayDay - 1 -> "Yesterday"
            sameYear && todayDay - photoDay in 2..6 ->
                SimpleDateFormat("EEEE", Locale.getDefault()).format(photoCal.time)
            sameYear ->
                SimpleDateFormat("MMMM", Locale.getDefault()).format(photoCal.time)
            else ->
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(photoCal.time)
        }
    }

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "0:00"
        val totalSeconds = durationMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val unit = 1024.0
        if (bytes < unit) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(unit)).toInt().coerceAtMost(4)
        val suffix = "KMGT"[exp - 1]
        val value = bytes / unit.pow(exp.toDouble())
        return String.format(Locale.getDefault(), "%.2f %sB", value, suffix)
    }

    fun formatResolution(width: Int, height: Int): String {
        return if (width > 0 && height > 0) {
            "$width × $height px"
        } else {
            "Unknown"
        }
    }
}
