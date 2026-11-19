package com.example.data.repository

import android.content.Context
import com.example.data.auth.GoogleAuthManager
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettingsEntity
import com.example.data.local.CustomLogoEntity
import com.example.data.local.LabelEntity
import com.example.data.local.RoomEntityMappers
import com.example.data.local.RoomEntityMappers.toDomain
import com.example.data.local.RoomEntityMappers.toEntity
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
import com.example.data.model.NewPlanStartMode
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
import com.example.data.sync.CloudSyncRepository
import com.example.data.sync.SyncStatus
import com.example.util.ImageUploadHelper
import com.example.util.IndonesianInstitutionHelper
import com.example.util.TradingViewHelper
import com.example.util.TradingViewQuote
import com.example.util.TransactionMathHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class FinanceRepository(private val context: Context? = null) {

    companion object {
        private var sharedContext: Context? = null
        private val testTrackedInstances = CopyOnWriteArrayList<WeakReference<FinanceRepository>>()

        fun setApplicationContext(context: Context) {
            sharedContext = context.applicationContext
        }

        internal fun clearSharedContextForTests() {
            sharedContext = null
            testTrackedInstances.forEach { ref ->
                ref.get()?.cancelScopesForTests()
            }
            testTrackedInstances.clear()
        }
    }

    private fun getActiveContext(): Context? = context ?: sharedContext

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val dbDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val ioScope = CoroutineScope(dbDispatcher + SupervisorJob())
    private val syncObserverScope = CoroutineScope(dbDispatcher + SupervisorJob())

    private val roomInitialized = AtomicBoolean(false)
    private val initialSeedComplete = CompletableDeferred<Unit>()
    private val pendingTransactionWrites = AtomicInteger(0)
    private val pendingWalletWrites = AtomicInteger(0)
    private val pendingCategoryWrites = AtomicInteger(0)
    private val pendingDebtWrites = AtomicInteger(0)
    private val transactionWriteSeq = AtomicLong(0L)
    private val walletWriteSeq = AtomicLong(0L)
    private val categoryWriteSeq = AtomicLong(0L)
    private val debtWriteSeq = AtomicLong(0L)
    @Volatile private var appDatabase: AppDatabase? = null

    init {
        testTrackedInstances.add(WeakReference(this))
    }

    internal fun cancelScopesForTests() {
        syncObserverScope.cancel()
        ioScope.cancel()
    }
    @Volatile private var cloudSyncRepository: CloudSyncRepository? = null
    private val googleAuthManager by lazy { GoogleAuthManager() }

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Guest)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncedAt = MutableStateFlow(0L)
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _wallets = MutableStateFlow<List<AccountWallet>>(emptyList())
    val wallets: StateFlow<List<AccountWallet>> = _wallets.asStateFlow()

    private val _assets = MutableStateFlow<List<AssetItem>>(emptyList())
    val assets: StateFlow<List<AssetItem>> = _assets.asStateFlow()

    private val _debts = MutableStateFlow<List<DebtItem>>(emptyList())
    val debts: StateFlow<List<DebtItem>> = _debts.asStateFlow()

    private val _upcomingItems = MutableStateFlow<List<UpcomingScheduleItem>>(emptyList())
    val upcomingItems: StateFlow<List<UpcomingScheduleItem>> = _upcomingItems.asStateFlow()

    private val _recurringTransactions = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    val recurringTransactions: StateFlow<List<RecurringTransaction>> = _recurringTransactions.asStateFlow()

    private val _budgets = MutableStateFlow<List<BudgetCategory>>(emptyList())
    val budgets: StateFlow<List<BudgetCategory>> = _budgets.asStateFlow()

    private val _categories = MutableStateFlow<List<MainCategory>>(FinanceStorageHelper.getDefaultCategories())
    val categories: StateFlow<List<MainCategory>> = _categories.asStateFlow()

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _labels = MutableStateFlow<List<String>>(
        listOf("Investasi", "Pribadi", "Keluarga", "Tabungan")
    )
    val labels: StateFlow<List<String>> = _labels.asStateFlow()

    private val _planTitle = MutableStateFlow("Pembagian Gaji")
    val planTitle: StateFlow<String> = _planTitle.asStateFlow()

    private val _plannedItems = MutableStateFlow<List<PlannedItem>>(emptyList())
    val plannedItems: StateFlow<List<PlannedItem>> = _plannedItems.asStateFlow()

    private val _financialPlans = MutableStateFlow<List<FinancialPlan>>(emptyList())
    val financialPlans: StateFlow<List<FinancialPlan>> = _financialPlans.asStateFlow()

    private val _activePlanId = MutableStateFlow("")
    val activePlanId: StateFlow<String> = _activePlanId.asStateFlow()

    private val _savingsGoals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val savingsGoals: StateFlow<List<SavingsGoal>> = _savingsGoals.asStateFlow()

    private val _events = MutableStateFlow<List<EventItem>>(emptyList())
    val events: StateFlow<List<EventItem>> = _events.asStateFlow()

    private val _timelineScenarios = MutableStateFlow<List<com.example.data.model.TimelineScenarioItem>>(emptyList())
    val timelineScenarios: StateFlow<List<com.example.data.model.TimelineScenarioItem>> = _timelineScenarios.asStateFlow()

    private val _selectedWalletFilter = MutableStateFlow("Semua")
    val selectedWalletFilter: StateFlow<String> = _selectedWalletFilter.asStateFlow()

    fun selectWalletFilter(walletName: String) {
        _selectedWalletFilter.value = walletName
        persistAppSettingsAndSync()
        saveToDisk()
    }

    init {
        val ctx = getActiveContext()
        val hasExistingDiskData = ctx != null && FinanceStorageHelper.hasSavedData(ctx)
        if (hasExistingDiskData) {
            val loaded = loadFromDisk(ctx!!)
            if (!loaded) {
                loadCleanInitialState()
                saveToDisk(ctx)
            }
        } else {
            loadCleanInitialState()
            if (ctx != null) {
                saveToDisk(ctx)
            }
        }
        syncDebtPaymentsWithTransactions(
            defaultTimestamp = if (hasExistingDiskData) System.currentTimeMillis() else 1L
        )
        if (ctx != null) {
            initializeRoomAndSync(ctx, hasExistingDiskData)
        }
    }

    fun initContext(ctx: Context) {
        sharedContext = ctx.applicationContext
        val hasExistingDiskData = FinanceStorageHelper.hasSavedData(ctx)
        if (!roomInitialized.get()) {
            if (hasExistingDiskData) {
                loadFromDisk(ctx)
            } else {
                saveToDisk(ctx)
            }
            syncDebtPaymentsWithTransactions(
                defaultTimestamp = if (hasExistingDiskData) System.currentTimeMillis() else 1L
            )
        }
        initializeRoomAndSync(ctx, hasExistingDiskData)
    }

    private fun initializeRoomAndSync(ctx: Context, hasExistingDiskData: Boolean = FinanceStorageHelper.hasSavedData(ctx)) {
        if (!roomInitialized.compareAndSet(false, true)) return
        val appCtx = ctx.applicationContext
        val db = AppDatabase.getInstance(appCtx)
        appDatabase = db

        val snapshotTxs = _transactions.value
        val snapshotWallets = _wallets.value
        val snapshotCategories = _categories.value
        val snapshotDebts = _debts.value
        val snapshotAssets = _assets.value
        val snapshotLabels = _labels.value
        val snapshotBudgets = _budgets.value
        val snapshotRecurring = _recurringTransactions.value
        val snapshotFinancialPlans = _financialPlans.value
        val snapshotEvents = _events.value
        val snapshotSavingsGoals = _savingsGoals.value
        val snapshotScenarios = _timelineScenarios.value
        val snapshotSettings = _settings.value
        val snapshotActivePlanId = _activePlanId.value
        val snapshotPlanTitle = _planTitle.value
        val snapshotSelectedWalletFilter = _selectedWalletFilter.value
        val snapshotUpcoming = _upcomingItems.value

        ioScope.launch {
            try {
                val txDao = db.transactionDao()
                val accountDao = db.accountDao()
                val categoryDao = db.categoryDao()
                val debtDao = db.debtDao()
                val assetDao = db.assetDao()
                val labelDao = db.labelDao()
                val budgetDao = db.budgetDao()
                val recurringDao = db.recurringDao()
                val financialPlanDao = db.financialPlanDao()
                val eventDao = db.eventDao()
                val savingsGoalDao = db.savingsGoalDao()
                val scenarioDao = db.timelineScenarioDao()
                val appSettingsDao = db.appSettingsDao()
                val customLogoDao = db.customLogoDao()

                val seedTs = if (hasExistingDiskData) System.currentTimeMillis() else 1L

                if (accountDao.getAllRaw().isEmpty() && snapshotWallets.isNotEmpty()) {
                    accountDao.upsertAll(
                        snapshotWallets.mapIndexed { idx, w ->
                            w.toEntity(sortOrder = idx, updatedAtOverride = seedTs)
                        }
                    )
                }
                if (categoryDao.getAllRaw().isEmpty() && snapshotCategories.isNotEmpty()) {
                    categoryDao.upsertAll(
                        snapshotCategories.mapIndexed { idx, c ->
                            c.toEntity(sortOrder = idx, updatedAtOverride = seedTs)
                        }
                    )
                }
                if (debtDao.getAllRaw().isEmpty() && snapshotDebts.isNotEmpty()) {
                    debtDao.upsertAll(
                        snapshotDebts.mapIndexed { idx, d ->
                            d.toEntity(sortOrder = idx, updatedAtOverride = seedTs)
                        }
                    )
                }
                if (assetDao.getAllRaw().isEmpty() && snapshotAssets.isNotEmpty()) {
                    assetDao.upsertAll(
                        snapshotAssets.mapIndexed { idx, a ->
                            a.toEntity(sortOrder = idx, updatedAtOverride = seedTs)
                        }
                    )
                }
                if (txDao.getAllRaw().isEmpty() && snapshotTxs.isNotEmpty()) {
                    val walletIdByName = snapshotWallets.associate { it.name.lowercase() to it.id }
                    txDao.upsertAll(
                        snapshotTxs.map { tx ->
                            tx.toEntity(
                                accountIdResolved = walletIdByName[tx.accountName.lowercase()] ?: tx.accountName,
                                toAccountIdResolved = walletIdByName[tx.toAccountName.lowercase()] ?: tx.toAccountName,
                                updatedAtOverride = seedTs
                            )
                        }
                    )
                }
                if (labelDao.getAllRaw().isEmpty() && snapshotLabels.isNotEmpty()) {
                    labelDao.upsertAll(
                        snapshotLabels.distinctBy { it.lowercase() }.mapIndexed { idx, lbl ->
                            LabelEntity(
                                id = RoomEntityMappers.labelDocId(lbl),
                                name = lbl,
                                sortOrder = idx,
                                updatedAt = seedTs,
                                isDeleted = false
                            )
                        }
                    )
                }
                if (budgetDao.getAllRaw().isEmpty() && snapshotBudgets.isNotEmpty()) {
                    budgetDao.upsertAll(
                        snapshotBudgets.mapIndexed { idx, b ->
                            b.toEntity(sortOrder = idx, updatedAtOverride = seedTs)
                        }
                    )
                }
                if (recurringDao.getAllRaw().isEmpty() && snapshotRecurring.isNotEmpty()) {
                    recurringDao.upsertAll(
                        snapshotRecurring.mapIndexed { idx, r ->
                            r.toEntity(sortOrder = idx, updatedAtOverride = seedTs)
                        }
                    )
                }
                if (financialPlanDao.getAllRaw().isEmpty() && snapshotFinancialPlans.isNotEmpty()) {
                    financialPlanDao.upsertAll(
                        snapshotFinancialPlans.mapIndexed { idx, p ->
                            p.toEntity(sortOrder = idx, updatedAtOverride = seedTs)
                        }
                    )
                }
                if (eventDao.getAllRaw().isEmpty() && snapshotEvents.isNotEmpty()) {
                    eventDao.upsertAll(
                        snapshotEvents.mapIndexed { idx, ev ->
                            ev.toEntity(sortOrder = idx, updatedAtOverride = seedTs)
                        }
                    )
                }
                if (savingsGoalDao.getAllRaw().isEmpty() && snapshotSavingsGoals.isNotEmpty()) {
                    savingsGoalDao.upsertAll(
                        snapshotSavingsGoals.mapIndexed { idx, g ->
                            g.toEntity(sortOrder = idx, updatedAtOverride = seedTs)
                        }
                    )
                }
                if (scenarioDao.getAllRaw().isEmpty() && snapshotScenarios.isNotEmpty()) {
                    scenarioDao.upsertAll(
                        snapshotScenarios.mapIndexed { idx, s ->
                            s.toEntity(sortOrder = idx, updatedAtOverride = seedTs)
                        }
                    )
                }
                if (appSettingsDao.getMain() == null) {
                    appSettingsDao.upsert(
                        AppSettingsEntity(
                            id = "main",
                            settingsJson = RoomEntityMappers.serializeUserSettings(snapshotSettings),
                            activePlanId = snapshotActivePlanId.ifBlank { "plan_default" },
                            planTitle = snapshotPlanTitle.ifBlank { "Pembagian Gaji" },
                            selectedWalletFilter = snapshotSelectedWalletFilter.ifBlank { "Semua" },
                            upcomingItemsJson = RoomEntityMappers.serializeUpcomingItems(snapshotUpcoming),
                            updatedAt = seedTs
                        )
                    )
                }
                if (customLogoDao.getAllRaw().isEmpty()) {
                    val referencedCustomPaths = buildList {
                        snapshotWallets.forEach { if (ImageUploadHelper.isCustomIconPath(it.iconName)) add(it.iconName) }
                        snapshotAssets.forEach { if (ImageUploadHelper.isCustomIconPath(it.iconUri)) add(it.iconUri) }
                        snapshotDebts.forEach { if (ImageUploadHelper.isCustomIconPath(it.institutionLogo)) add(it.institutionLogo) }
                    }
                    val logosMap = ImageUploadHelper.collectAllCustomLogos(appCtx, referencedCustomPaths)
                    if (logosMap.isNotEmpty()) {
                        customLogoDao.upsertAll(
                            logosMap.map { (fileName, b64) ->
                                CustomLogoEntity(
                                    id = RoomEntityMappers.customLogoDocId(fileName),
                                    fileName = fileName,
                                    base64Data = b64,
                                    updatedAt = seedTs,
                                    isDeleted = false
                                )
                            }
                        )
                    }
                }
            } finally {
                if (!initialSeedComplete.isCompleted) {
                    initialSeedComplete.complete(Unit)
                }
            }
        }

        // Start CloudSyncRepository and observe auth/sync status
        val syncRepo = runCatching {
            if (FirebaseApp.getApps(appCtx).isEmpty()) {
                FirebaseApp.initializeApp(appCtx)
            }
            if (FirebaseApp.getApps(appCtx).isNotEmpty()) {
                CloudSyncRepository(appCtx, db)
            } else {
                null
            }
        }.getOrNull()
        cloudSyncRepository = syncRepo
        if (syncRepo != null) {
            _currentUser.value = syncRepo.currentUser.value
            _syncStatus.value = syncRepo.syncStatus.value
            _lastSyncedAt.value = syncRepo.lastSyncedAt.value

            syncObserverScope.launch {
                syncRepo.syncStatus.collect { _syncStatus.value = it }
            }
            syncObserverScope.launch {
                syncRepo.lastSyncedAt.collect { _lastSyncedAt.value = it }
            }
            syncObserverScope.launch {
                syncRepo.currentUser.collect { _currentUser.value = it }
            }
        }

        // Observe Room DAOs reactively so Firestore real-time updates automatically refresh Compose UI
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.transactionDao().observeActive().collect { _ ->
                if (pendingTransactionWrites.get() == 0) {
                    val seqBefore = transactionWriteSeq.get()
                    val freshEntities = db.transactionDao().getAllRaw().filterNot { it.isDeleted }
                    if (pendingTransactionWrites.get() == 0 && transactionWriteSeq.get() == seqBefore) {
                        val mapped = freshEntities.map { it.toDomain() }
                            .sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator)
                        var applied = false
                        _transactions.update { current ->
                            if (pendingTransactionWrites.get() == 0 && transactionWriteSeq.get() == seqBefore) {
                                applied = true
                                mapped
                            } else {
                                current
                            }
                        }
                        if (applied) {
                            saveToDisk(appCtx)
                        }
                    }
                }
            }
        }
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.accountDao().observeActive().collect { _ ->
                if (pendingWalletWrites.get() == 0) {
                    val seqBefore = walletWriteSeq.get()
                    val freshEntities = db.accountDao().getAllRaw()
                        .filterNot { it.isDeleted }
                        .sortedWith(compareBy<com.example.data.local.AccountEntity> { it.sortOrder }.thenBy { it.name })
                    if (pendingWalletWrites.get() == 0 && walletWriteSeq.get() == seqBefore) {
                        val mapped = freshEntities.map { it.toDomain() }
                        var applied = false
                        _wallets.update { current ->
                            if (pendingWalletWrites.get() == 0 && walletWriteSeq.get() == seqBefore) {
                                applied = true
                                mapped
                            } else {
                                current
                            }
                        }
                        if (applied) {
                            withContext(Dispatchers.Main) {
                                IndonesianInstitutionHelper.syncWithWalletsAndAssets(mapped, _assets.value)
                            }
                            saveToDisk(appCtx)
                        }
                    }
                }
            }
        }
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.categoryDao().observeActive().collect { _ ->
                if (pendingCategoryWrites.get() == 0) {
                    val seqBefore = categoryWriteSeq.get()
                    val freshEntities = db.categoryDao().getAllRaw()
                        .filterNot { it.isDeleted }
                        .sortedWith(compareBy<com.example.data.local.CategoryEntity> { it.sortOrder }.thenBy { it.name })
                    if (pendingCategoryWrites.get() == 0 && categoryWriteSeq.get() == seqBefore) {
                        val mapped = freshEntities.map { it.toDomain() }
                        if (mapped.isNotEmpty()) {
                            var applied = false
                            _categories.update { current ->
                                if (pendingCategoryWrites.get() == 0 && categoryWriteSeq.get() == seqBefore) {
                                    applied = true
                                    mapped
                                } else {
                                    current
                                }
                            }
                            if (applied) {
                                saveToDisk(appCtx)
                            }
                        }
                    }
                }
            }
        }
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.debtDao().observeActive().collect { _ ->
                if (pendingDebtWrites.get() == 0) {
                    val seqBefore = debtWriteSeq.get()
                    val freshEntities = db.debtDao().getAllRaw()
                        .filterNot { it.isDeleted }
                        .sortedWith(compareBy<com.example.data.local.DebtEntity> { it.sortOrder }.thenBy { it.personName })
                    if (pendingDebtWrites.get() == 0 && debtWriteSeq.get() == seqBefore) {
                        val mapped = freshEntities.map { it.toDomain() }
                        var applied = false
                        _debts.update { current ->
                            if (pendingDebtWrites.get() == 0 && debtWriteSeq.get() == seqBefore) {
                                applied = true
                                mapped
                            } else {
                                current
                            }
                        }
                        if (applied) {
                            saveToDisk(appCtx)
                        }
                    }
                }
            }
        }
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.assetDao().observeActive().collect { entities ->
                val mapped = entities.map { it.toDomain() }
                _assets.value = mapped
                withContext(Dispatchers.Main) {
                    IndonesianInstitutionHelper.syncWithWalletsAndAssets(_wallets.value, mapped)
                }
                saveToDisk(appCtx)
            }
        }
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.labelDao().observeActive().collect { entities ->
                _labels.value = entities.map { it.name }.distinct()
                saveToDisk(appCtx)
            }
        }
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.budgetDao().observeActive().collect { entities ->
                _budgets.value = entities.map { it.toDomain() }
                saveToDisk(appCtx)
            }
        }
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.recurringDao().observeActive().collect { entities ->
                _recurringTransactions.value = entities.map { it.toDomain() }
                saveToDisk(appCtx)
            }
        }
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.financialPlanDao().observeActive().collect { entities ->
                val mapped = entities.map { it.toDomain() }
                if (mapped.isNotEmpty()) {
                    _financialPlans.value = mapped
                    val active = mapped.find { it.id == _activePlanId.value } ?: mapped.first()
                    _activePlanId.value = active.id
                    _planTitle.value = active.title
                    _plannedItems.value = active.items
                    saveToDisk(appCtx)
                } else {
                    _financialPlans.value = emptyList()
                    _activePlanId.value = ""
                    _plannedItems.value = emptyList()
                    saveToDisk(appCtx)
                }
            }
        }
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.eventDao().observeActive().collect { entities ->
                _events.value = entities.map { it.toDomain() }
                saveToDisk(appCtx)
            }
        }
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.savingsGoalDao().observeActive().collect { entities ->
                _savingsGoals.value = entities.map { it.toDomain() }
                saveToDisk(appCtx)
            }
        }
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.timelineScenarioDao().observeActive().collect { entities ->
                _timelineScenarios.value = entities.map { it.toDomain() }
                saveToDisk(appCtx)
            }
        }
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.appSettingsDao().observeMain().collect { entity ->
                if (entity != null) {
                    _settings.value = RoomEntityMappers.parseUserSettings(entity.settingsJson)
                    _selectedWalletFilter.value = entity.selectedWalletFilter.ifBlank { "Semua" }
                    _upcomingItems.value = RoomEntityMappers.parseUpcomingItems(entity.upcomingItemsJson)
                    val matchingPlan = if (entity.activePlanId.isNotBlank()) {
                        _financialPlans.value.find { it.id == entity.activePlanId }
                    } else null
                    if (matchingPlan != null) {
                        _activePlanId.value = matchingPlan.id
                        _planTitle.value = matchingPlan.title
                        _plannedItems.value = matchingPlan.items
                    } else if (entity.planTitle.isNotBlank()) {
                        _planTitle.value = entity.planTitle
                    }
                    saveToDisk(appCtx)
                }
            }
        }
        syncObserverScope.launch {
            initialSeedComplete.await()
            db.customLogoDao().observeActive().collect { entities ->
                var changed = false
                entities.forEach { logo ->
                    if (logo.base64Data.isNotBlank()) {
                        val file = ImageUploadHelper.base64ToFile(appCtx, logo.fileName, logo.base64Data)
                        if (file != null && file.exists()) {
                            changed = true
                        }
                    }
                }
                if (changed) {
                    withContext(Dispatchers.Main) {
                        IndonesianInstitutionHelper.syncWithWalletsAndAssets(_wallets.value, _assets.value)
                        IndonesianInstitutionHelper.notifyCustomLogosChanged()
                    }
                }
            }
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
        val result = googleAuthManager.signInWithGoogle(activityContext)
        result.onSuccess { user ->
            _currentUser.value = user
            cloudSyncRepository?.startSyncForUser(user)
        }
        return result
    }

    suspend fun trySilentSignIn(activityContext: Context): Result<FirebaseUser?> {
        val result = googleAuthManager.trySilentSignIn(activityContext)
        result.onSuccess { user ->
            if (user != null) {
                _currentUser.value = user
                cloudSyncRepository?.startSyncForUser(user)
            }
        }
        return result
    }

    suspend fun signOut(context: Context) {
        googleAuthManager.signOut(context) {
            cloudSyncRepository?.stopSync()
        }
        _currentUser.value = null
        _syncStatus.value = SyncStatus.Guest
    }

    suspend fun manualSync(): Result<Long> {
        val syncRepo = cloudSyncRepository
            ?: return Result.failure(IllegalStateException("Sinkronisasi cloud belum siap."))
        initialSeedComplete.await()
        return syncRepo.manualSync()
    }

    private fun resolveAccountId(name: String): String {
        if (name.isBlank()) return ""
        return _wallets.value.find { it.name.equals(name, ignoreCase = true) }?.id ?: name
    }

    private fun persistTransactionAndSync(tx: Transaction) {
        val db = appDatabase ?: return
        val accountId = resolveAccountId(tx.accountName)
        val toAccountId = resolveAccountId(tx.toAccountName)
        val entity = tx.toEntity(accountIdResolved = accountId, toAccountIdResolved = toAccountId)
        transactionWriteSeq.incrementAndGet()
        pendingTransactionWrites.incrementAndGet()
        ioScope.launch {
            try {
                initialSeedComplete.await()
                db.transactionDao().upsert(entity)
                cloudSyncRepository?.pushTransaction(entity)
            } finally {
                pendingTransactionWrites.decrementAndGet()
            }
        }
    }

    private fun persistTransactionsAndSync(txs: List<Transaction>) {
        if (txs.isEmpty()) return
        val db = appDatabase ?: return
        val entities = txs.map { tx ->
            tx.toEntity(
                accountIdResolved = resolveAccountId(tx.accountName),
                toAccountIdResolved = resolveAccountId(tx.toAccountName)
            )
        }
        transactionWriteSeq.incrementAndGet()
        pendingTransactionWrites.incrementAndGet()
        ioScope.launch {
            try {
                initialSeedComplete.await()
                db.transactionDao().upsertAll(entities)
                cloudSyncRepository?.pushTransactions(entities)
            } finally {
                pendingTransactionWrites.decrementAndGet()
            }
        }
    }

    private fun softDeleteTransactionsAndSync(txIds: Collection<String>, timestamp: Long = System.currentTimeMillis()) {
        if (txIds.isEmpty()) return
        val db = appDatabase ?: return
        val safeIds = txIds.map { RoomEntityMappers.sanitizeFirestoreId(it) }.distinct()
        transactionWriteSeq.incrementAndGet()
        pendingTransactionWrites.incrementAndGet()
        ioScope.launch {
            try {
                initialSeedComplete.await()
                val deletedEntities = safeIds.mapNotNull { safeId ->
                    db.transactionDao().softDelete(safeId, timestamp)
                    db.transactionDao().getById(safeId)
                }
                if (deletedEntities.isNotEmpty()) {
                    cloudSyncRepository?.pushTransactions(deletedEntities)
                }
            } finally {
                pendingTransactionWrites.decrementAndGet()
            }
        }
    }

    private fun persistWalletsAndSync(wallets: List<AccountWallet>, fullOrderList: List<AccountWallet> = _wallets.value) {
        if (wallets.isEmpty()) return
        val db = appDatabase ?: return
        val indexById = fullOrderList.mapIndexed { idx, w -> w.id to idx }.toMap()
        val entities = wallets.map { w -> w.toEntity(sortOrder = indexById[w.id] ?: 0) }
        walletWriteSeq.incrementAndGet()
        pendingWalletWrites.incrementAndGet()
        ioScope.launch {
            try {
                initialSeedComplete.await()
                db.accountDao().upsertAll(entities)
                cloudSyncRepository?.pushAccounts(entities)
            } finally {
                pendingWalletWrites.decrementAndGet()
            }
        }
    }

    private fun softDeleteWalletAndSync(walletId: String, timestamp: Long = System.currentTimeMillis()) {
        val db = appDatabase ?: return
        val safeId = RoomEntityMappers.sanitizeFirestoreId(walletId)
        walletWriteSeq.incrementAndGet()
        pendingWalletWrites.incrementAndGet()
        ioScope.launch {
            try {
                initialSeedComplete.await()
                db.accountDao().softDelete(safeId, timestamp)
                db.accountDao().getById(safeId)?.let { deleted ->
                    cloudSyncRepository?.pushAccount(deleted)
                }
            } finally {
                pendingWalletWrites.decrementAndGet()
            }
        }
    }

    private fun persistCategoryAndSync(
        category: MainCategory,
        sortOrder: Int = _categories.value.indexOfFirst { it.id == category.id }.coerceAtLeast(0)
    ) {
        val db = appDatabase ?: return
        val entity = category.toEntity(sortOrder = sortOrder)
        categoryWriteSeq.incrementAndGet()
        pendingCategoryWrites.incrementAndGet()
        ioScope.launch {
            try {
                initialSeedComplete.await()
                db.categoryDao().upsert(entity)
                cloudSyncRepository?.pushCategory(entity)
            } finally {
                pendingCategoryWrites.decrementAndGet()
            }
        }
    }

    private fun persistCategoriesAndSync(
        categoriesToPersist: List<MainCategory>,
        fullOrderList: List<MainCategory> = _categories.value
    ) {
        if (categoriesToPersist.isEmpty()) return
        val db = appDatabase ?: return
        val indexById = fullOrderList.mapIndexed { idx, c -> c.id to idx }.toMap()
        val entities = categoriesToPersist.map { c ->
            c.toEntity(sortOrder = indexById[c.id] ?: 0)
        }
        categoryWriteSeq.incrementAndGet()
        pendingCategoryWrites.incrementAndGet()
        ioScope.launch {
            try {
                initialSeedComplete.await()
                db.categoryDao().upsertAll(entities)
                cloudSyncRepository?.pushCategories(entities)
            } finally {
                pendingCategoryWrites.decrementAndGet()
            }
        }
    }

    private fun softDeleteCategoryAndSync(categoryId: String, timestamp: Long = System.currentTimeMillis()) {
        val db = appDatabase ?: return
        val safeId = RoomEntityMappers.sanitizeFirestoreId(categoryId)
        categoryWriteSeq.incrementAndGet()
        pendingCategoryWrites.incrementAndGet()
        ioScope.launch {
            try {
                initialSeedComplete.await()
                db.categoryDao().softDelete(safeId, timestamp)
                db.categoryDao().getById(safeId)?.let { deleted ->
                    cloudSyncRepository?.pushCategory(deleted)
                }
            } finally {
                pendingCategoryWrites.decrementAndGet()
            }
        }
    }

    private fun persistAssetsAndSync(assets: List<AssetItem>, fullOrderList: List<AssetItem> = _assets.value) {
        if (assets.isEmpty()) return
        val db = appDatabase ?: return
        val indexById = fullOrderList.mapIndexed { idx, a -> a.id to idx }.toMap()
        val entities = assets.map { a -> a.toEntity(sortOrder = indexById[a.id] ?: 0) }
        ioScope.launch {
            initialSeedComplete.await()
            db.assetDao().upsertAll(entities)
            cloudSyncRepository?.pushAssets(entities)
        }
    }

    private fun softDeleteAssetAndSync(assetId: String, timestamp: Long = System.currentTimeMillis()) {
        val db = appDatabase ?: return
        val safeId = RoomEntityMappers.sanitizeFirestoreId(assetId)
        ioScope.launch {
            initialSeedComplete.await()
            db.assetDao().softDelete(safeId, timestamp)
            db.assetDao().getById(safeId)?.let { deleted ->
                cloudSyncRepository?.pushAsset(deleted)
            }
        }
    }

    private fun persistDebtsAndSync(debts: List<DebtItem>, fullOrderList: List<DebtItem> = _debts.value) {
        if (debts.isEmpty()) return
        val db = appDatabase ?: return
        val indexById = fullOrderList.mapIndexed { idx, d -> d.id to idx }.toMap()
        val entities = debts.map { d -> d.toEntity(sortOrder = indexById[d.id] ?: 0) }
        debtWriteSeq.incrementAndGet()
        pendingDebtWrites.incrementAndGet()
        ioScope.launch {
            try {
                initialSeedComplete.await()
                db.debtDao().upsertAll(entities)
                cloudSyncRepository?.pushDebts(entities)
            } finally {
                pendingDebtWrites.decrementAndGet()
            }
        }
    }

    private fun softDeleteDebtAndSync(debtId: String, timestamp: Long = System.currentTimeMillis()) {
        val db = appDatabase ?: return
        val safeId = RoomEntityMappers.sanitizeFirestoreId(debtId)
        debtWriteSeq.incrementAndGet()
        pendingDebtWrites.incrementAndGet()
        ioScope.launch {
            try {
                initialSeedComplete.await()
                db.debtDao().softDelete(safeId, timestamp)
                db.debtDao().getById(safeId)?.let { deleted ->
                    cloudSyncRepository?.pushDebt(deleted)
                }
            } finally {
                pendingDebtWrites.decrementAndGet()
            }
        }
    }

    private fun persistCustomLogoFromIconPath(rawIconOrUri: String, timestamp: Long = System.currentTimeMillis()) {
        if (!ImageUploadHelper.isCustomIconPath(rawIconOrUri)) return
        val ctx = getActiveContext() ?: return
        val db = appDatabase ?: return
        ioScope.launch {
            initialSeedComplete.await()
            val file = ImageUploadHelper.resolveExistingCustomIconFile(ctx, rawIconOrUri) ?: return@launch
            val b64 = ImageUploadHelper.fileToBase64(file) ?: return@launch
            val entity = CustomLogoEntity(
                id = RoomEntityMappers.customLogoDocId(file.name),
                fileName = file.name,
                base64Data = b64,
                updatedAt = timestamp,
                isDeleted = false
            )
            db.customLogoDao().upsert(entity)
            cloudSyncRepository?.pushCustomLogo(entity)
        }
    }

    private fun persistLabelAndSync(
        labelName: String,
        sortOrder: Int = _labels.value.indexOfFirst { it.equals(labelName, ignoreCase = true) }.coerceAtLeast(0),
        timestamp: Long = System.currentTimeMillis()
    ) {
        val clean = labelName.trim()
        if (clean.isBlank()) return
        val db = appDatabase ?: return
        val entity = LabelEntity(
            id = RoomEntityMappers.labelDocId(clean),
            name = clean,
            sortOrder = sortOrder,
            updatedAt = timestamp,
            isDeleted = false
        )
        ioScope.launch {
            initialSeedComplete.await()
            db.labelDao().upsert(entity)
            cloudSyncRepository?.pushLabel(entity)
        }
    }

    private fun persistLabelsAndSync(labels: List<String>, timestamp: Long = System.currentTimeMillis()) {
        if (labels.isEmpty()) return
        val db = appDatabase ?: return
        val entities = labels.mapIndexedNotNull { idx, raw ->
            val clean = raw.trim()
            if (clean.isBlank()) null
            else LabelEntity(
                id = RoomEntityMappers.labelDocId(clean),
                name = clean,
                sortOrder = idx,
                updatedAt = timestamp,
                isDeleted = false
            )
        }
        if (entities.isEmpty()) return
        ioScope.launch {
            initialSeedComplete.await()
            db.labelDao().upsertAll(entities)
            cloudSyncRepository?.pushLabels(entities)
        }
    }

    private fun softDeleteLabelAndSync(labelName: String, timestamp: Long = System.currentTimeMillis()) {
        val clean = labelName.trim()
        if (clean.isBlank()) return
        val db = appDatabase ?: return
        val canonicalId = RoomEntityMappers.labelDocId(clean)
        ioScope.launch {
            initialSeedComplete.await()
            val all = db.labelDao().getAllRaw()
            val matchedIds = all.filter { it.id == canonicalId || it.name.equals(clean, ignoreCase = true) }.map { it.id }
            val idsToDelete = if (matchedIds.isNotEmpty()) matchedIds else listOf(canonicalId)
            val deleted = idsToDelete.mapNotNull { id ->
                db.labelDao().softDelete(id, timestamp)
                db.labelDao().getById(id)
            }
            if (deleted.isNotEmpty()) {
                cloudSyncRepository?.pushLabels(deleted)
            }
        }
    }

    private fun persistBudgetsAndSync(
        budgets: List<BudgetCategory>,
        fullOrderList: List<BudgetCategory> = _budgets.value
    ) {
        if (budgets.isEmpty()) return
        val db = appDatabase ?: return
        val indexById = fullOrderList.mapIndexed { idx, b -> b.id to idx }.toMap()
        val entities = budgets.map { it.toEntity(sortOrder = indexById[it.id] ?: 0) }
        ioScope.launch {
            initialSeedComplete.await()
            db.budgetDao().upsertAll(entities)
            cloudSyncRepository?.pushBudgets(entities)
        }
    }

    private fun softDeleteBudgetAndSync(budgetId: String, timestamp: Long = System.currentTimeMillis()) {
        val db = appDatabase ?: return
        val safeId = RoomEntityMappers.sanitizeFirestoreId(budgetId)
        ioScope.launch {
            initialSeedComplete.await()
            db.budgetDao().softDelete(safeId, timestamp)
            db.budgetDao().getById(safeId)?.let { deleted ->
                cloudSyncRepository?.pushBudget(deleted)
            }
        }
    }

    private fun persistRecurringAndSync(
        items: List<RecurringTransaction>,
        fullOrderList: List<RecurringTransaction> = _recurringTransactions.value
    ) {
        if (items.isEmpty()) return
        val db = appDatabase ?: return
        val indexById = fullOrderList.mapIndexed { idx, r -> r.id to idx }.toMap()
        val entities = items.map { it.toEntity(sortOrder = indexById[it.id] ?: 0) }
        ioScope.launch {
            initialSeedComplete.await()
            db.recurringDao().upsertAll(entities)
            cloudSyncRepository?.pushRecurrings(entities)
        }
    }

    private fun softDeleteRecurringAndSync(recurringId: String, timestamp: Long = System.currentTimeMillis()) {
        val db = appDatabase ?: return
        val safeId = RoomEntityMappers.sanitizeFirestoreId(recurringId)
        ioScope.launch {
            initialSeedComplete.await()
            db.recurringDao().softDelete(safeId, timestamp)
            db.recurringDao().getById(safeId)?.let { deleted ->
                cloudSyncRepository?.pushRecurring(deleted)
            }
        }
    }

    private fun persistFinancialPlansAndSync(
        plans: List<FinancialPlan>,
        fullOrderList: List<FinancialPlan> = _financialPlans.value
    ) {
        if (plans.isEmpty()) return
        val db = appDatabase ?: return
        val indexById = fullOrderList.mapIndexed { idx, p -> p.id to idx }.toMap()
        val entities = plans.map { it.toEntity(sortOrder = indexById[it.id] ?: 0) }
        ioScope.launch {
            initialSeedComplete.await()
            db.financialPlanDao().upsertAll(entities)
            cloudSyncRepository?.pushFinancialPlans(entities)
        }
    }

    private fun softDeleteFinancialPlanAndSync(planId: String, timestamp: Long = System.currentTimeMillis()) {
        val db = appDatabase ?: return
        val safeId = RoomEntityMappers.sanitizeFirestoreId(planId)
        ioScope.launch {
            initialSeedComplete.await()
            db.financialPlanDao().softDelete(safeId, timestamp)
            db.financialPlanDao().getById(safeId)?.let { deleted ->
                cloudSyncRepository?.pushFinancialPlan(deleted)
            }
        }
    }

    private fun persistEventsAndSync(
        events: List<EventItem>,
        fullOrderList: List<EventItem> = _events.value
    ) {
        if (events.isEmpty()) return
        val db = appDatabase ?: return
        val indexById = fullOrderList.mapIndexed { idx, ev -> ev.id to idx }.toMap()
        val entities = events.map { it.toEntity(sortOrder = indexById[it.id] ?: 0) }
        ioScope.launch {
            initialSeedComplete.await()
            db.eventDao().upsertAll(entities)
            cloudSyncRepository?.pushEvents(entities)
        }
    }

    private fun softDeleteEventAndSync(eventId: String, timestamp: Long = System.currentTimeMillis()) {
        val db = appDatabase ?: return
        val safeId = RoomEntityMappers.sanitizeFirestoreId(eventId)
        ioScope.launch {
            initialSeedComplete.await()
            db.eventDao().softDelete(safeId, timestamp)
            db.eventDao().getById(safeId)?.let { deleted ->
                cloudSyncRepository?.pushEvent(deleted)
            }
        }
    }

    private fun persistSavingsGoalsAndSync(
        goals: List<SavingsGoal>,
        fullOrderList: List<SavingsGoal> = _savingsGoals.value
    ) {
        if (goals.isEmpty()) return
        val db = appDatabase ?: return
        val indexById = fullOrderList.mapIndexed { idx, g -> g.id to idx }.toMap()
        val entities = goals.map { it.toEntity(sortOrder = indexById[it.id] ?: 0) }
        ioScope.launch {
            initialSeedComplete.await()
            db.savingsGoalDao().upsertAll(entities)
            cloudSyncRepository?.pushSavingsGoals(entities)
        }
    }

    private fun softDeleteSavingsGoalAndSync(goalId: String, timestamp: Long = System.currentTimeMillis()) {
        val db = appDatabase ?: return
        val safeId = RoomEntityMappers.sanitizeFirestoreId(goalId)
        ioScope.launch {
            initialSeedComplete.await()
            db.savingsGoalDao().softDelete(safeId, timestamp)
            db.savingsGoalDao().getById(safeId)?.let { deleted ->
                cloudSyncRepository?.pushSavingsGoal(deleted)
            }
        }
    }

    private fun persistTimelineScenariosAndSync(
        scenarios: List<TimelineScenarioItem>,
        fullOrderList: List<TimelineScenarioItem> = _timelineScenarios.value
    ) {
        if (scenarios.isEmpty()) return
        val db = appDatabase ?: return
        val indexById = fullOrderList.mapIndexed { idx, s -> s.id to idx }.toMap()
        val entities = scenarios.map { it.toEntity(sortOrder = indexById[it.id] ?: 0) }
        ioScope.launch {
            initialSeedComplete.await()
            db.timelineScenarioDao().upsertAll(entities)
            cloudSyncRepository?.pushTimelineScenarios(entities)
        }
    }

    private fun softDeleteTimelineScenarioAndSync(scenarioId: String, timestamp: Long = System.currentTimeMillis()) {
        val db = appDatabase ?: return
        val safeId = RoomEntityMappers.sanitizeFirestoreId(scenarioId)
        ioScope.launch {
            initialSeedComplete.await()
            db.timelineScenarioDao().softDelete(safeId, timestamp)
            db.timelineScenarioDao().getById(safeId)?.let { deleted ->
                cloudSyncRepository?.pushTimelineScenario(deleted)
            }
        }
    }

    private fun persistAppSettingsAndSync(timestamp: Long = System.currentTimeMillis()) {
        val db = appDatabase ?: return
        val entity = AppSettingsEntity(
            id = "main",
            settingsJson = RoomEntityMappers.serializeUserSettings(_settings.value),
            activePlanId = _activePlanId.value.ifBlank { "plan_default" },
            planTitle = _planTitle.value.ifBlank { "Pembagian Gaji" },
            selectedWalletFilter = _selectedWalletFilter.value.ifBlank { "Semua" },
            upcomingItemsJson = RoomEntityMappers.serializeUpcomingItems(_upcomingItems.value),
            updatedAt = timestamp
        )
        ioScope.launch {
            initialSeedComplete.await()
            db.appSettingsDao().upsert(entity)
            cloudSyncRepository?.pushAppSettings(entity)
        }
    }

    private fun loadFromDisk(ctx: Context): Boolean {
        val data = FinanceStorageHelper.loadData(ctx) ?: return false
        _transactions.value = data.transactions.sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator)
        _wallets.value = data.wallets
        _assets.value = data.assets
        _debts.value = data.debts
        _budgets.value = data.budgets
        _recurringTransactions.value = data.recurringTransactions
        _savingsGoals.value = data.savingsGoals
        _events.value = data.events

        if (data.financialPlans.isNotEmpty()) {
            _financialPlans.value = data.financialPlans
            val active = data.financialPlans.find { it.id == data.activePlanId } ?: data.financialPlans.first()
            _activePlanId.value = active.id
            _planTitle.value = active.title
            _plannedItems.value = active.items
        } else {
            val cal = Calendar.getInstance()
            val fallbackPlan = FinancialPlan(
                id = "plan_default",
                title = data.planTitle,
                monthIndex = cal.get(Calendar.MONTH) + 1,
                year = cal.get(Calendar.YEAR),
                items = data.plannedItems
            )
            _financialPlans.value = listOf(fallbackPlan)
            _activePlanId.value = fallbackPlan.id
            _plannedItems.value = data.plannedItems
            _planTitle.value = data.planTitle
        }

        _labels.value = data.labels
        _settings.value = data.settings
        _selectedWalletFilter.value = data.selectedWalletFilter
        _timelineScenarios.value = data.timelineScenarios
        if (data.categories.isNotEmpty()) {
            _categories.value = data.categories
        }
        IndonesianInstitutionHelper.syncWithWalletsAndAssets(data.wallets, data.assets)
        syncDebtPaymentsWithTransactions()
        return true
    }

    fun saveToDisk(ctx: Context? = getActiveContext()) {
        val c = ctx ?: return
        val data = FinanceStorageHelper.PersistentData(
            transactions = _transactions.value,
            wallets = _wallets.value,
            assets = _assets.value,
            debts = _debts.value,
            budgets = _budgets.value,
            recurringTransactions = _recurringTransactions.value,
            plannedItems = _plannedItems.value,
            planTitle = _planTitle.value,
            labels = _labels.value,
            settings = _settings.value,
            selectedWalletFilter = _selectedWalletFilter.value,
            categories = _categories.value,
            financialPlans = _financialPlans.value,
            activePlanId = _activePlanId.value,
            savingsGoals = _savingsGoals.value,
            events = _events.value,
            timelineScenarios = _timelineScenarios.value
        )
        ioScope.launch {
            FinanceStorageHelper.saveData(c, data)
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(c)
        }
    }

    fun loadCleanInitialState() {
        _wallets.value = emptyList()
        _assets.value = emptyList()
        _debts.value = emptyList()
        _upcomingItems.value = emptyList()
        _transactions.value = emptyList()
        _budgets.value = emptyList()
        _recurringTransactions.value = emptyList()
        _plannedItems.value = emptyList()
        _planTitle.value = "Rencana Bulanan"
        _financialPlans.value = emptyList()
        _activePlanId.value = ""
        _savingsGoals.value = emptyList()
        _events.value = emptyList()
        _timelineScenarios.value = emptyList()
        _selectedWalletFilter.value = "Semua"
        _categories.value = FinanceStorageHelper.getDefaultCategories()
        _labels.value = listOf("Investasi", "Pribadi", "Keluarga", "Tabungan")
        _settings.value = UserSettings(hasCompletedOnboarding = false)
        IndonesianInstitutionHelper.syncWithWalletsAndAssets(emptyList(), emptyList())
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
        val now = System.currentTimeMillis()
        val resolvedCurrency = currencyCode.ifBlank { "IDR" }
        val resolvedAccountName = accountName.trim().ifBlank {
            when (accountType) {
                "Bank" -> "Bank"
                "Dompet digital" -> "Dompet digital"
                else -> "Tunai"
            }
        }
        val resolvedIcon = accountIconName.ifBlank {
            when (accountType) {
                "Bank" -> "Bank"
                "Dompet digital" -> "Dompet digital"
                else -> "cash"
            }
        }

        val newWallet = AccountWallet(
            name = resolvedAccountName,
            bankType = "$accountType · $resolvedCurrency",
            balance = initialBalance,
            currency = resolvedCurrency,
            iconName = resolvedIcon,
            lastCategory = if (initialBalance > 0.0) "Saldo Awal" else "Belum ada transaksi",
            lastAmount = initialBalance,
            lastActivityTime = "Baru saja",
            updatedAt = now,
            isDeleted = false
        )
        addWallet(newWallet)

        val anyReminderEnabled = bills.any { it.reminderEnabled }
        bills.forEach { bill ->
            if (bill.title.isNotBlank()) {
                val recurring = RecurringTransaction(
                    title = bill.title.trim(),
                    amount = bill.amount.coerceAtLeast(0.0),
                    type = TransactionType.EXPENSE,
                    category = bill.category.ifBlank { "Bills & Fees" },
                    accountName = resolvedAccountName,
                    dayOfMonth = bill.dayOfMonth.coerceIn(1, 31),
                    frequency = "Setiap bulan",
                    autoDeduct = false,
                    reminderEnabled = bill.reminderEnabled,
                    reminderOptions = if (bill.reminderEnabled) listOf("1 hari sebelumnya") else emptyList(),
                    updatedAt = now,
                    isDeleted = false
                )
                addRecurringTransaction(recurring, checkImmediateProcess = false)
            }
        }

        budgets.forEach { budget ->
            if (budget.amount > 0.0) {
                val b = BudgetCategory(
                    category = budget.categoryName,
                    name = budget.categoryName,
                    categories = budget.matchCategories.ifEmpty { listOf(budget.categoryName) },
                    allocated = budget.amount,
                    spent = 0.0,
                    frequency = "Bulanan",
                    updatedAt = now,
                    isDeleted = false
                )
                addBudget(b)
            }
        }

        if (createMonthlyPlan) {
            val planItems = mutableListOf<PlannedItem>()
            if (monthlyIncome > 0.0) {
                planItems.add(
                    PlannedItem(
                        name = "Pendapatan Bulanan",
                        category = "Salary",
                        amount = monthlyIncome,
                        section = PlanSectionType.INCOME
                    )
                )
            }
            bills.filter { it.amount > 0.0 }.forEach { bill ->
                planItems.add(
                    PlannedItem(
                        name = bill.title.trim(),
                        category = bill.category.ifBlank { "Bills & Fees" },
                        amount = bill.amount,
                        section = PlanSectionType.EXPENSE
                    )
                )
            }
            budgets.filter { it.amount > 0.0 }.forEach { b ->
                planItems.add(
                    PlannedItem(
                        name = b.categoryName,
                        category = b.categoryName,
                        amount = b.amount,
                        section = PlanSectionType.EXPENSE
                    )
                )
            }
            val cal = Calendar.getInstance()
            val plan = FinancialPlan(
                id = "plan_${UUID.randomUUID().toString().take(8)}",
                title = "Rencana Bulanan",
                monthIndex = cal.get(Calendar.MONTH) + 1,
                year = cal.get(Calendar.YEAR),
                items = planItems,
                updatedAt = now,
                isDeleted = false
            )
            _financialPlans.value = listOf(plan)
            _activePlanId.value = plan.id
            _planTitle.value = plan.title
            _plannedItems.value = plan.items
            persistFinancialPlansAndSync(listOf(plan), _financialPlans.value)
        }

        _settings.value = _settings.value.copy(
            primaryCurrency = resolvedCurrency,
            monthlyStartDay = paydayDay.coerceIn(1, 31),
            billReminder = anyReminderEnabled || _settings.value.billReminder,
            hasCompletedOnboarding = true
        )
        persistAppSettingsAndSync(now)
        saveToDisk()
    }

    fun addTransaction(tx: Transaction) {
        val now = System.currentTimeMillis()
        val cleanTx = TransactionMathHelper.sanitizeTransaction(tx).copy(updatedAt = now, isDeleted = false)
        _transactions.update { (listOf(cleanTx) + it).sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator) }

        val modifiedWallets = mutableListOf<AccountWallet>()
        // Reactively adjust wallet balance
        _wallets.update { list ->
            list.map { wallet ->
                val isSource = wallet.name.equals(cleanTx.accountName, ignoreCase = true)
                val isDest = cleanTx.toAccountName.isNotBlank() && wallet.name.equals(cleanTx.toAccountName, ignoreCase = true)

                var newBalance = wallet.balance
                if (isSource) {
                    newBalance = TransactionMathHelper.applyTransactionToBalance(newBalance, cleanTx, isSourceAccount = true)
                }
                if (isDest && cleanTx.type == TransactionType.TRANSFER) {
                    newBalance = TransactionMathHelper.applyTransactionToBalance(newBalance, cleanTx, isSourceAccount = false)
                }

                if (isSource) {
                    wallet.copy(
                        balance = newBalance,
                        lastCategory = cleanTx.category,
                        lastAmount = cleanTx.amount,
                        lastActivityTime = "Baru saja",
                        updatedAt = now
                    ).also { modifiedWallets.add(it) }
                } else if (newBalance != wallet.balance) {
                    wallet.copy(
                        balance = newBalance,
                        lastCategory = "Transfer Masuk",
                        lastAmount = cleanTx.amount,
                        lastActivityTime = "Baru saja",
                        updatedAt = now
                    ).also { modifiedWallets.add(it) }
                } else {
                    wallet
                }
            }
        }

        val modifiedAssets = mutableListOf<AssetItem>()
        // If transferred to asset, increase asset value AND record into asset's riwayat pencatatan
        if (cleanTx.type == TransactionType.TRANSFER && (cleanTx.targetAssetId.isNotBlank() || cleanTx.targetAssetName.isNotBlank())) {
            val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
            val monthStr = if (cleanTx.monthIndex in 1..12) monthNames[cleanTx.monthIndex] else "Okt"
            _assets.update { list ->
                list.map { asset ->
                    if (asset.id == cleanTx.targetAssetId || (cleanTx.targetAssetName.isNotBlank() && asset.name.equals(cleanTx.targetAssetName, ignoreCase = true))) {
                        val record = AssetRecord(
                            id = "tx_${cleanTx.id}",
                            assetId = asset.id,
                            tag = "Transfer",
                            isIncrease = true,
                            amount = cleanTx.amount,
                            sourceWalletName = cleanTx.accountName,
                            date = "${cleanTx.dayOfMonth} $monthStr ${cleanTx.year}",
                            time = cleanTx.time.ifBlank { "12:00" },
                            note = if (cleanTx.note.isNotBlank()) cleanTx.note else "Transfer dari ${cleanTx.accountName}"
                        )
                        asset.copy(
                            value = asset.value + cleanTx.amount,
                            records = listOf(record) + asset.records,
                            updatedAt = now
                        ).also { modifiedAssets.add(it) }
                    } else asset
                }
            }
        }

        val modifiedDebts = mutableListOf<DebtItem>()
        // If transferred to debt (Pembayaran Utang), record into debt's records and increase paidAmount
        if ((cleanTx.type == TransactionType.TRANSFER || cleanTx.category.equals("Pembayaran Utang", ignoreCase = true)) &&
            (cleanTx.targetDebtId.isNotBlank() || cleanTx.targetDebtName.isNotBlank())) {
            val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
            val monthStr = if (cleanTx.monthIndex in 1..12) monthNames[cleanTx.monthIndex] else "Okt"
            val recordTime = cleanTx.time.ifBlank { "12:00" }
            val formattedDate = "${cleanTx.dayOfMonth} $monthStr ${cleanTx.year} · $recordTime"
            val debtRecord = DebtRecord(
                id = "tx_${cleanTx.id}",
                debtId = cleanTx.targetDebtId,
                type = "Pembayaran",
                amount = cleanTx.amount,
                date = formattedDate,
                note = if (cleanTx.note.isNotBlank()) cleanTx.note else "Transfer dari ${cleanTx.accountName}",
                sourceWalletName = cleanTx.accountName
            )
            _debts.update { list ->
                list.map { debt ->
                    if (debt.id == cleanTx.targetDebtId || (cleanTx.targetDebtName.isNotBlank() && debt.name.equals(cleanTx.targetDebtName, ignoreCase = true))) {
                        val newPaid = debt.paidAmount + cleanTx.amount
                        debt.copy(
                            paidAmount = newPaid,
                            records = listOf(debtRecord) + debt.records,
                            updatedAt = now
                        ).also { modifiedDebts.add(it) }
                    } else debt
                }
            }
        }
        persistTransactionAndSync(cleanTx)
        persistWalletsAndSync(modifiedWallets)
        persistAssetsAndSync(modifiedAssets)
        persistDebtsAndSync(modifiedDebts)
        saveToDisk()
    }

    fun deleteTransaction(txId: String) {
        val target = _transactions.value.find { it.id == txId } ?: return
        val now = System.currentTimeMillis()
        val cleanTx = TransactionMathHelper.sanitizeTransaction(target)
        _transactions.update { it.filterNot { item -> item.id == txId } }

        val modifiedWallets = mutableListOf<AccountWallet>()
        _wallets.update { list ->
            list.map { wallet ->
                val isSource = wallet.name.equals(cleanTx.accountName, ignoreCase = true)
                val isDest = cleanTx.toAccountName.isNotBlank() && wallet.name.equals(cleanTx.toAccountName, ignoreCase = true)

                var newBal = wallet.balance
                if (isSource) {
                    newBal = TransactionMathHelper.revertTransactionFromBalance(newBal, cleanTx, isSourceAccount = true)
                }
                if (isDest && cleanTx.type == TransactionType.TRANSFER) {
                    newBal = TransactionMathHelper.revertTransactionFromBalance(newBal, cleanTx, isSourceAccount = false)
                }

                if (newBal != wallet.balance) {
                    wallet.copy(balance = newBal, updatedAt = now).also { modifiedWallets.add(it) }
                } else {
                    wallet
                }
            }
        }

        val modifiedAssets = mutableListOf<AssetItem>()
        if (cleanTx.type == TransactionType.TRANSFER && (cleanTx.targetAssetId.isNotBlank() || cleanTx.targetAssetName.isNotBlank())) {
            val recId = "tx_${cleanTx.id}"
            val assetRecId = "asset_rec_${cleanTx.id}"
            _assets.update { list ->
                list.map { asset ->
                    if (asset.id == cleanTx.targetAssetId || (cleanTx.targetAssetName.isNotBlank() && asset.name.equals(cleanTx.targetAssetName, ignoreCase = true))) {
                        asset.copy(
                            value = (asset.value - cleanTx.amount).coerceAtLeast(0.0),
                            records = asset.records.filterNot { it.id == recId || it.id == assetRecId || it.id == cleanTx.id },
                            updatedAt = now
                        ).also { modifiedAssets.add(it) }
                    } else asset
                }
            }
        }

        val modifiedDebts = mutableListOf<DebtItem>()
        if ((cleanTx.type == TransactionType.TRANSFER || cleanTx.category.equals("Pembayaran Utang", ignoreCase = true)) &&
            (cleanTx.targetDebtId.isNotBlank() || cleanTx.targetDebtName.isNotBlank())) {
            val recId = "tx_${cleanTx.id}"
            _debts.update { list ->
                list.map { debt ->
                    if (debt.id == cleanTx.targetDebtId || (cleanTx.targetDebtName.isNotBlank() && debt.name.equals(cleanTx.targetDebtName, ignoreCase = true))) {
                        val newPaid = (debt.paidAmount - cleanTx.amount).coerceAtLeast(0.0)
                        debt.copy(
                            paidAmount = newPaid,
                            records = debt.records.filterNot { it.id == recId || it.id == cleanTx.id },
                            updatedAt = now
                        ).also { modifiedDebts.add(it) }
                    } else debt
                }
            }
        }
        softDeleteTransactionsAndSync(listOf(txId), now)
        persistWalletsAndSync(modifiedWallets)
        persistAssetsAndSync(modifiedAssets)
        persistDebtsAndSync(modifiedDebts)
        saveToDisk()
    }

    fun updateTransaction(updatedTx: Transaction) {
        val oldTxRaw = _transactions.value.find { it.id == updatedTx.id } ?: return
        val now = System.currentTimeMillis()
        val oldTx = TransactionMathHelper.sanitizeTransaction(oldTxRaw)
        val newTx = TransactionMathHelper.sanitizeTransaction(updatedTx).copy(updatedAt = now, isDeleted = false)

        _transactions.update { list ->
            list.map { if (it.id == updatedTx.id) newTx else it }
                .sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator)
        }

        val modifiedWallets = mutableListOf<AccountWallet>()
        _wallets.update { list ->
            list.map { wallet ->
                var bal = wallet.balance
                if (wallet.name.equals(oldTx.accountName, ignoreCase = true)) {
                    bal = TransactionMathHelper.revertTransactionFromBalance(bal, oldTx, isSourceAccount = true)
                }
                if (oldTx.type == TransactionType.TRANSFER && oldTx.toAccountName.isNotBlank() && wallet.name.equals(oldTx.toAccountName, ignoreCase = true)) {
                    bal = TransactionMathHelper.revertTransactionFromBalance(bal, oldTx, isSourceAccount = false)
                }

                if (wallet.name.equals(newTx.accountName, ignoreCase = true)) {
                    bal = TransactionMathHelper.applyTransactionToBalance(bal, newTx, isSourceAccount = true)
                }
                if (newTx.type == TransactionType.TRANSFER && newTx.toAccountName.isNotBlank() && wallet.name.equals(newTx.toAccountName, ignoreCase = true)) {
                    bal = TransactionMathHelper.applyTransactionToBalance(bal, newTx, isSourceAccount = false)
                }

                if (wallet.name.equals(newTx.accountName, ignoreCase = true)) {
                    wallet.copy(
                        balance = bal,
                        lastCategory = newTx.category,
                        lastAmount = newTx.amount,
                        lastActivityTime = "Baru saja",
                        updatedAt = now
                    ).also { modifiedWallets.add(it) }
                } else if (bal != wallet.balance) {
                    wallet.copy(balance = bal, updatedAt = now).also { modifiedWallets.add(it) }
                } else {
                    wallet
                }
            }
        }

        val modifiedAssets = mutableListOf<AssetItem>()
        if (oldTx.type == TransactionType.TRANSFER || newTx.type == TransactionType.TRANSFER) {
            val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
            val monthStr = if (newTx.monthIndex in 1..12) monthNames[newTx.monthIndex] else "Okt"
            val oldRecId = "tx_${oldTx.id}"
            val newRecId = "tx_${newTx.id}"

            _assets.update { list ->
                list.map { asset ->
                    var v = asset.value
                    var recs = asset.records
                    var changed = false
                    if (oldTx.type == TransactionType.TRANSFER && (asset.id == oldTx.targetAssetId || asset.name.equals(oldTx.targetAssetName, ignoreCase = true))) {
                        v = (v - oldTx.amount).coerceAtLeast(0.0)
                        recs = recs.filterNot { it.id == oldRecId || it.id == newRecId }
                        changed = true
                    }
                    if (newTx.type == TransactionType.TRANSFER && (asset.id == newTx.targetAssetId || asset.name.equals(newTx.targetAssetName, ignoreCase = true))) {
                        v += newTx.amount
                        val record = AssetRecord(
                            id = newRecId,
                            assetId = asset.id,
                            tag = "Transfer",
                            isIncrease = true,
                            amount = newTx.amount,
                            sourceWalletName = newTx.accountName,
                            date = "${newTx.dayOfMonth} $monthStr ${newTx.year}",
                            time = newTx.time.ifBlank { "12:00" },
                            note = if (newTx.note.isNotBlank()) newTx.note else "Transfer dari ${newTx.accountName}"
                        )
                        recs = listOf(record) + recs
                        changed = true
                    }
                    if (changed) asset.copy(value = v, records = recs, updatedAt = now).also { modifiedAssets.add(it) } else asset
                }
            }
        }

        val modifiedDebts = mutableListOf<DebtItem>()
        val isOldDebt = (oldTx.type == TransactionType.TRANSFER || oldTx.category.equals("Pembayaran Utang", ignoreCase = true)) &&
                (oldTx.targetDebtId.isNotBlank() || oldTx.targetDebtName.isNotBlank())
        val isNewDebt = (newTx.type == TransactionType.TRANSFER || newTx.category.equals("Pembayaran Utang", ignoreCase = true)) &&
                (newTx.targetDebtId.isNotBlank() || newTx.targetDebtName.isNotBlank())

        if (isOldDebt || isNewDebt) {
            val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
            val monthStr = if (newTx.monthIndex in 1..12) monthNames[newTx.monthIndex] else "Okt"
            val oldRecId = "tx_${oldTx.id}"
            val debtRecId = if (oldTx.id.startsWith("debt_rec_")) oldTx.id.removePrefix("debt_rec_") else ""
            val newRecId = if (newTx.id.startsWith("debt_rec_")) newTx.id.removePrefix("debt_rec_") else "tx_${newTx.id}"

            _debts.update { list ->
                list.map { debt ->
                    var paid = debt.paidAmount
                    var recs = debt.records
                    var changed = false
                    if (isOldDebt && (debt.id == oldTx.targetDebtId || debt.name.equals(oldTx.targetDebtName, ignoreCase = true))) {
                        paid = (paid - oldTx.amount).coerceAtLeast(0.0)
                        recs = recs.filterNot { it.id == oldRecId || it.id == debtRecId || it.id == oldTx.id }
                        changed = true
                    }
                    if (isNewDebt && (debt.id == newTx.targetDebtId || debt.name.equals(newTx.targetDebtName, ignoreCase = true))) {
                        paid += newTx.amount
                        val record = DebtRecord(
                            id = newRecId,
                            debtId = debt.id,
                            type = "Pembayaran",
                            amount = newTx.amount,
                            date = "${newTx.dayOfMonth} $monthStr ${newTx.year}",
                            time = newTx.time.ifBlank { "12:00" },
                            note = if (newTx.note.isNotBlank()) newTx.note else "Transfer dari ${newTx.accountName}",
                            sourceWalletName = newTx.accountName
                        )
                        recs = listOf(record) + recs
                        changed = true
                    }
                    if (changed) debt.copy(paidAmount = paid, records = recs, updatedAt = now).also { modifiedDebts.add(it) } else debt
                }
            }
        }
        persistTransactionAndSync(newTx)
        persistWalletsAndSync(modifiedWallets)
        persistAssetsAndSync(modifiedAssets)
        persistDebtsAndSync(modifiedDebts)
        saveToDisk()
    }

    fun batchDeleteTransactions(txIds: Set<String>) {
        if (txIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val targets = _transactions.value.filter { it.id in txIds }.map { TransactionMathHelper.sanitizeTransaction(it) }
        _transactions.update { it.filterNot { item -> item.id in txIds } }

        val modifiedWallets = mutableListOf<AccountWallet>()
        _wallets.update { list ->
            list.map { wallet ->
                var bal = wallet.balance
                targets.forEach { target ->
                    val isSource = wallet.name.equals(target.accountName, ignoreCase = true)
                    val isDest = target.toAccountName.isNotBlank() && wallet.name.equals(target.toAccountName, ignoreCase = true)

                    if (isSource) {
                        bal = TransactionMathHelper.revertTransactionFromBalance(bal, target, isSourceAccount = true)
                    }
                    if (isDest && target.type == TransactionType.TRANSFER) {
                        bal = TransactionMathHelper.revertTransactionFromBalance(bal, target, isSourceAccount = false)
                    }
                }
                if (bal != wallet.balance) {
                    wallet.copy(balance = bal, updatedAt = now).also { modifiedWallets.add(it) }
                } else wallet
            }
        }

        val modifiedAssets = mutableMapOf<String, AssetItem>()
        targets.filter { it.type == TransactionType.TRANSFER && (it.targetAssetId.isNotBlank() || it.targetAssetName.isNotBlank()) }.forEach { target ->
            val recId = "tx_${target.id}"
            val assetRecId = "asset_rec_${target.id}"
            _assets.update { list ->
                list.map { asset ->
                    if (asset.id == target.targetAssetId || (target.targetAssetName.isNotBlank() && asset.name.equals(target.targetAssetName, ignoreCase = true))) {
                        asset.copy(
                            value = (asset.value - target.amount).coerceAtLeast(0.0),
                            records = asset.records.filterNot { it.id == recId || it.id == assetRecId || it.id == target.id },
                            updatedAt = now
                        ).also { modifiedAssets[it.id] = it }
                    } else asset
                }
            }
        }

        val modifiedDebts = mutableMapOf<String, DebtItem>()
        targets.filter { (it.type == TransactionType.TRANSFER || it.category.equals("Pembayaran Utang", ignoreCase = true)) && (it.targetDebtId.isNotBlank() || it.targetDebtName.isNotBlank()) }.forEach { target ->
            val recId = "tx_${target.id}"
            val debtRecId = if (target.id.startsWith("debt_rec_")) target.id.removePrefix("debt_rec_") else ""
            _debts.update { list ->
                list.map { debt ->
                    if (debt.id == target.targetDebtId || (target.targetDebtName.isNotBlank() && debt.name.equals(target.targetDebtName, ignoreCase = true))) {
                        debt.copy(
                            paidAmount = (debt.paidAmount - target.amount).coerceAtLeast(0.0),
                            records = debt.records.filterNot { it.id == recId || it.id == debtRecId || it.id == target.id },
                            updatedAt = now
                        ).also { modifiedDebts[it.id] = it }
                    } else debt
                }
            }
        }

        softDeleteTransactionsAndSync(txIds, now)
        persistWalletsAndSync(modifiedWallets)
        persistAssetsAndSync(modifiedAssets.values.toList())
        persistDebtsAndSync(modifiedDebts.values.toList())
        saveToDisk()
    }

    fun batchUpdateCategory(txIds: Set<String>, newCategory: String, newSubcategory: String = "") {
        if (txIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val matchingCat = _categories.value.find { it.name.equals(newCategory, ignoreCase = true) }
        val newType = matchingCat?.type ?: when (newCategory) {
            "Salary", "Extra Income", "Bonus", "Investasi", "Pemberian" -> TransactionType.INCOME
            "Transfer", "Transfer Antar Bank", "Asset", "Aset", "Top Up", "Top Up E-Wallet" -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }
        val changed = mutableListOf<Transaction>()
        _transactions.update { list ->
            list.map { tx ->
                if (tx.id in txIds) {
                    tx.copy(category = newCategory, subcategory = newSubcategory, type = newType, updatedAt = now).also { changed.add(it) }
                } else tx
            }
        }
        persistTransactionsAndSync(changed)
        saveToDisk()
    }

    fun batchUpdateLabel(txIds: Set<String>, newLabel: String) {
        if (txIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val changed = mutableListOf<Transaction>()
        _transactions.update { list ->
            list.map { tx ->
                if (tx.id in txIds) {
                    tx.copy(label = newLabel, updatedAt = now).also { changed.add(it) }
                } else tx
            }
        }
        persistTransactionsAndSync(changed)
        saveToDisk()
    }

    fun addWallet(wallet: AccountWallet) {
        val now = System.currentTimeMillis()
        val stamped = wallet.copy(updatedAt = now, isDeleted = false)
        _wallets.update { it + stamped }
        IndonesianInstitutionHelper.registerCustomLogo(stamped.name, stamped.iconName)
        IndonesianInstitutionHelper.syncWithWalletsAndAssets(_wallets.value, _assets.value)
        persistWalletsAndSync(listOf(stamped))
        persistCustomLogoFromIconPath(stamped.iconName, now)
        saveToDisk()
    }

    fun updateWallet(wallet: AccountWallet) {
        val now = System.currentTimeMillis()
        val oldWallet = _wallets.value.find { it.id == wallet.id }
        val stamped = wallet.copy(updatedAt = now, isDeleted = false)
        _wallets.update { list -> list.map { if (it.id == stamped.id) stamped else it } }
        IndonesianInstitutionHelper.registerCustomLogo(stamped.name, stamped.iconName)
        val changedTxs = mutableListOf<Transaction>()
        if (oldWallet != null && oldWallet.name != stamped.name) {
            IndonesianInstitutionHelper.registerCustomLogo(oldWallet.name, stamped.iconName)
            _transactions.update { list ->
                list.map { tx ->
                    var updatedTx = tx
                    var modified = false
                    if (tx.accountName.equals(oldWallet.name, ignoreCase = true)) {
                        updatedTx = updatedTx.copy(accountName = stamped.name, updatedAt = now)
                        modified = true
                    }
                    if (tx.toAccountName.equals(oldWallet.name, ignoreCase = true)) {
                        updatedTx = updatedTx.copy(toAccountName = stamped.name, updatedAt = now)
                        modified = true
                    }
                    if (modified) changedTxs.add(updatedTx)
                    updatedTx
                }
            }
        }
        IndonesianInstitutionHelper.syncWithWalletsAndAssets(_wallets.value, _assets.value)
        persistWalletsAndSync(listOf(stamped))
        persistCustomLogoFromIconPath(stamped.iconName, now)
        persistTransactionsAndSync(changedTxs)
        saveToDisk()
    }

    fun deleteWallet(walletId: String) {
        _wallets.update { list -> list.filterNot { it.id == walletId } }
        IndonesianInstitutionHelper.syncWithWalletsAndAssets(_wallets.value, _assets.value)
        softDeleteWalletAndSync(walletId)
        saveToDisk()
    }

    fun reorderWallets(newWallets: List<AccountWallet>) {
        val now = System.currentTimeMillis()
        val stamped = newWallets.map { it.copy(updatedAt = now) }
        _wallets.value = stamped
        IndonesianInstitutionHelper.syncWithWalletsAndAssets(stamped, _assets.value)
        persistWalletsAndSync(stamped, stamped)
        saveToDisk()
    }

    fun reorderAssets(newAssets: List<AssetItem>) {
        val now = System.currentTimeMillis()
        val stamped = newAssets.map { it.copy(updatedAt = now) }
        _assets.value = stamped
        IndonesianInstitutionHelper.syncWithWalletsAndAssets(_wallets.value, stamped)
        persistAssetsAndSync(stamped, stamped)
        saveToDisk()
    }

    fun reorderDebts(newDebts: List<DebtItem>) {
        val now = System.currentTimeMillis()
        val stamped = newDebts.map { it.copy(updatedAt = now) }
        _debts.value = stamped
        persistDebtsAndSync(stamped, stamped)
        saveToDisk()
    }

    fun addAsset(asset: AssetItem) {
        val now = System.currentTimeMillis()
        val stamped = asset.copy(updatedAt = now, isDeleted = false)
        _assets.update { it + stamped }
        IndonesianInstitutionHelper.registerCustomLogo(stamped.name, stamped.iconUri)
        IndonesianInstitutionHelper.syncWithWalletsAndAssets(_wallets.value, _assets.value)
        persistAssetsAndSync(listOf(stamped))
        persistCustomLogoFromIconPath(stamped.iconUri, now)
        saveToDisk()
    }

    fun updateAsset(asset: AssetItem) {
        val now = System.currentTimeMillis()
        val oldAsset = _assets.value.find { it.id == asset.id }
        val syncedAsset = if (asset.isStock && asset.lotCount > 0.0 && asset.currentPrice > 0.0) {
            asset.copy(value = asset.lotCount * 100.0 * asset.currentPrice, updatedAt = now, isDeleted = false)
        } else {
            asset.copy(updatedAt = now, isDeleted = false)
        }
        _assets.update { list -> list.map { if (it.id == syncedAsset.id) syncedAsset else it } }
        IndonesianInstitutionHelper.registerCustomLogo(syncedAsset.name, syncedAsset.iconUri)
        val changedTxs = mutableListOf<Transaction>()
        if (oldAsset != null && oldAsset.name != syncedAsset.name) {
            IndonesianInstitutionHelper.registerCustomLogo(oldAsset.name, syncedAsset.iconUri)
            _transactions.update { list ->
                list.map { tx ->
                    if (tx.targetAssetId == syncedAsset.id || tx.targetAssetName.equals(oldAsset.name, ignoreCase = true)) {
                        tx.copy(targetAssetName = syncedAsset.name, updatedAt = now).also { changedTxs.add(it) }
                    } else tx
                }
            }
        }
        IndonesianInstitutionHelper.syncWithWalletsAndAssets(_wallets.value, _assets.value)
        persistAssetsAndSync(listOf(syncedAsset))
        persistCustomLogoFromIconPath(syncedAsset.iconUri, now)
        persistTransactionsAndSync(changedTxs)
        saveToDisk()
    }

    fun updateStockCurrentPrice(assetId: String, currentPrice: Double, dailyChange: Double = 0.0, dailyChangePercent: Double = 0.0) {
        val now = System.currentTimeMillis()
        val changed = mutableListOf<AssetItem>()
        _assets.update { list ->
            list.map { asset ->
                if (asset.id == assetId) {
                    val updatedVal = if (asset.isStock && asset.lotCount > 0.0) asset.lotCount * 100.0 * currentPrice else asset.value
                    asset.copy(
                        currentPrice = currentPrice,
                        dailyPriceChange = dailyChange,
                        dailyPriceChangePercent = dailyChangePercent,
                        lastPriceUpdate = now,
                        value = updatedVal,
                        updatedAt = now
                    ).also { changed.add(it) }
                } else asset
            }
        }
        persistAssetsAndSync(changed)
        saveToDisk()
    }

    suspend fun refreshStockPricesFromTradingView(): Map<String, TradingViewQuote> {
        val stockAssets = _assets.value.filter { it.isStock || it.tickerSymbol.isNotBlank() }
        if (stockAssets.isEmpty()) return emptyMap()

        val symbols = stockAssets.map { it.resolvedTicker.ifBlank { "IDX:BBRI" } }.distinct()
        val quotes = TradingViewHelper.fetchLiveQuotes(symbols)
        if (quotes.isEmpty()) return quotes

        var hasChanged = false
        val now = System.currentTimeMillis()
        val changedAssets = mutableListOf<AssetItem>()
        val updatedAssets = _assets.value.map { asset ->
            if (asset.isStock || asset.tickerSymbol.isNotBlank()) {
                val ticker = asset.resolvedTicker
                val quote = quotes[ticker]
                    ?: quotes[TradingViewHelper.formatSymbol(ticker)]
                    ?: quotes[TradingViewHelper.extractTickerCode(asset.name)]
                if (quote != null && quote.closePrice > 0.0) {
                    val isPriceChanged = asset.currentPrice != quote.closePrice ||
                            asset.dailyPriceChange != quote.changeAbsolute ||
                            asset.dailyPriceChangePercent != quote.changePercent
                    val updatedVal = if (asset.lotCount > 0.0) asset.lotCount * 100.0 * quote.closePrice else asset.value
                    val updatedPerf = if (asset.averagePrice > 0.0) {
                        ((quote.closePrice - asset.averagePrice) / asset.averagePrice) * 100.0
                    } else quote.changePercent
                    val newAsset = asset.copy(
                        currentPrice = quote.closePrice,
                        dailyPriceChange = quote.changeAbsolute,
                        dailyPriceChangePercent = quote.changePercent,
                        lastPriceUpdate = quote.timestamp,
                        performancePercent = updatedPerf,
                        value = updatedVal,
                        updatedAt = if (isPriceChanged) now else asset.updatedAt
                    )
                    if (isPriceChanged) {
                        hasChanged = true
                        changedAssets.add(newAsset)
                    }
                    newAsset
                } else asset
            } else asset
        }

        if (hasChanged) {
            _assets.value = updatedAssets
            persistAssetsAndSync(changedAssets, updatedAssets)
            saveToDisk()
        }
        return quotes
    }

    fun deleteAsset(assetId: String) {
        _assets.update { list -> list.filterNot { it.id == assetId } }
        IndonesianInstitutionHelper.syncWithWalletsAndAssets(_wallets.value, _assets.value)
        softDeleteAssetAndSync(assetId)
        saveToDisk()
    }

    data class ParsedDate(val day: Int, val month: Int, val year: Int, val dayOfWeek: String)

    fun parseDateString(dateStr: String): ParsedDate {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)
        val dayNames = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")

        val clean = dateStr.split("·").firstOrNull()?.trim() ?: dateStr.trim()
        val tokens = when {
            clean.contains("/") -> clean.split("/").map { it.trim() }
            clean.contains("-") -> clean.split("-").map { it.trim() }
            else -> clean.split(" ").filter { it.isNotBlank() }
        }

        var day = currentDay
        var month = currentMonth
        var year = currentYear

        if (clean.contains("/") || (clean.contains("-") && (tokens.firstOrNull()?.length ?: 0) != 4)) {
            day = tokens.getOrNull(0)?.toIntOrNull() ?: currentDay
            month = tokens.getOrNull(1)?.toIntOrNull() ?: currentMonth
            year = tokens.getOrNull(2)?.toIntOrNull() ?: currentYear
        } else if (clean.contains("-") && (tokens.firstOrNull()?.length ?: 0) == 4) {
            year = tokens.getOrNull(0)?.toIntOrNull() ?: currentYear
            month = tokens.getOrNull(1)?.toIntOrNull() ?: currentMonth
            day = tokens.getOrNull(2)?.toIntOrNull() ?: currentDay
        } else {
            day = tokens.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: currentDay
            val monthToken = tokens.getOrNull(1)?.lowercase() ?: ""
            month = when {
                monthToken.startsWith("jan") -> 1
                monthToken.startsWith("feb") -> 2
                monthToken.startsWith("mar") -> 3
                monthToken.startsWith("apr") -> 4
                monthToken.startsWith("mei") || monthToken.startsWith("may") -> 5
                monthToken.startsWith("jun") -> 6
                monthToken.startsWith("jul") -> 7
                monthToken.startsWith("agu") || monthToken.startsWith("aug") -> 8
                monthToken.startsWith("sep") -> 9
                monthToken.startsWith("okt") || monthToken.startsWith("oct") -> 10
                monthToken.startsWith("nov") -> 11
                monthToken.startsWith("des") || monthToken.startsWith("dec") -> 12
                else -> currentMonth
            }
            year = tokens.getOrNull(2)?.filter { it.isDigit() }?.toIntOrNull() ?: currentYear
        }

        val testCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
        }
        val dayOfWeek = dayNames[(testCal.get(Calendar.DAY_OF_WEEK) - 1 + 7) % 7]
        return ParsedDate(day, month, year, dayOfWeek)
    }

    fun syncDebtPaymentsWithTransactions(defaultTimestamp: Long = System.currentTimeMillis()) {
        val currentTxs = _transactions.value.toMutableList()
        var modified = false
        val newlyAddedTxs = mutableListOf<Transaction>()
        val existingTxIds = currentTxs.map { it.id }.toSet()

        for (debt in _debts.value) {
            val isLent = debt.type == DebtType.LENT

            for (rec in debt.records) {
                if (rec.sourceWalletName.isBlank()) continue
                val txId = "debt_rec_${rec.id}"
                val alreadyExists = existingTxIds.contains(txId) ||
                                    currentTxs.any { it.id == "tx_${rec.id}" || it.id == rec.id }
                if (!alreadyExists) {
                    val parsed = parseDateString(rec.date)
                    val wallet = rec.sourceWalletName
                    val recType = rec.type

                    val (txType, txCat, txTitle) = when (recType) {
                        "Pembayaran" -> {
                            if (isLent) {
                                Triple(
                                    TransactionType.INCOME,
                                    "Pendapatan",
                                    if (rec.note.isNotBlank()) rec.note else "Pelunasan Piutang: ${debt.name}"
                                )
                            } else {
                                Triple(
                                    TransactionType.EXPENSE,
                                    "Pembayaran Utang",
                                    if (rec.note.isNotBlank()) rec.note else "Pembayaran Utang: ${debt.name}"
                                )
                            }
                        }
                        "Pinjam Lagi" -> {
                            if (isLent) {
                                Triple(
                                    TransactionType.EXPENSE,
                                    "Pengeluaran",
                                    if (rec.note.isNotBlank()) rec.note else "Pemberian Pinjaman: ${debt.name}"
                                )
                            } else {
                                Triple(
                                    TransactionType.INCOME,
                                    "Pendapatan",
                                    if (rec.note.isNotBlank()) rec.note else "Penerimaan Pinjaman: ${debt.name}"
                                )
                            }
                        }
                        else -> {
                            Triple(
                                TransactionType.EXPENSE,
                                "Bunga Utang",
                                if (rec.note.isNotBlank()) rec.note else "Bunga Utang: ${debt.name}"
                            )
                        }
                    }

                    val tx = Transaction(
                        id = txId,
                        title = txTitle,
                        category = txCat,
                        amount = rec.amount,
                        type = txType,
                        accountName = wallet,
                        toAccountName = if (!isLent && recType == "Pembayaran") debt.name else "",
                        dayOfMonth = parsed.day,
                        monthIndex = parsed.month,
                        year = parsed.year,
                        dayOfWeekName = parsed.dayOfWeek,
                        note = rec.note,
                        time = rec.time.ifBlank { "12:00" },
                        targetDebtId = debt.id,
                        targetDebtName = debt.name,
                        updatedAt = defaultTimestamp
                    )
                    currentTxs.add(tx)
                    newlyAddedTxs.add(tx)
                    modified = true
                }
            }
        }

        if (modified) {
            _transactions.value = currentTxs.sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator)
            if (initialSeedComplete.isCompleted && newlyAddedTxs.isNotEmpty()) {
                persistTransactionsAndSync(newlyAddedTxs)
            }
            saveToDisk()
        }
    }

    fun addAssetRecord(assetId: String, record: AssetRecord, updateWalletBalance: Boolean = true) {
        val now = System.currentTimeMillis()
        val modifiedAssets = mutableListOf<AssetItem>()
        _assets.update { list ->
            list.map { asset ->
                if (asset.id == assetId) {
                    val newRecords = listOf(record) + asset.records
                    val updated = if (asset.isStock && record.lotCount > 0.0) {
                        val recordPrice = if (record.pricePerUnit > 0.0) {
                            record.pricePerUnit
                        } else {
                            record.amount / (record.lotCount * 100.0)
                        }
                        if (record.isIncrease) {
                            val newLot = asset.lotCount + record.lotCount
                            val currentAvg = if (asset.averagePrice > 0.0) asset.averagePrice else recordPrice
                            val newAvg = if (newLot > 0.0) {
                                ((asset.lotCount * currentAvg) + (record.lotCount * recordPrice)) / newLot
                            } else recordPrice
                            val currentP = if (asset.currentPrice > 0.0) asset.currentPrice else newAvg
                            val newValue = newLot * 100.0 * currentP
                            asset.copy(
                                lotCount = newLot,
                                averagePrice = newAvg,
                                value = newValue,
                                records = newRecords,
                                updatedAt = now
                            )
                        } else {
                            val newLot = (asset.lotCount - record.lotCount).coerceAtLeast(0.0)
                            val currentP = if (asset.currentPrice > 0.0) asset.currentPrice else asset.averagePrice
                            val newValue = newLot * 100.0 * currentP
                            asset.copy(
                                lotCount = newLot,
                                value = newValue,
                                records = newRecords,
                                updatedAt = now
                            )
                        }
                    } else {
                        val newValue = if (record.isIncrease) asset.value + record.amount else (asset.value - record.amount).coerceAtLeast(0.0)
                        asset.copy(value = newValue, records = newRecords, updatedAt = now)
                    }
                    modifiedAssets.add(updated)
                    updated
                } else asset
            }
        }
        val modifiedWallets = mutableListOf<AccountWallet>()
        var createdTx: Transaction? = null
        if (updateWalletBalance && record.sourceWalletName.isNotBlank()) {
            _wallets.update { list ->
                list.map { wallet ->
                    if (wallet.name.equals(record.sourceWalletName, ignoreCase = true)) {
                        val newBal = if (record.isIncrease) wallet.balance - record.amount else wallet.balance + record.amount
                        wallet.copy(balance = newBal, lastActivityTime = "Baru saja", updatedAt = now)
                            .also { modifiedWallets.add(it) }
                    } else wallet
                }
            }
            // Sinkronkan ke riwayat pencatatan transaksi utama (TransactionScreen)
            val targetAsset = _assets.value.find { it.id == assetId }
            val assetName = targetAsset?.name ?: "Aset"
            val parsed = parseDateString(record.date)
            val tx = Transaction(
                id = "asset_rec_${record.id}",
                title = if (record.note.isNotBlank()) record.note else "Transfer ke $assetName",
                category = "Asset",
                amount = record.amount,
                type = TransactionType.TRANSFER,
                accountName = record.sourceWalletName,
                dayOfMonth = parsed.day,
                monthIndex = parsed.month,
                year = parsed.year,
                dayOfWeekName = parsed.dayOfWeek,
                note = record.note,
                time = record.time.ifBlank { "12:00" },
                targetAssetId = assetId,
                targetAssetName = assetName,
                updatedAt = now
            )
            createdTx = tx
            _transactions.update { list -> listOf(tx) + list.filterNot { it.id == tx.id } }
        }
        persistAssetsAndSync(modifiedAssets)
        persistWalletsAndSync(modifiedWallets)
        createdTx?.let { persistTransactionAndSync(it) }
        saveToDisk()
    }

    fun updateAssetRecord(assetId: String, oldRecord: AssetRecord, updatedRecord: AssetRecord, updateWalletBalance: Boolean = true) {
        val now = System.currentTimeMillis()
        val modifiedAssets = mutableListOf<AssetItem>()
        _assets.update { list ->
            list.map { asset ->
                if (asset.id == assetId) {
                    val newRecords = asset.records.map { if (it.id == updatedRecord.id) updatedRecord else it }
                    val revertedValue = if (oldRecord.isIncrease) asset.value - oldRecord.amount else asset.value + oldRecord.amount
                    val finalValue = if (updatedRecord.isIncrease) revertedValue + updatedRecord.amount else (revertedValue - updatedRecord.amount).coerceAtLeast(0.0)
                    asset.copy(value = finalValue, records = newRecords, updatedAt = now)
                        .also { modifiedAssets.add(it) }
                } else asset
            }
        }
        val modifiedWallets = mutableListOf<AccountWallet>()
        var updatedTx: Transaction? = null
        var deletedTxId: String? = null
        if (updateWalletBalance) {
            if (oldRecord.sourceWalletName.isNotBlank() || updatedRecord.sourceWalletName.isNotBlank()) {
                _wallets.update { list ->
                    list.map { wallet ->
                        var b = wallet.balance
                        if (oldRecord.sourceWalletName.isNotBlank() && wallet.name.equals(oldRecord.sourceWalletName, ignoreCase = true)) {
                            b = if (oldRecord.isIncrease) b + oldRecord.amount else b - oldRecord.amount
                        }
                        if (updatedRecord.sourceWalletName.isNotBlank() && wallet.name.equals(updatedRecord.sourceWalletName, ignoreCase = true)) {
                            b = if (updatedRecord.isIncrease) b - updatedRecord.amount else b + updatedRecord.amount
                        }
                        if (b != wallet.balance) {
                            wallet.copy(balance = b, lastActivityTime = "Baru saja", updatedAt = now)
                                .also { modifiedWallets.add(it) }
                        } else wallet
                    }
                }
            }
            // Update juga di riwayat pencatatan transaksi utama
            val targetAsset = _assets.value.find { it.id == assetId }
            val assetName = targetAsset?.name ?: "Aset"
            if (updatedRecord.sourceWalletName.isNotBlank()) {
                val parsed = parseDateString(updatedRecord.date)
                val tx = Transaction(
                    id = "asset_rec_${updatedRecord.id}",
                    title = if (updatedRecord.note.isNotBlank()) updatedRecord.note else "Transfer ke $assetName",
                    category = "Asset",
                    amount = updatedRecord.amount,
                    type = TransactionType.TRANSFER,
                    accountName = updatedRecord.sourceWalletName,
                    dayOfMonth = parsed.day,
                    monthIndex = parsed.month,
                    year = parsed.year,
                    dayOfWeekName = parsed.dayOfWeek,
                    note = updatedRecord.note,
                    time = updatedRecord.time.ifBlank { "12:00" },
                    targetAssetId = assetId,
                    targetAssetName = assetName,
                    updatedAt = now
                )
                updatedTx = tx
                _transactions.update { list ->
                    val exists = list.any { it.id == tx.id }
                    if (exists) list.map { if (it.id == tx.id) tx else it } else listOf(tx) + list
                }
            } else {
                deletedTxId = "asset_rec_${updatedRecord.id}"
                _transactions.update { list -> list.filterNot { it.id == deletedTxId } }
            }
        }
        persistAssetsAndSync(modifiedAssets)
        persistWalletsAndSync(modifiedWallets)
        updatedTx?.let { persistTransactionAndSync(it) }
        deletedTxId?.let { softDeleteTransactionsAndSync(listOf(it), now) }
        saveToDisk()
    }

    fun deleteAssetRecord(assetId: String, recordId: String, revertWalletBalance: Boolean = true) {
        val now = System.currentTimeMillis()
        var targetRecord: AssetRecord? = null
        val modifiedAssets = mutableListOf<AssetItem>()
        _assets.update { list ->
            list.map { asset ->
                if (asset.id == assetId) {
                    targetRecord = asset.records.find { it.id == recordId }
                    val rec = targetRecord
                    val newRecords = asset.records.filterNot { it.id == recordId }
                    val updated = if (asset.isStock && rec != null && rec.lotCount > 0.0) {
                        val newLot = if (rec.isIncrease) {
                            (asset.lotCount - rec.lotCount).coerceAtLeast(0.0)
                        } else {
                            asset.lotCount + rec.lotCount
                        }
                        val currentP = if (asset.currentPrice > 0.0) asset.currentPrice else asset.averagePrice
                        val finalValue = newLot * 100.0 * currentP
                        asset.copy(lotCount = newLot, value = finalValue, records = newRecords, updatedAt = now)
                    } else {
                        val finalValue = if (rec != null) {
                            if (rec.isIncrease) (asset.value - rec.amount).coerceAtLeast(0.0) else asset.value + rec.amount
                        } else asset.value
                        asset.copy(value = finalValue, records = newRecords, updatedAt = now)
                    }
                    modifiedAssets.add(updated)
                    updated
                } else asset
            }
        }
        val rec = targetRecord
        val modifiedWallets = mutableListOf<AccountWallet>()
        var deletedTxIds: List<String> = emptyList()
        if (revertWalletBalance && rec != null && rec.sourceWalletName.isNotBlank()) {
            _wallets.update { list ->
                list.map { wallet ->
                    if (wallet.name.equals(rec.sourceWalletName, ignoreCase = true)) {
                        val b = if (rec.isIncrease) wallet.balance + rec.amount else wallet.balance - rec.amount
                        wallet.copy(balance = b, updatedAt = now).also { modifiedWallets.add(it) }
                    } else wallet
                }
            }
            // Hapus dari riwayat pencatatan transaksi utama
            deletedTxIds = listOf("asset_rec_$recordId", "tx_$recordId")
            _transactions.update { list ->
                list.filterNot { it.id == "asset_rec_$recordId" || it.id == "tx_$recordId" }
            }
        }
        persistAssetsAndSync(modifiedAssets)
        persistWalletsAndSync(modifiedWallets)
        softDeleteTransactionsAndSync(deletedTxIds, now)
        saveToDisk()
    }

    fun addDebt(debt: DebtItem, depositToWalletName: String? = null) {
        val now = System.currentTimeMillis()
        val debtToAdd = debt.copy(updatedAt = now, isDeleted = false)
        _debts.update { it + debtToAdd }
        val modifiedWallets = mutableListOf<AccountWallet>()
        val createdTxs = mutableListOf<Transaction>()
        if (!depositToWalletName.isNullOrBlank()) {
            val parsed = parseDateString(debtToAdd.startDate)
            if (debtToAdd.type == DebtType.BORROWED) {
                _wallets.update { list ->
                    list.map { w ->
                        if (w.name.equals(depositToWalletName, ignoreCase = true)) {
                            w.copy(balance = w.balance + debtToAdd.totalAmount, updatedAt = now)
                                .also { modifiedWallets.add(it) }
                        } else w
                    }
                }
                val initTx = Transaction(
                    id = "debt_init_${debtToAdd.id}",
                    title = "Penerimaan Pinjaman: ${debtToAdd.name}",
                    category = "Pendapatan",
                    amount = debtToAdd.totalAmount,
                    type = TransactionType.INCOME,
                    accountName = depositToWalletName,
                    dayOfMonth = parsed.day,
                    monthIndex = parsed.month,
                    year = parsed.year,
                    dayOfWeekName = parsed.dayOfWeek,
                    note = "Pencairan dana utang ${debtToAdd.name}",
                    targetDebtId = debtToAdd.id,
                    targetDebtName = debtToAdd.name,
                    updatedAt = now
                )
                createdTxs.add(initTx)
                _transactions.update { txs -> listOf(initTx) + txs }
            } else {
                _wallets.update { list ->
                    list.map { w ->
                        if (w.name.equals(depositToWalletName, ignoreCase = true)) {
                            w.copy(balance = (w.balance - debtToAdd.totalAmount).coerceAtLeast(0.0), updatedAt = now)
                                .also { modifiedWallets.add(it) }
                        } else w
                    }
                }
                val initTx = Transaction(
                    id = "debt_init_${debtToAdd.id}",
                    title = "Pemberian Pinjaman: ${debtToAdd.name}",
                    category = "Transfer",
                    amount = debtToAdd.totalAmount,
                    type = TransactionType.EXPENSE,
                    accountName = depositToWalletName,
                    dayOfMonth = parsed.day,
                    monthIndex = parsed.month,
                    year = parsed.year,
                    dayOfWeekName = parsed.dayOfWeek,
                    note = "Pemberian pinjaman untuk ${debtToAdd.name}",
                    targetDebtId = debtToAdd.id,
                    targetDebtName = debtToAdd.name,
                    updatedAt = now
                )
                createdTxs.add(initTx)
                _transactions.update { txs -> listOf(initTx) + txs }
            }
        }
        persistDebtsAndSync(listOf(debtToAdd))
        persistCustomLogoFromIconPath(debtToAdd.institutionLogo, now)
        persistWalletsAndSync(modifiedWallets)
        persistTransactionsAndSync(createdTxs)
        syncDebtPaymentsWithTransactions(defaultTimestamp = now)
        saveToDisk()
    }

    fun updateDebt(debt: DebtItem) {
        val now = System.currentTimeMillis()
        val updatedDebt = debt.copy(updatedAt = now, isDeleted = false)
        _debts.update { list -> list.map { if (it.id == updatedDebt.id) updatedDebt else it } }
        persistDebtsAndSync(listOf(updatedDebt))
        persistCustomLogoFromIconPath(updatedDebt.institutionLogo, now)
        saveToDisk()
    }

    fun markDebtFullyPaid(debtId: String) {
        val targetDebt = _debts.value.firstOrNull { it.id == debtId } ?: return
        val now = System.currentTimeMillis()
        val remaining = (targetDebt.totalAmount - targetDebt.paidAmount).coerceAtLeast(0.0)
        val isLent = targetDebt.type == DebtType.LENT
        val linkedWallet = targetDebt.linkedWalletName.ifBlank { _wallets.value.firstOrNull()?.name ?: "" }

        val cal = Calendar.getInstance()
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthIndex = cal.get(Calendar.MONTH) + 1
        val monthStr = monthNames.getOrElse(cal.get(Calendar.MONTH)) { "Nov" }
        val year = cal.get(Calendar.YEAR)
        val dateStr = "$day $monthStr $year"
        val timeStr = String.format("%02d.%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        val dayNames = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
        val dayOfWeek = dayNames[(cal.get(Calendar.DAY_OF_WEEK) - 1 + 7) % 7]

        val newRecordId = java.util.UUID.randomUUID().toString()
        val newRecord = if (remaining > 0) {
            DebtRecord(
                id = newRecordId,
                debtId = targetDebt.id,
                type = "Pembayaran",
                amount = remaining,
                date = dateStr,
                time = timeStr,
                note = "Pelunasan penuh",
                sourceWalletName = linkedWallet
            )
        } else null

        val modifiedDebts = mutableListOf<DebtItem>()
        _debts.update { list ->
            list.map { debt ->
                if (debt.id == debtId) {
                    val updatedRecords = if (newRecord != null) debt.records + newRecord else debt.records
                    debt.copy(paidAmount = debt.totalAmount, records = updatedRecords, updatedAt = now)
                        .also { modifiedDebts.add(it) }
                } else debt
            }
        }

        val modifiedWallets = mutableListOf<AccountWallet>()
        var createdTx: Transaction? = null
        if (remaining > 0 && linkedWallet.isNotBlank()) {
            if (isLent) {
                // Uang yang dipinjamkan telah lunas dibayar kembali -> masuk ke saldo akun penerima (bertambah)
                _wallets.update { list ->
                    list.map { w ->
                        if (w.name.equals(linkedWallet, ignoreCase = true)) {
                            w.copy(balance = w.balance + remaining, updatedAt = now)
                                .also { modifiedWallets.add(it) }
                        } else w
                    }
                }
                val tx = Transaction(
                    id = "debt_rec_$newRecordId",
                    title = "Pelunasan Piutang: ${targetDebt.name}",
                    category = "Pendapatan",
                    amount = remaining,
                    type = TransactionType.INCOME,
                    accountName = linkedWallet,
                    dayOfMonth = day,
                    monthIndex = monthIndex,
                    year = year,
                    dayOfWeekName = dayOfWeek,
                    note = "Pelunasan penuh piutang ${targetDebt.name}",
                    time = timeStr,
                    targetDebtId = debtId,
                    targetDebtName = targetDebt.name,
                    updatedAt = now
                )
                createdTx = tx
                _transactions.update { txs -> listOf(tx) + txs }
            } else {
                // Utang yang dipinjam dilunasi oleh user -> keluar dari saldo akun (berkurang)
                _wallets.update { list ->
                    list.map { w ->
                        if (w.name.equals(linkedWallet, ignoreCase = true)) {
                            w.copy(balance = (w.balance - remaining).coerceAtLeast(0.0), updatedAt = now)
                                .also { modifiedWallets.add(it) }
                        } else w
                    }
                }
                val tx = Transaction(
                    id = "debt_rec_$newRecordId",
                    title = "Pelunasan Utang: ${targetDebt.name}",
                    category = "Pembayaran Utang",
                    amount = remaining,
                    type = TransactionType.EXPENSE,
                    accountName = linkedWallet,
                    toAccountName = targetDebt.name,
                    dayOfMonth = day,
                    monthIndex = monthIndex,
                    year = year,
                    dayOfWeekName = dayOfWeek,
                    note = "Pelunasan penuh utang ${targetDebt.name}",
                    time = timeStr,
                    targetDebtId = debtId,
                    targetDebtName = targetDebt.name,
                    updatedAt = now
                )
                createdTx = tx
                _transactions.update { txs -> listOf(tx) + txs }
            }
        }
        persistDebtsAndSync(modifiedDebts)
        persistWalletsAndSync(modifiedWallets)
        createdTx?.let { persistTransactionAndSync(it) }
        saveToDisk()
    }

    fun addDebtRecord(debtId: String, record: DebtRecord, affectWalletName: String? = null) {
        debtWriteSeq.incrementAndGet()
        walletWriteSeq.incrementAndGet()
        transactionWriteSeq.incrementAndGet()
        val now = System.currentTimeMillis()
        val modifiedDebts = mutableListOf<DebtItem>()
        _debts.update { list ->
            list.map { debt ->
                if (debt.id == debtId) {
                    val updatedRecords = debt.records + record
                    val updated = when (record.type) {
                        "Pembayaran" -> {
                            val newPaid = (debt.paidAmount + record.amount).coerceAtMost(debt.totalAmount)
                            debt.copy(paidAmount = newPaid, records = updatedRecords, updatedAt = now)
                        }
                        "Pinjam Lagi" -> {
                            val newTotal = debt.totalAmount + record.amount
                            debt.copy(totalAmount = newTotal, records = updatedRecords, updatedAt = now)
                        }
                        "Bunga" -> {
                            val newTotal = debt.totalAmount + record.amount
                            debt.copy(totalAmount = newTotal, records = updatedRecords, updatedAt = now)
                        }
                        else -> debt.copy(records = updatedRecords, updatedAt = now)
                    }
                    modifiedDebts.add(updated)
                    updated
                } else debt
            }
        }

        val d = _debts.value.firstOrNull { it.id == debtId }
        val isLent = d?.type == DebtType.LENT
        val targetWallet = if (!affectWalletName.isNullOrBlank()) affectWalletName else record.sourceWalletName
        val modifiedWallets = mutableListOf<AccountWallet>()
        var createdTx: Transaction? = null

        if (targetWallet.isNotBlank()) {
            val parsed = parseDateString(record.date)
            val recTime = record.time.ifBlank { "12:00" }

            when (record.type) {
                "Pembayaran" -> {
                    if (isLent) {
                        _wallets.update { list ->
                            list.map { w ->
                                if (w.name.equals(targetWallet, ignoreCase = true)) {
                                    w.copy(balance = w.balance + record.amount, updatedAt = now)
                                        .also { modifiedWallets.add(it) }
                                } else w
                            }
                        }
                        val newTx = Transaction(
                            id = "debt_rec_${record.id}",
                            title = if (record.note.isNotBlank()) record.note else "Pelunasan Piutang: ${d?.name ?: "Piutang"}",
                            category = "Pendapatan",
                            amount = record.amount,
                            type = TransactionType.INCOME,
                            accountName = targetWallet,
                            dayOfMonth = parsed.day,
                            monthIndex = parsed.month,
                            year = parsed.year,
                            dayOfWeekName = parsed.dayOfWeek,
                            note = record.note.ifBlank { "Pembayaran piutang diterima" },
                            time = recTime,
                            targetDebtId = debtId,
                            targetDebtName = d?.name ?: "",
                            updatedAt = now
                        )
                        createdTx = newTx
                        _transactions.update { txs -> (listOf(newTx) + txs.filterNot { it.id == newTx.id }).sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator) }
                    } else {
                        _wallets.update { list ->
                            list.map { w ->
                                if (w.name.equals(targetWallet, ignoreCase = true)) {
                                    w.copy(balance = (w.balance - record.amount).coerceAtLeast(0.0), updatedAt = now)
                                        .also { modifiedWallets.add(it) }
                                } else w
                            }
                        }
                        val newTx = Transaction(
                            id = "debt_rec_${record.id}",
                            title = if (record.note.isNotBlank()) record.note else "Pembayaran Utang: ${d?.name ?: "Utang"}",
                            category = "Pembayaran Utang",
                            amount = record.amount,
                            type = TransactionType.EXPENSE,
                            accountName = targetWallet,
                            toAccountName = d?.name ?: "",
                            dayOfMonth = parsed.day,
                            monthIndex = parsed.month,
                            year = parsed.year,
                            dayOfWeekName = parsed.dayOfWeek,
                            note = record.note.ifBlank { "Pembayaran cicilan utang" },
                            time = recTime,
                            targetDebtId = debtId,
                            targetDebtName = d?.name ?: "",
                            updatedAt = now
                        )
                        createdTx = newTx
                        _transactions.update { txs -> (listOf(newTx) + txs.filterNot { it.id == newTx.id }).sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator) }
                    }
                }
                "Pinjam Lagi" -> {
                    if (isLent) {
                        _wallets.update { list ->
                            list.map { w ->
                                if (w.name.equals(targetWallet, ignoreCase = true)) {
                                    w.copy(balance = (w.balance - record.amount).coerceAtLeast(0.0), updatedAt = now)
                                        .also { modifiedWallets.add(it) }
                                } else w
                            }
                        }
                        val newTx = Transaction(
                            id = "debt_rec_${record.id}",
                            title = if (record.note.isNotBlank()) record.note else "Pinjaman Tambahan Diberikan: ${d?.name ?: "Piutang"}",
                            category = "Pengeluaran",
                            amount = record.amount,
                            type = TransactionType.EXPENSE,
                            accountName = targetWallet,
                            dayOfMonth = parsed.day,
                            monthIndex = parsed.month,
                            year = parsed.year,
                            dayOfWeekName = parsed.dayOfWeek,
                            note = record.note.ifBlank { "Pemberian pinjaman tambahan" },
                            time = recTime,
                            targetDebtId = debtId,
                            targetDebtName = d?.name ?: "",
                            updatedAt = now
                        )
                        createdTx = newTx
                        _transactions.update { txs -> (listOf(newTx) + txs.filterNot { it.id == newTx.id }).sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator) }
                    } else {
                        _wallets.update { list ->
                            list.map { w ->
                                if (w.name.equals(targetWallet, ignoreCase = true)) {
                                    w.copy(balance = w.balance + record.amount, updatedAt = now)
                                        .also { modifiedWallets.add(it) }
                                } else w
                            }
                        }
                        val newTx = Transaction(
                            id = "debt_rec_${record.id}",
                            title = if (record.note.isNotBlank()) record.note else "Penerimaan Pinjaman Tambahan: ${d?.name ?: "Utang"}",
                            category = "Pendapatan",
                            amount = record.amount,
                            type = TransactionType.INCOME,
                            accountName = targetWallet,
                            dayOfMonth = parsed.day,
                            monthIndex = parsed.month,
                            year = parsed.year,
                            dayOfWeekName = parsed.dayOfWeek,
                            note = record.note.ifBlank { "Penerimaan pinjaman tambahan" },
                            time = recTime,
                            targetDebtId = debtId,
                            targetDebtName = d?.name ?: "",
                            updatedAt = now
                        )
                        createdTx = newTx
                        _transactions.update { txs -> (listOf(newTx) + txs.filterNot { it.id == newTx.id }).sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator) }
                    }
                }
            }
        }
        persistDebtsAndSync(modifiedDebts)
        persistWalletsAndSync(modifiedWallets)
        createdTx?.let { persistTransactionAndSync(it) }
        saveToDisk()
    }

    fun updateDebtRecord(debtId: String, updatedRecord: DebtRecord) {
        debtWriteSeq.incrementAndGet()
        walletWriteSeq.incrementAndGet()
        transactionWriteSeq.incrementAndGet()
        val now = System.currentTimeMillis()
        val oldDebt = _debts.value.firstOrNull { it.id == debtId }
        val isLent = oldDebt?.type == DebtType.LENT
        val oldRecord = oldDebt?.records?.firstOrNull { it.id == updatedRecord.id }
        val modifiedDebts = mutableListOf<DebtItem>()
        _debts.update { list ->
            list.map { debt ->
                if (debt.id == debtId) {
                    val updatedRecords = debt.records.map { if (it.id == updatedRecord.id) updatedRecord else it }
                    val totalPaid = updatedRecords.filter { it.type == "Pembayaran" }.sumOf { it.amount }
                    val oldAddedToTotal = if (oldRecord != null && (oldRecord.type == "Pinjam Lagi" || oldRecord.type == "Bunga")) oldRecord.amount else 0.0
                    val newAddedToTotal = if (updatedRecord.type == "Pinjam Lagi" || updatedRecord.type == "Bunga") updatedRecord.amount else 0.0
                    val newTotalAmount = (debt.totalAmount - oldAddedToTotal + newAddedToTotal).coerceAtLeast(0.0)
                    debt.copy(totalAmount = newTotalAmount, paidAmount = totalPaid, records = updatedRecords, updatedAt = now)
                        .also { modifiedDebts.add(it) }
                } else debt
            }
        }
        val modifiedWallets = mutableListOf<AccountWallet>()
        val modifiedTxs = mutableListOf<Transaction>()
        var deletedTxIds: List<String> = emptyList()

        // Adjust wallet balances if oldRecord or updatedRecord had a sourceWalletName
        if (oldRecord != null) {
            val oldWallet = oldRecord.sourceWalletName
            val newWallet = updatedRecord.sourceWalletName

            if (oldWallet.isNotBlank()) {
                _wallets.update { list ->
                    list.map { w ->
                        if (w.name.equals(oldWallet, ignoreCase = true)) {
                            val revertedBal = when (oldRecord.type) {
                                "Pembayaran" -> if (isLent) (w.balance - oldRecord.amount).coerceAtLeast(0.0) else w.balance + oldRecord.amount
                                "Pinjam Lagi" -> if (isLent) w.balance + oldRecord.amount else (w.balance - oldRecord.amount).coerceAtLeast(0.0)
                                else -> w.balance
                            }
                            w.copy(balance = revertedBal, updatedAt = now)
                        } else w
                    }
                }
            }

            if (newWallet.isNotBlank()) {
                _wallets.update { list ->
                    list.map { w ->
                        if (w.name.equals(newWallet, ignoreCase = true)) {
                            val newBal = when (updatedRecord.type) {
                                "Pembayaran" -> if (isLent) w.balance + updatedRecord.amount else (w.balance - updatedRecord.amount).coerceAtLeast(0.0)
                                "Pinjam Lagi" -> if (isLent) (w.balance - updatedRecord.amount).coerceAtLeast(0.0) else w.balance + updatedRecord.amount
                                else -> w.balance
                            }
                            w.copy(balance = newBal, updatedAt = now)
                        } else w
                    }
                }
            }

            if (oldWallet.isNotBlank() || newWallet.isNotBlank()) {
                _wallets.value.filter {
                    it.name.equals(oldWallet, ignoreCase = true) || it.name.equals(newWallet, ignoreCase = true)
                }.forEach { modifiedWallets.add(it) }
            }

            val txId = "debt_rec_${updatedRecord.id}"
            if (newWallet.isBlank() || (updatedRecord.type != "Pembayaran" && updatedRecord.type != "Pinjam Lagi")) {
                deletedTxIds = listOf(txId, "tx_${updatedRecord.id}", updatedRecord.id)
                _transactions.update { txs ->
                    txs.filterNot { it.id == txId || it.id == "tx_${updatedRecord.id}" || it.id == updatedRecord.id }
                }
            } else {
                val parsed = parseDateString(updatedRecord.date)
                val d = _debts.value.firstOrNull { it.id == debtId }

                _transactions.update { txs ->
                    val exists = txs.any { it.id == txId || it.id == "tx_${updatedRecord.id}" }
                    val newType = if (isLent) {
                        if (updatedRecord.type == "Pembayaran") TransactionType.INCOME else TransactionType.EXPENSE
                    } else {
                        if (updatedRecord.type == "Pembayaran") TransactionType.EXPENSE else TransactionType.INCOME
                    }
                    val newCategory = if (isLent) {
                        if (updatedRecord.type == "Pembayaran") "Pendapatan" else "Pengeluaran"
                    } else {
                        if (updatedRecord.type == "Pembayaran") "Pembayaran Utang" else "Pendapatan"
                    }
                    val newTitle = if (updatedRecord.note.isNotBlank()) updatedRecord.note
                                   else if (updatedRecord.type == "Pinjam Lagi") {
                                       if (isLent) "Pinjaman Tambahan Diberikan: ${d?.name ?: ""}"
                                       else "Penerimaan Pinjaman Tambahan: ${d?.name ?: ""}"
                                   }
                                   else if (isLent) "Pelunasan Piutang: ${d?.name ?: ""}"
                                   else "Pembayaran Utang: ${d?.name ?: ""}"

                    if (exists) {
                        txs.map { tx ->
                            if (tx.id == txId || tx.id == "tx_${updatedRecord.id}") {
                                tx.copy(
                                    title = newTitle,
                                    amount = updatedRecord.amount,
                                    accountName = newWallet,
                                    note = updatedRecord.note.ifBlank { tx.note },
                                    type = newType,
                                    category = newCategory,
                                    dayOfMonth = parsed.day,
                                    monthIndex = parsed.month,
                                    year = parsed.year,
                                    dayOfWeekName = parsed.dayOfWeek,
                                    time = updatedRecord.time.ifBlank { tx.time },
                                    updatedAt = now
                                ).also { modifiedTxs.add(it) }
                            } else tx
                        }.sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator)
                    } else {
                        val newTx = Transaction(
                            id = txId,
                            title = newTitle,
                            category = newCategory,
                            amount = updatedRecord.amount,
                            type = newType,
                            accountName = newWallet,
                            toAccountName = if (!isLent && updatedRecord.type == "Pembayaran") (d?.name ?: "") else "",
                            dayOfMonth = parsed.day,
                            monthIndex = parsed.month,
                            year = parsed.year,
                            dayOfWeekName = parsed.dayOfWeek,
                            note = updatedRecord.note,
                            time = updatedRecord.time.ifBlank { "12:00" },
                            targetDebtId = debtId,
                            targetDebtName = d?.name ?: "",
                            updatedAt = now
                        )
                        modifiedTxs.add(newTx)
                        (listOf(newTx) + txs).sortedWith(TransactionMathHelper.transactionChronologicalDescendingComparator)
                    }
                }
            }
        }
        persistDebtsAndSync(modifiedDebts)
        persistWalletsAndSync(modifiedWallets.distinctBy { it.id })
        persistTransactionsAndSync(modifiedTxs)
        softDeleteTransactionsAndSync(deletedTxIds, now)
        saveToDisk()
    }

    fun deleteDebtRecord(debtId: String, recordId: String) {
        debtWriteSeq.incrementAndGet()
        walletWriteSeq.incrementAndGet()
        transactionWriteSeq.incrementAndGet()
        val now = System.currentTimeMillis()
        val targetDebt = _debts.value.firstOrNull { it.id == debtId }
        val rec = targetDebt?.records?.firstOrNull { it.id == recordId }
        val isLent = targetDebt?.type == DebtType.LENT

        val modifiedDebts = mutableListOf<DebtItem>()
        _debts.update { list ->
            list.map { debt ->
                if (debt.id == debtId) {
                    val updatedRecords = debt.records.filterNot { it.id == recordId }
                    val totalPaid = updatedRecords.filter { it.type == "Pembayaran" }.sumOf { it.amount }
                    debt.copy(paidAmount = totalPaid, records = updatedRecords, updatedAt = now)
                        .also { modifiedDebts.add(it) }
                } else debt
            }
        }
        val modifiedWallets = mutableListOf<AccountWallet>()
        if (rec != null && rec.sourceWalletName.isNotBlank()) {
            _wallets.update { list ->
                list.map { w ->
                    if (w.name.equals(rec.sourceWalletName, ignoreCase = true)) {
                        val revertedBal = when (rec.type) {
                            "Pembayaran" -> if (isLent) (w.balance - rec.amount).coerceAtLeast(0.0) else w.balance + rec.amount
                            "Pinjam Lagi" -> if (isLent) w.balance + rec.amount else (w.balance - rec.amount).coerceAtLeast(0.0)
                            else -> w.balance
                        }
                        w.copy(balance = revertedBal, updatedAt = now).also { modifiedWallets.add(it) }
                    } else w
                }
            }
        }
        val deletedTxIds = listOf("debt_rec_$recordId", "tx_$recordId", recordId)
        _transactions.update { txs ->
            txs.filterNot { it.id == "debt_rec_$recordId" || it.id == "tx_$recordId" || it.id == recordId }
        }
        persistDebtsAndSync(modifiedDebts)
        persistWalletsAndSync(modifiedWallets)
        softDeleteTransactionsAndSync(deletedTxIds, now)
        saveToDisk()
    }

    fun payDebt(debtId: String, amount: Double, walletName: String? = null, note: String = "Pembayaran cicilan") {
        val d = _debts.value.firstOrNull { it.id == debtId }
        val effectiveWallet = if (walletName != null) walletName
                              else d?.linkedWalletName?.ifBlank { _wallets.value.firstOrNull()?.name ?: "" } ?: ""
        val cal = Calendar.getInstance()
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthStr = monthNames.getOrElse(cal.get(Calendar.MONTH)) { "Nov" }
        val year = cal.get(Calendar.YEAR)
        val dateStr = "$day $monthStr $year"
        val timeStr = String.format("%02d.%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        addDebtRecord(
            debtId = debtId,
            record = DebtRecord(
                debtId = debtId,
                type = "Pembayaran",
                amount = amount,
                date = dateStr,
                time = timeStr,
                sourceWalletName = effectiveWallet,
                note = note
            ),
            affectWalletName = effectiveWallet.ifBlank { null }
        )
    }

    fun deleteDebt(debtId: String, deleteLinkedTransactions: Boolean = false) {
        val targetDebt = _debts.value.firstOrNull { it.id == debtId }
        val targetDebtName = targetDebt?.name ?: ""
        val recordIds = targetDebt?.records?.map { it.id }?.toSet() ?: emptySet()

        if (deleteLinkedTransactions) {
            val linkedTxs = _transactions.value.filter { tx ->
                tx.targetDebtId == debtId ||
                (targetDebtName.isNotBlank() && tx.targetDebtName.equals(targetDebtName, ignoreCase = true)) ||
                tx.id == "debt_init_$debtId" ||
                (tx.id.startsWith("debt_rec_") && recordIds.contains(tx.id.removePrefix("debt_rec_"))) ||
                (tx.id.startsWith("tx_") && recordIds.contains(tx.id.removePrefix("tx_"))) ||
                recordIds.contains(tx.id)
            }
            linkedTxs.forEach { tx ->
                deleteTransaction(tx.id)
            }
        }

        _debts.update { list -> list.filterNot { it.id == debtId } }
        softDeleteDebtAndSync(debtId)
        saveToDisk()
    }

    fun addBudget(budget: BudgetCategory) {
        val stamped = budget.copy(updatedAt = System.currentTimeMillis(), isDeleted = false)
        _budgets.update { it + stamped }
        persistBudgetsAndSync(listOf(stamped))
        saveToDisk()
    }

    fun updateBudget(budget: BudgetCategory) {
        val stamped = budget.copy(updatedAt = System.currentTimeMillis(), isDeleted = false)
        _budgets.update { list -> list.map { if (it.id == stamped.id) stamped else it } }
        persistBudgetsAndSync(listOf(stamped))
        saveToDisk()
    }

    fun deleteBudget(budgetId: String) {
        _budgets.update { list -> list.filterNot { it.id == budgetId } }
        softDeleteBudgetAndSync(budgetId)
        saveToDisk()
    }

    fun updateBudgetDisplayStyle(summaryStyle: String, listStyle: String) {
        _settings.value = _settings.value.copy(
            budgetSummaryStyle = summaryStyle,
            budgetListStyle = listStyle
        )
        persistAppSettingsAndSync()
        saveToDisk()
    }

    fun updateSettings(newSettings: UserSettings) {
        _settings.value = newSettings
        persistAppSettingsAndSync()
        saveToDisk()
    }

    fun updateMainMenuOrder(order: List<String>, hidden: List<String>) {
        _settings.value = _settings.value.copy(mainMenuOrder = order, hiddenMenuItems = hidden)
        persistAppSettingsAndSync()
        saveToDisk()
    }

    fun updateStatistikConfig(order: List<String>, hidden: List<String>, sideBySide: Boolean) {
        _settings.value = _settings.value.copy(
            statistikCardsOrder = order,
            hiddenStatistikCards = hidden,
            trenBerdampinganEnabled = sideBySide
        )
        persistAppSettingsAndSync()
        saveToDisk()
    }

    fun updateDashboardWidgets(order: List<String>, hidden: List<String>) {
        _settings.value = _settings.value.copy(
            dashboardWidgetsOrder = order,
            hiddenDashboardWidgets = hidden
        )
        persistAppSettingsAndSync()
        saveToDisk()
    }

    fun addSavingsGoal(goal: SavingsGoal) {
        val stamped = goal.copy(updatedAt = System.currentTimeMillis(), isDeleted = false)
        _savingsGoals.update { it + stamped }
        persistSavingsGoalsAndSync(listOf(stamped))
        saveToDisk()
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        val stamped = goal.copy(updatedAt = System.currentTimeMillis(), isDeleted = false)
        _savingsGoals.update { list -> list.map { if (it.id == stamped.id) stamped else it } }
        persistSavingsGoalsAndSync(listOf(stamped))
        saveToDisk()
    }

    fun deleteSavingsGoal(goalId: String) {
        _savingsGoals.update { list -> list.filterNot { it.id == goalId } }
        softDeleteSavingsGoalAndSync(goalId)
        saveToDisk()
    }

    fun updatePaydayAutoSave(enabled: Boolean, percent: Int = _settings.value.paydayAutoSavePercent) {
        _settings.value = _settings.value.copy(
            paydayAutoSaveEnabled = enabled,
            paydayAutoSavePercent = percent.coerceIn(1, 100)
        )
        persistAppSettingsAndSync()
        saveToDisk()
    }

    fun toggleSavingsGoalCompleted(goalId: String) {
        val now = System.currentTimeMillis()
        var updatedGoal: SavingsGoal? = null
        _savingsGoals.update { list ->
            list.map {
                if (it.id == goalId) it.copy(isCompleted = !it.isCompleted, updatedAt = now).also { updated -> updatedGoal = updated }
                else it
            }
        }
        updatedGoal?.let { persistSavingsGoalsAndSync(listOf(it)) }
        saveToDisk()
    }

    private fun parseSlashOrShortDate(dateStr: String): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance()
        val slashParts = dateStr.trim().split("/")
        if (slashParts.size == 3) {
            val d = slashParts[0].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
            val m = slashParts[1].toIntOrNull()?.coerceIn(1, 12) ?: (cal.get(Calendar.MONTH) + 1)
            val y = slashParts[2].toIntOrNull() ?: cal.get(Calendar.YEAR)
            return Triple(d, m, y)
        }
        val parsed = parseDateString(dateStr)
        return Triple(parsed.day, parsed.month, parsed.year)
    }

    fun addSavingsRecord(
        goalId: String,
        amount: Double,
        isWithdraw: Boolean,
        note: String,
        dateStr: String,
        walletName: String
    ): Boolean {
        if (amount <= 0.0) return false
        val goal = _savingsGoals.value.find { it.id == goalId } ?: return false
        if (isWithdraw && goal.currentAmount < amount) return false

        val cleanWallet = walletName.trim()
        val recId = UUID.randomUUID().toString()
        var linkedTxId = ""

        if (cleanWallet.isNotBlank()) {
            val wallet = _wallets.value.find { it.name.equals(cleanWallet, ignoreCase = true) } ?: return false
            if (!isWithdraw && wallet.balance < amount) return false

            linkedTxId = "sav_rec_$recId"
            val (day, month, year) = parseSlashOrShortDate(dateStr)
            val cal = Calendar.getInstance()
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            val tx = Transaction(
                id = linkedTxId,
                title = if (note.isNotBlank()) note else if (isWithdraw) "Penarikan: ${goal.name}" else "Setoran: ${goal.name}",
                category = "Tabungan",
                subcategory = goal.name,
                amount = amount,
                type = if (isWithdraw) TransactionType.INCOME else TransactionType.EXPENSE,
                accountName = wallet.name,
                dayOfMonth = day,
                monthIndex = month,
                year = year,
                dayOfWeekName = getDayOfWeekName(day, month, year),
                time = timeStr,
                note = note.ifBlank { if (isWithdraw) "Penarikan dari ${goal.name}" else "Setoran ke ${goal.name}" }
            )
            addTransaction(tx)
        }

        val record = SavingsRecord(
            id = recId,
            amount = amount,
            isWithdraw = isWithdraw,
            note = note.trim(),
            dateStr = dateStr,
            walletName = cleanWallet,
            linkedTransactionId = linkedTxId
        )

        val now = System.currentTimeMillis()
        var updatedGoal: SavingsGoal? = null
        _savingsGoals.update { list ->
            list.map { g ->
                if (g.id == goal.id) {
                    val newAmount = if (isWithdraw) (g.currentAmount - amount).coerceAtLeast(0.0) else g.currentAmount + amount
                    g.copy(currentAmount = newAmount, records = listOf(record) + g.records, updatedAt = now)
                        .also { updatedGoal = it }
                } else g
            }
        }
        updatedGoal?.let { persistSavingsGoalsAndSync(listOf(it)) }
        saveToDisk()
        return true
    }

    fun updateSavingsRecord(
        goalId: String,
        recordId: String,
        amount: Double,
        isWithdraw: Boolean,
        note: String,
        dateStr: String
    ): Boolean {
        if (amount <= 0.0) return false
        val goal = _savingsGoals.value.find { it.id == goalId } ?: return false
        val oldRec = goal.records.find { it.id == recordId } ?: return false

        val withoutOld = if (oldRec.isWithdraw) goal.currentAmount + oldRec.amount else (goal.currentAmount - oldRec.amount).coerceAtLeast(0.0)
        if (isWithdraw && withoutOld < amount) return false
        val finalAmount = if (isWithdraw) (withoutOld - amount).coerceAtLeast(0.0) else withoutOld + amount

        if (oldRec.linkedTransactionId.isNotBlank()) {
            val existingTx = _transactions.value.find { it.id == oldRec.linkedTransactionId }
            if (existingTx != null) {
                val (day, month, year) = parseSlashOrShortDate(dateStr)
                updateTransaction(
                    existingTx.copy(
                        title = if (note.isNotBlank()) note else if (isWithdraw) "Penarikan: ${goal.name}" else "Setoran: ${goal.name}",
                        amount = amount,
                        type = if (isWithdraw) TransactionType.INCOME else TransactionType.EXPENSE,
                        dayOfMonth = day,
                        monthIndex = month,
                        year = year,
                        dayOfWeekName = getDayOfWeekName(day, month, year),
                        note = note.ifBlank { existingTx.note }
                    )
                )
            }
        }

        val updatedRec = oldRec.copy(
            amount = amount,
            isWithdraw = isWithdraw,
            note = note.trim(),
            dateStr = dateStr
        )
        val now = System.currentTimeMillis()
        var updatedGoal: SavingsGoal? = null
        _savingsGoals.update { list ->
            list.map { g ->
                if (g.id == goalId) {
                    g.copy(
                        currentAmount = finalAmount,
                        records = g.records.map { if (it.id == recordId) updatedRec else it },
                        updatedAt = now
                    ).also { updatedGoal = it }
                } else g
            }
        }
        updatedGoal?.let { persistSavingsGoalsAndSync(listOf(it)) }
        saveToDisk()
        return true
    }

    fun deleteSavingsRecord(goalId: String, recordId: String) {
        val goal = _savingsGoals.value.find { it.id == goalId } ?: return
        val oldRec = goal.records.find { it.id == recordId } ?: return

        if (oldRec.linkedTransactionId.isNotBlank()) {
            deleteTransaction(oldRec.linkedTransactionId)
        }

        val newAmount = if (oldRec.isWithdraw) {
            goal.currentAmount + oldRec.amount
        } else {
            (goal.currentAmount - oldRec.amount).coerceAtLeast(0.0)
        }
        val now = System.currentTimeMillis()
        var updatedGoal: SavingsGoal? = null
        _savingsGoals.update { list ->
            list.map { g ->
                if (g.id == goalId) {
                    g.copy(
                        currentAmount = newAmount,
                        records = g.records.filterNot { it.id == recordId },
                        updatedAt = now
                    ).also { updatedGoal = it }
                } else g
            }
        }
        updatedGoal?.let { persistSavingsGoalsAndSync(listOf(it)) }
        saveToDisk()
    }

    fun depositToSavingsGoal(goalId: String, amount: Double, sourceWalletName: String): Boolean {
        val cal = Calendar.getInstance()
        val dateStr = String.format(
            Locale.getDefault(),
            "%02d/%02d/%04d",
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR)
        )
        return addSavingsRecord(goalId, amount, false, "", dateStr, sourceWalletName)
    }

    fun withdrawFromSavingsGoal(goalId: String, amount: Double, destinationWalletName: String): Boolean {
        val cal = Calendar.getInstance()
        val dateStr = String.format(
            Locale.getDefault(),
            "%02d/%02d/%04d",
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR)
        )
        return addSavingsRecord(goalId, amount, true, "", dateStr, destinationWalletName)
    }

    fun addEvent(event: EventItem) {
        val cleanName = event.name.trim()
        if (cleanName.isBlank()) return
        val now = System.currentTimeMillis()
        val cleanEvent = event.copy(name = cleanName, updatedAt = now, isDeleted = false)
        _events.update { it + cleanEvent }
        persistEventsAndSync(listOf(cleanEvent))
        if (!_labels.value.any { it.equals(cleanName, ignoreCase = true) }) {
            _labels.update { it + cleanName }
            persistLabelAndSync(cleanName, timestamp = now)
        }
        saveToDisk()
    }

    fun updateEvent(event: EventItem) {
        val oldEvent = _events.value.find { it.id == event.id }
        val cleanName = event.name.trim()
        if (cleanName.isBlank()) return
        val now = System.currentTimeMillis()
        val updated = event.copy(name = cleanName, updatedAt = now, isDeleted = false)
        _events.update { list -> list.map { if (it.id == updated.id) updated else it } }
        persistEventsAndSync(listOf(updated))
        if (oldEvent != null && !oldEvent.name.equals(cleanName, ignoreCase = true)) {
            val modifiedTxs = mutableListOf<Transaction>()
            _transactions.update { txs ->
                txs.map { tx ->
                    if (tx.label.equals(oldEvent.name, ignoreCase = true)) {
                        tx.copy(label = cleanName, updatedAt = now).also { modifiedTxs.add(it) }
                    } else if (tx.label.split(",").any { part -> part.trim().equals(oldEvent.name, ignoreCase = true) }) {
                        val updatedLabels = tx.label.split(",").map { part ->
                            val t = part.trim()
                            if (t.equals(oldEvent.name, ignoreCase = true)) cleanName else t
                        }.filter { it.isNotBlank() }.distinct().joinToString(", ")
                        tx.copy(label = updatedLabels, updatedAt = now).also { modifiedTxs.add(it) }
                    } else tx
                }
            }
            persistTransactionsAndSync(modifiedTxs)
            _labels.update { labels ->
                val mapped = labels.map { if (it.equals(oldEvent.name, ignoreCase = true)) cleanName else it }
                if (mapped.any { it.equals(cleanName, ignoreCase = true) }) mapped.distinct() else (mapped + cleanName).distinct()
            }
            if (_labels.value.none { it.equals(oldEvent.name, ignoreCase = true) }) {
                softDeleteLabelAndSync(oldEvent.name, now)
            }
            persistLabelAndSync(cleanName, timestamp = now)
        } else if (!_labels.value.any { it.equals(cleanName, ignoreCase = true) }) {
            _labels.update { it + cleanName }
            persistLabelAndSync(cleanName, timestamp = now)
        }
        saveToDisk()
    }

    fun deleteEvent(eventId: String) {
        val now = System.currentTimeMillis()
        val target = _events.value.find { it.id == eventId }
        _events.update { list -> list.filterNot { it.id == eventId } }
        softDeleteEventAndSync(eventId, now)
        if (target != null) {
            _labels.update { list -> list.filterNot { it.equals(target.name, ignoreCase = true) } }
            softDeleteLabelAndSync(target.name, now)
        }
        saveToDisk()
    }

    fun toggleEventCompleted(eventId: String) {
        val now = System.currentTimeMillis()
        var updatedEvent: EventItem? = null
        _events.update { list ->
            list.map {
                if (it.id == eventId) it.copy(isCompleted = !it.isCompleted, updatedAt = now).also { ev -> updatedEvent = ev }
                else it
            }
        }
        updatedEvent?.let { persistEventsAndSync(listOf(it)) }
        saveToDisk()
    }

    fun toggleEventPlannedExpensePaid(eventId: String, expenseId: String) {
        val now = System.currentTimeMillis()
        var updatedEvent: EventItem? = null
        _events.update { list ->
            list.map { ev ->
                if (ev.id == eventId) {
                    ev.copy(
                        plannedExpenses = ev.plannedExpenses.map { pe ->
                            if (pe.id == expenseId) pe.copy(isPaid = !pe.isPaid) else pe
                        },
                        updatedAt = now
                    ).also { updatedEvent = it }
                } else ev
            }
        }
        updatedEvent?.let { persistEventsAndSync(listOf(it)) }
        saveToDisk()
    }

    fun addEventPlannedExpense(eventId: String, expense: EventPlannedExpense) {
        val now = System.currentTimeMillis()
        var updatedEvent: EventItem? = null
        _events.update { list ->
            list.map { ev ->
                if (ev.id == eventId) ev.copy(plannedExpenses = ev.plannedExpenses + expense, updatedAt = now)
                    .also { updatedEvent = it } else ev
            }
        }
        updatedEvent?.let { persistEventsAndSync(listOf(it)) }
        saveToDisk()
    }

    fun updateEventPlannedExpense(eventId: String, expense: EventPlannedExpense) {
        val now = System.currentTimeMillis()
        var updatedEvent: EventItem? = null
        _events.update { list ->
            list.map { ev ->
                if (ev.id == eventId) {
                    ev.copy(
                        plannedExpenses = ev.plannedExpenses.map { if (it.id == expense.id) expense else it },
                        updatedAt = now
                    ).also { updatedEvent = it }
                } else ev
            }
        }
        updatedEvent?.let { persistEventsAndSync(listOf(it)) }
        saveToDisk()
    }

    fun deleteEventPlannedExpense(eventId: String, expenseId: String) {
        val now = System.currentTimeMillis()
        var updatedEvent: EventItem? = null
        _events.update { list ->
            list.map { ev ->
                if (ev.id == eventId) {
                    ev.copy(
                        plannedExpenses = ev.plannedExpenses.filterNot { it.id == expenseId },
                        updatedAt = now
                    ).also { updatedEvent = it }
                } else ev
            }
        }
        updatedEvent?.let { persistEventsAndSync(listOf(it)) }
        saveToDisk()
    }

    fun linkTransactionToEvent(txId: String, eventName: String, link: Boolean) {
        val cleanName = eventName.trim()
        if (cleanName.isBlank()) return
        val now = System.currentTimeMillis()
        val modifiedTxs = mutableListOf<Transaction>()
        _transactions.update { txs ->
            txs.map { tx ->
                if (tx.id == txId) {
                    val parts = tx.label.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                    if (link) {
                        if (parts.none { it.equals(cleanName, ignoreCase = true) }) parts.add(cleanName)
                    } else {
                        parts.removeAll { it.equals(cleanName, ignoreCase = true) }
                    }
                    tx.copy(label = parts.joinToString(", "), updatedAt = now).also { modifiedTxs.add(it) }
                } else tx
            }
        }
        persistTransactionsAndSync(modifiedTxs)
        saveToDisk()
    }

    fun addTimelineScenario(item: com.example.data.model.TimelineScenarioItem) {
        val stamped = item.copy(updatedAt = System.currentTimeMillis(), isDeleted = false)
        _timelineScenarios.update { it + stamped }
        persistTimelineScenariosAndSync(listOf(stamped))
        saveToDisk()
    }

    fun deleteTimelineScenario(itemId: String) {
        _timelineScenarios.update { list -> list.filterNot { it.id == itemId } }
        softDeleteTimelineScenarioAndSync(itemId)
        saveToDisk()
    }

    fun addLabel(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !_labels.value.any { it.equals(trimmed, ignoreCase = true) }) {
            _labels.update { it + trimmed }
            persistLabelAndSync(trimmed)
            saveToDisk()
        }
    }

    fun deleteLabel(name: String) {
        _labels.update { it.filterNot { label -> label.equals(name, ignoreCase = true) } }
        softDeleteLabelAndSync(name)
        saveToDisk()
    }

    // Category and Subcategory methods
    fun addCategory(category: MainCategory) {
        val now = System.currentTimeMillis()
        val stamped = category.copy(updatedAt = now, isDeleted = false)
        _categories.update { current ->
            current + stamped
        }
        persistCategoriesAndSync(listOf(stamped), _categories.value)
        saveToDisk()
    }

    fun updateCategory(category: MainCategory) {
        val now = System.currentTimeMillis()
        val oldCategory = _categories.value.find { it.id == category.id }
        val stamped = category.copy(updatedAt = now, isDeleted = false)
        _categories.update { current ->
            current.map { if (it.id == stamped.id) stamped else it }
        }
        val changedTxs = mutableListOf<Transaction>()
        if (oldCategory != null && !oldCategory.name.equals(stamped.name, ignoreCase = false)) {
            _transactions.update { list ->
                list.map { tx ->
                    if (tx.type == stamped.type && tx.category.equals(oldCategory.name, ignoreCase = true)) {
                        tx.copy(category = stamped.name, updatedAt = now).also { changedTxs.add(it) }
                    } else tx
                }
            }
        }
        persistCategoriesAndSync(listOf(stamped), _categories.value)
        persistTransactionsAndSync(changedTxs)
        saveToDisk()
    }

    fun reorderCategories(newCategories: List<MainCategory>) {
        if (newCategories.isEmpty()) return
        val now = System.currentTimeMillis()
        val stamped = newCategories.map { it.copy(updatedAt = now, isDeleted = false) }
        _categories.value = stamped
        persistCategoriesAndSync(stamped, stamped)
        saveToDisk()
    }

    fun reorderSubcategories(categoryId: String, newSubcategories: List<SubcategoryItem>) {
        val now = System.currentTimeMillis()
        var updatedCat: MainCategory? = null
        _categories.update { current ->
            current.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(subcategories = newSubcategories, updatedAt = now)
                        .also { updatedCat = it }
                } else cat
            }
        }
        updatedCat?.let { persistCategoriesAndSync(listOf(it), _categories.value) }
        saveToDisk()
    }

    fun deleteCategory(categoryId: String) {
        _categories.update { current ->
            current.filterNot { it.id == categoryId }
        }
        softDeleteCategoryAndSync(categoryId)
        saveToDisk()
    }

    fun addSubcategory(categoryId: String, subcategory: SubcategoryItem) {
        val now = System.currentTimeMillis()
        var updatedCat: MainCategory? = null
        _categories.update { current ->
            current.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(subcategories = cat.subcategories + subcategory, updatedAt = now)
                        .also { updatedCat = it }
                } else cat
            }
        }
        updatedCat?.let { persistCategoriesAndSync(listOf(it), _categories.value) }
        saveToDisk()
    }

    fun updateSubcategory(categoryId: String, subcategory: SubcategoryItem) {
        val now = System.currentTimeMillis()
        val parentCat = _categories.value.find { it.id == categoryId }
        val oldSub = parentCat?.subcategories?.find { it.id == subcategory.id }
        var updatedCat: MainCategory? = null
        _categories.update { current ->
            current.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(
                        subcategories = cat.subcategories.map { if (it.id == subcategory.id) subcategory else it },
                        updatedAt = now
                    ).also { updatedCat = it }
                } else cat
            }
        }
        val changedTxs = mutableListOf<Transaction>()
        if (parentCat != null && oldSub != null && !oldSub.name.equals(subcategory.name, ignoreCase = false)) {
            _transactions.update { list ->
                list.map { tx ->
                    if (
                        tx.type == parentCat.type &&
                        tx.category.equals(parentCat.name, ignoreCase = true) &&
                        tx.subcategory.equals(oldSub.name, ignoreCase = true)
                    ) {
                        tx.copy(subcategory = subcategory.name, updatedAt = now).also { changedTxs.add(it) }
                    } else tx
                }
            }
        }
        updatedCat?.let { persistCategoriesAndSync(listOf(it), _categories.value) }
        persistTransactionsAndSync(changedTxs)
        saveToDisk()
    }

    fun deleteSubcategory(categoryId: String, subcategoryId: String) {
        val now = System.currentTimeMillis()
        var updatedCat: MainCategory? = null
        _categories.update { current ->
            current.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(
                        subcategories = cat.subcategories.filterNot { it.id == subcategoryId },
                        updatedAt = now
                    ).also { updatedCat = it }
                } else cat
            }
        }
        updatedCat?.let { persistCategoriesAndSync(listOf(it), _categories.value) }
        saveToDisk()
    }

    fun toggleCategoryVisibility(categoryId: String) {
        val now = System.currentTimeMillis()
        var updatedCat: MainCategory? = null
        _categories.update { current ->
            current.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(isVisible = !cat.isVisible, updatedAt = now)
                        .also { updatedCat = it }
                } else cat
            }
        }
        updatedCat?.let { persistCategoriesAndSync(listOf(it), _categories.value) }
        saveToDisk()
    }

    fun toggleSubcategoryVisibility(categoryId: String, subcategoryId: String) {
        val now = System.currentTimeMillis()
        var updatedCat: MainCategory? = null
        _categories.update { current ->
            current.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(
                        subcategories = cat.subcategories.map {
                            if (it.id == subcategoryId) it.copy(isVisible = !it.isVisible) else it
                        },
                        updatedAt = now
                    ).also { updatedCat = it }
                } else cat
            }
        }
        updatedCat?.let { persistCategoriesAndSync(listOf(it), _categories.value) }
        saveToDisk()
    }

    fun addRecurringTransaction(
        recurring: RecurringTransaction,
        checkImmediateProcess: Boolean = true,
        currentDay: Int = 28,
        currentMonth: Int = 10,
        currentYear: Int = 2026
    ) {
        val now = System.currentTimeMillis()
        var itemToAdd = recurring.copy(updatedAt = now, isDeleted = false)
        if (checkImmediateProcess && recurring.isActive && recurring.autoDeduct && recurring.dayOfMonth <= currentDay) {
            val key = "$currentMonth/$currentYear"
            if (itemToAdd.lastProcessedMonthYear != key) {
                val dayOfWeek = getDayOfWeekName(itemToAdd.dayOfMonth, currentMonth, currentYear)
                val tx = Transaction(
                    title = itemToAdd.title,
                    category = itemToAdd.category,
                    subcategory = itemToAdd.subcategory,
                    amount = itemToAdd.amount,
                    type = itemToAdd.type,
                    accountName = itemToAdd.accountName,
                    toAccountName = if (itemToAdd.type == TransactionType.TRANSFER) itemToAdd.toAccountName else "",
                    dayOfMonth = itemToAdd.dayOfMonth,
                    monthIndex = currentMonth,
                    year = currentYear,
                    dayOfWeekName = dayOfWeek,
                    note = if (itemToAdd.note.isNotBlank()) "${itemToAdd.note} (Otomatis Rutin)" else "Otomatis dari Transaksi Berulang",
                    label = itemToAdd.label,
                    recurrence = "Bulanan (Tgl ${itemToAdd.dayOfMonth})"
                )
                addTransaction(tx)
                itemToAdd = itemToAdd.copy(lastProcessedMonthYear = key, updatedAt = System.currentTimeMillis())
            }
        }
        _recurringTransactions.update { it + itemToAdd }
        persistRecurringAndSync(listOf(itemToAdd))
        saveToDisk()
    }

    fun updateRecurringTransaction(recurring: RecurringTransaction) {
        val stamped = recurring.copy(updatedAt = System.currentTimeMillis(), isDeleted = false)
        _recurringTransactions.update { list ->
            list.map { if (it.id == stamped.id) stamped else it }
        }
        persistRecurringAndSync(listOf(stamped))
        saveToDisk()
    }

    fun deleteRecurringTransaction(id: String) {
        _recurringTransactions.update { it.filterNot { item -> item.id == id } }
        softDeleteRecurringAndSync(id)
        saveToDisk()
    }

    fun deleteRecurringWithTransactions(id: String, deleteHistory: Boolean) {
        val item = _recurringTransactions.value.find { it.id == id }
        if (deleteHistory && item != null) {
            val matchingIds = _transactions.value.filter { tx ->
                tx.title.equals(item.title, ignoreCase = true) ||
                    (tx.recurrence.isNotBlank() &&
                        tx.recurrence != "Tidak pernah" &&
                        tx.category.equals(item.category, ignoreCase = true) &&
                        tx.accountName.equals(item.accountName, ignoreCase = true) &&
                        (item.toAccountName.isBlank() || tx.toAccountName.equals(item.toAccountName, ignoreCase = true)) &&
                        tx.amount == item.amount)
            }.map { it.id }
            matchingIds.forEach { txId ->
                deleteTransaction(txId)
            }
        }
        _recurringTransactions.update { it.filterNot { r -> r.id == id } }
        softDeleteRecurringAndSync(id)
        saveToDisk()
    }

    fun toggleRecurringActive(id: String) {
        val now = System.currentTimeMillis()
        var updatedItem: RecurringTransaction? = null
        _recurringTransactions.update { list ->
            list.map {
                if (it.id == id) it.copy(isActive = !it.isActive, updatedAt = now).also { upd -> updatedItem = upd }
                else it
            }
        }
        updatedItem?.let { persistRecurringAndSync(listOf(it)) }
        saveToDisk()
    }

    fun processRecurringTransaction(id: String, month: Int, year: Int, dayOverride: Int? = null): Boolean {
        val recurring = _recurringTransactions.value.find { it.id == id } ?: return false
        val key = "$month/$year"
        val effectiveDay = (dayOverride ?: recurring.dayOfMonth).coerceIn(1, 31)
        val dayOfWeek = getDayOfWeekName(effectiveDay, month, year)
        val tx = Transaction(
            title = recurring.title,
            category = recurring.category,
            subcategory = recurring.subcategory,
            amount = recurring.amount,
            type = recurring.type,
            accountName = recurring.accountName,
            toAccountName = if (recurring.type == TransactionType.TRANSFER) recurring.toAccountName else "",
            dayOfMonth = effectiveDay,
            monthIndex = month,
            year = year,
            dayOfWeekName = dayOfWeek,
            note = if (recurring.note.isNotBlank()) recurring.note else "Otomatis dari Transaksi Berulang",
            label = recurring.label,
            recurrence = "${recurring.frequency} (Tgl ${recurring.dayOfMonth})"
        )
        addTransaction(tx)
        val now = System.currentTimeMillis()
        var updatedItem: RecurringTransaction? = null
        _recurringTransactions.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(lastProcessedMonthYear = key, snoozedUntilDate = "", lastSkippedDateLabel = "", updatedAt = now)
                        .also { upd -> updatedItem = upd }
                } else it
            }
        }
        updatedItem?.let { persistRecurringAndSync(listOf(it)) }
        saveToDisk()
        return true
    }

    fun skipRecurringOccurrence(id: String, month: Int, year: Int, skippedDateLabel: String = "") {
        val key = "$month/$year"
        val monthFull = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val now = System.currentTimeMillis()
        var updatedItem: RecurringTransaction? = null
        _recurringTransactions.update { list ->
            list.map {
                if (it.id == id) {
                    val label = skippedDateLabel.ifBlank { "${it.dayOfMonth} ${monthFull.getOrElse(month - 1) { "Nov" }}" }
                    it.copy(
                        lastProcessedMonthYear = key,
                        skippedCount = it.skippedCount + 1,
                        lastSkippedDateLabel = label,
                        snoozedUntilDate = "",
                        updatedAt = now
                    ).also { upd -> updatedItem = upd }
                } else it
            }
        }
        updatedItem?.let { persistRecurringAndSync(listOf(it)) }
        saveToDisk()
    }

    fun undoSkipRecurringOccurrence(id: String) {
        val now = System.currentTimeMillis()
        var updatedItem: RecurringTransaction? = null
        _recurringTransactions.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        lastProcessedMonthYear = "",
                        skippedCount = (it.skippedCount - 1).coerceAtLeast(0),
                        lastSkippedDateLabel = "",
                        updatedAt = now
                    ).also { upd -> updatedItem = upd }
                } else it
            }
        }
        updatedItem?.let { persistRecurringAndSync(listOf(it)) }
        saveToDisk()
    }

    fun snoozeRecurringOccurrence(id: String, snoozedUntilDate: String) {
        val parts = snoozedUntilDate.split("/")
        val newDay = parts.getOrNull(0)?.toIntOrNull()
        val now = System.currentTimeMillis()
        var updatedItem: RecurringTransaction? = null
        _recurringTransactions.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        snoozedUntilDate = snoozedUntilDate,
                        dayOfMonth = newDay ?: it.dayOfMonth,
                        updatedAt = now
                    ).also { upd -> updatedItem = upd }
                } else it
            }
        }
        updatedItem?.let { persistRecurringAndSync(listOf(it)) }
        saveToDisk()
    }

    fun updateRecurringReminders(id: String, enabled: Boolean, options: List<String>) {
        val now = System.currentTimeMillis()
        var updatedItem: RecurringTransaction? = null
        _recurringTransactions.update { list ->
            list.map {
                if (it.id == id) it.copy(reminderEnabled = enabled, reminderOptions = options, updatedAt = now)
                    .also { upd -> updatedItem = upd } else it
            }
        }
        updatedItem?.let { persistRecurringAndSync(listOf(it)) }
        saveToDisk()
    }

    fun markRecurringProcessed(id: String, month: Int, year: Int) {
        val key = "$month/$year"
        val now = System.currentTimeMillis()
        var updatedItem: RecurringTransaction? = null
        _recurringTransactions.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(lastProcessedMonthYear = key, snoozedUntilDate = "", lastSkippedDateLabel = "", updatedAt = now)
                        .also { upd -> updatedItem = upd }
                } else it
            }
        }
        updatedItem?.let { persistRecurringAndSync(listOf(it)) }
        saveToDisk()
    }

    fun checkAndExecuteDueRecurringTransactions(currentDay: Int, currentMonth: Int, currentYear: Int): Int {
        val key = "$currentMonth/$currentYear"
        var executedCount = 0
        _recurringTransactions.value.forEach { item ->
            if (item.isActive && item.autoDeduct && item.dayOfMonth <= currentDay && item.lastProcessedMonthYear != key) {
                processRecurringTransaction(item.id, currentMonth, currentYear)
                executedCount++
            }
        }
        return executedCount
    }

    private fun getDayOfWeekName(day: Int, month: Int, year: Int): String {
        val c = java.util.Calendar.getInstance()
        c.set(year, month - 1, day)
        return when (c.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY -> "Min"
            java.util.Calendar.MONDAY -> "Sen"
            java.util.Calendar.TUESDAY -> "Sel"
            java.util.Calendar.WEDNESDAY -> "Rab"
            java.util.Calendar.THURSDAY -> "Kam"
            java.util.Calendar.FRIDAY -> "Jum"
            java.util.Calendar.SATURDAY -> "Sab"
            else -> "Sen"
        }
    }

    private fun syncActivePlanToPlansList(now: Long = System.currentTimeMillis()): FinancialPlan? {
        val activeId = _activePlanId.value
        val curTitle = _planTitle.value
        val curItems = _plannedItems.value
        return if (_financialPlans.value.isEmpty()) {
            val cal = Calendar.getInstance()
            val plan = FinancialPlan(
                id = activeId.ifBlank { "plan_default" },
                title = curTitle,
                monthIndex = cal.get(Calendar.MONTH) + 1,
                year = cal.get(Calendar.YEAR),
                items = curItems,
                updatedAt = now,
                isDeleted = false
            )
            _activePlanId.value = plan.id
            _financialPlans.value = listOf(plan)
            plan
        } else {
            var updatedPlan: FinancialPlan? = null
            _financialPlans.update { list ->
                list.map {
                    if (it.id == activeId) it.copy(title = curTitle, items = curItems, updatedAt = now, isDeleted = false)
                        .also { upd -> updatedPlan = upd } else it
                }
            }
            updatedPlan
        }
    }

    fun updatePlanTitle(newTitle: String) {
        val now = System.currentTimeMillis()
        _planTitle.value = newTitle
        syncActivePlanToPlansList(now)?.let { persistFinancialPlansAndSync(listOf(it)) }
        persistAppSettingsAndSync(now)
        saveToDisk()
    }

    fun addPlannedItem(item: PlannedItem) {
        val now = System.currentTimeMillis()
        _plannedItems.update { it + item }
        syncActivePlanToPlansList(now)?.let { persistFinancialPlansAndSync(listOf(it)) }
        saveToDisk()
    }

    fun updatePlannedItem(item: PlannedItem) {
        val now = System.currentTimeMillis()
        _plannedItems.update { list -> list.map { if (it.id == item.id) item else it } }
        syncActivePlanToPlansList(now)?.let { persistFinancialPlansAndSync(listOf(it)) }
        saveToDisk()
    }

    fun deletePlannedItem(id: String) {
        val now = System.currentTimeMillis()
        _plannedItems.update { list -> list.filterNot { it.id == id } }
        syncActivePlanToPlansList(now)?.let { persistFinancialPlansAndSync(listOf(it)) }
        saveToDisk()
    }

    fun selectActivePlan(planId: String) {
        val target = _financialPlans.value.find { it.id == planId } ?: return
        _activePlanId.value = target.id
        _planTitle.value = target.title
        _plannedItems.value = target.items
        persistAppSettingsAndSync()
        saveToDisk()
    }

    fun createFinancialPlan(
        monthIndex: Int,
        year: Int,
        mode: NewPlanStartMode,
        customTitle: String? = null
    ): FinancialPlan {
        val monthFullNames = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val monthName = monthFullNames.getOrElse(monthIndex - 1) { "Bulan" }

        val generatedItems = when (mode) {
            NewPlanStartMode.COPY_ACTIVE -> {
                _plannedItems.value.map { it.copy(id = UUID.randomUUID().toString()) }
            }
            NewPlanStartMode.BILLS_ONLY -> {
                _recurringTransactions.value.filter { it.isActive }.map { rec ->
                    PlannedItem(
                        name = rec.title,
                        category = rec.category,
                        subcategory = rec.subcategory,
                        amount = rec.amount,
                        section = if (rec.type == TransactionType.INCOME) PlanSectionType.INCOME else PlanSectionType.EXPENSE
                    )
                }
            }
            NewPlanStartMode.EMPTY -> emptyList()
        }

        val resolvedTitle = customTitle?.trim()?.takeIf { it.isNotBlank() } ?: when (mode) {
            NewPlanStartMode.COPY_ACTIVE -> _planTitle.value.ifBlank { "Pembagian Gaji" }
            NewPlanStartMode.BILLS_ONLY -> "Tagihan $monthName $year"
            NewPlanStartMode.EMPTY -> "Rencana $monthName $year"
        }

        val now = System.currentTimeMillis()
        val newPlan = FinancialPlan(
            title = resolvedTitle,
            monthIndex = monthIndex,
            year = year,
            items = generatedItems,
            updatedAt = now,
            isDeleted = false
        )

        _financialPlans.update { list ->
            (list + newPlan).sortedWith(compareBy<FinancialPlan> { it.year }.thenBy { it.monthIndex })
        }
        _activePlanId.value = newPlan.id
        _planTitle.value = newPlan.title
        _plannedItems.value = newPlan.items
        persistFinancialPlansAndSync(listOf(newPlan), _financialPlans.value)
        persistAppSettingsAndSync(now)
        saveToDisk()
        return newPlan
    }

    fun deleteFinancialPlan(planId: String) {
        if (_financialPlans.value.size <= 1) return
        val now = System.currentTimeMillis()
        val remaining = _financialPlans.value.filterNot { it.id == planId }
        _financialPlans.value = remaining
        softDeleteFinancialPlanAndSync(planId, now)
        if (_activePlanId.value == planId && remaining.isNotEmpty()) {
            val next = remaining.first()
            _activePlanId.value = next.id
            _planTitle.value = next.title
            _plannedItems.value = next.items
            persistAppSettingsAndSync(now)
        }
        saveToDisk()
    }

    fun exportTransactionsJson(ctx: Context? = getActiveContext()): String {
        val root = JSONObject()
        root.put("app", "Keuangan")
        root.put("version", "3.0")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        root.put("exportedAt", sdf.format(Date()))
        root.put("totalTransactions", _transactions.value.size)
        root.put("totalWallets", _wallets.value.size)
        root.put("totalAssets", _assets.value.size)
        root.put("totalDebts", _debts.value.size)
        root.put("totalCategories", _categories.value.size)
        root.put("totalPlans", _plannedItems.value.size)
        root.put("totalBudgets", _budgets.value.size)
        root.put("totalRecurring", _recurringTransactions.value.size)
        root.put("totalSavingsGoals", _savingsGoals.value.size)
        root.put("totalEvents", _events.value.size)

        // Collect custom logos (Base64)
        val referencedCustomPaths = mutableListOf<String>()
        _wallets.value.forEach { w ->
            if (w.iconName.isNotBlank() && (w.iconName.startsWith("custom:") || w.iconName.contains("custom_icons"))) {
                referencedCustomPaths.add(w.iconName)
            }
        }
        _assets.value.forEach { a ->
            if (a.iconUri.isNotBlank() && (a.iconUri.startsWith("custom:") || a.iconUri.contains("custom_icons"))) {
                referencedCustomPaths.add(a.iconUri)
            }
        }
        _debts.value.forEach { d ->
            if (d.institutionLogo.isNotBlank() && (d.institutionLogo.startsWith("custom:") || d.institutionLogo.contains("custom_icons"))) {
                referencedCustomPaths.add(d.institutionLogo)
            }
        }

        val logosMap = if (ctx != null) {
            ImageUploadHelper.collectAllCustomLogos(ctx, referencedCustomPaths)
        } else emptyMap()

        // 1. Transactions
        val txArr = JSONArray()
        _transactions.value.forEach { tx ->
            val o = JSONObject().apply {
                put("id", tx.id)
                put("title", tx.title)
                put("category", tx.category)
                put("subcategory", tx.subcategory)
                put("amount", tx.amount)
                put("type", tx.type.name)
                put("accountName", tx.accountName)
                put("dayOfMonth", tx.dayOfMonth)
                put("monthIndex", tx.monthIndex)
                put("year", tx.year)
                put("dayOfWeekName", tx.dayOfWeekName)
                put("note", tx.note)
                put("label", tx.label)
                put("recurrence", tx.recurrence)
                put("time", tx.time)
                put("toAccountName", tx.toAccountName)
                put("transferFee", tx.transferFee)
                put("targetAssetId", tx.targetAssetId)
                put("targetAssetName", tx.targetAssetName)
                put("targetDebtId", tx.targetDebtId)
                put("targetDebtName", tx.targetDebtName)
            }
            txArr.put(o)
        }
        root.put("transactions", txArr)

        // 2. Wallets / Akun (with custom logos)
        val wArr = JSONArray()
        _wallets.value.forEach { w ->
            val o = JSONObject().apply {
                put("id", w.id)
                put("name", w.name)
                put("bankType", w.bankType)
                put("balance", w.balance)
                put("lastCategory", w.lastCategory)
                put("lastAmount", w.lastAmount)
                put("lastActivityTime", w.lastActivityTime)
                put("currency", w.currency)
                put("iconName", w.iconName)
                put("excludeFromNetWorth", w.excludeFromNetWorth)
                put("excludeFromBudget", w.excludeFromBudget)
                if (w.iconName.isNotBlank() && (w.iconName.startsWith("custom:") || w.iconName.contains("custom_icons"))) {
                    val fName = ImageUploadHelper.extractFileName(w.iconName)
                    put("customLogoKey", fName)
                    logosMap[fName]?.let { put("customLogoBase64", it) }
                }
            }
            wArr.put(o)
        }
        root.put("wallets", wArr)

        // 3. Assets (with custom logos & full fields)
        val aArr = JSONArray()
        _assets.value.forEach { a ->
            val o = JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("group", a.group)
                put("assetType", a.assetType)
                put("value", a.value)
                put("platform", a.platform)
                put("performancePercent", a.performancePercent)
                put("currency", a.currency)
                put("createdAtDate", a.createdAtDate)
                put("createdAtTime", a.createdAtTime)
                put("valuationMethod", a.valuationMethod)
                put("iconUri", a.iconUri)
                put("tickerSymbol", a.tickerSymbol)
                put("lotCount", a.lotCount)
                put("averagePrice", a.averagePrice)
                put("currentPrice", a.currentPrice)
                put("dailyPriceChange", a.dailyPriceChange)
                put("dailyPriceChangePercent", a.dailyPriceChangePercent)
                put("lastPriceUpdate", a.lastPriceUpdate)
                if (a.iconUri.isNotBlank() && (a.iconUri.startsWith("custom:") || a.iconUri.contains("custom_icons"))) {
                    val fName = ImageUploadHelper.extractFileName(a.iconUri)
                    put("customLogoKey", fName)
                    logosMap[fName]?.let { put("customLogoBase64", it) }
                }

                val rArr = JSONArray()
                a.records.forEach { r ->
                    val ro = JSONObject().apply {
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
                    rArr.put(ro)
                }
                put("records", rArr)
            }
            aArr.put(o)
        }
        root.put("assets", aArr)

        // 4. Debts (with custom logos & full fields)
        val dArr = JSONArray()
        _debts.value.forEach { d ->
            val o = JSONObject().apply {
                put("id", d.id)
                put("name", d.name)
                put("type", d.type.name)
                put("totalAmount", d.totalAmount)
                put("paidAmount", d.paidAmount)
                put("dueDate", d.dueDate)
                put("startDate", d.startDate)
                put("startTime", d.startTime)
                put("monthlyPayment", d.monthlyPayment)
                put("interestRate", d.interestRate)
                put("extraFees", d.extraFees)
                put("categoryIcon", d.categoryIcon)
                put("institutionLogo", d.institutionLogo)
                put("tenureMonths", d.tenureMonths)
                put("linkedWalletName", d.linkedWalletName)
                if (d.institutionLogo.isNotBlank() && (d.institutionLogo.startsWith("custom:") || d.institutionLogo.contains("custom_icons"))) {
                    val fName = ImageUploadHelper.extractFileName(d.institutionLogo)
                    put("customLogoKey", fName)
                    logosMap[fName]?.let { put("customLogoBase64", it) }
                }

                val rArr = JSONArray()
                d.records.forEach { r ->
                    val ro = JSONObject().apply {
                        put("id", r.id)
                        put("debtId", r.debtId)
                        put("type", r.type)
                        put("amount", r.amount)
                        put("date", r.date)
                        put("time", r.time)
                        put("sourceWalletName", r.sourceWalletName)
                        put("note", r.note)
                    }
                    rArr.put(ro)
                }
                put("records", rArr)
            }
            dArr.put(o)
        }
        root.put("debts", dArr)

        // 5. Budgets
        val bArr = JSONArray()
        _budgets.value.forEach { b ->
            val o = JSONObject().apply {
                put("id", b.id)
                put("category", b.category)
                put("allocated", b.allocated)
                put("spent", b.spent)
                put("name", b.name)
                put("categories", JSONArray(b.categories))
                put("accountNames", JSONArray(b.accountNames))
                put("frequency", b.frequency)
                put("customStartDate", b.customStartDate)
                put("customEndDate", b.customEndDate)
            }
            bArr.put(o)
        }
        root.put("budgets", bArr)

        // 6. Recurring Transactions
        val recArr = JSONArray()
        _recurringTransactions.value.forEach { r ->
            val o = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("amount", r.amount)
                put("type", r.type.name)
                put("category", r.category)
                put("subcategory", r.subcategory)
                put("accountName", r.accountName)
                put("toAccountName", r.toAccountName)
                put("dayOfMonth", r.dayOfMonth)
                put("frequency", r.frequency)
                put("startDate", r.startDate)
                put("endDate", r.endDate)
                put("note", r.note)
                put("label", r.label)
                put("isActive", r.isActive)
                put("autoDeduct", r.autoDeduct)
                put("lastProcessedMonthYear", r.lastProcessedMonthYear)
                put("endCondition", r.endCondition)
                put("endAfterCount", r.endAfterCount)
                put("logoName", r.logoName)
                put("skippedCount", r.skippedCount)
                put("snoozedUntilDate", r.snoozedUntilDate)
                put("lastSkippedDateLabel", r.lastSkippedDateLabel)
                put("reminderEnabled", r.reminderEnabled)
                put("reminderOptions", JSONArray(r.reminderOptions))
            }
            recArr.put(o)
        }
        root.put("recurringTransactions", recArr)

        // 7. Upcoming Schedules
        val upArr = JSONArray()
        _upcomingItems.value.forEach { up ->
            val o = JSONObject().apply {
                put("id", up.id)
                put("title", up.title)
                put("accountName", up.accountName)
                put("amount", up.amount)
                put("isIncome", up.isIncome)
                put("dateLabel", up.dateLabel)
                put("dayLabel", up.dayLabel)
            }
            upArr.put(o)
        }
        root.put("upcomingItems", upArr)

        // 8. Categories & Subcategories (including all custom categories added by user)
        val cArr = JSONArray()
        _categories.value.forEach { cat ->
            val co = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("type", cat.type.name)
                put("iconName", cat.iconName)
                put("isVisible", cat.isVisible)
                val subArr = JSONArray()
                cat.subcategories.forEach { sub ->
                    val so = JSONObject().apply {
                        put("id", sub.id)
                        put("name", sub.name)
                        put("iconName", sub.iconName)
                        put("isVisible", sub.isVisible)
                    }
                    subArr.put(so)
                }
                put("subcategories", subArr)
            }
            cArr.put(co)
        }
        root.put("categories", cArr)

        // 9. Planned Items (Perencanaan Keuangan)
        val pArr = JSONArray()
        _plannedItems.value.forEach { p ->
            val o = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("category", p.category)
                put("subcategory", p.subcategory)
                put("section", p.section.name)
                put("amount", p.amount)
            }
            pArr.put(o)
        }
        root.put("plannedItems", pArr)
        root.put("planTitle", _planTitle.value)

        // Multi Financial Plans
        val fpArr = JSONArray()
        _financialPlans.value.forEach { fp ->
            val fpItemsArr = JSONArray()
            fp.items.forEach { p ->
                fpItemsArr.put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("category", p.category)
                        put("subcategory", p.subcategory)
                        put("section", p.section.name)
                        put("amount", p.amount)
                    }
                )
            }
            fpArr.put(
                JSONObject().apply {
                    put("id", fp.id)
                    put("title", fp.title)
                    put("monthIndex", fp.monthIndex)
                    put("year", fp.year)
                    put("items", fpItemsArr)
                }
            )
        }
        root.put("financialPlans", fpArr)
        root.put("activePlanId", _activePlanId.value)

        // Savings Goals (Tabungan)
        val sgArr = JSONArray()
        _savingsGoals.value.forEach { sg ->
            val recsArr = JSONArray()
            sg.records.forEach { rec ->
                recsArr.put(
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
            sgArr.put(
                JSONObject().apply {
                    put("id", sg.id)
                    put("name", sg.name)
                    put("targetAmount", sg.targetAmount)
                    put("currentAmount", sg.currentAmount)
                    put("targetDate", sg.targetDate)
                    put("startDate", sg.startDate)
                    put("currency", sg.currency)
                    put("iconName", sg.iconName)
                    put("note", sg.note)
                    put("colorHex", sg.colorHex)
                    put("isCompleted", sg.isCompleted)
                    put("records", recsArr)
                }
            )
        }
        root.put("savingsGoals", sgArr)

        // Events (Acara)
        val evArr = JSONArray()
        _events.value.forEach { ev ->
            val peArr = JSONArray()
            ev.plannedExpenses.forEach { pe ->
                peArr.put(
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
            evArr.put(
                JSONObject().apply {
                    put("id", ev.id)
                    put("name", ev.name)
                    put("eventType", ev.eventType)
                    put("targetBudget", ev.targetBudget)
                    put("monthIndex", ev.monthIndex)
                    put("year", ev.year)
                    put("startDate", ev.startDate)
                    put("endDate", ev.endDate)
                    put("currency", ev.currency)
                    put("iconName", ev.iconName)
                    put("colorHex", ev.colorHex)
                    put("note", ev.note)
                    put("plannedExpenses", peArr)
                    put("isCompleted", ev.isCompleted)
                }
            )
        }
        root.put("events", evArr)

        // 10. Labels
        val lArr = JSONArray()
        _labels.value.forEach { lArr.put(it) }
        root.put("labels", lArr)

        // 11. User Settings
        val s = _settings.value
        val sObj = JSONObject().apply {
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
            put("budgetSummaryStyle", s.budgetSummaryStyle)
            put("budgetListStyle", s.budgetListStyle)
            put("paydayAutoSaveEnabled", s.paydayAutoSaveEnabled)
            put("paydayAutoSavePercent", s.paydayAutoSavePercent)
            put("mainMenuOrder", JSONArray(s.mainMenuOrder))
            put("hiddenMenuItems", JSONArray(s.hiddenMenuItems))
            put("statistikCardsOrder", JSONArray(s.statistikCardsOrder))
            put("hiddenStatistikCards", JSONArray(s.hiddenStatistikCards))
            put("trenBerdampinganEnabled", s.trenBerdampinganEnabled)
            put("dashboardWidgetsOrder", JSONArray(s.dashboardWidgetsOrder))
            put("hiddenDashboardWidgets", JSONArray(s.hiddenDashboardWidgets))
        }
        root.put("settings", sObj)
        root.put("selectedWalletFilter", _selectedWalletFilter.value)

        // Timeline Scenarios
        val tsArr = JSONArray()
        _timelineScenarios.value.forEach { ts ->
            tsArr.put(
                JSONObject().apply {
                    put("id", ts.id)
                    put("amount", ts.amount)
                    put("isIncome", ts.isIncome)
                    put("note", ts.note)
                    put("date", ts.date)
                    put("recurrence", ts.recurrence)
                }
            )
        }
        root.put("timelineScenarios", tsArr)

        // 12. Custom Logos (Base64 dictionary)
        val logosObj = JSONObject()
        logosMap.forEach { (fileName, b64) ->
            logosObj.put(fileName, b64)
        }
        root.put("customLogos", logosObj)
        root.put("totalCustomLogos", logosMap.size)

        return root.toString(2)
    }

    fun importTransactionsFromJson(
        jsonStr: String,
        replaceExisting: Boolean,
        ctx: Context? = getActiveContext()
    ): Result<com.example.data.model.BackupRestoreResult> {
        return try {
            val trimmed = jsonStr.trim()
            val rootObj = if (trimmed.startsWith("{")) JSONObject(trimmed) else null

            // 0. Extract & Restore Custom Logos (Base64)
            val availableLogos = mutableMapOf<String, String>()
            val customLogosObj = rootObj?.optJSONObject("customLogos")
            if (customLogosObj != null) {
                val keys = customLogosObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val b64 = customLogosObj.optString(key, "")
                    if (b64.isNotBlank()) {
                        availableLogos[key] = b64
                    }
                }
            }

            // Also check individual customLogoBase64 inside wallets, assets, debts
            val rawWalletsArr = rootObj?.optJSONArray("wallets")
            if (rawWalletsArr != null) {
                for (i in 0 until rawWalletsArr.length()) {
                    val wo = rawWalletsArr.getJSONObject(i)
                    val k = wo.optString("customLogoKey", "")
                    val b64 = wo.optString("customLogoBase64", "")
                    if (k.isNotBlank() && b64.isNotBlank() && !availableLogos.containsKey(k)) {
                        availableLogos[k] = b64
                    }
                }
            }
            val rawAssetsArr = rootObj?.optJSONArray("assets")
            if (rawAssetsArr != null) {
                for (i in 0 until rawAssetsArr.length()) {
                    val ao = rawAssetsArr.getJSONObject(i)
                    val k = ao.optString("customLogoKey", "")
                    val b64 = ao.optString("customLogoBase64", "")
                    if (k.isNotBlank() && b64.isNotBlank() && !availableLogos.containsKey(k)) {
                        availableLogos[k] = b64
                    }
                }
            }
            val rawDebtsArr = rootObj?.optJSONArray("debts")
            if (rawDebtsArr != null) {
                for (i in 0 until rawDebtsArr.length()) {
                    val doItem = rawDebtsArr.getJSONObject(i)
                    val k = doItem.optString("customLogoKey", "")
                    val b64 = doItem.optString("customLogoBase64", "")
                    if (k.isNotBlank() && b64.isNotBlank() && !availableLogos.containsKey(k)) {
                        availableLogos[k] = b64
                    }
                }
            }

            var restoredLogoCount = 0
            if (ctx != null && availableLogos.isNotEmpty()) {
                availableLogos.forEach { (fileName, b64) ->
                    val file = ImageUploadHelper.base64ToFile(ctx, fileName, b64)
                    if (file != null && file.exists()) {
                        restoredLogoCount++
                    }
                }
            }

            // 1. Parse Transactions
            val txArr = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                rootObj?.optJSONArray("transactions") ?: JSONArray()
            }

            val importedTxs = mutableListOf<Transaction>()
            for (i in 0 until txArr.length()) {
                val o = txArr.getJSONObject(i)
                importedTxs.add(
                    Transaction(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        title = o.optString("title", "Transaksi"),
                        category = o.optString("category", "Other"),
                        subcategory = o.optString("subcategory", ""),
                        amount = o.optDouble("amount", 0.0),
                        type = try { TransactionType.valueOf(o.optString("type", "EXPENSE")) } catch (e: Exception) { TransactionType.EXPENSE },
                        accountName = o.optString("accountName", ""),
                        dayOfMonth = o.optInt("dayOfMonth", 1),
                        monthIndex = o.optInt("monthIndex", 1),
                        year = o.optInt("year", 2026),
                        dayOfWeekName = o.optString("dayOfWeekName", "Sen"),
                        note = o.optString("note", ""),
                        label = o.optString("label", ""),
                        recurrence = o.optString("recurrence", "Tidak pernah"),
                        time = o.optString("time", "12:00"),
                        toAccountName = o.optString("toAccountName", ""),
                        transferFee = o.optDouble("transferFee", 0.0),
                        targetAssetId = o.optString("targetAssetId", ""),
                        targetAssetName = o.optString("targetAssetName", ""),
                        targetDebtId = o.optString("targetDebtId", ""),
                        targetDebtName = o.optString("targetDebtName", "")
                    )
                )
            }

            // 2. Parse Wallets
            val importedWallets = mutableListOf<AccountWallet>()
            val wArr = rootObj?.optJSONArray("wallets")
            if (wArr != null) {
                for (i in 0 until wArr.length()) {
                    val o = wArr.getJSONObject(i)
                    val rawIcon = o.optString("iconName", "Bank")
                    val resolvedIcon = if (ctx != null) {
                        ImageUploadHelper.resolveOrRestoreIconPath(ctx, rawIcon, availableLogos)
                    } else rawIcon

                    importedWallets.add(
                        AccountWallet(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name", "Dompet"),
                            bankType = o.optString("bankType", "Bank · IDR"),
                            balance = o.optDouble("balance", 0.0),
                            lastCategory = o.optString("lastCategory", ""),
                            lastAmount = o.optDouble("lastAmount", 0.0),
                            lastActivityTime = o.optString("lastActivityTime", ""),
                            currency = o.optString("currency", "IDR"),
                            iconName = resolvedIcon,
                            excludeFromNetWorth = o.optBoolean("excludeFromNetWorth", false),
                            excludeFromBudget = o.optBoolean("excludeFromBudget", false)
                        )
                    )
                }
            }

            // 3. Parse Assets
            val importedAssets = mutableListOf<AssetItem>()
            val aArr = rootObj?.optJSONArray("assets")
            if (aArr != null) {
                for (i in 0 until aArr.length()) {
                    val o = aArr.getJSONObject(i)
                    val recList = mutableListOf<AssetRecord>()
                    val rArr = o.optJSONArray("records")
                    if (rArr != null) {
                        for (j in 0 until rArr.length()) {
                            val ro = rArr.getJSONObject(j)
                            recList.add(
                                AssetRecord(
                                    id = ro.optString("id", UUID.randomUUID().toString()),
                                    assetId = ro.optString("assetId", ""),
                                    tag = ro.optString("tag", "Buy"),
                                    isIncrease = ro.optBoolean("isIncrease", true),
                                    amount = ro.optDouble("amount", 0.0),
                                    sourceWalletName = ro.optString("sourceWalletName", ""),
                                    date = ro.optString("date", "29 Okt 2026"),
                                    time = ro.optString("time", "00.15"),
                                    note = ro.optString("note", ""),
                                    lotCount = ro.optDouble("lotCount", 0.0),
                                    pricePerUnit = ro.optDouble("pricePerUnit", 0.0)
                                )
                            )
                        }
                    }

                    val rawIcon = o.optString("iconUri", "")
                    val resolvedIcon = if (ctx != null) {
                        ImageUploadHelper.resolveOrRestoreIconPath(ctx, rawIcon, availableLogos)
                    } else rawIcon

                    importedAssets.add(
                        AssetItem(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name", "Aset"),
                            group = o.optString("group", "Investasi"),
                            assetType = o.optString("assetType", "Bank Account"),
                            value = o.optDouble("value", 0.0),
                            platform = o.optString("platform", "Bibit"),
                            performancePercent = o.optDouble("performancePercent", 10.2),
                            currency = o.optString("currency", "IDR"),
                            createdAtDate = o.optString("createdAtDate", "29 Okt 2026"),
                            createdAtTime = o.optString("createdAtTime", "00.15"),
                            valuationMethod = o.optString("valuationMethod", "Manual"),
                            records = recList,
                            iconUri = resolvedIcon,
                            tickerSymbol = o.optString("tickerSymbol", ""),
                            lotCount = o.optDouble("lotCount", 0.0),
                            averagePrice = o.optDouble("averagePrice", 0.0),
                            currentPrice = o.optDouble("currentPrice", 0.0),
                            dailyPriceChange = o.optDouble("dailyPriceChange", 0.0),
                            dailyPriceChangePercent = o.optDouble("dailyPriceChangePercent", 0.0),
                            lastPriceUpdate = o.optLong("lastPriceUpdate", 0L)
                        )
                    )
                }
            }

            // 4. Parse Debts
            val importedDebts = mutableListOf<DebtItem>()
            val dArr = rootObj?.optJSONArray("debts")
            if (dArr != null) {
                for (i in 0 until dArr.length()) {
                    val o = dArr.getJSONObject(i)
                    val recList = mutableListOf<DebtRecord>()
                    val rArr = o.optJSONArray("records")
                    if (rArr != null) {
                        for (j in 0 until rArr.length()) {
                            val ro = rArr.getJSONObject(j)
                            recList.add(
                                DebtRecord(
                                    id = ro.optString("id", UUID.randomUUID().toString()),
                                    debtId = ro.optString("debtId", ""),
                                    type = ro.optString("type", "Pembayaran"),
                                    amount = ro.optDouble("amount", 0.0),
                                    date = ro.optString("date", "2 Nov 2026"),
                                    time = ro.optString("time", "12.56"),
                                    sourceWalletName = ro.optString("sourceWalletName", ""),
                                    note = ro.optString("note", "")
                                )
                            )
                        }
                    }

                    val rawLogo = o.optString("institutionLogo", "")
                    val resolvedLogo = if (ctx != null) {
                        ImageUploadHelper.resolveOrRestoreIconPath(ctx, rawLogo, availableLogos)
                    } else rawLogo

                    importedDebts.add(
                        DebtItem(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name", "Pinjaman"),
                            type = try { DebtType.valueOf(o.optString("type", "BORROWED")) } catch (e: Exception) { DebtType.BORROWED },
                            totalAmount = o.optDouble("totalAmount", 0.0),
                            paidAmount = o.optDouble("paidAmount", 0.0),
                            dueDate = o.optString("dueDate", "1 Des 2027"),
                            startDate = o.optString("startDate", "28 Mar 2026"),
                            startTime = o.optString("startTime", "12:00"),
                            monthlyPayment = o.optDouble("monthlyPayment", 0.0),
                            interestRate = o.optDouble("interestRate", 0.0),
                            extraFees = o.optDouble("extraFees", 0.0),
                            categoryIcon = o.optString("categoryIcon", "CreditCard"),
                            institutionLogo = resolvedLogo,
                            tenureMonths = o.optInt("tenureMonths", 0),
                            linkedWalletName = o.optString("linkedWalletName", ""),
                            records = recList
                        )
                    )
                }
            }

            // 5. Parse Categories & Subcategories
            val importedCategories = mutableListOf<MainCategory>()
            val cArr = rootObj?.optJSONArray("categories")
            if (cArr != null) {
                for (i in 0 until cArr.length()) {
                    val co = cArr.getJSONObject(i)
                    val catType = try {
                        TransactionType.valueOf(co.optString("type", "EXPENSE"))
                    } catch (e: Exception) {
                        TransactionType.EXPENSE
                    }
                    val subList = mutableListOf<SubcategoryItem>()
                    val subArr = co.optJSONArray("subcategories")
                    if (subArr != null) {
                        for (j in 0 until subArr.length()) {
                            val so = subArr.getJSONObject(j)
                            subList.add(
                                SubcategoryItem(
                                    id = so.optString("id", UUID.randomUUID().toString()),
                                    name = so.optString("name", "Sub"),
                                    iconName = so.optString("iconName", ""),
                                    isVisible = so.optBoolean("isVisible", true)
                                )
                            )
                        }
                    }
                    importedCategories.add(
                        MainCategory(
                            id = co.optString("id", UUID.randomUUID().toString()),
                            name = co.optString("name", "Kategori"),
                            type = catType,
                            iconName = co.optString("iconName", ""),
                            isVisible = co.optBoolean("isVisible", true),
                            subcategories = subList
                        )
                    )
                }
            }

            // 6. Parse Planned Items (Perencanaan)
            val importedPlans = mutableListOf<PlannedItem>()
            val pArr = rootObj?.optJSONArray("plannedItems")
            if (pArr != null) {
                for (i in 0 until pArr.length()) {
                    val po = pArr.getJSONObject(i)
                    val section = try {
                        PlanSectionType.valueOf(po.optString("section", "EXPENSE"))
                    } catch (e: Exception) {
                        PlanSectionType.EXPENSE
                    }
                    importedPlans.add(
                        PlannedItem(
                            id = po.optString("id", UUID.randomUUID().toString()),
                            name = po.optString("name", "Rencana"),
                            category = po.optString("category", ""),
                            subcategory = po.optString("subcategory", ""),
                            amount = po.optDouble("amount", 0.0),
                            section = section
                        )
                    )
                }
            }
            val importedPlanTitle = rootObj?.optString("planTitle", "") ?: ""

            val importedFinancialPlans = mutableListOf<FinancialPlan>()
            val fpArr = rootObj?.optJSONArray("financialPlans")
            if (fpArr != null) {
                for (i in 0 until fpArr.length()) {
                    val fpo = fpArr.getJSONObject(i)
                    val items = mutableListOf<PlannedItem>()
                    val iArr = fpo.optJSONArray("items")
                    if (iArr != null) {
                        for (j in 0 until iArr.length()) {
                            val po = iArr.getJSONObject(j)
                            val sec = try { PlanSectionType.valueOf(po.optString("section", "EXPENSE")) } catch (_: Exception) { PlanSectionType.EXPENSE }
                            items.add(
                                PlannedItem(
                                    id = po.optString("id", UUID.randomUUID().toString()),
                                    name = po.optString("name", "Rencana"),
                                    category = po.optString("category", ""),
                                    subcategory = po.optString("subcategory", ""),
                                    amount = po.optDouble("amount", 0.0),
                                    section = sec
                                )
                            )
                        }
                    }
                    importedFinancialPlans.add(
                        FinancialPlan(
                            id = fpo.optString("id", UUID.randomUUID().toString()),
                            title = fpo.optString("title", "Pembagian Gaji"),
                            monthIndex = fpo.optInt("monthIndex", 11),
                            year = fpo.optInt("year", 2026),
                            items = items
                        )
                    )
                }
            }
            val importedActivePlanId = rootObj?.optString("activePlanId", "") ?: ""

            val importedSavingsGoals = mutableListOf<SavingsGoal>()
            val sgArr = rootObj?.optJSONArray("savingsGoals")
            if (sgArr != null) {
                for (i in 0 until sgArr.length()) {
                    val o = sgArr.getJSONObject(i)
                    val recordsList = mutableListOf<SavingsRecord>()
                    val recArr = o.optJSONArray("records")
                    if (recArr != null) {
                        for (j in 0 until recArr.length()) {
                            val ro = recArr.getJSONObject(j)
                            recordsList.add(
                                SavingsRecord(
                                    id = ro.optString("id", UUID.randomUUID().toString()),
                                    amount = ro.optDouble("amount", 0.0),
                                    isWithdraw = ro.optBoolean("isWithdraw", false),
                                    note = ro.optString("note", ""),
                                    dateStr = ro.optString("dateStr", ""),
                                    walletName = ro.optString("walletName", ""),
                                    linkedTransactionId = ro.optString("linkedTransactionId", "")
                                )
                            )
                        }
                    }
                    importedSavingsGoals.add(
                        SavingsGoal(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name", "Tabungan"),
                            targetAmount = o.optDouble("targetAmount", 0.0),
                            currentAmount = o.optDouble("currentAmount", 0.0),
                            targetDate = o.optString("targetDate", ""),
                            startDate = o.optString("startDate", ""),
                            currency = o.optString("currency", "IDR"),
                            iconName = o.optString("iconName", "Savings"),
                            note = o.optString("note", ""),
                            colorHex = o.optLong("colorHex", 0xFF3B82F6L),
                            isCompleted = o.optBoolean("isCompleted", false),
                            records = recordsList
                        )
                    )
                }
            }

            val importedEvents = mutableListOf<EventItem>()
            val evArr = rootObj?.optJSONArray("events")
            if (evArr != null) {
                for (i in 0 until evArr.length()) {
                    val o = evArr.getJSONObject(i)
                    val plannedList = mutableListOf<EventPlannedExpense>()
                    val peArr = o.optJSONArray("plannedExpenses")
                    if (peArr != null) {
                        for (j in 0 until peArr.length()) {
                            val po = peArr.getJSONObject(j)
                            plannedList.add(
                                EventPlannedExpense(
                                    id = po.optString("id", UUID.randomUUID().toString()),
                                    name = po.optString("name", ""),
                                    category = po.optString("category", "Other"),
                                    subcategory = po.optString("subcategory", ""),
                                    amount = po.optDouble("amount", 0.0),
                                    isPaid = po.optBoolean("isPaid", false)
                                )
                            )
                        }
                    }
                    importedEvents.add(
                        EventItem(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name", "Acara"),
                            eventType = o.optString("eventType", "Perjalanan"),
                            targetBudget = o.optDouble("targetBudget", 0.0),
                            monthIndex = o.optInt("monthIndex", 11),
                            year = o.optInt("year", 2026),
                            startDate = o.optString("startDate", ""),
                            endDate = o.optString("endDate", ""),
                            currency = o.optString("currency", "IDR"),
                            iconName = o.optString("iconName", "Celebration"),
                            colorHex = o.optLong("colorHex", 0xFF3B82F6L),
                            note = o.optString("note", ""),
                            plannedExpenses = plannedList,
                            isCompleted = o.optBoolean("isCompleted", false)
                        )
                    )
                }
            }

            // 7. Parse Budgets
            val importedBudgets = mutableListOf<BudgetCategory>()
            val bArr = rootObj?.optJSONArray("budgets")
            if (bArr != null) {
                for (i in 0 until bArr.length()) {
                    val bo = bArr.getJSONObject(i)
                    val catsArr = bo.optJSONArray("categories")
                    val catsList = mutableListOf<String>()
                    if (catsArr != null) {
                        for (cIdx in 0 until catsArr.length()) {
                            catsList.add(catsArr.getString(cIdx))
                        }
                    }
                    val accsArr = bo.optJSONArray("accountNames")
                    val accsList = mutableListOf<String>()
                    if (accsArr != null) {
                        for (aIdx in 0 until accsArr.length()) {
                            accsList.add(accsArr.getString(aIdx))
                        }
                    }
                    importedBudgets.add(
                        BudgetCategory(
                            id = bo.optString("id", UUID.randomUUID().toString()),
                            category = bo.optString("category", "Kategori"),
                            allocated = bo.optDouble("allocated", 0.0),
                            spent = bo.optDouble("spent", 0.0),
                            name = bo.optString("name", ""),
                            categories = catsList,
                            accountNames = accsList,
                            frequency = bo.optString("frequency", "Bulanan"),
                            customStartDate = bo.optString("customStartDate", ""),
                            customEndDate = bo.optString("customEndDate", "")
                        )
                    )
                }
            }

            // 8. Parse Recurring Transactions
            val importedRecurring = mutableListOf<RecurringTransaction>()
            val recArr = rootObj?.optJSONArray("recurringTransactions") ?: rootObj?.optJSONArray("recurring")
            if (recArr != null) {
                for (i in 0 until recArr.length()) {
                    val ro = recArr.getJSONObject(i)
                    val remOpts = mutableListOf<String>()
                    val remArr = ro.optJSONArray("reminderOptions")
                    if (remArr != null) {
                        for (j in 0 until remArr.length()) {
                            remOpts.add(remArr.getString(j))
                        }
                    } else {
                        remOpts.addAll(listOf("Pada hari jatuh tempo", "1 hari sebelumnya"))
                    }
                    val endDateVal = ro.optString("endDate", "")
                    importedRecurring.add(
                        RecurringTransaction(
                            id = ro.optString("id", UUID.randomUUID().toString()),
                            title = ro.optString("title", "Transaksi Berulang"),
                            amount = ro.optDouble("amount", 0.0),
                            type = try { TransactionType.valueOf(ro.optString("type", "EXPENSE")) } catch (e: Exception) { TransactionType.EXPENSE },
                            category = ro.optString("category", "Other"),
                            subcategory = ro.optString("subcategory", ""),
                            accountName = ro.optString("accountName", ""),
                            toAccountName = ro.optString("toAccountName", ""),
                            dayOfMonth = ro.optInt("dayOfMonth", 1),
                            frequency = ro.optString("frequency", "Setiap bulan"),
                            startDate = ro.optString("startDate", ""),
                            endDate = endDateVal,
                            note = ro.optString("note", ""),
                            label = ro.optString("label", ""),
                            isActive = ro.optBoolean("isActive", true),
                            autoDeduct = ro.optBoolean("autoDeduct", true),
                            lastProcessedMonthYear = ro.optString("lastProcessedMonthYear", ""),
                            endCondition = ro.optString("endCondition", if (endDateVal.isNotBlank()) "ON_DATE" else "NEVER"),
                            endAfterCount = ro.optInt("endAfterCount", 0),
                            logoName = ro.optString("logoName", ""),
                            skippedCount = ro.optInt("skippedCount", 0),
                            snoozedUntilDate = ro.optString("snoozedUntilDate", ""),
                            lastSkippedDateLabel = ro.optString("lastSkippedDateLabel", ""),
                            reminderEnabled = ro.optBoolean("reminderEnabled", false),
                            reminderOptions = remOpts
                        )
                    )
                }
            }

            // 9. Parse Upcoming Schedules
            val importedUpcoming = mutableListOf<UpcomingScheduleItem>()
            val upArr = rootObj?.optJSONArray("upcomingItems")
            if (upArr != null) {
                for (i in 0 until upArr.length()) {
                    val uo = upArr.getJSONObject(i)
                    importedUpcoming.add(
                        UpcomingScheduleItem(
                            id = uo.optString("id", UUID.randomUUID().toString()),
                            title = uo.optString("title", "Jadwal"),
                            accountName = uo.optString("accountName", ""),
                            amount = uo.optDouble("amount", 0.0),
                            isIncome = uo.optBoolean("isIncome", false),
                            dateLabel = uo.optString("dateLabel", "1 Nov"),
                            dayLabel = uo.optString("dayLabel", "Min")
                        )
                    )
                }
            }

            // 10. Parse Labels
            val importedLabels = mutableListOf<String>()
            val lArr = rootObj?.optJSONArray("labels")
            if (lArr != null) {
                for (i in 0 until lArr.length()) {
                    importedLabels.add(lArr.getString(i))
                }
            }

            // 11. Parse Settings
            val sObj = rootObj?.optJSONObject("settings")
            val importedSettings: UserSettings? = if (sObj != null) {
                val defaultSettings = UserSettings()
                val menuOrderArr = sObj.optJSONArray("mainMenuOrder")
                val parsedMenuOrder = if (menuOrderArr != null && menuOrderArr.length() > 0) {
                    MutableList(menuOrderArr.length()) { idx -> menuOrderArr.getString(idx) }
                } else {
                    defaultSettings.mainMenuOrder
                }
                val hiddenArr = sObj.optJSONArray("hiddenMenuItems")
                val parsedHidden = if (hiddenArr != null) {
                    MutableList(hiddenArr.length()) { idx -> hiddenArr.getString(idx) }
                } else {
                    defaultSettings.hiddenMenuItems
                }
                val statOrderArr = sObj.optJSONArray("statistikCardsOrder")
                val parsedStatOrder = if (statOrderArr != null && statOrderArr.length() > 0) {
                    MutableList(statOrderArr.length()) { idx -> statOrderArr.getString(idx) }
                } else {
                    defaultSettings.statistikCardsOrder
                }
                val statHiddenArr = sObj.optJSONArray("hiddenStatistikCards")
                val parsedStatHidden = if (statHiddenArr != null) {
                    MutableList(statHiddenArr.length()) { idx -> statHiddenArr.getString(idx) }
                } else {
                    defaultSettings.hiddenStatistikCards
                }
                val dashOrderArr = sObj.optJSONArray("dashboardWidgetsOrder")
                val parsedDashOrder = if (dashOrderArr != null && dashOrderArr.length() > 0) {
                    MutableList(dashOrderArr.length()) { idx -> dashOrderArr.getString(idx) }
                } else {
                    defaultSettings.dashboardWidgetsOrder
                }
                val dashHiddenArr = sObj.optJSONArray("hiddenDashboardWidgets")
                val parsedDashHidden = if (dashHiddenArr != null) {
                    MutableList(dashHiddenArr.length()) { idx -> dashHiddenArr.getString(idx) }
                } else {
                    defaultSettings.hiddenDashboardWidgets
                }
                UserSettings(
                    language = sObj.optString("language", "Indonesia"),
                    isDarkMode = sObj.optBoolean("isDarkMode", true),
                    primaryCurrency = sObj.optString("primaryCurrency", "IDR"),
                    monthlyStartDay = sObj.optInt("monthlyStartDay", 1),
                    textSizePercent = sObj.optInt("textSizePercent", 100),
                    cashAccountsOnly = sObj.optBoolean("cashAccountsOnly", false),
                    showPlannedInBalance = sObj.optBoolean("showPlannedInBalance", true),
                    saveReminder = sObj.optBoolean("saveReminder", false),
                    billReminder = sObj.optBoolean("billReminder", false),
                    budgetAlert = sObj.optBoolean("budgetAlert", true),
                    passcodeEnabled = sObj.optBoolean("passcodeEnabled", false),
                    appPasscode = sObj.optString("appPasscode", "1234"),
                    themeColorName = sObj.optString("themeColorName", "Money Tracker Blue"),
                    numberFormat = sObj.optString("numberFormat", "1.000.000 (Titik)"),
                    dateFormat = sObj.optString("dateFormat", "DD/MM/YYYY"),
                    defaultHideBalance = sObj.optBoolean("defaultHideBalance", false),
                    dailyReminderTime = sObj.optString("dailyReminderTime", "20:00"),
                    dailyReminderEnabled = sObj.optBoolean("dailyReminderEnabled", true),
                    hapticFeedbackEnabled = sObj.optBoolean("hapticFeedbackEnabled", true),
                    startDayOfWeek = sObj.optString("startDayOfWeek", "Senin"),
                    budgetWarningPercent = sObj.optInt("budgetWarningPercent", 80),
                    targetSavingsPercent = sObj.optInt("targetSavingsPercent", 20),
                    confirmDeleteTransaction = sObj.optBoolean("confirmDeleteTransaction", true),
                    mainMenuOrder = parsedMenuOrder,
                    hiddenMenuItems = parsedHidden,
                    budgetSummaryStyle = sObj.optString("budgetSummaryStyle", "Sirkular"),
                    budgetListStyle = sObj.optString("budgetListStyle", "Tabel"),
                    paydayAutoSaveEnabled = sObj.optBoolean("paydayAutoSaveEnabled", false),
                    paydayAutoSavePercent = sObj.optInt("paydayAutoSavePercent", 10),
                    statistikCardsOrder = parsedStatOrder,
                    hiddenStatistikCards = parsedStatHidden,
                    trenBerdampinganEnabled = sObj.optBoolean("trenBerdampinganEnabled", true),
                    dashboardWidgetsOrder = parsedDashOrder,
                    hiddenDashboardWidgets = parsedDashHidden,
                    hasCompletedOnboarding = sObj.optBoolean("hasCompletedOnboarding", true)
                )
            } else null

            val importedFilter = rootObj?.optString("selectedWalletFilter", "") ?: ""

            val importedScenarios = mutableListOf<com.example.data.model.TimelineScenarioItem>()
            val tsArr = rootObj?.optJSONArray("timelineScenarios")
            if (tsArr != null) {
                for (i in 0 until tsArr.length()) {
                    val to = tsArr.getJSONObject(i)
                    importedScenarios.add(
                        com.example.data.model.TimelineScenarioItem(
                            id = to.optString("id", UUID.randomUUID().toString()),
                            amount = to.optDouble("amount", 0.0),
                            isIncome = to.optBoolean("isIncome", false),
                            note = to.optString("note", ""),
                            date = to.optString("date", ""),
                            recurrence = to.optString("recurrence", "Hanya sekali")
                        )
                    )
                }
            }

            val hasAnyData = importedTxs.isNotEmpty() ||
                    importedWallets.isNotEmpty() ||
                    importedAssets.isNotEmpty() ||
                    importedDebts.isNotEmpty() ||
                    importedCategories.isNotEmpty() ||
                    importedPlans.isNotEmpty() ||
                    importedFinancialPlans.isNotEmpty() ||
                    importedSavingsGoals.isNotEmpty() ||
                    importedEvents.isNotEmpty() ||
                    importedBudgets.isNotEmpty() ||
                    importedRecurring.isNotEmpty() ||
                    importedScenarios.isNotEmpty() ||
                    importedSettings != null ||
                    availableLogos.isNotEmpty()

            val hasValidEmptyBackupKeys = replaceExisting && rootObj != null && (
                    rootObj.has("transactions") ||
                            rootObj.has("wallets") ||
                            rootObj.has("assets") ||
                            rootObj.has("debts") ||
                            rootObj.has("savingsGoals") ||
                            rootObj.has("events") ||
                            rootObj.has("budgets") ||
                            rootObj.has("recurringTransactions") ||
                            rootObj.has("recurring")
                    )

            if (!hasAnyData && !hasValidEmptyBackupKeys) {
                return Result.failure(Exception("Tidak ditemukan data yang valid dalam file cadangan JSON"))
            }

            val now = System.currentTimeMillis()
            val stampedImportedTxs = importedTxs.map { it.copy(updatedAt = now, isDeleted = false) }
            val stampedImportedWallets = importedWallets.map { it.copy(updatedAt = now, isDeleted = false) }
            val stampedImportedAssets = importedAssets.map { it.copy(updatedAt = now, isDeleted = false) }
            val stampedImportedDebts = importedDebts.map { it.copy(updatedAt = now, isDeleted = false) }
            val stampedImportedCategories = importedCategories.map { it.copy(updatedAt = now, isDeleted = false) }
            val stampedImportedBudgets = importedBudgets.map { it.copy(updatedAt = now, isDeleted = false) }
            val stampedImportedRecurring = importedRecurring.map { it.copy(updatedAt = now, isDeleted = false) }
            val stampedImportedFinancialPlans = importedFinancialPlans.map { it.copy(updatedAt = now, isDeleted = false) }
            val stampedImportedEvents = importedEvents.map { it.copy(updatedAt = now, isDeleted = false) }
            val stampedImportedSavingsGoals = importedSavingsGoals.map { it.copy(updatedAt = now, isDeleted = false) }
            val stampedImportedScenarios = importedScenarios.map { it.copy(updatedAt = now, isDeleted = false) }

            val prevTxIds = _transactions.value.map { it.id }
            val prevWalletIds = _wallets.value.map { it.id }
            val prevAssetIds = _assets.value.map { it.id }
            val prevDebtIds = _debts.value.map { it.id }
            val prevCatIds = _categories.value.map { it.id }
            val prevBudgetIds = _budgets.value.map { it.id }
            val prevRecurringIds = _recurringTransactions.value.map { it.id }
            val prevFinancialPlanIds = _financialPlans.value.map { it.id }
            val prevEventIds = _events.value.map { it.id }
            val prevSavingsGoalIds = _savingsGoals.value.map { it.id }
            val prevScenarioIds = _timelineScenarios.value.map { it.id }
            val prevLabels = _labels.value.toList()

            if (replaceExisting) {
                val replaceTxs = stampedImportedTxs.isNotEmpty() || rootObj?.has("transactions") == true
                val replaceWallets = stampedImportedWallets.isNotEmpty() || rootObj?.has("wallets") == true
                val replaceAssets = stampedImportedAssets.isNotEmpty() || rootObj?.has("assets") == true
                val replaceDebts = stampedImportedDebts.isNotEmpty() || rootObj?.has("debts") == true
                val replaceCats = stampedImportedCategories.isNotEmpty()
                val replaceBudgets = stampedImportedBudgets.isNotEmpty() || rootObj?.has("budgets") == true
                val replaceRecurring = stampedImportedRecurring.isNotEmpty() ||
                    rootObj?.has("recurringTransactions") == true ||
                    rootObj?.has("recurring") == true
                val replaceEvents = stampedImportedEvents.isNotEmpty() || rootObj?.has("events") == true
                val replaceSavings = stampedImportedSavingsGoals.isNotEmpty() || rootObj?.has("savingsGoals") == true
                val replaceScenarios = stampedImportedScenarios.isNotEmpty() || rootObj?.has("timelineScenarios") == true
                val replaceLabels = importedLabels.isNotEmpty() || rootObj?.has("labels") == true

                if (replaceTxs) {
                    _transactions.value = stampedImportedTxs
                    val newIds = stampedImportedTxs.map { it.id }.toSet()
                    softDeleteTransactionsAndSync(prevTxIds.filterNot { it in newIds }, now)
                    persistTransactionsAndSync(stampedImportedTxs)
                }
                if (replaceWallets) {
                    _wallets.value = stampedImportedWallets
                    val newIds = stampedImportedWallets.map { it.id }.toSet()
                    prevWalletIds.filterNot { it in newIds }.forEach { softDeleteWalletAndSync(it, now) }
                    persistWalletsAndSync(stampedImportedWallets, stampedImportedWallets)
                    stampedImportedWallets.forEach { persistCustomLogoFromIconPath(it.iconName, now) }
                }
                if (replaceAssets) {
                    _assets.value = stampedImportedAssets
                    val newIds = stampedImportedAssets.map { it.id }.toSet()
                    prevAssetIds.filterNot { it in newIds }.forEach { softDeleteAssetAndSync(it, now) }
                    persistAssetsAndSync(stampedImportedAssets, stampedImportedAssets)
                    stampedImportedAssets.forEach { persistCustomLogoFromIconPath(it.iconUri, now) }
                }
                if (replaceDebts) {
                    _debts.value = stampedImportedDebts
                    val newIds = stampedImportedDebts.map { it.id }.toSet()
                    prevDebtIds.filterNot { it in newIds }.forEach { softDeleteDebtAndSync(it, now) }
                    persistDebtsAndSync(stampedImportedDebts, stampedImportedDebts)
                    stampedImportedDebts.forEach { persistCustomLogoFromIconPath(it.institutionLogo, now) }
                }
                if (replaceCats) {
                    _categories.value = stampedImportedCategories
                    val newIds = stampedImportedCategories.map { it.id }.toSet()
                    prevCatIds.filterNot { it in newIds }.forEach { softDeleteCategoryAndSync(it, now) }
                    persistCategoriesAndSync(stampedImportedCategories, stampedImportedCategories)
                }
                if (stampedImportedFinancialPlans.isNotEmpty()) {
                    _financialPlans.value = stampedImportedFinancialPlans
                    val active = stampedImportedFinancialPlans.find { it.id == importedActivePlanId } ?: stampedImportedFinancialPlans.first()
                    _activePlanId.value = active.id
                    _planTitle.value = active.title
                    _plannedItems.value = active.items
                    val newIds = stampedImportedFinancialPlans.map { it.id }.toSet()
                    prevFinancialPlanIds.filterNot { it in newIds }.forEach { softDeleteFinancialPlanAndSync(it, now) }
                    persistFinancialPlansAndSync(stampedImportedFinancialPlans, stampedImportedFinancialPlans)
                } else {
                    if (importedPlans.isNotEmpty() || rootObj?.has("plannedItems") == true) _plannedItems.value = importedPlans
                    if (importedPlanTitle.isNotBlank()) _planTitle.value = importedPlanTitle
                    syncActivePlanToPlansList(now)?.let { persistFinancialPlansAndSync(listOf(it)) }
                }
                if (replaceSavings) {
                    _savingsGoals.value = stampedImportedSavingsGoals
                    val newIds = stampedImportedSavingsGoals.map { it.id }.toSet()
                    prevSavingsGoalIds.filterNot { it in newIds }.forEach { softDeleteSavingsGoalAndSync(it, now) }
                    persistSavingsGoalsAndSync(stampedImportedSavingsGoals, stampedImportedSavingsGoals)
                }
                if (replaceEvents) {
                    _events.value = stampedImportedEvents
                    val newIds = stampedImportedEvents.map { it.id }.toSet()
                    prevEventIds.filterNot { it in newIds }.forEach { softDeleteEventAndSync(it, now) }
                    persistEventsAndSync(stampedImportedEvents, stampedImportedEvents)
                }
                if (replaceBudgets) {
                    _budgets.value = stampedImportedBudgets
                    val newIds = stampedImportedBudgets.map { it.id }.toSet()
                    prevBudgetIds.filterNot { it in newIds }.forEach { softDeleteBudgetAndSync(it, now) }
                    persistBudgetsAndSync(stampedImportedBudgets, stampedImportedBudgets)
                }
                if (replaceRecurring) {
                    _recurringTransactions.value = stampedImportedRecurring
                    val newIds = stampedImportedRecurring.map { it.id }.toSet()
                    prevRecurringIds.filterNot { it in newIds }.forEach { softDeleteRecurringAndSync(it, now) }
                    persistRecurringAndSync(stampedImportedRecurring, stampedImportedRecurring)
                }
                if (importedUpcoming.isNotEmpty() || rootObj?.has("upcomingItems") == true) _upcomingItems.value = importedUpcoming
                if (replaceLabels) {
                    _labels.value = importedLabels
                    val newLabelsSet = importedLabels.map { it.trim().lowercase() }.toSet()
                    prevLabels.filterNot { it.trim().lowercase() in newLabelsSet }.forEach { softDeleteLabelAndSync(it, now) }
                    persistLabelsAndSync(importedLabels, now)
                }
                if (replaceScenarios) {
                    _timelineScenarios.value = stampedImportedScenarios
                    val newIds = stampedImportedScenarios.map { it.id }.toSet()
                    prevScenarioIds.filterNot { it in newIds }.forEach { softDeleteTimelineScenarioAndSync(it, now) }
                    persistTimelineScenariosAndSync(stampedImportedScenarios, stampedImportedScenarios)
                }
                _settings.value = (importedSettings ?: _settings.value).copy(hasCompletedOnboarding = true)
                if (importedFilter.isNotBlank()) _selectedWalletFilter.value = importedFilter
                persistAppSettingsAndSync(now)
            } else {
                // Merge Transactions
                if (stampedImportedTxs.isNotEmpty()) {
                    val existingTxIds = _transactions.value.map { it.id }.toSet()
                    val newTxs = stampedImportedTxs.filterNot { it.id in existingTxIds }
                    _transactions.value = newTxs + _transactions.value
                    persistTransactionsAndSync(newTxs)
                }
                // Merge Wallets
                if (stampedImportedWallets.isNotEmpty()) {
                    val existingWalletNames = _wallets.value.map { it.name.trim().lowercase() }.toSet()
                    val existingWalletIds = _wallets.value.map { it.id }.toSet()
                    val newWallets = stampedImportedWallets.filterNot {
                        it.id in existingWalletIds || it.name.trim().lowercase() in existingWalletNames
                    }
                    _wallets.value = _wallets.value + newWallets
                    persistWalletsAndSync(newWallets, _wallets.value)
                    newWallets.forEach { persistCustomLogoFromIconPath(it.iconName, now) }
                }
                // Merge Assets (including records, average lot & stock positions)
                if (stampedImportedAssets.isNotEmpty()) {
                    val currentList = _assets.value.toMutableList()
                    val existingMap = currentList.associateBy { it.id }.toMutableMap()
                    val existingByName = currentList.associateBy { it.name.trim().lowercase() }.toMutableMap()
                    val changedAssets = mutableListOf<AssetItem>()

                    stampedImportedAssets.forEach { impAsset ->
                        val target = existingMap[impAsset.id] ?: existingByName[impAsset.name.trim().lowercase()]
                        if (target != null) {
                            val targetRecordIds = target.records.map { it.id }.toSet()
                            val newRecords = impAsset.records.filterNot { it.id in targetRecordIds }
                            val mergedRecords = target.records + newRecords

                            val updatedLot = if (impAsset.lotCount > 0) impAsset.lotCount else target.lotCount
                            val updatedAvg = if (impAsset.averagePrice > 0) impAsset.averagePrice else target.averagePrice
                            val updatedCur = if (impAsset.currentPrice > 0) impAsset.currentPrice else target.currentPrice
                            val updatedTicker = if (impAsset.tickerSymbol.isNotBlank()) impAsset.tickerSymbol else target.tickerSymbol
                            val updatedVal = if (impAsset.value > 0) impAsset.value else target.value

                            val updated = target.copy(
                                records = mergedRecords,
                                lotCount = updatedLot,
                                averagePrice = updatedAvg,
                                currentPrice = updatedCur,
                                tickerSymbol = updatedTicker,
                                value = updatedVal,
                                updatedAt = now
                            )
                            val idx = currentList.indexOfFirst { it.id == target.id }
                            if (idx >= 0) currentList[idx] = updated
                            changedAssets.add(updated)
                        } else {
                            currentList.add(impAsset)
                            existingMap[impAsset.id] = impAsset
                            existingByName[impAsset.name.trim().lowercase()] = impAsset
                            changedAssets.add(impAsset)
                        }
                    }
                    _assets.value = currentList
                    persistAssetsAndSync(changedAssets, currentList)
                    changedAssets.forEach { persistCustomLogoFromIconPath(it.iconUri, now) }
                }
                // Merge Debts (including records & payment history)
                if (stampedImportedDebts.isNotEmpty()) {
                    val currentDebts = _debts.value.toMutableList()
                    val existingMap = currentDebts.associateBy { it.id }.toMutableMap()
                    val existingByName = currentDebts.associateBy { it.name.trim().lowercase() }.toMutableMap()
                    val changedDebts = mutableListOf<DebtItem>()

                    stampedImportedDebts.forEach { impDebt ->
                        val target = existingMap[impDebt.id] ?: existingByName[impDebt.name.trim().lowercase()]
                        if (target != null) {
                            val targetRecordIds = target.records.map { it.id }.toSet()
                            val newRecords = impDebt.records.filterNot { it.id in targetRecordIds }
                            val mergedRecords = target.records + newRecords
                            val updated = target.copy(
                                records = mergedRecords,
                                paidAmount = maxOf(target.paidAmount, impDebt.paidAmount),
                                updatedAt = now
                            )
                            val idx = currentDebts.indexOfFirst { it.id == target.id }
                            if (idx >= 0) currentDebts[idx] = updated
                            changedDebts.add(updated)
                        } else {
                            currentDebts.add(impDebt)
                            existingMap[impDebt.id] = impDebt
                            existingByName[impDebt.name.trim().lowercase()] = impDebt
                            changedDebts.add(impDebt)
                        }
                    }
                    _debts.value = currentDebts
                    persistDebtsAndSync(changedDebts, currentDebts)
                    changedDebts.forEach { persistCustomLogoFromIconPath(it.institutionLogo, now) }
                }
                // Merge Categories & Subcategories (preserving added, renamed, edited icons/visibility, and order)
                if (stampedImportedCategories.isNotEmpty()) {
                    val currentCats = _categories.value.toMutableList()
                    val matchedExistingIds = mutableSetOf<String>()
                    val importedInOrder = mutableListOf<MainCategory>()

                    stampedImportedCategories.forEach { impCat ->
                        val existingCat = currentCats.firstOrNull {
                            impCat.id.isNotBlank() && it.id == impCat.id
                        } ?: currentCats.firstOrNull {
                            it.id !in matchedExistingIds &&
                                it.name.trim().equals(impCat.name.trim(), ignoreCase = true) &&
                                it.type == impCat.type
                        }

                        if (existingCat != null) {
                            matchedExistingIds.add(existingCat.id)
                            val matchedExistingSubIds = mutableSetOf<String>()
                            val importedSubs = impCat.subcategories.map { impSub ->
                                val matchedSub = existingCat.subcategories.firstOrNull {
                                    impSub.id.isNotBlank() && it.id == impSub.id
                                } ?: existingCat.subcategories.firstOrNull {
                                    it.id !in matchedExistingSubIds &&
                                        it.name.trim().equals(impSub.name.trim(), ignoreCase = true)
                                }
                                if (matchedSub != null) {
                                    matchedExistingSubIds.add(matchedSub.id)
                                    matchedSub.copy(
                                        name = impSub.name,
                                        iconName = impSub.iconName.ifBlank { matchedSub.iconName },
                                        isVisible = impSub.isVisible
                                    )
                                } else {
                                    impSub
                                }
                            }
                            val extraLocalSubs = existingCat.subcategories.filterNot { sub ->
                                sub.id in matchedExistingSubIds ||
                                    importedSubs.any { it.name.trim().equals(sub.name.trim(), ignoreCase = true) }
                            }
                            importedInOrder.add(
                                existingCat.copy(
                                    name = impCat.name,
                                    type = impCat.type,
                                    iconName = impCat.iconName.ifBlank { existingCat.iconName },
                                    isVisible = impCat.isVisible,
                                    subcategories = importedSubs + extraLocalSubs,
                                    updatedAt = now,
                                    isDeleted = false
                                )
                            )
                        } else {
                            importedInOrder.add(impCat)
                        }
                    }

                    val localOnlyCats = currentCats.filterNot { it.id in matchedExistingIds }
                    val mergedCats = importedInOrder + localOnlyCats
                    _categories.value = mergedCats
                    persistCategoriesAndSync(mergedCats, mergedCats)
                }
                // Merge Plans
                var appSettingsChanged = false
                if (stampedImportedFinancialPlans.isNotEmpty()) {
                    val existingFpIds = _financialPlans.value.map { it.id }.toSet()
                    val newFps = stampedImportedFinancialPlans.filterNot { it.id in existingFpIds }
                    _financialPlans.value = _financialPlans.value + newFps
                    persistFinancialPlansAndSync(newFps, _financialPlans.value)
                } else if (importedPlans.isNotEmpty()) {
                    val existingPlanNames = _plannedItems.value.map { it.name.trim().lowercase() to it.section }.toSet()
                    val newPlans = importedPlans.filterNot { (it.name.trim().lowercase() to it.section) in existingPlanNames }
                    _plannedItems.value = _plannedItems.value + newPlans
                    syncActivePlanToPlansList(now)?.let { persistFinancialPlansAndSync(listOf(it)) }
                }
                if (importedPlanTitle.isNotBlank() && _planTitle.value.isBlank()) {
                    _planTitle.value = importedPlanTitle
                    syncActivePlanToPlansList(now)?.let { persistFinancialPlansAndSync(listOf(it)) }
                    appSettingsChanged = true
                }
                // Merge Savings Goals (including nested records) & Events (including plannedExpenses)
                if (stampedImportedSavingsGoals.isNotEmpty()) {
                    val currentGoals = _savingsGoals.value.toMutableList()
                    val changedGoals = mutableListOf<SavingsGoal>()
                    stampedImportedSavingsGoals.forEach { impGoal ->
                        val existingIdx = currentGoals.indexOfFirst {
                            it.id == impGoal.id || it.name.trim().equals(impGoal.name.trim(), ignoreCase = true)
                        }
                        if (existingIdx >= 0) {
                            val existing = currentGoals[existingIdx]
                            val existingRecIds = existing.records.map { it.id }.toSet()
                            val newRecords = impGoal.records.filterNot { it.id in existingRecIds }
                            if (newRecords.isNotEmpty()) {
                                val mergedRecords = existing.records + newRecords
                                val deltaAmount = newRecords.sumOf { if (it.isWithdraw) -it.amount else it.amount }
                                val updatedGoal = existing.copy(
                                    records = mergedRecords,
                                    currentAmount = (existing.currentAmount + deltaAmount).coerceAtLeast(0.0),
                                    updatedAt = now
                                )
                                currentGoals[existingIdx] = updatedGoal
                                changedGoals.add(updatedGoal)
                            }
                        } else {
                            currentGoals.add(impGoal)
                            changedGoals.add(impGoal)
                        }
                    }
                    _savingsGoals.value = currentGoals
                    persistSavingsGoalsAndSync(changedGoals, currentGoals)
                }
                if (stampedImportedEvents.isNotEmpty()) {
                    val currentEvents = _events.value.toMutableList()
                    val changedEvents = mutableListOf<EventItem>()
                    stampedImportedEvents.forEach { impEvent ->
                        val existingIdx = currentEvents.indexOfFirst {
                            it.id == impEvent.id || it.name.trim().equals(impEvent.name.trim(), ignoreCase = true)
                        }
                        if (existingIdx >= 0) {
                            val existing = currentEvents[existingIdx]
                            val existingPeIds = existing.plannedExpenses.map { it.id }.toSet()
                            val newPes = impEvent.plannedExpenses.filterNot { it.id in existingPeIds }
                            if (newPes.isNotEmpty()) {
                                val updatedEv = existing.copy(
                                    plannedExpenses = existing.plannedExpenses + newPes,
                                    updatedAt = now
                                )
                                currentEvents[existingIdx] = updatedEv
                                changedEvents.add(updatedEv)
                            }
                        } else {
                            currentEvents.add(impEvent)
                            changedEvents.add(impEvent)
                        }
                    }
                    _events.value = currentEvents
                    persistEventsAndSync(changedEvents, currentEvents)
                }
                // Merge Budgets
                if (stampedImportedBudgets.isNotEmpty()) {
                    val existingBudgetCats = _budgets.value.map { it.category.trim().lowercase() }.toSet()
                    val newBudgets = stampedImportedBudgets.filterNot { it.category.trim().lowercase() in existingBudgetCats }
                    _budgets.value = _budgets.value + newBudgets
                    persistBudgetsAndSync(newBudgets, _budgets.value)
                }
                // Merge Recurring
                if (stampedImportedRecurring.isNotEmpty()) {
                    val existingRecIds = _recurringTransactions.value.map { it.id }.toSet()
                    val existingRecKeys = _recurringTransactions.value.map {
                        Triple(it.title.trim().lowercase(), it.amount, it.frequency)
                    }.toSet()
                    val newRec = stampedImportedRecurring.filterNot {
                        it.id in existingRecIds ||
                            Triple(it.title.trim().lowercase(), it.amount, it.frequency) in existingRecKeys
                    }
                    _recurringTransactions.value = _recurringTransactions.value + newRec
                    persistRecurringAndSync(newRec, _recurringTransactions.value)
                }
                // Merge Timeline Scenarios
                if (stampedImportedScenarios.isNotEmpty()) {
                    val existingScenarioIds = _timelineScenarios.value.map { it.id }.toSet()
                    val newScenarios = stampedImportedScenarios.filterNot { it.id in existingScenarioIds }
                    _timelineScenarios.value = _timelineScenarios.value + newScenarios
                    persistTimelineScenariosAndSync(newScenarios, _timelineScenarios.value)
                }
                // Merge Upcoming
                if (importedUpcoming.isNotEmpty()) {
                    val existingUpIds = _upcomingItems.value.map { it.id }.toSet()
                    val newUp = importedUpcoming.filterNot { it.id in existingUpIds }
                    if (newUp.isNotEmpty()) {
                        _upcomingItems.value = _upcomingItems.value + newUp
                        appSettingsChanged = true
                    }
                }
                // Merge Labels
                if (importedLabels.isNotEmpty()) {
                    val existingLabelKeys = _labels.value.map { it.trim().lowercase() }.toSet()
                    val merged = (_labels.value + importedLabels).distinct()
                    _labels.value = merged
                    val newLabels = merged.filterNot { it.trim().lowercase() in existingLabelKeys }
                    persistLabelsAndSync(newLabels, now)
                }
                // Update Settings if provided and ensure onboarding is completed after restoring data
                _settings.value = (importedSettings ?: _settings.value).copy(hasCompletedOnboarding = true)
                persistAppSettingsAndSync(now)
            }

            if (ctx != null && availableLogos.isNotEmpty()) {
                val db = appDatabase
                if (db != null) {
                    val logoEntities = availableLogos.keys.mapNotNull { fileName ->
                        val file = ImageUploadHelper.resolveExistingCustomIconFile(ctx, fileName) ?: return@mapNotNull null
                        val b64 = ImageUploadHelper.fileToBase64(file) ?: return@mapNotNull null
                        CustomLogoEntity(
                            id = RoomEntityMappers.customLogoDocId(file.name),
                            fileName = file.name,
                            base64Data = b64,
                            updatedAt = now,
                            isDeleted = false
                        )
                    }
                    if (logoEntities.isNotEmpty()) {
                        ioScope.launch {
                            initialSeedComplete.await()
                            db.customLogoDao().upsertAll(logoEntities)
                            cloudSyncRepository?.pushCustomLogos(logoEntities)
                        }
                    }
                }
            }

            if (ctx != null) {
                IndonesianInstitutionHelper.syncWithWalletsAndAssets(_wallets.value, _assets.value)
                IndonesianInstitutionHelper.notifyCustomLogosChanged()
            }
            saveToDisk(ctx)
            Result.success(
                com.example.data.model.BackupRestoreResult(
                    transactionCount = importedTxs.size,
                    walletCount = importedWallets.size,
                    assetCount = importedAssets.size,
                    debtCount = importedDebts.size,
                    categoryCount = importedCategories.size,
                    planCount = importedPlans.size,
                    budgetCount = importedBudgets.size,
                    recurringCount = importedRecurring.size,
                    savingsGoalCount = importedSavingsGoals.size,
                    eventCount = importedEvents.size,
                    settingsRestored = importedSettings != null,
                    logoCount = restoredLogoCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resetAllData(createDefaultWallet: Boolean = false) {
        val now = System.currentTimeMillis()
        val prevTxIds = _transactions.value.map { it.id }
        val prevWalletIds = _wallets.value.map { it.id }
        val prevAssetIds = _assets.value.map { it.id }
        val prevDebtIds = _debts.value.map { it.id }
        val prevCatIds = _categories.value.map { it.id }
        val prevBudgetIds = _budgets.value.map { it.id }
        val prevRecurringIds = _recurringTransactions.value.map { it.id }
        val prevPlanIds = _financialPlans.value.map { it.id }
        val prevEventIds = _events.value.map { it.id }
        val prevSavingsGoalIds = _savingsGoals.value.map { it.id }
        val prevScenarioIds = _timelineScenarios.value.map { it.id }
        val prevLabels = _labels.value.toList()

        val freshWallets = if (createDefaultWallet) {
            listOf(
                AccountWallet(
                    id = "wallet_dompet_utama",
                    name = "Dompet Utama",
                    balance = 0.0,
                    bankType = "Tunai · IDR",
                    currency = "IDR",
                    updatedAt = now
                )
            )
        } else {
            emptyList()
        }
        val freshCategories = FinanceStorageHelper.getDefaultCategories().map {
            it.copy(updatedAt = now, isDeleted = false)
        }
        val defaultLabels = listOf("Investasi", "Pribadi", "Keluarga", "Tabungan")

        _transactions.value = emptyList()
        _wallets.value = freshWallets
        _assets.value = emptyList()
        _debts.value = emptyList()
        _budgets.value = emptyList()
        _upcomingItems.value = emptyList()
        _plannedItems.value = emptyList()
        _planTitle.value = "Rencana Bulanan"
        _financialPlans.value = emptyList()
        _activePlanId.value = ""
        _savingsGoals.value = emptyList()
        _events.value = emptyList()
        _recurringTransactions.value = emptyList()
        _timelineScenarios.value = emptyList()
        _selectedWalletFilter.value = "Semua"
        _categories.value = freshCategories
        _labels.value = defaultLabels
        _settings.value = UserSettings(hasCompletedOnboarding = false)
        IndonesianInstitutionHelper.syncWithWalletsAndAssets(freshWallets, emptyList())

        softDeleteTransactionsAndSync(prevTxIds, now)
        val newWalletIds = freshWallets.map { it.id }.toSet()
        prevWalletIds.filterNot { it in newWalletIds }.forEach { softDeleteWalletAndSync(it, now) }
        if (freshWallets.isNotEmpty()) {
            persistWalletsAndSync(freshWallets, freshWallets)
        }
        prevAssetIds.forEach { softDeleteAssetAndSync(it, now) }
        prevDebtIds.forEach { softDeleteDebtAndSync(it, now) }
        val newCatIds = freshCategories.map { it.id }.toSet()
        prevCatIds.filterNot { it in newCatIds }.forEach { softDeleteCategoryAndSync(it, now) }
        persistCategoriesAndSync(freshCategories, freshCategories)

        prevBudgetIds.forEach { softDeleteBudgetAndSync(it, now) }
        prevRecurringIds.forEach { softDeleteRecurringAndSync(it, now) }
        prevPlanIds.forEach { softDeleteFinancialPlanAndSync(it, now) }
        prevEventIds.forEach { softDeleteEventAndSync(it, now) }
        prevSavingsGoalIds.forEach { softDeleteSavingsGoalAndSync(it, now) }
        prevScenarioIds.forEach { softDeleteTimelineScenarioAndSync(it, now) }
        val defaultLabelKeys = defaultLabels.map { it.lowercase() }.toSet()
        prevLabels.filterNot { it.trim().lowercase() in defaultLabelKeys }.forEach { softDeleteLabelAndSync(it, now) }
        persistLabelsAndSync(defaultLabels, now)
        persistAppSettingsAndSync(now)

        saveToDisk()
    }
}
