package com.gymbuddy.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.ZoneOffset

/** Thin, defensive wrapper around Health Connect; every call is safe to invoke when unavailable. */
class HealthConnectManager(private val context: Context) {

    val isAvailable: Boolean
        get() = runCatching {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        }.getOrDefault(false)

    private val client: HealthConnectClient? by lazy {
        runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    suspend fun hasPermissions(): Boolean = runCatching {
        val granted = client?.permissionController?.getGrantedPermissions() ?: emptySet()
        granted.containsAll(permissions)
    }.getOrDefault(false)

    suspend fun writeWorkout(startMs: Long, endMs: Long, title: String, calories: Int) {
        val c = client ?: return
        runCatching {
            val start = Instant.ofEpochMilli(startMs)
            val end = Instant.ofEpochMilli(endMs.coerceAtLeast(startMs + 1000))
            val offset = ZoneOffset.systemDefault().rules.getOffset(start)
            c.insertRecords(
                listOf(
                    ExerciseSessionRecord(
                        startTime = start,
                        startZoneOffset = offset,
                        endTime = end,
                        endZoneOffset = offset,
                        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                        title = title,
                    ),
                    ActiveCaloriesBurnedRecord(
                        startTime = start,
                        startZoneOffset = offset,
                        endTime = end,
                        endZoneOffset = offset,
                        energy = Energy.kilocalories(calories.toDouble()),
                    ),
                ),
            )
        }
    }

    suspend fun writeWeight(weightKg: Double, atMs: Long = System.currentTimeMillis()) {
        val c = client ?: return
        runCatching {
            val time = Instant.ofEpochMilli(atMs)
            val offset = ZoneOffset.systemDefault().rules.getOffset(time)
            c.insertRecords(
                listOf(
                    WeightRecord(time = time, zoneOffset = offset, weight = Mass.kilograms(weightKg)),
                ),
            )
        }
    }
}
