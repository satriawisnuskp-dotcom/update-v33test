package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccountWallet
import com.example.data.model.AssetItem
import com.example.data.model.AssetRecord
import com.example.data.model.BudgetCategory
import com.example.data.model.DebtItem
import com.example.data.model.DebtRecord
import com.example.data.model.DebtType
import com.example.data.model.EventItem
import com.example.data.model.FinancialPlan
import com.example.data.model.MainCategory
import com.example.data.model.NewPlanStartMode
import com.example.data.model.PlanSectionType
import com.example.data.model.PlannedItem
import com.example.data.model.RecurringTransaction
import com.example.data.model.SavingsGoal
import com.example.data.model.SubcategoryItem
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.UpcomingScheduleItem
import com.example.data.model.UserSettings
import com.example.data.repository.FinanceRepository
import com.example.data.sync.SyncStatus
import com.example.ui.components.FinancialDataPoint
import com.example.util.ChartDataHelper
import com.example.util.TradingViewHelper
import com.example.util.TradingViewQuote
import com.example.util.TransactionMathHelper
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MonthOption(
    val monthIndex: Int,
    val label: String,
    val year: Int
) {
    val first: Int get() = monthIndex
    val second: String get() = label
}

class FinanceViewModel(
    private val repository: FinanceRepository = FinanceRepository()
) : ViewModel() {

    companion object {
        fun generatePastYearMonthOptions(): List<MonthOption> {
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
            val list = mutableListOf<MonthOption>()
            // Start from 11 months ago up to the current month (total 12 months, ending on current month)
            val cal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -11)
            }
            for (i in 0..11) {
                val m = cal.get(Calendar.MONTH) + 1
                val y = cal.get(Calendar.YEAR)
                val label = "${monthNames[m - 1]} $y"
                list.add(MonthOption(monthIndex = m, label = label, year = y))
                cal.add(Calendar.MONTH, 1)
            }
            return list
        }
    }

    private val _isRefreshingStockPrices = MutableStateFlow(false)
    val isRefreshingStockPrices: StateFlow<Boolean> = _isRefreshingStockPrices.asStateFlow()

    private val _lastStockPriceRefreshTime = MutableStateFlow(System.currentTimeMillis())
    val lastStockPriceRefreshTime: StateFlow<Long> = _lastStockPriceRefreshTime.asStateFlow()

    private val _liveStockQuotes = MutableStateFlow<Map<String, TradingViewQuote>>(TradingViewHelper.getAllCachedQuotes())
    val liveStockQuotes: StateFlow<Map<String, TradingViewQuote>> = _liveStockQuotes.asStateFlow()

    init {
        // Automatically check and process any due recurring transactions upon initialization
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)
        repository.checkAndExecuteDueRecurringTransactions(currentDay, currentMonth, currentYear)

        // Initial real-time stock price sync from TradingView
        refreshStockPrices()

        // Background live stock price ticker polling (every 60s instead of 10s to prevent constant CPU/I/O churn and frame drops)
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(60_000)
                try {
                    if (repository.assets.value.any { it.isStock || it.tickerSymbol.isNotBlank() }) {
                        val quotes = repository.refreshStockPricesFromTradingView()
                        if (quotes.isNotEmpty()) {
                            _liveStockQuotes.value = quotes
                            _lastStockPriceRefreshTime.value = System.currentTimeMillis()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun initStorage(context: android.content.Context) {
        repository.initContext(context)
        _isBalanceHidden.value = repository.settings.value.defaultHideBalance
    }

    // Filter states dynamically ending at current month and year
    private val _selectedMonthIndex = MutableStateFlow(
        Calendar.getInstance().get(Calendar.MONTH) + 1
    )
    val selectedMonthIndex: StateFlow<Int> = _selectedMonthIndex.asStateFlow()

    private val _selectedYear = MutableStateFlow(
        Calendar.getInstance().get(Calendar.YEAR)
    )
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    val selectedWalletFilter: StateFlow<String> = repository.selectedWalletFilter

    private val _summaryWalletFilter = MutableStateFlow("Semua")
    val summaryWalletFilter: StateFlow<String> = _summaryWalletFilter.asStateFlow()

    private val _isBalanceHidden = MutableStateFlow(repository.settings.value.defaultHideBalance)
    val isBalanceHidden: StateFlow<Boolean> = _isBalanceHidden.asStateFlow()

    private val _summaryTab = MutableStateFlow(0) // 0: Statistik (Tampilan Utama), 1: Laporan, 2: Kalender
    val summaryTab: StateFlow<Int> = _summaryTab.asStateFlow()

    private val _summaryPeriod = MutableStateFlow(0) // 0: Bulanan, 1: Tahunan, 2: Kustom
    val summaryPeriod: StateFlow<Int> = _summaryPeriod.asStateFlow()

    private val _accountSubTab = MutableStateFlow(1) // 0: Nilai Bersih, 1: Akun, 2: Aset, 3: Utang (default 1: Akun)
    val accountSubTab: StateFlow<Int> = _accountSubTab.asStateFlow()

    private val _selectedWalletIdForDetail = MutableStateFlow<String?>(null)
    val selectedWalletIdForDetail: StateFlow<String?> = _selectedWalletIdForDetail.asStateFlow()

    private val _selectedAssetIdForDetail = MutableStateFlow<String?>(null)
    val selectedAssetIdForDetail: StateFlow<String?> = _selectedAssetIdForDetail.asStateFlow()

    private val _selectedDebtIdForDetail = MutableStateFlow<String?>(null)
    val selectedDebtIdForDetail: StateFlow<String?> = _selectedDebtIdForDetail.asStateFlow()

    fun selectDebtForDetail(id: String?) {
        _selectedDebtIdForDetail.value = id
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    val wallets: StateFlow<List<AccountWallet>> = repository.wallets
    val assets: StateFlow<List<AssetItem>> = repository.assets
    val debts: StateFlow<List<DebtItem>> = repository.debts
    val upcomingItems: StateFlow<List<UpcomingScheduleItem>> = repository.upcomingItems
    val budgets: StateFlow<List<BudgetCategory>> = repository.budgets
    val settings: StateFlow<UserSettings> = repository.settings
    val labels: StateFlow<List<String>> = repository.labels
    val categories: StateFlow<List<MainCategory>> = repository.categories
    val recurringTransactions: StateFlow<List<RecurringTransaction>> = repository.recurringTransactions
    val planTitle: StateFlow<String> = repository.planTitle
    val plannedItems: StateFlow<List<PlannedItem>> = repository.plannedItems
    val financialPlans: StateFlow<List<FinancialPlan>> = repository.financialPlans
    val activePlanId: StateFlow<String> = repository.activePlanId
    val savingsGoals: StateFlow<List<SavingsGoal>> = repository.savingsGoals
    val events: StateFlow<List<EventItem>> = repository.events
    val timelineScenarios: StateFlow<List<com.example.data.model.TimelineScenarioItem>> = repository.timelineScenarios

    // Cloud Auth & Real-Time Sync State
    val currentUser: StateFlow<FirebaseUser?> = repository.currentUser
    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus
    val lastSyncedAt: StateFlow<Long> = repository.lastSyncedAt

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _syncSuccessMessage = MutableStateFlow<String?>(null)
    val syncSuccessMessage: StateFlow<String?> = _syncSuccessMessage.asStateFlow()

    val plannedIncomeTotal: StateFlow<Double> = repository.plannedItems.map { list ->
        list.filter { it.section == PlanSectionType.INCOME }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val plannedExpenseTotal: StateFlow<Double> = repository.plannedItems.map { list ->
        list.filter { it.section == PlanSectionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val plannedSavingsTotal: StateFlow<Double> = repository.plannedItems.map { list ->
        list.filter { it.section == PlanSectionType.SAVINGS }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val unallocatedAmount: StateFlow<Double> = combine(
        plannedIncomeTotal,
        plannedExpenseTotal,
        plannedSavingsTotal
    ) { income, expense, savings ->
        income - (expense + savings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Filtered transactions
    val allTransactions: StateFlow<List<Transaction>> = repository.transactions
    val transactions: StateFlow<List<Transaction>> = combine(
        repository.transactions,
        _selectedMonthIndex,
        _selectedYear,
        repository.selectedWalletFilter,
        _searchQuery
    ) { txs, month, year, wallet, query ->
        txs.filter { tx ->
            val matchMonth = tx.monthIndex == month
            val matchYear = tx.year == year
            val matchWallet = wallet == "Semua" ||
                    tx.accountName.equals(wallet, ignoreCase = true) ||
                    (tx.type == TransactionType.TRANSFER && tx.toAccountName.equals(wallet, ignoreCase = true))
            val matchQuery = query.isBlank() ||
                    tx.title.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.accountName.contains(query, ignoreCase = true) ||
                    tx.toAccountName.contains(query, ignoreCase = true) ||
                    tx.targetAssetName.contains(query, ignoreCase = true) ||
                    tx.targetDebtName.contains(query, ignoreCase = true) ||
                    tx.note.contains(query, ignoreCase = true)
            matchMonth && matchYear && matchWallet && matchQuery
        }.sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recent transactions for Dashboard (real-time, sorted chronologically descending)
    val recentTransactions: StateFlow<List<Transaction>> = repository.transactions.map { list ->
        list.sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator).take(6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total Wallet Balance
    val totalWalletBalance: StateFlow<Double> = repository.wallets.combine(repository.selectedWalletFilter) { list, filter ->
        if (filter == "Semua") {
            list.sumOf { it.balance }
        } else {
            list.find { it.name.equals(filter, ignoreCase = true) }?.balance ?: 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Combined all-wallet balance
    val totalAllWalletsBalance: StateFlow<Double> = repository.wallets.map { list ->
        list.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Monthly Income
    val monthlyIncome: StateFlow<Double> = transactions.map { list ->
        list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Monthly Expense
    val monthlyExpense: StateFlow<Double> = transactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Monthly Transfer (Transfer Antar Akun & Transfer Aset)
    val monthlyTransfer: StateFlow<Double> = transactions.map { list ->
        list.filter { it.type == TransactionType.TRANSFER }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total Assets (calculates real market value for stocks based on real-time prices)
    val totalAssetsValue: StateFlow<Double> = repository.assets.map { list ->
        list.sumOf { it.realValue }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Stock Assets & Real-Time Portfolio Valuations
    val stockAssets: StateFlow<List<AssetItem>> = repository.assets.map { list ->
        list.filter { it.isStock || it.tickerSymbol.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalStockPortfolioRealValue: StateFlow<Double> = stockAssets.map { list ->
        list.sumOf { it.realValue }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalStockCostBasis: StateFlow<Double> = stockAssets.map { list ->
        list.sumOf { it.costBasis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalStockUnrealizedGainLoss: StateFlow<Double> = combine(
        totalStockPortfolioRealValue,
        totalStockCostBasis
    ) { realVal, cost ->
        realVal - cost
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalStockPerformancePercent: StateFlow<Double> = combine(
        totalStockPortfolioRealValue,
        totalStockCostBasis
    ) { realVal, cost ->
        if (cost > 0.0) ((realVal - cost) / cost) * 100.0 else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total Debts (remaining)
    val totalDebtsValue: StateFlow<Double> = repository.debts.map { list ->
        list.filter { it.type == DebtType.BORROWED }.sumOf { it.totalAmount - it.paidAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalLentValue: StateFlow<Double> = repository.debts.map { list ->
        list.filter { it.type == DebtType.LENT }.sumOf { it.totalAmount - it.paidAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Net Worth = Total Balance + Total Assets - Total Debts
    val netWorthValue: StateFlow<Double> = combine(
        totalAllWalletsBalance,
        totalAssetsValue,
        totalDebtsValue
    ) { balance, assets, debts ->
        balance + assets - debts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Reactive line chart datasets generated from real-time transaction history & balances
    val dashboardChartPoints: StateFlow<Map<String, List<FinancialDataPoint>>> = combine(
        repository.transactions,
        totalAllWalletsBalance
    ) { txs, balance ->
        ChartDataHelper.generateAllTimeframePoints(
            transactions = txs,
            currentBalance = balance,
            walletFilter = "Semua"
        )
    }.flowOn(Dispatchers.Default).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyMap()
    )

    val netWorthChartPoints: StateFlow<Map<String, List<FinancialDataPoint>>> = combine(
        repository.wallets,
        repository.assets,
        repository.debts,
        repository.transactions
    ) { w, a, d, txs ->
        ChartDataHelper.generateNetWorthTimeframePoints(
            wallets = w,
            assets = a,
            debts = d,
            transactions = txs
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val accountChartPoints: StateFlow<Map<String, List<FinancialDataPoint>>> = combine(
        repository.transactions,
        totalAllWalletsBalance,
        repository.selectedWalletFilter
    ) { txs, balance, walletFilter ->
        ChartDataHelper.generateAllTimeframePoints(
            transactions = txs,
            currentBalance = balance,
            walletFilter = walletFilter
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val assetsChartPoints: StateFlow<Map<String, List<FinancialDataPoint>>> = repository.assets.map { assetList ->
        ChartDataHelper.generateAssetsTimeframePoints(assetList, assetList.sumOf { it.realValue })
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val debtsChartPoints: StateFlow<Map<String, List<FinancialDataPoint>>> = repository.debts.map { debtList ->
        val totalBorrowed = debtList.filter { it.type == DebtType.BORROWED }.sumOf { it.totalAmount - it.paidAmount }
        ChartDataHelper.generateDebtsTimeframePoints(debtList, totalBorrowed)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun generateDebtChartPoints(debt: DebtItem): Map<String, List<FinancialDataPoint>> {
        return ChartDataHelper.generateIndividualDebtTimeframePoints(debt)
    }

    fun generateAssetChartPoints(asset: AssetItem): Map<String, List<FinancialDataPoint>> {
        return ChartDataHelper.generateIndividualAssetTimeframePoints(asset)
    }

    // Calendar daily expenses mapping (day -> total expense)
    val calendarDailyExpenses: StateFlow<Map<Int, Double>> = combine(
        repository.transactions,
        _selectedMonthIndex,
        _selectedYear
    ) { txs, month, year ->
        val map = mutableMapOf<Int, Double>()
        txs.filter { it.monthIndex == month && it.year == year && it.type == TransactionType.EXPENSE }.forEach { tx ->
            map[tx.dayOfMonth] = (map[tx.dayOfMonth] ?: 0.0) + tx.amount
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Month options dynamically reflecting 1 year back ending on the current month
    private val _monthOptions = MutableStateFlow(generatePastYearMonthOptions())
    val monthOptions: StateFlow<List<MonthOption>> = _monthOptions.asStateFlow()

    fun selectMonth(month: Int) {
        val match = _monthOptions.value.lastOrNull { it.monthIndex == month }
        if (match != null) {
            _selectedMonthIndex.value = match.monthIndex
            _selectedYear.value = match.year
        } else {
            _selectedMonthIndex.value = month
        }
    }

    fun selectYear(year: Int) {
        _selectedYear.value = year
    }

    fun selectMonthAndYear(month: Int, year: Int) {
        _selectedMonthIndex.value = month
        _selectedYear.value = year
    }

    fun selectWalletFilter(walletName: String) {
        repository.selectWalletFilter(walletName)
    }

    fun selectSummaryWalletFilter(walletName: String) {
        _summaryWalletFilter.value = walletName
    }

    fun resetSummaryWalletFilter() {
        _summaryWalletFilter.value = "Semua"
    }

    fun toggleBalanceVisibility() {
        _isBalanceHidden.value = !_isBalanceHidden.value
    }

    fun setSummaryTab(tab: Int) {
        _summaryTab.value = tab
    }

    fun setSummaryPeriod(period: Int) {
        _summaryPeriod.value = period
    }

    fun setAccountSubTab(tab: Int) {
        _accountSubTab.value = tab
    }

    fun selectWalletForDetail(walletId: String?) {
        _selectedWalletIdForDetail.value = walletId
    }

    fun selectAssetForDetail(assetId: String?) {
        _selectedAssetIdForDetail.value = assetId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        _isSearchOpen.value = !_isSearchOpen.value
        if (!_isSearchOpen.value) {
            _searchQuery.value = ""
        }
    }

    fun openSearch(query: String = "") {
        _isSearchOpen.value = true
        _searchQuery.value = query
    }

    fun closeSearch() {
        _isSearchOpen.value = false
        _searchQuery.value = ""
    }

    fun addTransaction(tx: Transaction) {
        repository.addTransaction(tx)
    }

    fun updateTransaction(tx: Transaction) {
        repository.updateTransaction(tx)
    }

    fun deleteTransaction(txId: String) {
        repository.deleteTransaction(txId)
    }

    fun batchDeleteTransactions(txIds: Set<String>) {
        repository.batchDeleteTransactions(txIds)
    }

    fun batchUpdateCategory(txIds: Set<String>, newCategory: String, newSubcategory: String = "") {
        repository.batchUpdateCategory(txIds, newCategory, newSubcategory)
    }

    fun batchUpdateLabel(txIds: Set<String>, newLabel: String) {
        repository.batchUpdateLabel(txIds, newLabel)
    }

    fun addWallet(wallet: AccountWallet) {
        repository.addWallet(wallet)
    }

    fun addWallet(name: String, bankType: String, initialBalance: Double) {
        repository.addWallet(
            AccountWallet(
                name = name,
                bankType = bankType,
                balance = initialBalance
            )
        )
    }

    fun updateWallet(wallet: AccountWallet) {
        repository.updateWallet(wallet)
    }

    fun deleteWallet(walletId: String) {
        repository.deleteWallet(walletId)
    }

    fun reorderWallets(newWallets: List<AccountWallet>) {
        repository.reorderWallets(newWallets)
    }

    fun reorderAssets(newAssets: List<AssetItem>) {
        repository.reorderAssets(newAssets)
    }

    fun reorderDebts(newDebts: List<DebtItem>) {
        repository.reorderDebts(newDebts)
    }

    fun addAsset(asset: AssetItem) {
        repository.addAsset(asset)
    }

    fun addAsset(name: String, group: String, value: Double, platform: String) {
        repository.addAsset(
            AssetItem(
                name = name,
                group = group,
                value = value,
                platform = platform
            )
        )
    }

    fun updateAsset(asset: AssetItem) {
        repository.updateAsset(asset)
    }

    fun updateStockCurrentPrice(assetId: String, newPrice: Double) {
        repository.updateStockCurrentPrice(assetId, newPrice)
    }

    fun refreshStockPrices() {
        viewModelScope.launch {
            _isRefreshingStockPrices.value = true
            try {
                val quotes = repository.refreshStockPricesFromTradingView()
                if (quotes.isNotEmpty()) {
                    _liveStockQuotes.value = quotes
                    _lastStockPriceRefreshTime.value = System.currentTimeMillis()
                }
            } catch (_: Exception) {
            } finally {
                _isRefreshingStockPrices.value = false
            }
        }
    }

    fun refreshSingleStock(symbol: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingStockPrices.value = true
            try {
                TradingViewHelper.fetchSingleQuote(symbol)
                val quotes = repository.refreshStockPricesFromTradingView()
                if (quotes.isNotEmpty()) {
                    _liveStockQuotes.value = quotes
                    _lastStockPriceRefreshTime.value = System.currentTimeMillis()
                }
            } catch (_: Exception) {
            } finally {
                _isRefreshingStockPrices.value = false
            }
        }
    }

    fun deleteAsset(assetId: String) {
        repository.deleteAsset(assetId)
    }

    fun addAssetRecord(assetId: String, record: AssetRecord, updateWalletBalance: Boolean = true) {
        repository.addAssetRecord(assetId, record, updateWalletBalance)
    }

    fun updateAssetRecord(assetId: String, oldRecord: AssetRecord, updatedRecord: AssetRecord, updateWalletBalance: Boolean = true) {
        repository.updateAssetRecord(assetId, oldRecord, updatedRecord, updateWalletBalance)
    }

    fun deleteAssetRecord(assetId: String, recordId: String, revertWalletBalance: Boolean = true) {
        repository.deleteAssetRecord(assetId, recordId, revertWalletBalance)
    }

    fun addDebt(name: String, totalAmount: Double, dueDate: String, type: DebtType = DebtType.BORROWED, paidAmount: Double = 0.0) {
        repository.addDebt(
            DebtItem(
                name = name,
                type = type,
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                dueDate = dueDate
            )
        )
    }

    fun addDebt(debt: DebtItem, depositToWalletName: String? = null) {
        repository.addDebt(debt, depositToWalletName)
    }

    fun updateDebt(debt: DebtItem) {
        repository.updateDebt(debt)
    }

    fun markDebtFullyPaid(debtId: String) {
        repository.markDebtFullyPaid(debtId)
    }

    fun deleteDebt(debtId: String, deleteLinkedTransactions: Boolean = false) {
        repository.deleteDebt(debtId, deleteLinkedTransactions)
    }

    fun payDebt(debtId: String, amount: Double, walletName: String? = null, note: String = "Pembayaran cicilan") {
        repository.payDebt(debtId, amount, walletName, note)
    }

    fun addDebtRecord(debtId: String, record: DebtRecord, affectWalletName: String? = null) {
        repository.addDebtRecord(debtId, record, affectWalletName)
    }

    fun updateDebtRecord(debtId: String, record: DebtRecord) {
        repository.updateDebtRecord(debtId, record)
    }

    fun deleteDebtRecord(debtId: String, recordId: String) {
        repository.deleteDebtRecord(debtId, recordId)
    }

    fun addBudget(category: String, allocated: Double) {
        repository.addBudget(
            BudgetCategory(
                category = category,
                name = category,
                categories = listOf(category),
                allocated = allocated,
                spent = 0.0
            )
        )
    }

    fun addBudget(budget: BudgetCategory) {
        repository.addBudget(budget)
    }

    fun updateBudget(budget: BudgetCategory) {
        repository.updateBudget(budget)
    }

    fun deleteBudget(budgetId: String) {
        repository.deleteBudget(budgetId)
    }

    fun updateBudgetDisplayStyle(summaryStyle: String, listStyle: String) {
        repository.updateBudgetDisplayStyle(summaryStyle, listStyle)
    }

    fun updateSettings(newSettings: UserSettings) {
        val prevDefaultHide = repository.settings.value.defaultHideBalance
        repository.updateSettings(newSettings)
        if (newSettings.defaultHideBalance != prevDefaultHide) {
            _isBalanceHidden.value = newSettings.defaultHideBalance
        }
    }

    fun updateStatistikConfig(order: List<String>, hidden: List<String>, sideBySide: Boolean) {
        repository.updateStatistikConfig(order, hidden, sideBySide)
    }

    fun updateDashboardWidgets(order: List<String>, hidden: List<String>) {
        repository.updateDashboardWidgets(order, hidden)
    }

    fun addLabel(name: String) {
        repository.addLabel(name)
    }

    fun deleteLabel(name: String) {
        repository.deleteLabel(name)
    }

    fun addRecurringTransaction(recurring: RecurringTransaction) {
        val cal = java.util.Calendar.getInstance()
        val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val currentMonth = cal.get(java.util.Calendar.MONTH) + 1
        val currentYear = cal.get(java.util.Calendar.YEAR)
        repository.addRecurringTransaction(
            recurring = recurring,
            checkImmediateProcess = true,
            currentDay = currentDay,
            currentMonth = currentMonth,
            currentYear = currentYear
        )
    }

    fun updateRecurringTransaction(recurring: RecurringTransaction) {
        repository.updateRecurringTransaction(recurring)
    }

    fun deleteRecurringTransaction(id: String) {
        repository.deleteRecurringTransaction(id)
    }

    fun deleteRecurringWithTransactions(id: String, deleteHistory: Boolean) {
        repository.deleteRecurringWithTransactions(id, deleteHistory)
    }

    fun toggleRecurringActive(id: String) {
        repository.toggleRecurringActive(id)
    }

    fun processRecurringTransaction(id: String) {
        val cal = java.util.Calendar.getInstance()
        val currentMonth = cal.get(java.util.Calendar.MONTH) + 1
        val currentYear = cal.get(java.util.Calendar.YEAR)
        repository.processRecurringTransaction(id, currentMonth, currentYear)
    }

    fun skipRecurringOccurrence(id: String, skippedDateLabel: String = "") {
        val cal = java.util.Calendar.getInstance()
        val currentMonth = cal.get(java.util.Calendar.MONTH) + 1
        val currentYear = cal.get(java.util.Calendar.YEAR)
        repository.skipRecurringOccurrence(id, currentMonth, currentYear, skippedDateLabel)
    }

    fun undoSkipRecurringOccurrence(id: String) {
        repository.undoSkipRecurringOccurrence(id)
    }

    fun snoozeRecurringOccurrence(id: String, snoozedUntilDate: String) {
        repository.snoozeRecurringOccurrence(id, snoozedUntilDate)
    }

    fun updateRecurringReminders(id: String, enabled: Boolean, options: List<String>) {
        repository.updateRecurringReminders(id, enabled, options)
    }

    fun markRecurringProcessedNow(id: String) {
        val cal = java.util.Calendar.getInstance()
        repository.markRecurringProcessed(id, cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.YEAR))
    }

    fun checkAndExecuteDueRecurringTransactions(): Int {
        val cal = java.util.Calendar.getInstance()
        val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val currentMonth = cal.get(java.util.Calendar.MONTH) + 1
        val currentYear = cal.get(java.util.Calendar.YEAR)
        return repository.checkAndExecuteDueRecurringTransactions(currentDay, currentMonth, currentYear)
    }

    fun updatePlanTitle(newTitle: String) {
        repository.updatePlanTitle(newTitle)
    }

    fun addPlannedItem(item: PlannedItem) {
        repository.addPlannedItem(item)
    }

    fun updatePlannedItem(item: PlannedItem) {
        repository.updatePlannedItem(item)
    }

    fun deletePlannedItem(id: String) {
        repository.deletePlannedItem(id)
    }

    fun selectActivePlan(planId: String) {
        repository.selectActivePlan(planId)
    }

    fun createFinancialPlan(
        monthIndex: Int,
        year: Int,
        mode: NewPlanStartMode,
        customTitle: String? = null
    ): FinancialPlan {
        return repository.createFinancialPlan(monthIndex, year, mode, customTitle)
    }

    fun deleteFinancialPlan(planId: String) {
        repository.deleteFinancialPlan(planId)
    }

    fun exportTransactionsJson(context: android.content.Context? = null): String {
        return repository.exportTransactionsJson(context)
    }

    fun importTransactionsFromJson(
        jsonStr: String,
        replaceExisting: Boolean,
        context: android.content.Context? = null
    ): Result<com.example.data.model.BackupRestoreResult> {
        return repository.importTransactionsFromJson(jsonStr, replaceExisting, context)
    }

    fun addCategory(category: MainCategory) {
        repository.addCategory(category)
    }

    fun updateCategory(category: MainCategory) {
        repository.updateCategory(category)
    }

    fun reorderCategories(newCategories: List<MainCategory>) {
        repository.reorderCategories(newCategories)
    }

    fun reorderSubcategories(categoryId: String, newSubcategories: List<SubcategoryItem>) {
        repository.reorderSubcategories(categoryId, newSubcategories)
    }

    fun deleteCategory(categoryId: String) {
        repository.deleteCategory(categoryId)
    }

    fun addSubcategory(categoryId: String, subcategory: SubcategoryItem) {
        repository.addSubcategory(categoryId, subcategory)
    }

    fun updateSubcategory(categoryId: String, subcategory: SubcategoryItem) {
        repository.updateSubcategory(categoryId, subcategory)
    }

    fun deleteSubcategory(categoryId: String, subcategoryId: String) {
        repository.deleteSubcategory(categoryId, subcategoryId)
    }

    fun toggleCategoryVisibility(categoryId: String) {
        repository.toggleCategoryVisibility(categoryId)
    }

    fun toggleSubcategoryVisibility(categoryId: String, subcategoryId: String) {
        repository.toggleSubcategoryVisibility(categoryId, subcategoryId)
    }

    fun resetAllData(createDefaultWallet: Boolean = false) {
        repository.resetAllData(createDefaultWallet)
    }

    fun completeOnboarding(
        currencyCode: String,
        accountName: String,
        accountType: String,
        accountIconName: String,
        initialBalance: Double,
        paydayDay: Int,
        monthlyIncome: Double,
        bills: List<com.example.data.model.OnboardingBillInput>,
        budgets: List<com.example.data.model.OnboardingBudgetInput>,
        createMonthlyPlan: Boolean
    ) {
        repository.completeOnboarding(
            currencyCode = currencyCode,
            accountName = accountName,
            accountType = accountType,
            accountIconName = accountIconName,
            initialBalance = initialBalance,
            paydayDay = paydayDay,
            monthlyIncome = monthlyIncome,
            bills = bills,
            budgets = budgets,
            createMonthlyPlan = createMonthlyPlan
        )
    }

    fun addSavingsGoal(goal: SavingsGoal) = repository.addSavingsGoal(goal)
    fun updateSavingsGoal(goal: SavingsGoal) = repository.updateSavingsGoal(goal)
    fun deleteSavingsGoal(goalId: String) = repository.deleteSavingsGoal(goalId)
    fun toggleSavingsGoalCompleted(goalId: String) = repository.toggleSavingsGoalCompleted(goalId)
    fun updatePaydayAutoSave(enabled: Boolean, percent: Int = 10) = repository.updatePaydayAutoSave(enabled, percent)
    fun addSavingsRecord(
        goalId: String,
        amount: Double,
        isWithdraw: Boolean,
        note: String,
        dateStr: String,
        walletName: String
    ): Boolean = repository.addSavingsRecord(goalId, amount, isWithdraw, note, dateStr, walletName)
    fun updateSavingsRecord(
        goalId: String,
        recordId: String,
        amount: Double,
        isWithdraw: Boolean,
        note: String,
        dateStr: String
    ): Boolean = repository.updateSavingsRecord(goalId, recordId, amount, isWithdraw, note, dateStr)
    fun deleteSavingsRecord(goalId: String, recordId: String) = repository.deleteSavingsRecord(goalId, recordId)
    fun depositToSavingsGoal(goalId: String, amount: Double, sourceWalletName: String): Boolean =
        repository.depositToSavingsGoal(goalId, amount, sourceWalletName)
    fun withdrawFromSavingsGoal(goalId: String, amount: Double, destinationWalletName: String): Boolean =
        repository.withdrawFromSavingsGoal(goalId, amount, destinationWalletName)

    fun addEvent(event: EventItem) = repository.addEvent(event)
    fun updateEvent(event: EventItem) = repository.updateEvent(event)
    fun deleteEvent(eventId: String) = repository.deleteEvent(eventId)
    fun toggleEventCompleted(eventId: String) = repository.toggleEventCompleted(eventId)
    fun toggleEventPlannedExpensePaid(eventId: String, expenseId: String) = repository.toggleEventPlannedExpensePaid(eventId, expenseId)
    fun addEventPlannedExpense(eventId: String, expense: com.example.data.model.EventPlannedExpense) = repository.addEventPlannedExpense(eventId, expense)
    fun updateEventPlannedExpense(eventId: String, expense: com.example.data.model.EventPlannedExpense) = repository.updateEventPlannedExpense(eventId, expense)
    fun deleteEventPlannedExpense(eventId: String, expenseId: String) = repository.deleteEventPlannedExpense(eventId, expenseId)
    fun linkTransactionToEvent(txId: String, eventName: String, link: Boolean) = repository.linkTransactionToEvent(txId, eventName, link)

    fun addTimelineScenario(item: com.example.data.model.TimelineScenarioItem) = repository.addTimelineScenario(item)
    fun deleteTimelineScenario(itemId: String) = repository.deleteTimelineScenario(itemId)
    fun updateMainMenuOrder(order: List<String>, hidden: List<String>) = repository.updateMainMenuOrder(order, hidden)

    fun clearAuthMessages() {
        _authErrorMessage.value = null
        _syncSuccessMessage.value = null
    }

    fun trySilentSignIn(activityContext: Context) {
        viewModelScope.launch {
            repository.trySilentSignIn(activityContext)
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        if (_isAuthLoading.value) return
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            _syncSuccessMessage.value = null
            val result = repository.signInWithGoogle(activityContext)
            result.fold(
                onSuccess = { user ->
                    val label = user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "Akun Google"
                    _syncSuccessMessage.value = "Berhasil masuk sebagai $label. Data lokal & cloud disinkronkan."
                },
                onFailure = { error ->
                    _authErrorMessage.value = error.message ?: "Gagal masuk dengan akun Google."
                }
            )
            _isAuthLoading.value = false
        }
    }

    fun signOut(context: Context) {
        if (_isAuthLoading.value) return
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            _syncSuccessMessage.value = null
            repository.signOut(context)
            _syncSuccessMessage.value = "Keluar dari akun Google. Aplikasi berjalan dalam mode lokal."
            _isAuthLoading.value = false
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            _authErrorMessage.value = null
            _syncSuccessMessage.value = null
            val result = repository.manualSync()
            result.fold(
                onSuccess = {
                    _syncSuccessMessage.value = "Sinkronisasi cloud selesai."
                },
                onFailure = { error ->
                    _authErrorMessage.value = error.message ?: "Sinkronisasi gagal. Periksa koneksi internet Anda."
                }
            )
        }
    }
}
