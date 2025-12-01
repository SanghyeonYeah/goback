package com.example.studyplanner.ui.problem

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studyplanner.databinding.ItemProblemBinding
import com.example.studyplanner.model.Problem

class ProblemAdapter(
    private val onSolveClick: (Problem) -> Unit
) : ListAdapter<Problem, ProblemAdapter.ProblemViewHolder>(ProblemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProblemViewHolder {
        val binding = ItemProblemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProblemViewHolder(binding, onSolveClick)
    }

    override fun onBindViewHolder(holder: ProblemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProblemViewHolder(
        private val binding: ItemProblemBinding,
        private val onSolveClick: (Problem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(problem: Problem) {
            binding.problemNumberTextView.text = "문제 ${problem.id}"
            binding.subjectTextView.text = problem.subject
            binding.pointsTextView.text = "${problem.points}점"
            binding.contentTextView.text = problem.content

            binding.solveButton.setOnClickListener {
                onSolveClick(problem)
            }
        }
    }

    class ProblemDiffCallback : DiffUtil.ItemCallback<Problem>() {
        override fun areItemsTheSame(oldItem: Problem, newItem: Problem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Problem, newItem: Problem): Boolean {
            return oldItem == newItem
        }
    }
}