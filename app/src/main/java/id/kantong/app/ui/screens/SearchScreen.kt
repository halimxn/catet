package id.kantong.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Txn
import id.kantong.app.data.fmt
import id.kantong.app.data.grouped
import id.kantong.app.data.startOfDay
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CalendarCard
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.FloatingCard
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.PeriodSheet
import id.kantong.app.ui.components.RecordDetailCard
import id.kantong.app.ui.components.RecordTimeline
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.RedFirst
import id.kantong.app.ui.components.accountIcon
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Outfit
import java.util.Calendar

private const val MAX_RESULTS = 200
private const val DAY_MS = 86_400_000L

/** Quick type filters above the results: key to label. */
private val TYPES = listOf("all" to "Semua", "out" to "Keluar", "in" to "Masuk", "reimb" to "Reimburse", "star" to "Bookmark")

private fun idListSaver() = listSaver<SnapshotStateList<Long>, Long>(save = { it.toList() }, restore = { it.toMutableStateList() })

private fun nextMonth(start: Long): Long = Calendar.getInstance().apply { timeInMillis = start; add(Calendar.MONTH, 1) }.timeInMillis

/**
 * Riwayat lengkap: back + month chip, title with the count, search field, type chips (Semua/Keluar/Masuk/
 * Reimburse/Bookmark) plus the detailed filters (kategori, akun, buku kas, waktu, nominal), the Masuk|Keluar
 * summary and the matching records as a timeline.
 */
@Composable
fun SearchScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    var query by rememberSaveable { mutableStateOf("") }
    // A category picked elsewhere (e.g. a row in Laporan) arrives preselected.
    val cats = rememberSaveable(saver = idListSaver()) { mutableStateListOf<Long>().apply { vm.takeHistoryCat()?.let { add(d.rootOf(it)?.id ?: it) } } }
    val accs = rememberSaveable(saver = idListSaver()) { mutableStateListOf<Long>() }
    val leds = rememberSaveable(saver = idListSaver()) { mutableStateListOf<Long>() }
    var type by rememberSaveable { mutableStateOf("all") }
    var month by rememberSaveable { mutableStateOf<Long?>(null) }
    var fromDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var toDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var minAmt by rememberSaveable { mutableStateOf<Long?>(null) }
    var maxAmt by rememberSaveable { mutableStateOf<Long?>(null) }
    var showFilters by rememberSaveable { mutableStateOf(cats.isNotEmpty()) }
    var monthEdit by remember { mutableStateOf(false) }
    var dateEdit by remember { mutableStateOf<String?>(null) } // "from" | "to"
    var amountEdit by remember { mutableStateOf<String?>(null) } // "min" | "max"
    var open by remember { mutableStateOf<Txn?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val hideKeyboard = { keyboard?.hide(); focus.clearFocus() }

    val q = query.trim()
    val digits = q.filter { it.isDigit() }
    val results = d.allTxns.filter { t ->
        (cats.isEmpty() || t.categoryId in cats || d.rootOf(t.categoryId)?.id in cats) &&
            (accs.isEmpty() || t.accountId in accs || t.toAccountId in accs) &&
            (leds.isEmpty() || t.ledgerId in leds) &&
            when (type) { "out" -> t.kind == "out"; "in" -> t.kind == "in"; "reimb" -> t.reimburse > 0; "star" -> t.starred; else -> true } &&
            (month == null || (t.time >= month!! && t.time < nextMonth(month!!))) &&
            (fromDay == null || t.time >= fromDay!!) &&
            (toDay == null || t.time < toDay!! + DAY_MS) &&
            (minAmt == null || t.amount >= minAmt!!) &&
            (maxAmt == null || t.amount <= maxAmt!!) &&
            (q.isEmpty() || t.merchant.contains(q, true) || t.note.contains(q, true) || (digits.isNotEmpty() && t.amount.toString().contains(digits)))
    }.sortedByDescending { it.time }
    val income = results.filter { it.kind == "in" }.sumOf { it.amount }
    val spent = results.filter { it.kind == "out" }.sumOf { it.amount }
    val extraCount = listOf(cats.isNotEmpty(), accs.isNotEmpty(), leds.isNotEmpty(), fromDay != null || toDay != null, minAmt != null || maxAmt != null).count { it }
    val ledgerNote = leds.singleOrNull()?.let { d.ledger(it) }?.let { " · Buku kas ${it.name}" } ?: ""

    Column(Modifier.fillMaxSize().background(K.Bg)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 46.dp)
            Spacer(Modifier.weight(1f))
            MonthChip(month, { monthEdit = true }, onClear = month?.let { { month = null } })
        }

        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            CTitle("Riwayat", Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp), subtitle = "${results.size} transaksi$ledgerNote")
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(K.Card).padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = K.Ink, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text("Cari nama, catatan, atau nominal", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(fontFamily = Outfit, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink),
                        cursorBrush = SolidColor(K.Green),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { hideKeyboard() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(Icons.Rounded.Close, "Hapus", tint = K.Muted, modifier = Modifier.size(32.dp).clip(CircleShape).clickable { query = "" }.padding(6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                TYPES.forEach { (key, label) -> OptionChip(label, type == key, { type = key }) }
                OptionChip(if (extraCount > 0) "Filter lain · $extraCount" else "Filter lain", showFilters, { showFilters = !showFilters }, outlined = true)
            }
            if (showFilters) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterRow("Kategori") {
                        (d.topCategories("out") + d.topCategories("in")).forEach { c ->
                            OptionChip(c.name, c.id in cats, { toggle(cats, c.id) }, icon = c.icon)
                        }
                    }
                    FilterRow("Akun") {
                        d.accounts.forEach { a -> OptionChip(a.name, a.id in accs, { toggle(accs, a.id) }, icon = accountIcon(a)) }
                    }
                    FilterRow("Buku kas") {
                        d.ledgers.forEach { l -> OptionChip(l.name, l.id in leds, { toggle(leds, l.id) }, icon = l.icon) }
                    }
                    FilterRow("Waktu") {
                        OptionChip(fromDay?.let { fmt(it, "d MMM yyyy") } ?: "Tanggal mulai", fromDay != null, { dateEdit = "from" },
                            onClear = fromDay?.let { { fromDay = null } })
                        OptionChip(toDay?.let { fmt(it, "d MMM yyyy") } ?: "Tanggal akhir", toDay != null, { dateEdit = "to" },
                            onClear = toDay?.let { { toDay = null } })
                    }
                    FilterRow("Nominal") {
                        OptionChip(minAmt?.let { "≥ ${it.grouped()}" } ?: "≥ …", minAmt != null, { amountEdit = "min" },
                            onClear = minAmt?.let { { minAmt = null } })
                        OptionChip(maxAmt?.let { "≤ ${it.grouped()}" } ?: "≤ …", maxAmt != null, { amountEdit = "max" },
                            onClear = maxAmt?.let { { maxAmt = null } })
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.Yellow).padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Masuk", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = K.Muted)
                    Money(income, 20.sp, color = K.Ink)
                }
                Box(Modifier.width(1.dp).height(36.dp).background(K.YellowLine))
                Column(Modifier.weight(1f).padding(start = 20.dp)) {
                    Text("Keluar", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = K.Muted)
                    Money(spent, 20.sp, color = K.Ink)
                }
            }

            if (results.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tidak ditemukan", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text("Coba kata kunci atau filter lain", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = K.Muted, textAlign = TextAlign.Center)
                }
            } else {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                    RecordTimeline(d, results.take(MAX_RESULTS), onOpen = { open = it }, withYear = true)
                    if (results.size > MAX_RESULTS) {
                        Text(
                            "Menampilkan $MAX_RESULTS catatan terbaru dari ${results.size}. Persempit dengan filter atau kata kunci.",
                            fontSize = 14.sp, fontWeight = FontWeight.Medium, color = K.Muted, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }

    if (monthEdit) {
        PeriodSheet(onDismiss = { monthEdit = false }, initial = month ?: System.currentTimeMillis(), onApply = { month = it })
    }

    dateEdit?.let { which ->
        CalendarCard(
            initial = (if (which == "from") fromDay else toDay) ?: System.currentTimeMillis(),
            onDismiss = { dateEdit = null }, withTime = false,
        ) { picked ->
            if (which == "from") fromDay = startOfDay(picked) else toDay = startOfDay(picked)
            dateEdit = null
        }
    }

    amountEdit?.let { which ->
        AmountCard(
            title = if (which == "min") "Nominal minimal" else "Nominal maksimal",
            initial = if (which == "min") minAmt else maxAmt,
            onDismiss = { amountEdit = null },
        ) { value ->
            if (which == "min") minAmt = value else maxAmt = value
            amountEdit = null
        }
    }

    open?.let { t ->
        val cur = d.allTxns.firstOrNull { it.id == t.id } ?: t
        RecordDetailCard(
            d, cur, onDismiss = { open = null }, onEdit = { open = null; nav.go(R.add(cur.id)) },
            onDelete = { vm.deleteTxn(cur); open = null }, onStar = { vm.toggleStar(cur) }, onReimburse = { vm.setReimburse(cur, it) },
        )
    }
}

private fun toggle(list: SnapshotStateList<Long>, id: Long) { if (id in list) list.remove(id) else list.add(id) }

/** Outlined month pill ("November 2026 ▾", or "Semua waktu ▾"); × clears the month. */
@Composable
private fun MonthChip(month: Long?, onClick: () -> Unit, onClear: (() -> Unit)?) {
    val shape = RoundedCornerShape(21.dp)
    Row(
        Modifier.height(42.dp).clip(shape).background(K.Sheet).border(1.5.dp, K.Notch, shape).pressable(onClick)
            .padding(start = 16.dp, end = if (onClear != null) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(month?.let { fmt(it, "MMMM yyyy") } ?: "Semua waktu", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1)
        Spacer(Modifier.width(4.dp))
        if (onClear != null) {
            Icon(Icons.Rounded.Close, "Semua waktu", tint = K.Ink, modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onClear).padding(4.dp))
        } else {
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = K.Ink, modifier = Modifier.size(20.dp))
        }
    }
}

/** One horizontally scrolling filter row: muted label, then its options. */
@Composable
private fun FilterRow(label: String, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, maxLines = 1, modifier = Modifier.width(66.dp))
        content()
    }
}

/**
 * Pill chip: cream (or outlined) when off, green when on, with an optional two-tone icon.
 * [onClear] adds a small × to reset it.
 */
@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit, icon: String? = null, outlined: Boolean = false, onClear: (() -> Unit)? = null) {
    val fg = if (selected) Color.White else K.Ink
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.height(36.dp).clip(shape).background(if (selected) K.Green else if (outlined) K.Sheet else K.Card)
            .let { if (outlined && !selected) it.border(1.5.dp, K.Notch, shape) else it }
            .pressable(onClick)
            .padding(start = if (icon != null) 8.dp else 14.dp, end = if (onClear != null) 8.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CatIcon(icon, 22.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
        if (onClear != null) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.Close, "Hapus", tint = fg, modifier = Modifier.size(20.dp).clip(CircleShape).clickable(onClick = onClear).padding(2.dp))
        }
    }
}

/** Floating card with a numeric field for the amount filters: "Hapus" clears, "Pilih" applies. */
@Composable
private fun AmountCard(title: String, initial: Long?, onDismiss: () -> Unit, onConfirm: (Long?) -> Unit) {
    var text by remember { mutableStateOf(initial?.toString() ?: "") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Overlay(onDismiss) {
        FloatingCard(Modifier.imePadding()) {
            RedFirst(title, 20.sp)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(K.Field).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty()) Text("0", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = K.Muted)
                BasicTextField(
                    value = text, onValueChange = { s -> text = s.filter { it.isDigit() }.take(13) }, singleLine = true,
                    textStyle = TextStyle(fontFamily = Outfit, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = K.Ink),
                    cursorBrush = SolidColor(K.Green),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm(text.toLongOrNull()) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            text.toLongOrNull()?.let {
                Spacer(Modifier.height(8.dp))
                Text(it.grouped(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2, modifier = Modifier.padding(start = 4.dp))
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CreamButton("Hapus", Modifier.weight(1f)) { onConfirm(null) }
                RedButton("Pilih", Modifier.weight(1f)) { onConfirm(text.toLongOrNull()) }
            }
        }
    }
}
