package com.yourorg.studyplanner.data.repository

import com.yourorg.studyplanner.data.local.db.RankingDao
import com.yourorg.studyplanner.data.remote.RankingApi
import com.yourorg.studyplanner.domain.model.Ranking
import javax.inject.Inject

/**
 * Ranking Repository
 * 일일 랭킹, 시즌 랭킹 관리 및 조회
 */
class RankingRepository @Inject constructor(
    private val rankingDao: RankingDao,
    private val rankingApi: RankingApi
) {

    /**
     * 일일 랭킹 조회
     */
    suspend fun getDailyRanking(limit: Int = 100): Result<List<Ranking>> = try {
        val response = rankingApi.getDailyRanking(limit)
        val rankings = response.rankings.map { it.toRanking() }
        rankings.forEach { rankingDao.insertRanking(it) }
        Result.success(rankings)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 시즌 랭킹 조회
     */
    suspend fun getSeasonRanking(seasonId: String, limit: Int = 100): Result<List<Ranking>> = try {
        val response = rankingApi.getSeasonRanking(seasonId, limit)
        val rankings = response.rankings.map { it.toRanking() }
        rankings.forEach { rankingDao.insertRanking(it) }
        Result.success(rankings)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 사용자 개인 랭킹 조회
     */
    suspend fun getUserRankingPosition(userId: String): Result<Ranking?> = try {
        val ranking = rankingDao.getUserRanking(userId)
        Result.success(ranking)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 과목별 랭킹 조회
     */
    suspend fun getSubjectRanking(subject: String, limit: Int = 50): Result<List<Ranking>> = try {
        val response = rankingApi.getSubjectRanking(subject, limit)
        val rankings = response.rankings.map { it.toRanking() }
        Result.success(rankings)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 상점 판매량 기반 상품 추천
     */
    suspend fun getTopReward(seasonId: String): Result<String?> = try {
        val response = rankingApi.getSeasonReward(seasonId)
        Result.success(response.reward)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 사용자 점수 업데이트
     */
    suspend fun updateUserScore(userId: String, scoreIncrease: Int): Result<Unit> = try {
        rankingApi.updateScore(userId, scoreIncrease)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 실시간 일일 랭킹 스트림 (Flow)
     */
    fun getDailyRankingFlow() = rankingDao.getDailyRankingFlow()

    /**
     * 시즌 종료 시 랭킹 초기화
     */
    suspend fun resetSeasonRanking(newSeasonId: String): Result<Unit> = try {
        rankingApi.resetRanking(newSeasonId)
        rankingDao.clearRankings()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 로컬에서 일일 랭킹 조회
     */
    fun getDailyRankingLocal() = rankingDao.getDailyRankingLocal()
}