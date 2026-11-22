package id.kantong.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Icon
import id.kantong.app.data.grouped
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone

/** Soft press feedback (scale 0.96) used by every tappable surface. */
@Composable
fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, tween(160), label = "press")
    return this.scale(s).clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** Horizontal progress bar. Drawn at its value right away (no replay on every screen visit); animates only when the value changes. */
@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier, track: Color = Color(0x121D2433), height: Dp = 8.dp) {
    @Suppress("NAME_SHADOWING") val height = height.coerceAtMost(8.dp) // thin, calm bars everywhere
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(450), label = "bar")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height)).background(track)) {
        Box(Modifier.fillMaxWidth(f).height(height).clip(RoundedCornerShape(height)).background(color))
    }
}

/** Section title in the sticker-book style: heavy, first letter in the accent color, optional dark "more" chevron. */
@Composable
fun SectionTitle(title: String, accent: Color = K.Expense, modifier: Modifier = Modifier, onMore: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = accent)) { append(title.take(1)) }
                append(title.drop(1))
            },
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = K.Ink, modifier = Modifier.weight(1f),
        )
        if (onMore != null) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(K.Ink).pressable(onMore), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ChevronRight, "Lihat semua", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** The rounded sheet under a summary, with the small notch on top. */
@Composable
fun PeekSheet(modifier: Modifier = Modifier, bottom: Dp = 112.dp, spacing: Dp = 0.dp, content: @Composable ColumnScope.() -> Unit) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)).background(K.Sheet)
                .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = bottom + navBottom),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
        Notch(Modifier.align(Alignment.TopCenter))
    }
}

/** Small filled triangle: down (expense), up (income) or pointing right (timeline hour marker). */
@Composable
fun Trend(up: Boolean, color: Color, size: Dp = 10.dp, pointRight: Boolean = false) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val p = androidx.compose.ui.graphics.Path().apply {
            when {
                pointRight -> { moveTo(w * .1f, 0f); lineTo(w, h / 2); lineTo(w * .1f, h) }
                up -> { moveTo(w / 2, h * .1f); lineTo(w, h * .9f); lineTo(0f, h * .9f) }
                else -> { moveTo(0f, h * .1f); lineTo(w, h * .1f); lineTo(w / 2, h * .9f) }
            }
            close()
        }
        drawPath(p, color)
    }
}
