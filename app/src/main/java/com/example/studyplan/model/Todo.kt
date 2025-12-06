package com.example.studyplanner.model

data class Todo(
    val id: String,
    val userId: String,
    val goalId: String,
    val date: String, // yyyy-MM-dd
    val subject: String, // 국어, 수학, 사회, 통합과학, 영어, 역사, 물리, 화학, 생명과학
    val content: String,
    val isCompleted: Boolean = false,
    val dueTime: String? = null, // HH:mm
    val priority: Int = 1, // 1: 낮음, 2: 중간, 3: 높음
    val duration: Int = 30, // 분 단위
    val createdAt: Long,
    val updatedAt: Long
)

data class TodoRequest(
    val goalId: String,
    val date: String,
    val subject: String,
    val content: String,
    val dueTime: String? = null,
    val priority: Int = 1,
    val duration: Int = 30
)

data class TodoResponse(
    val status: Int,
    val message: String,
    val data: Todo?
)

data class TodoListResponse(
    val status: Int,
    val message: String,
    val data: List<Todo>?
)

data class TodoStatusUpdate(
    val id: String,
    val isCompleted: Boolean
)

data class DailyTodoSummary(
    val date: String,
    val totalCount: Int,
    val completedCount: Int,
    val todos: List<Todo>
)