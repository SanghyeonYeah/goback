package com.yourorg.studyplanner.domain.model

data class StudyPlan(
    val id: String,
    val userId: String,
    val season: Int,
    val plans: List<DailyPlan>,
    val createdAt: Long,
    val updatedAt: Long
)

data class DailyPlan(
    val date: Long, // timestamp
    val dayOfWeek: Int, // 1=Monday, 7=Sunday
    val isWeekday: Boolean,
    val hasStudySession: Boolean, // 평일 면학 여부
    val studySessionTime: Int, // 면학 시간 (분)
    val subjectPlans: List<SubjectPlan>
)

data class SubjectPlan(
    val subject: String,
    val allocatedTime: Int, // 분 단위
    val priority: Int
)