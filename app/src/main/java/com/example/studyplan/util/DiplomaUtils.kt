package com.yourorg.studyplanner.util

/**
 * Diploma Utilities
 * 디플로마 관련 유틸리티
 */
object DiplomaUtils {

    // 디플로마 분류
    private val scienceDiplomas = setOf("IT", "공학", "수학", "물리", "화학", "생명과학", "IB(자연)")
    private val humanitiesDiplomas = setOf("인문학(문학/사학/철학)", "국제어문", "사회과학", "경제경영", "IB(인문)")
    private val artsPhysicalDiplomas = setOf("예술", "체육")

    /**
     * 디플로마 타입 반환
     */
    fun getDiplomaType(diploma: String): String {
        return when (diploma) {
            in scienceDiplomas -> "SCIENCE"
            in humanitiesDiplomas -> "HUMANITIES"
            in artsPhysicalDiplomas -> "ARTS_PHYSICAL"
            else -> "UNKNOWN"
        }
    }

    /**
     * 디플로마에 맞는 과목 리스트 반환
     */
    fun getRequiredSubjects(grade: Int, diploma: String): List<String> {
        val baseSubjects = when (grade) {
            1 -> listOf("국어", "수학", "사회", "통합과학", "영어", "역사")
            2, 3 -> listOf("국어", "수학", "사회", "통합과학", "영어", "역사", "물리I", "화학I", "생명과학I")
            else -> listOf("국어", "수학", "영어")
        }

        return baseSubjects
    }

    /**
     * 디플로마별 추천 전공 과목
     */
    fun getSpecializationSubjects(diploma: String): List<String> {
        return when (diploma) {
            "IT" -> listOf("물리I", "화학I", "수학")
            "공학" -> listOf("물리I", "화학I", "수학")
            "수학" -> listOf("수학")
            "물리" -> listOf("물리I", "수학")
            "화학" -> listOf("화학I", "수학")
            "생명과학" -> listOf("생명과학I", "화학I")
            "IB(자연)" -> listOf("물리I", "화학I", "생명과학I", "수학")
            "인문학(문학/사학/철학)" -> listOf("국어", "역사", "사회")
            "국제어문" -> listOf("국어", "영어")
            "사회과학" -> listOf("사회", "영어")
            "경제경영" -> listOf("수학", "사회")
            "IB(인문)" -> listOf("국어", "사회", "영어", "역사")
            "예술" -> listOf("국어", "사회")
            "체육" -> listOf("국어", "사회")
            else -> emptyList()
        }
    }

    /**
     * 점수 조정 계수 계산
     */
    fun getScoreMuliplier(diploma: String, subject: String): Float {
        return when (diploma) {
            in scienceDiplomas -> {
                when (subject) {
                    in listOf("물리I", "화학I", "생명과학I") -> 1.0f
                    in listOf("국어", "사회", "역사") -> 0.9f
                    else -> 1.0f
                }
            }
            in humanitiesDiplomas -> {
                when (subject) {
                    in listOf("국어", "사회", "역사") -> 1.0f
                    in listOf("물리I", "화학I", "생명과학I") -> 1.1f
                    else -> 1.0f
                }
            }
            in artsPhysicalDiplomas -> 1.1f
            else -> 1.0f
        }
    }

    /**
     * 학년별 수강 과목 선택 가능 여부
     */
    fun canSelectSubject(grade: Int, subject: String): Boolean {
        return when (grade) {
            1 -> subject in listOf("국어", "수학", "사회", "통합과학", "영어", "역사")
            2, 3 -> subject in listOf("국어", "수학", "사회", "통합과학", "영어", "역사", "물리I", "화학I", "생명과학I")
            else -> false
        }
    }

    /**
     * 학년별 전체 과목 리스트
     */
    fun getAllSubjectsForGrade(grade: Int): List<String> {
        return when (grade) {
            1 -> listOf("국어", "수학", "사회", "통합과학", "영어", "역사")
            2, 3 -> listOf("국어", "수학", "사회", "통합과학", "영어", "역사", "물리I", "화학I", "생명과학I")
            else -> emptyList()
        }
    }

    /**
     * 디플로마 리스트 (카테고리별)
     */
    fun getAllDiplomas(): Map<String, List<String>> {
        return mapOf(
            "이과" to scienceDiplomas.toList(),
            "문과" to humanitiesDiplomas.toList(),
            "예체" to artsPhysicalDiplomas.toList()
        )
    }
}