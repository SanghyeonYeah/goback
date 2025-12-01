package com.example.studyplanner.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studyplanner.databinding.FragmentHomeBinding
import com.example.studyplanner.ui.ranking.RankingItemAdapter
import com.example.studyplanner.util.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var binding: FragmentHomeBinding
    private lateinit var dailyRankingAdapter: RankingItemAdapter
    private lateinit var seasonRankingAdapter: RankingItemAdapter
    private lateinit var todoAdapter: HomeTodoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Setup daily todo list adapter
        todoAdapter = HomeTodoAdapter { itemId, isCompleted ->
            // Handle todo completion
        }

        binding.todoListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todoAdapter
        }

        // Setup daily ranking adapter
        dailyRankingAdapter = RankingItemAdapter { rankingId ->
            onRankingItemClicked(rankingId)
        }

        binding.dailyRankingRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dailyRankingAdapter
        }

        // Setup season ranking adapter
        seasonRankingAdapter = RankingItemAdapter { rankingId ->
            onRankingItemClicked(rankingId)
        }

        binding.seasonRankingRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = seasonRankingAdapter
        }

        binding.refreshButton.setOnClickListener {
            viewModel.refreshRankings()
        }

        // Display current date and time
        binding.dateTextView.text = DateUtils.getCurrentDate()
        binding.timeTextView.text = DateUtils.getCurrentTime()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.apply {
                    // User info
                    state.user?.let {
                        userNameTextView.text = it.nickname
                        gradeTextView.text = "학년: ${it.grade}"
                        diplomaTextView.text = "전공: ${it.diploma}"
                        remainingStudyTimeTextView.text = "남은 공부 시간: ${state.remainingStudyTime}분"
                    }

                    // Today completion status
                    if (state.todayCompletion) {
                        completionStatusTextView.text = "✓ 오늘 완료"
                        completionStatusTextView.setTextColor(android.graphics.Color.GREEN)
                    } else {
                        completionStatusTextView.text = "✗ 오늘 미완료"
                        completionStatusTextView.setTextColor(android.graphics.Color.RED)
                    }

                    // Update todo list
                    val todoItems = state.dailyRankings.take(5).map { ranking ->
                        HomeTodoItem(
                            id = ranking.id,
                            subject = ranking.studentId,
                            durationMinutes = 60,
                            timeSlot = "현재",
                            isCompleted = false
                        )
                    }
                    todoAdapter.submitList(todoItems)

                    // Update rankings
                    dailyRankingAdapter.submitList(state.dailyRankings)
                    seasonRankingAdapter.submitList(state.seasonRankings)

                    // Loading state
                    loadingProgressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Error message
                    if (state.error != null) {
                        errorMessageTextView.visibility = View.VISIBLE
                        errorMessageTextView.text = state.error
                    }
                }
            }
        }
    }

    private fun onRankingItemClicked(rankingId: String) {
        // Navigate to PVP or profile screen
    }
}