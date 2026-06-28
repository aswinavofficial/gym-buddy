package com.gymbuddy.domain

import java.util.concurrent.TimeUnit

/** Computes day-based workout streaks from completed-session timestamps. */
object StreakCalculator {

    private fun dayIndex(epochMillis: Long): Long =
        TimeUnit.MILLISECONDS.toDays(epochMillis)

    /** Current streak counting back from today (or yesterday if today is not yet trained). */
    fun currentStreak(timestamps: List<Long>, now: Long = System.currentTimeMillis()): Int {
        if (timestamps.isEmpty()) return 0
        val days = timestamps.map { dayIndex(it) }.toSortedSet().toList().asReversed()
        val today = dayIndex(now)
        // Allow the streak to be "alive" if the last workout was today or yesterday.
        var expected = when (days.first()) {
            today, today - 1 -> days.first()
            else -> return 0
        }
        var streak = 0
        for (d in days) {
            if (d == expected) {
                streak++
                expected -= 1
            } else if (d < expected) {
                break
            }
        }
        return streak
    }

    /** Count of distinct training days within the last [windowDays]. */
    fun sessionsInWindow(timestamps: List<Long>, windowDays: Int, now: Long = System.currentTimeMillis()): Int {
        val cutoff = now - TimeUnit.DAYS.toMillis(windowDays.toLong())
        return timestamps.count { it >= cutoff }
    }
}
