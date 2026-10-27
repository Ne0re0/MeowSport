package fr.meowsport.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.meowsport.data.Level
import fr.meowsport.data.Muscle
import fr.meowsport.data.MuscleLevel
import fr.meowsport.ui.components.BodyMap

/** Light blue (beginner) to black (expert). */
private val levelStops = listOf(
    Color(0xFF8FD3FF),
    Color(0xFF2F80ED),
    Color(0xFF1C3F9E),
    Color(0xFF0E1B44),
    Color(0xFF000000),
)

fun levelColor(score: Float): Color {
    val s = score.coerceIn(0f, 4f)
    val i = s.toInt().coerceAtMost(3)
    return lerp(levelStops[i], levelStops[i + 1], s - i)
}

@Composable
fun MuscleLevelsCard(
    hasBodyWeight: Boolean,
    levels: Map<Muscle, MuscleLevel>,
    onSetBodyWeight: () -> Unit,
) {
    var showDetails by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Niveau par muscle", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (!hasBodyWeight) {
                Text(
                    "Ton niveau se calcule à partir de tes charges rapportées à ton poids de corps. " +
                        "Renseigne ton poids pour voir ta carte.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onSetBodyWeight) { Text("Renseigner mon poids") }
                return@Column
            }

            val fills = levels.mapValues { levelColor(it.value.score) }
            Row(Modifier.fillMaxWidth()) {
                listOf(true to "Face", false to "Dos").forEach { (front, label) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        BodyMap(front = front, fills = fills, modifier = Modifier.fillMaxWidth().height(280.dp))
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(levelStops)),
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Level.entries.forEach {
                    Text(it.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), RoundedCornerShape(3.dp)),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Transparent : pas de données exploitables",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (levels.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Termine une séance pour voir apparaître tes niveaux.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            TextButton(onClick = { showDetails = !showDetails }) {
                Text(if (showDetails) "Masquer le détail" else "Voir le détail par muscle")
            }
            if (showDetails) {
                levels.values.sortedByDescending { it.score }.forEach { l ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(levelColor(l.score))
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${l.muscle.label} · ${l.level.label}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${l.exerciseName} : ${l.bestSet}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Calcul : meilleure série de chaque exercice, 1RM estimé (formule d'Epley) rapporté à ton poids " +
                        "et comparé à des standards de force publics. Chaque muscle prend le niveau de son meilleur exercice.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
