package com.gymbuddy.data.repo

import com.gymbuddy.data.local.dao.ExerciseDao
import com.gymbuddy.data.remote.DatasetRemoteSource
import com.gymbuddy.data.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SyncState {
    data object Idle : SyncState
    data object Checking : SyncState
    data class UpdateAvailable(val sha: String, val date: String) : SyncState
    data object UpToDate : SyncState
    data object Downloading : SyncState
    data class Done(val count: Int) : SyncState
    data class Error(val message: String) : SyncState
}

class SyncRepository(
    private val remote: DatasetRemoteSource,
    private val exerciseDao: ExerciseDao,
    private val settings: SettingsRepository,
) {
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: Flow<SyncState> = _state.asStateFlow()

    suspend fun checkForUpdate(currentSha: String): SyncState {
        _state.value = SyncState.Checking
        val remoteVersion = remote.latestSha()
        val result = when {
            remoteVersion == null -> SyncState.Error("Couldn't reach GitHub")
            remoteVersion.sha == currentSha -> SyncState.UpToDate
            else -> SyncState.UpdateAvailable(remoteVersion.sha, remoteVersion.date)
        }
        _state.value = result
        return result
    }

    /** Downloads the latest dataset and upserts it, preserving favorites. */
    suspend fun sync(): SyncState {
        _state.value = SyncState.Downloading
        return runCatching {
            val remoteVersion = remote.latestSha()
            val dtos = remote.fetchExercises()
            val favorites = exerciseDao.favoriteIds()
            exerciseDao.insertAll(dtos.map { it.toEntity() })
            if (favorites.isNotEmpty()) exerciseDao.restoreFavorites(favorites)
            val sha = remoteVersion?.sha ?: ""
            if (sha.isNotEmpty()) settings.setVersion(sha, System.currentTimeMillis())
            SyncState.Done(dtos.size)
        }.getOrElse { SyncState.Error(it.message ?: "Sync failed") }.also { _state.value = it }
    }

    fun reset() {
        _state.value = SyncState.Idle
    }
}
