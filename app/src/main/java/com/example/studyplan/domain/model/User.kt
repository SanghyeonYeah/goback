package com.yourorg.studyplanner.domain.model

data class User(
    val id: String,
    val name: String,
    val grade: Int, // 1, 2
    val diploma: String, // IT, 공학, 수학, 물리, 화학, 생명과학, IB(자연), 인문학, 국제어문, 사회과학, 경제경영, IB(인문), 예술, 체육
    val targetGrades: Map<String, Int>, // 과목별 목표 등급 (1~9)
    val dailyStudyTime: Int, // 분 단위
    val createdAt: Long
)

data class Subject(
    val name: String,
    val targetGrade: Int,
    val isRequired: Boolean
)