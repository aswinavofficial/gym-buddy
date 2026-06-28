package com.gymbuddy.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbuddy.data.local.entities.ExerciseEntity
import com.gymbuddy.data.local.entities.PersonalRecordEntity
import com.gymbuddy.data.local.entities.WorkoutPlanEntity
import com.gymbuddy.data.model.Language
import com.gymbuddy.data.repo.ExerciseRepository
import com.gymbuddy.data.repo.PlanRepository
import com.gymbuddy.data.repo.SettingsRepository
import com.gymbuddy.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    private val exerciseId: String,
    private val exerciseRepo: ExerciseRepository,
    private val planRepo: PlanRepository,
    private val settings: SettingsRepository,
    private val container: AppContainer,
) : ViewModel() {

    val exercise: StateFlow<ExerciseEntity?> =
        exerciseRepo.observeById(exerciseId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val language: StateFlow<Language> =
        settings.settings.map { it.language }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Language.EN)

    val record: StateFlow<PersonalRecordEntity?> =
        container.workoutRepository.observeRecords()
            .map { records -> records.firstOrNull { it.exerciseId == exerciseId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val plans: StateFlow<List<WorkoutPlanEntity>> =
        planRepo.observePlans().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLanguage(lang: Language) = viewModelScope.launch { settings.setLanguage(lang) }
    fun toggleFavorite() = viewModelScope.launch { exerciseRepo.toggleFavorite(exerciseId) }

    fun addToPlanDay(planId: Long, onDone: () -> Unit) = viewModelScope.launch {
        val plan = planRepo.getPlanWithDays(planId) ?: return@launch
        val day = plan.days.firstOrNull()?.day ?: run {
            val id = planRepo.addDay(planId, 0, "Day 1")
            planRepo.getDay(id)
        } ?: return@launch
        val order = plan.days.firstOrNull()?.items?.size ?: 0
        planRepo.addItem(day.id, exerciseId, order, plan.plan.goal.defaultSets, plan.plan.goal.repHigh, plan.plan.goal.restSec)
        onDone()
    }
}
