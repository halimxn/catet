package id.kantong.app.ui.components

import androidx.compose.ui.graphics.asAndroidBitmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Kantong's own full-color "sticker" illustrations for categories and shortcuts, drawn on a 48x48 grid
 * with a soft dark outline. No pig imagery anywhere: savings is a coin jar.
 */
private val O = Color(0xFF2F2F2F)
private const val SW = 2.5f

private fun DrawScope.fillStroke(p: Path, fill: Color) {
    drawPath(p, fill)
    drawPath(p, O, style = Stroke(SW, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
private fun DrawScope.rr(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Color) {
    drawRoundRect(fill, Offset(x, y), Size(w, h), CornerRadius(r, r))
    drawRoundRect(O, Offset(x, y), Size(w, h), CornerRadius(r, r), style = Stroke(SW, join = StrokeJoin.Round))
}
private fun DrawScope.circ(cx: Float, cy: Float, r: Float, fill: Color) {
    drawCircle(fill, r, Offset(cx, cy)); drawCircle(O, r, Offset(cx, cy), style = Stroke(SW))
}
private fun DrawScope.line(x1: Float, y1: Float, x2: Float, y2: Float, w: Float = SW, c: Color = O) =
    drawLine(c, Offset(x1, y1), Offset(x2, y2), w, StrokeCap.Round)
private fun path(block: Path.() -> Unit) = Path().apply(block)
private fun DrawScope.shine(x: Float, y: Float, w: Float, h: Float) = Unit // flat two-tone style: no highlights
private fun DrawScope.ovl(x: Float, y: Float, w: Float, h: Float, fill: Color) {
    drawOval(fill, Offset(x, y), Size(w, h)); drawOval(O, Offset(x, y), Size(w, h), style = Stroke(SW))
}
/** Colored stroke with the dark outline around it (arrows, handles). */
private fun DrawScope.fatStroke(p: Path, c: Color, w: Float = 2.6f) {
    drawPath(p, O, style = Stroke(w + 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(p, c, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float) = path {
    for (i in 0 until 10) {
        val a = (-90.0 + 36.0 * i) * PI / 180.0
        val rad = if (i % 2 == 0) outer else inner
        val x = cx + rad * cos(a).toFloat(); val y = cy + rad * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/**
 * Drawn once per (key, pixel size) into a bitmap and reused: replaying dozens of stroked vector paths
 * on every frame was the main GPU cost while scrolling and switching tabs.
 */
private val illusCache = HashMap<String, ImageBitmap>()

internal fun cachedBitmap(id: String, px: Int, grid: Float, draw: DrawScope.() -> Unit): ImageBitmap =
    illusCache.getOrPut("$id@$px") {
        val bmp = ImageBitmap(px, px)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(bmp), Size(px.toFloat(), px.toFloat())) {
            val k = px / grid
            scale(k, k, pivot = Offset.Zero) { draw() }
        }
        bmp
    }

@Composable
fun CatIcon(key: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
    val bmp = remember(key, px) { cachedBitmap("illus:$key", px, 48f) { drawIllus(key) } }
    Spacer(modifier.size(size).drawBehind { drawImage(bmp) })
}


/**
 * catet two-tone style: every fill snaps to the small R89 palette (sage, pale sage, gold, cream, pink,
 * off-white, deep green) so all illustrations share one calm look with the dark outline.
 */
private val toneCache = HashMap<Long, Color>()
private fun tone(argb: Long): Color = toneCache.getOrPut(argb) {
    val r = ((argb shr 16) and 0xFF) / 255f; val g = ((argb shr 8) and 0xFF) / 255f; val b = (argb and 0xFF) / 255f
    val mx = maxOf(r, g, b); val mn = minOf(r, g, b); val l = (mx + mn) / 2f; val d = mx - mn
    val sat = if (d == 0f) 0f else d / (1f - kotlin.math.abs(2 * l - 1f))
    var h = when {
        d == 0f -> 0f
        mx == r -> 60f * (((g - b) / d) % 6f)
        mx == g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }
    if (h < 0) h += 360f
    when {
        l < 0.33f -> Color(0xFF2E5242)
        l > 0.92f || (sat < 0.18f && l > 0.8f) -> Color(0xFFFDFCF6)
        sat < 0.18f -> Color(0xFFB9C4AA)
        h in 35f..65f -> if (l > 0.78f) Color(0xFFF6E2AE) else Color(0xFFE8B860)
        h in 15f..35f -> if (l > 0.8f) Color(0xFFF6E2AE) else Color(0xFFE8B860)
        h >= 290f || h < 15f -> Color(0xFFF2D9D5)
        h in 65f..170f -> if (l > 0.75f) Color(0xFFB9C4AA) else Color(0xFFA9B79A)
        else -> if (l > 0.75f) Color(0xFFDDE5E8) else Color(0xFFA9B79A)
    }
}

private fun DrawScope.drawIllus(key: String) {
    when (key) {
        "food" -> { // noodle bowl with chopsticks
            line(30f, 6f, 40f, 22f, 2.6f); line(26f, 7f, 34f, 22f, 2.6f)
            fillStroke(path { moveTo(14f, 18f); cubicTo(16f, 12f, 32f, 12f, 34f, 18f); close() }, tone(0xFFFFE7A3))
            fillStroke(path { moveTo(6f, 22f); lineTo(42f, 22f); cubicTo(42f, 34f, 34f, 41f, 24f, 41f); cubicTo(14f, 41f, 6f, 34f, 6f, 22f); close() }, tone(0xFFF3A06B))
            drawRect(tone(0xFFE8834F), Offset(9f, 27f), Size(30f, 3f)); shine(11f, 24f, 4f, 7f)
        }
        "transport", "motor" -> { // scooter
            circ(12f, 34f, 6f, tone(0xFF8A94A6)); circ(36f, 34f, 6f, tone(0xFF8A94A6))
            fillStroke(path { moveTo(14f, 32f); lineTo(30f, 32f); lineTo(34f, 20f); lineTo(40f, 20f); lineTo(38f, 26f); lineTo(33f, 31f); close() }, tone(0xFF7FB6F0))
            fillStroke(path { moveTo(8f, 30f); cubicTo(8f, 24f, 14f, 22f, 22f, 23f); lineTo(24f, 30f); close() }, tone(0xFF5E9CE0))
            line(33f, 20f, 31f, 12f); line(28f, 12f, 35f, 12f, 2.6f); shine(12f, 25f, 6f, 2.5f)
        }
        "shopping" -> { // bag
            fillStroke(path { moveTo(9f, 16f); lineTo(39f, 16f); lineTo(36f, 42f); lineTo(12f, 42f); close() }, tone(0xFFB9A4F2))
            drawPath(path { moveTo(17f, 18f); cubicTo(17f, 8f, 31f, 8f, 31f, 18f) }, O, style = Stroke(2.6f, cap = StrokeCap.Round))
            rr(18f, 25f, 12f, 8f, 3f, tone(0xFFFFE7A3)); shine(13f, 19f, 4f, 9f)
        }
        "bill" -> { // receipt
            fillStroke(path { moveTo(11f, 6f); lineTo(37f, 6f); lineTo(37f, 42f); lineTo(33f, 39f); lineTo(29f, 42f); lineTo(25f, 39f); lineTo(21f, 42f); lineTo(17f, 39f); lineTo(11f, 42f); close() }, tone(0xFFFFF3D6))
            line(16f, 14f, 32f, 14f); line(16f, 20f, 32f, 20f); line(16f, 26f, 26f, 26f)
            circ(31f, 31f, 4.5f, tone(0xFFF5C451))
        }
        "home" -> { // house
            fillStroke(path { moveTo(10f, 22f); lineTo(38f, 22f); lineTo(38f, 41f); lineTo(10f, 41f); close() }, tone(0xFFFFE3C2))
            fillStroke(path { moveTo(5f, 23f); lineTo(24f, 7f); lineTo(43f, 23f); close() }, tone(0xFFE8776A))
            rr(20f, 29f, 8f, 12f, 2f, tone(0xFF7FB6F0)); rr(30f, 26f, 5f, 5f, 1f, tone(0xFFB9E7F7))
        }
        "fun" -> { // ticket
            fillStroke(path { moveTo(6f, 14f); lineTo(42f, 14f); lineTo(42f, 20f); cubicTo(38f, 20f, 38f, 28f, 42f, 28f); lineTo(42f, 34f); lineTo(6f, 34f); lineTo(6f, 28f); cubicTo(10f, 28f, 10f, 20f, 6f, 20f); close() }, tone(0xFFF59AB8))
            line(30f, 16f, 30f, 32f, 1.8f); circ(19f, 24f, 4f, tone(0xFFFFE7A3))
        }
        "health" -> { // heart with plus
            fillStroke(path { moveTo(24f, 41f); cubicTo(6f, 30f, 4f, 18f, 12f, 12f); cubicTo(17f, 8f, 22f, 11f, 24f, 15f); cubicTo(26f, 11f, 31f, 8f, 36f, 12f); cubicTo(44f, 18f, 42f, 30f, 24f, 41f); close() }, tone(0xFFF08A80))
            line(24f, 19f, 24f, 31f, 3.2f, Color.White); line(18f, 25f, 30f, 25f, 3.2f, Color.White)
        }
        "edu" -> { // open book
            fillStroke(path { moveTo(24f, 13f); cubicTo(18f, 9f, 10f, 9f, 5f, 11f); lineTo(5f, 37f); cubicTo(10f, 35f, 18f, 35f, 24f, 39f); close() }, tone(0xFFA6DBA0))
            fillStroke(path { moveTo(24f, 13f); cubicTo(30f, 9f, 38f, 9f, 43f, 11f); lineTo(43f, 37f); cubicTo(38f, 35f, 30f, 35f, 24f, 39f); close() }, tone(0xFF8CCB85))
            line(10f, 18f, 19f, 18f, 1.8f); line(10f, 24f, 19f, 24f, 1.8f); line(29f, 18f, 38f, 18f, 1.8f)
        }
        "charity" -> { // heart over hand
            fillStroke(path { moveTo(4f, 32f); lineTo(14f, 30f); cubicTo(18f, 29f, 22f, 32f, 26f, 32f); lineTo(32f, 32f); cubicTo(35f, 32f, 35f, 36f, 32f, 36f); lineTo(22f, 36f); lineTo(34f, 36f); lineTo(42f, 30f); cubicTo(45f, 28f, 47f, 32f, 44f, 34f); lineTo(34f, 42f); lineTo(14f, 42f); lineTo(4f, 40f); close() }, tone(0xFFFFD9B8))
            fillStroke(path { moveTo(25f, 25f); cubicTo(14f, 18f, 15f, 9f, 21f, 9f); cubicTo(23f, 9f, 24f, 10f, 25f, 12f); cubicTo(26f, 10f, 27f, 9f, 29f, 9f); cubicTo(35f, 9f, 36f, 18f, 25f, 25f); close() }, tone(0xFF7FD3A8))
        }
        "salary" -> { // briefcase
            rr(17f, 9f, 14f, 8f, 3f, tone(0xFFFFFFFF)); rr(5f, 15f, 38f, 25f, 5f, tone(0xFF8FD6B4))
            line(5f, 25f, 43f, 25f); rr(20f, 22f, 8f, 7f, 2f, tone(0xFFF5C451)); shine(9f, 18f, 4f, 5f)
        }
        "business" -> { // store
            fillStroke(path { moveTo(8f, 20f); lineTo(40f, 20f); lineTo(40f, 41f); lineTo(8f, 41f); close() }, tone(0xFFFFF0DA))
            fillStroke(path { moveTo(5f, 20f); lineTo(9f, 8f); lineTo(39f, 8f); lineTo(43f, 20f); close() }, tone(0xFFF3A06B))
            line(17f, 8f, 15f, 20f, 1.8f); line(24f, 8f, 24f, 20f, 1.8f); line(31f, 8f, 33f, 20f, 1.8f)
            rr(13f, 27f, 9f, 14f, 2f, tone(0xFF7FB6F0)); rr(26f, 26f, 9f, 7f, 2f, tone(0xFFB9E7F7))
        }
        "gift" -> {
            rr(7f, 19f, 34f, 22f, 4f, tone(0xFFF59AB8)); rr(5f, 13f, 38f, 8f, 3f, tone(0xFFF7B4CB))
            line(24f, 13f, 24f, 41f, 4f, tone(0xFFF5C451))
            drawPath(path { moveTo(24f, 13f); cubicTo(18f, 4f, 10f, 9f, 16f, 13f); moveTo(24f, 13f); cubicTo(30f, 4f, 38f, 9f, 32f, 13f) }, O, style = Stroke(SW, cap = StrokeCap.Round))
        }
        "travel" -> { // plane
            fillStroke(path { moveTo(6f, 26f); lineTo(40f, 12f); cubicTo(44f, 11f, 45f, 15f, 42f, 17f); lineTo(14f, 34f); lineTo(8f, 32f); close() }, tone(0xFF8EC9F2))
            fillStroke(path { moveTo(20f, 21f); lineTo(14f, 10f); lineTo(19f, 9f); lineTo(30f, 17f); close() }, tone(0xFF5E9CE0))
            fillStroke(path { moveTo(24f, 26f); lineTo(26f, 38f); lineTo(30f, 36f); lineTo(31f, 22f); close() }, tone(0xFF5E9CE0))
        }
        "shield" -> {
            fillStroke(path { moveTo(24f, 5f); lineTo(39f, 11f); lineTo(39f, 22f); cubicTo(39f, 32f, 32f, 39f, 24f, 43f); cubicTo(16f, 39f, 9f, 32f, 9f, 22f); lineTo(9f, 11f); close() }, tone(0xFFB9A4F2))
            drawPath(path { moveTo(17f, 23f); lineTo(22f, 28f); lineTo(31f, 18f) }, Color.White, style = Stroke(3.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        "wifi" -> {
            circ(24f, 24f, 18f, tone(0xFFB9DAF7))
            drawPath(path { moveTo(13f, 21f); cubicTo(19f, 15f, 29f, 15f, 35f, 21f); moveTo(17f, 26f); cubicTo(21f, 22f, 27f, 22f, 31f, 26f) }, O, style = Stroke(2.6f, cap = StrokeCap.Round))
            drawCircle(O, 2.6f, Offset(24f, 31f))
        }
        "bolt" -> {
            circ(24f, 24f, 18f, tone(0xFFFFE7A3))
            fillStroke(path { moveTo(27f, 9f); lineTo(15f, 27f); lineTo(23f, 27f); lineTo(20f, 39f); lineTo(33f, 20f); lineTo(25f, 20f); close() }, tone(0xFFF5C451))
        }
        "jar" -> { // Kantong coin jar
            fillStroke(path { moveTo(13f, 17f); lineTo(35f, 17f); lineTo(38f, 23f); lineTo(38f, 37f); cubicTo(38f, 40f, 36f, 42f, 33f, 42f); lineTo(15f, 42f); cubicTo(12f, 42f, 10f, 40f, 10f, 37f); lineTo(10f, 23f); close() }, tone(0xFFBDEBD6))
            rr(15f, 9f, 18f, 8f, 3f, tone(0xFF9FDCC2))
            circ(19f, 33f, 4.5f, tone(0xFFF5C451)); circ(28f, 30f, 4.5f, tone(0xFFF5C451)); shine(13f, 22f, 3.5f, 9f)
        }
        "envelope", "budget" -> {
            rr(5f, 11f, 38f, 27f, 5f, tone(0xFFFFE7A3))
            drawPath(path { moveTo(6f, 13f); lineTo(24f, 27f); lineTo(42f, 13f) }, O, style = Stroke(SW, cap = StrokeCap.Round, join = StrokeJoin.Round))
            circ(37f, 35f, 7f, tone(0xFF7FD3A8)); line(37f, 31f, 37f, 39f, 2f, Color.White)
        }
        "history" -> { // calendar
            rr(6f, 9f, 36f, 32f, 6f, Color.White); drawRoundRect(tone(0xFFF08A80), Offset(7f, 10f), Size(34f, 9f), CornerRadius(5f, 5f))
            drawRoundRect(O, Offset(6f, 9f), Size(36f, 32f), CornerRadius(6f, 6f), style = Stroke(SW))
            line(15f, 6f, 15f, 13f, 2.6f); line(33f, 6f, 33f, 13f, 2.6f)
            circ(30f, 31f, 6f, tone(0xFF8EC9F2)); line(30f, 28f, 30f, 31f, 1.8f); line(30f, 31f, 32.5f, 32.5f, 1.8f)
        }
        "report" -> { // pie
            fillStroke(path { moveTo(22f, 9f); lineTo(22f, 26f); lineTo(39f, 26f); cubicTo(39f, 35f, 31f, 42f, 22f, 42f); cubicTo(13f, 42f, 5f, 35f, 5f, 26f); cubicTo(5f, 17f, 12f, 9f, 22f, 9f); close() }, tone(0xFF8EC9F2))
            fillStroke(path { moveTo(26f, 5f); cubicTo(35f, 5f, 43f, 13f, 43f, 22f); lineTo(26f, 22f); close() }, tone(0xFFF3A06B))
        }
        "bank" -> { // bank building with a coin in the pediment
            rr(10f, 20f, 6f, 15f, 2f, Color.White); rr(21f, 20f, 6f, 15f, 2f, Color.White); rr(32f, 20f, 6f, 15f, 2f, Color.White)
            fillStroke(path { moveTo(5f, 19f); lineTo(24f, 7f); lineTo(43f, 19f); close() }, tone(0xFF8EC9F2))
            circ(24f, 14.5f, 2.8f, tone(0xFFF5C451))
            rr(6f, 35f, 36f, 7f, 2.5f, tone(0xFFFFE7A3))
        }
        "ewallet" -> { // phone showing a coin
            rr(12f, 5f, 24f, 38f, 7f, tone(0xFFB9A4F2))
            rr(16f, 10f, 16f, 23f, 3.5f, Color.White)
            circ(24f, 21.5f, 5.5f, tone(0xFFF5C451)); line(24f, 19f, 24f, 24f, 2f)
            line(21f, 38f, 27f, 38f, 2.4f)
        }
        "cash" -> { // two banknotes
            rr(10f, 10f, 33f, 20f, 4f, tone(0xFFBDEBD6))
            rr(5f, 18f, 33f, 20f, 4f, tone(0xFF7FD3A8))
            circ(21.5f, 28f, 4.5f, tone(0xFFFFE7A3))
            drawCircle(O, 1.6f, Offset(10.5f, 28f)); drawCircle(O, 1.6f, Offset(32.5f, 28f))
        }
        "wallet" -> {
            rr(5f, 13f, 38f, 27f, 6f, tone(0xFFF3A06B))
            rr(30f, 21f, 13f, 11f, 4f, tone(0xFFFFE7A3)); drawCircle(O, 1.8f, Offset(35f, 26.5f))
            drawPath(path { moveTo(9f, 13f); lineTo(30f, 6f); lineTo(33f, 13f) }, O, style = Stroke(SW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        "ledger" -> { // two stacked books with a bookmark ribbon
            fillStroke(path { moveTo(34f, 36f); lineTo(38f, 36f); lineTo(38f, 44f); lineTo(36f, 42f); lineTo(34f, 44f); close() }, tone(0xFFFF8662))
            rr(14f, 6f, 26f, 32f, 4f, tone(0xFF8EC9F2))
            rr(7f, 11f, 26f, 32f, 4f, tone(0xFFF3A06B))
            line(12f, 13f, 12f, 41f, 1.8f); rr(16f, 18f, 12f, 7f, 2f, tone(0xFFFFF3D6)); shine(14f, 28f, 3f, 8f)
        }
        "recurring" -> { // calendar page with a refresh symbol
            rr(6f, 9f, 36f, 32f, 6f, Color.White); drawRoundRect(tone(0xFFFF8662), Offset(7f, 10f), Size(34f, 9f), CornerRadius(5f, 5f))
            drawRoundRect(O, Offset(6f, 9f), Size(36f, 32f), CornerRadius(6f, 6f), style = Stroke(SW))
            line(15f, 6f, 15f, 13f, 2.6f); line(33f, 6f, 33f, 13f, 2.6f)
            drawArc(O, 200f, 130f, false, Offset(17.5f, 23.5f), Size(13f, 13f), style = Stroke(2.6f, cap = StrokeCap.Round))
            drawArc(O, 20f, 130f, false, Offset(17.5f, 23.5f), Size(13f, 13f), style = Stroke(2.6f, cap = StrokeCap.Round))
            val heads = path {
                moveTo(31.7f, 25.5f); lineTo(30.9f, 28.9f); lineTo(27.5f, 28f); close()
                moveTo(16.3f, 34.5f); lineTo(17.1f, 31.1f); lineTo(20.5f, 32f); close()
            }
            drawPath(heads, O); drawPath(heads, O, style = Stroke(1.2f, join = StrokeJoin.Round))
        }
        "category" -> { // 2x2 grid of colored tiles
            rr(6f, 6f, 16f, 16f, 5f, tone(0xFFF3A06B)); rr(26f, 6f, 16f, 16f, 5f, tone(0xFF42DFFE))
            rr(6f, 26f, 16f, 16f, 5f, tone(0xFF68ECB4)); rr(26f, 26f, 16f, 16f, 5f, tone(0xFFEDB1F1))
            shine(9f, 9f, 3f, 6f)
        }
        "reimburse" -> { // briefcase with a banknote sticking out
            fillStroke(path { moveTo(12f, 19f); lineTo(14f, 6f); lineTo(36f, 9f); lineTo(34f, 21f); close() }, tone(0xFF7FD3A8))
            circ(24.5f, 11.5f, 2.8f, tone(0xFFBDEBD6))
            rr(5f, 16f, 38f, 25f, 5f, tone(0xFFB9A4F2))
            line(5f, 26f, 43f, 26f); rr(20f, 23f, 8f, 7f, 2f, tone(0xFFF5C451)); shine(9f, 19f, 4f, 5f)
        }
        "bookmark" -> { // book with a star on the cover
            rr(10f, 5f, 28f, 38f, 5f, tone(0xFF8EC9F2))
            line(15f, 6.5f, 15f, 41.5f, 1.8f)
            fillStroke(starPath(26.5f, 23.5f, 9f, 4f), tone(0xFFFFD976))
        }
        "pencil" -> { // chunky pencil, diagonal: pink eraser, yellow body, wood tip, graphite point
            rotate(45f, Offset(24f, 24f)) {
                rr(19f, 3f, 10f, 7f, 3f, tone(0xFFF7A1B5))            // eraser
                rr(19f, 9f, 10f, 4f, 1f, tone(0xFFC9CED6))            // metal band
                rr(19f, 13f, 10f, 22f, 1.5f, tone(0xFFFFD976))        // body
                line(24f, 14f, 24f, 34f, 1.6f, tone(0xFFF5C451))
                fillStroke(path { moveTo(19f, 35f); lineTo(29f, 35f); lineTo(24f, 45f); close() }, tone(0xFFF9D9B0)) // wood
                fillStroke(path { moveTo(22.2f, 41.2f); lineTo(25.8f, 41.2f); lineTo(24f, 45f); close() }, O)       // point
                shine(20.5f, 15f, 2.5f, 12f)
            }
        }
        "search" -> { // magnifier
            line(30f, 30f, 40f, 40f, 8f); line(30f, 30f, 40f, 40f, 3.5f, tone(0xFFF3A06B))
            circ(20f, 20f, 12.5f, tone(0xFF8EC9F2)); drawCircle(tone(0xFFCFEFFB), 8.5f, Offset(20f, 20f))
            shine(13.5f, 14f, 3.5f, 7f)
        }
        "transfer" -> { // wallet with two arrows above
            rr(6f, 25f, 36f, 18f, 6f, tone(0xFFF3A06B))
            rr(31f, 30f, 11f, 8f, 3.5f, tone(0xFFFFE7A3)); drawCircle(O, 1.6f, Offset(35.5f, 34f))
            fatStroke(path { moveTo(13f, 9.5f); lineTo(33f, 9.5f); moveTo(30f, 6.5f); lineTo(33f, 9.5f); lineTo(30f, 12.5f) }, tone(0xFF68ECB4))
            fatStroke(path { moveTo(35f, 19.5f); lineTo(15f, 19.5f); moveTo(18f, 16.5f); lineTo(15f, 19.5f); lineTo(18f, 22.5f) }, tone(0xFFFF8662))
        }
        "kanto" -> { // the hamster mascot, head and body (same drawing as Kanto, scaled to the 48 grid)
            scale(0.44f, 0.44f, pivot = Offset.Zero) { translate(-6f, -14f) { drawKanto(KantoMood.SMILE, coin = false) } }
        }
        "card" -> { // credit card with chip and stripe
            drawRoundRect(tone(0xFF8EC9F2), Offset(4f, 11f), Size(40f, 27f), CornerRadius(5f, 5f))
            drawRect(tone(0xFF5E9CE0), Offset(4f, 17f), Size(40f, 5f))
            drawRoundRect(O, Offset(4f, 11f), Size(40f, 27f), CornerRadius(5f, 5f), style = Stroke(SW, join = StrokeJoin.Round))
            rr(9f, 25f, 9f, 7f, 2f, tone(0xFFF5C451)); line(9f, 28.5f, 18f, 28.5f, 1.4f)
            line(24f, 31f, 37f, 31f, 2f)
        }
        "invest" -> { // rising bars with an up arrow
            rr(7f, 32f, 8f, 10f, 2.5f, tone(0xFF7FD3A8)); rr(19f, 25f, 8f, 17f, 2.5f, tone(0xFF68ECB4)); rr(31f, 18f, 8f, 24f, 2.5f, tone(0xFF7FD3A8))
            fatStroke(path { moveTo(6f, 22f); lineTo(15f, 14f); lineTo(21f, 18f); lineTo(34f, 6f); moveTo(27f, 6f); lineTo(34f, 6f); lineTo(34f, 13f) }, tone(0xFFF3A06B))
        }
        "gold" -> { // three stacked gold bars
            fun bar(x: Float, y: Float) {
                val p = path { moveTo(x, y + 11f); lineTo(x + 20f, y + 11f); lineTo(x + 16f, y); lineTo(x + 4f, y); close() }
                drawPath(p, tone(0xFFF5C451))
                drawRect(tone(0xFFFFD976), Offset(x + 4f, y), Size(12f, 11f))
                drawPath(p, O, style = Stroke(SW, cap = StrokeCap.Round, join = StrokeJoin.Round))
                line(x + 4f, y, x + 4f, y + 11f, 1.6f); line(x + 16f, y, x + 16f, y + 11f, 1.6f)
            }
            bar(4f, 31f); bar(24f, 31f); bar(14f, 20f); shine(19.5f, 22.5f, 6f, 2.2f)
        }
        "safe" -> { // vault with a round dial
            rr(10f, 37f, 6f, 6f, 2f, tone(0xFF8A94A6)); rr(32f, 37f, 6f, 6f, 2f, tone(0xFF8A94A6))
            rr(6f, 7f, 36f, 32f, 6f, tone(0xFFA9B8CC))
            rr(10f, 11f, 28f, 24f, 4f, tone(0xFFC9D5E3))
            circ(21f, 23f, 6.5f, Color.White); line(21f, 23f, 21f, 18.5f, 2f); drawCircle(O, 1.6f, Offset(21f, 23f))
            line(33f, 18f, 33f, 28f, 3f)
        }
        "coinbag" -> { // money sack with a coin in front
            fillStroke(path { moveTo(19f, 13f); cubicTo(8f, 20f, 5f, 30f, 8f, 37f); cubicTo(10f, 42f, 16f, 43f, 24f, 43f); cubicTo(32f, 43f, 38f, 42f, 40f, 37f); cubicTo(43f, 30f, 40f, 20f, 29f, 13f); close() }, tone(0xFFE0A46E))
            fillStroke(path { moveTo(17f, 5f); cubicTo(20f, 8f, 28f, 8f, 31f, 5f); lineTo(28f, 13f); lineTo(20f, 13f); close() }, tone(0xFFE0A46E))
            rr(18f, 11f, 12f, 4f, 2f, tone(0xFFF5C451)); shine(12f, 24f, 3f, 8f)
            circ(32f, 34f, 8f, tone(0xFFFFD976)); line(32f, 30f, 32f, 38f, 2.2f, tone(0xFFE2A93B))
        }
        "coffee" -> { // cup on a saucer with steam
            drawPath(path { moveTo(18f, 16f); cubicTo(15f, 13f, 21f, 10f, 18f, 5f); moveTo(27f, 16f); cubicTo(24f, 13f, 30f, 10f, 27f, 5f) }, O, style = Stroke(2.2f, cap = StrokeCap.Round))
            fatStroke(path { moveTo(34f, 24f); cubicTo(42f, 23f, 42f, 33f, 33f, 33f) }, tone(0xFFF3A06B))
            rr(6f, 37f, 36f, 6f, 3f, tone(0xFFFFE7A3))
            fillStroke(path { moveTo(10f, 20f); lineTo(36f, 20f); lineTo(34f, 34f); cubicTo(33.5f, 37f, 31f, 39f, 28f, 39f); lineTo(18f, 39f); cubicTo(15f, 39f, 12.5f, 37f, 12f, 34f); close() }, tone(0xFFF3A06B))
            shine(14f, 23f, 3f, 8f)
        }
        "fruit" -> { // orange with a leaf
            circ(24f, 27f, 15f, tone(0xFFF3A06B))
            line(24f, 12f, 25f, 7f, 2.4f)
            fillStroke(path { moveTo(25f, 10f); cubicTo(28f, 4f, 36f, 3f, 40f, 6f); cubicTo(37f, 12f, 30f, 13f, 25f, 10f); close() }, tone(0xFF7FD3A8))
            drawCircle(tone(0xFFE8834F), 1f, Offset(30f, 30f)); drawCircle(tone(0xFFE8834F), 1f, Offset(26f, 35f)); drawCircle(tone(0xFFE8834F), 1f, Offset(33f, 24f))
            shine(14f, 20f, 4f, 8f)
        }
        "fuel" -> { // fuel pump with nozzle
            drawPath(path { moveTo(29f, 33f); cubicTo(42f, 33f, 43f, 29f, 38f, 23f) }, O, style = Stroke(2.6f, cap = StrokeCap.Round))
            rr(35f, 13f, 6f, 10f, 2f, tone(0xFF8A94A6))
            rr(9f, 7f, 20f, 33f, 5f, tone(0xFFFF8662))
            rr(12.5f, 11f, 13f, 9f, 2.5f, tone(0xFFFFF3D6))
            fillStroke(path { moveTo(19f, 24f); cubicTo(16f, 28f, 15.5f, 30f, 15.5f, 31f); cubicTo(15.5f, 34f, 22.5f, 34f, 22.5f, 31f); cubicTo(22.5f, 30f, 22f, 28f, 19f, 24f); close() }, tone(0xFFFFD976))
            rr(6f, 38f, 26f, 5f, 2.5f, tone(0xFF8A94A6))
        }
        "parking" -> { // parking sign
            rr(6f, 6f, 36f, 36f, 9f, tone(0xFF5E9CE0))
            drawPath(path { moveTo(19f, 34f); lineTo(19f, 14f); lineTo(26f, 14f); cubicTo(33f, 14f, 33f, 25f, 26f, 25f); lineTo(19f, 25f) }, Color.White, style = Stroke(4.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            shine(10f, 10f, 3.5f, 8f)
        }
        "clothes" -> { // t-shirt
            fillStroke(path { moveTo(17f, 7f); cubicTo(19f, 11f, 29f, 11f, 31f, 7f); lineTo(43f, 13f); lineTo(39f, 22f); lineTo(34f, 20f); lineTo(34f, 41f); lineTo(14f, 41f); lineTo(14f, 20f); lineTo(9f, 22f); lineTo(5f, 13f); close() }, tone(0xFFB9A4F2))
            rr(25f, 23f, 6f, 6f, 2f, tone(0xFFFFE7A3)); shine(17f, 22f, 3f, 8f)
        }
        "beauty" -> { // lipstick and a small bottle
            rr(11f, 15f, 7f, 6f, 1.5f, Color.White); rr(9.5f, 9f, 10f, 7f, 2.5f, tone(0xFFFFD976))
            rr(6f, 21f, 17f, 21f, 5f, tone(0xFFEDB1F1)); shine(9f, 25f, 3f, 8f)
            fillStroke(path { moveTo(30.5f, 22f); lineTo(30.5f, 12f); lineTo(37.5f, 7f); lineTo(37.5f, 22f); close() }, tone(0xFFFF8662))
            rr(29.5f, 21f, 9f, 6f, 1.5f, Color.White); rr(28f, 26f, 12f, 16f, 3f, tone(0xFFF5C451))
        }
        "baby" -> { // baby bottle
            fillStroke(path { moveTo(19f, 13f); cubicTo(19f, 8f, 21f, 5f, 24f, 5f); cubicTo(27f, 5f, 29f, 8f, 29f, 13f); close() }, tone(0xFFFFCC80))
            rr(13f, 18f, 22f, 25f, 7f, Color.White)
            rr(14f, 12f, 20f, 7f, 3f, tone(0xFF8EC9F2))
            line(18f, 26f, 22f, 26f, 1.8f); line(18f, 31f, 22f, 31f, 1.8f); line(18f, 36f, 22f, 36f, 1.8f)
        }
        "pet" -> { // paw print
            ovl(8f, 15f, 7f, 9f, tone(0xFFF3A06B)); ovl(16f, 7f, 7.5f, 10f, tone(0xFFF3A06B))
            ovl(24.5f, 7f, 7.5f, 10f, tone(0xFFF3A06B)); ovl(33f, 15f, 7f, 9f, tone(0xFFF3A06B))
            fillStroke(path { moveTo(24f, 23f); cubicTo(31f, 23f, 37f, 30f, 36f, 36f); cubicTo(35f, 41f, 29f, 40f, 24f, 40f); cubicTo(19f, 40f, 13f, 41f, 12f, 36f); cubicTo(11f, 30f, 17f, 23f, 24f, 23f); close() }, tone(0xFFF3A06B))
            shine(17f, 28f, 3f, 5f)
        }
        "sport" -> { // football
            circ(24f, 24f, 18f, Color.White)
            line(24f, 18f, 24f, 11f, 2f); line(29.7f, 22.2f, 36.4f, 20f, 2f); line(27.5f, 28.9f, 31.6f, 34.5f, 2f)
            line(20.5f, 28.9f, 16.4f, 34.5f, 2f); line(18.3f, 22.2f, 11.6f, 20f, 2f)
            val pent = path { moveTo(24f, 18f); lineTo(29.7f, 22.2f); lineTo(27.5f, 28.9f); lineTo(20.5f, 28.9f); lineTo(18.3f, 22.2f); close() }
            drawPath(pent, O); drawPath(pent, O, style = Stroke(1.6f, join = StrokeJoin.Round))
        }
        "gadget" -> { // laptop
            rr(9f, 10f, 30f, 23f, 4f, tone(0xFFB9A4F2))
            drawRoundRect(tone(0xFFDDF5FC), Offset(12.5f, 13.5f), Size(23f, 15.5f), CornerRadius(2f, 2f))
            fillStroke(path { moveTo(5f, 33f); lineTo(43f, 33f); lineTo(43f, 37f); cubicTo(43f, 39f, 42f, 40f, 40f, 40f); lineTo(8f, 40f); cubicTo(6f, 40f, 5f, 39f, 5f, 37f); close() }, tone(0xFFEDB1F1))
            line(20f, 36.5f, 28f, 36.5f, 1.8f); shine(15f, 16f, 3f, 7f)
        }
        "phone" -> { // smartphone with signal arcs
            rr(7f, 9f, 20f, 34f, 6f, tone(0xFF8EC9F2))
            rr(10.5f, 14f, 13f, 21f, 3f, Color.White)
            line(14.5f, 11.5f, 19.5f, 11.5f, 1.8f); line(14.5f, 39f, 19.5f, 39f, 2.2f)
            listOf(6f, 12f).forEach { r ->
                drawArc(O, -80f, 70f, false, Offset(29f - r, 18f - r), Size(2 * r, 2 * r), style = Stroke(6.4f, cap = StrokeCap.Round))
                drawArc(tone(0xFF68ECB4), -80f, 70f, false, Offset(29f - r, 18f - r), Size(2 * r, 2 * r), style = Stroke(2.4f, cap = StrokeCap.Round))
            }
        }
        "water" -> { // water gallon
            fillStroke(path { moveTo(17f, 13f); lineTo(31f, 13f); cubicTo(38f, 13f, 40f, 17f, 40f, 22f); lineTo(40f, 38f); cubicTo(40f, 41f, 38f, 43f, 35f, 43f); lineTo(13f, 43f); cubicTo(10f, 43f, 8f, 41f, 8f, 38f); lineTo(8f, 22f); cubicTo(8f, 17f, 10f, 13f, 17f, 13f); close() }, tone(0xFF8EC9F2))
            rr(20.5f, 9f, 7f, 5f, 1.5f, tone(0xFFCFEFFB)); rr(19f, 4f, 10f, 6f, 2f, tone(0xFF5E9CE0))
            fillStroke(path { moveTo(24f, 20f); cubicTo(20f, 25f, 19f, 27f, 19f, 29f); cubicTo(19f, 32.5f, 29f, 32.5f, 29f, 29f); cubicTo(29f, 27f, 28f, 25f, 24f, 20f); close() }, Color.White)
            line(9.5f, 37.5f, 38.5f, 37.5f, 1.8f); shine(11f, 18f, 3f, 8f)
        }
        "tax" -> { // receipt with a percent mark
            fillStroke(path { moveTo(11f, 6f); lineTo(37f, 6f); lineTo(37f, 42f); lineTo(33f, 39f); lineTo(29f, 42f); lineTo(25f, 39f); lineTo(21f, 42f); lineTo(17f, 39f); lineTo(11f, 42f); close() }, tone(0xFFFFF3D6))
            line(17f, 29f, 31f, 13f, 2.8f)
            circ(18.5f, 15f, 3f, tone(0xFFFF8662)); circ(29.5f, 27f, 3f, tone(0xFFFF8662))
            line(16f, 34f, 32f, 34f, 2f)
        }
        "car" -> { // small car
            fillStroke(path { moveTo(5f, 34f); lineTo(5f, 29f); cubicTo(5f, 26f, 7f, 25f, 10f, 24.5f); lineTo(14f, 17f); cubicTo(15f, 15f, 16.5f, 14f, 19f, 14f); lineTo(29f, 14f); cubicTo(31.5f, 14f, 33f, 15f, 34.5f, 17f); lineTo(39f, 24.5f); cubicTo(42f, 25f, 43f, 26.5f, 43f, 29f); lineTo(43f, 34f); cubicTo(43f, 35.5f, 42f, 36.5f, 40.5f, 36.5f); lineTo(7.5f, 36.5f); cubicTo(6f, 36.5f, 5f, 35.5f, 5f, 34f); close() }, tone(0xFF42DFFE))
            fillStroke(path { moveTo(15.5f, 24.5f); lineTo(18.5f, 18f); lineTo(23f, 18f); lineTo(23f, 24.5f); close() }, tone(0xFFDDF5FC))
            fillStroke(path { moveTo(26f, 24.5f); lineTo(26f, 18f); lineTo(29.5f, 18f); lineTo(33f, 24.5f); close() }, tone(0xFFDDF5FC))
            drawCircle(tone(0xFFFFD976), 1.8f, Offset(40.5f, 29.5f)); shine(8f, 28f, 5f, 2f)
            circ(14f, 37f, 5f, tone(0xFF8A94A6)); circ(34f, 37f, 5f, tone(0xFF8A94A6))
            drawCircle(Color.White, 1.6f, Offset(14f, 37f)); drawCircle(Color.White, 1.6f, Offset(34f, 37f))
        }
        "train" -> { // commuter train front
            line(15f, 36f, 10f, 43f, 2.6f); line(33f, 36f, 38f, 43f, 2.6f)
            rr(10f, 5f, 28f, 32f, 8f, tone(0xFFFF8662))
            rr(14f, 10f, 20f, 12f, 3f, tone(0xFFDDF5FC))
            circ(16.5f, 29f, 2.5f, tone(0xFFFFD976)); circ(31.5f, 29f, 2.5f, tone(0xFFFFD976))
            line(21f, 29f, 27f, 29f, 2f)
        }
        "game" -> { // game controller
            fillStroke(path { moveTo(14f, 15f); lineTo(34f, 15f); cubicTo(41f, 15f, 44f, 22f, 44f, 30f); cubicTo(44f, 36f, 40f, 39f, 36f, 36f); lineTo(31f, 32f); lineTo(17f, 32f); lineTo(12f, 36f); cubicTo(8f, 39f, 4f, 36f, 4f, 30f); cubicTo(4f, 22f, 7f, 15f, 14f, 15f); close() }, tone(0xFFB9A4F2))
            line(10f, 24f, 18f, 24f, 2.8f); line(14f, 20f, 14f, 28f, 2.8f)
            drawCircle(tone(0xFFFFD976), 2.8f, Offset(31f, 22f)); drawCircle(O, 2.8f, Offset(31f, 22f), style = Stroke(1.8f))
            drawCircle(tone(0xFFFF8662), 2.8f, Offset(36f, 27f)); drawCircle(O, 2.8f, Offset(36f, 27f), style = Stroke(1.8f))
        }
        "music" -> { // beamed music notes
            line(16.5f, 36f, 16.5f, 13f, 2.8f); line(34.5f, 32f, 34.5f, 9f, 2.8f)
            fillStroke(path { moveTo(16.5f, 13f); lineTo(34.5f, 9f); lineTo(34.5f, 15f); lineTo(16.5f, 19f); close() }, tone(0xFFB9A4F2))
            ovl(7f, 32f, 11f, 8.5f, tone(0xFFB9A4F2)); ovl(25f, 28f, 11f, 8.5f, tone(0xFFB9A4F2))
            shine(9.5f, 34f, 3f, 2.5f); shine(27.5f, 30f, 3f, 2.5f)
        }
        "medicine" -> { // capsule pill
            rotate(-40f, Offset(24f, 24f)) {
                drawRoundRect(Color.White, Offset(6f, 16f), Size(36f, 16f), CornerRadius(8f, 8f))
                drawPath(path { moveTo(24f, 16f); lineTo(14f, 16f); cubicTo(9.58f, 16f, 6f, 19.58f, 6f, 24f); cubicTo(6f, 28.42f, 9.58f, 32f, 14f, 32f); lineTo(24f, 32f); close() }, tone(0xFFFF8662))
                drawRoundRect(O, Offset(6f, 16f), Size(36f, 16f), CornerRadius(8f, 8f), style = Stroke(SW))
                line(24f, 16f, 24f, 32f); shine(11f, 19f, 8f, 2.5f)
            }
        }
        "family" -> { // adult and child
            fillStroke(path { moveTo(5f, 43f); cubicTo(5f, 32f, 10f, 27f, 18f, 27f); cubicTo(26f, 27f, 31f, 32f, 31f, 43f); close() }, tone(0xFF7FD3A8))
            circ(18f, 17f, 8f, tone(0xFFFFD9B8))
            fillStroke(path { moveTo(27f, 43f); cubicTo(27f, 36f, 30f, 33f, 35f, 33f); cubicTo(40f, 33f, 43f, 36f, 43f, 43f); close() }, tone(0xFFFFCC80))
            circ(35f, 26f, 5.5f, tone(0xFFFFD9B8))
            drawCircle(O, 1.3f, Offset(15f, 17f)); drawCircle(O, 1.3f, Offset(21f, 17f))
            drawCircle(O, 1.1f, Offset(33f, 26f)); drawCircle(O, 1.1f, Offset(37f, 26f))
        }
        "rice" -> { // rice bowl with chopsticks
            line(28f, 6f, 38f, 21f, 2.6f); line(33f, 5f, 42f, 19f, 2.6f)
            fillStroke(path { moveTo(9f, 22f); cubicTo(10f, 13f, 16f, 10f, 24f, 10f); cubicTo(32f, 10f, 38f, 13f, 39f, 22f); close() }, Color.White)
            line(17f, 16f, 19f, 15f, 1.4f); line(24f, 13.5f, 26f, 14.5f, 1.4f); line(30f, 17f, 32f, 16.5f, 1.4f)
            rr(17f, 39f, 14f, 4f, 2f, tone(0xFF5E9CE0))
            fillStroke(path { moveTo(5f, 22f); lineTo(43f, 22f); cubicTo(43f, 34f, 35f, 41f, 24f, 41f); cubicTo(13f, 41f, 5f, 34f, 5f, 22f); close() }, tone(0xFF8EC9F2))
            drawRect(tone(0xFF5E9CE0), Offset(8f, 27f), Size(32f, 3f)); shine(10f, 24f, 4f, 7f)
        }
        "snack" -> { // bag of chips
            fillStroke(path { moveTo(10f, 7f); lineTo(38f, 7f); lineTo(36f, 12f); cubicTo(39f, 20f, 39f, 30f, 36f, 37f); lineTo(38f, 42f); lineTo(10f, 42f); lineTo(12f, 37f); cubicTo(9f, 30f, 9f, 20f, 12f, 12f); close() }, tone(0xFFFF8662))
            line(12f, 12f, 36f, 12f, 1.8f); line(12f, 37f, 36f, 37f, 1.8f)
            circ(24f, 24.5f, 7f, tone(0xFFFFD976))
            drawCircle(tone(0xFFE2A93B), 1f, Offset(22f, 22.5f)); drawCircle(tone(0xFFE2A93B), 1f, Offset(26.5f, 26f))
            shine(14f, 16f, 3f, 8f)
        }
        "laundry" -> { // washing machine
            rr(8f, 5f, 32f, 38f, 6f, Color.White)
            line(8f, 13f, 40f, 13f); drawCircle(O, 1.5f, Offset(13f, 9f)); drawCircle(O, 1.5f, Offset(17.5f, 9f)); line(28f, 9f, 35f, 9f, 2f)
            circ(24f, 28f, 10f, tone(0xFFDCE3EC)); circ(24f, 28f, 6.5f, tone(0xFF8EC9F2))
            shine(20f, 24f, 2.5f, 5f)
        }
        else -> { // coin stack
            rr(9f, 30f, 30f, 9f, 4.5f, tone(0xFFF5C451)); rr(11f, 21f, 30f, 9f, 4.5f, tone(0xFFFFD976)); rr(7f, 12f, 30f, 9f, 4.5f, tone(0xFFF5C451))
        }
    }
}

/** Two-tone icon as an Android bitmap (home-screen widgets). */
fun illusBitmap(key: String, px: Int): android.graphics.Bitmap =
    cachedBitmap("illus:$key", px, 48f) { drawIllus(key) }.asAndroidBitmap()
