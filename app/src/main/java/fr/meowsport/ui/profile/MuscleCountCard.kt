package fr.meowsport.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.meowsport.data.Muscle
import fr.meowsport.data.SessionFull
import fr.meowsport.ui.components.BodyMap
import java.time.LocalDate
import java.time.ZoneId

enum class CountPeriod(val label: String) { ALL("Depuis le début"), MONTH("Ce mois-ci"), WEEK("Cette semaine") }

/** Number of exercises done per muscle group (main muscle, at least one set checked). */
fun countByMuscle(sessions: List<SessionFull>, period: CountPeriod, weekStartsMonday: Boolean): Map<Muscle, Int> {
    val zone = ZoneId.systemDefault()
    val now = System.currentTimeMillis()
    val from: Long = when (period) {
        CountPeriod.ALL -> Long.MIN_VALUE
        CountPeriod.MONTH -> LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        CountPeriod.WEEK -> weekStart(now, zone, weekStartsMonday).atStartOfDay(zone).toInstant().toEpochMilli()
    }
    return sessions
        .filter { it.session.startedAt >= from }
        .flatMap { s -> s.exercises.filter { e -> e.sets.any { it.done } }.map { it.exercise.muscle } }
        .groupingBy { it }
        .eachCount()
}

private val low = Color(0xFFE4D8FF)
private val high = Color(0xFF4A1FA8)

private fun countColor(count: Int, max: Int): Color =
    lerp(low, high, if (max <= 1) 1f else (count - 1f) / (max - 1f))

@Composable
fun MuscleCountCard(sessions: List<SessionFull>, weekStartsMonday: Boolean) {
    var period by rememberSaveable { mutableStateOf(CountPeriod.ALL) }
    var showList by remember { mutableStateOf(false) }
    val counts = remember(sessions, period, weekStartsMonday) { countByMuscle(sessions, period, weekStartsMonday) }
    val max = counts.values.maxOrNull() ?: 0

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Exercices par muscle", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CountPeriod.entries.forEach { p ->
                    FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            val fills = counts.mapValues { countColor(it.value, max) }
            Row(Modifier.fillMaxWidth()) {
                listOf(true to "Face", false to "Dos").forEach { (front, label) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        BodyMap(front = front, fills = fills, modifier = Modifier.fillMaxWidth().height(280.dp))
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (max == 0) {
                Text(
                    "Aucun exercice sur cette période.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(low, high))),
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1", style = MaterialTheme.typography.labelSmall)
                Text("$max exercice${if (max > 1) "s" else ""}", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { showList = !showList }) {
                Text(if (showList) "Masquer les chiffres" else "Voir les chiffres par muscle")
            }
            if (showList) {
                Muscle.entries.sortedByDescending { counts[it] ?: 0 }.forEach { m ->
                    val c = counts[m] ?: 0
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(14.dp).clip(RoundedCornerShape(4.dp))
                                .background(if (c > 0) countColor(c, max) else Color.Transparent),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(m.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("$c", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
