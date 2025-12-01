package com.example.studyplanner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplanner.data.repository.UserRepository
import com.example.studyplanner.data.repository.ProblemRepository
import com.example.studyplanner.data.repository.RankingRepository
import com.example.studyplanner.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val user: User? = null,
    val dailyRank: Int = 0,
    val dailyPoints: Int = 0,
    val seasonRank: Int = 0,
    val seasonPoints: Int = 0,
    val todayProgress: Float = 0f,
    val remainingStudyTime: Int = 0, // 분 단위
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val userRepository: UserRepository,
    private val rankingRepository: RankingRepository,
    private val problemRepository: ProblemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val currentUser = userRepository.getCurrentUser()
                if (currentUser != null) {
                    val rankingInfo = rankingRepository.getUserRanking(currentUser.userId)

                    _uiState.value = _uiState.value.copy(
                        user = currentUser,
                        dailyRank = rankingInfo?.dailyRank ?: 0,
                        dailyPoints = rankingInfo?.dailyPoints ?: 0,
                        seasonRank = rankingInfo?.seasonRank ?: 0,
                        seasonPoints = rankingInfo?.seasonPoints ?: 0,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun refresh() {
        loadHomeData()
    }
}