package com.example.studyplanner.ui.pvp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.studyplanner.R
import com.example.studyplanner.databinding.FragmentPvpBinding
import java.util.*

class PvpFragment : Fragment() {

    private lateinit var binding: FragmentPvpBinding
    private lateinit var viewModel: PvpViewModel
    private var timeRemaining = 300 // 5분
    private var timer: Timer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPvpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(PvpViewModel::class.java)

        val matchId = arguments?.getString("matchId")
        if (matchId != null) {
            viewModel.loadPvpProblem(matchId)
        }

        binding.submitAnswerButton.setOnClickListener {
            val answer = binding.answerEditText.text.toString().trim()
            if (answer.isEmpty()) {
                Toast.makeText(requireContext(), "답을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.submitAnswer(matchId ?: "", answer)
        }

        observeViewModel()
        startTimer()
    }

    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                timeRemaining--
                binding.timerTextView.text = "${timeRemaining / 60}:${String.format("%02d", timeRemaining % 60)}"

                if (timeRemaining <= 0) {
                    timer?.cancel()
                    Toast.makeText(requireContext(), "시간 초과! 문제를 풀지 못했습니다.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }, 0, 1000)
    }

    private fun observeViewModel() {
        viewModel.problem.observe(viewLifecycleOwner) { problem ->
            if (problem != null) {
                binding.problemContentTextView.text = problem.content
                if (!problem.options.isNullOrEmpty()) {
                    binding.optionsTextView.text = problem.options.joinToString("\n")
                }
            }
        }

        viewModel.matchResult.observe(viewLifecycleOwner) { result ->
            timer?.cancel()
            if (result != null) {
                val message = when {
                    result.containsKey("winner") -> "결과: ${result["winner"]}"
                    result.containsKey("draw") -> "무승부!"
                    else -> "PVP 완료"
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }
}