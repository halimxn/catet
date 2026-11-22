package id.kantong.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class KantoMood { HAPPY, SMILE, PROUD }

/** Same soft outline as the CatIcon sticker pack, so Kanto belongs to the same family. */
private val Outline = Color(0xFF3B3442)
private const val OW = 3.2f


/**
 * Kanto, the hamster mascot: a hamster keeps food in its cheek pouches ("kantong pipi"), like a saver.
 * Clean sticker style on a 120x120 grid: cream body, bold outline, orange cap, rosy cheeks, hugging a coin.
 * Static by default; [bob] adds a gentle float for one-off moments (splash, celebrations).
 */
@Composable
fun Kanto(size: Dp = 96.dp, mood: KantoMood = KantoMood.SMILE, modifier: Modifier = Modifier, bob: Boolean = false) {
    val dy = if (bob) {
        val t = rememberInfiniteTransition(label = "bob")
        t.animateFloat(0f, -5f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "dy").value
    } else 0f
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
    val bmp = remember(mood, px) { cachedBitmap("kanto:$mood", px, 120f) { drawKanto(mood) } }
    Spacer(modifier.size(size).drawBehind { translate(top = dy * px / 120f) { drawImage(bmp) } })
}

private val Cream = Color(0xFFFFF8EE)
private val Fur = Color(0xFFF6B97A)
private val Pink = Color(0xFFF7A8B8)
private val Blush = Color(0xE6F7A1B4)

private fun svg(d: String): Path = androidx.compose.ui.graphics.vector.PathParser().parsePathString(d).toPath()
private val BODY = svg("M60 30 C80 30 93 40 97 55 C104 60 106 74 100 84 C98 98 82 108 60 108 C38 108 22 98 20 84 C14 74 16 60 23 55 C27 40 40 30 60 30 Z")
private val CAP = svg("M26 56 C29 40 43 32 60 32 C77 32 91 40 94 56 C86 50 76 47 68 49 C66 44 62 42 60 47 C58 42 54 44 52 49 C44 47 34 50 26 56 Z")

private fun DrawScope.ring(cx: Float, cy: Float, r: Float, fill: Color, w: Float = 3.2f) {
    drawCircle(fill, r, Offset(cx, cy)); drawCircle(Outline, r, Offset(cx, cy), style = Stroke(w))
}
private fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, fill: Color, w: Float = 3f) {
    drawOval(fill, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2))
    if (w > 0f) drawOval(Outline, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2), style = Stroke(w))
}

/** Draws the hamster. [coin] = the coin hugged in front (hidden when only the head is needed). */
internal fun DrawScope.drawKanto(mood: KantoMood, coin: Boolean = true) {
    oval(45f, 107f, 9f, 5f, Cream); oval(75f, 107f, 9f, 5f, Cream)          // feet
    ring(35f, 34f, 11f, Fur); drawCircle(Pink, 5.5f, Offset(35f, 34f))     // ears
    ring(85f, 34f, 11f, Fur); drawCircle(Pink, 5.5f, Offset(85f, 34f))
    drawPath(BODY, Cream); drawPath(BODY, Outline, style = Stroke(3.5f, join = StrokeJoin.Round))
    drawPath(CAP, Fur)
    oval(33f, 78f, 8.5f, 5.5f, Blush, 0f); oval(87f, 78f, 8.5f, 5.5f, Blush, 0f)
    // whiskers
    listOf(Offset(16f, 72f) to Offset(25f, 74f), Offset(16f, 80f) to Offset(25f, 79f), Offset(104f, 72f) to Offset(95f, 74f), Offset(104f, 80f) to Offset(95f, 79f))
        .forEach { (a, b) -> drawLine(Outline, a, b, 2f, StrokeCap.Round) }
    if (mood == KantoMood.HAPPY || mood == KantoMood.PROUD) {
        drawPath(svg("M40.5 68 Q46 61.5 51.5 68 M68.5 68 Q74 61.5 79.5 68"), Outline, style = Stroke(3.2f, cap = StrokeCap.Round))
    } else {
        oval(46f, 67f, 4.6f, 5.6f, Outline, 0f); drawCircle(Color.White, 1.8f, Offset(47.6f, 65f))
        oval(74f, 67f, 4.6f, 5.6f, Outline, 0f); drawCircle(Color.White, 1.8f, Offset(75.6f, 65f))
    }
    oval(60f, 75f, 3.6f, 2.6f, Color(0xFFF28CA0), 1.6f)                   // nose
    if (mood == KantoMood.SMILE) {
        drawPath(svg("M53.5 79.5 Q56.8 84 60 79.8 Q63.2 84 66.5 79.5"), Outline, style = Stroke(2.6f, cap = StrokeCap.Round))
    } else {
        val mouth = svg("M54 79 Q60 81 66 79 Q65.5 88 60 88.5 Q54.5 88 54 79 Z")
        drawPath(mouth, Color(0xFFC44A5A)); oval(60f, 85.5f, 3.6f, 2f, Color(0xFFF59AAA), 0f)
        drawPath(mouth, Outline, style = Stroke(2.4f, join = StrokeJoin.Round))
    }
    if (coin) {
        oval(48f, 95f, 7f, 5.5f, Cream)
        ring(60f, 96f, 10.5f, Color(0xFFFFD976), 3f)
        drawCircle(Color(0xFFE2A93B), 6.5f, Offset(60f, 96f), style = Stroke(1.8f))
        oval(72f, 95f, 7f, 5.5f, Cream)
    }
    if (mood == KantoMood.PROUD) {
        val p = svg("M14 30 Q14 38 22 38 Q14 38 14 46 Q14 38 6 38 Q14 38 14 30 Z")
        drawPath(p, Color(0xFFFFD976)); drawPath(p, Outline, style = Stroke(1.8f, join = StrokeJoin.Round))
    }
}
