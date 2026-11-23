package id.kantong.app.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import id.kantong.app.R
import java.io.OutputStream
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Monthly report as an A4 PDF, drawn with Android's own PdfDocument (no library, no network).
 * Page 1: summary vs last month, daily stats, insights, daily chart, categories, frequent stores.
 * Page 2: budget table, income by source, account balances, net worth. Then the transaction list.
 */
object PdfReport {
    private const val W = 595f
    private const val H = 842f
    private const val M = 40f

    private fun col(hex: Long) = hex.toInt()
    private val BG = col(0xFFFDFCF5); private val INK = col(0xFF24302A); private val MUTED = col(0xFF847C6E)
    private val GREEN = col(0xFF2E5242); private val SAGE = col(0xFFE4E9D9); private val PINK = col(0xFFF5DCD7)
    private val YELLOW = col(0xFFFAEAC0); private val CARD = col(0xFFFBF4E4); private val LINE = col(0xFFF1E8D4)
    private val UP = col(0xFF9A3B30); private val DOWN = col(0xFF3F8F63); private val ORANGE = col(0xFFD9853B)
    private val ON_YEL = col(0xFF7A6B4E); private val ON_SAGE = col(0xFF5F6B58); private val ON_PINK = col(0xFF8C6A64); private val GOLDINK = col(0xFFB08A2E)
    private val CATS = listOf(col(0xFF3F8F63), col(0xFF5B86C5), col(0xFFD9A23A), col(0xFF9C7AC2), col(0xFFD9685A))

    class Info(val transactions: Int, val pages: Int)

    private fun rp(v: Long) = (if (v < 0) "−" else "") + abs(v).grouped()
    private fun pct(v: Double) = String.format(java.util.Locale("in", "ID"), "%.0f%%", v)
    /** Change text, capped so a near-empty previous month doesn't print "▲1607%". */
    private fun chg(cur: Long, prev: Long): String { val p = abs(cur - prev) * 100 / prev; return if (p > 300) ">300%" else "$p%" }

    /** Data for one month and ledger. */
    private class Rep(private val d: AppData, val from: Long, val to: Long, ledger: Long) {
        val v = d.forLedger(ledger)
        val prevFrom = Calendar.getInstance().apply { timeInMillis = from; add(Calendar.MONTH, -1) }.timeInMillis
        val tx = v.txns.filter { it.time in from until to && it.kind != "transfer" }.sortedByDescending { it.time }
        val prev = v.txns.filter { it.time in prevFrom until from && it.kind != "transfer" }
        val income = tx.filter { it.kind == "in" }.sumOf { it.amount }
        val spent = tx.filter { it.kind == "out" }.sumOf { it.amount }
        val pIncome = prev.filter { it.kind == "in" }.sumOf { it.amount }
        val pSpent = prev.filter { it.kind == "out" }.sumOf { it.amount }
        val days = Calendar.getInstance().apply { timeInMillis = from }.getActualMaximum(Calendar.DAY_OF_MONTH)
        val elapsed = if (System.currentTimeMillis() in from until to) dayOfMonth() else days
        val daily = LongArray(days).also { a -> tx.filter { it.kind == "out" }.forEach { t -> a[Calendar.getInstance().apply { timeInMillis = t.time }.get(Calendar.DAY_OF_MONTH) - 1] += t.amount } }
        fun byRoot(list: List<Txn>, kind: String) = list.filter { it.kind == kind }.groupBy { v.rootOf(it.categoryId)?.name ?: "Tanpa kategori" }.mapValues { (_, l) -> l.sumOf { it.amount } }.toList().sortedByDescending { it.second }
        val cats = byRoot(tx, "out"); val pCats = byRoot(prev, "out").toMap()
        val incomeBy = byRoot(tx, "in")
        val frequent = tx.filter { it.kind == "out" && it.merchant.isNotBlank() }.groupBy { it.merchant.trim().lowercase() }
            .map { (_, l) -> Triple(l.first().merchant.trim(), l.size, l.sumOf { it.amount }) }.filter { it.second >= 2 }.sortedWith(compareByDescending<Triple<String, Int, Long>> { it.second }.thenByDescending { it.third }).take(4)
        val budgets = v.budgets.filter { it.period == "month" }.map { b ->
            val ids = b.categoryIdList.toSet()
            val used = tx.filter { it.kind == "out" && (v.rootOf(it.categoryId)?.id in ids || it.categoryId in ids) }.sumOf { it.amount }
            val lim = if (System.currentTimeMillis() in from until to) v.limitOf(b) else b.amount
            Triple(b.name, lim, used)
        }.sortedByDescending { if (it.second > 0) it.third * 100 / it.second else 0 }
        val perDay = run {
            val budgetTotal = budgets.sumOf { it.second }
            if (budgetTotal > 0) budgetTotal / days else 0L
        }
        fun balanceAt(a: Account, t: Long) = a.openingBalance + d.allTxns.filter { it.time < t }.sumOf {
            when {
                it.kind == "in" && it.accountId == a.id -> it.amount
                it.kind == "out" && it.accountId == a.id -> -it.amount
                it.kind == "transfer" && it.accountId == a.id -> -it.amount - it.fee
                it.kind == "transfer" && it.toAccountId == a.id -> it.amount
                else -> 0L
            }
        }
        val accounts = d.accounts.map { a -> Triple(a.name, balanceAt(a, from), balanceAt(a, minOf(to, System.currentTimeMillis() + 1))) }
        val cashEnd = accounts.sumOf { it.third }
        val invest = d.investValue
        val debtNet = d.receivable - d.payable

        val insights: List<Pair<Int, String>> = buildList {
            if (income > 0) {
                val rate = (income - spent) * 100.0 / income
                val pRate = if (pIncome > 0) (pIncome - pSpent) * 100.0 / pIncome else null
                add(DOWN to "Kamu menyisihkan ${pct(rate)} pemasukan" + (pRate?.let { if (rate >= it) ", lebih baik dari bulan lalu (${pct(it)})." else ", lebih rendah dari bulan lalu (${pct(it)})." } ?: "."))
            }
            cats.map { (n, s) -> Triple(n, s, pCats[n] ?: 0L) }.filter { it.third * 10 >= it.second && it.second > it.third * 6 / 5 }.maxByOrNull { (it.second - it.third) * 100 / it.third }?.let { (n, s, p) ->
                val big = tx.filter { it.kind == "out" && (v.rootOf(it.categoryId)?.name == n) }.maxByOrNull { it.amount }
                add(ORANGE to "$n naik ${chg(s, p)} dibanding bulan lalu" + (big?.let { ", terbesar dari ${it.merchant.ifBlank { n }} (Rp${it.amount.short().replace(" ", "")})." } ?: "."))
            }
            budgets.filter { it.second > 0 && it.third * 100 >= it.second * 90 }.take(2).takeIf { it.isNotEmpty() }?.let { l ->
                add(UP to "Anggaran " + l.joinToString(" dan ") { (n, lim, used) -> if (used > lim) "$n lewat Rp${(used - lim).short().replace(" ", "")}" else "$n sudah ${used * 100 / lim}%" } + ".")
            }
            if (spent > 0) {
                val weekend = tx.filter { it.kind == "out" }.filter { Calendar.getInstance().apply { timeInMillis = it.time }.get(Calendar.DAY_OF_WEEK).let { d -> d == Calendar.SATURDAY || d == Calendar.SUNDAY } }.sumOf { it.amount }
                add(col(0xFF5B86C5) to "Pengeluaran akhir pekan ${weekend * 100 / spent}% dari total bulan ini.")
            }
            if (isEmpty()) add(MUTED to "Belum ada catatan di bulan ini.")
        }
    }

    fun info(d: AppData, from: Long, ledger: Long, withList: Boolean): Info {
        val to = Calendar.getInstance().apply { timeInMillis = from; add(Calendar.MONTH, 1) }.timeInMillis
        val n = d.forLedger(ledger).txns.count { it.time in from until to && it.kind != "transfer" }
        return Info(n, 2 + if (withList && n > 0) (n + 37) / 38 else 0)
    }

    /** Writes the report for the month starting at [from]. */
    fun write(ctx: Context, d: AppData, from: Long, ledger: Long, withList: Boolean, out: OutputStream) {
        val to = Calendar.getInstance().apply { timeInMillis = from; add(Calendar.MONTH, 1) }.timeInMillis
        val r = Rep(d, from, to, ledger)
        val base = ResourcesCompat.getFont(ctx, R.font.nunito) ?: Typeface.DEFAULT
        fun face(w: Int) = if (Build.VERSION.SDK_INT >= 28) Typeface.create(base, w, false) else Typeface.create(base, if (w >= 700) Typeface.BOLD else Typeface.NORMAL)
        val fReg = face(600); val fBold = face(800); val fBlack = face(900)
        val month = fmt(from, "MMMM yyyy")
        val ledgerName = if (ledger == 0L) "Semua buku" else d.ledger(ledger)?.name ?: "Buku kas"
        val doc = PdfDocument()
        var pageNo = 0
        lateinit var page: PdfDocument.Page
        lateinit var c: Canvas
        var y = 0f

        // PDF embeds the variable font at its default weight, so heavier text gets synthetic bold.
        fun paint(size: Float, color: Int = INK, tf: Typeface = fReg, align: Paint.Align = Paint.Align.LEFT) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color; typeface = tf; textAlign = align
            isFakeBoldText = tf === fBold || tf === fBlack
            if (tf === fBlack) { style = Paint.Style.FILL_AND_STROKE; strokeWidth = size * 0.035f }
        }
        fun text(s: String, x: Float, yy: Float, size: Float, color: Int = INK, tf: Typeface = fReg, align: Paint.Align = Paint.Align.LEFT, maxW: Float = 0f) {
            val p = paint(size, color, tf, align)
            var t = s
            if (maxW > 0) while (t.length > 1 && p.measureText(t) > maxW) t = t.dropLast(2) + "…"
            c.drawText(t, x, yy, p)
        }
        fun rect(l: Float, t: Float, w: Float, h: Float, color: Int, r: Float = 10f) = c.drawRoundRect(RectF(l, t, l + w, t + h), r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        fun line(yy: Float) = c.drawLine(M, yy, W - M, yy, Paint().apply { color = LINE; strokeWidth = 0.8f })
        fun footer(note: String) {
            text("Dibuat dengan catet", M, H - 22f, 8f, MUTED)
            text(note, W / 2, H - 22f, 8f, MUTED, align = Paint.Align.CENTER)
            text("Halaman $pageNo", W - M, H - 22f, 8f, MUTED, align = Paint.Align.RIGHT)
        }
        fun newPage(note: String = "") {
            if (pageNo > 0) { footer(note); doc.finishPage(page) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), pageNo).create())
            c = page.canvas; c.drawColor(BG); y = M
        }
        fun heading(s: String, sub: String = "") { y += 18f; text(s, M, y, 12f, INK, fBlack); if (sub.isNotEmpty()) text(sub, M + paint(12f, tf = fBlack).measureText(s) + 8f, y, 8.5f, MUTED); y += 8f }
        fun change(cur: Long, prev: Long, goodUp: Boolean): Pair<String, Int>? {
            if (prev <= 0) return null
            val ch = (cur - prev) * 100.0 / prev
            val good = if (goodUp) ch >= 0 else ch <= 0
            return ((if (ch >= 0) "▲ " else "▼ ") + (if (abs(ch) > 300) ">300%" else pct(abs(ch))) + " vs bulan lalu") to (if (good) DOWN else UP)
        }

        // ---------- page 1 ----------
        newPage()
        c.drawArc(RectF(M, y, M + 16f, y + 16f), -40f, -280f, false, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f; color = GREEN; strokeCap = Paint.Cap.ROUND })
        c.drawCircle(M + 13.5f, y + 8f, 2.6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col(0xFFE8B860) })
        text("catet", M + 22f, y + 12f, 11f, INK, fBold)
        text("$ledgerName · dibuat ${fmt(System.currentTimeMillis(), "d MMM yyyy")}", W - M, y + 12f, 8.5f, MUTED, align = Paint.Align.RIGHT)
        y += 40f
        text("Laporan $month", M, y, 22f, INK, fBlack)
        y += 14f
        val bw = (W - 2 * M - 16f) / 3
        fun stat(i: Int, bg: Int, label: String, labelColor: Int, value: String, valueColor: Int, sub: Pair<String, Int>?) {
            val x = M + i * (bw + 8f)
            rect(x, y, bw, 58f, bg, 12f)
            text(label, x + 10f, y + 16f, 9f, labelColor, fBold)
            text(value, x + 10f, y + 34f, 15f, valueColor, fBlack)
            sub?.let { text(it.first, x + 10f, y + 49f, 8.5f, it.second, fBold) }
        }
        stat(0, SAGE, "Masuk", ON_SAGE, rp(r.income), GREEN, change(r.income, r.pIncome, true))
        stat(1, PINK, "Keluar", ON_PINK, rp(r.spent), INK, change(r.spent, r.pSpent, false))
        stat(2, YELLOW, "Sisa", ON_YEL, rp(r.income - r.spent), GREEN, if (r.income > 0) "tabungan ${pct((r.income - r.spent) * 100.0 / r.income)}" to ON_YEL else null)
        y += 66f
        val peakDay = r.daily.indices.maxByOrNull { r.daily[it] }
        listOf(
            "Rata-rata/hari" to rp(if (r.elapsed > 0) r.spent / r.elapsed else 0),
            "Hari terboros" to (peakDay?.takeIf { r.daily[it] > 0 }?.let { "${it + 1} ${fmt(from, "MMM")} · ${r.daily[it].short().replace(" ", "")}" } ?: "-"),
            "Transaksi" to "${r.tx.size} · ${r.tx.count { it.kind == "in" }} masuk",
        ).forEachIndexed { i, (l, v2) ->
            val x = M + i * (bw + 8f)
            rect(x, y, bw, 36f, CARD, 10f)
            text(l, x + 10f, y + 14f, 8.5f, MUTED, fBold)
            text(v2, x + 10f, y + 29f, 11f, INK, fBlack, maxW = bw - 16f)
        }
        y += 36f

        heading("Wawasan bulan ini")
        val insH = 12f + r.insights.size * 15f
        rect(M, y, W - 2 * M, insH, SAGE, 12f)
        r.insights.forEachIndexed { i, (color, s) ->
            val yy = y + 16f + i * 15f
            c.drawCircle(M + 14f, yy - 3f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
            text(s, M + 24f, yy, 9.5f, col(0xFF4D453A), fBold, maxW = W - 2 * M - 34f)
        }
        y += insH

        heading("Pengeluaran harian", if (r.perDay > 0) "— garis = jatah harian" else "")
        val chH = 70f
        rect(M, y, W - 2 * M, chH, CARD, 10f)
        val peak = max(r.daily.maxOrNull() ?: 0L, r.perDay).coerceAtLeast(1L)
        val slot = (W - 2 * M - 20f) / r.days
        r.daily.forEachIndexed { i, v2 ->
            if (v2 <= 0) return@forEachIndexed
            val h = (v2.toFloat() / peak) * (chH - 16f)
            rect(M + 10f + i * slot + slot * 0.15f, y + chH - 8f - h, slot * 0.7f, h, ORANGE, 2f)
        }
        if (r.perDay > 0) {
            val ly = y + chH - 8f - (r.perDay.toFloat() / peak) * (chH - 16f)
            c.drawLine(M + 8f, ly, W - M - 8f, ly, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col(0xFFA9B79A); strokeWidth = 1.4f; pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f) })
        }
        y += chH

        // categories (left) + frequent (right)
        val top = y
        heading("Per kategori")
        val leftW = (W - 2 * M) * 0.6f
        r.cats.take(6).forEachIndexed { i, (n, s) ->
            val p = if (r.spent > 0) s * 100 / r.spent else 0
            val prev = r.pCats[n] ?: 0L
            text(n, M, y + 10f, 9.5f, INK, fBold, maxW = leftW * 0.55f)
            if (prev > 0 && abs(s - prev) * 5 > prev) text((if (s > prev) "▲" else "▼") + chg(s, prev), M + leftW * 0.57f, y + 10f, 8f, if (s > prev) UP else DOWN, fBold)
            text("${s.short().replace(" ", "")} · $p%", M + leftW, y + 10f, 9.5f, INK, fBold, align = Paint.Align.RIGHT)
            rect(M, y + 14f, leftW, 4f, LINE, 2f)
            rect(M, y + 14f, leftW * p / 100f, 4f, CATS[i % CATS.size], 2f)
            y += 24f
        }
        if (r.cats.isEmpty()) { text("Tidak ada pengeluaran.", M, y + 12f, 9.5f, MUTED); y += 20f }
        val leftEnd = y
        y = top
        heading("")
        y -= 14f
        val rx = M + leftW + 22f; val rw = W - M - rx
        text("Paling sering", rx, y, 12f, INK, fBlack); y += 8f
        if (r.frequent.isEmpty()) text("—", rx, y + 12f, 9.5f, MUTED)
        r.frequent.forEach { (n, cnt, sum) ->
            text(n, rx, y + 12f, 9.5f, INK, fBold, maxW = rw * 0.55f)
            text("${cnt}× · ${sum.short().replace(" ", "")}", W - M, y + 12f, 9.5f, MUTED, fBold, align = Paint.Align.RIGHT)
            c.drawLine(rx, y + 17f, W - M, y + 17f, Paint().apply { color = LINE; strokeWidth = 0.8f })
            y += 20f
        }
        y = max(y, leftEnd)

        // ---------- page 2 ----------
        newPage()
        text("Rincian · $month", M, y + 14f, 15f, INK, fBlack); y += 18f
        fun table(cols: List<Pair<String, Float>>, rows: List<List<String>>, colors: List<List<Int?>> = emptyList(), boldLast: Boolean = false) {
            // header
            var x = M
            cols.forEachIndexed { i, (h, w) -> text(h, if (i == 0) x else x + w, y + 12f, 8.5f, MUTED, fBold, if (i == 0) Paint.Align.LEFT else Paint.Align.RIGHT); x += w }
            y += 16f; line(y)
            rows.forEachIndexed { ri, row ->
                x = M
                val bold = boldLast && ri == rows.lastIndex
                row.forEachIndexed { i, cell ->
                    val w = cols[i].second
                    val color = colors.getOrNull(ri)?.getOrNull(i) ?: INK
                    text(cell, if (i == 0) x else x + w, y + 13f, 9.5f, color, if (bold) fBlack else fBold, if (i == 0) Paint.Align.LEFT else Paint.Align.RIGHT, maxW = if (i == 0) w - 6f else 0f)
                    x += w
                }
                y += 18f; line(y)
            }
        }
        val tw = W - 2 * M
        heading("Anggaran per kategori")
        if (r.budgets.isEmpty()) { text("Belum ada anggaran bulanan.", M, y + 12f, 9.5f, MUTED); y += 18f }
        else {
            val rows = r.budgets.map { (n, lim, used) -> listOf(n, lim.grouped(), used.grouped(), rp(lim - used), if (lim > 0) "${used * 100 / lim}" else "-") } +
                listOf(listOf("Total", r.budgets.sumOf { it.second }.grouped(), r.budgets.sumOf { it.third }.grouped(), rp(r.budgets.sumOf { it.second - it.third }), r.budgets.sumOf { it.second }.let { l -> if (l > 0) "${r.budgets.sumOf { it.third } * 100 / l}" else "-" }))
            val colors = r.budgets.map { (_, lim, used) -> listOf(null, null, null, if (used > lim) UP else null, when { lim > 0 && used > lim -> UP; lim > 0 && used * 100 >= lim * 80 -> GOLDINK; else -> null }) }
            table(listOf("Kategori" to tw * 0.4f, "Batas" to tw * 0.16f, "Terpakai" to tw * 0.16f, "Sisa" to tw * 0.16f, "%" to tw * 0.12f), rows, colors, boldLast = true)
        }
        heading("Pemasukan per sumber")
        if (r.incomeBy.isEmpty()) { text("Tidak ada pemasukan.", M, y + 12f, 9.5f, MUTED); y += 18f }
        else table(listOf("Sumber" to tw * 0.6f, "Jumlah" to tw * 0.25f, "%" to tw * 0.15f), r.incomeBy.map { (n, s) -> listOf(n, s.grouped(), if (r.income > 0) "${s * 100 / r.income}%" else "-") })
        heading("Saldo akun")
        table(
            listOf("Akun" to tw * 0.4f, "Awal bulan" to tw * 0.22f, "Akhir" to tw * 0.22f, "Selisih" to tw * 0.16f),
            r.accounts.map { (n, a, b) -> listOf(n, rp(a), rp(b), (if (b - a > 0) "+" else "") + (b - a).short().replace(" ", "")) },
            r.accounts.map { (_, a, b) -> listOf(null, null, null, if (b > a) DOWN else if (b < a) UP else null) },
        )
        heading("Kekayaan bersih", "investasi & utang-piutang memakai nilai saat ini")
        listOf(Triple("Uang", r.cashEnd, YELLOW to ON_YEL), Triple("Investasi", r.invest, SAGE to ON_SAGE), Triple("Piutang − utang", r.debtNet, PINK to ON_PINK)).forEachIndexed { i, (l, v2, cc) ->
            val x = M + i * (bw + 8f)
            rect(x, y, bw, 40f, cc.first, 10f)
            text(l, x + 10f, y + 15f, 8.5f, cc.second, fBold)
            text(rp(v2), x + 10f, y + 32f, 12f, INK, fBlack)
        }
        y += 46f
        rect(M, y, tw, 34f, GREEN, 10f)
        text("Total kekayaan bersih", M + 12f, y + 21f, 10f, col(0xFFFFFFFF), fBold)
        text(rp(r.cashEnd + r.invest + r.debtNet), W - M - 12f, y + 22f, 14f, col(0xFFFFFFFF), fBlack, align = Paint.Align.RIGHT)
        y += 34f

        // ---------- transactions ----------
        if (withList && r.tx.isNotEmpty()) {
            val cols = listOf("Tanggal" to tw * 0.13f, "Catatan" to tw * 0.37f, "Kategori" to tw * 0.2f, "Akun" to tw * 0.13f, "Jumlah" to tw * 0.17f)
            r.tx.chunked(38).forEachIndexed { i, chunk ->
                newPage(if (i == 0) "" else "")
                text("Transaksi · $month" + if (i > 0) " (lanjutan)" else "", M, y + 14f, 15f, INK, fBlack); y += 26f
                var x = M
                cols.forEachIndexed { ci, (h, w) -> text(h, if (ci == cols.lastIndex) x + w else x, y + 12f, 8.5f, MUTED, fBold, if (ci == cols.lastIndex) Paint.Align.RIGHT else Paint.Align.LEFT); x += w }
                y += 16f; line(y)
                chunk.forEach { t ->
                    x = M
                    val cells = listOf(fmt(t.time, "d MMM"), t.merchant.ifBlank { t.note }.ifBlank { "-" }, d.category(t.categoryId)?.name ?: "-", d.account(t.accountId)?.name ?: "-", (if (t.kind == "in") "+" else "−") + t.amount.grouped())
                    cells.forEachIndexed { ci, cell ->
                        val w = cols[ci].second
                        val last = ci == cols.lastIndex
                        text(cell, if (last) x + w else x, y + 13f, 9f, if (last && t.kind == "in") GREEN else INK, fBold, if (last) Paint.Align.RIGHT else Paint.Align.LEFT, maxW = if (last) 0f else w - 6f)
                        x += w
                    }
                    y += 18f; line(y)
                }
            }
        }
        footer("")
        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }
}
