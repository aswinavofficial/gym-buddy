package com.gymbuddy.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbuddy.ui.components.EmptyState
import com.gymbuddy.ui.components.InfoChip

@Composable
fun PlansScreen(vm: PlanViewModel, onOpenPlan: (Long) -> Unit) {
    val plans by vm.plans.collectAsStateWithLifecycle()
    val generating by vm.generating

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text("Plans", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { vm.generateForMe(onOpenPlan) },
                    enabled = !generating,
                    modifier = Modifier.weight(1f),
                ) {
                    if (generating) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
                    }
                    Spacer(Modifier.size(8.dp))
                    Text("Generate for me")
                }
                OutlinedButton(onClick = { vm.createBlank(onOpenPlan) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Blank")
                }
            }
        }

        if (plans.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.ListAlt,
                    title = "No plans yet",
                    subtitle = "Generate a plan tailored to your profile, or build one from scratch.",
                )
            }
        } else {
            items(plans, key = { it.id }) { plan ->
                Card(
                    onClick = { onOpenPlan(plan.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (plan.isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(plan.goal.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { vm.delete(plan) }) {
                                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.size(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (plan.isActive) {
                                InfoChip("Active", container = MaterialTheme.colorScheme.primary, content = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                TextButton(onClick = { vm.setActive(plan.id) }) {
                                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("Set active")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
