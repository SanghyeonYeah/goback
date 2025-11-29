package com.yourorg.studyplanner.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yourorg.studyplanner.data.local.db.entity.*

@Database(
    entities = [
        UserEntity::class,
        CalendarEntity::class,
        ProblemEntity::class,
        SubmissionEntity::class,
        RankingEntity::class,
        ObjectiveEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(TypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun calendarDao(): CalendarDao
    abstract fun problemDao(): ProblemDao
    abstract fun submissionDao(): SubmissionDao
    abstract fun rankingDao(): RankingDao
    abstract fun objectiveDao(): ObjectiveDao
}

// Entity 정의
@androidx.room.Entity(tableName = "users")
data class UserEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val grade: Int,
    val diploma: String,
    val targetGrades: String, // JSON
    val dailyStudyTime: Int,
    val createdAt: Long
)

@androidx.room.Entity(tableName = "calendar")
data class CalendarEntity(
    @androidx.room.PrimaryKey val id: String,
    val userId: String,
    val date: Long,
    val status: String, // DONE, FAIL, PENDING
    val dailyScore: Int,
    val createdAt: Long
)

@androidx.room.Entity(tableName = "problems")
data class ProblemEntity(
    @androidx.room.PrimaryKey val id: String,
    val subject: String,
    val source: String,
    val content: String,
    val options: String, // JSON
    val correctAnswer: Int,
    val points: Int,
    val season: Int,
    val createdAt: Long
)

@androidx.room.Entity(tableName = "submissions")
data class SubmissionEntity(
    @androidx.room.PrimaryKey val id: String,
    val userId: String,
    val problemId: String,
    val selectedAnswer: Int,
    val isCorrect: Boolean,
    val score: Int,
    val submittedAt: Long
)

@androidx.room.Entity(tableName = "rankings")
data class RankingEntity(
    @androidx.room.PrimaryKey val id: String,
    val season: Int,
    val rank: Int,
    val userId: String,
    val userName: String,
    val totalScore: Int,
    val problemsSolved: Int,
    val winRate: Double
)

@androidx.room.Entity(tableName = "objectives")
data class ObjectiveEntity(
    @androidx.room.PrimaryKey val id: String,
    val userId: String,
    val subject: String,
    val targetGrade: Int,
    val season: Int,
    val achieved: Boolean
)

// Type Converter
class TypeConverter {
    @androidx.room.TypeConverter
    fun fromJson(json: String): List<String> {
        return com.google.gson.Gson().fromJson(json, Array<String>::class.java).toList()
    }

    @androidx.room.TypeConverter
    fun toJson(list: List<String>): String {
        return com.google.gson.Gson().toJson(list)
    }
}