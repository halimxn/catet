package id.kantong.app.data

import java.util.Calendar

/** First-run sample data so every screen has something real to show. Matches the design canvas numbers roughly. */
object Seed {
    fun accounts() = listOf(
        Account(1, "BCA", "bank", 9_240_000, "SKY"),
        Account(2, "Bank Jago", "bank", 3_960_000, "LAV"),
        Account(3, "GoPay", "ewallet", 1_750_000, "MINT"),
        Account(4, "OVO", "ewallet", 300_000, "LAV"),
        Account(5, "Dompet", "cash", 850_000, "BUTTER"),
    )

    fun categories() = listOf(
        Category(1, "Makan & minum", "food", "PEACH", "out", 2_500_000, 1),
        Category(2, "Transportasi", "transport", "SKY", "out", 800_000, 2),
        Category(3, "Belanja", "shopping", "LAV", "out", 1_000_000, 3),
        Category(4, "Tagihan", "bill", "BUTTER", "out", 1_000_000, 4),
        Category(5, "Rumah & kos", "home", "SKY", "out", 1_500_000, 5),
        Category(6, "Hiburan", "fun", "PINK", "out", 500_000, 6),
        Category(7, "Kesehatan", "health", "CORAL", "out", 300_000, 7),
        Category(8, "Pendidikan", "edu", "SAGE", "out", 400_000, 8),
        Category(9, "Sedekah", "charity", "MINT", "out", 0, 9),
        Category(10, "Gaji", "salary", "MINT", "in", 0, 1),
        Category(11, "Usaha", "business", "PEACH", "in", 0, 2),
        Category(12, "Bonus", "gift", "PINK", "in", 0, 3),
        Category(13, "Lainnya", "other", "SAGE", "in", 0, 4),
    ) + subcategories()

    /** Subcategories (parentId = the main category). Also added to existing installs by the v2 migration. */
    fun subcategories() = listOf(
        Category(14, "Makan berat", "food", "CORAL", "out", 0, 1, parentId = 1),
        Category(15, "Kopi & jajan", "coffee", "PEACH", "out", 0, 2, parentId = 1),
        Category(16, "Buah", "fruit", "SAGE", "out", 0, 3, parentId = 1),
        Category(17, "Bensin", "fuel", "CORAL", "out", 0, 1, parentId = 2),
        Category(18, "Ojek online", "motor", "SKY", "out", 0, 2, parentId = 2),
        Category(19, "Parkir", "parking", "BUTTER", "out", 0, 3, parentId = 2),
        Category(20, "Belanja harian", "shopping", "LAV", "out", 0, 1, parentId = 3),
        Category(21, "Pakaian", "clothes", "PINK", "out", 0, 2, parentId = 3),
    )

    fun ledgers() = listOf(
        Ledger(1, "Harian", "ledger", "PEACH", sortOrder = 1),
        Ledger(2, "Rumah tangga", "home", "SKY", sortOrder = 2),
        Ledger(3, "Usaha", "business", "MINT", sortOrder = 3),
        Ledger(4, "Liburan", "travel", "PINK", sortOrder = 4),
    )

    /** Monthly budgets for the main spending categories. */
    fun budgets() = categories().filter { it.kind == "out" && it.parentId == 0L && it.budget > 0 }
        .map { Budget(name = it.name, period = "month", categoryIds = it.id.toString(), amount = it.budget) }

    fun goals() = listOf(
        Goal(1, "Motor baru", "motor", "SKY", 7_000_000, 6_000_000),
        Goal(2, "Liburan Bali", "travel", "PEACH", 4_000_000, 1_200_000),
        Goal(3, "Dana darurat", "shield", "LAV", 6_000_000, 1_500_000),
    )

    fun bills() = listOf(
        Bill(1, "Internet rumah", 350_000, 22, 4, "wifi", "SKY"),
        Bill(2, "Streaming", 54_000, 25, 6, "fun", "PINK"),
        Bill(3, "Listrik token", 200_000, 28, 4, "bolt", "BUTTER"),
        Bill(4, "Kos", 1_500_000, 1, 5, "home", "SKY"),
    )

    /** About a month of transactions ending today. */
    fun transactions(): List<Txn> {
        val out = mutableListOf<Txn>()
        val cal = Calendar.getInstance()
        fun at(daysAgo: Int, h: Int, m: Int): Long {
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -daysAgo); c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m)
            return c.timeInMillis
        }
        var id = 1L
        fun add(daysAgo: Int, h: Int, amount: Long, kind: String, cat: Long?, acc: Long, merchant: String, note: String = "", ledger: Long = 1, reimburse: Int = 0, starred: Boolean = false) {
            out += Txn(id++, amount, kind, cat, acc, null, merchant, note, at(daysAgo, h, (id * 7 % 60).toInt()), ledger, reimburse, starred)
        }
        add(0, 12, 18_000, "out", 14, 5, "Mie Ayam Pak Kumis")
        add(0, 8, 15_500, "out", 18, 3, "GoRide")
        add(1, 19, 1_250_000, "in", 11, 1, "Transfer dari Rina", "Penjualan online", ledger = 3)
        add(1, 13, 32_000, "out", 1, 3, "Warung Padang")
        add(2, 10, 86_000, "out", 3, 1, "Indomaret")
        add(2, 16, 24_000, "out", 15, 3, "Kopi Kenangan Senja", starred = true)
        add(3, 9, 54_000, "out", 6, 1, "Streaming")
        add(3, 20, 45_000, "out", 1, 5, "Sate Madura")
        add(4, 11, 120_000, "out", 3, 1, "Toko Sinar Jaya", "Perlengkapan kantor", reimburse = 1)
        add(5, 8, 22_000, "out", 2, 3, "GoRide")
        add(5, 18, 250_000, "out", 6, 1, "Bioskop")
        add(6, 12, 28_000, "out", 1, 5, "Bakso Solo")
        add(7, 9, 350_000, "out", 4, 1, "Internet rumah", ledger = 2)
        add(8, 14, 412_000, "out", 20, 1, "Belanja bulanan", ledger = 2, starred = true)
        add(9, 10, 60_000, "out", 8, 2, "Kursus online")
        add(10, 19, 85_000, "out", 7, 1, "Apotek")
        add(11, 12, 35_000, "out", 1, 5, "Nasi Goreng")
        add(12, 7, 18_000, "out", 2, 3, "KRL")
        add(13, 17, 56_000, "out", 9, 5, "Sedekah Jumat")
        add(14, 13, 210_000, "out", 21, 1, "Pakaian")
        add(15, 20, 160_000, "out", 6, 1, "Konser kecil")
        add(16, 12, 40_000, "out", 1, 3, "Ayam Geprek")
        add(17, 9, 1_500_000, "out", 5, 1, "Kos bulan ini", ledger = 2)
        add(18, 9, 8_500_000, "in", 10, 1, "Gaji", "Gaji bulanan")
        add(18, 18, 900_000, "in", 12, 2, "Bonus proyek")
        add(19, 8, 25_000, "out", 2, 3, "GoRide")
        add(20, 12, 48_500, "out", 3, 5, "Toko Sinar Jaya")
        add(22, 13, 30_000, "out", 1, 5, "Soto Ayam")
        add(24, 19, 75_000, "out", 1, 1, "Makan keluarga")
        add(26, 10, 200_000, "out", 4, 1, "Listrik token", ledger = 2)
        return out
    }
}
