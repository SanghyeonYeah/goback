package com.example.studyplanner.model

data class Problem(
    val id: String,
    val seasonId: String,
    val subject: String, // 국어, 수학, 사회, 과학, 영어, 역사, 물리, 화학, 생명과학
    val content: String,
    val imageUrl: String? = null,
    val options: List<String>? = null, // 객관식 선택지
    val correctAnswer: String,
    val explanation: String? = null,
    val source: String, // 모의고사, 문제집
    val points: Int, // 2-5점
    val createdAt: Long,
    val updatedAt: Long
)

data class ProblemRequest(
    val seasonId: String,
    val subject: String,
    val content: String,
    val imageUrl: String? = null,
    val options: List<String>? = null,
    val correctAnswer: String,
    val explanation: String? = null,
    val source: String,
    val points: Int
)

data class ProblemResponse(
    val status: Int,
    val message: String,
    val data: Problem?
)

data class ProblemListResponse(
    val status: Int,
    val message: String,
    val data: List<Problem>?
)

data class ProblemSolveRequest(
    val problemId: String,
    val userId: String,
    val answer: String,
    val timeSpentSeconds: Int
)

data class ProblemSolveResponse(
    val status: Int,
    val message: String,
    val data: SolveResult?
)

data class SolveResult(
    val isCorrect: Boolean,
    val pointsEarned: Int,
    val explanation: String? = null
)