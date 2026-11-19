package com.example.util

import com.example.data.model.AccountWallet
import com.example.data.model.BudgetCategory
import com.example.data.model.MainCategory
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ComputedBudgetItem(
    val budget: BudgetCategory,
    val spent: Double,
    val remaining: Double,
    val overAmount: Double,
    val percentage: Float,
    val rawPercentInt: Int,
    val matchingTransactions: List<Transaction>
)

data class BudgetPeriodInfo(
    val daysRemaining: Int,
    val totalDays: Int,
    val paceProgress: Float
)

object BudgetMathHelper {

    val frequencyOptions = listOf(
        "Harian",
        "Mingguan",
        "Bulanan",
        "Triwulanan",
        "Tahunan",
        "Rentang khusus"
    )

    fun calculatePeriodInfo(
        frequency: String,
        selectedMonth: Int,
        selectedYear: Int
    ): BudgetPeriodInfo {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH) + 1
        val currentYear = now.get(Calendar.YEAR)
        val currentDay = now.get(Calendar.DAY_OF_MONTH)

        val monthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, (selectedMonth - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)

        return when (frequency) {
            "Harian" -> {
                val hour = now.get(Calendar.HOUR_OF_DAY)
                val pace = (hour / 24f).coerceIn(0f, 1f)
                BudgetPeriodInfo(daysRemaining = 1, totalDays = 1, paceProgress = pace)
            }
            "Mingguan" -> {
                val dow = ((now.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7) + 1 // 1..7
                val rem = (7 - dow).coerceAtLeast(1)
                BudgetPeriodInfo(daysRemaining = rem, totalDays = 7, paceProgress = (dow / 7f).coerceIn(0f, 1f))
            }
            "Triwulanan" -> {
                val qStartMonth = ((selectedMonth - 1) / 3) * 3 + 1
                var totalQuarterDays = 0
                var elapsedDays = 0
                for (m in qStartMonth..(qStartMonth + 2)) {
                    val c = Calendar.getInstance().apply {
                        set(Calendar.YEAR, selectedYear)
                        set(Calendar.MONTH, m - 1)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val md = c.getActualMaximum(Calendar.DAY_OF_MONTH)
                    totalQuarterDays += md
                    if (selectedYear < currentYear || (selectedYear == currentYear && m < currentMonth)) {
                        elapsedDays += md
                    } else if (selectedYear == currentYear && m == currentMonth) {
                        elapsedDays += currentDay
                    }
                }
                val rem = (totalQuarterDays - elapsedDays).coerceAtLeast(1)
                val pace = (elapsedDays.toFloat() / totalQuarterDays.coerceAtLeast(1)).coerceIn(0f, 1f)
                BudgetPeriodInfo(daysRemaining = rem, totalDays = totalQuarterDays, paceProgress = pace)
            }
            "Tahunan" -> {
                val isLeap = (selectedYear % 4 == 0 && selectedYear % 100 != 0) || (selectedYear % 400 == 0)
                val totalYearDays = if (isLeap) 366 else 365
                val elapsed = when {
                    selectedYear < currentYear -> totalYearDays
                    selectedYear > currentYear -> 0
                    else -> now.get(Calendar.DAY_OF_YEAR)
                }
                val rem = (totalYearDays - elapsed).coerceAtLeast(1)
                val pace = (elapsed.toFloat() / totalYearDays).coerceIn(0f, 1f)
                BudgetPeriodInfo(daysRemaining = rem, totalDays = totalYearDays, paceProgress = pace)
            }
            else -> {
                // Bulanan & Rentang khusus default
                val elapsed = when {
                    selectedYear < currentYear || (selectedYear == currentYear && selectedMonth < currentMonth) -> daysInMonth
                    selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > currentMonth) -> 0
                    else -> currentDay
                }
                val rem = (daysInMonth - elapsed).coerceAtLeast(1)
                val pace = (elapsed.toFloat() / daysInMonth).coerceIn(0f, 1f)
                BudgetPeriodInfo(daysRemaining = rem, totalDays = daysInMonth, paceProgress = pace)
            }
        }
    }

    private val dateParsers = ThreadLocal.withInitial {
        listOf("dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "yyyy-MM-dd").map { fmt ->
            SimpleDateFormat(fmt, Locale.getDefault()).apply { isLenient = false }
        }
    }

    private fun parseDateToTimestamp(dateStr: String): Long? {
        if (dateStr.isBlank()) return null
        val trimmed = dateStr.trim()
        val parsers = dateParsers.get() ?: return null
        for (sdf in parsers) {
            try {
                return sdf.parse(trimmed)?.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun transactionMatchesPeriod(
        tx: Transaction,
        budget: BudgetCategory,
        selectedMonth: Int,
        selectedYear: Int
    ): Boolean {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_MONTH)
        val currentMonth = now.get(Calendar.MONTH) + 1
        val currentYear = now.get(Calendar.YEAR)

        return when (budget.frequency) {
            "Harian" -> {
                val targetDay = if (selectedMonth == currentMonth && selectedYear == currentYear) currentDay else 1
                tx.monthIndex == selectedMonth && tx.year == selectedYear && tx.dayOfMonth == targetDay
            }
            "Mingguan" -> {
                if (selectedMonth == currentMonth && selectedYear == currentYear) {
                    val startCal = Calendar.getInstance().apply {
                        val dow = ((get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7)
                        add(Calendar.DAY_OF_MONTH, -dow)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val endCal = (startCal.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_MONTH, 6)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    val txCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, tx.year)
                        set(Calendar.MONTH, (tx.monthIndex - 1).coerceIn(0, 11))
                        set(Calendar.DAY_OF_MONTH, tx.dayOfMonth.coerceIn(1, 31))
                        set(Calendar.HOUR_OF_DAY, 12)
                    }
                    txCal.timeInMillis in startCal.timeInMillis..endCal.timeInMillis
                } else {
                    tx.monthIndex == selectedMonth && tx.year == selectedYear && tx.dayOfMonth in 1..7
                }
            }
            "Triwulanan" -> {
                val selectedQ = (selectedMonth - 1) / 3
                val txQ = (tx.monthIndex - 1) / 3
                tx.year == selectedYear && txQ == selectedQ
            }
            "Tahunan" -> {
                tx.year == selectedYear
            }
            "Rentang khusus" -> {
                val startMs = parseDateToTimestamp(budget.customStartDate)
                val endMs = parseDateToTimestamp(budget.customEndDate)
                if (startMs != null && endMs != null) {
                    val txCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, tx.year)
                        set(Calendar.MONTH, (tx.monthIndex - 1).coerceIn(0, 11))
                        set(Calendar.DAY_OF_MONTH, tx.dayOfMonth.coerceIn(1, 31))
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val t = txCal.timeInMillis
                    t in startMs..(endMs + 86_399_999L)
                } else {
                    tx.monthIndex == selectedMonth && tx.year == selectedYear
                }
            }
            else -> {
                // Bulanan
                tx.monthIndex == selectedMonth && tx.year == selectedYear
            }
        }
    }

    private fun categoryMatchesBudget(
        tx: Transaction,
        budget: BudgetCategory,
        allCategories: List<MainCategory>
    ): Boolean {
        val targets = budget.targetCategories.map { it.trim().lowercase() }.toSet()
        val txCat = tx.category.trim().lowercase()
        val txSub = tx.subcategory.trim().lowercase()

        if (txCat in targets || (txSub.isNotBlank() && txSub in targets)) return true

        // Alias normalization for seamless compatibility across sample & user transactions
        val foodAliases = setOf("food & drink", "food & dining", "makanan & minuman", "makanan")
        if (targets.any { it in foodAliases } && txCat in foodAliases) return true

        val marketAliases = setOf("groceries", "shopping", "market", "market / belanja", "belanja")
        if (targets.any { it in setOf("groceries", "market / belanja") } && txCat in marketAliases) return true

        val healthAliases = setOf("healthcare", "kesehatan")
        if (targets.any { it in healthAliases } && txCat in healthAliases) return true

        // Also match if budget tracks a MainCategory and transaction belongs to one of its subcategories
        for (mainCat in allCategories) {
            val mainLower = mainCat.name.trim().lowercase()
            if (mainLower in targets) {
                val subLowers = mainCat.subcategories.map { it.name.trim().lowercase() }.toSet()
                if (txCat in subLowers || (txSub.isNotBlank() && txSub in subLowers)) return true
            }
        }
        return false
    }

    fun computeBudgetItem(
        budget: BudgetCategory,
        allTransactions: List<Transaction>,
        wallets: List<AccountWallet>,
        allCategories: List<MainCategory>,
        selectedMonth: Int,
        selectedYear: Int
    ): ComputedBudgetItem {
        val excludedWallets = wallets.filter { it.excludeFromBudget }.map { it.name.trim().lowercase() }.toSet()
        val selectedAccountsLower = budget.accountNames.map { it.trim().lowercase() }.toSet()

        val matching = allTransactions.filter { tx ->
            if (tx.type != TransactionType.EXPENSE) return@filter false
            if (!transactionMatchesPeriod(tx, budget, selectedMonth, selectedYear)) return@filter false

            val accLower = tx.accountName.trim().lowercase()
            if (selectedAccountsLower.isNotEmpty()) {
                if (accLower !in selectedAccountsLower) return@filter false
            } else {
                if (accLower in excludedWallets) return@filter false
            }

            categoryMatchesBudget(tx, budget, allCategories)
        }

        val dynamicSpent = matching.sumOf { it.amount }
        val allocated = budget.allocated.coerceAtLeast(0.0)
        val remaining = (allocated - dynamicSpent).coerceAtLeast(0.0)
        val over = (dynamicSpent - allocated).coerceAtLeast(0.0)
        val pct = if (allocated > 0) (dynamicSpent / allocated).toFloat().coerceIn(0f, 1f) else 0f
        val rawPct = if (allocated > 0) kotlin.math.round((dynamicSpent / allocated) * 100).toInt().coerceAtLeast(0) else 0

        return ComputedBudgetItem(
            budget = budget,
            spent = dynamicSpent,
            remaining = remaining,
            overAmount = over,
            percentage = pct,
            rawPercentInt = rawPct,
            matchingTransactions = matching
        )
    }
}
