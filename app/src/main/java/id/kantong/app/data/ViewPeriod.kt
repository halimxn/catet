package id.kantong.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

/**
 * Month being viewed on Beranda and Laporan (chosen in the period sheet). 0 = the current month.
 * Not persisted: the app always opens on the current month.
 */
object ViewPeriod {
    var monthStart by mutableLongStateOf(0L)

    /** Start of the viewed month. */
    fun start(): Long = if (monthStart == 0L) startOfMonth() else monthStart

    /** End (exclusive) of the viewed month. */
    fun end(): Long = java.util.Calendar.getInstance().apply { timeInMillis = start(); add(java.util.Calendar.MONTH, 1) }.timeInMillis

    fun isCurrent(): Boolean = start() == startOfMonth()
}
