package com.gymbuddy.data.repo

import com.gymbuddy.data.local.dao.ExerciseDao
import com.gymbuddy.data.local.entities.ExerciseEntity
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val dao: ExerciseDao) {

    fun search(
        query: String,
        category: String?,
        equipment: String?,
        target: String?,
        favoritesOnly: Boolean,
    ): Flow<List<ExerciseEntity>> = dao.search(query.trim(), category, equipment, target, favoritesOnly)

    fun observeById(id: String): Flow<ExerciseEntity?> = dao.observeById(id)

    suspend fun getById(id: String): ExerciseEntity? = dao.getById(id)

    fun observeByIds(ids: List<String>): Flow<List<ExerciseEntity>> = dao.observeByIds(ids)

    fun categories(): Flow<List<String>> = dao.categories()
    fun equipmentTypes(): Flow<List<String>> = dao.equipmentTypes()
    fun targets(): Flow<List<String>> = dao.targets()

    suspend fun allEquipment(): List<String> = dao.equipmentTypesNow()

    suspend fun toggleFavorite(id: String) {
        val current = dao.getById(id) ?: return
        dao.setFavorite(id, !current.isFavorite)
    }
}
