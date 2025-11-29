package com.yourorg.studyplanner.data.local.db

import androidx.room.*

@Dao
interface SubmissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: SubmissionEntity)

    @Query("SELECT * FROM submissions WHERE userId = :userId")
    suspend fun getSubmissionsByUser(userId: String): List<SubmissionEntity>

    @Query("SELECT SUM(score) FROM submissions WHERE userId = :userId AND submittedAt BETWEEN :startDate AND :endDate")
    suspend fun getTotalScore(userId: String, startDate: Long, endDate: Long): Int?
}