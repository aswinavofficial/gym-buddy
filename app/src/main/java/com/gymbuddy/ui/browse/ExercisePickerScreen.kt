package com.gymbuddy.ui.browse

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gymbuddy.ui.containerViewModel

/** Reuses the browse UI as an exercise picker that returns a selected exercise id. */
@Composable
fun ExercisePickerScreen(onPicked: (String) -> Unit, onBack: () -> Unit) {
    val vm = containerViewModel(key = "picker") { BrowseViewModel(it.exerciseRepository) }
    BrowseScreen(
        vm = vm,
        onExerciseClick = onPicked,
        title = "Pick an exercise",
        onBack = onBack,
        showFavorites = false,
        modifier = Modifier,
    )
}
