package com.gymbuddy.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "exercises",
    indices = [Index("category"), Index("equipment"), Index("target"), Index("name")],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val bodyPart: String,
    val equipment: String,
    val target: String,
    val muscleGroup: String,
    val secondaryMuscles: List<String>,
    val instructionsEn: String?,
    val instructionsIt: String?,
    val instructionsTr: String?,
    /** JSON-encoded Map<langCode, List<step>>. */
    val stepsJson: String,
    val image: String,
    val gifUrl: String,
    val createdAt: String,
    val isFavorite: Boolean = false,
) {
    fun instructionFor(lang: String): String? = when (lang) {
        "it" -> instructionsIt
        "tr" -> instructionsTr
        else -> instructionsEn
    } ?: instructionsEn

    fun stepsFor(lang: String): List<String> {
        val all = runCatching {
            Json.decodeFromString<Map<String, List<String>>>(stepsJson)
        }.getOrDefault(emptyMap())
        return all[lang] ?: all["en"] ?: emptyList()
    }
}
