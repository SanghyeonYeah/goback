package com.yourorg.studyplanner.di

import android.content.Context
import com.yourorg.studyplanner.data.remote.*
import com.yourorg.studyplanner.network.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Network Module
 * Retrofit API 서비스 인스턴스 제공
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * ApiService 제공
     */
    @Singleton
    @Provides
    fun provideApiService(
        @ApplicationContext context: Context
    ): ApiService {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return RetrofitClient.getRetrofitInstance(sharedPreferences)
            .create(ApiService::class.java)
    }

    /**
     * AuthApi 제공
     */
    @Singleton
    @Provides
    fun provideAuthApi(
        @ApplicationContext context: Context
    ): AuthApi {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return RetrofitClient.getRetrofitInstance(sharedPreferences)
            .create(AuthApi::class.java)
    }

    /**
     * ProblemApi 제공
     */
    @Singleton
    @Provides
    fun provideProblemApi(
        @ApplicationContext context: Context
    ): ProblemApi {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return RetrofitClient.getRetrofitInstance(sharedPreferences)
            .create(ProblemApi::class.java)
    }

    /**
     * RankingApi 제공
     */
    @Singleton
    @Provides
    fun provideRankingApi(
        @ApplicationContext context: Context
    ): RankingApi {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return RetrofitClient.getRetrofitInstance(sharedPreferences)
            .create(RankingApi::class.java)
    }

    /**
     * PvpApi 제공
     */
    @Singleton
    @Provides
    fun providePvpApi(
        @ApplicationContext context: Context
    ): PvpApi {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return RetrofitClient.getRetrofitInstance(sharedPreferences)
            .create(PvpApi::class.java)
    }
}