package com.gymbuddy.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbuddy.ui.Format
import com.gymbuddy.ui.components.BarChart
import com.gymbuddy.ui.components.EmptyState
import com.gymbuddy.ui.components.LineChart
import com.gymbuddy.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProgressScreen(vm: ProgressViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val names by vm.namesState.collectAsStateWithLifecycle()
    var showWeightDialog by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text("Progress", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    SectionHeader("Weekly training volume")
                    Spacer(Modifier.height(12.dp))
                    if (state.weeklyVolume.all { it.volume == 0f }) {
                        Text("Log workouts to see your volume trend.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        BarChart(
                            values = state.weeklyVolume.map { it.volume },
                            labels = state.weeklyVolume.map { it.weekLabel },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                        )
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            state.weeklyVolume.forEach {
                                Text(it.weekLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    SectionHeader("Body weight", action = {
                        TextButton(onClick = { showWeightDialog = true }) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                            Text("Log")
                        }
                    })
                    Spacer(Modifier.height(12.dp))
                    if (state.bodyMetrics.size < 2) {
                        Text("Log your weight regularly to track the trend.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LineChart(
                            values = state.bodyMetrics.map { Format.weightValue(it.weightKg, state.units).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                        )
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Format.weight(state.bodyMetrics.first().weightKg, state.units), style = MaterialTheme.typography.labelSmall)
                            Text(Format.weight(state.bodyMetrics.last().weightKg, state.units), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Achievements")
        }
        item {
            if (state.achievements.isEmpty()) {
                Text("Complete workouts to unlock achievements.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.achievements.forEach { a ->
                        val type = runCatching { com.gymbuddy.data.model.AchievementType.valueOf(a.type) }.getOrNull()
                        if (type != null) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(type.emoji)
                                    Spacer(Modifier.size(6.dp))
                                    Text(type.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeader("Personal records") }
        if (state.records.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.EmojiEvents,
                    title = "No records yet",
                    subtitle = "Lift with weights logged to set your first PR.",
                )
            }
        } else {
            items(state.records.size) { index ->
                val pr = state.records[index]
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                names[pr.exerciseId]?.let { Format.titleCase(it) } ?: "Exercise",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                "Best ${Format.weight(pr.bestWeight, state.units)} × ${pr.bestReps}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${Format.trim(pr.bestEst1Rm)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("est. 1RM", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showWeightDialog) {
        LogWeightDialog(units = state.units, onDismiss = { showWeightDialog = false }, onSave = {
            vm.logBodyWeight(it); showWeightDialog = false
        })
    }
}

@Composable
private fun LogWeightDialog(
    units: com.gymbuddy.data.model.Units,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.MonitorWeight, null) },
        title = { Text("Log body weight") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Weight (${units.weightLabel})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val v = text.toDoubleOrNull() ?: return@TextButton
                    onSave(Format.toKg(v, units))
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
