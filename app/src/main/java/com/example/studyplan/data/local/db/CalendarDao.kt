package com.yourorg.studyplanner.data.local.db

import androidx.room.*

@Dao
interface CalendarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendar(calendar: CalendarEntity)

    @Query("SELECT * FROM calendar WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    suspend fun getCalendarRange(userId: String, startDate: Long, endDate: Long): List<CalendarEntity>

    @Update
    suspend fun updateCalendar(calendar: CalendarEntity)
}