package id.kantong.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Holding
import id.kantong.app.data.InvKind
import id.kantong.app.data.InvTxn
import id.kantong.app.data.Position
import id.kantong.app.data.Prefs
import id.kantong.app.data.Prices
import id.kantong.app.data.fmt
import id.kantong.app.data.grouped
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CalendarCard
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CatetSheet
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.MeowToggle
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.SheetHeader
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

private val GoldInk = Color(0xFF7A5A12)
private val BondTone = Color(0xFFE8A79C)

private fun pct(v: Double): String = (if (v >= 0) "+" else "−") + String.format(Locale("in", "ID"), "%.1f", abs(v)) + "%"
private fun gainColor(v: Double) = if (v < 0) K.Danger else K.ChartIn
private fun kindColor(kind: String) = when (kind) { "stock" -> K.Green; "gold" -> K.Gold; "bond" -> BondTone; else -> K.SageMid }

/** Pastel badge (background, text) for a position. */
private fun badge(p: Position): Pair<Color, Color> = when (p.h.kind) {
    "gold" -> K.Yellow to GoldInk
    "bond", "fund" -> K.Card to K.Ink2
    else -> if (p.gain < 0) K.Pink to K.Danger else K.SageCard to K.Green
}

@Composable
private fun Badge(p: Position, size: Int = 42) {
    val (bg, fg) = badge(p)
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)).background(bg), contentAlignment = Alignment.Center) {
        Text(if (p.h.kind == "gold") "Au" else p.h.code.take(4), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = fg, maxLines = 1)
    }
}

/**
 * "Investasi" block on Aset: header with show switch and refresh (↻ + price time), a sage portfolio card
 * (value, today and total gain in percent, allocation bar) and the holdings list. Hidden when the switch is off.
 */
@Composable
fun InvestSection(d: AppData, vm: AppViewModel, nav: Nav, hidden: Boolean) {
    var adding by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val show = Prefs.showInvest
    val positions = d.positions
    val stocks = d.holdings.filter { InvKind.live(it.kind) }

    Row(Modifier.fillMaxWidth().padding(top = 26.dp, start = 2.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        CatIcon("invest", 26.dp)
        Spacer(Modifier.width(8.dp))
        Text("Investasi", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.weight(1f))
        if (show && stocks.isNotEmpty()) {
            val t = stocks.maxOf { it.priceTime }
            Row(
                Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(K.Field).pressable {
                    if (busy) return@pressable
                    busy = true
                    scope.launch {
                        val n = vm.refreshPrices(d.holdings)
                        busy = false
                        note = if (n == 0) "Gagal memperbarui harga. Cek koneksi internet." else null
                        if (note != null) { delay(4000); note = null }
                    }
                }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(Icons.Rounded.Refresh, "Perbarui harga", tint = K.Green, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (busy) "…" else if (t > 0) fmt(t, "HH.mm") else "Perbarui", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = K.OnSage)
            }
            Spacer(Modifier.width(8.dp))
        }
        MeowToggle(show) { Prefs.showInvest = it }
    }
    note?.let { Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.Danger, modifier = Modifier.padding(start = 4.dp, top = 6.dp)) }
    Spacer(Modifier.height(10.dp))

    if (!show) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(K.Field).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Investasi disembunyikan", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink2)
            Text("Kekayaan bersih hanya menghitung uang di rekening, dompet, dan e-wallet.", fontSize = 12.5.sp, color = K.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 3.dp))
        }
        return
    }

    if (positions.isEmpty()) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(K.SageCard).padding(18.dp)) {
            Text("Pantau saham, emas, dan obligasi", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
            Text("Catat setiap pembelian; harga rata-rata dan untung/rugi dihitung otomatis. Harga saham BEI bisa diperbarui dengan sekali ketuk.", fontSize = 13.sp, color = K.OnSage, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(12.dp))
            RedButton("Tambah investasi", Modifier.fillMaxWidth()) { adding = true }
        }
    } else {
        val value = positions.sumOf { it.value }
        val cost = positions.sumOf { it.cost }
        val day = positions.sumOf { it.dayGain }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(K.SageCard).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Nilai portofolio", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.OnSage)
                    Money(value, 32.sp, hidden = hidden)
                }
                Box(Modifier.size(54.dp).clip(CircleShape).background(K.Tile), contentAlignment = Alignment.Center) { CatIcon("invest", 34.dp) }
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val dayPct = if (value - day > 0) day * 100.0 / (value - day) else 0.0
                val totPct = if (cost > 0) (value - cost) * 100.0 / cost else 0.0
                PctBox("Hari ini", dayPct, Modifier.weight(1f))
                PctBox("Total untung", totPct, Modifier.weight(1f))
            }
            if (value > 0) {
                val byKind = positions.groupBy { it.h.kind }.mapValues { (_, v) -> v.sumOf { it.value } }.filterValues { it > 0 }
                Row(Modifier.fillMaxWidth().padding(top = 14.dp).height(8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    byKind.forEach { (k, v) -> Box(Modifier.weight(v.toFloat()).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(kindColor(k))) }
                }
                Row(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    byKind.forEach { (k, v) -> Text("${InvKind.label(k)} ${Math.round(v * 100.0 / value)}%", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = K.OnSage) }
                }
            }
        }
        Column(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(K.Card).padding(horizontal = 16.dp, vertical = 2.dp)) {
            positions.forEachIndexed { i, p ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
                Row(Modifier.fillMaxWidth().pressable { nav.go(R.holding(p.h.id)) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Badge(p)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.h.name.ifBlank { p.h.code }, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(InvKind.qtyText(p.h.kind, p.qty), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (hidden) "•••••" else p.value.grouped(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1)
                        Text(pct(p.gainPct), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = gainColor(p.gainPct))
                    }
                }
            }
        }
        Box(
            Modifier.padding(top = 10.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp)).background(K.Field).pressable { adding = true },
            contentAlignment = Alignment.Center,
        ) { Text("+ Tambah investasi", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = K.Green) }
    }

    if (adding) InvTxnSheet(d, vm, null, null, "new") { adding = false }
}

@Composable
private fun PctBox(label: String, v: Double, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(K.Tile).padding(horizontal = 12.dp, vertical = 9.dp)) {
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
        Text(pct(v), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = gainColor(v))
    }
}

/** Detail of one investment: price card with chart (average and target lines), key numbers, history, Jual / Dividen / Beli lagi. */
@Composable
fun HoldingScreen(d: AppData, vm: AppViewModel, nav: Nav, id: Long) {
    val h = d.holding(id)
    if (h == null) { LaunchedEffect(Unit) { nav.back() }; return }
    val p = d.position(h)
    val hidden = Prefs.hideAmounts
    var sheet by remember { mutableStateOf<String?>(null) }
    var edit by remember { mutableStateOf<String?>(null) } // price | target
    var range by remember { mutableStateOf("1mo") }
    var closes by remember { mutableStateOf<List<Double>>(emptyList()) }
    var removeTx by remember { mutableStateOf<InvTxn?>(null) }
    var removeAll by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val live = InvKind.live(h.kind)

    if (live && Prefs.showInvest) LaunchedEffect(range, h.code) {
        closes = withContext(Dispatchers.IO) { runCatching { Prices.closes(h.code, range) }.getOrDefault(emptyList()) }
    }

    Column(Modifier.fillMaxSize().background(K.Bg).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Badge(p, 44)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(h.name.ifBlank { h.code }, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${InvKind.label(h.kind)} · ${h.code}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
            }
            if (live) CRoundButton(Icons.Rounded.Refresh, "Perbarui harga", { scope.launch { vm.refreshPrices(listOf(h)) } }, size = 40.dp, outlined = true)
        }

        Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(K.SageCard).padding(18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(if (live) "Harga sekarang" else "Harga (diisi manual)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.OnSage)
                    Text(h.lastPrice.grouped(), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = K.Green)
                }
                if (live && h.prevClose > 0) Text(
                    (if (h.lastPrice >= h.prevClose) "+" else "−") + abs(h.lastPrice - h.prevClose).grouped() + " · " + pct(p.dayPct).drop(1),
                    fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = gainColor(p.dayPct),
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(K.Tile).padding(horizontal = 10.dp, vertical = 4.dp),
                ) else if (!live) Text(
                    "Ubah harga", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Green,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(K.Tile).pressable { edit = "price" }.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
            if (live) {
                PriceChart(closes, p.avg, h.targetPrice.toDouble(), Modifier.padding(top = 10.dp).fillMaxWidth().height(80.dp))
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (p.avg > 0) Text("- - rata-rata ${Math.round(p.avg).grouped()}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = K.OnSage)
                    if (h.targetPrice > 0) Text("- - target ${h.targetPrice.grouped()}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = GoldInk)
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("5d" to "1M", "1mo" to "1B", "3mo" to "3B", "1y" to "1T").forEach { (r, l) ->
                        Box(
                            Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(12.dp)).background(if (range == r) K.Tile else Color.Transparent).pressable { range = r },
                            contentAlignment = Alignment.Center,
                        ) { Text(l, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = if (range == r) K.Ink else K.OnSage) }
                    }
                }
            }
        }

        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("Jumlah · rata-rata", "${InvKind.qtyText(h.kind, p.qty)} · ${Math.round(p.avg).grouped()}", K.Yellow, K.OnYellow, Modifier.weight(1f))
            Stat("Untung", if (hidden) "•••••" else (if (p.gain >= 0) "+" else "−") + abs(p.gain).grouped(), K.Yellow, K.OnYellow, Modifier.weight(1f), gainColor(p.gain.toDouble()))
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("Modal", if (hidden) "•••••" else p.cost.grouped(), K.Card, K.Muted, Modifier.weight(1f))
            if (h.kind == "stock") Stat("Dividen diterima", if (hidden) "•••••" else "Rp" + p.dividends.grouped(), K.Card, K.Muted, Modifier.weight(1f))
            else Stat("Nilai", if (hidden) "•••••" else p.value.grouped(), K.Card, K.Muted, Modifier.weight(1f))
        }
        Row(
            Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Card).pressable { edit = "target" }.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Target jual", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                Text(if (h.targetPrice > 0) h.targetPrice.grouped() + if (h.lastPrice >= h.targetPrice) " · tercapai" else "" else "Belum diatur", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (h.targetPrice > 0 && h.lastPrice >= h.targetPrice) K.ChartIn else K.Ink)
            }
            Text("Atur ›", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = K.Green)
        }

        Row(Modifier.padding(top = 18.dp, start = 4.dp, end = 4.dp, bottom = 6.dp)) {
            Text("Riwayat", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.weight(1f))
            Text("${p.history.size} transaksi", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(K.Card).padding(horizontal = 14.dp, vertical = 2.dp)) {
            if (p.history.isEmpty()) Text("Belum ada transaksi.", fontSize = 13.sp, color = K.Muted, modifier = Modifier.padding(vertical = 14.dp))
            p.history.forEachIndexed { i, t ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
                val (label, bg, fg) = when (t.type) { "buy" -> Triple("Beli", K.SageCard, K.Green); "sell" -> Triple("Jual", K.Pink, K.Danger); else -> Triple("Dividen", K.Yellow, GoldInk) }
                Row(Modifier.fillMaxWidth().pressable { removeTx = t }.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = fg, modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 9.dp, vertical = 3.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (t.type == "dividend") "Rp${t.price.grouped()} × ${t.qty.toLong().grouped()} lembar" else "${InvKind.qtyText(h.kind, t.qty)} × ${t.price.grouped()}",
                            fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1,
                        )
                        Text(fmt(t.time, "d MMM yyyy") + (d.account(d.allTxns.firstOrNull { it.id == t.txnId }?.let { it.toAccountId ?: it.accountId })?.let { " · ${it.name}" } ?: ""), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                    }
                    val amt = (t.qty * t.price).toLong()
                    Text(
                        if (hidden) "•••••" else (if (t.type == "buy") "" else "+") + amt.grouped(), fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (t.type == "buy") K.Ink else K.ChartIn,
                    )
                }
            }
        }

        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlatButton("Jual", Modifier.weight(1f)) { if (p.qty > 0) sheet = "sell" }
            if (h.kind == "stock") FlatButton("Dividen", Modifier.weight(1f)) { if (p.qty > 0) sheet = "dividend" }
            RedButton("Beli lagi", Modifier.weight(1.3f)) { sheet = "buy" }
        }
        Text(
            "Hapus investasi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = K.Danger, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).clip(RoundedCornerShape(12.dp)).pressable { removeAll = true }.padding(10.dp),
        )
        Spacer(Modifier.height(40.dp))
    }

    sheet?.let { m -> InvTxnSheet(d, vm, h, p, m) { sheet = null } }
    edit?.let { e ->
        NumberSheet(
            if (e == "price") "Ubah harga" else "Target jual",
            if (e == "price") "Harga per ${if (h.kind == "gold") "gram" else "unit"}" else "Harga target per lembar",
            if (e == "price") h.lastPrice else h.targetPrice,
            onDismiss = { edit = null },
        ) { v -> vm.saveHolding(if (e == "price") h.copy(lastPrice = v, priceTime = System.currentTimeMillis()) else h.copy(targetPrice = v)); edit = null }
    }
    removeTx?.let { t ->
        ConfirmSheet("Hapus transaksi ini?", "Catatan uang yang terkait juga dihapus.", onDismiss = { removeTx = null }) { vm.deleteInvTxn(t); removeTx = null }
    }
    if (removeAll) ConfirmSheet("Hapus ${h.code}?", "Semua riwayat beli, jual, dan dividen ikut dihapus.", onDismiss = { removeAll = false }) {
        vm.deleteHolding(h, p.history); removeAll = false; nav.back()
    }
}

@Composable
private fun Stat(label: String, value: String, bg: Color, labelColor: Color, modifier: Modifier, valueColor: Color = K.Ink) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(bg).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = labelColor, maxLines = 1)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FlatButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(50.dp).clip(RoundedCornerShape(17.dp)).background(K.Field).pressable(onClick), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
    }
}

/** Daily closes as a green line with the average buy price (sage, dashed) and target (gold, dashed). */
@Composable
private fun PriceChart(closes: List<Double>, avg: Double, target: Double, modifier: Modifier) {
    if (closes.size < 2) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("Grafik muncul setelah harga dimuat", fontSize = 12.sp, color = K.OnSage) }
        return
    }
    val refs = listOfNotNull(avg.takeIf { it > 0 }, target.takeIf { it > 0 })
    val lo = (closes + refs).min(); val hi = (closes + refs).max()
    val span = (hi - lo).takeIf { it > 0 } ?: 1.0
    Canvas(modifier) {
        fun y(v: Double) = (size.height - 6f) - ((v - lo) / span * (size.height - 12f)).toFloat()
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        if (avg > 0) drawLine(K.SageMid, Offset(0f, y(avg)), Offset(size.width, y(avg)), 4f, pathEffect = dash)
        if (target > 0) drawLine(K.Gold, Offset(0f, y(target)), Offset(size.width, y(target)), 4f, pathEffect = dash)
        val path = Path()
        closes.forEachIndexed { i, v ->
            val x = i * size.width / (closes.size - 1)
            if (i == 0) path.moveTo(x, y(v)) else path.lineTo(x, y(v))
        }
        drawPath(path, K.Green, style = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(K.Green, 9f, Offset(size.width, y(closes.last())))
    }
}

/** Number input box: small label, big grouped value; allows a decimal comma when [decimal]. */
@Composable
private fun NumField(label: String, text: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, decimal: Boolean = false, suffix: String = "") {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(K.Tile).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (text.isEmpty()) Text("0", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = K.Muted.copy(alpha = 0.5f))
                val shown = if (decimal) text else text.toLongOrNull()?.grouped() ?: text
                BasicTextField(
                    value = androidx.compose.ui.text.input.TextFieldValue(shown, androidx.compose.ui.text.TextRange(shown.length)),
                    onValueChange = { v -> onChange(v.text.filter { it.isDigit() || (decimal && it == ',') }.trimStart('0').take(14)) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink),
                    cursorBrush = SolidColor(K.Green),
                    keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (suffix.isNotEmpty()) Text(suffix, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = K.Muted)
        }
    }
}

private fun String.num(): Double = replace(".", "").replace(',', '.').toDoubleOrNull() ?: 0.0

/**
 * Buy / sell / dividend sheet, or a new investment ([mode] = "new"). Shows the effect live:
 * average before → after for a buy, realised gain for a sell, total for a dividend.
 */
@Composable
fun InvTxnSheet(d: AppData, vm: AppViewModel, h: Holding?, p: Position?, mode: String, onDismiss: () -> Unit) {
    var kind by remember { mutableStateOf(h?.kind ?: "stock") }
    var code by remember { mutableStateOf(h?.code ?: "") }
    var name by remember { mutableStateOf(h?.name ?: "") }
    var quote by remember { mutableStateOf<Prices.Quote?>(null) }
    var checking by remember { mutableStateOf(false) }
    var qty by remember { mutableStateOf(if (mode == "sell" && p != null) (p.qty / InvKind.step(h!!.kind)).let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString().replace('.', ',') } else "") }
    var price by remember { mutableStateOf(if (mode != "dividend" && (h?.lastPrice ?: 0) > 0) h!!.lastPrice.toString() else "") }
    var fee by remember { mutableStateOf("") }
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var pickDate by remember { mutableStateOf(false) }
    val accounts = d.accounts.filter { d.balanceOf(it) >= 0 || it.type != "credit" }
    var accountId by remember { mutableLongStateOf(h?.accountId?.takeIf { it > 0 } ?: accounts.firstOrNull { it.type == "bank" }?.id ?: 0L) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val k = h?.kind ?: kind
    val step = InvKind.step(k)
    val q = qty.num() * step
    val pr = price.num().toLong()
    val fe = fee.num().toLong()
    val title = when (mode) { "new" -> "Tambah investasi"; "buy" -> "Beli lagi ${h?.code}"; "sell" -> "Jual ${h?.code}"; else -> "Dividen ${h?.code}" }

    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader(title, onDismiss, size = 20)
            Text(
                when (mode) { "dividend" -> "Masuk ke rekening yang dipilih"; "sell" -> "Rata-rata beli tidak berubah"; else -> "Harga rata-rata dihitung otomatis" },
                fontSize = 13.sp, color = K.Muted,
            )
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                if (mode == "new") Row(Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InvKind.all.forEach { (key, label) ->
                        Text(
                            label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (kind == key) Color.White else K.Ink2,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (kind == key) K.Green else K.Field).pressable { kind = key; quote = null }.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(K.Yellow).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mode == "new") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(K.Tile).padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(if (kind == "stock") "Kode saham" else "Kode / nama singkat", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
                                BasicTextField(
                                    value = code, onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(10); quote = null }, singleLine = true,
                                    textStyle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink), cursorBrush = SolidColor(K.Green), modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (kind == "stock") Box(
                                Modifier.height(54.dp).clip(RoundedCornerShape(16.dp)).background(K.Green).pressable {
                                    if (code.isBlank() || checking) return@pressable
                                    checking = true; err = null
                                    scope.launch {
                                        quote = withContext(Dispatchers.IO) { runCatching { Prices.quote(code) }.getOrNull() }
                                        checking = false
                                        quote?.let { qt -> if (name.isBlank()) name = qt.name; if (price.isBlank()) price = qt.price.toString() } ?: run { err = "Kode tidak ditemukan atau tidak ada internet" }
                                    }
                                }.padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(if (checking) "…" else "Cek", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
                        }
                        quote?.let { Text("✓ ${it.name} · ${it.price.grouped()}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.ChartIn, modifier = Modifier.padding(start = 4.dp)) }
                        if (kind != "stock") Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Tile).padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Nama", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
                            BasicTextField(
                                value = name, onValueChange = { name = it.take(40) }, singleLine = true,
                                textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink), cursorBrush = SolidColor(K.Green), modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (mode == "dividend") {
                        NumField("Dividen per lembar", price, { price = it }, Modifier.fillMaxWidth(), suffix = "× ${(p?.qty ?: 0.0).toLong().grouped()} lembar")
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumField("Jumlah ${InvKind.unit(k)}", qty, { qty = it }, Modifier.weight(1f), decimal = k != "stock")
                            NumField(if (k == "gold") "Harga / gram" else if (k == "stock") "Harga / lembar" else "Harga / unit", price, { price = it }, Modifier.weight(1.3f))
                        }
                        NumField("Biaya broker (opsional)", fee, { fee = it }, Modifier.fillMaxWidth())
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (mode != "dividend" && (h?.lastPrice ?: quote?.price ?: 0L) > 0) Pill("Pakai harga sekarang") { price = (h?.lastPrice?.takeIf { it > 0 } ?: quote?.price ?: 0L).toString() }
                        Pill(fmt(time, "d MMM yyyy") + " ▾") { pickDate = true }
                    }
                }

                // effect preview
                Column(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(K.SageCard).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val old = p
                    when (mode) {
                        "buy", "new" -> {
                            val oq = old?.qty ?: 0.0; val oc = old?.cost?.toDouble() ?: 0.0
                            val nq = oq + q
                            val navg = if (nq > 0) (oc + q * pr + fe) / nq else 0.0
                            if (old != null && oq > 0) PreviewRow("Rata-rata", "${Math.round(old.avg).grouped()} → ${Math.round(navg).grouped()}", true)
                            else PreviewRow("Rata-rata beli", Math.round(navg).grouped(), true)
                            PreviewRow("Total", if (old != null && oq > 0) "${InvKind.qtyText(k, oq)} → ${InvKind.qtyText(k, nq)}" else InvKind.qtyText(k, nq))
                            PreviewRow("Uang keluar", "Rp" + (q * pr + fe).toLong().grouped())
                        }
                        "sell" -> {
                            val gain = (q * pr - fe - (old?.avg ?: 0.0) * q).toLong()
                            PreviewRow("Untung/rugi jual", (if (gain >= 0) "+Rp" else "−Rp") + abs(gain).grouped(), true, if (gain < 0) K.Danger else K.ChartIn)
                            PreviewRow("Sisa", InvKind.qtyText(k, ((old?.qty ?: 0.0) - q).coerceAtLeast(0.0)))
                            PreviewRow("Uang masuk", "Rp" + (q * pr - fe).toLong().coerceAtLeast(0).grouped())
                        }
                        else -> PreviewRow("Total dividen", "Rp" + ((old?.qty ?: 0.0) * pr).toLong().grouped(), true, K.ChartIn)
                    }
                    Text(if (mode == "buy" || mode == "new") "Dicatat dari" else "Masuk ke", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.OnSage, modifier = Modifier.padding(top = 6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (accounts.map { it.id to it.name } + (0L to "Tanpa akun")).forEach { (id, n) ->
                            Text(
                                n, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = if (accountId == id) Color.White else K.Ink2,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (accountId == id) K.Green else K.Tile).pressable { accountId = id }.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
            err?.let { Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.Danger, modifier = Modifier.padding(top = 8.dp)) }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                RedButton(if (mode == "sell") "Simpan penjualan" else if (mode == "dividend") "Simpan dividen" else "Simpan pembelian", Modifier.weight(1.5f)) {
                    err = when {
                        mode == "new" && code.isBlank() -> "Isi kode terlebih dahulu"
                        mode != "dividend" && q <= 0 -> "Isi jumlah terlebih dahulu"
                        pr <= 0 -> "Isi harga terlebih dahulu"
                        mode == "sell" && q > (p?.qty ?: 0.0) + 0.0001 -> "Jumlah melebihi yang dimiliki"
                        else -> null
                    }
                    if (err != null) return@RedButton
                    val holding = h ?: Holding(
                        kind = kind, code = code.trim(), name = name.trim().ifBlank { quote?.name ?: code.trim() },
                        lastPrice = quote?.price ?: pr, prevClose = quote?.prevClose ?: 0, priceTime = quote?.time ?: System.currentTimeMillis(),
                    )
                    val type = if (mode == "new") "buy" else mode
                    val tq = if (mode == "dividend") p?.qty ?: 0.0 else q
                    vm.addInvTxn(holding, InvTxn(holdingId = holding.id, type = type, qty = tq, price = pr, fee = if (mode == "dividend") 0 else fe, time = time), accountId, newHolding = h == null)
                    onDismiss()
                }
            }
        }
    }
    if (pickDate) CalendarCard(time, onDismiss = { pickDate = false }, withTime = false) { time = it; pickDate = false }
}

@Composable
private fun Pill(text: String, onClick: () -> Unit) {
    Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Green, modifier = Modifier.clip(RoundedCornerShape(50)).background(K.Tile).pressable(onClick).padding(horizontal = 12.dp, vertical = 6.dp))
}

@Composable
private fun PreviewRow(label: String, value: String, strong: Boolean = false, color: Color = K.Ink) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.OnSage, modifier = Modifier.weight(1f))
        Text(value, fontSize = if (strong) 16.sp else 13.5.sp, fontWeight = FontWeight.ExtraBold, color = if (strong && color == K.Ink) K.Green else color, maxLines = 1)
    }
}

/** One-number sheet (manual price, target price). */
@Composable
private fun NumberSheet(title: String, label: String, initial: Long, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var v by remember { mutableStateOf(if (initial > 0) initial.toString() else "") }
    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader(title, onDismiss, size = 20)
            Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(K.Yellow).padding(12.dp)) {
                NumField(label, v, { v = it }, Modifier.fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                RedButton("Simpan", Modifier.weight(1f)) { onSave(v.num().toLong()) }
            }
        }
    }
}

@Composable
private fun ConfirmSheet(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader(title, onDismiss, size = 19)
            Text(body, fontSize = 14.sp, color = K.Muted, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                RedButton("Hapus", Modifier.weight(1f), onClick = onConfirm)
            }
        }
    }
}
