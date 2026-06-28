package com.gymbuddy.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gymbuddy.data.model.Experience
import com.gymbuddy.data.model.Goal
import com.gymbuddy.data.model.Sex
import com.gymbuddy.data.model.Units

@Entity(tableName = "profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val sex: Sex = Sex.OTHER,
    val birthYear: Int = 1995,
    val heightCm: Double = 175.0,
    val weightKg: Double = 75.0,
    val goal: Goal = Goal.GENERAL,
    val experience: Experience = Experience.BEGINNER,
    val daysPerWeek: Int = 3,
    val availableEquipment: List<String> = emptyList(),
    val units: Units = Units.METRIC,
)

@Entity(tableName = "plans")
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val goal: Goal = Goal.GENERAL,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false,
)

@Entity(
    tableName = "plan_days",
    indices = [Index("planId")],
)
data class PlanDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val dayIndex: Int,
    val label: String,
)

@Entity(
    tableName = "plan_items",
    indices = [Index("dayId"), Index("exerciseId")],
)
data class PlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val exerciseId: String,
    val orderIndex: Int,
    val targetSets: Int,
    val targetReps: Int,
    val restSec: Int,
    val supersetGroup: Int? = null,
)

@Entity(
    tableName = "sessions",
    indices = [Index("planDayId")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planDayId: Long? = null,
    val title: String = "Workout",
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val totalCalories: Int = 0,
    val notes: String = "",
)

@Entity(
    tableName = "set_logs",
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val setIndex: Int,
    val reps: Int,
    val weightKg: Double?,
    val rpe: Int? = null,
    val done: Boolean = false,
    val loggedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "personal_records")
data class PersonalRecordEntity(
    @PrimaryKey val exerciseId: String,
    val bestEst1Rm: Double = 0.0,
    val bestWeight: Double = 0.0,
    val bestReps: Int = 0,
    val bestVolume: Double = 0.0,
    val achievedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val weightKg: Double,
    val bodyFatPct: Double? = null,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val armCm: Double? = null,
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val type: String,
    val unlockedAt: Long = System.currentTimeMillis(),
)
