package id.kantong.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Small user preferences, readable from any composable (they are Compose state) and saved on every change.
 * Call [init] once from MainActivity before setContent.
 */
object Prefs {
    private var sp: SharedPreferences? = null

    private var _ledger by mutableLongStateOf(0L)
    private var _hide by mutableStateOf(false)
    private var _showInvest by mutableStateOf(true)
    private var _broker by mutableStateOf("ajaib")
    private var _ihsg by mutableStateOf("")
    private var _haul by mutableStateOf("0")
    private var _sedekah by mutableStateOf("0")
    private var _zgold by mutableStateOf("0")
    private var _haptics by mutableStateOf(true)
    private var _colorBg by mutableStateOf(false)
    private var _colorIcon by mutableStateOf(true)
    private var _remark by mutableStateOf(true)
    private var _time by mutableStateOf(true)
    private var _accountIcon by mutableStateOf(false)
    private var _ledgerIcon by mutableStateOf(false)
    private var _reimburseIcon by mutableStateOf(true)
    private var _autoBackup by mutableStateOf(false)
    private var _autoBackupUri by mutableStateOf("")
    private var _lastBackupAt by mutableLongStateOf(0L)
    private var _backupSig by mutableStateOf("")
    private var _backupError by mutableStateOf("")
    private var _reminder by mutableStateOf(false)
    private var _onboarded by mutableStateOf(true)
    private var _reminderHour by mutableIntStateOf(20)
    private var _reminderMinute by mutableIntStateOf(0)
    private var _pinHash by mutableStateOf("")
    private var _pinSalt by mutableStateOf("")

    fun init(ctx: Context) {
        val p = ctx.getSharedPreferences("kantong_prefs", Context.MODE_PRIVATE).also { sp = it }
        _ledger = p.getLong("ledger", 0L)
        _hide = p.getBoolean("hide", false)
        _showInvest = p.getBoolean("showInvest", true)
        _broker = p.getString("broker", "ajaib") ?: "ajaib"
        _ihsg = p.getString("ihsg", "") ?: ""
        _haul = p.getString("haul", "0") ?: "0"
        _sedekah = p.getString("sedekah_target", "0") ?: "0"
        _zgold = p.getString("zakat_gold", "0") ?: "0"
        _haptics = p.getBoolean("haptics", true)
        _colorBg = p.getBoolean("style_bg", false)
        _colorIcon = p.getBoolean("style_icon", true)
        _remark = p.getBoolean("style_remark", true)
        _time = p.getBoolean("style_time", true)
        _accountIcon = p.getBoolean("style_account", false)
        _ledgerIcon = p.getBoolean("style_ledger", false)
        _reimburseIcon = p.getBoolean("style_reimburse", true)
        _autoBackup = p.getBoolean("auto_backup", false)
        _autoBackupUri = p.getString("auto_backup_uri", "") ?: ""
        _lastBackupAt = p.getLong("last_backup_at", 0L)
        _backupSig = p.getString("backup_sig", "") ?: ""
        _backupError = p.getString("backup_error", "") ?: ""
        _reminder = p.getBoolean("reminder", false)
        _onboarded = p.getBoolean("onboarded", false)
        _reminderHour = p.getInt("reminder_hour", 20)
        _reminderMinute = p.getInt("reminder_minute", 0)
        _pinHash = p.getString("pin_hash", "") ?: ""
        _pinSalt = p.getString("pin_salt", "") ?: ""
    }

    /** For code that can run without MainActivity (broadcast receivers, background work). */
    fun ensure(ctx: Context) { if (sp == null) init(ctx) }

    private fun put(key: String, v: Boolean) { sp?.edit()?.putBoolean(key, v)?.apply() }
    private fun put(key: String, v: String) { sp?.edit()?.putString(key, v)?.apply() }
    private fun put(key: String, v: Int) { sp?.edit()?.putInt(key, v)?.apply() }

    /** Ledger shown in the headers (0 = all ledgers). */
    var ledger: Long get() = _ledger; set(v) { _ledger = v; sp?.edit()?.putLong("ledger", v)?.apply() }
    /** Hide amounts on the home screen. */
    var hideAmounts: Boolean get() = _hide; set(v) { _hide = v; put("hide", v) }
    /** Show investments on Aset and count them in net worth. */
    var showInvest: Boolean get() = _showInvest; set(v) { _showInvest = v; put("showInvest", v) }
    /** Stock broker whose fee preset fills buy / sell fees. */
    var broker: String get() = _broker; set(v) { _broker = v; put("broker", v) }
    /** Last IHSG quote: "price|prevClose|time". */
    var ihsg: String get() = _ihsg; set(v) { _ihsg = v; put("ihsg", v) }
    /** Zakat: start of the haul year (0 = not set), monthly sedekah target, gold price per gram for the nisab. */
    var haulStart: Long get() = _haul.toLongOrNull() ?: 0L; set(v) { _haul = v.toString(); put("haul", v.toString()) }
    var sedekahTarget: Long get() = _sedekah.toLongOrNull() ?: 0L; set(v) { _sedekah = v.toString(); put("sedekah_target", v.toString()) }
    var zakatGold: Long get() = _zgold.toLongOrNull() ?: 0L; set(v) { _zgold = v.toString(); put("zakat_gold", v.toString()) }
    /** Vibrate on keypad taps. */
    var haptics: Boolean get() = _haptics; set(v) { _haptics = v; put("haptics", v) }

    // Record tile style (Pengaturan > Gaya tampilan catatan)
    var colorBg: Boolean get() = _colorBg; set(v) { _colorBg = v; put("style_bg", v) }
    var colorIcon: Boolean get() = _colorIcon; set(v) { _colorIcon = v; put("style_icon", v) }
    var showRemark: Boolean get() = _remark; set(v) { _remark = v; put("style_remark", v) }
    var showTime: Boolean get() = _time; set(v) { _time = v; put("style_time", v) }
    var showAccountIcon: Boolean get() = _accountIcon; set(v) { _accountIcon = v; put("style_account", v) }
    var showLedgerIcon: Boolean get() = _ledgerIcon; set(v) { _ledgerIcon = v; put("style_ledger", v) }
    var showReimburseIcon: Boolean get() = _reimburseIcon; set(v) { _reimburseIcon = v; put("style_reimburse", v) }

    // Cadangan otomatis: one file (e.g. on Google Drive) picked through the system picker, rewritten automatically.
    var autoBackup: Boolean get() = _autoBackup; set(v) { _autoBackup = v; put("auto_backup", v) }
    /** Persisted SAF uri of the sync file ("" = none picked). */
    var autoBackupUri: String get() = _autoBackupUri; set(v) { _autoBackupUri = v; put("auto_backup_uri", v) }
    /** Time of the last successful write (0 = never). */
    var lastBackupAt: Long get() = _lastBackupAt; set(v) { _lastBackupAt = v; sp?.edit()?.putLong("last_backup_at", v)?.apply() }
    /** Signature of the data written last, to skip unchanged writes. */
    var backupSig: String get() = _backupSig; set(v) { _backupSig = v; put("backup_sig", v) }
    /** Reason of the last failed write ("" = last write succeeded). */
    var backupError: String get() = _backupError; set(v) { _backupError = v; put("backup_error", v) }

    // Pengingat harian
    var reminder: Boolean get() = _reminder; set(v) { _reminder = v; put("reminder", v) }
    /** First-run introduction finished (or skipped). Settings can reset it to show it again. */
    var onboarded: Boolean get() = _onboarded; set(v) { _onboarded = v; put("onboarded", v) }
    var reminderHour: Int get() = _reminderHour; set(v) { _reminderHour = v; put("reminder_hour", v) }
    var reminderMinute: Int get() = _reminderMinute; set(v) { _reminderMinute = v; put("reminder_minute", v) }

    // Kunci aplikasi: salted SHA-256 of the 4-digit PIN ("" = no lock).
    var pinHash: String get() = _pinHash; set(v) { _pinHash = v; put("pin_hash", v) }
    var pinSalt: String get() = _pinSalt; set(v) { _pinSalt = v; put("pin_salt", v) }
}
