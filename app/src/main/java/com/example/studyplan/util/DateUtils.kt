package com.yourorg.studyplanner.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Date Utilities
 * 날짜/시간 관련 유틸리티 함수
 */
object DateUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)

    /**
     * 오늘 날짜 문자열 반환
     */
    fun getTodayString(): String = dateFormat.format(Date())

    /**
     * 특정 날짜의 요일 반환
     */
    fun getDayOfWeek(dateString: String): String {
        val date = dateFormat.parse(dateString) ?: return ""
        val calendar = Calendar.getInstance().apply { time = date }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "일"
            Calendar.MONDAY -> "월"
            Calendar.TUESDAY -> "화"
            Calendar.WEDNESDAY -> "수"
            Calendar.THURSDAY -> "목"
            Calendar.FRIDAY -> "금"
            Calendar.SATURDAY -> "토"
            else -> ""
        }
    }

    /**
     * 날짜 문자열이 평일인지 확인
     */
    fun isWeekday(dateString: String): Boolean {
        val date = dateFormat.parse(dateString) ?: return false
        val calendar = Calendar.getInstance().apply { time = date }
        return calendar.get(Calendar.DAY_OF_WEEK) !in listOf(Calendar.SUNDAY, Calendar.SATURDAY)
    }

    /**
     * 날짜 문자열이 주말인지 확인
     */
    fun isWeekend(dateString: String): Boolean = !isWeekday(dateString)

    /**
     * 두 날짜 사이의 일수 계산
     */
    fun daysBetween(startDate: String, endDate: String): Long {
        val start = dateFormat.parse(startDate) ?: return 0
        val end = dateFormat.parse(endDate) ?: return 0
        return (end.time - start.time) / (1000 * 60 * 60 * 24)
    }

    /**
     * 날짜에 일수 추가
     */
    fun addDays(dateString: String, days: Int): String {
        val date = dateFormat.parse(dateString) ?: return ""
        val calendar = Calendar.getInstance().apply {
            time = date
            add(Calendar.DAY_OF_MONTH, days)
        }
        return dateFormat.format(calendar.time)
    }

    /**
     * 현재 시즌 ID 반환 (분기 기반)
     */
    fun getCurrentSeasonId(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val quarter = (month - 1) / 3 + 1
        return "${year}Q$quarter"
    }

    /**
     * 시즌의 시작 날짜 반환
     */
    fun getSeasonStartDate(seasonId: String): String {
        val year = seasonId.substring(0, 4).toInt()
        val quarter = seasonId.substring(5).toInt()
        val month = (quarter - 1) * 3 + 1
        return "$year-${String.format("%02d", month)}-01"
    }

    /**
     * 시즌의 종료 날짜 반환
     */
    fun getSeasonEndDate(seasonId: String): String {
        val year = seasonId.substring(0, 4).toInt()
        val quarter = seasonId.substring(5).toInt()
        val month = quarter * 3
        val lastDay = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            else -> 31
        }
        return "$year-${String.format("%02d", month)}-$lastDay"
    }

    /**
     * 남은 시즌 주수 계산
     */
    fun getWeeksRemainingInSeason(): Int {
        val today = Date()
        val seasonEnd = dateFormat.parse(getSeasonEndDate(getCurrentSeasonId())) ?: return 0
        val daysDiff = (seasonEnd.time - today.time) / (1000 * 60 * 60 * 24)
        return (daysDiff / 7).toInt()
    }
}