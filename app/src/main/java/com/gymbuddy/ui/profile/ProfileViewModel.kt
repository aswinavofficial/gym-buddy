package com.gymbuddy.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbuddy.data.local.entities.UserProfileEntity
import com.gymbuddy.data.model.Language
import com.gymbuddy.data.model.Units
import com.gymbuddy.data.repo.AppSettings
import com.gymbuddy.data.repo.SyncState
import com.gymbuddy.di.AppContainer
import com.gymbuddy.media.DownloadProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val container: AppContainer) : ViewModel() {
    private val settingsRepo = container.settingsRepository
    private val syncRepo = container.syncRepository

    val profile: StateFlow<UserProfileEntity?> =
        container.profileRepository.observe()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<AppSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val syncState: StateFlow<SyncState> =
        syncRepo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncState.Idle)

    val downloadState: StateFlow<DownloadProgress> =
        container.mediaDownloadController.state
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DownloadProgress.Idle)

    fun saveProfile(profile: UserProfileEntity) = viewModelScope.launch {
        container.profileRepository.save(profile)
    }

    fun setLanguage(lang: Language) = viewModelScope.launch { settingsRepo.setLanguage(lang) }
    fun setUnits(units: Units) = viewModelScope.launch {
        val p = container.profileRepository.get()
        container.profileRepository.save(p.copy(units = units))
    }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepo.setDynamicColor(enabled) }
    fun setDarkMode(value: Boolean?) = viewModelScope.launch { settingsRepo.setDarkMode(value) }
    fun setWeeklyGoal(goal: Int) = viewModelScope.launch { settingsRepo.setWeeklyGoal(goal) }
    fun setReminders(enabled: Boolean, hour: Int) = viewModelScope.launch { settingsRepo.setReminders(enabled, hour) }
    fun setHealthConnect(enabled: Boolean) = viewModelScope.launch { settingsRepo.setHealthConnect(enabled) }

    fun checkForUpdate() = viewModelScope.launch {
        syncRepo.checkForUpdate(settings.value.dataSha)
    }
    fun sync() = viewModelScope.launch { syncRepo.sync() }

    fun startMediaDownload() = container.mediaDownloadController.start()
    fun cancelMediaDownload() = container.mediaDownloadController.cancel()
}
