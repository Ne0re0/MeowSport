package fr.meowsport.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import fr.meowsport.data.Exercise
import fr.meowsport.data.Muscle
import kotlin.math.max
import kotlin.math.min

private object PathCache {
    private val cache = HashMap<String, Path>()
    fun get(d: String): Path = cache.getOrPut(d) { PathParser().parsePathString(d).toPath() }
}

/** Cover image of an exercise: a body with the worked muscle highlighted. */
@Composable
fun ExerciseCover(exercise: Exercise, modifier: Modifier = Modifier, zoom: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(scheme.surfaceContainerHighest, scheme.surfaceContainerLow),
                ),
            ),
    ) {
        MuscleFigure(
            primary = exercise.muscle,
            secondary = exercise.secondaryMuscles,
            zoom = zoom,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
fun MuscleFigure(
    primary: Muscle,
    secondary: List<Muscle>,
    modifier: Modifier = Modifier,
    zoom: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val bodyColor = scheme.onSurface.copy(alpha = 0.10f)
    val muscleColor = scheme.onSurface.copy(alpha = 0.20f)
    val highlight = scheme.primary
    val secondaryColor = scheme.primary.copy(alpha = 0.42f)
    val muscles = if (primary.front) BodyGeometry.front else BodyGeometry.back

    Canvas(modifier) {
        val viewport = if (zoom) focusRect(muscles[primary].orEmpty(), size.width / size.height)
        else Rect(0f, 0f, 100f, 200f)
        val s = min(size.width / viewport.width, size.height / viewport.height)
        val dx = size.width / 2 - viewport.center.x * s
        val dy = size.height / 2 - viewport.center.y * s
        clipRect {
            withTransform({
                translate(dx, dy)
                scale(s, s, pivot = Offset.Zero)
            }) {
                drawMirrored(BodyGeometry.body, bodyColor)
                for ((m, paths) in muscles) {
                    val c = when {
                        m == primary -> highlight
                        m in secondary -> secondaryColor
                        else -> muscleColor
                    }
                    drawMirrored(paths, c)
                }
            }
        }
    }
}

private fun DrawScope.drawMirrored(paths: List<String>, color: Color) {
    for (d in paths) drawPath(PathCache.get(d), color)
    withTransform({ scale(-1f, 1f, pivot = Offset(50f, 0f)) }) {
        for (d in paths) drawPath(PathCache.get(d), color)
    }
}

/** Part of the figure to show so the muscle is large but still in context. */
private fun focusRect(paths: List<String>, ratio: Float): Rect {
    if (paths.isEmpty()) return Rect(0f, 0f, 100f, 200f)
    var b = paths.map { PathCache.get(it).getBounds() }.reduce { a, c -> a.union(c) }
    b = Rect(min(b.left, 100 - b.right), b.top, max(b.right, 100 - b.left), b.bottom)
    var h = max(b.height * 2.6f, 70f)
    var w = max(b.width * 1.5f, h * ratio)
    h = w / ratio
    if (h > 200f) { h = 200f; w = h * ratio }
    val cy = b.center.y.coerceIn(h / 2, 200f - h / 2)
    return Rect(50f - w / 2, cy - h / 2, 50f + w / 2, cy + h / 2)
}

private fun Rect.union(o: Rect) = Rect(min(left, o.left), min(top, o.top), max(right, o.right), max(bottom, o.bottom))
