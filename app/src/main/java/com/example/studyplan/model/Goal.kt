package com.example.studyplanner.model

data class Goal(
    val id: String,
    val userId: String,
    val seasonId: String,
    val korean: Int, // 목표 등급 (1-9)
    val math: Int,
    val social: Int,
    val science: Int, // 통합과학 (1학년) 또는 선택 과목 (2학년)
    val english: Int,
    val history: Int,
    val physics: Int? = null, // 2학년 이상
    val chemistry: Int? = null, // 2학년 이상
    val biology: Int? = null, // 2학년 이상
    val startDate: Long,
    val endDate: Long,
    val achievementRate: Double = 0.0, // 달성률
    val isAchieved: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

data class GoalRequest(
    val korean: Int,
    val math: Int,
    val social: Int,
    val science: Int,
    val english: Int,
    val history: Int,
    val physics: Int? = null,
    val chemistry: Int? = null,
    val biology: Int? = null,
    val startDate: Long,
    val endDate: Long,
    val seasonId: String
)

data class GoalResponse(
    val status: Int,
    val message: String,
    val data: Goal?
)

data class GoalListResponse(
    val status: Int,
    val message: String,
    val data: List<Goal>?
)