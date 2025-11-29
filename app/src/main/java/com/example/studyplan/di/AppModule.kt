package com.yourorg.studyplanner.di

import android.content.Context
import androidx.room.Room
import com.yourorg.studyplanner.data.local.db.AppDatabase
import com.yourorg.studyplanner.data.remote.*
import com.yourorg.studyplanner.data.repository.*
import com.yourorg.studyplanner.network.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App Module
 * Hilt를 사용한 의존성 주입 설정
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Room Database 인스턴스
     */
    @Singleton
    @Provides
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "studyplanner_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * DAO 인스턴스들
     */
    @Singleton
    @Provides
    fun provideUserDao(appDatabase: AppDatabase) = appDatabase.userDao()

    @Singleton
    @Provides
    fun provideCalendarDao(appDatabase: AppDatabase) = appDatabase.calendarDao()

    @Singleton
    @Provides
    fun provideProblemDao(appDatabase: AppDatabase) = appDatabase.problemDao()

    @Singleton
    @Provides
    fun provideSubmissionDao(appDatabase: AppDatabase) = appDatabase.submissionDao()

    @Singleton
    @Provides
    fun provideRankingDao(appDatabase: AppDatabase) = appDatabase.rankingDao()

    @Singleton
    @Provides
    fun provideObjectiveDao(appDatabase: AppDatabase) = appDatabase.objectiveDao()

    /**
     * Repository 인스턴스들
     */
    @Singleton
    @Provides
    fun provideUserRepository(
        userDao: com.yourorg.studyplanner.data.local.db.UserDao,
        authApi: AuthApi
    ) = UserRepository(userDao, authApi)

    @Singleton
    @Provides
    fun providePlanRepository(
        calendarDao: com.yourorg.studyplanner.data.local.db.CalendarDao,
        apiService: ApiService
    ) = PlanRepository(calendarDao, apiService)

    @Singleton
    @Provides
    fun provideProblemRepository(
        problemDao: com.yourorg.studyplanner.data.local.db.ProblemDao,
        submissionDao: com.yourorg.studyplanner.data.local.db.SubmissionDao,
        problemApi: ProblemApi
    ) = ProblemRepository(problemDao, submissionDao, problemApi)

    @Singleton
    @Provides
    fun provideRankingRepository(
        rankingDao: com.yourorg.studyplanner.data.local.db.RankingDao,
        rankingApi: RankingApi
    ) = RankingRepository(rankingDao, rankingApi)

    @Singleton
    @Provides
    fun providePvpRepository(
        pvpApi: PvpApi
    ) = PvpRepository(pvpApi)
}