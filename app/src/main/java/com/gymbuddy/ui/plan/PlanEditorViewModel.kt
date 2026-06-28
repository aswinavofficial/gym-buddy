package com.gymbuddy.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbuddy.data.local.PlanWithDays
import com.gymbuddy.data.local.entities.ExerciseEntity
import com.gymbuddy.data.local.entities.PlanItemEntity
import com.gymbuddy.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PendingPick {
    data class AddToDay(val dayId: Long) : PendingPick
    data class Swap(val item: PlanItemEntity) : PendingPick
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlanEditorViewModel(
    private val planId: Long,
    private val container: AppContainer,
) : ViewModel() {
    private val planRepo = container.planRepository

    val plan: StateFlow<PlanWithDays?> =
        planRepo.observePlan(planId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val exerciseMap: StateFlow<Map<String, ExerciseEntity>> =
        planRepo.observePlan(planId)
            .flatMapLatest { p ->
                val ids = p?.days?.flatMap { d -> d.items.map { it.exerciseId } }?.distinct() ?: emptyList()
                container.exerciseRepository.observeByIds(ids).map { list -> list.associateBy { it.id } }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    var pending: PendingPick? = null
        private set

    fun beginAdd(dayId: Long) { pending = PendingPick.AddToDay(dayId) }
    fun beginSwap(item: PlanItemEntity) { pending = PendingPick.Swap(item) }

    fun completePick(exerciseId: String) {
        val p = pending ?: return
        pending = null
        viewModelScope.launch {
            when (p) {
                is PendingPick.AddToDay -> {
                    val goal = plan.value?.plan?.goal
                    val order = plan.value?.days?.firstOrNull { it.day.id == p.dayId }?.items?.size ?: 0
                    planRepo.addItem(
                        p.dayId, exerciseId, order,
                        goal?.defaultSets ?: 3, goal?.repHigh ?: 10, goal?.restSec ?: 60,
                    )
                }
                is PendingPick.Swap -> planRepo.swapItemExercise(p.item, exerciseId)
            }
        }
    }

    fun updateItem(item: PlanItemEntity) = viewModelScope.launch { planRepo.updateItem(item) }
    fun deleteItem(item: PlanItemEntity) = viewModelScope.launch { planRepo.deleteItem(item) }

    fun addDay() = viewModelScope.launch {
        val index = plan.value?.days?.size ?: 0
        planRepo.addDay(planId, index, "Day ${index + 1}")
    }
    fun renameDay(dayId: Long, label: String) = viewModelScope.launch {
        planRepo.getDay(dayId)?.let { planRepo.renameDay(it, label) }
    }
    fun deleteDay(dayId: Long) = viewModelScope.launch {
        planRepo.getDay(dayId)?.let { planRepo.deleteDay(it) }
    }
    fun startDay(dayId: Long, onReady: (Long) -> Unit) = viewModelScope.launch {
        onReady(container.workoutRepository.startFromDay(dayId))
    }

    fun setActive() = viewModelScope.launch { planRepo.setActive(planId) }
    fun rename(name: String) = viewModelScope.launch {
        plan.value?.plan?.let { planRepo.renamePlan(it, name) }
    }
}
