package com.gymbuddy.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymbuddy.data.local.entities.UserProfileEntity
import com.gymbuddy.data.model.Experience
import com.gymbuddy.data.model.Goal
import com.gymbuddy.data.model.Units
import com.gymbuddy.ui.Format
import com.gymbuddy.ui.components.SegmentedChoice
import java.util.Calendar

@Composable
fun EditProfileDialog(
    profile: UserProfileEntity,
    onDismiss: () -> Unit,
    onSave: (UserProfileEntity) -> Unit,
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var name by remember { mutableStateOf(profile.name) }
    var age by remember { mutableStateOf((currentYear - profile.birthYear).toString()) }
    var height by remember {
        mutableStateOf(Format.trim(if (profile.units == Units.METRIC) profile.heightCm else profile.heightCm / 2.54))
    }
    var weight by remember { mutableStateOf(Format.trim(Format.weightValue(profile.weightKg, profile.units))) }
    var goal by remember { mutableStateOf(profile.goal) }
    var experience by remember { mutableStateOf(profile.experience) }
    var days by remember { mutableStateOf(profile.daysPerWeek) }

    fun build(): UserProfileEntity {
        val heightCm = height.toDoubleOrNull()?.let {
            if (profile.units == Units.METRIC) it else it * 2.54
        } ?: profile.heightCm
        val weightKg = weight.toDoubleOrNull()?.let { Format.toKg(it, profile.units) } ?: profile.weightKg
        val birthYear = age.toIntOrNull()?.let { currentYear - it } ?: profile.birthYear
        return profile.copy(
            name = name.trim(),
            birthYear = birthYear,
            heightCm = heightCm,
            weightKg = weightKg,
            goal = goal,
            experience = experience,
            daysPerWeek = days,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Age") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Height (${profile.units.heightLabel})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Weight (${profile.units.weightLabel})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Goal")
                SegmentedChoice(
                    options = Goal.entries,
                    selected = goal,
                    label = { it.label.substringBefore(" ") },
                    onSelect = { goal = it },
                )
                Text("Experience")
                SegmentedChoice(
                    options = Experience.entries,
                    selected = experience,
                    label = { it.label },
                    onSelect = { experience = it },
                )
                Text("Days per week: $days")
                SegmentedChoice(
                    options = (1..7).toList(),
                    selected = days,
                    label = { it.toString() },
                    onSelect = { days = it },
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(build()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
