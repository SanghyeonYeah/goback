package com.cnsa.studyplanner.ui.plan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cnsa.studyplanner.databinding.FragmentPlanBinding
import com.cnsa.studyplanner.data.model.Todo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PlanFragment : Fragment() {
    
    private var _binding: FragmentPlanBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: PlanViewModel by viewModels()
    private lateinit var todoAdapter: TodoAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlanBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupObservers()
        loadData()
    }
    
    private fun setupViews() {
        // 뒤로가기
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
        
        // 진행률 표시
        updateProgress()
        
        // Todo RecyclerView 설정
        setupTodoRecyclerView()
        
        // 날짜 선택 (현재는 오늘 날짜)
        binding.tvDate.text = getCurrentDate()
    }
    
    private fun setupTodoRecyclerView() {
        todoAdapter = TodoAdapter(
            onTodoClick = { todo ->
                // Todo 상세 보기 또는 수정
                showTodoDetail(todo)
            },
            onTodoComplete = { todo ->
                // Todo 완료 처리
                completeTodo(todo)
            }
        )
        
        binding.rvTodos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = todoAdapter
        }
    }
    
    private fun setupObservers() {
        // Todo 목록
        viewModel.todos.observe(viewLifecycleOwner) { todos ->
            if (todos.isEmpty()) {
                binding.rvTodos.visibility = View.GONE
                binding.tvEmptyMessage.visibility = View.VISIBLE
            } else {
                binding.rvTodos.visibility = View.VISIBLE
                binding.tvEmptyMessage.visibility = View.GONE
                
                // 날짜별로 그룹화
                val groupedTodos = groupTodosByDate(todos)
                todoAdapter.submitList(groupedTodos)
            }
        }
        
        // 진행률
        viewModel.totalTodos.observe(viewLifecycleOwner) { total ->
            viewModel.completedTodos.observe(viewLifecycleOwner) { completed ->
                val percentage = if (total > 0) (completed * 100) / total else 0
                binding.progressBar.progress = percentage
                binding.tvProgressPercentage.text = "${percentage}%"
                binding.tvTotalTodos.text = "총 ${total}개의 할일"
            }
        }
        
        // 로딩 상태
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
    
    private fun loadData() {
        lifecycleScope.launch {
            val userId = viewModel.getUserId()
            if (userId != null) {
                viewModel.loadTodos(userId)
            }
        }
    }
    
    private fun groupTodosByDate(todos: List<Todo>): List<TodoItem> {
        val items = mutableListOf<TodoItem>()
        val dateFormat = SimpleDateFormat("MM월 dd일 (E)", Locale.KOREAN)
        
        val grouped = todos.groupBy { 
            dateFormat.format(it.scheduledDate)
        }
        
        grouped.forEach { (date, todosForDate) ->
            // 날짜 헤더 추가
            val completedCount = todosForDate.count { it.completed }
            val totalCount = todosForDate.size
            items.add(TodoItem.DateHeader(date, completedCount, totalCount))
            
            // Todo 아이템 추가
            todosForDate.forEach { todo ->
                items.add(TodoItem.TodoData(todo))
            }
        }
        
        return items
    }
    
    private fun completeTodo(todo: Todo) {
        lifecycleScope.launch {
            // 실제 소요 시간 입력 다이얼로그 표시
            showTimeInputDialog(todo) { actualTime ->
                viewModel.completeTodo(todo.todoId, actualTime)
            }
        }
    }
    
    private fun showTodoDetail(todo: Todo) {
        // Todo 상세 보기 다이얼로그
        TodoDetailDialog.newInstance(todo).show(
            childFragmentManager,
            "TodoDetailDialog"
        )
    }
    
    private fun showTimeInputDialog(todo: Todo, onTimeEntered: (Int) -> Unit) {
        val dialog = TimeInputDialog.newInstance(
            todo.title,
            todo.estimatedTime
        )
        dialog.onTimeEntered = onTimeEntered
        dialog.show(childFragmentManager, "TimeInputDialog")
    }
    
    private fun updateProgress() {
        // 학습 진행률 업데이트 (ViewModel에서 처리)
    }
    
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
        return dateFormat.format(Date())
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion fun newInstance() = PlanFragment()
}

// TodoAdapter.kt
class TodoAdapter(
    private val onTodoClick: (Todo) -> Unit,
    private val onTodoComplete: (Todo) -> Unit
) : androidx.recyclerview.widget.ListAdapter<TodoItem, androidx.recyclerview.widget.RecyclerView.ViewHolder>(
    TodoItemDiffCallback()
) {
    
    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_TODO = 1
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TodoItem.DateHeader -> TYPE_DATE_HEADER
            is TodoItem.TodoData -> TYPE_TODO
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        
        return when (viewType) {
            TYPE_DATE_HEADER -> {
                val binding = ItemDateHeaderBinding.inflate(inflater, parent, false)
                DateHeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemTodoBinding.inflate(inflater, parent, false)
                TodoViewHolder(binding)
            }
        }
    }
    
    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TodoItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is TodoItem.TodoData -> (holder as TodoViewHolder).bind(item.todo)
        }
    }
    
    inner class DateHeaderViewHolder(
        private val binding: ItemDateHeaderBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(header: TodoItem.DateHeader) {
            binding.tvDate.text = header.date
            binding.tvCompletion.text = "${header.completedCount}/${header.totalCount} 완료"
        }
    }
    
    inner class TodoViewHolder(
        private val binding: ItemTodoBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(todo: Todo) {
            binding.tvSubject.text = todo.subject
            binding.tvTitle.text = todo.title
            binding.tvActivityType.text = todo.activityType.name
            binding.tvEstimatedTime.text = "${todo.estimatedTime}분"
            
            // 체크박스 상태
            binding.cbCompleted.isChecked = todo.completed
            
            // 완료 상태에 따른 스타일 변경
            if (todo.completed) {
                binding.tvTitle.paintFlags = 
                    binding.tvTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                binding.root.alpha = 0.6f
            } else {
                binding.tvTitle.paintFlags = 
                    binding.tvTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.root.alpha = 1.0f
            }
            
            // 과목별 색상
            val color = getSubjectColor(todo.subject)
            binding.viewSubjectColor.setBackgroundColor(color)
            
            // 클릭 이벤트
            binding.root.setOnClickListener {
                onTodoClick(todo)
            }
            
            binding.cbCompleted.setOnClickListener {
                if (!todo.completed) {
                    onTodoComplete(todo)
                }
            }
        }
        
        private fun getSubjectColor(subject: String): Int {
            val context = binding.root.context
            return when (subject) {
                "국어" -> context.getColor(android.R.color.holo_orange_light)
                "수학" -> context.getColor(android.R.color.holo_purple)
                "영어" -> context.getColor(android.R.color.holo_red_light)
                "사회" -> context.getColor(android.R.color.holo_blue_light)
                "통합과학" -> context.getColor(android.R.color.holo_green_light)
                "역사" -> context.getColor(android.R.color.holo_orange_dark)
                else -> context.getColor(android.R.color.darker_gray)
            }
        }
    }
}

sealed class TodoItem {
    data class DateHeader(
        val date: String,
        val completedCount: Int,
        val totalCount: Int
    ) : TodoItem()
    
    data class TodoData(val todo: Todo) : TodoItem()
}

class TodoItemDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<TodoItem>() {
    override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
        return when {
            oldItem is TodoItem.DateHeader && newItem is TodoItem.DateHeader ->
                oldItem.date == newItem.date
            oldItem is TodoItem.TodoData && newItem is TodoItem.TodoData ->
                oldItem.todo.todoId == newItem.todo.todoId
            else -> false
        }
    }
    
    override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
        return oldItem == newItem
    }
}

// PlanViewModel.kt
class PlanViewModel : androidx.lifecycle.ViewModel() {
    private val _todos = androidx.lifecycle.MutableLiveData<List<Todo>>()
    val todos: androidx.lifecycle.LiveData<List<Todo>> = _todos
    
    private val _totalTodos = androidx.lifecycle.MutableLiveData<Int>()
    val totalTodos: androidx.lifecycle.LiveData<Int> = _totalTodos
    
    private val _completedTodos = androidx.lifecycle.MutableLiveData<Int>()
    val completedTodos: androidx.lifecycle.LiveData<Int> = _completedTodos
    
    private val _isLoading = androidx.lifecycle.MutableLiveData<Boolean>()
    val isLoading: androidx.lifecycle.LiveData<Boolean> = _isLoading
    
    fun loadTodos(userId: Int) {
        // Repository를 통해 데이터 로드
        // 실제 구현에서는 Repository 주입 필요
    }
    
    fun completeTodo(todoId: Int, actualTime: Int) {
        // Todo 완료 처리
    }
    
    fun getUserId(): Int? {
        // SessionManager에서 userId 가져오기
        return null
    }
}
