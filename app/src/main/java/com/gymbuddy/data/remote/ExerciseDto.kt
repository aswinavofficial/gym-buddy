package com.gymbuddy.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors a record in the upstream `data/exercises.json`. */
@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val category: String = "",
    @SerialName("body_part") val bodyPart: String = "",
    val equipment: String = "",
    val instructions: Map<String, String> = emptyMap(),
    @SerialName("instruction_steps") val instructionSteps: Map<String, List<String>> = emptyMap(),
    @SerialName("muscle_group") val muscleGroup: String = "",
    @SerialName("secondary_muscles") val secondaryMuscles: List<String> = emptyList(),
    val target: String = "",
    val image: String = "",
    @SerialName("gif_url") val gifUrl: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

/** Subset of the GitHub commits API response used for update detection. */
@Serializable
data class CommitDto(
    val sha: String,
    val commit: CommitMeta = CommitMeta(),
)

@Serializable
data class CommitMeta(
    val committer: CommitActor = CommitActor(),
)

@Serializable
data class CommitActor(
    val date: String = "",
)
