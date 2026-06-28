package com.gymbuddy.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbuddy.data.local.entities.ExerciseEntity
import com.gymbuddy.data.repo.ExerciseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BrowseFilters(
    val query: String = "",
    val category: String? = null,
    val equipment: String? = null,
    val target: String? = null,
    val favoritesOnly: Boolean = false,
) {
    val activeCount: Int
        get() = listOf(category, equipment, target).count { it != null } + if (favoritesOnly) 1 else 0
}

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModel(private val repo: ExerciseRepository) : ViewModel() {

    private val _filters = MutableStateFlow(BrowseFilters())
    val filters: StateFlow<BrowseFilters> = _filters.asStateFlow()

    val exercises: StateFlow<List<ExerciseEntity>> = _filters
        .flatMapLatest { f ->
            repo.search(f.query, f.category, f.equipment, f.target, f.favoritesOnly)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = repo.categories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val equipment = repo.equipmentTypes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val targets = repo.targets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _filters.value = _filters.value.copy(query = q) }
    fun setCategory(v: String?) { _filters.value = _filters.value.copy(category = v) }
    fun setEquipment(v: String?) { _filters.value = _filters.value.copy(equipment = v) }
    fun setTarget(v: String?) { _filters.value = _filters.value.copy(target = v) }
    fun toggleFavoritesOnly() { _filters.value = _filters.value.copy(favoritesOnly = !_filters.value.favoritesOnly) }
    fun clearFilters() { _filters.value = BrowseFilters(query = _filters.value.query) }

    fun toggleFavorite(id: String) {
        viewModelScope.launch { repo.toggleFavorite(id) }
    }
}
