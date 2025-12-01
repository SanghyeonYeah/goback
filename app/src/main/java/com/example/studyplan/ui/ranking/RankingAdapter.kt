package com.example.studyplanner.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplanner.data.repository.RankingRepository
import com.example.studyplanner.domain.model.RankingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RankingUiState(
    val rankings: List<RankingItem> = emptyList(),
    val userRank: RankingItem? = null,
    val rankType: String = "DAILY",
    val isLoading: Boolean = false,
    val error: String? = null
)

class RankingViewModel(
    private val rankingRepository: RankingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    fun loadRankings(seasonId: String, rankType: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                rankType = rankType,
                isLoading = true,
                error = null
            )

            try {
                val result = when (rankType.uppercase()) {
                    "DAILY" -> rankingRepository.getDailyRanking(seasonId)
                    "SEASON" -> rankingRepository.getSeasonRanking(seasonId)
                    else -> null
                }

                if (result != null) {
                    _uiState.value = _uiState.value.copy(
                        rankings = result.rankings,
                        userRank = result.userRank,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Invalid rank type",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Unknown error",
                    isLoading = false
                )
            }
        }
    }

    fun switchRankType(seasonId: String, rankType: String) {
        loadRankings(seasonId, rankType)
    }

    fun refreshRankings(seasonId: String) {
        loadRankings(seasonId, _uiState.value.rankType)
    }
}