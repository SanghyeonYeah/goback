package com.cnsa.studyplanner.network

import com.cnsa.studyplanner.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // ============= Auth APIs =============
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("auth/google")
    suspend fun loginWithGoogle(
        @Body request: Map<String, String>
    ): Response<AuthResponse>
    
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") sessionId: String
    ): Response<ApiResponse<Unit>>
    
    @GET("auth/verify")
    suspend fun verifySession(
        @Header("Authorization") sessionId: String
    ): Response<ApiResponse<User>>
    
    // ============= User APIs =============
    @GET("users/{userId}")
    suspend fun getUserInfo(
        @Path("userId") userId: Int,
        @Header("Authorization") sessionId: String
    ): Response<ApiResponse<User>>
    
    @PUT("users/{userId}")
    suspend fun updateUserInfo(
        @Path("userId") userId: Int,
        @Header("Authorization") sessionId: String,
        @Body user: User
    ): Response<ApiResponse<User>>
    
    // ============= Goal APIs =============
    @POST("goals")
    suspend fun createGoals(
        @Header("Authorization") sessionId: String,
        @Body request: GoalRequest
    ): Response<ApiResponse<List<Goal>>>
    
    @GET("goals/user/{userId}")
    suspend fun getUserGoals(
        @Path("userId") userId: Int,
        @Header("Authorization") sessionId: String,
        @Query("seasonId") seasonId: Int? = null
    ): Response<ApiResponse<List<Goal>>>
    
    @PUT("goals/{goalId}")
    suspend fun updateGoal(
        @Path("goalId") goalId: Int,
        @Header("Authorization") sessionId: String,
        @Body goal: Goal
    ): Response<ApiResponse<Goal>>
    
    // ============= Todo APIs =============
    @GET("todos/user/{userId}")
    suspend fun getUserTodos(
        @Path("userId") userId: Int,
        @Header("Authorization") sessionId: String,
        @Query("date") date: String? = null
    ): Response<ApiResponse<List<Todo>>>
    
    @POST("todos")
    suspend fun createTodo(
        @Header("Authorization") sessionId: String,
        @Body todo: Todo
    ): Response<ApiResponse<Todo>>
    
    @PUT("todos/{todoId}/complete")
    suspend fun completeTodo(
        @Path("todoId") todoId: Int,
        @Header("Authorization") sessionId: String,
        @Body actualTime: Map<String, Int>
    ): Response<ApiResponse<Todo>>
    
    @DELETE("todos/{todoId}")
    suspend fun deleteTodo(
        @Path("todoId") todoId: Int,
        @Header("Authorization") sessionId: String
    ): Response<ApiResponse<Unit>>
    
    // ============= AI Plan Generation APIs =============
    @POST("plan/generate")
    suspend fun generateStudyPlan(
        @Header("Authorization") sessionId: String,
        @Body request: Map<String, Any>
    ): Response<ApiResponse<StudyPlanRecommendation>>
    
    @GET("plan/user/{userId}")
    suspend fun getUserPlan(
        @Path("userId") userId: Int,
        @Header("Authorization") sessionId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<ApiResponse<List<Todo>>>
    
    // ============= Problem APIs =============
    @GET("problems")
    suspend fun getProblems(
        @Header("Authorization") sessionId: String,
        @Query("seasonId") seasonId: Int,
        @Query("subject") subject: String? = null,
        @Query("difficulty") difficulty: String? = null
    ): Response<ApiResponse<List<Problem>>>
    
    @GET("problems/{problemId}")
    suspend fun getProblem(
        @Path("problemId") problemId: Int,
        @Header("Authorization") sessionId: String
    ): Response<ApiResponse<Problem>>
    
    @POST("problems/submit")
    suspend fun submitProblem(
        @Header("Authorization") sessionId: String,
        @Body submission: ProblemSubmission
    ): Response<ProblemResult>
    
    @Multipart
    @POST("problems/create")
    suspend fun createProblem(
        @Header("Authorization") sessionId: String,
        @Part("problem") problem: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<ApiResponse<Problem>>
    
    @DELETE("problems/{problemId}")
    suspend fun deleteProblem(
        @Path("problemId") problemId: Int,
        @Header("Authorization") sessionId: String
    ): Response<ApiResponse<Unit>>
    
    // ============= Ranking APIs =============
    @GET("rankings/daily")
    suspend fun getDailyRanking(
        @Header("Authorization") sessionId: String,
        @Query("seasonId") seasonId: Int,
        @Query("date") date: String? = null
    ): Response<RankingResponse>
    
    @GET("rankings/season")
    suspend fun getSeasonRanking(
        @Header("Authorization") sessionId: String,
        @Query("seasonId") seasonId: Int,
        @Query("limit") limit: Int = 100
    ): Response<RankingResponse>
    
    @GET("rankings/user/{userId}")
    suspend fun getUserRanking(
        @Path("userId") userId: Int,
        @Header("Authorization") sessionId: String,
        @Query("seasonId") seasonId: Int
    ): Response<ApiResponse<SeasonRanking>>
    
    // ============= Season APIs =============
    @GET("seasons/active")
    suspend fun getActiveSeason(
        @Header("Authorization") sessionId: String
    ): Response<SeasonResponse>
    
    @GET("seasons")
    suspend fun getAllSeasons(
        @Header("Authorization") sessionId: String
    ): Response<SeasonResponse>
    
    @POST("seasons")
    suspend fun createSeason(
        @Header("Authorization") sessionId: String,
        @Body season: Season
    ): Response<SeasonResponse>
    
    @PUT("seasons/{seasonId}")
    suspend fun updateSeason(
        @Path("seasonId") seasonId: Int,
        @Header("Authorization") sessionId: String,
        @Body season: Season
    ): Response<SeasonResponse>
    
    @DELETE("seasons/{seasonId}")
    suspend fun deleteSeason(
        @Path("seasonId") seasonId: Int,
        @Header("Authorization") sessionId: String
    ): Response<ApiResponse<Unit>>
    
    // ============= PVP APIs =============
    @POST("pvp/match")
    suspend fun requestPvpMatch(
        @Header("Authorization") sessionId: String,
        @Body request: PvpRequest
    ): Response<PvpMatchResponse>
    
    @GET("pvp/match/{matchId}")
    suspend fun getPvpMatch(
        @Path("matchId") matchId: Int,
        @Header("Authorization") sessionId: String
    ): Response<PvpMatchResponse>
    
    @POST("pvp/submit")
    suspend fun submitPvpAnswer(
        @Header("Authorization") sessionId: String,
        @Body submission: PvpSubmission
    ): Response<PvpResult>
    
    @GET("pvp/history/{userId}")
    suspend fun getPvpHistory(
        @Path("userId") userId: Int,
        @Header("Authorization") sessionId: String,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<List<PvpMatch>>>
    
    // ============= Calendar APIs =============
    @GET("calendar/user/{userId}")
    suspend fun getCalendarData(
        @Path("userId") userId: Int,
        @Header("Authorization") sessionId: String,
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<ApiResponse<List<CalendarData>>>
    
    @POST("calendar/record")
    suspend fun recordDailyCompletion(
        @Header("Authorization") sessionId: String,
        @Body record: CalendarRecord
    ): Response<ApiResponse<CalendarRecord>>
    
    // ============= Report APIs =============
    @GET("reports/season/{seasonId}/user/{userId}")
    suspend fun getSeasonReport(
        @Path("seasonId") seasonId: Int,
        @Path("userId") userId: Int,
        @Header("Authorization") sessionId: String
    ): Response<SeasonReportResponse>
    
    // ============= Admin APIs =============
    @GET("admin/stats")
    suspend fun getAdminStats(
        @Header("Authorization") sessionId: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("admin/users")
    suspend fun getAllUsers(
        @Header("Authorization") sessionId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponse<List<User>>>
}
