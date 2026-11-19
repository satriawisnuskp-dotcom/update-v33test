package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountWallet
import com.example.data.model.MainCategory
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.defaultDashboardWidgetOrder
import com.example.ui.dialogs.BudgetDetailSheet
import com.example.ui.dialogs.CustomizeDashboardWidgetsSheet
import com.example.ui.screens.summary.UpcomingTimelineCard
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.AppThemeState
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExpenseHeatTile
import com.example.ui.theme.ExpenseHeatTileActive
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TrophyYellowLight
import com.example.ui.theme.getBalanceCardGradient
import com.example.ui.theme.getTopHeaderGradient
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.BudgetMathHelper
import com.example.util.CategoryIconHelper
import com.example.util.ComputedBudgetItem
import com.example.util.CurrencyHelper
import com.example.util.SafeToSpendCalculator
import com.example.util.SafeToSpendSummary
import java.util.Calendar
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onAddUpcomingClick: () -> Unit,
    onLevelClick: () -> Unit = {},
    onNavigateToSummary: (subTab: Int) -> Unit = {},
    onOpenCalculators: () -> Unit = {},
    onOpenSplitBill: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onEditTransaction: (Transaction) -> Unit = {},
    onOpenBudget: () -> Unit = {},
    onOpenRecurring: () -> Unit = {},
    onOpenProfileSync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val syncStatus by viewModel.syncStatus.collectAsState()
    val totalBalance by viewModel.totalWalletBalance.collectAsState()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsState()
    val upcomingItems by viewModel.upcomingItems.collectAsState()
    val timelineScenarios by viewModel.timelineScenarios.collectAsState()
    val selectedMonth by viewModel.selectedMonthIndex.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val monthName = remember(selectedMonth) {
        when (selectedMonth) {
            1 -> "Januari"; 2 -> "Februari"; 3 -> "Maret"; 4 -> "April"
            5 -> "Mei"; 6 -> "Juni"; 7 -> "Juli"; 8 -> "Agustus"
            9 -> "September"; 10 -> "Oktober"; 11 -> "November"; else -> "Desember"
        }
    }

    val safeSummary = remember(
        wallets, allTransactions, recurringTransactions, upcomingItems, savingsGoals, settings
    ) {
        SafeToSpendCalculator.calculate(
            wallets = wallets,
            transactions = allTransactions,
            recurringTransactions = recurringTransactions,
            upcomingItems = upcomingItems,
            savingsGoals = savingsGoals,
            settings = settings
        )
    }

    val monthlyBudgetItems = remember(budgets, allTransactions, wallets, categories, selectedMonth, selectedYear) {
        budgets
            .filter { it.frequency == "Bulanan" }
            .ifEmpty { budgets }
            .map { b ->
                BudgetMathHelper.computeBudgetItem(
                    budget = b,
                    allTransactions = allTransactions,
                    wallets = wallets,
                    allCategories = categories,
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear
                )
            }
    }

    val currentCal = remember { Calendar.getInstance() }
    val currentDay = currentCal.get(Calendar.DAY_OF_MONTH)
    val currentMonth = currentCal.get(Calendar.MONTH) + 1
    val currentYear = currentCal.get(Calendar.YEAR)
    val daysInSelectedMonth = remember(selectedMonth, selectedYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val daysRemainingInMonth = (daysInSelectedMonth - currentDay).coerceAtLeast(1)

    val budgetAlertsCount = remember(monthlyBudgetItems, settings.budgetWarningPercent) {
        monthlyBudgetItems.count { it.rawPercentInt >= settings.budgetWarningPercent }
    }
    val recurringAlertsCount = remember(recurringTransactions, currentDay) {
        recurringTransactions.count { it.isActive && it.reminderEnabled && it.dayOfMonth in currentDay..(currentDay + 3) }
    }
    val notificationCount = upcomingItems.size + budgetAlertsCount + recurringAlertsCount

    var showCustomizeWidgetsSheet by remember { mutableStateOf(false) }
    var showSafeToSpendDetailSheet by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showSpendingHeatmapInfo by remember { mutableStateOf(false) }
    var detailBudgetItem by remember { mutableStateOf<ComputedBudgetItem?>(null) }

    val hiddenSet = remember(settings.hiddenDashboardWidgets) { settings.hiddenDashboardWidgets.toSet() }
    val orderedWidgets = remember(settings.dashboardWidgetsOrder, hiddenSet) {
        (settings.dashboardWidgetsOrder + defaultDashboardWidgetOrder)
            .distinct()
            .filter { it !in hiddenSet }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(getTopHeaderGradient(AccentBlue))
    ) {
        // ==================== HEADER ATAS DASBOR ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Dasbor",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SyncStatusBadge(
                    syncStatus = syncStatus,
                    compact = true,
                    onClick = onOpenProfileSync,
                    modifier = Modifier.testTag("badge_dashboard_sync_status")
                )

                val headerIconTint = if (AppThemeState.isDarkMode) Color.White else TextWhite

                IconButton(
                    onClick = { showCustomizeWidgetsSheet = true },
                    modifier = Modifier.testTag("btn_customize_dashboard")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Sesuaikan Dasbor",
                        tint = headerIconTint,
                        modifier = Modifier.size(21.dp)
                    )
                }

                IconButton(
                    onClick = { showNotificationsDialog = true },
                    modifier = Modifier.testTag("btn_dashboard_notifications")
                ) {
                    if (notificationCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = ExpenseRed, contentColor = Color.White) {
                                    Text("$notificationCount", fontSize = 9.sp)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifikasi & Pengingat",
                                tint = headerIconTint,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifikasi & Pengingat",
                            tint = headerIconTint,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (orderedWidgets.isEmpty()) {
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
                            Text("Semua widget dasbor disembunyikan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showCustomizeWidgetsSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Atur Widget Dasbor", color = Color.White)
                            }
                        }
                    }
                }
            }

            orderedWidgets.forEach { widgetId ->
                when (widgetId) {
                    "SAFE_TO_SPEND" -> item(key = "WIDGET_SAFE_TO_SPEND") {
                        SafeToSpendHeroCard(
                            summary = safeSummary,
                            isBalanceHidden = isBalanceHidden,
                            onClick = { showSafeToSpendDetailSheet = true }
                        )
                    }

                    "BUDGET" -> item(key = "WIDGET_BUDGET") {
                        DashboardBudgetWidgetCard(
                            budgetItems = monthlyBudgetItems,
                            allTransactions = allTransactions,
                            wallets = wallets,
                            categories = categories,
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear,
                            daysRemaining = daysRemainingInMonth,
                            warningPercent = settings.budgetWarningPercent,
                            isBalanceHidden = isBalanceHidden,
                            onOpenAllBudgets = onOpenBudget,
                            onSelectBudget = { detailBudgetItem = it }
                        )
                    }

                    "UPCOMING" -> item(key = "WIDGET_UPCOMING") {
                        UpcomingTimelineCard(
                            currentBalance = totalBalance,
                            upcomingItems = upcomingItems,
                            recurringTransactions = recurringTransactions,
                            timelineScenarios = timelineScenarios,
                            isBalanceHidden = isBalanceHidden,
                            onAddClick = onAddUpcomingClick,
                            onDeleteScenario = { viewModel.deleteTimelineScenario(it) },
                            onRecurringClick = onOpenRecurring
                        )
                    }

                    "SPENDING_ACTIVITY" -> item(key = "WIDGET_SPENDING_ACTIVITY") {
                        DashboardSpendingHeatmapCard(
                            monthName = monthName,
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear,
                            todayDay = currentDay,
                            todayMonth = currentMonth,
                            todayYear = currentYear,
                            transactions = allTransactions,
                            isBalanceHidden = isBalanceHidden,
                            onInfoClick = { showSpendingHeatmapInfo = true },
                            onNavigateToTransactions = onNavigateToTransactions
                        )
                    }
                }
            }
        }
    }

    if (showCustomizeWidgetsSheet) {
        CustomizeDashboardWidgetsSheet(
            initialOrder = settings.dashboardWidgetsOrder,
            initialHidden = settings.hiddenDashboardWidgets,
            isBalanceHidden = isBalanceHidden,
            onToggleBalanceHidden = { viewModel.toggleBalanceVisibility() },
            onSave = { order, hidden -> viewModel.updateDashboardWidgets(order, hidden) },
            onDismiss = { showCustomizeWidgetsSheet = false }
        )
    }

    if (showSafeToSpendDetailSheet) {
        AmanDibelanjakanDetailSheet(
            summary = safeSummary,
            isBalanceHidden = isBalanceHidden,
            onDismiss = { showSafeToSpendDetailSheet = false }
        )
    }

    if (showNotificationsDialog) {
        DashboardNotificationsDialog(
            viewModel = viewModel,
            onDismiss = { showNotificationsDialog = false }
        )
    }

    if (showSpendingHeatmapInfo) {
        AlertDialog(
            onDismissRequest = { showSpendingHeatmapInfo = false },
            containerColor = DarkSurface,
            title = { Text("Aktivitas Pengeluaran Harian", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Kalender panas menampilkan intensitas pengeluaran harian selama bulan ini. Warna merah lebih pekat berarti pengeluaran hari tersebut lebih tinggi dibanding hari lain. Ketuk tanggal mana saja untuk melihat ringkasan transaksi hari itu.",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSpendingHeatmapInfo = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Mengerti", color = Color.White)
                }
            }
        )
    }

    detailBudgetItem?.let { currentItem ->
        BudgetDetailSheet(
            item = currentItem,
            daysRemaining = daysRemainingInMonth,
            warningPercent = settings.budgetWarningPercent,
            onEdit = {
                detailBudgetItem = null
                onOpenBudget()
            },
            onDelete = {
                viewModel.deleteBudget(currentItem.budget.id)
                detailBudgetItem = null
            },
            onDismiss = { detailBudgetItem = null }
        )
    }
}

@Composable
private fun SafeToSpendHeroCard(
    summary: SafeToSpendSummary,
    isBalanceHidden: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .testTag("card_safe_to_spend")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(getBalanceCardGradient(AccentBlue))
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AMAN DIBELANJAKAN HARI INI",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${summary.shoppingWalletCount} akun belanja",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Rincian aman dibelanjakan",
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isBalanceHidden) "Rp ••••••" else CurrencyHelper.formatRupiah(summary.dailySafeToSpend),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "per hari sampai ${summary.nextPaydayShortLabel}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.88f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = { summary.dailyProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (summary.dailyProgress >= 0.9f) ExpenseRed else Color(0xFF34D399),
                    trackColor = Color.White.copy(alpha = 0.24f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = if (isBalanceHidden) "•••• keluar hari ini"
                        else "${CurrencyHelper.formatRupiah(summary.spentToday)} keluar hari ini",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SafeToSpendMetricTile(
                        title = "Bebas sampai gajian",
                        value = if (isBalanceHidden) "••••" else CurrencyHelper.formatRupiah(summary.freeUntilPayday),
                        modifier = Modifier.weight(1f)
                    )
                    SafeToSpendMetricTile(
                        title = "${summary.billCountBeforePayday} tagihan",
                        value = if (isBalanceHidden) "••••" else CurrencyHelper.formatRupiah(summary.unpaidBillsBeforePayday),
                        modifier = Modifier.weight(1f)
                    )
                    SafeToSpendMetricTile(
                        title = "Untuk tujuan",
                        value = if (isBalanceHidden) "••••" else CurrencyHelper.formatRupiah(summary.goalReserves),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SafeToSpendMetricTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.22f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(title, fontSize = 10.sp, color = Color.White.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmanDibelanjakanDetailSheet(
    summary: SafeToSpendSummary,
    isBalanceHidden: Boolean,
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
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AccentBlue.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = AccentBlueBright, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Aman dibelanjakan", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                }
            }

            // Blue Summary Banner Card matching video 01:57
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(getBalanceCardGradient(AccentBlue))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sampai ${summary.nextPaydayShortLabel} · ${summary.daysUntilPayday} hari",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (isBalanceHidden) "Rp ••••••" else CurrencyHelper.formatRupiah(summary.dailySafeToSpend),
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "per hari",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBalanceHidden) "•••• ÷ ${summary.daysUntilPayday} hari"
                            else "${CurrencyHelper.formatRupiah(summary.freeUntilPayday)} ÷ ${summary.daysUntilPayday} hari",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Text(
                text = if (isBalanceHidden) "Ke mana saldo Anda pergi" else "Ke mana ${CurrencyHelper.formatRupiah(summary.shoppingBalance)} Anda pergi",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SafeDetailBreakdownRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = AccentBlueBright,
                        title = "Di akun belanja Anda",
                        subtitle = "${summary.shoppingWalletCount} akun · Akun tabungan tidak dihitung",
                        amountText = if (isBalanceHidden) "••••" else CurrencyHelper.formatRupiah(summary.shoppingBalance),
                        valueColor = TextWhite
                    )
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                    SafeDetailBreakdownRow(
                        icon = Icons.Default.EventRepeat,
                        iconTint = ExpenseRed,
                        title = "Tagihan sebelum gajian",
                        subtitle = summary.billPreviewSubtitle,
                        amountText = if (isBalanceHidden) "••••" else "-${CurrencyHelper.formatRupiah(summary.unpaidBillsBeforePayday)}",
                        valueColor = ExpenseRed
                    )
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                    SafeDetailBreakdownRow(
                        icon = Icons.Default.Flag,
                        iconTint = TrophyYellowLight,
                        title = "Disisihkan untuk tujuan",
                        subtitle = "Tujuan dengan tenggat, siklus ini",
                        amountText = if (isBalanceHidden) "••••" else "-${CurrencyHelper.formatRupiah(summary.goalReserves)}",
                        valueColor = TrophyYellowLight
                    )
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                    SafeDetailBreakdownRow(
                        icon = Icons.Default.Check,
                        iconTint = IncomeGreen,
                        title = "Bebas sampai gajian",
                        subtitle = if (isBalanceHidden) "${summary.daysUntilPayday} hari tersisa"
                        else "${CurrencyHelper.formatCompactRupiah(summary.freeUntilPayday)} ÷ ${summary.daysUntilPayday} hr = ${CurrencyHelper.formatRupiah(summary.dailySafeToSpend)}/hr",
                        amountText = if (isBalanceHidden) "••••" else CurrencyHelper.formatRupiah(summary.freeUntilPayday),
                        valueColor = IncomeGreen
                    )
                }
            }

            Text(
                text = "Dihitung otomatis dari saldo akun belanja dikurangi tagihan yang jatuh tempo sebelum tanggal gajian dan cadangan target tabungan.",
                fontSize = 11.sp,
                color = TextMuted,
                lineHeight = 15.sp
            )

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Selesai", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun SafeDetailBreakdownRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    amountText: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconTint.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                Text(subtitle, fontSize = 11.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(amountText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun DashboardBudgetWidgetCard(
    budgetItems: List<ComputedBudgetItem>,
    allTransactions: List<Transaction>,
    wallets: List<AccountWallet>,
    categories: List<MainCategory>,
    selectedMonth: Int,
    selectedYear: Int,
    daysRemaining: Int,
    warningPercent: Int,
    isBalanceHidden: Boolean,
    onOpenAllBudgets: () -> Unit,
    onSelectBudget: (ComputedBudgetItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val shortMonths = remember {
        listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_dashboard_budget")
    ) {
        if (budgetItems.isEmpty()) {
            // Video 00:49 compact empty state card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAllBudgets() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AccentBlue.copy(alpha = 0.16f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = AccentBlueBright, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Belum ada anggaran", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Buat anggaran pertama Anda untuk melacak pengeluaran dan mencapai tujuan keuangan Anda",
                            fontSize = 11.5.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Buka anggaran",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { expanded = !expanded }
                            .padding(end = 6.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Text("Anggaran", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceElevated
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$daysRemaining hr tersisa", fontSize = 10.5.sp, color = TextMuted)
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Lihat tren 6 bulan",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenAllBudgets() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Tampilkan semua", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccentBlueBright)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AccentBlueBright, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val totalAllocated = budgetItems.sumOf { it.budget.allocated }
                val totalSpent = budgetItems.sumOf { it.spent }
                val ratio = if (totalAllocated > 0.0) (totalSpent / totalAllocated).toFloat().coerceIn(0f, 1f) else 0f
                val isOver = totalSpent > totalAllocated && totalAllocated > 0.0
                val diff = totalAllocated - totalSpent

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Terpakai sejauh ini", fontSize = 11.sp, color = TextMuted)
                        Text(
                            text = if (isBalanceHidden) "Rp ••••••" else CurrencyHelper.formatRupiah(totalSpent),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOver) ExpenseRed else TextWhite
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total anggaran", fontSize = 11.sp, color = TextMuted)
                        Text(
                            text = if (isBalanceHidden) "Rp ••••••" else CurrencyHelper.formatRupiah(totalAllocated),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlueBright
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        isOver -> ExpenseRed
                        (ratio * 100) >= warningPercent -> TrophyYellowLight
                        else -> IncomeGreen
                    },
                    trackColor = DarkSurfaceElevated
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBalanceHidden) "Status anggaran tersembunyi"
                        else if (isOver) "Melebihi anggaran ${CurrencyHelper.formatCompactRupiah(abs(diff))}"
                        else "Tersisa ${CurrencyHelper.formatCompactRupiah(diff)} · ${(ratio * 100).toInt()}% terpakai",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOver) ExpenseRed else IncomeGreen
                    )
                }

                // Expanded 6-month grouped bar chart matching video 01:08
                AnimatedVisibility(visible = expanded) {
                    val sixMonthsHistory = remember(budgetItems, allTransactions, wallets, categories, selectedMonth, selectedYear) {
                        (5 downTo 0).map { offset ->
                            var m = selectedMonth - offset
                            var y = selectedYear
                            while (m <= 0) {
                                m += 12
                                y -= 1
                            }
                            val spentInM = budgetItems.sumOf { bItem ->
                                BudgetMathHelper.computeBudgetItem(
                                    budget = bItem.budget,
                                    allTransactions = allTransactions,
                                    wallets = wallets,
                                    allCategories = categories,
                                    selectedMonth = m,
                                    selectedYear = y
                                ).spent
                            }
                            Triple(shortMonths.getOrElse(m) { "" }, totalAllocated, spentInM)
                        }
                    }
                    val maxBarVal = remember(sixMonthsHistory) {
                        sixMonthsHistory.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(1.0) ?: 1.0
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("6 Bulan Terakhir", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(AccentBlueBright))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Anggaran", fontSize = 10.sp, color = TextMuted)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(IncomeGreen))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Terpakai", fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    sixMonthsHistory.forEachIndexed { idx, (_, alloc, spent) ->
                                        val allocRatio = (alloc / maxBarVal).toFloat().coerceIn(0.06f, 1f)
                                        val spentRatio = (spent / maxBarVal).toFloat().coerceIn(if (spent > 0) 0.06f else 0.02f, 1f)
                                        val spentColor = if (spent > alloc && alloc > 0) ExpenseRed else IncomeGreen
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(9.dp)
                                                        .height((60 * allocRatio).dp)
                                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                                        .background(AccentBlueBright.copy(alpha = if (idx == 5) 1f else 0.65f))
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .width(9.dp)
                                                        .height((60 * spentRatio).dp)
                                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                                        .background(spentColor.copy(alpha = if (idx == 5) 1f else 0.7f))
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    sixMonthsHistory.forEachIndexed { idx, (lbl, _, _) ->
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = lbl,
                                                fontSize = 9.5.sp,
                                                fontWeight = if (idx == 5) FontWeight.Bold else FontWeight.Normal,
                                                color = if (idx == 5) TextWhite else TextMuted,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(6.dp))

                budgetItems.take(4).forEach { item ->
                    val primaryCat = item.budget.targetCategories.firstOrNull() ?: item.budget.category
                    val catColor = CategoryIconHelper.getCategoryColor(primaryCat, TransactionType.EXPENSE)
                    val pct = item.rawPercentInt
                    val over = item.spent > item.budget.allocated && item.budget.allocated > 0
                    val barColor = when {
                        over -> ExpenseRed
                        pct >= warningPercent -> TrophyYellowLight
                        else -> IncomeGreen
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectBudget(item) }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = catColor.copy(alpha = 0.16f),
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = CategoryIconHelper.getCategoryIcon(primaryCat),
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.budget.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (isBalanceHidden) "•••• / ••••"
                                        else "${CurrencyHelper.formatCompactRupiah(item.spent)} / ${CurrencyHelper.formatCompactRupiah(item.budget.allocated)}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LinearProgressIndicator(
                                        progress = { item.percentage },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = barColor,
                                        trackColor = DarkSurfaceElevated
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (over) ExpenseRedSoft else IncomeGreenSoft
                                    ) {
                                        Text(
                                            text = "$pct%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (over) ExpenseRed else IncomeGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
}

@Composable
private fun DashboardSpendingHeatmapCard(
    monthName: String,
    selectedMonth: Int,
    selectedYear: Int,
    todayDay: Int,
    todayMonth: Int,
    todayYear: Int,
    transactions: List<Transaction>,
    isBalanceHidden: Boolean,
    onInfoClick: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    var selectedTileDay by remember(selectedMonth, selectedYear) { mutableIntStateOf(-1) }

    val monthExpenses = remember(transactions, selectedMonth, selectedYear) {
        transactions.filter {
            it.type == TransactionType.EXPENSE && it.monthIndex == selectedMonth && it.year == selectedYear
        }
    }
    val dailyTotals = remember(monthExpenses) {
        monthExpenses.groupBy { it.dayOfMonth }.mapValues { e -> e.value.sumOf { it.amount } }
    }
    val maxDayAmt = remember(dailyTotals) { (dailyTotals.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0) }

    val calFirst = remember(selectedMonth, selectedYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val daysInMonth = remember(calFirst) { calFirst.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val leadingBlanks = remember(calFirst) {
        val dow = calFirst.get(Calendar.DAY_OF_WEEK) // Sun=1..Sat=7
        if (dow == Calendar.SUNDAY) 6 else dow - 2
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_spending_heatmap")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Aktivitas Pengeluaran", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text("$monthName $selectedYear", fontSize = 11.5.sp, color = TextMuted)
                }
                IconButton(onClick = onInfoClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "Info kalender pengeluaran", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val dayHeaders = listOf("S", "S", "R", "K", "J", "S", "M")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                dayHeaders.forEach { d ->
                    Text(d, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextSubtle, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val totalCells = leadingBlanks + daysInMonth
            val rows = (totalCells + 6) / 7
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (c in 0 until 7) {
                        val cellIdx = r * 7 + c
                        val dayNum = cellIdx - leadingBlanks + 1
                        if (dayNum in 1..daysInMonth) {
                            val amt = dailyTotals[dayNum] ?: 0.0
                            val ratio = if (amt > 0.0) (amt / maxDayAmt).toFloat().coerceIn(0.15f, 1f) else 0f
                            val isToday = dayNum == todayDay && selectedMonth == todayMonth && selectedYear == todayYear
                            val isSelected = selectedTileDay == dayNum
                            val tileBg = when {
                                amt <= 0.0 -> DarkSurfaceElevated
                                ratio < 0.45f -> ExpenseHeatTile
                                ratio < 0.75f -> ExpenseHeatTileActive
                                else -> ExpenseRed.copy(alpha = 0.75f)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(tileBg)
                                    .then(
                                        when {
                                            isToday -> Modifier.border(1.5.dp, AccentBlueBright, RoundedCornerShape(8.dp))
                                            isSelected -> Modifier.border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                                            else -> Modifier
                                        }
                                    )
                                    .clickable {
                                        selectedTileDay = if (selectedTileDay == dayNum) -1 else dayNum
                                    }
                                    .padding(horizontal = 4.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$dayNum",
                                    fontSize = 10.sp,
                                    fontWeight = if (isToday || amt > 0.0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (ratio >= 0.75f) Color.White else if (isToday) AccentBlueBright else TextWhite,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                                if (amt > 0.0) {
                                    Text(
                                        text = if (isBalanceHidden) "••" else "-${CurrencyHelper.formatCompact(amt)}",
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (ratio >= 0.75f) Color.White else ExpenseRed,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                        modifier = Modifier.align(Alignment.BottomStart)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (selectedTileDay in 1..daysInMonth) {
                val dayTxs = monthExpenses.filter { it.dayOfMonth == selectedTileDay }
                val daySum = dayTxs.sumOf { it.amount }
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$selectedTileDay $monthName $selectedYear", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(
                                text = if (dayTxs.isEmpty()) "Tidak ada pengeluaran"
                                else "${dayTxs.size} transaksi · Total ${if (isBalanceHidden) "••••" else CurrencyHelper.formatRupiah(daySum)}",
                                fontSize = 11.sp,
                                color = if (dayTxs.isEmpty()) TextMuted else ExpenseRed
                            )
                        }
                        Text(
                            text = "Lihat Transaksi ›",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlueBright,
                            modifier = Modifier
                                .clickable { onNavigateToTransactions() }
                                .padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sedikit", fontSize = 10.sp, color = TextSubtle)
                Spacer(modifier = Modifier.width(6.dp))
                listOf(
                    DarkSurfaceElevated,
                    ExpenseHeatTile,
                    ExpenseHeatTileActive,
                    ExpenseRed.copy(alpha = 0.75f)
                ).forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(c)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text("Banyak", fontSize = 10.sp, color = TextSubtle)
            }
        }
    }
}

@Composable
fun DashboardNotificationsDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val upcomingItems by viewModel.upcomingItems.collectAsState()
    val totalBalance by viewModel.totalWalletBalance.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val selectedMonth by viewModel.selectedMonthIndex.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()

    val overWarningBudgets = remember(budgets, allTransactions, wallets, categories, selectedMonth, selectedYear, settings.budgetWarningPercent) {
        budgets.map { b ->
            BudgetMathHelper.computeBudgetItem(b, allTransactions, wallets, categories, selectedMonth, selectedYear)
        }.filter { it.rawPercentInt >= settings.budgetWarningPercent }
    }
    val currentDay = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
    val dueRecurring = remember(recurringTransactions, currentDay) {
        recurringTransactions.filter { it.isActive && it.reminderEnabled && it.dayOfMonth in currentDay..(currentDay + 3) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pengingat & Notifikasi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TrophyYellowLight,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Status Keuangan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TrophyYellowLight
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total saldo likuid saat ini: ${CurrencyHelper.formatRupiah(totalBalance)}. (${wallets.size} dompet aktif)",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                if (overWarningBudgets.isNotEmpty()) {
                    Text(
                        text = "Peringatan Anggaran (${overWarningBudgets.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ExpenseRed
                    )
                    overWarningBudgets.take(3).forEach { b ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ExpenseRedSoft,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(b.budget.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                Text("${b.rawPercentInt}% terpakai", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }
                    }
                }

                if (dueRecurring.isNotEmpty()) {
                    Text(
                        text = "Tagihan Berulang Segera Jatuh Tempo (${dueRecurring.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TrophyYellowLight
                    )
                    dueRecurring.take(3).forEach { rec ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(rec.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    Text("Tgl ${rec.dayOfMonth} · ${rec.accountName}", fontSize = 10.sp, color = TextMuted)
                                }
                                Text(CurrencyHelper.formatRupiah(rec.amount), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TrophyYellowLight)
                            }
                        }
                    }
                }

                Text(
                    text = "Kewajiban Mendatang (${upcomingItems.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )

                if (upcomingItems.isEmpty()) {
                    Text(
                        text = "Tidak ada tagihan atau pembayaran yang tertunda saat ini. Semua lancar!",
                        fontSize = 11.sp,
                        color = TextSubtle
                    )
                } else {
                    upcomingItems.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = "Jatuh tempo: ${item.dateLabel} (${item.dayLabel})",
                                        fontSize = 10.sp,
                                        color = ExpenseRed
                                    )
                                }
                                Text(
                                    text = CurrencyHelper.formatRupiah(item.amount),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Tutup", color = Color.White)
            }
        }
    )
}
