package com.cnsa.studyplanner.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cnsa.studyplanner.databinding.FragmentHomeBinding
import com.cnsa.studyplanner.ui.goal.GoalSettingActivity
import com.cnsa.studyplanner.ui.problem.ProblemListActivity
import com.cnsa.studyplanner.ui.ranking.RankingFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupObservers()
        loadData()
    }
    
    private fun setupViews() {
        // D-day 설정
        binding.tvDday.text = calculateDday()
        binding.tvCurrentDate.text = getCurrentDate()
        
        // 일일 랭킹 카드
        binding.cardDailyRanking.setOnClickListener {
            // 일일 랭킹 보기
            navigateToRanking("daily")
        }
        
        // 시즌 랭킹 카드
        binding.cardSeasonRanking.setOnClickListener {
            // 시즌 랭킹 보기
            navigateToRanking("season")
        }
        
        // 오늘의 과제 카드
        binding.cardTodayTask.setOnClickListener {
            // 과제 상세 보기
        }
        
        // View Task 버튼
        binding.btnViewTask.setOnClickListener {
            // 학습 플랜 화면으로 이동
            navigateToPlan()
        }
        
        // 문제 풀이 시작 버튼
        binding.cardProblemSolving.setOnClickListener {
            val intent = Intent(requireContext(), ProblemListActivity::class.java)
            startActivity(intent)
        }
        
        // 진행 중인 작업 RecyclerView
        setupTaskRecyclerView()
    }
    
    private fun setupTaskRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task ->
                // 작업 상세 보기
            },
            onTaskComplete = { task ->
                // 작업 완료 처리
                viewModel.completeTask(task.id)
            }
        )
        
        binding.rvInProgressTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }
    }
    
    private fun setupObservers() {
        // 사용자 정보
        viewModel.userInfo.observe(viewLifecycleOwner) { user ->
            binding.tvUsername.text = user.username
            binding.tvDiploma.text = user.diploma
        }
        
        // 일일 랭킹 정보
        viewModel.dailyRanking.observe(viewLifecycleOwner) { ranking ->
            binding.tvDailyRank.text = "${ranking.rank}위"
            binding.tvDailyPoints.text = "${ranking.points}점"
            
            // 순위 변동 표시
            if (ranking.rankChange > 0) {
                binding.tvDailyChange.text = "+${ranking.rankChange}"
                binding.tvDailyChange.setTextColor(
                    resources.getColor(android.R.color.holo_green_dark, null)
                )
            } else if (ranking.rankChange < 0) {
                binding.tvDailyChange.text = "${ranking.rankChange}"
                binding.tvDailyChange.setTextColor(
                    resources.getColor(android.R.color.holo_red_dark, null)
                )
            } else {
                binding.tvDailyChange.text = "0"
                binding.tvDailyChange.setTextColor(
                    resources.getColor(android.R.color.darker_gray, null)
                )
            }
        }
        
        // 시즌 랭킹 정보
        viewModel.seasonRanking.observe(viewLifecycleOwner) { ranking ->
            binding.tvSeasonRank.text = "${ranking.rank}위"
            binding.tvSeasonPoints.text = "${ranking.points}점"
            
            // 순위 변동 표시
            if (ranking.rankChange > 0) {
                binding.tvSeasonChange.text = "+${ranking.rankChange}"
                binding.tvSeasonChange.setTextColor(
                    resources.getColor(android.R.color.holo_green_dark, null)
                )
            } else if (ranking.rankChange < 0) {
                binding.tvSeasonChange.text = "${ranking.rankChange}"
                binding.tvSeasonChange.setTextColor(
                    resources.getColor(android.R.color.holo_red_dark, null)
                )
            } else {
                binding.tvSeasonChange.text = "0"
                binding.tvSeasonChange.setTextColor(
                    resources.getColor(android.R.color.darker_gray, null)
                )
            }
        }
        
        // 학습 진행률
        viewModel.studyProgress.observe(viewLifecycleOwner) { progress ->
            binding.tvProgressPercent.text = "${progress.percentage}%"
            binding.progressBar.progress = progress.percentage
            binding.tvProgressTitle.text = progress.title
        }
        
        // 진행 중인 작업 목록
        viewModel.inProgressTasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
            binding.tvInProgressCount.text = tasks.size.toString()
        }
        
        // 로딩 상태
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.shimmerLayout.startShimmer()
                binding.shimmerLayout.visibility = View.VISIBLE
                binding.mainContent.visibility = View.GONE
            } else {
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                binding.mainContent.visibility = View.VISIBLE
            }
        }
    }
    
    private fun loadData() {
        lifecycleScope.launch {
            viewModel.loadHomeData()
        }
    }
    
    private fun calculateDday(): String {
        val goalDate = viewModel.getGoalEndDate() ?: return "D-day"
        
        val today = Calendar.getInstance()
        val goal = Calendar.getInstance().apply {
            time = goalDate
        }
        
        val diffInMillis = goal.timeInMillis - today.timeInMillis
        val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        
        return when {
            diffInDays > 0 -> "D-${diffInDays}"
            diffInDays == 0 -> "D-Day"
            else -> "D+${-diffInDays}"
        }
    }
    
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("MM월 dd일 (E)", Locale.KOREAN)
        return dateFormat.format(Date())
    }
    
    private fun navigateToRanking(type: String) {
        val fragment = RankingFragment.newInstance(type)
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun navigateToPlan() {
        // 학습 플랜 Fragment로 이동 (MainActivity에서 처리)
        (activity as? MainActivity)?.navigateToPlan()
    }
    
    override fun onResume() {
        super.onResume()
        // 화면 복귀 시 데이터 새로고침
        loadData()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance() = HomeFragment()
    }
}

// TaskAdapter.kt
class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onTaskComplete: (Task) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(task: Task) {
            binding.tvSubject.text = task.subject
            binding.tvTitle.text = task.title
            binding.tvProgress.text = "${task.progress}%"
            binding.progressBar.progress = task.progress
            
            // 과목별 색상 설정
            val color = when (task.subject) {
                "국어" -> android.R.color.holo_orange_light
                "수학" -> android.R.color.holo_purple
                "영어" -> android.R.color.holo_red_light
                "사회" -> android.R.color.holo_blue_light
                "과학" -> android.R.color.holo_green_light
                "역사" -> android.R.color.holo_orange_dark
                else -> android.R.color.darker_gray
            }
            
            binding.ivSubjectIcon.setBackgroundColor(
                binding.root.context.getColor(color)
            )
            
            binding.root.setOnClickListener {
                onTaskClick(task)
            }
            
            binding.btnComplete.setOnClickListener {
                onTaskComplete(task)
            }
        }
    }
    
    class TaskDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }
}

// Task 데이터 클래스
data class Task(
    val id: Int,
    val subject: String,
    val title: String,
    val progress: Int
)
