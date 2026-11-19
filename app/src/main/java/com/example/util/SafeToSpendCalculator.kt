package com.example.util

import com.example.data.model.AccountWallet
import com.example.data.model.RecurringTransaction
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.UpcomingScheduleItem
import com.example.data.model.UserSettings
import java.util.Calendar

data class SafeToSpendSummary(
    val shoppingBalance: Double,
    val unpaidBillsBeforePayday: Double,
    val billCountBeforePayday: Int,
    val goalReserves: Double,
    val freeUntilPayday: Double,
    val daysUntilPayday: Int,
    val dailySafeToSpend: Double,
    val nextPaydayLabel: String,
    val spentToday: Double,
    val dailyProgress: Float,
    val shoppingWalletCount: Int,
    val nextPaydayShortLabel: String = nextPaydayLabel,
    val billPreviewSubtitle: String = ""
)

object SafeToSpendCalculator {

    private val shortMonths = listOf(
        "", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )

    private val shortDays = mapOf(
        Calendar.MONDAY to "Sen",
        Calendar.TUESDAY to "Sel",
        Calendar.WEDNESDAY to "Rab",
        Calendar.THURSDAY to "Kam",
        Calendar.FRIDAY to "Jum",
        Calendar.SATURDAY to "Sab",
        Calendar.SUNDAY to "Min"
    )

    fun calculate(
        wallets: List<AccountWallet>,
        transactions: List<Transaction>,
        recurringTransactions: List<RecurringTransaction>,
        upcomingItems: List<UpcomingScheduleItem>,
        savingsGoals: List<SavingsGoal>,
        settings: UserSettings,
        nowCal: Calendar = Calendar.getInstance()
    ): SafeToSpendSummary {
        val todayDay = nowCal.get(Calendar.DAY_OF_MONTH)
        val todayMonth = nowCal.get(Calendar.MONTH) + 1
        val todayYear = nowCal.get(Calendar.YEAR)

        // Shopping wallets: wallets included in budget (or all wallets if user excluded everything)
        val budgetWallets = wallets.filter { !it.excludeFromBudget }
        val effectiveWallets = if (budgetWallets.isNotEmpty()) budgetWallets else wallets
        val shoppingBalance = effectiveWallets.sumOf { it.balance }.coerceAtLeast(0.0)

        // Compute next payday calendar from settings.monthlyStartDay (1..28/31)
        val startDaySetting = settings.monthlyStartDay.coerceIn(1, 28)
        val nextPaydayCal = (nowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (todayDay < startDaySetting) {
                set(Calendar.DAY_OF_MONTH, startDaySetting.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
            } else {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, startDaySetting.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
            }
        }
        val todayMidnight = (nowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = (nextPaydayCal.timeInMillis - todayMidnight.timeInMillis).coerceAtLeast(86_400_000L)
        val daysUntilPayday = ((diffMillis / 86_400_000L).toInt()).coerceAtLeast(1)
        val nextDayNum = nextPaydayCal.get(Calendar.DAY_OF_MONTH)
        val nextMonStr = shortMonths.getOrElse(nextPaydayCal.get(Calendar.MONTH) + 1) { "Bln" }
        val nextDowStr = shortDays[nextPaydayCal.get(Calendar.DAY_OF_WEEK)] ?: ""
        val nextPaydayLabel = "$nextDayNum $nextMonStr ${nextPaydayCal.get(Calendar.YEAR)}"
        val nextPaydayShortLabel = if (nextDowStr.isNotEmpty()) "$nextDowStr, $nextDayNum $nextMonStr" else "$nextDayNum $nextMonStr"

        // Active unpaid bills due before next payday
        val currentMonthKey = "$todayMonth/$todayYear"
        var billSum = 0.0
        var billCount = 0
        val countedTitles = mutableSetOf<String>()
        val billPreviewParts = mutableListOf<String>()

        recurringTransactions.filter {
            it.isActive && it.type == TransactionType.EXPENSE && it.lastProcessedMonthYear != currentMonthKey
        }.forEach { rec ->
            val dueInCycle = if (todayDay < startDaySetting) {
                rec.dayOfMonth in todayDay until startDaySetting
            } else {
                rec.dayOfMonth >= todayDay || rec.dayOfMonth < startDaySetting
            }
            if (dueInCycle) {
                billSum += rec.amount
                billCount++
                countedTitles.add(rec.title.trim().lowercase())
                if (billPreviewParts.size < 2) {
                    val mIdx = if (rec.dayOfMonth >= todayDay) todayMonth else ((todayMonth % 12) + 1)
                    val mShort = shortMonths.getOrElse(mIdx) { "" }
                    billPreviewParts.add("${rec.title} · ${rec.dayOfMonth} $mShort")
                }
            }
        }

        upcomingItems.filter { !it.isIncome }.forEach { up ->
            if (up.title.trim().lowercase() !in countedTitles) {
                val parsedDay = up.dateLabel.trim().split(" ").firstOrNull()?.toIntOrNull()
                val dueInCycle = if (parsedDay != null) {
                    if (todayDay < startDaySetting) parsedDay in todayDay until startDaySetting
                    else parsedDay >= todayDay || parsedDay < startDaySetting
                } else true
                if (dueInCycle) {
                    billSum += up.amount
                    billCount++
                    if (billPreviewParts.size < 2) {
                        billPreviewParts.add("${up.title} · ${up.dateLabel}")
                    }
                }
            }
        }

        val billPreviewSubtitle = if (billPreviewParts.isEmpty()) {
            "Tidak ada tagihan jatuh tempo sebelum gajian"
        } else {
            billPreviewParts.joinToString(", ")
        }

        // Goal reserves (active savings goals remaining or payday auto-save target)
        val activeGoalsMonthly = savingsGoals.filter { !it.isCompleted }.sumOf { g ->
            val rem = g.remainingAmount
            if (rem > 0.0) (rem * 0.1).coerceAtMost(shoppingBalance * 0.25) else 0.0
        }
        val paydayReserve = if (settings.paydayAutoSaveEnabled && settings.paydayAutoSavePercent > 0) {
            shoppingBalance * (settings.paydayAutoSavePercent / 100.0)
        } else 0.0
        val goalReserves = maxOf(activeGoalsMonthly, paydayReserve).coerceAtMost(shoppingBalance * 0.5)

        val freeUntilPayday = (shoppingBalance - billSum - goalReserves).coerceAtLeast(0.0)
        val dailySafe = (freeUntilPayday / daysUntilPayday.toDouble()).coerceAtLeast(0.0)

        val spentToday = transactions.filter {
            it.type == TransactionType.EXPENSE &&
                it.dayOfMonth == todayDay &&
                it.monthIndex == todayMonth &&
                it.year == todayYear
        }.sumOf { it.amount }

        val dailyProgress = when {
            dailySafe > 0.0 -> (spentToday / dailySafe).toFloat().coerceIn(0f, 1f)
            spentToday > 0.0 -> 1f
            else -> 0f
        }

        return SafeToSpendSummary(
            shoppingBalance = shoppingBalance,
            unpaidBillsBeforePayday = billSum,
            billCountBeforePayday = billCount,
            goalReserves = goalReserves,
            freeUntilPayday = freeUntilPayday,
            daysUntilPayday = daysUntilPayday,
            dailySafeToSpend = dailySafe,
            nextPaydayLabel = nextPaydayLabel,
            spentToday = spentToday,
            dailyProgress = dailyProgress,
            shoppingWalletCount = effectiveWallets.size,
            nextPaydayShortLabel = nextPaydayShortLabel,
            billPreviewSubtitle = billPreviewSubtitle
        )
    }
}
