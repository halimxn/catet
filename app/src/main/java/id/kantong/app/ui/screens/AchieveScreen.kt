package id.kantong.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.kantong.app.data.AppData
import id.kantong.app.data.Prefs
import id.kantong.app.data.dayOfMonth
import id.kantong.app.data.daysInMonth
import id.kantong.app.data.fmt
import id.kantong.app.data.startOfDay
import id.kantong.app.data.startOfMonth
import id.kantong.app.ui.Nav
import id.kantong.app.ui.components.CRoundButton
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CatetSheet
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K
import java.util.Calendar
import kotlin.math.roundToInt

private const val DAY = 86_400_000L

/** Days with at least one record, the running streak (alive until a whole day passes without one) and the record. */
internal class StreakInfo(val days: Set<Long>, val current: Int, val longest: Int, val today: Boolean)

internal fun streakOf(d: AppData): StreakInfo {
    val days = d.allTxns.map { startOfDay(it.time) }.toSet()
    val today = startOfDay()
    var day = if (today in days) today else startOfDay(today - DAY / 2)
    var cur = 0
    while (day in days) { cur++; day = startOfDay(day - DAY / 2) }
    var best = 0; var run = 0; var prev = 0L
    days.sorted().forEach { x -> run = if (prev != 0L && ((x - prev).toDouble() / DAY).roundToInt() == 1) run + 1 else 1; best = maxOf(best, run); prev = x }
    return StreakInfo(days, cur, best, today in days)
}

/** One badge: [met] is true when the data shows it right now; once met it is kept in [Prefs] for good. */
internal class Badge(
    val key: String, val name: String, val desc: String, val icon: String, val bg: Color,
    val progress: Float, val progressText: String, val remaining: String?, val met: Boolean,
) {
    val earnedAt: Long get() = Prefs.badgeTime(key)
    val earned: Boolean get() = met || earnedAt > 0
}

internal fun badgesOf(d: AppData, s: StreakInfo): List<Badge> {
    val n = d.allTxns.size
    val monthStart = startOfMonth()
    val prevStart = startOfMonth(monthStart - DAY)
    val prevSpent = d.allTxns.filter { it.kind == "out" && it.time >= prevStart && it.time < monthStart }
    val budget = d.budgetTotal
    val underBudget = budget > 0 && prevSpent.size >= 5 && prevSpent.sumOf { it.amount } <= budget
    val goalBest = d.goals.filter { it.target > 0 }.maxOfOrNull { (it.saved.toFloat() / it.target).coerceAtMost(1f) } ?: 0f
    val sedekahTarget = Prefs.sedekahTarget
    val sedekah = d.allTxns.filter { it.time >= monthStart && isSedekah(d, it) }.sumOf { it.amount }
    val borrow = d.debtStates.filter { it.debt.direction == "borrow" }
    val borrowTotal = borrow.sumOf { it.debt.amount }
    val borrowPaid = borrow.sumOf { it.debt.amount - it.remaining }
    // a finished calendar month with a record on every day
    val first = s.days.minOrNull()
    var fullMonth = false
    if (first != null) {
        var m = startOfMonth(first)
        while (m < monthStart && !fullMonth) {
            val len = daysInMonth(m)
            fullMonth = (0 until len).all { i -> Calendar.getInstance().apply { timeInMillis = m; add(Calendar.DAY_OF_MONTH, i) }.timeInMillis.let { startOfDay(it) } in s.days }
            m = Calendar.getInstance().apply { timeInMillis = m; add(Calendar.MONTH, 1) }.timeInMillis
        }
    }
    val doneThisMonth = s.days.count { it >= monthStart }
    fun streakBadge(key: String, name: String, goal: Int, bg: Color) = Badge(
        key, name, "Mencatat $goal hari berturut-turut", "flame", bg,
        (s.current.toFloat() / goal).coerceAtMost(1f), "${minOf(s.current, goal)} / $goal hari",
        (goal - s.current).takeIf { it > 0 }?.let { "$it hari lagi" }, s.longest >= goal,
    )
    return listOf(
        Badge("first", "Langkah pertama", "Membuat catatan pertama", "pencil", K.SageCard, if (n > 0) 1f else 0f, "${minOf(n, 1)} / 1 catatan", null, n > 0),
        streakBadge("streak7", "Seminggu penuh", 7, K.Yellow),
        streakBadge("streak30", "Sebulan penuh", 30, K.Pink),
        Badge(
            "records100", "100 catatan", "Membuat 100 catatan", "bill", K.Pink, (n / 100f).coerceAtMost(1f), "${minOf(n, 100)} / 100 catatan",
            (100 - n).takeIf { it > 0 }?.let { "$it lagi" }, n >= 100,
        ),
        Badge(
            "budget", "Pas anggaran", "Sebulan penuh belanja tidak melewati anggaran", "envelope", K.SageCard,
            if (budget > 0 && d.spentMonth <= budget) dayOfMonth().toFloat() / daysInMonth() else 0f,
            when { budget <= 0 -> "Atur anggaran dulu"; d.spentMonth <= budget -> "Aman ${dayOfMonth()} / ${daysInMonth()} hari"; else -> "Bulan ini lewat anggaran" },
            if (budget > 0 && d.spentMonth <= budget) "${daysInMonth() - dayOfMonth()} hari lagi" else null, underBudget,
        ),
        Badge(
            "goal", "Target tercapai", "Satu target tabungan terkumpul penuh", "jar", K.Yellow, goalBest,
            if (d.goals.isEmpty()) "Belum ada target" else "Terdekat ${(goalBest * 100).roundToInt()}%", null, goalBest >= 1f,
        ),
        Badge(
            "sedekah", "Rajin berbagi", "Target sedekah sebulan terpenuhi", "charity", K.Yellow,
            if (sedekahTarget > 0) (sedekah.toFloat() / sedekahTarget).coerceAtMost(1f) else 0f,
            if (sedekahTarget > 0) "${(sedekah * 100 / sedekahTarget).coerceAtMost(100)}% dari target" else "Atur target sedekah dulu", null,
            sedekahTarget > 0 && sedekah >= sedekahTarget,
        ),
        Badge(
            "debtfree", "Bebas utang", "Semua utang lunas", "card", K.SageCard,
            if (borrowTotal > 0) (borrowPaid.toFloat() / borrowTotal).coerceIn(0f, 1f) else 0f,
            if (borrow.isEmpty()) "Belum ada utang tercatat" else "Sisa ${borrow.count { !it.done }} utang", null,
            borrow.isNotEmpty() && borrow.all { it.done },
        ),
        Badge(
            "fullmonth", "Tanpa absen", "Mencatat setiap hari dalam satu bulan", "history", K.Pink,
            doneThisMonth.toFloat() / daysInMonth(), "$doneThisMonth / ${daysInMonth()} hari bulan ini", null, fullMonth,
        ),
    )
}

@Composable
internal fun BadgeIcon(key: String, size: Dp, tint: Color = K.ChartOut) {
    if (key == "flame") Icon(Icons.Rounded.LocalFireDepartment, null, tint = tint, modifier = Modifier.size(size)) else CatIcon(key, size)
}

/** Round medal: pastel ring when earned, an orange progress ring when not. */
@Composable
internal fun Medal(b: Badge, size: Dp) {
    val inner = size * 0.74f
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (b.earned) {
            Box(Modifier.size(size).clip(CircleShape).background(b.bg))
        } else {
            Canvas(Modifier.size(size)) {
                val w = this.size.minDimension * 0.09f
                drawArc(K.c(0xFFEDE6D6, 0xFF313732), 0f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w), style = Stroke(w))
                if (b.progress > 0f) drawArc(
                    K.ChartOut, -90f, 360f * b.progress, false, topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
                    size = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w), style = Stroke(w, cap = StrokeCap.Round),
                )
            }
        }
        Box(Modifier.size(inner).clip(CircleShape).background(if (b.earned) K.Tile else K.Field), contentAlignment = Alignment.Center) {
            Box(Modifier.alpha(if (b.earned) 1f else 0.38f)) { BadgeIcon(b.icon, inner * 0.6f, if (b.earned) K.ChartOut else K.Muted) }
        }
    }
}

@Composable
fun AchieveScreen(d: AppData, nav: Nav) {
    val day = id.kantong.app.ui.theme.AppClock.day
    val s = remember(d, day) { streakOf(d) }
    val badges = remember(d, day) { badgesOf(d, s) }
    var open by remember { mutableStateOf<Badge?>(null) }
    val monthStart = startOfMonth()
    val today = dayOfMonth()
    val len = daysInMonth()
    val next = badges.filter { !it.earned && it.progress > 0f }.maxByOrNull { it.progress }

    Column(Modifier.fillMaxSize().background(K.Bg).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { CRoundButton(Icons.Rounded.ChevronLeft, "Kembali", { nav.back() }, size = 44.dp) }
        CTitle("Pencapaian", Modifier.padding(top = 8.dp))

        // streak card
        Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(K.Yellow).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mencatat beruntun", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${s.current}", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, lineHeight = 46.sp)
                        Text(" hari", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink, modifier = Modifier.padding(bottom = 7.dp))
                    }
                }
                Box(Modifier.size(62.dp).clip(RoundedCornerShape(20.dp)).background(K.Tile), contentAlignment = Alignment.Center) {
                    BadgeIcon("flame", 38.dp, if (s.today) K.ChartOut else K.ChartOut.copy(alpha = 0.45f))
                }
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat("${s.longest} hari", "terpanjang", Modifier.weight(1f))
                MiniStat("${s.days.count { it >= monthStart }} / $today", "hari tercatat", Modifier.weight(1f))
            }
            // this month: green = recorded, pale = missed, dashed = today (if not yet) and days to come
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..len).chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        week.forEach { day ->
                            val t = startOfDay(Calendar.getInstance().apply { timeInMillis = monthStart; set(Calendar.DAY_OF_MONTH, day) }.timeInMillis)
                            val done = t in s.days
                            val dashed = !done && day >= today
                            val color = if (day == today && !done) K.ChartOut else K.YellowLine
                            Box(
                                Modifier.weight(1f).height(30.dp).clip(RoundedCornerShape(10.dp))
                                    .background(when { done -> K.Green; day == today -> K.Tile; dashed -> Color.Transparent; else -> K.c(0xFFF3E2B3, 0xFF4B4431) })
                                    .drawBehind {
                                        if (dashed) drawRoundRect(
                                            color, cornerRadius = CornerRadius(10.dp.toPx()),
                                            style = Stroke(1.4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$day", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                                    color = when { done -> K.Yellow; day == today -> K.ChartOut; dashed -> K.c(0xFFC9B78E, 0xFF6E6650); else -> K.c(0xFFB8A57E, 0xFF8F8466) },
                                )
                            }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        // nearest badge
        next?.let { b ->
            Row(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(K.Pink).pressable { open = b }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(K.veil(0.55f)), contentAlignment = Alignment.Center) { BadgeIcon(b.icon, 26.dp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Berikutnya", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = K.OnPink)
                    Text(b.name, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                    Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(K.veil(0.60f))) {
                        Box(Modifier.fillMaxWidth(b.progress).height(7.dp).clip(RoundedCornerShape(4.dp)).background(K.Green))
                    }
                }
                Spacer(Modifier.width(12.dp))
                // short figure: "12/30" for counts, "86%" for everything else
                val count = Regex("^(\\d+) / (\\d+)").find(b.progressText)
                Text(count?.let { "${it.groupValues[1]}/${it.groupValues[2]}" } ?: "${(b.progress * 100).roundToInt()}%", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = K.OnPink)
            }
        }

        // badges
        Row(Modifier.padding(start = 2.dp, end = 2.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Lencana", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.weight(1f))
            Text("${badges.count { it.earned }} dari ${badges.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Muted)
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(K.Card).padding(vertical = 16.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            badges.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { b ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).pressable { open = b }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Medal(b, 66.dp)
                            Text(
                                b.name, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = if (b.earned) K.Ink else K.Muted,
                                textAlign = TextAlign.Center, lineHeight = 15.sp, modifier = Modifier.padding(top = 7.dp, start = 4.dp, end = 4.dp),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(120.dp))
    }
    open?.let { b -> BadgeSheet(b, isNew = false) { open = null } }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(15.dp)).background(K.Tile).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = K.OnYellow)
    }
}

/** Badge detail; the same sheet greets a newly earned badge. */
@Composable
internal fun BadgeSheet(b: Badge, isNew: Boolean, onDismiss: () -> Unit) {
    Overlay(onDismiss) {
        CatetSheet {
            Column(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Medal(b, 104.dp)
                if (isNew) Text("Lencana baru", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = K.Muted, modifier = Modifier.padding(top = 14.dp))
                Text(b.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, modifier = Modifier.padding(top = if (isNew) 2.dp else 14.dp))
                Text(b.desc, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = K.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (b.earned) {
                        val at = b.earnedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
                        SheetChip("Didapat ${fmt(at, "d MMM yyyy")}", K.SageCard, K.OnSage)
                    } else {
                        SheetChip(b.progressText, K.Yellow, K.OnYellow)
                        b.remaining?.let { SheetChip(it, K.Field, K.Muted) }
                    }
                }
            }
            RedButton(if (isNew) "Lanjutkan" else "Oke", Modifier.padding(top = 20.dp).fillMaxWidth()) { onDismiss() }
        }
    }
}

@Composable
private fun SheetChip(text: String, bg: Color, fg: Color) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = fg, modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 14.dp, vertical = 7.dp))
}

/**
 * Keeps earned badges in [Prefs] and greets a new one with [BadgeSheet]. The very first run marks everything already
 * earned as seen, so long-time users are not flooded.
 */
@Composable
internal fun BadgeWatcher(d: AppData) {
    var fresh by remember { mutableStateOf<Badge?>(null) }
    LaunchedEffect(d) {
        if (!d.loaded) return@LaunchedEffect
        // off the main thread: the full-month check walks every day since the first record
        val met = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { badgesOf(d, streakOf(d)) }.filter { it.met && Prefs.badgeTime(it.key) == 0L }
        if (met.isEmpty()) { if (!Prefs.badgesInit) Prefs.badgesInit = true; return@LaunchedEffect }
        val now = System.currentTimeMillis()
        met.forEach { Prefs.earnBadge(it.key, now) }
        if (Prefs.badgesInit) fresh = met.first() else Prefs.badgesInit = true
    }
    fresh?.let { b -> BadgeSheet(b, isNew = true) { fresh = null } }
}
