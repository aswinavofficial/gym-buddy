package com.gymbuddy.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbuddy.data.local.entities.ExerciseEntity
import com.gymbuddy.ui.Format
import com.gymbuddy.ui.components.ExerciseThumb
import com.gymbuddy.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    vm: BrowseViewModel,
    onExerciseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Exercises",
    onBack: (() -> Unit)? = null,
    showFavorites: Boolean = true,
) {
    val filters by vm.filters.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val equipment by vm.equipment.collectAsStateWithLifecycle()
    val targets by vm.targets.collectAsStateWithLifecycle()

    var showSheet by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${exercises.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = filters.query,
            onValueChange = vm::setQuery,
            placeholder = { Text("Search exercises, muscles…") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (filters.query.isNotEmpty()) {
                    IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Filled.Close, "Clear") }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BadgedBox(
                badge = { if (filters.activeCount > 0) Badge { Text("${filters.activeCount}") } },
            ) {
                AssistChip(
                    onClick = { showSheet = true },
                    label = { Text("Filters") },
                    leadingIcon = { Icon(Icons.Filled.FilterList, null, Modifier.size(18.dp)) },
                )
            }
            if (showFavorites) {
                FilterChip(
                    selected = filters.favoritesOnly,
                    onClick = { vm.toggleFavoritesOnly() },
                    label = { Text("Favorites") },
                    leadingIcon = {
                        Icon(
                            if (filters.favoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            null,
                            Modifier.size(18.dp),
                        )
                    },
                )
            }
            if (filters.activeCount > 0) {
                AssistChip(
                    onClick = { vm.clearFilters() },
                    label = { Text("Clear") },
                    colors = AssistChipDefaults.assistChipColors(),
                )
            }
        }

        if (exercises.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.SearchOff,
                title = "No exercises found",
                subtitle = "Try adjusting your search or filters.",
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(exercises, key = { it.id }) { ex ->
                    ExerciseCard(
                        exercise = ex,
                        onClick = { onExerciseClick(ex.id) },
                        onFavorite = if (showFavorites) ({ vm.toggleFavorite(ex.id) }) else null,
                    )
                }
            }
        }
    }

    if (showSheet) {
        FilterBottomSheet(
            categories = categories,
            equipment = equipment,
            targets = targets,
            filters = filters,
            onCategory = vm::setCategory,
            onEquipment = vm::setEquipment,
            onTarget = vm::setTarget,
            onClear = vm::clearFilters,
            onDismiss = { showSheet = false },
        )
    }
}

@Composable
fun ExerciseCard(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    onFavorite: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Box {
            ExerciseThumb(
                path = exercise.image,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .aspectRatio(1f),
            )
            if (onFavorite != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                ) {
                    IconButton(onClick = onFavorite, modifier = Modifier.size(34.dp)) {
                        Icon(
                            if (exercise.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            "Favorite",
                            tint = if (exercise.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(
                Format.titleCase(exercise.name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${Format.titleCase(exercise.target)} · ${Format.titleCase(exercise.equipment)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
