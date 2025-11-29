package com.yourorg.studyplanner.domain.model

data class Ranking(
    val id: String,
    val season: Int,
    val entries: List<RankingEntry>
)

data class RankingEntry(
    val rank: Int,
    val userId: String,
    val userName: String,
    val totalScore: Int,
    val problemsSolved: Int,
    val winRate: Double
)