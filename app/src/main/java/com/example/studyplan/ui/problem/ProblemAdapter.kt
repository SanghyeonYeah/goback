package com.example.studyplanner.ui.problem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplanner.data.repository.ProblemRepository
import com.example.studyplanner.domain.model.Problem
import com.example.studyplanner.domain.model.SubmitAnswerResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProblemUiState(
    val currentProblem: Problem? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val submitResponse: SubmitAnswerResponse? = null,
    val isSubmitted: Boolean = false
)

class ProblemViewModel(
    private val problemRepository: ProblemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProblemUiState())
    val uiState: StateFlow<ProblemUiState> = _uiState.asStateFlow()

    fun loadProblem(problemId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val problem = problemRepository.getProblem(problemId)
                _uiState.value = _uiState.value.copy(
                    currentProblem = problem,
                    isLoading = false,
                    isSubmitted = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun submitAnswer(
        userId: String,
        problemId: Long,
        selectedAnswer: Int,
        timeSpent: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = problemRepository.submitAnswer(userId, problemId, selectedAnswer, timeSpent)
                _uiState.value = _uiState.value.copy(
                    submitResponse = response,
                    isSubmitted = true,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun reset() {
        _uiState.value = ProblemUiState()
    }
}