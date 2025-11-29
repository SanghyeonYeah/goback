package com.yourorg.studyplanner.data.remote

import retrofit2.http.*

/**
 * Ranking API Interface
 * 랭킹 조회, 점수 업데이트, 시즌 관리 관련 엔드포인트
 */
interface RankingApi {

    /**
     * 일일 랭킹 조회
     */
    @GET("api/v1/ranking/daily")
    suspend fun getDailyRanking(
        @Query("limit") limit: Int = 100
    ): RankingsResponse

    /**
     * 시즌 랭킹 조회
     */
    @GET("api/v1/ranking/season/{seasonId}")
    suspend fun getSeasonRanking(
        @Path("seasonId") seasonId: String,
        @Query("limit") limit: Int = 100
    ): RankingsResponse

    /**
     * 과목별 랭킹 조회
     */
    @GET("api/v1/ranking/subject/{subject}")
    suspend fun getSubjectRanking(
        @Path("subject") subject: String,
        @Query("limit") limit: Int = 50
    ): RankingsResponse

    /**
     * 사용자 점수 업데이트
     */
    @PATCH("api/v1/ranking/score/{userId}")
    suspend fun updateScore(
        @Path("userId") userId: String,
        @Query("score") scoreIncrease: Int
    ): ScoreUpdateResponse

    /**
     * 시즌 상품 조회
     */
    @GET("api/v1/season/{seasonId}/reward")
    suspend fun getSeasonReward(
        @Path("seasonId") seasonId: String
    ): RewardResponse

    /**
     * 랭킹 초기화 (시즌 종료)
     */
    @POST("api/v1/ranking/reset")
    suspend fun resetRanking(
        @Query("newSeasonId") newSeasonId: String
    ): ResetResponse
}

// Response Models
data class RankingsResponse(
    val rankings: List<RankingData>
)

data class RankingData(
    val rank: Int,
    val userId: String,
    val username: String,
    val score: Int,
    val dailyScore: Int,
    val seasonScore: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val rating: Float,
    val accuracy: Float,
    val level: Int,
    val badge: String?
) {
    fun toRanking() = com.yourorg.studyplanner.domain.model.Ranking(
        rank = rank,
        userId = userId,
        username = username,
        score = score,
        wins = wins,
        losses = losses,
        draws = draws,
        rating = rating.toInt()
    )
}

data class ScoreUpdateResponse(
    val success: Boolean,
    val newScore: Int,
    val newRank: Int
)

data class RewardResponse(
    val reward: String?
)

data class ResetResponse(
    val success: Boolean,
    val newSeasonId: String
)