package com.yourorg.studyplanner.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.concurrent.TimeUnit

/**
 * Retrofit Client
 * API 통신을 위한 Retrofit 클라이언트 설정
 */
object RetrofitClient {

    private const val BASE_URL = "https://api.studyplanner.com/"
    private const val TIMEOUT_SECONDS = 30L

    private var retrofitInstance: Retrofit? = null
    private var okHttpClient: OkHttpClient? = null

    /**
     * Retrofit 인스턴스 생성 또는 반환
     */
    fun getRetrofitInstance(sharedPreferences: SharedPreferences): Retrofit {
        if (retrofitInstance == null) {
            retrofitInstance = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient(sharedPreferences))
                .addConverterFactory(GsonConverterFactory.create(createGson()))
                .build()
        }
        return retrofitInstance!!
    }

    /**
     * OkHttpClient 설정
     */
    private fun getOkHttpClient(sharedPreferences: SharedPreferences): OkHttpClient {
        if (okHttpClient == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(AuthInterceptor(sharedPreferences))
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
        return okHttpClient!!
    }

    /**
     * Gson 설정
     */
    private fun createGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    /**
     * 클라이언트 초기화 (재설정 시)
     */
    fun resetClient() {
        retrofitInstance = null
        okHttpClient = null
    }
}