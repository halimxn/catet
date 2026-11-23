package id.kantong.app.ui.screens

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.ui.draw.alpha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.Account
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Goal
import id.kantong.app.data.Prefs
import id.kantong.app.data.Txn
import id.kantong.app.data.fmt
import id.kantong.app.data.grouped
import id.kantong.app.data.rupiah
import id.kantong.app.data.short
import id.kantong.app.data.startOfDay
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CChip
import id.kantong.app.ui.components.CGroupLabel
import id.kantong.app.ui.components.CList
import id.kantong.app.ui.components.CListRow
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CSection
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.MeowSegmented
import id.kantong.app.ui.components.PillMenu
import id.kantong.app.ui.components.PillOption
import id.kantong.app.ui.components.ProgressBar
import id.kantong.app.ui.components.accountIcon
import id.kantong.app.ui.components.anchor
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.components.rememberAnchor
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private const val DAY_MS = 86_400_000L

/** Chart windows of the "Rentang waktu" menu (shared with the account detail panel). */
internal val ASSET_RANGE_DAYS = listOf(30, 90, 365)
internal val ASSET_RANGE_LABELS = listOf("1 bulan terakhir", "3 bulan terakhir", "1 tahun terakhir")
private val ASSET_RANGE_CHIPS = listOf("1 bulan", "3 bulan", "1 tahun")

/** "−809.025" with a real minus sign, "0", "60.024.751". */
internal fun assetAmount(v: Long): String = if (v < 0) "−" + abs(v).grouped() else v.grouped()

/** One chart series: a value per point, from the start of the first day to the end of today. */
internal class AssetSeries(val values: List<Long>, val from: Long, val to: Long)

/**
 * Builds the chart series over the last [days] days (more than 120 days -> weekly points).
 * mode 0 (Bersih): balance at the end of each point, walking back from [current] using [effect];
 * mode 1 (Keluar) / 2 (Masuk): sum of out / in records per point.
 */
internal fun assetChartSeries(mode: Int, days: Int, current: Long, txns: List<Txn>, effect: (Txn) -> Long): AssetSeries {
    val step = if (days > 120) 7 else 1
    val count = days / step
    val span = step * DAY_MS
    val end = startOfDay() + DAY_MS // end of today
    val from = end - count * span
    val buckets = LongArray(count)
    var after = 0L // records dated after today (still part of the current balance)
    txns.forEach { t ->
        when {
            t.time >= end -> if (mode == 0) after += effect(t)
            t.time >= from -> {
                val i = ((t.time - from) / span).toInt().coerceIn(0, count - 1)
                buckets[i] += when (mode) {
                    0 -> effect(t)
                    1 -> if (t.kind == "out") t.amount else 0L
                    else -> if (t.kind == "in") t.amount else 0L
                }
            }
        }
    }
    val values = if (mode == 0) {
        val v = LongArray(count)
        var bal = current - after
        for (i in count - 1 downTo 0) { v[i] = bal; bal -= buckets[i] }
        v.toList()
    } else buckets.toList()
    return AssetSeries(values, from, end - 1)
}

/** Effect of one record on the total of all accounts (transfers only lose their fee). */
private fun netEffect(t: Txn): Long = when (t.kind) {
    "in" -> t.amount
    "out" -> -t.amount
    else -> -t.fee
}

/**
 * Chart card: title + period chip, the headline value with a change / average chip, the
 * "Bersih / Keluar / Masuk" segmented and the chart. [current] is the headline for Bersih.
 * [title] overrides the mode title (used by the account detail panel for the account name).
 */
@Composable
internal fun AssetChartCard(
    series: AssetSeries, current: Long, mode: Int, onMode: (Int) -> Unit, range: Int, onRange: (Int) -> Unit,
    modifier: Modifier = Modifier, title: String? = null, chartHeight: Dp = 170.dp,
    bg: Color = K.Field, chipBg: Color = K.Sheet, padding: Dp = 16.dp, leading: (@Composable () -> Unit)? = null,
) {
    val rangeAnchor = rememberAnchor()
    var menu by remember { mutableStateOf(false) }
    val values = series.values
    val weekly = values.size > 0 && (series.to + 1 - series.from) / values.size > DAY_MS
    val heading = title ?: when (mode) {
        0 -> "Kekayaan bersih"
        1 -> if (weekly) "Pengeluaran mingguan" else "Pengeluaran harian"
        else -> if (weekly) "Pemasukan mingguan" else "Pemasukan harian"
    }
    val headline: String
    val headColor: Color
    val chipText: String
    val chipColor: Color
    if (mode == 0) {
        headline = assetAmount(current)
        headColor = if (current < 0) K.Danger else K.Ink
        val delta = (values.lastOrNull() ?: 0L) - (values.firstOrNull() ?: 0L)
        val since = fmt(series.from, "d MMM")
        when {
            delta > 0 -> { chipText = "+${delta.short()} sejak $since"; chipColor = Tone.MINT.base }
            delta < 0 -> { chipText = "${delta.short()} sejak $since"; chipColor = Tone.CORAL.base }
            else -> { chipText = "Tetap sejak $since"; chipColor = chipBg }
        }
    } else {
        val total = values.sum()
        val days = ((series.to + 1 - series.from) / DAY_MS).coerceAtLeast(1L)
        headline = total.grouped()
        headColor = K.Ink
        chipText = "rata-rata ${(total / days).short()}/hari"
        chipColor = chipBg
    }

    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(bg).padding(padding)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Text(
                heading, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier.anchor(rangeAnchor).height(36.dp).clip(RoundedCornerShape(18.dp)).background(chipBg)
                    .pressable { menu = true }.padding(start = 14.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(ASSET_RANGE_CHIPS[range], fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink, maxLines = 1, softWrap = false)
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Rentang waktu", tint = K.Ink, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(headline, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = headColor, maxLines = 1, softWrap = false)
            Spacer(Modifier.width(8.dp))
            Text(
                chipText, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, color = K.Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).clip(RoundedCornerShape(12.dp)).background(chipColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        MeowSegmented(listOf("Bersih", "Keluar", "Masuk"), mode, onMode, Modifier.fillMaxWidth(), fill = true)
        Spacer(Modifier.height(16.dp))
        AssetChart(series, mode, chartHeight)
    }
    if (menu) {
        PillMenu(rangeAnchor.rect, { menu = false }, title = "Rentang waktu") {
            ASSET_RANGE_LABELS.forEachIndexed { i, label ->
                PillOption(label, Tone.PEACH, i == range, { onRange(i); menu = false })
            }
        }
    }
}

private val CHART_TOP = 8.dp
private val CHART_BOTTOM = 4.dp
private val CHART_GUTTER = 56.dp

/** Point positions of the chart area (shared by drawing, touch and the overlays). */
private class ChartGeo(
    val w: Float, val top: Float, val base: Float, val padX: Float,
    val n: Int, val bars: Boolean, val lo: Long, val hi: Long,
) {
    fun x(i: Int): Float = when {
        bars -> w * (i + .5f) / n
        n <= 1 -> w / 2f
        else -> padX + i * (w - 2 * padX) / (n - 1)
    }
    fun y(v: Long): Float = top + (base - top) * (1f - (v - lo).toFloat() / (hi - lo).toFloat())
    fun nearest(px: Float): Int {
        if (n <= 1) return 0
        val i = if (bars) (px / w * n).toInt() else ((px - padX) / ((w - 2 * padX) / (n - 1))).roundToInt()
        return i.coerceIn(0, n - 1)
    }
}

private fun Density.chartGeo(w: Float, h: Float, n: Int, bars: Boolean, lo: Long, hi: Long) =
    ChartGeo(w, CHART_TOP.toPx(), h - CHART_BOTTOM.toPx(), (if (bars) 0.dp else 6.dp).toPx(), n, bars, lo, hi)

/** Smallest "round" value (1, 1.5, 2, 2.5, 3, 4, 5, 6, 8 x 10^k) that is at least [v]. */
private fun niceCeil(v: Long): Long {
    if (v <= 0L) return 0L
    var m = 1L
    while (m <= v / 10) m *= 10
    for (c in longArrayOf(10, 15, 20, 25, 30, 40, 50, 60, 80, 100)) {
        val s = m * c / 10
        if (s >= v) return s
    }
    return m * 10
}

/** Axis bounds: 0 is always inside, both ends rounded. */
private fun chartScale(values: List<Long>): Pair<Long, Long> {
    val lo = -niceCeil(maxOf(0L, -(values.minOrNull() ?: 0L)))
    var hi = niceCeil(maxOf(0L, values.maxOrNull() ?: 0L))
    if (hi == lo) hi = lo + 100_000L
    return lo to hi
}

/**
 * Bersih: smooth area line with the last point highlighted; Keluar / Masuk: bars per day (per week for 1 year).
 * Dashed grid with short labels on the right, three dates underneath. Tap or drag for a guide line + tooltip.
 */
@Composable
private fun AssetChart(series: AssetSeries, mode: Int, height: Dp) {
    val values = series.values
    val n = values.size
    if (n == 0) return
    val bars = mode != 0
    val color = when (mode) { 0 -> Tone.SKY.strong; 1 -> Tone.CORAL.strong; else -> Tone.MINT.strong }
    val (lo, hi) = remember(values) { chartScale(values) }
    var sel by remember(values, mode) { mutableIntStateOf(-1) }
    val span = (series.to + 1 - series.from) / n

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier.weight(1f).height(height)
                    .pointerInput(values, mode) {
                        detectTapGestures { p ->
                            val i = chartGeo(size.width.toFloat(), size.height.toFloat(), n, bars, lo, hi).nearest(p.x)
                            sel = if (sel == i) -1 else i
                        }
                    }
                    .pointerInput(values, mode) {
                        detectHorizontalDragGestures(
                            onDragStart = { p -> sel = chartGeo(size.width.toFloat(), size.height.toFloat(), n, bars, lo, hi).nearest(p.x) },
                        ) { change, _ ->
                            change.consume()
                            sel = chartGeo(size.width.toFloat(), size.height.toFloat(), n, bars, lo, hi).nearest(change.position.x)
                        }
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val g = chartGeo(size.width, size.height, n, bars, lo, hi)
                    val w = size.width
                    // dashed grid: top, middle, bottom
                    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                    for (k in 0..2) {
                        val gy = g.top + (g.base - g.top) * k / 2f
                        drawLine(K.Ink.copy(alpha = .16f), Offset(0f, gy), Offset(w, gy), 1.dp.toPx(), pathEffect = dash)
                    }
                    if (sel in 0 until n) {
                        val gx = g.x(sel)
                        drawLine(K.Ink.copy(alpha = .4f), Offset(gx, g.top), Offset(gx, g.base), 1.5.dp.toPx(), StrokeCap.Round)
                    }
                    if (bars) {
                        val slot = w / n
                        val bw = min(slot * .62f, 14.dp.toPx())
                        val r = min(bw / 2f, 4.dp.toPx())
                        val zero = g.y(0L)
                        values.forEachIndexed { i, v ->
                            if (v > 0) {
                                val cx = g.x(i)
                                val yTop = min(g.y(v), zero - 2.dp.toPx())
                                val bar = Path().apply {
                                    addRoundRect(
                                        RoundRect(
                                            Rect(cx - bw / 2f, yTop, cx + bw / 2f, zero),
                                            topLeft = CornerRadius(r), topRight = CornerRadius(r),
                                        ),
                                    )
                                }
                                drawPath(bar, if (sel >= 0 && sel != i) color.copy(alpha = .45f) else color)
                            }
                        }
                    } else {
                        val pts = values.mapIndexed { i, v -> Offset(g.x(i), g.y(v)) }
                        fun Path.curve() {
                            moveTo(pts[0].x, pts[0].y)
                            for (i in 1 until pts.size) {
                                val a = pts[i - 1]; val b = pts[i]
                                val mx = (a.x + b.x) / 2f
                                cubicTo(mx, a.y, mx, b.y, b.x, b.y)
                            }
                        }
                        val line = Path().apply { curve() }
                        val zero = g.y(0L)
                        val area = Path().apply {
                            curve()
                            lineTo(pts.last().x, zero)
                            lineTo(pts.first().x, zero)
                            close()
                        }
                        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = .35f), color.copy(alpha = 0f)), startY = g.top, endY = zero))
                        drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        val p = pts[if (sel in 0 until n) sel else n - 1]
                        drawCircle(Color.White, 5.dp.toPx(), p)
                        drawCircle(color, 5.dp.toPx(), p, style = Stroke(3.dp.toPx()))
                    }
                }
                if (sel in 0 until n) {
                    val i = sel
                    val start = series.from + i * span
                    val date = if (span > DAY_MS) "${fmt(start, "d MMM")} – ${fmt(start + span - 1, "d MMM")}" else fmt(start, "d MMM yyyy")
                    PinAt({ w, h -> val g = chartGeo(w, h, n, bars, lo, hi); Offset(g.x(i), g.y(if (bars) maxOf(values[i], 0L) else values[i])) }) {
                        Column(Modifier.clip(RoundedCornerShape(12.dp)).background(K.Ink).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(date, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = .75f), maxLines = 1, softWrap = false)
                            Text(assetAmount(values[i]), fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
                        }
                    }
                } else if (!bars) {
                    PinAt({ w, h -> val g = chartGeo(w, h, n, false, lo, hi); Offset(g.x(n - 1), g.y(values[n - 1])) }) {
                        Text(
                            values[n - 1].short(), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = Color.White,
                            maxLines = 1, softWrap = false,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(K.Ink).padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            // grid labels on the right
            Box(Modifier.width(CHART_GUTTER).height(height)) {
                listOf(hi, (hi + lo) / 2, lo).forEachIndexed { k, v ->
                    Text(
                        v.short(), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, color = K.Muted,
                        maxLines = 1, softWrap = false, textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().offset(y = CHART_TOP + (height - CHART_TOP - CHART_BOTTOM) * (k / 2f) - 8.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(end = CHART_GUTTER)) {
            val axis = listOf(series.from, series.from + (series.to - series.from) / 2, series.to)
            axis.forEachIndexed { k, t ->
                if (k > 0) Spacer(Modifier.weight(1f))
                Text(fmt(t, "d MMM"), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** Places [content] centered on the point returned by [at] (in px), above it if there is room, else below. */
@Composable
private fun PinAt(at: Density.(Float, Float) -> Offset, content: @Composable () -> Unit) {
    Layout(content, Modifier.fillMaxSize()) { measurables, c ->
        val p = measurables.first().measure(c.copy(minWidth = 0, minHeight = 0))
        val pt = at(this, c.maxWidth.toFloat(), c.maxHeight.toFloat())
        val gap = 10.dp.toPx()
        val x = (pt.x - p.width / 2f).roundToInt().coerceIn(0, (c.maxWidth - p.width).coerceAtLeast(0))
        val above = pt.y - gap - p.height
        val y = (if (above >= 0f) above else pt.y + gap).roundToInt().coerceIn(0, (c.maxHeight - p.height).coerceAtLeast(0))
        layout(c.maxWidth, c.maxHeight) { p.place(x, y) }
    }
}

/** One account group of the Aset tab: label, subtotal and its accounts. */
private class AccountGroup(val label: String, val accounts: List<Pair<Account, Long>>) {
    val total: Long get() = accounts.sumOf { it.second }
}

/** Tunai & e-wallet, Bank, Lainnya, then every account with a negative balance (credit cards, loans). */
private fun accountGroups(d: AppData): List<AccountGroup> {
    val all = d.accounts.map { it to d.balanceOf(it) }
    val (debt, rest) = all.partition { it.second < 0 }
    return listOf(
        AccountGroup("Tunai & e-wallet", rest.filter { it.first.type == "cash" || it.first.type == "ewallet" }),
        AccountGroup("Bank", rest.filter { it.first.type == "bank" }),
        AccountGroup("Lainnya", rest.filter { it.first.type !in setOf("cash", "ewallet", "bank") }),
        AccountGroup("Kartu kredit & utang", debt),
    ).filter { it.accounts.isNotEmpty() }
}

private fun accountTypeLabel(a: Account): String = when (a.type) {
    "cash" -> "Tunai"
    "ewallet" -> "E-wallet"
    "bank" -> "Bank"
    else -> "Akun"
}

/** "Rp25.300.000", "−Rp2.450.000", or dots when amounts are hidden. */
private fun money(v: Long, hidden: Boolean): String = if (hidden) (if (v < 0) "−" else "") + "Rp•••••" else v.rupiah()

/**
 * Aset tab: title with transfer + add account, the yellow net-worth card (eye hides amounts),
 * accounts grouped by type in cream lists, then the net-worth / spending / income chart and the savings goals.
 */
@Composable
fun AssetsScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    var ledgerMenu by remember { mutableStateOf(false) }
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var range by rememberSaveable { mutableIntStateOf(0) }
    val ledgerAnchor = rememberAnchor()
    val ledgerId = Prefs.ledger
    val series = remember(d, mode, range, ledgerId) {
        assetChartSeries(mode, ASSET_RANGE_DAYS[range], d.totalBalance, if (mode == 0) d.allTxns else d.forLedger(ledgerId).txns, ::netEffect)
    }
    val hidden = Prefs.hideAmounts
    val cash = d.totalBalance
    val invest = if (Prefs.showInvest) d.investValue else 0L
    val net = cash + invest + d.receivable - d.payable
    val groups = remember(d) { accountGroups(d) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        CTitle("Aset") {
            CChip("Transfer", icon = "transfer", border = true, height = 40.dp) { vm.startAdd("transfer"); nav.go(R.add()) }
            Spacer(Modifier.width(10.dp))
            CRoundButton(Icons.Rounded.Add, "Tambah akun", { nav.go(R.accountEdit()) }, size = 40.dp, outlined = true)
        }
        Spacer(Modifier.height(14.dp))

        // net worth
        CCard(K.Yellow, radius = 28.dp, padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).pressable { Prefs.hideAmounts = !hidden },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Kekayaan bersih", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
                Spacer(Modifier.width(7.dp))
                Icon(
                    if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (hidden) "Tampilkan nominal" else "Sembunyikan nominal",
                    tint = K.Ink2, modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Money(net, 44.sp, color = if (net < 0) K.Danger else K.Green, hidden = hidden)
            if (net > 0 && invest > 0) Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (cash > 0) Box(Modifier.weight(cash.toFloat()).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(K.Gold))
                Box(Modifier.weight(invest.toFloat()).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(K.SageMid))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NetBox("Uang", K.Gold, money(cash, hidden), Modifier.weight(1f))
                NetBox("Investasi", K.SageMid, if (Prefs.showInvest) money(d.investValue, hidden) else "disembunyikan", Modifier.weight(1f), dim = !Prefs.showInvest)
            }
            if (d.liabilitiesTotal < 0) Text(
                "Termasuk utang ${money(abs(d.liabilitiesTotal), hidden)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow,
                modifier = Modifier.padding(top = 8.dp, start = 2.dp),
            )
        }

        // money
        Row(Modifier.fillMaxWidth().padding(top = 24.dp, start = 2.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            CatIcon("wallet", 26.dp)
            Spacer(Modifier.width(8.dp))
            Text("Uang", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.weight(1f))
            Text(money(cash, hidden), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink2)
        }
        if (d.accounts.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Belum ada akun. Ketuk tombol + untuk menambah dompet, rekening atau e-wallet.",
                fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = K.Muted,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.Card)
                    .pressable { nav.go(R.accountEdit()) }.padding(16.dp),
            )
        }
        groups.forEachIndexed { gi, g ->
            Spacer(Modifier.height(if (gi == 0) 10.dp else 16.dp))
            CGroupLabel(g.label, right = money(g.total, hidden), rightColor = if (g.total < 0) K.Danger else K.Muted)
            Spacer(Modifier.height(8.dp))
            CList {
                g.accounts.forEachIndexed { i, (a, bal) ->
                    CListRow(
                        accountIcon(a), a.name, a.remark.ifBlank { accountTypeLabel(a) }, divider = i > 0,
                        onClick = { nav.go(R.account(a.id)) },
                    ) {
                        Text(
                            money(bal, hidden), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (bal < 0) K.Danger else K.Ink, maxLines = 1, softWrap = false,
                        )
                    }
                }
            }
        }

        // debts summary
        Row(
            Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.Card).pressable { nav.go(R.DEBTS) }.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CatIcon("card", 30.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Utang & piutang", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                val overdue = d.debtStates.count { it.overdueDays > 0 }
                Text(
                    if (d.receivable == 0L && d.payable == 0L) "Catat pinjaman atau patungan" else "Piutang ${money(d.receivable, hidden)} · utang ${money(d.payable, hidden)}" + if (overdue > 0) " · $overdue lewat" else "",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (overdue > 0) K.Danger else K.Muted, maxLines = 1,
                )
            }
            Text("›", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = K.Muted)
        }

        // investments
        InvestSection(d, vm, nav, hidden)

        // chart
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Grafik", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            val current = d.ledger(ledgerId)
            CChip(
                current?.name ?: "Semua buku", Modifier.anchor(ledgerAnchor).weight(1f, fill = false),
                icon = current?.icon ?: "ledger", border = true,
            ) { ledgerMenu = true }
        }
        Spacer(Modifier.height(10.dp))
        AssetChartCard(series, net, mode, { mode = it }, range, { range = it }, bg = K.Card, chipBg = K.Sheet, padding = 18.dp)

        // savings goals
        Spacer(Modifier.height(26.dp))
        CSection("Target tabungan", action = "Lihat semua", onAction = { nav.go(R.GOALS) })
        Spacer(Modifier.height(10.dp))
        if (d.goals.isEmpty()) {
            Text(
                "Belum ada target. Ketuk di sini untuk membuat target tabungan pertama.",
                fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = K.Muted,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.Card).pressable { nav.go(R.GOALS) }.padding(16.dp),
            )
        } else {
            CList {
                d.goals.take(3).forEachIndexed { i, g -> GoalRow(g, divider = i > 0, hidden = hidden) { nav.go(R.GOALS) } }
            }
        }
        // room for the bottom tab bar
        Spacer(Modifier.height(100.dp))
    }

    if (ledgerMenu) {
        PillMenu(ledgerAnchor.rect, { ledgerMenu = false }) {
            PillOption("Semua buku", Tone.PEACH, ledgerId == 0L, { Prefs.ledger = 0L; ledgerMenu = false }, icon = "ledger")
            d.activeLedgers.forEach { l ->
                PillOption(l.name, Tone.of(l.tone), ledgerId == l.id, { Prefs.ledger = l.id; ledgerMenu = false }, icon = l.icon)
            }
            PillOption("Kelola buku kas", Tone.PEACH, false, { ledgerMenu = false; nav.go(R.LEDGERS) }, glyph = Icons.Rounded.Settings)
        }
    }
}

/** "Aset / Rp25.300.000" column of the net-worth card. */
@Composable
private fun HeroStat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, maxLines = 1)
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Savings goal row in a cream list: icon, name, "Rp6.000.000 dari Rp7.000.000", progress bar, percent. */
@Composable
private fun GoalRow(g: Goal, divider: Boolean, hidden: Boolean, onClick: () -> Unit) {
    val frac = if (g.target > 0) (g.saved.toFloat() / g.target).coerceIn(0f, 1f) else 0f
    val pct = if (g.target > 0) (g.saved * 100 / g.target).coerceIn(0L, 999L) else 0L
    Column {
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
        Row(Modifier.fillMaxWidth().pressable(onClick).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            CatIcon(g.icon, 32.dp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(g.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${money(g.saved, hidden)} dari ${money(g.target, hidden)}", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = K.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                ProgressBar(frac, K.Green, track = K.SageMid.copy(alpha = .45f), height = 6.dp)
            }
            Spacer(Modifier.width(13.dp))
            Text("$pct%", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1, softWrap = false)
        }
    }
}

/** Uang / Investasi box on the net worth card. */
@Composable
private fun NetBox(label: String, dot: Color, value: String, modifier: Modifier, dim: Boolean = false) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color(0x99FFFFFF)).padding(horizontal = 12.dp, vertical = 8.dp).let { if (dim) it.alpha(0.5f) else it }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
        }
        Text(value, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1)
    }
}
