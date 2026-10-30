package fr.meowsport.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import fr.meowsport.ProfilePhoto
import fr.meowsport.R
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.FilterChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.meowsport.Profile
import fr.meowsport.app
import fr.meowsport.data.Session
import fr.meowsport.data.Muscle
import fr.meowsport.data.MuscleLevel
import fr.meowsport.data.StrengthLevels
import fr.meowsport.ui.components.Stepper
import fr.meowsport.ui.components.WeightField
import fr.meowsport.ui.formatDate
import fr.meowsport.ui.formatDuration
import fr.meowsport.ui.formatVolume
import fr.meowsport.ui.templates.ActiveSessionBanner
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private data class WeekStat(val start: LocalDate, val count: Int, val minutes: Long)

private fun Session.durationMs() = (endedAt ?: startedAt) - startedAt

private fun weekStart(ms: Long, zone: ZoneId, monday: Boolean): LocalDate =
    Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        .with(TemporalAdjusters.previousOrSame(if (monday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onOpenSession: (Long) -> Unit, onResumeSession: (Long) -> Unit, onOpenSettings: () -> Unit) {
    val app = LocalContext.current.app
    val history by remember { app.repo.observeHistory() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val active by remember { app.repo.observeActiveSession() }.collectAsStateWithLifecycle(initialValue = null)
    val profile by app.profile.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }

    val zone = ZoneId.systemDefault()
    val settings by app.settings.state.collectAsStateWithLifecycle()
    val monday = settings.weekStartsMonday
    val thisWeek = weekStart(System.currentTimeMillis(), zone, monday)
    val weeks = remember(history, monday) {
        val byWeek = history.groupBy { weekStart(it.startedAt, zone, monday) }
        (7 downTo 0).map { i ->
            val start = thisWeek.minusWeeks(i.toLong())
            val list = byWeek[start].orEmpty()
            WeekStat(start, list.size, list.sumOf { it.durationMs() } / 60_000)
        }
    }
    val current = weeks.last()
    val totalMs = history.sumOf { it.durationMs() }

    val levels by produceState(emptyMap<Muscle, MuscleLevel>(), history, profile.bodyWeight, profile.female) {
        val bw = profile.bodyWeight
        value = if (bw == null) emptyMap() else withContext(Dispatchers.Default) {
            StrengthLevels.compute(app.repo.finishedSessionsFull(), bw, profile.female)
        }
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val photo by produceState<ImageBitmap?>(null, profile.photoVersion) {
        value = if (profile.photoVersion == 0L) null else withContext(Dispatchers.IO) {
            ProfilePhoto.load(context)?.asImageBitmap()
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = runCatching { withContext(Dispatchers.IO) { ProfilePhoto.save(context, uri) } }.isSuccess
                if (ok) {
                    app.profile.update(app.profile.state.value.copy(photoVersion = System.currentTimeMillis()))
                } else {
                    snackbar.showSnackbar("Impossible d'utiliser cette image.")
                }
            }
        }
    }
    fun pickPhoto() = photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    fun removePhoto() {
        ProfilePhoto.delete(context)
        app.profile.update(app.profile.state.value.copy(photoVersion = 0L))
    }
    var menu by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val msg = runCatching {
                    val json = app.backup.export()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
                    }
                }.fold(
                    onSuccess = { "Export terminé ✓" },
                    onFailure = { "L'export a échoué : ${it.message}" },
                )
                snackbar.showSnackbar(msg)
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val msg = runCatching {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                    }
                    app.backup.import(json)
                }.fold(
                    onSuccess = { r ->
                        buildString {
                            append("Import terminé : ${r.templates} modèle${if (r.templates > 1) "s" else ""}")
                            append(" et ${r.sessions} séance${if (r.sessions > 1) "s" else ""} ajouté${if (r.templates + r.sessions > 1) "s" else ""}")
                            if (r.skipped > 0) append(" (${r.skipped} déjà présent${if (r.skipped > 1) "s" else ""})")
                        }
                    },
                    onFailure = { it.message ?: "L'import a échoué." },
                )
                snackbar.showSnackbar(msg, withDismissAction = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Réglages") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Options") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Exporter mes données") },
                                leadingIcon = { Icon(Icons.Default.Upload, null) },
                                onClick = {
                                    menu = false
                                    val day = java.time.LocalDate.now().toString()
                                    exportLauncher.launch("meowsport-$day.json")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Importer un fichier") },
                                leadingIcon = { Icon(Icons.Default.Download, null) },
                                onClick = {
                                    menu = false
                                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                                },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Header(
                    profile = profile,
                    photo = photo,
                    onEdit = { editing = true },
                    onPickPhoto = ::pickPhoto,
                    onRemovePhoto = ::removePhoto,
                )
            }
            active?.let { s -> item { ActiveSessionBanner(s, onClick = { onResumeSession(s.id) }) } }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Cette semaine", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Stat("${current.count} / ${profile.weeklyGoal}", "séances", Modifier.weight(1f))
                            Stat(formatDuration(current.minutes * 60_000), "à la salle", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (current.count.toFloat() / profile.weeklyGoal.coerceAtLeast(1)).coerceAtMost(1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        )
                        if (current.count >= profile.weeklyGoal && profile.weeklyGoal > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text("🎯 Objectif de la semaine atteint !", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Temps à la salle, 8 dernières semaines", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        WeeksChart(weeks)
                    }
                }
            }
            item {
                MuscleLevelsCard(
                    hasBodyWeight = profile.bodyWeight != null,
                    levels = levels,
                    onSetBodyWeight = { editing = true },
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallStat("${history.size}", "séances au total", Modifier.weight(1f))
                    SmallStat(formatDuration(totalMs), "au total", Modifier.weight(1f))
                    SmallStat(
                        if (history.isEmpty()) "–" else formatDuration(totalMs / history.size),
                        "par séance",
                        Modifier.weight(1f),
                    )
                }
            }
            item { Text("Historique", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            if (history.isEmpty()) {
                item {
                    Text(
                        "Tes séances terminées apparaîtront ici.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(history, key = { it.id }) { s -> HistoryRow(s, onClick = { onOpenSession(s.id) }) }
        }
    }

    if (editing) {
        EditProfileDialog(profile, onDismiss = { editing = false }, onSave = { app.profile.update(it); editing = false })
    }
}

@Composable
private fun Header(
    profile: Profile,
    photo: ImageBitmap?,
    onEdit: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    var photoMenu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { if (photo == null) onPickPhoto() else photoMenu = true },
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null) {
                    Image(photo, contentDescription = "Photo de profil", contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                } else {
                    Image(
                        painterResource(R.drawable.logo_cat),
                        contentDescription = "Ajouter une photo de profil",
                        modifier = Modifier.padding(8.dp).matchParentSize(),
                    )
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PhotoCamera, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
            DropdownMenu(expanded = photoMenu, onDismissRequest = { photoMenu = false }) {
                DropdownMenuItem(text = { Text("Changer la photo") }, onClick = { photoMenu = false; onPickPhoto() })
                DropdownMenuItem(text = { Text("Retirer la photo") }, onClick = { photoMenu = false; onRemovePhoto() })
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(profile.name, style = MaterialTheme.typography.headlineMedium)
            Text(
                "Objectif : ${profile.weeklyGoal} séance${if (profile.weeklyGoal > 1) "s" else ""} par semaine",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Modifier le profil") }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SmallStat(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

private val weekLabel = DateTimeFormatter.ofPattern("dd/MM")

@Composable
private fun WeeksChart(weeks: List<WeekStat>) {
    val max = (weeks.maxOfOrNull { it.minutes } ?: 0L).coerceAtLeast(60L)
    val bar = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Column {
        Row(Modifier.fillMaxWidth()) {
            weeks.forEach { w ->
                Text(
                    if (w.minutes > 0) formatDuration(w.minutes * 60_000).replace(" ", "") else "",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Canvas(Modifier.fillMaxWidth().height(110.dp).padding(vertical = 4.dp)) {
            val slot = size.width / weeks.size
            val bw = slot * 0.55f
            weeks.forEachIndexed { i, w ->
                val x = i * slot + (slot - bw) / 2
                drawRoundRect(track, Offset(x, 0f), Size(bw, size.height), CornerRadius(bw / 3))
                val h = size.height * (w.minutes.toFloat() / max)
                if (h > 0) drawRoundRect(bar, Offset(x, size.height - h), Size(bw, h), CornerRadius(bw / 3))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            weeks.forEachIndexed { i, w ->
                Text(
                    if (i == weeks.lastIndex) "Cette sem." else weekLabel.format(w.start),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(s: Session, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.name, style = MaterialTheme.typography.titleMedium)
                Text(formatDate(s.startedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${formatDuration(s.durationMs())} · ${formatVolume(s.totalVolume)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (s.improved) {
                Icon(Icons.Default.EmojiEvents, contentDescription = "Record battu", tint = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun EditProfileDialog(profile: Profile, onDismiss: () -> Unit, onSave: (Profile) -> Unit) {
    var name by remember { mutableStateOf(profile.name) }
    var goal by remember { mutableIntStateOf(profile.weeklyGoal) }
    var bodyWeight by remember { mutableStateOf(profile.bodyWeight ?: 0.0) }
    var female by remember { mutableStateOf(profile.female) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mon profil") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Prénom") }, singleLine = true)
                Spacer(Modifier.height(16.dp))
                Stepper(
                    "Séances par semaine", "$goal",
                    onMinus = { goal = (goal - 1).coerceAtLeast(1) },
                    onPlus = { goal = (goal + 1).coerceAtMost(14) },
                )
                Spacer(Modifier.height(16.dp))
                WeightField(value = bodyWeight, onValue = { bodyWeight = it }, label = "Poids de corps (kg)")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !female, onClick = { female = false }, label = { Text("Homme") })
                    FilterChip(selected = female, onClick = { female = true }, label = { Text("Femme") })
                }
                Text(
                    "Utilisés pour calculer ton niveau par muscle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(
                    profile.copy(
                        name = name.trim().ifBlank { "Athlète" },
                        weeklyGoal = goal,
                        bodyWeight = bodyWeight.takeIf { it in 25.0..300.0 },
                        female = female,
                    ),
                ) }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
