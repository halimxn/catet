package id.kantong.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Budget
import id.kantong.app.data.Prefs
import id.kantong.app.data.daysInMonth
import id.kantong.app.data.grouped
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CChip
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CatetLogo
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.accountIcon
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Outfit

/**
 * First-run introduction (R89 style): welcome, first ledger name, current balances, monthly budget.
 * Every step can be skipped; nothing is overwritten unless the user changes it.
 */
@Composable
fun OnboardingScreen(d: AppData, vm: AppViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val ledger = d.activeLedgers.firstOrNull()
    var ledgerName by rememberSaveable { mutableStateOf(ledger?.name ?: "Harian") }
    val accounts = remember(d.accounts) {
        listOfNotNull(
            d.accounts.firstOrNull { it.type == "cash" },
            d.accounts.firstOrNull { it.type == "bank" },
            d.accounts.firstOrNull { it.type == "ewallet" },
        ).ifEmpty { d.accounts.take(3) }
    }
    val balances = remember { mutableStateMapOf<Long, String>().apply { accounts.forEach { put(it.id, d.balanceOf(it).coerceAtLeast(0).toString()) } } }
    val monthBudgets = d.budgets.filter { it.period == "month" }
    var budget by rememberSaveable { mutableStateOf(if (monthBudgets.isEmpty()) "" else monthBudgets.sumOf { it.amount }.toString()) }

    fun finish() {
        // ledger name
        if (ledger != null && ledgerName.isNotBlank() && ledgerName.trim() != ledger.name) vm.saveLedger(ledger.copy(name = ledgerName.trim()))
        // balances: keep history, shift the opening balance so today's balance matches what the user typed
        accounts.forEach { a ->
            val typed = balances[a.id]?.toLongOrNull() ?: return@forEach
            val now = d.balanceOf(a)
            if (typed != now.coerceAtLeast(0)) vm.saveAccount(a.copy(openingBalance = a.openingBalance + (typed - now)))
        }
        // monthly budget: only created when there is none yet
        val amount = budget.toLongOrNull() ?: 0L
        if (monthBudgets.isEmpty() && amount > 0) {
            val ids = d.topCategories("out").joinToString(",") { it.id.toString() }
            vm.saveBudget(Budget(name = "Bulanan", period = "month", categoryIds = ids, amount = amount))
        }
        Prefs.onboarded = true
    }

    BackHandler(enabled = step > 0) { step-- }
    Column(Modifier.fillMaxSize().background(K.Bg).statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
        // progress + skip
        Row(Modifier.fillMaxWidth().padding(top = 14.dp).height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { i ->
                    Box(Modifier.width(if (i == step) 26.dp else 10.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (i <= step) K.Green else K.Notch))
                }
            }
            if (step < 3) Text("Lewati", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Muted, modifier = Modifier.pressable { Prefs.onboarded = true }.padding(8.dp))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
            when (step) {
                0 -> Welcome()
                1 -> StepHead("Buku kas pertama", "Satu buku kas untuk catatan harian. Nanti bisa ditambah, misalnya untuk rumah tangga atau usaha.")
                2 -> StepHead("Saldo saat ini", "Isi uang yang ada sekarang supaya sisa uang dan aset langsung benar. Boleh dilewati.")
                else -> StepHead("Anggaran bulanan", "Batas belanja sebulan. catet membaginya jadi jatah harian sampai akhir bulan.")
            }
            Spacer(Modifier.height(18.dp))
            when (step) {
                1 -> CCard(K.SageCard, radius = 28.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(K.Tile), contentAlignment = Alignment.Center) { CatIcon(ledger?.icon ?: "ledger", 30.dp) }
                        Spacer(Modifier.width(12.dp))
                        Text("Nama buku kas", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
                    }
                    Spacer(Modifier.height(12.dp))
                    Field(ledgerName, { ledgerName = it.take(24) }, "Misalnya: Harian")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Harian", "Pribadi", "Keluarga").forEach { s -> CChip(s, bg = K.Tile, selected = ledgerName == s) { ledgerName = s } }
                    }
                }
                2 -> CCard(K.Yellow, radius = 28.dp, padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    if (accounts.isEmpty()) Text("Belum ada akun. Tambahkan nanti dari tab Aset.", fontSize = 15.sp, color = K.Muted, modifier = Modifier.padding(vertical = 14.dp))
                    accounts.forEachIndexed { i, a ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(K.YellowLine))
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(K.Tile), contentAlignment = Alignment.Center) { CatIcon(accountIcon(a), 28.dp) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                                Text(when (a.type) { "cash" -> "Uang tunai"; "bank" -> "Rekening bank"; else -> "Dompet digital" }, fontSize = 13.sp, color = K.Muted)
                            }
                            MoneyField(balances[a.id] ?: "", { balances[a.id] = it }, Modifier.width(150.dp))
                        }
                    }
                }
                3 -> CCard(K.SageCard, radius = 28.dp) {
                    if (monthBudgets.isNotEmpty()) {
                        Text("Anggaran bulanan sudah ada", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                        Text("${monthBudgets.size} anggaran, total Rp${monthBudgets.sumOf { it.amount }.grouped()}. Ubah kapan saja lewat Saya › Anggaran.", fontSize = 14.sp, color = K.Muted, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        Text("Batas belanja per bulan", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
                        Spacer(Modifier.height(10.dp))
                        MoneyField(budget, { budget = it }, Modifier.fillMaxWidth(), big = true)
                        val amt = budget.toLongOrNull() ?: 0L
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Tile).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            CatIcon("history", 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Jatah harian", fontSize = 13.sp, color = K.Muted)
                                Text(if (amt > 0) "± Rp${(amt / daysInMonth()).grouped()} per hari" else "Isi anggaran untuk melihat jatah", fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Green)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3_000_000L, 5_000_000L, 8_000_000L).forEach { v -> CChip("${v / 1_000_000}jt", bg = K.Tile, selected = amt == v) { budget = v.toString() } }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) CreamButton("Kembali", Modifier.weight(1f)) { step-- }
            RedButton(when (step) { 0 -> "Mulai"; 3 -> "Selesai"; else -> "Lanjut" }, Modifier.weight(if (step > 0) 1.4f else 1f)) {
                if (step < 3) step++ else finish()
            }
        }
    }
}

@Composable
private fun Welcome() {
    CCard(K.Yellow, radius = 30.dp, padding = androidx.compose.foundation.layout.PaddingValues(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CatetLogo(56.dp)
            Spacer(Modifier.width(10.dp))
            Text("catet", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, letterSpacing = (-1).sp)
        }
        Text("Catet dulu, aman kemudian.", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = K.Ink2, modifier = Modifier.padding(top = 10.dp))
        Text("Catat uang harian dengan santai dan tahu jatahmu sampai gajian.", fontSize = 15.sp, color = K.Muted, modifier = Modifier.padding(top = 4.dp))
    }
    Spacer(Modifier.height(16.dp))
    listOf(
        Triple("bill", "Catat dalam hitungan detik", "Pilih kategori, ketik nominal, simpan."),
        Triple("envelope", "Jatah harian otomatis", "Anggaran bulanan dibagi rata sampai akhir bulan."),
        Triple("report", "Laporan yang mudah dibaca", "Lihat ke mana uang pergi tiap bulan."),
    ).forEach { (icon, title, sub) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(K.Card), contentAlignment = Alignment.Center) { CatIcon(icon, 32.dp) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                Text(sub, fontSize = 13.5.sp, color = K.Muted)
            }
        }
    }
}

@Composable
private fun StepHead(title: String, sub: String) {
    Text(title, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, letterSpacing = (-0.3).sp)
    Text(sub, fontSize = 15.sp, color = K.Muted, lineHeight = 21.sp, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, hint: String) {
    Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(K.Tile).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(hint, fontSize = 17.sp, color = K.Muted)
        BasicTextField(
            value, onChange, singleLine = true, cursorBrush = SolidColor(K.Green),
            textStyle = TextStyle(fontFamily = Outfit, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink), modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Digits-only money input shown with thousand separators. */
@Composable
private fun MoneyField(digits: String, onChange: (String) -> Unit, modifier: Modifier, big: Boolean = false) {
    val shown = digits.toLongOrNull()?.grouped() ?: ""
    Row(
        modifier.height(if (big) 60.dp else 46.dp).clip(RoundedCornerShape(15.dp)).background(K.Tile).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Rp", fontSize = if (big) 18.sp else 14.sp, fontWeight = FontWeight.ExtraBold, color = K.Green)
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            shown, { t -> onChange(t.filter { it.isDigit() }.take(13).trimStart('0')) }, singleLine = true, cursorBrush = SolidColor(K.Green),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                fontFamily = Outfit, fontSize = if (big) 28.sp else 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Green,
                textAlign = if (big) TextAlign.Start else TextAlign.End,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}
