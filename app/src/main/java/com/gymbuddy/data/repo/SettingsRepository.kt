package com.gymbuddy.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gymbuddy.BuildConfig
import com.gymbuddy.data.model.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val onboardingDone: Boolean = false,
    val language: Language = Language.EN,
    val dynamicColor: Boolean = true,
    val darkMode: Boolean? = null,
    val dataSha: String = BuildConfig.BUNDLED_DATASET_SHA,
    val lastSyncedAt: Long = 0L,
    val weeklyGoal: Int = 4,
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 18,
    val healthConnectEnabled: Boolean = false,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val LANGUAGE = stringPreferencesKey("language")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val DARK = stringPreferencesKey("dark_mode") // "on" | "off" | absent(system)
        val SHA = stringPreferencesKey("data_sha")
        val SYNCED = longPreferencesKey("last_synced")
        val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        val REMINDERS = booleanPreferencesKey("reminders")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val HEALTH = booleanPreferencesKey("health_connect")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            onboardingDone = p[Keys.ONBOARDING] ?: false,
            language = Language.entries.firstOrNull { it.code == p[Keys.LANGUAGE] } ?: Language.EN,
            dynamicColor = p[Keys.DYNAMIC] ?: true,
            darkMode = when (p[Keys.DARK]) {
                "on" -> true
                "off" -> false
                else -> null
            },
            dataSha = p[Keys.SHA] ?: BuildConfig.BUNDLED_DATASET_SHA,
            lastSyncedAt = p[Keys.SYNCED] ?: 0L,
            weeklyGoal = p[Keys.WEEKLY_GOAL] ?: 4,
            remindersEnabled = p[Keys.REMINDERS] ?: false,
            reminderHour = p[Keys.REMINDER_HOUR] ?: 18,
            healthConnectEnabled = p[Keys.HEALTH] ?: false,
        )
    }

    suspend fun setOnboardingDone(done: Boolean) = edit { it[Keys.ONBOARDING] = done }
    suspend fun setLanguage(lang: Language) = edit { it[Keys.LANGUAGE] = lang.code }
    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC] = enabled }
    suspend fun setDarkMode(value: Boolean?) = edit {
        if (value == null) it.remove(Keys.DARK) else it[Keys.DARK] = if (value) "on" else "off"
    }

    suspend fun setVersion(sha: String, syncedAt: Long) = edit {
        it[Keys.SHA] = sha
        it[Keys.SYNCED] = syncedAt
    }

    suspend fun setWeeklyGoal(goal: Int) = edit { it[Keys.WEEKLY_GOAL] = goal }
    suspend fun setReminders(enabled: Boolean, hour: Int) = edit {
        it[Keys.REMINDERS] = enabled
        it[Keys.REMINDER_HOUR] = hour
    }
    suspend fun setHealthConnect(enabled: Boolean) = edit { it[Keys.HEALTH] = enabled }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
