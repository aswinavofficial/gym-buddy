package com.gymbuddy.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbuddy.data.local.entities.AchievementEntity
import com.gymbuddy.data.local.entities.BodyMetricEntity
import com.gymbuddy.data.local.entities.PersonalRecordEntity
import com.gymbuddy.data.model.Units
import com.gymbuddy.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class WeeklyVolume(val weekLabel: String, val volume: Float)

data class ProgressUiState(
    val units: Units = Units.METRIC,
    val records: List<PersonalRecordEntity> = emptyList(),
    val recordNames: Map<String, String> = emptyMap(),
    val bodyMetrics: List<BodyMetricEntity> = emptyList(),
    val weeklyVolume: List<WeeklyVolume> = emptyList(),
    val achievements: List<AchievementEntity> = emptyList(),
    val totalVolume: Double = 0.0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(private val container: AppContainer) : ViewModel() {
    private val workoutRepo = container.workoutRepository

    private val recordNames: StateFlow<Map<String, String>> =
        workoutRepo.observeRecords()
            .flatMapLatest { records ->
                container.exerciseRepository.observeByIds(records.map { it.exerciseId })
                    .map { list -> list.associate { it.id to it.name } }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val state: StateFlow<ProgressUiState> = combine(
        workoutRepo.observeRecords(),
        workoutRepo.observeBodyMetrics(),
        workoutRepo.observeHistory(),
        workoutRepo.observeAchievements(),
        container.profileRepository.observe(),
    ) { records, metrics, history, achievements, profile ->
        val volumes = history.associate { sws ->
            sws.session.id to sws.sets.filter { it.done }.sumOf { (it.weightKg ?: 0.0) * it.reps }
        }
        val now = System.currentTimeMillis()
        val weekly = (7 downTo 0).map { w ->
            val start = now - TimeUnit.DAYS.toMillis((w + 1) * 7L)
            val end = now - TimeUnit.DAYS.toMillis(w * 7L)
            val vol = history.filter { it.session.startedAt in start until end }
                .sumOf { volumes[it.session.id] ?: 0.0 }
            WeeklyVolume(if (w == 0) "now" else "-${w}w", vol.toFloat())
        }
        ProgressUiState(
            units = profile?.units ?: Units.METRIC,
            records = records.sortedByDescending { it.bestEst1Rm },
            bodyMetrics = metrics,
            weeklyVolume = weekly,
            achievements = achievements,
            totalVolume = volumes.values.sum(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressUiState())

    val namesState: StateFlow<Map<String, String>> get() = recordNames

    fun logBodyWeight(weightKg: Double) = viewModelScope.launch {
        workoutRepo.addBodyMetric(BodyMetricEntity(weightKg = weightKg))
    }
}
