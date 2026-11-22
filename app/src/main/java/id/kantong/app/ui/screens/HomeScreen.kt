package id.kantong.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Prefs
import id.kantong.app.data.Txn
import id.kantong.app.data.ViewPeriod
import id.kantong.app.data.dayOfMonth
import id.kantong.app.data.daysInMonth
import id.kantong.app.data.fmt
import id.kantong.app.data.grouped
import id.kantong.app.data.monthKey
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.BudgetRing
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CChip
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CSection
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CatetLogo
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.PeriodSheet
import id.kantong.app.ui.components.PillMenu
import id.kantong.app.ui.components.PillOption
import id.kantong.app.ui.components.RecordDetailCard
import id.kantong.app.ui.components.RecordTimeline
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.anchor
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.components.rememberAnchor
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone

/**
 * Beranda (R89 style): ledger pill, viewed month as the title, yellow balance card with income/expense,
 * budget ring and three quick actions, a pink card for the bill waiting to be recorded, a sage card for the
 * daily allowance, then the latest records. Every tap leads to one clear place.
 */
@Composable
fun HomeScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    var open by remember { mutableStateOf<Txn?>(null) }
    var budget by remember { mutableStateOf(false) }
    var period by remember { mutableStateOf(false) }
    var ledgerMenu by remember { mutableStateOf(false) }
    val ledgerAnchor = rememberAnchor()
    val v = d.forLedger(Prefs.ledger)
    val hidden = Prefs.hideAmounts
    val ledger = d.ledger(Prefs.ledger)
    val from = ViewPeriod.start(); val to = ViewPeriod.end()
    val monthTx = v.txns.filter { it.time in from until to }
    val income = monthTx.filter { it.kind == "in" }.sumOf { it.amount }
    val spent = monthTx.filter { it.kind == "out" }.sumOf { it.amount }
    val current = ViewPeriod.isCurrent()
    val daysLeft = (daysInMonth() - dayOfMonth() + 1).coerceAtLeast(1)
    val budgetTotal = v.budgetTotal
    val endOfMonth = to - 1

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        // header: logo left; search + ledger pill right (one line each, nothing stacked)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CatetLogo(32.dp)
            Spacer(Modifier.width(7.dp))
            Text("catet", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, letterSpacing = (-0.2).sp)
            Spacer(Modifier.weight(1f))
            CRoundButton(Icons.Rounded.Search, "Cari transaksi", { nav.go(R.SEARCH) }, size = 42.dp, outlined = true)
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier.anchor(ledgerAnchor).height(42.dp).clip(RoundedCornerShape(21.dp)).background(K.Card)
                    .pressable { ledgerMenu = true }.padding(start = 4.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(K.Tile), contentAlignment = Alignment.Center) { CatIcon(ledger?.icon ?: "ledger", 23.dp) }
                Spacer(Modifier.width(7.dp))
                Text(ledger?.name ?: "Semua buku", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1, modifier = Modifier.widthIn(max = 110.dp))
                Icon(Icons.Rounded.KeyboardArrowDown, "Pilih buku kas", tint = K.Ink, modifier = Modifier.size(18.dp))
            }
        }

        // title: the viewed month only (tap to change)
        Row(Modifier.padding(top = 22.dp).pressable { period = true }, verticalAlignment = Alignment.CenterVertically) {
            Text(fmt(from, "MMMM yyyy"), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, letterSpacing = (-0.3).sp, maxLines = 1)
            Icon(Icons.Rounded.KeyboardArrowDown, "Pilih bulan", tint = K.Ink, modifier = Modifier.size(26.dp))
        }

        // yellow balance card
        CCard(K.Yellow, Modifier.padding(top = 16.dp), radius = 30.dp) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (current) "Sisa uang bulan ini" else "Sisa uang", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
                        Icon(
                            if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (hidden) "Tampilkan nominal" else "Sembunyikan nominal",
                            tint = K.Muted, modifier = Modifier.padding(start = 6.dp).size(28.dp).clip(CircleShape).pressable { Prefs.hideAmounts = !hidden }.padding(4.dp),
                        )
                    }
                    Money(income - spent, 46.sp, Modifier.padding(top = 4.dp), hidden = hidden)
                }
                if (budgetTotal > 0 && current) {
                    Column(Modifier.pressable { budget = true }, horizontalAlignment = Alignment.CenterHorizontally) {
                        BudgetRing(v.spentMonth.toFloat() / budgetTotal, 64.dp, 9.dp) {
                            Text("${(v.spentMonth * 100 / budgetTotal)}%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = K.Green)
                        }
                        Text("anggaran", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7A6F5C))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                InOut(Icons.Rounded.ArrowUpward, K.SageMid, "Masuk", income, K.Income, hidden)
                Box(Modifier.width(1.5.dp).height(40.dp).background(K.YellowLine))
                Spacer(Modifier.width(16.dp))
                InOut(Icons.Rounded.ArrowDownward, Color(0xFFF2D9D5), "Keluar", spent, K.Expense, hidden)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("coinbag", "Pemasukan") { vm.startAdd("in"); nav.go(R.add()) }
                QuickTile("bill", "Pengeluaran") { vm.startAdd("out"); nav.go(R.add()) }
                QuickTile("transfer", "Transfer") { vm.startAdd("transfer"); nav.go(R.add()) }
            }
        }

        // pink card: bills due and not yet recorded this month
        if (current) {
            val due = d.bills.filter { it.paidMonthKey != monthKey() && it.dueDay <= dayOfMonth() + 3 }.sortedBy { it.dueDay }
            due.firstOrNull()?.let { b ->
                val soft = Color(0xFF5B514D)
                CCard(K.Pink, Modifier.padding(top = 14.dp), radius = 30.dp) {
                    // label row: what this card is, and how many are waiting
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Menunggu dicatat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = soft, modifier = Modifier.weight(1f))
                        if (due.size > 1) Text(
                            "1 dari ${due.size}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = soft,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x66FFFFFF)).padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x80FFFFFF)), contentAlignment = Alignment.Center) {
                            CatIcon(b.icon.ifBlank { "bill" }, 30.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.name, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1)
                            Text(if (b.dueDay < dayOfMonth()) "Lewat tanggal ${b.dueDay}" else "Jatuh tempo tanggal ${b.dueDay}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = soft)
                        }
                        Text(if (hidden) "•••••" else "−" + b.amount.grouped(), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        // secondary action blends into the pink card; the green one is the clear next step
                        Box(
                            Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(15.dp)).background(Color(0x73FFFFFF)).pressable { nav.go(R.RECURRING) },
                            contentAlignment = Alignment.Center,
                        ) { Text("Ubah", fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = soft) }
                        Row(
                            Modifier.weight(1.4f).height(46.dp).clip(RoundedCornerShape(15.dp)).background(K.Green)
                                .pressable { d.accounts.firstOrNull()?.let { vm.payBill(b, it.id) } },
                            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Catat", fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // sage card: daily allowance from the monthly budget
        if (current) {
            CCard(K.SageCard, Modifier.padding(top = 14.dp), radius = 30.dp) {
                if (budgetTotal > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Jatah harian", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4E4A42), modifier = Modifier.weight(1f))
                        CChip("Sisa $daysLeft hari", bg = K.Tile, height = 28.dp)
                    }
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                        Money(v.safePerDay.coerceAtLeast(0), 38.sp, hidden = hidden)
                        Text(" per hari", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4F4A43), modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.Tile).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CatIcon("history", 32.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Anggaran bulan ini", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted)
                            Text(
                                if (hidden) "•••••" else "Rp${v.spentMonth.grouped()} dari Rp${budgetTotal.grouped()}",
                                fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(17.dp)).background(K.Tile).pressable { budget = true },
                        contentAlignment = Alignment.Center,
                    ) { Text("Lihat anggaran  ›", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Green) }
                } else {
                    Text("Jatah harian", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4E4A42))
                    Text("Atur anggaran bulanan supaya tahu jatah belanja per hari sampai akhir bulan.", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = K.Muted, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(12.dp))
                    RedButton("Atur anggaran", Modifier.fillMaxWidth()) { budget = true }
                }
            }
        }

        // latest records
        CSection("Riwayat", Modifier.padding(top = 26.dp, bottom = 4.dp), action = "Lihat semua") { nav.go(R.SEARCH) }
        val recent = monthTx.take(40)
        if (recent.isEmpty()) {
            Text(
                "Belum ada catatan di ${fmt(from, "MMMM")}. Ketuk Pengeluaran atau Pemasukan di atas untuk mulai mencatat.",
                fontSize = 15.sp, fontWeight = FontWeight.Medium, color = K.Muted, lineHeight = 21.sp, modifier = Modifier.padding(vertical = 16.dp, horizontal = 2.dp),
            )
        } else {
            RecordTimeline(d, recent, onOpen = { open = it })
        }
        Spacer(Modifier.height(120.dp))
    }

    if (ledgerMenu) {
        PillMenu(ledgerAnchor.rect, onDismiss = { ledgerMenu = false }, title = "Pilih buku kas") {
            PillOption("Semua buku", Tone.PEACH, Prefs.ledger == 0L, { Prefs.ledger = 0L; ledgerMenu = false }, icon = "ledger")
            d.activeLedgers.forEach { l ->
                PillOption(l.name, Tone.of(l.tone), Prefs.ledger == l.id, { Prefs.ledger = l.id; ledgerMenu = false }, icon = l.icon)
            }
            PillOption("Kelola buku kas", Tone.PEACH, false, { ledgerMenu = false; nav.go(R.LEDGERS) }, glyph = Icons.Rounded.Settings)
        }
    }
    if (period) PeriodSheet(onDismiss = { period = false })
    if (budget) BudgetSheet(d, vm) { budget = false }
    open?.let { t ->
        val cur = d.allTxns.firstOrNull { it.id == t.id } ?: t
        RecordDetailCard(
            d, cur, onDismiss = { open = null }, onEdit = { open = null; nav.go(R.add(cur.id)) },
            onDelete = { vm.deleteTxn(cur); open = null }, onStar = { vm.toggleStar(cur) }, onReimburse = { vm.setReimburse(cur, it) },
        )
    }
}

@Composable
private fun RowScope.InOut(icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, label: String, value: Long, color: Color, hidden: Boolean) {
    Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(19.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = K.Ink, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B6252))
        }
        Text(if (hidden) "•••••" else "Rp${value.grouped()}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun RowScope.QuickTile(icon: String, label: String, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).height(92.dp).clip(RoundedCornerShape(22.dp)).background(K.Tile).pressable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        CatIcon(icon, 36.dp)
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1)
    }
}
