package id.kantong.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.ViewPeriod
import id.kantong.app.data.monthKey
import id.kantong.app.data.startOfMonth
import id.kantong.app.ui.theme.K
import java.util.Calendar

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

private fun monthStartOf(year: Int, month: Int): Long = Calendar.getInstance().apply {
    clear(); set(year, month, 1, 0, 0, 0)
}.timeInMillis

/**
 * "Pilih periode": year arrows and a 12-month grid (the chosen one green),
 * "Terapkan" applies it. By default it sets [ViewPeriod.monthStart] (0 when the current month is chosen).
 */
@Composable
fun PeriodSheet(
    onDismiss: () -> Unit,
    initial: Long = ViewPeriod.start(),
    onApply: (Long) -> Unit = { ViewPeriod.monthStart = if (monthKey(it) == monthKey()) 0L else it },
) {
    val init = Calendar.getInstance().apply { timeInMillis = initial }
    var year by remember { mutableIntStateOf(init.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(init.get(Calendar.MONTH)) }
    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader("Pilih periode", onDismiss, size = 21)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CRoundButton(Icons.Rounded.ChevronLeft, "Tahun sebelumnya", { year-- }, size = 34.dp)
                Spacer(Modifier.width(24.dp))
                Text("$year", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                Spacer(Modifier.width(24.dp))
                CRoundButton(Icons.Rounded.ChevronRight, "Tahun berikutnya", { year++ }, size = 34.dp)
            }
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                MONTHS.chunked(4).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        row.forEachIndexed { c, name ->
                            val m = r * 4 + c
                            val on = m == month
                            Box(
                                Modifier.weight(1f).height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (on) K.Green else K.Card)
                                    .border(if (on) 1.5.dp else 1.dp, if (on) K.Green else K.Line, RoundedCornerShape(14.dp))
                                    .pressable { month = m },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    name,
                                    fontSize = 15.5.sp,
                                    fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Bold,
                                    maxLines = 1,
                                    color = if (on) Color.White else K.Ink,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            RedButton("Terapkan", Modifier.fillMaxWidth()) {
                val chosen = monthStartOf(year, month)
                onApply(chosen)
                onDismiss()
            }
        }
    }
}
