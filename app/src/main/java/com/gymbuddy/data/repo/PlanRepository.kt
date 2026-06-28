package com.gymbuddy.data.repo

import com.gymbuddy.data.local.PlanWithDays
import com.gymbuddy.data.local.dao.ExerciseDao
import com.gymbuddy.data.local.dao.PlanDao
import com.gymbuddy.data.local.entities.PlanDayEntity
import com.gymbuddy.data.local.entities.PlanItemEntity
import com.gymbuddy.data.local.entities.UserProfileEntity
import com.gymbuddy.data.local.entities.WorkoutPlanEntity
import com.gymbuddy.domain.PlanGenerator
import kotlinx.coroutines.flow.Flow

class PlanRepository(
    private val planDao: PlanDao,
    private val exerciseDao: ExerciseDao,
) {
    fun observePlans(): Flow<List<WorkoutPlanEntity>> = planDao.observePlans()
    fun observePlan(planId: Long): Flow<PlanWithDays?> = planDao.observePlanWithDays(planId)
    fun observeActivePlan(): Flow<PlanWithDays?> = planDao.observeActivePlan()

    suspend fun getDay(dayId: Long): PlanDayEntity? = planDao.getDay(dayId)
    suspend fun getPlanWithDays(planId: Long): PlanWithDays? = planDao.getPlanWithDays(planId)

    suspend fun createBlankPlan(name: String): Long {
        val id = planDao.insertPlan(WorkoutPlanEntity(name = name))
        planDao.insertDay(PlanDayEntity(planId = id, dayIndex = 0, label = "Day 1"))
        return id
    }

    suspend fun setActive(planId: Long) = planDao.setActivePlan(planId)

    suspend fun deletePlan(plan: WorkoutPlanEntity) = planDao.deletePlan(plan)

    suspend fun renamePlan(plan: WorkoutPlanEntity, name: String) =
        planDao.updatePlan(plan.copy(name = name))

    suspend fun addDay(planId: Long, dayIndex: Int, label: String): Long =
        planDao.insertDay(PlanDayEntity(planId = planId, dayIndex = dayIndex, label = label))

    suspend fun deleteDay(day: PlanDayEntity) = planDao.deleteDay(day)
    suspend fun renameDay(day: PlanDayEntity, label: String) = planDao.updateDay(day.copy(label = label))

    suspend fun addItem(dayId: Long, exerciseId: String, order: Int, sets: Int, reps: Int, rest: Int): Long =
        planDao.insertItem(
            PlanItemEntity(
                dayId = dayId,
                exerciseId = exerciseId,
                orderIndex = order,
                targetSets = sets,
                targetReps = reps,
                restSec = rest,
            ),
        )

    suspend fun updateItem(item: PlanItemEntity) = planDao.updateItem(item)
    suspend fun deleteItem(item: PlanItemEntity) = planDao.deleteItem(item)

    /** One-tap exercise swap within a plan item. */
    suspend fun swapItemExercise(item: PlanItemEntity, newExerciseId: String) =
        planDao.updateItem(item.copy(exerciseId = newExerciseId))

    /** Generates and persists a tailored plan from the profile; returns the new plan id. */
    suspend fun generateForProfile(profile: UserProfileEntity, makeActive: Boolean = true): Long {
        val equipment = profile.availableEquipment
        val pool = exerciseDao.forEquipment(equipment, if (equipment.isEmpty()) 1 else 0)
            .ifEmpty { exerciseDao.forEquipment(emptyList(), 1) }
        val generated = PlanGenerator.generate(profile, pool)

        val planId = planDao.insertPlan(
            WorkoutPlanEntity(name = generated.name, goal = generated.goal, isActive = false),
        )
        generated.days.forEachIndexed { dayIndex, day ->
            val dayId = planDao.insertDay(
                PlanDayEntity(planId = planId, dayIndex = dayIndex, label = day.label),
            )
            planDao.insertItems(
                day.items.mapIndexed { i, it ->
                    PlanItemEntity(
                        dayId = dayId,
                        exerciseId = it.exerciseId,
                        orderIndex = i,
                        targetSets = it.targetSets,
                        targetReps = it.targetReps,
                        restSec = it.restSec,
                    )
                },
            )
        }
        if (makeActive) planDao.setActivePlan(planId)
        return planId
    }
}
