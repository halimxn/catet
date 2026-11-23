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
    /** Bonds are counted per Rp1 juta nominal; their price is per Rp1 juta (par = 1.000.000). */
    fun unit(kind: String) = when (kind) { "stock" -> "lot"; "gold" -> "gram"; "bond" -> "jt"; else -> "unit" }
    /** Price as shown: bonds in % of par (stored per Rp1 juta), others in rupiah. */
    fun priceText(kind: String, v: Double): String = if (kind == "bond") String.format(java.util.Locale("in", "ID"), "%.2f%%", v / 10_000.0) else Math.round(v).grouped()
    fun qtyText(kind: String, qty: Double): String {
        if (kind == "bond") return "Rp" + Math.round(qty * 1_000_000).grouped()
        val v = qty / step(kind)
        val s = if (v % 1.0 == 0.0) v.toLong().grouped() else String.format(java.util.Locale("in", "ID"), "%,.2f", v)
        return "$s ${unit(kind)}"
    }
    /** Live prices: IDX stocks, and gold from a supported seller. */
    fun live(kind: String, code: String = "") = kind == "stock" || (kind == "gold" && code in Prices.GOLD_VENDORS) ||
        (kind == "bond" && Prices.bondTradable(code))
}

/** Approximate all-in stock fees per broker (buy %, sell % incl. 0.1% final tax). Editable fee field stays the source of truth. */
object Brokers {
    class Fee(val key: String, val name: String, val buy: Double, val sell: Double)
    val all = listOf(
        Fee("ajaib", "Ajaib", 0.1513, 0.2513),
        Fee("stockbit", "Stockbit", 0.15, 0.25),
        Fee("ipot", "IPOT", 0.15, 0.25),
        Fee("bni", "BNI Sekuritas", 0.17, 0.27),
        Fee("mirae", "Mirae", 0.149, 0.249),
        Fee("none", "Tanpa biaya", 0.0, 0.0),
    )
    fun of(key: String) = all.firstOrNull { it.key == key } ?: all.first()
}

/** Delayed IDX prices from Yahoo Finance (no key), gold prices from galeri24.co.id. Only tickers are sent. */
object Prices {
    /** Gold sellers listed on galeri24.co.id (Pegadaian group): key -> heading on the page. */
    val GOLD_VENDORS = linkedMapOf("ANTAM" to "Harga ANTAM", "UBS" to "Harga UBS", "GALERI24" to "Harga GALERI 24")
    fun goldName(key: String) = when (key) { "ANTAM" -> "Emas Antam"; "UBS" -> "Emas UBS"; "GALERI24" -> "Emas Galeri24"; else -> "Emas" }

    class Gold(val sell: Long, val buyback: Long)

    /** Fair price of a bond in % of par plus its coupon, from IBPA/PHEI (benchmark and retail series). */
    class Bond(val pricePct: Double, val coupon: Double) {
        /** Price per Rp1 juta nominal. */
        val perJuta: Long get() = Math.round(pricePct * 10_000)
    }

    /** SBR and ST can't be traded, so they stay at par. */
    fun bondTradable(code: String) = code.uppercase().let { c -> !c.startsWith("SBR") && !c.startsWith("ST") && c.isNotBlank() }

    /** All bonds on the IBPA fair-price page: code -> price and coupon. */
    fun bonds(): Map<String, Bond> {
        val c = URL("https://www.ibpa.co.id/Data/HPW-dan-Imbal-Hasil").openConnection() as HttpURLConnection
        c.connectTimeout = 10000; c.readTimeout = 10000
        c.setRequestProperty("User-Agent", "Mozilla/5.0")
        val html = c.inputStream.use { it.readBytes().decodeToString() }.also { c.disconnect() }
        val text = html.replace(Regex("<[^>]+>"), " ").replace("&nbsp;", " ").replace(Regex("\\s+"), " ")
        fun num(s: String) = s.replace(".", "").replace(',', '.').toDoubleOrNull()
        val out = LinkedHashMap<String, Bond>()
        // row: CODE ttm yieldToday priceToday yieldYesterday priceYesterday coupon
        Regex("""\b([A-Z]{2,4}\d{3,4}(?:T\d)?) ([\d,]+) ([\d,]+) ([\d,]+) ([\d,]+) ([\d,]+) ([\d,]+)""").findAll(text).forEach { m ->
            val price = num(m.groupValues[4]); val coupon = num(m.groupValues[7])
            if (price != null && coupon != null && price in 50.0..150.0) out.putIfAbsent(m.groupValues[1], Bond(price, coupon))
        }
        return out
    }

    /** Finds a bond by code; "ORI025" also matches "ORI025T6". */
    fun findBond(all: Map<String, Bond>, code: String): Pair<String, Bond>? {
        val c = code.trim().uppercase()
        all[c]?.let { return c to it }
        return all.entries.firstOrNull { it.key.startsWith(c) }?.let { it.key to it.value }
    }

    /** Price per gram (1 g row) for every supported seller: what you pay (sell) and what you get back (buyback). */
    fun gold(): Map<String, Gold> {
        val c = URL("https://galeri24.co.id/harga-emas").openConnection() as HttpURLConnection
        c.connectTimeout = 10000; c.readTimeout = 10000
        c.setRequestProperty("User-Agent", "Mozilla/5.0")
        val html = c.inputStream.use { it.readBytes().decodeToString() }.also { c.disconnect() }
        val text = html.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ")
        val out = LinkedHashMap<String, Gold>()
        GOLD_VENDORS.forEach { (key, heading) ->
            val i = text.indexOf("$heading ")
            if (i < 0) return@forEach
            val block = text.substring(i, minOf(text.length, i + 400))
            Regex("""(?<![\d.,])1 Rp([\d.]+) Rp([\d.]+)""").find(block)?.let { m ->
                val sell = m.groupValues[1].replace(".", "").toLongOrNull()
                val buy = m.groupValues[2].replace(".", "").toLongOrNull()
                if (sell != null && buy != null) out[key] = Gold(sell, buy)
            }
        }
        return out
    }

    class Quote(val price: Long, val prevClose: Long, val time: Long, val name: String)

    private fun get(url: String): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "Mozilla/5.0")
        return c.inputStream.use { JSONObject(it.readBytes().decodeToString()) }.also { c.disconnect() }
    }

    private fun symbol(code: String) = code.trim().uppercase() + ".JK"

    fun quote(code: String, raw: Boolean = false): Quote {
        val meta = get("https://query1.finance.yahoo.com/v8/finance/chart/${if (raw) code else symbol(code)}?range=1d&interval=1d")
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
