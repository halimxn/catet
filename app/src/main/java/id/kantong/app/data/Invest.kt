package id.kantong.app.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Current state of one investment, replayed from its history: quantity, weighted average buy price
 * (buy fees included), cost basis, market value, realised gain from sells and dividends received.
 * A sell lowers quantity and cost at the average price; the average itself stays the same.
 */
class Position(
    val h: Holding,
    val qty: Double,
    val avg: Double,
    val cost: Long,
    val value: Long,
    val realized: Long,
    val dividends: Long,
    val history: List<InvTxn>,
) {
    val gain: Long get() = value - cost
    val gainPct: Double get() = if (cost > 0) gain * 100.0 / cost else 0.0
    val dayPct: Double get() = if (h.prevClose > 0 && h.lastPrice > 0) (h.lastPrice - h.prevClose) * 100.0 / h.prevClose else 0.0
    val dayGain: Long get() = if (h.prevClose > 0 && h.lastPrice > 0) ((h.lastPrice - h.prevClose) * qty).toLong() else 0L

    companion object {
        fun of(h: Holding, txns: List<InvTxn>): Position {
            var qty = 0.0; var cost = 0.0; var realized = 0.0; var div = 0L
            txns.sortedBy { it.time }.forEach { t ->
                when (t.type) {
                    "buy" -> { cost += t.qty * t.price + t.fee; qty += t.qty }
                    "sell" -> {
                        val q = t.qty.coerceAtMost(qty)
                        val avg = if (qty > 0) cost / qty else 0.0
                        realized += q * t.price - t.fee - avg * q
                        cost -= avg * q; qty -= q
                    }
                    "dividend" -> div += (t.qty * t.price).toLong()
                }
            }
            val price = if (h.lastPrice > 0) h.lastPrice.toDouble() else if (qty > 0) cost / qty else 0.0
            return Position(h, qty, if (qty > 0) cost / qty else 0.0, cost.toLong(), (qty * price).toLong(), realized.toLong(), div, txns.sortedByDescending { it.time })
        }
    }
}

/** How an investment kind is counted and labelled. Stocks are entered in lots of 100 shares. */
object InvKind {
    val all = listOf("stock" to "Saham", "gold" to "Emas", "bond" to "Obligasi", "fund" to "Reksa dana")
    fun label(kind: String) = all.firstOrNull { it.first == kind }?.second ?: "Investasi"
    /** Units per entered quantity: 1 lot = 100 shares. */
    fun step(kind: String): Double = if (kind == "stock") 100.0 else 1.0
    fun unit(kind: String) = when (kind) { "stock" -> "lot"; "gold" -> "gram"; else -> "unit" }
    fun qtyText(kind: String, qty: Double): String {
        val v = qty / step(kind)
        val s = if (v % 1.0 == 0.0) v.toLong().grouped() else String.format(java.util.Locale("in", "ID"), "%,.2f", v)
        return "$s ${unit(kind)}"
    }
    /** Live prices are only available for IDX stocks. */
    fun live(kind: String) = kind == "stock"
}

/** Delayed IDX prices from Yahoo Finance (no key). Only the ticker is sent. */
object Prices {
    class Quote(val price: Long, val prevClose: Long, val time: Long, val name: String)

    private fun get(url: String): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "Mozilla/5.0")
        return c.inputStream.use { JSONObject(it.readBytes().decodeToString()) }.also { c.disconnect() }
    }

    private fun symbol(code: String) = code.trim().uppercase() + ".JK"

    fun quote(code: String): Quote {
        val meta = get("https://query1.finance.yahoo.com/v8/finance/chart/${symbol(code)}?range=1d&interval=1d")
            .getJSONObject("chart").getJSONArray("result").getJSONObject(0).getJSONObject("meta")
        return Quote(
            Math.round(meta.getDouble("regularMarketPrice")),
            Math.round(meta.optDouble("chartPreviousClose", meta.optDouble("previousClose", 0.0))),
            meta.optLong("regularMarketTime") * 1000,
            meta.optString("longName").ifBlank { meta.optString("shortName") },
        )
    }

    /** Daily closes for a range like "1mo", "3mo", "1y". */
    fun closes(code: String, range: String): List<Double> {
        val q = get("https://query1.finance.yahoo.com/v8/finance/chart/${symbol(code)}?range=$range&interval=1d")
            .getJSONObject("chart").getJSONArray("result").getJSONObject(0)
            .getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("close")
        return (0 until q.length()).mapNotNull { if (q.isNull(it)) null else q.getDouble(it) }
    }
}
