package id.kantong.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.grouped
import id.kantong.app.ui.theme.K
import kotlin.math.abs

/*
 * catet building blocks (R89 reference): big rounded pastel cards, cream list cards, green primary,
 * outlined secondary, round icon buttons, money with a small "Rp". Screens should be built from these
 * plus the restyled helpers in Meow.kt (MeowSegmented, RedButton, CreamButton, MeowToggle, IconTile).
 */

/** Big rounded card (yellow balance, pink pending, sage budget, cream list...). */
@Composable
fun CCard(
    color: Color, modifier: Modifier = Modifier, radius: Dp = 28.dp, padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) = Column(modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(color).padding(padding), content = content)

/** Pill chip; [selected] turns it green. */
@Composable
fun CChip(
    text: String, modifier: Modifier = Modifier, icon: String? = null, bg: Color = K.Card, border: Boolean = false,
    selected: Boolean = false, height: Dp = 36.dp, onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(height / 2)
    Row(
        modifier.height(height).clip(shape).background(if (selected) K.Green else bg)
            .let { if (border && !selected) it.border(1.5.dp, K.Notch, shape) else it }
            .let { if (onClick != null) it.pressable(onClick) else it }
            .padding(start = if (icon != null) 8.dp else 13.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { CatIcon(icon, 22.dp); Spacer(Modifier.width(6.dp)) }
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else K.Ink, maxLines = 1)
    }
}

/** Round icon button (close, back, search, add). */
@Composable
fun CRoundButton(icon: ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 42.dp, outlined: Boolean = false) {
    Box(
        modifier.size(size).clip(CircleShape).background(if (outlined) K.Sheet else Color(0xFFF1EDE2))
            .let { if (outlined) it.border(1.5.dp, K.Notch, CircleShape) else it }.pressable(onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = K.Ink, modifier = Modifier.size(size * 0.5f)) }
}

/** Money with a small "Rp": e.g. Money(7004500, 52.sp). */
@Composable
fun Money(
    value: Long, size: TextUnit, modifier: Modifier = Modifier, color: Color = K.Green, hidden: Boolean = false,
    sign: String = if (value < 0) "−" else "", weight: FontWeight = FontWeight.ExtraBold,
) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontSize = size * 0.45f)) { append(sign + "Rp") }
            append(if (hidden) "•••••" else abs(value).grouped())
        },
        modifier, fontSize = size, fontWeight = weight, color = color, maxLines = 1, letterSpacing = (-size.value * 0.03f).sp,
    )
}

/** Two-arc budget ring: deep green = used, sage = left. */
@Composable
fun BudgetRing(fraction: Float, size: Dp, stroke: Dp, modifier: Modifier = Modifier, track: Color = K.SageMid, center: @Composable BoxScope.() -> Unit = {}) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx(); val d = this.size.minDimension - sw
            val tl = Offset(sw / 2, sw / 2); val sz = Size(d, d)
            val used = (fraction.coerceIn(0f, 1f) * 360f)
            val gap = if (used in 1f..359f) 16f else 0f
            if (used < 360f) drawArc(track, -90f + used + gap / 2, 360f - used - gap, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            if (used > 0f) drawArc(K.Green, -90f + gap / 2, (used - gap).coerceAtLeast(0.1f), false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
        }
        center()
    }
}

/** catet logo: a "C" made of the budget ring with a gold coin in its opening. */
@Composable
fun CatetLogo(size: Dp, modifier: Modifier = Modifier, dark: Boolean = false) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 34f
        val sw = 6.5f * u; val r = 11.5f * u; val c = Offset(16f * u, 17f * u)
        val tl = Offset(c.x - r, c.y - r); val sz = Size(2 * r, 2 * r)
        drawArc(if (dark) Color(0xFFFDFCF5) else K.Green, 48f, 150f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
        drawArc(K.SageMid, 205f, 85f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
        drawCircle(K.Gold, 4.6f * u, Offset(27.5f * u, 17f * u))
        drawCircle(if (dark) Color(0xFFFDFCF5) else Color(0xFF2F2F2F), 4.6f * u, Offset(27.5f * u, 17f * u), style = Stroke(1.8f * u))
    }
}

/** Big screen title with optional subtitle. */
@Composable
fun CTitle(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, letterSpacing = (-0.3).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1)
        }
        trailing()
    }
}

/** Section header: bold title left, green action right. */
@Composable
fun CSection(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.weight(1f))
        if (action != null) Text(action, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = K.Green, modifier = Modifier.pressable { onAction?.invoke() })
    }
}

/** Small grey label over a group with an optional right value (day headers, account groups). */
@Composable
fun CGroupLabel(left: String, modifier: Modifier = Modifier, right: String? = null, rightColor: Color = K.Muted) {
    Row(modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(left, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, modifier = Modifier.weight(1f), maxLines = 1)
        if (right != null) Text(right, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = rightColor, maxLines = 1)
    }
}

/** Cream list card; put CListRow items inside. */
@Composable
fun CList(modifier: Modifier = Modifier, color: Color = K.Card, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(color).padding(horizontal = 16.dp), content = content)

/** Row in a CList: two-tone icon, title, subtitle, trailing (chevron by default when clickable). */
@Composable
fun CListRow(
    icon: String?, title: String, subtitle: String? = null, divider: Boolean = true, onClick: (() -> Unit)? = null,
    height: Dp = 62.dp, trailing: (@Composable () -> Unit)? = null,
) {
    Column {
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
        Row(
            Modifier.fillMaxWidth().height(height).let { if (onClick != null) it.pressable(onClick) else it },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            if (icon != null) CatIcon(icon, 32.dp)
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            when {
                trailing != null -> trailing()
                onClick != null -> Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA39D90))
            }
        }
    }
}

/** Sheet handle bar. */
@Composable
fun CHandle(modifier: Modifier = Modifier) =
    Box(modifier.padding(top = 10.dp).size(40.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFDDD6C6)))
