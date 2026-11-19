package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AccountWallet
import com.example.data.model.AssetItem
import com.example.data.model.AssetRecord
import com.example.data.model.BudgetCategory
import com.example.data.model.DebtItem
import com.example.data.model.DebtRecord
import com.example.data.model.DebtType
import com.example.data.model.EventItem
import com.example.data.model.EventPlannedExpense
import com.example.data.model.FinancialPlan
import com.example.data.model.MainCategory
import com.example.data.model.PlanSectionType
import com.example.data.model.PlannedItem
import com.example.data.model.RecurringTransaction
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsRecord
import com.example.data.model.SubcategoryItem
import com.example.data.model.TimelineScenarioItem
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.UpcomingScheduleItem
import com.example.data.model.UserSettings
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val amount: Double,
    val categoryId: String,
    val subcategory: String = "",
    val accountId: String,
    val accountName: String,
    val toAccountId: String = "",
    val toAccountName: String = "",
    val note: String = "",
    val label: String = "",
    val recurrence: String = "Tidak pernah",
    val date: Long, // epoch millis
    val dayOfMonth: Int,
    val monthIndex: Int,
    val year: Int,
    val dayOfWeekName: String,
    val time: String = "12:00",
    val transferFee: Double = 0.0,
    val targetAssetId: String = "",
    val targetAssetName: String = "",
    val targetDebtId: String = "",
    val targetDebtName: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String = "Bank · IDR",
    val initialBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val icon: String = "Bank",
    val color: Long = 0xFF3B82F6L,
    val currency: String = "IDR",
    val lastCategory: String = "Other",
    val lastAmount: Double = 0.0,
    val lastActivityTime: String = "Baru saja",
    val excludeFromNetWorth: Boolean = false,
    val excludeFromBudget: Boolean = false,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String = "EXPENSE",
    val icon: String = "",
    val color: Long = 0xFF3B82F6L,
    val isVisible: Boolean = true,
    val subcategoriesJson: String = "[]",
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val personName: String,
    val amount: Double,
    val paidAmount: Double = 0.0,
    val type: String = "BORROWED",
    val dueDate: String,
    val isPaid: Boolean = false,
    val startDate: String = "",
    val startTime: String = "12:00",
    val monthlyPayment: Double = 0.0,
    val interestRate: Double = 0.0,
    val extraFees: Double = 0.0,
    val categoryIcon: String = "CreditCard",
    val institutionLogo: String = "",
    val tenureMonths: Int = 0,
    val linkedWalletName: String = "",
    val recordsJson: String = "[]",
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val groupName: String = "Investasi",
    val assetType: String = "Bank Account",
    val value: Double = 0.0,
    val platform: String = "Bibit",
    val performancePercent: Double = 0.0,
    val currency: String = "IDR",
    val createdAtDate: String = "",
    val createdAtTime: String = "",
    val valuationMethod: String = "Manual",
    val recordsJson: String = "[]",
    val iconUri: String = "",
    val tickerSymbol: String = "",
    val lotCount: Double = 0.0,
    val averagePrice: Double = 0.0,
    val currentPrice: Double = 0.0,
    val dailyPriceChange: Double = 0.0,
    val dailyPriceChangePercent: Double = 0.0,
    val lastPriceUpdate: Long = 0L,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "labels")
data class LabelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val category: String,
    val allocated: Double,
    val spent: Double = 0.0,
    val name: String = "",
    val categoriesJson: String = "[]",
    val accountNamesJson: String = "[]",
    val frequency: String = "Bulanan",
    val customStartDate: String = "",
    val customEndDate: String = "",
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "recurring_transactions")
data class RecurringEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: String = "EXPENSE",
    val category: String,
    val subcategory: String = "",
    val accountName: String,
    val toAccountName: String = "",
    val dayOfMonth: Int = 1,
    val frequency: String = "Setiap bulan",
    val startDate: String = "",
    val endDate: String = "",
    val note: String = "",
    val label: String = "",
    val isActive: Boolean = true,
    val autoDeduct: Boolean = true,
    val lastProcessedMonthYear: String = "",
    val endCondition: String = "NEVER",
    val endAfterCount: Int = 0,
    val logoName: String = "",
    val skippedCount: Int = 0,
    val snoozedUntilDate: String = "",
    val lastSkippedDateLabel: String = "",
    val reminderEnabled: Boolean = false,
    val reminderOptionsJson: String = "[]",
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "financial_plans")
data class FinancialPlanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "Pembagian Gaji",
    val monthIndex: Int = 10,
    val year: Int = 2026,
    val itemsJson: String = "[]",
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
    val plannedExpensesJson: String = "[]",
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
    val recordsJson: String = "[]",
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "timeline_scenarios")
data class TimelineScenarioEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val isIncome: Boolean,
    val note: String,
    val date: String,
    val recurrence: String = "Hanya sekali",
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: String = "main",
    val settingsJson: String = "{}",
    val activePlanId: String = "plan_default",
    val planTitle: String = "Pembagian Gaji",
    val selectedWalletFilter: String = "Semua",
    val upcomingItemsJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_logos")
data class CustomLogoEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val base64Data: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

object RoomEntityMappers {

    fun sanitizeFirestoreId(rawId: String): String {
        val cleaned = rawId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return cleaned.ifBlank { UUID.randomUUID().toString() }.take(128)
    }

    fun labelDocId(labelName: String): String {
        val normalized = labelName.trim().lowercase().replace(Regex("[^a-z0-9_\\-]"), "_")
        return "lbl_${normalized.ifBlank { UUID.randomUUID().toString() }}".take(128)
    }

    fun customLogoDocId(fileName: String): String {
        return "logo_${sanitizeFirestoreId(fileName)}".take(128)
    }

    fun computeTransactionEpoch(day: Int, monthIndex: Int, year: Int, timeStr: String): Long {
        val parts = timeStr.replace(".", ":").split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 12
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year.coerceIn(1970, 2100))
            set(Calendar.MONTH, (monthIndex - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun serializeStringList(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    fun parseStringList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i -> arr.optString(i, "") }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun Transaction.toEntity(
        accountIdResolved: String = accountName,
        toAccountIdResolved: String = toAccountName,
        updatedAtOverride: Long = updatedAt,
        isDeletedOverride: Boolean = isDeleted
    ): TransactionEntity = TransactionEntity(
        id = sanitizeFirestoreId(id),
        title = title,
        type = type.name,
        amount = amount,
        categoryId = category,
        subcategory = subcategory,
        accountId = sanitizeFirestoreId(accountIdResolved.ifBlank { accountName.ifBlank { "default" } }),
        accountName = accountName,
        toAccountId = if (toAccountIdResolved.isBlank()) "" else sanitizeFirestoreId(toAccountIdResolved),
        toAccountName = toAccountName,
        note = note,
        label = label,
        recurrence = recurrence,
        date = computeTransactionEpoch(dayOfMonth, monthIndex, year, time),
        dayOfMonth = dayOfMonth,
        monthIndex = monthIndex,
        year = year,
        dayOfWeekName = dayOfWeekName,
        time = time,
        transferFee = transferFee,
        targetAssetId = targetAssetId,
        targetAssetName = targetAssetName,
        targetDebtId = targetDebtId,
        targetDebtName = targetDebtName,
        updatedAt = updatedAtOverride,
        isDeleted = isDeletedOverride
    )

    fun TransactionEntity.toDomain(): Transaction = Transaction(
        id = id,
        title = title,
        category = categoryId,
        subcategory = subcategory,
        amount = amount,
        type = enumValues<TransactionType>().firstOrNull { it.name == type } ?: TransactionType.EXPENSE,
        accountName = accountName.ifBlank { accountId },
        dayOfMonth = dayOfMonth,
        monthIndex = monthIndex,
        year = year,
        dayOfWeekName = dayOfWeekName,
        note = note,
        label = label,
        recurrence = recurrence,
        time = time,
        toAccountName = toAccountName,
        transferFee = transferFee,
        targetAssetId = targetAssetId,
        targetAssetName = targetAssetName,
        targetDebtId = targetDebtId,
        targetDebtName = targetDebtName,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    fun AccountWallet.toEntity(
        sortOrder: Int = 0,
        initialBalanceOverride: Double = balance,
        updatedAtOverride: Long = updatedAt,
        isDeletedOverride: Boolean = isDeleted
    ): AccountEntity = AccountEntity(
        id = sanitizeFirestoreId(id),
        name = name,
        type = bankType,
        initialBalance = initialBalanceOverride,
        currentBalance = balance,
        icon = iconName,
        color = 0xFF3B82F6L,
        currency = currency,
        lastCategory = lastCategory,
        lastAmount = lastAmount,
        lastActivityTime = lastActivityTime,
        excludeFromNetWorth = excludeFromNetWorth,
        excludeFromBudget = excludeFromBudget,
        sortOrder = sortOrder,
        updatedAt = updatedAtOverride,
        isDeleted = isDeletedOverride
    )

    fun AccountEntity.toDomain(): AccountWallet = AccountWallet(
        id = id,
        name = name,
        bankType = type,
        balance = currentBalance,
        lastCategory = lastCategory,
        lastAmount = lastAmount,
        lastActivityTime = lastActivityTime,
        currency = currency,
        iconName = icon,
        excludeFromNetWorth = excludeFromNetWorth,
        excludeFromBudget = excludeFromBudget,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    fun serializeSubcategories(list: List<SubcategoryItem>): String {
        val arr = JSONArray()
        list.forEach { sub ->
            arr.put(
                JSONObject().apply {
                    put("id", sub.id)
                    put("name", sub.name)
                    put("iconName", sub.iconName)
                    put("isVisible", sub.isVisible)
                }
            )
        }
        return arr.toString()
    }

    fun parseSubcategories(json: String): List<SubcategoryItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                SubcategoryItem(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    name = o.optString("name", ""),
                    iconName = o.optString("iconName", ""),
                    isVisible = o.optBoolean("isVisible", true)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun MainCategory.toEntity(
        sortOrder: Int = 0,
        updatedAtOverride: Long = updatedAt,
        isDeletedOverride: Boolean = isDeleted
    ): CategoryEntity = CategoryEntity(
        id = sanitizeFirestoreId(id),
        name = name,
        type = type.name,
        icon = iconName,
        color = 0xFF3B82F6L,
        isVisible = isVisible,
        subcategoriesJson = serializeSubcategories(subcategories),
        sortOrder = sortOrder,
        updatedAt = updatedAtOverride,
        isDeleted = isDeletedOverride
    )

    fun CategoryEntity.toDomain(): MainCategory = MainCategory(
        id = id,
        name = name,
        type = enumValues<TransactionType>().firstOrNull { it.name == type } ?: TransactionType.EXPENSE,
        iconName = icon,
        isVisible = isVisible,
        subcategories = parseSubcategories(subcategoriesJson),
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    fun serializeDebtRecords(list: List<DebtRecord>): String {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("debtId", r.debtId)
                    put("type", r.type)
                    put("amount", r.amount)
                    put("date", r.date)
                    put("time", r.time)
                    put("sourceWalletName", r.sourceWalletName)
                    put("note", r.note)
                }
            )
        }
        return arr.toString()
    }

    fun parseDebtRecords(json: String): List<DebtRecord> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                DebtRecord(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    debtId = o.optString("debtId", ""),
                    type = o.optString("type", "Pembayaran"),
                    amount = o.optDouble("amount", 0.0),
                    date = o.optString("date", ""),
                    time = o.optString("time", ""),
                    sourceWalletName = o.optString("sourceWalletName", ""),
                    note = o.optString("note", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun DebtItem.toEntity(
        sortOrder: Int = 0,
        updatedAtOverride: Long = updatedAt,
        isDeletedOverride: Boolean = isDeleted
    ): DebtEntity = DebtEntity(
        id = sanitizeFirestoreId(id),
        personName = name,
        amount = totalAmount,
        paidAmount = paidAmount,
        type = type.name,
        dueDate = dueDate,
        isPaid = totalAmount > 0 && paidAmount >= totalAmount,
        startDate = startDate,
        startTime = startTime,
        monthlyPayment = monthlyPayment,
        interestRate = interestRate,
        extraFees = extraFees,
        categoryIcon = categoryIcon,
        institutionLogo = institutionLogo,
        tenureMonths = tenureMonths,
        linkedWalletName = linkedWalletName,
        recordsJson = serializeDebtRecords(records),
        sortOrder = sortOrder,
        updatedAt = updatedAtOverride,
        isDeleted = isDeletedOverride
    )

    fun DebtEntity.toDomain(): DebtItem = DebtItem(
        id = id,
        name = personName,
        type = enumValues<DebtType>().firstOrNull { it.name == type } ?: DebtType.BORROWED,
        totalAmount = amount,
        paidAmount = paidAmount,
        dueDate = dueDate,
        startDate = startDate,
        startTime = startTime,
        monthlyPayment = monthlyPayment,
        interestRate = interestRate,
        extraFees = extraFees,
        categoryIcon = categoryIcon,
        institutionLogo = institutionLogo,
        tenureMonths = tenureMonths,
        linkedWalletName = linkedWalletName,
        records = parseDebtRecords(recordsJson),
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    fun serializeAssetRecords(list: List<AssetRecord>): String {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("assetId", r.assetId)
                    put("tag", r.tag)
                    put("isIncrease", r.isIncrease)
                    put("amount", r.amount)
                    put("sourceWalletName", r.sourceWalletName)
                    put("date", r.date)
                    put("time", r.time)
                    put("note", r.note)
                    put("lotCount", r.lotCount)
                    put("pricePerUnit", r.pricePerUnit)
                }
            )
        }
        return arr.toString()
    }

    fun parseAssetRecords(json: String): List<AssetRecord> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                AssetRecord(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    assetId = o.optString("assetId", ""),
                    tag = o.optString("tag", "Buy"),
                    isIncrease = o.optBoolean("isIncrease", true),
                    amount = o.optDouble("amount", 0.0),
                    sourceWalletName = o.optString("sourceWalletName", ""),
                    date = o.optString("date", ""),
                    time = o.optString("time", ""),
                    note = o.optString("note", ""),
                    lotCount = o.optDouble("lotCount", 0.0),
                    pricePerUnit = o.optDouble("pricePerUnit", 0.0)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun AssetItem.toEntity(
        sortOrder: Int = 0,
        updatedAtOverride: Long = updatedAt,
        isDeletedOverride: Boolean = isDeleted
    ): AssetEntity = AssetEntity(
        id = sanitizeFirestoreId(id),
        name = name,
        groupName = group,
        assetType = assetType,
        value = value,
        platform = platform,
        performancePercent = performancePercent,
        currency = currency,
        createdAtDate = createdAtDate,
        createdAtTime = createdAtTime,
        valuationMethod = valuationMethod,
        recordsJson = serializeAssetRecords(records),
        iconUri = iconUri,
        tickerSymbol = tickerSymbol,
        lotCount = lotCount,
        averagePrice = averagePrice,
        currentPrice = currentPrice,
        dailyPriceChange = dailyPriceChange,
        dailyPriceChangePercent = dailyPriceChangePercent,
        lastPriceUpdate = lastPriceUpdate,
        sortOrder = sortOrder,
        updatedAt = updatedAtOverride,
        isDeleted = isDeletedOverride
    )

    fun AssetEntity.toDomain(): AssetItem = AssetItem(
        id = id,
        name = name,
        group = groupName,
        assetType = assetType,
        value = value,
        platform = platform,
        performancePercent = performancePercent,
        currency = currency,
        createdAtDate = createdAtDate,
        createdAtTime = createdAtTime,
        valuationMethod = valuationMethod,
        records = parseAssetRecords(recordsJson),
        iconUri = iconUri,
        tickerSymbol = tickerSymbol,
        lotCount = lotCount,
        averagePrice = averagePrice,
        currentPrice = currentPrice,
        dailyPriceChange = dailyPriceChange,
        dailyPriceChangePercent = dailyPriceChangePercent,
        lastPriceUpdate = lastPriceUpdate,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    // ==================== BUDGETS ====================

    fun BudgetCategory.toEntity(
        sortOrder: Int = 0,
        updatedAtOverride: Long = updatedAt,
        isDeletedOverride: Boolean = isDeleted
    ): BudgetEntity = BudgetEntity(
        id = sanitizeFirestoreId(id),
        category = category,
        allocated = allocated,
        spent = spent,
        name = name,
        categoriesJson = serializeStringList(categories),
        accountNamesJson = serializeStringList(accountNames),
        frequency = frequency,
        customStartDate = customStartDate,
        customEndDate = customEndDate,
        sortOrder = sortOrder,
        updatedAt = updatedAtOverride,
        isDeleted = isDeletedOverride
    )

    fun BudgetEntity.toDomain(): BudgetCategory = BudgetCategory(
        id = id,
        category = category,
        allocated = allocated,
        spent = spent,
        name = name,
        categories = parseStringList(categoriesJson),
        accountNames = parseStringList(accountNamesJson),
        frequency = frequency,
        customStartDate = customStartDate,
        customEndDate = customEndDate,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    // ==================== RECURRING TRANSACTIONS ====================

    fun RecurringTransaction.toEntity(
        sortOrder: Int = 0,
        updatedAtOverride: Long = updatedAt,
        isDeletedOverride: Boolean = isDeleted
    ): RecurringEntity = RecurringEntity(
        id = sanitizeFirestoreId(id),
        title = title,
        amount = amount,
        type = type.name,
        category = category,
        subcategory = subcategory,
        accountName = accountName,
        toAccountName = toAccountName,
        dayOfMonth = dayOfMonth,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        note = note,
        label = label,
        isActive = isActive,
        autoDeduct = autoDeduct,
        lastProcessedMonthYear = lastProcessedMonthYear,
        endCondition = endCondition,
        endAfterCount = endAfterCount,
        logoName = logoName,
        skippedCount = skippedCount,
        snoozedUntilDate = snoozedUntilDate,
        lastSkippedDateLabel = lastSkippedDateLabel,
        reminderEnabled = reminderEnabled,
        reminderOptionsJson = serializeStringList(reminderOptions),
        sortOrder = sortOrder,
        updatedAt = updatedAtOverride,
        isDeleted = isDeletedOverride
    )

    fun RecurringEntity.toDomain(): RecurringTransaction = RecurringTransaction(
        id = id,
        title = title,
        amount = amount,
        type = enumValues<TransactionType>().firstOrNull { it.name == type } ?: TransactionType.EXPENSE,
        category = category,
        subcategory = subcategory,
        accountName = accountName,
        toAccountName = toAccountName,
        dayOfMonth = dayOfMonth,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        note = note,
        label = label,
        isActive = isActive,
        autoDeduct = autoDeduct,
        lastProcessedMonthYear = lastProcessedMonthYear,
        endCondition = endCondition,
        endAfterCount = endAfterCount,
        logoName = logoName,
        skippedCount = skippedCount,
        snoozedUntilDate = snoozedUntilDate,
        lastSkippedDateLabel = lastSkippedDateLabel,
        reminderEnabled = reminderEnabled,
        reminderOptions = parseStringList(reminderOptionsJson),
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    // ==================== FINANCIAL PLANS ====================

    fun serializePlannedItems(list: List<PlannedItem>): String {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("category", p.category)
                    put("subcategory", p.subcategory)
                    put("amount", p.amount)
                    put("section", p.section.name)
                }
            )
        }
        return arr.toString()
    }

    fun parsePlannedItems(json: String): List<PlannedItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val sec = enumValues<PlanSectionType>().firstOrNull {
                    it.name == o.optString("section", "EXPENSE")
                } ?: PlanSectionType.EXPENSE
                PlannedItem(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    name = o.optString("name", ""),
                    category = o.optString("category", ""),
                    subcategory = o.optString("subcategory", ""),
                    amount = o.optDouble("amount", 0.0),
                    section = sec
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun FinancialPlan.toEntity(
        sortOrder: Int = 0,
        updatedAtOverride: Long = updatedAt,
        isDeletedOverride: Boolean = isDeleted
    ): FinancialPlanEntity = FinancialPlanEntity(
        id = sanitizeFirestoreId(id),
        title = title,
        monthIndex = monthIndex,
        year = year,
        itemsJson = serializePlannedItems(items),
        sortOrder = sortOrder,
        updatedAt = updatedAtOverride,
        isDeleted = isDeletedOverride
    )

    fun FinancialPlanEntity.toDomain(): FinancialPlan = FinancialPlan(
        id = id,
        title = title,
        monthIndex = monthIndex,
        year = year,
        items = parsePlannedItems(itemsJson),
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    // ==================== EVENTS ====================

    fun serializeEventPlannedExpenses(list: List<EventPlannedExpense>): String {
        val arr = JSONArray()
        list.forEach { pe ->
            arr.put(
                JSONObject().apply {
                    put("id", pe.id)
                    put("name", pe.name)
                    put("category", pe.category)
                    put("subcategory", pe.subcategory)
                    put("amount", pe.amount)
                    put("isPaid", pe.isPaid)
                }
            )
        }
        return arr.toString()
    }

    fun parseEventPlannedExpenses(json: String): List<EventPlannedExpense> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                EventPlannedExpense(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    name = o.optString("name", ""),
                    category = o.optString("category", "Other"),
                    subcategory = o.optString("subcategory", ""),
                    amount = o.optDouble("amount", 0.0),
                    isPaid = o.optBoolean("isPaid", false)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun EventItem.toEntity(
        sortOrder: Int = 0,
        updatedAtOverride: Long = updatedAt,
        isDeletedOverride: Boolean = isDeleted
    ): EventEntity = EventEntity(
        id = sanitizeFirestoreId(id),
        name = name,
        eventType = eventType,
        targetBudget = targetBudget,
        monthIndex = monthIndex,
        year = year,
        startDate = startDate,
        endDate = endDate,
        currency = currency,
        iconName = iconName,
        colorHex = colorHex,
        note = note,
        plannedExpensesJson = serializeEventPlannedExpenses(plannedExpenses),
        isCompleted = isCompleted,
        sortOrder = sortOrder,
        updatedAt = updatedAtOverride,
        isDeleted = isDeletedOverride
    )

    fun EventEntity.toDomain(): EventItem = EventItem(
        id = id,
        name = name,
        eventType = eventType,
        targetBudget = targetBudget,
        monthIndex = monthIndex,
        year = year,
        startDate = startDate,
        endDate = endDate,
        currency = currency,
        iconName = iconName,
        colorHex = colorHex,
        note = note,
        plannedExpenses = parseEventPlannedExpenses(plannedExpensesJson),
        isCompleted = isCompleted,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    // ==================== SAVINGS GOALS ====================

    fun serializeSavingsRecords(list: List<SavingsRecord>): String {
        val arr = JSONArray()
        list.forEach { rec ->
            arr.put(
                JSONObject().apply {
                    put("id", rec.id)
                    put("amount", rec.amount)
                    put("isWithdraw", rec.isWithdraw)
                    put("note", rec.note)
                    put("dateStr", rec.dateStr)
                    put("walletName", rec.walletName)
                    put("linkedTransactionId", rec.linkedTransactionId)
                }
            )
        }
        return arr.toString()
    }

    fun parseSavingsRecords(json: String): List<SavingsRecord> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                SavingsRecord(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    amount = o.optDouble("amount", 0.0),
                    isWithdraw = o.optBoolean("isWithdraw", false),
                    note = o.optString("note", ""),
                    dateStr = o.optString("dateStr", ""),
                    walletName = o.optString("walletName", ""),
                    linkedTransactionId = o.optString("linkedTransactionId", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun SavingsGoal.toEntity(
        sortOrder: Int = 0,
        updatedAtOverride: Long = updatedAt,
        isDeletedOverride: Boolean = isDeleted
    ): SavingsGoalEntity = SavingsGoalEntity(
        id = sanitizeFirestoreId(id),
        name = name,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        targetDate = targetDate,
        startDate = startDate,
        currency = currency,
        iconName = iconName,
        note = note,
        colorHex = colorHex,
        isCompleted = isCompleted,
        recordsJson = serializeSavingsRecords(records),
        sortOrder = sortOrder,
        updatedAt = updatedAtOverride,
        isDeleted = isDeletedOverride
    )

    fun SavingsGoalEntity.toDomain(): SavingsGoal = SavingsGoal(
        id = id,
        name = name,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        targetDate = targetDate,
        startDate = startDate,
        currency = currency,
        iconName = iconName,
        note = note,
        colorHex = colorHex,
        isCompleted = isCompleted,
        records = parseSavingsRecords(recordsJson),
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    // ==================== TIMELINE SCENARIOS & UPCOMING ====================

    fun TimelineScenarioItem.toEntity(
        sortOrder: Int = 0,
        updatedAtOverride: Long = updatedAt,
        isDeletedOverride: Boolean = isDeleted
    ): TimelineScenarioEntity = TimelineScenarioEntity(
        id = sanitizeFirestoreId(id),
        amount = amount,
        isIncome = isIncome,
        note = note,
        date = date,
        recurrence = recurrence,
        sortOrder = sortOrder,
        updatedAt = updatedAtOverride,
        isDeleted = isDeletedOverride
    )

    fun TimelineScenarioEntity.toDomain(): TimelineScenarioItem = TimelineScenarioItem(
        id = id,
        amount = amount,
        isIncome = isIncome,
        note = note,
        date = date,
        recurrence = recurrence,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    fun serializeUpcomingItems(list: List<UpcomingScheduleItem>): String {
        val arr = JSONArray()
        list.forEach { up ->
            arr.put(
                JSONObject().apply {
                    put("id", up.id)
                    put("title", up.title)
                    put("accountName", up.accountName)
                    put("amount", up.amount)
                    put("isIncome", up.isIncome)
                    put("dateLabel", up.dateLabel)
                    put("dayLabel", up.dayLabel)
                }
            )
        }
        return arr.toString()
    }

    fun parseUpcomingItems(json: String): List<UpcomingScheduleItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                UpcomingScheduleItem(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    title = o.optString("title", "Jadwal"),
                    accountName = o.optString("accountName", ""),
                    amount = o.optDouble("amount", 0.0),
                    isIncome = o.optBoolean("isIncome", false),
                    dateLabel = o.optString("dateLabel", ""),
                    dayLabel = o.optString("dayLabel", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ==================== FULL USER SETTINGS ====================

    fun serializeUserSettings(s: UserSettings): String {
        return JSONObject().apply {
            put("language", s.language)
            put("isDarkMode", s.isDarkMode)
            put("primaryCurrency", s.primaryCurrency)
            put("monthlyStartDay", s.monthlyStartDay)
            put("textSizePercent", s.textSizePercent)
            put("cashAccountsOnly", s.cashAccountsOnly)
            put("showPlannedInBalance", s.showPlannedInBalance)
            put("saveReminder", s.saveReminder)
            put("billReminder", s.billReminder)
            put("budgetAlert", s.budgetAlert)
            put("passcodeEnabled", s.passcodeEnabled)
            put("appPasscode", s.appPasscode)
            put("themeColorName", s.themeColorName)
            put("numberFormat", s.numberFormat)
            put("dateFormat", s.dateFormat)
            put("defaultHideBalance", s.defaultHideBalance)
            put("dailyReminderTime", s.dailyReminderTime)
            put("dailyReminderEnabled", s.dailyReminderEnabled)
            put("hapticFeedbackEnabled", s.hapticFeedbackEnabled)
            put("startDayOfWeek", s.startDayOfWeek)
            put("budgetWarningPercent", s.budgetWarningPercent)
            put("targetSavingsPercent", s.targetSavingsPercent)
            put("confirmDeleteTransaction", s.confirmDeleteTransaction)
            put("mainMenuOrder", JSONArray(s.mainMenuOrder))
            put("hiddenMenuItems", JSONArray(s.hiddenMenuItems))
            put("budgetSummaryStyle", s.budgetSummaryStyle)
            put("budgetListStyle", s.budgetListStyle)
            put("paydayAutoSaveEnabled", s.paydayAutoSaveEnabled)
            put("paydayAutoSavePercent", s.paydayAutoSavePercent)
            put("statistikCardsOrder", JSONArray(s.statistikCardsOrder))
            put("hiddenStatistikCards", JSONArray(s.hiddenStatistikCards))
            put("trenBerdampinganEnabled", s.trenBerdampinganEnabled)
            put("dashboardWidgetsOrder", JSONArray(s.dashboardWidgetsOrder))
            put("hiddenDashboardWidgets", JSONArray(s.hiddenDashboardWidgets))
            put("hasCompletedOnboarding", s.hasCompletedOnboarding)
        }.toString()
    }

    fun parseUserSettings(json: String): UserSettings {
        val def = UserSettings()
        if (json.isBlank()) return def
        return try {
            val sObj = JSONObject(json)
            fun optStringList(key: String, fallback: List<String>, nonEmptyOnly: Boolean = false): List<String> {
                val arr = sObj.optJSONArray(key) ?: return fallback
                if (nonEmptyOnly && arr.length() == 0) return fallback
                return MutableList(arr.length()) { idx -> arr.optString(idx, "") }.filter { it.isNotBlank() }
            }
            UserSettings(
                language = sObj.optString("language", def.language),
                isDarkMode = sObj.optBoolean("isDarkMode", def.isDarkMode),
                primaryCurrency = sObj.optString("primaryCurrency", def.primaryCurrency),
                monthlyStartDay = sObj.optInt("monthlyStartDay", def.monthlyStartDay),
                textSizePercent = sObj.optInt("textSizePercent", def.textSizePercent),
                cashAccountsOnly = sObj.optBoolean("cashAccountsOnly", def.cashAccountsOnly),
                showPlannedInBalance = sObj.optBoolean("showPlannedInBalance", def.showPlannedInBalance),
                saveReminder = sObj.optBoolean("saveReminder", def.saveReminder),
                billReminder = sObj.optBoolean("billReminder", def.billReminder),
                budgetAlert = sObj.optBoolean("budgetAlert", def.budgetAlert),
                passcodeEnabled = sObj.optBoolean("passcodeEnabled", def.passcodeEnabled),
                appPasscode = sObj.optString("appPasscode", def.appPasscode),
                themeColorName = sObj.optString("themeColorName", def.themeColorName),
                numberFormat = sObj.optString("numberFormat", def.numberFormat),
                dateFormat = sObj.optString("dateFormat", def.dateFormat),
                defaultHideBalance = sObj.optBoolean("defaultHideBalance", def.defaultHideBalance),
                dailyReminderTime = sObj.optString("dailyReminderTime", def.dailyReminderTime),
                dailyReminderEnabled = sObj.optBoolean("dailyReminderEnabled", def.dailyReminderEnabled),
                hapticFeedbackEnabled = sObj.optBoolean("hapticFeedbackEnabled", def.hapticFeedbackEnabled),
                startDayOfWeek = sObj.optString("startDayOfWeek", def.startDayOfWeek),
                budgetWarningPercent = sObj.optInt("budgetWarningPercent", def.budgetWarningPercent),
                targetSavingsPercent = sObj.optInt("targetSavingsPercent", def.targetSavingsPercent),
                confirmDeleteTransaction = sObj.optBoolean("confirmDeleteTransaction", def.confirmDeleteTransaction),
                mainMenuOrder = optStringList("mainMenuOrder", def.mainMenuOrder, nonEmptyOnly = true),
                hiddenMenuItems = optStringList("hiddenMenuItems", def.hiddenMenuItems),
                budgetSummaryStyle = sObj.optString("budgetSummaryStyle", def.budgetSummaryStyle),
                budgetListStyle = sObj.optString("budgetListStyle", def.budgetListStyle),
                paydayAutoSaveEnabled = sObj.optBoolean("paydayAutoSaveEnabled", def.paydayAutoSaveEnabled),
                paydayAutoSavePercent = sObj.optInt("paydayAutoSavePercent", def.paydayAutoSavePercent),
                statistikCardsOrder = optStringList("statistikCardsOrder", def.statistikCardsOrder, nonEmptyOnly = true),
                hiddenStatistikCards = optStringList("hiddenStatistikCards", def.hiddenStatistikCards),
                trenBerdampinganEnabled = sObj.optBoolean("trenBerdampinganEnabled", def.trenBerdampinganEnabled),
                dashboardWidgetsOrder = optStringList("dashboardWidgetsOrder", def.dashboardWidgetsOrder, nonEmptyOnly = true),
                hiddenDashboardWidgets = optStringList("hiddenDashboardWidgets", def.hiddenDashboardWidgets),
                hasCompletedOnboarding = sObj.optBoolean("hasCompletedOnboarding", true)
            )
        } catch (_: Exception) {
            def
        }
    }
}
