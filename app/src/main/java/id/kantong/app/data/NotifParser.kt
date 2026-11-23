package id.kantong.app.data

/** Bank and e-wallet apps whose notifications Catet can read. [short] is the badge text, [type] the account type it maps to. */
data class DetectApp(val pkg: String, val name: String, val short: String, val type: String)

object DetectApps {
    val all = listOf(
        DetectApp("com.bca", "BCA mobile", "BCA", "bank"),
        DetectApp("com.bca.mybca.omni.android", "myBCA", "BCA", "bank"),
        DetectApp("id.bmri.livin", "Livin' Mandiri", "MDR", "bank"),
        DetectApp("id.co.bri.brimo", "BRImo", "BRI", "bank"),
        DetectApp("src.com.bni", "BNI Mobile", "BNI", "bank"),
        DetectApp("id.bni.wondr", "wondr by BNI", "BNI", "bank"),
        DetectApp("com.bsm.activity2", "BSI Mobile", "BSI", "bank"),
        DetectApp("com.jago.digitalBanking", "Jago", "JG", "bank"),
        DetectApp("id.co.bankbkemobile.digitalbank", "SeaBank", "SB", "bank"),
        DetectApp("com.bcadigital.blu", "blu", "BLU", "bank"),
        DetectApp("com.btpn.dc", "Jenius", "JN", "bank"),
        DetectApp("id.co.cimbniaga.mobile.android", "OCTO", "CMB", "bank"),
        DetectApp("com.gojek.gopay", "GoPay", "GP", "ewallet"),
        DetectApp("com.gojek.app", "Gojek", "GJ", "ewallet"),
        DetectApp("ovo.id", "OVO", "OVO", "ewallet"),
        DetectApp("id.dana", "DANA", "DN", "ewallet"),
        DetectApp("com.shopeepay.id", "ShopeePay", "SP", "ewallet"),
        DetectApp("com.shopee.id", "Shopee", "SH", "ewallet"),
        DetectApp("com.telkom.mwallet", "LinkAja", "LA", "ewallet"),
        DetectApp("id.flip", "Flip", "FL", "ewallet"),
        DetectApp("com.grabtaxi.passenger", "Grab", "GR", "ewallet"),
    )

    fun of(pkg: String): DetectApp? = all.firstOrNull { it.pkg == pkg }
}

/**
 * Reads a transaction out of a notification's title and text, on the device. Returns null for anything that is not
 * clearly a completed money movement: OTPs, promos, order updates, or text it cannot read with confidence.
 */
object NotifParser {
    data class Parsed(val amount: Long, val kind: String, val merchant: String)

    private val secret = Regex("(?i)(\\botp\\b|kode verifikasi|kode rahasia|kode aktivasi|kode keamanan|verification code|jangan (berikan|bagikan|beritahu)|\\bpassword\\b|kata sandi|perangkat baru|login)")
    private val promo = Regex("(?i)(promo|diskon|voucher|kupon|cashback (hingga|s\\.?d|sampai|up to)|gratis ongkir|jangan lewatkan|hemat (hingga|sampai)|nikmati|dapatkan|\\bayo\\b|\\byuk\\b|hanya hari ini|berlaku hingga|klaim)")
    private val failed = Regex("(?i)(gagal|dibatalkan|ditolak|tidak berhasil|failed|pending|menunggu pembayaran|segera bayar|jatuh tempo|tagihan .* akan)")
    private val amountRx = Regex("(?i)(?:rp\\.?|idr)\\s?([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]+)(?:[.,]([0-9]{1,2}))?(?![0-9])")
    private val notAmount = Regex("(?i)(saldo|sisa|biaya|admin|limit|cashback|poin|minimal|min\\.)\\s*(akhir|kamu|anda|aktif|tersedia)?\\s*:?\\s*$")
    private val inRx = Regex("(?i)(dana masuk|uang masuk|transfer masuk|saldo masuk|masuk ke (rekening|akun|saldo)|menerima|diterima|terima (dana|uang|transfer|saldo)|\\bkredit\\b|\\bcr\\b|incoming|refund|pengembalian dana|top ?up|isi saldo|received)")
    private val outRx = Regex("(?i)(pembayaran|bayar|dibayar|pembelian|beli |transfer ke|transfer keluar|transfer berhasil|dana keluar|uang keluar|\\bdebit\\b|\\bdb\\b|tarik tunai|penarikan|qris|purchase|payment|terkirim|mengirim|kirim (uang|saldo|dana)|transaksi)")
    private val stop = "(?=\\s+(?:sebesar|senilai|rp\\.?|idr|pada|tgl|tanggal|via|telah|berhasil|sukses|sudah|dengan|melalui|menggunakan|jam|pukul|ref|no\\.?|ke|di|untuk)\\b|\\s*[.,;:()!\\n]|\\s+rp|$)"
    private val fromRx = Regex("(?i)\\b(?:dari|from|pengirim)\\s*:?\\s+([^\\n.,;:()!]{2,40}?)$stop")
    private val toRx = Regex("(?i)\\b(?:di|ke|kepada|untuk|at|to|merchant|penerima|tujuan)\\s*:?\\s+([^\\n.,;:()!]{2,40}?)$stop")
    private val holder = Regex("(?i)\\ba\\.?\\s?n\\.?\\s+([a-z][a-z .']{1,30}?)(?=\\s*[,;:()!\\n]|\\s+(?:sebesar|senilai|rp|idr|pada|berhasil)\\b|$)")
    private val generic = Regex("(?i)^(rekening|akun|saldo|aplikasi|kamu|anda|sana|sini|nomor|no|va|virtual account|bank|e-?wallet|dompet|merchant|toko|sebesar|transaksi|pembayaran)\\b.*|^[0-9].*")

    fun parse(title: String, text: String, appName: String): Parsed? {
        val all = "$title\n$text".trim()
        if (all.isBlank() || secret.containsMatchIn(all) || failed.containsMatchIn(all)) return null
        val amount = amountRx.findAll(all).firstNotNullOfOrNull { m ->
            if (notAmount.containsMatchIn(all.substring(maxOf(0, m.range.first - 22), m.range.first))) null
            else m.groupValues[1].replace(".", "").replace(",", "").toLongOrNull()
        } ?: return null
        if (amount < 100 || amount > 10_000_000_000L) return null
        val inAt = inRx.find(all)?.range?.first ?: Int.MAX_VALUE
        val outAt = outRx.find(all)?.range?.first ?: Int.MAX_VALUE
        if (inAt == Int.MAX_VALUE && outAt == Int.MAX_VALUE) return null
        // promos often carry an amount; only accept them when they also read like a finished transaction
        if (promo.containsMatchIn(all) && !Regex("(?i)(berhasil|sukses|telah|sudah)").containsMatchIn(all)) return null
        val kind = if (inAt < outAt) "in" else "out"
        return Parsed(amount, kind, merchant(all, title, kind, appName))
    }

    private fun merchant(all: String, title: String, kind: String, appName: String): String {
        val primary = if (kind == "in") fromRx else toRx
        val picks = (primary.findAll(all).map { it.groupValues[1] } + holder.findAll(all).map { it.groupValues[1] })
        picks.map { clean(it) }.firstOrNull { it.isNotEmpty() && !generic.matches(it) && !it.equals(appName, true) }?.let { return it }
        holder.find(all)?.let { clean(it.groupValues[1]).takeIf { n -> n.isNotEmpty() }?.let { return it } }
        val t = clean(title)
        if (t.isNotEmpty() && t.length <= 28 && !amountRx.containsMatchIn(t) && !inRx.containsMatchIn(t) && !outRx.containsMatchIn(t) &&
            !t.equals(appName, true) && !generic.matches(t)
        ) return t
        return if (kind == "in") "Dana masuk" else "Pembayaran $appName"
    }

    private fun clean(s: String): String {
        var x = s.replace(Regex("\\s+"), " ").trim().trim('-', '*', '"', '\'')
        x = x.replace(Regex("(?i)^(merchant|toko)\\s+"), "")
        if (x.length > 28) x = x.take(28).substringBeforeLast(' ')
        // SHOUTING names read better in title case
        if (x.length > 3 && x == x.uppercase() && x.any { it.isLetter() }) {
            x = x.lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
        }
        return x
    }

    /** Keyword guess of a category name for a merchant, used when there is no earlier record to learn from. */
    fun categoryHints(merchant: String, kind: String): List<String> {
        val m = merchant.lowercase()
        fun has(vararg w: String) = w.any { m.contains(it) }
        return when {
            kind == "in" && has("gaji", "payroll", "salary") -> listOf("gaji")
            kind == "in" -> emptyList()
            has("kopi", "coffee", "cafe", "kafe", "resto", "makan", "food", "bakso", "ayam", "mie ", "warung", "starbucks", "mcd", "kfc", "pizza", "burger", "bakery", "roti", "janji jiwa", "kenangan", "fore") -> listOf("makan", "kuliner", "jajan")
            has("gojek", "goride", "gocar", "grab", "bensin", "pertamina", "shell", "parkir", "tol", "krl", "mrt", "transjakarta", "kai ", "maxim", "bluebird") -> listOf("transport")
            has("pln", "listrik", "pdam", "indihome", "wifi", "internet", "biznet", "first media") -> listOf("tagihan", "listrik")
            has("pulsa", "paket data", "telkomsel", "indosat", "smartfren", "axis", " xl", "by.u") -> listOf("pulsa", "tagihan")
            has("indomaret", "alfamart", "alfamidi", "supermarket", "hypermart", "superindo", "tokopedia", "shopee", "lazada", "blibli", "belanja", "lawson", "familymart") -> listOf("belanja")
            has("apotek", "klinik", "rumah sakit", "dokter", "halodoc", "kimia farma") -> listOf("kesehatan")
            else -> emptyList()
        }
    }
}
