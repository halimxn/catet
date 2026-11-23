# catet: panduan untuk agen AI (Antigravity)

Baca file ini **sebelum mengubah apa pun**. Semua aturan di sini sudah disetujui pemilik aplikasi lewat banyak putaran desain. Jangan menafsirkan ulang gaya; ikuti token, komponen, dan aturan penempatan di bawah.

---

## 1. Ringkasan produk

- **Nama:** `catet` (selalu huruf kecil di wordmark). Slogan: "Catet dulu, aman kemudian."
- **Fungsi:** pencatat keuangan harian untuk pengguna Indonesia (Rupiah, bahasa Indonesia). Fokus utama: sisa uang bulan ini, jatah harian dari anggaran, dan riwayat yang mudah dibaca.
- **Platform:** Android, Kotlin + Jetpack Compose + Room. minSdk 26. Rilis R8, ditandatangani debug key.
- **ID aplikasi tetap `id.kantong.app`** dan paket kode tetap `id.kantong.app`. JANGAN diganti: menggantinya membuat aplikasi terpasang sebagai aplikasi baru dan data pengguna hilang.
- **Referensi desain utama:** "R89" (Dribbble "pennykin", aplikasi uang saku keluarga): latar krem, hijau tua, kartu besar pastel (kuning, pink, sage), ikon dua warna. Fungsi dan alurnya mengikuti aplikasi "Meow Expense" (buku kas, subkategori, reimburse, bookmark, anggaran per periode, berulang).
- **Mockup final:** folder `desain/`
  - `1-Beranda.png`
  - `2-Tambah-Laporan.png`
  - `2b-Aset-Saya.png`
  - `3-Layar-pendukung.png`: Riwayat, Anggaran, Detail, Buku kas, Periode
  - `4-Identitas.png`
  - `5-Alur-navigasi.png`
- **Referensi gaya:** folder `referensi/` (R1–R120). Referensi yang dipakai adalah R89.

## 2. Aturan wajib (tidak boleh dilanggar)

1. **Tidak ada babi.** Tidak boleh ada gambar, ikon, ilustrasi, emoji, maupun kata yang berkaitan dengan babi, termasuk "celengan" atau "celeng", bahkan sebagai opsi yang ditolak. Untuk tabungan, pakai toples koin, kantong uang, atau target.
2. **Tidak ada maskot.** Hamster Kanto di `Mascot.kt` hanya sisa lama; jangan dipakai di layar baru.
3. **Bahasa Indonesia** yang singkat dan ramah. Nominal ditulis "Rp7.004.500": titik sebagai pemisah ribuan, tanpa desimal.
4. **Warna hanya dari token `K`** (lihat bagian 3). Jangan menulis warna heksa baru di layar; kalau butuh warna baru, tambahkan ke `Theme.kt` dulu.
5. **Hanya pastel dan tenang.** Tidak ada kartu atau chip gelap kecuali tombol utama hijau tua. Tidak ada gradasi mencolok, bayangan tebal, atau neon.
6. **Ikon memakai `CatIcon(key)`** (gaya dua warna: garis gelap dengan isian sage/emas/krem/pink). Jangan memakai ikon Material untuk kategori. Ikon Material hanya untuk glyph kecil UI (panah, cari, mata, centang, tutup).
7. **Jangan menghapus fitur yang sudah ada.** Fitur boleh dipindah ke tempat lain asal tetap bisa dijangkau.
8. **Data pengguna aman.** Perubahan skema Room wajib memakai Migration (lihat `MIGRATION_1_2` di `data/Model.kt`). Jangan memakai `fallbackToDestructiveMigration`.
9. **Selalu cek visual** di HP atau emulator pada lebar sekitar 360dp sebelum menyatakan selesai.

## 3. Sistem desain

### Token warna (`ui/theme/Theme.kt`, `object K`)

| Token | Nilai | Pakai untuk |
|---|---|---|
| `Bg`, `Sheet` | #FDFCF5 | latar layar dan lembar |
| `Card` | #FBF4E4 | kartu daftar krem (riwayat, daftar setelan) |
| `Field` | #F4EFE3 | input, tombol keypad, pill netral |
| `Tile` | #FDFCF6 | kotak putih di dalam kartu berwarna |
| `Yellow` | #FAEAC0 | kartu utama (saldo, nominal, arus kas) |
| `Pink` | #F5DCD7 | kartu tindakan tertunda (tagihan menunggu) |
| `SageCard` | #E4E9D9 | kartu anggaran/jatah, grafik harian |
| `Green` (alias `Red`) | #2E5242 | tombol utama, pilihan aktif, nominal saldo |
| `Ink` / `Ink2` / `Muted` | #2B2B29 / #4D453A / #6F6A60 | teks utama / label / keterangan |
| `Income` | #2E5242 | nominal pemasukan (+) |
| `Expense` | = Ink | nominal pengeluaran (−) |
| `Danger` | #9A3B30 | lewat anggaran, utang, hapus |
| `Gold`, `SageMid`, `Cream` | #E8B860, #A9B79A, #F6E2AE | isian ikon, cincin, aksen |
| `Notch`, `Line`, `YellowLine` | garis tipis dan pemisah |

**Warna grafik** (sudah diuji dengan validator palet buta warna):
- Pasangan masuk/keluar: `ChartIn` #3F8F63 dan `ChartOut` #D9853B. Selalu sertakan keterangan warna atau label nama, karena kedua warna ini agak mirip bagi pembaca buta warna.
- Kategori: `ChartCats`, lima warna dengan urutan tetap: hijau, biru, kuning, ungu, koral. Kategori keenam dan seterusnya digabung menjadi "Lainnya".
- Kalender: `Heat`, lima tingkat hijau dari muda ke tua.

### Tipografi

- **Font:** Nunito (variable, `res/font/nunito.ttf`). Ketebalan dipetakan di `Theme.kt`: Normal 600, Medium 680, SemiBold 720, Bold 800, ExtraBold 850.

| Elemen | Ukuran (sp) | Ketebalan |
|---|---|---|
| Judul layar | 30 | ExtraBold |
| Judul bagian | 20 | ExtraBold |
| Judul kartu | 16–16,5 | Bold/SemiBold |
| Isi | 15 | (bawaan) |
| Keterangan | 13 | (bawaan), warna Muted |

- **Nominal besar:**
  - Saldo 46–54sp, jatah 38–40sp, keduanya ExtraBold hijau.
  - "Rp" ditulis 45% lebih kecil dan menempel di angka; gunakan komponen `Money()`.
  - Jangan pernah membuat "Rp" melayang (superscript).
- **Nominal di daftar:** tanpa "Rp", tebal, rata kanan. Pengeluaran "−24.000" berwarna ink; pemasukan "+1.250.000" berwarna hijau.

### Bentuk dan jarak

- Padding horizontal layar 20dp.
- Kartu besar: radius 26–30dp, padding 18dp, jarak antarkartu 12–15dp.
- Kartu daftar: radius 21–22dp. Baris setinggi sekitar 62dp, tanpa garis pemisah di riwayat, dengan garis tipis di daftar setelan.
- Tombol utama: tinggi 46–54dp, radius 15–18dp, hijau dengan teks putih. Tombol sekunder: outline 1,5dp gelap di atas krem.
- Tombol bulat (tutup, kembali, cari, tambah): 40–44dp.
- Tab layar butuh ruang kosong sekitar 100–120dp di akhir scroll, karena bar navigasi menimpa konten.

### Komponen (pakai ini, jangan membuat versi baru)

- **`ui/components/Catet.kt`:**
  - `CCard`, `CChip`, `CRoundButton`
  - `Money`, `BudgetRing`, `CatetLogo`
  - `CTitle`, `CSection`, `CGroupLabel`
  - `CList`, `CListRow`, `CHandle`
- **`ui/components/Meow.kt`** (sudah bergaya baru):
  - `MeowSegmented` (pilihan aktif hijau)
  - `RedButton` (tombol utama hijau), `CreamButton` (outline)
  - `MeowToggle`, `IconTile`, `SquareButton`
  - Overlay dan lembar: `Overlay`, `SheetCard`, `FloatingCard`, `PillMenu` + `PillOption` (menu dropdown), `CalendarCard`, `PeriodCard`
- **`ui/components/Records.kt`:**
  - `RecordTimeline` (riwayat per hari), `CleanRow`, `DayHeader`
  - `RecordDetailCard` (lembar detail)
  - `StarMark`, `SmallChip`, `CatetSheet`, `SheetHeader`
- **Lainnya:**
  - `ui/components/PeriodSheet.kt`: pilih bulan berupa kartu bulan geser (HorizontalPager) + strip 2×6
  - `CalendarCard` (Meow.kt): lembar "Pilih tanggal" bergaya kalender pengeluaran, hari bulan lain bergaris putus-putus
  - `ui/components/TimeWheel.kt`: `TimePickerCard` roda jam/menit + chip menit cepat
  - `ui/components/ReportCharts.kt`: `GroupedBars`, `DailyLine`, `MonthHeatmap`
  - `ui/components/Charts.kt`: `Donut`
- **`ui/components/Illus.kt`:** `CatIcon(key, size)`. Semua isian otomatis dipetakan ke palet dua warna lewat fungsi `tone()`. Ikon baru digambar di `drawIllus` pada grid 48 dengan garis luar memakai helper `fillStroke`, `rr`, dan `circ`.
- **`Modifier.pressable(onClick)`** di `Common.kt` untuk semua area yang bisa diketuk.

## 4. Navigasi dan alur ketuk

**Tab bawah** (`ui/KantongApp.kt`):
- Empat tab: Beranda, Laporan, Aset, Saya.
- Kapsul mengambang putih (`K.Tile`, tinggi 64dp, radius 32dp, margin samping 20dp, bayangan lembut) di atas gradasi krem. Tab aktif: pil kuning 54x30 di belakang ikon + label hijau ExtraBold; tab lain abu #9C968A.
- Tidak ada tombol + mengambang.

**Rute** (`object R`):
- Tab: `HOME`, `REPORTS`, `ASSETS`, `ME`.
- Layar yang didorong di atas tab:
  - `add(editId)`, `SEARCH`
  - `CATEGORIES`, `categoryAdd`
  - `LEDGERS`, `ledger(id)`
  - `account(id)`, `accountEdit(id)`
  - `types(kind)`
  - `RECURRING`, `REIMBURSE`, `BOOKMARKS`, `GOALS`

**Alur ketuk di Beranda** (lihat `desain/5-Alur-navigasi.png`):

| Ketuk | Tujuan |
|---|---|
| Pil buku kas | Menu buku kas (`PillMenu`) |
| Judul bulan | `PeriodSheet` (mengubah `ViewPeriod`) |
| Cari | Riwayat lengkap (`R.SEARCH`) |
| Ikon mata | `Prefs.hideAmounts` |
| Cincin anggaran / "Lihat anggaran" | `BudgetSheet` |
| Tombol cepat Pemasukan / Pengeluaran / Transfer | `vm.startAdd(kind)`, lalu `R.add()` |
| Kartu pink: Ubah | `R.RECURRING` |
| Kartu pink: Catat | `vm.payBill` (langsung tercatat) |
| Baris riwayat | `RecordDetailCard`, lalu Ubah (`R.add(id)`) atau Hapus |
| "Lihat semua" | `R.SEARCH` |

**Bulan yang dilihat** dibagi antara Beranda dan Laporan lewat `data/ViewPeriod.kt`. Nilai 0 berarti bulan berjalan.

## 5. Aturan penempatan per layar

**Beranda** (`HomeScreen.kt`), urutan wajib:
1. Logo + "catet" (kiri); pil buku kas dua baris di kanan (ikon dua warna + nama + "Buku kas" + ▾).
2. Judul bulan 27sp dengan ▾, di bawahnya satu baris "Minggu, 22 Nov · sisa N hari" (Muted). Tombol cari bulat 46dp dengan `CatIcon("search")` di kanan judul.
   Warna teks mengikuti R89: `Ink` hijau-hitam, `Muted` abu-cokelat; label di kartu memakai `OnYellow` / `OnPink` / `OnSage`.
3. Kartu **kuning**:
   - "Sisa uang bulan ini" + mata.
   - Nominal besar hijau, dengan cincin anggaran di kanan (persen di tengah, label "anggaran").
   - Dua kotak putih transparan Masuk | Keluar (`InOutBox`).
   - Tiga tombol cepat putih: Pemasukan, Pengeluaran, Transfer.
4. Kartu **pink** "Menunggu dicatat · 1 dari N": tagihan jatuh tempo yang belum dibayar, dengan tombol Ubah (outline) dan Catat (hijau). Hanya muncul kalau ada tagihan.
5. Kartu **sage** "Jatah harian":
   - Nominal per hari dan chip "Sisa N hari".
   - Strip 7 hari (`WeekStrip`): batang = pengeluaran ÷ jatah, oranye kalau lewat, hari ini bergaris putus-putus.
   - Baris putih "Anggaran terpakai" + "Lihat ›" (buka `BudgetSheet`).
   - Kalau belum ada anggaran: ajakan dengan tombol "Atur anggaran".
6. "Riwayat" + "Lihat semua", lalu `RecordTimeline` bulan yang dilihat.

**Riwayat** (`RecordTimeline` dan `SearchScreen.kt`):
- Label hari bertebal Ink2. Total harian berupa pill kecil di kanan: hijau di atas sage kalau positif, Ink2 di atas Field kalau negatif, tanpa "Rp".
- Baris:
  - ikon 30dp tanpa kotak,
  - judul 16sp,
  - chip "Reimburse" kuning dan bintang emas kalau di-bookmark,
  - keterangan "● Kategori › ● Akun",
  - nominal tanpa "Rp" di kanan.
- Jam tidak ditampilkan di daftar; jam ada di lembar detail.
- Riwayat lengkap berisi kotak cari, chip filter (Semua, Keluar, Masuk, Reimburse, Bookmark, dan "Filter lain"), kartu kuning Masuk|Keluar, lalu timeline.

**Tambah transaksi** (`AddTransactionScreen.kt`): susunan Meow dengan gaya R89.
- Tombol tutup bulat + segmen Masuk/Keluar/Transfer (urutan Masuk dulu di semua layar).
- Kartu kuning:
  - chip kategori dan chip tanggal,
  - nominal 54sp dengan ekspresi hitungan di bawahnya,
  - chip akun, kolom catatan, dan tombol "⋯" (buku kas, reimburse).
- "Kategori" + "Semua ›", lalu satu baris lingkaran kategori 58dp yang bisa digeser.
  - Kategori terpilih: latar kuning dengan cincin hijau.
  - Ketuk lagi atau tekan lama untuk memilih subkategori.
- Keypad 4×4 datar tanpa kotak:
  - Angka 27sp. Kolom kanan ⌫ + − = berupa lingkaran sage.
  - Tombol "," dipakai untuk simpan & lanjut.
- Tombol lebar "✓ Simpan · RpX".
- Jangan kembali ke grid 12 kategori. Itu ditolak karena terlalu menumpuk.

**Laporan** (`ReportsScreen.kt`): satu tampilan tanpa tab, urutannya:
1. Judul + cari + chip periode (bulan/minggu/tahun).
2. Kartu kuning "Arus kas": sisa uang, bar Masuk dan bar Keluar beserta persennya.
3. Kartu krem "Masuk vs keluar" 6 bulan: batang berpasangan, angka hanya di bulan terpilih, ketuk batang untuk pindah bulan.
4. Baris "Grafik" + segmen **Pemasukan | Pengeluaran** (Masuk selalu di kiri). Segmen ini satu-satunya pengatur jenis untuk semua grafik di bawahnya. Jangan menambah tab ganda lagi.
5. Tiga kotak sorotan (rata-rata/hari, hari terboros atau teramai, jumlah transaksi).
6. Kartu sage grafik harian dengan garis batas anggaran putus-putus (khusus pengeluaran).
7. Kartu krem donat per kategori, ditambah daftar lengkap kalau kategorinya lebih dari lima.
8. Kartu krem kalender (heatmap).

**Aset** (`AssetsScreen.kt`):
- Judul + chip Transfer + tombol "+" bulat.
- Kartu kuning "Kekayaan bersih" (+ mata) dengan kolom Aset | Utang.
- Kelompok akun dengan subtotal: Tunai & e-wallet, Bank, Lainnya, Kartu kredit & utang.
- Di bawahnya: bagian Grafik dan Target tabungan.

**Saya** (`SettingsScreen.kt`):
- Logo + "catet", judul "Saya".
- Kartu kuning cadangan dengan tombol Pulihkan dan Cadangkan.
- Daftar "Kelola": Buku kas, Kategori, Transaksi berulang, Anggaran, Reimburse, Bookmark, Target tabungan.
- Daftar "Pengaturan": Keamanan & pengingat, Tampilan, Cadangan & sinkron, Data, Tentang.

**Lembar** (sheet): sudut atas radius 30, pegangan (`CHandle`), tombol tutup bulat, tombol aksi di bawah (outline + hijau). Scrim memakai `K.Scrim`.

## 6. Status fitur

**Sudah ada dan bergaya baru (selesai pola R89):**
- Beranda, Riwayat lengkap (`SearchScreen`), Tambah/ubah transaksi (kalkulator, subkategori, transfer + biaya, reimburse, buku kas).
- Laporan dengan grafik, Aset, Saya (PIN, pengingat harian, cadangan otomatis SAF, ekspor CSV, impor, hapus data, gaya tampilan catatan).
- Lembar anggaran per kategori (`BudgetSheet`), form anggaran (`BudgetForm`), detail transaksi (`RecordDetailCard`), pilih bulan, menu buku kas.
- `CategoriesScreen`, `CategoryAddScreen` (kategori & subkategori).
- `LedgersScreen`, `LedgerDetailScreen` (daftar & analisis buku kas).
- `RecurringScreen` (tagihan rutin bulanan), `ReimburseScreen`, `BookmarksScreen`, `GoalsScreen` (target tabungan, bersih tanpa maskot & tanpa babi).
- `AccountDetailScreen`, `AccountEditScreen`.
- `AllTypesScreen` (distribusi kategori laporan), `LockScreen` (layar kunci PIN bersih dengan ikon shield).
- Teks kategori panjang di riwayat sudah dilindungi dari pemotongan (`widthIn` pada nama akun).
- Tombol "Atur" di baris kategori pada layar Tambah terhubung dengan benar.
- Onboarding 4 langkah (`OnboardingScreen.kt`, `Prefs.onboarded`): sambutan, nama buku kas, saldo akun, anggaran bulanan. Data disimpan hanya saat Selesai; "Lewati" tidak mengubah data.
- Tab bawah berupa kapsul mengambang putih (64dp, radius 32, bayangan lembut); tab aktif = pil kuning di belakang ikon + label hijau ExtraBold.
- Rollover anggaran (`Budget.rollover`, `rolloverOver`; `AppData.carryFor`/`limitOf`): sisa periode lalu ditambahkan, opsional kelebihan dikurangkan. `budgetTotal` memakai `limitOf`.
- Investasi (`data/Invest.kt`, `ui/screens/InvestScreen.kt`, tabel `holdings` + `inv_txns`, DB v3 `MIGRATION_2_3`):
  - Aset dibagi **Uang** (kartu krem) dan **Investasi** (kartu sage + daftar krem, sakelar `Prefs.showInvest`).
  - Harga rata-rata tertimbang dihitung dari riwayat beli (`Position.of`); jual tidak mengubah rata-rata.
  - Beli = transfer keluar dari akun (toAccountId null); jual/dividen = transfer masuk (accountId 0), supaya tidak dihitung pengeluaran/pemasukan.
  - Harga saham BEI dari Yahoo Finance (`Prices`), hanya saat tombol ↻ diketuk atau detail dibuka.
  - Emas: penjual Antam / UBS / Galeri24 (Pegadaian) dari galeri24.co.id (`Prices.gold()`, baris 1 gram); `code` = kunci penjual, nilai memakai harga buyback, beli memakai harga jual. "Lainnya" = harga manual.
  - Obligasi: kolom `coupon` (% per tahun, DB v5 `MIGRATION_4_5`); kupon bersih per bulan = modal × kupon ÷ 12 × 0,9 (pajak final 10%). Reksa dana: harga manual (tidak ada sumber gratis yang andal).
  - Biaya broker otomatis (`Brokers`, `Prefs.broker`): Ajaib, Stockbit, IPOT, BNI, Mirae, Tanpa biaya; mengisi biaya beli/jual saham sampai pengguna mengetik sendiri.
- Utang & piutang (`data/Debts.kt`, `ui/screens/DebtScreen.kt`, tabel `debts` + `debt_pays`, DB v4 `MIGRATION_3_4`). Arisan sengaja tidak dibuat (keputusan pemilik).
  - Rute `R.DEBTS`, `R.debt(id)`; masuk dari kartu ringkas di Aset dan Saya › Kelola. Piutang/utang ikut kekayaan bersih.
  - Cicilan bulanan terjadwal (`installments`, `firstDue`), status lewat jatuh tempo, "Ingatkan via WA" (wa.me, pengguna yang menekan kirim).
  - Patungan dari `RecordDetailCard` (`onSplit`, `SplitSheet`): catatan pengeluaran jadi bagian sendiri, bagian teman jadi piutang lewat transfer dari akun yang sama sehingga saldo tidak berubah.

**Belum dibuat** (rencana berikutnya, urut prioritas):
1. **Zakat & sedekah:** kalkulator sederhana dari aset, dibuka dari "Kelola".
2. **Foto struk** di transaksi, ditampilkan sebagai thumbnail di lembar detail. Setelah itu OCR struk on-device (ML Kit).
6. **Widget layar utama:** sisa uang dan jatah harian, memakai gaya kartu kuning.
7. **Ekspor PDF laporan bulanan.**
8. **Deteksi notifikasi transaksi** on-device (izin notification listener).
9. **Gamifikasi ringan:** streak mencatat dan lencana, tetap tanpa maskot.
10. **Mode gelap:** butuh token gelap tersendiri; jangan sekadar membalik warna.

## 7. Struktur kode dan data

- `data/Model.kt`: entitas Room (Account, Category dengan `parentId` untuk subkategori, Txn, Goal, Bill, Ledger, Budget), DAO, dan `MIGRATION_1_2`. Versi DB 2.
- `data/AppViewModel.kt`: `AppData` dengan fungsi bantu `forLedger`, `spentMonth`, `incomeMonth`, `budgetTotal`, `safePerDay`, `spentFor(budget)`, `rootOf`, `subsOf`, `balanceOf`, dll. Ada juga aksi VM: save/delete untuk setiap entitas, `payBill`, `toggleStar`, `setReimburse`, `startAdd`, ekspor/impor JSON (kunci `"app": "kantong"` dipertahankan agar cadangan lama tetap terbaca).
- `data/Prefs.kt`: preferensi berbentuk Compose state (buku kas aktif, sembunyikan nominal, opsi tampilan baris, PIN, pengingat, cadangan otomatis).
- `data/Format.kt`: `grouped()`, `rupiah()`, `short()`, `fmt()`, rentang periode.
- **Performa:**
  - Tab dibiarkan tetap tersusun (`TabsHost`) dan diparkir di luar layar.
  - Ikon digambar sekali ke bitmap cache.
  - Tidak ada animasi transisi halaman; dulu terasa berat, jangan ditambah lagi.
  - Overlay memakai `OverlayHost` di dalam aplikasi, bukan Dialog sistem.
- **Kepadatan layar:** `MainActivity` menskalakan density supaya lebar layar sama dengan 390dp. Desain dibuat untuk lebar 390.

## 8. Build dan pasang (Windows)

**Build:**

```bash
cd /d/Koding/Catet
export JAVA_HOME="D:/AndroidDev/jdk17"
/d/AndroidDev/gradle-8.9/bin/gradle compileReleaseKotlin --no-daemon -q   # cek cepat, sekitar 2 menit
/d/AndroidDev/gradle-8.9/bin/gradle assembleRelease --no-daemon -q        # APK, 6-10 menit
```

**Pasang:**
- Perintah: `D:/AndroidDev/sdk/platform-tools/adb.exe install -r app/build/outputs/apk/release/app-release.apk`
- HP OPPO menampilkan layar "Lanjutkan pemasangan"; ketuk tombolnya (sekitar 360,1368).
- **Sebelum mengetuk lewat `adb shell input`, pastikan `dumpsys window` → `mCurrentFocus` berisi `id.kantong.app`.** Aplikasi lain bisa sedang terbuka di depan.
- Jangan mengubah setelan sistem HP dan jangan memasukkan PIN pengguna.

## 9. Cara kerja yang diharapkan

1. Baca layar dan komponen terkait sebelum mengedit.
2. Ubah sesedikit mungkin, dan pakai ulang komponen bersama.
3. Build, pasang, lalu ambil tangkapan layar dan bandingkan dengan mockup di `desain/`.
4. Kalau ragu soal gaya, ikuti R89 (`referensi/Referensi-23.png`, R89) dan mockup. Jangan menebak gaya baru.
5. Laporkan ke pengguna dalam bahasa Indonesia yang singkat: apa yang berubah, apa yang belum dicek.
