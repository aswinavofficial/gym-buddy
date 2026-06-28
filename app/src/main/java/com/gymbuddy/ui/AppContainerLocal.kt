package com.gymbuddy.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.gymbuddy.di.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
