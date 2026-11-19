package com.example.data.model

import java.util.UUID

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class DebtType {
    BORROWED, // Dipinjam
    LENT      // Dipinjamkan
}

data class SubcategoryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconName: String = "",
    val isVisible: Boolean = true
)

data class MainCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val iconName: String = "",
    val isVisible: Boolean = true,
    val subcategories: List<SubcategoryItem> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val subcategory: String = "",
    val amount: Double,
    val type: TransactionType,
    val accountName: String,
    val dayOfMonth: Int,
    val monthIndex: Int, // 1 to 12 (10 = Oktober)
    val year: Int = 2026,
    val dayOfWeekName: String = "Sen",
    val note: String = "",
    val label: String = "",
    val recurrence: String = "Tidak pernah",
    val time: String = "18:51",
    val toAccountName: String = "",
    val transferFee: Double = 0.0,
    val targetAssetId: String = "",
    val targetAssetName: String = "",
    val targetDebtId: String = "",
    val targetDebtName: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class AccountWallet(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bankType: String = "Bank",
    val balance: Double,
    val lastCategory: String = "Other",
    val lastAmount: Double = 0.0,
    val lastActivityTime: String = "2 hari lalu",
    val currency: String = "IDR",
    val iconName: String = "Bank",
    val excludeFromNetWorth: Boolean = false,
    val excludeFromBudget: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class AssetRecord(
    val id: String = UUID.randomUUID().toString(),
    val assetId: String,
    val tag: String = "Buy", // "Buy", "Invest more", "Sold"
    val isIncrease: Boolean = true,
    val amount: Double,
    val sourceWalletName: String = "",
    val date: String = "7 Okt 2026",
    val time: String = "10.09",
    val note: String = "",
    val lotCount: Double = 0.0,
    val pricePerUnit: Double = 0.0
)

data class AssetItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val group: String = "Investasi", // "Tunai & Bank" or "Investasi"
    val assetType: String = "Bank Account", // "Bank Account", "Emas", "Saham", "Reksadana", "Kripto", "Properti", etc.
    val value: Double,
    val platform: String = "Bibit",
    val performancePercent: Double = 10.2,
    val currency: String = "IDR",
    val createdAtDate: String = "29 Okt 2026",
    val createdAtTime: String = "00.15",
    val valuationMethod: String = "Manual",
    val records: List<AssetRecord> = emptyList(),
    val iconUri: String = "",
    val tickerSymbol: String = "",
    val lotCount: Double = 0.0,
    val averagePrice: Double = 0.0,
    val currentPrice: Double = 0.0,
    val dailyPriceChange: Double = 0.0,
    val dailyPriceChangePercent: Double = 0.0,
    val lastPriceUpdate: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    val isStock: Boolean
        get() = assetType.equals("Saham", ignoreCase = true) || name.contains("Saham", ignoreCase = true)

    val sharesCount: Double
        get() = lotCount * 100.0

    // Modal Pembelian (Cost Basis): lot * 100 * averagePrice, atau fallback ke value
    val costBasis: Double
        get() = if (lotCount > 0.0 && averagePrice > 0.0) lotCount * 100.0 * averagePrice else value

    // Nilai Real / Nilai Pasar Aktual:
    // Jika lot > 0 dan currentPrice > 0 -> lot * 100 * currentPrice
    // Jika currentPrice == 0 tapi lot > 0 dan averagePrice > 0 -> lot * 100 * averagePrice
    // Fallback: value
    val realValue: Double
        get() {
            if (lotCount > 0.0 && currentPrice > 0.0) {
                return lotCount * 100.0 * currentPrice
            }
            if (lotCount > 0.0 && averagePrice > 0.0) {
                return lotCount * 100.0 * averagePrice
            }
            return value
        }

    // Capital Gain / Loss (Unrealized Profit/Loss)
    val unrealizedGainLoss: Double
        get() = realValue - costBasis

    // Persentase Untung / Rugi aktual
    val calculatedPerformancePercent: Double
        get() {
            if (isStock && costBasis > 0.0 && (currentPrice > 0.0 || (lotCount > 0.0 && averagePrice > 0.0))) {
                return ((realValue - costBasis) / costBasis) * 100.0
            }
            return performancePercent
        }

    val resolvedTicker: String
        get() {
            if (tickerSymbol.isNotBlank()) {
                val clean = tickerSymbol.trim().uppercase()
                return if (!clean.contains(":") && clean.length in 3..6) "IDX:$clean" else clean
            }
            val upper = name.uppercase()
            val indonesianStocks = listOf(
                "BBRI", "BBCA", "BMRI", "BBNI", "TLKM", "ASII", "GOTO", "AMMN",
                "ANTM", "BRIS", "ICBP", "INDF", "UNVR", "ADRO", "PGAS", "KLBF",
                "MDKA", "ACES", "CPIN", "SMGR", "INCO", "PTBA", "MEDC", "BUKA",
                "ARTO", "TPIA", "BREN", "CUAN", "GGRM", "HMSP", "MYOR", "BUMI",
                "UNTR", "BRPT", "INKP", "TKIM", "MAPI", "AKRA", "EXCL", "ISAT"
            )
            for (code in indonesianStocks) {
                if (upper.contains(code)) return "IDX:$code"
            }
            if (assetType.equals("Kripto", ignoreCase = true) || upper.contains("BITCOIN") || upper.contains("BTC")) return "BINANCE:BTCUSDT"
            if (upper.contains("ETHEREUM") || upper.contains("ETH")) return "BINANCE:ETHUSDT"
            if (assetType.equals("Emas", ignoreCase = true) || upper.contains("EMAS") || upper.contains("GOLD")) return "TVC:GOLD"
            if (isStock) {
                val words = upper.split(" ", "_", "-", "/")
                for (w in words) {
                    val letters = w.filter { it.isLetter() }
                    if (letters.length in 4..5 && !letters.equals("SAHAM", ignoreCase = true)) {
                        return "IDX:$letters"
                    }
                }
                return "IDX:BBRI"
            }
            return ""
        }
}

data class DebtRecord(
    val id: String = UUID.randomUUID().toString(),
    val debtId: String,
    val type: String = "Pembayaran", // "Pembayaran", "Pinjam Lagi", "Bunga"
    val amount: Double,
    val date: String = "2 Nov 2026",
    val time: String = "12.56",
    val sourceWalletName: String = "",
    val note: String = ""
)

data class DebtItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: DebtType = DebtType.BORROWED,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val dueDate: String,
    val startDate: String = "28 Mar 2026",
    val startTime: String = "12:00",
    val monthlyPayment: Double = 0.0,
    val interestRate: Double = 0.0,
    val extraFees: Double = 0.0,
    val categoryIcon: String = "CreditCard", // "House", "Building", "Car", "CreditCard", "Bank", "Motor", "Tools", "Cash"
    val institutionLogo: String = "",
    val tenureMonths: Int = 0,
    val linkedWalletName: String = "",
    val records: List<DebtRecord> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    val remainingAmount: Double
        get() = (totalAmount - paidAmount).coerceAtLeast(0.0)

    val progressPercent: Int
        get() = if (totalAmount > 0) ((paidAmount / totalAmount) * 100).toInt().coerceIn(0, 100) else 0
}

data class UpcomingScheduleItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val accountName: String,
    val amount: Double,
    val isIncome: Boolean,
    val dateLabel: String, // e.g. "1 Nov"
    val dayLabel: String   // e.g. "Min"
)

data class BudgetCategory(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val allocated: Double,
    val spent: Double = 0.0,
    val name: String = "",
    val categories: List<String> = emptyList(),
    val accountNames: List<String> = emptyList(),
    val frequency: String = "Bulanan",
    val customStartDate: String = "",
    val customEndDate: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    val displayName: String get() = name.ifBlank { category }
    val targetCategories: List<String> get() = categories.ifEmpty { listOf(category) }
    val remaining: Double get() = (allocated - spent).coerceAtLeast(0.0)
    val overAmount: Double get() = (spent - allocated).coerceAtLeast(0.0)
    val percentage: Float get() = if (allocated > 0) (spent / allocated).toFloat().coerceIn(0f, 1f) else 0f
    val rawPercentInt: Int get() = if (allocated > 0) kotlin.math.round((spent / allocated) * 100).toInt().coerceAtLeast(0) else 0
}

val defaultStatistikCardOrder = listOf(
    "TREND_INCOME",
    "TREND_EXPENSE",
    "EXPENSE_BREAKDOWN",
    "EXPENSE_PER_LABEL",
    "UPCOMING_TIMELINE",
    "CASH_FLOW",
    "INCOME_BREAKDOWN",
    "EARNINGS_SUMMARY",
    "SAVINGS_RATE",
    "DAILY_AVERAGE",
    "PERIOD_COMPARISON",
    "MOST_CHANGED_CATEGORY",
    "DAILY_PATTERN",
    "LARGEST_EXPENSES",
    "UPCOMING_BILLS",
    "PERIOD_HIGHLIGHT",
    "MONEY_FLOW_SANKEY",
    "FIXED_COSTS",
    "CASH_FLOW_JOURNEY",
    "EXPENSE_PER_ACCOUNT",
    "MONEY_PATH"
)

val defaultDashboardWidgetOrder = listOf(
    "SAFE_TO_SPEND",
    "BUDGET",
    "UPCOMING",
    "SPENDING_ACTIVITY"
)

data class UserSettings(
    val language: String = "Indonesia",
    val isDarkMode: Boolean = true,
    val primaryCurrency: String = "IDR",
    val monthlyStartDay: Int = 1,
    val textSizePercent: Int = 100,
    val cashAccountsOnly: Boolean = false,
    val showPlannedInBalance: Boolean = true,
    val saveReminder: Boolean = false,
    val billReminder: Boolean = false,
    val budgetAlert: Boolean = true,
    val passcodeEnabled: Boolean = false,
    val appPasscode: String = "1234",
    val themeColorName: String = "Money Tracker Blue",
    val numberFormat: String = "1.000.000 (Titik)",
    val dateFormat: String = "DD/MM/YYYY",
    val defaultHideBalance: Boolean = false,
    val dailyReminderTime: String = "20:00",
    val dailyReminderEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val startDayOfWeek: String = "Senin",
    val budgetWarningPercent: Int = 80,
    val targetSavingsPercent: Int = 20,
    val confirmDeleteTransaction: Boolean = true,
    val mainMenuOrder: List<String> = listOf(
        "Anggaran",
        "Perencana",
        "Acara",
        "Aset",
        "Tabungan",
        "Nilai Bersih",
        "Jalan Uang",
        "Utang",
        "Tagihan",
        "Cari",
        "Kategori",
        "Label",
        "Daftar Tunggu"
    ),
    val hiddenMenuItems: List<String> = emptyList(),
    val budgetSummaryStyle: String = "Sirkular",
    val budgetListStyle: String = "Tabel",
    val paydayAutoSaveEnabled: Boolean = false,
    val paydayAutoSavePercent: Int = 10,
    val statistikCardsOrder: List<String> = defaultStatistikCardOrder,
    val hiddenStatistikCards: List<String> = emptyList(),
    val trenBerdampinganEnabled: Boolean = true,
    val dashboardWidgetsOrder: List<String> = defaultDashboardWidgetOrder,
    val hiddenDashboardWidgets: List<String> = emptyList(),
    val hasCompletedOnboarding: Boolean = false
)

data class OnboardingBillInput(
    val id: String,
    val title: String,
    val amount: Double,
    val dayOfMonth: Int,
    val reminderEnabled: Boolean = false,
    val category: String = "Bills & Fees"
)

data class OnboardingBudgetInput(
    val categoryName: String,
    val matchCategories: List<String>,
    val amount: Double
)

data class SavingsRecord(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val isWithdraw: Boolean = false,
    val note: String = "",
    val dateStr: String = "", // "dd/MM/yyyy"
    val walletName: String = "", // empty = paper note ("Tidak ada akun")
    val linkedTransactionId: String = ""
)

data class SavingsGoal(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: String = "",
    val startDate: String = "",
    val currency: String = "IDR",
    val iconName: String = "Savings",
    val note: String = "",
    val colorHex: Long = 0xFF3B82F6L,
    val isCompleted: Boolean = false,
    val records: List<SavingsRecord> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    val progressPercent: Int
        get() = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toInt().coerceIn(0, 100) else 0
    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)
}

data class EventPlannedExpense(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "Other",
    val subcategory: String = "",
    val amount: Double = 0.0,
    val isPaid: Boolean = false
)

data class EventItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val eventType: String = "Perjalanan",
    val targetBudget: Double = 0.0,
    val monthIndex: Int = 11,
    val year: Int = 2026,
    val startDate: String = "",
    val endDate: String = "",
    val currency: String = "IDR",
    val iconName: String = "Celebration",
    val colorHex: Long = 0xFF3B82F6L,
    val note: String = "",
    val plannedExpenses: List<EventPlannedExpense> = emptyList(),
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class TimelineScenarioItem(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val isIncome: Boolean,
    val note: String,
    val date: String,
    val recurrence: String = "Hanya sekali",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class RecurringTransaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: String,
    val subcategory: String = "",
    val accountName: String,
    val toAccountName: String = "",
    val dayOfMonth: Int = 1, // 1..31
    val frequency: String = "Setiap bulan", // "Setiap hari", "Setiap minggu", "Setiap 2 minggu", "Setiap bulan", "Setiap tahun"
    val startDate: String = "", // e.g. "01/11/2026"
    val endDate: String = "", // e.g. "01/11/2027" or empty for tanpa batas
    val note: String = "",
    val label: String = "",
    val isActive: Boolean = true,
    val autoDeduct: Boolean = true,
    val lastProcessedMonthYear: String = "", // format "M/yyyy", e.g. "10/2026"
    val endCondition: String = "NEVER", // "NEVER", "ON_DATE", "AFTER_COUNT"
    val endAfterCount: Int = 0,
    val logoName: String = "",
    val skippedCount: Int = 0,
    val snoozedUntilDate: String = "",
    val lastSkippedDateLabel: String = "",
    val reminderEnabled: Boolean = false,
    val reminderOptions: List<String> = listOf("Pada hari jatuh tempo", "1 hari sebelumnya"),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

enum class PlanSectionType {
    INCOME,
    EXPENSE,
    SAVINGS
}

enum class NewPlanStartMode {
    COPY_ACTIVE,
    BILLS_ONLY,
    EMPTY
}

data class PlannedItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "",
    val subcategory: String = "",
    val amount: Double,
    val section: PlanSectionType
)

data class FinancialPlan(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Pembagian Gaji",
    val monthIndex: Int = 10,
    val year: Int = 2026,
    val items: List<PlannedItem> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class BackupRestoreResult(
    val transactionCount: Int = 0,
    val walletCount: Int = 0,
    val assetCount: Int = 0,
    val debtCount: Int = 0,
    val categoryCount: Int = 0,
    val planCount: Int = 0,
    val budgetCount: Int = 0,
    val recurringCount: Int = 0,
    val savingsGoalCount: Int = 0,
    val eventCount: Int = 0,
    val settingsRestored: Boolean = false,
    val logoCount: Int = 0
) {
    val totalItems: Int get() = transactionCount + walletCount + assetCount + debtCount + categoryCount + planCount + budgetCount + recurringCount + savingsGoalCount + eventCount + logoCount
}

