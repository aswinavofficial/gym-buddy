package com.gymbuddy.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbuddy.data.local.entities.PlanItemEntity
import com.gymbuddy.ui.Format
import com.gymbuddy.ui.components.ExerciseThumb
import com.gymbuddy.ui.components.NumberStepper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    vm: PlanEditorViewModel,
    onBack: () -> Unit,
    onPickExercise: () -> Unit,
    onStartDay: (Long) -> Unit,
    onOpenExercise: (String) -> Unit,
) {
    val plan by vm.plan.collectAsStateWithLifecycle()
    val exMap by vm.exerciseMap.collectAsStateWithLifecycle()
    var editItem by remember { mutableStateOf<PlanItemEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan?.plan?.name ?: "Plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (plan?.plan?.isActive == true) {
                        AssistChip(onClick = {}, label = { Text("Active") }, leadingIcon = {
                            Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp))
                        })
                    } else {
                        TextButton(onClick = { vm.setActive() }) { Text("Set active") }
                    }
                },
            )
        },
    ) { padding ->
        val p = plan ?: return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            p.days.sortedBy { it.day.dayIndex }.forEach { dayWithItems ->
                item(key = "day-${dayWithItems.day.id}") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            dayWithItems.day.label,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        if (dayWithItems.items.isNotEmpty()) {
                            IconButton(onClick = { vm.startDay(dayWithItems.day.id, onStartDay) }) {
                                Icon(Icons.Filled.PlayArrow, "Start", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(onClick = { vm.deleteDay(dayWithItems.day.id) }) {
                            Icon(Icons.Filled.Delete, "Delete day")
                        }
                    }
                }
                items(dayWithItems.items.size, key = { i -> "item-${dayWithItems.items[i].id}" }) { i ->
                    val item = dayWithItems.items[i]
                    val ex = exMap[item.exerciseId]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExerciseThumb(
                                path = ex?.image ?: "",
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                            )
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    ex?.let { Format.titleCase(it.name) } ?: "Exercise",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                )
                                Text(
                                    "${item.targetSets} × ${item.targetReps} · ${item.restSec}s rest",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { editItem = item }) { Icon(Icons.Filled.Edit, "Edit") }
                            IconButton(onClick = { vm.beginSwap(item); onPickExercise() }) {
                                Icon(Icons.Filled.SwapHoriz, "Swap")
                            }
                            IconButton(onClick = { vm.deleteItem(item) }) { Icon(Icons.Filled.Delete, "Remove") }
                        }
                    }
                }
                item(key = "add-${dayWithItems.day.id}") {
                    OutlinedButton(
                        onClick = { vm.beginAdd(dayWithItems.day.id); onPickExercise() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Add exercise")
                    }
                }
            }
            item {
                Button(onClick = { vm.addDay() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Add day")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    editItem?.let { item ->
        ItemEditDialog(
            item = item,
            onDismiss = { editItem = null },
            onSave = { updated -> vm.updateItem(updated); editItem = null },
        )
    }
}

@Composable
private fun ItemEditDialog(
    item: PlanItemEntity,
    onDismiss: () -> Unit,
    onSave: (PlanItemEntity) -> Unit,
) {
    var sets by remember { mutableStateOf(item.targetSets) }
    var reps by remember { mutableStateOf(item.targetReps) }
    var rest by remember { mutableStateOf(item.restSec) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit targets") },
        text = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                NumberStepper("Sets", sets, { sets = it }, min = 1, max = 10)
                NumberStepper("Reps", reps, { reps = it }, min = 1, max = 50)
                NumberStepper("Rest s", rest, { rest = it }, min = 0, max = 600, step = 15)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(item.copy(targetSets = sets, targetReps = reps, restSec = rest)) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
