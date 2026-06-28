package com.gymbuddy.ui.home

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbuddy.ui.Format
import com.gymbuddy.ui.components.ProgressRing
import com.gymbuddy.ui.components.SectionHeader
import com.gymbuddy.ui.components.StatTile

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onStartWorkout: (Long) -> Unit,
    onOpenSession: (Long) -> Unit,
    onBrowse: () -> Unit,
    onSeeHistory: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hi ${state.name} 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Let's make today count.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StreakBadge(state.streak)
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ProgressRing(
                        progress = if (state.weeklyGoal == 0) 0f else state.sessionsThisWeek / state.weeklyGoal.toFloat(),
                        size = 96.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${state.sessionsThisWeek}/${state.weeklyGoal}", fontWeight = FontWeight.Bold)
                            Text("week", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Weekly goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (state.sessionsThisWeek >= state.weeklyGoal) "Goal smashed! 🎉"
                            else "${(state.weeklyGoal - state.sessionsThisWeek).coerceAtLeast(0)} to go this week",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("${state.caloriesThisWeek}", "kcal this week", Modifier.weight(1f))
                StatTile(
                    "${state.totalWorkouts}",
                    "total workouts",
                    Modifier.weight(1f),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        item {
            val next = state.nextWorkout
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Next workout", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(4.dp))
                    if (next != null) {
                        Text(next.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${next.exerciseCount} exercises", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.startFromDay(next.dayId, onStartWorkout) }) {
                            Icon(Icons.Filled.PlayArrow, null)
                            Spacer(Modifier.size(6.dp))
                            Text("Start workout")
                        }
                    } else {
                        Text("No active plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Create a plan or start a freestyle session.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.startFreestyle(onStartWorkout) }) {
                                Icon(Icons.Filled.Bolt, null)
                                Spacer(Modifier.size(6.dp))
                                Text("Freestyle")
                            }
                            OutlinedButton(onClick = onBrowse) { Text("Browse") }
                        }
                    }
                }
            }
        }

        if (state.recent.isNotEmpty()) {
            item {
                SectionHeader("Recent activity", action = {
                    Text(
                        "See all",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onSeeHistory),
                    )
                })
            }
            items(state.recent.size) { index ->
                val s = state.recent[index]
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().clickable { onOpenSession(s.session.id) },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.session.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${Format.shortDate(s.session.startedAt)} · ${s.sets.count { it.done }} sets · ${s.session.totalCalories} kcal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakBadge(streak: Int) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Text("$streak", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}
