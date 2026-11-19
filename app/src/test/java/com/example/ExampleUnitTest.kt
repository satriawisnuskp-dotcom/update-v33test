package com.example

import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.util.ChartDataHelper
import com.example.util.CurrencyHelper
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCurrencyFormatting() {
        assertEquals("Rp341.472", CurrencyHelper.formatRupiah(341472.0))
        assertEquals("Rp6.182.200", CurrencyHelper.formatRupiah(6182200.0))
        assertEquals("4.8M", CurrencyHelper.formatCompact(4800000.0))
        assertEquals("170K", CurrencyHelper.formatCompact(170000.0))
        assertEquals("-100K", CurrencyHelper.formatCalendarExpense(100000.0))
    }

    @Test
    fun testThousandsSeparatorVisualTransformation() {
        val transformation = com.example.util.ThousandsSeparatorVisualTransformation('.')

        // Test empty string
        val emptyResult = transformation.filter(androidx.compose.ui.text.AnnotatedString(""))
        assertEquals("", emptyResult.text.text)

        // Test single/double/triple digits
        assertEquals("5", transformation.filter(androidx.compose.ui.text.AnnotatedString("5")).text.text)
        assertEquals("50", transformation.filter(androidx.compose.ui.text.AnnotatedString("50")).text.text)
        assertEquals("500", transformation.filter(androidx.compose.ui.text.AnnotatedString("500")).text.text)

        // Test thousands
        assertEquals("1.000", transformation.filter(androidx.compose.ui.text.AnnotatedString("1000")).text.text)
        assertEquals("10.000", transformation.filter(androidx.compose.ui.text.AnnotatedString("10000")).text.text)
        assertEquals("100.000", transformation.filter(androidx.compose.ui.text.AnnotatedString("100000")).text.text)

        // Test millions and billions
        assertEquals("1.500.000", transformation.filter(androidx.compose.ui.text.AnnotatedString("1500000")).text.text)
        assertEquals("25.000.000", transformation.filter(androidx.compose.ui.text.AnnotatedString("25000000")).text.text)
        assertEquals("1.000.000.000", transformation.filter(androidx.compose.ui.text.AnnotatedString("1000000000")).text.text)

        // Test offset mapping bounds and monotonicity
        val millionTransformed = transformation.filter(androidx.compose.ui.text.AnnotatedString("1000000"))
        assertEquals("1.000.000", millionTransformed.text.text)
        val mapping = millionTransformed.offsetMapping

        // originalToTransformed checks
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(9, mapping.originalToTransformed(7))

        // transformedToOriginal checks
        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(7, mapping.transformedToOriginal(9))
    }

    @Test
    fun testRepositoryInitialBalancesAndReactiveAddition() {
        val repo = FinanceRepository()
        repo.addWallet(com.example.data.model.AccountWallet(name = "BRI Utama", balance = 1_000_000.0))
        val initialWallets = repo.wallets.value
        val bri = initialWallets.find { it.name == "BRI Utama" }
        val initialBalance = bri?.balance ?: 0.0

        // Add expense of 50.000 to BRI Utama
        val tx = Transaction(
            title = "Beli Kopi",
            category = "Food & Dining",
            amount = 50000.0,
            type = TransactionType.EXPENSE,
            accountName = "BRI Utama",
            dayOfMonth = 28,
            monthIndex = 10,
            dayOfWeekName = "Rab"
        )
        repo.addTransaction(tx)

        val updatedBri = repo.wallets.value.find { it.name == "BRI Utama" }
        assertEquals(initialBalance - 50000.0, updatedBri?.balance ?: 0.0, 0.01)
        assertTrue(repo.transactions.value.any { it.title == "Beli Kopi" })
    }

    @Test
    fun testNetWorthCalculation() {
        val repo = FinanceRepository()
        repo.addWallet(com.example.data.model.AccountWallet(name = "BRI Utama", balance = 5_000_000.0))
        repo.addAsset(com.example.data.model.AssetItem(name = "Emas", value = 3_000_000.0))
        repo.addDebt(
            com.example.data.model.DebtItem(
                name = "Cicilan",
                type = com.example.data.model.DebtType.BORROWED,
                totalAmount = 1_000_000.0,
                paidAmount = 250_000.0,
                dueDate = "31 Des 2026"
            )
        )
        val totalWallets = repo.wallets.value.sumOf { it.balance }
        val totalAssets = repo.assets.value.sumOf { it.value }
        val totalDebts = repo.debts.value.sumOf { it.totalAmount - it.paidAmount }

        val netWorth = totalWallets + totalAssets - totalDebts
        assertEquals(7_250_000.0, netWorth, 0.01)
    }

    @Test
    fun testMonthAndYearSelection() {
        val repo = FinanceRepository()
        val viewModel = com.example.ui.viewmodel.FinanceViewModel(repo)

        viewModel.selectMonthAndYear(11, 2027)
        assertEquals(11, viewModel.selectedMonthIndex.value)
        assertEquals(2027, viewModel.selectedYear.value)
    }

    @Test
    fun testRecurringTransactionAutoDeduct() {
        val repo = FinanceRepository()
        repo.addWallet(com.example.data.model.AccountWallet(name = "BRI Utama", balance = 1_000_000.0))
        val bri = repo.wallets.value.find { it.name == "BRI Utama" }
        val initialBalance = bri?.balance ?: 0.0

        val recurring = com.example.data.model.RecurringTransaction(
            title = "Langganan Spotify Family",
            amount = 86000.0,
            type = TransactionType.EXPENSE,
            category = "Bills & Fees",
            accountName = "BRI Utama",
            dayOfMonth = 15,
            autoDeduct = true,
            isActive = true
        )
        // Add recurring transaction with currentDay = 28 (today is past day 15), so it should auto-deduct for month 10!
        repo.addRecurringTransaction(recurring, checkImmediateProcess = true, currentDay = 28, currentMonth = 10, currentYear = 2026)

        val updatedBri = repo.wallets.value.find { it.name == "BRI Utama" }
        assertEquals(initialBalance - 86000.0, updatedBri?.balance ?: 0.0, 0.01)

        val createdTx = repo.transactions.value.find { it.title == "Langganan Spotify Family" }
        assertTrue("Automatic transaction must be recorded in transactions list", createdTx != null)
        assertEquals(15, createdTx?.dayOfMonth)
        assertEquals(10, createdTx?.monthIndex)
    }

    @Test
    fun testPlannerCalculationsAndEdit() {
        val repo = FinanceRepository()
        val viewModel = com.example.ui.viewmodel.FinanceViewModel(repo)
        viewModel.createFinancialPlan(
            monthIndex = 11,
            year = 2026,
            mode = com.example.data.model.NewPlanStartMode.EMPTY,
            customTitle = "Rencana Uji"
        )
        repo.addPlannedItem(
            com.example.data.model.PlannedItem(
                name = "Gaji Pokok",
                category = "Salary",
                amount = 2684000.0,
                section = com.example.data.model.PlanSectionType.INCOME
            )
        )
        repo.addPlannedItem(
            com.example.data.model.PlannedItem(
                name = "Bonus",
                category = "Bonus",
                amount = 3074000.0,
                section = com.example.data.model.PlanSectionType.INCOME
            )
        )
        repo.addPlannedItem(
            com.example.data.model.PlannedItem(
                name = "Biaya Hidup",
                category = "Food & Drink",
                amount = 4150000.0,
                section = com.example.data.model.PlanSectionType.EXPENSE
            )
        )
        repo.addPlannedItem(
            com.example.data.model.PlannedItem(
                name = "Dana Darurat",
                category = "Savings",
                amount = 1600000.0,
                section = com.example.data.model.PlanSectionType.SAVINGS
            )
        )

        val incomeTotal = repo.plannedItems.value.filter { it.section == com.example.data.model.PlanSectionType.INCOME }.sumOf { it.amount }
        val expenseTotal = repo.plannedItems.value.filter { it.section == com.example.data.model.PlanSectionType.EXPENSE }.sumOf { it.amount }
        val savingsTotal = repo.plannedItems.value.filter { it.section == com.example.data.model.PlanSectionType.SAVINGS }.sumOf { it.amount }

        assertEquals(5758000.0, incomeTotal, 0.01)
        assertEquals(4150000.0, expenseTotal, 0.01)
        assertEquals(1600000.0, savingsTotal, 0.01)

        val unallocated = incomeTotal - (expenseTotal + savingsTotal)
        assertEquals(8000.0, unallocated, 0.01)

        // Test editing an item with pencil edit
        val gajiItem = repo.plannedItems.value.find { it.name == "Gaji Pokok" }
        assertTrue(gajiItem != null)
        repo.updatePlannedItem(gajiItem!!.copy(amount = 3000000.0))

        val updatedIncome = repo.plannedItems.value.filter { it.section == com.example.data.model.PlanSectionType.INCOME }.sumOf { it.amount }
        assertEquals(5758000.0 + (3000000.0 - 2684000.0), updatedIncome, 0.01)
    }

    @Test
    fun testChartPeriodOneMonthSpansFullMonth() {
        val repo = FinanceRepository()
        val wallets = repo.wallets.value
        val assets = repo.assets.value
        val debts = repo.debts.value
        val transactions = repo.transactions.value

        val chartData = com.example.util.ChartDataHelper.getNetWorthTrend(
            wallets = wallets,
            assets = assets,
            debts = debts,
            transactions = transactions,
            subTab = 0,
            period = com.example.util.ChartPeriod.ONE_MONTH
        )

        // 1M should span a full month with 30 data points
        assertEquals(30, chartData.points.size)
        assertEquals(30, chartData.rawValues.size)
        assertTrue(chartData.startLabel.isNotBlank())
        assertTrue(chartData.midLabel.isNotBlank())
        assertTrue(chartData.endLabel.isNotBlank())
    }

    @Test
    fun testCustomUploadedIconToModels() {
        val customWallet = com.example.data.model.AccountWallet(
            name = "Dompet Pribadi",
            balance = 1500000.0,
            iconName = "custom:/data/user/0/com.aistudio.keuangan/files/custom_icons/wallet_123.png"
        )
        assertTrue(customWallet.iconName.startsWith("custom:"))

        val customAsset = com.example.data.model.AssetItem(
            name = "Emas Batangan",
            value = 10000000.0,
            iconUri = "custom:/data/user/0/com.aistudio.keuangan/files/custom_icons/asset_123.png"
        )
        assertTrue(customAsset.iconUri.startsWith("custom:"))
    }

    @Test
    fun testTransactionChronologicalDescendingComparatorWithTime() {
        val helper = com.example.util.TransactionMathHelper
        assertEquals(14 * 3600 + 30 * 60, helper.parseTimeToSeconds("14:30"))
        assertEquals(9 * 3600 + 15 * 60, helper.parseTimeToSeconds("09:15"))
        assertEquals(20 * 3600 + 45 * 60, helper.parseTimeToSeconds("08:45 PM"))
        assertEquals(8 * 3600 + 45 * 60, helper.parseTimeToSeconds("08:45 AM"))

        val tx1 = Transaction(id = "1", title = "Sarapan", category = "Makanan", type = TransactionType.EXPENSE, accountName = "Dompet", amount = 25000.0, dayOfMonth = 10, monthIndex = 11, year = 2026, time = "07:30")
        val tx2 = Transaction(id = "2", title = "Makan Siang", category = "Makanan", type = TransactionType.EXPENSE, accountName = "Dompet", amount = 45000.0, dayOfMonth = 10, monthIndex = 11, year = 2026, time = "12:15")
        val tx3 = Transaction(id = "3", title = "Kopi Sore", category = "Makanan", type = TransactionType.EXPENSE, accountName = "Dompet", amount = 30000.0, dayOfMonth = 10, monthIndex = 11, year = 2026, time = "16:45")
        val tx4 = Transaction(id = "4", title = "Makan Malam", category = "Makanan", type = TransactionType.EXPENSE, accountName = "Dompet", amount = 60000.0, dayOfMonth = 10, monthIndex = 11, year = 2026, time = "20:00")
        val txOlderDay = Transaction(id = "5", title = "Kemarin", category = "Makanan", type = TransactionType.EXPENSE, accountName = "Dompet", amount = 50000.0, dayOfMonth = 9, monthIndex = 11, year = 2026, time = "23:59")

        val list = listOf(tx1, txOlderDay, tx3, tx2, tx4)
        val sorted = list.sortedWith(helper.transactionChronologicalDescendingComparator)

        assertEquals("Makan Malam", sorted[0].title)
        assertEquals("Kopi Sore", sorted[1].title)
        assertEquals("Makan Siang", sorted[2].title)
        assertEquals("Sarapan", sorted[3].title)
        assertEquals("Kemarin", sorted[4].title)
    }

    @Test
    fun testIndonesianInstitutionHelperCustomLogoSync() {
        val helper = com.example.util.IndonesianInstitutionHelper
        val initialVersion = helper.logoRegistryVersion

        helper.registerCustomLogo("BCA Digital", "custom:/data/user/0/com.aistudio.keuangan/files/custom_icons/bca_new.png")
        assertTrue(helper.logoRegistryVersion > initialVersion)

        val resolved = helper.getRegisteredLogo("BCA Digital")
        assertEquals("custom:/data/user/0/com.aistudio.keuangan/files/custom_icons/bca_new.png", resolved)

        val resolvedIgnoreCase = helper.getRegisteredLogo("bca digital")
        assertEquals("custom:/data/user/0/com.aistudio.keuangan/files/custom_icons/bca_new.png", resolvedIgnoreCase)
    }

    @Test
    fun testImageUploadHelperFileNameExtraction() {
        val helper = com.example.util.ImageUploadHelper
        assertEquals("wallet_123.png", helper.extractFileName("custom:/data/user/0/app/files/custom_icons/wallet_123.png"))
        assertEquals("asset_456.png", helper.extractFileName("file:///storage/emulated/0/asset_456.png"))
        assertEquals("icon_789.png", helper.extractFileName("/storage/emulated/0/icon_789.png"))
        assertEquals("Bank", helper.extractFileName("Bank"))
    }

    @Test
    fun testStockAssetLotAverageAndRealValueCalculations() {
        val stockAsset = com.example.data.model.AssetItem(
            name = "Saham BBRI",
            assetType = "Saham",
            value = 0.0,
            lotCount = 10.0,
            averagePrice = 4500.0,
            currentPrice = 5000.0
        )

        assertTrue(stockAsset.isStock)
        assertEquals(1000.0, stockAsset.sharesCount, 0.01)
        assertEquals(4500000.0, stockAsset.costBasis, 0.01)
        assertEquals(5000000.0, stockAsset.realValue, 0.01)
        assertEquals(500000.0, stockAsset.unrealizedGainLoss, 0.01)
        assertEquals(11.11, stockAsset.calculatedPerformancePercent, 0.05)
    }

    @Test
    fun testLentDebtRepaymentIncreasesWalletBalance() {
        val repo = FinanceRepository()
        val walletName = "BRI Utama"
        repo.addWallet(com.example.data.model.AccountWallet(name = walletName, balance = 2_000_000.0))
        val initialWallet = repo.wallets.value.find { it.name.equals(walletName, ignoreCase = true) }
        val initialBalance = initialWallet?.balance ?: 0.0
        assertTrue("Initial balance should be positive", initialBalance > 0)

        // Create a LENT debt (uang yang dipinjamkan)
        val lentDebt = com.example.data.model.DebtItem(
            id = "lent_test_1",
            name = "Pinjaman Budi",
            type = com.example.data.model.DebtType.LENT,
            totalAmount = 1000000.0,
            dueDate = "30 Des 2026",
            linkedWalletName = walletName
        )
        repo.addDebt(lentDebt, depositToWalletName = null)

        // Record a repayment of 300.000 from borrower
        val paymentRecord = com.example.data.model.DebtRecord(
            id = "rec_lent_1",
            debtId = lentDebt.id,
            type = "Pembayaran",
            amount = 300000.0,
            sourceWalletName = walletName,
            note = "Cicilan ke-1 dari Budi"
        )
        repo.addDebtRecord(lentDebt.id, paymentRecord, affectWalletName = walletName)

        // The wallet balance MUST have INCREASED by 300.000, not decreased!
        val walletAfterPayment = repo.wallets.value.find { it.name.equals(walletName, ignoreCase = true) }
        assertEquals(initialBalance + 300000.0, walletAfterPayment?.balance ?: 0.0, 0.01)

        // And an Income transaction should have been recorded
        val tx = repo.transactions.value.find { it.id == "debt_rec_rec_lent_1" }
        assertEquals(TransactionType.INCOME, tx?.type)
        assertEquals(300000.0, tx?.amount ?: 0.0, 0.01)

        // Delete the record and verify wallet balance reverts (decreases back)
        repo.deleteDebtRecord(lentDebt.id, paymentRecord.id)
        val walletAfterDelete = repo.wallets.value.find { it.name.equals(walletName, ignoreCase = true) }
        assertEquals(initialBalance, walletAfterDelete?.balance ?: 0.0, 0.01)
    }

    @Test
    fun testBorrowedDebtPaymentDecreasesWalletBalance() {
        val repo = FinanceRepository()
        val walletName = "BRI Utama"
        repo.addWallet(com.example.data.model.AccountWallet(name = walletName, balance = 2_000_000.0))
        val initialWallet = repo.wallets.value.find { it.name.equals(walletName, ignoreCase = true) }
        val initialBalance = initialWallet?.balance ?: 0.0
        assertTrue("Initial balance should be positive", initialBalance > 0)

        // Create a BORROWED debt (utang yang dipinjam user)
        val borrowedDebt = com.example.data.model.DebtItem(
            id = "borrowed_test_1",
            name = "KTA Bank",
            type = com.example.data.model.DebtType.BORROWED,
            totalAmount = 2000000.0,
            dueDate = "30 Des 2026",
            linkedWalletName = walletName
        )
        repo.addDebt(borrowedDebt, depositToWalletName = null)

        // User pays debt 100.000
        val paymentRecord = com.example.data.model.DebtRecord(
            id = "rec_borrowed_1",
            debtId = borrowedDebt.id,
            type = "Pembayaran",
            amount = 100000.0,
            sourceWalletName = walletName,
            note = "Cicilan KTA"
        )
        repo.addDebtRecord(borrowedDebt.id, paymentRecord, affectWalletName = walletName)

        // The wallet balance MUST have DECREASED by 100.000
        val walletAfterPayment = repo.wallets.value.find { it.name.equals(walletName, ignoreCase = true) }
        assertEquals(initialBalance - 100000.0, walletAfterPayment?.balance ?: 0.0, 0.01)
    }

    @Test
    fun testReactiveFinancialChartPointsAndPeriods() {
        val helper = com.example.util.ChartDataHelper
        val currentBal = 2500000.0

        val tx1 = Transaction(
            id = "tx_chart_1",
            title = "Gaji",
            amount = 1000000.0,
            type = TransactionType.INCOME,
            category = "Salary",
            accountName = "BRI Utama",
            dayOfMonth = 5,
            monthIndex = 11,
            year = 2026,
            time = "10:00"
        )
        val tx2 = Transaction(
            id = "tx_chart_2",
            title = "Belanja",
            amount = 200000.0,
            type = TransactionType.EXPENSE,
            category = "Shopping",
            accountName = "BRI Utama",
            dayOfMonth = 6,
            monthIndex = 11,
            year = 2026,
            time = "14:00"
        )

        val txs = listOf(tx1, tx2)

        // 1. Check all 5 periods exist and are non-empty
        val allPointsMap = helper.generateAllTimeframePoints(txs, currentBal)
        assertTrue(allPointsMap.containsKey("7D"))
        assertTrue(allPointsMap.containsKey("1M"))
        assertTrue(allPointsMap.containsKey("3M"))
        assertTrue(allPointsMap.containsKey("YTD"))
        assertTrue(allPointsMap.containsKey("ALL"))

        // 2. 7D has 7 points
        val points7D = allPointsMap["7D"]!!
        assertEquals(7, points7D.size)

        // The final point must match the current balance exactly
        assertEquals(currentBal, points7D.last().value, 0.01)

        // 3. 1M has 30 points
        val points1M = allPointsMap["1M"]!!
        assertEquals(30, points1M.size)
        assertEquals(currentBal, points1M.last().value, 0.01)

        // 4. Test empty transactions: does not crash and renders valid baseline with currentBalance
        val emptyPointsMap = helper.generateAllTimeframePoints(emptyList(), currentBal)
        val empty7D = emptyPointsMap["7D"]!!
        assertEquals(7, empty7D.size)
        assertEquals(currentBal, empty7D.first().value, 0.01)
        assertEquals(currentBal, empty7D.last().value, 0.01)
        assertEquals(0.0, empty7D.last().deltaAmount, 0.01)
        assertEquals(0.0, empty7D.last().percentChange, 0.01)

        // 5. Test single transaction
        val singleTxList = listOf(tx1)
        val singlePointsMap = helper.generateAllTimeframePoints(singleTxList, currentBal)
        val single7D = singlePointsMap["7D"]!!
        assertEquals(7, single7D.size)
        assertEquals(currentBal, single7D.last().value, 0.01)
    }

    @Test
    fun testViewModelChartPointsReactiveSync() {
        val repo = FinanceRepository()
        repo.addWallet(com.example.data.model.AccountWallet(name = "BRI Utama", balance = 1_500_000.0))
        val initialWallets = repo.wallets.value.sumOf { it.balance }
        val initialDashboardPoints = ChartDataHelper.generateAllTimeframePoints(repo.transactions.value, initialWallets)["7D"]
        assertTrue(!initialDashboardPoints.isNullOrEmpty())
        val initialEndBal = initialDashboardPoints!!.last().value

        // Add transaction of 150.000 expense
        val newTx = Transaction(
            title = "Test Belanja Baru",
            category = "Other",
            amount = 150000.0,
            type = TransactionType.EXPENSE,
            accountName = "BRI Utama",
            dayOfMonth = 7,
            monthIndex = 11,
            year = 2026,
            time = "18:00"
        )
        repo.addTransaction(newTx)

        val updatedWallets = repo.wallets.value.sumOf { it.balance }
        val updatedDashboardPoints = ChartDataHelper.generateAllTimeframePoints(repo.transactions.value, updatedWallets)["7D"]
        assertTrue(!updatedDashboardPoints.isNullOrEmpty())
        val updatedEndBal = updatedDashboardPoints!!.last().value

        // End balance in chart points must decrease by 150.000
        assertEquals(initialEndBal - 150000.0, updatedEndBal, 0.01)

        // Delete transaction and verify re-calculation
        val created = repo.transactions.value.find { it.title == "Test Belanja Baru" }
        assertTrue(created != null)
        repo.deleteTransaction(created!!.id)

        val revertedWallets = repo.wallets.value.sumOf { it.balance }
        val revertedDashboardPoints = ChartDataHelper.generateAllTimeframePoints(repo.transactions.value, revertedWallets)["7D"]
        assertEquals(initialEndBal, revertedDashboardPoints!!.last().value, 0.01)
    }

    @Test
    fun testDailyScrubbingAndDragCoordinateMapping() {
        val helper = ChartDataHelper
        val currentBal = 1000000.0
        val pointsMap = helper.generateAllTimeframePoints(emptyList(), currentBal)

        // 3M timeframe must have daily resolution (90 daily points, not 16 points stepping by 6 days)
        val points3M = pointsMap["3M"]!!
        assertEquals(90, points3M.size)

        // Verify Drag Coordinate Mapping formula
        val canvasWidth = 1000f
        val dataSize = points3M.size
        fun calculateIndex(dragX: Float): Int {
            return ((dragX / canvasWidth) * (dataSize - 1)).roundToInt().coerceIn(0, dataSize - 1)
        }

        // Left edge touch (0px) -> index 0
        assertEquals(0, calculateIndex(0f))
        // Slight negative overshoot -> clamped to index 0
        assertEquals(0, calculateIndex(-50f))
        // Midpoint touch (500px) -> index 45
        assertEquals(45, calculateIndex(500f))
        // Right edge touch (1000px) -> index 89 (dataSize - 1)
        assertEquals(89, calculateIndex(1000f))
        // Positive overshoot (1200px) -> clamped to index 89
        assertEquals(89, calculateIndex(1200f))

        // Check that small incremental drag of ~11.2px steps through consecutive indices
        val stepPx = canvasWidth / (dataSize - 1)
        for (i in 0 until dataSize) {
            val posX = i * stepPx
            assertEquals(i, calculateIndex(posX))
        }
    }

    @Test
    fun testBudgetMathHelperAndRepositoryCrud() {
        val repo = FinanceRepository()
        val viewModel = com.example.ui.viewmodel.FinanceViewModel(repo)

        val wallets = listOf(
            com.example.data.model.AccountWallet(name = "BCA", balance = 5000000.0),
            com.example.data.model.AccountWallet(name = "Tabungan", balance = 2000000.0, excludeFromBudget = true)
        )
        val categories = listOf(
            com.example.data.model.MainCategory(
                name = "Food & Drink",
                type = TransactionType.EXPENSE,
                subcategories = listOf(com.example.data.model.SubcategoryItem(name = "Kopi"))
            )
        )
        val transactions = listOf(
            Transaction(
                id = "b_tx_1",
                title = "Sarapan",
                amount = 100000.0,
                type = TransactionType.EXPENSE,
                category = "Food & Dining",
                accountName = "BCA",
                dayOfMonth = 10,
                monthIndex = 11,
                year = 2026
            ),
            Transaction(
                id = "b_tx_2",
                title = "Kopi Susu",
                amount = 50000.0,
                type = TransactionType.EXPENSE,
                category = "Food & Drink",
                subcategory = "Kopi",
                accountName = "BCA",
                dayOfMonth = 11,
                monthIndex = 11,
                year = 2026
            ),
            Transaction(
                id = "b_tx_excluded",
                title = "Makan dari Tabungan",
                amount = 200000.0,
                type = TransactionType.EXPENSE,
                category = "Food & Drink",
                accountName = "Tabungan",
                dayOfMonth = 12,
                monthIndex = 11,
                year = 2026
            )
        )

        val budget = com.example.data.model.BudgetCategory(
            id = "b_test_1",
            category = "Food & Drink",
            name = "Anggaran Makan",
            allocated = 120000.0,
            categories = listOf("Food & Drink"),
            accountNames = emptyList(),
            frequency = "Bulanan"
        )

        val computed = com.example.util.BudgetMathHelper.computeBudgetItem(
            budget = budget,
            allTransactions = transactions,
            wallets = wallets,
            allCategories = categories,
            selectedMonth = 11,
            selectedYear = 2026
        )

        // Should sum BCA transactions (100k + 50k = 150k) and exclude Tabungan (excludeFromBudget = true)
        assertEquals(150000.0, computed.spent, 0.01)
        assertEquals(0.0, computed.remaining, 0.01)
        assertEquals(30000.0, computed.overAmount, 0.01)
        assertEquals(125, computed.rawPercentInt)
        assertEquals(1f, computed.percentage, 0.001f)

        // Test ViewModel budget CRUD and display style persistence
        viewModel.addBudget(budget)
        assertTrue(viewModel.budgets.value.any { it.id == "b_test_1" && it.displayName == "Anggaran Makan" })

        viewModel.updateBudget(budget.copy(allocated = 300000.0))
        assertEquals(300000.0, viewModel.budgets.value.first { it.id == "b_test_1" }.allocated, 0.01)

        viewModel.updateBudgetDisplayStyle("Bilah", "Kisi")
        assertEquals("Bilah", viewModel.settings.value.budgetSummaryStyle)
        assertEquals("Kisi", viewModel.settings.value.budgetListStyle)

        viewModel.deleteBudget("b_test_1")
        assertTrue(viewModel.budgets.value.none { it.id == "b_test_1" })
    }

    @Test
    fun testMultiPlanCreateSwitchAndDelete() {
        val repo = FinanceRepository()
        val viewModel = com.example.ui.viewmodel.FinanceViewModel(repo)

        repo.addRecurringTransaction(
            com.example.data.model.RecurringTransaction(
                title = "Listrik PLN",
                amount = 400_000.0,
                type = TransactionType.EXPENSE,
                category = "Bills & Fees",
                accountName = "Tunai",
                dayOfMonth = 15,
                isActive = true
            ),
            checkImmediateProcess = false
        )
        viewModel.createFinancialPlan(
            monthIndex = 11,
            year = 2026,
            mode = com.example.data.model.NewPlanStartMode.BILLS_ONLY,
            customTitle = "Rencana Awal"
        )

        val initialPlanCount = viewModel.financialPlans.value.size
        assertTrue(initialPlanCount >= 1)
        val initialPlanId = viewModel.activePlanId.value

        // 1. Create plan copying active items
        val copiedPlan = viewModel.createFinancialPlan(
            monthIndex = 12,
            year = 2026,
            mode = com.example.data.model.NewPlanStartMode.COPY_ACTIVE,
            customTitle = "Rencana Desember"
        )
        assertEquals(initialPlanCount + 1, viewModel.financialPlans.value.size)
        assertEquals(copiedPlan.id, viewModel.activePlanId.value)
        assertEquals("Rencana Desember", viewModel.planTitle.value)
        assertEquals(copiedPlan.items.size, viewModel.plannedItems.value.size)

        // 2. Create plan with bills only (all active recurring items)
        val billsPlan = viewModel.createFinancialPlan(
            monthIndex = 1,
            year = 2027,
            mode = com.example.data.model.NewPlanStartMode.BILLS_ONLY
        )
        assertEquals(billsPlan.id, viewModel.activePlanId.value)
        val activeRecurringCount = repo.recurringTransactions.value.count { it.isActive }
        assertEquals(activeRecurringCount, viewModel.plannedItems.value.size)

        // 3. Create empty plan
        val emptyPlan = viewModel.createFinancialPlan(
            monthIndex = 2,
            year = 2027,
            mode = com.example.data.model.NewPlanStartMode.EMPTY
        )
        assertEquals(emptyPlan.id, viewModel.activePlanId.value)
        assertTrue(viewModel.plannedItems.value.isEmpty())

        // 4. Switch back to initial plan and delete created plans
        viewModel.selectActivePlan(initialPlanId)
        assertEquals(initialPlanId, viewModel.activePlanId.value)
        viewModel.deleteFinancialPlan(copiedPlan.id)
        viewModel.deleteFinancialPlan(billsPlan.id)
        viewModel.deleteFinancialPlan(emptyPlan.id)
        assertEquals(initialPlanCount, viewModel.financialPlans.value.size)
    }

    @Test
    fun testEventCrudAndLabelSync() {
        val repo = FinanceRepository()
        val viewModel = com.example.ui.viewmodel.FinanceViewModel(repo)
        repo.addWallet(com.example.data.model.AccountWallet(name = "Tunai", balance = 5_000_000.0))

        val event = com.example.data.model.EventItem(
            id = "ev_bali_2026",
            name = "Liburan Bali",
            targetBudget = 5000000.0,
            monthIndex = 11,
            year = 2026,
            iconName = "Flight",
            colorHex = 0xFF3B82F6L,
            note = "Tiket & Hotel"
        )
        viewModel.addEvent(event)
        assertTrue(viewModel.events.value.any { it.id == "ev_bali_2026" })
        assertTrue(viewModel.labels.value.contains("Liburan Bali"))

        // Add a transaction linked to this event label
        val tx = Transaction(
            id = "ev_tx_1",
            title = "Tiket Pesawat",
            category = "Transport",
            amount = 1500000.0,
            type = TransactionType.EXPENSE,
            accountName = repo.wallets.value.first().name,
            dayOfMonth = 15,
            monthIndex = 11,
            year = 2026,
            label = "Liburan Bali"
        )
        repo.addTransaction(tx)

        // Rename event and verify both labels and linked transactions sync
        viewModel.updateEvent(event.copy(name = "Trip Bali 2026", targetBudget = 6000000.0))
        val updatedEvent = viewModel.events.value.first { it.id == "ev_bali_2026" }
        assertEquals("Trip Bali 2026", updatedEvent.name)
        assertEquals(6000000.0, updatedEvent.targetBudget, 0.01)
        assertTrue(viewModel.labels.value.contains("Trip Bali 2026"))
        assertEquals(
            "Trip Bali 2026",
            repo.transactions.value.first { it.id == "ev_tx_1" }.label
        )

        // Delete event
        viewModel.deleteEvent("ev_bali_2026")
        assertTrue(viewModel.events.value.none { it.id == "ev_bali_2026" })
    }

    @Test
    fun testSavingsGoalSingleDeductionDepositAndWithdraw() {
        val repo = FinanceRepository()
        val viewModel = com.example.ui.viewmodel.FinanceViewModel(repo)
        repo.addWallet(com.example.data.model.AccountWallet(name = "Tunai", balance = 2_000_000.0))

        val wallet = repo.wallets.value.first()
        val initialWalletBalance = wallet.balance

        val goal = com.example.data.model.SavingsGoal(
            id = "goal_darurat",
            name = "Dana Darurat",
            targetAmount = 10000000.0,
            currentAmount = 0.0,
            targetDate = "Des 2027"
        )
        viewModel.addSavingsGoal(goal)
        assertTrue(viewModel.savingsGoals.value.any { it.id == "goal_darurat" })

        // Deposit 500.000 -> wallet must decrease by EXACTLY 500.000 (not double-deducted 1.000.000)
        val depositOk = viewModel.depositToSavingsGoal("goal_darurat", 500000.0, wallet.name)
        assertTrue(depositOk)

        val afterDepositWalletBalance = repo.wallets.value.first { it.name == wallet.name }.balance
        assertEquals(initialWalletBalance - 500000.0, afterDepositWalletBalance, 0.01)
        assertEquals(
            500000.0,
            viewModel.savingsGoals.value.first { it.id == "goal_darurat" }.currentAmount,
            0.01
        )

        // Withdraw 200.000 -> wallet must increase by 200.000 and goal currentAmount becomes 300.000
        val withdrawOk = viewModel.withdrawFromSavingsGoal("goal_darurat", 200000.0, wallet.name)
        assertTrue(withdrawOk)

        val afterWithdrawWalletBalance = repo.wallets.value.first { it.name == wallet.name }.balance
        assertEquals(initialWalletBalance - 300000.0, afterWithdrawWalletBalance, 0.01)
        assertEquals(
            300000.0,
            viewModel.savingsGoals.value.first { it.id == "goal_darurat" }.currentAmount,
            0.01
        )
    }

    @Test
    fun testNegativeNetWorthCalculationsAndFormatting() {
        assertEquals("-Rp500.000", CurrencyHelper.formatRupiah(-500_000.0))
        assertEquals("-500.000", CurrencyHelper.formatRupiah(-500_000.0, withPrefix = false))
        assertEquals("-Rp1.5M", CurrencyHelper.formatCompactRupiah(-1_500_000.0))

        val wallets = listOf(com.example.data.model.AccountWallet(name = "BCA", balance = 1_000_000.0))
        val assets = listOf(com.example.data.model.AssetItem(name = "Emas", value = 500_000.0))
        val debts = listOf(
            com.example.data.model.DebtItem(
                name = "KPR",
                type = com.example.data.model.DebtType.BORROWED,
                totalAmount = 4_000_000.0,
                paidAmount = 0.0,
                dueDate = "31 Des 2027"
            )
        )
        // Net Worth = 1_000_000 + 500_000 - 4_000_000 = -2_500_000
        val timeframeMap = ChartDataHelper.generateNetWorthTimeframePoints(wallets, assets, debts, emptyList())
        val monthPoints = timeframeMap["1M"]!!
        assertEquals(-2_500_000.0, monthPoints.last().value, 0.01)
        assertEquals("-Rp2.500.000", monthPoints.last().formattedValue)

        val trend = ChartDataHelper.getNetWorthTrend(wallets, assets, debts, emptyList(), subTab = 0, period = com.example.util.ChartPeriod.ONE_MONTH)
        assertEquals(-2_500_000.0, trend.total, 0.01)
        assertTrue(trend.rawValues.all { it < 0.0 })

        val monthlyBars = ChartDataHelper.getMonthlyNetWorthBars(wallets, assets, debts, emptyList())
        assertEquals(-2_500_000.0, monthlyBars.last().value, 0.01)
        assertEquals("-2.5M", monthlyBars.last().formattedValue)
    }

    @Test
    fun testLabelExpenseBreakdownIntegrationAndMultiLabels() {
        val registeredLabels = listOf("Investasi", "Liburan Bali", "Hobi")
        val transactions = listOf(
            Transaction(
                id = "lbl_tx_1",
                title = "Tiket & Hotel",
                category = "Transport",
                amount = 600_000.0,
                type = TransactionType.EXPENSE,
                accountName = "BCA",
                dayOfMonth = 10,
                monthIndex = 11,
                year = 2026,
                label = "liburan bali, Investasi"
            ),
            Transaction(
                id = "lbl_tx_2",
                title = "Makan Siang",
                category = "Food & Dining",
                amount = 100_000.0,
                type = TransactionType.EXPENSE,
                accountName = "BCA",
                dayOfMonth = 11,
                monthIndex = 11,
                year = 2026,
                label = ""
            )
        )

        val breakdown = com.example.util.TransactionMathHelper.buildLabelExpenseBreakdown(transactions, registeredLabels)
        val liburan = breakdown.first { it.label == "Liburan Bali" }
        val investasi = breakdown.first { it.label == "Investasi" }
        val hobi = breakdown.first { it.label == "Hobi" }
        val tanpaLabel = breakdown.first { it.label == "Tanpa Label" }

        assertEquals(600_000.0, liburan.amount, 0.01)
        assertEquals(600_000.0, investasi.amount, 0.01)
        assertEquals(1, liburan.transactionCount)
        assertEquals(true, liburan.isRegistered)
        assertEquals(0.0, hobi.amount, 0.01)
        assertEquals(true, hobi.isRegistered)
        assertEquals(100_000.0, tanpaLabel.amount, 0.01)
        assertEquals(false, tanpaLabel.isRegistered)
    }
}

