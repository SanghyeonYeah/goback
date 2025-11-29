package com.yourorg.studyplanner.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream

/**
 * GNN (Graph Neural Network) Model
 * 과목 간 연관성 분석 및 효율적인 학습 순서 결정
 */
class GnnModel(context: Context) {
    private lateinit var interpreter: Interpreter
    private val modelName = "gnn.tflite"

    // 과목 간 선수학습 관계
    private val subjectGraph = mapOf(
        "국어" to listOf(),
        "수학" to listOf(),
        "영어" to listOf(),
        "사회" to listOf(),
        "통합과학" to listOf(),
        "역사" to listOf(),
        "물리I" to listOf("수학"),
        "화학I" to listOf("통합과학", "수학"),
        "생명과학I" to listOf("통합과학")
    )

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
     * 과목 간 연관성을 고려하여 효율적인 학습 순서 결정
     * @param currentGrades 현재 각 과목 등급
     * @param targetGrades 목표 각 과목 등급
     * @return 추천 학습 순서 (과목 리스트)
     */
    fun determineOptimalStudyOrder(
        currentGrades: Map<String, Int>,
        targetGrades: Map<String, Int>
    ): List<String> {
        val subjects = listOf("국어", "수학", "사회", "통합과학", "영어", "역사", "물리I", "화학I", "생명과학I")

        // 그래프 구성: 인접 행렬
        val adjacencyMatrix = buildAdjacencyMatrix(subjects)

        // 각 과목의 학습 필요도 계산
        val studyPriority = subjects.map { subject ->
            val current = currentGrades[subject] ?: 5
            val target = targetGrades[subject] ?: 5
            val gap = current - target // 음수일수록 더 학습 필요
            gap to subject
        }

        // GNN으로 과목 연관성을 고려한 우선순위 조정
        val input = FloatArray(subjects.size * subjects.size)
        var idx = 0
        for (i in subjects.indices) {
            for (j in subjects.indices) {
                input[idx++] = adjacencyMatrix[i][j].toFloat()
            }
        }

        val output = Array(1) { FloatArray(subjects.size) }
        interpreter.runForMultipleInputsOutputs(arrayOf(input), mapOf(0 to output[0]))

        // 우선순위 계산 및 정렬
        val adjustedPriority = subjects.mapIndexed { index, subject ->
            val importance = output[0][index]
            val studyGap = (currentGrades[subject] ?: 5) - (targetGrades[subject] ?: 5)
            (importance * Math.abs(studyGap).toFloat()) to subject
        }

        return adjustedPriority.sortedByDescending { it.first }.map { it.second }
    }

    /**
     * 선택한 과목 조합에서 부족한 기초 과목 추천
     * @param selectedSubjects 선택한 과목
     * @return 함께 학습해야 할 추천 과목
     */
    fun recommendFoundationSubjects(selectedSubjects: List<String>): List<String> {
        val recommended = mutableSetOf<String>()

        selectedSubjects.forEach { subject ->
            subjectGraph[subject]?.forEach { prerequisite ->
                if (!selectedSubjects.contains(prerequisite)) {
                    recommended.add(prerequisite)
                }
            }
        }

        return recommended.toList()
    }

    /**
     * 학습 순서와 각 과목의 학습 시간 권장도 제공
     * @param subjects 학습할 과목 리스트
     * @param currentGrades 현재 등급
     * @param targetGrades 목표 등급
     * @param totalDailyHours 일일 학습 가능 시간
     * @return 과목별 권장 학습 시간 및 순서
     */
    fun optimizeStudyPlan(
        subjects: List<String>,
        currentGrades: Map<String, Int>,
        targetGrades: Map<String, Int>,
        totalDailyHours: Float
    ): List<Pair<String, Float>> {
        val studyOrder = determineOptimalStudyOrder(currentGrades, targetGrades)

        // 각 과목별 필요한 학습 시간 계산
        val timeAllocation = subjects.map { subject ->
            val current = currentGrades[subject] ?: 5
            val target = targetGrades[subject] ?: 5
            val gap = current - target

            // 등급 차이가 크면 더 많은 시간 할당
            val timeRatio = Math.abs(gap).toFloat() / 4f
            val allocatedTime = totalDailyHours * timeRatio

            subject to allocatedTime
        }

        return timeAllocation.sortedBy { studyOrder.indexOf(it.first) }
    }

    private fun buildAdjacencyMatrix(subjects: List<String>): Array<IntArray> {
        val size = subjects.size
        val matrix = Array(size) { IntArray(size) }

        subjects.forEachIndexed { i, subject ->
            subjectGraph[subject]?.forEach { prerequisite ->
                val j = subjects.indexOf(prerequisite)
                if (j != -1) {
                    matrix[i][j] = 1 // 방향: subject <- prerequisite
                }
            }
        }

        return matrix
    }

    fun release() {
        interpreter.close()
    }
}