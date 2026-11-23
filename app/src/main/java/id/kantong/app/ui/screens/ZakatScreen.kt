package id.kantong.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Prefs
import id.kantong.app.data.Prices
import id.kantong.app.data.Txn
import id.kantong.app.data.fmt
import id.kantong.app.data.grouped
import id.kantong.app.data.short
import id.kantong.app.data.startOfMonth
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.CEmpty
import id.kantong.app.ui.components.CList
import id.kantong.app.ui.components.CPicker
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CSection
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CalendarCard
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CatetSheet
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.MeowSegmented
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.SelOpt
import id.kantong.app.ui.components.SheetHeader
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val NISAB_GRAM = 85
private const val YEAR_MS = 365L * 86_400_000L

/** Spending records that are zakat (by category or note). */
internal fun isZakat(d: AppData, t: Txn) = t.kind == "out" && (d.rootOf(t.categoryId)?.name?.contains("zakat", true) == true || d.category(t.categoryId)?.name?.contains("zakat", true) == true)

/** Spending records that are sedekah / infak / donations (charity icon or name), excluding zakat. */
internal fun isSedekah(d: AppData, t: Txn): Boolean {
    if (t.kind != "out" || isZakat(d, t)) return false
    val c = d.category(t.categoryId); val r = d.rootOf(t.categoryId)
    return listOfNotNull(c, r).any { it.icon == "charity" || it.name.contains("sedekah", true) || it.name.contains("infak", true) || it.name.contains("donasi", true) || it.name.contains("wakaf", true) }
}

/**
 * Zakat & sedekah: zakat maal against the 85 g gold nisab (2.5% once reached), monthly zakat on income,
 * a sedekah target, the asset breakdown, settings (haul start, gold price, target), history and two actions.
 */
@Composable
fun ZakatScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    val hidden = Prefs.hideAmounts
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf(false) }
    var giving by remember { mutableStateOf(false) }
    var pickHaul by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf(false) }

    val gold = Prefs.zakatGold
    fun refreshGold() {
        if (busy) return
        busy = true
        scope.launch {
            withContext(Dispatchers.IO) { runCatching { Prices.gold() }.getOrNull() }?.let { t ->
                (t["ANTAM"] ?: t.values.firstOrNull())?.let { Prefs.zakatGold = it.buyback }
            }
            busy = false
        }
    }
    LaunchedEffect(Unit) { if (Prefs.zakatGold <= 0L) refreshGold() }

    val cash = d.assetsTotal
    val invest = d.investValue
    val receivable = d.receivable
    val debt = kotlin.math.abs(d.liabilitiesTotal) + d.payable
    val total = (cash + invest + receivable - debt).coerceAtLeast(0)
    val nisab = gold * NISAB_GRAM
    val haulFrom = Prefs.haulStart.takeIf { it > 0 }?.let { s -> var x = s; val now = System.currentTimeMillis(); while (x + YEAR_MS <= now) x += YEAR_MS; x } ?: (System.currentTimeMillis() - YEAR_MS)
    val zakatPaidYear = d.allTxns.filter { isZakat(d, it) && it.time >= haulFrom }.sumOf { it.amount }
    val maalDue = if (nisab > 0 && total >= nisab) (total * 25 / 1000 - zakatPaidYear).coerceAtLeast(0) else 0L
    val monthStart = startOfMonth()
    val income = d.allTxns.filter { it.kind == "in" && it.time >= monthStart }.sumOf { it.amount }
    val incomeNisab = nisab / 12
    val zakatPaidMonth = d.allTxns.filter { isZakat(d, it) && it.time >= monthStart }.sumOf { it.amount }
    val incomeDue = if (incomeNisab > 0 && income >= incomeNisab) (income * 25 / 1000 - zakatPaidMonth).coerceAtLeast(0) else 0L
    val sedekahMonth = d.allTxns.filter { isSedekah(d, it) && it.time >= monthStart }.sumOf { it.amount }
    val target = Prefs.sedekahTarget
    val history = d.allTxns.filter { isZakat(d, it) || isSedekah(d, it) }.sortedByDescending { it.time }.take(5)

    Column(Modifier.fillMaxSize().background(K.Bg).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 44.dp) }
        CTitle("Zakat & sedekah", Modifier.padding(top = 8.dp), subtitle = "Hitung zakat, pantau sedekah")

        // zakat maal
        Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(K.SageCard).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Zakat maal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.OnSage, modifier = Modifier.weight(1f))
                StatusChip(
                    when { nisab <= 0 -> "Menunggu harga emas"; total < nisab -> "Belum wajib"; maalDue == 0L -> "Sudah dibayar"; else -> "Wajib" },
                    if (maalDue > 0) K.Green else K.OnSage,
                )
            }
            Money(maalDue, 32.sp, hidden = hidden)
            val pct = if (nisab > 0) (total.toFloat() / nisab).coerceIn(0f, 1f) else 0f
            Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xBFFFFFFF))) {
                Box(Modifier.fillMaxWidth(pct).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(K.Green))
            }
            Row(Modifier.padding(top = 7.dp)) {
                Text("Harta ${if (hidden) "•••" else "Rp" + total.short().replace(" ", "")}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = K.OnSage, modifier = Modifier.weight(1f))
                Text(if (nisab > 0) "nisab Rp${nisab.short().replace(" ", "")}" else "nisab …", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = K.OnSage)
            }
        }

        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(K.Yellow).padding(14.dp)) {
                Text("Zakat penghasilan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
                Text(if (hidden) "•••" else "Rp" + incomeDue.short().replace(" ", ""), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                Text(if (incomeNisab > 0 && income >= incomeNisab) (if (incomeDue == 0L) "sudah dibayar" else "2,5% bulan ini") else "belum wajib", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
            }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(K.Pink).padding(14.dp)) {
                Text("Sedekah bulan ini", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.OnPink)
                Text(if (hidden) "•••" else "Rp" + sedekahMonth.short().replace(" ", ""), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                val sp = if (target > 0) (sedekahMonth.toFloat() / target).coerceIn(0f, 1f) else 0f
                Box(Modifier.padding(top = 7.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xB3FFFFFF))) {
                    Box(Modifier.fillMaxWidth(sp).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(K.Green))
                }
            }
        }

        CSection("Rincian harta", Modifier.padding(top = 24.dp, bottom = 10.dp))
        CList {
            LineRow("Uang", cash, hidden, divider = false)
            LineRow("Investasi", invest, hidden)
            LineRow("Piutang lancar", receivable, hidden)
            LineRow("Utang & kewajiban", -debt, hidden)
            LineRow("Total", total, hidden, strong = true)
        }
        CList(Modifier.padding(top = 10.dp)) {
            SetRow("Awal haul", if (Prefs.haulStart > 0) fmt(Prefs.haulStart, "d MMM yyyy") else "Atur ›", divider = false) { pickHaul = true }
            SetRow("Harga emas · Antam buyback", if (busy) "…" else if (gold > 0) "${gold.grouped()}/g ↻" else "Ambil ↻") { refreshGold() }
            SetRow("Target sedekah bulanan", if (target > 0) target.grouped() + " ›" else "Atur ›") { editTarget = true }
        }

        CSection("Riwayat", Modifier.padding(top = 24.dp, bottom = 10.dp), action = "Lihat semua") { nav.go(R.SEARCH) }
        if (history.isEmpty()) CEmpty("Belum ada catatan zakat atau sedekah.", "charity", Modifier.clip(RoundedCornerShape(22.dp)).background(K.Card))
        else CList {
            history.forEachIndexed { i, t ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
                Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    CatIcon(d.category(t.categoryId)?.icon ?: "charity", 30.dp)
                    Spacer(Modifier.padding(start = 12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.merchant.ifBlank { t.note }.ifBlank { d.category(t.categoryId)?.name ?: "Sedekah" }, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1)
                        Text("${d.category(t.categoryId)?.name ?: ""} · ${fmt(t.time, "d MMM")}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                    }
                    Text(if (hidden) "•••" else "−" + t.amount.grouped(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                }
            }
        }

        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(17.dp)).background(K.Field).pressable { paying = true }, contentAlignment = Alignment.Center) {
                Text("Bayar zakat", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
            }
            RedButton("Catat sedekah", Modifier.weight(1.3f)) { giving = true }
        }
        Text(
            "Perkiraan sesuai pedoman BAZNAS (2,5%, nisab 85 g emas).", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 40.dp),
        )
    }

    if (paying) CharitySheet(d, vm, zakat = true, suggestMaal = maalDue, suggestIncome = incomeDue) { paying = false }
    if (giving) CharitySheet(d, vm, zakat = false, suggestMaal = 0, suggestIncome = 0) { giving = false }
    if (pickHaul) CalendarCard(if (Prefs.haulStart > 0) Prefs.haulStart else System.currentTimeMillis(), onDismiss = { pickHaul = false }, withTime = false) { Prefs.haulStart = it; pickHaul = false }
    if (editTarget) TargetSheet(Prefs.sedekahTarget, { editTarget = false }) { Prefs.sedekahTarget = it; editTarget = false }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = color, modifier = Modifier.clip(RoundedCornerShape(50)).background(K.Tile).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
private fun LineRow(label: String, value: Long, hidden: Boolean, divider: Boolean = true, strong: Boolean = false) {
    if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, fontWeight = if (strong) FontWeight.ExtraBold else FontWeight.SemiBold, color = if (strong) K.Ink else K.Muted, modifier = Modifier.weight(1f))
        Text(
            if (hidden) "•••••" else (if (value < 0) "−" else "") + kotlin.math.abs(value).grouped(),
            fontSize = if (strong) 16.sp else 15.sp, fontWeight = FontWeight.ExtraBold, color = if (strong) K.Green else K.Ink,
        )
    }
}

@Composable
private fun SetRow(label: String, value: String, divider: Boolean = true, onClick: () -> Unit) {
    if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
    Row(Modifier.fillMaxWidth().pressable(onClick).padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
    }
}

/** Pay zakat (maal / penghasilan) or record sedekah (sedekah / infak / wakaf): a spending record in that category. */
@Composable
private fun CharitySheet(d: AppData, vm: AppViewModel, zakat: Boolean, suggestMaal: Long, suggestIncome: Long, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(if (zakat) (if (suggestMaal > 0 || suggestIncome == 0L) 0 else 1) else 0) }
    var amount by remember { mutableStateOf(if (zakat) (if (suggestMaal > 0) suggestMaal else suggestIncome).takeIf { it > 0 }?.toString() ?: "" else "") }
    var note by remember { mutableStateOf("") }
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var accountId by remember { mutableLongStateOf(d.accounts.firstOrNull()?.id ?: 0L) }
    var pick by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val types = if (zakat) listOf("Maal", "Penghasilan") else listOf("Sedekah", "Infak", "Wakaf")
    val amt = amount.num().toLong()

    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader(if (zakat) "Bayar zakat" else "Catat sedekah", onDismiss, size = 20)
            Text("Tercatat sebagai pengeluaran", fontSize = 13.sp, color = K.Muted)
            MeowSegmented(types, type, {
                type = it
                if (zakat) amount = (if (it == 0) suggestMaal else suggestIncome).takeIf { v -> v > 0 }?.toString() ?: amount
            }, Modifier.padding(top = 12.dp).fillMaxWidth(), fill = true, fontSize = 14.sp)
            Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(K.Yellow).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Jumlah", amount, { amount = it }, Modifier.fillMaxWidth())
                if (!zakat) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Tile).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("Catatan (opsional)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
                    androidx.compose.foundation.text.BasicTextField(
                        value = note, onValueChange = { note = it.take(40) }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(K.Green), modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Pill(fmt(time, "d MMM yyyy") + " ▾") { pick = true } }
            }
            CPicker(
                "Dibayar dari", d.accounts.map { a -> SelOpt(a.id.toString(), a.name, "Saldo Rp" + d.balanceOf(a).grouped()) },
                accountId.toString(), Modifier.padding(top = 10.dp), title = "Pilih akun",
            ) { accountId = it.toLongOrNull() ?: accountId }
            err?.let { Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.Danger, modifier = Modifier.padding(top = 8.dp)) }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                RedButton("Simpan", Modifier.weight(1.4f)) {
                    err = when { amt <= 0 -> "Isi jumlah terlebih dahulu"; accountId <= 0 -> "Pilih akun terlebih dahulu"; else -> null }
                    if (err != null) return@RedButton
                    val label = (if (zakat) "Zakat " else "") + types[type]
                    vm.recordCharity(if (zakat) "Zakat" else "Sedekah", amt, accountId, note.ifBlank { label }, time)
                    onDismiss()
                }
            }
        }
    }
    if (pick) CalendarCard(time, onDismiss = { pick = false }, withTime = false) { time = it; pick = false }
}

@Composable
private fun TargetSheet(initial: Long, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var v by remember { mutableStateOf(if (initial > 0) initial.toString() else "") }
    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader("Target sedekah bulanan", onDismiss, size = 20)
            Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(K.Yellow).padding(12.dp)) {
                NumField("Jumlah per bulan", v, { v = it }, Modifier.fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                RedButton("Simpan", Modifier.weight(1f)) { onSave(v.num().toLong()) }
            }
        }
    }
}
