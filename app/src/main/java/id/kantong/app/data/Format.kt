package id.kantong.app.data

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

val ID: Locale = Locale.forLanguageTag("id-ID")

/** 4221000 -> "4.221.000" */
fun Long.grouped(): String = NumberFormat.getIntegerInstance(ID).format(this)
fun Int.grouped(): String = toLong().grouped()

/** 4221000 -> "Rp4.221.000"; negative values get a real minus sign. */
fun Long.rupiah(): String = (if (this < 0) "−" else "") + "Rp" + abs(this).grouped()

/** Compact Indonesian units: 350000 -> "350 rb", 1600000 -> "1,6 jt". */
fun Long.short(): String {
    val a = abs(this)
    val s = when {
        a >= 1_000_000_000 -> trim(a / 1_000_000_000.0) + " M"
        a >= 1_000_000 -> trim(a / 1_000_000.0) + " jt"
        a >= 1_000 -> trim(a / 1_000.0) + " rb"
        else -> a.toString()
    }
    return if (this < 0) "−$s" else s
}

private fun trim(v: Double): String {
    val r = Math.round(v * 10) / 10.0
    return if (r % 1.0 == 0.0) r.toLong().toString() else r.toString().replace('.', ',')
}

fun monthKey(time: Long = System.currentTimeMillis()): Int {
    val c = Calendar.getInstance().apply { timeInMillis = time }
    return c.get(Calendar.YEAR) * 100 + (c.get(Calendar.MONTH) + 1)
}

fun startOfMonth(time: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply {
    timeInMillis = time; set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun startOfDay(time: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply {
    timeInMillis = time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun daysInMonth(time: Long = System.currentTimeMillis()): Int = Calendar.getInstance().apply { timeInMillis = time }.getActualMaximum(Calendar.DAY_OF_MONTH)
fun dayOfMonth(time: Long = System.currentTimeMillis()): Int = Calendar.getInstance().apply { timeInMillis = time }.get(Calendar.DAY_OF_MONTH)

fun fmt(time: Long, pattern: String): String = SimpleDateFormat(pattern, ID).format(Date(time))
fun monthName(time: Long = System.currentTimeMillis()): String = fmt(time, "MMMM yyyy")

/** "Hari ini", "Kemarin", or "Senin, 17 Nov". */
fun dayLabel(time: Long): String {
    val today = startOfDay()
    val d = startOfDay(time)
    return when (d) {
        today -> "Hari ini"
        today - 86_400_000L -> "Kemarin"
        else -> fmt(time, "EEEE, d MMM")
    }
}

fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 4..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..17 -> "Selamat sore"
    else -> "Selamat malam"
}

/** Monday-first weeks (common in Indonesia). */
fun startOfWeek(time: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply {
    timeInMillis = startOfDay(time)
    firstDayOfWeek = Calendar.MONDAY
    val back = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    add(Calendar.DAY_OF_YEAR, -back)
}.timeInMillis

fun startOfYear(time: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply {
    timeInMillis = startOfMonth(time); set(Calendar.MONTH, Calendar.JANUARY)
}.timeInMillis

private fun shift(time: Long, field: Int, amount: Int): Long = Calendar.getInstance().apply { timeInMillis = time; add(field, amount) }.timeInMillis

/** [from, to) of the current period: day | week | month | year | custom (uses [start]/[end], end inclusive by day). */
fun periodRange(period: String, start: Long = 0, end: Long = 0, now: Long = System.currentTimeMillis()): Pair<Long, Long> = when (period) {
    "day" -> startOfDay(now).let { it to shift(it, Calendar.DAY_OF_YEAR, 1) }
    "week" -> startOfWeek(now).let { it to shift(it, Calendar.DAY_OF_YEAR, 7) }
    "year" -> startOfYear(now).let { it to shift(it, Calendar.YEAR, 1) }
    "custom" -> startOfDay(start) to shift(startOfDay(if (end > 0) end else start), Calendar.DAY_OF_YEAR, 1)
    else -> startOfMonth(now).let { it to shift(it, Calendar.MONTH, 1) }
}

/** Indonesian name of a budget/report period. */
fun periodName(period: String): String = when (period) {
    "day" -> "Harian"; "week" -> "Mingguan"; "year" -> "Tahunan"; "custom" -> "Kustom"; else -> "Bulanan"
}

/** Report window chosen in Laporan: weekly / monthly / yearly around [anchor]. */
fun reportRange(mode: String, anchor: Long): Pair<Long, Long> = when (mode) {
    "week" -> startOfWeek(anchor).let { it to shift(it, Calendar.DAY_OF_YEAR, 7) }
    "year" -> startOfYear(anchor).let { it to shift(it, Calendar.YEAR, 1) }
    else -> startOfMonth(anchor).let { it to shift(it, Calendar.MONTH, 1) }
}
