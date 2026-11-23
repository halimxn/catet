package id.kantong.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException
import java.security.MessageDigest

/**
 * Automatic backup to one file the user picked with the system picker (Google Drive or any other storage).
 * The file holds the same JSON as [AppViewModel.exportTo] (version 2), so "Pulihkan cadangan" can read it.
 * It is rewritten when the app goes to the background and the data changed since the last write.
 */
object AutoBackup {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()

    /** Result of a write: null = success, otherwise a short Indonesian reason. */
    suspend fun writeNow(ctx: Context): String? = withContext(Dispatchers.IO) {
        lock.withLock { write(ctx.applicationContext, force = true) }
    }

    /** Call when the app goes to the background (activity ON_STOP). Writes only if sync is on and data changed. */
    fun onAppBackground(ctx: Context) {
        if (!Prefs.autoBackup || Prefs.autoBackupUri.isEmpty()) return
        val app = ctx.applicationContext
        scope.launch { if (!lock.isLocked) lock.withLock { write(app, force = false) } }
    }

    private suspend fun write(ctx: Context, force: Boolean): String? {
        val uriText = Prefs.autoBackupUri
        if (uriText.isEmpty()) return "Belum ada file dipilih"
        val uri = Uri.parse(uriText)
        val data = dataJson(ctx)
        val sig = signature(data)
        if (!force && sig == Prefs.backupSig && Prefs.backupError.isEmpty()) return null
        val hasPermission = ctx.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
        val error: String? = if (!hasPermission) "izin file hilang" else try {
            val text = data.put("exportedAt", System.currentTimeMillis()).toString(2).toByteArray()
            writeBytes(ctx, uri, text)
            null
        } catch (e: SecurityException) {
            "izin file hilang"
        } catch (e: FileNotFoundException) {
            "file tidak ditemukan"
        } catch (e: Exception) {
            e.message?.take(80) ?: "file tidak bisa ditulis"
        }
        withContext(Dispatchers.Main) {
            if (error == null) {
                Prefs.lastBackupAt = System.currentTimeMillis()
                Prefs.backupSig = sig
                Prefs.backupError = ""
            } else {
                Prefs.backupError = error
            }
        }
        return error
    }

    /** "wt" truncates; a few providers only accept "w", so fall back to it. */
    private fun writeBytes(ctx: Context, uri: Uri, bytes: ByteArray) {
        val stream = try {
            ctx.contentResolver.openOutputStream(uri, "wt")
        } catch (e: SecurityException) {
            throw e
        } catch (e: FileNotFoundException) {
            throw e
        } catch (e: Exception) {
            ctx.contentResolver.openOutputStream(uri, "w")
        }
        val out = stream ?: throw FileNotFoundException()
        out.use { it.write(bytes) }
    }

    /** Remembers [uri] (from CreateDocument) as the sync file, keeps access across restarts and turns sync on. */
    fun choose(ctx: Context, uri: Uri) {
        val cr = ctx.contentResolver
        cr.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val old = Prefs.autoBackupUri
        if (old.isNotEmpty() && old != uri.toString()) release(ctx, old)
        Prefs.autoBackupUri = uri.toString()
        Prefs.backupSig = ""
        Prefs.backupError = ""
        Prefs.lastBackupAt = 0L
        Prefs.autoBackup = true
    }

    /** Stops syncing and forgets the file (the file itself stays where it is). */
    fun forget(ctx: Context) {
        val old = Prefs.autoBackupUri
        if (old.isNotEmpty()) release(ctx, old)
        Prefs.autoBackup = false
        Prefs.autoBackupUri = ""
        Prefs.backupSig = ""
        Prefs.backupError = ""
        Prefs.lastBackupAt = 0L
    }

    private fun release(ctx: Context, uriText: String) {
        try {
            ctx.contentResolver.releasePersistableUriPermission(
                Uri.parse(uriText), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: Exception) {
        }
    }

    /** Same structure as [AppViewModel.exportTo], without "exportedAt" (added at write time). */
    private suspend fun dataJson(ctx: Context): JSONObject {
        val dao = Db.get(ctx).dao()
        val root = JSONObject().put("app", "kantong").put("version", 2)
        root.put("accounts", JSONArray(dao.allAccounts().sortedBy { it.id }.map { JSONObject().put("id", it.id).put("name", it.name).put("type", it.type).put("openingBalance", it.openingBalance).put("tone", it.tone).put("icon", it.icon).put("remark", it.remark) }))
        root.put("categories", JSONArray(dao.allCategories().sortedBy { it.id }.map { JSONObject().put("id", it.id).put("name", it.name).put("icon", it.icon).put("tone", it.tone).put("kind", it.kind).put("budget", it.budget).put("sortOrder", it.sortOrder).put("parentId", it.parentId) }))
        root.put("transactions", JSONArray(dao.allTransactions().sortedBy { it.id }.map { JSONObject().put("id", it.id).put("amount", it.amount).put("kind", it.kind).put("categoryId", it.categoryId ?: JSONObject.NULL).put("accountId", it.accountId).put("toAccountId", it.toAccountId ?: JSONObject.NULL).put("merchant", it.merchant).put("note", it.note).put("time", it.time).put("ledgerId", it.ledgerId).put("reimburse", it.reimburse).put("starred", it.starred).put("fee", it.fee) }))
        root.put("ledgers", JSONArray(dao.allLedgers().sortedBy { it.id }.map { JSONObject().put("id", it.id).put("name", it.name).put("icon", it.icon).put("tone", it.tone).put("archived", it.archived).put("sortOrder", it.sortOrder) }))
        root.put("budgets", JSONArray(dao.allBudgets().sortedBy { it.id }.map { JSONObject().put("id", it.id).put("name", it.name).put("period", it.period).put("categoryIds", it.categoryIds).put("amount", it.amount).put("start", it.start).put("end", it.end) }))
        root.put("goals", JSONArray(dao.allGoals().sortedBy { it.id }.map { JSONObject().put("id", it.id).put("name", it.name).put("icon", it.icon).put("tone", it.tone).put("target", it.target).put("saved", it.saved).put("deadline", it.deadline ?: JSONObject.NULL) }))
        root.put("bills", JSONArray(dao.allBills().sortedBy { it.id }.map { JSONObject().put("id", it.id).put("name", it.name).put("amount", it.amount).put("dueDay", it.dueDay).put("categoryId", it.categoryId ?: JSONObject.NULL).put("icon", it.icon).put("tone", it.tone).put("paidMonthKey", it.paidMonthKey) }))
        return root
    }

    /** Hash of the whole data (any edit changes it, not only counts and sums). */
    private fun signature(data: JSONObject): String =
        MessageDigest.getInstance("SHA-256").digest(data.toString().toByteArray()).joinToString("") { "%02x".format(it) }

    // ---------- CSV export (Excel / Google Sheets) ----------

    /** Writes every record as CSV: comma separated, every field quoted, UTF-8 with BOM so Excel shows Indonesian text. */
    suspend fun exportCsv(ctx: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val dao = Db.get(ctx).dao()
        val cats = dao.allCategories().associateBy { it.id }
        val accounts = dao.allAccounts().associateBy { it.id }
        val ledgers = dao.allLedgers().associateBy { it.id }
        val sb = StringBuilder()
        fun row(vararg cells: String) {
            sb.append(cells.joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" }).append("\r\n")
        }
        row("Tanggal", "Jam", "Jenis", "Kategori", "Subkategori", "Akun", "Ke akun", "Buku kas", "Nominal", "Biaya", "Catatan", "Reimburse", "Bookmark")
        dao.allTransactions().sortedBy { it.time }.forEach { t ->
            val c = t.categoryId?.let { cats[it] }
            val main = if (c != null && c.parentId != 0L) cats[c.parentId] else c
            val sub = if (c != null && c.parentId != 0L) c else null
            row(
                fmt(t.time, "yyyy-MM-dd"),
                fmt(t.time, "HH:mm"),
                when (t.kind) { "in" -> "Pemasukan"; "transfer" -> "Transfer"; else -> "Pengeluaran" },
                if (t.kind == "transfer") "" else main?.name ?: "Tanpa kategori",
                sub?.name ?: "",
                accounts[t.accountId]?.name ?: "",
                t.toAccountId?.let { accounts[it]?.name } ?: "",
                ledgers[t.ledgerId]?.name ?: "",
                t.amount.toString(),
                if (t.fee > 0) t.fee.toString() else "",
                listOf(t.merchant, t.note).filter { it.isNotBlank() }.joinToString(" - "),
                when (t.reimburse) { 1 -> "Menunggu"; 2 -> "Sudah diganti"; else -> "" },
                if (t.starred) "Ya" else "",
            )
        }
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        writeBytes(ctx, uri, bom + sb.toString().toByteArray(Charsets.UTF_8))
    }

    // ---------- Hapus semua data ----------

    /**
     * Deletes every record, account, budget, goal, bill and ledger, then puts back what the app needs to work:
     * the ledger "Harian" (id 1, the default of new records), the default categories and one empty cash account.
     * Automatic sync is switched off so the backup file is not overwritten with the empty data.
     */
    suspend fun wipeAll(ctx: Context) = withContext(Dispatchers.IO) {
        val dao = Db.get(ctx).dao()
        dao.clearTxns(); dao.clearBills(); dao.clearGoals(); dao.clearBudgets()
        dao.clearCategories(); dao.clearAccounts(); dao.clearLedgers()
        dao.clearHoldings(); dao.clearInvTxns(); dao.clearDebts(); dao.clearDebtPays(); dao.clearDetected()
        dao.insertLedgers(listOf(Ledger(1, "Harian", "ledger", "PEACH", sortOrder = 1)))
        dao.insertCategories(Seed.categories())
        dao.insertAccounts(listOf(Account(name = "Dompet", type = "cash", openingBalance = 0, tone = "BUTTER")))
        withContext(Dispatchers.Main) {
            Prefs.ledger = 0L
            Prefs.autoBackup = false
        }
    }
}
