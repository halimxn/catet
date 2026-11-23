package id.kantong.app.data

import java.util.Calendar

/** One installment of a scheduled debt. */
class Installment(val due: Long, val amount: Long, val paid: Boolean)

/**
 * Where a debt stands: paid so far, remaining, whether it's done, the monthly schedule (when installments > 0),
 * the next amount and date to pay, and how many days it's overdue.
 */
class DebtState(val debt: Debt, val pays: List<DebtPay>) {
    val paid: Long = pays.sumOf { it.amount }
    val remaining: Long = (debt.amount - paid).coerceAtLeast(0)
    val done: Boolean = debt.settled == 1 || remaining == 0L
    val pct: Float = if (debt.amount > 0) (paid.toFloat() / debt.amount).coerceIn(0f, 1f) else 0f

    val schedule: List<Installment> = if (debt.installments <= 0) emptyList() else {
        val per = debt.amount / debt.installments
        var covered = paid
        (0 until debt.installments).map { i ->
            val amt = if (i == debt.installments - 1) debt.amount - per * (debt.installments - 1) else per
            val due = Calendar.getInstance().apply { timeInMillis = debt.firstDue; add(Calendar.MONTH, i) }.timeInMillis
            val ok = covered >= amt
            covered = (covered - amt).coerceAtLeast(0)
            Installment(due, amt, ok || done)
        }
    }

    /** Next date that money is expected (first unpaid installment, or the due date). 0 = none. */
    val nextDue: Long = when {
        done -> 0
        schedule.isNotEmpty() -> schedule.firstOrNull { !it.paid }?.due ?: 0
        else -> debt.dueDate
    }
    val nextAmount: Long = if (schedule.isNotEmpty()) schedule.firstOrNull { !it.paid }?.amount ?: remaining else remaining
    val overdueDays: Int = if (nextDue in 1 until startOfDay()) ((startOfDay() - startOfDay(nextDue)) / 86_400_000L).toInt() else 0

    companion object {
        fun of(debt: Debt, pays: List<DebtPay>) = DebtState(debt, pays.sortedByDescending { it.time })
    }
}
