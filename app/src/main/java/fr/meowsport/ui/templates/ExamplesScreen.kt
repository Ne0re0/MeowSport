package fr.meowsport.ui.templates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.meowsport.app
import fr.meowsport.data.ExampleProgram
import fr.meowsport.data.ExampleTemplate
import fr.meowsport.data.Exercise
import fr.meowsport.data.ProgramCatalog
import fr.meowsport.ui.components.ExerciseCover
import fr.meowsport.ui.formatRest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamplesScreen(onBack: () -> Unit) {
    val repo = LocalContext.current.app.repo
    val all by remember { repo.observeExercises() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val byKey = remember(all) { all.filter { it.key != null }.associateBy { it.key!! } }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Templates added during this visit, so their button shows a check.
    val added = remember { mutableStateListOf<String>() }

    fun add(days: List<ExampleTemplate>) {
        scope.launch {
            days.forEach { repo.addExampleTemplate(it) }
            added += days.map { it.name }
            val msg = if (days.size == 1) "« ${days[0].name} » ajouté à tes modèles"
            else "${days.size} modèles ajoutés à tes séances"
            snackbar.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exemples de modèles") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    "Ajoute un modèle à tes séances, puis règle tes charges dedans ou pendant ta première séance. " +
                        "Tu peux tout modifier ensuite.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(ProgramCatalog.all, key = { it.name }) { p ->
                ProgramCard(p, byKey, added, onAdd = ::add)
            }
        }
    }
}

@Composable
private fun ProgramCard(
    p: ExampleProgram,
    byKey: Map<String, Exercise>,
    added: List<String>,
    onAdd: (List<ExampleTemplate>) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(p.name, style = MaterialTheme.typography.titleLarge)
            Text(p.frequency, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(p.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            p.days.forEach { day ->
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                DayRow(day, byKey, isAdded = day.name in added, onAdd = { onAdd(listOf(day)) })
            }
            if (p.days.size > 1) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onAdd(p.days) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ajouter tout le programme (${p.days.size} modèles)")
                }
            }
        }
    }
}

@Composable
private fun DayRow(day: ExampleTemplate, byKey: Map<String, Exercise>, isAdded: Boolean, onAdd: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(day.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${day.exercises.size} exercices · ${day.exercises.sumOf { it.sets }} séries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = onAdd) {
                Icon(if (isAdded) Icons.Default.Check else Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (isAdded) "Ajouté" else "Ajouter")
            }
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(start = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                day.exercises.forEach { e ->
                    val ex = byKey[e.key]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (ex != null) ExerciseCover(ex, Modifier.size(40.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ex?.name ?: e.key, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${e.sets} × ${e.reps} · repos ${formatRest(e.restSetSec)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
