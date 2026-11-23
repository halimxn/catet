package id.kantong.app.ui.components

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.material.icons.rounded.Schedule

import androidx.compose.ui.draw.drawBehind

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import id.kantong.app.data.Prefs
import id.kantong.app.data.fmt
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone
import java.util.Calendar

/*
 * Kantong's "sticker book" building blocks, modelled on Meow-style bookkeeping apps:
 * cream canvas, rounded sheets, tinted pills with saturated icon tiles, berry-red selection,
 * heavy rounded type, floating cards over a warm scrim. Every screen should be built from these.
 */

// ---------------------------------------------------------------- basics

/** Rounded-square icon button on a neutral tile (back, settings, help). */
@Composable
fun SquareButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, bg: Color = K.Field, tint: Color = K.Ink, size: Dp = 44.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(bg).pressable(onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size * 0.58f))
    }
}

/** Pill segmented control: neutral track, the selected option sits on a lighter chip. */
@Composable
fun MeowSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, fill: Boolean = false, fontSize: androidx.compose.ui.unit.TextUnit = 15.sp) {
    Row(modifier.clip(RoundedCornerShape(16.dp)).background(K.c(0xFFF1EDE2, 0xFF2C312C)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                (if (fill) Modifier.weight(1f) else Modifier).height(36.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (on) K.Green else Color.Transparent).pressable { onSelect(i) }.padding(horizontal = if (fontSize.value < 15f) 10.dp else 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, fontSize = fontSize, fontWeight = FontWeight.SemiBold, color = if (on) K.OnGreen else K.Ink2, maxLines = 1, softWrap = false) }
        }
    }
}

/** Header chip: tinted pill, saturated icon tile, bold label (optionally a small second line). */
@Composable
fun HeaderChip(icon: String, label: String, tone: Tone, modifier: Modifier = Modifier, sub: String? = null, onClick: () -> Unit) {
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(K.Sheet).border(1.5.dp, K.Notch, RoundedCornerShape(24.dp)).pressable(onClick).padding(start = 5.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(K.c(0xFFE6E7E0, 0xFF3A3F3B)), contentAlignment = Alignment.Center) { CatIcon(icon, 26.dp) }
        Spacer(Modifier.width(9.dp))
        Column {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = K.Ink2, maxLines = 1)
        }
    }
}

/** A sticker icon on a rounded tile in the tone's saturated color (or neutral when colorful icons are off). */
@Composable
fun IconTile(icon: String, tone: Tone, size: Dp = 52.dp, modifier: Modifier = Modifier, colorful: Boolean = true) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.34f)).background(if (colorful) tone.base else K.Card),
        contentAlignment = Alignment.Center,
    ) { CatIcon(icon, size * 0.68f) }
}

/** Tiny tile used inside record chips (account, ledger, reimburse markers). */
@Composable
fun MiniTile(icon: String, tone: Tone, size: Dp = 24.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(7.dp)).background(tone.base), contentAlignment = Alignment.Center) { CatIcon(icon, size * 0.86f) }
}

/** Small "three bars" glyph shown next to a balance. */
@Composable
fun BalanceGlyph(color: Color = K.Ink, size: Dp = 16.dp) {
    Canvas(Modifier.size(size)) {
        val h = this.size.height / 5f
        listOf(1f, .78f, .56f).forEachIndexed { i, w ->
            drawRoundRect(color, Offset(0f, i * h * 1.9f), Size(this.size.width * w, h * 1.2f), CornerRadius(h, h))
        }
    }
}

/** "Nov·Pengeluaran ▼ / Nov·Pemasukan ▲" with big values, plus an optional balance line under the left value. */
@Composable
fun StatPair(
    leftTitle: String, leftValue: String, rightTitle: String, rightValue: String,
    modifier: Modifier = Modifier, prefix: String? = null, balance: String? = null,
    leftColor: Color = K.Red, rightColor: Color = K.Ink, leftUp: Boolean = false, rightUp: Boolean = true,
    balanceTrailing: @Composable () -> Unit = {},
) {
    Row(modifier.fillMaxWidth()) {
        StatColumn(prefix, leftTitle, leftValue, leftColor, leftUp, Modifier.weight(1f)) {
            if (balance != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(balance, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = K.Ink, maxLines = 1)
                    Spacer(Modifier.width(5.dp)); BalanceGlyph()
                    balanceTrailing()
                }
            }
        }
        StatColumn(prefix, rightTitle, rightValue, rightColor, rightUp, Modifier.weight(1f)) {}
    }
}

@Composable
private fun StatColumn(prefix: String?, title: String, value: String, color: Color, up: Boolean, modifier: Modifier, extra: @Composable () -> Unit) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildAnnotatedString {
                    if (prefix != null) { append(prefix); withStyle(SpanStyle(color = K.Red)) { append("·") } }
                    append(title)
                },
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            Trend(up, if (up) K.Ink else K.Red, 11.dp)
        }
        Spacer(Modifier.height(2.dp))
        val size = when { value.length > 13 -> 20.sp; value.length > 11 -> 24.sp; else -> 28.sp }
        Text(value, fontSize = size, fontWeight = FontWeight.Bold, color = color, maxLines = 1, letterSpacing = (-0.4).sp)
        Spacer(Modifier.height(2.dp))
        extra()
    }
}

/** Shortcut in the sheet's top row: sticker on a neutral tile, bold label under it. */
@Composable
fun Shortcut(icon: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.width(66.dp).pressable(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        CatIcon(icon, 44.dp)
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.Ink, maxLines = 1)
    }
}

/** Primary (berry red) and secondary (cream) buttons used in forms and dialogs. */
@Composable
fun RedButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.height(50.dp).clip(shape).background(if (enabled) K.Green else K.RedTrack)
            .let { if (enabled) it.pressable(onClick) else it },
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.OnGreen, maxLines = 1) }
}

@Composable
fun CreamButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.height(50.dp).clip(RoundedCornerShape(16.dp)).background(K.c(0xFFFDFBF6, 0xFF252A25)).border(1.5.dp, K.c(0xFF3D3D3B, 0xFF5A605B), RoundedCornerShape(16.dp)).pressable(onClick), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = K.Ink, maxLines = 1)
    }
}

/** Red check button (save) as a rounded square, like the keypad's confirm key. */
@Composable
fun CheckButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 52.dp, enabled: Boolean = true) {
    val shape = RoundedCornerShape(size * 0.32f)
    Box(
        modifier.size(size).clip(shape).background(if (enabled) K.Green else K.RedTrack)
            .let { if (enabled) it.pressable(onClick) else it },
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Check, "Simpan", tint = K.OnGreen, modifier = Modifier.size(size * 0.6f)) }
}

/** Custom switch: berry thumb on a blush track when on, ink thumb on a sand track when off. */
@Composable
fun MeowToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier.width(52.dp).height(32.dp).clickable(remember { MutableInteractionSource() }, null) { onChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(15.dp)).background(if (checked) K.Green else K.c(0xFFD9D2C2, 0xFF3A3F3B)))
        Box(Modifier.offset(x = if (checked) 25.dp else 3.dp).size(24.dp).clip(CircleShape).background(Color.White))
    }
}

/** Settings row: glyph tile, bold title, optional subtitle, chevron or custom trailing. */
@Composable
fun SettingRow(
    icon: ImageVector, title: String, subtitle: String? = null, onClick: (() -> Unit)? = null, divider: Boolean = true,
    trailing: @Composable () -> Unit = { Icon(Icons.Rounded.ChevronRight, null, tint = K.Ink, modifier = Modifier.size(28.dp)) },
) {
    Column(Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(K.Field), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = K.Ink, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink)
                if (subtitle != null) Text(subtitle, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = K.Ink2, lineHeight = 17.sp)
            }
            Spacer(Modifier.width(8.dp))
            trailing()
        }
        if (divider) Box(Modifier.padding(start = 58.dp).fillMaxWidth().height(1.dp).background(K.Line))
    }
}

/** Form row inside a grouped card: glyph tile, label, value and a chevron. */
@Composable
fun FormRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit, valueColor: Color = K.Ink, divider: Boolean = true, leading: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) leading() else Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(K.Field), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = K.Ink, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink, modifier = Modifier.weight(1f))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 190.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            Icon(Icons.Rounded.ChevronRight, null, tint = K.Ink, modifier = Modifier.size(26.dp))
        }
        if (divider) Box(Modifier.padding(start = 58.dp).fillMaxWidth().height(1.dp).background(K.Line))
    }
}

/** Grouped card on a sheet (forms, settings blocks). */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(K.Card).padding(horizontal = 16.dp, vertical = 6.dp), content = content)
}

/** Floating action button: rounded square in berry red (or a tone), white glyph. */
@Composable
fun MeowFab(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, bg: Color = K.Red, size: Dp = 56.dp) {
    Box(
        modifier.size(size).shadow(8.dp, CircleShape, spotColor = bg.copy(alpha = .5f)).clip(CircleShape).background(bg).pressable(onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = K.OnGreen, modifier = Modifier.size(size * 0.5f)) }
}

/** Kanto empty state with a heavy title and a friendly line. */
@Composable
fun EmptyState(title: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Kanto(104.dp, KantoMood.SMILE)
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink)
        Spacer(Modifier.height(4.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/**
 * Text that shrinks (down to [minSize]) until every word fits on a line and the whole text fits in
 * [maxLines], so a long single word ("Transportasi") never breaks in the middle.
 * Uses BoxWithConstraints: do not place it inside an intrinsic-size measurement.
 */
@Composable
fun FitText(
    text: String, modifier: Modifier = Modifier, maxSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    minSize: androidx.compose.ui.unit.TextUnit = 10.sp, maxLines: Int = 2, color: Color = K.Ink, weight: FontWeight = FontWeight.SemiBold,
) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val base = androidx.compose.material3.LocalTextStyle.current
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val maxW = constraints.maxWidth
        val fitted = remember(text, maxW, maxSize, minSize, maxLines) {
            fun style(sz: Float) = base.copy(fontSize = sz.sp, fontWeight = weight, lineHeight = (sz * 1.18f).sp)
            fun width(w: String, sz: Float) = measurer.measure(w, style(sz), softWrap = false, maxLines = 1).size.width
            fun lines(t: String, sz: Float) = measurer.measure(t, style(sz), constraints = androidx.compose.ui.unit.Constraints(maxWidth = maxW.coerceAtLeast(1))).lineCount
            fun fits(t: String, sz: Float) = t.split(' ', '\n').none { w -> w.isNotEmpty() && width(w, sz) > maxW } && lines(t, sz) <= maxLines
            var s = maxSize.value
            while (s > minSize.value && !fits(text, s)) s -= 0.5f
            if (fits(text, s)) return@remember text to s
            // Still too long at the smallest size: hyphenate the long word at a syllable boundary
            // (before a consonant followed by a vowel, e.g. "Trans-portasi"), keeping both halves whole.
            val vowels = "aiueoAIUEO"
            val hyphenated = text.split(' ').joinToString(" ") { w ->
                if (width(w, s) <= maxW) w else {
                    val cut = (w.length - 3 downTo 2).firstOrNull { i ->
                        w[i] !in vowels && w[i + 1] in vowels && width(w.substring(0, i) + "-", s) <= maxW && width(w.substring(i), s) <= maxW
                    }
                    if (cut == null) w else w.substring(0, cut) + "-\n" + w.substring(cut)
                }
            }
            // Last resort (too many lines even with whole words): shrink a little more, never below 9sp.
            var t = s
            while (t > 9f && !fits(hyphenated, t)) t -= 0.5f
            hyphenated to t
        }
        Text(fitted.first, fontSize = fitted.second.sp, lineHeight = (fitted.second * 1.18f).sp, fontWeight = weight, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

/** Haptic tick for keypads and toggles (respects the "Getar keyboard" preference). */
@Composable
fun rememberTick(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { if (Prefs.haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } }
}

// ---------------------------------------------------------------- overlays

/**
 * Overlays are drawn by [OverlayHostView] on top of the whole app, inside the app's own window:
 * the scrim reaches under the status and navigation bars, and there is no system dialog animation.
 */
class OverlayHost { internal val layers = androidx.compose.runtime.mutableStateListOf<OverlayLayer>() }
class OverlayLayer { internal var content: @Composable () -> Unit by mutableStateOf({}) }
val LocalOverlayHost = androidx.compose.runtime.staticCompositionLocalOf<OverlayHost?> { null }

/** Place once, as the last child of the app's root box. */
@Composable
fun OverlayHostView(host: OverlayHost) {
    host.layers.forEach { layer -> androidx.compose.runtime.key(layer) { layer.content() } }
}

@Composable
private fun InAppLayer(content: @Composable () -> Unit) {
    val host = LocalOverlayHost.current ?: run { content(); return }
    val layer = remember { OverlayLayer() }
    androidx.compose.runtime.SideEffect { layer.content = content }
    androidx.compose.runtime.DisposableEffect(host) {
        host.layers.add(layer)
        onDispose { host.layers.remove(layer) }
    }
}

/**
 * Full-screen overlay with a warm scrim. Content is placed at the bottom (sheets, cards) unless
 * [center] is set. Back and tapping the scrim call [onDismiss].
 */
@Composable
fun Overlay(onDismiss: () -> Unit, center: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    InAppLayer {
        androidx.activity.compose.BackHandler(onBack = onDismiss)
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(K.Scrim).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
            Box(
                Modifier.fillMaxSize().imePadding().let { if (center) it.statusBarsPadding().navigationBarsPadding().padding(16.dp) else it.statusBarsPadding() },
                contentAlignment = if (center) Alignment.Center else Alignment.BottomCenter, content = content,
            )
        }
    }
}

/** Bottom sheet card used inside [Overlay]: rounded top, little notch, cream background. */
@Composable
fun SheetCard(modifier: Modifier = Modifier, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier.fillMaxWidth().heightIn(max = 720.dp).clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)).background(K.Bg)
            .clickable(remember { MutableInteractionSource() }, null) {}.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp + navBottom),
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(K.Sheet).let { if (scroll) it.verticalScroll(rememberScrollState()) else it }
                .padding(horizontal = 18.dp, vertical = 20.dp),
            content = content,
        )
        Notch(Modifier.align(Alignment.TopCenter).offset(y = (-12).dp))
    }
}

/** The small tab on the top edge of sheets. */
@Composable
fun Notch(modifier: Modifier = Modifier) {
    Box(modifier.width(84.dp).height(14.dp).clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)).background(K.Notch))
}

/** Floating card (detail, small dialogs): cream with a sand ring, floating above the bottom edge. */
@Composable
fun FloatingCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier.padding(start = 16.dp, end = 16.dp, bottom = 28.dp + navBottom).fillMaxWidth()
            .clip(RoundedCornerShape(36.dp)).background(K.Sheet).border(7.dp, K.Notch, RoundedCornerShape(36.dp))
            .clickable(remember { MutableInteractionSource() }, null) {}.padding(horizontal = 22.dp, vertical = 22.dp),
        content = content,
    )
}

/** One option in a pill menu: tinted pill with icon tile; the selected one turns berry red with a check. */
@Composable
fun PillOption(label: String, tone: Tone, selected: Boolean, onClick: () -> Unit, icon: String? = null, sub: String? = null, glyph: ImageVector? = null) {
    Row(
        Modifier.fillMaxWidth().height(if (sub != null) 58.dp else 52.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) K.Yellow else Color.Transparent).pressable(onClick).padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            icon != null -> Box(Modifier.size(36.dp).clip(CircleShape).background(K.c(0xFFE6E7E0, 0xFF3A3F3B)), contentAlignment = Alignment.Center) { CatIcon(icon, 24.dp) }
            glyph != null -> Box(Modifier.size(36.dp).clip(CircleShape).background(K.c(0xFFF1EDE2, 0xFF2C312C)), contentAlignment = Alignment.Center) { Icon(glyph, null, tint = K.Green, modifier = Modifier.size(20.dp)) }
            else -> Spacer(Modifier.width(6.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (glyph != null) K.Green else K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1)
        }
        if (selected) Icon(Icons.Rounded.Check, null, tint = K.Green, modifier = Modifier.size(22.dp))
    }
}

/**
 * Pill menu that floats next to [anchor] (window bounds of the tapped element) over a warm scrim.
 * Build options with [PillOption]. A [title] gets its first letter in red.
 */
@Composable
fun PillMenu(anchor: Rect?, onDismiss: () -> Unit, width: Dp = 236.dp, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    InAppLayer {
        androidx.activity.compose.BackHandler(onBack = onDismiss)
        val density = LocalDensity.current
        val marginPx = with(density) { 12.dp.roundToPx() }
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(K.Scrim).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
            Layout(content = {
                Column(
                    Modifier.width(width).heightIn(max = 520.dp).clip(RoundedCornerShape(22.dp)).background(K.Sheet)
                        .clickable(remember { MutableInteractionSource() }, null) {}
                        .verticalScroll(rememberScrollState()).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (title != null) { Box(Modifier.padding(start = 8.dp, top = 6.dp, bottom = 2.dp)) { RedFirst(title, 17.sp) } }
                    content()
                }
            }) { measurables, constraints ->
                val p = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
                val w = constraints.maxWidth; val h = constraints.maxHeight
                val a = anchor ?: Rect(w / 2f, h / 2f, w / 2f, h / 2f)
                // Horizontally: start at the anchor's left edge when it sits on the left half, else end at its right edge.
                val x = (if (a.center.x < w / 2f) a.left.toInt() else a.right.toInt() - p.width).coerceIn(marginPx, (w - p.width - marginPx).coerceAtLeast(marginPx))
                // Vertically: below the anchor if it fits, otherwise above it, otherwise centered.
                val below = a.bottom.toInt() + marginPx
                val above = a.top.toInt() - marginPx - p.height
                val y = when {
                    below + p.height <= h - marginPx -> below
                    above >= marginPx -> above
                    else -> ((h - p.height) / 2).coerceAtLeast(marginPx)
                }
                layout(w, h) { p.place(IntOffset(x, y)) }
            }
        }
    }
}

/** Text whose first letter is berry red (section and dialog titles). */
@Composable
fun RedFirst(text: String, size: androidx.compose.ui.unit.TextUnit = 20.sp, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString { withStyle(SpanStyle(color = K.Red)) { append(text.take(1)) }; append(text.drop(1)) },
        fontSize = size, fontWeight = FontWeight.Bold, color = K.Ink, modifier = modifier,
    )
}

/** Remembers the window bounds of an element so a [PillMenu] can float next to it. */
class Anchor { var rect by mutableStateOf<Rect?>(null) }

@Composable
fun rememberAnchor() = remember { Anchor() }

fun Modifier.anchor(a: Anchor): Modifier = onGloballyPositioned { a.rect = it.boundsInWindow() }

// ---------------------------------------------------------------- pickers

private val WEEKDAYS = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

/**
 * "Pilih tanggal" sheet, styled like the spending calendar: quick chips (Hari ini / Kemarin / 2 hari lalu),
 * a cream month card where each day is tinted by that day's spending (when [d] is given), days of the
 * previous/next month drawn with a dashed border (tap one to jump there), then the time chip, Batal and "Pilih d MMM".
 */
@Composable
fun CalendarCard(initial: Long, onDismiss: () -> Unit, withTime: Boolean = true, d: id.kantong.app.data.AppData? = null, onConfirm: (Long) -> Unit) {
    var picked by remember { mutableLongStateOf(initial) }
    var shown by remember { mutableLongStateOf(initial) } // any time inside the month on screen
    var showTimePicker by remember { mutableStateOf(false) }
    fun sameDay(a: Long, b: Long): Boolean {
        val x = Calendar.getInstance().apply { timeInMillis = a }; val y = Calendar.getInstance().apply { timeInMillis = b }
        return x.get(Calendar.YEAR) == y.get(Calendar.YEAR) && x.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR)
    }
    /** [picked]'s clock time moved onto the day of [day]. */
    fun onDay(day: Long): Long {
        val t = Calendar.getInstance().apply { timeInMillis = picked }
        return Calendar.getInstance().apply {
            timeInMillis = day
            set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY)); set(Calendar.MINUTE, t.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val dayMs = 86_400_000L
    val todayStart = id.kantong.app.data.startOfDay()
    val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f))

    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader("Pilih tanggal", onDismiss, size = 21)
            Text(if (d != null) "Warna = pengeluaran hari itu" else "Ketuk tanggal", fontSize = 13.sp, color = K.Muted)

            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Hari ini" to 0, "Kemarin" to 1, "2 hari lalu" to 2).forEach { (label, back) ->
                    val day = todayStart - back * dayMs
                    val on = sameDay(picked, day)
                    Box(
                        Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (on) K.Green else K.Field)
                            .pressable { picked = onDay(day); shown = day }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (on) K.OnGreen else K.Ink2) }
                }
            }

            val first = Calendar.getInstance().apply { timeInMillis = shown; set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            val firstMs = first.timeInMillis
            val lead = (first.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            val days = first.getActualMaximum(Calendar.DAY_OF_MONTH)
            val cells = ((lead + days + 6) / 7) * 7
            val spent = remember(firstMs, d) {
                LongArray(days).also { arr ->
                    d?.txns?.forEach { t ->
                        if (t.kind == "out" && t.time >= firstMs) {
                            val c = Calendar.getInstance().apply { timeInMillis = t.time }
                            if (c.get(Calendar.YEAR) == first.get(Calendar.YEAR) && c.get(Calendar.MONTH) == first.get(Calendar.MONTH)) arr[c.get(Calendar.DAY_OF_MONTH) - 1] += t.amount
                        }
                    }
                }
            }
            val peak = spent.maxOrNull() ?: 0L
            fun step(by: Int) { shown = Calendar.getInstance().apply { timeInMillis = firstMs; add(Calendar.MONTH, by) }.timeInMillis }

            Column(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(26.dp)).background(K.Card).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CRoundButton(Icons.Rounded.ChevronLeft, "Bulan sebelumnya", { step(-1) }, size = 34.dp, outlined = true)
                    Text(fmt(firstMs, "MMMM yyyy"), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
                    CRoundButton(Icons.Rounded.ChevronRight, "Bulan berikutnya", { step(1) }, size = 34.dp, outlined = true)
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    WEEKDAYS.forEach { w -> Text(w, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = K.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(6.dp))
                (0 until cells).chunked(7).forEach { week ->
                    Row(Modifier.padding(bottom = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        week.forEach { cell ->
                            val dayMsOf = Calendar.getInstance().apply { timeInMillis = firstMs; add(Calendar.DAY_OF_MONTH, cell - lead) }.timeInMillis
                            val dayNum = Calendar.getInstance().apply { timeInMillis = dayMsOf }.get(Calendar.DAY_OF_MONTH)
                            val inMonth = cell - lead in 0 until days
                            val future = dayMsOf > todayStart
                            val on = sameDay(picked, dayMsOf)
                            val level = if (!inMonth || peak <= 0L) 0 else spent[cell - lead].let { v -> if (v <= 0L) 0 else ((v * 4 + peak - 1) / peak).toInt().coerceIn(1, 4) }
                            val shape = RoundedCornerShape(11.dp)
                            Box(
                                Modifier.weight(1f).height(38.dp).let { m ->
                                    when {
                                        !inMonth -> m.drawBehind {
                                            drawRoundRect(K.YellowLine, cornerRadius = androidx.compose.ui.geometry.CornerRadius(11.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.4.dp.toPx(), pathEffect = dash))
                                        }
                                        future -> m.clip(shape).background(K.Tile)
                                        else -> m.clip(shape).background(K.Heat[level])
                                    }
                                }.let { m -> if (on) m.border(2.5.dp, K.Ink, shape) else m }
                                    .clip(shape).pressable { picked = onDay(dayMsOf); if (!inMonth) shown = dayMsOf },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$dayNum", fontSize = 13.5.sp, fontWeight = if (inMonth) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = when { !inMonth || future -> K.Muted.copy(alpha = 0.6f); level >= 3 -> K.OnGreen; else -> K.Ink },
                                )
                            }
                        }
                    }
                }
                if (d != null) Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Sedikit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = K.Muted)
                    K.Heat.forEach { Box(Modifier.padding(start = 4.dp).size(16.dp, 9.dp).clip(RoundedCornerShape(3.dp)).background(it)) }
                    Text("Banyak", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = K.Muted, modifier = Modifier.padding(start = 4.dp).weight(1f))
                    Box(Modifier.size(16.dp, 11.dp).drawBehind {
                        drawRoundRect(K.YellowLine, cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.4.dp.toPx(), pathEffect = dash))
                    })
                    Text(" bulan lain", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = K.Muted)
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (withTime) {
                    Row(
                        Modifier.height(50.dp).clip(RoundedCornerShape(16.dp)).background(K.SageCard).pressable { showTimePicker = true }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Schedule, "Ubah jam", tint = K.Green, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(fmt(picked, "HH.mm"), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Green)
                    }
                }
                CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                RedButton("Pilih " + fmt(picked, "d MMM"), Modifier.weight(1.2f)) { onConfirm(picked) }
            }
        }
    }
    if (showTimePicker) {
        val pc = Calendar.getInstance().apply { timeInMillis = picked }
        TimePickerCard(
            initialHour = pc.get(Calendar.HOUR_OF_DAY),
            initialMinute = pc.get(Calendar.MINUTE),
            onDismiss = { showTimePicker = false },
        ) { h, m ->
            picked = Calendar.getInstance().apply {
                timeInMillis = picked
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
            }.timeInMillis
            showTimePicker = false
        }
    }
}

/**
 * Report period picker: Mingguan / Bulanan / Tahunan, month grid (or weeks of the month), year stepper.
 * Returns the mode ("week" | "month" | "year") and an anchor time inside the chosen period.
 */
@Composable
fun PeriodCard(mode: String, anchor: Long, onDismiss: () -> Unit, onConfirm: (String, Long) -> Unit) {
    var m by remember { mutableStateOf(mode) }
    val start = Calendar.getInstance().apply { timeInMillis = anchor }
    var year by remember { mutableIntStateOf(start.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(start.get(Calendar.MONTH)) }
    var week by remember { mutableIntStateOf((start.get(Calendar.DAY_OF_MONTH) - 1) / 7) }
    var page by remember { mutableIntStateOf(maxOf(start.get(Calendar.YEAR), Calendar.getInstance().get(Calendar.YEAR))) } // last year shown in the year grid
    val now = Calendar.getInstance()
    val nowYear = now.get(Calendar.YEAR); val nowMonth = now.get(Calendar.MONTH)
    val full = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    val days = Calendar.getInstance().apply { clear(); set(year, month, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
    val weeks = (days + 6) / 7
    val w = week.coerceAtMost(weeks - 1)

    @Composable
    fun Cell(label: String, on: Boolean, dim: Boolean, modifier: Modifier, onClick: () -> Unit) {
        Box(
            modifier.height(40.dp).let { mm ->
                if (dim && !on) mm.drawBehind {
                    drawRoundRect(K.YellowLine, cornerRadius = androidx.compose.ui.geometry.CornerRadius(13.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.4.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))))
                } else mm.clip(RoundedCornerShape(13.dp)).background(if (on) K.Green else K.Tile)
            }.clip(RoundedCornerShape(13.dp)).pressable(onClick),
            contentAlignment = Alignment.Center,
        ) { Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = if (on) K.OnGreen else if (dim) K.Muted.copy(alpha = 0.7f) else K.Ink2, maxLines = 1) }
    }

    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader("Periode laporan", onDismiss, size = 21)
            MeowSegmented(
                listOf("Minggu", "Bulan", "Tahun"), listOf("week", "month", "year").indexOf(m),
                { m = listOf("week", "month", "year")[it] }, Modifier.padding(top = 14.dp).fillMaxWidth(), fill = true, fontSize = 14.sp,
            )
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                CRoundButton(Icons.Rounded.ChevronLeft, if (m == "year") "Sebelumnya" else "Tahun sebelumnya", { if (m == "year") page -= 6 else year-- }, size = 36.dp)
                Text(
                    if (m == "year") "${page - 5} – $page" else "$year", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f),
                )
                CRoundButton(Icons.Rounded.ChevronRight, if (m == "year") "Berikutnya" else "Tahun berikutnya", { if (m == "year") page += 6 else year++ }, size = 36.dp)
            }
            Column(
                Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(22.dp)).background(K.Card).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (m == "year") {
                    (page - 5..page).chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { y -> Cell("$y", y == year, y > nowYear, Modifier.weight(1f)) { year = y } }
                        }
                    }
                } else {
                    (0..11).chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { i -> Cell(full[i].take(3), i == month, year > nowYear || (year == nowYear && i > nowMonth), Modifier.weight(1f)) { month = i } }
                        }
                    }
                }
            }
            if (m == "week") {
                Text("Minggu ke", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink2, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.Card).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0 until weeks).forEach { i -> Cell("${i * 7 + 1}–${minOf(days, i * 7 + 7)}", i == w, false, Modifier.weight(1f)) { week = i } }
                }
            }
            val label = when (m) {
                "year" -> "Lihat $year"
                "week" -> "Lihat ${w * 7 + 1}–${minOf(days, w * 7 + 7)} ${full[month].take(3)}"
                else -> "Lihat ${full[month]} $year"
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                RedButton(label, Modifier.weight(1.6f)) {
                    val day = if (m == "week") w * 7 + 1 else 1
                    val chosen = Calendar.getInstance().apply {
                        clear()
                        set(year, if (m == "year") 0 else month, day, 12, 0, 0)
                    }.timeInMillis
                    onConfirm(m, chosen)
                }
            }
        }
    }
}
