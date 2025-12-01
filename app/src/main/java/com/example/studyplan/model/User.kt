package com.example.studyplanner.model

data class User(
    val id: String,
    val username: String,
    val studentId: String,
    val diploma: String, // IT, 공학, 수학, 물리, 화학, 생명과학, IB(자연), 인문학, 국제어문, 사회과학, 경제경영, IB(인문), 예술, 체육
    val grade: Int, // 1, 2, 3
    val email: String,
    val profileImageUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class UserResponse(
    val status: Int,
    val message: String,
    val data: User?
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class SignupRequest(
    val username: String,
    val password: String,
    val studentId: String,
    val diploma: String,
    val grade: Int
)

data class AuthResponse(
    val status: Int,
    val message: String,
    val data: AuthData?
)

data class AuthData(
    val user: User,
    val sessionKey: String,
    val token: String
)