package com.yourorg.studyplanner.network

import okhttp3.Interceptor
import okhttp3.Response
import android.content.SharedPreferences

/**
 * Auth Interceptor
 * 모든 HTTP 요청에 JWT 토큰 자동 추가
 */
class AuthInterceptor(private val sharedPreferences: SharedPreferences) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // SharedPreferences에서 토큰 조회
        val token = sharedPreferences.getString("auth_token", null)

        // 토큰이 없으면 원본 요청 진행
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        // 토큰이 있으면 Authorization 헤더 추가
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .build()

        return chain.proceed(newRequest)
    }
}