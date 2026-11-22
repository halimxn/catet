package id.kantong.app.data

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object Db {
    @Volatile private var db: KantongDb? = null
    fun get(ctx: Context): KantongDb = db ?: synchronized(this) {
        db ?: Room.databaseBuilder(ctx.applicationContext, KantongDb::class.java, "kantong.db").addMigrations(MIGRATION_1_2).build().also { db = it }
    }
}

/**
 * Everything the UI reads, as one snapshot. [txns] may be narrowed to one ledger with [forLedger];
 * [allTxns] always holds every record (account balances never depend on the ledger filter).
 */
data class AppData(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val txns: List<Txn> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val loaded: Boolean = false,
    val ledgers: List<Ledger> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val allTxns: List<Txn> = txns,
) {
    /** Same snapshot with records limited to one ledger (0 = all ledgers). */
    fun forLedger(ledgerId: Long): AppData = if (ledgerId == 0L) this else copy(txns = allTxns.filter { it.ledgerId == ledgerId })

    fun ledger(id: Long?): Ledger? = ledgers.firstOrNull { it.id == id }
    val activeLedgers: List<Ledger> get() = ledgers.filter { !it.archived }

    // ----- categories & subcategories -----
    fun topCategories(kind: String): List<Category> = categories.filter { it.kind == kind && it.parentId == 0L }
    fun subsOf(parentId: Long): List<Category> = categories.filter { it.parentId == parentId }.sortedBy { it.sortOrder }
    /** The main category of [categoryId] (itself when it already is one). */
    fun rootOf(categoryId: Long?): Category? = category(categoryId)?.let { c -> if (c.parentId == 0L) c else category(c.parentId) ?: c }

    // ----- budgets -----
    /** Start (inclusive) and end (exclusive) of the budget's current period. */
    fun periodOf(b: Budget): Pair<Long, Long> = periodRange(b.period, b.start, b.end)
    fun spentFor(b: Budget): Long {
        val (from, to) = periodOf(b)
        val ids = b.categoryIdList.toSet()
        return allTxns.filter { it.kind == "out" && it.time in from until to && (rootOf(it.categoryId)?.id in ids || it.categoryId in ids) }.sumOf { it.amount }
    }
    fun spentForCategory(b: Budget, categoryId: Long): Long {
        val (from, to) = periodOf(b)
        return allTxns.filter { it.kind == "out" && it.time in from until to && (it.categoryId == categoryId || rootOf(it.categoryId)?.id == categoryId) }.sumOf { it.amount }
    }

    // ----- reimburse & bookmarks -----
    val toReimburse: List<Txn> get() = allTxns.filter { it.reimburse == 1 }
    val reimbursed: List<Txn> get() = allTxns.filter { it.reimburse == 2 }
    val starred: List<Txn> get() = allTxns.filter { it.starred }

    private val monthStart get() = startOfMonth()
    val monthTxns: List<Txn> get() = txns.filter { it.time >= monthStart }
    val spentMonth: Long get() = monthTxns.filter { it.kind == "out" }.sumOf { it.amount }
    val incomeMonth: Long get() = monthTxns.filter { it.kind == "in" }.sumOf { it.amount }
    val spentToday: Long get() = txns.filter { it.kind == "out" && it.time >= startOfDay() }.sumOf { it.amount }
    val budgetTotal: Long get() = budgets.filter { it.period == "month" }.sumOf { it.amount }
    val leftToSpend: Long get() = (budgetTotal - spentMonth)
    val safePerDay: Long get() {
        val daysLeft = (daysInMonth() - dayOfMonth() + 1).coerceAtLeast(1)
        return (leftToSpend / daysLeft).coerceAtLeast(0)
    }

    fun category(id: Long?): Category? = categories.firstOrNull { it.id == id }
    fun account(id: Long?): Account? = accounts.firstOrNull { it.id == id }
    fun spentIn(categoryId: Long): Long = monthTxns.filter { it.kind == "out" && (it.categoryId == categoryId || rootOf(it.categoryId)?.id == categoryId) }.sumOf { it.amount }

    fun balanceOf(a: Account): Long = a.openingBalance +
        allTxns.sumOf {
            when {
                it.kind == "in" && it.accountId == a.id -> it.amount
                it.kind == "out" && it.accountId == a.id -> -it.amount
                it.kind == "transfer" && it.accountId == a.id -> -it.amount - it.fee
                it.kind == "transfer" && it.toAccountId == a.id -> it.amount
                else -> 0L
            }
        }
    /** Sum of positive balances. */
    val assetsTotal: Long get() = accounts.sumOf { balanceOf(it).coerceAtLeast(0) }
    /** Sum of negative balances (a negative number). */
    val liabilitiesTotal: Long get() = accounts.sumOf { balanceOf(it).coerceAtMost(0) }
    val totalBalance: Long get() = accounts.sumOf { balanceOf(it) }
    val savedInGoals: Long get() = goals.sumOf { it.saved }

    /** Spending per day of the current month, index 0 = day 1, up to today. */
    val dailySpend: List<Long> get() {
        val today = dayOfMonth()
        val arr = LongArray(today)
        monthTxns.filter { it.kind == "out" }.forEach { val d = dayOfMonth(it.time) - 1; if (d in arr.indices) arr[d] += it.amount }
        return arr.toList()
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = Db.get(app).dao()

    val data: StateFlow<AppData> = combine(
        combine(dao.accounts(), dao.categories(), dao.transactions(), dao.goals(), dao.bills()) { a, c, t, g, b -> AppData(a, c, t, g, b, loaded = true) },
        dao.ledgers(), dao.budgets(),
    ) { d, l, b -> d.copy(ledgers = l, budgets = b) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppData())

    /** One-shot filter handed to Riwayat when another screen opens it for a category. */
    private var pendingHistoryCat: Long? = null
    fun openHistoryFor(categoryId: Long?) { pendingHistoryCat = categoryId }
    fun takeHistoryCat(): Long? = pendingHistoryCat.also { pendingHistoryCat = null }

    /** One-shot kind ("out" | "in" | "transfer") for the next Catat screen, e.g. the Transfer chip on Aset. */
    private var pendingAddKind: String? = null
    fun startAdd(kind: String) { pendingAddKind = kind }
    fun takeAddKind(): String? = pendingAddKind.also { pendingAddKind = null }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.categoryCount() == 0) {
                dao.insertAccounts(Seed.accounts()); dao.insertCategories(Seed.categories())
                dao.insertLedgers(Seed.ledgers()); dao.insertBudgets(Seed.budgets())
                dao.insertGoals(Seed.goals())
                // Bills already due earlier this month start as paid, so the home screen shows what is still upcoming.
                dao.insertBills(Seed.bills().map { if (it.dueDay < dayOfMonth()) it.copy(paidMonthKey = monthKey()) else it })
                dao.insertTxns(Seed.transactions())
            }
        }
    }

    fun saveTxn(t: Txn) = viewModelScope.launch(Dispatchers.IO) { dao.upsertTxn(t) }
    fun deleteTxn(t: Txn) = viewModelScope.launch(Dispatchers.IO) { dao.deleteTxn(t) }
    fun saveCategory(c: Category) = viewModelScope.launch(Dispatchers.IO) { dao.upsertCategory(c) }
    fun saveGoal(g: Goal) = viewModelScope.launch(Dispatchers.IO) { dao.upsertGoal(g) }
    fun saveAccount(a: Account) = viewModelScope.launch(Dispatchers.IO) { dao.upsertAccount(a) }
    fun saveLedger(l: Ledger) = viewModelScope.launch(Dispatchers.IO) { dao.upsertLedger(l) }
    /** Deletes a ledger; its records move to [moveTo] so no money disappears from the books. */
    fun deleteLedger(l: Ledger, moveTo: Long) = viewModelScope.launch(Dispatchers.IO) { dao.moveLedger(l.id, moveTo); dao.deleteLedger(l) }
    fun saveBudget(b: Budget) = viewModelScope.launch(Dispatchers.IO) { dao.upsertBudget(b) }
    fun deleteBudget(b: Budget) = viewModelScope.launch(Dispatchers.IO) { dao.deleteBudget(b) }
    /** Deletes a category and its subcategories. Records keep their amount but show as "Tanpa kategori". */
    fun deleteCategory(c: Category) = viewModelScope.launch(Dispatchers.IO) { dao.deleteSubcategories(c.id); dao.deleteCategory(c) }
    /** Only accounts without records can be deleted (balances must stay explainable). */
    fun deleteAccount(a: Account) = viewModelScope.launch(Dispatchers.IO) {
        if (dao.allTransactions().none { it.accountId == a.id || it.toAccountId == a.id }) dao.deleteAccount(a)
    }
    fun saveBill(b: Bill) = viewModelScope.launch(Dispatchers.IO) { dao.upsertBill(b) }
    fun deleteBill(b: Bill) = viewModelScope.launch(Dispatchers.IO) { dao.deleteBill(b) }
    fun toggleStar(t: Txn) = viewModelScope.launch(Dispatchers.IO) { dao.upsertTxn(t.copy(starred = !t.starred)) }
    fun setReimburse(t: Txn, state: Int) = viewModelScope.launch(Dispatchers.IO) { dao.upsertTxn(t.copy(reimburse = state)) }

    /** Move money into a goal: records a transfer-like note and raises the goal's saved amount. */
    fun deposit(goal: Goal, amount: Long) = viewModelScope.launch(Dispatchers.IO) {
        dao.upsertGoal(goal.copy(saved = (goal.saved + amount).coerceAtLeast(0)))
    }

    fun payBill(b: Bill, accountId: Long) = viewModelScope.launch(Dispatchers.IO) {
        dao.upsertTxn(Txn(amount = b.amount, kind = "out", categoryId = b.categoryId, accountId = accountId, merchant = b.name, note = "Tagihan", time = System.currentTimeMillis()))
        dao.upsertBill(b.copy(paidMonthKey = monthKey()))
    }

    // ---------- Backup: one JSON file the user saves anywhere via the system picker (e.g. Google Drive). ----------

    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val root = JSONObject().put("app", "kantong").put("version", 2).put("exportedAt", System.currentTimeMillis())
        root.put("accounts", JSONArray(dao.allAccounts().map { JSONObject().put("id", it.id).put("name", it.name).put("type", it.type).put("openingBalance", it.openingBalance).put("tone", it.tone).put("icon", it.icon).put("remark", it.remark) }))
        root.put("categories", JSONArray(dao.allCategories().map { JSONObject().put("id", it.id).put("name", it.name).put("icon", it.icon).put("tone", it.tone).put("kind", it.kind).put("budget", it.budget).put("sortOrder", it.sortOrder).put("parentId", it.parentId) }))
        root.put("transactions", JSONArray(dao.allTransactions().map { JSONObject().put("id", it.id).put("amount", it.amount).put("kind", it.kind).put("categoryId", it.categoryId ?: JSONObject.NULL).put("accountId", it.accountId).put("toAccountId", it.toAccountId ?: JSONObject.NULL).put("merchant", it.merchant).put("note", it.note).put("time", it.time).put("ledgerId", it.ledgerId).put("reimburse", it.reimburse).put("starred", it.starred).put("fee", it.fee) }))
        root.put("ledgers", JSONArray(dao.allLedgers().map { JSONObject().put("id", it.id).put("name", it.name).put("icon", it.icon).put("tone", it.tone).put("archived", it.archived).put("sortOrder", it.sortOrder) }))
        root.put("budgets", JSONArray(dao.allBudgets().map { JSONObject().put("id", it.id).put("name", it.name).put("period", it.period).put("categoryIds", it.categoryIds).put("amount", it.amount).put("start", it.start).put("end", it.end) }))
        root.put("goals", JSONArray(dao.allGoals().map { JSONObject().put("id", it.id).put("name", it.name).put("icon", it.icon).put("tone", it.tone).put("target", it.target).put("saved", it.saved).put("deadline", it.deadline ?: JSONObject.NULL) }))
        root.put("bills", JSONArray(dao.allBills().map { JSONObject().put("id", it.id).put("name", it.name).put("amount", it.amount).put("dueDay", it.dueDay).put("categoryId", it.categoryId ?: JSONObject.NULL).put("icon", it.icon).put("tone", it.tone).put("paidMonthKey", it.paidMonthKey) }))
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(root.toString(2).toByteArray()) }
    }

    /** Replaces all data with the backup's content. Throws if the file is not a Kantong backup. */
    suspend fun importFrom(uri: Uri) = withContext(Dispatchers.IO) {
        val text = getApplication<Application>().contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
        val root = JSONObject(text)
        require(root.optString("app") == "kantong") { "Bukan file backup catet" }
        fun JSONObject.longOrNull(k: String): Long? = if (isNull(k)) null else getLong(k)
        fun arr(k: String): List<JSONObject> = root.getJSONArray(k).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
        fun arrOrEmpty(k: String): List<JSONObject> = if (root.has(k)) arr(k) else emptyList()
        val accounts = arr("accounts").map { Account(it.getLong("id"), it.getString("name"), it.getString("type"), it.getLong("openingBalance"), it.optString("tone", "SKY"), it.optString("icon"), it.optString("remark")) }
        val cats = arr("categories").map { Category(it.getLong("id"), it.getString("name"), it.getString("icon"), it.getString("tone"), it.getString("kind"), it.getLong("budget"), it.optInt("sortOrder"), it.optLong("parentId")) }
        val txns = arr("transactions").map { Txn(it.getLong("id"), it.getLong("amount"), it.getString("kind"), it.longOrNull("categoryId"), it.getLong("accountId"), it.longOrNull("toAccountId"), it.optString("merchant"), it.optString("note"), it.getLong("time"), it.optLong("ledgerId", 1), it.optInt("reimburse"), it.optBoolean("starred"), it.optLong("fee")) }
        val ledgers = arrOrEmpty("ledgers").map { Ledger(it.getLong("id"), it.getString("name"), it.getString("icon"), it.getString("tone"), it.optBoolean("archived"), it.optInt("sortOrder")) }.ifEmpty { Seed.ledgers() }
        val budgets = arrOrEmpty("budgets").map { Budget(it.getLong("id"), it.getString("name"), it.getString("period"), it.getString("categoryIds"), it.getLong("amount"), it.optLong("start"), it.optLong("end")) }
            .ifEmpty { cats.filter { it.kind == "out" && it.budget > 0 }.map { Budget(name = it.name, period = "month", categoryIds = it.id.toString(), amount = it.budget) } }
        val goals = arr("goals").map { Goal(it.getLong("id"), it.getString("name"), it.getString("icon"), it.getString("tone"), it.getLong("target"), it.getLong("saved"), it.longOrNull("deadline")) }
        val bills = arr("bills").map { Bill(it.getLong("id"), it.getString("name"), it.getLong("amount"), it.getInt("dueDay"), it.longOrNull("categoryId"), it.getString("icon"), it.getString("tone"), it.optInt("paidMonthKey")) }
        dao.clearTxns(); dao.clearBills(); dao.clearGoals(); dao.clearCategories(); dao.clearAccounts(); dao.clearLedgers(); dao.clearBudgets()
        dao.insertAccounts(accounts); dao.insertCategories(cats); dao.insertTxns(txns); dao.insertGoals(goals); dao.insertBills(bills)
        dao.insertLedgers(ledgers); dao.insertBudgets(budgets)
    }
}
