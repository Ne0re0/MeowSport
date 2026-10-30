package fr.meowsport.ui.templates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.meowsport.app
import fr.meowsport.data.EditLine
import fr.meowsport.ui.components.ExerciseCover
import fr.meowsport.ui.components.Stepper
import fr.meowsport.ui.components.WeightField
import fr.meowsport.ui.formatRest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditScreen(
    templateId: Long?,
    picked: LongArray?,
    onPickedConsumed: () -> Unit,
    onPickExercises: () -> Unit,
    onDone: () -> Unit,
) {
    val repo = LocalContext.current.app.repo
    val appSettings = LocalContext.current.app.settings
    val vm: TemplateEditViewModel = viewModel(key = "template-$templateId") { TemplateEditViewModel(repo, appSettings, templateId) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmLeave by remember { mutableStateOf(false) }

    LaunchedEffect(picked) {
        if (picked != null) {
            vm.addExercises(picked.toList())
            onPickedConsumed()
        }
    }

    fun leave() {
        if (vm.dirty) confirmLeave = true else onDone()
    }
    BackHandler { leave() }

    fun save() {
        vm.save(onSaved = onDone)?.let { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (templateId == null) "Nouveau modèle" else "Modifier le modèle") },
                navigationIcon = {
                    IconButton(onClick = { leave() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = { TextButton(onClick = { save() }) { Text("Enregistrer") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = vm.name,
                    onValueChange = vm::rename,
                    label = { Text("Nom de la séance") },
                    placeholder = { Text("Ex. Push, Haut du corps, Jambes…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(vm.lines, key = { it.key }) { ui ->
                LineEditor(
                    line = ui.data,
                    isFirst = vm.lines.firstOrNull()?.key == ui.key,
                    isLast = vm.lines.lastOrNull()?.key == ui.key,
                    onChange = { change -> vm.update(ui.key, change) },
                    onMove = { vm.move(ui.key, it) },
                    onRemove = { vm.remove(ui.key) },
                )
            }
            item {
                OutlinedButton(onClick = onPickExercises, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ajouter des exercices")
                }
            }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Quitter sans enregistrer ?") },
            text = { Text("Tes modifications seront perdues.") },
            confirmButton = { TextButton(onClick = { confirmLeave = false; onDone() }) { Text("Quitter") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false; save() }) { Text("Enregistrer") } },
        )
    }
}

@Composable
private fun LineEditor(
    line: EditLine,
    isFirst: Boolean,
    isLast: Boolean,
    onChange: ((EditLine) -> EditLine) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseCover(line.exercise, Modifier.size(64.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(line.exercise.name, style = MaterialTheme.typography.titleMedium)
                    val sub = listOf(line.exercise.nameFr, line.exercise.muscle.label).filter { it.isNotBlank() }
                    Text(
                        sub.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column {
                    IconButton(onClick = { onMove(-1) }, enabled = !isFirst, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Monter")
                    }
                    IconButton(onClick = { onMove(1) }, enabled = !isLast, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descendre")
                    }
                }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "Retirer") }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stepper(
                    "Séries", line.sets.toString(),
                    onMinus = { onChange { it.copy(sets = (it.sets - 1).coerceAtLeast(1)) } },
                    onPlus = { onChange { it.copy(sets = (it.sets + 1).coerceAtMost(20)) } },
                )
                Stepper(
                    "Répétitions", line.reps.toString(),
                    onMinus = { onChange { it.copy(reps = (it.reps - 1).coerceAtLeast(1)) } },
                    onPlus = { onChange { it.copy(reps = (it.reps + 1).coerceAtMost(100)) } },
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeightField(
                    value = line.weight,
                    onValue = { w -> onChange { it.copy(weight = w) } },
                    label = "Poids (kg)",
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                FilterChip(
                    selected = line.perHand,
                    onClick = { onChange { it.copy(perHand = !it.perHand) } },
                    label = { Text("par main") },
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stepper(
                    "Repos entre séries", formatRest(line.restSetSec),
                    onMinus = { onChange { it.copy(restSetSec = (it.restSetSec - 15).coerceAtLeast(0)) } },
                    onPlus = { onChange { it.copy(restSetSec = (it.restSetSec + 15).coerceAtMost(600)) } },
                )
                Stepper(
                    "Repos après l'exercice", formatRest(line.restAfterSec),
                    onMinus = { onChange { it.copy(restAfterSec = (it.restAfterSec - 15).coerceAtLeast(0)) } },
                    onPlus = { onChange { it.copy(restAfterSec = (it.restAfterSec + 15).coerceAtMost(900)) } },
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = line.note,
                onValueChange = { v -> onChange { it.copy(note = v) } },
                label = { Text("Note (réglages, consignes…)") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
