package com.gymbuddy.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gymbuddy.data.model.Language
import com.gymbuddy.media.mediaUrl
import com.gymbuddy.ui.Format
import com.gymbuddy.ui.components.InfoChip
import com.gymbuddy.ui.components.SectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(vm: DetailViewModel, onBack: () -> Unit) {
    val exercise by vm.exercise.collectAsStateWithLifecycle()
    val language by vm.language.collectAsStateWithLifecycle()
    val record by vm.record.collectAsStateWithLifecycle()
    val plans by vm.plans.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPlanMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.let { Format.titleCase(it.name) } ?: "", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { vm.toggleFavorite() }) {
                        Icon(
                            if (exercise?.isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            "Favorite",
                            tint = if (exercise?.isFavorite == true) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val ex = exercise ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.2f),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(mediaUrl(ex.gifUrl)).crossfade(true).build(),
                    contentDescription = ex.name,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(Format.titleCase(ex.bodyPart))
                InfoChip(Format.titleCase(ex.equipment))
                InfoChip("Target: ${Format.titleCase(ex.target)}")
            }

            record?.let { pr ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Column {
                            Text("Personal record", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "Est. 1RM ${Format.trim(pr.bestEst1Rm)} kg · best ${Format.trim(pr.bestWeight)} kg × ${pr.bestReps}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            FilledTonalButton(onClick = { showPlanMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add to a plan")
                DropdownMenu(expanded = showPlanMenu, onDismissRequest = { showPlanMenu = false }) {
                    if (plans.isEmpty()) {
                        DropdownMenuItem(text = { Text("No plans yet") }, onClick = { showPlanMenu = false })
                    }
                    plans.forEach { plan ->
                        DropdownMenuItem(
                            text = { Text(plan.name) },
                            onClick = {
                                showPlanMenu = false
                                vm.addToPlanDay(plan.id) {
                                    scope.launch { snackbar.showSnackbar("Added to ${plan.name}") }
                                }
                            },
                        )
                    }
                }
            }

            // Muscles
            val secondary = ex.secondaryMuscles.filter { it.isNotBlank() }
            if (secondary.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionHeader("Muscles worked")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(
                            "Primary: ${Format.titleCase(ex.muscleGroup.ifBlank { ex.target })}",
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        secondary.forEach { InfoChip(Format.titleCase(it)) }
                    }
                }
            }

            // Instructions with language toggle
            val available = Language.entries.filter { ex.instructionFor(it.code)?.isNotBlank() == true }
            if (available.isNotEmpty()) {
                SectionHeader("How to perform")
                if (available.size > 1) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        available.forEachIndexed { index, lang ->
                            SegmentedButton(
                                selected = lang == language,
                                onClick = { vm.setLanguage(lang) },
                                shape = SegmentedButtonDefaults.itemShape(index, available.size),
                            ) { Text(lang.label) }
                        }
                    }
                }
                val activeLang = if (language in available) language else available.first()
                val steps = ex.stepsFor(activeLang.code)
                if (steps.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        steps.forEachIndexed { i, step ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(
                                        "${i + 1}",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                                Text(step, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    Text(
                        ex.instructionFor(activeLang.code).orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
