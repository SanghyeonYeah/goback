package com.yourorg.studyplanner.data.remote

import retrofit2.http.*

/**
 * PVP API Interface
 * PVP 매치, 대전 관련 엔드포인트
 */
interface PvpApi {

    /**
     * PVP 매치 시작
     */
    @POST("api/v1/pvp/match/start")
    suspend fun startMatch(
        @Query("userId") userId: String,
        @Query("opponentId") opponentId: String
    ): MatchStartResponse

    /**
     * PVP 매치 신청
     */
    @POST("api/v1/pvp/match/request")
    suspend fun requestMatch(
        @Query("myUserId") myUserId: String,
        @Query("targetUserId") targetUserId: String
    ): MatchStartResponse

    /**
     * PVP 문제 답 제출
     */
    @POST("api/v1/pvp/match/{matchId}/answer")
    suspend fun submitPvpAnswer(
        @Path("matchId") matchId: String,
        @Query("userId") userId: String,
        @Query("problemId") problemId: String,
        @Query("answer") selectedAnswer: String,
        @Query("timeSpent") timeSpent: Int
    ): PvpAnswerResponse

    /**
     * PVP 매치 결과 조회
     */
    @GET("api/v1/pvp/match/{matchId}/result")
    suspend fun getMatchResult(
        @Path("matchId") matchId: String
    ): MatchResultResponse

    /**
     * 사용자 PVP 전적 조회
     */
    @GET("api/v1/pvp/record/{userId}")
    suspend fun getPvpRecord(
        @Path("userId") userId: String
    ): PvpRecordResponse

    /**
     * 매치 포기
     */
    @POST("api/v1/pvp/match/{matchId}/forfeit")
    suspend fun forfeit(
        @Path("matchId") matchId: String,
        @Query("userId") userId: String
    ): ForfeitResponse

    /**
     * 최근 매치 조회
     */
    @GET("api/v1/pvp/matches/{userId}")
    suspend fun getRecentMatches(
        @Path("userId") userId: String,
        @Query("limit") limit: Int = 20
    ): RecentMatchesResponse

    /**
     * 1:1 대전 이력 조회
     */
    @GET("api/v1/pvp/history/{userId}/{opponentId}")
    suspend fun getHeadToHead(
        @Path("userId") userId: String,
        @Path("opponentId") opponentId: String,
        @Query("limit") limit: Int = 10
    ): HeadToHeadResponse
}

// Response Models
data class MatchStartResponse(
    val matchId: String,
    val problemId: String,
    val timeLimit: Int,
    val opponentUsername: String
)

data class PvpAnswerResponse(
    val result: Map<String, Any>
)

data class MatchResultResponse(
    val result: Map<String, Any>
)

data class PvpRecordResponse(
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val rating: Int,
    val totalMatches: Int,
    val winRate: Float
)

data class ForfeitResponse(
    val success: Boolean,
    val opponentWins: Int
)

data class RecentMatchesResponse(
    val matches: List<Map<String, Any>>
)

data class HeadToHeadResponse(
    val myWins: Int,
    val opponentWins: Int,
    val draws: Int
)