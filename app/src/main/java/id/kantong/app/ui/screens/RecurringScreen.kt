package id.kantong.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Bill
import id.kantong.app.data.dayOfMonth
import id.kantong.app.data.grouped
import id.kantong.app.data.monthKey
import id.kantong.app.data.rupiah
import id.kantong.app.ui.Nav
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CatetSheet
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.DangerOutlineButton
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.PillMenu
import id.kantong.app.ui.components.PillOption
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.SheetHeader
import id.kantong.app.ui.components.SmallChip
import id.kantong.app.ui.components.anchor
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.components.rememberAnchor
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone

/**
 * Monthly recurring bills (R89):
 * Back + round "+", big title, yellow monthly bill summary card,
 * list of bill cards (with status pill, due date and quick pay),
 * plus an add/edit bottom sheet.
 */
@Composable
fun RecurringScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    var formOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Bill?>(null) }
    val payAccount = d.accounts.firstOrNull()

    val totalAmount = d.bills.sumOf { it.amount }
    val curMonth = monthKey()
    val paidBills = d.bills.filter { it.paidMonthKey == curMonth }
    val unpaidBills = d.bills.filter { it.paidMonthKey != curMonth }
    val paidAmount = paidBills.sumOf { it.amount }
    val unpaidAmount = unpaidBills.sumOf { it.amount }

    Column(Modifier.fillMaxSize().background(K.Bg)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 44.dp)
            Spacer(Modifier.weight(1f))
            CRoundButton(Icons.Rounded.Add, "Tambah tagihan", { editing = null; formOpen = true }, size = 44.dp, outlined = true)
        }

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        ) {
            CTitle("Berulang", subtitle = "Tagihan dan pengeluaran rutin bulanan")
            Spacer(Modifier.height(16.dp))

            // Yellow Summary Card
            CCard(K.Yellow, radius = 28.dp, padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Total Tagihan Bulanan", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
                        Spacer(Modifier.height(4.dp))
                        Money(totalAmount, 36.sp, color = K.Green)
                    }
                    Spacer(Modifier.width(12.dp))
                    CatIcon("bill", 50.dp)
                }
                Spacer(Modifier.height(14.dp))
                Spacer(Modifier.fillMaxWidth().height(1.5.dp).background(K.YellowLine))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Menunggu (${unpaidBills.size})", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        Spacer(Modifier.height(2.dp))
                        Text(unpaidAmount.rupiah(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (unpaidAmount > 0) K.Danger else K.Ink)
                    }
                    Box(Modifier.width(1.5.dp).height(36.dp).background(K.YellowLine))
                    Column(Modifier.weight(1f).padding(start = 20.dp)) {
                        Text("Sudah Dibayar (${paidBills.size})", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        Spacer(Modifier.height(2.dp))
                        Text(paidAmount.rupiah(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = K.Income)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (d.bills.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.Card)
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CatIcon("bill", 52.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada tagihan berulang", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Catat tagihan rutin seperti listrik, sewa, atau internet supaya tidak terlewat.",
                        fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, color = K.Muted, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    RedButton("Tambah Tagihan", onClick = { editing = null; formOpen = true })
                }
            } else {
                Text("Daftar Tagihan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                Spacer(Modifier.height(10.dp))

                val sortedBills = d.bills.sortedWith(
                    compareBy<Bill> { it.paidMonthKey == curMonth }
                        .thenBy { it.dueDay },
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    sortedBills.forEach { b ->
                        RecurringCardR89(
                            d = d,
                            b = b,
                            onPay = if (payAccount != null) { { vm.payBill(b, payAccount.id); Unit } } else null,
                            onClick = { editing = b; formOpen = true },
                        )
                    }
                }
            }
        }
    }

    if (formOpen) {
        RecurringSheet(d, vm, editing) { formOpen = false }
    }
}

/** R89 Bill Card: card background adapts if pending or paid, status chip, due day, quick pay action. */
@Composable
private fun RecurringCardR89(d: AppData, b: Bill, onPay: (() -> Unit)?, onClick: () -> Unit) {
    val paid = b.paidMonthKey == monthKey()
    val diff = b.dueDay - dayOfMonth()
    val cardBg = when {
        paid -> K.Card
        diff < 0 -> K.Pink
        else -> K.Card
    }
    val (statusLabel, statusBg, statusColor) = when {
        paid -> Triple("Lunas bulan ini", K.SageCard, K.Green)
        diff > 0 -> Triple("$diff hari lagi", K.Yellow, K.Ink)
        diff == 0 -> Triple("Jatuh tempo hari ini", K.Yellow, K.Ink)
        else -> Triple("Terlambat ${-diff} hari", K.Pink, K.Danger)
    }

    CCard(cardBg, radius = 22.dp, padding = PaddingValues(16.dp), modifier = Modifier.pressable(onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(K.Tile),
                contentAlignment = Alignment.Center,
            ) {
                CatIcon(b.icon.ifBlank { "bill" }, 28.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(b.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        statusLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = statusColor,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(statusBg).padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Tgl ${b.dueDay}", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = K.Muted)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(b.amount.grouped(), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = if (!paid && diff < 0) K.Danger else K.Ink, maxLines = 1)
        }

        if (!paid && onPay != null) {
            Spacer(Modifier.height(12.dp))
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    d.category(b.categoryId)?.name ?: "Kategori",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted, modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.height(34.dp).clip(RoundedCornerShape(12.dp)).background(K.Green).pressable(onPay)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Catat Lunas", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/** Add / edit sheet (R89): form inside CatetSheet with SheetHeader, category selector, due day picker, and clean buttons. */
@Composable
private fun RecurringSheet(d: AppData, vm: AppViewModel, editing: Bill?, onDismiss: () -> Unit) {
    var catId by remember { mutableStateOf(editing?.categoryId) }
    var digits by remember { mutableStateOf(editing?.amount?.takeIf { it > 0 }?.toString() ?: "") }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var due by remember { mutableIntStateOf(editing?.dueDay ?: dayOfMonth().coerceAtMost(28)) }
    var catMenu by remember { mutableStateOf(false) }
    var dayPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val catAnchor = rememberAnchor()
    val cat = d.category(catId)
    val amount = digits.toLongOrNull() ?: 0L
    val canSave = name.isNotBlank() && cat != null && amount > 0

    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader(if (editing != null) "Ubah Tagihan" else "Tambah Tagihan", onDismiss)
            Spacer(Modifier.height(12.dp))

            if (confirmDelete && editing != null) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("Hapus tagihan \"${editing.name}\"?", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                    Spacer(Modifier.height(6.dp))
                    Text("Tagihan ini tidak akan muncul lagi di daftar berulang.", fontSize = 13.5.sp, color = K.Muted)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CreamButton("Batal", Modifier.weight(1f)) { confirmDelete = false }
                        RedButton("Hapus", Modifier.weight(1f)) { vm.deleteBill(editing); onDismiss() }
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Category Picker Row
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Field)
                            .anchor(catAnchor).pressable { catMenu = true }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (cat != null) {
                            CatIcon(cat.icon, 26.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(cat.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink, modifier = Modifier.weight(1f))
                        } else {
                            Text("Pilih Kategori", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, modifier = Modifier.weight(1f))
                        }
                        Text("Pilih ›", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = K.Green)
                    }

                    // Amount Field
                    Column {
                        Text("Nominal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        Box(
                            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(K.Field).padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            val display = if (digits.isNotEmpty()) digits.toLongOrNull()?.grouped() ?: digits else ""
                            if (display.isEmpty()) Text("0", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                            BasicTextField(
                                value = TextFieldValue(display, TextRange(display.length)),
                                onValueChange = { v -> digits = v.text.filter { it.isDigit() }.trimStart('0').take(12) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink),
                                cursorBrush = SolidColor(K.Green),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // Name Field
                    Column {
                        Text("Nama Tagihan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        Box(
                            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(K.Field).padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (name.isEmpty()) Text("Contoh: WiFi Rumah, Listrik", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = K.Muted)
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it.take(40) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink),
                                cursorBrush = SolidColor(K.Green),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // Due Day Picker Row
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Field)
                            .pressable { dayPicker = true }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Jatuh Tempo Bulanan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                            Spacer(Modifier.height(2.dp))
                            Text("Setiap tanggal $due", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                        }
                        Text("Ubah ›", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = K.Green)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Actions
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                        RedButton("Simpan", Modifier.weight(1f), enabled = canSave) {
                            val c = cat ?: return@RedButton
                            vm.saveBill(
                                Bill(
                                    id = editing?.id ?: 0L,
                                    name = name.trim(),
                                    amount = amount,
                                    dueDay = due,
                                    categoryId = c.id,
                                    icon = c.icon,
                                    tone = c.tone,
                                    paidMonthKey = editing?.paidMonthKey ?: 0,
                                ),
                            )
                            onDismiss()
                        }
                    }

                    if (editing != null) {
                        DangerOutlineButton("Hapus Tagihan", Modifier.fillMaxWidth()) { confirmDelete = true }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (catMenu) {
        PillMenu(catAnchor.rect, { catMenu = false }, title = "Pilih Kategori") {
            d.topCategories("out").forEach { c ->
                PillOption(c.name, Tone.of(c.tone), c.id == catId, { catId = c.id; catMenu = false }, icon = c.icon)
            }
        }
    }

    if (dayPicker) {
        Overlay({ dayPicker = false }) {
            Column(
                Modifier.padding(24.dp).clip(RoundedCornerShape(26.dp)).background(K.Sheet)
                    .padding(20.dp),
            ) {
                Text("Pilih Tanggal Jatuh Tempo", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                Spacer(Modifier.height(14.dp))
                (1..28).chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        week.forEach { n ->
                            val on = n == due
                            Box(
                                Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp))
                                    .background(if (on) K.Green else K.Field)
                                    .pressable { due = n; dayPicker = false },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("$n", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else K.Ink)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                CreamButton("Tutup", Modifier.fillMaxWidth()) { dayPicker = false }
            }
        }
    }
}
