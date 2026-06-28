package com.gymbuddy.domain

import com.gymbuddy.data.local.entities.PlanItemEntity
import com.gymbuddy.data.local.entities.SetLogEntity
import com.gymbuddy.data.model.Goal

/** Adaptive progressive-overload suggestions based on recent performance. */
object ProgressionEngine {

    data class Suggestion(
        val weightKg: Double?,
        val reps: Int,
        val rationale: String,
    )

    /**
     * Suggests the next-session target for an exercise.
     * - If the last session hit all target sets at/above target reps → add load (or a rep).
     * - If it fell short repeatedly → deload ~10%.
     * - Otherwise → repeat the same target.
     */
    fun suggest(
        item: PlanItemEntity,
        recentSets: List<SetLogEntity>,
        goal: Goal,
    ): Suggestion {
        if (recentSets.isEmpty()) {
            return Suggestion(null, item.targetReps, "Start logging to get suggestions")
        }
        // Sets from the most recent session for this exercise.
        val lastSessionSets = recentSets
            .sortedByDescending { it.loggedAt }
            .take(item.targetSets)

        val lastWeight = lastSessionSets.mapNotNull { it.weightKg }.maxOrNull()
        val hitAll = lastSessionSets.size >= item.targetSets &&
            lastSessionSets.all { it.reps >= item.targetReps }
        val missedBadly = lastSessionSets.all { it.reps < item.targetReps - 2 }

        val increment = when {
            goal == Goal.STRENGTH -> 2.5
            goal == Goal.BUILD_MUSCLE -> 2.5
            else -> 1.25
        }

        return when {
            lastWeight == null -> Suggestion(
                null,
                if (hitAll) item.targetReps + 1 else item.targetReps,
                if (hitAll) "Great form — add a rep next time" else "Match last session",
            )
            hitAll -> Suggestion(
                lastWeight + increment,
                item.targetReps,
                "All sets complete — add ${fmt(increment)} kg",
            )
            missedBadly -> Suggestion(
                (lastWeight * 0.9),
                item.targetReps,
                "Deload to rebuild momentum",
            )
            else -> Suggestion(lastWeight, item.targetReps, "Repeat to consolidate")
        }
    }

    private fun fmt(v: Double): String =
        if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
}
