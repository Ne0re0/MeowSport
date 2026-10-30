package fr.meowsport.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.meowsport.Accent
import fr.meowsport.AppSettings
import fr.meowsport.ThemeMode
import fr.meowsport.app

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.app
    val store = app.settings
    val s by store.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    val dark = MaterialTheme.colorScheme.background.let { (it.red + it.green + it.blue) / 3f < 0.5f }
    fun set(change: (AppSettings) -> AppSettings) = store.update(change)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Section("Apparence") {
                    Label("Thème")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { m ->
                            FilterChip(
                                selected = s.theme == m,
                                onClick = { set { it.copy(theme = m) } },
                                label = { Text(m.label) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Label("Couleur principale")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Accent.entries.forEach { a ->
                            val c = Color(if (dark) a.dark else a.light)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .background(c, CircleShape)
                                        .border(
                                            if (s.accent == a) 3.dp else 0.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape,
                                        )
                                        .clickable { set { it.copy(accent = a) } },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (s.accent == a) Icon(Icons.Default.Check, null, tint = Color.White)
                                }
                                Text(a.label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            item {
                Section("Nouveaux exercices") {
                    Stepper("Séries par défaut", "${s.defaultSets}", { set { it.copy(defaultSets = (it.defaultSets - 1).coerceAtLeast(1)) } }) {
                        set { it.copy(defaultSets = (it.defaultSets + 1).coerceAtMost(20)) }
                    }
                    Stepper("Répétitions par défaut", "${s.defaultReps}", { set { it.copy(defaultReps = (it.defaultReps - 1).coerceAtLeast(1)) } }) {
                        set { it.copy(defaultReps = (it.defaultReps + 1).coerceAtMost(100)) }
                    }
                    Stepper("Repos entre séries", sec(s.defaultRestSetSec), { set { it.copy(defaultRestSetSec = (it.defaultRestSetSec - 15).coerceAtLeast(0)) } }) {
                        set { it.copy(defaultRestSetSec = (it.defaultRestSetSec + 15).coerceAtMost(900)) }
                    }
                    Stepper("Repos après l'exercice", sec(s.defaultRestAfterSec), { set { it.copy(defaultRestAfterSec = (it.defaultRestAfterSec - 15).coerceAtLeast(0)) } }) {
                        set { it.copy(defaultRestAfterSec = (it.defaultRestAfterSec + 15).coerceAtMost(900)) }
                    }
                }
            }
            item {
                Section("Pendant la séance") {
                    Toggle("Garder l'écran allumé", null, s.keepScreenOn) { v -> set { it.copy(keepScreenOn = v) } }
                    Toggle("Notification de séance", "Chrono, pause et fin depuis la notification", s.sessionNotification) { v -> set { it.copy(sessionNotification = v) } }
                    Toggle("Lancer le repos automatiquement", "Quand tu coches une série", s.autoStartRest) { v -> set { it.copy(autoStartRest = v) } }
                }
            }
            item {
                Section("Fin du repos") {
                    Toggle("Bips de compte à rebours", "À 2 s, 1 s et 0 s (le dernier plus fort)", s.countdownBeeps) { v -> set { it.copy(countdownBeeps = v) } }
                    Label("Volume des bips : ${s.beepVolume} %")
                    Slider(
                        value = s.beepVolume.toFloat(),
                        onValueChange = { v -> set { it.copy(beepVolume = v.toInt()) } },
                        valueRange = 10f..100f,
                        enabled = s.countdownBeeps,
                    )
                    OutlinedButton(onClick = { app.restAlarm.test(scope) }) {
                        Icon(Icons.Default.VolumeUp, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tester le son")
                    }
                    Toggle("Baisser le son des autres applis", "Pendant les bips", s.duckOtherApps) { v -> set { it.copy(duckOtherApps = v) } }
                    Toggle("Vibrer à la fin du repos", null, s.vibrateAtEnd) { v -> set { it.copy(vibrateAtEnd = v) } }
                }
            }
            item {
                Section("Fin de séance") {
                    Toggle("Mettre à jour le modèle", "Reprend les poids et répétitions de la séance", s.updateTemplate) { v -> set { it.copy(updateTemplate = v) } }
                    Toggle("Afficher les félicitations", "Quand tu as progressé", s.showCongrats) { v -> set { it.copy(showCongrats = v) } }
                }
            }
            item {
                Section("Statistiques") {
                    Toggle("La semaine commence le lundi", "Sinon le dimanche", s.weekStartsMonday) { v -> set { it.copy(weekStartsMonday = v) } }
                }
            }
            item {
                TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Réinitialiser les réglages", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Réinitialiser ?") },
            text = { Text("Tous les réglages reviennent à leur valeur par défaut. Tes séances et modèles ne sont pas touchés.") },
            confirmButton = {
                TextButton(onClick = { store.reset(); confirmReset = false }) { Text("Réinitialiser") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Annuler") } },
        )
    }
}

private fun sec(total: Int): String =
    if (total < 60) "$total s" else "${total / 60} min" + (if (total % 60 != 0) " ${"%02d".format(total % 60)}" else "")

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun Toggle(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Stepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onMinus) { Icon(Icons.Default.Remove, contentDescription = "Moins") }
        Text(value, fontWeight = FontWeight.Bold, modifier = Modifier.width(72.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = onPlus) { Icon(Icons.Default.Add, contentDescription = "Plus") }
    }
}
