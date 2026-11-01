package fr.meowsport.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.meowsport.app
import fr.meowsport.data.Muscle
import fr.meowsport.data.SessionFull
import fr.meowsport.data.formatWeight
import fr.meowsport.ui.components.ExerciseCover

private data class Summary(val sessions: Int, val best: Double)

/** All exercises of the catalog; the ones already done show how many times and the record. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(onOpen: (Long) -> Unit) {
    val repo = LocalContext.current.app.repo
    val all by remember { repo.observeExercises() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val history by remember { repo.observeHistory() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val sessions by produceState(emptyList<SessionFull>(), history) { value = repo.finishedSessionsFull() }
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf<Muscle?>(null) }
    var doneOnly by rememberSaveable { mutableStateOf(false) }

    val summaries = remember(sessions) {
        sessions.flatMap { s -> s.exercises.filter { e -> e.sets.any { it.done } } }
            .groupBy { it.exercise.id }
            .mapValues { (_, list) -> Summary(list.size, list.maxOf { e -> e.sets.filter { it.done }.maxOf { it.weight } }) }
    }
    val shown = remember(all, query, muscle, doneOnly, summaries) {
        all.filter { (muscle == null || it.muscle == muscle) && it.matches(query) && (!doneOnly || it.id in summaries) }
            .sortedWith(compareByDescending<fr.meowsport.data.Exercise> { summaries[it.id]?.sessions ?: 0 }.thenBy { it.name.lowercase() })
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Exercices") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Rechercher un exercice") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FilterChip(selected = doneOnly, onClick = { doneOnly = !doneOnly }, label = { Text("Déjà pratiqués") }) }
                item { FilterChip(selected = muscle == null, onClick = { muscle = null }, label = { Text("Tous") }) }
                items(Muscle.entries) { m ->
                    FilterChip(selected = muscle == m, onClick = { muscle = if (muscle == m) null else m }, label = { Text(m.label) })
                }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (shown.isEmpty()) {
                    item {
                        Text(
                            "Aucun exercice trouvé.",
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(shown, key = { it.id }) { ex ->
                    val sum = summaries[ex.id]
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpen(ex.id) }.padding(horizontal = 16.dp, vertical = 8.dp),
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
                        if (sum != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                if (sum.best > 0) {
                                    Text("${formatWeight(sum.best)} kg", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(
                                    "${sum.sessions} séance${if (sum.sessions > 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(start = 84.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}
