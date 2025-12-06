package com.cnsa.studyplanner.data.repository

import android.content.Context
import com.cnsa.studyplanner.data.model.*
import com.cnsa.studyplanner.network.ApiService
import com.cnsa.studyplanner.network.RetrofitClient
import com.cnsa.studyplanner.util.SessionManager
import com.cnsa.studyplanner.util.PasswordUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val context: Context) {
    
    private val apiService: ApiService = RetrofitClient.getApiService()
    private val sessionManager = SessionManager(context)
    
    suspend fun login(email: String, password: String): AuthResponse {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    response.body() ?: AuthResponse(false, "응답이 없습니다")
                } else {
                    AuthResponse(false, "로그인 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                AuthResponse(false, "네트워크 오류: ${e.message}")
            }
        }
    }
    
    suspend fun register(
        username: String,
        studentNumber: String,
        password: String,
        grade: Int,
        diploma: String,
        email: String
    ): AuthResponse {
        return withContext(Dispatchers.IO) {
            try {
                val request = RegisterRequest(
                    username = username,
                    password = password,
                    studentNumber = studentNumber,
                    diploma = diploma,
                    grade = grade,
                    email = email
                )
                
                val response = apiService.register(request)
                if (response.isSuccessful) {
                    response.body() ?: AuthResponse(false, "응답이 없습니다")
                } else {
                    AuthResponse(false, "회원가입 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                AuthResponse(false, "네트워크 오류: ${e.message}")
            }
        }
    }
    
    suspend fun loginWithGoogle(googleId: String, email: String, displayName: String): AuthResponse {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf(
                    "google_id" to googleId,
                    "email" to email,
                    "display_name" to displayName
                )
                
                val response = apiService.loginWithGoogle(request)
                if (response.isSuccessful) {
                    response.body() ?: AuthResponse(false, "응답이 없습니다")
                } else {
                    AuthResponse(false, "Google 로그인 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                AuthResponse(false, "네트워크 오류: ${e.message}")
            }
        }
    }
    
    suspend fun getUserInfo(userId: Int): User? {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId() ?: return@withContext null
                val response = apiService.getUserInfo(userId, sessionId)
                if (response.isSuccessful) {
                    response.body()?.data
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun logout() {
        withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId()
                if (sessionId != null) {
                    apiService.logout(sessionId)
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                sessionManager.clearSession()
            }
        }
    }
}

class ProblemRepository(private val context: Context) {
    
    private val apiService: ApiService = RetrofitClient.getApiService()
    private val sessionManager = SessionManager(context)
    
    suspend fun getProblems(seasonId: Int, subject: String? = null): List<Problem> {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId() ?: return@withContext emptyList()
                val response = apiService.getProblems(sessionId, seasonId, subject)
                if (response.isSuccessful) {
                    response.body()?.data ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun getProblem(problemId: Int): Problem? {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId() ?: return@withContext null
                val response = apiService.getProblem(problemId, sessionId)
                if (response.isSuccessful) {
                    response.body()?.data
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun submitProblem(problemId: Int, selectedAnswer: Int, timeSpent: Int?): ProblemResult {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId()
                    ?: return@withContext ProblemResult(false, false, 0f, 0)
                val userId = sessionManager.getUserId()
                    ?: return@withContext ProblemResult(false, false, 0f, 0)
                
                val submission = ProblemSubmission(userId, problemId, selectedAnswer, timeSpent)
                val response = apiService.submitProblem(sessionId, submission)
                
                if (response.isSuccessful) {
                    response.body() ?: ProblemResult(false, false, 0f, 0)
                } else {
                    ProblemResult(false, false, 0f, 0, "제출 실패")
                }
            } catch (e: Exception) {
                ProblemResult(false, false, 0f, 0, "네트워크 오류: ${e.message}")
            }
        }
    }
}

class PlanRepository(private val context: Context) {
    
    private val apiService: ApiService = RetrofitClient.getApiService()
    private val sessionManager = SessionManager(context)
    
    suspend fun getUserTodos(userId: Int, date: String? = null): List<Todo> {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId() ?: return@withContext emptyList()
                val response = apiService.getUserTodos(userId, sessionId, date)
                if (response.isSuccessful) {
                    response.body()?.data ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun completeTodo(todoId: Int, actualTime: Int): Todo? {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId() ?: return@withContext null
                val response = apiService.completeTodo(
                    todoId,
                    sessionId,
                    mapOf("actual_time" to actualTime)
                )
                if (response.isSuccessful) {
                    response.body()?.data
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun generateStudyPlan(goals: Map<String, Int>, startDate: String, endDate: String): StudyPlanRecommendation? {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId() ?: return@withContext null
                val userId = sessionManager.getUserId() ?: return@withContext null
                
                val request = mapOf(
                    "user_id" to userId,
                    "goals" to goals,
                    "start_date" to startDate,
                    "end_date" to endDate
                )
                
                val response = apiService.generateStudyPlan(sessionId, request)
                if (response.isSuccessful) {
                    response.body()?.data
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

class RankingRepository(private val context: Context) {
    
    private val apiService: ApiService = RetrofitClient.getApiService()
    private val sessionManager = SessionManager(context)
    
    suspend fun getDailyRanking(seasonId: Int, date: String? = null): List<DailyRanking> {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId() ?: return@withContext emptyList()
                val response = apiService.getDailyRanking(sessionId, seasonId, date)
                if (response.isSuccessful) {
                    response.body()?.rankings?.map {
                        DailyRanking(
                            userId = it.userId,
                            username = it.username,
                            diploma = it.diploma,
                            seasonId = it.seasonId,
                            rankingDate = it.lastUpdated,
                            dailyPoints = it.dailyPoints,
                            rankPosition = it.rankPosition,
                            rankChange = it.rankChange
                        )
                    } ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun getSeasonRanking(seasonId: Int, limit: Int = 100): List<SeasonRanking> {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId() ?: return@withContext emptyList()
                val response = apiService.getSeasonRanking(sessionId, seasonId, limit)
                if (response.isSuccessful) {
                    response.body()?.rankings ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun getUserRanking(userId: Int, seasonId: Int): SeasonRanking? {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId() ?: return@withContext null
                val response = apiService.getUserRanking(userId, sessionId, seasonId)
                if (response.isSuccessful) {
                    response.body()?.data
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

class GoalRepository(private val context: Context) {
    
    private val apiService: ApiService = RetrofitClient.getApiService()
    private val sessionManager = SessionManager(context)
    
    suspend fun createGoals(seasonId: Int, goals: Map<String, Int>): List<Goal> {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId() ?: return@withContext emptyList()
                val userId = sessionManager.getUserId() ?: return@withContext emptyList()
                
                val request = GoalRequest(userId, seasonId, goals)
                val response = apiService.createGoals(sessionId, request)
                
                if (response.isSuccessful) {
                    response.body()?.data ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun getUserGoals(userId: Int, seasonId: Int?): List<Goal> {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId() ?: return@withContext emptyList()
                val response = apiService.getUserGoals(userId, sessionId, seasonId)
                if (response.isSuccessful) {
                    response.body()?.data ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

class PvpRepository(private val context: Context) {
    
    private val apiService: ApiService = RetrofitClient.getApiService()
    private val sessionManager = SessionManager(context)
    
    suspend fun requestPvpMatch(seasonId: Int): PvpMatchResponse {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId()
                    ?: return@withContext PvpMatchResponse(false, message = "세션 없음")
                val userId = sessionManager.getUserId()
                    ?: return@withContext PvpMatchResponse(false, message = "사용자 정보 없음")
                
                val request = PvpRequest(userId, seasonId)
                val response = apiService.requestPvpMatch(sessionId, request)
                
                if (response.isSuccessful) {
                    response.body() ?: PvpMatchResponse(false, message = "응답 없음")
                } else {
                    PvpMatchResponse(false, message = "매칭 실패")
                }
            } catch (e: Exception) {
                PvpMatchResponse(false, message = "네트워크 오류: ${e.message}")
            }
        }
    }
    
    suspend fun submitPvpAnswer(matchId: Int, selectedAnswer: Int, timeSpent: Int): PvpResult {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId()
                    ?: return@withContext PvpResult(false, "error", false, false, 0)
                val userId = sessionManager.getUserId()
                    ?: return@withContext PvpResult(false, "error", false, false, 0)
                
                val submission = PvpSubmission(matchId, userId, selectedAnswer, timeSpent)
                val response = apiService.submitPvpAnswer(sessionId, submission)
                
                if (response.isSuccessful) {
                    response.body() ?: PvpResult(false, "error", false, false, 0)
                } else {
                    PvpResult(false, "error", false, false, 0)
                }
            } catch (e: Exception) {
                PvpResult(false, "error", false, false, 0)
            }
        }
    }
}

class SeasonRepository(private val context: Context) {
    
    private val apiService: ApiService = RetrofitClient.getApiService()
    private val sessionManager = SessionManager(context)
    
    suspend fun getActiveSeason(): Season? {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId() ?: return@withContext null
                val response = apiService.getActiveSeason(sessionId)
                if (response.isSuccessful) {
                    response.body()?.season
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun getAllSeasons(): List<Season> {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = sessionManager.getSessionId() ?: return@withContext emptyList()
                val response = apiService.getAllSeasons(sessionId)
                if (response.isSuccessful) {
                    response.body()?.seasons ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
