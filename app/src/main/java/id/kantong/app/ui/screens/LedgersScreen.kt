package id.kantong.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Ledger
import id.kantong.app.data.Prefs
import id.kantong.app.data.grouped
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CList
import id.kantong.app.ui.components.CListRow
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CatetSheet
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.MeowSegmented
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.SheetHeader
import id.kantong.app.ui.components.SmallChip
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K
import id.kantong.app.ui.theme.Tone
import kotlin.math.abs

private val LEDGER_ICONS = listOf(
    "ledger", "home", "business", "travel", "shopping", "food", "edu", "health",
    "gift", "charity", "invest", "jar", "bookmark", "family", "car",
)

/**
 * Buku kas (R89): back + round "+", big title, a yellow summary card (book used on Beranda, active books,
 * records), Aktif / Arsip segment, the books as a cream list, then the green "Buku kas baru" button.
 */
@Composable
fun LedgersScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    val hidden = Prefs.hideAmounts
    val list = d.ledgers.filter { it.archived == (tab == 1) }
    val stats = remember(d) {
        d.allTxns.groupBy { it.ledgerId }.mapValues { (_, ts) ->
            ts.size to ts.sumOf { t -> when (t.kind) { "in" -> t.amount; "out" -> -t.amount; else -> 0L } }
        }
    }
    val activeCount = d.ledgers.count { !it.archived }
    val archivedCount = d.ledgers.size - activeCount
    val current = d.ledger(Prefs.ledger)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 42.dp)
            Spacer(Modifier.weight(1f))
            CRoundButton(Icons.Rounded.Add, "Tambah buku kas", { adding = true }, size = 42.dp, outlined = true)
        }
        Spacer(Modifier.height(14.dp))
        CTitle(
            "Buku kas",
            subtitle = "$activeCount aktif" + if (archivedCount > 0) " · $archivedCount diarsipkan" else "",
        )
        Spacer(Modifier.height(16.dp))

        // yellow summary: the book shown on Beranda + counts
        CCard(K.Yellow, radius = 28.dp, padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
            Text("Dipakai di Beranda", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(K.Tile), contentAlignment = Alignment.Center) {
                    CatIcon(current?.icon ?: "ledger", 30.dp)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    current?.name ?: "Semua buku", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = K.Green,
                    maxLines = 1, modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LedgerStat("Buku aktif", "$activeCount")
                Box(Modifier.padding(end = 16.dp).width(1.5.dp).height(38.dp).background(K.YellowLine))
                LedgerStat("Total catatan", d.allTxns.size.grouped())
            }
        }
        Spacer(Modifier.height(18.dp))

        MeowSegmented(listOf("Aktif", "Arsip"), tab, { tab = it }, Modifier.fillMaxWidth(), fill = true)
        Spacer(Modifier.height(14.dp))

        if (list.isEmpty()) {
            LedgerEmptyCard(
                if (tab == 1) "Belum ada arsip" else "Belum ada buku kas",
                if (tab == 1) "Buku kas yang diarsipkan akan tampil di sini."
                else "Pisahkan catatan harian, rumah tangga, atau liburan dalam buku sendiri.",
            )
        } else {
            CList {
                list.forEachIndexed { i, l ->
                    val (count, net) = stats[l.id] ?: (0 to 0L)
                    CListRow(
                        l.icon, l.name,
                        if (count == 0) "Belum ada catatan" else "$count transaksi",
                        divider = i > 0,
                        onClick = { nav.go(R.ledger(l.id)) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (hidden) "•••••" else (if (net < 0) "−" else "") + abs(net).grouped(),
                                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (net < 0) K.Danger else K.Ink, maxLines = 1,
                                )
                                if (Prefs.ledger == l.id) {
                                    Spacer(Modifier.height(2.dp))
                                    SmallChip("Dipakai", K.SageCard, K.Green)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = K.Muted)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        RedButton("Buku kas baru", Modifier.fillMaxWidth()) { adding = true }
        Spacer(Modifier.height(32.dp))
    }

    if (adding) LedgerFormSheet(d, vm, null) { adding = false }
}

@Composable
private fun RowScope.LedgerStat(label: String, value: String) {
    Column(Modifier.weight(1f)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1)
    }
}

/** Calm empty card (no mascot): sticker, title and a friendly line on cream. */
@Composable
private fun LedgerEmptyCard(title: String, text: String) {
    CCard(K.Card, radius = 22.dp, padding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            CatIcon("ledger", 48.dp)
            Spacer(Modifier.height(10.dp))
            Text(title, fontSize = 16.5.sp, fontWeight = FontWeight.Bold, color = K.Ink)
            Spacer(Modifier.height(4.dp))
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted, textAlign = TextAlign.Center, lineHeight = 18.sp)
        }
    }
}

/**
 * Add / edit ledger sheet (also opened from the ledger detail screen): name, sticker, color, Batal / Simpan.
 * [editing] = null adds a new ledger at the end of the list.
 */
@Composable
internal fun LedgerFormSheet(d: AppData, vm: AppViewModel, editing: Ledger?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var icon by remember { mutableStateOf(editing?.icon ?: "ledger") }
    var tone by remember { mutableStateOf(editing?.let { Tone.of(it.tone) } ?: Tone.PEACH) }
    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader(if (editing == null) "Buku kas baru" else "Ubah buku kas", onDismiss)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(K.Yellow), contentAlignment = Alignment.Center) {
                        CatIcon(icon, 34.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it.take(30) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink),
                        cursorBrush = SolidColor(K.Green),
                        decorationBox = { inner ->
                            Box(
                                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(18.dp)).background(K.Field).padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (name.isEmpty()) Text("Nama buku kas", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Muted.copy(alpha = .6f))
                                inner()
                            }
                        },
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text("Ikon", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink2)
                Spacer(Modifier.height(10.dp))
                LEDGER_ICONS.chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { k ->
                            val on = k == icon
                            Box(
                                Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                                    .background(if (on) K.Yellow else K.Field)
                                    .let { m -> if (on) m.border(2.dp, K.Green, CircleShape) else m }
                                    .pressable { icon = k },
                                contentAlignment = Alignment.Center,
                            ) { CatIcon(k, 34.dp) }
                        }
                        repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Warna", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = K.Ink2)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tone.entries.forEach { t ->
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).background(t.strong)
                                .let { m -> if (t == tone) m.border(2.5.dp, K.Ink, CircleShape) else m }
                                .pressable { tone = t },
                            contentAlignment = Alignment.Center,
                        ) { if (t == tone) Icon(Icons.Rounded.Check, null, tint = K.OnGreen, modifier = Modifier.size(18.dp)) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                RedButton("Simpan", Modifier.weight(1f), enabled = name.isNotBlank()) {
                    vm.saveLedger(
                        Ledger(
                            id = editing?.id ?: 0L,
                            name = name.trim(),
                            icon = icon,
                            tone = tone.name,
                            archived = editing?.archived ?: false,
                            sortOrder = editing?.sortOrder ?: ((d.ledgers.maxOfOrNull { it.sortOrder } ?: 0) + 1),
                        ),
                    )
                    onDismiss()
                }
            }
        }
    }
}
