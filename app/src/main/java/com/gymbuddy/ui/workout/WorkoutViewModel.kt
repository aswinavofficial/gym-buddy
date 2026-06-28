package com.gymbuddy.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbuddy.data.local.entities.ExerciseEntity
import com.gymbuddy.data.local.entities.SetLogEntity
import com.gymbuddy.data.repo.FinishResult
import com.gymbuddy.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseGroup(val exercise: ExerciseEntity?, val exerciseId: String, val sets: List<SetLogEntity>)

data class WorkoutUiState(
    val title: String = "Workout",
    val groups: List<ExerciseGroup> = emptyList(),
    val completedSets: Int = 0,
    val totalSets: Int = 0,
    val volumeKg: Double = 0.0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val sessionId: Long,
    private val container: AppContainer,
) : ViewModel() {
    private val repo = container.workoutRepository

    val ui: StateFlow<WorkoutUiState> =
        repo.observeSession(sessionId).flatMapLatest { sws ->
            val ids = sws?.sets?.map { it.exerciseId }?.distinct() ?: emptyList()
            container.exerciseRepository.observeByIds(ids).map { exercises ->
                val byId = exercises.associateBy { it.id }
                val groups = sws?.sets
                    ?.groupBy { it.exerciseId }
                    ?.map { (exId, sets) -> ExerciseGroup(byId[exId], exId, sets.sortedBy { it.setIndex }) }
                    ?: emptyList()
                val all = sws?.sets ?: emptyList()
                WorkoutUiState(
                    title = sws?.session?.title ?: "Workout",
                    groups = groups,
                    completedSets = all.count { it.done },
                    totalSets = all.size,
                    volumeKg = all.filter { it.done }.sumOf { (it.weightKg ?: 0.0) * it.reps },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutUiState())

    private val startTime = System.currentTimeMillis()
    private val _elapsed = MutableStateFlow(0L)
    val elapsed: StateFlow<Long> = _elapsed.asStateFlow()

    private val _rest = MutableStateFlow<Int?>(null)
    val rest: StateFlow<Int?> = _rest.asStateFlow()
    private var restJob: Job? = null

    private val _finish = MutableStateFlow<FinishResult?>(null)
    val finishResult: StateFlow<FinishResult?> = _finish.asStateFlow()

    var pendingAdd = false
        private set

    init {
        viewModelScope.launch {
            while (_finish.value == null) {
                _elapsed.value = System.currentTimeMillis() - startTime
                delay(1000)
            }
        }
    }

    fun toggleDone(set: SetLogEntity, defaultRest: Int = 90) = viewModelScope.launch {
        val nowDone = !set.done
        repo.updateSet(set.copy(done = nowDone))
        if (nowDone) startRest(defaultRest)
    }

    fun updateSet(set: SetLogEntity, reps: Int, weight: Double?) = viewModelScope.launch {
        repo.updateSet(set.copy(reps = reps, weightKg = weight))
    }

    fun addSet(group: ExerciseGroup) = viewModelScope.launch {
        val last = group.sets.lastOrNull()
        repo.addSet(sessionId, group.exerciseId, group.sets.size, last?.reps ?: 10, last?.weightKg)
    }

    fun deleteSet(set: SetLogEntity) = viewModelScope.launch { repo.deleteSet(set.id) }

    fun beginAddExercise() { pendingAdd = true }
    fun completePick(exerciseId: String) {
        if (!pendingAdd) return
        pendingAdd = false
        viewModelScope.launch { repo.addSet(sessionId, exerciseId, 0, 10, null) }
    }

    fun startRest(seconds: Int) {
        restJob?.cancel()
        _rest.value = seconds
        restJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _rest.value = remaining
            }
            _rest.value = null
        }
    }

    fun addRest(delta: Int) { _rest.value?.let { startRest((it + delta).coerceAtLeast(1)) } }
    fun skipRest() { restJob?.cancel(); _rest.value = null }

    fun finish() = viewModelScope.launch {
        val result = repo.finishSession(sessionId)
        _finish.value = result
        // Mirror to Health Connect if available & permitted.
        runCatching {
            val hc = container.healthConnectManager
            if (hc.isAvailable && hc.hasPermissions()) {
                hc.writeWorkout(startTime, System.currentTimeMillis(), ui.value.title, result.calories)
            }
        }
    }

    fun cancel() = viewModelScope.launch { repo.cancelSession(sessionId) }
}
