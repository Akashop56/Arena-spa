package com.ronin.ai.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object Constants {
    const val APP_VERSION = "2.0.0"
    const val ASSISTANT_DEFAULT_NAME = "RONIN"

    /** Maximum conversation messages kept in short-term memory. */
    const val MAX_CONVERSATION_MESSAGES = 80

    /** AI request timeout (network level). */
    const val AI_READ_TIMEOUT_SECONDS = 60L
    const val AI_TEST_READ_TIMEOUT_SECONDS = 30L

    const val MAX_OUTPUT_TOKENS = 1024

    /** Notification channel + ids */
    const val NOTIFICATION_CHANNEL_ID = "ronin_assistant"
    const val NOTIFICATION_SOURCE = "RONIN"
}

object TimeFormat {
    private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

    fun clock(timestamp: Long): String = clockFormat.format(Date(timestamp))

    fun dateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    fun day(timestamp: Long): String = dayFormat.format(Date(timestamp))

    fun relative(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - timestamp
        return when {
            diff < 60_000L -> "just now"
            diff < 3_600_000L -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            diff < 86_400_000L -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
            diff < 604_800_000L -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
            else -> day(timestamp)
        }
    }
}

/** Returns the first N words of a string (for memory titles etc.). */
fun String.firstWords(n: Int): String {
    val words = trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val taken = words.take(n).joinToString(" ")
    return if (words.size > n) "$taken…" else taken
}

/** Extracts searchable keywords (2+ chars, lowercased) from a query. */
fun String.keywords(): List<String> =
    lowercase(Locale.ROOT)
        .split(Regex("[^a-z0-9\\u0900-\\u097F]+"))
        .filter { it.length >= 2 }
        .distinct()
