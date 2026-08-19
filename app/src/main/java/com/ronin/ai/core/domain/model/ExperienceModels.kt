package com.ronin.ai.core.domain.model

enum class ExperienceCategory(val label: String) {
    ERROR("Error"),
    SOLUTION("Solution"),
    FIX("Successful fix"),
    PREFERENCE("Preference")
}

/**
 * One record in RONIN's self-improvement system: something that failed,
 * how it was solved, or a preference discovered while working.
 */
data class ExperienceItem(
    val id: Long = 0L,
    val category: ExperienceCategory,
    val title: String,
    val detail: String,
    val context: String = "",
    val resolved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
