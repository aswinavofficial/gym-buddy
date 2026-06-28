package com.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gymbuddy.data.local.PlanWithDays
import com.gymbuddy.data.local.entities.PlanDayEntity
import com.gymbuddy.data.local.entities.PlanItemEntity
import com.gymbuddy.data.local.entities.WorkoutPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query("SELECT * FROM plans ORDER BY isActive DESC, createdAt DESC")
    fun observePlans(): Flow<List<WorkoutPlanEntity>>

    @Transaction
    @Query("SELECT * FROM plans WHERE id = :planId")
    fun observePlanWithDays(planId: Long): Flow<PlanWithDays?>

    @Transaction
    @Query("SELECT * FROM plans WHERE isActive = 1 LIMIT 1")
    fun observeActivePlan(): Flow<PlanWithDays?>

    @Transaction
    @Query("SELECT * FROM plans WHERE id = :planId")
    suspend fun getPlanWithDays(planId: Long): PlanWithDays?

    @Query("SELECT * FROM plan_days WHERE id = :dayId")
    suspend fun getDay(dayId: Long): PlanDayEntity?

    @Query("SELECT * FROM plan_items WHERE dayId = :dayId ORDER BY orderIndex")
    suspend fun itemsForDay(dayId: Long): List<PlanItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: WorkoutPlanEntity)

    @Delete
    suspend fun deletePlan(plan: WorkoutPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: PlanDayEntity): Long

    @Update
    suspend fun updateDay(day: PlanDayEntity)

    @Delete
    suspend fun deleteDay(day: PlanDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PlanItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PlanItemEntity>)

    @Update
    suspend fun updateItem(item: PlanItemEntity)

    @Delete
    suspend fun deleteItem(item: PlanItemEntity)

    @Query("UPDATE plans SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE plans SET isActive = 1 WHERE id = :planId")
    suspend fun markActive(planId: Long)

    @Transaction
    suspend fun setActivePlan(planId: Long) {
        clearActive()
        markActive(planId)
    }
}
