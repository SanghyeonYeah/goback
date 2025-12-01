package com.example.studyplanner.ui.problem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studyplanner.databinding.FragmentProblemBinding

class ProblemFragment : Fragment() {

    private lateinit var binding: FragmentProblemBinding
    private lateinit var viewModel: ProblemViewModel
    private lateinit var adapter: ProblemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProblemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(ProblemViewModel::class.java)

        adapter = ProblemAdapter { problem ->
            viewModel.solveProblem(problem.id)
        }

        binding.problemRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.problemRecyclerView.adapter = adapter

        setupSubjectFilter()
        observeViewModel()
        viewModel.loadProblems()
    }

    private fun setupSubjectFilter() {
        val subjects = listOf("모든 과목", "국어", "수학", "영어", "사회", "과학", "역사")
        binding.subjectFilterSpinner.setOnItemSelectedListener { position ->
            val selectedSubject = if (position == 0) null else subjects[position]
            viewModel.filterBySubject(selectedSubject)
        }
    }

    private fun observeViewModel() {
        viewModel.problems.observe(viewLifecycleOwner) { problems ->
            adapter.submitList(problems)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.solveResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                val message = if (it.isCorrect) {
                    "정답입니다! +${it.pointsEarned}점"
                } else {
                    "오답입니다."
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}