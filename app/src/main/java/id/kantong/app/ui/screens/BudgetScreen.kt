package id.kantong.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Budget
import id.kantong.app.data.dayOfMonth
import id.kantong.app.data.daysInMonth
import id.kantong.app.data.fmt
import id.kantong.app.data.grouped
import id.kantong.app.data.periodName
import id.kantong.app.data.rupiah
import id.kantong.app.data.short
import id.kantong.app.data.startOfDay
import id.kantong.app.ui.components.BudgetRing
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CalendarCard
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CatetSheet
import id.kantong.app.ui.components.CheckButton
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.DangerOutlineButton
import id.kantong.app.ui.components.GroupCard
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.Notch
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.PillMenu
import id.kantong.app.ui.components.PillOption
import id.kantong.app.ui.components.ProgressBar
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.MeowToggle
import id.kantong.app.ui.components.SheetHeader
import id.kantong.app.ui.components.SmallChip
import id.kantong.app.ui.components.anchor
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.components.rememberAnchor
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone
import kotlin.math.roundToInt

private val BUDGET_PERIODS = listOf("day", "week", "month", "year", "custom")

/**
 * Budget sheet opened from Beranda: a yellow summary (ring, spent, total, what is left), then one card per
 * budget with its progress ("Hampir habis" from 75%, "Lebih RpX" when over). Tapping a card edits it,
 * "Atur anggaran" adds one. The add/edit form replaces the list inside the same overlay; back or the scrim
 * closes the form first.
 */
@Composable
fun BudgetSheet(d: AppData, vm: AppViewModel, onDismiss: () -> Unit) {
    var formOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Budget?>(null) }
    Overlay(onDismiss = { if (formOpen) formOpen = false else onDismiss() }) {
        if (formOpen) {
            key(editing?.id ?: 0L) {
                BudgetForm(d, vm, editing, onClose = { formOpen = false })
            }
        } else {
            CatetSheet {
                SheetHeader("Anggaran ${fmt(System.currentTimeMillis(), "MMMM")}", onDismiss)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    if (d.budgets.isEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Belum ada anggaran", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Batasi belanja per kategori agar lebih terkendali", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                color = K.Muted, textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        val total = d.budgets.sumOf { d.limitOf(it) }
                        val spent = d.budgets.sumOf { d.spentFor(it) }
                        Spacer(Modifier.height(16.dp))
                        BudgetSummary(total, spent)
                        Spacer(Modifier.height(20.dp))
                        Text("Per kategori", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.padding(start = 2.dp))
                        d.budgets.forEach { b ->
                            key(b.id) { BudgetBlock(d, b) { editing = b; formOpen = true } }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                RedButton("Atur anggaran", Modifier.fillMaxWidth()) { editing = null; formOpen = true }
            }
        }
    }
}

/** Rounded share in percent of [part] over [whole] (0 when there is no whole). */
private fun budPct(part: Long, whole: Long): Int = if (whole > 0) (part * 100.0 / whole).roundToInt() else 0

/** Yellow summary card: ring with the used share, spent amount, total and a "Sisa … · N hari" chip. */
@Composable
private fun BudgetSummary(total: Long, spent: Long) {
    val left = total - spent
    val now = System.currentTimeMillis()
    val daysLeft = daysInMonth(now) - dayOfMonth(now) + 1
    CCard(K.Yellow, radius = 26.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BudgetRing(if (total > 0) spent.toFloat() / total else 0f, 92.dp, 12.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${budPct(spent, total)}%", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = K.Green, maxLines = 1)
                    Text("terpakai", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1)
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text("Terpakai", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = K.Muted)
                Money(spent, 24.sp)
                Text("dari ${total.rupiah()}", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Text(
                    (if (left >= 0) "Sisa Rp${left.short()}" else "Lebih Rp${(-left).short()}") + " · $daysLeft hari",
                    fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (left >= 0) K.Ink else K.Danger, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(13.dp)).background(K.Tile).padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** One budget: icon, name (+ warning chip), "RpX dari RpY", share in percent and a progress bar. */
@Composable
private fun BudgetBlock(d: AppData, b: Budget, onClick: () -> Unit) {
    val cats = b.categoryIdList.mapNotNull { d.category(it) }
    val spent = d.spentFor(b)
    val carry = d.carryFor(b)
    val limit = b.amount + carry
    val pct = budPct(spent, limit)
    val over = spent > limit
    val warn = !over && pct >= 75
    val barColor = when { over -> Color(0xFFD9776A); warn -> K.Gold; else -> K.Green }
    Column(
        Modifier.padding(top = 9.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(K.Card).pressable(onClick)
            .padding(start = 15.dp, end = 15.dp, top = 13.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CatIcon(cats.firstOrNull()?.icon ?: "other", 30.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        b.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (over || warn) {
                        Spacer(Modifier.width(8.dp))
                        SmallChip(
                            if (over) "Lebih Rp${(spent - limit).short()}" else "Hampir habis",
                            if (over) K.Pink else K.Yellow, if (over) K.Danger else K.Ink,
                        )
                    }
                }
                Text(
                    "Rp${spent.short()} dari Rp${limit.short()}" + if (b.period != "month") " · ${periodName(b.period)}" else "",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (carry != 0L) Text(
                    (if (carry > 0) "↻ Sisa lalu +Rp" else "↻ Kelebihan lalu −Rp") + kotlin.math.abs(carry).short(),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (carry > 0) K.ChartIn else K.Danger, maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("$pct%", fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = if (over) K.Danger else K.Ink, maxLines = 1)
        }
        Spacer(Modifier.height(10.dp))
        ProgressBar(if (limit > 0) spent.toFloat() / limit else 0f, barColor, track = K.c(0xFFEFE6D1, 0xFF313732), height = 8.dp)
    }
}

/** Add / edit form: period, categories, amount, then the name with the save check. */
@Composable
private fun BudgetForm(d: AppData, vm: AppViewModel, editing: Budget?, onClose: () -> Unit) {
    var period by remember { mutableStateOf(editing?.period) }
    var start by remember { mutableLongStateOf(editing?.start ?: 0L) }
    var end by remember { mutableLongStateOf(editing?.end ?: 0L) }
    var ids by remember { mutableStateOf(editing?.categoryIdList ?: emptyList()) }
    var digits by remember { mutableStateOf(editing?.amount?.takeIf { it > 0 }?.toString() ?: "") }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var rollover by remember { mutableStateOf(editing?.rollover == 1) }
    var rolloverOver by remember { mutableStateOf(editing?.rolloverOver == 1) }
    var periodMenu by remember { mutableStateOf(false) }
    var catMenu by remember { mutableStateOf(false) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableLongStateOf(0L) }
    var confirmDelete by remember { mutableStateOf(false) }
    val periodAnchor = rememberAnchor()
    val catAnchor = rememberAnchor()

    val outCats = d.topCategories("out")
    val chosen = ids.mapNotNull { d.category(it) }
    val firstName = chosen.firstOrNull()?.name
    val amount = digits.toLongOrNull() ?: 0L
    val canSave = period != null && chosen.isNotEmpty() && amount > 0
    val periodLabel = when (val p = period) {
        null -> "Pilih"
        "custom" -> "${fmt(start, "d MMM")} – ${fmt(end, "d MMM yyyy")}"
        else -> periodName(p)
    }

    CatetSheet {
        SheetHeader(if (editing != null) "Ubah Anggaran" else "Atur Anggaran", onClose)
        Spacer(Modifier.height(12.dp))

        if (confirmDelete && editing != null) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Hapus anggaran \"${editing.name}\"?", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                Spacer(Modifier.height(6.dp))
                Text("Anggaran ini akan dihapus dari daftar.", fontSize = 13.5.sp, color = K.Muted)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CreamButton("Batal", Modifier.weight(1f)) { confirmDelete = false }
                    RedButton("Hapus", Modifier.weight(1f)) { vm.deleteBudget(editing); onClose() }
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Periode Row
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Field)
                        .anchor(periodAnchor).pressable { periodMenu = true }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Periode Anggaran", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        Spacer(Modifier.height(2.dp))
                        Text(periodLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                    }
                    Text("Pilih ›", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = K.Green)
                }

                // Kategori Selector Row
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Field)
                        .anchor(catAnchor).pressable { catMenu = true }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Kategori", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (chosen.isEmpty()) "Pilih kategori" else chosen.joinToString(", ") { it.name },
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (chosen.isEmpty()) K.Muted else K.Ink,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text("Pilih ›", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = K.Green)
                }

                // Nominal Field
                Column {
                    Text("Batas Anggaran", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
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

                // Rollover
                if (period != "custom") Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.Card).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Bawa sisa ke periode berikutnya", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                            Text("Sisa yang tidak terpakai ditambahkan ke periode berikutnya", fontSize = 12.5.sp, color = K.Muted)
                        }
                        Spacer(Modifier.width(10.dp))
                        MeowToggle(rollover) { rollover = it; if (!it) rolloverOver = false }
                    }
                    if (rollover) Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Bawa juga kelebihan", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                            Text("Kalau lewat, periode berikutnya berkurang", fontSize = 12.5.sp, color = K.Muted)
                        }
                        Spacer(Modifier.width(10.dp))
                        MeowToggle(rolloverOver) { rolloverOver = it }
                    }
                }

                // Nama Anggaran Field
                Column {
                    Text("Nama Anggaran (opsional)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(K.Field).padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (name.isEmpty()) Text(firstName ?: "Contoh: Makan Bulanan", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = K.Muted)
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

                Spacer(Modifier.height(8.dp))

                // Actions
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CreamButton("Batal", Modifier.weight(1f), onClick = onClose)
                    RedButton("Simpan", Modifier.weight(1f), enabled = canSave) {
                        val p = period ?: return@RedButton
                        vm.saveBudget(
                            Budget(
                                id = editing?.id ?: 0L,
                                name = name.trim().ifBlank { firstName ?: "Anggaran" },
                                period = p,
                                categoryIds = ids.joinToString(","),
                                amount = amount,
                                start = if (p == "custom") start else 0L,
                                end = if (p == "custom") end else 0L,
                                rollover = if (rollover && p != "custom") 1 else 0,
                                rolloverOver = if (rollover && rolloverOver && p != "custom") 1 else 0,
                            ),
                        )
                        onClose()
                    }
                }

                if (editing != null) {
                    DangerOutlineButton("Hapus Anggaran", Modifier.fillMaxWidth()) { confirmDelete = true }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (periodMenu) {
        PillMenu(periodAnchor.rect, { periodMenu = false }) {
            BUDGET_PERIODS.forEach { p ->
                PillOption(periodName(p), Tone.PEACH, p == period, {
                    periodMenu = false
                    if (p == "custom") {
                        pendingStart = if (start > 0) start else startOfDay()
                        pickStart = true
                    } else period = p
                })
            }
        }
    }
    if (catMenu) {
        PillMenu(catAnchor.rect, { catMenu = false }, title = "Kategori") {
            outCats.forEach { c ->
                val on = c.id in ids
                PillOption(c.name, Tone.of(c.tone), on, { ids = if (on) ids - c.id else ids + c.id }, icon = c.icon)
            }
            PillOption("Selesai", Tone.PEACH, false, { catMenu = false }, glyph = Icons.Rounded.Check)
        }
    }
    if (pickStart) {
        CalendarCard(pendingStart, onDismiss = { pickStart = false }, withTime = false) {
            pendingStart = startOfDay(it)
            pickStart = false
            pickEnd = true
        }
    }
    if (pickEnd) {
        CalendarCard(if (end >= pendingStart) end else pendingStart, onDismiss = { pickEnd = false }, withTime = false) {
            val e = startOfDay(it)
            start = minOf(pendingStart, e)
            end = maxOf(pendingStart, e)
            period = "custom"
            pickEnd = false
        }
    }
}

/** Bottom sheet holding separate cream blocks on the sand backdrop (like the reference form). */
@Composable
private fun BudFormSheet(content: @Composable ColumnScope.() -> Unit) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        Modifier.imePadding().fillMaxWidth().heightIn(max = 720.dp).clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)).background(K.Bg)
            .clickable(remember { MutableInteractionSource() }, null) {}
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp + navBottom),
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        Notch(Modifier.align(Alignment.TopCenter).offset(y = (-12).dp))
    }
}

/**
 * Form row like the reference: 44dp glyph tile, 17sp label, value + chevron on the right, 64dp high.
 * With [input] the label is followed by that content (e.g. an amount pill) instead of value and chevron.
 * Also used by the recurring form.
 */
@Composable
internal fun BudFormRow(
    icon: ImageVector, label: String, value: String = "", onClick: (() -> Unit)? = null, divider: Boolean = true,
    valueColor: Color = K.Ink, leading: (@Composable () -> Unit)? = null, input: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) leading() else Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(K.Field), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = K.Ink, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            if (input != null) {
                Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = K.Ink, maxLines = 1)
                Spacer(Modifier.width(12.dp))
                input()
            } else {
                Text(
                    label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    value, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp),
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = K.Ink, modifier = Modifier.size(24.dp))
            }
        }
        if (divider) Box(Modifier.padding(start = 56.dp).fillMaxWidth().height(1.dp).background(K.Line))
    }
}

/** Sand pill input. With [number] it keeps digits only and shows them grouped ("1.250.000"). */
@Composable
private fun BudInput(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, number: Boolean = false, height: Dp = 52.dp) {
    val shown = if (number) (value.toLongOrNull()?.grouped() ?: "") else value
    val style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = K.Ink)
    val deco: @Composable (@Composable () -> Unit) -> Unit = { inner ->
        Box(
            Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(K.Field).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (shown.isEmpty()) Text(hint, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = K.Muted.copy(alpha = .6f), maxLines = 1)
            inner()
        }
    }
    if (number) {
        BasicTextField(
            value = TextFieldValue(shown, TextRange(shown.length)),
            onValueChange = { v -> onChange(v.text.filter { it.isDigit() }.trimStart('0').take(13)) },
            modifier = modifier,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(K.Ink2),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = deco,
        )
    } else {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = modifier,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(K.Ink2),
            decorationBox = deco,
        )
    }
}
