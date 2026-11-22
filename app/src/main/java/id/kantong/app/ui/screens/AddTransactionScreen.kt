package id.kantong.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.Account
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Category
import id.kantong.app.data.Prefs
import id.kantong.app.data.Txn
import id.kantong.app.data.fmt
import id.kantong.app.data.grouped
import id.kantong.app.data.startOfDay
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.CChip
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CalendarCard
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.FloatingCard
import id.kantong.app.ui.components.MeowSegmented
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.PillMenu
import id.kantong.app.ui.components.PillOption
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.RedFirst
import id.kantong.app.ui.components.SheetCard
import id.kantong.app.ui.components.accountIcon
import id.kantong.app.ui.components.anchor
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.components.rememberAnchor
import id.kantong.app.ui.components.rememberTick
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone
import kotlinx.coroutines.delay
import kotlin.math.abs

private val KINDS = listOf("out", "in", "transfer")
private const val OPS = "+−"
private const val MAX_DIGITS = 12

@Composable
fun AddTransactionScreen(d: AppData, vm: AppViewModel, nav: Nav, editId: Long) {
    val startKind = remember { vm.takeAddKind() }
    var kind by rememberSaveable { mutableIntStateOf(KINDS.indexOf(startKind).coerceAtLeast(0)) }
    var expr by rememberSaveable { mutableStateOf("") }
    var feeExpr by rememberSaveable { mutableStateOf("") }
    var feeMode by rememberSaveable { mutableStateOf(false) } // transfer only: keypad types into the fee
    var remark by rememberSaveable { mutableStateOf("") }
    var pickedCat by rememberSaveable { mutableLongStateOf(0L) }
    var pickedAcc by rememberSaveable { mutableLongStateOf(0L) }
    var fromId by rememberSaveable { mutableLongStateOf(0L) }
    var toId by rememberSaveable { mutableLongStateOf(0L) }
    var pickedLedger by rememberSaveable { mutableLongStateOf(0L) }
    var time by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var reimburse by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableLongStateOf(editId) } // becomes 0 after "simpan & lanjut"
    var prefilled by rememberSaveable { mutableStateOf(false) }
    var savedTick by remember { mutableIntStateOf(0) }
    var showSaved by remember { mutableStateOf(false) }

    var showCal by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var accMenu by remember { mutableStateOf(false) }
    var feeHelp by remember { mutableStateOf(false) }
    var allCats by remember { mutableStateOf(false) }
    val moreAnchor = rememberAnchor()
    val accAnchor = rememberAnchor()
    val tick = rememberTick()

    val edited = if (editId > 0) d.allTxns.firstOrNull { it.id == editId } else null
    LaunchedEffect(edited) {
        val t = edited ?: return@LaunchedEffect
        if (prefilled) return@LaunchedEffect
        prefilled = true
        kind = KINDS.indexOf(t.kind).coerceAtLeast(0)
        expr = if (t.amount > 0) t.amount.toString() else ""
        remark = t.merchant
        pickedCat = t.categoryId ?: 0L
        pickedAcc = t.accountId
        if (t.kind == "transfer") { fromId = t.accountId; toId = t.toAccountId ?: 0L }
        time = t.time
        pickedLedger = t.ledgerId
        reimburse = t.reimburse != 0
        feeExpr = if (t.fee > 0) t.fee.toString() else ""
    }
    LaunchedEffect(savedTick) {
        if (savedTick > 0) { showSaved = true; delay(1200); showSaved = false }
    }

    val kindKey = KINDS[kind]
    val isTransfer = kindKey == "transfer"
    val mains = d.topCategories(if (isTransfer) "out" else kindKey).sortedBy { it.sortOrder }
    val catId = d.category(pickedCat.takeIf { it > 0 })?.takeIf { it.kind == kindKey }?.id ?: mains.firstOrNull()?.id
    val cat = d.category(catId)
    val account = d.account(pickedAcc.takeIf { it > 0 }) ?: d.accounts.firstOrNull()
    val fromAcc = d.account(fromId.takeIf { it > 0 })
    val toAcc = d.account(toId.takeIf { it > 0 })
    val ledger = d.ledger(pickedLedger.takeIf { it > 0 })
        ?: d.activeLedgers.firstOrNull { it.id == Prefs.ledger }
        ?: d.activeLedgers.firstOrNull()

    val amount = evaluate(expr)
    val fee = evaluate(feeExpr)
    val canSave = amount > 0 && if (isTransfer) {
        fromAcc != null && toAcc != null && fromAcc.id != toAcc.id
    } else {
        catId != null && account != null
    }

    fun press(k: String) {
        tick()
        val typingFee = isTransfer && feeMode
        val cur = if (typingFee) feeExpr else expr
        val next = when {
            k == "⌫" -> cur.dropLast(1)
            k == "=" -> evaluate(cur).takeIf { it > 0 }?.toString() ?: ""
            k.length == 1 && k[0] in OPS -> when {
                cur.isEmpty() -> cur
                cur.last() in OPS -> cur.dropLast(1) + k
                else -> cur + k
            }
            else -> {
                val token = cur.takeLastWhile { it.isDigit() }
                when {
                    token.isEmpty() && k.startsWith("0") -> cur
                    token.length + k.length > MAX_DIGITS -> cur
                    else -> cur + k
                }
            }
        }
        if (typingFee) feeExpr = next else expr = next
    }

    fun save(keepGoing: Boolean) {
        if (!canSave) return
        tick()
        val base = if (editing > 0) edited else null
        vm.saveTxn(
            Txn(
                id = if (editing > 0) editing else 0,
                amount = amount,
                kind = kindKey,
                categoryId = if (isTransfer) null else catId,
                accountId = if (isTransfer) fromAcc!!.id else account!!.id,
                toAccountId = if (isTransfer) toAcc!!.id else null,
                merchant = remark.trim(),
                note = base?.note ?: "",
                time = time,
                ledgerId = ledger?.id ?: 1L,
                reimburse = if (!isTransfer && reimburse) (if (base?.reimburse == 2) 2 else 1) else 0,
                starred = base?.starred ?: false,
                fee = if (isTransfer) fee else 0,
            ),
        )
        if (keepGoing) {
            expr = ""; remark = ""; feeExpr = ""; feeMode = false
            editing = 0L
            savedTick++
        } else {
            nav.back()
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 1. Close + kind switch
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CRoundButton(Icons.Rounded.Close, "Tutup", { nav.back() }, size = 42.dp)
            Spacer(Modifier.width(10.dp))
            MeowSegmented(
                // Same order as Beranda (Masuk first); internal kind stays 0 = out, 1 = in, 2 = transfer.
                listOf("Masuk", "Keluar", "Transfer"), when (kind) { 0 -> 1; 1 -> 0; else -> 2 }, { i -> val k = when (i) { 0 -> 1; 1 -> 0; else -> 2 }; kind = k; if (k != 2) feeMode = false },
                Modifier.weight(1f), fill = true, fontSize = 14.5.sp,
            )
        }

        // 2. Amount card + categories (or transfer fee); scrolls on short phones
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(K.Yellow)
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 16.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val chipMod = Modifier.weight(1f, fill = false).padding(end = 10.dp)
                    if (isTransfer) {
                        CardChip("transfer", "Transfer", chipMod)
                    } else {
                        CardChip(cat?.icon ?: "category", cat?.name ?: "Kategori", chipMod) { tick(); allCats = true }
                    }
                    val today = startOfDay(time) == startOfDay()
                    CardChip("history", if (today) "Hari ini" else fmt(time, "d MMM, HH.mm")) { tick(); showCal = true }
                }
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().let { if (isTransfer) it.pressable { feeMode = false } else it }) {
                    val size = amount.grouped().length.let { n ->
                        when { n <= 7 -> 54; n <= 9 -> 46; n <= 11 -> 40; n <= 13 -> 34; else -> 30 }
                    }.sp
                    Money(amount, size, color = if (amount > 0) K.Green else K.Green.copy(alpha = .35f))
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    val hasOp = expr.any { it in OPS }
                    val (line, lineColor) = when {
                        showSaved -> "Tersimpan ✓" to K.Green
                        isTransfer && feeMode -> "Ketuk di sini untuk ubah jumlah" to K.Muted
                        hasOp -> prettyExpr(expr).let { if (it.length > 30) "…" + it.takeLast(29) else it } to K.Muted
                        expr.isEmpty() -> "Ketik nominalnya" to K.Muted
                        else -> "" to K.Muted
                    }
                    Text(line, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = lineColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    val tags = listOfNotNull(
                        ledger?.name?.takeIf { d.activeLedgers.size > 1 },
                        "Reimburse".takeIf { !isTransfer && reimburse },
                    ).joinToString(" · ")
                    if (tags.isNotEmpty()) Text(tags, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = K.Green, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(14.dp))
                if (isTransfer) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TransferAccountChip(d, fromAcc, "Dari", nav, Modifier.weight(1f)) { id -> if (id == toId) toId = fromId; fromId = id }
                        Box(
                            Modifier.padding(horizontal = 6.dp).size(34.dp).clip(CircleShape).background(K.Tile).pressable {
                                tick(); val f = fromId; fromId = toId; toId = f
                            },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.SwapHoriz, "Tukar akun", tint = K.Green, modifier = Modifier.size(22.dp)) }
                        TransferAccountChip(d, toAcc, "Ke", nav, Modifier.weight(1f)) { id -> if (id == fromId) fromId = toId; toId = id }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (!isTransfer) {
                        Row(
                            Modifier.widthIn(max = 140.dp).height(40.dp).clip(RoundedCornerShape(14.dp)).background(K.Tile).anchor(accAnchor)
                                .pressable { tick(); accMenu = true }.padding(start = 8.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CatIcon(accountIcon(account), 24.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(account?.name ?: "Akun", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            Icon(Icons.Rounded.ArrowDropDown, null, tint = K.Ink, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Row(
                        Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(14.dp)).background(K.Tile).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CatIcon("pencil", 20.dp)
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = remark, onValueChange = { remark = it }, singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = K.Ink), cursorBrush = SolidColor(K.Green),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner -> Box { if (remark.isEmpty()) Text("Tambah catatan", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1); inner() } },
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    val flagged = !isTransfer && reimburse
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(if (flagged) K.Green else K.Tile).anchor(moreAnchor)
                            .pressable { tick(); moreMenu = true },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.MoreHoriz, "Buku kas dan reimburse", tint = if (flagged) Color.White else K.Ink, modifier = Modifier.size(22.dp)) }
                }
            }

            Spacer(Modifier.height(18.dp))
            if (isTransfer) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Biaya transfer", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.weight(1f))
                    CRoundButton(Icons.Rounded.QuestionMark, "Tentang biaya transfer", { feeHelp = true }, size = 30.dp)
                }
                Spacer(Modifier.height(10.dp))
                val feeShape = RoundedCornerShape(18.dp)
                Row(
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(58.dp).clip(feeShape).background(K.Card)
                        .then(if (feeMode) Modifier.border(2.dp, K.Green, feeShape) else Modifier)
                        .pressable { tick(); feeMode = true }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CatIcon("coinbag", 30.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (feeMode) "Ketik biayanya" else "Biaya admin (opsional)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = K.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Rp" + if (feeExpr.isEmpty()) "0" else prettyExpr(feeExpr), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (feeExpr.isEmpty()) K.Muted else K.Green, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Kategori", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.weight(1f))
                    Text("Semua ›", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = K.Green, modifier = Modifier.pressable { tick(); allCats = true })
                }
                Spacer(Modifier.height(10.dp))
                CategoryRow(d, mains, catId, onPick = { pickedCat = it }, onSettings = { nav.go(R.CATEGORIES) })
            }
            Spacer(Modifier.height(8.dp))
        }

        // 3. Flat keypad + full-width save
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            KeyRow { DigitKey("1") { press("1") }; DigitKey("2") { press("2") }; DigitKey("3") { press("3") }; OpKey("Hapus", icon = Icons.AutoMirrored.Rounded.Backspace) { press("⌫") } }
            KeyRow { DigitKey("4") { press("4") }; DigitKey("5") { press("5") }; DigitKey("6") { press("6") }; OpKey("Tambah", glyph = "+") { press("+") } }
            KeyRow { DigitKey("7") { press("7") }; DigitKey("8") { press("8") }; DigitKey("9") { press("9") }; OpKey("Kurang", glyph = "−") { press("−") } }
            KeyRow {
                DigitKey("000") { press("000") }; DigitKey("0") { press("0") }
                Box(Modifier.weight(1f).fillMaxHeight().let { if (canSave) it.pressable { save(true) } else it }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAddCheck, "Simpan dan catat lagi", tint = if (canSave) K.Green else K.Muted.copy(alpha = .5f), modifier = Modifier.size(28.dp))
                }
                OpKey("Hitung", glyph = "=") { press("=") }
            }
        }
        Box(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
                .height(54.dp).clip(RoundedCornerShape(18.dp)).background(if (canSave) K.Green else K.RedTrack)
                .let { if (canSave) it.pressable { save(false) } else it },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (amount > 0) "Simpan · Rp${amount.grouped()}" else "Simpan",
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    // ---------------------------------------------------------------- overlays
    if (showCal) {
        CalendarCard(time, onDismiss = { showCal = false }) { time = it; showCal = false }
    }
    if (moreMenu) {
        PillMenu(moreAnchor.rect, onDismiss = { moreMenu = false }) {
            d.activeLedgers.forEach { l ->
                PillOption(l.name, Tone.of(l.tone), l.id == ledger?.id, onClick = { pickedLedger = l.id; moreMenu = false }, icon = l.icon, sub = "Buku kas")
            }
            PillOption("Kelola buku kas", Tone.PEACH, false, onClick = { moreMenu = false; nav.go(R.LEDGERS) }, glyph = Icons.Rounded.Settings)
            if (!isTransfer) {
                PillOption(
                    "Reimburse", Tone.MINT, reimburse, onClick = { tick(); reimburse = !reimburse; moreMenu = false },
                    icon = "reimburse", sub = "Ditagih balik nanti",
                )
            }
        }
    }
    if (accMenu) {
        PillMenu(accAnchor.rect, onDismiss = { accMenu = false }) {
            d.accounts.forEach { a ->
                PillOption(
                    a.name, Tone.of(a.tone), a.id == account?.id,
                    onClick = { pickedAcc = a.id; accMenu = false }, icon = accountIcon(a), sub = money(d.balanceOf(a)),
                )
            }
            PillOption("Tambah akun", Tone.PEACH, false, onClick = { accMenu = false; nav.go(R.accountEdit()) }, glyph = Icons.Rounded.Add)
        }
    }
    if (allCats && !isTransfer) {
        AllCategoriesSheet(
            d, mains, catId,
            onPick = { id, done -> pickedCat = id; if (done) allCats = false },
            onSettings = { allCats = false; nav.go(R.CATEGORIES) },
            onDismiss = { allCats = false },
        )
    }
    if (feeHelp) {
        Overlay(onDismiss = { feeHelp = false }) {
            FloatingCard {
                RedFirst("Biaya transfer", 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Biaya admin transfer dipotong dari akun asal, terpisah dari jumlah yang dikirim.",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(16.dp))
                RedButton("Mengerti", Modifier.fillMaxWidth()) { feeHelp = false }
            }
        }
    }
}

// ---------------------------------------------------------------- amount card

/** White pill on the yellow card: two-tone icon + label (category, date). */
@Composable
private fun CardChip(icon: String, label: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(K.Tile)
            .let { if (onClick != null) it.pressable(onClick) else it }.padding(start = 6.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CatIcon(icon, 26.dp)
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TransferAccountChip(d: AppData, chosen: Account?, hint: String, nav: Nav, modifier: Modifier, onPick: (Long) -> Unit) {
    val a = rememberAnchor()
    var open by remember { mutableStateOf(false) }
    val tick = rememberTick()
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).background(K.Tile).anchor(a)
            .pressable { tick(); open = true }.padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (chosen != null) {
            CatIcon(accountIcon(chosen), 26.dp)
        } else {
            Box(Modifier.size(26.dp).clip(CircleShape).background(K.Field), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, null, tint = K.Green, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(hint, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, maxLines = 1)
            Text(chosen?.name ?: "Pilih akun", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (chosen != null) K.Ink else K.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (open) {
        PillMenu(a.rect, onDismiss = { open = false }) {
            d.accounts.forEach { acc ->
                PillOption(
                    acc.name, Tone.of(acc.tone), acc.id == chosen?.id,
                    onClick = { onPick(acc.id); open = false }, icon = accountIcon(acc), sub = money(d.balanceOf(acc)),
                )
            }
            PillOption("Tambah akun", Tone.PEACH, false, onClick = { open = false; nav.go(R.accountEdit()) }, glyph = Icons.Rounded.Add)
        }
    }
}

// ---------------------------------------------------------------- categories

/** One scrollable row of round category buttons; the last one opens category settings. */
@Composable
private fun CategoryRow(d: AppData, mains: List<Category>, chosen: Long?, onPick: (Long) -> Unit, onSettings: () -> Unit) {
    val rootId = d.rootOf(chosen)?.id
    val tick = rememberTick()
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        mains.forEach { c ->
            val selected = c.id == rootId
            val label = if (selected) d.category(chosen)?.name ?: c.name else c.name
            CategoryRound(d, c, label, selected, chosen, onPick)
        }
        CatCircle(label = "Atur", selected = false, modifier = Modifier.pressable { tick(); onSettings() }, glyph = Icons.Rounded.Settings)
    }
}

/** Round category button: tap picks it (tap again or long-press opens its subcategories). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryRound(d: AppData, c: Category, label: String, selected: Boolean, chosen: Long?, onPick: (Long) -> Unit) {
    val subs = d.subsOf(c.id)
    val a = rememberAnchor()
    var open by remember { mutableStateOf(false) }
    val tick = rememberTick()
    CatCircle(
        label = label, selected = selected, icon = c.icon, hasSubs = subs.isNotEmpty(), circleModifier = Modifier.anchor(a),
        modifier = Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
            onLongClick = if (subs.isNotEmpty()) ({ tick(); open = true }) else null,
        ) { tick(); if (selected && subs.isNotEmpty()) open = true else onPick(c.id) },
    )
    if (open) {
        PillMenu(a.rect, onDismiss = { open = false }) {
            PillOption(c.name, Tone.of(c.tone), chosen == c.id, onClick = { onPick(c.id); open = false }, icon = c.icon)
            subs.forEach { s ->
                PillOption(s.name, Tone.of(s.tone), chosen == s.id, onClick = { onPick(s.id); open = false }, icon = s.icon)
            }
        }
    }
}

/** 58dp cream circle with a two-tone icon and a label; selected = yellow with a green ring. */
@Composable
private fun CatCircle(
    label: String, selected: Boolean, modifier: Modifier = Modifier, icon: String? = null, glyph: ImageVector? = null,
    hasSubs: Boolean = false, circleModifier: Modifier = Modifier,
) {
    Column(modifier.width(66.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(circleModifier.size(58.dp)) {
            Box(
                Modifier.fillMaxSize().clip(CircleShape).background(if (selected) K.Yellow else K.Card)
                    .then(if (selected) Modifier.border(2.4.dp, K.Green, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) CatIcon(icon, 32.dp)
                else if (glyph != null) Icon(glyph, null, tint = K.Green, modifier = Modifier.size(26.dp))
            }
            if (hasSubs) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape).background(K.Tile).border(1.dp, K.Line, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.ArrowDropDown, "Ada subkategori", tint = K.Green, modifier = Modifier.size(16.dp)) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (selected) K.Green else K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

/** "Semua ›": every main category in a grid, then the subcategories of the picked one. */
@Composable
private fun AllCategoriesSheet(
    d: AppData, mains: List<Category>, chosen: Long?, onPick: (Long, Boolean) -> Unit, onSettings: () -> Unit, onDismiss: () -> Unit,
) {
    val tick = rememberTick()
    val root = d.rootOf(chosen)
    Overlay(onDismiss = onDismiss) {
        SheetCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Semua kategori", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.weight(1f))
                Text("Atur ›", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = K.Green, modifier = Modifier.pressable { tick(); onSettings() })
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                mains.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { c ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                                val subs = d.subsOf(c.id)
                                CatCircle(
                                    label = c.name, selected = c.id == root?.id, icon = c.icon, hasSubs = subs.isNotEmpty(),
                                    modifier = Modifier.pressable { tick(); onPick(c.id, subs.isEmpty()) },
                                )
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            val subs = root?.let { d.subsOf(it.id) }.orEmpty()
            if (root != null && subs.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text("Subkategori ${root.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = K.Muted)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CChip(root.name, icon = root.icon, selected = chosen == root.id, height = 40.dp, onClick = { tick(); onPick(root.id, true) })
                    subs.forEach { s ->
                        CChip(s.name, icon = s.icon, selected = chosen == s.id, height = 40.dp, onClick = { tick(); onPick(s.id, true) })
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- keypad

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
private fun RowScope.DigitKey(label: String, onClick: () -> Unit) {
    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).pressable(onClick), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1)
    }
}

/** Operator key: 48dp sage circle with a green glyph (text) or icon. */
@Composable
private fun RowScope.OpKey(label: String, icon: ImageVector? = null, glyph: String? = null, onClick: () -> Unit) {
    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(K.SageCard).pressable(onClick), contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, label, tint = K.Green, modifier = Modifier.size(24.dp))
            else Text(glyph ?: "", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = K.Green, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------- helpers


/** Balance with a real minus sign (U+2212) for negatives. */
private fun money(v: Long): String = (if (v < 0) "−" else "") + abs(v).grouped()

/** "18000+5000" -> "18.000 + 5.000". */
private fun prettyExpr(expr: String): String {
    val sb = StringBuilder()
    val num = StringBuilder()
    fun flush() {
        if (num.isNotEmpty()) { sb.append(num.toString().toLongOrNull()?.grouped() ?: num.toString()); num.clear() }
    }
    for (c in expr) {
        if (c.isDigit()) num.append(c) else { flush(); sb.append(' ').append(c).append(' ') }
    }
    flush()
    return sb.toString().trim()
}

/** Safe calculator: digits with + and − only, evaluated left to right. A trailing operator is ignored; result is never negative. */
private fun evaluate(expr: String): Long {
    var result = 0L
    var op = '+'
    val num = StringBuilder()
    fun apply() {
        if (num.isEmpty()) return
        val v = num.toString().take(MAX_DIGITS).toLongOrNull() ?: 0L
        result = if (op == '−') result - v else result + v
        num.clear()
    }
    for (c in expr) {
        when {
            c.isDigit() -> num.append(c)
            c in OPS -> { apply(); op = c }
        }
    }
    apply()
    return result.coerceAtLeast(0L)
}
