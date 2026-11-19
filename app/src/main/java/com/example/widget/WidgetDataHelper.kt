package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import com.example.data.model.AccountWallet
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceStorageHelper
import com.example.util.BudgetMathHelper
import com.example.util.CurrencyHelper
import java.util.Calendar
import kotlin.math.roundToInt

object WidgetNavigation {
    const val EXTRA_WIDGET_ACTION = "com.example.widget.EXTRA_WIDGET_ACTION"
    const val ACTION_QUICK_ADD_EXPENSE = "QUICK_ADD_EXPENSE"
    const val ACTION_QUICK_ADD_INCOME = "QUICK_ADD_INCOME"
    const val ACTION_QUICK_ADD_TRANSFER = "QUICK_ADD_TRANSFER"
    const val ACTION_OPEN_SUMMARY = "OPEN_SUMMARY"
    const val ACTION_OPEN_ACCOUNTS = "OPEN_ACCOUNTS"
    const val ACTION_OPEN_BUDGET = "OPEN_BUDGET"
    const val ACTION_OPEN_DASHBOARD = "OPEN_DASHBOARD"
    const val ACTION_TOGGLE_HIDE_BALANCE = "com.example.widget.ACTION_TOGGLE_HIDE_BALANCE"
}

data class WidgetSnapshot(
    val totalWalletBalance: Double,
    val netWorth: Double,
    val monthlyIncome: Double,
    val monthlyExpense: Double,
    val todayExpense: Double,
    val totalBudgetLimit: Double,
    val totalBudgetSpent: Double,
    val budgetPercent: Int,
    val hasBudgets: Boolean,
    val walletCount: Int,
    val topWallets: List<AccountWallet>,
    val isBalanceHidden: Boolean
)

object WidgetDataHelper {
    private const val PREFS_NAME = "money_tracker_widget_prefs"
    private const val KEY_HIDE_BALANCE = "hide_balance"

    fun isBalanceHidden(context: Context, defaultValue: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_HIDE_BALANCE)) {
            prefs.getBoolean(KEY_HIDE_BALANCE, defaultValue)
        } else {
            defaultValue
        }
    }

    fun toggleBalanceHidden(context: Context) {
        val stored = FinanceStorageHelper.loadData(context)
        val defaultVal = stored?.settings?.defaultHideBalance ?: false
        val next = !isBalanceHidden(context, defaultVal)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HIDE_BALANCE, next)
            .apply()
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    fun loadSnapshot(context: Context): WidgetSnapshot {
        val data = FinanceStorageHelper.loadData(context)
        if (data == null) {
            return WidgetSnapshot(
                totalWalletBalance = 0.0,
                netWorth = 0.0,
                monthlyIncome = 0.0,
                monthlyExpense = 0.0,
                todayExpense = 0.0,
                totalBudgetLimit = 0.0,
                totalBudgetSpent = 0.0,
                budgetPercent = 0,
                hasBudgets = false,
                walletCount = 0,
                topWallets = emptyList(),
                isBalanceHidden = isBalanceHidden(context, false)
            )
        }

        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)

        val wallets = data.wallets
        val totalWalletBalance = wallets.sumOf { safeAmount(it.balance) }
        val liquidForNetWorth = wallets.filterNot { it.excludeFromNetWorth }.sumOf { safeAmount(it.balance) }
        val totalAssets = data.assets.sumOf { safeAmount(it.realValue) }
        val totalBorrowedDebts = data.debts
            .filter { it.type == DebtType.BORROWED }
            .sumOf { safeAmount(it.remainingAmount) }
        val netWorth = liquidForNetWorth + totalAssets - totalBorrowedDebts

        val currentMonthTxs = data.transactions.filter {
            it.monthIndex == currentMonth && it.year == currentYear
        }
        val monthlyIncome = currentMonthTxs
            .filter { it.type == TransactionType.INCOME }
            .sumOf { safeAmount(it.amount) }
        val monthlyExpense = currentMonthTxs
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { safeAmount(it.amount) }
        val todayExpense = currentMonthTxs
            .filter { it.dayOfMonth == currentDay && it.type == TransactionType.EXPENSE }
            .sumOf { safeAmount(it.amount) }

        val categories = if (data.categories.isNotEmpty()) {
            data.categories
        } else {
            FinanceStorageHelper.getDefaultCategories()
        }
        val computedBudgets = data.budgets.map { budget ->
            BudgetMathHelper.computeBudgetItem(
                budget = budget,
                allTransactions = data.transactions,
                wallets = wallets,
                allCategories = categories,
                selectedMonth = currentMonth,
                selectedYear = currentYear
            )
        }
        val totalBudgetLimit = computedBudgets.sumOf { safeAmount(it.budget.allocated) }
        val totalBudgetSpent = computedBudgets.sumOf { safeAmount(it.spent) }
        val budgetPct = if (totalBudgetLimit > 0.0) {
            ((totalBudgetSpent / totalBudgetLimit) * 100.0).roundToInt().coerceAtLeast(0)
        } else {
            0
        }

        val hideBalance = isBalanceHidden(context, data.settings.defaultHideBalance)

        return WidgetSnapshot(
            totalWalletBalance = totalWalletBalance,
            netWorth = netWorth,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            todayExpense = todayExpense,
            totalBudgetLimit = totalBudgetLimit,
            totalBudgetSpent = totalBudgetSpent,
            budgetPercent = budgetPct,
            hasBudgets = data.budgets.isNotEmpty() && totalBudgetLimit > 0.0,
            walletCount = wallets.size,
            topWallets = wallets.take(3),
            isBalanceHidden = hideBalance
        )
    }

    fun formatAmountOrMasked(amount: Double, isHidden: Boolean): String {
        return if (isHidden) "Rp •••••••" else CurrencyHelper.formatRupiah(amount)
    }

    fun formatCompactOrMasked(amount: Double, isHidden: Boolean): String {
        return if (isHidden) "Rp •••••" else CurrencyHelper.formatCompactRupiah(amount)
    }

    fun createLaunchPendingIntent(
        context: Context,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(WidgetNavigation.EXTRA_WIDGET_ACTION, action)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun safeAmount(value: Double): Double {
        return if (value.isFinite()) value else 0.0
    }
}

object WidgetUpdateHelper {
    fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext ?: context
        val manager = runCatching { AppWidgetManager.getInstance(appContext) }.getOrNull() ?: return

        runCatching {
            val balanceIds = manager.getAppWidgetIds(
                ComponentName(appContext, BalanceSummaryWidgetProvider::class.java)
            )
            if (balanceIds.isNotEmpty()) {
                BalanceSummaryWidgetProvider.updateWidgets(appContext, manager, balanceIds)
            }
        }

        runCatching {
            val quickIds = manager.getAppWidgetIds(
                ComponentName(appContext, QuickEntryWidgetProvider::class.java)
            )
            if (quickIds.isNotEmpty()) {
                QuickEntryWidgetProvider.updateWidgets(appContext, manager, quickIds)
            }
        }

        runCatching {
            val walletBudgetIds = manager.getAppWidgetIds(
                ComponentName(appContext, WalletBudgetWidgetProvider::class.java)
            )
            if (walletBudgetIds.isNotEmpty()) {
                WalletBudgetWidgetProvider.updateWidgets(appContext, manager, walletBudgetIds)
            }
        }
    }
}
