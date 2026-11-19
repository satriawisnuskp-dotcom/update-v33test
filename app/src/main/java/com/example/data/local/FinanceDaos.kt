package com.example.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC")
    fun observeActive(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllRaw(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Upsert
    suspend fun upsert(entity: TransactionEntity)

    @Upsert
    suspend fun upsertAll(entities: List<TransactionEntity>)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAllRaw(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AccountEntity?

    @Upsert
    suspend fun upsert(entity: AccountEntity)

    @Upsert
    suspend fun upsertAll(entities: List<AccountEntity>)

    @Query("UPDATE accounts SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM accounts")
    suspend fun clearAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllRaw(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CategoryEntity?

    @Upsert
    suspend fun upsert(entity: CategoryEntity)

    @Upsert
    suspend fun upsertAll(entities: List<CategoryEntity>)

    @Query("UPDATE categories SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE isDeleted = 0 ORDER BY sortOrder ASC, personName ASC")
    fun observeActive(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts")
    suspend fun getAllRaw(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DebtEntity?

    @Upsert
    suspend fun upsert(entity: DebtEntity)

    @Upsert
    suspend fun upsertAll(entities: List<DebtEntity>)

    @Query("UPDATE debts SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM debts")
    suspend fun clearAll()
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets WHERE isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActive(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets")
    suspend fun getAllRaw(): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AssetEntity?

    @Upsert
    suspend fun upsert(entity: AssetEntity)

    @Upsert
    suspend fun upsertAll(entities: List<AssetEntity>)

    @Query("UPDATE assets SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM assets")
    suspend fun clearAll()
}

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels WHERE isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActive(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels")
    suspend fun getAllRaw(): List<LabelEntity>

    @Query("SELECT * FROM labels WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LabelEntity?

    @Upsert
    suspend fun upsert(entity: LabelEntity)

    @Upsert
    suspend fun upsertAll(entities: List<LabelEntity>)

    @Query("UPDATE labels SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM labels")
    suspend fun clearAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE isDeleted = 0 ORDER BY sortOrder ASC, category ASC")
    fun observeActive(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllRaw(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BudgetEntity?

    @Upsert
    suspend fun upsert(entity: BudgetEntity)

    @Upsert
    suspend fun upsertAll(entities: List<BudgetEntity>)

    @Query("UPDATE budgets SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM budgets")
    suspend fun clearAll()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_transactions WHERE isDeleted = 0 ORDER BY sortOrder ASC, dayOfMonth ASC")
    fun observeActive(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAllRaw(): List<RecurringEntity>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurringEntity?

    @Upsert
    suspend fun upsert(entity: RecurringEntity)

    @Upsert
    suspend fun upsertAll(entities: List<RecurringEntity>)

    @Query("UPDATE recurring_transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM recurring_transactions")
    suspend fun clearAll()
}

@Dao
interface FinancialPlanDao {
    @Query("SELECT * FROM financial_plans WHERE isDeleted = 0 ORDER BY year ASC, monthIndex ASC, sortOrder ASC")
    fun observeActive(): Flow<List<FinancialPlanEntity>>

    @Query("SELECT * FROM financial_plans")
    suspend fun getAllRaw(): List<FinancialPlanEntity>

    @Query("SELECT * FROM financial_plans WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FinancialPlanEntity?

    @Upsert
    suspend fun upsert(entity: FinancialPlanEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FinancialPlanEntity>)

    @Query("UPDATE financial_plans SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM financial_plans")
    suspend fun clearAll()
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE isDeleted = 0 ORDER BY sortOrder ASC, year DESC, monthIndex DESC")
    fun observeActive(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events")
    suspend fun getAllRaw(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EventEntity?

    @Upsert
    suspend fun upsert(entity: EventEntity)

    @Upsert
    suspend fun upsertAll(entities: List<EventEntity>)

    @Query("UPDATE events SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM events")
    suspend fun clearAll()
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeActive(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllRaw(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SavingsGoalEntity?

    @Upsert
    suspend fun upsert(entity: SavingsGoalEntity)

    @Upsert
    suspend fun upsertAll(entities: List<SavingsGoalEntity>)

    @Query("UPDATE savings_goals SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM savings_goals")
    suspend fun clearAll()
}

@Dao
interface TimelineScenarioDao {
    @Query("SELECT * FROM timeline_scenarios WHERE isDeleted = 0 ORDER BY sortOrder ASC, date ASC")
    fun observeActive(): Flow<List<TimelineScenarioEntity>>

    @Query("SELECT * FROM timeline_scenarios")
    suspend fun getAllRaw(): List<TimelineScenarioEntity>

    @Query("SELECT * FROM timeline_scenarios WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TimelineScenarioEntity?

    @Upsert
    suspend fun upsert(entity: TimelineScenarioEntity)

    @Upsert
    suspend fun upsertAll(entities: List<TimelineScenarioEntity>)

    @Query("UPDATE timeline_scenarios SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM timeline_scenarios")
    suspend fun clearAll()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 'main' LIMIT 1")
    fun observeMain(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 'main' LIMIT 1")
    suspend fun getMain(): AppSettingsEntity?

    @Upsert
    suspend fun upsert(entity: AppSettingsEntity)

    @Query("DELETE FROM app_settings")
    suspend fun clearAll()
}

@Dao
interface CustomLogoDao {
    @Query("SELECT * FROM custom_logos WHERE isDeleted = 0 ORDER BY fileName ASC")
    fun observeActive(): Flow<List<CustomLogoEntity>>

    @Query("SELECT * FROM custom_logos")
    suspend fun getAllRaw(): List<CustomLogoEntity>

    @Query("SELECT * FROM custom_logos WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CustomLogoEntity?

    @Upsert
    suspend fun upsert(entity: CustomLogoEntity)

    @Upsert
    suspend fun upsertAll(entities: List<CustomLogoEntity>)

    @Query("UPDATE custom_logos SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM custom_logos")
    suspend fun clearAll()
}
