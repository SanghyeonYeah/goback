package com.yourorg.studyplanner.data.repository

import com.yourorg.studyplanner.data.local.db.ProblemDao
import com.yourorg.studyplanner.data.local.db.SubmissionDao
import com.yourorg.studyplanner.data.remote.ProblemApi
import com.yourorg.studyplanner.domain.model.Problem
import com.yourorg.studyplanner.domain.model.Submission
import javax.inject.Inject

/**
 * Problem Repository
 * 문제 조회, 풀이 제출, 채점 결과 관리
 */
class ProblemRepository @Inject constructor(
    private val problemDao: ProblemDao,
    private val submissionDao: SubmissionDao,
    private val problemApi: ProblemApi
) {

    /**
     * 모의고사 문제 조회
     */
    suspend fun getMockExamProblems(
        subject: String,
        difficulty: Int = 3
    ): Result<List<Problem>> = try {
        val response = problemApi.getMockExamProblems(subject, difficulty)
        val problems = response.problems.map { it.toProblem() }
        problems.forEach { problemDao.insertProblem(it) }
        Result.success(problems)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * PVP용 랜덤 문제 조회
     */
    suspend fun getRandomProblemForPvp(
        subject: String? = null,
        timeLimit: Int = 300 // 5분 기본값
    ): Result<Problem> = try {
        val response = problemApi.getRandomProblem(subject, timeLimit)
        val problem = response.problem.toProblem()
        problemDao.insertProblem(problem)
        Result.success(problem)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 특정 문제 상세 조회
     */
    suspend fun getProblemDetail(problemId: String): Result<Problem> = try {
        val problem = problemDao.getProblem(problemId)
        if (problem != null) {
            Result.success(problem)
        } else {
            val response = problemApi.getProblem(problemId)
            val newProblem = response.toProblem()
            problemDao.insertProblem(newProblem)
            Result.success(newProblem)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 문제 풀이 제출
     */
    suspend fun submitProblemAnswer(
        userId: String,
        problemId: String,
        selectedAnswer: String,
        timeSpent: Int // 풀이 소요 시간 (초)
    ): Result<Submission> = try {
        val response = problemApi.submitAnswer(
            userId,
            problemId,
            selectedAnswer,
            timeSpent
        )

        val submission = response.toSubmission()
        submissionDao.insertSubmission(submission)

        Result.success(submission)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 채점 결과 조회
     */
    suspend fun getSubmissionResult(submissionId: String): Result<Submission> = try {
        val submission = submissionDao.getSubmission(submissionId)
        Result.success(submission)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 사용자의 풀이 기록 조회
     */
    suspend fun getUserSubmissions(
        userId: String,
        subject: String? = null,
        limit: Int = 50
    ): Result<List<Submission>> = try {
        val submissions = submissionDao.getUserSubmissions(userId, subject, limit)
        Result.success(submissions)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 과목별 정확도 통계
     */
    suspend fun getAccuracyBySubject(userId: String): Result<Map<String, Float>> = try {
        val submissions = submissionDao.getUserSubmissions(userId)

        val accuracy = mutableMapOf<String, Float>()
        val subjectGroups = submissions.groupBy { it.subject }

        subjectGroups.forEach { (subject, submissionList) ->
            val correctCount = submissionList.count { it.isCorrect }
            val rate = if (submissionList.isNotEmpty()) {
                correctCount.toFloat() / submissionList.size
            } else {
                0f
            }
            accuracy[subject] = rate
        }

        Result.success(accuracy)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 점수 계산 (디플로마 보정 적용)
     */
    fun calculateScoreWithDiplomaBonus(
        baseScore: Int,
        diploma: String,
        subject: String
    ): Int {
        return when (diploma) {
            "IT", "공학", "수학", "물리", "화학", "생명과학", "IB(자연)" -> {
                // 이과
                when (subject) {
                    in listOf("물리I", "화학I", "생명과학I") -> baseScore
                    in listOf("국어", "사회", "역사") -> (baseScore * 0.9).toInt()
                    else -> baseScore
                }
            }
            "인문학(문학/사학/철학)", "국제어문", "사회과학", "경제경영", "IB(인문)" -> {
                // 문과
                when (subject) {
                    in listOf("국어", "사회", "역사") -> baseScore
                    in listOf("물리I", "화학I", "생명과학I") -> (baseScore * 1.1).toInt()
                    else -> baseScore
                }
            }
            "예술", "체육" -> {
                // 예체
                (baseScore * 1.1).toInt()
            }
            else -> baseScore
        }
    }

    /**
     * 로컬에서 문제 조회
     */
    fun getProblemLocal(problemId: String) = problemDao.getProblem(problemId)
}