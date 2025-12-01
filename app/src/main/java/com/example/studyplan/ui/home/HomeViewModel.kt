package com.example.studyplanner.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplanner.model.*
import com.example.studyplanner.network.RetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel : ViewModel() {

    private val apiService = RetrofitClient.getApiService()

    private val _currentGoal = MutableLiveData<Goal?>()
    val currentGoal: LiveData<Goal?> = _currentGoal

    private val _dailyTodos = MutableLiveData<List<Todo>>()
    val dailyTodos: LiveData<List<Todo>> = _dailyTodos

    private val _seasonRanking = MutableLiveData<List<SeasonRanking>>()
    val seasonRanking: LiveData<List<SeasonRanking>> = _seasonRanking

    private val _dailyRanking = MutableLiveData<List<DailyRanking>>()
    val dailyRanking: LiveData<List<DailyRanking>> = _dailyRanking

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadCurrentGoal() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = getToken()
                val response = apiService.getCurrentGoal("Bearer $token")
                if (response.isSuccessful) {
                    _currentGoal.value = response.body()?.data
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadDailyTodos() {
        viewModelScope.launch {
            try {
                val token = getToken()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
                val response = apiService.getTodosByDate("Bearer $token", today)
                if (response.isSuccessful) {
                    _dailyTodos.value = response.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadRankings() {
        viewModelScope.launch {
            try {
                val token = getToken()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())

                val seasonResponse = apiService.getDailyRanking("Bearer $token", today)
                val dailyResponse = apiService.getDailyRanking("Bearer $token", today)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getToken(): String {
        // SharedPreferences에서 토큰 가져오기
        return "your_token_here"
    }
}