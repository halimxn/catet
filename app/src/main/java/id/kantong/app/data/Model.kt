package id.kantong.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** Money is stored as whole rupiah (Long). Timestamps are epoch millis. */

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // bank | ewallet | cash
    val openingBalance: Long, // negative = liability (loan, credit card)
    val tone: String = "SKY",
    val icon: String = "", // CatIcon key; empty = picked from the type
    val remark: String = "",
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String, // key understood by ui.components.iconFor()
    val tone: String,
    val kind: String, // out | in
    val budget: Long = 0, // legacy (v1) monthly budget; budgets now live in [Budget]
    val sortOrder: Int = 0,
    val parentId: Long = 0, // 0 = top-level category, otherwise a subcategory of that id
)

@Entity(tableName = "transactions")
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val kind: String, // out | in | transfer
    val categoryId: Long?,
    val accountId: Long,
    val toAccountId: Long? = null,
    val merchant: String = "",
    val note: String = "",
    val time: Long,
    val ledgerId: Long = 1,
    val reimburse: Int = 0, // 0 = no, 1 = waiting to be reimbursed, 2 = reimbursed
    val starred: Boolean = false, // bookmarked
    val fee: Long = 0, // transfer charge
    val photo: String = "", // receipt file name in private storage ("" = none)
    val items: String = "", // receipt line items: "name\tqty\tprice" per line
)

/** A ledger ("buku kas"): a separate book of records, e.g. daily needs, household, a trip. */
@Entity(tableName = "ledgers")
data class Ledger(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val tone: String,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

/** A spending limit for one or more categories over a period. */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val period: String, // day | week | month | year | custom
    val categoryIds: String, // comma separated category ids
    val amount: Long,
    val start: Long = 0, // custom period only
    val end: Long = 0,
    val rollover: Int = 0, // 1 = unspent amount of the previous period is added to this one
    val rolloverOver: Int = 0, // 1 = overspending of the previous period is taken from this one
) {
    val categoryIdList: List<Long> get() = categoryIds.split(',').mapNotNull { it.trim().toLongOrNull() }
}

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val tone: String,
    val target: Long,
    val saved: Long,
    val deadline: Long? = null,
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    val dueDay: Int,
    val categoryId: Long?,
    val icon: String,
    val tone: String,
    val paidMonthKey: Int = 0, // yyyyMM of the last month it was paid
)

/** An investment: stock (price per share, quantity in shares), gold (per gram), bond or fund (per unit). */
@Entity(tableName = "holdings")
data class Holding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String, // stock | gold | bond | fund
    val code: String, // BBCA, ORI025, ...
    val name: String,
    val lastPrice: Long = 0,
    val prevClose: Long = 0,
    val priceTime: Long = 0,
    val targetPrice: Long = 0,
    val accountId: Long = 0, // account used for buys, sells and dividends (0 = none)
    val coupon: Double = 0.0, // bonds: yearly coupon in percent
)

/** One buy, sell or dividend of a [Holding]. qty = shares / grams / units; price per unit (dividend: per share). */
@Entity(tableName = "inv_txns")
data class InvTxn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val holdingId: Long,
    val type: String, // buy | sell | dividend
    val qty: Double,
    val price: Long,
    val fee: Long = 0,
    val time: Long,
    val txnId: Long = 0, // linked cash record (0 = none)
)

/** Money lent to someone (direction "lend" = piutang) or borrowed from them ("borrow" = utang). */
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val direction: String, // lend | borrow
    val amount: Long,
    val time: Long,
    val dueDate: Long = 0, // 0 = no due date (ignored when installments > 0)
    val note: String = "",
    val installments: Int = 0, // 0 = no schedule; N = equal monthly installments
    val firstDue: Long = 0, // date of the first installment
    val settled: Int = 0, // 1 = marked paid off by hand
    val txnId: Long = 0, // cash record of the loan itself (0 = none)
    val sourceTxnId: Long = 0, // spending record it was split from (patungan)
)

/** One repayment of a [Debt]. */
@Entity(tableName = "debt_pays")
data class DebtPay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Long,
    val time: Long,
    val txnId: Long = 0,
)

@Dao
interface KantongDao {
    @Query("SELECT * FROM accounts ORDER BY id") fun accounts(): Flow<List<Account>>
    @Query("SELECT * FROM categories ORDER BY kind, sortOrder, id") fun categories(): Flow<List<Category>>
    @Query("SELECT * FROM transactions ORDER BY time DESC") fun transactions(): Flow<List<Txn>>
    @Query("SELECT * FROM goals ORDER BY id") fun goals(): Flow<List<Goal>>
    @Query("SELECT * FROM bills ORDER BY dueDay") fun bills(): Flow<List<Bill>>
    @Query("SELECT * FROM ledgers ORDER BY sortOrder, id") fun ledgers(): Flow<List<Ledger>>
    @Query("SELECT * FROM budgets ORDER BY id") fun budgets(): Flow<List<Budget>>
    @Query("SELECT * FROM ledgers") suspend fun allLedgers(): List<Ledger>
    @Query("SELECT * FROM budgets") suspend fun allBudgets(): List<Budget>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertLedger(l: Ledger): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertBudget(b: Budget): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLedgers(l: List<Ledger>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBudgets(b: List<Budget>)
    @Delete suspend fun deleteLedger(l: Ledger)
    @Delete suspend fun deleteBudget(b: Budget)
    @Delete suspend fun deleteCategory(c: Category)
    @Delete suspend fun deleteAccount(a: Account)
    @Delete suspend fun deleteBill(b: Bill)
    @Query("DELETE FROM categories WHERE parentId = :parent") suspend fun deleteSubcategories(parent: Long)
    @Query("UPDATE transactions SET ledgerId = :to WHERE ledgerId = :from") suspend fun moveLedger(from: Long, to: Long)
    @Query("DELETE FROM ledgers") suspend fun clearLedgers()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()

    @Query("SELECT * FROM accounts") suspend fun allAccounts(): List<Account>
    @Query("SELECT * FROM categories") suspend fun allCategories(): List<Category>
    @Query("SELECT * FROM transactions") suspend fun allTransactions(): List<Txn>
    @Query("SELECT * FROM goals") suspend fun allGoals(): List<Goal>
    @Query("SELECT * FROM bills") suspend fun allBills(): List<Bill>
    @Query("SELECT COUNT(*) FROM categories") suspend fun categoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAccount(a: Account): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCategory(c: Category): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertTxn(t: Txn): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertGoal(g: Goal): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertBill(b: Bill): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAccounts(a: List<Account>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCategories(c: List<Category>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTxns(t: List<Txn>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGoals(g: List<Goal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBills(b: List<Bill>)
    @Update suspend fun updateCategory(c: Category)
    @Delete suspend fun deleteTxn(t: Txn)
    @Delete suspend fun deleteGoal(g: Goal)

    @Query("SELECT * FROM holdings ORDER BY id") fun holdings(): Flow<List<Holding>>
    @Query("SELECT * FROM inv_txns ORDER BY time") fun invTxns(): Flow<List<InvTxn>>
    @Query("SELECT * FROM holdings") suspend fun allHoldings(): List<Holding>
    @Query("SELECT * FROM inv_txns") suspend fun allInvTxns(): List<InvTxn>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertHolding(h: Holding): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertInvTxn(t: InvTxn): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHoldings(h: List<Holding>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertInvTxns(t: List<InvTxn>)
    @Delete suspend fun deleteHolding(h: Holding)
    @Delete suspend fun deleteInvTxn(t: InvTxn)
    @Query("DELETE FROM inv_txns WHERE holdingId = :id") suspend fun deleteInvTxnsOf(id: Long)
    @Query("DELETE FROM transactions WHERE id = :id") suspend fun deleteTxnById(id: Long)
    @Query("DELETE FROM holdings") suspend fun clearHoldings()
    @Query("DELETE FROM inv_txns") suspend fun clearInvTxns()

    @Query("SELECT * FROM debts ORDER BY time DESC") fun debts(): Flow<List<Debt>>
    @Query("SELECT * FROM debt_pays ORDER BY time") fun debtPays(): Flow<List<DebtPay>>
    @Query("SELECT * FROM debts") suspend fun allDebts(): List<Debt>
    @Query("SELECT * FROM debt_pays") suspend fun allDebtPays(): List<DebtPay>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertDebt(d: Debt): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertDebtPay(p: DebtPay): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDebts(d: List<Debt>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDebtPays(p: List<DebtPay>)
    @Delete suspend fun deleteDebt(d: Debt)
    @Delete suspend fun deleteDebtPay(p: DebtPay)
    @Query("DELETE FROM debt_pays WHERE debtId = :id") suspend fun deleteDebtPaysOf(id: Long)
    @Query("DELETE FROM debts") suspend fun clearDebts()
    @Query("DELETE FROM debt_pays") suspend fun clearDebtPays()

    @Query("DELETE FROM accounts") suspend fun clearAccounts()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM transactions") suspend fun clearTxns()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM bills") suspend fun clearBills()
}

@Database(entities = [Account::class, Category::class, Txn::class, Goal::class, Bill::class, Ledger::class, Budget::class, Holding::class, InvTxn::class, Debt::class, DebtPay::class], version = 7, exportSchema = false)
abstract class KantongDb : RoomDatabase() {
    abstract fun dao(): KantongDao
}

/** v1 -> v2: ledgers, budgets, subcategories, reimburse/bookmark/fee on records, account icons. Keeps every existing row. */
/** v2 -> v3: budget rollover flags, investments (holdings + their buys/sells/dividends). Keeps every existing row. */
/** v3 -> v4: debts and their repayments. Keeps every existing row. */
/** v4 -> v5: yearly coupon for bonds. */
/** v5 -> v6: receipt photo on records. */
/** v6 -> v7: receipt line items on records. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN items TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN photo TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE holdings ADD COLUMN coupon REAL NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `debts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `person` TEXT NOT NULL, `direction` TEXT NOT NULL, `amount` INTEGER NOT NULL, `time` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, `note` TEXT NOT NULL, `installments` INTEGER NOT NULL, `firstDue` INTEGER NOT NULL, `settled` INTEGER NOT NULL, `txnId` INTEGER NOT NULL, `sourceTxnId` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `debt_pays` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `debtId` INTEGER NOT NULL, `amount` INTEGER NOT NULL, `time` INTEGER NOT NULL, `txnId` INTEGER NOT NULL)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE budgets ADD COLUMN rollover INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE budgets ADD COLUMN rolloverOver INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS `holdings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `kind` TEXT NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `lastPrice` INTEGER NOT NULL, `prevClose` INTEGER NOT NULL, `priceTime` INTEGER NOT NULL, `targetPrice` INTEGER NOT NULL, `accountId` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `inv_txns` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `holdingId` INTEGER NOT NULL, `type` TEXT NOT NULL, `qty` REAL NOT NULL, `price` INTEGER NOT NULL, `fee` INTEGER NOT NULL, `time` INTEGER NOT NULL, `txnId` INTEGER NOT NULL)")
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN icon TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE accounts ADD COLUMN remark TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE categories ADD COLUMN parentId INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN ledgerId INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE transactions ADD COLUMN reimburse INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN starred INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN fee INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS `ledgers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `tone` TEXT NOT NULL, `archived` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `period` TEXT NOT NULL, `categoryIds` TEXT NOT NULL, `amount` INTEGER NOT NULL, `start` INTEGER NOT NULL, `end` INTEGER NOT NULL)")
        Seed.ledgers().forEach { l ->
            db.execSQL("INSERT INTO ledgers (id, name, icon, tone, archived, sortOrder) VALUES (?, ?, ?, ?, 0, ?)", arrayOf<Any>(l.id, l.name, l.icon, l.tone, l.sortOrder))
        }
        // Default subcategories under the sample main categories, only where that main category exists.
        Seed.subcategories().forEach { c ->
            db.execSQL(
                "INSERT INTO categories (name, icon, tone, kind, budget, sortOrder, parentId) SELECT ?, ?, ?, ?, 0, ?, ? WHERE EXISTS (SELECT 1 FROM categories WHERE id = ? AND parentId = 0 AND kind = ?)",
                arrayOf<Any>(c.name, c.icon, c.tone, c.kind, c.sortOrder, c.parentId, c.parentId, c.kind),
            )
        }
        // Old per-category monthly limits become monthly budgets.
        db.execSQL("INSERT INTO budgets (name, period, categoryIds, amount, start, `end`) SELECT name, 'month', CAST(id AS TEXT), budget, 0, 0 FROM categories WHERE budget > 0 AND kind = 'out'")
    }
}
