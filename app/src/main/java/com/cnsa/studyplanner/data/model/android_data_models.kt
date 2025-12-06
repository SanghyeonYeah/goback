package com.cnsa.studyplanner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.Date

// ============= User 관련 =============
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0,
    val username: String,
    val email: String,
    val studentNumber: String,
    val diploma: String,
    val grade: Int,
    val googleId: String? = null,
    val createdAt: Date = Date(),
    val lastLogin: Date? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    @SerializedName("student_number")
    val studentNumber: String,
    val diploma: String,
    val grade: Int,
    val email: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    @SerializedName("user_id")
    val userId: Int? = null,
    @SerializedName("session_id")
    val sessionId: String? = null,
    val user: User? = null
)

// ============= Goal 관련 =============
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val goalId: Int = 0,
    val userId: Int,
    val seasonId: Int,
    val subject: String,
    val targetGrade: Int,  // 1-5
    val startDate: Date,
    val endDate: Date,
    val achieved: Boolean = false,
    val createdAt: Date = Date()
)

data class GoalRequest(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("season_id")
    val seasonId: Int,
    val goals: Map<String, Int>  // subject -> target grade
)

// ============= Todo 관련 =============
@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val todoId: Int = 0,
    val userId: Int,
    val goalId: Int? = null,
    val subject: String,
    val title: String,
    val description: String? = null,
    val activityType: ActivityType,
    val difficulty: Difficulty,
    val estimatedTime: Int,  // 분 단위
    val actualTime: Int? = null,
    val scheduledDate: Date,
    val completed: Boolean = false,
    val completedAt: Date? = null,
    val createdAt: Date = Date()
)

enum class ActivityType {
    @SerializedName("문제풀이")
    PROBLEM_SOLVING,
    @SerializedName("개념학습")
    CONCEPT_LEARNING,
    @SerializedName("복습")
    REVIEW,
    @SerializedName("모의고사")
    MOCK_EXAM
}

enum class Difficulty {
    @SerializedName("쉬움")
    EASY,
    @SerializedName("보통")
    MEDIUM,
    @SerializedName("어려움")
    HARD
}

// ============= Study Data 관련 =============
@Entity(tableName = "study_data")
data class StudyData(
    @PrimaryKey(autoGenerate = true)
    val studyId: Int = 0,
    val userId: Int,
    val subject: String,
    val activityType: ActivityType,
    val difficulty: Difficulty,
    val startTime: Date,
    val endTime: Date,
    val estimatedTime: Int,
    val actualTime: Int,
    val performanceScore: Float? = null,
    val createdAt: Date = Date()
)

// ============= Problem 관련 =============
@Entity(tableName = "problems")
data class Problem(
    @PrimaryKey(autoGenerate = true)
    val problemId: Int = 0,
    val seasonId: Int,
    val subject: String,
    val questionText: String,
    val questionImageUrl: String? = null,
    val option1: String,
    val option2: String,
    val option3: String? = null,
    val option4: String? = null,
    val option5: String? = null,
    val correctAnswer: Int,  // 1-5
    val difficulty: Difficulty,
    val basePoints: Int,  // 2-5점
    val problemType: ProblemType = ProblemType.MOCK_EXAM,
    val createdAt: Date = Date()
)

enum class ProblemType {
    @SerializedName("모의고사")
    MOCK_EXAM,
    @SerializedName("문제집")
    WORKBOOK
}

data class ProblemAttempt(
    val attemptId: Int = 0,
    val userId: Int,
    val problemId: Int,
    val selectedAnswer: Int,
    val isCorrect: Boolean,
    val pointsEarned: Float,
    val timeSpent: Int? = null,  // 초 단위
    val attemptedAt: Date = Date()
)

data class ProblemSubmission(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("problem_id")
    val problemId: Int,
    @SerializedName("selected_answer")
    val selectedAnswer: Int,
    @SerializedName("time_spent")
    val timeSpent: Int? = null
)

data class ProblemResult(
    val success: Boolean,
    @SerializedName("is_correct")
    val isCorrect: Boolean,
    @SerializedName("points_earned")
    val pointsEarned: Float,
    @SerializedName("correct_answer")
    val correctAnswer: Int,
    val explanation: String? = null
)

// ============= Ranking 관련 =============
data class SeasonRanking(
    val rankingId: Int = 0,
    val userId: Int,
    val username: String,
    val diploma: String,
    val seasonId: Int,
    val totalPoints: Float,
    val dailyPoints: Float,
    val rankPosition: Int,
    val rankChange: Int = 0,  // 순위 변동
    val lastUpdated: Date = Date()
)

data class DailyRanking(
    val dailyRankingId: Int = 0,
    val userId: Int,
    val username: String,
    val diploma: String,
    val seasonId: Int,
    val rankingDate: Date,
    val dailyPoints: Float,
    val rankPosition: Int,
    val rankChange: Int = 0
)

data class RankingResponse(
    val success: Boolean,
    val rankings: List<SeasonRanking>,
    @SerializedName("user_ranking")
    val userRanking: SeasonRanking? = null
)

// ============= Season 관련 =============
@Entity(tableName = "seasons")
data class Season(
    @PrimaryKey(autoGenerate = true)
    val seasonId: Int = 0,
    val seasonName: String,
    val startDate: Date,
    val endDate: Date,
    val isActive: Boolean = true,
    val createdAt: Date = Date()
)

data class SeasonResponse(
    val success: Boolean,
    val season: Season? = null,
    val seasons: List<Season>? = null
)

// ============= PVP 관련 =============
data class PvpMatch(
    val matchId: Int = 0,
    val seasonId: Int,
    val player1Id: Int,
    val player1Name: String,
    val player2Id: Int,
    val player2Name: String,
    val problemId: Int,
    val winnerId: Int? = null,
    val player1Answer: Int? = null,
    val player2Answer: Int? = null,
    val player1Time: Int? = null,
    val player2Time: Int? = null,
    val matchStatus: MatchStatus = MatchStatus.WAITING,
    val startedAt: Date = Date(),
    val completedAt: Date? = null
)

enum class MatchStatus {
    @SerializedName("waiting")
    WAITING,
    @SerializedName("in_progress")
    IN_PROGRESS,
    @SerializedName("completed")
    COMPLETED
}

data class PvpRequest(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("season_id")
    val seasonId: Int
)

data class PvpMatchResponse(
    val success: Boolean,
    val match: PvpMatch? = null,
    val problem: Problem? = null,
    val message: String? = null
)

data class PvpSubmission(
    @SerializedName("match_id")
    val matchId: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("selected_answer")
    val selectedAnswer: Int,
    @SerializedName("time_spent")
    val timeSpent: Int
)

data class PvpResult(
    val success: Boolean,
    val winner: String,  // "player1", "player2", "draw"
    @SerializedName("player1_correct")
    val player1Correct: Boolean,
    @SerializedName("player2_correct")
    val player2Correct: Boolean,
    @SerializedName("correct_answer")
    val correctAnswer: Int
)

// ============= Calendar 관련 =============
@Entity(tableName = "calendar_records")
data class CalendarRecord(
    @PrimaryKey(autoGenerate = true)
    val recordId: Int = 0,
    val userId: Int,
    val recordDate: Date,
    val allCompleted: Boolean,
    val completionRate: Float,
    val totalTasks: Int,
    val completedTasks: Int,
    val bonusApplied: Boolean = false,
    val createdAt: Date = Date()
)

data class CalendarData(
    val date: Date,
    val status: CalendarStatus,
    val completionRate: Float,
    val totalTasks: Int,
    val completedTasks: Int
)

enum class CalendarStatus {
    COMPLETE,    // 완
    INCOMPLETE,  // 실
    PARTIAL,     // 부분 완료
    NONE         // 데이터 없음
}

// ============= Report 관련 =============
data class SeasonReport(
    val reportId: Int = 0,
    val userId: Int,
    val seasonId: Int,
    val goalId: Int,
    val subject: String,
    val targetGrade: Int,
    val achievedGrade: Int? = null,
    val goalAchieved: Boolean = false,
    val totalStudyTime: Int,  // 분
    val totalProblemsSolved: Int,
    val averageAccuracy: Float? = null,
    val createdAt: Date = Date()
)

data class SeasonReportResponse(
    val success: Boolean,
    val reports: List<SeasonReport>,
    @SerializedName("overall_achievement")
    val overallAchievement: Float,
    @SerializedName("total_study_time")
    val totalStudyTime: Int,
    @SerializedName("improvement_suggestions")
    val improvementSuggestions: List<String>
)

// ============= API Response 공통 =============
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
)

// ============= UI 모델 =============
data class HomeData(
    val user: User,
    val dailyRanking: DailyRanking?,
    val seasonRanking: SeasonRanking?,
    val studyProgress: StudyProgress,
    val inProgressTasks: List<Todo>,
    val goalEndDate: Date?
)

data class StudyProgress(
    val title: String,
    val percentage: Int,
    val completedTasks: Int,
    val totalTasks: Int
)

data class SubjectProgress(
    val subject: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val percentage: Int,
    val color: Int
)

// ============= AI 모델 입력/출력 =============
data class AIModelInput(
    val features: FloatArray,
    val modelType: AIModelType
)

enum class AIModelType {
    VAE,
    TRANSFORMER,
    GNN,
    DQN
}

data class StudyPlanRecommendation(
    val todos: List<Todo>,
    val subjectAllocation: Map<String, Int>,  // subject -> minutes
    val confidence: Float,
    val generatedBy: AIModelType
)
