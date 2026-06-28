package com.gymbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbuddy.ui.LocalAppContainer
import com.gymbuddy.ui.nav.NavGraph
import com.gymbuddy.ui.theme.GymBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as GymBuddyApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                val settings by container.settingsRepository.settings
                    .collectAsStateWithLifecycle(initialValue = null)
                val darkTheme = settings?.darkMode ?: isSystemInDarkTheme()
                GymBuddyTheme(
                    darkTheme = darkTheme,
                    dynamicColor = settings?.dynamicColor ?: true,
                ) {
                    Surface(Modifier.fillMaxSize()) {
                        val s = settings
                        if (s == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            NavGraph(startMain = s.onboardingDone)
                        }
                    }
                }
            }
        }
    }
}
