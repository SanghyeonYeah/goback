package com.yourorg.studyplanner.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream

/**
 * Transformer Model
 * 시계열 학습 데이터 기반 미래 성적 예측
 */
class TransformerModel(context: Context) {
    private lateinit var interpreter: Interpreter
    private val modelName = "transformer.tflite"

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
     * 과거 성적과 학습 시간 기반으로 미래 성적 예측
     * @param historicalScores 과거 성적 데이터 (시계열)
     * @param studyTimeData 학습 시간 데이터 (시계열)
     * @return 예측된 미래 성적 (등급)
     */
    fun predictFutureGrade(
        historicalScores: FloatArray,
        studyTimeData: FloatArray
    ): IntArray {
        val sequenceLength = 12 // 12주
        val input = FloatArray(sequenceLength * 2)

        // 과거 점수와 학습 시간을 시퀀스로 구성
        for (i in 0 until sequenceLength) {
            input[i] = if (i < historicalScores.size) historicalScores[i] else 0f
            input[i + sequenceLength] = if (i < studyTimeData.size) studyTimeData[i] else 0f
        }

        val output = Array(1) { IntArray(4) } // 향후 4주 예측
        interpreter.runForMultipleInputsOutputs(arrayOf(input), mapOf(0 to output[0]))

        return output[0]
    }

    /**
     * 현재 학습 진도와 목표 등급에 기반한 학습 계획 최적화
     * @param currentGrade 현재 등급 (1~9)
     * @param targetGrade 목표 등급 (1~9)
     * @param weeksPassed 지난 주
     * @param weeksRemaining 남은 주
     * @return 최적화된 주간 학습 시간 권장값
     */
    fun optimizeStudySchedule(
        currentGrade: Int,
        targetGrade: Int,
        weeksPassed: Int,
        weeksRemaining: Int
    ): FloatArray {
        val input = floatArrayOf(
            currentGrade.toFloat(),
            targetGrade.toFloat(),
            weeksPassed.toFloat(),
            weeksRemaining.toFloat()
        )

        val output = Array(1) { FloatArray(weeksRemaining) }
        interpreter.runForMultipleInputsOutputs(arrayOf(input), mapOf(0 to output[0]))

        // 정규화: 총 시간이 현실적인 범위 내
        return normalizeSchedule(output[0])
    }

    /**
     * 학습 시간의 과목별 배분 예측
     * @param targetGrades 과목별 목표 등급
     * @param dailyStudyHours 일일 학습 가능 시간
     * @return 과목별 권장 학습 시간
     */
    fun predictSubjectTimeAllocation(
        targetGrades: Map<String, Int>,
        dailyStudyHours: Float
    ): Map<String, Float> {
        val subjects = listOf("국어", "수학", "사회", "통합과학", "영어", "역사", "물리I", "화학I", "생명과학I")
        val gradeArray = FloatArray(subjects.size) { index ->
            targetGrades[subjects.getOrNull(index)] ?.toFloat() ?: 5f
        }

        val input = FloatArray(subjects.size + 1)
        gradeArray.forEachIndexed { index, grade -> input[index] = grade }
        input[subjects.size] = dailyStudyHours

        val output = Array(1) { FloatArray(subjects.size) }
        interpreter.runForMultipleInputsOutputs(arrayOf(input), mapOf(0 to output[0]))

        val result = mutableMapOf<String, Float>()
        subjects.forEachIndexed { index, subject ->
            result[subject] = output[0][index] * dailyStudyHours
        }

        return result
    }

    private fun normalizeSchedule(schedule: FloatArray): FloatArray {
        val sum = schedule.sum()
        return if (sum > 0) schedule.map { it / sum * 420f }.toFloatArray() // 7시간/일
        else schedule
    }

    fun release() {
        interpreter.close()
    }
}