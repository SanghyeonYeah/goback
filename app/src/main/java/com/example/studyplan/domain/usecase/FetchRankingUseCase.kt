package com.yourorg.studyplanner.domain.usecase

import com.yourorg.studyplanner.domain.model.Ranking

class FetchRankingUseCase(
    private val rankingRepository: RankingRepository
) {
    suspend operator fun invoke(season: Int, isDaily: Boolean = false): Result<Ranking> {
        return rankingRepository.fetchRanking(season, isDaily)
    }
}