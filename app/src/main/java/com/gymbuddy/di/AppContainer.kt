package com.gymbuddy.di

import android.content.Context
import coil.ImageLoader
import com.gymbuddy.data.AppJson
import com.gymbuddy.data.health.HealthConnectManager
import com.gymbuddy.data.local.AppDatabase
import com.gymbuddy.data.remote.DatasetRemoteSource
import com.gymbuddy.data.remote.GitHubApi
import com.gymbuddy.data.repo.ExerciseRepository
import com.gymbuddy.data.repo.PlanRepository
import com.gymbuddy.data.repo.ProfileRepository
import com.gymbuddy.data.repo.SettingsRepository
import com.gymbuddy.data.repo.SyncRepository
import com.gymbuddy.data.repo.WorkoutRepository
import com.gymbuddy.data.seed.AssetSeeder
import com.gymbuddy.media.CoilProvider
import com.gymbuddy.media.MediaDownloadController
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Manual dependency container, created once in [com.gymbuddy.GymBuddyApp]. */
class AppContainer(context: Context) {

    val database: AppDatabase = AppDatabase.build(context)
    val imageLoader: ImageLoader = CoilProvider.build(context)

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
        )
        .build()

    private val gitHubApi: GitHubApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttp)
        .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GitHubApi::class.java)

    private val remoteSource = DatasetRemoteSource(gitHubApi, okHttp)

    val settingsRepository = SettingsRepository(context)
    val exerciseRepository = ExerciseRepository(database.exerciseDao())
    val profileRepository = ProfileRepository(database.profileDao())
    val planRepository = PlanRepository(database.planDao(), database.exerciseDao())
    val workoutRepository = WorkoutRepository(
        workoutDao = database.workoutDao(),
        exerciseDao = database.exerciseDao(),
        planDao = database.planDao(),
        recordsDao = database.recordsDao(),
        profileDao = database.profileDao(),
    )
    val syncRepository = SyncRepository(remoteSource, database.exerciseDao(), settingsRepository)
    val seeder = AssetSeeder(context, database.exerciseDao())
    val mediaDownloadController = MediaDownloadController(context)
    val healthConnectManager = HealthConnectManager(context)
}
