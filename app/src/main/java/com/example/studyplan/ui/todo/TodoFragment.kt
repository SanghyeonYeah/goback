package com.example.studyplanner.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studyplanner.R
import com.example.studyplanner.databinding.FragmentTodoBinding
import com.example.studyplanner.model.Todo
import java.text.SimpleDateFormat
import java.util.*

class TodoFragment : Fragment() {

    private lateinit var binding: FragmentTodoBinding
    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTodoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(TodoViewModel::class.java)

        adapter = TodoAdapter { todo, isCompleted ->
            viewModel.updateTodoStatus(todo.id, isCompleted)
        }

        binding.todoRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.todoRecyclerView.adapter = adapter

        binding.addTodoButton.setOnClickListener {
            // Todo 추가 다이얼로그 표시
            Toast.makeText(requireContext(), "할 일 추가 기능", Toast.LENGTH_SHORT).show()
        }

        observeViewModel()
        viewModel.loadTodosForToday()
    }

    private fun observeViewModel() {
        viewModel.todos.observe(viewLifecycleOwner) { todos ->
            adapter.submitList(todos)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}