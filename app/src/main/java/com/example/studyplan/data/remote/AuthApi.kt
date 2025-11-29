package com.yourorg.studyplanner.data.remote

import retrofit2.http.*

/**
 * Auth API Interface
 * 사용자 인증 관련 엔드포인트
 */
interface AuthApi {

    /**
     * 구글 OAuth2 회원가입
     */
    @POST("api/v1/auth/signup/google")
    suspend fun signUpGoogle(
        @Query("token") googleToken: String
    ): AuthResponse

    /**
     * 아이디/비번 회원가입
     */
    @POST("api/v1/auth/signup")
    suspend fun signUpCredentials(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("grade") grade: Int,
        @Query("diploma") diploma: String,
        @Body targetGrades: Map<String, Int>,
        @Query("hours") availableStudyHours: Float
    ): AuthResponse

    /**
     * 로그인
     */
    @POST("api/v1/auth/login")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): AuthResponse

    /**
     * Google OAuth2 로그인
     */
    @POST("api/v1/auth/login/google")
    suspend fun loginGoogle(
        @Query("token") googleToken: String
    ): AuthResponse

    /**
     * 프로필 업데이트
     */
    @PATCH("api/v1/auth/profile/{userId}")
    suspend fun updateProfile(
        @Path("userId") userId: String,
        @Body targetGrades: Map<String, Int>?,
        @Query("hours") availableStudyHours: Float?
    ): AuthResponse

    /**
     * 면학 시간 설정
     */
    @PATCH("api/v1/auth/study-settings/{userId}")
    suspend fun updateStudySettings(
        @Path("userId") userId: String,
        @Query("inSession") isWeekdayInSession: Boolean,
        @Query("sessionHours") studyHoursOutOfSession: Float?,
        @Query("weekendHours") weekendStudyHours: Float?
    ): AuthResponse

    /**
     * 디플로마 변경
     */
    @PATCH("api/v1/auth/diploma/{userId}")
    suspend fun changeDiploma(
        @Path("userId") userId: String,
        @Query("diploma") newDiploma: String
    ): AuthResponse
}

// Response Models
data class AuthResponse(
    val user: UserData
) {
    fun toUser() = user.toUser()
}

data class UserData(
    val id: String,
    val username: String,
    val email: String,
    val grade: Int,
    val diploma: String,
    val targetGrades: Map<String, Int>,
    val availableStudyHours: Float,
    val isWeekdayInSession: Boolean,
    val weekdaySessionHours: Float?,
    val weekendStudyHours: Float?,
    val createdAt: String,
    val token: String
) {
    fun toUser() = com.yourorg.studyplanner.domain.model.User(
        id = id,
        username = username,
        email = email,
        grade = grade,
        diploma = diploma,
        targetGrades = targetGrades,
        availableStudyHours = availableStudyHours,
        isWeekdayInSession = isWeekdayInSession,
        token = token
    )
}