package com.example.studyplanner.ui.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplanner.model.CalendarDay
import com.example.studyplanner.network.RetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarViewModel : ViewModel() {

    private val apiService = RetrofitClient.getApiService()

    private val _calendarData = MutableLiveData<Map<String, CalendarDay>>()
    val calendarData: LiveData<Map<String, CalendarDay>> = _calendarData

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val dayStatusMap = mutableMapOf<String, String>()

    fun loadCurrentMonth() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = getToken()
                val calendar = Calendar.getInstance()
                val yearMonth = SimpleDateFormat("yyyy-MM", Locale.KOREA).format(calendar.time)

                val response = apiService.getCalendarMonth("Bearer $token", yearMonth)
                // 응답 처리
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getDayStatus(date: java.time.LocalDate): String? {
        val dateString = date.toString()
        return dayStatusMap[dateString]
    }

    private fun getToken(): String {
        return "your_token_here"
    }
}