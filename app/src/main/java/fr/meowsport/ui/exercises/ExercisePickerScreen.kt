package fr.meowsport.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.meowsport.app
import fr.meowsport.data.Equipment
import fr.meowsport.data.Exercise
import fr.meowsport.data.Muscle
import fr.meowsport.ui.components.ExerciseCover
import kotlinx.coroutines.launch
import java.text.Normalizer

private fun String.norm(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

private fun Exercise.matches(q: String): Boolean {
    if (q.isBlank()) return true
    val words = q.norm().split(' ').filter { it.isNotBlank() }
    val hay = "$name $nameFr ${muscle.label} ${equipment.label}".norm()
    return words.all { hay.contains(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    onBack: () -> Unit,
    onPicked: (List<Long>) -> Unit,
    /** Pick one exercise with a single tap, to replace another one. */
    single: Boolean = false,
    initialMuscle: Muscle? = null,
) {
    val repo = LocalContext.current.app.repo
    val all by remember { repo.observeExercises() }.collectAsStateWithLifecycle(initialValue = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf(initialMuscle) }
    val selected = remember { mutableStateListOf<Long>() }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val shown = remember(all, query, muscle) {
        all.filter { (muscle == null || it.muscle == muscle) && it.matches(query) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (single) "Remplacer par…" else "Choisir des exercices") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
                actions = {
                    IconButton(onClick = { creating = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Créer un exercice")
                    }
                },
            )
        },
        bottomBar = {
            if (!single && selected.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = { onPicked(selected.toList()) },
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp),
                    ) {
                        Text("Ajouter ${selected.size} exercice${if (selected.size > 1) "s" else ""}")
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Rechercher (bench, curl, mollets…)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FilterChip(selected = muscle == null, onClick = { muscle = null }, label = { Text("Tous") }) }
                items(Muscle.entries) { m ->
                    FilterChip(
                        selected = muscle == m,
                        onClick = { muscle = if (muscle == m) null else m },
                        label = { Text(m.label) },
                    )
                }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (shown.isEmpty() && all.isNotEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Aucun exercice trouvé.")
                            TextButton(onClick = { creating = true }) { Text("Créer « ${query.ifBlank { "nouvel exercice" }} »") }
                        }
                    }
                }
                items(shown, key = { it.id }) { ex ->
                    val isSelected = ex.id in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (single) onPicked(listOf(ex.id))
                                else if (isSelected) selected.remove(ex.id) else selected.add(ex.id)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ExerciseCover(ex, Modifier.size(56.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (ex.nameFr.isNotBlank()) {
                                Text(ex.nameFr, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(
                                "${ex.muscle.label} · ${ex.equipment.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!single) {
                            Checkbox(checked = isSelected, onCheckedChange = { if (it) selected.add(ex.id) else selected.remove(ex.id) })
                        }
                    }
                    HorizontalDivider(Modifier.padding(start = 84.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }

    if (creating) {
        CreateExerciseDialog(
            initialName = query,
            onDismiss = { creating = false },
            onCreate = { name, m, eq ->
                creating = false
                scope.launch {
                    val id = repo.createExercise(name, m, eq)
                    if (single) {
                        onPicked(listOf(id))
                        return@launch
                    }
                    selected.add(id)
                    query = ""
                    muscle = null
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateExerciseDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onCreate: (String, Muscle, Equipment) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var muscle by remember { mutableStateOf(Muscle.CHEST) }
    var equipment by remember { mutableStateOf(Equipment.MACHINE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel exercice") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("Muscle principal", style = MaterialTheme.typography.labelLarge)
                ChipGrid(Muscle.entries, muscle, { it.label }) { muscle = it }
                Spacer(Modifier.height(12.dp))
                Text("Matériel", style = MaterialTheme.typography.labelLarge)
                ChipGrid(Equipment.entries, equipment, { it.label }) { equipment = it }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name, muscle, equipment) }) { Text("Créer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipGrid(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            FilterChip(selected = item == selected, onClick = { onSelect(item) }, label = { Text(label(item)) })
        }
    }
}
