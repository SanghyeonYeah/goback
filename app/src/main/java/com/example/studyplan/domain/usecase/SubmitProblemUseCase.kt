package com.yourorg.studyplanner.domain.usecase

import com.yourorg.studyplanner.domain.model.*

class SubmitProblemUseCase(
    private val problemRepository: ProblemRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        userId: String,
        problemId: String,
        selectedAnswer: Int
    ): Result<Submission> {
        return try {
            val problem = problemRepository.getProblem(problemId).getOrThrow()
            val user = userRepository.getUser(userId).getOrThrow()

            val isCorrect = problem.correctAnswer == selectedAnswer
            var score = if (isCorrect) problem.points else 0

            // 디플로마별 점수 조정
            score = adjustScoreByDiploma(score, problem.subject, user.diploma)

            val submission = Submission(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                problemId = problemId,
                selectedAnswer = selectedAnswer,
                isCorrect = isCorrect,
                score = score,
                submittedAt = System.currentTimeMillis()
            )

            problemRepository.saveSubmission(submission)
            Result.success(submission)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun adjustScoreByDiploma(score: Int, subject: String, diploma: String): Int {
        val scienceSubjects = setOf("물리I", "화학I", "생명과학I")
        val humanitiesSubjects = setOf("국어", "영어", "역사")

        return when {
            diploma.contains("이과") && subject in scienceSubjects -> score
            diploma.contains("이과") && subject in humanitiesSubjects -> (score * 0.9).toInt()
            diploma.contains("문과") && subject in humanitiesSubjects -> score
            diploma.contains("문과") && subject in scienceSubjects -> (score * 1.1).toInt()
            diploma.contains("예") -> (score * 1.1).toInt()
            else -> score
        }
    }
}