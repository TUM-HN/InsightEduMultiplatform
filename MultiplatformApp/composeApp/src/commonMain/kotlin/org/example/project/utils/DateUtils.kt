package org.example.project.utils

import kotlinx.datetime.LocalDateTime

fun formatAttendanceDate(dateString: String): String {
    return try {
        val sanitized = if (dateString.contains(":")) {
            val parts = dateString.split(":")
            if (parts.size > 3) {
                parts.take(3).joinToString(":") + "." + parts.drop(3).joinToString("")
            } else {
                dateString
            }
        } else {
            dateString
        }

        val dateTime = LocalDateTime.parse(sanitized)
        val dayOfWeek = dateTime.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        val dayOfMonth = dateTime.dayOfMonth
        val month = dateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')

        "$dayOfWeek $dayOfMonth $month $hour:$minute"
    } catch (e: Exception) {
        dateString
    }
}
