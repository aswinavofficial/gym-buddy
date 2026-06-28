package com.gymbuddy.domain

import kotlin.math.roundToInt

/**
 * Estimates calories burned during resistance training using the ACSM MET formula:
 *   kcal/min = MET * 3.5 * bodyWeightKg / 200
 * Values are intentionally labelled as estimates in the UI.
 */
object CalorieCalculator {

    private const val SECONDS_PER_REP = 3.5
    private const val REST_MET = 1.5

    /** Calories for a single working set. */
    fun caloriesForSet(met: Double, weightKg: Double, reps: Int): Double {
        val minutes = (reps * SECONDS_PER_REP) / 60.0
        return met * 3.5 * weightKg / 200.0 * minutes
    }

    /**
     * Calories for a whole session: active calories from the logged sets plus a light
     * allowance for the rest time across the full session duration.
     */
    fun caloriesForSession(
        activeCalories: Double,
        bodyWeightKg: Double,
        totalDurationMin: Double,
        activeMinutes: Double,
    ): Int {
        val restMinutes = (totalDurationMin - activeMinutes).coerceAtLeast(0.0)
        val restCalories = REST_MET * 3.5 * bodyWeightKg / 200.0 * restMinutes
        return (activeCalories + restCalories).roundToInt()
    }
}

/** One-rep-max estimators (Epley + Brzycki averaged), used for PRs and strength trends. */
object OneRepMax {
    fun estimate(weightKg: Double, reps: Int): Double {
        if (reps <= 0 || weightKg <= 0) return 0.0
        if (reps == 1) return weightKg
        val epley = weightKg * (1 + reps / 30.0)
        val brzycki = weightKg * 36.0 / (37.0 - reps).coerceAtLeast(1.0)
        return (epley + brzycki) / 2.0
    }
}
