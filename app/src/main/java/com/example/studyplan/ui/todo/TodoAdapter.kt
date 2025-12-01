package com.example.studyplanner.ui.objective

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplanner.domain.model.Objective
import com.example.studyplanner.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ObjectiveUiState(
    val isLoading: Boolean = false,
    val objectives: List<Objective> = emptyList(),
    val categories: List<String> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ObjectiveViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObjectiveUiState())
    val uiState: StateFlow<ObjectiveUiState> = _uiState

    init {
        loadObjectives()
    }

    fun loadObjectives() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val user = userRepository.getCurrentUser()
                val objectives = userRepository.getUserObjectives(user.id)
                val categories = getSubjectCategories(user.grade)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    objectives = objectives,
                    categories = categories
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "목표 불러오기 실패"
                )
            }
        }
    }

    fun addObjective(subject: String, targetGrade: Int) {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                val newObjective = Objective(
                    id = System.currentTimeMillis().toString(),
                    userId = user.id,
                    subject = subject,
                    targetGrade = targetGrade
                )
                // Save to repository
                loadObjectives()
                _uiState.value = _uiState.value.copy(
                    successMessage = "$subject 목표가 설정되었습니다."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "목표 추가 실패"
                )
            }
        }
    }

    fun updateObjective(objectiveId: String, targetGrade: Int) {
        viewModelScope.launch {
            try {
                // Update objective
                loadObjectives()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "목표 수정 실패"
                )
            }
        }
    }

    fun deleteObjective(objectiveId: String) {
        viewModelScope.launch {
            try {
                // Delete objective
                loadObjectives()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "목표 삭제 실패"
                )
            }
        }
    }

    private fun getSubjectCategories(grade: Int): List<String> {
        return when (grade) {
            1 -> listOf("국어", "수학", "사회", "통합과학", "영어", "역사")
            2 -> listOf("국어", "수학", "사회", "통합과학", "영어", "역사", "물리I", "화학I", "생명과학I")
            3 -> listOf("국어", "수학", "사회", "영어", "역사", "물리I", "화학I", "생명과학I")
            else -> emptyList()
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }
}