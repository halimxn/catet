package id.kantong.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.ViewPeriod
import id.kantong.app.data.monthKey
import id.kantong.app.data.short
import id.kantong.app.ui.theme.K
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.absoluteValue

private val MONTHS = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
private val CARD_TONES = listOf(K.SageCard, K.Pink, K.Card, K.Yellow) // one per quarter

private fun monthStartOf(year: Int, month: Int): Long = Calendar.getInstance().apply {
    clear(); set(year, month, 1, 0, 0, 0)
}.timeInMillis

private class MonthSum(val income: Long, val spent: Long, val daily: List<Long>)

private fun monthSum(d: AppData?, year: Int, month: Int): MonthSum {
    if (d == null) return MonthSum(0, 0, emptyList())
    val from = monthStartOf(year, month)
    val to = Calendar.getInstance().apply { timeInMillis = from; add(Calendar.MONTH, 1) }.timeInMillis
    val tx = d.txns.filter { it.time in from until to }
    val days = Calendar.getInstance().apply { timeInMillis = from }.getActualMaximum(Calendar.DAY_OF_MONTH)
    val daily = LongArray(days)
    tx.filter { it.kind == "out" }.forEach {
        val day = Calendar.getInstance().apply { timeInMillis = it.time }.get(Calendar.DAY_OF_MONTH)
        daily[day - 1] += it.amount
    }
    return MonthSum(tx.filter { it.kind == "in" }.sumOf { it.amount }, tx.filter { it.kind == "out" }.sumOf { it.amount }, daily.toList())
}

/**
 * "Pilih periode" (R89): big pastel month cards you swipe through (neighbours peek in at the sides),
 * a year stepper, a 2×6 month strip for quick jumps and a green "Lihat <bulan>" button.
 * Each card shows Masuk / Keluar and a mini bar chart of daily spending (needs [d]).
 * By default it sets [ViewPeriod.monthStart] (0 when the current month is chosen).
 */
@Composable
fun PeriodSheet(
    onDismiss: () -> Unit,
    initial: Long = ViewPeriod.start(),
    d: AppData? = null,
    onApply: (Long) -> Unit = { ViewPeriod.monthStart = if (monthKey(it) == monthKey()) 0L else it },
) {
    val init = Calendar.getInstance().apply { timeInMillis = initial }
    val now = Calendar.getInstance()
    val nowYear = now.get(Calendar.YEAR)
    val nowMonth = now.get(Calendar.MONTH)
    var year by remember { mutableIntStateOf(init.get(Calendar.YEAR)) }
    val pager = rememberPagerState(initialPage = init.get(Calendar.MONTH)) { 12 }
    val scope = rememberCoroutineScope()
    val sums = remember(year, d) { (0..11).map { monthSum(d, year, it) } }
    fun future(m: Int) = year > nowYear || (year == nowYear && m > nowMonth)

    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader("Pilih periode", onDismiss, size = 21)
            Text("Geser kartu atau ketuk bulan", fontSize = 13.sp, color = K.Muted)

            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = 40.dp),
                modifier = Modifier.fillMaxWidth().height(286.dp).padding(top = 14.dp),
            ) { m ->
                val off = ((pager.currentPage - m) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                val fut = future(m)
                MonthCard(
                    month = m, year = year, sum = sums[m], future = fut,
                    label = when {
                        year == nowYear && m == nowMonth -> "Bulan ini"
                        fut -> "Belum tiba"
                        (year == nowYear && m == nowMonth - 1) || (year == nowYear - 1 && nowMonth == 0 && m == 11) -> "Bulan lalu"
                        else -> null
                    },
                    modifier = Modifier.graphicsLayer {
                        val s = 1f - 0.14f * off
                        scaleX = s; scaleY = s
                        alpha = (if (fut) 0.55f else 1f) * (1f - 0.3f * off)
                    }.pressable { scope.launch { pager.animateScrollToPage(m) } },
                )
            }

            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                CRoundButton(Icons.Rounded.ChevronLeft, "Tahun sebelumnya", { year-- }, size = 36.dp)
                Text("$year", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                CRoundButton(Icons.Rounded.ChevronRight, "Tahun berikutnya", { year++ }, size = 36.dp)
            }

            // 2×6 month strip for quick jumps.
            Column(
                Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(22.dp)).background(K.Card).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (0..11).chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { m ->
                            val on = m == pager.currentPage
                            Box(
                                Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(13.dp))
                                    .background(if (on) K.Green else K.Tile)
                                    .graphicsLayer { alpha = if (future(m) && !on) 0.45f else 1f }
                                    .pressable { scope.launch { pager.animateScrollToPage(m) } },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(MONTHS[m].take(3), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Color.White else K.Ink2, maxLines = 1)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            val m = pager.currentPage
            val fut = future(m)
            RedButton(if (fut) "Bulan ini belum tiba" else "Lihat ${MONTHS[m]}", Modifier.fillMaxWidth(), enabled = !fut) {
                onApply(monthStartOf(year, m))
                onDismiss()
            }
        }
    }
}

@Composable
private fun MonthCard(month: Int, year: Int, sum: MonthSum, future: Boolean, label: String?, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().clip(RoundedCornerShape(30.dp)).background(CARD_TONES[month / 3])
            .border(1.5.dp, Color(0x142F2F2F), RoundedCornerShape(30.dp)).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(MONTHS[month].take(3), fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, lineHeight = 44.sp, letterSpacing = (-1).sp)
                Text("${MONTHS[month]} $year", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Ink2, modifier = Modifier.padding(top = 2.dp))
            }
            Box(Modifier.size(58.dp).clip(CircleShape).background(K.Tile), contentAlignment = Alignment.Center) { CatIcon("history", 40.dp) }
        }
        Box(Modifier.height(30.dp).padding(top = 6.dp)) {
            if (label != null) {
                Text(
                    label, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = if (future) K.Muted else K.Green,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xB3FFFFFF)).padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SumBox("Masuk", "+" + sum.income.short(), K.Income, Modifier.weight(1f))
            SumBox("Keluar", "−" + sum.spent.short(), K.Ink, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        val peak = sum.daily.maxOrNull() ?: 0L
        if (peak <= 0L) {
            Text(if (future) "Belum ada catatan" else "Tidak ada pengeluaran", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = K.Muted)
        } else {
            Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                sum.daily.forEach { v ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight((v.toFloat() / peak).coerceAtLeast(0.08f))
                            .clip(RoundedCornerShape(2.dp)).background(if (v == peak) K.Green else K.Green.copy(alpha = 0.26f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SumBox(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color(0x9EFFFFFF)).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = K.Muted)
        Text(value.replace(" ", ""), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1)
    }
}
