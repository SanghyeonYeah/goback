package com.yourorg.studyplanner.domain.usecase

import com.yourorg.studyplanner.domain.model.Problem

class StartPvpMatchUseCase(
    private val pvpRepository: PvpRepository,
    private val problemRepository: ProblemRepository
) {
    suspend operator fun invoke(
        userId: String,
        opponentId: String
    ): Result<Pair<Problem, Long>> { // Problem과 제한시간(밀리초)
        return try {
            val randomProblem = problemRepository.getRandomProblem().getOrThrow()
            val timeLimit = 5 * 60 * 1000L // 5분

            pvpRepository.createMatch(userId, opponentId, randomProblem.id)

            Result.success(Pair(randomProblem, timeLimit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}