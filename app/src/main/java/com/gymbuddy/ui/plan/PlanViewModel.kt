package com.gymbuddy.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbuddy.data.local.entities.WorkoutPlanEntity
import com.gymbuddy.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanViewModel(private val container: AppContainer) : ViewModel() {
    private val planRepo = container.planRepository
    private val profileRepo = container.profileRepository

    val plans: StateFlow<List<WorkoutPlanEntity>> =
        planRepo.observePlans().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var generating = androidx.compose.runtime.mutableStateOf(false)
        private set

    fun generateForMe(onCreated: (Long) -> Unit) = viewModelScope.launch {
        generating.value = true
        val profile = profileRepo.get()
        val id = planRepo.generateForProfile(profile, makeActive = true)
        generating.value = false
        onCreated(id)
    }

    fun createBlank(onCreated: (Long) -> Unit) = viewModelScope.launch {
        onCreated(planRepo.createBlankPlan("My plan"))
    }

    fun setActive(planId: Long) = viewModelScope.launch { planRepo.setActive(planId) }
    fun delete(plan: WorkoutPlanEntity) = viewModelScope.launch { planRepo.deletePlan(plan) }
}
