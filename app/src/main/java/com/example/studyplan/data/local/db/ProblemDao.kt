package com.yourorg.studyplanner.data.local.db

import androidx.room.*

@Dao
interface ProblemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblem(problem: ProblemEntity)

    @Query("SELECT * FROM problems WHERE id = :problemId")
    suspend fun getProblemById(problemId: String): ProblemEntity?

    @Query("SELECT * FROM problems WHERE subject = :subject AND season = :season ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomProblem(subject: String, season: Int): ProblemEntity?

    @Query("SELECT * FROM problems WHERE season = :season")
    suspend fun getProblemsBySeason(season: Int): List<ProblemEntity>
}