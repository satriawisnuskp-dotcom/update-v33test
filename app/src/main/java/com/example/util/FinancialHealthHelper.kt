package com.example.util

import com.example.data.model.AccountWallet
import com.example.data.model.AssetItem
import com.example.data.model.BudgetCategory
import com.example.data.model.DebtItem
import com.example.data.model.DebtType
import com.example.data.model.MainCategory
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import kotlin.math.abs
import kotlin.math.roundToInt

data class HealthPillarReport(
    val name: String,
    val description: String,
    val score: Int,
    val maxScore: Int = 20,
    val statusLevel: Int // 1 = baik (hijau), 0 = cukup (kuning), -1 = perlu perhatian (merah)
)

data class FinancialHealthReport(
    val score: Int,
    val statusLabel: String,
    val statusLevel: Int,
    val savingsRatePct: Double,
    val netSurplus: Double,
    val prevNetDeltaPct: Double?,
    val hasPrevNetComparison: Boolean,
    val expenseChangePct: Double?,
    val trackedDaysCount: Int,
    val daysInMonth: Int,
    val longestStreakDays: Int,
    val incomeSourceCount: Int,
    val safeBudgetCount: Int,
    val totalBudgetCount: Int,
    val borrowedDebtTotal: Double,
    val debtRatioPct: Int,
    val pillars: List<HealthPillarReport>,
    val recommendationTitle: String,
    val recommendationDesc: String
)

object FinancialHealthHelper {

    fun calculateConsecutiveStreak(trackedDays: Set<Int>): Int {
        if (trackedDays.isEmpty()) return 0
        val sorted = trackedDays.sorted()
        var longest = 1
        var current = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1) {
                current++
                if (current > longest) longest = current
            } else {
                current = 1
            }
        }
        return longest
    }

    fun calculate(
        periodTransactions: List<Transaction>,
        prevPeriodTransactions: List<Transaction>,
        allTransactions: List<Transaction>,
        wallets: List<AccountWallet>,
        assets: List<AssetItem>,
        debts: List<DebtItem>,
        budgets: List<BudgetCategory>,
        categories: List<MainCategory>,
        selectedMonth: Int,
        selectedYear: Int,
        daysInMonth: Int,
        targetSavingsPercent: Int = 20,
        budgetWarningPercent: Int = 80
    ): FinancialHealthReport {
        val currentIncome = periodTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val currentExpense = periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        val netSurplus = currentIncome - currentExpense

        val prevIncome = prevPeriodTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val prevExpense = prevPeriodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        val prevNet = prevIncome - prevExpense

        val hasPrevNetComparison = prevIncome > 0.0 || prevExpense > 0.0
        val prevNetDeltaPct: Double? = if (hasPrevNetComparison && abs(prevNet) > 0.0) {
            ((netSurplus - prevNet) / abs(prevNet)) * 100.0
        } else if (hasPrevNetComparison && prevNet == 0.0 && netSurplus != 0.0) {
            if (netSurplus > 0) 100.0 else -100.0
        } else {
            null
        }

        val savingsRatePct = if (currentIncome > 0.0) {
            ((currentIncome - currentExpense) / currentIncome) * 100.0
        } else {
            0.0
        }

        // Pillar 1: Tingkat Tabungan (0..20)
        val safeTargetPct = targetSavingsPercent.coerceIn(5, 80).toDouble()
        val pillar1Score = when {
            currentIncome <= 0.0 && currentExpense <= 0.0 -> 0
            currentIncome <= 0.0 -> 2
            savingsRatePct >= safeTargetPct * 1.5 -> 20
            savingsRatePct >= safeTargetPct -> 17
            savingsRatePct >= safeTargetPct * 0.5 -> 12
            savingsRatePct > 0.0 -> 8
            else -> 3
        }
        val pillar1Desc = if (currentIncome > 0.0) {
            "${String.format(java.util.Locale.US, "%.1f", savingsRatePct)}% disimpan (Target min ${safeTargetPct.toInt()}%)"
        } else {
            "Belum ada pemasukan tercatat di periode ini"
        }
        val pillar1 = HealthPillarReport(
            name = "Tingkat Tabungan",
            description = pillar1Desc,
            score = pillar1Score,
            statusLevel = if (pillar1Score >= 15) 1 else if (pillar1Score >= 8) 0 else -1
        )

        // Pillar 2: Kontrol Pengeluaran (0..20)
        val expenseChangePct: Double? = if (prevExpense > 0.0) {
            ((currentExpense - prevExpense) / prevExpense) * 100.0
        } else {
            null
        }
        val pillar2Score = when {
            currentExpense <= 0.0 && currentIncome <= 0.0 -> 0
            expenseChangePct != null -> when {
                expenseChangePct <= -10.0 -> 20
                expenseChangePct <= 0.0 -> 17
                expenseChangePct <= 15.0 -> 13
                expenseChangePct <= 35.0 -> 8
                else -> 4
            }
            currentIncome > 0.0 -> when {
                currentExpense <= currentIncome * 0.5 -> 18
                currentExpense <= currentIncome * 0.8 -> 14
                currentExpense <= currentIncome -> 10
                else -> 5
            }
            else -> 8
        }
        val pillar2Desc = when {
            expenseChangePct != null && expenseChangePct <= 0.0 ->
                "Hemat ${String.format(java.util.Locale.US, "%.1f", abs(expenseChangePct))}% dibanding periode lalu"
            expenseChangePct != null ->
                "Naik +${String.format(java.util.Locale.US, "%.1f", expenseChangePct)}% dibanding periode lalu"
            currentIncome > 0.0 ->
                "Rasio belanja ${(if (currentIncome > 0) (currentExpense / currentIncome * 100).roundToInt() else 0)}% dari pemasukan"
            else -> "Belum ada pembanding pengeluaran"
        }
        val pillar2 = HealthPillarReport(
            name = "Kontrol Pengeluaran",
            description = pillar2Desc,
            score = pillar2Score,
            statusLevel = if (pillar2Score >= 15) 1 else if (pillar2Score >= 9) 0 else -1
        )

        // Pillar 3: Konsistensi Pelacakan (0..20)
        val trackedDays = periodTransactions.map { it.dayOfMonth }.toSet()
        val safeDaysInMonth = daysInMonth.coerceAtLeast(28)
        val longestStreak = calculateConsecutiveStreak(trackedDays)
        val trackedRatio = trackedDays.size.toFloat() / safeDaysInMonth.toFloat()
        val pillar3Score = when {
            trackedDays.isEmpty() -> 0
            longestStreak >= 14 || trackedRatio >= 0.65f -> 20
            longestStreak >= 7 || trackedRatio >= 0.40f -> 15
            longestStreak >= 3 || trackedDays.size >= 5 -> 10
            else -> 5
        }
        val pillar3Desc = "${trackedDays.size} dari $safeDaysInMonth hari aktif (Streak: $longestStreak hr)"
        val pillar3 = HealthPillarReport(
            name = "Konsistensi Pelacakan",
            description = pillar3Desc,
            score = pillar3Score,
            statusLevel = if (pillar3Score >= 15) 1 else if (pillar3Score >= 8) 0 else -1
        )

        // Pillar 4: Diversifikasi Pendapatan (0..20)
        val incomeCategories = periodTransactions
            .filter { it.type == TransactionType.INCOME }
            .map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val incomeSourceCount = incomeCategories.size
        val activePositiveWallets = wallets.count { it.balance > 0.0 }
        val pillar4Score = when {
            incomeSourceCount >= 3 -> 20
            incomeSourceCount == 2 -> 16
            incomeSourceCount == 1 && activePositiveWallets >= 2 -> 13
            incomeSourceCount == 1 -> 10
            activePositiveWallets >= 1 -> 5
            else -> 0
        }
        val pillar4Desc = if (incomeSourceCount > 0) {
            "$incomeSourceCount sumber pemasukan · $activePositiveWallets dompet aktif"
        } else {
            "Belum ada sumber pemasukan pada periode ini"
        }
        val pillar4 = HealthPillarReport(
            name = "Diversifikasi Pendapatan",
            description = pillar4Desc,
            score = pillar4Score,
            statusLevel = if (pillar4Score >= 15) 1 else if (pillar4Score >= 8) 0 else -1
        )

        // Pillar 5: Progres Anggaran & Rasio Utang (0..20)
        val totalWalletBalance = wallets.filterNot { it.excludeFromNetWorth }.sumOf { it.balance }
        val totalAssetVal = assets.sumOf { it.realValue }
        val totalWealthBase = (totalWalletBalance + totalAssetVal).coerceAtLeast(0.0)
        val borrowedDebtTotal = debts
            .filter { it.type == DebtType.BORROWED }
            .sumOf { it.remainingAmount }
        val debtRatioPct = if (borrowedDebtTotal <= 0.0) {
            0
        } else if (totalWealthBase > 0.0) {
            ((borrowedDebtTotal / totalWealthBase) * 100.0).roundToInt().coerceAtLeast(0)
        } else {
            100
        }

        val computedBudgets = budgets.map { b ->
            BudgetMathHelper.computeBudgetItem(
                budget = b,
                allTransactions = allTransactions,
                wallets = wallets,
                allCategories = categories,
                selectedMonth = selectedMonth,
                selectedYear = selectedYear
            )
        }
        val safeBudgetCount = computedBudgets.count { it.rawPercentInt < budgetWarningPercent }
        val totalBudgetCount = computedBudgets.size
        val pillar5Score = if (totalBudgetCount > 0) {
            val budgetRatioScore = ((safeBudgetCount.toFloat() / totalBudgetCount.toFloat()) * 14f).roundToInt()
            val debtBonus = when {
                debtRatioPct <= 20 -> 6
                debtRatioPct <= 45 -> 4
                debtRatioPct <= 70 -> 2
                else -> 0
            }
            (budgetRatioScore + debtBonus).coerceIn(0, 20)
        } else {
            when {
                borrowedDebtTotal <= 0.0 && currentIncome > 0.0 -> 16
                debtRatioPct <= 25 -> 14
                debtRatioPct <= 50 -> 10
                else -> 5
            }
        }
        val pillar5Desc = if (totalBudgetCount > 0) {
            "$safeBudgetCount dari $totalBudgetCount anggaran aman · Beban utang $debtRatioPct%"
        } else {
            "Belum ada anggaran aktif · Beban utang $debtRatioPct%"
        }
        val pillar5 = HealthPillarReport(
            name = "Progres Anggaran",
            description = pillar5Desc,
            score = pillar5Score,
            statusLevel = if (pillar5Score >= 15) 1 else if (pillar5Score >= 9) 0 else -1
        )

        val pillars = listOf(pillar1, pillar2, pillar3, pillar4, pillar5)
        val totalScore = pillars.sumOf { it.score }.coerceIn(0, 100)

        val (statusLabel, statusLevel) = when {
            totalScore >= 80 -> "Sangat Baik" to 1
            totalScore >= 60 -> "Baik" to 1
            totalScore >= 40 -> "Cukup" to 0
            else -> "Perlu Perhatian" to -1
        }

        val weakest = pillars.minByOrNull { it.score }
        val (recTitle, recDesc) = when (weakest?.name) {
            "Tingkat Tabungan" -> "Tingkatkan Rasio Tabungan" to
                "Sisihkan minimal ${safeTargetPct.toInt()}% dari pemasukan secara otomatis di awal bulan agar dana cadangan cepat bertumbuh."
            "Kontrol Pengeluaran" -> "Kendalikan Pengeluaran Harian" to
                "Evaluasi kategori pengeluaran terbesar Anda dan tetapkan batas harian untuk menjaga arus kas tetap surplus."
            "Konsistensi Pelacakan" -> "Bangun Kebiasaan Catat Harian" to
                "Mencatat transaksi secara rutin 7 hari berturut-turut akan meningkatkan skor pelacakan dan akurasi anggaran Anda."
            "Diversifikasi Pendapatan" -> "Perluas Sumber Pemasukan" to
                "Pertimbangkan menambah pos pendapatan tambahan seperti hasil investasi, dividen, atau proyek sampingan."
            else -> if (totalBudgetCount == 0) {
                "Buat Anggaran Kategori" to
                    "Tambahkan anggaran untuk kategori belanja utama Anda agar pengeluaran lebih terukur dan terkendali."
            } else {
                "Jaga Batas Anggaran & Utang" to
                    "Pastikan penggunaan tiap anggaran berada di bawah $budgetWarningPercent% dan prioritaskan pelunasan cicilan berbunga tinggi."
            }
        }

        val emergencyMonths = if (currentExpense > 0.0) {
            (totalWalletBalance / currentExpense).coerceAtLeast(0.0)
        } else if (totalWalletBalance > 0.0) {
            12.0
        } else {
            0.0
        }

        return FinancialHealthReport(
            score = totalScore,
            statusLabel = statusLabel,
            statusLevel = statusLevel,
            savingsRatePct = savingsRatePct,
            netSurplus = netSurplus,
            prevNetDeltaPct = prevNetDeltaPct,
            hasPrevNetComparison = hasPrevNetComparison,
            expenseChangePct = expenseChangePct,
            trackedDaysCount = trackedDays.size,
            daysInMonth = safeDaysInMonth,
            longestStreakDays = longestStreak,
            incomeSourceCount = incomeSourceCount,
            safeBudgetCount = safeBudgetCount,
            totalBudgetCount = totalBudgetCount,
            borrowedDebtTotal = borrowedDebtTotal,
            debtRatioPct = debtRatioPct,
            pillars = pillars,
            recommendationTitle = recTitle,
            recommendationDesc = recDesc
        )
    }
}
