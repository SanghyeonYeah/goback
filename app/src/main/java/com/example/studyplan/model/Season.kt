package com.example.studyplanner.model

data class Season(
    val id: String,
    val name: String, // 예: "2024 가을 시즌"
    val startDate: Long,
    val endDate: Long,
    val description: String? = null,
    val isActive: Boolean = true,
    val rewardMessage: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class SeasonRequest(
    val name: String,
    val startDate: Long,
    val endDate: Long,
    val description: String? = null,
    val rewardMessage: String? = null
)

data class SeasonResponse(
    val status: Int,
    val message: String,
    val data: Season?
)

data class SeasonListResponse(
    val status: Int,
    val message: String,
    val data: List<Season>?
)

data class CalendarDay(
    val date: String, // yyyy-MM-dd
    val status: String, // "완" 또는 "실"
    val totalTodos: Int,
    val completedTodos: Int,
    val scoreBonus: Int // +10% 또는 0%
)

data class SeasonRanking(
    val rank: Int,
    val userId: String,
    val totalScore: Int,
    val goalAchievementCount: Int,
    val seasonId: String
)

data class DailyRanking(
    val rank: Int,
    val userId: String,
    val dailyScore: Int,
    val date: String
)