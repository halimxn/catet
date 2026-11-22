package id.kantong.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Category
import id.kantong.app.data.ID
import id.kantong.app.data.Prefs
import id.kantong.app.data.Txn
import id.kantong.app.data.grouped
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CSection
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CatetSheet
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.DangerOutlineButton
import id.kantong.app.ui.components.Donut
import id.kantong.app.ui.components.MeowSegmented
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.PillMenu
import id.kantong.app.ui.components.PillOption
import id.kantong.app.ui.components.ProgressBar
import id.kantong.app.ui.components.RecordDetailCard
import id.kantong.app.ui.components.RecordTimeline
import id.kantong.app.ui.components.SheetHeader
import id.kantong.app.ui.components.anchor
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.components.rememberAnchor
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone

/** Totals of one main category inside a ledger. */
private class LedgerSlice(val cat: Category?, val amount: Long) {
    val name: String get() = cat?.name ?: "Tanpa kategori"
    val icon: String get() = cat?.icon ?: "other"
}

private fun ledgerPct(part: Long, whole: Long): String =
    "%.1f".format(ID, if (whole > 0) part * 100.0 / whole else 0.0) + "%"

/** Chart color of the i-th category: the five fixed ChartCats, the rest share the "Lainnya" color. */
private fun ledgerColor(i: Int): Color = K.ChartCats.getOrNull(i) ?: K.SageMid

/**
 * Ledger detail (R89): back + round "⋯" (Ubah, Arsipkan / Aktifkan lagi, Hapus with records moved to
 * another book), big title, yellow summary card (sisa, Masuk | Keluar), Catatan / Statistik segment,
 * then the record timeline or the category breakdown per kind.
 */
@Composable
fun LedgerDetailScreen(d: AppData, vm: AppViewModel, nav: Nav, id: Long) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<Txn?>(null) }
    val settingsAnchor = rememberAnchor()
    val l = d.ledger(id)

    if (l == null) {
        // Not loaded yet, or just deleted (the screen is being popped).
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(12.dp))
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 42.dp)
            if (d.loaded) {
                Spacer(Modifier.height(24.dp))
                LedgerEmpty("ledger", "Buku kas tidak ditemukan", "Buku ini mungkin sudah dihapus.")
            }
        }
        return
    }

    val tone = Tone.of(l.tone)
    val hidden = Prefs.hideAmounts
    val txns = d.allTxns.filter { it.ledgerId == id }
    val outList = txns.filter { it.kind == "out" }
    val inList = txns.filter { it.kind == "in" }
    val out = outList.sumOf { it.amount }
    val inn = inList.sumOf { it.amount }
    val net = inn - out

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 42.dp)
            Spacer(Modifier.weight(1f))
            CRoundButton(
                Icons.Rounded.MoreHoriz, "Pengaturan buku", { menu = true },
                Modifier.anchor(settingsAnchor), size = 42.dp, outlined = true,
            )
        }
        Spacer(Modifier.height(14.dp))
        CTitle(
            l.name,
            subtitle = (if (l.archived) "Diarsipkan" else "Buku kas") + " · " +
                if (txns.isEmpty()) "belum ada catatan" else "${txns.size} transaksi",
        )
        Spacer(Modifier.height(16.dp))

        // yellow summary: what is left in this book, then Masuk | Keluar
        CCard(K.Yellow, radius = 28.dp, padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp)).pressable { Prefs.hideAmounts = !hidden },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Sisa buku ini", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
                        Spacer(Modifier.width(7.dp))
                        Icon(
                            if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            if (hidden) "Tampilkan nominal" else "Sembunyikan nominal",
                            tint = K.Ink2, modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Money(net, 44.sp, color = if (net < 0) K.Danger else K.Green, hidden = hidden)
                }
                Box(Modifier.size(54.dp).clip(CircleShape).background(K.Tile), contentAlignment = Alignment.Center) {
                    CatIcon(l.icon.ifBlank { "ledger" }, 34.dp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LedgerInOut(Icons.Rounded.ArrowUpward, K.SageMid, "Masuk", inn, K.Income, hidden)
                Box(Modifier.padding(end = 16.dp).width(1.5.dp).height(40.dp).background(K.YellowLine))
                LedgerInOut(Icons.Rounded.ArrowDownward, K.Pink, "Keluar", out, K.Expense, hidden)
            }
        }
        Spacer(Modifier.height(18.dp))

        MeowSegmented(listOf("Catatan", "Statistik"), tab, { tab = it }, Modifier.fillMaxWidth(), fill = true)
        Spacer(Modifier.height(16.dp))

        if (tab == 0) {
            if (txns.isEmpty()) {
                LedgerEmpty(l.icon.ifBlank { "ledger" }, "Belum ada catatan", "Catatan yang disimpan ke buku \"${l.name}\" akan tampil di sini.")
            } else {
                RecordTimeline(d, txns, onOpen = { open = it }, withYear = true)
            }
        } else {
            LedgerStatBlock(d, "Pengeluaran", outList, K.Expense, hidden)
            Spacer(Modifier.height(22.dp))
            LedgerStatBlock(d, "Pemasukan", inList, K.Income, hidden)
        }
        Spacer(Modifier.height(32.dp))
    }

    if (menu) {
        PillMenu(settingsAnchor.rect, { menu = false }) {
            PillOption("Ubah buku", tone, false, { menu = false; editing = true }, glyph = Icons.Rounded.Edit)
            PillOption(
                if (l.archived) "Aktifkan lagi" else "Arsipkan", tone, false,
                { menu = false; vm.saveLedger(l.copy(archived = !l.archived)) },
                glyph = if (l.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
            )
            PillOption("Hapus buku", tone, false, { menu = false; confirmDelete = true }, glyph = Icons.Rounded.Delete)
        }
    }

    if (editing) LedgerFormSheet(d, vm, l) { editing = false }

    if (confirmDelete) {
        val target = d.activeLedgers.firstOrNull { it.id != l.id }
        Overlay({ confirmDelete = false }) {
            CatetSheet {
                SheetHeader("Hapus buku kas", { confirmDelete = false })
                Spacer(Modifier.height(10.dp))
                Text(
                    if (target != null) "Hapus buku \"${l.name}\"? Catatannya dipindah ke buku \"${target.name}\"."
                    else "Buku \"${l.name}\" belum bisa dihapus karena tidak ada buku aktif lain untuk menampung catatannya.",
                    fontSize = 15.sp, fontWeight = FontWeight.Medium, color = K.Ink2, lineHeight = 21.sp,
                )
                Spacer(Modifier.height(20.dp))
                if (target != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CreamButton("Batal", Modifier.weight(1f)) { confirmDelete = false }
                        DangerOutlineButton("Hapus", Modifier.weight(1f), filled = true) {
                            if (Prefs.ledger == l.id) Prefs.ledger = 0L
                            vm.deleteLedger(l, target.id)
                            confirmDelete = false
                            nav.back()
                        }
                    }
                } else {
                    CreamButton("Tutup", Modifier.fillMaxWidth()) { confirmDelete = false }
                }
            }
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

@Composable
private fun RowScope.LedgerInOut(icon: ImageVector, bg: Color, label: String, value: Long, color: Color, hidden: Boolean) {
    Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(19.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = K.Ink, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
        }
        Text(
            if (hidden) "•••••" else "Rp${value.grouped()}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = color,
            maxLines = 1, modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Calm empty card (no mascot): sticker, title and a friendly line on cream. */
@Composable
private fun LedgerEmpty(icon: String, title: String, text: String) {
    CCard(K.Card, radius = 22.dp, padding = PaddingValues(horizontal = 20.dp, vertical = 28.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            CatIcon(icon, 48.dp)
            Spacer(Modifier.height(10.dp))
            Text(title, fontSize = 16.5.sp, fontWeight = FontWeight.Bold, color = K.Ink)
            Spacer(Modifier.height(4.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted, textAlign = TextAlign.Center, lineHeight = 18.sp)
        }
    }
}

/**
 * "Pengeluaran" / "Pemasukan": section header with the total, then a cream card with the donut by main
 * category (top five + "Lainnya"), its legend, and every category as a share bar.
 */
@Composable
private fun LedgerStatBlock(d: AppData, title: String, list: List<Txn>, totalColor: Color, hidden: Boolean) {
    val slices = list.groupBy { d.rootOf(it.categoryId)?.id }
        .map { (cid, ts) -> LedgerSlice(d.category(cid), ts.sumOf { it.amount }) }
        .sortedByDescending { it.amount }
    val total = slices.sumOf { it.amount }
    val top = slices.take(5)
    val rest = slices.drop(5).sumOf { it.amount }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CSection(title, Modifier.weight(1f))
        Text(if (hidden) "•••••" else "Rp${total.grouped()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = totalColor, maxLines = 1)
    }
    Spacer(Modifier.height(10.dp))

    CCard(K.Card, radius = 22.dp, padding = PaddingValues(18.dp)) {
        if (slices.isEmpty()) {
            Text(
                "Belum ada ${title.lowercase()} di buku ini.", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = K.Muted,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Donut(
                    top.mapIndexed { i, s -> s.amount.toFloat() to ledgerColor(i) } + if (rest > 0) listOf(rest.toFloat() to K.SageMid) else emptyList(),
                    size = 116.dp, stroke = 20.dp,
                )
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    top.forEachIndexed { i, s -> LedgerLegend(ledgerColor(i), s.name, ledgerPct(s.amount, total)) }
                    if (rest > 0) LedgerLegend(K.SageMid, "Lainnya", ledgerPct(rest, total))
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                slices.forEachIndexed { i, s ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        CatIcon(s.icon, 30.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    s.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (hidden) "•••••" else s.amount.grouped(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1)
                            }
                            Spacer(Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProgressBar(
                                    if (total > 0) s.amount.toFloat() / total else 0f, ledgerColor(i), Modifier.weight(1f),
                                    track = K.Field, height = 7.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(ledgerPct(s.amount, total), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerLegend(color: Color, name: String, pct: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(7.dp))
        Text(name, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(4.dp))
        Text(pct, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, maxLines = 1)
    }
}
