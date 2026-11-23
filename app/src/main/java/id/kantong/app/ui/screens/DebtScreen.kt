package id.kantong.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Debt
import id.kantong.app.data.DebtPay
import id.kantong.app.data.DebtState
import id.kantong.app.data.Prefs
import id.kantong.app.data.Txn
import id.kantong.app.data.fmt
import id.kantong.app.data.grouped
import id.kantong.app.data.startOfDay
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.MeowSegmented
import id.kantong.app.ui.components.CList
import id.kantong.app.ui.components.CSection
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CalendarCard
import id.kantong.app.ui.components.CatetSheet
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.SheetHeader
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K
import java.util.Calendar

private fun initials(name: String) = name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "?" }
private fun lend(x: Debt) = x.direction == "lend"
private fun tint(x: Debt) = if (lend(x)) K.Green else K.Danger

@Composable
private fun Avatar(x: Debt, size: Int = 42) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(if (lend(x)) K.SageCard else K.Pink), contentAlignment = Alignment.Center) {
        Text(initials(x.person), fontSize = (size * 0.33f).sp, fontWeight = FontWeight.ExtraBold, color = tint(x))
    }
}

private fun dueText(s: DebtState): String = when {
    s.done -> "Lunas"
    s.overdueDays > 0 -> "lewat ${s.overdueDays} hari"
    s.nextDue > 0 -> fmt(s.nextDue, "d MMM")
    s.pays.isNotEmpty() -> "dicicil ${s.pays.size}×"
    else -> fmt(s.debt.time, "d MMM yyyy")
}

/** Utang & piutang: net position card, filter, and one row per loan. */
@Composable
fun DebtsScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    var filter by remember { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    val hidden = Prefs.hideAmounts
    val all = d.debtStates
    val list = when (filter) {
        1 -> all.filter { lend(it.debt) && !it.done }
        2 -> all.filter { !lend(it.debt) && !it.done }
        3 -> all.filter { it.done }
        else -> all.filter { !it.done }
    }.sortedWith(compareByDescending<DebtState> { it.overdueDays }.thenBy { if (it.nextDue > 0) it.nextDue else Long.MAX_VALUE })

    Column(Modifier.fillMaxSize().background(K.Bg).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 44.dp)
            Spacer(Modifier.weight(1f))
            CRoundButton(Icons.Rounded.Add, "Catat pinjaman", { adding = true }, size = 44.dp, outlined = true)
        }
        CTitle("Utang & piutang", Modifier.padding(top = 8.dp), subtitle = "Pinjaman, cicilan, dan patungan")

        val net = d.receivable - d.payable
        Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(K.Yellow).padding(18.dp)) {
            Text("Posisi bersih", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
            Money(net, 32.sp, color = if (net < 0) K.Danger else K.Green, hidden = hidden, sign = if (net > 0) "+" else if (net < 0) "−" else "")
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SumTile("Piutang", d.receivable, K.Green, hidden, Modifier.weight(1f))
                SumTile("Utang", d.payable, K.Danger, hidden, Modifier.weight(1f))
            }
        }

        MeowSegmented(listOf("Aktif", "Piutang", "Utang", "Lunas"), filter, { filter = it }, Modifier.padding(top = 16.dp).fillMaxWidth(), fill = true, fontSize = 14.sp)

        if (list.isEmpty()) {
            id.kantong.app.ui.components.CEmpty(
                if (filter == 3) "Belum ada yang lunas." else "Belum ada catatan. Ketuk + untuk mencatat pinjaman, atau pakai Patungan dari detail transaksi.", "card",
                Modifier.padding(top = 12.dp).clip(RoundedCornerShape(22.dp)).background(K.Card),
            )
        } else {
            CList(Modifier.padding(top = 12.dp)) {
                list.forEachIndexed { i, s ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
                    Row(Modifier.fillMaxWidth().pressable { nav.go(R.debt(s.debt.id)) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(s.debt)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.debt.person, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                (if (lend(s.debt)) "Piutang · " else "Utang · ") + dueText(s),
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (s.overdueDays > 0) K.Danger else K.Muted,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (hidden) "•••••" else (if (lend(s.debt)) "+" else "−") + (if (s.done) s.debt.amount else s.remaining).grouped(),
                                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (s.done) K.Muted else tint(s.debt),
                            )
                            Text(if (s.done) "lunas" else if (s.paid > 0) "dari ${s.debt.amount.grouped()}" else "sisa", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
    if (adding) DebtAddSheet(d, vm) { adding = false }
}

@Composable
private fun SumTile(label: String, value: Long, color: Color, hidden: Boolean, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color(0x99FFFFFF)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
        Text(if (hidden) "•••••" else value.grouped(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

/** One loan: remaining with progress, next due, installment schedule, history; WhatsApp reminder for piutang. */
@Composable
fun DebtDetailScreen(d: AppData, vm: AppViewModel, nav: Nav, id: Long) {
    val x = d.debt(id)
    if (x == null) { LaunchedEffect(Unit) { nav.back() }; return }
    val s = d.debtState(x)
    val hidden = Prefs.hideAmounts
    val ctx = LocalContext.current
    var paying by remember { mutableStateOf(false) }
    var removePay by remember { mutableStateOf<DebtPay?>(null) }
    var removeAll by remember { mutableStateOf(false) }
    val bg = if (lend(x)) K.SageCard else K.Pink
    val soft = if (lend(x)) K.OnSage else K.OnPink

    Column(Modifier.fillMaxSize().background(K.Bg).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 44.dp)
        }
        CTitle(x.person, Modifier.padding(top = 8.dp), subtitle = (if (lend(x)) "Pinjam ke saya" else "Saya pinjam") + if (x.installments > 0) " · ${x.installments}× cicilan" else "") { Avatar(x, 48) }

        Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(bg).padding(18.dp)) {
            Text(if (s.done) "Sudah lunas" else if (lend(x)) "Sisa yang belum kembali" else "Sisa utang", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = soft)
            Money(if (s.done) x.amount else s.remaining, 32.sp, color = tint(x), hidden = hidden)
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xB3FFFFFF))) {
                Box(Modifier.fillMaxWidth(s.pct).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(tint(x)))
            }
            Row(Modifier.padding(top = 6.dp)) {
                Text("${if (lend(x)) "Kembali" else "Dibayar"} ${if (hidden) "•••" else s.paid.grouped()} (${Math.round(s.pct * 100)}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = soft, modifier = Modifier.weight(1f))
                Text("dari ${if (hidden) "•••" else x.amount.grouped()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = soft)
            }
            if (!s.done && s.nextDue > 0) Row(
                Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Tile).padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(if (x.installments > 0) "Cicilan berikutnya" else "Jatuh tempo", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                    Text("${if (hidden) "•••" else "Rp" + s.nextAmount.grouped()} · ${fmt(s.nextDue, "d MMM")}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                }
                if (s.overdueDays > 0) Text("lewat ${s.overdueDays} hari", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = K.Danger)
            }
        }

        if (s.schedule.isNotEmpty()) {
            CSection("Jadwal cicilan · ${s.schedule.count { it.paid }}/${s.schedule.size} lunas", Modifier.padding(top = 24.dp, bottom = 10.dp))
            CList {
                s.schedule.forEachIndexed { i, it ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
                    val late = !it.paid && it.due < startOfDay()
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(if (it.paid) K.Green else if (late) K.Pink else K.Field),
                            contentAlignment = Alignment.Center,
                        ) { Text(if (it.paid) "✓" else if (late) "!" else "", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (it.paid) Color.White else K.Danger) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(fmt(it.due, "d MMM yyyy"), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                            if (late) Text("belum dibayar", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = K.Danger)
                        }
                        Text(if (hidden) "•••" else it.amount.grouped(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (it.paid) K.Muted else K.Ink)
                    }
                }
            }
        }

        CSection("Riwayat", Modifier.padding(top = 24.dp, bottom = 10.dp))
        CList {
            s.pays.forEach { p ->
                HistRow("Cicil", K.SageCard, K.Green, "${if (lend(x)) "Diterima" else "Dibayar"} ${if (hidden) "•••" else p.amount.grouped()}", fmt(p.time, "d MMM yyyy") + accountOf(d, p.txnId)) { removePay = p }
                Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
            }
            HistRow(
                if (lend(x)) "Pinjam" else "Utang", K.Yellow, Color(0xFF7A5A12),
                "${if (lend(x)) "Dipinjamkan" else "Dipinjam"} ${if (hidden) "•••" else x.amount.grouped()}",
                fmt(x.time, "d MMM yyyy") + accountOf(d, x.txnId) + if (x.note.isNotBlank()) " · ${x.note}" else "",
            ) {}
        }

        if (!s.done) {
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (lend(x)) CreamButton("Ingatkan via WA", Modifier.weight(1f)) {
                        val msg = "Halo ${x.person}, mau mengingatkan pinjaman Rp${x.amount.grouped()}" +
                            (if (s.paid > 0) " (sisa Rp${s.remaining.grouped()})" else "") +
                            (if (s.nextDue > 0) ", jatuh tempo ${fmt(s.nextDue, "d MMMM")}" else "") + ". Terima kasih."
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=" + Uri.encode(msg)))) }
                }
                RedButton(if (lend(x)) "Catat cicilan" else "Bayar cicilan", Modifier.weight(1f)) { paying = true }
            }
            Text(
                "Tandai lunas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = K.Green, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(12.dp)).pressable { vm.saveDebt(x.copy(settled = 1)) }.padding(10.dp),
            )
        } else if (x.settled == 1 && s.remaining > 0) Text(
            "Batalkan tanda lunas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = K.Green, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(12.dp)).pressable { vm.saveDebt(x.copy(settled = 0)) }.padding(10.dp),
        )
        Text(
            "Hapus catatan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = K.Danger, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).pressable { removeAll = true }.padding(10.dp),
        )
        Spacer(Modifier.height(40.dp))
    }

    if (paying) DebtPaySheet(d, vm, x, s) { paying = false }
    removePay?.let { p -> ConfirmSheet("Hapus cicilan ini?", "Catatan uang yang terkait juga dihapus.", onDismiss = { removePay = null }) { vm.deleteDebtPay(p); removePay = null } }
    if (removeAll) ConfirmSheet("Hapus catatan ${x.person}?", "Pinjaman, cicilan, dan catatan uangnya ikut dihapus.", onDismiss = { removeAll = false }) {
        vm.deleteDebt(x, s.pays); removeAll = false; nav.back()
    }
}

private fun accountOf(d: AppData, txnId: Long): String {
    if (txnId <= 0) return ""
    val t = d.allTxns.firstOrNull { it.id == txnId } ?: return ""
    return (d.account(t.toAccountId) ?: d.account(t.accountId))?.let { " · ${it.name}" } ?: ""
}

@Composable
private fun HistRow(tag: String, bg: Color, fg: Color, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().pressable(onClick).padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(tag, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = fg, modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 9.dp, vertical = 3.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1)
            Text(sub, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Account picker plus "Tanpa akun" (same select row as every form). */
@Composable
private fun AccountChips(d: AppData, selected: Long, onPick: (Long) -> Unit) {
    id.kantong.app.ui.components.CPicker(
        "Akun", d.accounts.map { a -> id.kantong.app.ui.components.SelOpt(a.id.toString(), a.name, "Saldo Rp" + d.balanceOf(a).grouped()) } +
            id.kantong.app.ui.components.SelOpt("0", "Tanpa akun", "saldo tidak berubah"),
        selected.toString(), title = "Pilih akun",
    ) { onPick(it.toLongOrNull() ?: 0L) }
}

@Composable
private fun TextBox(label: String, value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(K.Tile).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
        Box {
            if (value.isEmpty()) Text(hint, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Muted.copy(alpha = 0.5f))
            BasicTextField(
                value = value, onValueChange = { onChange(it.take(40)) }, singleLine = true,
                textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink), cursorBrush = SolidColor(K.Green), modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** New loan: direction, person, amount, account, date, due date or monthly installments, note. */
@Composable
private fun DebtAddSheet(d: AppData, vm: AppViewModel, onDismiss: () -> Unit) {
    var dir by remember { mutableStateOf("lend") }
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var due by remember { mutableLongStateOf(0L) }
    var inst by remember { mutableIntStateOf(0) }
    var accountId by remember { mutableLongStateOf(d.accounts.firstOrNull { it.type == "bank" }?.id ?: d.accounts.firstOrNull()?.id ?: 0L) }
    var pick by remember { mutableStateOf<String?>(null) } // time | due
    var err by remember { mutableStateOf<String?>(null) }
    val people = d.debts.map { it.person }.distinct().take(6)
    val amt = amount.num().toLong()

    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader("Catat pinjaman", onDismiss, size = 20)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Field).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("lend" to "Saya meminjamkan", "borrow" to "Saya meminjam").forEach { (k, l) ->
                        Box(
                            Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp)).background(if (dir == k) K.Green else Color.Transparent).pressable { dir = k },
                            contentAlignment = Alignment.Center,
                        ) { Text(l, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = if (dir == k) Color.White else K.Muted) }
                    }
                }
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(K.Yellow).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextBox(if (dir == "lend") "Dipinjamkan ke" else "Pinjam dari", person, { person = it }, "Nama orang", Modifier.fillMaxWidth())
                    if (people.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        people.forEach { Pill(it) { person = it } }
                    }
                    NumField("Jumlah", amount, { amount = it }, Modifier.fillMaxWidth())
                    TextBox("Catatan (opsional)", note, { note = it }, "Misal: modal usaha", Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill("Tanggal ${fmt(time, "d MMM")}") { pick = "time" }
                        if (inst == 0) Pill(if (due > 0) "Jatuh tempo ${fmt(due, "d MMM")}" else "+ Jatuh tempo") { pick = "due" }
                    }
                }
                Text("Cicilan bulanan", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink2, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 2, 3, 6, 10, 12).forEach { n ->
                        Text(
                            if (n == 0) "Tanpa cicilan" else "$n×", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (inst == n) Color.White else K.Ink2,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (inst == n) K.Green else K.Field).pressable { inst = n }.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
                if (inst > 0 && amt > 0) Text(
                    "${inst}× Rp${(amt / inst).grouped()} mulai ${fmt(firstMonth(time), "d MMM")}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.OnSage,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                )
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.SageCard).padding(12.dp)) {
                    Text(if (dir == "lend") "Uang keluar dari" else "Uang masuk ke", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.OnSage, modifier = Modifier.padding(bottom = 6.dp))
                    AccountChips(d, accountId) { accountId = it }
                }
            }
            err?.let { Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.Danger, modifier = Modifier.padding(top = 8.dp)) }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                RedButton("Simpan", Modifier.weight(1.4f)) {
                    err = when { person.isBlank() -> "Isi nama orang terlebih dahulu"; amt <= 0 -> "Isi jumlah terlebih dahulu"; else -> null }
                    if (err != null) return@RedButton
                    vm.addDebt(
                        Debt(person = person.trim(), direction = dir, amount = amt, time = time, dueDate = if (inst == 0) due else 0, note = note.trim(),
                            installments = inst, firstDue = if (inst > 0) firstMonth(time) else 0),
                        accountId,
                    )
                    onDismiss()
                }
            }
        }
    }
    pick?.let { p ->
        CalendarCard(if (p == "time") time else if (due > 0) due else time, onDismiss = { pick = null }, withTime = false) {
            if (p == "time") time = it else due = it; pick = null
        }
    }
}

/** Same day next month: the first installment date. */
private fun firstMonth(from: Long): Long = Calendar.getInstance().apply { timeInMillis = from; add(Calendar.MONTH, 1) }.timeInMillis

/** Repayment: amount (defaults to the next installment), account, date. */
@Composable
private fun DebtPaySheet(d: AppData, vm: AppViewModel, x: Debt, s: DebtState, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(s.nextAmount.coerceAtMost(s.remaining).toString()) }
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var accountId by remember { mutableLongStateOf(d.allTxns.firstOrNull { it.id == x.txnId }?.let { it.accountId.takeIf { a -> a > 0 } ?: it.toAccountId } ?: d.accounts.firstOrNull()?.id ?: 0L) }
    var pick by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val amt = amount.num().toLong()
    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader(if (lend(x)) "Cicilan dari ${x.person}" else "Bayar ke ${x.person}", onDismiss, size = 20)
            Text("Sisa Rp${s.remaining.grouped()}", fontSize = 13.sp, color = K.Muted)
            Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(K.Yellow).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Jumlah", amount, { amount = it }, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("Lunasi ${s.remaining.grouped()}") { amount = s.remaining.toString() }
                    Pill(fmt(time, "d MMM yyyy") + " ▾") { pick = true }
                }
            }
            Column(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.SageCard).padding(12.dp)) {
                Text(if (lend(x)) "Uang masuk ke" else "Uang keluar dari", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.OnSage, modifier = Modifier.padding(bottom = 6.dp))
                AccountChips(d, accountId) { accountId = it }
            }
            err?.let { Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.Danger, modifier = Modifier.padding(top = 8.dp)) }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                RedButton("Simpan", Modifier.weight(1.4f)) {
                    err = when { amt <= 0 -> "Isi jumlah terlebih dahulu"; amt > s.remaining -> "Melebihi sisa Rp${s.remaining.grouped()}"; else -> null }
                    if (err != null) return@RedButton
                    vm.payDebt(x, amt, time, accountId); onDismiss()
                }
            }
        }
    }
    if (pick) CalendarCard(time, onDismiss = { pick = false }, withTime = false) { time = it; pick = false }
}

/** Patungan: split a spending record evenly or by hand; friends' shares become piutang. */
@Composable
fun SplitSheet(d: AppData, vm: AppViewModel, t: Txn, onDismiss: () -> Unit) {
    val names = remember { mutableStateListOf("", "") }
    val custom = remember { mutableStateListOf("", "") }
    var even by remember { mutableStateOf(true) }
    var mine by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    val people = d.debts.map { it.person }.distinct().take(8)
    val filled = names.count { it.isNotBlank() }
    val share = if (even) t.amount / (filled + 1) else 0L
    val friendAmts = names.indices.map { i -> if (names[i].isBlank()) 0L else if (even) share else custom.getOrElse(i) { "" }.num().toLong() }
    val myShare = if (even) t.amount - share * filled else mine.num().toLong()

    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader("Patungan", onDismiss, size = 20)
            Text("Dari: ${t.merchant.ifBlank { d.category(t.categoryId)?.name ?: "Pengeluaran" }} · Rp${t.amount.grouped()}", fontSize = 13.sp, color = K.Muted)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(true to "Bagi rata", false to "Atur sendiri").forEach { (v, l) ->
                        Text(
                            l, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (even == v) Color.White else K.Ink2,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (even == v) K.Green else K.Field).pressable { even = v }.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(K.Yellow).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Tile).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Saya", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.weight(1f))
                        if (even) Text(myShare.grouped(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                        else NumField("Bagian saya", mine, { mine = it }, Modifier.width(140.dp))
                    }
                    names.indices.forEach { i ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextBox("Teman ${i + 1}", names[i], { names[i] = it }, "Nama", Modifier.weight(1f))
                            if (even) Text(if (names[i].isBlank()) "-" else share.grouped(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Green, modifier = Modifier.width(90.dp), textAlign = TextAlign.End)
                            else NumField("Jumlah", custom[i], { custom[i] = it }, Modifier.width(120.dp))
                        }
                    }
                    if (people.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        people.filter { it !in names }.forEach { p -> Pill(p) { val i = names.indexOfFirst { it.isBlank() }; if (i >= 0) names[i] = p else { names.add(p); custom.add("") } } }
                    }
                }
                Box(
                    Modifier.padding(top = 8.dp).fillMaxWidth().height(42.dp).clip(RoundedCornerShape(15.dp)).background(K.Field).pressable { names.add(""); custom.add("") },
                    contentAlignment = Alignment.Center,
                ) { Text("+ Tambah teman", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Green) }
                Text(
                    "Pengeluaran Anda tercatat Rp${myShare.coerceAtLeast(0).grouped()}. Rp${friendAmts.sum().grouped()} jadi piutang ke ${friendAmts.count { it > 0 }} teman.",
                    fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.OnSage,
                    modifier = Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.SageCard).padding(12.dp),
                )
            }
            err?.let { Text(it, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = K.Danger, modifier = Modifier.padding(top = 8.dp)) }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                RedButton("Simpan patungan", Modifier.weight(1.4f)) {
                    err = when {
                        filled == 0 -> "Isi nama minimal satu teman"
                        myShare < 0 || myShare + friendAmts.sum() != t.amount -> "Jumlah bagian harus pas Rp${t.amount.grouped()}"
                        else -> null
                    }
                    if (err != null) return@RedButton
                    vm.splitTxn(t, myShare, names.indices.map { names[it] to friendAmts[it] })
                    onDismiss()
                }
            }
        }
    }
}
