package com.yourorg.studyplanner.domain.model

data class Problem(
    val id: String,
    val subject: String,
    val source: String, // 모의고사, 문제집
    val content: String,
    val options: List<String>,
    val correctAnswer: Int,
    val points: Int, // 2~5
    val season: Int,
    val createdAt: Long
)

data class Submission(
    val id: String,
    val userId: String,
    val problemId: String,
    val selectedAnswer: Int,
    val isCorrect: Boolean,
    val score: Int, // 디플로마별 조정된 점수
    val submittedAt: Long
)