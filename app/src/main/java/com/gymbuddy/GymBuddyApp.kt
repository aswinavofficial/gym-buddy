package com.gymbuddy

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.gymbuddy.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GymBuddyApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            // Seed the bundled dataset into Room on first launch.
            runCatching { container.seeder.seedIfEmpty() }
        }
    }

    override fun newImageLoader(): ImageLoader = container.imageLoader
}
