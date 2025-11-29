package com.yourorg.studyplanner.data.remote

import retrofit2.http.*

/**
 * API Service Interface
 * 기본 API 엔드포인트 정의
 */
interface ApiService {

    /**
     * 학습 계획 생성
     */
    @POST("api/v1/plan/generate")
    suspend fun generatePlan(
        @Query("userId") userId: String,
        @Query("grade") grade: Int,
        @Query("diploma") diploma: String,
        @Body targetGrades: Map<String, Int>,
        @Query("availableHours") availableStudyHours: Float,
        @Query("isInSession") isWeekdayInSession: Boolean,
        @Query("sessionHours") weekdaySessionHours: Float?,
        @Query("weekendHours") weekendStudyHours: Float?
    ): PlanResponse

    /**
     * 캘린더 상태 업데이트
     */
    @PATCH("api/v1/calendar/{date}")
    suspend fun updateCalendarStatus(
        @Query("userId") userId: String,
        @Path("date") date: String,
        @Query("status") status: String,
        @Query("completion") completionPercentage: Int
    ): CalendarResponse

    /**
     * 시즌 목표 달성 여부 확인
     */
    @GET("api/v1/season/{seasonId}/goals")
    suspend fun checkSeasonGoals(
        @Query("userId") userId: String,
        @Path("seasonId") seasonId: String
    ): SeasonGoalResponse

    /**
     * 보완 학습 계획 생성
     */
    @POST("api/v1/plan/supplementary")
    suspend fun generateSupplementaryPlan(
        @Query("userId") userId: String,
        @Body subjects: List<String>,
        @Query("hours") availableStudyHours: Float
    ): PlanResponse

    /**
     * 학습 완료 보너스 적용
     */
    @POST("api/v1/bonus/completion")
    suspend fun applyCompletionBonus(
        @Query("userId") userId: String,
        @Query("date") targetDate: String
    ): BonusResponse

    /**
     * 계획 조정
     */
    @PATCH("api/v1/plan/adjust")
    suspend fun adjustPlan(
        @Query("userId") userId: String,
        @Body updatedTargetGrades: Map<String, Int>
    ): PlanResponse
}

// Response Models
data class PlanResponse(
    val planData: List<PlanData>
)

data class PlanData(
    val date: String,
    val dayOfWeek: String,
    val subjects: List<SubjectPlan>,
    val totalHours: Float,
    val status: String // "예정", "완", "실"
) {
    fun toCalendarCell() = CalendarCell(
        date = date,
        dayOfWeek = dayOfWeek,
        subjects = subjects,
        status = status
    )
}

data class SubjectPlan(
    val subject: String,
    val hours: Float,
    val chapters: List<String>
)

data class CalendarCell(
    val date: String,
    val dayOfWeek: String,
    val subjects: List<SubjectPlan>,
    val status: String
)

data class CalendarResponse(
    val cell: CalendarData
) {
    fun toCalendarCell() = cell.toCalendarCell()
}

data class CalendarData(
    val date: String,
    val status: String,
    val completionPercentage: Int
) {
    fun toCalendarCell() = CalendarCell(
        date = date,
        dayOfWeek = "",
        subjects = emptyList(),
        status = status
    )
}

data class SeasonGoalResponse(
    val achievedGoals: Map<String, Boolean>
)

data class BonusResponse(
    val bonusApplied: Boolean,
    val nextDayBonusPercentage: Int
)