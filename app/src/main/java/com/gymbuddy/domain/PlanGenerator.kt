package com.gymbuddy.domain

import com.gymbuddy.data.local.entities.ExerciseEntity
import com.gymbuddy.data.local.entities.UserProfileEntity
import com.gymbuddy.data.model.Experience
import com.gymbuddy.data.model.Goal

data class GeneratedItem(
    val exerciseId: String,
    val targetSets: Int,
    val targetReps: Int,
    val restSec: Int,
)

data class GeneratedDay(val label: String, val items: List<GeneratedItem>)

data class GeneratedPlan(val name: String, val goal: Goal, val days: List<GeneratedDay>)

/**
 * Rule-based generator that tailors a split + exercise selection to the user's profile.
 * Pure function over an exercise [pool] (already filtered to available equipment by the caller).
 */
object PlanGenerator {

    private val PUSH = listOf("chest", "shoulders", "upper arms")
    private val PULL = listOf("back", "lower arms", "upper arms")
    private val LEGS = listOf("upper legs", "lower legs")
    private val UPPER = listOf("chest", "back", "shoulders", "upper arms", "lower arms")
    private val LOWER = listOf("upper legs", "lower legs", "waist")
    private val FULL = listOf("chest", "back", "upper legs", "shoulders", "upper arms", "waist")
    private val CORE = listOf("waist")

    private fun split(days: Int): List<Pair<String, List<String>>> = when (days.coerceIn(1, 7)) {
        1 -> listOf("Full Body" to FULL)
        2 -> listOf("Upper Body" to UPPER, "Lower Body" to LOWER)
        3 -> listOf("Push" to PUSH, "Pull" to PULL, "Legs" to LEGS)
        4 -> listOf("Upper A" to UPPER, "Lower A" to LOWER, "Upper B" to UPPER, "Lower B" to LOWER)
        5 -> listOf("Push" to PUSH, "Pull" to PULL, "Legs" to LEGS, "Upper" to UPPER, "Lower" to LOWER)
        6 -> listOf(
            "Push A" to PUSH, "Pull A" to PULL, "Legs A" to LEGS,
            "Push B" to PUSH, "Pull B" to PULL, "Legs B" to LEGS,
        )
        else -> listOf(
            "Push" to PUSH, "Pull" to PULL, "Legs" to LEGS,
            "Upper" to UPPER, "Lower" to LOWER, "Full Body" to FULL, "Core & Mobility" to CORE,
        )
    }

    private fun exercisesPerDay(exp: Experience): Int = when (exp) {
        Experience.BEGINNER -> 5
        Experience.INTERMEDIATE -> 6
        Experience.ADVANCED -> 8
    }

    fun generate(profile: UserProfileEntity, pool: List<ExerciseEntity>): GeneratedPlan {
        val byCategory = pool.groupBy { it.category.lowercase() }
        val perDay = exercisesPerDay(profile.experience)
        val goal = profile.goal

        // Prefer compound movements (free weights / machines) first for better stimulus.
        fun rank(e: ExerciseEntity): Int = when {
            "barbell" in e.equipment || "leverage" in e.equipment -> 0
            "dumbbell" in e.equipment || "smith" in e.equipment -> 1
            "body weight" in e.equipment -> 2
            else -> 3
        }

        val days = split(profile.daysPerWeek).map { (label, focus) ->
            val chosen = LinkedHashSet<String>()
            val items = mutableListOf<GeneratedItem>()
            // Round-robin across the day's focus categories until we hit the target count.
            var guard = 0
            val perCategoryQueues = focus.associateWith { cat ->
                (byCategory[cat] ?: emptyList()).sortedBy { rank(it) }.toMutableList()
            }
            while (items.size < perDay && guard < perDay * focus.size + focus.size) {
                for (cat in focus) {
                    if (items.size >= perDay) break
                    val queue = perCategoryQueues[cat] ?: continue
                    val next = queue.firstOrNull { it.id !in chosen }
                    if (next != null) {
                        chosen += next.id
                        items += GeneratedItem(
                            exerciseId = next.id,
                            targetSets = goal.defaultSets,
                            targetReps = goal.repHigh,
                            restSec = goal.restSec,
                        )
                    }
                }
                guard++
            }
            GeneratedDay(label, items)
        }.filter { it.items.isNotEmpty() }

        return GeneratedPlan(
            name = "${goal.label} • ${profile.daysPerWeek}-day",
            goal = goal,
            days = days,
        )
    }
}
