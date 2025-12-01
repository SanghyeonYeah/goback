package com.example.studyplanner.ui.pvp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplanner.model.Problem
import com.example.studyplanner.network.RetrofitClient
import kotlinx.coroutines.launch

class PvpViewModel : ViewModel() {

    private val apiService = RetrofitClient.getApiService()

    private val _problem = MutableLiveData<Problem?>()
    val problem: LiveData<Problem?> = _problem

    private val _matchResult = MutableLiveData<Map<String, Any>?>()
    val matchResult: LiveData<Map<String, Any>?> = _matchResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadPvpProblem(matchId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = getToken()
                val response = apiService.getPvpProblem("Bearer $token", matchId)
                if (response.isSuccessful) {
                    _problem.value = response.body()?.data
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitAnswer(matchId: String, answer: String) {
        viewModelScope.launch {
            try {
                val token = getToken()
                val answerMap = mapOf("answer" to answer)
                val response = apiService.submitPvpAnswer("Bearer $token", matchId, answerMap)
                if (response.isSuccessful) {
                    _matchResult.value = response.body()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getToken(): String {
        return "your_token_here"
    }
}