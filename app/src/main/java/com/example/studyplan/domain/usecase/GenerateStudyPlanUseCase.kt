package com.yourorg.studyplanner.domain.usecase

import com.yourorg.studyplanner.domain.model.*
import kotlinx.coroutines.flow.Flow

class GenerateStudyPlanUseCase(
    private val planRepository: PlanRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): Flow<Result<StudyPlan>> {
        return planRepository.generatePlan(userId)
    }
}

// 학습 시간 배분 로직
fun allocateStudyTime(
    user: User,
    dailyPlan: DailyPlan,
    dailyStudyHours: Int
): List<SubjectPlan> {
    val subjectPlans = mutableListOf<SubjectPlan>()

    // 목표 등급별 가중치 계산
    val weights = user.targetGrades.mapValues { (_, grade) ->
        (10 - grade).toDouble() / 10.0 // 낮은 등급(높은 목표)에 더 많은 시간 배분
    }

    val totalWeight = weights.values.sum()
    val availableMinutes = dailyStudyHours * 60

    weights.forEach { (subject, weight) ->
        val allocatedMinutes = (availableMinutes * (weight / totalWeight)).toInt()
        subjectPlans.add(
            SubjectPlan(
                subject = subject,
                allocatedTime = allocatedMinutes,
                priority = (10 - user.targetGrades[subject]!!)
            )
        )
    }

    return subjectPlans.sortedByDescending { it.priority }
}