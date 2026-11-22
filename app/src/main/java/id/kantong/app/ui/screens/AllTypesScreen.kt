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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import id.kantong.app.data.grouped
import id.kantong.app.data.monthName
import id.kantong.app.data.reportRange
import id.kantong.app.data.rupiah
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.Donut
import id.kantong.app.ui.components.Money
import id.kantong.app.ui.components.ProgressBar
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K

private fun typePct(p: Double): String = String.format(ID, "%.1f%%", p)

/** Total of one main category; [cat] is null for uncategorized records. */
private class TypeSlice(val cat: Category?, val amount: Long) {
    val name get() = cat?.name ?: "Tanpa kategori"
    val icon get() = cat?.icon ?: "other"
}

/**
 * All categories of one kind (R89):
 * Back button, big title, yellow total card, donut distribution card,
 * and a list of category progress rows.
 */
@Composable
fun AllTypesScreen(d: AppData, vm: AppViewModel, nav: Nav, kind: String) {
    val v = d.forLedger(Prefs.ledger)
    val now = System.currentTimeMillis()
    val (from, until) = reportRange("month", now)
    val slices = v.txns.filter { it.kind == kind && it.time >= from && it.time < until }
        .groupBy { v.rootOf(it.categoryId)?.id }
        .map { (id, list) -> TypeSlice(id?.let { v.category(it) }, list.sumOf { it.amount }) }
        .sortedByDescending { it.amount }
    val total = slices.sumOf { it.amount }
    val isIncome = kind == "in"
    val pageTitle = if (isIncome) "Semua pemasukan" else "Semua pengeluaran"

    Column(Modifier.fillMaxSize().background(K.Bg)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 44.dp)
        }

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        ) {
            CTitle(pageTitle, subtitle = "Rincian kategori ${monthName(now)}")
            Spacer(Modifier.height(16.dp))

            // Yellow Summary Card
            CCard(K.Yellow, radius = 28.dp, padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Total " + if (isIncome) "pemasukan" else "pengeluaran", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2)
                        Spacer(Modifier.height(4.dp))
                        Money(total, 36.sp, color = if (isIncome) K.Income else K.Ink)
                    }
                    Spacer(Modifier.width(12.dp))
                    CatIcon(if (isIncome) "salary" else "shopping", 50.dp)
                }
                Spacer(Modifier.height(14.dp))
                Spacer(Modifier.fillMaxWidth().height(1.5.dp).background(K.YellowLine))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Bulan", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        Spacer(Modifier.height(2.dp))
                        Text(monthName(now), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                    }
                    Box(Modifier.width(1.5.dp).height(36.dp).background(K.YellowLine))
                    Column(Modifier.weight(1f).padding(start = 20.dp)) {
                        Text("Kategori aktif", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        Spacer(Modifier.height(2.dp))
                        Text("${slices.size} kategori", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Green)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (slices.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(K.Card)
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CatIcon("other", 52.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada catatan", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Catatan ${if (isIncome) "pemasukan" else "pengeluaran"} bulan ini akan muncul di sini.",
                        fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, color = K.Muted, textAlign = TextAlign.Center,
                    )
                }
            } else {
                // Donut Chart Card
                CCard(K.Card, radius = 24.dp, padding = PaddingValues(16.dp)) {
                    Text("Distribusi kategori", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                    Spacer(Modifier.height(14.dp))
                    // fixed category colors: up to five slices, the 5th slot folds the rest into "Lainnya"
                    val shown = if (slices.size > 5) slices.take(4) else slices
                    val rest = if (slices.size > 5) slices.drop(4).sumOf { it.amount } else 0L
                    val rows = shown.mapIndexed { i, s -> Triple(s.name, s.amount, K.ChartCats[i]) } +
                        (if (rest > 0) listOf(Triple("Lainnya", rest, K.ChartCats[4])) else emptyList())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Donut(rows.map { it.second.toFloat() to it.third }, size = 114.dp, stroke = 20.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rows.forEach { (name, amount, color) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
                                    Spacer(Modifier.width(6.dp))
                                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    val share = if (total > 0) amount.toDouble() / total else 0.0
                                    Text(typePct(share * 100), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = K.Muted)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Daftar kategori", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                Spacer(Modifier.height(10.dp))

                // Category Rows Card
                CCard(K.Card, radius = 24.dp, padding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        slices.forEachIndexed { idx, s ->
                            val share = if (total > 0) s.amount.toDouble() / total else 0.0
                            val cat = s.cat
                            val click: (() -> Unit)? = cat?.let { { vm.openHistoryFor(it.id); nav.go(R.SEARCH) } }

                            Row(
                                Modifier.fillMaxWidth().let { if (click != null) it.pressable(click) else it },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(K.Tile),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CatIcon(s.icon, 26.dp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(s.name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(s.amount.grouped(), fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = if (isIncome) K.Income else K.Ink)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ProgressBar(share.toFloat(), if (isIncome) K.ChartIn else K.Green, Modifier.weight(1f), track = K.Field, height = 6.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(typePct(share * 100), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = K.Muted)
                                    }
                                }
                            }
                            if (idx < slices.lastIndex) {
                                Spacer(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
                            }
                        }
                    }
                }
            }
        }
    }
}
