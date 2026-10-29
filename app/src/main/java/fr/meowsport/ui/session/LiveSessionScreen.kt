package fr.meowsport.ui.session

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.core.content.ContextCompat
import fr.meowsport.data.Session
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.meowsport.app
import fr.meowsport.data.FinishResult
import fr.meowsport.data.SessionExerciseFull
import fr.meowsport.data.SessionSet
import fr.meowsport.data.formatWeight
import fr.meowsport.ui.components.ExerciseCover
import fr.meowsport.ui.components.IntField
import fr.meowsport.ui.components.WeightField
import fr.meowsport.ui.formatClock
import fr.meowsport.ui.formatDuration
import fr.meowsport.ui.formatRest
import fr.meowsport.ui.formatVolume
import fr.meowsport.ui.theme.ClockStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSessionScreen(
    sessionId: Long,
    picked: LongArray?,
    onPickedConsumed: () -> Unit,
    onPickExercises: () -> Unit,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val repo = LocalContext.current.app.repo
    val vm: LiveSessionViewModel = viewModel(key = "session-$sessionId") { LiveSessionViewModel(repo, sessionId) }
    val full by vm.session.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    // Keep the screen on while training.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Ask once for the notification that shows the clock during the session.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Session finished from the notification: go back to the home screen.
    val endedAt = full?.session?.endedAt
    LaunchedEffect(endedAt) {
        if (endedAt != null && vm.result == null && !vm.finishing) onFinished()
    }

    LaunchedEffect(picked) {
        if (picked != null) {
            vm.addExercises(picked.toList())
            onPickedConsumed()
        }
    }

    val data = full
    val session = data?.session
    val exercises = data?.sorted.orEmpty()
    val totalSets = exercises.sumOf { it.sets.size }
    val doneSets = exercises.sumOf { ex -> ex.sets.count { it.done } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(session?.name ?: "Séance", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "$doneSets / $totalSets séries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Options") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Ajouter un exercice") },
                                leadingIcon = { Icon(Icons.Default.Add, null) },
                                onClick = { menu = false; onPickExercises() },
                            )
                            DropdownMenuItem(
                                text = { Text("Abandonner la séance") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = { menu = false; confirmDiscard = true },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (data == null || session == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            SessionClock(vm, session, doneSets, totalSets)
            RestArea(vm, session.restEndsAt, session.restTotalSec)

            LazyColumn(
                modifier = Modifier.weight(1f).imePadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (session.previousNote.isNotBlank()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(Modifier.padding(14.dp).fillMaxWidth()) {
                                Text("📝 Ta note de la dernière fois", style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.height(4.dp))
                                Text(session.previousNote, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (exercises.isEmpty()) {
                    item {
                        OutlinedButton(onClick = onPickExercises, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ajouter un exercice")
                        }
                    }
                }
                items(exercises, key = { it.item.id }) { ex ->
                    ExerciseBlock(
                        ex = ex,
                        onToggle = { vm.toggleSet(it.id) },
                        onValues = { set, w, r -> vm.updateSet(set.id, w, r) },
                        onAddSet = { vm.addSet(ex.item.id) },
                        onRemoveSet = { vm.removeLastSet(ex.item.id) },
                        onRemoveExercise = { vm.removeExercise(ex.item.id) },
                        onNote = { vm.exerciseNote(ex.item.id, it) },
                    )
                }
                item {
                    var note by rememberSaveable(session.id) { mutableStateOf(session.note) }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it; vm.sessionNote(it) },
                        label = { Text("Note pour la prochaine fois") },
                        placeholder = { Text("Ex. monter à 28 kg au développé, épaule un peu sensible…") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Button(
                        onClick = { confirmFinish = true },
                        enabled = !vm.finishing,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Terminer la séance", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (confirmFinish) {
        val left = totalSets - doneSets
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Terminer la séance ?") },
            text = {
                Text(
                    if (left > 0) "Il reste $left série${if (left > 1) "s" else ""} non cochée${if (left > 1) "s" else ""}. " +
                        "Seules les séries cochées comptent et mettent à jour ton modèle." +
                        "\n\n« Terminer sans sauvegarder » supprime la séance et ne change pas ton modèle."
                    else "Ton modèle sera mis à jour avec ce que tu viens de faire." +
                        "\n\n« Terminer sans sauvegarder » supprime la séance et ne change pas ton modèle.",
                )
            },
            confirmButton = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { confirmFinish = false; vm.finish() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Sauvegarder et terminer") }
                    OutlinedButton(
                        onClick = { confirmFinish = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Continuer la séance") }
                    TextButton(
                        onClick = { confirmFinish = false; vm.discard(onFinished) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Terminer sans sauvegarder") }
                }
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Abandonner la séance ?") },
            text = { Text("Elle sera supprimée et ton modèle ne changera pas.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; vm.discard(onFinished) }) { Text("Abandonner") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Annuler") } },
        )
    }

    vm.result?.let { r -> FinishDialog(r, onClose = onFinished) }
}

/** Reads the ticking clock here only, so the rest of the screen does not redraw every tick. */
@Composable
private fun SessionClock(vm: LiveSessionViewModel, session: Session, doneSets: Int, totalSets: Int) {
    val now by vm.now.collectAsStateWithLifecycle()
    val paused = session.pausedAt != null
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatClock(session.elapsedMs(now)),
                style = ClockStyle,
                color = if (paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(onClick = { vm.togglePause() }) {
                Icon(
                    if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (paused) "Reprendre le chrono" else "Mettre le chrono en pause",
                )
            }
        }
        if (paused) {
            Text("En pause", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        if (totalSets > 0) {
            LinearProgressIndicator(
                progress = { doneSets.toFloat() / totalSets },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 6.dp).clip(CircleShape),
            )
        }
    }
}

@Composable
private fun RestArea(vm: LiveSessionViewModel, restEnd: Long?, totalSec: Int) {
    val now by vm.now.collectAsStateWithLifecycle()
    AnimatedVisibility(visible = restEnd != null && restEnd > now) {
        RestCard(
            remainingMs = ((restEnd ?: now) - now).coerceAtLeast(0),
            totalSec = totalSec,
            onMinus = { vm.adjustRest(-15) },
            onPlus = { vm.adjustRest(15) },
            onSkip = { vm.skipRest() },
        )
    }
}

@Composable
private fun RestCard(remainingMs: Long, totalSec: Int, onMinus: () -> Unit, onPlus: () -> Unit, onSkip: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Récupération", style = MaterialTheme.typography.labelLarge)
            Text(
                formatClock(remainingMs + 999),
                style = ClockStyle,
            )
            LinearProgressIndicator(
                progress = { if (totalSec > 0) (remainingMs / (totalSec * 1000f)).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().clip(CircleShape),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onMinus) { Text("−15 s") }
                FilledTonalButton(onClick = onPlus) { Text("+15 s") }
                Button(onClick = onSkip) {
                    Icon(Icons.Default.SkipNext, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Passer")
                }
            }
        }
    }
}

@Composable
private fun ExerciseBlock(
    ex: SessionExerciseFull,
    onToggle: (SessionSet) -> Unit,
    onValues: (SessionSet, Double, Int) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: () -> Unit,
    onRemoveExercise: () -> Unit,
    onNote: (String) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var showNote by rememberSaveable(ex.item.id) { mutableStateOf(ex.item.note.isNotBlank()) }
    val sets = ex.sortedSets
    val allDone = sets.isNotEmpty() && sets.all { it.done }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (allDone) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseCover(ex.exercise, Modifier.size(width = 72.dp, height = 84.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(ex.exercise.name, style = MaterialTheme.typography.titleMedium)
                    if (ex.exercise.nameFr.isNotBlank()) {
                        Text(ex.exercise.nameFr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    val target = if (ex.item.targetSets > 0) {
                        "Objectif ${ex.item.targetSets} × ${ex.item.targetReps}" +
                            if (ex.item.targetWeight > 0) " · ${formatWeight(ex.item.targetWeight)} kg${if (ex.item.perHand) "/main" else ""}" else ""
                    } else {
                        ex.exercise.muscle.label
                    }
                    Text(target, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Repos ${formatRest(ex.item.restSetSec)} · puis ${formatRest(ex.item.restAfterSec)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Options de l'exercice") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Ajouter une série") },
                            leadingIcon = { Icon(Icons.Default.Add, null) },
                            onClick = { menu = false; onAddSet() },
                        )
                        DropdownMenuItem(
                            text = { Text("Retirer la dernière série") },
                            leadingIcon = { Icon(Icons.Default.Remove, null) },
                            enabled = sets.size > 1,
                            onClick = { menu = false; onRemoveSet() },
                        )
                        DropdownMenuItem(
                            text = { Text("Note sur l'exercice") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menu = false; showNote = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Retirer l'exercice") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menu = false; onRemoveExercise() },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Série", Modifier.width(44.dp), style = MaterialTheme.typography.labelMedium)
                Text(
                    if (ex.item.perHand) "kg / main" else "kg",
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.width(8.dp))
                Text("Reps", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(56.dp))
            }
            sets.forEachIndexed { index, set ->
                SetRow(index + 1, set, onToggle = { onToggle(set) }, onValues = { w, r -> onValues(set, w, r) })
            }
            TextButton(onClick = onAddSet) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Série")
            }
            if (showNote) {
                var note by rememberSaveable(ex.item.id) { mutableStateOf(ex.item.note) }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it; onNote(it) },
                    label = { Text("Note sur l'exercice") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SetRow(number: Int, set: SessionSet, onToggle: () -> Unit, onValues: (Double, Int) -> Unit) {
    val bg by animateColorAsState(
        if (set.done) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent,
        label = "setBg",
    )
    // Local copies so typing feels instant; the database is updated on each change.
    var weight by remember(set.id) { mutableStateOf(set.weight) }
    var reps by remember(set.id) { mutableStateOf(set.reps) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(12.dp))
        WeightField(
            value = weight,
            onValue = { weight = it; onValues(it, reps) },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        IntField(
            value = reps,
            onValue = { reps = it; onValues(weight, it) },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        FilledIconToggleButton(
            checked = set.done,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                checkedContainerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Default.Check, contentDescription = if (set.done) "Série faite" else "Cocher la série")
        }
    }
}

@Composable
private fun FinishDialog(r: FinishResult, onClose: () -> Unit) {
    val improved = r.improvements.isNotEmpty()
    AlertDialog(
        onDismissRequest = onClose,
        icon = { Text(if (improved) "🏆" else "✅", style = MaterialTheme.typography.displaySmall) },
        title = {
            Text(
                if (improved) "Bravo, tu as fait mieux que la dernière fois !" else "Séance terminée",
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "${formatDuration(r.durationMs)} · ${r.doneSets} séries · ${formatVolume(r.volume)}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (improved) {
                    Spacer(Modifier.height(12.dp))
                    r.improvements.forEach {
                        Text("💪 $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onClose) { Text("OK") } },
    )
}
