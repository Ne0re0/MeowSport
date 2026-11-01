package fr.meowsport.ui.exercises

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.meowsport.app
import fr.meowsport.data.Exercise
import fr.meowsport.data.SessionFull
import fr.meowsport.data.formatWeight
import fr.meowsport.ui.components.ExerciseCover
import fr.meowsport.ui.formatDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

enum class Metric(val label: String, val unit: String) {
    MAX("Charge max", "kg"),
    E1RM("1RM estimé", "kg"),
    VOLUME("Volume", "kg"),
    REPS("Reps max", "reps");

    fun of(p: ExercisePoint): Double = when (this) {
        MAX -> p.maxWeight
        E1RM -> p.e1rm
        VOLUME -> p.volume
        REPS -> p.maxReps.toDouble()
    }
}

private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE)
private fun short(ms: Long) = shortDate.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(exerciseId: Long, onBack: () -> Unit, onOpenSession: (Long) -> Unit) {
    val repo = LocalContext.current.app.repo
    val history by remember { repo.observeHistory() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val exercise by produceState<Exercise?>(null, exerciseId) { value = repo.exercise(exerciseId) }
    val sessions by produceState<List<SessionFull>?>(null, history) { value = repo.finishedSessionsFull() }
    val points = remember(sessions, exerciseId) { historyOf(sessions.orEmpty(), exerciseId) }
    val weighted = points.any { it.maxWeight > 0 }
    var metric by rememberSaveable(weighted) { mutableStateOf(if (weighted) Metric.MAX else Metric.REPS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                },
            )
        },
    ) { padding ->
        val ex = exercise ?: return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExerciseCover(ex, Modifier.size(width = 80.dp, height = 92.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        if (ex.nameFr.isNotBlank()) Text(ex.nameFr, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${ex.muscle.label} · ${ex.equipment.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (ex.note.isNotBlank()) Text("📌 ${ex.note}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (sessions != null && points.isEmpty()) {
                item {
                    Text(
                        "Tu n'as pas encore fait cet exercice. Ta courbe apparaîtra après ta première séance terminée.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (points.isNotEmpty()) {
                item {
                    val best = points.maxBy { it.maxWeight }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (weighted) {
                            Stat("${formatWeight(best.maxWeight)} kg", "record", Modifier.weight(1f))
                            Stat("${formatWeight(points.maxOf { it.e1rm })} kg", "1RM estimé", Modifier.weight(1f))
                        } else {
                            Stat("${points.maxOf { it.maxReps }}", "reps max", Modifier.weight(1f))
                        }
                        Stat("${points.size}", "séance${if (points.size > 1) "s" else ""}", Modifier.weight(1f))
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(if (weighted) Metric.entries else listOf(Metric.REPS)) { m ->
                            FilterChip(selected = metric == m, onClick = { metric = m }, label = { Text(m.label) })
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        EvolutionChart(points, metric, Modifier.fillMaxWidth().padding(12.dp))
                    }
                    if (points.firstOrNull()?.perHand == true && metric != Metric.VOLUME && metric != Metric.REPS) {
                        Text(
                            "Charges par main.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                item { Text("Historique", style = MaterialTheme.typography.titleMedium) }
                items(points.reversed()) { p ->
                    Card(
                        Modifier.fillMaxWidth().clickable { onOpenSession(p.sessionId) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(formatDate(p.date), style = MaterialTheme.typography.titleSmall)
                            Text(
                                p.sets.joinToString(" · ") { s ->
                                    if (s.weight > 0) "${s.reps} × ${formatWeight(s.weight)} kg" else "${s.reps} reps"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Line chart of one metric over time. Tap a point to read its value. */
@Composable
private fun EvolutionChart(points: List<ExercisePoint>, metric: Metric, modifier: Modifier = Modifier) {
    var selected by remember(points, metric) { mutableStateOf(points.lastIndex) }
    val values = points.map { metric.of(it) }
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    fun fmt(v: Double) = if (metric == Metric.REPS) v.toInt().toString() else formatWeight(Math.round(v * 10) / 10.0)

    Column(modifier) {
        val p = points.getOrNull(selected)
        if (p != null) {
            Text(
                "${formatDate(p.date)} : ${fmt(values[selected])} ${metric.unit}",
                style = MaterialTheme.typography.titleSmall,
                color = line,
            )
        }
        Spacer(Modifier.height(8.dp))
        val minT = points.first().date
        val maxT = points.last().date
        var lo = values.min()
        var hi = values.max()
        if (hi - lo < 1e-6) { lo -= 1; hi += 1 }
        val pad = (hi - lo) * 0.1
        lo = (lo - pad).coerceAtLeast(0.0)
        hi += pad
        Canvas(
            Modifier.fillMaxWidth().height(200.dp).pointerInput(points, metric) {
                detectTapGestures { tap ->
                    val left = 40.dp.toPx()
                    val w = size.width - left - 8.dp.toPx()
                    fun x(t: Long) = if (maxT == minT) left + w / 2 else left + w * ((t - minT).toFloat() / (maxT - minT))
                    selected = points.indices.minBy { abs(x(points[it].date) - tap.x) }
                }
            },
        ) {
            val left = 40.dp.toPx()
            val bottom = size.height - 18.dp.toPx()
            val top = 6.dp.toPx()
            val w = size.width - left - 8.dp.toPx()
            fun x(t: Long) = if (maxT == minT) left + w / 2 else left + w * ((t - minT).toFloat() / (maxT - minT))
            fun y(v: Double) = (bottom - (bottom - top) * ((v - lo) / (hi - lo))).toFloat()

            // Horizontal grid with values.
            for (i in 0..3) {
                val v = lo + (hi - lo) * i / 3
                val yy = y(v)
                drawLine(grid, Offset(left, yy), Offset(size.width, yy), strokeWidth = 1f)
                val text = measurer.measure(fmt(v), labelStyle)
                drawText(text, topLeft = Offset(left - text.size.width - 4.dp.toPx(), yy - text.size.height / 2))
            }
            // Dates at both ends.
            val first = measurer.measure(short(minT), labelStyle)
            drawText(first, topLeft = Offset(left, bottom + 4.dp.toPx()))
            if (maxT != minT) {
                val last = measurer.measure(short(maxT), labelStyle)
                drawText(last, topLeft = Offset(size.width - last.size.width, bottom + 4.dp.toPx()))
            }

            val path = Path()
            points.forEachIndexed { i, pt ->
                val o = Offset(x(pt.date), y(values[i]))
                if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            }
            drawPath(path, line, style = Stroke(width = 3.dp.toPx()))
            points.forEachIndexed { i, pt ->
                val o = Offset(x(pt.date), y(values[i]))
                drawCircle(line, radius = if (i == selected) 6.dp.toPx() else 3.5.dp.toPx(), center = o)
                if (i == selected) drawCircle(line.copy(alpha = 0.25f), radius = 11.dp.toPx(), center = o)
            }
        }
    }
}
