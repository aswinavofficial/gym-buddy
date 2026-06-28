package com.gymbuddy.data.seed

import android.content.Context
import com.gymbuddy.data.AppJson
import com.gymbuddy.data.local.dao.ExerciseDao
import com.gymbuddy.data.remote.ExerciseDto
import com.gymbuddy.data.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

/** Seeds the local database from the bundled `assets/exercises.json` on first launch. */
class AssetSeeder(
    private val context: Context,
    private val exerciseDao: ExerciseDao,
) {
    suspend fun seedIfEmpty(): Boolean = withContext(Dispatchers.IO) {
        if (exerciseDao.count() > 0) return@withContext false
        val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        val dtos = AppJson.decodeFromString(ListSerializer(ExerciseDto.serializer()), json)
        exerciseDao.insertAll(dtos.map { it.toEntity() })
        true
    }
}
