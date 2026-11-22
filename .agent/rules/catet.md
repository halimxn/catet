---
trigger: always_on
---

# catet: aturan selalu aktif

- Sebelum mengubah kode atau desain, baca `AGENTS.md` di root proyek. Isinya: sistem desain, komponen, aturan penempatan per layar, status fitur, cara build.
- Gaya visual mengikuti referensi R89 dan mockup di `desain/`. Fungsi mengikuti pola Meow Expense. Jangan membuat gaya baru.
- Warna hanya dari token `K` (`ui/theme/Theme.kt`), font Nunito, ikon dengan `CatIcon(key)`, komponen dari `ui/components/Catet.kt` dan `Meow.kt`.
- Tidak boleh ada babi dalam bentuk apa pun (gambar, ikon, kata "celengan"/"celeng"). Tidak boleh ada maskot.
- Teks UI dalam bahasa Indonesia. Nominal ditulis "Rp7.004.500"; di daftar tanpa "Rp".
- Jangan mengganti `applicationId`/paket `id.kantong.app`. Perubahan database wajib memakai Migration.
- Jangan menghapus fitur yang sudah ada; boleh dipindah asal tetap terjangkau.
- Setiap perubahan tampilan dicek di HP (lebar ±360dp) dan dibandingkan dengan mockup sebelum dinyatakan selesai.
