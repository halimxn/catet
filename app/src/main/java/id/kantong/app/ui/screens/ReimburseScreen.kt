package id.kantong.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.Txn
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.MeowSegmented
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.RecordDetailCard
import id.kantong.app.ui.components.RecordTimeline
import id.kantong.app.ui.theme.K

/**
 * Reimburse: yellow summary (still waiting vs already paid back), a Menunggu / Sudah diganti segment,
 * then the records as a timeline. Opening a record shows the detail sheet where it can be marked as paid back.
 */
@Composable
fun ReimburseScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var open by remember { mutableStateOf<Txn?>(null) }
    val waiting = d.toReimburse
    val done = d.reimbursed
    val list = if (tab == 0) waiting else done

    Column(Modifier.fillMaxSize().background(K.Bg)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 46.dp)
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            CTitle("Reimburse", Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp), subtitle = "Pengeluaran yang akan diganti")
            Spacer(Modifier.height(14.dp))

            CCard(K.Yellow, Modifier.padding(horizontal = 20.dp), radius = 28.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Belum kembali", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
                        Spacer(Modifier.height(2.dp))
                        Money(waiting.sumOf { it.amount }, 38.sp)
                        Text("${waiting.size} catatan menunggu", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1)
                    }
                    Spacer(Modifier.width(12.dp))
                    CatIcon("reimburse", 52.dp)
                }
                Spacer(Modifier.height(14.dp))
                Spacer(Modifier.fillMaxWidth().height(1.5.dp).background(K.YellowLine))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sudah kembali", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted, maxLines = 1)
                        Text("${done.size} catatan", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted, maxLines = 1)
                    }
                    Money(done.sumOf { it.amount }, 20.sp, color = K.Ink)
                }
            }

            Spacer(Modifier.height(16.dp))
            MeowSegmented(listOf("Menunggu", "Sudah diganti"), tab, { tab = it }, Modifier.padding(horizontal = 20.dp).fillMaxWidth(), fill = true)
            Spacer(Modifier.height(16.dp))

            if (list.isEmpty()) {
                if (tab == 0) ReimburseEmpty("Belum ada reimburse", "Aktifkan Reimburse saat mencatat pengeluaran yang akan diganti kantor atau teman.")
                else ReimburseEmpty("Belum ada yang diganti", "Catatan yang sudah dibayar kembali akan pindah ke sini.")
            } else {
                if (tab == 0) {
                    Text(
                        "Ketuk catatan untuk menandainya sudah diganti.", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted,
                        modifier = Modifier.padding(start = 24.dp, end = 20.dp, bottom = 10.dp),
                    )
                }
                Column(Modifier.padding(horizontal = 20.dp)) {
                    RecordTimeline(d, list, onOpen = { open = it }, withYear = true)
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
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

/** Empty state on a cream card: two-tone sticker, bold title, muted hint (no mascot). */
@Composable
private fun ReimburseEmpty(title: String, text: String) {
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.Card).padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CatIcon("reimburse", 52.dp)
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink)
        Spacer(Modifier.height(4.dp))
        Text(text, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, color = K.Muted, textAlign = TextAlign.Center)
    }
}
