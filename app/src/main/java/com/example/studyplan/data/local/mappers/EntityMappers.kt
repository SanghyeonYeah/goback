package com.yourorg.studyplanner.data.local.mappers

import com.google.gson.Gson
import com.yourorg.studyplanner.data.local.db.entity.*
import com.yourorg.studyplanner.domain.model.*

private val gson = Gson()

// User
fun UserEntity.toDomain(): User {
    val map: Map<String, Int> =
        gson.fromJson(targetGradesJson, Map::class.java)
            .mapKeys { it.key as String }
            .mapValues { (it.value as Double).toInt() }

    return User(
        id = id,
        name = name,
        grade = grade,
        diploma = diploma,
        targetGrades = map,
        dailyStudyTime = dailyStudyTime,
        createdAt = createdAt
    )
}

fun User.toEntity(): UserEntity =
    UserEntity(
        id = id,
        name = name,
        grade = grade,
        diploma = diploma,
        targetGradesJson = gson.toJson(targetGrades),
        dailyStudyTime = dailyStudyTime,
        createdAt = createdAt
    )

// Problem
fun ProblemEntity.toDomain(): Problem = Problem(
    id = id,
    subject = subject,
    source = source,
    content = content,
    options = gson.fromJson(optionsJson, Array<String>::class.java).toList(),
    correctAnswer = correctAnswer,
    points = points,
    season = season,
    createdAt = createdAt
)

fun Problem.toEntity(): ProblemEntity =
    ProblemEntity(
        id = id,
        subject = subject,
        source = source,
        content = content,
        optionsJson = gson.toJson(options),
        correctAnswer = correctAnswer,
        points = points,
        season = season,
        createdAt = createdAt
    )

// Submission / Ranking / Objective / Calendar 등도 같은 패턴으로 추가
