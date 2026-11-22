package id.kantong.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.ID
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Outfit
import java.util.Calendar
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt

/*
 * Laporan charts (Canvas): thin marks, faint gridlines, text always in ink/muted (never series colors).
 */

private val Grid = Color(0xFFE9E0CC)
private val FutureLine = Color(0xFFDCD2BE)
private val FutureText = Color(0xFFB9B09E)

/** One x-axis group of [GroupedBars]: label plus one value per series. */
class BarGroup(val label: String, val values: List<Long>)

/** One x-axis label of [DailyLine] at slot [index]. */
class XLabel(val index: Int, val text: String, val bold: Boolean = false)

/** Gridline values for a data max: 0, step, 2·step... with a "jt"/"rb" unit picked from the step. */
private class Axis(val top: Float, val ticks: List<Long>, val div: Long, val unit: String) {
    fun tick(v: Long, zeroUnit: Boolean): String = if (v == 0L && !zeroUnit) "0" else num(v) + unit
    fun num(v: Long): String = String.format(ID, "%.1f", v.toDouble() / div).removeSuffix(",0")
}

private fun niceAxis(maxValue: Long): Axis {
    val m = maxValue.coerceAtLeast(1L).toDouble()
    val raw = max(m / 2, 1.0)
    val mag = 10.0.pow(floor(log10(raw)))
    val step = listOf(5.0, 2.5, 2.0, 1.0).map { it * mag }.first { it <= raw }.roundToLong().coerceAtLeast(1L)
    val ticks = generateSequence(0L) { it + step }.takeWhile { it <= m }.toList()
    val (div, unit) = when {
        step >= 1_000_000_000L -> 1_000_000_000L to "M"
        step >= 1_000_000L -> 1_000_000L to "jt"
        step >= 1_000L -> 1_000L to "rb"
        else -> 1L to ""
    }
    return Axis(max(m * 1.1, ticks.last().toDouble()).toFloat(), ticks, div, unit)
}

private fun style(size: Float, weight: FontWeight, color: Color) = TextStyle(fontFamily = Outfit, fontSize = size.sp, fontWeight = weight, color = color)

/** Draws [s] with its anchor point at ([x], [y]); [h]/[v] = 0 start/top, .5 center, 1 end/bottom. */
private fun DrawScope.label(m: TextMeasurer, s: String, x: Float, y: Float, st: TextStyle, h: Float = .5f, v: Float = .5f) {
    val r = m.measure(s, st)
    val left = (x - r.size.width * h).coerceIn(0f, max(0f, size.width - r.size.width))
    drawText(r, topLeft = Offset(left, y - r.size.height * v))
}

/**
 * Grouped bars (one bar per series in each group, 2dp apart, 4dp rounded tops). Value labels only on
 * the [selected] group. Tapping a group calls [onSelect].
 */
@Composable
fun GroupedBars(
    groups: List<BarGroup>, colors: List<Color>, selected: Int, modifier: Modifier = Modifier,
    height: Dp = 150.dp, onSelect: ((Int) -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    val n = groups.size.coerceAtLeast(1)
    val axis = niceAxis(groups.maxOfOrNull { g -> g.values.maxOrNull() ?: 0L } ?: 0L)
    val small = n > 6
    Canvas(
        modifier.fillMaxWidth().height(height).let { mod ->
            if (onSelect == null) mod else mod.pointerInput(n) {
                detectTapGestures { o ->
                    val x0 = 30.dp.toPx()
                    val i = floor((o.x - x0) / ((size.width - x0) / n)).toInt()
                    if (i in 0 until n) onSelect(i)
                }
            }
        },
    ) {
        val x0 = 30.dp.toPx(); val top = 14.dp.toPx(); val base = size.height - 22.dp.toPx()
        val sc = (base - top) / axis.top
        val tickStyle = style(10.5f, FontWeight.Bold, K.Muted)
        axis.ticks.forEach { v ->
            val y = base - v * sc
            drawLine(Grid, Offset(x0, y), Offset(size.width, y), 1.dp.toPx())
            label(measurer, axis.tick(v, true), x0 - 6.dp.toPx(), y, tickStyle, h = 1f)
        }
        val k = colors.size.coerceAtLeast(1)
        val gw = (size.width - x0) / n
        val gap = 2.dp.toPx()
        val bw = min(15.dp.toPx(), (gw - 8.dp.toPx() - gap * (k - 1)) / k).coerceAtLeast(2f)
        val r = 4.dp.toPx()
        groups.forEachIndexed { i, g ->
            val cx = x0 + gw * i + gw / 2
            val left = cx - (bw * k + gap * (k - 1)) / 2
            g.values.take(k).forEachIndexed { j, v ->
                if (v <= 0) return@forEachIndexed
                val x = left + j * (bw + gap)
                val y = base - v * sc
                val rr = min(r, min(base - y, bw / 2))
                val c = CornerRadius(rr)
                drawPath(Path().apply { addRoundRect(RoundRect(Rect(x, y, x + bw, base), c, c, CornerRadius.Zero, CornerRadius.Zero)) }, colors[j])
                if (i == selected) label(measurer, axis.num(v), x + bw / 2, y - 3.dp.toPx(), style(10.5f, FontWeight.ExtraBold, K.Ink), v = 1f)
            }
            val on = i == selected
            label(
                measurer, g.label, cx, size.height - 3.dp.toPx(),
                style(if (small) 10f else 11.5f, if (on) FontWeight.ExtraBold else FontWeight.Bold, if (on) K.Ink else K.Muted), v = 1f,
            )
        }
    }
}

/**
 * Daily area + line over [slots] days ([values] may be shorter: days not reached yet are left empty).
 * Optional dashed [limit] line, the highest day labelled with [peakText], a ring on [today].
 */
@Composable
fun DailyLine(
    values: List<Long>, slots: Int, color: Color, bg: Color, xLabels: List<XLabel>, peakText: (Int, Long) -> String,
    modifier: Modifier = Modifier, height: Dp = 140.dp, limit: Long = 0, limitText: String = "", today: Int = -1,
) {
    val measurer = rememberTextMeasurer()
    val axis = niceAxis(max(values.maxOrNull() ?: 0L, limit))
    Canvas(modifier.fillMaxWidth().height(height)) {
        val x0 = 42.dp.toPx(); val top = 14.dp.toPx(); val base = size.height - 22.dp.toPx()
        val sc = (base - top) / axis.top
        val step = (size.width - x0) / (slots - 1).coerceAtLeast(1)
        val muted = style(10.5f, FontWeight.Bold, K.Muted)
        axis.ticks.forEach { v ->
            val y = base - v * sc
            drawLine(Grid, Offset(x0, y), Offset(size.width, y), 1.dp.toPx())
            label(measurer, axis.tick(v, false), x0 - 6.dp.toPx(), y, muted, h = 1f)
        }
        val pts = values.mapIndexed { i, v -> Offset(x0 + step * i, base - v * sc) }
        if (pts.size >= 2) {
            val area = Path().apply {
                moveTo(pts.first().x, base); pts.forEach { lineTo(it.x, it.y) }; lineTo(pts.last().x, base); close()
            }
            drawPath(area, color.copy(alpha = .14f))
            val line = Path().apply { moveTo(pts.first().x, pts.first().y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
            drawPath(line, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else if (pts.size == 1) {
            drawCircle(color, 3.dp.toPx(), pts[0])
        }
        if (limit > 0) {
            val by = base - limit * sc
            drawLine(
                K.Ink.copy(alpha = .55f), Offset(x0, by), Offset(size.width, by), 1.4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
            if (limitText.isNotEmpty()) label(measurer, limitText, size.width, by - 4.dp.toPx(), style(10.5f, FontWeight.ExtraBold, K.Muted), h = 1f, v = 1f)
        }
        val peak = values.indices.maxByOrNull { values[it] } ?: -1
        if (peak >= 0 && values[peak] > 0) {
            val p = pts[peak]
            drawCircle(color, 4.5.dp.toPx(), p)
            drawCircle(bg, 4.5.dp.toPx(), p, style = Stroke(2.dp.toPx()))
            val st = style(10.5f, FontWeight.ExtraBold, K.Ink)
            val text = peakText(peak, values[peak])
            val w = measurer.measure(text, st).size.width
            val right = p.x + 8.dp.toPx() + w <= size.width
            label(measurer, text, if (right) p.x + 8.dp.toPx() else p.x - 8.dp.toPx(), max(p.y, top), st, h = if (right) 0f else 1f)
        }
        if (today in pts.indices) {
            drawCircle(bg, 5.dp.toPx(), pts[today])
            drawCircle(color, 5.dp.toPx(), pts[today], style = Stroke(2.5.dp.toPx()))
        }
        xLabels.forEach { l ->
            val x = x0 + step * l.index
            label(
                measurer, l.text, x, size.height - 3.dp.toPx(),
                style(11f, if (l.bold) FontWeight.ExtraBold else FontWeight.Bold, if (l.bold) K.Ink else K.Muted),
                h = if (l.index == slots - 1 && slots > 1) 1f else .5f, v = 1f,
            )
        }
    }
}

/**
 * Month heatmap, Monday first. [values] has one entry per day; days at or after [elapsed] are in the
 * future (dashed outline), [today] (0-based, -1 = none) gets an ink outline.
 */
@Composable
fun MonthHeatmap(monthStart: Long, values: List<Long>, elapsed: Int, today: Int, modifier: Modifier = Modifier) {
    val lead = Calendar.getInstance().apply { timeInMillis = monthStart }.let { (it.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7 }
    val peak = values.maxOrNull() ?: 0L
    val cells = List(lead) { -1 } + values.indices.toList()
    Column(modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach {
                Text(it, Modifier.weight(1f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = K.Muted, textAlign = TextAlign.Center)
            }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 7).forEach { c ->
                    val i = week.getOrNull(c) ?: -1
                    if (i < 0) {
                        Spacer(Modifier.weight(1f).height(25.dp))
                    } else {
                        HeatCell(i + 1, heatLevel(values[i], peak), future = i >= elapsed, today = i == today, Modifier.weight(1f))
                    }
                }
            }
        }
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("sedikit", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = K.Muted)
            K.Heat.forEach { Box(Modifier.size(18.dp, 10.dp).clip(RoundedCornerShape(3.dp)).background(it)) }
            Text("banyak", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = K.Muted)
        }
    }
}

/** 0 for no spending, 1..4 by square-root share of the busiest day (so one big day does not flatten the rest). */
private fun heatLevel(v: Long, peak: Long): Int =
    if (v <= 0 || peak <= 0) 0 else (1 + (sqrt(v.toDouble() / peak) * 3.999).toInt()).coerceIn(1, 4)

@Composable
private fun HeatCell(day: Int, level: Int, future: Boolean, today: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier.height(25.dp).let { m ->
            when {
                future -> m.drawBehind {
                    drawRoundRect(
                        FutureLine, cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(1.4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))),
                    )
                }
                today -> m.clip(shape).background(K.Heat[level]).border(2.dp, K.Ink, shape)
                else -> m.clip(shape).background(K.Heat[level])
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$day", fontSize = 12.sp, fontWeight = if (future) FontWeight.Bold else FontWeight.ExtraBold,
            color = when { future -> FutureText; level >= 3 -> Color.White; else -> K.Ink },
        )
    }
}
