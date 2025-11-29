package com.yourorg.studyplanner.data.local.db

import androidx.room.*

@Dao
interface ObjectiveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjective(objective: ObjectiveEntity)

    @Query("SELECT * FROM objectives WHERE userId = :userId AND season = :season")
    suspend fun getObjectivesByUserAndSeason(userId: String, season: Int): List<ObjectiveEntity>

    @Update
    suspend fun updateObjective(objective: ObjectiveEntity)
}