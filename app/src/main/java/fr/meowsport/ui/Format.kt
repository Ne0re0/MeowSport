package fr.meowsport.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 01:02:03 or 02:03. */
fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** 1 h 05, 45 min. */
fun formatDuration(ms: Long): String {
    val minutes = (ms / 60_000).coerceAtLeast(0)
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "$m min"
        m == 0L -> "$h h"
        else -> "%d h %02d".format(h, m)
    }
}

/** 1:30 for 90 seconds, 45 s for 45. */
fun formatRest(sec: Int): String =
    if (sec < 60) "$sec s" else "%d:%02d".format(sec / 60, sec % 60)

private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.FRANCE)
private val longDateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy · HH:mm", Locale.FRANCE)

fun formatDate(ms: Long): String =
    dateFormatter.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())).replaceFirstChar { it.uppercase() }

fun formatLongDate(ms: Long): String =
    longDateFormatter.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())).replaceFirstChar { it.uppercase() }

fun formatAgo(ms: Long, now: Long = System.currentTimeMillis()): String {
    val days = ((now - ms) / 86_400_000L).toInt()
    return when {
        days <= 0 -> "aujourd'hui"
        days == 1 -> "hier"
        days < 7 -> "il y a $days jours"
        days < 14 -> "il y a 1 semaine"
        days < 60 -> "il y a ${days / 7} semaines"
        else -> "il y a ${days / 30} mois"
    }
}

fun formatVolume(kg: Double): String =
    if (kg >= 1000) String.format(Locale.FRANCE, "%.1f t", kg / 1000) else "${kg.toInt()} kg"
