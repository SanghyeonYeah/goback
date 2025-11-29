package com.yourorg.studyplanner.data.local.db

import androidx.room.*

@Dao
interface RankingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRanking(ranking: RankingEntity)

    @Query("SELECT * FROM rankings WHERE season = :season ORDER BY rank ASC")
    suspend fun getRankingBySeason(season: Int): List<RankingEntity>
}