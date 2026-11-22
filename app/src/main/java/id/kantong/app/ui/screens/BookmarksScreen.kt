package id.kantong.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.RecordDetailCard
import id.kantong.app.ui.components.RecordTimeline
import id.kantong.app.ui.theme.K

/**
 * Bookmark: yellow summary (how many records, their Keluar | Masuk totals), then the starred records as a
 * timeline. Opening a record shows the detail sheet where the star can be removed.
 */
@Composable
fun BookmarksScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    var open by remember { mutableStateOf<Txn?>(null) }
    val list = d.starred
    val out = list.filter { it.kind == "out" }.sumOf { it.amount }
    val inn = list.filter { it.kind == "in" }.sumOf { it.amount }

    Column(Modifier.fillMaxSize().background(K.Bg)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 46.dp)
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            CTitle("Bookmark", Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp), subtitle = "Catatan yang kamu tandai")
            Spacer(Modifier.height(14.dp))

            CCard(K.Yellow, Modifier.padding(horizontal = 20.dp), radius = 28.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tersimpan", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            buildAnnotatedString {
                                append("${list.size}")
                                withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)) { append(" catatan") }
                            },
                            fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = K.Green, maxLines = 1,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    CatIcon("bookmark", 52.dp)
                }
                Spacer(Modifier.height(14.dp))
                Spacer(Modifier.fillMaxWidth().height(1.5.dp).background(K.YellowLine))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Keluar", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        Money(out, 20.sp, color = K.Ink)
                    }
                    Box(Modifier.width(1.5.dp).height(36.dp).background(K.YellowLine))
                    Column(Modifier.weight(1f).padding(start = 20.dp)) {
                        Text("Masuk", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        Money(inn, 20.sp, color = K.Income)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            if (list.isEmpty()) {
                Column(
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.Card)
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CatIcon("bookmark", 52.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("Belum ada bookmark", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ketuk ikon bintang di detail catatan untuk menyimpannya di sini.", fontSize = 13.sp, lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium, color = K.Muted, textAlign = TextAlign.Center,
                    )
                }
            } else {
                Text(
                    "Ketuk catatan lalu bintang untuk melepas bookmark.", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted,
                    modifier = Modifier.padding(start = 24.dp, end = 20.dp, bottom = 10.dp),
                )
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
