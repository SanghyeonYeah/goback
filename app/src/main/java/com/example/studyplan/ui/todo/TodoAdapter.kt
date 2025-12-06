package com.example.studyplanner.ui.todo

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studyplanner.databinding.ItemTodoBinding
import com.example.studyplanner.model.Todo
import com.example.studyplanner.util.strikethrough

class TodoAdapter(
    private val onStatusChanged: (Todo, Boolean) -> Unit
) : ListAdapter<Todo, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding, onStatusChanged)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TodoViewHolder(
        private val binding: ItemTodoBinding,
        private val onStatusChanged: (Todo, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(todo: Todo) {
            binding.todoContentTextView.text = todo.content
            binding.todoSubjectTextView.text = todo.subject
            binding.todoCheckBox.isChecked = todo.isCompleted

            if (todo.isCompleted) {
                binding.todoContentTextView.text = todo.content.strikethrough()
                binding.todoContentTextView.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.todoContentTextView.text = todo.content
                binding.todoContentTextView.paintFlags = 0
            }

            binding.todoCheckBox.setOnCheckedChangeListener { _, isChecked ->
                onStatusChanged(todo, isChecked)
            }
        }
    }

    class TodoDiffCallback : DiffUtil.ItemCallback<Todo>() {
        override fun areItemsTheSame(oldItem: Todo, newItem: Todo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Todo, newItem: Todo): Boolean {
            return oldItem == newItem
        }
    }
}