package com.yourorg.studyplanner.domain.model

data class Objective(
    val id: String,
    val userId: String,
    val subject: String,
    val targetGrade: Int,
    val season: Int,
    val achieved: Boolean
)