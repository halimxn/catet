package id.kantong.app.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import id.kantong.app.MainActivity
import id.kantong.app.R
import id.kantong.app.widget.CatetWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

/** Turns bank / e-wallet notifications into waiting records, and saves or skips them. Everything stays on the device. */
object Detector {
    private const val CHANNEL = "detect"
    private const val KEEP_MS = 60L * 86_400_000L
    private const val REPEAT_MS = 10L * 60_000L

    fun hasAccess(ctx: Context) = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    // banks often post the same notification twice at once: one at a time, so the repeat check sees the first
    private val lock = kotlinx.coroutines.sync.Mutex()

    suspend fun handle(ctx: Context, pkg: String, title: String, text: String, time: Long) = lock.withLock { handleLocked(ctx, pkg, title, text, time) }

    private suspend fun handleLocked(ctx: Context, pkg: String, title: String, text: String, time: Long) {
        val app = DetectApps.of(pkg) ?: return
        if (!Prefs.detectOn || !Prefs.detectEnabled(pkg)) return
        val p = NotifParser.parse(title, text, app.name, app.type) ?: return
        val dao = Db.get(ctx).dao()
        val key = "$pkg|${p.amount}|${p.kind}|${p.merchant.lowercase()}"
        if (dao.detectedSince(key, time - REPEAT_MS) > 0) return
        val cats = dao.allCategories()
        val accs = dao.allAccounts()
        val cat = dao.lastCategoryFor(p.kind, p.merchant)?.takeIf { id -> cats.any { it.id == id } } ?: guessCategory(cats, p)
        val acc = accountFor(app, accs)
        val x = Detected(pkg = pkg, app = app.name, amount = p.amount, kind = p.kind, merchant = p.merchant, time = time, key = key, categoryId = cat, accountId = acc)
        val id = dao.upsertDetected(x)
        dao.pruneDetected(time - KEEP_MS)
        notify(ctx, x.copy(id = id), cats.firstOrNull { it.id == cat }?.name, accs.firstOrNull { it.id == acc }?.name)
        runCatching { CatetWidgets.refresh(ctx) }
    }

    private fun guessCategory(cats: List<Category>, p: NotifParser.Parsed): Long? {
        val pool = cats.filter { it.kind == p.kind }
        val hints = NotifParser.categoryHints(p.merchant, p.kind)
        return hints.firstNotNullOfOrNull { h -> pool.sortedBy { it.parentId }.firstOrNull { it.name.contains(h, true) } }?.id
            ?: pool.firstOrNull { it.parentId == 0L && it.name.contains("lain", true) }?.id
    }

    private fun accountFor(app: DetectApp, accs: List<Account>): Long {
        Prefs.detectAccount(app.pkg).takeIf { id -> accs.any { it.id == id } }?.let { return it }
        val word = app.name.substringBefore(' ').lowercase()
        return accs.firstOrNull { it.name.lowercase().contains(word) || it.name.lowercase().contains(app.short.lowercase()) }?.id
            ?: accs.firstOrNull { it.type == app.type }?.id ?: accs.firstOrNull()?.id ?: 0L
    }

    suspend fun save(ctx: Context, id: Long, categoryId: Long?, accountId: Long) {
        val dao = Db.get(ctx).dao()
        val x = dao.detectedById(id) ?: return
        val accs = dao.allAccounts()
        // the chosen or guessed account may have been deleted since; fall back to one that exists
        val acc = listOf(accountId, x.accountId).firstOrNull { a -> a != 0L && accs.any { it.id == a } } ?: accs.firstOrNull()?.id ?: return
        val cat = (categoryId ?: x.categoryId)?.takeIf { c -> dao.allCategories().any { it.id == c } }
        if (dao.claimDetected(id, 1) == 0) return // already saved or skipped (double tap, notification + sheet)
        dao.upsertTxn(
            Txn(
                amount = x.amount, kind = x.kind, categoryId = cat, accountId = acc, merchant = x.merchant,
                time = x.time, ledgerId = Prefs.ledger.takeIf { it != 0L } ?: 1L,
            ),
        )
        cancel(ctx, id)
        runCatching { CatetWidgets.refresh(ctx) }
    }

    suspend fun skip(ctx: Context, id: Long) {
        Db.get(ctx).dao().claimDetected(id, 2)
        cancel(ctx, id)
        runCatching { CatetWidgets.refresh(ctx) }
    }

    private fun nid(id: Long) = 5000 + (id % 100_000).toInt()
    private fun cancel(ctx: Context, id: Long) = NotificationManagerCompat.from(ctx).cancel(nid(id))

    private fun notify(ctx: Context, x: Detected, cat: String?, acc: String?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Transaksi terdeteksi", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Transaksi dari notifikasi bank dan e-wallet, siap dicatat"
                },
            )
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        fun action(a: String) = PendingIntent.getBroadcast(
            ctx, nid(x.id) * 4 + if (a == "save") 1 else 2,
            Intent(ctx, DetectActionReceiver::class.java).setAction("id.kantong.app.DETECT_$a").putExtra("id", x.id), flags,
        )
        val review = PendingIntent.getActivity(
            ctx, nid(x.id) * 4 + 3,
            Intent(ctx, MainActivity::class.java).putExtra("catet_action", "review").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), flags,
        )
        val sign = if (x.kind == "in") "+" else "−"
        val body = listOfNotNull(acc ?: x.app, cat).joinToString(" → ")
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_fg)
            .setColor(0xFF2E5242.toInt())
            .setContentTitle("${sign}Rp${x.amount.grouped()} · ${x.merchant}")
            .setContentText(body)
            .setContentIntent(review)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .addAction(0, "Catat", action("save"))
            .addAction(0, "Tinjau", review)
            .addAction(0, "Lewati", action("skip"))
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(nid(x.id), n)
        } catch (_: SecurityException) {
        }
    }
}

/** Listens to notifications from the supported bank and e-wallet apps (the user grants access in Android settings). */
class TxnListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Prefs.ensure(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val s = sbn ?: return
        if (s.packageName == packageName || DetectApps.of(s.packageName) == null) return
        val n = s.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0 || n.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        val e = n.extras ?: return
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (e.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: e.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val time = s.postTime.takeIf { it > 0 } ?: System.currentTimeMillis()
        scope.launch { runCatching { Detector.handle(applicationContext, s.packageName, title, text, time) } }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

/** "Catat" / "Lewati" buttons on a detection notification. */
class DetectActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra("id", 0L).takeIf { it > 0 } ?: return
        Prefs.ensure(ctx)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == "id.kantong.app.DETECT_save") Detector.save(ctx.applicationContext, id, null, 0L) else Detector.skip(ctx.applicationContext, id)
            } finally {
                pending.finish()
            }
        }
    }
}
