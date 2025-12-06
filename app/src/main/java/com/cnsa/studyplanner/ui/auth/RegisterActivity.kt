package com.cnsa.studyplanner.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cnsa.studyplanner.databinding.ActivityRegisterBinding
import com.cnsa.studyplanner.data.repository.UserRepository
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userRepository: UserRepository
    
    private val diplomas = listOf(
        "IT", "공학", "수학", "물리", "화학", "생명과학", "IB(자연)",
        "인문학", "국제어문", "사회과학", "경제경영", "IB(인문)",
        "예술", "체육"
    )
    
    private val grades = listOf("1학년", "2학년", "3학년")
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        userRepository = UserRepository(this)
        
        setupViews()
    }
    
    private fun setupViews() {
        // 학번 Spinner 설정
        val gradeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, grades)
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGrade.adapter = gradeAdapter
        
        // 디플로마 Spinner 설정
        val diplomaAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, diplomas)
        diplomaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDiploma.adapter = diplomaAdapter
        
        // 회원가입 버튼
        binding.btnRegister.setOnClickListener {
            validateAndRegister()
        }
        
        // 뒤로가기
        binding.ivBack.setOnClickListener {
            finish()
        }
        
        // 비밀번호 표시/숨김
        binding.ivPasswordToggle.setOnClickListener {
            togglePasswordVisibility()
        }
    }
    
    private fun validateAndRegister() {
        val username = binding.etUsername.text.toString().trim()
        val studentNumber = binding.etStudentNumber.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val grade = grades.indexOf(binding.spinnerGrade.selectedItem.toString()) + 1
        val diploma = binding.spinnerDiploma.selectedItem.toString()
        
        // 유효성 검사
        if (username.isEmpty()) {
            binding.etUsername.error = "아이디를 입력해주세요"
            return
        }
        
        if (username.length < 4) {
            binding.etUsername.error = "아이디는 4자 이상이어야 합니다"
            return
        }
        
        if (studentNumber.isEmpty()) {
            binding.etStudentNumber.error = "학번을 입력해주세요"
            return
        }
        
        // 학번 형식 검증 (예: 20240101)
        if (!studentNumber.matches(Regex("\\d{8}"))) {
            binding.etStudentNumber.error = "올바른 학번 형식이 아닙니다 (8자리 숫자)"
            return
        }
        
        if (password.isEmpty()) {
            binding.etPassword.error = "비밀번호를 입력해주세요"
            return
        }
        
        if (password.length < 6) {
            binding.etPassword.error = "비밀번호는 6자 이상이어야 합니다"
            return
        }
        
        // 회원가입 진행
        performRegister(username, studentNumber, password, grade, diploma)
    }
    
    private fun performRegister(
        username: String,
        studentNumber: String,
        password: String,
        grade: Int,
        diploma: String
    ) {
        binding.btnRegister.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val result = userRepository.register(
                    username = username,
                    studentNumber = studentNumber,
                    password = password,
                    grade = grade,
                    diploma = diploma,
                    email = "${studentNumber}@cnsa.hs.kr"
                )
                
                if (result.success) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "회원가입이 완료되었습니다!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 로그인 화면으로 이동
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this@RegisterActivity,
                        result.message ?: "회원가입 실패",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    "오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.btnRegister.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun togglePasswordVisibility() {
        val isVisible = binding.etPassword.inputType == 
            (android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
        
        if (isVisible) {
            binding.etPassword.inputType = 
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.ivPasswordToggle.setImageResource(android.R.drawable.ic_menu_view)
        } else {
            binding.etPassword.inputType = 
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.ivPasswordToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        }
        
        binding.etPassword.setSelection(binding.etPassword.text.length)
    }
}
