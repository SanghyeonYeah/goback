package com.cnsa.studyplanner.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cnsa.studyplanner.MainActivity
import com.cnsa.studyplanner.databinding.ActivityLoginBinding
import com.cnsa.studyplanner.data.repository.UserRepository
import com.cnsa.studyplanner.util.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var userRepository: UserRepository
    private lateinit var sessionManager: SessionManager
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        userRepository = UserRepository(this)
        sessionManager = SessionManager(this)
        
        // 이미 로그인되어 있는지 확인
        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }
        
        setupGoogleSignIn()
        setupViews()
    }
    
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken("YOUR_CLIENT_ID") // Firebase Console에서 가져온 Client ID
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }
    
    private fun setupViews() {
        // 일반 로그인 버튼
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            
            if (validateInput(email, password)) {
                performLogin(email, password)
            }
        }
        
        // Google 로그인 버튼
        binding.btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }
        
        // 회원가입 버튼
        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        
        // 비밀번호 찾기
        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "비밀번호 찾기 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
        
        // 비밀번호 표시/숨김
        binding.ivPasswordToggle.setOnClickListener {
            togglePasswordVisibility()
        }
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "이메일을 입력해주세요"
            return false
        }
        
        if (!email.endsWith("@cnsa.hs.kr")) {
            binding.etEmail.error = "학교 이메일만 사용 가능합니다 (@cnsa.hs.kr)"
            return false
        }
        
        if (password.isEmpty()) {
            binding.etPassword.error = "비밀번호를 입력해주세요"
            return false
        }
        
        if (password.length < 6) {
            binding.etPassword.error = "비밀번호는 6자 이상이어야 합니다"
            return false
        }
        
        return true
    }
    
    private fun performLogin(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val result = userRepository.login(email, password)
                
                if (result.success) {
                    // 세션 저장
                    result.sessionId?.let { sessionId ->
                        result.userId?.let { userId ->
                            sessionManager.createSession(userId, sessionId)
                        }
                    }
                    
                    Toast.makeText(
                        this@LoginActivity,
                        "로그인 성공!",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    navigateToMain()
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        result.message ?: "로그인 실패",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.btnLogin.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }
    
    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            
            // 학교 도메인 확인
            val email = account?.email ?: ""
            if (!email.endsWith("@cnsa.hs.kr")) {
                Toast.makeText(
                    this,
                    "학교 이메일만 사용 가능합니다 (@cnsa.hs.kr)",
                    Toast.LENGTH_LONG
                ).show()
                googleSignInClient.signOut()
                return
            }
            
            // 서버에 Google 로그인 요청
            lifecycleScope.launch {
                try {
                    val result = userRepository.loginWithGoogle(
                        googleId = account.id ?: "",
                        email = email,
                        displayName = account.displayName ?: ""
                    )
                    
                    if (result.success) {
                        result.sessionId?.let { sessionId ->
                            result.userId?.let { userId ->
                                sessionManager.createSession(userId, sessionId)
                            }
                        }
                        
                        Toast.makeText(
                            this@LoginActivity,
                            "Google 로그인 성공!",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        navigateToMain()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            result.message ?: "Google 로그인 실패",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@LoginActivity,
                        "오류가 발생했습니다: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(
                this,
                "Google 로그인 실패: ${e.statusCode}",
                Toast.LENGTH_LONG
            ).show()
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
        
        // 커서를 끝으로 이동
        binding.etPassword.setSelection(binding.etPassword.text.length)
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
