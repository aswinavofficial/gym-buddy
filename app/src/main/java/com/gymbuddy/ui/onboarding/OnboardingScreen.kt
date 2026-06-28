package com.gymbuddy.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.gymbuddy.data.local.entities.UserProfileEntity
import com.gymbuddy.data.model.Experience
import com.gymbuddy.data.model.Goal
import com.gymbuddy.data.model.Sex
import com.gymbuddy.data.model.Units
import com.gymbuddy.ui.LocalAppContainer
import com.gymbuddy.ui.components.SelectableCard
import com.gymbuddy.ui.components.SegmentedChoice
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    var step by remember { mutableStateOf(0) }
    val totalSteps = 4

    var name by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(Sex.OTHER) }
    var units by remember { mutableStateOf(Units.METRIC) }
    var age by remember { mutableStateOf("28") }
    var heightText by remember { mutableStateOf("175") }
    var weightText by remember { mutableStateOf("75") }
    var goal by remember { mutableStateOf(Goal.BUILD_MUSCLE) }
    var experience by remember { mutableStateOf(Experience.BEGINNER) }
    var days by remember { mutableStateOf(3f) }
    val equipmentOptions = remember { mutableStateOf<List<String>>(emptyList()) }
    val selectedEquipment = remember { mutableStateOf<Set<String>>(emptySet()) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        equipmentOptions.value = container.exerciseRepository.allEquipment()
    }

    fun finish() {
        if (saving) return
        saving = true
        scope.launch {
            val heightCm = heightText.toDoubleOrNull()?.let {
                if (units == Units.METRIC) it else it * 2.54
            } ?: 175.0
            val weightKg = weightText.toDoubleOrNull()?.let {
                if (units == Units.METRIC) it else it / 2.2046226218
            } ?: 75.0
            val birthYear = age.toIntOrNull()?.let { currentYear - it } ?: 1995
            val profile = UserProfileEntity(
                name = name.trim(),
                sex = sex,
                birthYear = birthYear,
                heightCm = heightCm,
                weightKg = weightKg,
                goal = goal,
                experience = experience,
                daysPerWeek = days.toInt(),
                availableEquipment = selectedEquipment.value.toList(),
                units = units,
            )
            container.profileRepository.save(profile)
            runCatching { container.planRepository.generateForProfile(profile, makeActive = true) }
            container.settingsRepository.setOnboardingDone(true)
            onDone()
        }
    }

    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
        Column(Modifier.fillMaxSize()) {
            LinearProgressIndicator(
                progress = { (step + 1f) / totalSteps },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding-step",
                modifier = Modifier.weight(1f),
            ) { s ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (s) {
                        0 -> {
                            StepTitle("Welcome to Gym Buddy", "Let's set up your training. First, the basics.")
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Your name (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text("Sex", style = MaterialTheme.typography.labelLarge)
                            SegmentedChoice(
                                options = Sex.entries,
                                selected = sex,
                                label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                                onSelect = { sex = it },
                            )
                            Text("Units", style = MaterialTheme.typography.labelLarge)
                            SegmentedChoice(
                                options = Units.entries,
                                selected = units,
                                label = { if (it == Units.METRIC) "Metric (kg/cm)" else "Imperial (lb/in)" },
                                onSelect = { units = it },
                            )
                        }
                        1 -> {
                            StepTitle("About you", "We use these to estimate calories and tailor intensity.")
                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it.filter { c -> c.isDigit() }.take(3) },
                                label = { Text("Age") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = heightText,
                                onValueChange = { heightText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Height (${units.heightLabel})") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Weight (${units.weightLabel})") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        2 -> {
                            StepTitle("Your goal", "We'll pick rep ranges and rest to match.")
                            Goal.entries.forEach { g ->
                                SelectableCard(
                                    selected = goal == g,
                                    title = g.label,
                                    subtitle = "${g.defaultSets} sets · ${g.repLow}-${g.repHigh} reps · ${g.restSec}s rest",
                                    onClick = { goal = g },
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Experience", style = MaterialTheme.typography.labelLarge)
                            SegmentedChoice(
                                options = Experience.entries,
                                selected = experience,
                                label = { it.label },
                                onSelect = { experience = it },
                            )
                        }
                        3 -> {
                            StepTitle("Schedule & equipment", "How often will you train, and what do you have?")
                            Text("Days per week: ${days.toInt()}", style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = days,
                                onValueChange = { days = it },
                                valueRange = 1f..7f,
                                steps = 5,
                            )
                            Text("Available equipment", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "Leave empty to include everything.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                equipmentOptions.value.forEach { eq ->
                                    val isSel = eq in selectedEquipment.value
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            selectedEquipment.value =
                                                if (isSel) selectedEquipment.value - eq else selectedEquipment.value + eq
                                        },
                                        label = { Text(eq.replaceFirstChar { it.uppercase() }) },
                                        leadingIcon = if (isSel) {
                                            { Icon(Icons.Filled.Check, null, Modifier.height(16.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step > 0) {
                    OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = { if (step < totalSteps - 1) step++ else finish() },
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (step < totalSteps - 1) "Continue" else "Create my plan")
                }
            }
            if (step == totalSteps - 1) {
                TextButton(onClick = { finish() }, enabled = !saving, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Skip plan, just browse")
                }
            }
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
    }
}
