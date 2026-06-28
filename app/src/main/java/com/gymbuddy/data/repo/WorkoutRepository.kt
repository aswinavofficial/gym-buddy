package com.gymbuddy.data.repo

import com.gymbuddy.data.local.SessionWithSets
import com.gymbuddy.data.local.dao.ExerciseDao
import com.gymbuddy.data.local.dao.PlanDao
import com.gymbuddy.data.local.dao.ProfileDao
import com.gymbuddy.data.local.dao.RecordsDao
import com.gymbuddy.data.local.dao.WorkoutDao
import com.gymbuddy.data.local.entities.AchievementEntity
import com.gymbuddy.data.local.entities.BodyMetricEntity
import com.gymbuddy.data.local.entities.PersonalRecordEntity
import com.gymbuddy.data.local.entities.PlanItemEntity
import com.gymbuddy.data.local.entities.SetLogEntity
import com.gymbuddy.data.local.entities.UserProfileEntity
import com.gymbuddy.data.local.entities.WorkoutSessionEntity
import com.gymbuddy.data.model.AchievementType
import com.gymbuddy.domain.CalorieCalculator
import com.gymbuddy.domain.MetTable
import com.gymbuddy.domain.OneRepMax
import com.gymbuddy.domain.ProgressionEngine
import com.gymbuddy.domain.StreakCalculator
import kotlinx.coroutines.flow.Flow

data class FinishResult(
    val sessionId: Long,
    val calories: Int,
    val volumeKg: Double,
    val newRecords: List<String>,
    val newAchievements: List<AchievementType>,
)

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val planDao: PlanDao,
    private val recordsDao: RecordsDao,
    private val profileDao: ProfileDao,
) {
    fun observeSession(id: Long): Flow<SessionWithSets?> = workoutDao.observeSessionWithSets(id)
    fun observeHistory(): Flow<List<SessionWithSets>> = workoutDao.observeHistory()
    fun observeRecent(limit: Int): Flow<List<SessionWithSets>> = workoutDao.observeRecent(limit)
    fun observeCompletedTimestamps(): Flow<List<Long>> = workoutDao.observeCompletedTimestamps()
    fun observeCompletedCount(): Flow<Int> = workoutDao.observeCompletedCount()
    fun observeBodyMetrics(): Flow<List<BodyMetricEntity>> = recordsDao.observeBodyMetrics()
    fun observeRecords() = recordsDao.observeRecords()
    fun observeAchievements() = recordsDao.observeAchievements()

    suspend fun startFreestyle(): Long =
        workoutDao.insertSession(WorkoutSessionEntity(title = "Freestyle workout"))

    /** Starts a session from a plan day, pre-seeding editable set rows from the plan's targets. */
    suspend fun startFromDay(dayId: Long): Long {
        val day = planDao.getDay(dayId)
        val items = planDao.itemsForDay(dayId)
        val sessionId = workoutDao.insertSession(
            WorkoutSessionEntity(planDayId = dayId, title = day?.label ?: "Workout"),
        )
        items.forEach { item ->
            repeat(item.targetSets) { setIndex ->
                workoutDao.insertSet(
                    SetLogEntity(
                        sessionId = sessionId,
                        exerciseId = item.exerciseId,
                        setIndex = setIndex,
                        reps = item.targetReps,
                        weightKg = null,
                        done = false,
                    ),
                )
            }
        }
        return sessionId
    }

    suspend fun addSet(sessionId: Long, exerciseId: String, setIndex: Int, reps: Int, weightKg: Double?): Long =
        workoutDao.insertSet(
            SetLogEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                setIndex = setIndex,
                reps = reps,
                weightKg = weightKg,
            ),
        )

    suspend fun updateSet(set: SetLogEntity) = workoutDao.updateSet(set)
    suspend fun deleteSet(id: Long) = workoutDao.deleteSet(id)

    suspend fun cancelSession(sessionId: Long) {
        workoutDao.getSession(sessionId)?.let {
            if (it.endedAt == null) workoutDao.updateSession(it.copy(endedAt = it.startedAt))
        }
    }

    suspend fun progressionFor(item: PlanItemEntity): ProgressionEngine.Suggestion {
        val recent = workoutDao.recentSetsFor(item.exerciseId, item.targetSets * 2)
        val profile = profileDao.get() ?: UserProfileEntity()
        return ProgressionEngine.suggest(item, recent, profile.goal)
    }

    suspend fun addBodyMetric(metric: BodyMetricEntity) {
        recordsDao.insertBodyMetric(metric)
        // keep profile weight in sync with the latest entry
        val profile = profileDao.get() ?: UserProfileEntity()
        profileDao.upsert(profile.copy(weightKg = metric.weightKg))
    }

    /** Finalises a session: computes calories, updates PRs + achievements. */
    suspend fun finishSession(sessionId: Long): FinishResult {
        val session = workoutDao.getSession(sessionId)
            ?: return FinishResult(sessionId, 0, 0.0, emptyList(), emptyList())
        val doneSets = workoutDao.setsForSession(sessionId).filter { it.done }
        val profile = profileDao.get() ?: UserProfileEntity()
        val bodyWeight = profile.weightKg

        val exercises = exerciseDao.getByIds(doneSets.map { it.exerciseId }.distinct())
            .associateBy { it.id }

        var activeCalories = 0.0
        var activeMinutes = 0.0
        var volume = 0.0
        doneSets.forEach { s ->
            val ex = exercises[s.exerciseId]
            val met = if (ex != null) MetTable.metFor(ex.category, ex.equipment, ex.target) else 4.0
            activeCalories += CalorieCalculator.caloriesForSet(met, bodyWeight, s.reps)
            activeMinutes += s.reps * 3.5 / 60.0
            volume += (s.weightKg ?: 0.0) * s.reps
        }
        val endedAt = System.currentTimeMillis()
        val durationMin = (endedAt - session.startedAt) / 60000.0
        val calories = CalorieCalculator.caloriesForSession(activeCalories, bodyWeight, durationMin, activeMinutes)

        workoutDao.updateSession(session.copy(endedAt = endedAt, totalCalories = calories))

        // Update personal records.
        val newRecords = mutableListOf<String>()
        doneSets.groupBy { it.exerciseId }.forEach { (exId, exSets) ->
            val best1rm = exSets.filter { it.weightKg != null }
                .maxOfOrNull { OneRepMax.estimate(it.weightKg!!, it.reps) } ?: 0.0
            val bestWeight = exSets.mapNotNull { it.weightKg }.maxOrNull() ?: 0.0
            val bestReps = exSets.maxOfOrNull { it.reps } ?: 0
            val bestVolume = exSets.sumOf { (it.weightKg ?: 0.0) * it.reps }
            val existing = recordsDao.getRecord(exId)
            val improved = existing == null || best1rm > existing.bestEst1Rm ||
                bestWeight > existing.bestWeight || bestVolume > existing.bestVolume
            if (improved && best1rm > 0) {
                recordsDao.upsertRecord(
                    PersonalRecordEntity(
                        exerciseId = exId,
                        bestEst1Rm = maxOf(best1rm, existing?.bestEst1Rm ?: 0.0),
                        bestWeight = maxOf(bestWeight, existing?.bestWeight ?: 0.0),
                        bestReps = maxOf(bestReps, existing?.bestReps ?: 0),
                        bestVolume = maxOf(bestVolume, existing?.bestVolume ?: 0.0),
                        achievedAt = endedAt,
                    ),
                )
                exercises[exId]?.let { newRecords += it.name }
            }
        }

        val newAchievements = evaluateAchievements(volume, newRecords.isNotEmpty())
        return FinishResult(sessionId, calories, volume, newRecords, newAchievements)
    }

    private suspend fun evaluateAchievements(sessionVolume: Double, prHit: Boolean): List<AchievementType> {
        val allStamps = workoutDao.completedTimestampsNow()
        val count = allStamps.size
        val streak = StreakCalculator.currentStreak(allStamps)
        val unlocked = recordsDao.achievementsNow().map { it.type }.toMutableSet()
        val toUnlock = mutableListOf<AchievementType>()

        fun consider(type: AchievementType, condition: Boolean) {
            if (condition && type.name !in unlocked) {
                toUnlock += type
                unlocked += type.name
            }
        }
        consider(AchievementType.FIRST_WORKOUT, count >= 1)
        consider(AchievementType.TEN_WORKOUTS, count >= 10)
        consider(AchievementType.HUNDRED_WORKOUTS, count >= 100)
        consider(AchievementType.STREAK_3, streak >= 3)
        consider(AchievementType.STREAK_7, streak >= 7)
        consider(AchievementType.STREAK_30, streak >= 30)
        consider(AchievementType.VOLUME_10K, sessionVolume >= 10_000)
        consider(AchievementType.PR_FIRST, prHit)

        toUnlock.forEach { recordsDao.unlock(AchievementEntity(type = it.name)) }
        return toUnlock
    }
}
