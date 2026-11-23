package id.kantong.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.Account
import id.kantong.app.data.AppData
import id.kantong.app.data.Prefs
import id.kantong.app.data.Txn
import id.kantong.app.data.dayLabel
import id.kantong.app.data.fmt
import id.kantong.app.data.grouped
import id.kantong.app.data.startOfDay
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone

/** Sticker key for an account (its own icon, else one picked from its type). */
fun accountIcon(a: Account?): String = a?.icon?.ifEmpty { null } ?: when (a?.type) { "bank" -> "bank"; "ewallet" -> "ewallet"; "cash" -> "cash"; else -> "wallet" }

/** Pastel family of a record: its category's tone, peach for transfers. */
fun toneOf(d: AppData, t: Txn): Tone = if (t.kind == "transfer") Tone.PEACH else Tone.of(d.category(t.categoryId)?.tone ?: "SAGE")

/** Signed amount text and color: −Rp18.000 in ink, +Rp1.250.000 in green, transfers unsigned and neutral. */
fun amountText(t: Txn): Pair<String, Color> = when (t.kind) {
    "out" -> "−Rp${t.amount.grouped()}" to K.Expense
    "in" -> "+Rp${t.amount.grouped()}" to K.Income
    else -> "Rp${t.amount.grouped()}" to K.Ink2
}

/** Title (category, or "Transfer") and remark (merchant/note, or "BCA → GoPay"). */
fun recordTitle(d: AppData, t: Txn): Pair<String, String> = if (t.kind == "transfer") {
    t.merchant.ifBlank { "Transfer" } to when {
        d.account(t.accountId) == null -> "${otherSide(t)} → ${d.account(t.toAccountId)?.name ?: "-"}"
        d.account(t.toAccountId) == null -> "${d.account(t.accountId)?.name ?: "-"} → ${otherSide(t)}"
        else -> "${d.account(t.accountId)?.name} → ${d.account(t.toAccountId)?.name}"
    }
} else {
    (d.category(t.categoryId)?.name ?: "Tanpa kategori") to t.merchant.ifBlank { t.note }
}

private const val DAY_MS = 86_400_000L

/**
 * Records grouped by day: a small grey day label ("Hari ini", "Kemarin", "Kamis, 19 Nov") with the day's
 * net total on the right, then a cream card holding that day's rows (no dividers).
 */
@Composable
fun RecordTimeline(d: AppData, txns: List<Txn>, onOpen: (Txn) -> Unit, withYear: Boolean = false) {
    val days = txns.sortedByDescending { it.time }.groupBy { startOfDay(it.time) }
    Column {
        days.forEach { (day, list) ->
            DayHeader(day, list.filter { it.kind == "in" }.sumOf { it.amount }, list.filter { it.kind == "out" }.sumOf { it.amount }, withYear)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp)).background(K.Card).padding(vertical = 4.dp)) {
                list.forEach { t -> CleanRow(d, t) { onOpen(t) } }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

/**
 * One history row: two-tone icon, title (+ Reimburse chip, star when bookmarked), a muted line
 * "● Kategori › ● Akun" (+ time), and the signed amount.
 */
@Composable
fun CleanRow(d: AppData, t: Txn, onClick: () -> Unit) {
    val (amount, color) = amountText(t)
    val cat = d.category(t.categoryId)
    val account = d.account(t.accountId)
    val remark = t.merchant.ifBlank { t.note }
    val transfer = t.kind == "transfer"
    val useRemark = !transfer && Prefs.showRemark && remark.isNotBlank()
    val title = when {
        transfer -> t.merchant.ifBlank { "Transfer" }
        useRemark -> remark
        else -> cat?.name ?: "Tanpa kategori"
    }
    Row(
        Modifier.fillMaxWidth().pressable(onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (transfer) {
            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.CompareArrows, null, tint = K.Ink, modifier = Modifier.size(26.dp))
            }
        } else CatIcon(cat?.icon ?: "other", 30.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (Prefs.showReimburseIcon && t.reimburse > 0) {
                    Spacer(Modifier.width(7.dp))
                    SmallChip(if (t.reimburse == 2) "Diganti" else "Reimburse", if (t.reimburse == 2) K.SageCard else K.Yellow)
                }
                if (t.starred) {
                    Spacer(Modifier.width(6.dp))
                    StarMark(15.dp)
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    transfer -> {
                        d.account(t.accountId)?.let { AccountMark(it) } ?: SubText(otherSide(t))
                        Sep()
                        d.account(t.toAccountId)?.let { AccountMark(it) } ?: SubText(otherSide(t))
                    }
                    useRemark -> {
                        Dot(K.Gold)
                        SubText(cat?.name ?: "Tanpa kategori", Modifier.weight(1f, fill = false))
                        Sep()
                        AccountMark(account)
                    }
                    else -> AccountMark(account)
                }
                if (Prefs.showLedgerIcon) d.ledger(t.ledgerId)?.let {
                    Spacer(Modifier.width(6.dp))
                    CatIcon(it.icon, 14.dp)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(amount.replace("Rp", ""), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1)
    }
}

@Composable
private fun SubText(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)

@Composable
private fun Sep() = SubText("  ›  ")

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(6.dp).clip(CircleShape).background(color))
    Spacer(Modifier.width(5.dp))
}

/** Account in the row's second line: a green dot (or its icon when the display option is on) and the name. */
@Composable
private fun AccountMark(a: Account?) {
    if (Prefs.showAccountIcon && a != null) {
        CatIcon(accountIcon(a), 14.dp)
        Spacer(Modifier.width(4.dp))
    } else Dot(K.Green)
    SubText(a?.name ?: "-", Modifier.widthIn(max = 130.dp))
}

/** Small pill chip next to a title ("Reimburse", "Hampir habis"). */
@Composable
internal fun SmallChip(text: String, bg: Color, color: Color = K.Ink) {
    Text(
        text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(bg).padding(horizontal = 9.dp, vertical = 1.dp),
    )
}

/** Gold star with an ink outline; [filled] = false draws only the outline. */
@Composable
fun StarMark(size: Dp, filled: Boolean = true) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 18f
        val pts = listOf(9f to 1.8f, 11.2f to 6.3f, 16.2f to 7f, 12.6f to 10.5f, 13.5f to 15.4f, 9f to 13.1f, 4.5f to 15.4f, 5.4f to 10.5f, 1.8f to 7f, 6.8f to 6.3f)
        val p = Path().apply {
            pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * u, y * u) else lineTo(x * u, y * u) }
            close()
        }
        if (filled) drawPath(p, K.Gold)
        drawPath(p, K.Ink, style = Stroke(1.5f * u))
    }
}

/** Day label left ("Hari ini", "Kemarin", "Kamis, 19 Nov"), the day's net total right (green +, ink −). */
@Composable
fun DayHeader(day: Long, income: Long, expense: Long, withYear: Boolean = false) {
    val sameYear = fmt(day, "yyyy") == fmt(System.currentTimeMillis(), "yyyy")
    val today = startOfDay()
    val label = when {
        withYear && (day == today || day == today - DAY_MS) -> "${dayLabel(day)}, ${fmt(day, "EEEE d MMM")}"
        withYear && !sameYear -> fmt(day, "EEEE, d MMM yyyy")
        else -> dayLabel(day)
    }
    val net = income - expense
    val right = when {
        income == 0L && expense == 0L -> null
        net >= 0 -> "+Rp${net.grouped()}"
        else -> "−Rp${(-net).grouped()}"
    }
    // Day label in ink, the day's net total in a soft pill: readable, but never competing with row amounts.
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 2.dp, top = 4.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink2, modifier = Modifier.weight(1f), maxLines = 1)
        if (right != null) Text(
            right.replace("Rp", ""), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (net >= 0) K.Income else K.Ink2, maxLines = 1,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (net >= 0) K.SageCard else K.Field).padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

/** One record as a standalone row (kept for callers that list records one by one). */
@Composable
fun RecordRow(d: AppData, t: Txn, hourLabel: String?, last: Boolean, onClick: () -> Unit) {
    RecordTile(d, t, Modifier.padding(bottom = if (last) 0.dp else 8.dp), onClick)
}

/** A single record on its own cream card (e.g. the preview in display settings). */
@Composable
fun RecordTile(d: AppData, t: Txn, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp)).background(K.Card).padding(vertical = 4.dp)) {
        CleanRow(d, t, onClick)
    }
}

/** Bottom sheet used by the record, budget and period sheets: rounded top, handle, cream background. */
@Composable
internal fun CatetSheet(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier.fillMaxWidth().heightIn(max = 760.dp).clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(K.Sheet)
            .clickable(remember { MutableInteractionSource() }, null) {}
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp + navBottom),
    ) {
        CHandle(Modifier.align(Alignment.CenterHorizontally))
        content()
    }
}

/** Sheet title with the round close button. */
@Composable
internal fun SheetHeader(title: String, onClose: () -> Unit, size: Int = 22) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = size.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        CRoundButton(Icons.Rounded.Close, "Tutup", onClose, size = 34.dp)
    }
}

/** Outlined button with danger text (delete). */
@Composable
internal fun DangerOutlineButton(label: String, modifier: Modifier = Modifier, filled: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier.height(50.dp).clip(shape).background(if (filled) K.Danger else K.c(0xFFFDFBF6, 0xFF252A25))
            .let { if (filled) it else it.border(1.5.dp, K.c(0xFFC9A49E, 0xFF7A4A44), shape) }.pressable(onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (filled) K.OnGreen else K.Danger, maxLines = 1) }
}

/**
 * Detail sheet for a record: icon in a cream circle, title, date and time, the amount, Reimburse and
 * Bookmark toggle chips, the facts in a cream card, then Hapus (with a confirm step) and Ubah.
 */
@Composable
fun RecordDetailCard(
    d: AppData, t: Txn, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit,
    onStar: () -> Unit, onReimburse: (Int) -> Unit, onSplit: (() -> Unit)? = null,
) {
    var confirm by remember { mutableStateOf(false) }
    val transfer = t.kind == "transfer"
    val cat = d.category(t.categoryId)
    val root = d.rootOf(t.categoryId)
    val title = when {
        transfer -> t.merchant.ifBlank { "Transfer" }
        t.merchant.isNotBlank() -> t.merchant
        t.note.isNotBlank() -> t.note
        else -> cat?.name ?: "Tanpa kategori"
    }
    val note = t.note.takeIf { it.isNotBlank() && it != title }
    Overlay(onDismiss) {
        CatetSheet {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(62.dp).clip(CircleShape).background(K.Card), contentAlignment = Alignment.Center) {
                        if (transfer) Icon(Icons.AutoMirrored.Rounded.CompareArrows, null, tint = K.Ink, modifier = Modifier.size(32.dp))
                        else CatIcon(cat?.icon ?: "other", 38.dp)
                    }
                    Spacer(Modifier.weight(1f))
                    CRoundButton(Icons.Rounded.Close, "Tutup", onDismiss, size = 34.dp)
                }
                Spacer(Modifier.height(12.dp))
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(fmt(t.time, "EEEE, d MMMM yyyy · HH.mm"), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = K.Muted, modifier = Modifier.padding(top = 2.dp))
                Spacer(Modifier.height(8.dp))
                Money(
                    t.amount, 36.sp, color = when (t.kind) { "in" -> K.Income; "out" -> K.Expense; else -> K.Ink2 },
                    sign = when (t.kind) { "in" -> "+"; "out" -> "−"; else -> "" },
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!transfer) {
                        ToggleChip(if (t.reimburse == 2) "Sudah diganti" else "Reimburse", t.reimburse > 0, if (t.reimburse == 2) K.SageCard else K.Yellow, {
                            CatIcon("reimburse", 18.dp)
                        }) { onReimburse(if (t.reimburse > 0) 0 else 1) }
                    }
                    ToggleChip("Bookmark", t.starred, K.Yellow, { StarMark(16.dp, filled = t.starred) }, onStar)
                }
                Spacer(Modifier.height(16.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(K.Card).padding(horizontal = 16.dp, vertical = 2.dp)) {
                    if (transfer) {
                        InfoRow("Dari akun", d.account(t.accountId)?.name ?: "-", first = true)
                        InfoRow("Ke akun", d.account(t.toAccountId)?.name ?: "-")
                        if (t.fee > 0) InfoRow("Biaya transfer", "Rp${t.fee.grouped()}")
                    } else {
                        InfoRow(
                            "Kategori", if (cat != null && root != null && root.id != cat.id) "${root.name} · ${cat.name}" else cat?.name ?: "Tanpa kategori",
                            first = true, icon = cat?.icon,
                        )
                        InfoRow("Akun", d.account(t.accountId)?.name ?: "-")
                    }
                    InfoRow("Buku kas", d.ledger(t.ledgerId)?.name ?: "-")
                    if (note != null) InfoRow("Catatan", note)
                    if (t.reimburse > 0) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
                            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Sudah diganti", fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = K.Muted, modifier = Modifier.weight(1f))
                                MeowToggle(t.reimburse == 2) { onReimburse(if (it) 2 else 1) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            if (confirm) {
                Text("Hapus catatan ini? Tindakan ini tidak bisa dibatalkan.", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    CreamButton("Batal", Modifier.weight(1f)) { confirm = false }
                    DangerOutlineButton("Hapus", Modifier.weight(1f), filled = true, onClick = onDelete)
                }
            } else {
                if (onSplit != null && t.kind == "out") Box(
                    Modifier.padding(bottom = 9.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(15.dp)).background(K.Field).pressable(onSplit),
                    contentAlignment = Alignment.Center,
                ) { Text("Patungan dengan teman", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Green) }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    DangerOutlineButton("Hapus", Modifier.weight(1f)) { confirm = true }
                    Row(
                        Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(15.dp)).background(K.Green).pressable(onEdit),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Rounded.Edit, null, tint = K.OnGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ubah", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.OnGreen, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Toggle chip in the detail sheet: filled when on, outlined when off. */
@Composable
private fun ToggleChip(label: String, on: Boolean, onBg: Color, leading: @Composable () -> Unit, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.height(32.dp).clip(shape).background(if (on) onBg else K.Tile)
            .let { if (on) it else it.border(1.4.dp, K.c(0xFFE0D7C4, 0xFF3A3F3B), shape) }.pressable(onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1)
    }
}

@Composable
private fun InfoRow(label: String, value: String, first: Boolean = false, icon: String? = null) {
    Column {
        if (!first) Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = K.Muted)
            Spacer(Modifier.width(12.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { CatIcon(icon, 22.dp); Spacer(Modifier.width(6.dp)) }
                Text(
                    value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink, textAlign = TextAlign.End, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    }
}

/** Label for the side of a transfer that isn't an account: investments or debts. */
internal fun otherSide(t: Txn): String = when {
    t.merchant.startsWith("Beli ") || t.merchant.startsWith("Jual ") || t.merchant.startsWith("Dividen ") -> "Investasi"
    t.merchant.isNotBlank() -> "Utang-piutang"
    else -> "-"
}
