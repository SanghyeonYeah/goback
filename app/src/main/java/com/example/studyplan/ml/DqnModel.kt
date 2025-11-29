package com.yourorg.studyplanner.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream

/**
 * DQN (Deep Q-Network) Model
 * 강화학습 기반 PVP 매칭 및 게임 난이도 조정
 */
class DqnModel(context: Context) {
    private lateinit var interpreter: Interpreter
    private val modelName = "dqn.tflite"

    init {
        loadModel(context)
    }

    private fun loadModel(context: Context) {
        val assetManager = context.assets
        val assetFileDescriptor = assetManager.openFd(modelName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val buffer: MappedByteBuffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
        interpreter = Interpreter(buffer)
        inputStream.close()
    }

    /**
     * 학생의 실력과 승률을 고려하여 상대방 선정
     * @param studentRating 자신의 레이팅
     * @param winRate 자신의 승률
     * @param availableOpponents 현재 이용 가능한 상대 리스트
     * @return 최적의 상대 ID
     */
    fun selectOptimalOpponent(
        studentRating: Float,
        winRate: Float,
        availableOpponents: List<Pair<String, Float>> // (상대ID, 상대레이팅)
    ): String {
        val input = FloatArray(1 + 1 + availableOpponents.size)
        input[0] = studentRating
        input[1] = winRate
        availableOpponents.forEachIndexed { index, (_, opponentRating) ->
            input[2 + index] = opponentRating
        }

        val output = Array(1) { FloatArray(availableOpponents.size) }
        interpreter.runForMultipleInputsOutputs(arrayOf(input), mapOf(0 to output[0]))

        // 가장 높은 Q-value를 가진 상대 선택
        val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: 0
        return availableOpponents[maxIndex].first
    }

    /**
     * 현재 성과에 따라 다음 문제의 난이도 조정
     * @param recentPerformance 최근 성적 (0.0~1.0, 1.0이 만점)
     * @param currentDifficultyLevel 현재 난이도 레벨 (1~5)
     * @param consecutiveCorrect 연속 정답 횟수
     * @param consecutiveWrong 연속 오답 횟수
     * @return 조정된 난이도 레벨
     */
    fun adjustDifficultyLevel(
        recentPerformance: Float,
        currentDifficultyLevel: Int,
        consecutiveCorrect: Int,
        consecutiveWrong: Int
    ): Int {
        val input = floatArrayOf(
            recentPerformance,
            currentDifficultyLevel.toFloat(),
            consecutiveCorrect.toFloat(),
            consecutiveWrong.toFloat()
        )

        val output = Array(1) { FloatArray(1) }
        interpreter.runForMultipleInputsOutputs(arrayOf(input), mapOf(0 to output[0]))

        // 출력값을 난이도 레벨로 변환 (1~5)
        val newLevel = (output[0][0] * 4 + 1).toInt().coerceIn(1, 5)
        return newLevel
    }

    /**
     * 학생의 취약한 주제 파악 및 강화 문제 추천
     * @param correctAnswersByTopic 주제별 정답 수
     * @param wrongAnswersByTopic 주제별 오답 수
     * @param maxRecommendations 추천할 주제 개수
     * @return 강화 학습이 필요한 주제 리스트
     */
    fun identifyWeakTopics(
        correctAnswersByTopic: Map<String, Int>,
        wrongAnswersByTopic: Map<String, Int>,
        maxRecommendations: Int = 3
    ): List<String> {
        val allTopics = (correctAnswersByTopic.keys + wrongAnswersByTopic.keys).distinct()

        val topicScores = allTopics.map { topic ->
            val correct = correctAnswersByTopic[topic] ?: 0
            val wrong = wrongAnswersByTopic[topic] ?: 0
            val total = correct + wrong

            if (total > 0) {
                val accuracy = correct.toFloat() / total.toFloat()
                topic to (1 - accuracy) // 1에 가까울수록 약함
            } else {
                topic to 0.5f
            }
        }

        return topicScores
            .sortedByDescending { it.second }
            .take(maxRecommendations)
            .map { it.first }
    }

    /**
     * PVP 매칭 후 기대 승률 계산
     * @param myRating 내 레이팅
     * @param opponentRating 상대 레이팅
     * @return 내가 이길 기대 확률 (0.0~1.0)
     */
    fun calculateExpectedWinProbability(
        myRating: Float,
        opponentRating: Float
    ): Float {
        // ELO 레이팅 기반 계산
        val ratingDiff = myRating - opponentRating
        val expected = 1f / (1f + Math.pow(10.0, (-ratingDiff / 400.0)).toFloat())
        return expected.coerceIn(0.1f, 0.9f) // 극단적 확률 방지
    }

    /**
     * 레이팅 변화 계산 (매치 결과 기반)
     * @param currentRating 현재 레이팅
     * @param opponentRating 상대 레이팅
     * @param matchResult 1.0 = 승리, 0.5 = 무승부, 0.0 = 패배
     * @param kFactor 변동 계수 (기본값 32)
     * @return 새로운 레이팅
     */
    fun calculateNewRating(
        currentRating: Float,
        opponentRating: Float,
        matchResult: Float,
        kFactor: Float = 32f
    ): Float {
        val expectedWin = calculateExpectedWinProbability(currentRating, opponentRating)
        val ratingChange = kFactor * (matchResult - expectedWin)
        return (currentRating + ratingChange).coerceAtLeast(0f)
    }

    fun release() {
        interpreter.close()
    }
}