package com.gymbuddy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbuddy.data.local.PlanWithDays
import com.gymbuddy.data.local.SessionWithSets
import com.gymbuddy.data.local.entities.UserProfileEntity
import com.gymbuddy.di.AppContainer
import com.gymbuddy.domain.StreakCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NextWorkout(val planId: Long, val dayId: Long, val label: String, val exerciseCount: Int)

data class HomeUiState(
    val name: String = "",
    val streak: Int = 0,
    val weeklyGoal: Int = 4,
    val sessionsThisWeek: Int = 0,
    val caloriesThisWeek: Int = 0,
    val totalWorkouts: Int = 0,
    val nextWorkout: NextWorkout? = null,
    val recent: List<SessionWithSets> = emptyList(),
    val hasPlan: Boolean = false,
)

class HomeViewModel(container: AppContainer) : ViewModel() {

    private val workoutRepo = container.workoutRepository
    private val profileRepo = container.profileRepository
    private val planRepo = container.planRepository
    private val settings = container.settingsRepository

    val state: StateFlow<HomeUiState> = combine(
        profileRepo.observe(),
        planRepo.observeActivePlan(),
        workoutRepo.observeHistory(),
        workoutRepo.observeCompletedTimestamps(),
        settings.settings,
    ) { profile, activePlan, history, timestamps, appSettings ->
        val now = System.currentTimeMillis()
        val weekAgo = now - 7L * 24 * 3600 * 1000
        val thisWeek = history.filter { it.session.startedAt >= weekAgo }
        HomeUiState(
            name = profile?.name?.ifBlank { "there" } ?: "there",
            streak = StreakCalculator.currentStreak(timestamps),
            weeklyGoal = appSettings.weeklyGoal,
            sessionsThisWeek = thisWeek.size,
            caloriesThisWeek = thisWeek.sumOf { it.session.totalCalories },
            totalWorkouts = timestamps.size,
            nextWorkout = activePlan?.let { nextWorkout(it, timestamps.size) },
            recent = history.take(5),
            hasPlan = activePlan != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun startFromDay(dayId: Long, onReady: (Long) -> Unit) = viewModelScope.launch {
        onReady(workoutRepo.startFromDay(dayId))
    }

    fun startFreestyle(onReady: (Long) -> Unit) = viewModelScope.launch {
        onReady(workoutRepo.startFreestyle())
    }

    private fun nextWorkout(plan: PlanWithDays, completedCount: Int): NextWorkout? {
        val days = plan.days.sortedBy { it.day.dayIndex }
        if (days.isEmpty()) return null
        val target = days[completedCount % days.size]
        return NextWorkout(
            planId = plan.plan.id,
            dayId = target.day.id,
            label = target.day.label,
            exerciseCount = target.items.size,
        )
    }
}
