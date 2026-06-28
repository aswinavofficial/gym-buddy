package com.gymbuddy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gymbuddy.data.local.dao.ExerciseDao
import com.gymbuddy.data.local.dao.PlanDao
import com.gymbuddy.data.local.dao.ProfileDao
import com.gymbuddy.data.local.dao.RecordsDao
import com.gymbuddy.data.local.dao.WorkoutDao
import com.gymbuddy.data.local.entities.AchievementEntity
import com.gymbuddy.data.local.entities.BodyMetricEntity
import com.gymbuddy.data.local.entities.ExerciseEntity
import com.gymbuddy.data.local.entities.PersonalRecordEntity
import com.gymbuddy.data.local.entities.PlanDayEntity
import com.gymbuddy.data.local.entities.PlanItemEntity
import com.gymbuddy.data.local.entities.SetLogEntity
import com.gymbuddy.data.local.entities.UserProfileEntity
import com.gymbuddy.data.local.entities.WorkoutPlanEntity
import com.gymbuddy.data.local.entities.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        UserProfileEntity::class,
        WorkoutPlanEntity::class,
        PlanDayEntity::class,
        PlanItemEntity::class,
        WorkoutSessionEntity::class,
        SetLogEntity::class,
        PersonalRecordEntity::class,
        BodyMetricEntity::class,
        AchievementEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun profileDao(): ProfileDao
    abstract fun planDao(): PlanDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun recordsDao(): RecordsDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "gymbuddy.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
