package com.example.studyplanner.ui.problem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProblemFragment : Fragment() {

    private val viewModel: ProblemViewModel by viewModels()
    private var startTime: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_problem, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startTime = System.currentTimeMillis()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }

        // 문제 로드
        arguments?.getLong("problemId")?.let { problemId ->
            viewModel.loadProblem(problemId)
        }
    }

    private fun updateUI(state: ProblemUiState) {
        // UI 업데이트 로직
        // state.currentProblem -> 문제 표시
        // state.isLoading -> 로딩 인디케이터
        // state.submitResponse -> 정답 표시
        // state.isSubmitted -> 제출 완료 화면
    }

    fun submitAnswer(selectedAnswer: Int) {
        arguments?.getLong("problemId")?.let { problemId ->
            val timeSpent = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            viewModel.submitAnswer(
                userId = "current_user_id", // TODO: 현재 사용자 ID 가져오기
                problemId = problemId,
                selectedAnswer = selectedAnswer,
                timeSpent = timeSpent
            )
        }
    }
}