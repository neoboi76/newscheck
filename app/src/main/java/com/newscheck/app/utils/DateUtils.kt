package com.newscheck.app.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {

    fun formatRelative(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val instant = Instant.parse(isoString)
            val now = Instant.now()
            val minutes = ChronoUnit.MINUTES.between(instant, now)
            when {
                minutes < 1    -> "Just now"
                minutes < 60   -> "${minutes}m ago"
                minutes < 1440 -> "${minutes / 60}h ago"
                minutes < 10080 -> "${minutes / 1440}d ago"
                else -> DateTimeFormatter.ofPattern("MMM d, yyyy")
                    .withZone(ZoneId.systemDefault())
                    .format(instant)
            }
        } catch (e: Exception) { isoString ?: "" }
    }

    fun formatFull(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val instant = Instant.parse(isoString)
            DateTimeFormatter.ofPattern("MMMM d, yyyy · h:mm a")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } catch (e: Exception) { isoString ?: "" }
    }
}