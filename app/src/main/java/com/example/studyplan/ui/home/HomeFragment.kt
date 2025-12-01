package com.example.studyplanner.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studyplanner.R
import com.example.studyplanner.databinding.FragmentHomeBinding
import com.example.studyplanner.util.calculateDday
import com.example.studyplanner.util.toKoreanDate

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: HomeViewModel

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
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        binding.problemButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_problemFragment)
        }

        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        }

        observeViewModel()
        viewModel.loadCurrentGoal()
        viewModel.loadDailyTodos()
        viewModel.loadRankings()
    }

    private fun observeViewModel() {
        viewModel.currentGoal.observe(viewLifecycleOwner) { goal ->
            if (goal != null) {
                binding.goalNameTextView.text = "현재 목표"
                binding.ddayTextView.text = "D-${goal.endDate.calculateDday()}"
            }
        }

        viewModel.dailyTodos.observe(viewLifecycleOwner) { todos ->
            val completedCount = todos.count { it.isCompleted }
            val totalCount = todos.size
            binding.todayProgressTextView.text = "$completedCount / $totalCount 완료"
            binding.progressBar.progress = if (totalCount > 0) (completedCount * 100) / totalCount else 0
        }

        viewModel.seasonRanking.observe(viewLifecycleOwner) { ranking ->
            if (ranking.isNotEmpty()) {
                val topUser = ranking.first()
                binding.seasonRankingTextView.text = "1등: ${topUser.userId} (${topUser.totalScore}점)"
            }
        }

        viewModel.dailyRanking.observe(viewLifecycleOwner) { ranking ->
            if (ranking.isNotEmpty()) {
                val topUser = ranking.first()
                binding.dailyRankingTextView.text = "오늘 1등: ${topUser.userId} (${topUser.dailyScore}점)"
            }
        }
    }
}