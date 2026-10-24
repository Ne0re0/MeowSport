package fr.meowsport.ui.history

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.meowsport.app
import fr.meowsport.data.formatWeight
import fr.meowsport.ui.components.ExerciseCover
import fr.meowsport.ui.formatDuration
import fr.meowsport.ui.formatLongDate
import fr.meowsport.ui.formatVolume
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(sessionId: Long, onBack: () -> Unit) {
    val repo = LocalContext.current.app.repo
    val full by remember(sessionId) { repo.observeSession(sessionId) }.collectAsStateWithLifecycle(initialValue = null)
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(full?.session?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Supprimer") }
                },
            )
        },
    ) { padding ->
        val data = full ?: return@Scaffold
        val s = data.session
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(formatLongDate(s.startedAt), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${formatDuration((s.endedAt ?: s.startedAt) - s.startedAt)} · ${formatVolume(s.totalVolume)}" +
                            if (s.improved) " · 🏆 progrès" else "",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            if (s.note.isNotBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Text("📝 ${s.note}", Modifier.padding(14.dp).fillMaxWidth())
                    }
                }
            }
            items(data.sorted, key = { it.item.id }) { ex ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.padding(12.dp)) {
                        ExerciseCover(ex.exercise, Modifier.size(56.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ex.exercise.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            ex.sortedSets.forEachIndexed { i, set ->
                                val unit = if (ex.item.perHand) " kg/main" else " kg"
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Série ${i + 1} : ${formatWeight(set.weight)}$unit × ${set.reps}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (set.done) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(if (set.done) "✓" else "–", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (ex.item.note.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("📝 ${ex.item.note}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer cette séance ?") },
            text = { Text("Elle disparaîtra de ton historique et de tes statistiques.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        repo.deleteSession(sessionId)
                        onBack()
                    }
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }
}
