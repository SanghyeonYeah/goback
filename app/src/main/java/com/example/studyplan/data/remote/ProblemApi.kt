package com.yourorg.studyplanner.data.remote

import retrofit2.http.*

/**
 * Problem API Interface
 * 문제 조회, 풀이 제출, 채점 관련 엔드포인트
 */
interface ProblemApi {

    /**
     * 모의고사 문제 조회
     */
    @GET("api/v1/problems/mock-exam")
    suspend fun getMockExamProblems(
        @Query("subject") subject: String,
        @Query("difficulty") difficulty: Int = 3
    ): ProblemsResponse

    /**
     * 랜덤 문제 조회 (PVP용)
     */
    @GET("api/v1/problems/random")
    suspend fun getRandomProblem(
        @Query("subject") subject: String? = null,
        @Query("timeLimit") timeLimit: Int = 300
    ): ProblemResponse

    /**
     * 특정 문제 상세 조회
     */
    @GET("api/v1/problems/{problemId}")
    suspend fun getProblem(
        @Path("problemId") problemId: String
    ): ProblemDetailResponse

    /**
     * 문제 풀이 제출
     */
    @POST("api/v1/problems/{problemId}/submit")
    suspend fun submitAnswer(
        @Query("userId") userId: String,
        @Path("problemId") problemId: String,
        @Query("answer") selectedAnswer: String,
        @Query("timeSpent") timeSpent: Int
    ): SubmissionResponse
}

// Response Models
data class ProblemsResponse(
    val problems: List<ProblemData>
)

data class ProblemResponse(
    val problem: ProblemData
)

data class ProblemDetailResponse(
    val problemData: ProblemData
) {
    fun toProblem() = problemData.toProblem()
}

data class ProblemData(
    val id: String,
    val subject: String,
    val topic: String,
    val difficulty: Int,
    val content: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String,
    val points: Int,
    val year: Int,
    val examType: String // "모의고사", "기출"
) {
    fun toProblem() = com.yourorg.studyplanner.domain.model.Problem(
        id = id,
        subject = subject,
        topic = topic,
        difficulty = difficulty,
        content = content,
        options = options,
        correctAnswer = correctAnswer,
        explanation = explanation,
        points = points
    )
}

data class SubmissionResponse(
    val submission: SubmissionData
) {
    fun toSubmission() = submission.toSubmission()
}

data class SubmissionData(
    val id: String,
    val userId: String,
    val problemId: String,
    val subject: String,
    val selectedAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val score: Int,
    val timeSpent: Int,
    val submittedAt: String
) {
    fun toSubmission() = com.yourorg.studyplanner.domain.model.Submission(
        id = id,
        userId = userId,
        problemId = problemId,
        subject = subject,
        selectedAnswer = selectedAnswer,
        correctAnswer = correctAnswer,
        isCorrect = isCorrect,
        score = score,
        timeSpent = timeSpent
    )
}