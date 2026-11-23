package id.kantong.app.ui.screens

import id.kantong.app.data.short

import id.kantong.app.data.startOfDay

import id.kantong.app.data.startOfWeek

import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.ui.draw.drawBehind

import androidx.compose.foundation.border

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
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
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
    var splitTx by remember { mutableStateOf<Txn?>(null) }
    var budget by remember { mutableStateOf(false) }
    var review by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(id.kantong.app.data.LaunchAction.review) {
        if (id.kantong.app.data.LaunchAction.review) { id.kantong.app.data.LaunchAction.review = false; if (d.pendingDetected.isNotEmpty()) review = true }
    }
    androidx.compose.runtime.LaunchedEffect(id.kantong.app.data.LaunchAction.budget) {
        if (id.kantong.app.data.LaunchAction.budget) { id.kantong.app.data.LaunchAction.budget = false; budget = true }
    }
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
        // header (R89): logo left, two-line ledger pill right
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CatetLogo(30.dp)
            Spacer(Modifier.width(7.dp))
            Text("catet", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, letterSpacing = (-0.2).sp)
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.anchor(ledgerAnchor).height(46.dp).clip(RoundedCornerShape(23.dp)).background(K.Tile)
                    .border(1.dp, K.Line, RoundedCornerShape(23.dp)).pressable { ledgerMenu = true }.padding(start = 5.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CatIcon(ledger?.icon ?: "ledger", 34.dp)
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(ledger?.name ?: "Semua buku", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1, lineHeight = 15.sp, modifier = Modifier.widthIn(max = 110.dp))
                    Text("Buku kas", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, lineHeight = 12.sp)
                }
                Icon(Icons.Rounded.KeyboardArrowDown, "Pilih buku kas", tint = K.Ink, modifier = Modifier.padding(start = 2.dp).size(17.dp))
            }
        }

        // title: viewed month (tap to change) with a date line; two-tone search on the right
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f).pressable { period = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(fmt(from, "MMMM yyyy"), fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, letterSpacing = (-0.3).sp, maxLines = 1)
                    Icon(Icons.Rounded.KeyboardArrowDown, "Pilih bulan", tint = K.Ink, modifier = Modifier.size(23.dp))
                }
                Text(
                    if (current) fmt(System.currentTimeMillis(), "EEEE, d MMM") + " · sisa $daysLeft hari" else "${monthTx.size} catatan",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.Muted,
                )
            }
            // streak chip: solid when today is recorded, dashed while it still waits for a record
            val streak = remember(d.allTxns) { streakOf(d) }
            Row(
                Modifier.padding(end = 8.dp).height(46.dp).clip(RoundedCornerShape(23.dp)).background(if (streak.today) K.Yellow else K.Tile)
                    .drawBehind {
                        if (!streak.today) drawRoundRect(
                            K.ChartOut, cornerRadius = androidx.compose.ui.geometry.CornerRadius(23.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                        )
                    }
                    .pressable { nav.go(R.ACHIEVE) }.padding(start = 10.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, "Pencapaian", tint = K.ChartOut, modifier = Modifier.size(23.dp))
                Spacer(Modifier.width(3.dp))
                Text("${streak.current}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = if (streak.today) K.Ink else K.Muted)
            }
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(K.Tile).border(1.dp, K.Line, CircleShape).pressable { nav.go(R.SEARCH) },
                contentAlignment = Alignment.Center,
            ) { CatIcon("search", 28.dp) }
        }

        // yellow balance card
        CCard(K.Yellow, Modifier.padding(top = 16.dp), radius = 30.dp) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (current) "Sisa uang bulan ini" else "Sisa uang", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
                        Icon(
                            if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (hidden) "Tampilkan nominal" else "Sembunyikan nominal",
                            tint = K.OnYellow, modifier = Modifier.padding(start = 6.dp).size(28.dp).clip(CircleShape).pressable { Prefs.hideAmounts = !hidden }.padding(4.dp),
                        )
                    }
                    Money(income - spent, 46.sp, Modifier.padding(top = 4.dp), hidden = hidden)
                }
                if (budgetTotal > 0 && current) {
                    Column(Modifier.pressable { budget = true }, horizontalAlignment = Alignment.CenterHorizontally) {
                        BudgetRing(v.spentMonth.toFloat() / budgetTotal, 64.dp, 9.dp) {
                            Text("${(v.spentMonth * 100 / budgetTotal)}%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = K.Green)
                        }
                        Text("anggaran", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                InOutBox("↙ Masuk", "+" + income.grouped(), K.Income, hidden, Modifier.weight(1f))
                InOutBox("↗ Keluar", "−" + spent.grouped(), K.Expense, hidden, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("coinbag", "Pemasukan") { vm.startAdd("in"); nav.go(R.add()) }
                QuickTile("bill", "Pengeluaran") { vm.startAdd("out"); nav.go(R.add()) }
                QuickTile("transfer", "Transfer") { vm.startAdd("transfer"); nav.go(R.add()) }
            }
        }

        // pink card: everything waiting to be recorded — notification detections first, then bills due
        if (current) {
            val due = d.bills.filter { it.paidMonthKey != monthKey() && it.dueDay <= dayOfMonth() + 3 }.sortedBy { it.dueDay }
            val found = d.pendingDetected
            val waiting = found.size + due.size
            val x = found.firstOrNull()
            val b = due.firstOrNull()
            if (x != null || b != null) {
                val soft = K.OnPink
                CCard(K.Pink, Modifier.padding(top = 14.dp), radius = 30.dp) {
                    // label row: what this card is, and how many are waiting
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Menunggu dicatat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = soft, modifier = Modifier.weight(1f))
                        if (waiting > 1) Text(
                            "1 dari $waiting", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = soft,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x66FFFFFF)).padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x80FFFFFF)), contentAlignment = Alignment.Center) {
                            CatIcon(if (x != null) d.category(x.categoryId)?.icon ?: "bill" else b!!.icon.ifBlank { "bill" }, 30.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(x?.merchant ?: b!!.name, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1)
                            if (x != null) Row(verticalAlignment = Alignment.CenterVertically) {
                                // bell = read from a notification, so it never gets mixed up with a bill
                                Icon(Icons.Rounded.NotificationsActive, null, tint = soft, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${x.app} · ${whenText(x.time)}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = soft, maxLines = 1)
                            } else Text(
                                if (b!!.dueDay < dayOfMonth()) "Lewat tanggal ${b.dueDay}" else "Jatuh tempo tanggal ${b.dueDay}",
                                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = soft,
                            )
                        }
                        val amt = if (x != null) (if (x.kind == "in") "+" else "−") + x.amount.grouped() else "−" + b!!.amount.grouped()
                        Text(if (hidden) "•••••" else amt, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        // secondary action blends into the pink card; the green one is the clear next step
                        Box(
                            Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(15.dp)).background(Color(0x73FFFFFF))
                                .pressable { if (x != null) review = true else nav.go(R.RECURRING) },
                            contentAlignment = Alignment.Center,
                        ) { Text(if (x != null) "Tinjau" else "Ubah", fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = soft) }
                        Row(
                            Modifier.weight(1.4f).height(46.dp).clip(RoundedCornerShape(15.dp)).background(K.Green)
                                .pressable {
                                    if (x != null) vm.saveDetected(x, x.categoryId, x.accountId)
                                    else d.accounts.firstOrNull()?.let { vm.payBill(b!!, it.id) }
                                },
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
                        Text("Jatah harian", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.OnSage, modifier = Modifier.weight(1f))
                        CChip("Sisa $daysLeft hari", bg = K.Tile, height = 28.dp)
                    }
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                        Money(v.safePerDay.coerceAtLeast(0), 38.sp, hidden = hidden)
                        Text(" per hari", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.OnSage, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    WeekStrip(v, v.safePerDay)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.Tile).pressable { budget = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CatIcon("budget", 32.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Anggaran terpakai", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                            Text(
                                if (hidden) "•••••" else "Rp${v.spentMonth.short().replace(" ", "")} dari Rp${budgetTotal.short().replace(" ", "")}",
                                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1,
                            )
                        }
                        Text("Lihat ›", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = K.Green)
                    }
                } else {
                    Text("Jatah harian", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.OnSage)
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
            id.kantong.app.ui.components.CEmpty(
                "Belum ada catatan di ${fmt(from, "MMMM")}. Ketuk Pengeluaran atau Pemasukan di atas untuk mulai mencatat.", "pencil",
                Modifier.padding(top = 6.dp).clip(RoundedCornerShape(24.dp)).background(K.Card),
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
    if (period) PeriodSheet(onDismiss = { period = false }, d = d)
    if (budget) BudgetSheet(d, vm) { budget = false }
    if (review) DetectReviewSheet(d, vm) { review = false }
    BadgeWatcher(d)
    open?.let { t ->
        val cur = d.allTxns.firstOrNull { it.id == t.id } ?: t
        RecordDetailCard(
            d, cur, onDismiss = { open = null }, onEdit = { open = null; nav.go(R.add(cur.id)) },
            onDelete = { vm.deleteTxn(cur); open = null }, onStar = { vm.toggleStar(cur) }, onReimburse = { vm.setReimburse(cur, it) },
            onSplit = { open = null; splitTx = cur },
        )
    }
    splitTx?.let { s -> SplitSheet(d, vm, s) { splitTx = null } }
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
        Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(22.dp)).background(K.Tile).pressable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        CatIcon(icon, 40.dp)
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1)
    }
}

/** Masuk / Keluar box on the yellow card: soft label, bold amount. */
@Composable
private fun InOutBox(label: String, value: String, color: Color, hidden: Boolean, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color(0x8CFFFFFF)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
        Text(if (hidden) "•••••" else value, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1)
    }
}

/**
 * This week (Mon–Sun) against the daily allowance: bar height = spent ÷ allowance, orange when over,
 * today outlined with a dashed border, later days empty.
 */
@Composable
private fun WeekStrip(v: AppData, perDay: Long) {
    val dayMs = 86_400_000L
    val monday = startOfWeek()
    val todayIdx = ((startOfDay() - monday) / dayMs).toInt()
    val spent = (0..6).map { i ->
        val s = monday + i * dayMs
        v.txns.filter { it.kind == "out" && it.time in s until s + dayMs }.sumOf { it.amount }
    }
    val names = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    val over = (0..todayIdx).count { perDay > 0 && spent[it] > perDay }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..6).forEach { i ->
            val ratio = if (perDay > 0) spent[i].toFloat() / perDay else 0f
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xA6FFFFFF)).let { m ->
                        if (i == todayIdx) m.drawBehind {
                            drawRoundRect(
                                K.SageMid, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    1.5.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                                ),
                            )
                        } else m
                    },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (i <= todayIdx && spent[i] > 0) Box(
                        Modifier.fillMaxWidth().fillMaxHeight((ratio / 1.4f).coerceIn(0.1f, 1f)).clip(RoundedCornerShape(10.dp))
                            .background(if (ratio > 1f) K.ChartOut else K.Green.copy(alpha = 0.85f)),
                    )
                }
                Text(
                    names[i], fontSize = 11.sp, fontWeight = if (i == todayIdx) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (i == todayIdx) K.Ink else K.OnSage, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
    Text(
        if (over > 0) "Minggu ini · $over hari lewat jatah" else "Minggu ini · semua hari sesuai jatah",
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.OnSage, modifier = Modifier.padding(top = 4.dp),
    )
}
