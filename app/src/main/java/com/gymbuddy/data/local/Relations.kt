package com.gymbuddy.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.gymbuddy.data.local.entities.PlanDayEntity
import com.gymbuddy.data.local.entities.PlanItemEntity
import com.gymbuddy.data.local.entities.SetLogEntity
import com.gymbuddy.data.local.entities.WorkoutPlanEntity
import com.gymbuddy.data.local.entities.WorkoutSessionEntity

data class PlanDayWithItems(
    @Embedded val day: PlanDayEntity,
    @Relation(parentColumn = "id", entityColumn = "dayId")
    val items: List<PlanItemEntity>,
)

data class PlanWithDays(
    @Embedded val plan: WorkoutPlanEntity,
    @Relation(entity = PlanDayEntity::class, parentColumn = "id", entityColumn = "planId")
    val days: List<PlanDayWithItems>,
)

data class SessionWithSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val sets: List<SetLogEntity>,
)
