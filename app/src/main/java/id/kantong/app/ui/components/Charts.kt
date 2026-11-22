package id.kantong.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Ring chart: one arc per segment (value, color), small gaps between them. */
@Composable
fun Donut(segments: List<Pair<Float, Color>>, size: Dp = 140.dp, stroke: Dp = 18.dp, center: @Composable () -> Unit = {}) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx()
            val total = segments.sumOf { it.first.toDouble() }.toFloat().coerceAtLeast(1f)
            var start = -90f
            segments.forEach { (f, c) ->
                val sweep = 360f * f / total
                drawArc(c, start + 1.5f, (sweep - 3f).coerceAtLeast(0f), false, Offset(sw / 2, sw / 2), Size(this.size.width - sw, this.size.height - sw), style = Stroke(sw, cap = StrokeCap.Butt))
                start += sweep
            }
        }
        center()
    }
}
