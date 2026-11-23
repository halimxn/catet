package id.kantong.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotifParserTest {
    private fun p(title: String, text: String, app: String = "GoPay") = NotifParser.parse(title, text, app)

    @Test fun ewalletPayment() =
        assertEquals(NotifParser.Parsed(25_000, "out", "Kopi Kenangan"), p("Pembayaran berhasil", "Pembayaran Rp25.000 ke Kopi Kenangan berhasil."))

    @Test fun incomingFromPerson() =
        assertEquals(NotifParser.Parsed(150_000, "in", "Rani Puspita"), p("DANA", "Kamu menerima Rp150.000 dari RANI PUSPITA", "DANA"))

    @Test fun bankTransferToAccountHolder() =
        assertEquals(
            NotifParser.Parsed(1_250_000, "out", "Budi Santoso"),
            p("m-BCA", "Transfer ke 1234567890 a.n. BUDI SANTOSO sebesar Rp 1.250.000,00 berhasil", "BCA mobile"),
        )

    @Test fun qrisSkipsBalance() =
        assertEquals(
            NotifParser.Parsed(32_500, "out", "Warung Bu Sri"),
            p("Livin'", "Transaksi QRIS sebesar Rp32.500 di WARUNG BU SRI berhasil. Saldo Rp1.234.567", "Livin' Mandiri"),
        )

    @Test fun balanceBeforeAmount() =
        assertEquals(NotifParser.Parsed(20_000, "out", "PLN"), p("BRImo", "Saldo Anda Rp 500.000. Pembayaran Rp 20.000 ke PLN berhasil", "BRImo"))

    @Test fun incomingCompany() =
        assertEquals(NotifParser.Parsed(2_500_000, "in", "Pt Maju Jaya"), p("BRImo", "Dana masuk Rp 2.500.000 dari PT MAJU JAYA ke rekening Anda", "BRImo"))

    @Test fun topUp() = assertEquals(NotifParser.Parsed(100_000, "in", "Dana masuk"), p("OVO", "Top Up OVO Cash Rp100.000 berhasil", "OVO"))

    @Test fun idrFormat() = assertEquals(45_000L, p("Jago", "Payment IDR 45,000.00 to Tokopedia successful", "Jago")?.amount)

    @Test fun otpIgnored() = assertNull(p("GoPay", "Kode OTP kamu 123456. Jangan berikan kode ini ke siapa pun. Rp0"))
    @Test fun promoIgnored() = assertNull(p("GoPay", "Cashback hingga Rp50.000 pakai GoPay! Yuk transaksi sekarang"))
    @Test fun failedIgnored() = assertNull(p("GoPay", "Pembayaran Rp10.000 gagal"))
    @Test fun noAmountIgnored() = assertNull(p("Shopee", "Pesanan kamu sedang dikirim", "Shopee"))
    @Test fun noDirectionIgnored() = assertNull(p("BCA", "Info Rp10.000", "BCA mobile"))

    @Test fun paymentRequestIgnored() = assertNull(p("Shopee", "Selesaikan pembayaran Rp150.000 sebelum 23.59", "Shopee"))
    @Test fun bankTopUpIsOut() = assertEquals("out", NotifParser.parse("BCA", "Top up OVO Rp100.000 berhasil", "BCA mobile", "bank")?.kind)
    @Test fun walletTopUpIsIn() = assertEquals("in", NotifParser.parse("OVO", "Top up OVO Cash Rp100.000 berhasil", "OVO", "ewallet")?.kind)
    @Test fun creditCardIsOut() = assertEquals("out", NotifParser.parse("BCA", "Transaksi kartu kredit Rp250.000 di TOKOPEDIA berhasil", "BCA mobile", "bank")?.kind)
}
