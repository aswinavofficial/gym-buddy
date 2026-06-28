package com.gymbuddy.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbuddy.data.local.entities.SetLogEntity
import com.gymbuddy.ui.Format
import com.gymbuddy.ui.components.ExerciseThumb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    vm: WorkoutViewModel,
    onPickExercise: () -> Unit,
    onFinished: () -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val elapsed by vm.elapsed.collectAsStateWithLifecycle()
    val rest by vm.rest.collectAsStateWithLifecycle()
    val finish by vm.finishResult.collectAsStateWithLifecycle()
    var showCancel by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(ui.title, maxLines = 1, fontWeight = FontWeight.Bold)
                        Text(Format.duration(elapsed), style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showCancel = true }) { Icon(Icons.Filled.Close, "Cancel") }
                },
                actions = {
                    TextButton(onClick = { vm.finish() }) { Text("Finish") }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = rest != null) {
                rest?.let { RestBar(it, onAdd = { vm.addRest(15) }, onSkip = { vm.skipRest() }) }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat("${ui.completedSets}/${ui.totalSets}", "sets", Modifier.weight(1f))
                    MiniStat("${ui.volumeKg.toInt()}", "kg volume", Modifier.weight(1f))
                }
            }
            ui.groups.forEach { group ->
                item(key = "g-${group.exerciseId}") {
                    ExerciseGroupCard(
                        group = group,
                        onUpdate = { set, reps, weight -> vm.updateSet(set, reps, weight) },
                        onToggle = { vm.toggleDone(it) },
                        onAddSet = { vm.addSet(group) },
                        onDeleteSet = { vm.deleteSet(it) },
                    )
                }
            }
            item {
                OutlinedButton(
                    onClick = { vm.beginAddExercise(); onPickExercise() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add exercise")
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showCancel) {
        AlertDialog(
            onDismissRequest = { showCancel = false },
            title = { Text("Discard workout?") },
            text = { Text("Your logged sets for this session will be discarded.") },
            confirmButton = {
                TextButton(onClick = { vm.cancel(); showCancel = false; onFinished() }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { showCancel = false }) { Text("Keep going") } },
        )
    }

    finish?.let { result ->
        WorkoutSummaryDialog(result = result, onDismiss = onFinished)
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun ExerciseGroupCard(
    group: ExerciseGroup,
    onUpdate: (SetLogEntity, Int, Double?) -> Unit,
    onToggle: (SetLogEntity) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (SetLogEntity) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumb(group.exercise?.image ?: "", Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(10.dp))
                Text(
                    group.exercise?.let { Format.titleCase(it.name) } ?: "Exercise",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 4.dp)) {
                Text("Set", Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall)
                Text("Kg", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Text("Reps", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(88.dp))
            }
            group.sets.forEach { set ->
                SetRow(
                    set = set,
                    onUpdate = onUpdate,
                    onToggle = { onToggle(set) },
                    onDelete = { onDeleteSet(set) },
                )
            }
            TextButton(onClick = onAddSet) {
                Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add set")
            }
        }
    }
}

@Composable
private fun SetRow(
    set: SetLogEntity,
    onUpdate: (SetLogEntity, Int, Double?) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    var weightText by remember(set.id) { mutableStateOf(set.weightKg?.let { Format.trim(it) } ?: "") }
    var repsText by remember(set.id) { mutableStateOf(set.reps.toString()) }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${set.setIndex + 1}", Modifier.width(36.dp), fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = weightText,
            onValueChange = {
                weightText = it.filter { c -> c.isDigit() || c == '.' }
                onUpdate(set, repsText.toIntOrNull() ?: set.reps, weightText.toDoubleOrNull())
            },
            singleLine = true,
            placeholder = { Text("–") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        OutlinedTextField(
            value = repsText,
            onValueChange = {
                repsText = it.filter { c -> c.isDigit() }
                onUpdate(set, repsText.toIntOrNull() ?: 0, weightText.toDoubleOrNull())
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        FilledIconButton(
            onClick = onToggle,
            colors = if (set.done) {
                IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            } else {
                IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(Icons.Filled.Check, "Done", tint = if (set.done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete set", Modifier.size(18.dp)) }
    }
}

@Composable
private fun RestBar(seconds: Int, onAdd: () -> Unit, onSkip: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primary) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(10.dp))
            Text(
                "Rest · ${Format.duration(seconds * 1000L)}",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(onClick = onAdd) { Text("+15s") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onSkip) {
                Text("Skip", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
