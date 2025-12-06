package com.cnsa.studyplanner.util

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

// ============= SessionManager =============
class SessionManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREF_NAME = "StudyPlannerSession"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    fun createSession(userId: Int, sessionId: String) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, userId)
            putString(KEY_SESSION_ID, sessionId)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    fun getUserId(): Int? {
        val userId = prefs.getInt(KEY_USER_ID, -1)
        return if (userId != -1) userId else null
    }
    
    fun getSessionId(): String? {
        return prefs.getString(KEY_SESSION_ID, null)
    }
    
    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun setUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }
    
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}

// ============= PasswordUtil =============
object PasswordUtil {
    
    /**
     * 비밀번호 해시 생성 (SHA-256 + Salt)
     */
    fun hashPassword(password: String, salt: String): String {
        val bytes = (password + salt).toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Salt 생성
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(32)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 비밀번호 검증
     */
    fun verifyPassword(password: String, salt: String, hashedPassword: String): Boolean {
        val hash = hashPassword(password, salt)
        return hash == hashedPassword
    }
    
    /**
     * 비밀번호 강도 체크
     */
    fun checkPasswordStrength(password: String): PasswordStrength {
        if (password.length < 6) return PasswordStrength.WEAK
        
        val hasDigit = password.any { it.isDigit() }
        val hasLetter = password.any { it.isLetter() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        
        return when {
            password.length >= 10 && hasDigit && hasLetter && hasSpecial -> PasswordStrength.STRONG
            password.length >= 8 && hasDigit && hasLetter -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
    
    enum class PasswordStrength {
        WEAK, MEDIUM, STRONG
    }
}

// ============= AIModelLoader =============
class AIModelLoader(private val context: Context) {
    
    private val interpreters = mutableMapOf<AIModelType, org.tensorflow.lite.Interpreter>()
    
    enum class AIModelType(val filename: String) {
        VAE("vae_model.tflite"),
        TRANSFORMER("transformer_model.tflite"),
        GNN("gnn_model.tflite"),
        DQN("dqn_model.tflite")
    }
    
    /**
     * TFLite 모델 로드
     */
    fun loadModel(modelType: AIModelType): org.tensorflow.lite.Interpreter? {
        if (interpreters.containsKey(modelType)) {
            return interpreters[modelType]
        }
        
        try {
            val assetManager = context.assets
            val modelFile = assetManager.open("models/${modelType.filename}")
            val modelBytes = modelFile.readBytes()
            
            val buffer = java.nio.ByteBuffer.allocateDirect(modelBytes.size)
            buffer.order(java.nio.ByteOrder.nativeOrder())
            buffer.put(modelBytes)
            
            val interpreter = org.tensorflow.lite.Interpreter(buffer)
            interpreters[modelType] = interpreter
            
            return interpreter
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * VAE로 학습 계획 생성
     */
    fun generatePlanWithVAE(inputFeatures: FloatArray): FloatArray? {
        val interpreter = loadModel(AIModelType.VAE) ?: return null
        
        try {
            val input = Array(1) { inputFeatures }
            val output = Array(1) { FloatArray(128) }
            
            interpreter.run(input, output)
            return output[0]
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Transformer로 학습 시간 예측
     */
    fun predictWithTransformer(sequence: Array<FloatArray>): FloatArray? {
        val interpreter = loadModel(AIModelType.TRANSFORMER) ?: return null
        
        try {
            val input = Array(1) { sequence }
            val output = Array(1) { FloatArray(6) }
            
            interpreter.run(input, output)
            return output[0]
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * GNN으로 과목별 가중치 계산
     */
    fun calculateWeightsWithGNN(subjectFeatures: Array<FloatArray>): FloatArray? {
        val interpreter = loadModel(AIModelType.GNN) ?: return null
        
        try {
            val input = Array(1) { subjectFeatures }
            val output = Array(1) { FloatArray(6) }
            
            interpreter.run(input, output)
            return output[0]
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * DQN으로 최적 전략 선택
     */
    fun selectStrategyWithDQN(state: FloatArray): Int? {
        val interpreter = loadModel(AIModelType.DQN) ?: return null
        
        try {
            val input = Array(1) { state }
            val output = Array(1) { FloatArray(10) }
            
            interpreter.run(input, output)
            
            // Q-value가 최대인 행동 선택
            val qValues = output[0]
            return qValues.indices.maxByOrNull { qValues[it] }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 모든 인터프리터 해제
     */
    fun close() {
        interpreters.values.forEach { it.close() }
        interpreters.clear()
    }
}

// ============= DateUtil =============
object DateUtil {
    
    /**
     * Date를 yyyy-MM-dd 형식 문자열로 변환
     */
    fun formatDate(date: Date): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(date)
    }
    
    /**
     * yyyy-MM-dd 문자열을 Date로 변환
     */
    fun parseDate(dateString: String): Date? {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            format.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 두 날짜 사이의 일수 계산
     */
    fun daysBetween(start: Date, end: Date): Int {
        val diff = end.time - start.time
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }
    
    /**
     * D-day 계산
     */
    fun calculateDday(targetDate: Date): String {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val days = daysBetween(today, targetDate)
        
        return when {
            days > 0 -> "D-${days}"
            days == 0 -> "D-Day"
            else -> "D+${-days}"
        }
    }
    
    /**
     * 날짜 포맷팅 (한글)
     */
    fun formatKoreanDate(date: Date): String {
        val format = java.text.SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREAN)
        return format.format(date)
    }
}

// ============= SubjectColorUtil =============
object SubjectColorUtil {
    
    fun getSubjectColor(context: Context, subject: String): Int {
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
    
    fun getSubjectIcon(subject: String): Int {
        return when (subject) {
            "국어" -> R.drawable.ic_korean
            "수학" -> R.drawable.ic_math
            "영어" -> R.drawable.ic_english
            "사회" -> R.drawable.ic_social
            "통합과학" -> R.drawable.ic_science
            "역사" -> R.drawable.ic_history
            else -> R.drawable.ic_subject_default
        }
    }
}

// ============= DiplomaUtil =============
object DiplomaUtil {
    
    /**
     * 디플로마별 과목 보너스 계산
     */
    fun calculateDiplomaBonus(diploma: String, subject: String, basePoints: Float): Float {
        val scienceSubjects = listOf("수학", "통합과학", "물리", "화학", "생명과학")
        val artsSubjects = listOf("국어", "영어", "역사", "사회")
        
        return when {
            // 이과 디플로마
            diploma in listOf("IT", "공학", "수학", "물리", "화학", "생명과학", "IB(자연)") -> {
                when (subject) {
                    in scienceSubjects -> basePoints  // 이과 과목 보너스 없음
                    in artsSubjects -> basePoints * 0.9f  // 문과 과목 -10%
                    else -> basePoints
                }
            }
            // 문과 디플로마
            diploma in listOf("인문학", "국제어문", "사회과학", "경제경영", "IB(인문)") -> {
                when (subject) {
                    in artsSubjects -> basePoints  // 문과 과목 보너스 없음
                    in scienceSubjects -> basePoints * 1.1f  // 이과 과목 +10%
                    else -> basePoints
                }
            }
            // 예체 디플로마
            diploma in listOf("예술", "체육") -> {
                basePoints * 1.1f  // 모든 과목 +10%
            }
            else -> basePoints
        }
    }
    
    /**
     * 디플로마별 필수 과목 가져오기
     */
    fun getRequiredSubjects(diploma: String): List<String> {
        // 1학년은 모두 동일한 과목
        return listOf("국어", "수학", "영어", "역사", "통합과학", "사회")
    }
}

// ============= ValidationUtil =============
object ValidationUtil {
    
    fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }
    
    fun isValidStudentNumber(studentNumber: String): Boolean {
        return studentNumber.matches("\\d{8}".toRegex())
    }
    
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
}
