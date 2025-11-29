package com.yourorg.studyplanner.data.repository

import com.yourorg.studyplanner.data.local.db.UserDao
import com.yourorg.studyplanner.data.remote.AuthApi
import com.yourorg.studyplanner.domain.model.User
import javax.inject.Inject

/**
 * User Repository
 * 사용자 인증, 프로필 관리, 로컬/원격 데이터 동기화
 */
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val authApi: AuthApi
) {

    /**
     * 구글 OAuth2를 통한 회원가입
     */
    suspend fun signUpWithGoogle(googleToken: String): Result<User> = try {
        val response = authApi.signUpGoogle(googleToken)
        val user = response.toUser()
        userDao.insertUser(user)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 아이디/비번 회원가입
     */
    suspend fun signUpWithCredentials(
        username: String,
        password: String,
        grade: Int,
        diploma: String,
        targetGrades: Map<String, Int>,
        availableStudyHours: Float
    ): Result<User> = try {
        val response = authApi.signUpCredentials(
            username,
            password,
            grade,
            diploma,
            targetGrades,
            availableStudyHours
        )
        val user = response.toUser()
        userDao.insertUser(user)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 로그인 (아이디/비번)
     */
    suspend fun login(username: String, password: String): Result<User> = try {
        val response = authApi.login(username, password)
        val user = response.toUser()
        userDao.insertUser(user)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 로그인 (Google OAuth2)
     */
    suspend fun loginWithGoogle(googleToken: String): Result<User> = try {
        val response = authApi.loginGoogle(googleToken)
        val user = response.toUser()
        userDao.insertUser(user)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    suspend fun getCurrentUser(): Result<User?> = try {
        val user = userDao.getCurrentUser()
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 사용자 프로필 업데이트
     */
    suspend fun updateUserProfile(
        userId: String,
        targetGrades: Map<String, Int>?,
        availableStudyHours: Float?
    ): Result<User> = try {
        val response = authApi.updateProfile(userId, targetGrades, availableStudyHours)
        val user = response.toUser()
        userDao.updateUser(user)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 면학 시간 설정 업데이트
     */
    suspend fun updateStudySessionSettings(
        userId: String,
        isWeekdayInSession: Boolean,
        studyHoursOutOfSession: Float? = null,
        weekendStudyHours: Float? = null
    ): Result<Unit> = try {
        authApi.updateStudySettings(
            userId,
            isWeekdayInSession,
            studyHoursOutOfSession,
            weekendStudyHours
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 디플로마 변경
     */
    suspend fun changeDiploma(userId: String, newDiploma: String): Result<User> = try {
        val response = authApi.changeDiploma(userId, newDiploma)
        val user = response.toUser()
        userDao.updateUser(user)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 사용자 정보 로컬에서 조회
     */
    fun getUserLocal(userId: String) = userDao.getUserById(userId)

    /**
     * 로그아웃
     */
    suspend fun logout(): Result<Unit> = try {
        userDao.deleteCurrentUser()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}