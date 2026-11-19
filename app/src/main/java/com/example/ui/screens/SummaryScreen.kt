package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountWallet
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.dialogs.CsvExportSuccessDialog
import com.example.ui.dialogs.CustomDateRange
import com.example.ui.dialogs.CustomizeStatisticsDialog
import com.example.ui.dialogs.DateRangePickerDialog
import com.example.ui.dialogs.MonthYearPickerDialog
import com.example.ui.dialogs.PdfExportSuccessDialog
import com.example.ui.dialogs.StatCardCatalog
import com.example.ui.screens.summary.EksporLaporanSheet
import com.example.ui.screens.summary.PdfPreviewScreenDialog
import com.example.ui.screens.summary.SummaryKalenderView
import com.example.ui.screens.summary.SummaryLaporanView
import com.example.ui.screens.summary.SummaryStatistikView
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.AppThemeState
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.theme.getTopHeaderGradient
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.ChartDataHelper
import com.example.util.ChartPeriod
import com.example.util.CsvExportHelper
import com.example.util.CsvExportResult
import com.example.util.CurrencyHelper
import com.example.util.InstitutionLogoBadge
import com.example.util.PdfExportResult
import com.example.util.PdfReportHelper
import com.example.util.TransactionMathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTransactions: (category: String?) -> Unit = {},
    onNavigateToAccount: (walletName: String) -> Unit = {},
    onNavigateToAssets: () -> Unit = {},
    onNavigateToDebts: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onAddTransaction: (category: String?) -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {},
    onOpenTimelineAdd: () -> Unit = {},
    onOpenRecurring: () -> Unit = {},
    onOpenJalanUang: () -> Unit = {}
) {
    val context = LocalContext.current
    val selectedMonth by viewModel.selectedMonthIndex.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val monthOptions by viewModel.monthOptions.collectAsState()
    val selectedWallet by viewModel.summaryWalletFilter.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resetSummaryWalletFilter()
    }
    val summaryTab by viewModel.summaryTab.collectAsState()
    val summaryPeriod by viewModel.summaryPeriod.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val upcomingItems by viewModel.upcomingItems.collectAsState()
    val timelineScenarios by viewModel.timelineScenarios.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val labels by viewModel.labels.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsState()

    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    var showAccountFilterSheet by remember { mutableStateOf(false) }
    var showCustomizeStatsDialog by remember { mutableStateOf(false) }
    var showMonthYearPicker by remember { mutableStateOf(false) }
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showPdfPreviewDialog by remember { mutableStateOf(false) }
    var customRange by remember {
        mutableStateOf<CustomDateRange?>(CustomDateRange(1, selectedMonth, selectedYear, 28, selectedMonth, selectedYear))
    }

    val walletFilteredTransactions = remember(allTransactions, selectedWallet) {
        if (selectedWallet == "Semua") allTransactions
        else allTransactions.filter { tx ->
            tx.accountName.equals(selectedWallet, ignoreCase = true) ||
                (tx.type == TransactionType.TRANSFER && tx.toAccountName.equals(selectedWallet, ignoreCase = true))
        }
    }

    val effectivePeriodMode = if (summaryTab == 1) summaryPeriod else 0
    val periodTransactions = remember(walletFilteredTransactions, selectedMonth, selectedYear, effectivePeriodMode, customRange) {
        when (effectivePeriodMode) {
            1 -> walletFilteredTransactions.filter { it.year == selectedYear }
            2 -> {
                val r = customRange
                if (r != null) {
                    walletFilteredTransactions.filter { tx ->
                        val isAfterStart = tx.year > r.startYear || (tx.year == r.startYear && (tx.monthIndex > r.startMonth || (tx.monthIndex == r.startMonth && tx.dayOfMonth >= r.startDay)))
                        val isBeforeEnd = tx.year < r.endYear || (tx.year == r.endYear && (tx.monthIndex < r.endMonth || (tx.monthIndex == r.endMonth && tx.dayOfMonth <= r.endDay)))
                        isAfterStart && isBeforeEnd
                    }
                } else {
                    walletFilteredTransactions.filter { it.monthIndex == selectedMonth && it.year == selectedYear }
                }
            }
            else -> walletFilteredTransactions.filter { it.monthIndex == selectedMonth && it.year == selectedYear }
        }
    }

    val (computedIncomeTotal, incomeBreakdown) = remember(periodTransactions) {
        TransactionMathHelper.buildCategoryBreakdown(periodTransactions, TransactionType.INCOME)
    }
    val (computedExpenseTotal, expenseBreakdown) = remember(periodTransactions) {
        TransactionMathHelper.buildCategoryBreakdown(periodTransactions, TransactionType.EXPENSE)
    }

    val currentIncome = computedIncomeTotal
    val currentExpense = computedExpenseTotal
    val netCashFlow = currentIncome - currentExpense

    val chartData = remember(periodTransactions, selectedMonth, selectedYear) {
        ChartDataHelper.getExpenseTrend(periodTransactions, ChartPeriod.ONE_MONTH, selectedMonth, selectedYear)
    }

    val cumulativeData = remember(allTransactions, selectedMonth, selectedYear, selectedWallet) {
        ChartDataHelper.getCumulativeComparison(allTransactions, selectedMonth, selectedYear, selectedWallet)
    }

    val monthNamesShort = remember {
        listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    }
    val activeMonthName = monthNamesShort.getOrElse(selectedMonth) { "" }
    val selectedMonthLabel = "$activeMonthName $selectedYear".trim()
    val savingsRate = if (currentIncome > 0) (((currentIncome - currentExpense) / currentIncome) * 100).toInt() else 0
    val activePeriodLabel = if (effectivePeriodMode == 2 && customRange != null) {
        customRange?.label.orEmpty()
    } else if (effectivePeriodMode == 1) {
        "Tahun $selectedYear"
    } else {
        selectedMonthLabel
    }

    var pdfExportResult by remember { mutableStateOf<PdfExportResult?>(null) }
    var csvExportResult by remember { mutableStateOf<CsvExportResult?>(null) }

    val handleExportPdf: () -> Unit = {
        if (!isExporting) {
            isExporting = true
            val labelSnapshot = activePeriodLabel
            val txSnapshot = periodTransactions.toList()
            val expSnapshot = expenseBreakdown.toList()
            val incSnapshot = incomeBreakdown.toList()
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    PdfReportHelper.generateAndSavePdf(
                        context = context,
                        periodLabel = labelSnapshot,
                        walletName = selectedWallet,
                        incomeTotal = currentIncome,
                        expenseTotal = currentExpense,
                        netCashFlow = netCashFlow,
                        savingsRate = savingsRate,
                        expenseBreakdown = expSnapshot,
                        incomeBreakdown = incSnapshot,
                        transactions = txSnapshot
                    )
                }
                isExporting = false
                if (result != null) {
                    pdfExportResult = result
                    Toast.makeText(context, "Laporan PDF berhasil disimpan!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal membuat laporan PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val handleExportCsv: () -> Unit = {
        if (!isExporting) {
            isExporting = true
            val labelSnapshot = activePeriodLabel
            val txSnapshot = periodTransactions.toList()
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    CsvExportHelper.generateAndSaveCsv(
                        context = context,
                        periodLabel = labelSnapshot,
                        transactions = txSnapshot
                    )
                }
                isExporting = false
                if (result != null) {
                    csvExportResult = result
                    Toast.makeText(context, "Spreadsheet CSV berhasil diekspor!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal membuat file CSV", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(getTopHeaderGradient(AccentBlue))
    ) {
        // ==================== HEADER ATAS ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Ringkasan",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { showCustomizeStatsDialog = true },
                    modifier = Modifier.testTag("btn_customize_statistics")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Sesuaikan Statistik",
                        tint = if (AppThemeState.isDarkMode) Color.White else TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showAccountFilterSheet = true }
                        .testTag("btn_account_filter_sheet")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Filter berdasarkan Akun",
                            tint = AccentBlueBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (selectedWallet == "Semua") "Semua" else selectedWallet,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // ==================== PEMILIH BULAN HORIZONTAL ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurfaceElevated,
                modifier = Modifier
                    .padding(start = 12.dp, end = 6.dp)
                    .clickable { showMonthYearPicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Pilih Bulan dan Tahun",
                        tint = AccentBlueBright,
                        modifier = Modifier.size(15.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            val monthListState = rememberLazyListState()
            LaunchedEffect(selectedMonth, selectedYear, monthOptions) {
                val targetIdx = monthOptions.indexOfFirst { it.monthIndex == selectedMonth && it.year == selectedYear }
                    .let { if (it >= 0) it else monthOptions.indexOfFirst { opt -> opt.monthIndex == selectedMonth } }
                if (targetIdx >= 0) {
                    monthListState.animateScrollToItem(targetIdx)
                }
            }
            LazyRow(
                state = monthListState,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(monthOptions, key = { it.year * 100 + it.monthIndex }) { option ->
                    val isSelected = selectedMonth == option.monthIndex && selectedYear == option.year
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) AccentBlue else DarkSurfaceElevated,
                        modifier = Modifier.clickable { viewModel.selectMonthAndYear(option.monthIndex, option.year) }
                    ) {
                        Text(
                            text = option.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // ==================== TAB TAMPILAN: STATISTIK | LAPORAN | KALENDER ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                Triple(0, "Statistik", Icons.Default.BarChart),
                Triple(1, "Laporan", Icons.Default.Description),
                Triple(2, "Kalender", Icons.Default.CalendarMonth)
            )

            tabs.forEach { (index, title, icon) ->
                val isSelected = summaryTab == index
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) AccentBlue else DarkSurfaceElevated,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.setSummaryTab(index) }
                        .testTag("summary_tab_${title.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextMuted
                        )
                    }
                }
            }
        }

        // ==================== MODE SEKILAS KHUSUS TAB LAPORAN ====================
        if (summaryTab == 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sekilas",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )

                Row(
                    modifier = Modifier
                        .background(DarkSurfaceElevated, RoundedCornerShape(14.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val periodOptions = listOf(
                        Triple(0, "Bulanan", Icons.Default.CalendarMonth),
                        Triple(1, "Tahunan", Icons.Default.BarChart),
                        Triple(2, "Kustom", Icons.Default.FilterList)
                    )

                    periodOptions.forEach { (index, title, icon) ->
                        val isSelected = summaryPeriod == index
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) AccentBlue else Color.Transparent,
                            modifier = Modifier.clickable {
                                viewModel.setSummaryPeriod(index)
                                if (index == 2) showCustomDateDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            if (summaryPeriod == 2 && customRange != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showCustomDateDialog = true }
                        ) {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = customRange?.label ?: "Pilih Rentang",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setSummaryPeriod(0) },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Reset rentang", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // ==================== TAB CONTENT ====================
        when (summaryTab) {
            0 -> {
                SummaryStatistikView(
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    monthName = activeMonthName,
                    monthOptions = monthOptions,
                    allTransactions = allTransactions,
                    periodTransactions = periodTransactions,
                    recurringTransactions = recurringTransactions,
                    upcomingItems = upcomingItems,
                    timelineScenarios = timelineScenarios,
                    wallets = wallets,
                    assets = assets,
                    debts = debts,
                    budgets = budgets,
                    labels = labels,
                    savingsGoals = savingsGoals,
                    settings = settings,
                    currentIncome = currentIncome,
                    currentExpense = currentExpense,
                    chartData = chartData,
                    expenseSlices = expenseBreakdown,
                    incomeSlices = incomeBreakdown,
                    selectedWallet = selectedWallet,
                    isBalanceHidden = isBalanceHidden,
                    onOpenCustomize = { showCustomizeStatsDialog = true },
                    onOpenTimelineAdd = onOpenTimelineAdd,
                    onDeleteTimelineScenario = { id -> viewModel.deleteTimelineScenario(id) },
                    onOpenRecurring = onOpenRecurring,
                    onOpenJalanUang = onOpenJalanUang,
                    onNavigateToAccount = onNavigateToAccount,
                    onTransactionClick = onTransactionClick
                )
            }

            1 -> {
                SummaryLaporanView(
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    monthName = activeMonthName,
                    monthOptions = monthOptions,
                    allTransactions = allTransactions,
                    periodTransactions = periodTransactions,
                    recurringTransactions = recurringTransactions,
                    wallets = wallets,
                    assets = assets,
                    debts = debts,
                    budgets = budgets,
                    categories = categories,
                    savingsGoals = savingsGoals,
                    settings = settings,
                    currentIncome = currentIncome,
                    currentExpense = currentExpense,
                    chartData = chartData,
                    cumulativeData = cumulativeData,
                    expenseSlices = expenseBreakdown,
                    incomeSlices = incomeBreakdown,
                    selectedWallet = selectedWallet,
                    onOpenExportSheet = { showExportSheet = true },
                    onOpenRecurring = onOpenRecurring,
                    onNavigateToAccount = onNavigateToAccount,
                    onNavigateToAssets = onNavigateToAssets,
                    onNavigateToPlanner = onNavigateToPlanner,
                    onNavigateToBudgets = onNavigateToBudgets,
                    onAddTransaction = onAddTransaction,
                    onTransactionClick = onTransactionClick
                )
            }

            else -> {
                SummaryKalenderView(
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    monthName = activeMonthName,
                    transactions = periodTransactions,
                    onTransactionClick = onTransactionClick,
                    onAddTransactionForDate = { onAddTransaction(null) },
                    onSelectMonth = { viewModel.selectMonth(it) },
                    onSelectMonthYear = { m, y -> viewModel.selectMonthAndYear(m, y) },
                    onNavigateToTransactions = { onNavigateToTransactions(null) }
                )
            }
        }
    }

    if (showAccountFilterSheet) {
        FilterAkunBottomSheet(
            wallets = wallets,
            selectedWallet = selectedWallet,
            onSelectWallet = { name ->
                viewModel.selectSummaryWalletFilter(name)
                showAccountFilterSheet = false
            },
            onDismiss = { showAccountFilterSheet = false }
        )
    }

    if (showCustomizeStatsDialog) {
        val currentConfigs = StatCardCatalog.buildConfigs(
            order = settings.statistikCardsOrder,
            hidden = settings.hiddenStatistikCards
        )
        CustomizeStatisticsDialog(
            initialCards = currentConfigs,
            isSideBySideTrends = settings.trenBerdampinganEnabled,
            onDismiss = { showCustomizeStatsDialog = false },
            onSave = { updatedCards, sideBySide ->
                viewModel.updateStatistikConfig(
                    order = updatedCards.map { it.id },
                    hidden = updatedCards.filter { !it.isVisible }.map { it.id },
                    sideBySide = sideBySide
                )
            }
        )
    }

    if (showExportSheet) {
        EksporLaporanSheet(
            periodLabel = activePeriodLabel,
            transactionCount = periodTransactions.size,
            onSelectPdfPreview = { showPdfPreviewDialog = true },
            onExportExcelCsv = handleExportCsv,
            onDismiss = { showExportSheet = false }
        )
    }

    if (showPdfPreviewDialog) {
        PdfPreviewScreenDialog(
            periodLabel = activePeriodLabel,
            walletName = selectedWallet,
            incomeTotal = currentIncome,
            expenseTotal = currentExpense,
            netCashFlow = netCashFlow,
            savingsRate = savingsRate,
            expenseBreakdown = expenseBreakdown,
            incomeBreakdown = incomeBreakdown,
            transactions = periodTransactions,
            onGenerateAndSavePdf = {
                showPdfPreviewDialog = false
                handleExportPdf()
            },
            onDismiss = { showPdfPreviewDialog = false }
        )
    }

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            currentMonth = selectedMonth,
            currentYear = selectedYear,
            onDismiss = { showMonthYearPicker = false },
            onSelectMonthYear = { m, y -> viewModel.selectMonthAndYear(m, y) }
        )
    }

    if (showCustomDateDialog) {
        DateRangePickerDialog(
            initialRange = customRange,
            onDismiss = { showCustomDateDialog = false },
            onConfirm = { range ->
                customRange = range
                viewModel.setSummaryPeriod(2)
                showCustomDateDialog = false
            }
        )
    }

    pdfExportResult?.let { result ->
        PdfExportSuccessDialog(
            exportResult = result,
            periodLabel = activePeriodLabel,
            onDismiss = { pdfExportResult = null }
        )
    }

    csvExportResult?.let { result ->
        CsvExportSuccessDialog(
            exportResult = result,
            periodLabel = activePeriodLabel,
            onDismiss = { csvExportResult = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterAkunBottomSheet(
    wallets: List<AccountWallet>,
    selectedWallet: String,
    onSelectWallet: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter berdasarkan Akun",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isSelAll = selectedWallet == "Semua"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelAll) AccentBlue.copy(alpha = 0.16f) else DarkSurfaceElevated,
                        border = BorderStroke(1.dp, if (isSelAll) AccentBlue else DarkCardBorder.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectWallet("Semua") }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(AccentBlue.copy(alpha = 0.18f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AccentBlueBright, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Semua Akun", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text("${wallets.size} dompet aktif", fontSize = 11.5.sp, color = TextMuted)
                                }
                            }
                            if (isSelAll) {
                                Icon(Icons.Default.Check, contentDescription = "Dipilih", tint = AccentBlueBright)
                            }
                        }
                    }
                }

                items(wallets, key = { it.id }) { wallet ->
                    val isSel = selectedWallet.equals(wallet.name, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSel) AccentBlue.copy(alpha = 0.16f) else DarkSurfaceElevated,
                        border = BorderStroke(1.dp, if (isSel) AccentBlue else DarkCardBorder.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectWallet(wallet.name) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                InstitutionLogoBadge(walletName = wallet.name, iconName = wallet.iconName, size = 34.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(wallet.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text(CurrencyHelper.formatRupiah(wallet.balance), fontSize = 11.5.sp, color = TextMuted)
                                }
                            }
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = "Dipilih", tint = AccentBlueBright)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }
            }
        }
    }
}
