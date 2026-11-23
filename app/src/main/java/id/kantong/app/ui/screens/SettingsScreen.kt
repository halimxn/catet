package id.kantong.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DriveFolderUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import id.kantong.app.data.AppData
import id.kantong.app.data.AppViewModel
import id.kantong.app.data.AutoBackup
import id.kantong.app.data.Prefs
import id.kantong.app.data.Reminder
import id.kantong.app.data.Txn
import id.kantong.app.data.dayOfMonth
import id.kantong.app.data.fmt
import id.kantong.app.data.monthKey
import id.kantong.app.data.rupiah
import id.kantong.app.ui.Nav
import id.kantong.app.ui.R
import id.kantong.app.ui.components.CCard
import id.kantong.app.ui.components.CGroupLabel
import id.kantong.app.ui.components.CList
import id.kantong.app.ui.components.CListRow
import id.kantong.app.ui.components.CSection
import id.kantong.app.ui.components.CTitle
import id.kantong.app.ui.components.CatIcon
import id.kantong.app.ui.components.CatetLogo
import id.kantong.app.ui.components.CreamButton
import id.kantong.app.ui.components.FloatingCard
import id.kantong.app.ui.components.MeowToggle
import id.kantong.app.ui.components.Overlay
import id.kantong.app.ui.components.RecordTile
import id.kantong.app.ui.components.RedButton
import id.kantong.app.ui.components.RedFirst
import id.kantong.app.ui.components.SheetCard
import id.kantong.app.ui.components.TimePickerCard
import id.kantong.app.ui.components.pressable
import id.kantong.app.ui.theme.K
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class BackupStatus(val ok: Boolean, val title: String, val message: String)

/**
 * "Saya" tab: catet logo, title, the yellow backup card (Pulihkan / Cadangkan), the "Kelola" list
 * (buku kas, kategori, berulang, reimburse, bookmark, tabungan, anggaran) and every setting grouped
 * under "Pengaturan".
 */
@Composable
fun SettingsScreen(d: AppData, vm: AppViewModel, nav: Nav) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var about by remember { mutableStateOf(false) }
    var style by remember { mutableStateOf(false) }
    var budget by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<BackupStatus?>(null) }
    var pinMode by remember { mutableStateOf<PinMode?>(null) }
    var notifDenied by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    fun syncNow(showResult: Boolean) {
        if (syncing) return
        syncing = true
        scope.launch {
            val error = AutoBackup.writeNow(ctx)
            syncing = false
            if (showResult) status = if (error == null) {
                BackupStatus(true, "Sinkron otomatis aktif", "Data sudah tersimpan di file pilihan Anda dan akan diperbarui otomatis setiap kali catet ditutup.")
            } else {
                BackupStatus(false, "Sinkron gagal", "Gagal: $error")
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) scope.launch {
            status = try {
                vm.exportTo(uri)
                BackupStatus(true, "Cadangan tersimpan", "Semua data sudah tersimpan di file. Simpan juga salinannya di Google Drive agar aman.")
            } catch (e: Exception) {
                BackupStatus(false, "Cadangan gagal", e.message ?: "File tidak bisa ditulis.")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            status = try {
                vm.importFrom(uri)
                BackupStatus(true, "Data dipulihkan", "Semua data sekarang berasal dari file cadangan.")
            } catch (e: Exception) {
                BackupStatus(false, "Gagal memulihkan", e.message ?: "File cadangan tidak valid.")
            }
        }
    }
    val syncLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            try {
                AutoBackup.choose(ctx, uri)
                syncNow(showResult = true)
            } catch (e: Exception) {
                status = BackupStatus(false, "File tidak bisa dipakai", "Lokasi ini tidak mengizinkan akses tetap. Pilih Google Drive atau folder lain.")
            }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) scope.launch {
            status = try {
                AutoBackup.exportCsv(ctx, uri)
                BackupStatus(true, "CSV tersimpan", "Buka file ini di Excel atau Google Sheets.")
            } catch (e: Exception) {
                BackupStatus(false, "Ekspor gagal", e.message ?: "File tidak bisa ditulis.")
            }
        }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) Reminder.schedule(ctx, Prefs.reminderHour, Prefs.reminderMinute) else { Reminder.cancel(ctx); notifDenied = true }
    }

    fun pickSyncFile() { AppLock.allowLeave(); syncLauncher.launch("catet-sinkron.json") }
    fun backupToFile() { AppLock.allowLeave(); exportLauncher.launch("catet-backup-${fmt(System.currentTimeMillis(), "yyyyMMdd")}.json") }
    fun setAutoBackup(on: Boolean) {
        when {
            !on -> Prefs.autoBackup = false
            Prefs.autoBackupUri.isEmpty() -> pickSyncFile()
            else -> { Prefs.autoBackup = true; syncNow(showResult = false) }
        }
    }
    fun setReminder(on: Boolean) {
        when {
            !on -> Reminder.cancel(ctx)
            Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ->
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            !NotificationManagerCompat.from(ctx).areNotificationsEnabled() -> notifDenied = true
            else -> Reminder.schedule(ctx, Prefs.reminderHour, Prefs.reminderMinute)
        }
    }
    fun pickTime() {
        showTimePicker = true
    }

    val time = "%02d.%02d".format(Prefs.reminderHour, Prefs.reminderMinute)
    val syncUri = Prefs.autoBackupUri
    val fileName by produceState<String?>(null, syncUri) { value = if (syncUri.isEmpty()) null else displayName(ctx, syncUri) }
    val syncFailed = Prefs.backupError.isNotEmpty() && !syncing
    val syncStatus = when {
        syncing -> "Menyinkronkan…"
        Prefs.backupError.isNotEmpty() -> "Gagal: ${Prefs.backupError}"
        Prefs.lastBackupAt > 0 -> "Terakhir disinkron: ${fmt(Prefs.lastBackupAt, "d MMM, HH.mm")}"
        else -> "Belum pernah disinkron"
    }
    val autoOn = Prefs.autoBackup && syncUri.isNotEmpty()

    // live counts for the "Kelola" list
    val thisMonth = monthKey()
    val today = dayOfMonth()
    val waitingBills = d.bills.count { it.paidMonthKey != thisMonth && it.dueDay <= today }
    val reimburseLeft = d.toReimburse.sumOf { it.amount }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.clip(RoundedCornerShape(12.dp)).pressable { about = true }, verticalAlignment = Alignment.CenterVertically) {
            CatetLogo(34.dp)
            Spacer(Modifier.width(8.dp))
            Text("catet", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
        }
        Spacer(Modifier.height(18.dp))
        CTitle("Saya", subtitle = "Kelola buku kas, kategori, dan cadangan")
        Spacer(Modifier.height(16.dp))

        // backup card
        CCard(K.Yellow, radius = 26.dp, padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CatIcon("ledger", 30.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    val title = when {
                        !autoOn -> "Cadangan otomatis mati"
                        syncFailed -> "Cadangan otomatis gagal"
                        else -> "Cadangan otomatis aktif"
                    }
                    val sub = when {
                        !autoOn -> "Cadangkan ke file agar data aman"
                        syncing -> "Menyinkronkan…"
                        syncFailed -> "Gagal: ${Prefs.backupError}"
                        Prefs.lastBackupAt > 0 -> "Terakhir ${fmt(Prefs.lastBackupAt, "EEEE, d MMM · HH.mm")}"
                        else -> "Belum pernah dicadangkan"
                    }
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        sub, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (syncFailed && autoOn) K.Danger else K.Muted,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                CreamButton("Pulihkan", Modifier.weight(1f).height(44.dp)) { confirmRestore = true }
                RedButton("Cadangkan", Modifier.weight(1f).height(44.dp)) { if (autoOn) syncNow(showResult = true) else backupToFile() }
            }
        }

        // manage
        Spacer(Modifier.height(24.dp))
        CSection("Kelola")
        Spacer(Modifier.height(10.dp))
        CList {
            CListRow("ledger", "Buku kas", "${d.activeLedgers.size} buku kas", divider = false, onClick = { nav.go(R.LEDGERS) })
            CListRow("category", "Kategori", "${d.categories.size} kategori", onClick = { nav.go(R.CATEGORIES) })
            CListRow(
                "recurring", "Transaksi berulang",
                if (d.bills.isEmpty()) "Tagihan dan catatan rutin"
                else "${d.bills.size} aktif" + if (waitingBills > 0) " · $waitingBills menunggu" else "",
                onClick = { nav.go(R.RECURRING) },
            )
            CListRow(
                "budget", "Anggaran",
                if (d.budgets.isEmpty()) "Batas belanja per periode" else "${d.budgets.size} anggaran",
                onClick = { budget = true },
            )
            CListRow(
                "reimburse", "Reimburse",
                if (reimburseLeft > 0) "${reimburseLeft.rupiah()} belum kembali" else "Tidak ada yang menunggu",
                onClick = { nav.go(R.REIMBURSE) },
            )
            CListRow(
                "card", "Utang & piutang",
                if (d.receivable == 0L && d.payable == 0L) "Pinjaman dan patungan" else "Piutang ${d.receivable.rupiah()} · utang ${d.payable.rupiah()}",
                onClick = { nav.go(R.DEBTS) },
            )
            CListRow("bookmark", "Bookmark", "${d.starred.size} transaksi", onClick = { nav.go(R.BOOKMARKS) })
            CListRow(
                "jar", "Target tabungan",
                if (d.goals.isEmpty()) "Target menabung" else "${d.goals.size} target",
                onClick = { nav.go(R.GOALS) },
            )
        }

        // settings
        Spacer(Modifier.height(24.dp))
        CSection("Pengaturan")

        SetGroup("Keamanan & pengingat") {
            SetRow(
                Icons.Rounded.Lock, "Kunci PIN",
                if (AppLock.enabled) "PIN diminta saat membuka catet" else "Minta PIN 4 angka saat membuka catet",
                divider = false, onClick = { pinMode = if (AppLock.enabled) PinMode.DISABLE else PinMode.SET },
                trailing = { MeowToggle(AppLock.enabled) { pinMode = if (it) PinMode.SET else PinMode.DISABLE } },
            )
            if (AppLock.enabled) {
                SetRow(Icons.Rounded.Key, "Ganti PIN", "Masukkan PIN lama, lalu PIN baru", onClick = { pinMode = PinMode.CHANGE })
            }
            SetRow(
                Icons.Rounded.NotificationsActive, "Pengingat harian",
                if (Prefs.reminder) "Setiap hari pukul $time" else "Ingatkan saya mencatat setiap hari",
                onClick = { setReminder(!Prefs.reminder) },
                trailing = { MeowToggle(Prefs.reminder) { setReminder(it) } },
            )
            SetRow(Icons.Rounded.Schedule, "Jam pengingat", "Pukul $time", onClick = { pickTime() })
        }

        SetGroup("Tampilan") {
            SetRow(Icons.Rounded.Palette, "Gaya tampilan catatan", "Atur tampilan daftar catatan", divider = false, onClick = { style = true })
            SetRow(
                Icons.Rounded.VisibilityOff, "Sembunyikan nominal", "Ganti angka dengan titik-titik",
                onClick = { Prefs.hideAmounts = !Prefs.hideAmounts },
                trailing = { MeowToggle(Prefs.hideAmounts) { Prefs.hideAmounts = it } },
            )
            SetRow(
                Icons.Rounded.Vibration, "Getar keyboard", "Getar saat mengetuk tombol angka",
                onClick = { Prefs.haptics = !Prefs.haptics },
                trailing = { MeowToggle(Prefs.haptics) { Prefs.haptics = it } },
            )
        }

        SetGroup("Cadangan & sinkron") {
            SetRow(
                Icons.Rounded.CloudSync, "Sinkron otomatis",
                "File disimpan di Google Drive/penyimpanan pilihan Anda dan diperbarui otomatis",
                divider = false, onClick = { setAutoBackup(!Prefs.autoBackup) },
                trailing = { MeowToggle(Prefs.autoBackup) { setAutoBackup(it) } },
            )
            if (syncUri.isEmpty()) {
                SetRow(
                    Icons.Rounded.DriveFolderUpload, "Pilih file cadangan",
                    "Pilih Google Drive dan akun Anda di layar berikutnya", onClick = { pickSyncFile() },
                )
            } else {
                SetRow(
                    Icons.Rounded.Sync, "Sinkron sekarang", syncStatus,
                    subtitleColor = if (syncFailed) K.Danger else K.Muted,
                    onClick = { syncNow(showResult = false) },
                )
                SetRow(
                    Icons.Rounded.FolderOpen, "Ganti file",
                    fileName?.let { "Sekarang: $it" } ?: "Pilih lokasi atau akun Google lain", onClick = { pickSyncFile() },
                )
                SetRow(
                    Icons.Rounded.CloudOff, "Matikan", "Berhenti sinkron dan lupakan file ini (filenya tidak dihapus)",
                    onClick = { AutoBackup.forget(ctx) },
                )
            }
            SetRow(
                Icons.Rounded.CloudUpload, "Buat cadangan", "Simpan semua data ke satu file, misalnya di Google Drive",
                onClick = { backupToFile() },
            )
            SetRow(
                Icons.Rounded.CloudDownload, "Pulihkan cadangan", "Ganti semua data dengan isi file cadangan",
                onClick = { confirmRestore = true },
            )
            SetRow(
                Icons.Rounded.PhoneAndroid, "Cadangan Android (Google)",
                "Android juga mencadangkan data aplikasi ke akun Google Anda secara otomatis bila Cadangan Google aktif di HP",
            )
        }

        SetGroup("Data") {
            SetRow(
                Icons.Rounded.TableChart, "Ekspor ke CSV (Excel)", "Semua catatan dalam satu file untuk Excel atau Google Sheets",
                divider = false,
                onClick = { AppLock.allowLeave(); csvLauncher.launch("catet-catatan-${fmt(System.currentTimeMillis(), "yyyyMMdd")}.csv") },
            )
            SetRow(
                Icons.Rounded.DeleteForever, "Hapus semua data", "Hapus semua catatan, akun, anggaran, dan tabungan",
                titleColor = K.Danger, iconTint = K.Danger, onClick = { confirmWipe = true },
            )
        }

        SetGroup("Tentang") {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 62.dp).pressable { about = true }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                CatetLogo(32.dp)
                Column(Modifier.weight(1f)) {
                    Text("catet 1.0", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = K.Ink)
                    Text("Gratis, tanpa iklan. Data tersimpan di HP", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = K.Muted)
                }
            }
        }
        // room for the bottom tab bar
        Spacer(Modifier.height(100.dp))
    }

    if (about) {
        Overlay({ about = false }) {
            FloatingCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CatetLogo(30.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Tentang catet", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = K.Ink)
                }
                Spacer(Modifier.height(8.dp))
                DialogText("catet mencatat keuangan Anda tanpa akun dan tanpa iklan. Semua data tersimpan di HP ini; aktifkan sinkron otomatis atau buat cadangan agar aman.")
                Spacer(Modifier.height(16.dp))
                RedButton("Oke", Modifier.fillMaxWidth()) { about = false }
            }
        }
    }

    if (style) RecordStyleSheet(d) { style = false }

    if (budget) BudgetSheet(d, vm) { budget = false }

    pinMode?.let { mode -> PinSheet(mode, onDismiss = { pinMode = null }, onDone = { pinMode = null }) }

    if (confirmRestore) {
        Overlay({ confirmRestore = false }) {
            FloatingCard {
                RedFirst("Pulihkan cadangan", 20.sp)
                Spacer(Modifier.height(8.dp))
                DialogText("Semua data sekarang akan diganti. Lanjutkan?")
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CreamButton("Batal", Modifier.weight(1f)) { confirmRestore = false }
                    RedButton("Pulihkan", Modifier.weight(1f)) {
                        confirmRestore = false
                        AppLock.allowLeave()
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }
                }
            }
        }
    }

    if (notifDenied) {
        Overlay({ notifDenied = false }) {
            FloatingCard {
                RedFirst("Notifikasi mati", 20.sp)
                Spacer(Modifier.height(8.dp))
                DialogText("Pengingat harian butuh izin notifikasi. Izinkan notifikasi catet di pengaturan HP, lalu nyalakan lagi pengingatnya.")
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CreamButton("Nanti", Modifier.weight(1f)) { notifDenied = false }
                    RedButton("Buka pengaturan", Modifier.weight(1f)) {
                        notifDenied = false
                        AppLock.allowLeave()
                        try {
                            ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    if (confirmWipe) {
        var typed by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        Overlay({ if (!busy) confirmWipe = false }) {
            FloatingCard {
                RedFirst("Hapus semua data", 20.sp)
                Spacer(Modifier.height(8.dp))
                DialogText("Semua catatan, akun, anggaran, tabungan, tagihan, dan buku kas akan dihapus. Ini tidak bisa dibatalkan, jadi buat cadangan dulu bila perlu.")
                Spacer(Modifier.height(12.dp))
                Text("Ketik HAPUS untuk melanjutkan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = K.Ink)
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = typed,
                    onValueChange = { typed = it.take(10) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Ink),
                    cursorBrush = SolidColor(K.Green),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.Field).padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            if (typed.isEmpty()) Text("HAPUS", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = K.Muted.copy(alpha = .5f))
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CreamButton("Batal", Modifier.weight(1f)) { if (!busy) confirmWipe = false }
                    RedButton("Hapus semua", Modifier.weight(1f), enabled = typed.trim() == "HAPUS" && !busy) {
                        busy = true
                        val wasSyncing = Prefs.autoBackup
                        scope.launch {
                            status = try {
                                AutoBackup.wipeAll(ctx)
                                BackupStatus(
                                    true, "Semua data dihapus",
                                    "catet sekarang kosong. Kategori bawaan, buku kas Harian, dan akun Dompet sudah disiapkan." +
                                        if (wasSyncing) " Sinkron otomatis dimatikan agar file cadangan Anda tidak tertimpa." else "",
                                )
                            } catch (e: Exception) {
                                BackupStatus(false, "Gagal menghapus", e.message ?: "Coba lagi.")
                            }
                            busy = false
                            confirmWipe = false
                        }
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerCard(
            initialHour = Prefs.reminderHour,
            initialMinute = Prefs.reminderMinute,
            onDismiss = { showTimePicker = false },
        ) { h, m ->
            if (Prefs.reminder) Reminder.schedule(ctx, h, m) else { Prefs.reminderHour = h; Prefs.reminderMinute = m }
            showTimePicker = false
        }
    }

    status?.let { s ->
        Overlay({ status = null }) {
            FloatingCard {
                Text(s.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (s.ok) K.Ink else K.Danger)
                Spacer(Modifier.height(8.dp))
                DialogText(s.message)
                Spacer(Modifier.height(16.dp))
                RedButton("Oke", Modifier.fillMaxWidth()) { status = null }
            }
        }
    }
}

/** Name of the picked sync file as the storage app reports it (e.g. "catet-sinkron.json"). */
private suspend fun displayName(ctx: Context, uri: String): String? = withContext(Dispatchers.IO) {
    try {
        ctx.contentResolver.query(Uri.parse(uri), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun DialogText(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink2, lineHeight = 22.sp)
}

/** Small grey label + a cream list of setting rows. */
@Composable
private fun SetGroup(label: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(16.dp))
    CGroupLabel(label)
    Spacer(Modifier.height(8.dp))
    CList { content() }
}

/** Setting row in a CList (same look as CListRow, with a glyph icon and a subtitle that may wrap). */
@Composable
private fun SetRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
    titleColor: Color = K.Ink,
    subtitleColor: Color = K.Muted,
    iconTint: Color = K.Ink,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column {
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(K.Line))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 62.dp).let { if (onClick != null) it.pressable(onClick) else it }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, color = subtitleColor)
            }
            when {
                trailing != null -> trailing()
                onClick != null -> Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA39D90))
            }
        }
    }
}

/**
 * "Gaya tampilan catatan": a live preview of a record tile and switches for each part of it.
 */
@Composable
fun RecordStyleSheet(d: AppData, onDismiss: () -> Unit) {
    val now = remember { System.currentTimeMillis() }
    val sample = d.allTxns.filter { it.kind == "out" }.maxByOrNull { it.time } ?: Txn(
        amount = 25_000, kind = "out", categoryId = d.topCategories("out").firstOrNull()?.id,
        accountId = d.accounts.firstOrNull()?.id ?: 0, merchant = "Contoh catatan", time = now,
        ledgerId = d.activeLedgers.firstOrNull()?.id ?: 1, reimburse = 1,
    )
    Overlay(onDismiss) {
        SheetCard {
            RecordTile(d, sample, onClick = {})
            Box(Modifier.padding(vertical = 16.dp).fillMaxWidth().height(1.dp).background(K.Line))
            StyleToggle("Latar berwarna", Prefs.colorBg) { Prefs.colorBg = it }
            StyleToggle("Ikon berwarna", Prefs.colorIcon) { Prefs.colorIcon = it }
            StyleToggle("Tampilkan catatan", Prefs.showRemark) { Prefs.showRemark = it }
            StyleToggle("Tampilkan jam", Prefs.showTime) { Prefs.showTime = it }
            StyleToggle("Ikon akun", Prefs.showAccountIcon) { Prefs.showAccountIcon = it }
            StyleToggle("Ikon buku kas", Prefs.showLedgerIcon) { Prefs.showLedgerIcon = it }
            StyleToggle("Ikon reimburse", Prefs.showReimburseIcon) { Prefs.showReimburseIcon = it }
        }
    }
}

@Composable
private fun StyleToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = K.Ink, modifier = Modifier.weight(1f))
        MeowToggle(checked, onChange)
    }
}
