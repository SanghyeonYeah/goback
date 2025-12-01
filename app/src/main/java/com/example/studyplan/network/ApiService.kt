package com.example.studyplanner.network

import com.example.studyplanner.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ==================== Auth ====================
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("/api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @GET("/api/auth/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<UserResponse>

    @POST("/api/auth/logout")
    suspend fun logout(@Header("Authorization") token: String): Response<Map<String, Any>>

    // ==================== Goal ====================
    @POST("/api/goals")
    suspend fun createGoal(
        @Header("Authorization") token: String,
        @Body request: GoalRequest
    ): Response<GoalResponse>

    @GET("/api/goals/current")
    suspend fun getCurrentGoal(@Header("Authorization") token: String): Response<GoalResponse>

    @GET("/api/goals/{goalId}")
    suspend fun getGoal(
        @Header("Authorization") token: String,
        @Path("goalId") goalId: String
    ): Response<GoalResponse>

    @PUT("/api/goals/{goalId}")
    suspend fun updateGoal(
        @Header("Authorization") token: String,
        @Path("goalId") goalId: String,
        @Body request: GoalRequest
    ): Response<GoalResponse>

    @GET("/api/goals/season/{seasonId}")
    suspend fun getGoalBySeason(
        @Header("Authorization") token: String,
        @Path("seasonId") seasonId: String
    ): Response<GoalListResponse>

    // ==================== Todo ====================
    @POST("/api/todos")
    suspend fun createTodo(
        @Header("Authorization") token: String,
        @Body request: TodoRequest
    ): Response<TodoResponse>

    @GET("/api/todos/date/{date}")
    suspend fun getTodosByDate(
        @Header("Authorization") token: String,
        @Path("date") date: String
    ): Response<TodoListResponse>

    @GET("/api/todos/{todoId}")
    suspend fun getTodo(
        @Header("Authorization") token: String,
        @Path("todoId") todoId: String
    ): Response<TodoResponse>

    @PUT("/api/todos/{todoId}")
    suspend fun updateTodo(
        @Header("Authorization") token: String,
        @Path("todoId") todoId: String,
        @Body request: TodoRequest
    ): Response<TodoResponse>

    @PATCH("/api/todos/{todoId}/status")
    suspend fun updateTodoStatus(
        @Header("Authorization") token: String,
        @Path("todoId") todoId: String,
        @Body update: TodoStatusUpdate
    ): Response<TodoResponse>

    @DELETE("/api/todos/{todoId}")
    suspend fun deleteTodo(
        @Header("Authorization") token: String,
        @Path("todoId") todoId: String
    ): Response<Map<String, Any>>

    @GET("/api/todos/goal/{goalId}")
    suspend fun getTodosByGoal(
        @Header("Authorization") token: String,
        @Path("goalId") goalId: String
    ): Response<TodoListResponse>

    // ==================== Problem ====================
    @POST("/api/problems")
    suspend fun createProblem(
        @Header("Authorization") token: String,
        @Body request: ProblemRequest
    ): Response<ProblemResponse>

    @GET("/api/problems/season/{seasonId}")
    suspend fun getProblemsBySeason(
        @Header("Authorization") token: String,
        @Path("seasonId") seasonId: String
    ): Response<ProblemListResponse>

    @GET("/api/problems/{problemId}")
    suspend fun getProblem(
        @Header("Authorization") token: String,
        @Path("problemId") problemId: String
    ): Response<ProblemResponse>

    @GET("/api/problems/subject/{subject}")
    suspend fun getProblemsBySubject(
        @Header("Authorization") token: String,
        @Path("subject") subject: String,
        @Query("seasonId") seasonId: String
    ): Response<ProblemListResponse>

    @GET("/api/problems/random")
    suspend fun getRandomProblem(
        @Header("Authorization") token: String,
        @Query("seasonId") seasonId: String
    ): Response<ProblemResponse>

    @POST("/api/problems/solve")
    suspend fun solveProblem(
        @Header("Authorization") token: String,
        @Body request: ProblemSolveRequest
    ): Response<ProblemSolveResponse>

    // ==================== Ranking ====================
    @GET("/api/rankings/season/{seasonId}")
    suspend fun getSeasonRanking(
        @Header("Authorization") token: String,
        @Path("seasonId") seasonId: String
    ): Response<Map<String, Any>>

    @GET("/api/rankings/daily")
    suspend fun getDailyRanking(
        @Header("Authorization") token: String,
        @Query("date") date: String
    ): Response<Map<String, Any>>

    @GET("/api/rankings/user/{userId}")
    suspend fun getUserRanking(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<Map<String, Any>>

    // ==================== PVP ====================
    @POST("/api/pvp/challenge")
    suspend fun challengeUser(
        @Header("Authorization") token: String,
        @Query("targetUserId") targetUserId: String,
        @Query("seasonId") seasonId: String
    ): Response<Map<String, Any>>

    @GET("/api/pvp/problem/{matchId}")
    suspend fun getPvpProblem(
        @Header("Authorization") token: String,
        @Path("matchId") matchId: String
    ): Response<ProblemResponse>

    @POST("/api/pvp/answer")
    suspend fun submitPvpAnswer(
        @Header("Authorization") token: String,
        @Query("matchId") matchId: String,
        @Body answer: Map<String, String>
    ): Response<Map<String, Any>>

    // ==================== Season ====================
    @GET("/api/seasons/active")
    suspend fun getActiveSeason(@Header("Authorization") token: String): Response<SeasonResponse>

    @GET("/api/seasons/{seasonId}")
    suspend fun getSeason(
        @Header("Authorization") token: String,
        @Path("seasonId") seasonId: String
    ): Response<SeasonResponse>

    @GET("/api/seasons")
    suspend fun getAllSeasons(@Header("Authorization") token: String): Response<SeasonListResponse>

    // ==================== Calendar ====================
    @GET("/api/calendar/{date}")
    suspend fun getCalendarDay(
        @Header("Authorization") token: String,
        @Path("date") date: String
    ): Response<Map<String, Any>>

    @GET("/api/calendar/month/{yearMonth}")
    suspend fun getCalendarMonth(
        @Header("Authorization") token: String,
        @Path("yearMonth") yearMonth: String // yyyy-MM
    ): Response<Map<String, Any>>
}