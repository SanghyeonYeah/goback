package com.yourorg.studyplanner.data.repository

import com.yourorg.studyplanner.data.local.db.CalendarDao
import com.yourorg.studyplanner.data.remote.ApiService
import com.yourorg.studyplanner.domain.model.CalendarCell
import javax.inject.Inject

/**
 * Plan Repository
 * 학습 계획 생성, 조회, 업데이트 관리
 */
class PlanRepository @Inject constructor(
    private val calendarDao: CalendarDao,
    private val apiService: ApiService
) {

    /**
     * 학습 계획 생성
     * VAE, Transformer, GNN, DQN 모델 활용
     */
    suspend fun generateStudyPlan(
        userId: String,
        grade: Int,
        diploma: String,
        targetGrades: Map<String, Int>,
        availableStudyHours: Float,
        isWeekdayInSession: Boolean,
        weekdaySessionHours: Float? = null,
        weekendStudyHours: Float? = null
    ): Result<List<CalendarCell>> = try {
        val response = apiService.generatePlan(
            userId,
            grade,
            diploma,
            targetGrades,
            availableStudyHours,
            isWeekdayInSession,
            weekdaySessionHours,
            weekendStudyHours
        )

        val calendarCells = response.planData.map { it.toCalendarCell() }
        calendarCells.forEach { calendarDao.insertCalendarCell(it) }

        Result.success(calendarCells)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 캘린더 셀 상태 업데이트 (완/실)
     */
    suspend fun updateCalendarStatus(
        userId: String,
        date: String,
        status: String, // "완" or "실"
        completionPercentage: Int
    ): Result<CalendarCell> = try {
        val response = apiService.updateCalendarStatus(
            userId,
            date,
            status,
            completionPercentage
        )

        val cell = response.toCalendarCell()
        calendarDao.updateCalendarCell(cell)

        Result.success(cell)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 사용자의 캘린더 전체 조회
     */
    suspend fun getUserCalendar(userId: String, startDate: String, endDate: String): Result<List<CalendarCell>> = try {
        val cells = calendarDao.getUserCalendar(userId, startDate, endDate)
        Result.success(cells)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 현재 월의 캘린더 조회
     */
    suspend fun getCurrentMonthCalendar(userId: String): Result<List<CalendarCell>> = try {
        val cells = calendarDao.getCurrentMonthCalendar(userId)
        Result.success(cells)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 시즌 종료 후 목표 달성 여부 확인
     */
    suspend fun checkSeasonGoalAchievement(userId: String, seasonId: String): Result<Map<String, Boolean>> = try {
        val response = apiService.checkSeasonGoals(userId, seasonId)
        Result.success(response.achievedGoals)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 보완 학습 계획 생성 (목표 미달성 과목 기반)
     */
    suspend fun generateSupplementaryPlan(
        userId: String,
        supplementarySubjects: List<String>,
        availableStudyHours: Float
    ): Result<List<CalendarCell>> = try {
        val response = apiService.generateSupplementaryPlan(
            userId,
            supplementarySubjects,
            availableStudyHours
        )

        val calendarCells = response.planData.map { it.toCalendarCell() }
        calendarCells.forEach { calendarDao.insertCalendarCell(it) }

        Result.success(calendarCells)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 학습 완료 시 다음날 문제 점수 +10% 보너스 적용
     */
    suspend fun applyCompletionBonus(userId: String, targetDate: String): Result<Unit> = try {
        apiService.applyCompletionBonus(userId, targetDate)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 계획 조정 (목표 등급 변경 시)
     */
    suspend fun adjustPlan(
        userId: String,
        updatedTargetGrades: Map<String, Int>
    ): Result<List<CalendarCell>> = try {
        val response = apiService.adjustPlan(userId, updatedTargetGrades)
        val calendarCells = response.planData.map { it.toCalendarCell() }
        calendarCells.forEach { calendarDao.updateCalendarCell(it) }

        Result.success(calendarCells)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 로컬 캘린더 셀 조회
     */
    fun getCalendarCellLocal(userId: String, date: String) =
        calendarDao.getCalendarCell(userId, date)
}