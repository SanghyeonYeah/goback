package com.yourorg.studyplanner.domain.model

data class CalendarCell(
    val date: Long,
    val status: CalendarStatus, // DONE, FAIL, PENDING
    val dailyScore: Int,
    val targetAchievements: Map<String, Boolean>
)

enum class CalendarStatus {
    DONE, FAIL, PENDING
}