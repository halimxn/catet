package id.kantong.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.ui.theme.K
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

private val ROW = 50.dp

/** Index of the row sitting in the middle band of a wheel. */
private fun LazyListState.centered(rowPx: Float): Int =
    firstVisibleItemIndex + if (firstVisibleItemScrollOffset > rowPx / 2) 1 else 0

/**
 * "Pilih jam" (R89): hour and minute scroll wheels in a sage card with a white middle band,
 * quick-minute chips (:00 :15 :30 :45), Sekarang / −1 jam / +1 jam, then Batal / "Pilih HH.mm".
 */
@Composable
fun TimePickerCard(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val hours = rememberLazyListState(initialHour.coerceIn(0, 23))
    val minutes = rememberLazyListState(initialMinute.coerceIn(0, 59))
    val rowPx = with(LocalDensity.current) { ROW.toPx() }
    val hour by remember { derivedStateOf { hours.centered(rowPx).coerceIn(0, 23) } }
    val minute by remember { derivedStateOf { minutes.centered(rowPx).coerceIn(0, 59) } }
    val scope = rememberCoroutineScope()
    fun go(h: Int, m: Int) = scope.launch {
        launch { hours.animateScrollToItem(h) }
        launch { minutes.animateScrollToItem(m) }
    }

    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader("Pilih jam", onDismiss, size = 21)
            Text("Gulir angka, atau pakai pilihan cepat", fontSize = 13.sp, color = K.Muted)

            androidx.compose.foundation.layout.Column(
                Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(26.dp)).background(K.SageCard).padding(12.dp),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    WheelLabel("Jam", Modifier.weight(1f)); Spacer(Modifier.width(18.dp)); WheelLabel("Menit", Modifier.weight(1f))
                }
                Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(ROW * 3)) {
                    Box(Modifier.fillMaxWidth().padding(top = ROW).height(ROW).clip(RoundedCornerShape(18.dp)).background(K.Tile))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Wheel(hours, 24, hour, Modifier.weight(1f))
                        Text(":", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = K.Green, modifier = Modifier.width(18.dp), textAlign = TextAlign.Center)
                        Wheel(minutes, 60, minute, Modifier.weight(1f))
                    }
                }
                Text("Menit cepat", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink2, modifier = Modifier.padding(start = 2.dp, top = 12.dp, bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 15, 30, 45).forEach { m ->
                        TimeChip(":%02d".format(m), Modifier.weight(1f), if (minute == m) K.Green else K.Tile, if (minute == m) Color.White else K.Ink) { go(hour, m) }
                    }
                }
            }

            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TimeChip("Sekarang", Modifier.weight(1f), K.Yellow, K.Ink) {
                    val c = Calendar.getInstance(); go(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
                }
                TimeChip("−1 jam", Modifier.weight(1f), K.Field, K.Ink) { go((hour + 23) % 24, minute) }
                TimeChip("+1 jam", Modifier.weight(1f), K.Field, K.Ink) { go((hour + 1) % 24, minute) }
            }

            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CreamButton("Batal", Modifier.weight(1f), onClick = onDismiss)
                RedButton("Pilih %02d.%02d".format(hour, minute), Modifier.weight(1f)) {
                    onConfirm(hour, minute)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun WheelLabel(text: String, modifier: Modifier) =
    Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink2, textAlign = TextAlign.Center, modifier = modifier)

@Composable
private fun Wheel(state: LazyListState, count: Int, selected: Int, modifier: Modifier) {
    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        contentPadding = PaddingValues(vertical = ROW),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(ROW * 3),
    ) {
        items(count) { i ->
            val d = abs(i - selected)
            Box(Modifier.height(ROW).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "%02d".format(i),
                    fontSize = when (d) { 0 -> 32.sp; 1 -> 22.sp; else -> 18.sp },
                    fontWeight = FontWeight.ExtraBold,
                    color = when (d) { 0 -> K.Green; 1 -> K.Green.copy(alpha = 0.55f); else -> K.SageMid },
                )
            }
        }
    }
}

@Composable
private fun TimeChip(label: String, modifier: Modifier, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(modifier.height(38.dp).clip(RoundedCornerShape(13.dp)).background(bg).pressable(onClick), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = fg, maxLines = 1)
    }
}
