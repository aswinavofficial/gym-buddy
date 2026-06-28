package com.gymbuddy.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbuddy.data.model.Language
import com.gymbuddy.data.model.Units
import com.gymbuddy.data.repo.SyncState
import com.gymbuddy.media.DownloadProgress
import com.gymbuddy.reminder.ReminderScheduler
import com.gymbuddy.ui.Format
import com.gymbuddy.ui.LocalAppContainer
import com.gymbuddy.ui.components.NumberStepper
import com.gymbuddy.ui.components.SectionHeader
import com.gymbuddy.ui.components.SegmentedChoice

@Composable
fun ProfileScreen(vm: ProfileViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val syncState by vm.syncState.collectAsStateWithLifecycle()
    val download by vm.downloadState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val container = LocalAppContainer.current
    var showEdit by remember { mutableStateOf(false) }

    val hcLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        vm.setHealthConnect(granted.containsAll(container.healthConnectManager.permissions))
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                profile?.name?.ifBlank { "Athlete" } ?: "Athlete",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                profile?.goal?.label ?: "",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        OutlinedButton(onClick = { showEdit = true }) {
                            Icon(Icons.Filled.Edit, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Edit")
                        }
                    }
                    profile?.let { p ->
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ProfileStat(Format.weight(p.weightKg, p.units), "weight")
                            ProfileStat(Format.height(p.heightCm, p.units), "height")
                            ProfileStat("${p.daysPerWeek}/wk", "frequency")
                            ProfileStat(p.experience.label, "level")
                        }
                    }
                }
            }
        }

        item { SectionHeader("Preferences") }
        item {
            SettingsCard {
                Text("Instruction language", style = MaterialTheme.typography.labelLarge)
                SegmentedChoice(
                    options = Language.entries,
                    selected = settings.language,
                    label = { it.label },
                    onSelect = { vm.setLanguage(it) },
                )
                Spacer(Modifier.height(12.dp))
                Text("Units", style = MaterialTheme.typography.labelLarge)
                SegmentedChoice(
                    options = Units.entries,
                    selected = profile?.units ?: Units.METRIC,
                    label = { if (it == Units.METRIC) "Metric" else "Imperial" },
                    onSelect = { vm.setUnits(it) },
                )
                Spacer(Modifier.height(12.dp))
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                val themeOptions = listOf<Boolean?>(null, false, true)
                SegmentedChoice(
                    options = themeOptions,
                    selected = settings.darkMode,
                    label = { when (it) { null -> "System"; false -> "Light"; else -> "Dark" } },
                    onSelect = { vm.setDarkMode(it) },
                )
                Spacer(Modifier.height(8.dp))
                ToggleRow("Dynamic color (Material You)", settings.dynamicColor) { vm.setDynamicColor(it) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Weekly workout goal", Modifier.weight(1f))
                    NumberStepper("", settings.weeklyGoal, { vm.setWeeklyGoal(it) }, min = 1, max = 14)
                }
            }
        }

        item { SectionHeader("Reminders") }
        item {
            SettingsCard {
                ToggleRow(
                    "Daily workout reminder",
                    settings.remindersEnabled,
                    leading = Icons.Filled.Notifications,
                ) { enabled ->
                    vm.setReminders(enabled, settings.reminderHour)
                    if (enabled) ReminderScheduler.schedule(context, settings.reminderHour)
                    else ReminderScheduler.cancel(context)
                }
                if (settings.remindersEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Reminder hour", Modifier.weight(1f))
                        NumberStepper("", settings.reminderHour, {
                            vm.setReminders(true, it)
                            ReminderScheduler.schedule(context, it)
                        }, min = 0, max = 23)
                    }
                }
            }
        }

        item { SectionHeader("Exercise data & offline") }
        item {
            SettingsCard {
                Text("Dataset version: ${settings.dataSha.take(7)}", style = MaterialTheme.typography.bodyMedium)
                if (settings.lastSyncedAt > 0) {
                    Text(
                        "Last updated ${Format.date(settings.lastSyncedAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SyncRow(syncState, onCheck = { vm.checkForUpdate() }, onSync = { vm.sync() })
                Spacer(Modifier.height(12.dp))
                DownloadRow(download, onStart = { vm.startMediaDownload() }, onCancel = { vm.cancelMediaDownload() })
            }
        }

        item { SectionHeader("Integrations") }
        item {
            SettingsCard {
                val hc = container.healthConnectManager
                ToggleRow(
                    if (hc.isAvailable) "Sync with Health Connect" else "Health Connect unavailable",
                    settings.healthConnectEnabled && hc.isAvailable,
                    leading = Icons.Filled.HealthAndSafety,
                    enabled = hc.isAvailable,
                ) { enabled ->
                    if (enabled) hcLauncher.launch(hc.permissions) else vm.setHealthConnect(false)
                }
                Text(
                    "Write workouts, calories and body weight to Health Connect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { SectionHeader("About") }
        item {
            SettingsCard {
                Text("Gym Buddy", fontWeight = FontWeight.Bold)
                Text(
                    "Exercise data from the open hasaneyldrm/exercises-dataset, used for educational, " +
                        "non-commercial purposes. Calorie figures are MET-based estimates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showEdit) {
        profile?.let { p ->
            EditProfileDialog(profile = p, onDismiss = { showEdit = false }, onSave = {
                vm.saveProfile(it); showEdit = false
            })
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    leading: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (leading != null) {
            Icon(leading, null, Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
        }
        Text(title, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun SyncRow(state: SyncState, onCheck: () -> Unit, onSync: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        when (state) {
            is SyncState.Checking, SyncState.Downloading -> {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(10.dp))
                Text(if (state is SyncState.Downloading) "Downloading…" else "Checking…", Modifier.weight(1f))
            }
            is SyncState.UpdateAvailable -> {
                Text("Update available", Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                Button(onClick = onSync) { Text("Update now") }
            }
            is SyncState.UpToDate -> {
                Text("You're up to date ✓", Modifier.weight(1f))
                TextButton(onClick = onCheck) { Text("Check") }
            }
            is SyncState.Done -> {
                Text("Updated ${state.count} exercises ✓", Modifier.weight(1f))
            }
            is SyncState.Error -> {
                Text(state.message, Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onCheck) { Text("Retry") }
            }
            SyncState.Idle -> {
                Text("Check for the latest exercises", Modifier.weight(1f))
                OutlinedButton(onClick = onCheck) {
                    Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Check")
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(state: DownloadProgress, onStart: () -> Unit, onCancel: () -> Unit) {
    when (state) {
        is DownloadProgress.Running -> {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (state.total > 0) "Downloading ${state.current}/${state.total}" else "Starting…",
                        Modifier.weight(1f),
                    )
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
                LinearProgressIndicator(
                    progress = { if (state.total > 0) state.current.toFloat() / state.total else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        DownloadProgress.Done -> Text("All media downloaded ✓ — fully offline", color = MaterialTheme.colorScheme.primary)
        DownloadProgress.Failed -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Download failed", Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onStart) { Text("Retry") }
            }
        }
        DownloadProgress.Idle -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Download all media for offline")
                    Text("≈130 MB — images + animations", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onStart) {
                    Icon(Icons.Filled.CloudDownload, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Download")
                }
            }
        }
    }
}
