package com.example.ui.screens.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.data.model.MainCategory
import com.example.data.model.RecurringTransaction
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.UserSettings
import com.example.ui.dialogs.FinancialHealthDetailSheet
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
import com.example.ui.theme.getBalanceCardGradient
import com.example.ui.viewmodel.MonthOption
import com.example.util.CategoryIconHelper
import com.example.util.CategorySlice
import com.example.util.ChartSeriesData
import com.example.util.CumulativeComparisonData
import com.example.util.CurrencyHelper
import com.example.util.FinancialHealthHelper
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Tab "Laporan" sesuai referensi video:
 * 1. Banner interaktif "Buku harian uang Anda" -> BukuHarianStoryDialog (7 slide)
 * 2. AKTIVITAS PENGELUARAN (grafik tumpang tindih periode ini vs bulan lalu + scrubber + PDF sheet)
 * 3. Arus Kas Bulanan
 * 4. Masih direncanakan (toggle & item berulang)
 * 5. Pendapatan vs Pengeluaran (kumulatif dengan scrubber sentuh)
 * 6. Skor Kesehatan Keuangan (dapat diklik untuk rincian penuh)
 * 7. Rincian Pendapatan & Pengeluaran + Disisihkan ke tabungan & aset
 * 8. Pengeluaran Menurut Hari Kerja (S S R K J S M)
 * 9. Catatan Harian (streak tracker)
 */
@Composable
fun SummaryLaporanView(
    selectedMonth: Int,
    selectedYear: Int,
    monthName: String,
    monthOptions: List<MonthOption>,
    allTransactions: List<Transaction>,
    periodTransactions: List<Transaction>,
    recurringTransactions: List<RecurringTransaction>,
    wallets: List<AccountWallet>,
    assets: List<AssetItem>,
    debts: List<DebtItem>,
    budgets: List<BudgetCategory>,
    categories: List<MainCategory>,
    savingsGoals: List<SavingsGoal>,
    settings: UserSettings,
    currentIncome: Double,
    currentExpense: Double,
    chartData: ChartSeriesData,
    cumulativeData: CumulativeComparisonData,
    expenseSlices: List<CategorySlice>,
    incomeSlices: List<CategorySlice>,
    selectedWallet: String = "Semua",
    onOpenExportSheet: () -> Unit = {},
    onOpenRecurring: () -> Unit = {},
    onNavigateToAccount: (walletName: String) -> Unit = {},
    onNavigateToAssets: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onAddTransaction: (category: String?) -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {}
) {
    var isPlannedActive by remember { mutableStateOf(true) }
    var showStoryDialog by remember { mutableStateOf(false) }
    var showHealthScoreSheet by remember { mutableStateOf(false) }
    var showDailyRecordInfoDialog by remember { mutableStateOf(false) }
    var expenseChartPeriod by remember { mutableIntStateOf(0) } // 0: Bulan, 1: 7 Hari
    var expandBreakdown by remember { mutableStateOf(false) }
    var detailCategoryTarget by remember { mutableStateOf<Pair<String, TransactionType>?>(null) }

    val monthNamesShort = remember {
        listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
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
    val prevMonthExpense = remember(prevPeriodTransactions) {
        prevPeriodTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val prevMonthName = monthNamesShort.getOrElse(prevM) { "Lalu" }

    // Dual-series daily spending (Current vs Previous month)
    val (currentSeries, prevSeries, chartLabels, peakDayIdx) = remember(
        expenseChartPeriod, periodTransactions, prevPeriodTransactions, chartData, daysInMonth, monthName
    ) {
        if (expenseChartPeriod == 0) {
            val curPts = (1..daysInMonth).map { d ->
                periodTransactions.filter { it.type == TransactionType.EXPENSE && it.dayOfMonth == d }.sumOf { it.amount }
            }
            val prevPts = (1..daysInMonth).map { d ->
                prevPeriodTransactions.filter { it.type == TransactionType.EXPENSE && it.dayOfMonth == d }.sumOf { it.amount }
            }
            val lbls = (1..daysInMonth).map { "$it $monthName" }
            val maxV = curPts.maxOrNull() ?: 0.0
            val pIdx = if (maxV > 0.0) curPts.indexOf(maxV) else -1
            Tuple4(curPts, prevPts, lbls, pIdx)
        } else {
            val startD = (daysInMonth - 6).coerceAtLeast(1)
            val days = (startD..daysInMonth).toList()
            val curPts = days.map { d ->
                periodTransactions.filter { it.type == TransactionType.EXPENSE && it.dayOfMonth == d }.sumOf { it.amount }
            }
            val prevPts = days.map { d ->
                prevPeriodTransactions.filter { it.type == TransactionType.EXPENSE && it.dayOfMonth == d }.sumOf { it.amount }
            }
            val lbls = days.map { "$it $monthName" }
            val maxV = curPts.maxOrNull() ?: 0.0
            val pIdx = if (maxV > 0.0) curPts.indexOf(maxV) else -1
            Tuple4(curPts, prevPts, lbls, pIdx)
        }
    }

    val (dayOfWeekTotals, dowPeakIndex) = remember(periodTransactions) {
        val expenseTxs = periodTransactions.filter { it.type == TransactionType.EXPENSE }
        val totals = DoubleArray(7) { 0.0 }
        val tempCal = Calendar.getInstance()
        for (tx in expenseTxs) {
            tempCal.set(tx.year, (tx.monthIndex - 1).coerceIn(0, 11), tx.dayOfMonth)
            val dow = tempCal.get(Calendar.DAY_OF_WEEK)
            val idx = if (dow == Calendar.SUNDAY) 6 else dow - 2
            if (idx in 0..6) totals[idx] += tx.amount
        }
        val list = totals.toList()
        val maxVal = list.maxOrNull() ?: 0.0
        val pIdx = if (maxVal > 0) list.indexOf(maxVal) else 2
        list to pIdx
    }
    val dayNamesIndo = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    val peakDayName = dayNamesIndo.getOrElse(dowPeakIndex) { "Rabu" }

    val trackedDaysSet = remember(periodTransactions) {
        periodTransactions.map { it.dayOfMonth }.toSet()
    }
    val currentStreakDays = remember(trackedDaysSet) {
        FinancialHealthHelper.calculateConsecutiveStreak(trackedDaysSet)
    }

    val effNet = currentIncome - currentExpense
    val healthReport = remember(
        periodTransactions, prevPeriodTransactions, allTransactions,
        wallets, assets, debts, budgets, categories,
        selectedMonth, selectedYear, daysInMonth, settings
    ) {
        FinancialHealthHelper.calculate(
            periodTransactions = periodTransactions,
            prevPeriodTransactions = prevPeriodTransactions,
            allTransactions = allTransactions,
            wallets = wallets,
            assets = assets,
            debts = debts,
            budgets = budgets,
            categories = categories,
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            daysInMonth = daysInMonth,
            targetSavingsPercent = settings.targetSavingsPercent,
            budgetWarningPercent = settings.budgetWarningPercent
        )
    }
    val healthScore = healthReport.score

    val currentMonthKey = "$selectedMonth/$selectedYear"
    val activeRecurring = remember(recurringTransactions, currentMonthKey) {
        recurringTransactions.filter { it.isActive && it.lastProcessedMonthYear != currentMonthKey }
    }
    val plannedIncTotal = remember(activeRecurring) {
        activeRecurring.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val plannedExpTotal = remember(activeRecurring) {
        activeRecurring.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    // Savings & Assets allocation summary
    val totalWalletBalance = remember(wallets) { wallets.filterNot { it.excludeFromNetWorth }.sumOf { it.balance } }
    val totalAssetValue = remember(assets) { assets.sumOf { it.realValue } }
    val totalSavingsGoalsSaved = remember(savingsGoals) { savingsGoals.sumOf { it.currentAmount } }
    val transferToSavingsAmount = remember(periodTransactions) {
        periodTransactions.filter {
            it.type == TransactionType.TRANSFER ||
                it.category.contains("Tabungan", ignoreCase = true) ||
                it.category.contains("Investasi", ignoreCase = true)
        }.sumOf { it.amount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ==================== BANNER: BUKU HARIAN UANG ANDA ====================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showStoryDialog = true }
                    .testTag("banner_buku_harian"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(getBalanceCardGradient(AccentBlue))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = "Buku harian uang Anda",
                                            tint = Color(0xFFFDE68A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Buku harian uang Anda",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Melihat kembali ke mana uang Anda pergi",
                                        fontSize = 11.5.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Buka cerita bulan ini",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "$monthName $selectedYear",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "${periodTransactions.size} transaksi · ${trackedDaysSet.size} hr aktif",
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==================== 1. AKTIVITAS PENGELUARAN (DUAL COMPARISON LINE CHART) ====================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AKTIVITAS PENGELUARAN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSubtle
                            )
                            Text(
                                text = "$monthName $selectedYear",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceElevated,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(0 to "Bulan", 1 to "7 Hari").forEach { (idx, lbl) ->
                                        val sel = expenseChartPeriod == idx
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (sel) AccentBlue else Color.Transparent,
                                            modifier = Modifier.clickable { expenseChartPeriod = idx }
                                        ) {
                                            Text(
                                                text = lbl,
                                                fontSize = 10.5.sp,
                                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (sel) Color.White else TextMuted,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceElevated,
                                border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenExportSheet() }
                                    .testTag("btn_laporan_pdf")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "Ekspor Laporan",
                                        tint = AccentBlueBright,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentBlueBright)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = CurrencyHelper.formatRupiah(currentExpense),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (prevMonthExpense > 0) {
                            val expenseDiff = prevMonthExpense - currentExpense
                            val isSaved = expenseDiff >= 0
                            val pct = ((abs(expenseDiff) / prevMonthExpense) * 100).toInt()
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSaved) IncomeGreenSoft else ExpenseRedSoft
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = if (isSaved) IncomeGreen else ExpenseRed,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isSaved) "-$pct% vs $prevMonthName" else "+$pct% vs $prevMonthName",
                                        fontSize = 10.sp,
                                        color = if (isSaved) IncomeGreen else ExpenseRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Legend for overlapping curves
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp, 3.dp).background(ExpenseRed, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("$monthName $selectedYear", fontSize = 10.sp, color = TextMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp, 2.dp).background(TextSubtle.copy(alpha = 0.7f)))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("$prevMonthName (Pembanding)", fontSize = 10.sp, color = TextSubtle)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Overlapping Dual-Period Spending Activity Line Chart
                    DualPeriodComparisonLineChart(
                        currentValues = currentSeries,
                        previousValues = prevSeries,
                        labels = chartLabels,
                        currentLabel = monthName,
                        previousLabel = prevMonthName,
                        peakDayIndex = peakDayIdx,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val peakDayEntry = periodTransactions.filter { it.type == TransactionType.EXPENSE }
                        .groupBy { it.dayOfMonth }
                        .maxByOrNull { entry -> entry.value.sumOf { it.amount } }
                    val peakAmt = peakDayEntry?.value?.sumOf { it.amount } ?: 0.0
                    val peakDayNum = peakDayEntry?.key ?: 1
                    val dailyAvg = if (currentExpense > 0) currentExpense / daysInMonth.toDouble() else 0.0
                    val topCatSlice = expenseSlices.firstOrNull()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Per hari", fontSize = 10.sp, color = TextSubtle)
                            Text(CurrencyHelper.formatCompactRupiah(dailyAvg), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hari terboros", fontSize = 10.sp, color = TextSubtle)
                            Text(CurrencyHelper.formatCompactRupiah(peakAmt), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text("$peakDayNum $monthName", fontSize = 9.5.sp, color = TextMuted)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1.25f)
                                .then(
                                    if (topCatSlice != null) {
                                        Modifier.clickable {
                                            detailCategoryTarget = topCatSlice.category to TransactionType.EXPENSE
                                        }
                                    } else Modifier
                                )
                        ) {
                            Text("Kategori teratas", fontSize = 10.sp, color = TextSubtle)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = topCatSlice?.let { CategoryIconHelper.getCategoryIcon(it.category) } ?: Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = topCatSlice?.let { CategoryIconHelper.getCategoryColor(it.category, TransactionType.EXPENSE) } ?: TextSubtle,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = topCatSlice?.category ?: "Belum ada",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (topCatSlice != null) {
                                Text(CurrencyHelper.formatCompactRupiah(topCatSlice.amount), fontSize = 9.5.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }

        // ==================== 2. ARUS KAS BULANAN ====================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Ringkasan", fontSize = 10.sp, color = TextSubtle, fontWeight = FontWeight.SemiBold)
                    Text("Arus Kas Bulanan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)

                    Spacer(modifier = Modifier.height(14.dp))

                    val maxVal = maxOf(currentIncome, currentExpense, 1.0)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IncomeGreen))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(CurrencyHelper.formatRupiah(currentIncome), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Text("Pemasukan", fontSize = 11.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { (currentIncome / maxVal).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                        color = IncomeGreen,
                        trackColor = DarkSurfaceElevated
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpenseRed))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(CurrencyHelper.formatRupiah(currentExpense), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Text("Pengeluaran", fontSize = 11.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { (currentExpense / maxVal).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                        color = ExpenseRed,
                        trackColor = DarkSurfaceElevated
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isPositive = effNet >= 0
                        val netDiffText = (if (isPositive) "+" else "-") + CurrencyHelper.formatRupiah(abs(effNet))
                        Text(
                            text = if (isPositive) {
                                "Pada $monthName $selectedYear, arus kas bulanan Anda positif sebesar $netDiffText."
                            } else {
                                "Pada $monthName $selectedYear, pengeluaran melebihi pemasukan sebesar ${CurrencyHelper.formatRupiah(abs(effNet))}."
                            },
                            fontSize = 11.5.sp,
                            color = TextMuted,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // ==================== 3. TERENCANA (MASIH DIRENCANAKAN) ====================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Terencana", fontSize = 10.sp, color = TextSubtle, fontWeight = FontWeight.SemiBold)
                            Text("Masih direncanakan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Switch(
                            checked = isPlannedActive,
                            onCheckedChange = { isPlannedActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentBlue,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceElevated
                            )
                        )
                    }

                    Text(
                        text = "Tagihan dan pemasukan berulang yang masih dijadwalkan untuk periode ini.",
                        fontSize = 11.5.sp,
                        color = TextMuted,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    if (isPlannedActive) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pemasukan terencana · ${activeRecurring.count { it.type == TransactionType.INCOME }} item", fontSize = 11.5.sp, color = TextMuted)
                            Text("+${CurrencyHelper.formatRupiah(plannedIncTotal)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pengeluaran terencana · ${activeRecurring.count { it.type == TransactionType.EXPENSE }} item", fontSize = 11.5.sp, color = TextMuted)
                            Text("-${CurrencyHelper.formatRupiah(plannedExpTotal)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.45f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Jika semua berjalan sesuai rencana", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Proyeksi pemasukan", fontSize = 11.5.sp, color = TextMuted)
                            Text(CurrencyHelper.formatRupiah(currentIncome + plannedIncTotal), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Proyeksi pengeluaran", fontSize = 11.5.sp, color = TextMuted)
                            Text(CurrencyHelper.formatRupiah(currentExpense + plannedExpTotal), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        val projNet = (currentIncome + plannedIncTotal) - (currentExpense + plannedExpTotal)
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bersih akhir periode", fontSize = 11.5.sp, color = TextMuted)
                            Text(
                                text = (if (projNet >= 0) "+" else "-") + CurrencyHelper.formatRupiah(abs(projNet)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (projNet >= 0) IncomeGreen else ExpenseRed
                            )
                        }

                        if (activeRecurring.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.45f))
                            Spacer(modifier = Modifier.height(8.dp))
                            activeRecurring.take(5).forEach { rec ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onOpenRecurring() }
                                        .padding(vertical = 5.dp, horizontal = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.EventRepeat,
                                            contentDescription = null,
                                            tint = AccentBlueBright,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${rec.dayOfMonth} $monthName", fontSize = 10.5.sp, color = TextSubtle, modifier = Modifier.width(46.dp))
                                        Text(
                                            text = rec.title,
                                            fontSize = 12.sp,
                                            color = TextWhite,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = (if (rec.type == TransactionType.INCOME) "+" else "-") + CurrencyHelper.formatRupiah(rec.amount),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rec.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==================== 4. PENDAPATAN VS PENGELUARAN (KUMULATIF + SCRUBBER) ====================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pendapatan vs Pengeluaran", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Surface(shape = RoundedCornerShape(8.dp), color = DarkSurfaceElevated) {
                            Text("Kumulatif", fontSize = 10.sp, color = TextSubtle, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = (if (effNet >= 0) "+" else "-") + CurrencyHelper.formatCompactRupiah(abs(effNet)),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (effNet >= 0) IncomeGreen else ExpenseRed
                            )
                            Text("Selisih periode berjalan", fontSize = 10.5.sp, color = TextMuted)
                        }

                        val remainingPct = if (currentIncome > 0) (((currentIncome - currentExpense) / currentIncome) * 100).toInt().coerceIn(0, 100) else 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (remainingPct > 0) IncomeGreenSoft else ExpenseRedSoft
                        ) {
                            Text(
                                text = "$remainingPct% pemasukan tersisa",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingPct > 0) IncomeGreen else ExpenseRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp, 3.dp).background(IncomeGreen))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Pemasukan", fontSize = 10.5.sp, color = TextSubtle)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(CurrencyHelper.formatCompactRupiah(currentIncome), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp, 3.dp).background(ExpenseRed))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Pengeluaran", fontSize = 10.5.sp, color = TextSubtle)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(CurrencyHelper.formatCompactRupiah(currentExpense), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentIncome <= 0.0 && currentExpense <= 0.0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada data pemasukan atau pengeluaran pada periode ini",
                                fontSize = 11.5.sp,
                                color = TextSubtle,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        InteractiveCumulativeDualChart(
                            rawIncome = cumulativeData.rawIncomeList.ifEmpty { listOf(0.0, currentIncome) },
                            rawExpense = cumulativeData.rawExpenseList.ifEmpty { listOf(0.0, currentExpense) },
                            dayLabels = cumulativeData.dayLabels.ifEmpty { listOf("1 $monthName", "$daysInMonth $monthName") },
                            daysInMonth = daysInMonth
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isSurplus = currentIncome >= currentExpense
                        val statusMsg = when {
                            currentIncome <= 0.0 && currentExpense <= 0.0 -> "Catat pemasukan dan pengeluaran untuk melihat kurva perbandingan."
                            isSurplus -> "Pemasukan tetap unggul sebesar ${CurrencyHelper.formatCompactRupiah(currentIncome - currentExpense)} pada periode ini."
                            else -> "Pengeluaran melampaui pemasukan sebesar ${CurrencyHelper.formatCompactRupiah(currentExpense - currentIncome)}."
                        }
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (currentIncome <= 0.0 && currentExpense <= 0.0) Icons.Default.Info else if (isSurplus) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (currentIncome <= 0.0 && currentExpense <= 0.0) TextMuted else if (isSurplus) IncomeGreen else ExpenseRed,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = statusMsg, fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }
        }

        // ==================== 5. SKOR KESEHATAN KEUANGAN ====================
        item {
            val healthColor = when (healthReport.statusLevel) {
                1 -> IncomeGreen
                0 -> TrophyYellowLight
                else -> ExpenseRed
            }
            val healthSoftBg = when (healthReport.statusLevel) {
                1 -> IncomeGreenSoft
                0 -> TrophyYellowSoft
                else -> ExpenseRedSoft
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showHealthScoreSheet = true }
                    .testTag("card_laporan_health_score"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Wawasan mendalam", fontSize = 10.sp, color = TextSubtle, fontWeight = FontWeight.SemiBold)
                            Text("Skor Kesehatan Keuangan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = healthSoftBg,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = healthReport.statusLabel,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = healthColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        FinancialHealthScoreGauge(
                            score = healthScore,
                            label = "$healthScore/100",
                            modifier = Modifier.size(68.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Surplus Bersih", fontSize = 10.sp, color = TextSubtle)
                            Text(
                                text = (if (effNet >= 0) "+" else "-") + CurrencyHelper.formatRupiah(abs(effNet)),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (effNet >= 0) TextWhite else ExpenseRed
                            )
                        }
                        val deltaPct = healthReport.prevNetDeltaPct
                        val deltaBadgeText = if (deltaPct != null) {
                            val sign = if (deltaPct >= 0) "+" else ""
                            "$sign${String.format(Locale.US, "%.1f", deltaPct)}% vs $prevMonthName"
                        } else {
                            "Periode perdana"
                        }
                        val isDeltaPositive = (deltaPct ?: 0.0) >= 0.0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (deltaPct == null) DarkSurfaceElevated else if (isDeltaPositive) IncomeGreenSoft else ExpenseRedSoft
                        ) {
                            Text(
                                text = deltaBadgeText,
                                fontSize = 10.sp,
                                color = if (deltaPct == null) TextMuted else if (isDeltaPositive) IncomeGreen else ExpenseRed,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    healthReport.pillars.forEachIndexed { idx, pillar ->
                        val pillarColor = when (pillar.statusLevel) {
                            1 -> IncomeGreen
                            0 -> TrophyYellowLight
                            else -> ExpenseRed
                        }
                        val pillarIcon = when (idx) {
                            0 -> Icons.Default.Savings
                            1 -> Icons.Default.Speed
                            2 -> Icons.Default.CalendarToday
                            3 -> Icons.Default.AccountBalanceWallet
                            else -> Icons.Default.PieChart
                        }
                        val ratio = if (pillar.maxScore > 0) (pillar.score.toFloat() / pillar.maxScore.toFloat()).coerceIn(0f, 1f) else 0f
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = pillarIcon,
                                        contentDescription = null,
                                        tint = pillarColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(pillar.name, fontSize = 11.5.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(pillar.description, fontSize = 11.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${pillar.score}/${pillar.maxScore}",
                                        fontSize = 11.5.sp,
                                        color = pillarColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = pillarColor,
                                trackColor = DarkSurfaceElevated
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = TrophyYellowLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${healthReport.recommendationTitle} — ${healthReport.recommendationDesc}",
                                fontSize = 11.sp,
                                color = TextWhite,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // ==================== 6. RINCIAN PENDAPATAN & PENGELUARAN + DISISIHKAN ====================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rincian Pendapatan & Pengeluaran", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text("Kategori teratas berdasarkan jumlah · ketuk untuk detail", fontSize = 10.5.sp, color = TextSubtle)
                        }
                        if (incomeSlices.size > 4 || expenseSlices.size > 4) {
                            val totalCats = maxOf(incomeSlices.size, expenseSlices.size)
                            Text(
                                text = if (expandBreakdown) "Ringkas" else "Tampilkan semua ($totalCats)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentBlueBright,
                                modifier = Modifier
                                    .clickable { expandBreakdown = !expandBreakdown }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IncomeGreen))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pemasukan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Text(CurrencyHelper.formatRupiah(currentIncome), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val visibleIncome = if (expandBreakdown) incomeSlices else incomeSlices.take(4)
                    if (visibleIncome.isEmpty()) {
                        Text("Belum ada pemasukan pada periode ini", fontSize = 11.5.sp, color = TextSubtle, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        visibleIncome.forEach { slice ->
                            BreakdownCategoryRow(
                                slice = slice,
                                total = currentIncome,
                                txType = TransactionType.INCOME,
                                onClick = { detailCategoryTarget = slice.category to TransactionType.INCOME }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.45f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpenseRed))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pengeluaran", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Text(CurrencyHelper.formatRupiah(currentExpense), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val visibleExpense = if (expandBreakdown) expenseSlices else expenseSlices.take(4)
                    if (visibleExpense.isEmpty()) {
                        Text("Belum ada pengeluaran pada periode ini", fontSize = 11.5.sp, color = TextSubtle, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        visibleExpense.forEach { slice ->
                            BreakdownCategoryRow(
                                slice = slice,
                                total = currentExpense,
                                txType = TransactionType.EXPENSE,
                                onClick = { detailCategoryTarget = slice.category to TransactionType.EXPENSE }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.45f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Disisihkan ke tabungan & aset
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onNavigateToAssets() }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = AccentBlueBright, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Disisihkan ke tabungan & aset", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Kelola aset", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Transfer / investasi periode ini", fontSize = 11.sp, color = TextMuted)
                                Text(CurrencyHelper.formatRupiah(transferToSavingsAmount), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = AccentBlueBright)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Target tabungan terkumpul", fontSize = 11.sp, color = TextMuted)
                                Text(CurrencyHelper.formatRupiah(totalSavingsGoalsSaved), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = IncomeGreen)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Saldo kas & portofolio aset", fontSize = 11.sp, color = TextMuted)
                                Text(CurrencyHelper.formatRupiah(totalWalletBalance + totalAssetValue), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (effNet >= 0.0) {
                                    "${CurrencyHelper.formatRupiah(effNet)} dari sisa periode ini tetap ada di akun Anda."
                                } else {
                                    "Arus kas periode ini defisit ${CurrencyHelper.formatRupiah(abs(effNet))}."
                                },
                                fontSize = 10.5.sp,
                                color = if (effNet >= 0.0) IncomeGreen else ExpenseRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // ==================== 7. PENGELUARAN MENURUT HARI KERJA ====================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Pengeluaran Menurut Hari Kerja", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text("Hari pengeluaran puncak: $peakDayName", fontSize = 10.5.sp, color = TextSubtle)

                    Spacer(modifier = Modifier.height(14.dp))

                    DayOfWeekSpendingBarChart(
                        dayTotals = dayOfWeekTotals,
                        peakDayIndex = dowPeakIndex,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val weekdayAvg = if (dayOfWeekTotals.take(5).sum() > 0) dayOfWeekTotals.take(5).sum() / 5.0 else 0.0
                    val weekendAvg = if (dayOfWeekTotals.drop(5).sum() > 0) dayOfWeekTotals.drop(5).sum() / 2.0 else 0.0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hari kerja", fontSize = 10.sp, color = TextSubtle)
                            Text(CurrencyHelper.formatRupiah(weekdayAvg), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text("rata-rata harian", fontSize = 9.5.sp, color = TextMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Akhir pekan", fontSize = 10.sp, color = TextSubtle)
                            Text(CurrencyHelper.formatRupiah(weekendAvg), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text("rata-rata harian", fontSize = 9.5.sp, color = TextMuted)
                        }
                    }
                }
            }
        }

        // ==================== 8. CATATAN HARIAN ====================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hari demi hari", fontSize = 10.sp, color = TextSubtle, fontWeight = FontWeight.SemiBold)
                            Text("Catatan Harian", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(8.dp), color = DarkSurfaceElevated) {
                                Text(
                                    text = "${trackedDaysSet.size}/$daysInMonth hari terlacak",
                                    fontSize = 10.5.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = DarkSurfaceElevated,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .clickable { showDailyRecordInfoDialog = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info Catatan Harian",
                                        tint = TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (currentStreakDays > 0) "Rekor terbaik adalah $currentStreakDays hari berturut-turut"
                        else "Belum ada catatan transaksi di periode ini",
                        fontSize = 11.5.sp,
                        color = TextWhite,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DailyTrackingCalendarGrid(
                        daysInMonth = daysInMonth,
                        trackedDays = trackedDaysSet,
                        currentStreakDays = currentStreakDays,
                        onDayClick = { onAddTransaction(null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(IncomeGreen.copy(alpha = 0.5f)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ada catatan", fontSize = 10.5.sp, color = TextMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(DarkSurfaceElevated))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tidak ada catatan", fontSize = 10.5.sp, color = TextMuted)
                        }
                    }
                }
            }
        }
    }

    if (showStoryDialog) {
        BukuHarianStoryDialog(
            monthLabel = "$monthName $selectedYear",
            periodTransactions = periodTransactions,
            prevPeriodTransactions = prevPeriodTransactions,
            incomeTotal = currentIncome,
            expenseTotal = currentExpense,
            expenseSlices = expenseSlices,
            daysInMonth = daysInMonth,
            onDismiss = { showStoryDialog = false }
        )
    }

    if (showHealthScoreSheet) {
        FinancialHealthDetailSheet(
            score = healthScore,
            savingsRatePct = healthReport.savingsRatePct,
            netSurplusFormatted = CurrencyHelper.formatRupiah(effNet),
            report = healthReport,
            onDismiss = { showHealthScoreSheet = false },
            onNavigateToBudgets = {
                showHealthScoreSheet = false
                onNavigateToBudgets()
            },
            onNavigateToPlanner = {
                showHealthScoreSheet = false
                onNavigateToPlanner()
            },
            onAddTransaction = {
                showHealthScoreSheet = false
                onAddTransaction(null)
            }
        )
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

    if (showDailyRecordInfoDialog) {
        AlertDialog(
            onDismissRequest = { showDailyRecordInfoDialog = false },
            containerColor = DarkSurface,
            titleContentColor = TextWhite,
            textContentColor = TextMuted,
            title = { Text("Tentang Catatan Harian", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Kalender ini menandai setiap hari Anda mencatat transaksi. Menjaga rekor beruntun (streak) membantu mendeteksi pola belanja dan menjaga akurasi fitur Aman Dibelanjakan. Ketuk tanggal mana saja untuk mencatat transaksi baru.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showDailyRecordInfoDialog = false }) {
                    Text("Mengerti", color = AccentBlueBright, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
private fun BreakdownCategoryRow(
    slice: CategorySlice,
    total: Double,
    txType: TransactionType,
    onClick: () -> Unit
) {
    val ratio = if (total > 0) (slice.amount / total).toFloat() else 0f
    val pct = (ratio * 100).toInt()
    val color = CategoryIconHelper.getCategoryColor(slice.category, txType)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.18f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = CategoryIconHelper.getCategoryIcon(slice.category),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(slice.category, fontSize = 12.5.sp, color = TextWhite, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("$pct%", fontSize = 10.5.sp, color = TextSubtle, modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
            Spacer(modifier = Modifier.width(8.dp))
            Text(CurrencyHelper.formatRupiah(slice.amount), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }
        Spacer(modifier = Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = DarkSurfaceElevated
        )
    }
}

@Composable
private fun DualPeriodComparisonLineChart(
    currentValues: List<Double>,
    previousValues: List<Double>,
    labels: List<String>,
    currentLabel: String,
    previousLabel: String,
    peakDayIndex: Int,
    modifier: Modifier = Modifier
) {
    if (currentValues.isEmpty()) return
    val maxVal = maxOf(
        currentValues.maxOrNull() ?: 0.0,
        previousValues.maxOrNull() ?: 0.0,
        1000.0
    )
    var isDragging by remember(currentValues) { mutableStateOf(false) }
    var touchIndex by remember(currentValues) { mutableIntStateOf(-1) }

    val milestoneIndices = remember(labels.size) {
        val count = labels.size
        if (count <= 7) labels.indices.toList()
        else {
            val step = (count - 1) / 4f
            listOf(
                0,
                (step * 1).roundToInt().coerceIn(0, count - 1),
                (step * 2).roundToInt().coerceIn(0, count - 1),
                (step * 3).roundToInt().coerceIn(0, count - 1),
                count - 1
            ).distinct()
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (peakDayIndex in currentValues.indices && currentValues[peakDayIndex] > 0) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Puncak ${labels.getOrElse(peakDayIndex) { "" }}: ", fontSize = 10.5.sp, color = TextMuted)
                    Text(CurrencyHelper.formatCompactRupiah(currentValues[peakDayIndex]), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                }
            } else {
                Text("Geser pada grafik untuk membandingkan harian", fontSize = 10.sp, color = TextSubtle, modifier = Modifier.padding(vertical = 4.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(currentValues) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val canvasWidth = size.width.toFloat()
                        if (currentValues.isNotEmpty() && canvasWidth > 0f) {
                            isDragging = true
                            val idx = if (currentValues.size > 1) {
                                ((down.position.x / canvasWidth) * (currentValues.size - 1))
                                    .roundToInt()
                                    .coerceIn(0, currentValues.lastIndex)
                            } else 0
                            touchIndex = idx
                        }
                        try {
                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val dragChange = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!dragChange.pressed) break
                                val w = size.width.toFloat()
                                if (currentValues.isNotEmpty() && w > 0f) {
                                    val idx = if (currentValues.size > 1) {
                                        ((dragChange.position.x / w) * (currentValues.size - 1))
                                            .roundToInt()
                                            .coerceIn(0, currentValues.lastIndex)
                                    } else 0
                                    if (idx != touchIndex) {
                                        touchIndex = idx
                                    }
                                }
                                dragChange.consume()
                            }
                        } finally {
                            isDragging = false
                            touchIndex = -1
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val count = currentValues.size
                if (count < 2) return@Canvas

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                listOf(0.15f, 0.5f, 0.88f).forEach { f ->
                    drawLine(
                        color = DarkCardBorder.copy(alpha = 0.45f),
                        start = Offset(0f, h * f),
                        end = Offset(w, h * f),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                val stepX = w / (count - 1).toFloat()

                // Previous month curve (dashed subtle gray)
                if (previousValues.any { it > 0.0 }) {
                    val prevPts = (0 until count).map { i ->
                        val v = previousValues.getOrElse(i) { 0.0 }
                        val ny = (v / maxVal).toFloat().coerceIn(0.05f, 0.90f)
                        Offset(i * stepX, h * (0.92f - ny * 0.8f))
                    }
                    val prevPath = Path().apply {
                        moveTo(prevPts[0].x, prevPts[0].y)
                        for (i in 1 until prevPts.size) {
                            val p = prevPts[i - 1]
                            val c = prevPts[i]
                            val cx = (p.x + c.x) / 2f
                            cubicTo(cx, p.y, cx, c.y, c.x, c.y)
                        }
                    }
                    drawPath(
                        prevPath,
                        color = TextSubtle.copy(alpha = 0.65f),
                        style = Stroke(width = 1.8.dp.toPx(), pathEffect = dashEffect, cap = StrokeCap.Round)
                    )
                }

                // Current period curve (red solid + area gradient)
                val curPts = currentValues.mapIndexed { i, v ->
                    val ny = (v / maxVal).toFloat().coerceIn(0.05f, 0.90f)
                    Offset(i * stepX, h * (0.92f - ny * 0.8f))
                }
                val curPath = Path().apply {
                    moveTo(curPts[0].x, curPts[0].y)
                    for (i in 1 until curPts.size) {
                        val p = curPts[i - 1]
                        val c = curPts[i]
                        val cx = (p.x + c.x) / 2f
                        cubicTo(cx, p.y, cx, c.y, c.x, c.y)
                    }
                }
                val fillPath = Path().apply {
                    addPath(curPath)
                    lineTo(curPts.last().x, h)
                    lineTo(curPts.first().x, h)
                    close()
                }
                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(
                        listOf(ExpenseRed.copy(alpha = 0.28f), Color.Transparent)
                    )
                )
                drawPath(
                    curPath,
                    color = ExpenseRed,
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )

                if (isDragging && touchIndex in curPts.indices) {
                    val p = curPts[touchIndex]
                    drawLine(
                        color = ExpenseRed.copy(alpha = 0.75f),
                        start = Offset(p.x, 0f),
                        end = Offset(p.x, h),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    drawCircle(color = ExpenseRed.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = p)
                    drawCircle(color = ExpenseRed, radius = 5.5.dp.toPx(), center = p)
                    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = p)
                } else if (peakDayIndex in curPts.indices && currentValues[peakDayIndex] > 0) {
                    val pk = curPts[peakDayIndex]
                    drawCircle(color = ExpenseRed.copy(alpha = 0.25f), radius = 7.dp.toPx(), center = pk)
                    drawCircle(color = ExpenseRed, radius = 3.8.dp.toPx(), center = pk)
                }
            }

            if (isDragging && touchIndex in currentValues.indices) {
                val curV = currentValues[touchIndex]
                val prevV = previousValues.getOrElse(touchIndex) { 0.0 }
                val lbl = labels.getOrElse(touchIndex) { "" }
                val stepXPercent = touchIndex.toFloat() / (currentValues.size - 1).coerceAtLeast(1)
                val alignBias = (stepXPercent * 2f - 1f).coerceIn(-0.85f, 0.85f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.align(BiasAlignment(alignBias, -0.75f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(lbl, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(ExpenseRed, CircleShape))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("$currentLabel: ${CurrencyHelper.formatCompactRupiah(curV)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(TextSubtle, CircleShape))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("$previousLabel: ${CurrencyHelper.formatCompactRupiah(prevV)}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            milestoneIndices.forEachIndexed { idx, i ->
                Text(
                    text = labels.getOrElse(i) { "" },
                    fontSize = 9.5.sp,
                    color = if (i == touchIndex) AccentBlueBright else TextSubtle,
                    fontWeight = if (i == touchIndex) FontWeight.Bold else FontWeight.Normal,
                    textAlign = when (idx) {
                        0 -> TextAlign.Start
                        milestoneIndices.lastIndex -> TextAlign.End
                        else -> TextAlign.Center
                    }
                )
            }
        }
    }
}

@Composable
private fun InteractiveCumulativeDualChart(
    rawIncome: List<Double>,
    rawExpense: List<Double>,
    dayLabels: List<String>,
    daysInMonth: Int
) {
    val count = maxOf(rawIncome.size, rawExpense.size)
    val maxP = maxOf(rawIncome.maxOrNull() ?: 1.0, rawExpense.maxOrNull() ?: 1.0, 1.0)
    var isDragging by remember(rawIncome, rawExpense) { mutableStateOf(false) }
    var touchIdx by remember(rawIncome, rawExpense) { mutableIntStateOf(-1) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .pointerInput(count) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val canvasWidth = size.width.toFloat()
                        if (count > 0 && canvasWidth > 0f) {
                            isDragging = true
                            val idx = if (count > 1) {
                                ((down.position.x / canvasWidth) * (count - 1))
                                    .roundToInt()
                                    .coerceIn(0, count - 1)
                            } else 0
                            touchIdx = idx
                        }
                        try {
                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val dragChange = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!dragChange.pressed) break
                                val w = size.width.toFloat()
                                if (count > 0 && w > 0f) {
                                    val idx = if (count > 1) {
                                        ((dragChange.position.x / w) * (count - 1))
                                            .roundToInt()
                                            .coerceIn(0, count - 1)
                                    } else 0
                                    if (idx != touchIdx) {
                                        touchIdx = idx
                                    }
                                }
                                dragChange.consume()
                            }
                        } finally {
                            isDragging = false
                            touchIdx = -1
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                for (i in 0..3) {
                    val y = h * (i / 3f)
                    drawLine(DarkCardBorder.copy(alpha = 0.4f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }

                fun drawAreaSeries(pts: List<Double>, color: Color, strokeWidth: Float): List<Offset> {
                    if (pts.isEmpty()) return emptyList()
                    val linePath = Path()
                    val fillPath = Path()
                    val coords = mutableListOf<Offset>()
                    pts.forEachIndexed { i, v ->
                        val x = if (pts.size > 1) (i / (pts.size - 1f)) * w else 0f
                        val y = h - ((v / maxP).toFloat().coerceIn(0f, 1f) * (h * 0.9f))
                        coords.add(Offset(x, y))
                        if (i == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }
                    fillPath.lineTo(w, h)
                    fillPath.close()
                    drawPath(fillPath, Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent)))
                    drawPath(linePath, color = color, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                    coords.lastOrNull()?.let { endPt ->
                        drawCircle(color = color.copy(alpha = 0.25f), radius = 6.dp.toPx(), center = endPt)
                        drawCircle(color = color, radius = 3.5.dp.toPx(), center = endPt)
                    }
                    return coords
                }

                val incCoords = drawAreaSeries(rawIncome, IncomeGreen, 2.8.dp.toPx())
                val expCoords = drawAreaSeries(rawExpense, ExpenseRed, 2.5.dp.toPx())

                if (isDragging && touchIdx in 0 until count && count > 1) {
                    val x = (touchIdx / (count - 1f)) * w
                    drawLine(
                        color = Color.White.copy(alpha = 0.55f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                    )
                    incCoords.getOrNull(touchIdx)?.let { p ->
                        drawCircle(color = IncomeGreen.copy(alpha = 0.35f), radius = 8.dp.toPx(), center = p)
                        drawCircle(color = IncomeGreen, radius = 4.5.dp.toPx(), center = p)
                        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = p)
                    }
                    expCoords.getOrNull(touchIdx)?.let { p ->
                        drawCircle(color = ExpenseRed.copy(alpha = 0.35f), radius = 8.dp.toPx(), center = p)
                        drawCircle(color = ExpenseRed, radius = 4.5.dp.toPx(), center = p)
                        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = p)
                    }
                }
            }

            if (isDragging && touchIdx in 0 until count) {
                val inc = rawIncome.getOrElse(touchIdx) { rawIncome.lastOrNull() ?: 0.0 }
                val exp = rawExpense.getOrElse(touchIdx) { rawExpense.lastOrNull() ?: 0.0 }
                val lbl = dayLabels.getOrElse(touchIdx) { "Tgl ${touchIdx + 1}" }
                val stepXPercent = touchIdx.toFloat() / (count - 1).coerceAtLeast(1)
                val alignBias = (stepXPercent * 2f - 1f).coerceIn(-0.85f, 0.85f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.align(BiasAlignment(alignBias, -0.75f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(lbl, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(IncomeGreen, CircleShape))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Pemasukan: +${CurrencyHelper.formatCompactRupiah(inc)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(ExpenseRed, CircleShape))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Pengeluaran: -${CurrencyHelper.formatCompactRupiah(exp)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("1", fontSize = 9.5.sp, color = TextSubtle)
            Text("9", fontSize = 9.5.sp, color = TextSubtle)
            Text("17", fontSize = 9.5.sp, color = TextSubtle)
            Text("25", fontSize = 9.5.sp, color = TextSubtle)
            Text("$daysInMonth", fontSize = 9.5.sp, color = TextSubtle)
        }
    }
}
