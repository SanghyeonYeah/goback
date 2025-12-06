package com.cnsa.studyplanner.ui.problem

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cnsa.studyplanner.databinding.ActivityProblemSolveBinding
import com.cnsa.studyplanner.data.model.Problem
import com.cnsa.studyplanner.data.repository.ProblemRepository
import com.cnsa.studyplanner.util.SessionManager
import kotlinx.coroutines.launch

class ProblemSolveActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProblemSolveBinding
    private lateinit var problemRepository: ProblemRepository
    private lateinit var sessionManager: SessionManager
    
    private var currentProblem: Problem? = null
    private var startTime: Long = 0
    private var selectedAnswer: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProblemSolveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        problemRepository = ProblemRepository(this)
        sessionManager = SessionManager(this)
        
        val problemId = intent.getIntExtra("problem_id", -1)
        if (problemId != -1) {
            loadProblem(problemId)
        } else {
            Toast.makeText(this, "문제를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        setupViews()
    }
    
    private fun setupViews() {
        // 뒤로가기
        binding.ivBack.setOnClickListener {
            finish()
        }
        
        // 답안 선택
        binding.rgAnswers.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbOption1.id -> selectedAnswer = 1
                binding.rbOption2.id -> selectedAnswer = 2
                binding.rbOption3.id -> selectedAnswer = 3
                binding.rbOption4.id -> selectedAnswer = 4
                binding.rbOption5.id -> selectedAnswer = 5
            }
        }
        
        // 제출 버튼
        binding.btnSubmit.setOnClickListener {
            submitAnswer()
        }
    }
    
    private fun loadProblem(problemId: Int) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val problem = problemRepository.getProblem(problemId)
                
                if (problem != null) {
                    currentProblem = problem
                    displayProblem(problem)
                    startTime = System.currentTimeMillis()
                } else {
                    Toast.makeText(
                        this@ProblemSolveActivity,
                        "문제를 불러올 수 없습니다",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProblemSolveActivity,
                    "오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun displayProblem(problem: Problem) {
        // 과목 표시
        binding.tvSubject.text = problem.subject
        
        // 문제 텍스트
        binding.tvQuestion.text = problem.questionText
        
        // 문제 이미지 (있는 경우)
        if (!problem.questionImageUrl.isNullOrEmpty()) {
            binding.ivQuestionImage.visibility = android.view.View.VISIBLE
            Glide.with(this)
                .load(problem.questionImageUrl)
                .into(binding.ivQuestionImage)
        } else {
            binding.ivQuestionImage.visibility = android.view.View.GONE
        }
        
        // 점수 표시
        binding.tvPoints.text = "${problem.basePoints}점"
        
        // 난이도 표시
        binding.tvDifficulty.text = when (problem.difficulty) {
            com.cnsa.studyplanner.data.model.Difficulty.EASY -> "쉬움"
            com.cnsa.studyplanner.data.model.Difficulty.MEDIUM -> "보통"
            com.cnsa.studyplanner.data.model.Difficulty.HARD -> "어려움"
        }
        
        // 답안 옵션
        binding.rbOption1.text = "1. ${problem.option1}"
        binding.rbOption2.text = "2. ${problem.option2}"
        
        if (!problem.option3.isNullOrEmpty()) {
            binding.rbOption3.visibility = android.view.View.VISIBLE
            binding.rbOption3.text = "3. ${problem.option3}"
        } else {
            binding.rbOption3.visibility = android.view.View.GONE
        }
        
        if (!problem.option4.isNullOrEmpty()) {
            binding.rbOption4.visibility = android.view.View.VISIBLE
            binding.rbOption4.text = "4. ${problem.option4}"
        } else {
            binding.rbOption4.visibility = android.view.View.GONE
        }
        
        if (!problem.option5.isNullOrEmpty()) {
            binding.rbOption5.visibility = android.view.View.VISIBLE
            binding.rbOption5.text = "5. ${problem.option5}"
        } else {
            binding.rbOption5.visibility = android.view.View.GONE
        }
        
        // 정답률 업데이트
        updateStatistics()
    }
    
    private fun updateStatistics() {
        currentProblem?.let { problem ->
            // 서버에서 통계 가져오기 (구현 필요)
            binding.tvAccuracy.text = "정답률: --%"
            binding.tvCurrentNumber.text = "0"
            binding.tvTotalNumber.text = "0"
        }
    }
    
    private fun submitAnswer() {
        if (selectedAnswer == 0) {
            Toast.makeText(this, "답을 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        val problem = currentProblem ?: return
        val timeSpent = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        
        binding.btnSubmit.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val result = problemRepository.submitProblem(
                    problem.problemId,
                    selectedAnswer,
                    timeSpent
                )
                
                // 결과 표시
                showResult(result.isCorrect, result.pointsEarned, result.correctAnswer)
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProblemSolveActivity,
                    "제출 중 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.btnSubmit.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun showResult(isCorrect: Boolean, points: Float, correctAnswer: Int) {
        // 결과 화면으로 전환
        binding.layoutQuestion.visibility = android.view.View.GONE
        binding.layoutResult.visibility = android.view.View.VISIBLE
        
        if (isCorrect) {
            binding.tvResultTitle.text = "정답입니다!"
            binding.tvResultTitle.setTextColor(getColor(android.R.color.holo_green_dark))
            binding.ivResultIcon.setImageResource(R.drawable.ic_check_circle)
            binding.tvEarnedPoints.text = "+${points}점 획득!"
        } else {
            binding.tvResultTitle.text = "오답입니다"
            binding.tvResultTitle.setTextColor(getColor(android.R.color.holo_red_dark))
            binding.ivResultIcon.setImageResource(R.drawable.ic_cancel)
            binding.tvCorrectAnswer.text = "정답: ${correctAnswer}번"
            binding.tvCorrectAnswer.visibility = android.view.View.VISIBLE
        }
        
        // 다음 문제 버튼
        binding.btnNextProblem.setOnClickListener {
            finish()
            // 다음 문제로 이동 (구현 필요)
        }
        
        // 종료 버튼
        binding.btnFinish.setOnClickListener {
            finish()
        }
    }
}

// ProblemListActivity.kt
class ProblemListActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProblemListBinding
    private lateinit var problemRepository: ProblemRepository
    private lateinit var seasonRepository: com.cnsa.studyplanner.data.repository.SeasonRepository
    
    private lateinit var problemAdapter: ProblemAdapter
    private var currentSubject: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProblemListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        problemRepository = ProblemRepository(this)
        seasonRepository = com.cnsa.studyplanner.data.repository.SeasonRepository(this)
        
        currentSubject = intent.getStringExtra("subject")
        
        setupViews()
        loadProblems()
    }
    
    private fun setupViews() {
        // 뒤로가기
        binding.ivBack.setOnClickListener {
            finish()
        }
        
        // 제목
        binding.tvTitle.text = currentSubject?.let { "${it} 문제 풀이" } ?: "문제 풀이"
        
        // RecyclerView 설정
        problemAdapter = ProblemAdapter { problem ->
            val intent = Intent(this, ProblemSolveActivity::class.java)
            intent.putExtra("problem_id", problem.problemId)
            startActivity(intent)
        }
        
        binding.rvProblems.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ProblemListActivity)
            adapter = problemAdapter
        }
    }
    
    private fun loadProblems() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val season = seasonRepository.getActiveSeason()
                if (season != null) {
                    val problems = problemRepository.getProblems(season.seasonId, currentSubject)
                    problemAdapter.submitList(problems)
                    
                    if (problems.isEmpty()) {
                        binding.tvEmptyMessage.visibility = android.view.View.VISIBLE
                    } else {
                        binding.tvEmptyMessage.visibility = android.view.View.GONE
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProblemListActivity,
                    "문제 목록을 불러올 수 없습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
}

// ProblemAdapter
class ProblemAdapter(
    private val onProblemClick: (Problem) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Problem, ProblemAdapter.ViewHolder>(
    ProblemDiffCallback()
) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProblemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemProblemBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(problem: Problem) {
            binding.tvSubject.text = problem.subject
            binding.tvPoints.text = "${problem.basePoints}점"
            
            val difficultyText = when (problem.difficulty) {
                com.cnsa.studyplanner.data.model.Difficulty.EASY -> "쉬움"
                com.cnsa.studyplanner.data.model.Difficulty.MEDIUM -> "보통"
                com.cnsa.studyplanner.data.model.Difficulty.HARD -> "어려움"
            }
            binding.tvDifficulty.text = difficultyText
            
            // 문제 미리보기
            binding.tvPreview.text = problem.questionText.take(50) + "..."
            
            binding.root.setOnClickListener {
                onProblemClick(problem)
            }
        }
    }
}

class ProblemDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Problem>() {
    override fun areItemsTheSame(oldItem: Problem, newItem: Problem) =
        oldItem.problemId == newItem.problemId
    
    override fun areContentsTheSame(oldItem: Problem, newItem: Problem) =
        oldItem == newItem
}
