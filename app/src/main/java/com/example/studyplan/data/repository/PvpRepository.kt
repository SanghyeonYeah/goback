package com.yourorg.studyplanner.data.repository

import com.yourorg.studyplanner.data.remote.PvpApi
import com.yourorg.studyplanner.domain.model.Ranking
import javax.inject.Inject

/**
 * PVP Repository
 * PVP 매칭, 게임 진행, 결과 관리
 */
class PvpRepository @Inject constructor(
    private val pvpApi: PvpApi
) {

    /**
     * PVP 매치 시작
     */
    suspend fun startPvpMatch(
        userId: String,
        opponentId: String
    ): Result<String> = try { // 매치 ID 반환
        val response = pvpApi.startMatch(userId, opponentId)
        Result.success(response.matchId)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 랭킹에서 사용자 검색 및 PVP 대전 신청
     */
    suspend fun requestPvpWithRankingUser(
        myUserId: String,
        targetUserId: String
    ): Result<String> = try { // 매치 ID 반환
        val response = pvpApi.requestMatch(myUserId, targetUserId)
        Result.success(response.matchId)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * PVP 문제 풀이 제출
     */
    suspend fun submitPvpAnswer(
        matchId: String,
        userId: String,
        problemId: String,
        selectedAnswer: String,
        timeSpent: Int
    ): Result<Map<String, Any>> = try { // 개인 결과 반환
        val response = pvpApi.submitPvpAnswer(
            matchId,
            userId,
            problemId,
            selectedAnswer,
            timeSpent
        )
        Result.success(response.result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * PVP 매치 결과 조회
     * - 한 명만 틀리면 틀린 사람 패배
     * - 둘 다 맞거나 둘 다 틀리면 무승부
     * - 시간 초과 시 틀린 것으로 간주
     */
    suspend fun getPvpMatchResult(matchId: String): Result<Map<String, Any>> = try {
        val response = pvpApi.getMatchResult(matchId)
        Result.success(response.result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 사용자의 PVP 전적 조회
     */
    suspend fun getPvpRecord(userId: String): Result<Map<String, Int>> = try {
        val response = pvpApi.getPvpRecord(userId)
        Result.success(
            mapOf(
                "wins" to response.wins,
                "losses" to response.losses,
                "draws" to response.draws,
                "rating" to response.rating
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * PVP 거절 (매치 시작 후 포기)
     */
    suspend fun forfeitMatch(matchId: String, userId: String): Result<Unit> = try {
        pvpApi.forfeit(matchId, userId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 최근 PVP 매치 조회
     */
    suspend fun getRecentMatches(userId: String, limit: Int = 20): Result<List<Map<String, Any>>> = try {
        val response = pvpApi.getRecentMatches(userId, limit)
        Result.success(response.matches)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 특정 상대와의 대전 이력 조회
     */
    suspend fun getHeadToHeadHistory(
        userId: String,
        opponentId: String,
        limit: Int = 10
    ): Result<Map<String, Int>> = try {
        val response = pvpApi.getHeadToHead(userId, opponentId, limit)
        Result.success(
            mapOf(
                "myWins" to response.myWins,
                "opponentWins" to response.opponentWins,
                "draws" to response.draws
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }
}