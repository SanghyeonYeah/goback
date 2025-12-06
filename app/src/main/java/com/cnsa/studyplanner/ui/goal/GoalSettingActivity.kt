package com.cnsa.studyplanner.ui.goal

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cnsa.studyplanner.MainActivity
import com.cnsa.studyplanner.databinding.ActivityGoalSettingBinding
import com.cnsa.studyplanner.data.repository.GoalRepository
import com.cnsa.studyplanner.data.repository.SeasonRepository
import com.cnsa.studyplanner.util.SessionManager
import com.cnsa.studyplanner.util.DateUtil
import kotlinx.coroutines.launch
import java.util.*

class GoalSettingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGoalSettingBinding
    private lateinit var goalRepository: GoalRepository
    private lateinit var seasonRepository: SeasonRepository
    private lateinit var sessionManager: SessionManager
    
    private val subjects = listOf("국어", "수학", "사회", "통합과학", "영어", "역사")
    private val goalGrades = mutableMapOf<String, Int>()
    
    private var startDate: Date? = null
    private var endDate: Date? = null
    private var seasonId: Int? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        goalRepository = GoalRepository(this)
        seasonRepository = SeasonRepository(this)
        sessionManager = SessionManager(this)
        
        setupViews()
        loadActiveSeason()
    }
    
    private fun setupViews() {
        // 과목별 목표 등급 선택
        setupGradeSelectors()
        
        // 공부 기간 선택
        binding.btnStartDate.setOnClickListener {
            showDatePicker(true)
        }
        
        binding.btnEndDate.setOnClickListener {
            showDatePicker(false)
        }
        
        // 계획 세우기 버튼
        binding.btnCreatePlan.setOnClickListener {
            createGoals()
        }
    }
    
    private fun setupGradeSelectors() {
        // 국어
        setupSubjectGradeSelector("국어", listOf(
            binding.btnKoreanGrade1,
            binding.btnKoreanGrade2,
            binding.btnKoreanGrade3,
            binding.btnKoreanGrade4,
            binding.btnKoreanGrade5
        ))
        
        // 수학
        setupSubjectGradeSelector("수학", listOf(
            binding.btnMathGrade1,
            binding.btnMathGrade2,
            binding.btnMathGrade3,
            binding.btnMathGrade4,
            binding.btnMathGrade5
        ))
        
        // 사회
        setupSubjectGradeSelector("사회", listOf(
            binding.btnSocialGrade1,
            binding.btnSocialGrade2,
            binding.btnSocialGrade3,
            binding.btnSocialGrade4,
            binding.btnSocialGrade5
        ))
        
        // 통합과학
        setupSubjectGradeSelector("통합과학", listOf(
            binding.btnScienceGrade1,
            binding.btnScienceGrade2,
            binding.btnScienceGrade3,
            binding.btnScienceGrade4,
            binding.btnScienceGrade5
        ))
        
        // 영어
        setupSubjectGradeSelector("영어", listOf(
            binding.btnEnglishGrade1,
            binding.btnEnglishGrade2,
            binding.btnEnglishGrade3,
            binding.btnEnglishGrade4,
            binding.btnEnglishGrade5
        ))
        
        // 역사
        setupSubjectGradeSelector("역사", listOf(
            binding.btnHistoryGrade1,
            binding.btnHistoryGrade2,
            binding.btnHistoryGrade3,
            binding.btnHistoryGrade4,
            binding.btnHistoryGrade5
        ))
    }
    
    private fun setupSubjectGradeSelector(
        subject: String,
        buttons: List<com.google.android.material.button.MaterialButton>
    ) {
        buttons.forEachIndexed { index, button ->
            val grade = index + 1
            button.setOnClickListener {
                goalGrades[subject] = grade
                updateButtonSelection(buttons, index)
            }
        }
    }
    
    private fun updateButtonSelection(
        buttons: List<com.google.android.material.button.MaterialButton>,
        selectedIndex: Int
    ) {
        buttons.forEachIndexed { index, button ->
            if (index == selectedIndex) {
                button.setBackgroundResource(R.drawable.bg_grade_selected)
                button.setTextColor(getColor(android.R.color.white))
            } else {
                button.setBackgroundResource(R.drawable.bg_grade_unselected)
                button.setTextColor(getColor(android.R.color.black))
            }
        }
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time
                
                if (isStartDate) {
                    startDate = selectedDate
                    binding.tvStartDate.text = DateUtil.formatDate(selectedDate)
                } else {
                    endDate = selectedDate
                    binding.tvEndDate.text = DateUtil.formatDate(selectedDate)
                }
                
                updateDuration()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePicker.show()
    }
    
    private fun updateDuration() {
        if (startDate != null && endDate != null) {
            val days = DateUtil.daysBetween(startDate!!, endDate!!)
            binding.tvDuration.text = "총 학습 기간: ${days}일"
        }
    }
    
    private fun loadActiveSeason() {
        lifecycleScope.launch {
            val season = seasonRepository.getActiveSeason()
            if (season != null) {
                seasonId = season.seasonId
                startDate = season.startDate
                endDate = season.endDate
                
                binding.tvStartDate.text = DateUtil.formatDate(season.startDate)
                binding.tvEndDate.text = DateUtil.formatDate(season.endDate)
                updateDuration()
            }
        }
    }
    
    private fun createGoals() {
        // 유효성 검증
        if (goalGrades.isEmpty()) {
            Toast.makeText(this, "최소 1개 과목의 목표 등급을 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (startDate == null || endDate == null) {
            Toast.makeText(this, "학습 기간을 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (seasonId == null) {
            Toast.makeText(this, "활성 시즌이 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 목표 생성
        binding.btnCreatePlan.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val goals = goalRepository.createGoals(seasonId!!, goalGrades)
                
                if (goals.isNotEmpty()) {
                    // 목표 요약 표시
                    showGoalSummary()
                    
                    Toast.makeText(
                        this@GoalSettingActivity,
                        "목표가 설정되었습니다!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 메인 화면으로 이동
                    navigateToMain()
                } else {
                    Toast.makeText(
                        this@GoalSettingActivity,
                        "목표 설정에 실패했습니다",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@GoalSettingActivity,
                    "오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.btnCreatePlan.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun showGoalSummary() {
        binding.layoutGoalSummary.visibility = android.view.View.VISIBLE
        
        val summaryText = buildString {
            append("목표 요약\n\n")
            goalGrades.forEach { (subject, grade) ->
                append("$subject: ${grade}등급 달성\n")
            }
            append("\n학습 기간: ${DateUtil.formatDate(startDate!!)} ~ ${DateUtil.formatDate(endDate!!)}\n")
            append("총 ${DateUtil.daysBetween(startDate!!, endDate!!)}일")
        }
        
        binding.tvGoalSummary.text = summaryText
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
