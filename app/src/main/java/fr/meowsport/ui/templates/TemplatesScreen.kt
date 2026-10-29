package fr.meowsport.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.meowsport.app
import fr.meowsport.data.Session
import fr.meowsport.data.TemplateFull
import fr.meowsport.ui.components.ExerciseCover
import fr.meowsport.ui.formatAgo
import fr.meowsport.ui.formatClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(onEdit: (Long?) -> Unit, onOpenSession: (Long) -> Unit, onExamples: () -> Unit) {
    val repo = LocalContext.current.app.repo
    val templates by remember { repo.observeTemplates() }.collectAsStateWithLifecycle(initialValue = null)
    val active by remember { repo.observeActiveSession() }.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var toDelete by remember { mutableStateOf<TemplateFull?>(null) }

    fun start(templateId: Long?) {
        val running = active
        if (running != null) {
            onOpenSession(running.id)
        } else {
            scope.launch { onOpenSession(repo.startSession(templateId)) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MeowSport", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { start(null) }) { Text("Séance libre") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEdit(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nouveau modèle") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            active?.let { s -> item { ActiveSessionBanner(s, onClick = { onOpenSession(s.id) }) } }
            val list = templates
            when {
                list == null -> Unit
                list.isEmpty() -> {
                    item { EmptyState() }
                    item { ExamplesButton(onExamples) }
                }
                else -> {
                    item {
                        Text(
                            "Mes modèles",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(list, key = { it.template.id }) { t ->
                        TemplateCard(
                            t = t,
                            onStart = { start(t.template.id) },
                            onEdit = { onEdit(t.template.id) },
                            onDelete = { toDelete = t },
                        )
                    }
                    item { ExamplesButton(onExamples) }
                }
            }
        }
    }

    toDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Supprimer « ${t.template.name} » ?") },
            text = { Text("Le modèle sera supprimé. L'historique de tes séances est conservé.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.deleteTemplate(t.template.id) }
                    toDelete = null
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annuler") } },
        )
    }
}

@Composable
fun ActiveSessionBanner(s: Session, onClick: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (s.pausedAt != null) "Séance en pause" else "Séance en cours", style = MaterialTheme.typography.labelLarge)
                Text(s.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(formatClock(s.elapsedMs(now)), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun TemplateCard(t: TemplateFull, onStart: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val lines = t.sorted
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.template.name, style = MaterialTheme.typography.titleLarge)
                    val muscles = lines.map { it.exercise.muscle.label }.distinct().take(4).joinToString(" · ")
                    Text(
                        "${lines.size} exercice${if (lines.size > 1) "s" else ""}" +
                            if (muscles.isNotEmpty()) " · $muscles" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                    t.template.lastPerformedAt?.let {
                        Text(
                            "Dernière fois : ${formatAgo(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Modifier") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menu = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
            if (lines.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lines, key = { it.line.id }) { l ->
                        ExerciseCover(l.exercise, Modifier.size(56.dp))
                    }
                }
            }
            if (t.template.nextNote.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "📝 ${t.template.nextNote}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth(), enabled = lines.isNotEmpty()) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Démarrer la séance")
            }
        }
    }
}

@Composable
private fun ExamplesButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Exemples de modèles (Upper/Lower, PPL…)")
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 64.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🐱💪", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text("Aucun modèle pour l'instant", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Crée ton premier modèle avec « Nouveau modèle », ou pars d'un exemple tout prêt.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
