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
import id.kantong.app.data.monthKey
import id.kantong.app.data.startOfDay
import id.kantong.app.ui.components.illusBitmap

/** Snapshot the widgets draw from (read straight from the database). */
private class WData(
    val spentToday: Long, val leftToday: Long, val budgetTotal: Long, val spentMonth: Long, val daysLeft: Int,
    val warnings: List<Pair<String, Int>>, // name, percent used (>100 = over)
    val favorites: List<Category>, val hidden: Boolean, val month: String,
    val leftMonth: Long, val perDay: Long, val lastDays: List<Pair<String, Long>>, // weekday letter, spent (oldest first, today last)
    val streak: Int, val recordedToday: Boolean, val recentDays: List<Boolean>, val waiting: Int,
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
    val day = 86_400_000L
    val today = startOfDay()
    val letters = listOf("M", "S", "S", "R", "K", "J", "S") // Minggu..Sabtu
    // start of the day `back` days ago (the +1h keeps it safe around clock changes)
    fun dayStart(back: Int) = if (back == 0) today else startOfDay(today - back * day + 3_600_000L)
    val lastDays = (4 downTo 0).map { back ->
        val start = dayStart(back)
        val end = if (back == 0) today + day else dayStart(back - 1)
        val dow = java.util.Calendar.getInstance().apply { timeInMillis = start }.get(java.util.Calendar.DAY_OF_WEEK) - 1
        letters[dow] to v.txns.filter { it.kind == "out" && it.time >= start && it.time < end }.sumOf { it.amount }
    }
    val s = id.kantong.app.ui.screens.streakOf(all)
    val recent = (4 downTo 0).map { back -> dayStart(back) in s.days }
    val dueBills = all.bills.count { it.paidMonthKey != monthKey() && it.dueDay <= dayOfMonth() + 3 }
    return WData(
        v.spentToday, (v.safePerDay - v.spentToday).coerceAtLeast(0), v.budgetTotal, v.spentMonth,
        (daysInMonth() - dayOfMonth() + 1).coerceAtLeast(1), warnings, favorites, Prefs.hideAmounts,
        java.text.SimpleDateFormat("MMMM", java.util.Locale("in", "ID")).format(java.util.Date()),
        v.incomeMonth - v.spentMonth, v.safePerDay, lastDays, s.current, s.today, recent,
        dao.pendingDetectedCount() + dueBills,
    )
}

private fun c(p: ColorProvider) = p
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
private fun Tile(ctx: Context, label: String, icon: Bitmap, bg: ColorProvider, labelColor: ColorProvider, intent: Intent, modifier: GlanceModifier) {
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

/** White glyphs, tinted per theme with ColorFilter (plus, catet arc, flame, calendar). */
private fun glyph(kind: String, px: Int): Bitmap {
    val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val cv = Canvas(b); val k = px / 24f
    val st = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.6f * k; color = 0xFFFFFFFF.toInt(); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    when (kind) {
        "plus" -> { cv.drawLine(12 * k, 5 * k, 12 * k, 19 * k, st); cv.drawLine(5 * k, 12 * k, 19 * k, 12 * k, st) }
        "logo" -> { st.strokeWidth = 3.4f * k; cv.drawArc(RectF(4 * k, 4 * k, 20 * k, 20 * k), -40f, -280f, false, st); cv.drawCircle(17.6f * k, 11.6f * k, 2.6f * k, fill) }
        "calendar" -> {
            cv.drawRoundRect(RectF(4 * k, 6 * k, 20 * k, 20 * k), 3 * k, 3 * k, st); cv.drawLine(4 * k, 10.5f * k, 20 * k, 10.5f * k, st)
            cv.drawLine(9 * k, 4 * k, 9 * k, 7 * k, st); cv.drawLine(15 * k, 4 * k, 15 * k, 7 * k, st)
        }
        else -> { // flame
            val path = android.graphics.Path().apply {
                moveTo(12 * k, 3 * k)
                cubicTo(13 * k, 7 * k, 18.5f * k, 9.5f * k, 18.5f * k, 14.5f * k)
                cubicTo(18.5f * k, 18.5f * k, 15.5f * k, 21 * k, 12 * k, 21 * k)
                cubicTo(8.5f * k, 21 * k, 5.5f * k, 18.5f * k, 5.5f * k, 14.8f * k)
                cubicTo(5.5f * k, 11.8f * k, 7.5f * k, 10 * k, 8.6f * k, 8.4f * k)
                cubicTo(9 * k, 10.5f * k, 10 * k, 11.4f * k, 11 * k, 11.6f * k)
                cubicTo(10.6f * k, 8.5f * k, 11 * k, 5.5f * k, 12 * k, 3 * k)
                close()
            }
            cv.drawPath(path, fill)
        }
    }
    return b
}

@Composable
private fun Glyph(kind: String, tint: ColorProvider, size: androidx.compose.ui.unit.Dp) =
    Image(ImageProvider(glyph(kind, 72)), null, GlanceModifier.size(size), colorFilter = androidx.glance.ColorFilter.tint(tint))

/** Widget header: small rounded square with a glyph, then the label. */
@Composable
private fun Head(glyphKind: String, label: String, squareBg: ColorProvider, glyphTint: ColorProvider, labelColor: ColorProvider) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.size(18.dp).cornerRadius(6.dp).background(squareBg), contentAlignment = Alignment.Center) { Glyph(glyphKind, glyphTint, 12.dp) }
        Spacer(GlanceModifier.width(6.dp))
        Text(label, maxLines = 1, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = labelColor))
    }
}

/** 2×2 layout shared by every square widget: header, value, one line, then a 32dp bottom zone. */
@Composable
private fun Square(
    bg: ColorProvider, open: Intent, head: @Composable () -> Unit, value: String, valueColor: ColorProvider,
    line: String, lineColor: ColorProvider, zone: @Composable androidx.glance.layout.RowScope.() -> Unit,
) {
    Column(GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(bg).padding(14.dp).clickable(actionStartActivity(open))) {
        head()
        Spacer(GlanceModifier.height(10.dp))
        Text(value, maxLines = 1, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor))
        Text(line, maxLines = 1, style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = lineColor))
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) { zone() }
    }
}

/** One box in a bottom-zone strip of five. */
@Composable
private fun androidx.glance.layout.RowScope.DayBox(bg: ColorProvider, content: @Composable () -> Unit = {}) {
    Box(GlanceModifier.defaultWeight().height(32.dp).padding(horizontal = 2.dp)) {
        Box(GlanceModifier.fillMaxSize().cornerRadius(10.dp).background(bg), contentAlignment = Alignment.Center) { content() }
    }
}

/** 2×2 Sisa bulan ini: money left this month, budget used, Catat. */
class MonthWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = load(context)
        provideContent {
            val pct = if (d.budgetTotal > 0) (d.spentMonth * 100 / d.budgetTotal).toInt() else -1
            Square(
                K.Yellow, openApp(context, "home"),
                { Head("logo", "Sisa bulan ini", K.Green, K.OnGreen, K.OnYellow) },
                (if (d.leftMonth < 0) "−" else "") + "Rp" + money(d.leftMonth, d.hidden), if (d.leftMonth < 0) K.Danger else K.Green,
                if (pct >= 0) "$pct% anggaran terpakai" else "Keluar Rp" + money(d.spentMonth, d.hidden), K.OnYellow,
            ) {
                Row(
                    GlanceModifier.fillMaxWidth().height(32.dp).cornerRadius(12.dp).background(K.Green).clickable(actionStartActivity(openApp(context, "add"))),
                    verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Glyph("plus", K.OnGreen, 13.dp)
                    Spacer(GlanceModifier.width(4.dp))
                    Text("Catat", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = K.OnGreen))
                }
            }
        }
    }
}

/** 2×2 Jatah harian: allowance per day and the last five days (green = within, orange = over). */
class DailyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = load(context)
        provideContent {
            val has = d.budgetTotal > 0
            Square(
                K.SageCard, openApp(context, if (has) "home" else "budget"),
                { Head("calendar", "Jatah harian", K.Green, K.OnGreen, K.OnSage) },
                if (has) "Rp" + money(d.perDay.coerceAtLeast(0), d.hidden) else "Belum ada", K.Ink,
                if (has) "per hari · ${d.daysLeft} hari lagi" else "Ketuk untuk atur anggaran", K.OnSage,
            ) {
                d.lastDays.forEachIndexed { i, (letter, spent) ->
                    val today = i == d.lastDays.lastIndex
                    val over = has && spent > d.perDay
                    val bg = when { today || !has -> K.Tile; over -> K.ChartOut; else -> K.Green }
                    DayBox(bg) { Text(letter, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (today || !has) K.OnSage else K.OnGreen)) }
                }
            }
        }
    }
}

/** 2×2 Streak: days in a row with a record, and the last five days. */
class StreakWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = load(context)
        provideContent {
            Square(
                K.Card, openApp(context, "achieve"),
                { Head("flame", "Streak", K.Yellow, K.ChartOut, K.Muted) },
                "${d.streak} hari", K.Ink,
                if (d.recordedToday) "Hari ini sudah tercatat" else "Belum catat hari ini", if (d.recordedToday) K.Muted else K.ChartOut,
            ) {
                d.recentDays.forEachIndexed { i, done ->
                    val today = i == d.recentDays.lastIndex
                    DayBox(if (done) K.Yellow else K.Field) { if (today) Glyph("flame", K.ChartOut, 13.dp) }
                }
            }
        }
    }
}

/** 2×1 shared layout: icon circle, title, one line. */
@Composable
private fun Half(
    bg: ColorProvider, open: Intent, well: ColorProvider, icon: @Composable () -> Unit,
    title: String, titleColor: ColorProvider, line: String, lineColor: ColorProvider,
) {
    Row(
        GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(bg).padding(horizontal = 14.dp).clickable(actionStartActivity(open)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(GlanceModifier.size(36.dp).cornerRadius(18.dp).background(well), contentAlignment = Alignment.Center) { icon() }
        Spacer(GlanceModifier.width(10.dp))
        Column {
            Text(title, maxLines = 1, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = titleColor))
            Text(line, maxLines = 1, style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = lineColor))
        }
    }
}

/** 2×1 Catat: one big entry button with today's spending. */
class CatatWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = load(context)
        provideContent {
            Half(K.Green, openApp(context, "add"), K.GreenWell, { Glyph("plus", K.OnGreen, 19.dp) }, "Catat", K.OnGreen, "Hari ini −" + money(d.spentToday, d.hidden), K.OnGreen)
        }
    }
}

/** 2×1 Menunggu: how many bills and notification detections wait to be recorded. */
class WaitWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = load(context)
        provideContent {
            Half(
                K.Pink, openApp(context, if (d.waiting > 0) "review" else "home"), K.Well,
                { Text(if (d.waiting > 9) "9+" else "${d.waiting}", style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.OnPink)) },
                if (d.waiting > 0) "Menunggu" else "Semua beres", K.Ink, if (d.waiting > 0) "dicatat" else "tidak ada tunggakan", K.OnPink,
            )
        }
    }
}

class QuickWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = QuickWidget() }
class BudgetWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = BudgetWidget() }
class MonthWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = MonthWidget() }
class DailyWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = DailyWidget() }
class StreakWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = StreakWidget() }
class CatatWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = CatatWidget() }
class WaitWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = WaitWidget() }

/** Redraws every widget (called after data changes). Widgets not on the home screen cost nothing. */
object CatetWidgets {
    suspend fun refresh(ctx: Context) {
        runCatching { QuickWidget().updateAll(ctx) }
        runCatching { BudgetWidget().updateAll(ctx) }
        runCatching { MonthWidget().updateAll(ctx) }
        runCatching { DailyWidget().updateAll(ctx) }
        runCatching { StreakWidget().updateAll(ctx) }
        runCatching { CatatWidget().updateAll(ctx) }
        runCatching { WaitWidget().updateAll(ctx) }
    }
}

/** Widget palette: each colour has a day and a night value, so widgets follow the phone's light/dark mode. */
private object K {
    private fun dn(day: Long, night: Long) = androidx.glance.color.ColorProvider(Color(day), Color(night))
    val Ink = dn(0xFF24302A, 0xFFF1ECDF)
    val Ink2 = dn(0xFF4D453A, 0xFFD8D0C0)
    val Muted = dn(0xFF847C6E, 0xFFA39D90)
    val OnYellow = dn(0xFF7A6B4E, 0xFFF6DFA0)
    val OnPink = dn(0xFF8C6A64, 0xFFF4C9C0)
    val OnSage = dn(0xFF5F6B58, 0xFFCFE0BF)
    val Green = dn(0xFF2E5242, 0xFF9FD3B0)
    val OnGreen = dn(0xFFFAEAC0, 0xFF15221B)
    val Danger = dn(0xFF9A3B30, 0xFFF08A7C)
    val ChartIn = dn(0xFF3F8F63, 0xFF8DDCAA)
    val ChartOut = dn(0xFFD9853B, 0xFFF6A85E)
    val Yellow = dn(0xFFFAEAC0, 0xFF4A3F22)
    val Pink = dn(0xFFF5DCD7, 0xFF4A2E2B)
    val SageCard = dn(0xFFE4E9D9, 0xFF2F3A2C)
    val Card = dn(0xFFFDFCF6, 0xFF252A25)
    val Tile = dn(0xFFFDFCF6, 0x38000000)
    val Field = dn(0xFFF4EFE3, 0xFF2C312C)
    val Well = dn(0x99FFFFFF, 0x38000000) // circle behind an icon on a coloured widget
    val GreenWell = dn(0x2EFAEAC0, 0x1F15221B)
}
