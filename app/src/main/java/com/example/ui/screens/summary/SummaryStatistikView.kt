package com.example.ui.screens.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountWallet
import com.example.data.model.AssetItem
import com.example.data.model.BudgetCategory
import com.example.data.model.DebtItem
import com.example.data.model.DebtType
import com.example.data.model.RecurringTransaction
import com.example.data.model.SavingsGoal
import com.example.data.model.TimelineScenarioItem
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.UpcomingScheduleItem
import com.example.data.model.UserSettings
import com.example.data.model.defaultStatistikCardOrder
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TrophyYellowLight
import com.example.ui.theme.TrophyYellowSoft
import com.example.ui.viewmodel.MonthOption
import com.example.util.CategoryIconHelper
import com.example.util.CategorySlice
import com.example.util.ChartSeriesData
import com.example.util.CurrencyHelper
import com.example.util.FinancialHealthHelper
import com.example.util.InstitutionLogoBadge
import com.example.util.TransactionMathHelper
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Tab "Statistik" sesuai referensi video:
 * Kumpulan kartu modular yang urutan & visibilitasnya diatur penuh oleh
 * `settings.statistikCardsOrder`, `settings.hiddenStatistikCards`, dan `settings.trenBerdampinganEnabled`.
 */
@Composable
fun SummaryStatistikView(
    selectedMonth: Int,
    selectedYear: Int,
    monthName: String,
    monthOptions: List<MonthOption>,
    allTransactions: List<Transaction>,
    periodTransactions: List<Transaction>,
    recurringTransactions: List<RecurringTransaction>,
    upcomingItems: List<UpcomingScheduleItem>,
    timelineScenarios: List<TimelineScenarioItem>,
    wallets: List<AccountWallet>,
    assets: List<AssetItem>,
    debts: List<DebtItem>,
    budgets: List<BudgetCategory> = emptyList(),
    labels: List<String> = emptyList(),
    savingsGoals: List<SavingsGoal> = emptyList(),
    settings: UserSettings,
    currentIncome: Double,
    currentExpense: Double,
    chartData: ChartSeriesData,
    expenseSlices: List<CategorySlice>,
    incomeSlices: List<CategorySlice>,
    selectedWallet: String = "Semua",
    isBalanceHidden: Boolean = false,
    onOpenCustomize: () -> Unit = {},
    onOpenTimelineAdd: () -> Unit = {},
    onDeleteTimelineScenario: (String) -> Unit = {},
    onOpenRecurring: () -> Unit = {},
    onOpenJalanUang: () -> Unit = {},
    onNavigateToAccount: (walletName: String) -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {}
) {
    val isBarModeMap = remember { mutableStateMapOf<String, Boolean>() }
    var detailCategoryTarget by remember { mutableStateOf<Pair<String, TransactionType>?>(null) }
    var detailLabelTarget by remember { mutableStateOf<String?>(null) }

    val monthNamesShort = remember {
        listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    }
    val dayNamesFull = remember {
        listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    }

    val daysInMonth = remember(selectedMonth, selectedYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val walletFilteredTransactions = remember(allTransactions, selectedWallet) {
        if (selectedWallet == "Semua") allTransactions
        else allTransactions.filter { tx ->
            tx.accountName.equals(selectedWallet, ignoreCase = true) ||
                (tx.type == TransactionType.TRANSFER && tx.toAccountName.equals(selectedWallet, ignoreCase = true))
        }
    }

    val (prevM, prevY) = remember(selectedMonth, selectedYear) {
        if (selectedMonth > 1) (selectedMonth - 1) to selectedYear else 12 to (selectedYear - 1)
    }
    val prevPeriodTransactions = remember(walletFilteredTransactions, prevM, prevY) {
        walletFilteredTransactions.filter { it.monthIndex == prevM && it.year == prevY }
    }
    val prevMonthIncome = remember(prevPeriodTransactions) {
        prevPeriodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val prevMonthExpense = remember(prevPeriodTransactions) {
        prevPeriodTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val prevMonthName = monthNamesShort.getOrElse(prevM) { "Lalu" }

    // 6-month sequence for Trend cards, CashFlow, Savings & Earnings Summary
    val past6Months = remember(selectedMonth, selectedYear) {
        (5 downTo 0).map { offset ->
            var m = selectedMonth - offset
            var y = selectedYear
            while (m <= 0) {
                m += 12
                y -= 1
            }
            Triple(m, y, monthNamesShort.getOrElse(m) { "" })
        }
    }
    val past6Income = remember(walletFilteredTransactions, past6Months) {
        past6Months.map { (m, y, _) ->
            walletFilteredTransactions.filter { it.monthIndex == m && it.year == y && it.type == TransactionType.INCOME }.sumOf { it.amount }
        }
    }
    val past6Expense = remember(walletFilteredTransactions, past6Months) {
        past6Months.map { (m, y, _) ->
            walletFilteredTransactions.filter { it.monthIndex == m && it.year == y && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        }
    }
    val past6Labels = remember(past6Months) { past6Months.map { it.third } }

    // Label breakdown integrated with registered labels & comma-separated multi-labels
    // Card "Pengeluaran per label" only displays transactions that actually carry a label.
    val labelSummaries = remember(periodTransactions, labels) {
        TransactionMathHelper.buildLabelExpenseBreakdown(periodTransactions, labels)
    }
    val labelChartPalette = remember {
        listOf(
            Color(0xFFE95B5B),
            Color(0xFFF59E0B),
            Color(0xFFFBBF24),
            Color(0xFF10B981),
            Color(0xFF38BDF8),
            Color(0xFF818CF8),
            Color(0xFFA855F7),
            Color(0xFF14B8A6)
        )
    }
    val labelSlices = remember(labelSummaries) {
        val activeLabeled = labelSummaries.filter {
            it.amount > 0.0 && !it.label.equals("Tanpa Label", ignoreCase = true)
        }
        val labeledTotal = activeLabeled.sumOf { it.amount }
        if (labeledTotal <= 0.0) {
            emptyList()
        } else {
            activeLabeled.mapIndexed { idx, item ->
                DonutSliceData(
                    label = item.label,
                    amount = item.amount,
                    percentage = (item.amount / labeledTotal).toFloat(),
                    color = labelChartPalette[idx % labelChartPalette.size]
                )
            }
        }
    }

    // Account expense breakdown
    val expensePerAccount = remember(periodTransactions) {
        periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.accountName.ifBlank { "Utama" } }
            .map { (acc, txs) -> acc to txs.sumOf { it.amount } }
            .sortedByDescending { it.second }
    }

    // Most changed categories (abs delta)
    val categoryDeltas = remember(periodTransactions, prevPeriodTransactions) {
        val curMap = periodTransactions.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }
        val prevMap = prevPeriodTransactions.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }
        (curMap.keys + prevMap.keys).map { cat ->
            val cur = curMap[cat] ?: 0.0
            val prev = prevMap[cat] ?: 0.0
            Triple(cat, cur - prev, cur)
        }.sortedByDescending { abs(it.second) }
    }

    // Day-of-week totals
    val (dayOfWeekTotals, dowPeakIndex) = remember(periodTransactions) {
        val totals = DoubleArray(7) { 0.0 }
        val tempCal = Calendar.getInstance()
        periodTransactions.filter { it.type == TransactionType.EXPENSE }.forEach { tx ->
            tempCal.set(tx.year, (tx.monthIndex - 1).coerceIn(0, 11), tx.dayOfMonth)
            val dow = tempCal.get(Calendar.DAY_OF_WEEK)
            val idx = if (dow == Calendar.SUNDAY) 6 else dow - 2
            if (idx in 0..6) totals[idx] += tx.amount
        }
        val list = totals.toList()
        val maxV = list.maxOrNull() ?: 0.0
        list to (if (maxV > 0.0) list.indexOf(maxV) else 2)
    }

    val largestExpenses = remember(periodTransactions) {
        periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sortedByDescending { it.amount }
            .take(5)
    }

    val currentMonthKey = "$selectedMonth/$selectedYear"
    val activeRecurring = remember(recurringTransactions, currentMonthKey) {
        recurringTransactions.filter { it.isActive && it.lastProcessedMonthYear != currentMonthKey }
    }
    val activeExpenseRecurring = remember(recurringTransactions) {
        recurringTransactions.filter { it.isActive && it.type == TransactionType.EXPENSE }
    }
    val fixedCostsTotal = remember(activeExpenseRecurring) {
        activeExpenseRecurring.sumOf { it.amount }
    }

    val totalLiquidBalance = remember(wallets) { wallets.filterNot { it.excludeFromNetWorth }.sumOf { it.balance } }
    val totalAssetValue = remember(assets) { assets.sumOf { it.realValue } }
    val totalDebtValue = remember(debts) { debts.filter { it.type == DebtType.BORROWED }.sumOf { it.remainingAmount } }
    val effNetWorth = totalLiquidBalance + totalAssetValue - totalDebtValue
    val effNet = currentIncome - currentExpense

    // Real dynamic Money Path milestone completion summary (matching JalanUangScreen 25 subtasks)
    val (moneyPathDone, moneyPathTotal, currentStageLabel, nextStepTitle) = remember(
        allTransactions, periodTransactions, wallets, debts, savingsGoals, assets, budgets, recurringTransactions, settings, currentIncome, currentExpense
    ) {
        val totalWalletBal = totalLiquidBalance
        val totalGainLoss = assets.sumOf { it.unrealizedGainLoss }
        val streak = FinancialHealthHelper.calculateConsecutiveStreak(periodTransactions.map { it.dayOfMonth }.toSet())
        val sr = if (currentIncome > 0.0) (currentIncome - currentExpense) / currentIncome else 0.0

        val checks = listOf(
            "Catat transaksi pertama" to allTransactions.isNotEmpty(),
            "Catat 3 hari berturut-turut" to (streak >= 3),
            "Catat sepuluh entri" to (allTransactions.size >= 10),
            "Tambahkan catatan ke entri" to allTransactions.any { it.note.isNotBlank() },
            "Atur hari gajian atau auto-save" to (settings.monthlyStartDay != 1 || settings.paydayAutoSaveEnabled),
            "Siapkan dana darurat 3-6 bulan" to ((currentExpense > 0.0 && totalWalletBal >= currentExpense * 3.0) || savingsGoals.any { it.name.contains("darurat", true) && it.currentAmount > 0.0 }),
            "Amankan asuransi & pos kesehatan" to (recurringTransactions.any { it.category.contains("Kesehatan", true) || it.title.contains("Asuransi", true) } || allTransactions.any { it.category.contains("Kesehatan", true) || it.title.contains("Asuransi", true) }),
            "Pisahkan dompet tabungan & operasional" to (wallets.size >= 2),
            "Catat pengeluaran & anggaran bulanan" to (budgets.isNotEmpty() || periodTransactions.any { it.type == TransactionType.EXPENSE }),
            "Daftar semua kewajiban & pinjaman" to (debts.isNotEmpty() || recurringTransactions.any { it.category.contains("Utang", true) || it.category.contains("Cicilan", true) }),
            "Tentukan tenggat pelunasan utang" to (debts.isNotEmpty() && debts.all { it.dueDate.isNotBlank() }),
            "Jaga arus kas tetap surplus" to (currentIncome > 0.0 && currentIncome >= currentExpense),
            "Lakukan pembayaran cicilan tercatat" to debts.any { it.paidAmount > 0.0 },
            "Sisihkan minimal 10% pendapatan" to (sr >= 0.10),
            "Tetapkan target tabungan spesifik" to savingsGoals.isNotEmpty(),
            "Otomatisasi setoran tabungan gajian" to settings.paydayAutoSaveEnabled,
            "Catat setoran tabungan rutin" to savingsGoals.any { it.records.isNotEmpty() },
            "Miliki aset investasi pertama" to assets.isNotEmpty(),
            "Diversifikasi instrumen aset" to (assets.map { it.assetType }.distinct().size >= 2),
            "Evaluasi nilai pasar portofolio" to (totalAssetValue > 0.0),
            "Kalahkan laju inflasi (portofolio hijau)" to (assets.isNotEmpty() && totalGainLoss > 0.0),
            "Capai rasio tabungan >= 50%" to (sr >= 0.50),
            "Hasil investasi 4% tutupi biaya hidup" to (currentExpense > 0.0 && (totalAssetValue * 0.04 / 12.0) >= currentExpense),
            "Dana investasi capai Rp100 Juta+" to (totalAssetValue >= 100_000_000.0),
            "Kemandirian finansial penuh (25x tahunan)" to (currentExpense > 0.0 && (totalWalletBal + totalAssetValue) >= (currentExpense * 12.0 * 25.0))
        )
        val done = checks.count { it.second }
        val firstPending = checks.firstOrNull { !it.second }?.first ?: "Semua langkah utama tercapai!"
        val stage = when {
            done < 5 -> "1 · Arus kas"
            done < 9 -> "2 · Perlindungan"
            done < 13 -> "3 · Utang"
            done < 17 -> "4 · Menabung"
            done < 21 -> "5 · Berkembang"
            else -> "6 · Kebebasan"
        }
        Tuple4Stat(done, checks.size, stage, firstPending)
    }

    val hiddenSet = remember(settings.hiddenStatistikCards) { settings.hiddenStatistikCards.toSet() }
    val fullOrder = remember(settings.statistikCardsOrder) {
        (settings.statistikCardsOrder + defaultStatistikCardOrder).distinct()
    }
    val visibleCardIds = remember(fullOrder, hiddenSet) {
        fullOrder.filter { it !in hiddenSet }
    }
    val bothTrendsVisible = "TREND_INCOME" in visibleCardIds && "TREND_EXPENSE" in visibleCardIds
    val firstTrendCardId = remember(visibleCardIds) {
        visibleCardIds.firstOrNull { it == "TREND_INCOME" || it == "TREND_EXPENSE" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (visibleCardIds.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = AccentBlueBright, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Semua kartu statistik disembunyikan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ketuk di bawah untuk mengatur kembali kartu yang ingin ditampilkan.", fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AccentBlue,
                            modifier = Modifier.clickable { onOpenCustomize() }
                        ) {
                            Text("Sesuaikan Statistik", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp))
                        }
                    }
                }
            }
        }

        visibleCardIds.forEach { cardId ->
            when (cardId) {
                "TREND_INCOME", "TREND_EXPENSE" -> {
                    if (settings.trenBerdampinganEnabled && bothTrendsVisible) {
                        if (cardId == firstTrendCardId) {
                            item(key = "SIDE_BY_SIDE_TRENDS") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StatTrendMiniCard(
                                        title = "Tren Pemasukan",
                                        currentValue = currentIncome,
                                        prevValue = prevMonthIncome,
                                        values = past6Income,
                                        labels = past6Labels,
                                        accentColor = IncomeGreen,
                                        isExpense = false,
                                        isBarMode = isBarModeMap["TREND_INCOME"] ?: true,
                                        isSideBySide = true,
                                        onToggleMode = { isBarModeMap["TREND_INCOME"] = !(isBarModeMap["TREND_INCOME"] ?: true) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatTrendMiniCard(
                                        title = "Tren Pengeluaran",
                                        currentValue = currentExpense,
                                        prevValue = prevMonthExpense,
                                        values = past6Expense,
                                        labels = past6Labels,
                                        accentColor = ExpenseRed,
                                        isExpense = true,
                                        isBarMode = isBarModeMap["TREND_EXPENSE"] ?: true,
                                        isSideBySide = true,
                                        onToggleMode = { isBarModeMap["TREND_EXPENSE"] = !(isBarModeMap["TREND_EXPENSE"] ?: true) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = cardId) {
                            val isIncome = cardId == "TREND_INCOME"
                            StatTrendMiniCard(
                                title = if (isIncome) "Tren Pemasukan" else "Tren Pengeluaran",
                                currentValue = if (isIncome) currentIncome else currentExpense,
                                prevValue = if (isIncome) prevMonthIncome else prevMonthExpense,
                                values = if (isIncome) past6Income else past6Expense,
                                labels = past6Labels,
                                accentColor = if (isIncome) IncomeGreen else ExpenseRed,
                                isExpense = !isIncome,
                                isBarMode = isBarModeMap[cardId] ?: true,
                                isSideBySide = false,
                                onToggleMode = { isBarModeMap[cardId] = !(isBarModeMap[cardId] ?: true) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                "EXPENSE_BREAKDOWN" -> item(key = cardId) {
                    val useBar = isBarModeMap[cardId] ?: false
                    StatBreakdownCard(
                        title = "Pengeluaran",
                        totalAmount = currentExpense,
                        slices = expenseSlices,
                        txType = TransactionType.EXPENSE,
                        useBarMode = useBar,
                        onToggleMode = { isBarModeMap[cardId] = !useBar },
                        onCategoryClick = { cat -> detailCategoryTarget = cat to TransactionType.EXPENSE }
                    )
                }

                "INCOME_BREAKDOWN" -> item(key = cardId) {
                    val useBar = isBarModeMap[cardId] ?: false
                    StatBreakdownCard(
                        title = "Pemasukan",
                        totalAmount = currentIncome,
                        slices = incomeSlices,
                        txType = TransactionType.INCOME,
                        useBarMode = useBar,
                        onToggleMode = { isBarModeMap[cardId] = !useBar },
                        onCategoryClick = { cat -> detailCategoryTarget = cat to TransactionType.INCOME }
                    )
                }

                "EXPENSE_PER_LABEL" -> item(key = cardId) {
                    val useBar = isBarModeMap[cardId] ?: false
                    StatLabelBreakdownCard(
                        slices = labelSlices,
                        useBarMode = useBar,
                        onToggleMode = { isBarModeMap[cardId] = !useBar },
                        onLabelClick = { lbl -> detailLabelTarget = lbl }
                    )
                }

                "UPCOMING_TIMELINE" -> item(key = cardId) {
                    UpcomingTimelineCard(
                        currentBalance = totalLiquidBalance,
                        upcomingItems = upcomingItems,
                        recurringTransactions = recurringTransactions,
                        timelineScenarios = timelineScenarios,
                        isBalanceHidden = isBalanceHidden,
                        onAddClick = onOpenTimelineAdd,
                        onDeleteScenario = onDeleteTimelineScenario,
                        onRecurringClick = onOpenRecurring
                    )
                }

                "CASH_FLOW" -> item(key = cardId) {
                    val useLine = isBarModeMap[cardId] ?: false
                    StatCashFlowCard(
                        monthName = "$monthName $selectedYear",
                        income = currentIncome,
                        expense = currentExpense,
                        netFlow = effNet,
                        past6Income = past6Income,
                        past6Expense = past6Expense,
                        past6Labels = past6Labels,
                        useLineMode = useLine,
                        onToggleMode = { isBarModeMap[cardId] = !useLine }
                    )
                }

                "EARNINGS_SUMMARY" -> item(key = cardId) {
                    StatEarningsSummaryCard(
                        currentIncome = currentIncome,
                        currentExpense = currentExpense,
                        daysInMonth = daysInMonth
                    )
                }

                "SAVINGS_RATE" -> item(key = cardId) {
                    StatSavingsRateCard(
                        currentIncome = currentIncome,
                        currentExpense = currentExpense,
                        prevIncome = prevMonthIncome,
                        prevExpense = prevMonthExpense,
                        past6Income = past6Income,
                        past6Expense = past6Expense,
                        targetPct = settings.targetSavingsPercent
                    )
                }

                "DAILY_AVERAGE" -> item(key = cardId) {
                    StatDailyAverageCard(
                        daysInMonth = daysInMonth,
                        expense = currentExpense,
                        prevExpense = prevMonthExpense,
                        past6Expense = past6Expense,
                        activeDaysCount = periodTransactions.map { it.dayOfMonth }.toSet().size
                    )
                }

                "PERIOD_COMPARISON" -> item(key = cardId) {
                    StatPeriodComparisonCard(
                        curLabel = "$monthName $selectedYear",
                        prevLabel = prevMonthName,
                        curIncome = currentIncome,
                        curExpense = currentExpense,
                        prevIncome = prevMonthIncome,
                        prevExpense = prevMonthExpense
                    )
                }

                "MOST_CHANGED_CATEGORY" -> item(key = cardId) {
                    StatMostChangedCard(
                        prevMonthName = prevMonthName,
                        deltas = categoryDeltas,
                        onCategoryClick = { cat -> detailCategoryTarget = cat to TransactionType.EXPENSE }
                    )
                }

                "DAILY_PATTERN" -> item(key = cardId) {
                    val peakDayName = dayNamesFull.getOrElse(dowPeakIndex) { "Rabu" }
                    val peakDayAmt = dayOfWeekTotals.getOrElse(dowPeakIndex) { 0.0 }
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Pola harian", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(
                                text = if (peakDayAmt > 0.0) {
                                    "$peakDayName adalah hari terborosmu (${CurrencyHelper.formatCompactRupiah(peakDayAmt)})"
                                } else {
                                    "Intensitas belanja per hari dalam seminggu"
                                },
                                fontSize = 11.sp,
                                color = if (peakDayAmt > 0.0) ExpenseRed else TextSubtle,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DayOfWeekSpendingBarChart(dayTotals = dayOfWeekTotals, peakDayIndex = dowPeakIndex)
                        }
                    }
                }

                "LARGEST_EXPENSES" -> item(key = cardId) {
                    StatLargestExpensesCard(
                        expenses = largestExpenses,
                        totalExpense = currentExpense,
                        monthName = monthName,
                        onTransactionClick = onTransactionClick
                    )
                }

                "UPCOMING_BILLS" -> item(key = cardId) {
                    StatUpcomingBillsCard(
                        activeRecurring = activeRecurring,
                        upcomingItems = upcomingItems,
                        monthName = monthName,
                        onOpenRecurring = onOpenRecurring
                    )
                }

                "PERIOD_HIGHLIGHT" -> item(key = cardId) {
                    StatPeriodHighlightCard(
                        monthName = "$monthName $selectedYear",
                        periodTransactions = periodTransactions,
                        currentIncome = currentIncome,
                        currentExpense = currentExpense,
                        topExpenseSlice = expenseSlices.firstOrNull(),
                        onTopCategoryClick = { cat -> detailCategoryTarget = cat to TransactionType.EXPENSE }
                    )
                }

                "MONEY_FLOW_SANKEY" -> item(key = cardId) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Aliran uang", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text("Distribusi pemasukan ke pengeluaran & sisa", fontSize = 10.5.sp, color = TextSubtle)
                            Spacer(modifier = Modifier.height(10.dp))
                            val incPairs = incomeSlices.map { it.category to it.amount }
                            val expPairs = expenseSlices.map { it.category to it.amount }
                            if (incPairs.isEmpty() && expPairs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Text("Belum ada transaksi untuk alur dana", fontSize = 11.5.sp, color = TextSubtle)
                                }
                            } else {
                                SankeyMoneyFlowView(
                                    incomeCategories = incPairs,
                                    expenseCategories = expPairs,
                                    savingsAmount = effNet.coerceAtLeast(0.0),
                                    onCategoryClick = { cat ->
                                        val isInc = incPairs.any { it.first.equals(cat, ignoreCase = true) }
                                        detailCategoryTarget = cat to (if (isInc) TransactionType.INCOME else TransactionType.EXPENSE)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(180.dp)
                                )
                            }
                        }
                    }
                }

                "FIXED_COSTS" -> item(key = cardId) {
                    StatFixedCostsCard(
                        fixedCostsTotal = fixedCostsTotal,
                        currentIncome = currentIncome,
                        activeBills = activeExpenseRecurring,
                        onOpenRecurring = onOpenRecurring
                    )
                }

                "CASH_FLOW_JOURNEY" -> item(key = cardId) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Perjalanan arus kas", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text("Pergerakan pengeluaran harian sepanjang periode", fontSize = 10.5.sp, color = TextSubtle)
                                }
                                Text(
                                    text = (if (effNet >= 0) "+" else "-") + CurrencyHelper.formatCompactRupiah(abs(effNet)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (effNet >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            MiniMonthTrendLineChart(
                                values = chartData.rawValues.ifEmpty { listOf(0.0, currentExpense) },
                                labels = chartData.pointLabels,
                                lineColor = if (effNet >= 0) IncomeGreen else ExpenseRed,
                                modifier = Modifier.fillMaxWidth().height(105.dp)
                            )
                        }
                    }
                }

                "EXPENSE_PER_ACCOUNT" -> item(key = cardId) {
                    StatExpensePerAccountCard(
                        accounts = expensePerAccount,
                        totalExpense = currentExpense,
                        onAccountClick = onNavigateToAccount
                    )
                }

                "MONEY_PATH" -> item(key = cardId) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenJalanUang() }
                            .testTag("card_stat_jalan_uang")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(AccentBlue.copy(alpha = 0.18f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Route, contentDescription = null, tint = AccentBlueBright, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Jalan Uang", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                        Text(currentStageLabel, fontSize = 11.sp, color = AccentBlueBright, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$moneyPathDone dari $moneyPathTotal selesai", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Buka Jalan Uang", tint = TextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { (moneyPathDone.toFloat() / moneyPathTotal.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = AccentBlueBright,
                                trackColor = DarkSurfaceElevated
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurfaceElevated,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Langkah berikutnya", fontSize = 10.sp, color = TextSubtle)
                                        Text(nextStepTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DarkSurfaceElevated,
                                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onOpenJalanUang() }
                                ) {
                                    Text(
                                        text = "Pelajari",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AccentBlue,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onOpenJalanUang() }
                                ) {
                                    Text(
                                        text = "Lakukan",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    detailCategoryTarget?.let { (catName, txType) ->
        CategoryDetailScreen(
            categoryName = catName,
            txType = txType,
            initialMonth = selectedMonth,
            initialYear = selectedYear,
            monthOptions = monthOptions,
            allTransactions = allTransactions,
            selectedWallet = selectedWallet,
            onTransactionClick = onTransactionClick,
            onDismiss = { detailCategoryTarget = null }
        )
    }

    detailLabelTarget?.let { lblName ->
        LabelDetailScreen(
            labelName = lblName,
            initialMonth = selectedMonth,
            initialYear = selectedYear,
            monthOptions = monthOptions,
            allTransactions = allTransactions,
            selectedWallet = selectedWallet,
            onTransactionClick = onTransactionClick,
            onDismiss = { detailLabelTarget = null }
        )
    }
}

private data class Tuple4Stat<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
private fun StatTrendMiniCard(
    title: String,
    currentValue: Double,
    prevValue: Double,
    values: List<Double>,
    labels: List<String>,
    accentColor: Color,
    isExpense: Boolean,
    isBarMode: Boolean,
    isSideBySide: Boolean,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nonZero = values.filter { it > 0.0 }
    val avg = if (nonZero.isNotEmpty()) nonZero.average() else currentValue
    val deltaPct = when {
        prevValue > 0.0 -> (((currentValue - prevValue) / prevValue) * 100.0).roundToInt()
        currentValue > 0.0 -> 100
        else -> 0
    }
    val deltaPositive = deltaPct >= 0
    val deltaGood = if (isExpense) !deltaPositive else deltaPositive
    val deltaColor = if (deltaGood) IncomeGreen else ExpenseRed
    val deltaSoft = if (deltaGood) IncomeGreenSoft else ExpenseRedSoft

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleMode, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = if (isBarMode) Icons.Default.ShowChart else Icons.Default.BarChart,
                        contentDescription = "Ubah tipe grafik",
                        tint = AccentBlueBright,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceElevated
            ) {
                Text(
                    text = "Rata-rata: ${CurrencyHelper.formatCompactRupiah(avg)}/bln",
                    fontSize = 9.5.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isBarMode) {
                val maxV = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(66.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        values.forEachIndexed { i, v ->
                            val ratio = (v / maxV).toFloat().coerceIn(0.08f, 1f)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                if (v > 0.0) {
                                    Text(
                                        text = CurrencyHelper.formatCompact(v),
                                        fontSize = 7.5.sp,
                                        fontWeight = if (i == values.lastIndex) FontWeight.Bold else FontWeight.Normal,
                                        color = if (i == values.lastIndex) accentColor else TextSubtle,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(if (isSideBySide) 12.dp else 18.dp)
                                        .height((46 * ratio).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (i == values.lastIndex) accentColor else accentColor.copy(alpha = 0.45f))
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        values.indices.forEach { i ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = labels.getOrElse(i) { "" },
                                    fontSize = 8.sp,
                                    color = TextSubtle,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            } else {
                MiniMonthTrendLineChart(
                    values = values,
                    labels = labels,
                    lineColor = accentColor,
                    modifier = Modifier.fillMaxWidth().height(84.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isSideBySide) {
                Surface(shape = RoundedCornerShape(10.dp), color = DarkSurfaceElevated, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bulan Lalu", fontSize = 9.5.sp, color = TextSubtle)
                            Text(CurrencyHelper.formatCompactRupiah(prevValue), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Bulan Ini", fontSize = 9.5.sp, color = TextSubtle)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(CurrencyHelper.formatCompactRupiah(currentValue), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(shape = RoundedCornerShape(5.dp), color = deltaSoft) {
                                    Text(
                                        text = "${if (deltaPositive) "▲" else "▼"} ${abs(deltaPct)}%",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = deltaColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(shape = RoundedCornerShape(10.dp), color = DarkSurfaceElevated, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bulan Lalu", fontSize = 10.sp, color = TextSubtle)
                            Text(CurrencyHelper.formatCompactRupiah(prevValue), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                        }
                        Text("→", fontSize = 14.sp, color = TextSubtle)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Bulan Ini", fontSize = 10.sp, color = TextSubtle)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(CurrencyHelper.formatCompactRupiah(currentValue), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(6.dp), color = deltaSoft) {
                                    Text(
                                        text = "${if (deltaPositive) "▲" else "▼"} ${abs(deltaPct)}%",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = deltaColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBreakdownCard(
    title: String,
    totalAmount: Double,
    slices: List<CategorySlice>,
    txType: TransactionType,
    useBarMode: Boolean,
    onToggleMode: () -> Unit,
    onCategoryClick: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text(CurrencyHelper.formatRupiah(totalAmount), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = if (txType == TransactionType.INCOME) IncomeGreen else ExpenseRed)
                }
                IconButton(onClick = onToggleMode, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (useBarMode) Icons.Default.DonutLarge else Icons.Default.BarChart,
                        contentDescription = "Ubah tampilan Donat / Batang",
                        tint = AccentBlueBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (slices.isEmpty() || totalAmount <= 0.0) {
                Text("Belum ada data pada periode ini", fontSize = 12.sp, color = TextSubtle)
            } else if (!useBarMode) {
                val donutSlices = slices.map { s ->
                    DonutSliceData(
                        label = s.category,
                        amount = s.amount,
                        percentage = (s.amount / totalAmount.coerceAtLeast(1.0)).toFloat(),
                        color = CategoryIconHelper.getCategoryColor(s.category, txType)
                    )
                }
                CategoryDonutCard(
                    title = "Distribusi $title",
                    totalAmount = totalAmount,
                    slices = donutSlices,
                    isExpense = txType == TransactionType.EXPENSE,
                    onSliceClick = onCategoryClick
                )
            } else {
                slices.forEach { slice ->
                    val ratio = (slice.amount / totalAmount.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                    val color = CategoryIconHelper.getCategoryColor(slice.category, txType)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCategoryClick(slice.category) }
                            .padding(vertical = 5.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(CategoryIconHelper.getCategoryIcon(slice.category), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(slice.category, fontSize = 12.5.sp, color = TextWhite, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text("${(ratio * 100).roundToInt()}%", fontSize = 11.sp, color = TextMuted)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(CurrencyHelper.formatCompactRupiah(slice.amount), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = DarkSurfaceElevated
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatLabelBreakdownCard(
    slices: List<DonutSliceData>,
    useBarMode: Boolean,
    onToggleMode: () -> Unit,
    onLabelClick: (String) -> Unit
) {
    val total = remember(slices) { slices.sumOf { it.amount } }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_expense_per_label")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Pengeluaran per label", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text(CurrencyHelper.formatRupiah(total), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ExpenseRed)
                }
                IconButton(
                    onClick = onToggleMode,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("btn_toggle_label_mode")
                ) {
                    Icon(
                        imageVector = if (useBarMode) Icons.Default.DonutLarge else Icons.Default.BarChart,
                        contentDescription = "Ubah tampilan Donat / Batang",
                        tint = AccentBlueBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (slices.isEmpty() || total <= 0.0) {
                Text("Belum ada data pada periode ini", fontSize = 12.sp, color = TextSubtle)
            } else if (!useBarMode) {
                CategoryDonutCard(
                    title = "Distribusi Pengeluaran per label",
                    totalAmount = total,
                    slices = slices,
                    isExpense = true,
                    onSliceClick = onLabelClick
                )
            } else {
                slices.forEach { slice ->
                    val ratio = (slice.amount / total.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                    val color = slice.color
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLabelClick(slice.label) }
                            .padding(vertical = 5.dp)
                            .testTag("row_stat_label_${slice.label}")
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Label, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = slice.label,
                                fontSize = 12.5.sp,
                                color = TextWhite,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${(ratio * 100).roundToInt()}%", fontSize = 11.sp, color = TextMuted)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(CurrencyHelper.formatCompactRupiah(slice.amount), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = DarkSurfaceElevated
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCashFlowCard(
    monthName: String,
    income: Double,
    expense: Double,
    netFlow: Double,
    past6Income: List<Double>,
    past6Expense: List<Double>,
    past6Labels: List<String>,
    useLineMode: Boolean,
    onToggleMode: () -> Unit
) {
    val netHistory = past6Income.indices.map { i -> past6Income[i] - past6Expense[i] }
    val surplusMonthsCount = past6Income.indices.count { i -> past6Income[i] > past6Expense[i] }
    val maxPairVal = maxOf(past6Income.maxOrNull() ?: 0.0, past6Expense.maxOrNull() ?: 0.0, 1.0)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Arus Kas", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text(
                        text = "$monthName · Bersih: ${(if (netFlow >= 0) "+" else "-") + CurrencyHelper.formatCompactRupiah(abs(netFlow))}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (netFlow >= 0) IncomeGreen else ExpenseRed
                    )
                }
                IconButton(onClick = onToggleMode, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (useLineMode) Icons.Default.BarChart else Icons.Default.ShowChart,
                        contentDescription = "Ubah tampilan Arus Kas",
                        tint = AccentBlueBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (useLineMode) {
                MiniMonthTrendLineChart(
                    values = netHistory.map { it.coerceAtLeast(0.0) },
                    labels = past6Labels,
                    lineColor = AccentBlueBright,
                    modifier = Modifier.fillMaxWidth().height(95.dp)
                )
            } else {
                // 6-month grouped Income vs Expense bar chart with net labels
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(86.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        past6Income.indices.forEach { idx ->
                            val inc = past6Income[idx]
                            val exp = past6Expense[idx]
                            val net = inc - exp
                            val incRatio = (inc / maxPairVal).toFloat().coerceIn(0.06f, 1f)
                            val expRatio = (exp / maxPairVal).toFloat().coerceIn(0.06f, 1f)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                if (inc > 0.0 || exp > 0.0) {
                                    Text(
                                        text = (if (net >= 0) "+" else "-") + CurrencyHelper.formatCompact(abs(net)),
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (net >= 0) IncomeGreen else ExpenseRed,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(9.dp)
                                            .height((60 * incRatio).dp)
                                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                            .background(IncomeGreen)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(9.dp)
                                            .height((60 * expRatio).dp)
                                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                            .background(ExpenseRed)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        past6Income.indices.forEach { idx ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = past6Labels.getOrElse(idx) { "" },
                                    fontSize = 9.sp,
                                    color = TextSubtle,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = IncomeGreenSoft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Pemasukan melebihi pengeluaran dalam $surplusMonthsCount dari 6 bulan terakhir",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IncomeGreen,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun StatEarningsSummaryCard(
    currentIncome: Double,
    currentExpense: Double,
    daysInMonth: Int
) {
    val saved = currentIncome - currentExpense
    val ratePct = if (currentIncome > 0.0) ((saved / currentIncome) * 100.0).roundToInt().coerceIn(0, 100) else 0
    val incPerDay = if (daysInMonth > 0) currentIncome / daysInMonth else 0.0
    val expPerDay = if (daysInMonth > 0) currentExpense / daysInMonth else 0.0
    val projectedEndOfMonthSaved = ((incPerDay - expPerDay) * daysInMonth)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ringkasan Penghasilan", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)

            // Semi-donut arc gauge centered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(
                    modifier = Modifier
                        .size(width = 170.dp, height = 95.dp)
                ) {
                    val strokeW = 12.dp.toPx()
                    val diameter = size.width - strokeW
                    val topLeft = androidx.compose.ui.geometry.Offset(strokeW / 2f, strokeW / 2f)
                    val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
                    drawArc(
                        color = DarkSurfaceElevated,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokeW, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = if (saved >= 0) IncomeGreen else ExpenseRed,
                        startAngle = 180f,
                        sweepAngle = (ratePct / 100f) * 180f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokeW, cap = StrokeCap.Round)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "$ratePct% Dihemat",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (saved >= 0) IncomeGreen else ExpenseRed
                    )
                    Text(
                        text = "${CurrencyHelper.formatCompactRupiah(abs(saved))} ${if (saved >= 0) "Dihemat" else "Defisit"} siklus ini",
                        fontSize = 10.5.sp,
                        color = TextMuted
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatSmallBox("PENGHASILAN / HARI", CurrencyHelper.formatCompactRupiah(incPerDay), IncomeGreen, Modifier.weight(1f))
                StatSmallBox("PENGELUARAN / HARI", CurrencyHelper.formatCompactRupiah(expPerDay), ExpenseRed, Modifier.weight(1f))
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (projectedEndOfMonthSaved >= 0) IncomeGreenSoft else ExpenseRedSoft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (projectedEndOfMonthSaved >= 0) {
                        "Dengan kecepatan ini Anda akan menyimpan sekitar ${CurrencyHelper.formatCompactRupiah(projectedEndOfMonthSaved)} pada akhir bulan"
                    } else {
                        "Dengan kecepatan ini pengeluaran melampaui pemasukan sekitar ${CurrencyHelper.formatCompactRupiah(abs(projectedEndOfMonthSaved))}"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (projectedEndOfMonthSaved >= 0) IncomeGreen else ExpenseRed,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun StatSavingsRateCard(
    currentIncome: Double,
    currentExpense: Double,
    prevIncome: Double,
    prevExpense: Double,
    past6Income: List<Double>,
    past6Expense: List<Double>,
    targetPct: Int
) {
    val saved = currentIncome - currentExpense
    val rate = if (currentIncome > 0.0) ((saved / currentIncome) * 100.0).roundToInt() else 0
    val prevRate = if (prevIncome > 0.0) (((prevIncome - prevExpense) / prevIncome) * 100.0).roundToInt() else 0
    val diffPts = rate - prevRate
    val met = rate >= targetPct
    val rateHistory = past6Income.indices.map { i ->
        val inc = past6Income[i]
        val exp = past6Expense[i]
        if (inc > 0.0) (((inc - exp) / inc) * 100.0).coerceIn(0.0, 100.0) else 0.0
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rasio menabung", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$rate%", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = if (met) IncomeGreen else TrophyYellowLight)
                    Text("dari pemasukan yang tersisa periode ini", fontSize = 11.sp, color = TextMuted)
                    if (prevIncome > 0.0) {
                        Text(
                            text = "${if (diffPts >= 0) "▲ +" else "▼ "}$diffPts poin vs periode lalu",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (diffPts >= 0) IncomeGreen else ExpenseRed,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // 6-bar mini sparkline on right
                Row(
                    modifier = Modifier
                        .width(96.dp)
                        .height(54.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    rateHistory.forEachIndexed { idx, r ->
                        val ratio = (r / 100.0).toFloat().coerceIn(0.1f, 1f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((48 * ratio).dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(if (idx == rateHistory.lastIndex) IncomeGreen else IncomeGreen.copy(alpha = 0.38f))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (rate.coerceIn(0, 100) / 100f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (met) IncomeGreen else TrophyYellowLight,
                trackColor = DarkSurfaceElevated
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Target pribadi: $targetPct% · Tersisih ${CurrencyHelper.formatCompactRupiah(saved.coerceAtLeast(0.0))}", fontSize = 10.5.sp, color = TextSubtle)
        }
    }
}

@Composable
private fun StatDailyAverageCard(
    daysInMonth: Int,
    expense: Double,
    prevExpense: Double,
    past6Expense: List<Double>,
    activeDaysCount: Int
) {
    val dExp = if (daysInMonth > 0) expense / daysInMonth else 0.0
    val projectedTotal = dExp * daysInMonth
    val maxHist = past6Expense.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rata-rata harian", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(CurrencyHelper.formatRupiah(dExp), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = ExpenseRed)
                    Text("dihabiskan per hari periode ini · $activeDaysCount/$daysInMonth hr aktif", fontSize = 10.5.sp, color = TextMuted)
                }

                // Mini 6-month trend bars on right
                Row(
                    modifier = Modifier
                        .width(90.dp)
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    past6Expense.forEachIndexed { idx, v ->
                        val ratio = (v / maxHist).toFloat().coerceIn(0.1f, 1f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((44 * ratio).dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(if (idx == past6Expense.lastIndex) ExpenseRed else ExpenseRed.copy(alpha = 0.4f))
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatSmallBox("Periode lalu", CurrencyHelper.formatCompactRupiah(prevExpense), TextMuted, Modifier.weight(1f))
                StatSmallBox("Perkiraan total", CurrencyHelper.formatCompactRupiah(projectedTotal), AccentBlueBright, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatPeriodComparisonCard(
    curLabel: String,
    prevLabel: String,
    curIncome: Double,
    curExpense: Double,
    prevIncome: Double,
    prevExpense: Double
) {
    val curNet = curIncome - curExpense
    val prevNet = prevIncome - prevExpense
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Periode ini vs lalu", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Metrik", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextSubtle, modifier = Modifier.weight(1f))
                Text(curLabel, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextSubtle, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text(prevLabel, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextSubtle, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
            ComparisonLineRow("Pemasukan", curIncome, prevIncome, IncomeGreen)
            ComparisonLineRow("Pengeluaran", curExpense, prevExpense, ExpenseRed)
            ComparisonLineRow("Bersih", curNet, prevNet, if (curNet >= 0) IncomeGreen else ExpenseRed)
        }
    }
}

@Composable
private fun ComparisonLineRow(label: String, cur: Double, prev: Double, curColor: Color) {
    val maxV = maxOf(abs(cur), abs(prev), 1.0)
    val ratio = (abs(cur) / maxV).toFloat().coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(7.dp).background(curColor, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, fontSize = 12.sp, color = TextWhite)
            }
            Text(CurrencyHelper.formatCompactRupiah(cur), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = curColor, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text(CurrencyHelper.formatCompactRupiah(prev), fontSize = 12.sp, color = TextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = curColor,
            trackColor = DarkSurfaceElevated
        )
    }
}

@Composable
private fun StatMostChangedCard(
    prevMonthName: String,
    deltas: List<Triple<String, Double, Double>>,
    onCategoryClick: (String) -> Unit
) {
    val maxDelta = deltas.maxOfOrNull { abs(it.second) }?.coerceAtLeast(1.0) ?: 1.0
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Kategori paling berubah", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text("Perubahan terbesar dibanding $prevMonthName", fontSize = 10.5.sp, color = TextSubtle)
            if (deltas.isEmpty()) {
                Text("Belum ada pembanding kategori yang cukup", fontSize = 11.5.sp, color = TextMuted)
            } else {
                deltas.take(5).forEach { (cat, delta, _) ->
                    val catColor = CategoryIconHelper.getCategoryColor(cat, TransactionType.EXPENSE)
                    val isReduced = delta <= 0
                    val changeColor = if (isReduced) IncomeGreen else ExpenseRed
                    val ratio = (abs(delta) / maxDelta).toFloat().coerceIn(0.08f, 1f)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCategoryClick(cat) }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(8.dp), color = catColor.copy(alpha = 0.16f), modifier = Modifier.size(26.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(CategoryIconHelper.getCategoryIcon(cat), contentDescription = null, tint = catColor, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat, fontSize = 12.5.sp, color = TextWhite, fontWeight = FontWeight.Medium)
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = if (isReduced) IncomeGreenSoft else ExpenseRedSoft) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = (if (delta >= 0) "+" else "-") + CurrencyHelper.formatCompactRupiah(abs(delta)),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = changeColor
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = if (isReduced) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = changeColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = changeColor,
                            trackColor = DarkSurfaceElevated
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatLargestExpensesCard(
    expenses: List<Transaction>,
    totalExpense: Double,
    monthName: String,
    onTransactionClick: (Transaction) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pengeluaran terbesar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
            if (expenses.isEmpty()) {
                Text("Belum ada transaksi pengeluaran di periode ini", fontSize = 12.sp, color = TextMuted)
            } else {
                expenses.forEach { tx ->
                    val c = CategoryIconHelper.getCategoryColor(tx.category, TransactionType.EXPENSE)
                    val sharePct = if (totalExpense > 0.0) ((tx.amount / totalExpense) * 100.0).roundToInt() else 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onTransactionClick(tx) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(8.dp), color = c.copy(alpha = 0.16f), modifier = Modifier.size(30.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(CategoryIconHelper.getCategoryIcon(tx.category), contentDescription = null, tint = c, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.title.ifBlank { tx.category }, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${tx.dayOfMonth} $monthName · ${tx.accountName}", fontSize = 10.5.sp, color = TextMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("-${CurrencyHelper.formatCompactRupiah(tx.amount)}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            Text("$sharePct% total", fontSize = 10.sp, color = TextSubtle)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatUpcomingBillsCard(
    activeRecurring: List<RecurringTransaction>,
    upcomingItems: List<UpcomingScheduleItem>,
    monthName: String,
    onOpenRecurring: () -> Unit
) {
    val bills = activeRecurring.filter { it.type == TransactionType.EXPENSE }
    val totalDue = bills.sumOf { it.amount } + upcomingItems.filter { !it.isIncome }.sumOf { it.amount }
    val monthShort = monthName.take(3)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onOpenRecurring() }
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventRepeat, contentDescription = null, tint = AccentBlueBright, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tagihan mendatang", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
                Text("Total jatuh tempo ${CurrencyHelper.formatCompactRupiah(totalDue)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
            }
            if (bills.isEmpty() && upcomingItems.isEmpty()) {
                Text("Semua tagihan berulang sudah dibayar atau belum dijadwalkan.", fontSize = 11.5.sp, color = TextMuted)
            } else {
                bills.take(4).forEach { rec ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceElevated,
                            modifier = Modifier.width(44.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("${rec.dayOfMonth}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                Text(monthShort, fontSize = 9.sp, color = AccentBlueBright, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rec.title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${rec.category} · ${rec.accountName}", fontSize = 10.sp, color = TextSubtle, maxLines = 1)
                        }
                        Text(CurrencyHelper.formatCompactRupiah(rec.amount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPeriodHighlightCard(
    monthName: String,
    periodTransactions: List<Transaction>,
    currentIncome: Double,
    currentExpense: Double,
    topExpenseSlice: CategorySlice?,
    onTopCategoryClick: (String) -> Unit
) {
    val peakDayEntry = periodTransactions.filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.dayOfMonth }
        .maxByOrNull { e -> e.value.sumOf { it.amount } }
    val peakAmt = peakDayEntry?.value?.sumOf { it.amount } ?: 0.0
    val peakDayStr = peakDayEntry?.key?.let { "Tgl $it" } ?: "-"
    val remainingPct = if (currentIncome > 0.0) (((currentIncome - currentExpense) / currentIncome) * 100.0).roundToInt().coerceIn(0, 100) else 0

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sorotan periode", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text("Ringkasan fakta utama sepanjang $monthName", fontSize = 10.5.sp, color = TextSubtle)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatSmallBox("Hari terboros ($peakDayStr)", CurrencyHelper.formatCompactRupiah(peakAmt), ExpenseRed, Modifier.weight(1f))
                StatSmallBox(
                    label = "Kategori teratas",
                    value = topExpenseSlice?.category ?: "-",
                    valueColor = TextWhite,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (topExpenseSlice != null) Modifier.clickable { onTopCategoryClick(topExpenseSlice.category) } else Modifier
                        )
                )
                StatSmallBox("Tersisa pemasukan", "$remainingPct%", IncomeGreen, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatFixedCostsCard(
    fixedCostsTotal: Double,
    currentIncome: Double,
    activeBills: List<RecurringTransaction>,
    onOpenRecurring: () -> Unit
) {
    val fixedPct = if (currentIncome > 0.0) ((fixedCostsTotal / currentIncome) * 100).roundToInt().coerceIn(0, 100) else 0
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onOpenRecurring() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Biaya tetap", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text("Kewajiban rutin berulang", fontSize = 10.5.sp, color = TextSubtle)
                }
                Text(CurrencyHelper.formatRupiah(fixedCostsTotal), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = DarkSurfaceElevated, style = Stroke(10.dp.toPx()))
                        drawArc(
                            color = AccentBlueBright,
                            startAngle = -90f,
                            sweepAngle = (fixedPct / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(CurrencyHelper.formatCompactRupiah(fixedCostsTotal), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("$fixedPct%", fontSize = 10.sp, color = AccentBlueBright, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("$fixedPct% dari pemasukan periode ini", fontSize = 11.5.sp, color = TextMuted)
            }
            if (activeBills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                activeBills.take(4).forEach { bill ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bill.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${CurrencyHelper.formatCompactRupiah(bill.amount)} / ${bill.frequency.replace("Setiap ", "", ignoreCase = true).lowercase()}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatExpensePerAccountCard(
    accounts: List<Pair<String, Double>>,
    totalExpense: Double,
    onAccountClick: (String) -> Unit
) {
    val palette = listOf(AccentBlueBright, ExpenseRed, TrophyYellowLight, IncomeGreen, Color(0xFFA855F7))
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AccentBlueBright, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pengeluaran per akun", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
            if (accounts.isEmpty() || totalExpense <= 0.0) {
                Text("Belum ada pengeluaran per akun pada periode ini", fontSize = 12.sp, color = TextMuted)
            } else {
                // Multi-color stacked horizontal allocation bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    accounts.forEachIndexed { idx, (_, amt) ->
                        val w = (amt / totalExpense).toFloat().coerceAtLeast(0.03f)
                        Box(
                            modifier = Modifier
                                .weight(w)
                                .height(10.dp)
                                .background(palette[idx % palette.size])
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                accounts.forEachIndexed { idx, (accName, amt) ->
                    val ratio = (amt / totalExpense.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                    val barColor = palette[idx % palette.size]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAccountClick(accName) }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            InstitutionLogoBadge(walletName = accName, size = 24.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(accName, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextWhite, modifier = Modifier.weight(1f))
                            Text("${(ratio * 100).roundToInt()}%", fontSize = 11.sp, color = TextMuted)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(CurrencyHelper.formatCompactRupiah(amt), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = barColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = barColor,
                            trackColor = DarkSurfaceElevated
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatSmallBox(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = DarkSurfaceElevated, modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 9.5.sp, color = TextSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(3.dp))
            Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
