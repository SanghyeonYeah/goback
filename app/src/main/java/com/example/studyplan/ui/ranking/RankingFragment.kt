package com.example.studyplanner.ui.ranking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RankingFragment : Fragment() {

    private val viewModel: RankingViewModel by viewModels()

    private lateinit var rankingRecyclerView: RecyclerView
    private lateinit var dailyButton: Button
    private lateinit var seasonButton: Button

    private var rankingAdapter: RankingItemAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ranking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뷰 초기화
        rankingRecyclerView = view.findViewById(R.id.ranking_recycler_view)
        dailyButton = view.findViewById(R.id.btn_daily_ranking)
        seasonButton = view.findViewById(R.id.btn_season_ranking)

        // RecyclerView 설정
        rankingRecyclerView.layoutManager = LinearLayoutManager(context)

        // 버튼 리스너
        dailyButton.setOnClickListener {
            viewModel.loadRankings("2024-SPRING", "DAILY")
        }

        seasonButton.setOnClickListener {
            viewModel.loadRankings("2024-SPRING", "SEASON")
        }

        // UI 상태 감시
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }

        // 초기 데이터 로드
        viewModel.loadRankings("2024-SPRING", "DAILY")
    }

    private fun updateUI(state: RankingUiState) {
        when {
            state.isLoading -> {
                // TODO: 로딩 인디케이터 표시
            }
            state.error != null -> {
                // TODO: 에러 메시지 표시
            }
            else -> {
                rankingAdapter = RankingItemAdapter(
                    items = state.rankings,
                    onItemClick = { item ->
                        // 랭킹 항목 클릭
                        // TODO: 사용자 프로필 화면으로 이동
                    },
                    onChallengeClick = { item ->
                        // PVP 도전
                        // TODO: PVP 매칭 시작
                    }
                )
                rankingRecyclerView.adapter = rankingAdapter

                // 사용자 랭킹 표시
                state.userRank?.let {
                    // TODO: 사용자 랭킹 섹션 업데이트
                }
            }
        }
    }
}