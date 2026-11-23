package id.kantong.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import id.kantong.app.MainActivity
import id.kantong.app.data.AppData
import id.kantong.app.data.Category
import id.kantong.app.data.Db
import id.kantong.app.data.Prefs
import id.kantong.app.data.daysInMonth
import id.kantong.app.data.dayOfMonth
import id.kantong.app.data.grouped
import id.kantong.app.data.short
import id.kantong.app.ui.components.illusBitmap
import id.kantong.app.ui.theme.K

/** Snapshot the widgets draw from (read straight from the database). */
private class WData(
    val spentToday: Long, val leftToday: Long, val budgetTotal: Long, val spentMonth: Long, val daysLeft: Int,
    val warnings: List<Pair<String, Int>>, // name, percent used (>100 = over)
    val favorites: List<Category>, val hidden: Boolean, val month: String,
)

private suspend fun load(ctx: Context): WData {
    Prefs.init(ctx)
    val dao = Db.get(ctx).dao()
    val txns = dao.allTransactions()
    val all = AppData(
        accounts = dao.allAccounts(), categories = dao.allCategories(), txns = txns, goals = dao.allGoals(), bills = dao.allBills(),
        loaded = true, ledgers = dao.allLedgers(), budgets = dao.allBudgets(), allTxns = txns,
    )
    val v = all.forLedger(Prefs.ledger)
    val since = System.currentTimeMillis() - 30L * 86_400_000L
    val counts = v.txns.filter { it.kind == "out" && it.time >= since }.groupingBy { v.rootOf(it.categoryId)?.id }.eachCount()
    val tops = v.topCategories("out").sortedBy { it.sortOrder }
    val favorites = (tops.sortedByDescending { counts[it.id] ?: 0 }).take(4)
    val warnings = v.budgets.filter { it.period == "month" }.mapNotNull { b ->
        val lim = v.limitOf(b); val s = v.spentFor(b)
        if (lim <= 0 || s * 100 < lim * 80) null else shortName(b.name) to (s * 100 / lim).toInt()
    }.sortedByDescending { it.second }.take(2)
    return WData(
        v.spentToday, (v.safePerDay - v.spentToday).coerceAtLeast(0), v.budgetTotal, v.spentMonth,
        (daysInMonth() - dayOfMonth() + 1).coerceAtLeast(1), warnings, favorites, Prefs.hideAmounts,
        java.text.SimpleDateFormat("MMMM", java.util.Locale("in", "ID")).format(java.util.Date()),
    )
}

private fun c(color: Color) = ColorProvider(color)
/** Compact rupiah for widgets: 544rb, 4,4jt (no long decimals). */
private fun money(v: Long, hidden: Boolean): String {
    if (hidden) return "•••"
    val a = kotlin.math.abs(v)
    return when {
        a >= 1_000_000 -> String.format(java.util.Locale("in", "ID"), "%.1f", a / 1_000_000.0).removeSuffix(",0") + "jt"
        a >= 1_000 -> "${a / 1_000}rb"
        else -> a.toString()
    }
}
/** One short word for a tile label: "Makan & minum" -> "Makan". */
private fun shortName(n: String) = n.split(" ", "&", "/").firstOrNull { it.isNotBlank() }?.take(9) ?: n.take(9)

private fun openApp(ctx: Context, action: String, category: Long = 0) = Intent(ctx, MainActivity::class.java)
    .setData(Uri.parse("catet://$action/$category"))
    .putExtra("catet_action", action).putExtra("category", category)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

/** "Catat" icon: yellow notebook with a gold plus badge, light outline for the green tile. */
private fun addIcon(px: Int): Bitmap {
    val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val cv = Canvas(b); val k = px / 48f
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.8f * k; color = 0xFFFDFCF6.toInt(); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    val note = RectF(10 * k, 7 * k, 34 * k, 39 * k)
    fill.color = 0xFFFAEAC0.toInt(); cv.drawRoundRect(note, 5 * k, 5 * k, fill); cv.drawRoundRect(note, 5 * k, 5 * k, stroke)
    val ink = Paint(stroke).apply { color = 0xFF2E5242.toInt() }
    cv.drawLine(16 * k, 16 * k, 28 * k, 16 * k, ink); cv.drawLine(16 * k, 23 * k, 24 * k, 23 * k, ink)
    fill.color = 0xFFE8B860.toInt(); cv.drawCircle(34 * k, 34 * k, 8 * k, fill); cv.drawCircle(34 * k, 34 * k, 8 * k, stroke)
    cv.drawLine(34 * k, 30 * k, 34 * k, 38 * k, ink); cv.drawLine(30 * k, 34 * k, 38 * k, 34 * k, ink)
    return b
}

private fun logo(px: Int): Bitmap {
    val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val cv = Canvas(b); val k = px / 26f
    val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5 * k; color = 0xFF2E5242.toInt(); strokeCap = Paint.Cap.ROUND }
    cv.drawArc(RectF(4 * k, 4 * k, 22 * k, 22 * k), -40f, -280f, false, arc)
    val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE8B860.toInt() }
    cv.drawCircle(19.5f * k, 13 * k, 3.4f * k, dot)
    return b
}

@Composable
private fun Tile(ctx: Context, label: String, icon: Bitmap, bg: Color, labelColor: Color, intent: Intent, modifier: GlanceModifier) {
    Column(modifier.clickable(actionStartActivity(intent)), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(GlanceModifier.fillMaxWidth().height(38.dp).cornerRadius(13.dp).background(bg), contentAlignment = Alignment.Center) {
            Image(ImageProvider(icon), label, GlanceModifier.size(24.dp))
        }
        Spacer(GlanceModifier.height(2.dp))
        Text(label, maxLines = 1, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c(labelColor)))
    }
}

@Composable
private fun QuickRow(ctx: Context, d: WData, count: Int) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        d.favorites.take(count).forEach { cat ->
            Tile(ctx, shortName(cat.name), illusBitmap(cat.icon, 96), K.Tile, K.Ink2, openApp(ctx, "add", cat.id), GlanceModifier.defaultWeight().padding(horizontal = 2.dp))
        }
        Tile(ctx, "Catat", addIcon(96), K.Green, K.Green, openApp(ctx, "add"), GlanceModifier.defaultWeight().padding(horizontal = 2.dp))
    }
}

/** Catat cepat 4×1: today's spending + favourite categories + Catat. */
class QuickWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = load(context)
        provideContent {
            Row(
                GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(K.Yellow).padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                    .clickable(actionStartActivity(openApp(context, "home"))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(GlanceModifier.width(80.dp)) {
                    Text("Hari ini", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c(K.OnYellow)))
                    Text("−Rp" + money(d.spentToday, d.hidden), maxLines = 1, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c(K.Ink)))
                    Text("sisa ${money(d.leftToday, d.hidden)}", maxLines = 1, style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = c(K.ChartIn)))
                }
                Box(GlanceModifier.defaultWeight()) { QuickRow(context, d, 3) }
            }
        }
    }
}

/** Anggaran + Catat 4×2: remaining budget, progress, warning chips, then the quick row. */
class BudgetWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = load(context)
        provideContent {
            Column(GlanceModifier.fillMaxSize().cornerRadius(26.dp).background(K.SageCard).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(GlanceModifier.fillMaxWidth().clickable(actionStartActivity(openApp(context, "budget"))), verticalAlignment = Alignment.CenterVertically) {
                    Image(ImageProvider(logo(64)), "catet", GlanceModifier.size(16.dp))
                    Spacer(GlanceModifier.width(6.dp))
                    Text("Anggaran ${d.month}", style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = c(K.OnSage)), modifier = GlanceModifier.defaultWeight())
                    Box(GlanceModifier.cornerRadius(10.dp).background(K.Tile).padding(horizontal = 9.dp, vertical = 3.dp)) {
                        Text("sisa ${d.daysLeft} hari", style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = c(K.Green)))
                    }
                }
                if (d.budgetTotal <= 0) {
                    Text("Belum ada anggaran bulanan. Ketuk untuk mengatur.", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c(K.OnSage)),
                        modifier = GlanceModifier.padding(vertical = 10.dp).clickable(actionStartActivity(openApp(context, "budget"))))
                } else {
                    val left = (d.budgetTotal - d.spentMonth)
                    val pct = (d.spentMonth.toFloat() / d.budgetTotal).coerceIn(0f, 1f)
                    Row(GlanceModifier.fillMaxWidth().padding(top = 4.dp).clickable(actionStartActivity(openApp(context, "budget"))), verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (left >= 0) "Rp" + (if (d.hidden) "•••" else left.grouped()) else "Lewat Rp" + money(-left, d.hidden),
                            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c(if (left >= 0) K.Green else K.Danger)), modifier = GlanceModifier.defaultWeight(),
                        )
                        Text("${Math.round(pct * 100)}%", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c(K.Green)))
                    }
                    LinearProgressIndicator(pct, GlanceModifier.fillMaxWidth().height(8.dp).padding(top = 4.dp), color = c(if (pct >= 1f) K.Danger else K.Green), backgroundColor = c(K.Tile))
                    if (d.warnings.isNotEmpty()) Row(GlanceModifier.padding(top = 5.dp)) {
                        d.warnings.forEach { (name, pct) ->
                            val over = pct > 100
                            Box(GlanceModifier.cornerRadius(10.dp).background(if (over) K.Pink else K.Yellow).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text(if (over) "$name lewat" else "$name $pct%", maxLines = 1, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c(if (over) K.Danger else K.OnYellow)))
                            }
                            Spacer(GlanceModifier.width(6.dp))
                        }
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                QuickRow(context, d, 4)
            }
        }
    }
}

class QuickWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = QuickWidget() }
class BudgetWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = BudgetWidget() }

/** Redraws both widgets (called after data changes). */
object CatetWidgets {
    suspend fun refresh(ctx: Context) {
        runCatching { QuickWidget().updateAll(ctx) }
        runCatching { BudgetWidget().updateAll(ctx) }
    }
}
