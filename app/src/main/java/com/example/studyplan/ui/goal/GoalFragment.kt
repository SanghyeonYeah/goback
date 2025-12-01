package com.example.studyplanner.ui.goal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.studyplanner.R
import com.example.studyplanner.databinding.FragmentGoalBinding
import com.example.studyplanner.model.GoalRequest
import java.text.SimpleDateFormat
import java.util.*

class GoalFragment : Fragment() {

    private lateinit var binding: FragmentGoalBinding
    private lateinit var viewModel: GoalViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(GoalViewModel::class.java)

        setupGradeSpinners()

        binding.createGoalButton.setOnClickListener {
            validateAndCreateGoal()
        }

        observeViewModel()
    }

    private fun setupGradeSpinners() {
        val grades = (1..9).map { "${it}등급" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, grades)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.koreanGradeSpinner.adapter = adapter
        binding.mathGradeSpinner.adapter = adapter
        binding.englishGradeSpinner.adapter = adapter
        binding.socialGradeSpinner.adapter = adapter
        binding.scienceGradeSpinner.adapter = adapter
        binding.historyGradeSpinner.adapter = adapter
    }

    private fun validateAndCreateGoal() {
        val startDate = binding.startDatePicker.calendar.timeInMillis
        val endDate = binding.endDatePicker.calendar.timeInMillis

        if (startDate >= endDate) {
            Toast.makeText(requireContext(), "시작 날짜가 종료 날짜보다 이전이어야 합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val goalRequest = GoalRequest(
            korean = binding.koreanGradeSpinner.selectedItemPosition + 1,
            math = binding.mathGradeSpinner.selectedItemPosition + 1,
            social = binding.socialGradeSpinner.selectedItemPosition + 1,
            science = binding.scienceGradeSpinner.selectedItemPosition + 1,
            english = binding.englishGradeSpinner.selectedItemPosition + 1,
            history = binding.historyGradeSpinner.selectedItemPosition + 1,
            startDate = startDate,
            endDate = endDate,
            seasonId = "current_season"
        )

        viewModel.createGoal(goalRequest)
    }

    private fun observeViewModel() {
        viewModel.goalCreated.observe(viewLifecycleOwner) { isCreated ->
            if (isCreated) {
                Toast.makeText(requireContext(), "목표가 생성되었습니다.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_goalFragment_to_todoFragment)
            }
        }
    }
}