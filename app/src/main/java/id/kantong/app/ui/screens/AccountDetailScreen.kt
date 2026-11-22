package id.kantong.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreHoriz
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.Account
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Prefs
import id.kantong.app.data.Txn
import id.kantong.app.data.periodRange
import id.kantong.app.data.rupiah
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CChip
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.DangerOutlineButton
import id.kantong.app.ui.components.FloatingCard
import id.kantong.app.ui.components.MeowSegmented
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.Overlay
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
import kotlin.math.abs

/** Effect of one record on account [id] (same rules as AppData.balanceOf). */
private fun effectOn(t: Txn, id: Long): Long = when {
    t.kind == "in" && t.accountId == id -> t.amount
    t.kind == "out" && t.accountId == id -> -t.amount
    t.kind == "transfer" && t.accountId == id -> -t.amount - t.fee
    t.kind == "transfer" && t.toAccountId == id -> t.amount
    else -> 0L
}

private fun typeLabel(a: Account): String = when (a.type) {
    "cash" -> "Tunai"
    "ewallet" -> "E-wallet"
    "bank" -> "Bank"
    else -> "Akun"
}

/**
 * Account detail (R89): round back button, account name + type, yellow balance card with this month's
 * Masuk | Keluar, Ubah / Transfer chips, the account chart, then its records (or transfers) as a timeline.
 * The "⋯" menu keeps Ubah and Hapus (only without records).
 */
@Composable
fun AccountDetailScreen(d: AppData, vm: AppViewModel, nav: Nav, id: Long) {
    val a = d.account(id)
    if (a == null) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(10.dp))
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 42.dp)
            Spacer(Modifier.height(14.dp))
            CTitle("Akun tidak ditemukan", subtitle = "Akun ini mungkin sudah dihapus.")
        }
        return
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var range by rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<Txn?>(null) }
    val menuAnchor = rememberAnchor()
    val hidden = Prefs.hideAmounts

    val touching = remember(d.allTxns, id) { d.allTxns.filter { it.accountId == id || it.toAccountId == id } }
    val list = if (tab == 0) touching.filter { it.kind != "transfer" } else touching.filter { it.kind == "transfer" }
    val balance = d.balanceOf(a)
    val series = remember(touching, balance, mode, range) {
        assetChartSeries(mode, ASSET_RANGE_DAYS[range], balance, touching) { effectOn(it, id) }
    }
    val (monthIn, monthOut) = remember(touching, id) {
        val (from, to) = periodRange("month")
        val effects = touching.filter { it.time in from until to }.map { effectOn(it, id) }
        effects.filter { it > 0 }.sum() to abs(effects.filter { it < 0 }.sum())
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 42.dp)
            Spacer(Modifier.weight(1f))
            CRoundButton(Icons.Rounded.MoreHoriz, "Pengaturan akun", { menu = true }, Modifier.anchor(menuAnchor), size = 42.dp, outlined = true)
        }
        Spacer(Modifier.height(14.dp))
        CTitle(a.name, subtitle = listOf(typeLabel(a), a.remark.trim()).filter { it.isNotEmpty() }.joinToString(" · "))
        Spacer(Modifier.height(14.dp))

        // balance
        CCard(K.Yellow, radius = 28.dp, padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).pressable { Prefs.hideAmounts = !hidden },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Saldo", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
                Spacer(Modifier.width(7.dp))
                Icon(
                    if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (hidden) "Tampilkan nominal" else "Sembunyikan nominal",
                    tint = K.Ink2, modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Money(balance, 44.sp, color = if (balance < 0) K.Danger else K.Green, hidden = hidden)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonthStat("Masuk bulan ini", amount(monthIn, hidden), K.Green, Modifier.weight(1f))
                Box(Modifier.padding(horizontal = 16.dp).width(1.5.dp).height(38.dp).background(K.YellowLine))
                MonthStat("Keluar bulan ini", amount(monthOut, hidden), K.Ink, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CChip("Ubah", icon = "pencil", border = true, height = 40.dp) { nav.go(R.accountEdit(id)) }
            CChip("Transfer", icon = "transfer", border = true, height = 40.dp) { vm.startAdd("transfer"); nav.go(R.add()) }
        }

        // chart
        Spacer(Modifier.height(15.dp))
        AssetChartCard(
            series, balance, mode, { mode = it }, range, { range = it },
            title = if (mode == 0) "Saldo akun" else null, chartHeight = 150.dp,
            bg = K.Card, chipBg = K.Sheet, padding = 18.dp,
        )

        // records
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Riwayat", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.weight(1f))
            MeowSegmented(listOf("Catatan", "Transfer"), tab, { tab = it }, fontSize = 14.sp)
        }
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) {
            Text(
                if (tab == 0) "Belum ada catatan. Pemasukan dan pengeluaran dari akun ini akan muncul di sini."
                else "Belum ada transfer. Transfer dari atau ke akun ini akan muncul di sini.",
                fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = K.Muted,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.Card).padding(16.dp),
            )
        } else {
            RecordTimeline(d, list, onOpen = { open = it }, withYear = true)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (menu) {
        PillMenu(menuAnchor.rect, { menu = false }) {
            PillOption("Ubah", Tone.PEACH, false, { menu = false; nav.go(R.accountEdit(id)) }, glyph = Icons.Rounded.Edit)
            PillOption("Hapus", Tone.PEACH, false, { menu = false; deleting = true }, glyph = Icons.Rounded.Delete)
        }
    }
    if (deleting) {
        val hasRecords = touching.isNotEmpty()
        Overlay({ deleting = false }) {
            FloatingCard {
                Text(if (hasRecords) "Tidak bisa dihapus" else "Hapus akun?", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                Spacer(Modifier.height(10.dp))
                Text(
                    if (hasRecords) "Akun ini masih punya catatan, jadi tidak bisa dihapus."
                    else "Akun \"${a.name}\" akan dihapus. Tindakan ini tidak bisa dibatalkan.",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2, lineHeight = 21.sp,
                )
                Spacer(Modifier.height(16.dp))
                if (hasRecords) {
                    RedButton("Mengerti", Modifier.fillMaxWidth()) { deleting = false }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CreamButton("Batal", Modifier.weight(1f)) { deleting = false }
                        DangerOutlineButton("Hapus", Modifier.weight(1f), filled = true) { deleting = false; vm.deleteAccount(a); nav.back() }
                    }
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

/** "Rp1.250.000" or dots when amounts are hidden. */
private fun amount(v: Long, hidden: Boolean): String = if (hidden) "Rp•••••" else v.rupiah()

/** "Masuk bulan ini / Rp1.250.000" column of the balance card. */
@Composable
private fun MonthStat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, maxLines = 1)
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
