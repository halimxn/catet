package id.kantong.app.ui.screens

import id.kantong.app.data.startOfWeek

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Category
import id.kantong.app.data.ID
import id.kantong.app.data.Prefs
import id.kantong.app.data.Txn
import id.kantong.app.data.ViewPeriod
import id.kantong.app.data.daysInMonth
import id.kantong.app.data.fmt
import id.kantong.app.data.grouped
import id.kantong.app.data.reportRange
import id.kantong.app.data.rupiah
import id.kantong.app.data.startOfDay
import id.kantong.app.data.startOfMonth
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.BarGroup
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CChip
import id.kantong.app.ui.components.CList
import id.kantong.app.ui.components.CListRow
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.DailyLine
import id.kantong.app.ui.components.Donut
import id.kantong.app.ui.components.GroupedBars
import id.kantong.app.ui.components.MeowSegmented
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.MonthHeatmap
import id.kantong.app.ui.components.PeriodCard
import id.kantong.app.ui.components.PillMenu
import id.kantong.app.ui.components.PillOption
import id.kantong.app.ui.components.XLabel
import id.kantong.app.ui.components.anchor
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.components.rememberAnchor
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToLong

private const val DAY = 86_400_000L
private val FlowTrack = Color(0xFFF3E3B5)

/**
 * Laporan tab: period chip (week / month / year; months follow [ViewPeriod]), Ringkasan / Pengeluaran /
 * Pemasukan, then cash flow, 6-period bars, stat tiles, daily line, category donut and a month heatmap.
 */
@Composable
fun ReportsScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    var mode by rememberSaveable { mutableStateOf("month") }
    var anchor by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    val tab = 0
    var summaryKind by rememberSaveable { mutableStateOf("out") }
    var ledgerMenu by remember { mutableStateOf(false) }
    var periodOpen by remember { mutableStateOf(false) }
    val ledgerAnchor = rememberAnchor()

    val v = d.forLedger(Prefs.ledger)
    val ledger = d.ledger(Prefs.ledger)
    val (from, to) = if (mode == "month") ViewPeriod.start() to ViewPeriod.end() else reportRange(mode, anchor)
    val now = System.currentTimeMillis()
    val slots = dayIndex(to - 1, from) + 1
    val elapsed = when { now < from -> 0; now >= to -> slots; else -> dayIndex(now, from) + 1 }
    val today = if (now in from until to) dayIndex(now, from) else -1

    val records = v.txns.filter { it.time in from until to && it.kind != "transfer" }
    val income = records.filter { it.kind == "in" }.sumOf { it.amount }
    val spent = records.filter { it.kind == "out" }.sumOf { it.amount }
    val focus = when (tab) { 2 -> "in"; 1 -> "out"; else -> summaryKind }
    val focusRecs = records.filter { it.kind == focus }
    val focusTotal = focusRecs.sumOf { it.amount }
    val daily = LongArray(slots).also { arr -> focusRecs.forEach { t -> dayIndex(t.time, from).let { if (it in arr.indices) arr[it] += t.amount } } }.toList()
    val perDay = if (elapsed > 0) focusTotal / elapsed else 0L
    val word = when (mode) { "week" -> "mingguan"; "year" -> fmt(from, "yyyy"); else -> fmt(from, "MMMM") }
    val range = rangeText(mode, from, to, elapsed)
    val openCat: (Category) -> Unit = { vm.openHistoryFor(it.id); nav.go(R.SEARCH) }
    val setMonth: (Long) -> Unit = { t -> startOfMonth(t).let { s -> ViewPeriod.monthStart = if (s == startOfMonth()) 0L else s } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        // Title, ledger, search and period.
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Laporan", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, letterSpacing = (-0.3).sp, maxLines = 1)
                if (d.activeLedgers.size > 1 || Prefs.ledger != 0L) {
                    Text(
                        "${ledger?.name ?: "Semua buku"} ▾", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Muted, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.anchor(ledgerAnchor).pressable { ledgerMenu = true },
                    )
                }
            }
            CRoundButton(Icons.Rounded.Search, "Cari", { nav.go(R.SEARCH) }, size = 38.dp)
            Spacer(Modifier.width(8.dp))
            CChip(periodLabel(mode, from, to) + " ▾", bg = K.Tile, border = true, height = 38.dp) { periodOpen = true }
        }

        // 1) Cash flow (Ringkasan) or the total of the focused kind.
        Spacer(Modifier.height(12.dp))
        CCard(K.Yellow, radius = 26.dp, padding = CardPad) {
            if (tab == 0) {
                CardHead("Arus kas $word", range)
                Money(income - spent, 36.sp, Modifier.padding(top = 4.dp), color = if (income - spent < 0) K.Danger else K.Green)
                Text("sisa = masuk − keluar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                val top = maxOf(income, spent)
                FlowBar("Masuk", income.rupiah(), K.ChartIn, frac(income, top), Modifier.padding(top = 10.dp))
                FlowBar(
                    "Keluar", spent.rupiah() + if (income > 0) " · " + percent(spent, income) else "", K.ChartOut,
                    frac(spent, top), Modifier.padding(top = 9.dp),
                )
            } else {
                CardHead((if (focus == "out") "Pengeluaran " else "Pemasukan ") + word, range)
                Money(focusTotal, 36.sp, Modifier.padding(top = 4.dp), color = if (focus == "out") K.Ink else K.Green)
                val sub = when {
                    focus == "in" -> "sisa ${(income - spent).rupiah()} setelah pengeluaran"
                    income > 0 -> "${percent(spent, income)} dari pemasukan"
                    else -> "belum ada pemasukan di periode ini"
                }
                Text(sub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
            }
        }

        // 2) Last 6 months / weeks (12 months in year mode).
        // Stable window: the latest 6 periods while the chosen one is among them, so tapping a bar never shifts the chart.
        val latest = buckets(mode, if (mode == "week") startOfWeek() else startOfMonth())
        val buckets = if (mode != "year" && latest.any { from in it.from until it.to }) latest else buckets(mode, from)
        val series = if (tab == 0) listOf("in", "out") else listOf(focus)
        val colors = series.map { if (it == "in") K.ChartIn else K.ChartOut }
        val groups = buckets.map { b -> BarGroup(b.label, series.map { k -> v.txns.filter { it.kind == k && it.time in b.from until b.to }.sumOf { it.amount } }) }
        val selected = if (mode == "year") (if (today >= 0) Calendar.getInstance().get(Calendar.MONTH) else 11) else buckets.indexOfFirst { from in it.from until it.to }.takeIf { it >= 0 } ?: buckets.lastIndex
        Spacer(Modifier.height(12.dp))
        CCard(K.Card, radius = 26.dp, padding = CardPad) {
            CardHead(
                when (tab) { 0 -> "Masuk vs keluar"; 1 -> "Pengeluaran"; else -> "Pemasukan" },
                when (mode) { "week" -> "6 minggu"; "year" -> "12 bulan"; else -> "6 bulan" },
            )
            if (series.size > 1) {
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendKey(K.ChartIn, "Masuk"); LegendKey(K.ChartOut, "Keluar")
                }
            }
            GroupedBars(groups, colors, selected, Modifier.padding(top = 8.dp)) { i ->
                val b = buckets[i]
                when (mode) {
                    "week" -> anchor = b.from
                    "year" -> { mode = "month"; setMonth(b.from) }
                    else -> setMonth(b.from)
                }
            }
            if (groups.all { g -> g.values.all { it == 0L } }) Text(
                "Belum ada catatan · ketuk bulan lain untuk melihatnya", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }

        // Ringkasan: choose whether the charts below show spending or income.
        if (tab == 0) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Grafik", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.weight(1f))
                MeowSegmented(listOf("Pemasukan", "Pengeluaran"), if (summaryKind == "in") 0 else 1, { summaryKind = if (it == 0) "in" else "out" }, fontSize = 14.sp)
            }
        }

        // 3) Stat tiles.
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            StatTile("Rata-rata/hari", compactRp(perDay))
            StatTile(if (focus == "out") "Hari terboros" else "Hari teramai", busiestWeekday(focusRecs))
            StatTile("Transaksi", (if (tab == 0) records.size else focusRecs.size).toLong().grouped())
        }

        // 4) Daily line.
        Spacer(Modifier.height(12.dp))
        CCard(K.SageCard, radius = 26.dp, padding = CardPad) {
            CardHead(if (focus == "out") "Pengeluaran harian" else "Pemasukan harian", "rata-rata ${compactRp(perDay)}")
            if (focusTotal == 0L) {
                EmptyText(if (focus == "out") "Belum ada pengeluaran di periode ini." else "Belum ada pemasukan di periode ini.")
            } else {
                val monthBudget = if (focus == "out") d.budgets.filter { it.period == "month" }.sumOf { it.amount } else 0L
                val limit = if (monthBudget > 0) monthBudget / daysInMonth(from) else 0L
                DailyLine(
                    daily.take(elapsed), slots, if (focus == "out") K.ChartOut else K.ChartIn, K.SageCard, xLabels(mode, from, slots, today),
                    peakText = { i, amount -> "${fmt(from + i * DAY + DAY / 2, "d MMM")} · ${compactRp(amount)}" },
                    modifier = Modifier.padding(top = 6.dp), limit = limit,
                    limitText = if (limit > 0) "batas ${compactRp(limit)}" else "", today = today,
                )
            }
        }

        // 5) Per category.
        val slices = reportSlices(v, records, focus)
        Spacer(Modifier.height(12.dp))
        CCard(K.Card, radius = 26.dp, padding = CardPad) {
            CardHead("Per kategori", action = "Semua ›") { nav.go(R.types(focus)) }
            if (focusTotal == 0L) {
                EmptyText(if (focus == "out") "Belum ada pengeluaran untuk dibagi per kategori." else "Belum ada pemasukan untuk dibagi per kategori.", icon = "category")
            } else {
                CategoryDonut(slices, focusTotal, focus, openCat) { nav.go(R.types(focus)) }
            }
        }
        if (slices.size > 5) {
            Spacer(Modifier.height(12.dp))
            CList {
                slices.forEachIndexed { i, s ->
                    CListRow(
                        s.icon, s.name, "${percent(s.amount, focusTotal)} · ${s.count} transaksi", divider = i > 0,
                        onClick = s.cat?.let { c -> { openCat(c) } },
                        trailing = { Text(s.amount.grouped(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1) },
                    )
                }
            }
        }

        // 6) Month heatmap (month view only).
        if (mode == "month") {
            Spacer(Modifier.height(12.dp))
            CCard(K.Card, radius = 26.dp, padding = CardPad) {
                CardHead(if (focus == "out") "Kalender pengeluaran" else "Kalender pemasukan", fmt(from, "MMMM"))
                MonthHeatmap(from, daily, elapsed, today)
            }
        }
        Spacer(Modifier.height(100.dp))
    }

    if (ledgerMenu) {
        PillMenu(ledgerAnchor.rect, { ledgerMenu = false }) {
            PillOption("Semua buku", Tone.PEACH, Prefs.ledger == 0L, { Prefs.ledger = 0L; ledgerMenu = false }, icon = "ledger")
            d.activeLedgers.forEach { l ->
                PillOption(l.name, Tone.of(l.tone), Prefs.ledger == l.id, { Prefs.ledger = l.id; ledgerMenu = false }, icon = l.icon)
            }
            PillOption("Kelola buku kas", Tone.PEACH, false, { ledgerMenu = false; nav.go(R.LEDGERS) }, glyph = Icons.Rounded.Settings)
        }
    }
    if (periodOpen) {
        PeriodCard(mode, if (mode == "month") from else anchor, { periodOpen = false }) { m, a ->
            mode = m
            if (m == "month") setMonth(a) else anchor = a
            periodOpen = false
        }
    }
}

private val CardPad = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)

/** Day number (0-based) of [time] counted from [from] (a start of day). */
private fun dayIndex(time: Long, from: Long): Int = ((startOfDay(time) - from + DAY / 2) / DAY).toInt()

private fun plus(time: Long, field: Int, amount: Int): Long = Calendar.getInstance().apply { timeInMillis = time; add(field, amount) }.timeInMillis

/** Chip text: "Nov 2026", "17–23 Nov" or "2026". */
private fun periodLabel(mode: String, from: Long, to: Long): String = when (mode) {
    "week" -> weekText(from, to)
    "year" -> fmt(from, "yyyy")
    else -> fmt(from, "MMM yyyy")
}

private fun weekText(from: Long, to: Long): String {
    val last = to - DAY
    return if (fmt(from, "MM") == fmt(last, "MM")) "${fmt(from, "d")}–${fmt(last, "d MMM")}" else "${fmt(from, "d MMM")} – ${fmt(last, "d MMM")}"
}

/** Covered part of the period: "1–21 Nov", "17–23 Nov", "Jan–Nov 2026". */
private fun rangeText(mode: String, from: Long, to: Long, elapsed: Int): String = when (mode) {
    "week" -> weekText(from, to)
    "year" -> if (elapsed in 1 until dayIndex(to - 1, from) + 1) "${fmt(from, "MMM")}–${fmt(from + (elapsed - 1) * DAY, "MMM yyyy")}" else fmt(from, "yyyy")
    else -> {
        val days = dayIndex(to - 1, from) + 1
        val last = if (elapsed in 1 until days) elapsed else days
        "1–$last ${fmt(from, "MMM")}"
    }
}

private class Bucket(val label: String, val from: Long, val to: Long)

/** Bar groups: the last 6 months / weeks ending at the viewed one, or the 12 months of the viewed year. */
private fun buckets(mode: String, from: Long): List<Bucket> = when (mode) {
    "week" -> (5 downTo 0).map { i -> plus(from, Calendar.DAY_OF_YEAR, -7 * i).let { s -> Bucket(fmt(s, "d/M"), s, plus(s, Calendar.DAY_OF_YEAR, 7)) } }
    "year" -> (0 until 12).map { i -> plus(from, Calendar.MONTH, i).let { s -> Bucket(fmt(s, "MMM").take(3), s, plus(s, Calendar.MONTH, 1)) } }
    else -> (5 downTo 0).map { i -> plus(from, Calendar.MONTH, -i).let { s -> Bucket(fmt(s, "MMM").take(3), s, plus(s, Calendar.MONTH, 1)) } }
}

/** X labels of the daily line: weekdays (week), 1/8/15/22/last + today (month), quarter months (year). */
private fun xLabels(mode: String, from: Long, slots: Int, today: Int): List<XLabel> = when {
    slots <= 7 -> (0 until slots).map { XLabel(it, fmt(from + it * DAY + DAY / 2, "EEE").take(3), it == today) }
    mode == "year" -> listOf(0, 3, 6, 9).map { m -> plus(from, Calendar.MONTH, m).let { XLabel(dayIndex(it, from), fmt(it, "MMM").take(3)) } }
    else -> listOf(0, 7, 14, 21, slots - 1).filter { today < 0 || abs(it - today) >= 3 }.map { XLabel(it, "${it + 1}") } +
        (if (today >= 0) listOf(XLabel(today, "${today + 1}", bold = true)) else emptyList())
}

/** Weekday name with the highest total, or "–". */
private fun busiestWeekday(records: List<Txn>): String =
    records.groupBy { Calendar.getInstance().apply { timeInMillis = it.time }.get(Calendar.DAY_OF_WEEK) }
        .maxByOrNull { (_, list) -> list.sumOf { it.amount } }?.value?.first()?.let { fmt(it.time, "EEEE") } ?: "–"

private fun frac(v: Long, top: Long): Float = if (top > 0) (v.toDouble() / top).toFloat().coerceIn(0f, 1f) else 0f

/** "34%" (whole percent; "<1%" for tiny shares). */
private fun percent(part: Long, whole: Long): String {
    if (whole <= 0) return "0%"
    val p = part * 100.0 / whole
    return if (p > 0 && p < 0.5) "<1%" else "${p.roundToLong()}%"
}

/** Compact amount: 1680000 -> "1,68jt", 875000 -> "875rb". */
private fun compact(v: Long): String {
    val a = abs(v)
    val s = when {
        a >= 1_000_000_000L -> dec2(a / 1e9) + "M"
        a >= 1_000_000L -> dec2(a / 1e6) + "jt"
        a >= 1_000L -> (a / 1e3).roundToLong().toString() + "rb"
        else -> a.toString()
    }
    return if (v < 0) "−$s" else s
}

private fun dec2(x: Double): String = String.format(ID, "%.2f", x).trimEnd('0').trimEnd(',')

private fun compactRp(v: Long): String = (if (v < 0) "−" else "") + "Rp" + compact(abs(v))

/** Card header: bold title, muted note or a green action on the right. */
@Composable
private fun CardHead(title: String, note: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 16.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (note != null) Text(note, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.Muted, maxLines = 1)
        if (action != null) Text(action, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Green, maxLines = 1, modifier = Modifier.pressable { onAction?.invoke() })
    }
}

/** Empty state: a two-tone sticker in a soft circle with a short line, so a section never looks blank. */
@Composable
private fun EmptyText(text: String, icon: String = "report") {
    id.kantong.app.ui.components.CEmpty(text, icon)
}

@Composable
private fun LegendKey(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.Muted)
    }
}

/** Labelled bar on the cash flow card. */
@Composable
private fun FlowBar(label: String, value: String, color: Color, fraction: Float, modifier: Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Ink, modifier = Modifier.weight(1f))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1)
        }
        Box(Modifier.padding(top = 5.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(FlowTrack)) {
            if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(color))
        }
    }
}

@Composable
private fun RowScope.StatTile(label: String, value: String) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(K.Card).padding(horizontal = 12.dp, vertical = 12.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = K.Muted, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
    }
}

/** Total of one main category (records of its subcategories included). [cat] is null for uncategorized records. */
private class ReportSlice(val cat: Category?, val amount: Long, val count: Int) {
    val name get() = cat?.name ?: "Tanpa kategori"
    val icon get() = cat?.icon ?: "other"
}

private fun reportSlices(d: AppData, records: List<Txn>, kind: String): List<ReportSlice> =
    records.filter { it.kind == kind }
        .groupBy { d.rootOf(it.categoryId)?.id }
        .map { (id, list) -> ReportSlice(id?.let { d.category(it) }, list.sumOf { it.amount }, list.size) }
        .sortedByDescending { it.amount }

/** Donut in fixed category colors (5th slot folds the rest into "Lainnya") with a legend of amount and share. */
@Composable
private fun CategoryDonut(slices: List<ReportSlice>, total: Long, kind: String, onOpen: (Category) -> Unit, onOthers: () -> Unit) {
    val shown = if (slices.size > 5) slices.take(4) else slices
    val rest = if (slices.size > 5) slices.drop(4).sumOf { it.amount } else 0L
    val rows = shown.mapIndexed { i, s -> Triple(s.name, s.amount, K.ChartCats[i]) } +
        (if (rest > 0) listOf(Triple("Lainnya", rest, K.ChartCats[4])) else emptyList())
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Donut(rows.map { it.second.toFloat() to it.third }, size = 120.dp, stroke = 20.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (kind == "out") "Keluar" else "Masuk", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                Text(compactRp(total), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            rows.forEachIndexed { i, (name, amount, color) ->
                val cat = shown.getOrNull(i)?.cat
                val click: (() -> Unit)? = when {
                    i >= shown.size -> onOthers
                    cat != null -> { { onOpen(cat) } }
                    else -> null
                }
                Row(
                    Modifier.fillMaxWidth().height(24.dp).let { if (click != null) it.pressable(click) else it },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
                    Spacer(Modifier.width(7.dp))
                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(compact(amount), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1)
                    Text(percent(amount, total), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = K.Muted, maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.width(36.dp))
                }
            }
        }
    }
}
