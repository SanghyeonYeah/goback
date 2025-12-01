package com.example.studyplanner.ui.objective

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studyplanner.databinding.FragmentObjectiveBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ObjectiveFragment : Fragment() {

    private val viewModel: ObjectiveViewModel by viewModels()
    private lateinit var binding: FragmentObjectiveBinding
    private lateinit var categoryAdapter: ObjectiveCategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentObjectiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        categoryAdapter = ObjectiveCategoryAdapter { subject ->
            showCreateObjectiveDialog(subject)
        }

        binding.categoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        binding.addObjectiveButton.setOnClickListener {
            showCreateObjectiveDialog()
        }

        binding.refreshButton.setOnClickListener {
            viewModel.loadObjectives()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.apply {
                    // Show categories
                    categoryAdapter.submitList(state.categories)

                    // Show objectives
                    val objectiveText = state.objectives.joinToString("\n") { objective ->
                        "${objective.subject}: ${objective.targetGrade}등급"
                    }
                    objectiveListTextView.text = if (objectiveText.isEmpty()) {
                        "등록된 목표가 없습니다."
                    } else {
                        objectiveText
                    }

                    // Loading state
                    loadingProgressBar.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    // Show messages
                    if (state.error != null) {
                        Snackbar.make(root, state.error, Snackbar.LENGTH_SHORT).show()
                        viewModel.clearMessages()
                    }

                    if (state.successMessage != null) {
                        Snackbar.make(root, state.successMessage, Snackbar.LENGTH_SHORT).show()
                        viewModel.clearMessages()
                    }
                }
            }
        }
    }

    private fun showCreateObjectiveDialog(selectedSubject: String = "") {
        val dialog = ObjectiveCreateDialog(
            selectedSubject = selectedSubject,
            onConfirm = { subject, targetGrade ->
                viewModel.addObjective(subject, targetGrade)
            }
        )
        dialog.show(parentFragmentManager, "ObjectiveCreateDialog")
    }
}