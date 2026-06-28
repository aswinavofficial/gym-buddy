package com.gymbuddy.data

import com.gymbuddy.data.local.entities.ExerciseEntity
import com.gymbuddy.data.remote.ExerciseDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Shared lenient JSON parser for the dataset and remote payloads. */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

fun ExerciseDto.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
    category = category,
    bodyPart = bodyPart,
    equipment = equipment,
    target = target,
    muscleGroup = muscleGroup,
    secondaryMuscles = secondaryMuscles,
    instructionsEn = instructions["en"],
    instructionsIt = instructions["it"],
    instructionsTr = instructions["tr"],
    stepsJson = AppJson.encodeToString(instructionSteps),
    image = image,
    gifUrl = gifUrl,
    createdAt = createdAt,
)
