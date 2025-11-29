package com.yourorg.studyplanner.util

/**
 * Study Time Utilities
 * 학습 시간 배분 관련 유틸리티
 */
object StudyTimeUtils {

    /**
     * 목표 등급 차이에 따른 가중치 계산
     */
    fun calculateGradeWeight(currentGrade: Int, targetGrade: Int): Float {
        val gap = currentGrade - targetGrade // 양수일수록 학습 필요
        return when {
            gap <= 0 -> 0.5f // 이미 목표 도달
            gap == 1 -> 1.0f
            gap == 2 -> 1.5f
            gap == 3 -> 2.0f
            gap == 4 -> 2.5f
            gap >= 5 -> 3.0f
            else -> 1.0f
        }
    }

    /**
     * 전체 학습 시간에서 과목별 시간 배분
     */
    fun allocateTimeBySubjects(
        totalHours: Float,
        targetGrades: Map<String, Int>,
        subjects: List<String>
    ): Map<String, Float> {
        val weights = subjects.associate { subject ->
            val currentGrade = 5 // 평균 5등급 가정
            val targetGrade = targetGrades[subject] ?: 5
            subject to calculateGradeWeight(currentGrade, targetGrade)
        }

        val totalWeight = weights.values.sum()

        return weights.mapValues { (_, weight) ->
            if (totalWeight > 0) totalHours * (weight / totalWeight) else totalHours / subjects.size
        }
    }

    /**
     * 면학 시간 기반 학습 계획 생성
     * - 면학 시간: [면학 시간 - 과목 공부 시간]
     * - 비면학 시간: [과목 공부 시간]
     */
    fun calculateStudyTimeForWeekday(
        isInSession: Boolean,
        sessionHours: Float,
        outOfSessionHours: Float
    ): Float {
        return if (isInSession) {
            sessionHours - outOfSessionHours // 면학 시간에서 공부 시간 제외
        } else {
            outOfSessionHours
        }
    }

    /**
     * 주간 학습 시간 계획
     */
    fun generateWeeklySchedule(
        subjectTimeMap: Map<String, Float>,
        isWeekdayInSession: Boolean,
        weekdayOutOfSessionHours: Float,
        weekendHours: Float
    ): Map<String, Map<String, Float>> {
        val weeklyPlan = mutableMapOf<String, Map<String, Float>>()
        val subjects = subjectTimeMap.keys.toList()

        // 평일 5일
        for (day in 1..5) {
            val dayName = arrayOf("월", "화", "수", "목", "금")[day - 1]
            val dailyTime = calculateStudyTimeForWeekday(
                isWeekdayInSession,
                5f, // 면학 5시간 가정
                weekdayOutOfSessionHours / 5
            )

            val dayPlan = allocateTimeBySubjects(dailyTime, emptyMap(), subjects)
                .filterValues { it > 0 }
            weeklyPlan[dayName] = dayPlan
        }

        // 토요일
        val saturdayPlan = allocateTimeBySubjects(
            weekendHours / 2,
            emptyMap(),
            subjects
        ).filterValues { it > 0 }
        weeklyPlan["토"] = saturdayPlan

        // 일요일
        val sundayPlan = allocateTimeBySubjects(
            weekendHours / 2,
            emptyMap(),
            subjects
        ).filterValues { it > 0 }
        weeklyPlan["일"] = sundayPlan

        return weeklyPlan
    }

    /**
     * 1등급 학생의 과목별 학습 시간 비율
     */
    fun getTopStudentTimeRatio(): Map<String, Float> {
        return mapOf(
            "국어" to 0.12f,
            "수학" to 0.18f,
            "사회" to 0.10f,
            "통합과학" to 0.08f,
            "영어" to 0.15f,
            "역사" to 0.09f,
            "물리I" to 0.08f,
            "화학I" to 0.10f,
            "생명과학I" to 0.10f
        )
    }

    /**
     * 시간 문자열 포맷 (분 → "시간 분" 형식)
     */
    fun formatTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}시간 ${mins}분"
            hours > 0 -> "${hours}시간"
            else -> "${mins}분"
        }
    }
}