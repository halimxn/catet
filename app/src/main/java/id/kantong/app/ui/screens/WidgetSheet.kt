package id.kantong.app.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.R
import id.kantong.app.ui.components.CatetSheet
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.SheetHeader
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K
import id.kantong.app.widget.BudgetWidgetReceiver
import id.kantong.app.widget.CatatWidgetReceiver
import id.kantong.app.widget.DailyWidgetReceiver
import id.kantong.app.widget.MonthWidgetReceiver
import id.kantong.app.widget.QuickWidgetReceiver
import id.kantong.app.widget.StreakWidgetReceiver
import id.kantong.app.widget.WaitWidgetReceiver

private class WidgetOpt(val name: String, val size: String, val preview: Int, val receiver: Class<*>)

private val widgetOpts = listOf(
    WidgetOpt("Sisa bulan ini", "2×2", R.drawable.widget_month_preview, MonthWidgetReceiver::class.java),
    WidgetOpt("Jatah harian", "2×2", R.drawable.widget_daily_preview, DailyWidgetReceiver::class.java),
    WidgetOpt("Streak", "2×2", R.drawable.widget_streak_preview, StreakWidgetReceiver::class.java),
    WidgetOpt("Catat", "2×1", R.drawable.widget_catat_preview, CatatWidgetReceiver::class.java),
    WidgetOpt("Menunggu dicatat", "2×1", R.drawable.widget_wait_preview, WaitWidgetReceiver::class.java),
    WidgetOpt("Catat cepat", "4×1", R.drawable.widget_quick_preview, QuickWidgetReceiver::class.java),
    WidgetOpt("Anggaran & catat", "4×2", R.drawable.widget_budget_preview, BudgetWidgetReceiver::class.java),
)

/** Every home-screen widget with a Pasang button (Android then asks where to place it). */
@Composable
fun WidgetSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val mgr = AppWidgetManager.getInstance(ctx)
    Overlay(onDismiss) {
        CatetSheet {
            SheetHeader("Widget layar utama", onDismiss, size = 21)
            Text("Mengikuti mode terang/gelap HP", fontSize = 13.sp, color = K.Muted)
            Column(
                Modifier.padding(top = 12.dp).heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                widgetOpts.forEach { w ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(K.Card).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(K.Field).padding(6.dp), contentAlignment = Alignment.Center) {
                            Image(painterResource(w.preview), null, contentScale = ContentScale.Fit)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.name, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                            Text(w.size, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = K.Muted)
                        }
                        Box(
                            Modifier.height(36.dp).clip(RoundedCornerShape(12.dp)).background(K.Green)
                                .pressable { if (mgr.isRequestPinAppWidgetSupported) mgr.requestPinAppWidget(ComponentName(ctx, w.receiver), null, null) }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Pasang", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = K.OnGreen) }
                    }
                }
            }
        }
    }
}
