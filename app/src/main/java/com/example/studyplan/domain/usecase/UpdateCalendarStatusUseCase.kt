package com.yourorg.studyplanner.domain.usecase

import com.yourorg.studyplanner.domain.model.*

class UpdateCalendarStatusUseCase(
    private val planRepository: PlanRepository
) {
    suspend operator fun invoke(
        userId: String,
        date: Long,
        status: CalendarStatus,
        nextDayScoreBonus: Int = 0
    ): Result<Unit> {
        return try {
            planRepository.updateCalendarStatus(userId, date, status)

            // 완료 시 다음날 점수 +10%, 미완료 시 그대로
            if (status == CalendarStatus.DONE) {
                planRepository.applyScoreBonus(userId, date, 10)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}