package com.gymbuddy.data.local

import androidx.room.TypeConverter
import com.gymbuddy.data.model.Experience
import com.gymbuddy.data.model.Goal
import com.gymbuddy.data.model.Sex
import com.gymbuddy.data.model.Units
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        runCatching { Json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())

    @TypeConverter fun fromSex(v: Sex): String = v.name
    @TypeConverter fun toSex(v: String): Sex = runCatching { Sex.valueOf(v) }.getOrDefault(Sex.OTHER)

    @TypeConverter fun fromUnits(v: Units): String = v.name
    @TypeConverter fun toUnits(v: String): Units = runCatching { Units.valueOf(v) }.getOrDefault(Units.METRIC)

    @TypeConverter fun fromGoal(v: Goal): String = v.name
    @TypeConverter fun toGoal(v: String): Goal = runCatching { Goal.valueOf(v) }.getOrDefault(Goal.GENERAL)

    @TypeConverter fun fromExperience(v: Experience): String = v.name
    @TypeConverter fun toExperience(v: String): Experience =
        runCatching { Experience.valueOf(v) }.getOrDefault(Experience.BEGINNER)
}
