package id.kantong.app.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import id.kantong.app.MainActivity
import java.util.Calendar

/**
 * Daily "Sudah catat hari ini?" notification.
 * Uses a 10-minute alarm window each day (re-armed when it fires): an inexact daily repeating alarm may be
 * delivered many hours late, a window alarm arrives close to the chosen time and needs no exact-alarm permission.
 */
object Reminder {
    const val CHANNEL = "pengingat"
    private const val REQUEST = 2001
    private const val NOTIFICATION_ID = 2001
    private const val WINDOW = 10 * 60_000L

    /** Saves the time and arms the next reminder. */
    fun schedule(ctx: Context, hour: Int, minute: Int) {
        Prefs.ensure(ctx)
        Prefs.reminderHour = hour
        Prefs.reminderMinute = minute
        Prefs.reminder = true
        createChannel(ctx)
        arm(ctx, hour, minute)
    }

    fun cancel(ctx: Context) {
        Prefs.ensure(ctx)
        Prefs.reminder = false
        ctx.getSystemService(AlarmManager::class.java)?.cancel(alarmIntent(ctx))
    }

    /** Re-arms the reminder if it is on (app start, boot, app update). */
    fun restore(ctx: Context) {
        Prefs.ensure(ctx)
        if (Prefs.reminder) arm(ctx, Prefs.reminderHour, Prefs.reminderMinute)
    }

    private fun arm(ctx: Context, hour: Int, minute: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        am.setWindow(AlarmManager.RTC_WAKEUP, nextTime(hour, minute), WINDOW, alarmIntent(ctx))
    }

    /** Next occurrence of hour:minute, at least one minute from now. */
    private fun nextTime(hour: Int, minute: Int): Long {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= System.currentTimeMillis() + 60_000L) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    private fun alarmIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx, REQUEST, Intent(ctx, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun createChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Pengingat harian", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Pengingat untuk mencatat keuangan setiap hari"
                },
            )
        }
    }

    internal fun show(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nmc = NotificationManagerCompat.from(ctx)
        if (!nmc.areNotificationsEnabled()) return
        createChannel(ctx)
        val open = Intent(ctx, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val content = PendingIntent.getActivity(ctx, REQUEST, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(id.kantong.app.R.drawable.ic_launcher_fg)
            .setColor(0xFFBD505A.toInt())
            .setContentTitle("Sudah catat hari ini?")
            .setContentText("Catat pengeluaran hari ini biar keuanganmu tetap rapi")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Catat pengeluaran hari ini biar keuanganmu tetap rapi"))
            .setContentIntent(content)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            nmc.notify(NOTIFICATION_ID, n)
        } catch (_: SecurityException) {
        }
    }
}

/** Fires once a day: shows the reminder and arms the next one. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.ensure(context)
        if (!Prefs.reminder) return
        Reminder.show(context)
        Reminder.restore(context)
    }
}

/** Alarms are cleared on reboot and app update: put the reminder back. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> Reminder.restore(context)
        }
    }
}
