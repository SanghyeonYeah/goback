package com.yourorg.studyplanner.util

/**
 * Score Utilities
 * 점수 계산 및 관련 유틸리티
 */
object ScoreUtils {

    /**
     * 디플로마 보정 적용 점수 계산
     */
    fun calculateDiplomaAdjustedScore(
        baseScore: Int,
        diploma: String,
        subject: String
    ): Int {
        return when (diploma) {
            in listOf("IT", "공학", "수학", "물리", "화학", "생명과학", "IB(자연)") -> {
                // 이과
                when (subject) {
                    in listOf("물리I", "화학I", "생명과학I") -> baseScore
                    in listOf("국어", "사회", "역사") -> (baseScore * 0.9).toInt()
                    else -> baseScore
                }
            }
            in listOf("인문학(문학/사학/철학)", "국제어문", "사회과학", "경제경영", "IB(인문)") -> {
                // 문과
                when (subject) {
                    in listOf("국어", "사회", "역사") -> baseScore
                    in listOf("물리I", "화학I", "생명과학I") -> (baseScore * 1.1).toInt()
                    else -> baseScore
                }
            }
            in listOf("예술", "체육") -> {
                // 예체
                (baseScore * 1.1).toInt()
            }
            else -> baseScore
        }
    }

    /**
     * 완료 보너스 적용 점수 계산
     * 학습 완료 시 다음날 문제 점수 +10%
     */
    fun applyCompletionBonus(baseScore: Int, hasCompletionBonus: Boolean): Int {
        return if (hasCompletionBonus) {
            (baseScore * 1.1).toInt()
        } else {
            baseScore
        }
    }

    /**
     * 정답률에 따른 등급 계산
     */
    fun calculateGradeFromPercentage(percentage: Float): Int {
        return when {
            percentage >= 96 -> 1
            percentage >= 89 -> 2
            percentage >= 82 -> 3
            percentage >= 74 -> 4
            percentage >= 65 -> 5
            percentage >= 54 -> 6
            percentage >= 41 -> 7
            percentage >= 23 -> 8
            else -> 9
        }
    }

    /**
     * PVP 매치 점수 계산
     * - 승리: 기본 점수 + 상대 레이팅 차이에 따른 보너스
     * - 패배: 기본 점수 - 페널티
     * - 무승부: 기본 점수 / 2
     */
    fun calculatePvpScore(
        baseScore: Int,
        result: String, // "WIN", "LOSE", "DRAW"
        myRating: Float,
        opponentRating: Float
    ): Int {
        val ratingDiff = Math.abs(myRating - opponentRating)
        val diffBonus = (ratingDiff / 100).toInt()

        return when (result) {
            "WIN" -> baseScore + diffBonus
            "LOSE" -> Math.max(baseScore / 2 - diffBonus / 2, 10)
            "DRAW" -> baseScore / 2
            else -> 0
        }
    }

    /**
     * 시간 효율 점수 (풀이 시간이 짧을수록 높은 점수)
     */
    fun calculateTimeEfficiencyScore(
        baseScore: Int,
        timeSpent: Int,
        recommendedTime: Int
    ): Int {
        val efficiency = when {
            timeSpent <= recommendedTime * 0.5 -> 1.2
            timeSpent <= recommendedTime -> 1.0
            timeSpent <= recommendedTime * 1.5 -> 0.8
            else -> 0.5
        }
        return (baseScore * efficiency).toInt()
    }

    /**
     * 누적 점수에 따른 뱃지 획득 확인
     */
    fun checkBadgeUnlock(totalScore: Int, previousBadges: List<String>): List<String> {
        val newBadges = mutableListOf<String>()

        if (totalScore >= 1000 && "철학자" !in previousBadges) newBadges.add("철학자")
        if (totalScore >= 5000 && "석학" !in previousBadges) newBadges.add("석학")
        if (totalScore >= 10000 && "대사상가" !in previousBadges) newBadges.add("대사상가")

        return newBadges
    }

    /**
     * 랭킹 점수 계산
     */
    fun calculateRankingScore(
        problemScore: Int,
        timeBonus: Int,
        bonusApplied: Boolean,
        diplomaAdjustedScore: Int
    ): Int {
        var totalScore = diplomaAdjustedScore + timeBonus
        if (bonusApplied) {
            totalScore = (totalScore * 1.1).toInt()
        }
        return totalScore
    }
}