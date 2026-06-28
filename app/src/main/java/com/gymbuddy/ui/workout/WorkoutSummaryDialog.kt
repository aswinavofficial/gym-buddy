package com.gymbuddy.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymbuddy.data.model.AchievementType
import com.gymbuddy.data.repo.FinishResult

@Composable
fun WorkoutSummaryDialog(result: FinishResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text("Done") } },
        title = {
            Text(
                "Workout complete! 🎉",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("${result.calories}", "kcal")
                    Stat("${result.volumeKg.toInt()}", "kg")
                }
                if (result.newRecords.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("🎯 New personal records", fontWeight = FontWeight.Bold)
                            result.newRecords.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
                result.newAchievements.forEach { type ->
                    AchievementRow(type)
                }
            }
        },
    )
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun AchievementRow(type: AchievementType) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(type.emoji, style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.padding(start = 12.dp)) {
                Text("Achievement unlocked: ${type.title}", fontWeight = FontWeight.Bold)
                Text(type.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
