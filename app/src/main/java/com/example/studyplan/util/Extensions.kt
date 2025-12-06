package com.example.studyplanner.util

import android.text.SpannableString
import android.text.style.StrikethroughSpan
import java.text.SimpleDateFormat
import java.util.*

fun String.strikethrough(): SpannableString {
    return SpannableString(this).apply {
        setSpan(StrikethroughSpan(), 0, length, 0)
    }
}

fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    return sdf.format(Date(this))
}

fun Long.toFormattedDateTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
    return sdf.format(Date(this))
}

fun Long.toKoreanDate(): String {
    val sdf = SimpleDateFormat("M월 d일", Locale.KOREA)
    return sdf.format(Date(this))
}

fun String.toTimeInMillis(): Long {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    return sdf.parse(this)?.time ?: 0L
}

fun Long.calculateDday(): Int {
    val todayInMillis = System.currentTimeMillis()
    val diffInMillis = this - todayInMillis
    return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
}

fun Int.gradeToString(): String {
    return when (this) {
        1 -> "1등급"
        2 -> "2등급"
        3 -> "3등급"
        4 -> "4등급"
        5 -> "5등급"
        6 -> "6등급"
        7 -> "7등급"
        8 -> "8등급"
        9 -> "9등급"
        else -> "미정"
    }
}

fun String.diplomaToCategory(): String {
    return when (this) {
        in listOf("IT", "공학", "수학", "물리", "화학", "생명과학", "IB(자연)") -> "이과"
        in listOf("인문학", "국제어문", "사회과학", "경제경영", "IB(인문)") -> "문과"
        in listOf("예술", "체육") -> "예체"
        else -> "기타"
    }
}

fun String.scoreAdjustment(diploma: String, subject: String, baseScore: Int): Int {
    val category = diploma.diplomaToCategory()
    val scienceSubjects = listOf("물리", "화학", "생명과학", "통합과학")
    val liberalSubjects = listOf("국어", "영어", "역사", "사회")

    return when {
        category == "이과" -> {
            when {
                subject in scienceSubjects -> baseScore
                else -> (baseScore * 0.9).toInt()
            }
        }
        category == "문과" -> {
            when {
                subject in liberalSubjects -> baseScore
                else -> (baseScore * 1.1).toInt()
            }
        }
        category == "예체" -> (baseScore * 1.1).toInt()
        else -> baseScore
    }
}

fun getSubjectsByGrade(grade: Int): List<String> {
    return when (grade) {
        1 -> listOf("국어", "수학", "사회", "통합과학", "영어", "역사")
        2, 3 -> listOf("국어", "수학", "사회", "통합과학", "영어", "역사", "물리", "화학", "생명과학")
        else -> emptyList()
    }
}

fun Int.percentToBonus(): Int = if (this >= 100) 10 else 0