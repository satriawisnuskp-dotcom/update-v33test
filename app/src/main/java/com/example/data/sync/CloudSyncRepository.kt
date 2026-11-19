package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.R
import com.example.data.local.AccountEntity
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettingsEntity
import com.example.data.local.AssetEntity
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CustomLogoEntity
import com.example.data.local.DebtEntity
import com.example.data.local.EventEntity
import com.example.data.local.FinancialPlanEntity
import com.example.data.local.LabelEntity
import com.example.data.local.RecurringEntity
import com.example.data.local.RoomEntityMappers
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TimelineScenarioEntity
import com.example.data.local.TransactionEntity
import com.example.util.ImageUploadHelper
import com.example.util.IndonesianInstitutionHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

sealed interface SyncStatus {
    data object Guest : SyncStatus
    data object Syncing : SyncStatus
    data class Synced(val lastSyncedAt: Long) : SyncStatus
    data object Offline : SyncStatus
    data class Error(val message: String) : SyncStatus
}

class CloudSyncRepository(
    private val firestore: FirebaseFirestore,
    private val database: AppDatabase,
    appContext: Context,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context, database: AppDatabase) : this(
        firestore = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        database = database,
        appContext = context.applicationContext,
        auth = Firebase.auth
    )

    companion object {
        private const val TAG = "CloudSyncRepository"
        private const val MAX_BATCH_OPS = 350
    }

    private val appContext: Context = appContext.applicationContext
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val migrationMutex = Mutex()
    private val listenerJobs = mutableListOf<Job>()
    private var activeSyncedUid: String? = null

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity(this.appContext))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Guest)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncedAt = MutableStateFlow(0L)
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        if (user != null) {
            startSyncForUser(user)
        } else {
            syncScope.launch {
                stopSync()
                _syncStatus.value = SyncStatus.Guest
            }
        }
    }

    init {
        configureOfflinePersistence()
        registerNetworkCallback(this.appContext)
        auth.addAuthStateListener(authStateListener)
        auth.currentUser?.let { startSyncForUser(it) }
    }

    private fun configureOfflinePersistence() {
        try {
            val settings = firestoreSettings {
                setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            }
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            Log.d(TAG, "Firestore settings already configured: ${e.message}")
        }
    }

    private fun checkCurrentConnectivity(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    private fun registerNetworkCallback(context: Context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    if (auth.currentUser != null && _syncStatus.value is SyncStatus.Offline) {
                        syncScope.launch {
                            manualSync()
                        }
                    }
                }

                override fun onLost(network: Network) {
                    val stillOnline = checkCurrentConnectivity(context)
                    _isOnline.value = stillOnline
                    if (!stillOnline && auth.currentUser != null) {
                        _syncStatus.value = SyncStatus.Offline
                    }
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    fun startSyncForUser(user: FirebaseUser) {
        if (activeSyncedUid == user.uid && listenerJobs.isNotEmpty()) return
        syncScope.launch {
            stopSync()
            activeSyncedUid = user.uid
            if (!_isOnline.value) {
                _syncStatus.value = SyncStatus.Offline
            } else {
                _syncStatus.value = SyncStatus.Syncing
            }
            migrateLocalDataToCloud(user)
            attachRealtimeListeners(user.uid)
        }
    }

    suspend fun stopSync() {
        withContext(NonCancellable) {
            listenerJobs.forEach { it.cancel() }
            listenerJobs.clear()
            activeSyncedUid = null
        }
    }

    suspend fun manualSync(): Result<Long> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Silakan login dengan akun Google terlebih dahulu."))
        return try {
            if (!_isOnline.value) {
                _syncStatus.value = SyncStatus.Offline
                return Result.failure(IllegalStateException("Perangkat sedang offline. Perubahan akan otomatis disinkronkan saat online."))
            }
            _syncStatus.value = SyncStatus.Syncing
            migrateLocalDataToCloud(user)
            attachRealtimeListeners(user.uid)
            val ts = System.currentTimeMillis()
            _lastSyncedAt.value = ts
            _syncStatus.value = SyncStatus.Synced(ts)
            Result.success(ts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Manual sync failed: ${e.message}", e)
            val errMsg = e.localizedMessage ?: "Sinkronisasi gagal"
            _syncStatus.value = if (_isOnline.value) SyncStatus.Error(errMsg) else SyncStatus.Offline
            Result.failure(e)
        }
    }

    private suspend fun ensureLocalCustomLogosIndexed(now: Long) {
        try {
            val referenced = buildList {
                database.accountDao().getAllRaw().filter { !it.isDeleted }.forEach {
                    if (ImageUploadHelper.isCustomIconPath(it.icon)) add(it.icon)
                }
                database.assetDao().getAllRaw().filter { !it.isDeleted }.forEach {
                    if (ImageUploadHelper.isCustomIconPath(it.iconUri)) add(it.iconUri)
                }
                database.debtDao().getAllRaw().filter { !it.isDeleted }.forEach {
                    if (ImageUploadHelper.isCustomIconPath(it.institutionLogo)) add(it.institutionLogo)
                }
            }
            val collected = ImageUploadHelper.collectAllCustomLogos(appContext, referenced)
            if (collected.isEmpty()) return

            val existingById = database.customLogoDao().getAllRaw().associateBy { it.id }
            val toUpsert = mutableListOf<CustomLogoEntity>()
            collected.forEach { (fileName, b64) ->
                val docId = RoomEntityMappers.customLogoDocId(fileName)
                val existing = existingById[docId]
                if (existing == null || (!existing.isDeleted && existing.base64Data != b64)) {
                    toUpsert.add(
                        CustomLogoEntity(
                            id = docId,
                            fileName = fileName,
                            base64Data = b64,
                            updatedAt = existing?.updatedAt?.coerceAtLeast(now) ?: now,
                            isDeleted = false
                        )
                    )
                }
            }
            if (toUpsert.isNotEmpty()) {
                database.customLogoDao().upsertAll(toUpsert)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed indexing local custom logos: ${e.message}")
        }
    }

    private suspend fun applyCustomLogosToDisk(items: List<CustomLogoEntity>) {
        if (items.isEmpty()) return
        var fileTouched = false
        val iconsDir = ImageUploadHelper.getCustomIconsDir(appContext)
        items.forEach { item ->
            val safeName = File(item.fileName).name
            if (safeName.isBlank()) return@forEach
            if (item.isDeleted) {
                val target = File(iconsDir, safeName)
                if (target.exists() && target.delete()) {
                    fileTouched = true
                }
            } else if (item.base64Data.isNotBlank()) {
                val restored = ImageUploadHelper.base64ToFile(appContext, safeName, item.base64Data)
                if (restored != null && restored.exists()) {
                    fileTouched = true
                }
            }
        }
        if (fileTouched) {
            withContext(Dispatchers.Main) {
                IndonesianInstitutionHelper.notifyCustomLogosChanged()
            }
        }
    }

    private suspend fun migrateLocalDataToCloud(user: FirebaseUser) {
        migrationMutex.withLock {
            val uid = user.uid
            if (auth.currentUser?.uid != uid) return
            val now = System.currentTimeMillis()

            try {
                // 1. Ensure user profile doc exists without overwriting immutable createdAt
                val userRef = firestore.collection("users").document(uid)
                val existingProfile = userRef.get().await()
                if (!existingProfile.exists()) {
                    val createMap = mutableMapOf<String, Any>(
                        "uid" to uid,
                        "displayName" to (user.displayName ?: "Pengguna").take(120),
                        "email" to (user.email ?: "").take(200),
                        "photoUrl" to (user.photoUrl?.toString() ?: "").take(600),
                        "lastSyncedAt" to now,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to now,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                    userRef.set(createMap).await()
                } else {
                    val updateMap = mapOf<String, Any>(
                        "displayName" to (user.displayName ?: "Pengguna").take(120),
                        "email" to (user.email ?: "").take(200),
                        "photoUrl" to (user.photoUrl?.toString() ?: "").take(600),
                        "lastSyncedAt" to now,
                        "updatedAt" to now,
                        "serverUpdatedAt" to FieldValue.serverTimestamp()
                    )
                    userRef.update(updateMap).await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            }

            // Ensure all disk custom logos are indexed in Room before reconciling customLogos collection
            ensureLocalCustomLogosIndexed(now)

            // 2. Reconcile & Migrate all 14 synced collections (LWW by updatedAt)
            val pendingBatchWrites = mutableListOf<Pair<String, Pair<String, Map<String, Any>>>>()

            reconcileCollection(
                uid = uid,
                collectionName = "customLogos",
                localItems = database.customLogoDao().getAllRaw(),
                getId = { it.id },
                getUpdatedAt = { it.updatedAt },
                parseDoc = ::parseCustomLogoDocument,
                toMap = ::customLogoToMap,
                upsertLocal = {
                    applyCustomLogosToDisk(it)
                    database.customLogoDao().upsertAll(it)
                },
                pendingWrites = pendingBatchWrites
            )

            reconcileCollection(
                uid = uid,
                collectionName = "accounts",
                localItems = database.accountDao().getAllRaw(),
                getId = { it.id },
                getUpdatedAt = { it.updatedAt },
                parseDoc = ::parseAccountDocument,
                toMap = ::accountToMap,
                upsertLocal = { database.accountDao().upsertAll(it) },
                pendingWrites = pendingBatchWrites
            )

            reconcileCollection(
                uid = uid,
                collectionName = "categories",
                localItems = database.categoryDao().getAllRaw(),
                getId = { it.id },
                getUpdatedAt = { it.updatedAt },
                parseDoc = ::parseCategoryDocument,
                toMap = ::categoryToMap,
                upsertLocal = { database.categoryDao().upsertAll(it) },
                pendingWrites = pendingBatchWrites
            )

            reconcileCollection(
                uid = uid,
                collectionName = "debts",
                localItems = database.debtDao().getAllRaw(),
                getId = { it.id },
                getUpdatedAt = { it.updatedAt },
                parseDoc = ::parseDebtDocument,
                toMap = ::debtToMap,
                upsertLocal = { database.debtDao().upsertAll(it) },
                pendingWrites = pendingBatchWrites
            )

            reconcileCollection(
                uid = uid,
                collectionName = "assets",
                localItems = database.assetDao().getAllRaw(),
                getId = { it.id },
                getUpdatedAt = { it.updatedAt },
                parseDoc = ::parseAssetDocument,
                toMap = ::assetToMap,
                upsertLocal = { database.assetDao().upsertAll(it) },
                pendingWrites = pendingBatchWrites
            )

            reconcileCollection(
                uid = uid,
                collectionName = "transactions",
                localItems = database.transactionDao().getAllRaw(),
                getId = { it.id },
                getUpdatedAt = { it.updatedAt },
                parseDoc = ::parseTransactionDocument,
                toMap = ::transactionToMap,
                upsertLocal = { database.transactionDao().upsertAll(it) },
                pendingWrites = pendingBatchWrites
            )

            reconcileCollection(
                uid = uid,
                collectionName = "labels",
                localItems = database.labelDao().getAllRaw(),
                getId = { it.id },
                getUpdatedAt = { it.updatedAt },
                parseDoc = ::parseLabelDocument,
                toMap = ::labelToMap,
                upsertLocal = { database.labelDao().upsertAll(it) },
                pendingWrites = pendingBatchWrites
            )

            reconcileCollection(
                uid = uid,
                collectionName = "budgets",
                localItems = database.budgetDao().getAllRaw(),
                getId = { it.id },
                getUpdatedAt = { it.updatedAt },
                parseDoc = ::parseBudgetDocument,
                toMap = ::budgetToMap,
                upsertLocal = { database.budgetDao().upsertAll(it) },
                pendingWrites = pendingBatchWrites
            )

            reconcileCollection(
                uid = uid,
                collectionName = "recurringTransactions",
                localItems = database.recurringDao().getAllRaw(),
                getId = { it.id },
                getUpdatedAt = { it.updatedAt },
                parseDoc = ::parseRecurringDocument,
                toMap = ::recurringToMap,
                upsertLocal = { database.recurringDao().upsertAll(it) },
                pendingWrites = pendingBatchWrites
            )

            reconcileCollection(
                uid = uid,
                collectionName = "financialPlans",
                localItems = database.financialPlanDao().getAllRaw(),
                getId = { it.id },
                getUpdatedAt = { it.updatedAt },
                parseDoc = ::parseFinancialPlanDocument,
                toMap = ::financialPlanToMap,
                upsertLocal = { database.financialPlanDao().upsertAll(it) },
                pendingWrites = pendingBatchWrites
            )

            reconcileCollection(
                uid = uid,
                collectionName = "events",
                localItems = database.eventDao().getAllRaw(),
                getId = { it.id },
                getUpdatedAt = { it.updatedAt },
                parseDoc = ::parseEventDocument,
                toMap = ::eventToMap,
                upsertLocal = { database.eventDao().upsertAll(it) },
                pendingWrites = pendingBatchWrites
            )

            reconcileCollection(
                uid = uid,
                collectionName = "savingsGoals",
                localItems = database.savingsGoalDao().getAllRaw(),
                getId = { it.id },
                getUpdatedAt = { it.updatedAt },
                parseDoc = ::parseSavingsGoalDocument,
                toMap = ::savingsGoalToMap,
                upsertLocal = { database.savingsGoalDao().upsertAll(it) },
                pendingWrites = pendingBatchWrites
            )

            reconcileCollection(
                uid = uid,
                collectionName = "timelineScenarios",
                localItems = database.timelineScenarioDao().getAllRaw(),
                getId = { it.id },
                getUpdatedAt = { it.updatedAt },
                parseDoc = ::parseTimelineScenarioDocument,
                toMap = ::timelineScenarioToMap,
                upsertLocal = { database.timelineScenarioDao().upsertAll(it) },
                pendingWrites = pendingBatchWrites
            )

            reconcileAppSettings(uid, pendingBatchWrites)

            if (pendingBatchWrites.isNotEmpty() && auth.currentUser?.uid == uid) {
                pendingBatchWrites.chunked(MAX_BATCH_OPS).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { (colName, docPair) ->
                        val ref = firestore.collection("users").document(uid)
                            .collection(colName).document(docPair.first)
                        batch.set(ref, docPair.second, SetOptions.merge())
                    }
                    try {
                        batch.commit().await()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        handleFirestoreError(e, OperationType.WRITE, "users/$uid/batch")
                        throw e
                    }
                }
            }

            val syncedTime = System.currentTimeMillis()
            _lastSyncedAt.value = syncedTime
            _syncStatus.value = if (_isOnline.value) SyncStatus.Synced(syncedTime) else SyncStatus.Offline
        }
    }

    private suspend fun reconcileAppSettings(
        uid: String,
        pendingWrites: MutableList<Pair<String, Pair<String, Map<String, Any>>>>
    ) {
        val path = "users/$uid/appSettings/main"
        val docSnap = try {
            firestore.collection("users").document(uid)
                .collection("appSettings").document("main").get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }

        val remote = if (docSnap.exists()) parseAppSettingsDocument(docSnap) else null
        val local = database.appSettingsDao().getMain()

        if (remote != null && (local == null || remote.updatedAt > local.updatedAt)) {
            database.appSettingsDao().upsert(remote)
        } else if (local != null && (remote == null || local.updatedAt > remote.updatedAt)) {
            pendingWrites.add("appSettings" to ("main" to appSettingsToMap(local)))
        }
    }

    private suspend fun <T> reconcileCollection(
        uid: String,
        collectionName: String,
        localItems: List<T>,
        getId: (T) -> String,
        getUpdatedAt: (T) -> Long,
        parseDoc: (DocumentSnapshot) -> T?,
        toMap: (T) -> Map<String, Any>,
        upsertLocal: suspend (List<T>) -> Unit,
        pendingWrites: MutableList<Pair<String, Pair<String, Map<String, Any>>>>
    ) {
        val path = "users/$uid/$collectionName"
        val remoteSnap = try {
            firestore.collection("users").document(uid).collection(collectionName).get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }

        val remoteMap = mutableMapOf<String, T>()
        remoteSnap.documents.forEach { doc ->
            parseDoc(doc)?.let { parsed ->
                remoteMap[getId(parsed)] = parsed
            }
        }

        val localMap = localItems.associateBy { getId(it) }
        val toSaveLocally = mutableListOf<T>()

        // Remote -> Local (Last-Write-Wins)
        remoteMap.forEach { (id, remoteItem) ->
            val localItem = localMap[id]
            if (localItem == null || getUpdatedAt(remoteItem) > getUpdatedAt(localItem)) {
                toSaveLocally.add(remoteItem)
            }
        }
        if (toSaveLocally.isNotEmpty()) {
            upsertLocal(toSaveLocally)
        }

        // Local -> Cloud (Upload missing or newer local items)
        localItems.forEach { localItem ->
            val id = getId(localItem)
            val remoteItem = remoteMap[id]
            if (remoteItem == null || getUpdatedAt(localItem) > getUpdatedAt(remoteItem)) {
                pendingWrites.add(collectionName to (RoomEntityMappers.sanitizeFirestoreId(id) to toMap(localItem)))
            }
        }
    }

    private fun attachRealtimeListeners(uid: String) {
        if (listenerJobs.isNotEmpty() && activeSyncedUid == uid) return
        listenerJobs.forEach { it.cancel() }
        listenerJobs.clear()

        listenerJobs += observeAndMergeCollection(
            uid = uid,
            collectionName = "customLogos",
            parseDoc = ::parseCustomLogoDocument,
            getId = { it.id },
            getUpdatedAt = { it.updatedAt },
            getAllLocalRaw = { database.customLogoDao().getAllRaw() },
            upsertLocal = {
                applyCustomLogosToDisk(it)
                database.customLogoDao().upsertAll(it)
            }
        )

        listenerJobs += observeAndMergeCollection(
            uid = uid,
            collectionName = "transactions",
            parseDoc = ::parseTransactionDocument,
            getId = { it.id },
            getUpdatedAt = { it.updatedAt },
            getAllLocalRaw = { database.transactionDao().getAllRaw() },
            upsertLocal = { database.transactionDao().upsertAll(it) }
        )

        listenerJobs += observeAndMergeCollection(
            uid = uid,
            collectionName = "accounts",
            parseDoc = ::parseAccountDocument,
            getId = { it.id },
            getUpdatedAt = { it.updatedAt },
            getAllLocalRaw = { database.accountDao().getAllRaw() },
            upsertLocal = { database.accountDao().upsertAll(it) }
        )

        listenerJobs += observeAndMergeCollection(
            uid = uid,
            collectionName = "categories",
            parseDoc = ::parseCategoryDocument,
            getId = { it.id },
            getUpdatedAt = { it.updatedAt },
            getAllLocalRaw = { database.categoryDao().getAllRaw() },
            upsertLocal = { database.categoryDao().upsertAll(it) }
        )

        listenerJobs += observeAndMergeCollection(
            uid = uid,
            collectionName = "debts",
            parseDoc = ::parseDebtDocument,
            getId = { it.id },
            getUpdatedAt = { it.updatedAt },
            getAllLocalRaw = { database.debtDao().getAllRaw() },
            upsertLocal = { database.debtDao().upsertAll(it) }
        )

        listenerJobs += observeAndMergeCollection(
            uid = uid,
            collectionName = "assets",
            parseDoc = ::parseAssetDocument,
            getId = { it.id },
            getUpdatedAt = { it.updatedAt },
            getAllLocalRaw = { database.assetDao().getAllRaw() },
            upsertLocal = { database.assetDao().upsertAll(it) }
        )

        listenerJobs += observeAndMergeCollection(
            uid = uid,
            collectionName = "labels",
            parseDoc = ::parseLabelDocument,
            getId = { it.id },
            getUpdatedAt = { it.updatedAt },
            getAllLocalRaw = { database.labelDao().getAllRaw() },
            upsertLocal = { database.labelDao().upsertAll(it) }
        )

        listenerJobs += observeAndMergeCollection(
            uid = uid,
            collectionName = "budgets",
            parseDoc = ::parseBudgetDocument,
            getId = { it.id },
            getUpdatedAt = { it.updatedAt },
            getAllLocalRaw = { database.budgetDao().getAllRaw() },
            upsertLocal = { database.budgetDao().upsertAll(it) }
        )

        listenerJobs += observeAndMergeCollection(
            uid = uid,
            collectionName = "recurringTransactions",
            parseDoc = ::parseRecurringDocument,
            getId = { it.id },
            getUpdatedAt = { it.updatedAt },
            getAllLocalRaw = { database.recurringDao().getAllRaw() },
            upsertLocal = { database.recurringDao().upsertAll(it) }
        )

        listenerJobs += observeAndMergeCollection(
            uid = uid,
            collectionName = "financialPlans",
            parseDoc = ::parseFinancialPlanDocument,
            getId = { it.id },
            getUpdatedAt = { it.updatedAt },
            getAllLocalRaw = { database.financialPlanDao().getAllRaw() },
            upsertLocal = { database.financialPlanDao().upsertAll(it) }
        )

        listenerJobs += observeAndMergeCollection(
            uid = uid,
            collectionName = "events",
            parseDoc = ::parseEventDocument,
            getId = { it.id },
            getUpdatedAt = { it.updatedAt },
            getAllLocalRaw = { database.eventDao().getAllRaw() },
            upsertLocal = { database.eventDao().upsertAll(it) }
        )

        listenerJobs += observeAndMergeCollection(
            uid = uid,
            collectionName = "savingsGoals",
            parseDoc = ::parseSavingsGoalDocument,
            getId = { it.id },
            getUpdatedAt = { it.updatedAt },
            getAllLocalRaw = { database.savingsGoalDao().getAllRaw() },
            upsertLocal = { database.savingsGoalDao().upsertAll(it) }
        )

        listenerJobs += observeAndMergeCollection(
            uid = uid,
            collectionName = "timelineScenarios",
            parseDoc = ::parseTimelineScenarioDocument,
            getId = { it.id },
            getUpdatedAt = { it.updatedAt },
            getAllLocalRaw = { database.timelineScenarioDao().getAllRaw() },
            upsertLocal = { database.timelineScenarioDao().upsertAll(it) }
        )

        listenerJobs += observeAndMergeAppSettings(uid)
    }

    private fun observeAndMergeAppSettings(uid: String): Job {
        val path = "users/$uid/appSettings/main"
        return syncScope.launch {
            firestore.collection("users").document(uid)
                .collection("appSettings").document("main")
                .snapshots()
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    handleFirestoreError(rootError, OperationType.GET, path)
                    _syncStatus.value = if (_isOnline.value) {
                        SyncStatus.Error(rootError.localizedMessage ?: "Gagal memantau pengaturan")
                    } else {
                        SyncStatus.Offline
                    }
                }
                .collect { docSnap ->
                    if (docSnap.exists()) {
                        val remote = parseAppSettingsDocument(docSnap)
                        if (remote != null) {
                            val local = database.appSettingsDao().getMain()
                            if (local == null || remote.updatedAt > local.updatedAt) {
                                database.appSettingsDao().upsert(remote)
                            }
                        }
                    }
                    val ts = System.currentTimeMillis()
                    _lastSyncedAt.value = ts
                    _syncStatus.value = if (docSnap.metadata.isFromCache && !_isOnline.value) {
                        SyncStatus.Offline
                    } else {
                        SyncStatus.Synced(ts)
                    }
                }
        }
    }

    private fun <T> observeAndMergeCollection(
        uid: String,
        collectionName: String,
        parseDoc: (DocumentSnapshot) -> T?,
        getId: (T) -> String,
        getUpdatedAt: (T) -> Long,
        getAllLocalRaw: suspend () -> List<T>,
        upsertLocal: suspend (List<T>) -> Unit
    ): Job {
        val path = "users/$uid/$collectionName"
        return syncScope.launch {
            firestore.collection("users").document(uid).collection(collectionName)
                .snapshots()
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    handleFirestoreError(rootError, OperationType.LIST, path)
                    _syncStatus.value = if (_isOnline.value) {
                        SyncStatus.Error(rootError.localizedMessage ?: "Gagal memantau sinkronisasi")
                    } else {
                        SyncStatus.Offline
                    }
                }
                .collect { querySnapshot ->
                    val localMap = getAllLocalRaw().associateBy { getId(it) }
                    val winners = mutableListOf<T>()
                    querySnapshot.documents.forEach { doc ->
                        val remote = parseDoc(doc) ?: return@forEach
                        val local = localMap[getId(remote)]
                        if (local == null || getUpdatedAt(remote) > getUpdatedAt(local)) {
                            winners.add(remote)
                        }
                    }
                    if (winners.isNotEmpty()) {
                        upsertLocal(winners)
                    }
                    val ts = System.currentTimeMillis()
                    _lastSyncedAt.value = ts
                    _syncStatus.value = if (querySnapshot.metadata.isFromCache && !_isOnline.value) {
                        SyncStatus.Offline
                    } else {
                        SyncStatus.Synced(ts)
                    }
                }
        }
    }

    // ==================== PUSH METHODS ====================

    fun pushTransaction(entity: TransactionEntity) {
        pushSingle("transactions", entity.id, transactionToMap(entity))
    }

    fun pushTransactions(entities: List<TransactionEntity>) {
        pushBatch("transactions", entities.map { it.id to transactionToMap(it) })
    }

    fun pushAccount(entity: AccountEntity) {
        pushSingle("accounts", entity.id, accountToMap(entity))
    }

    fun pushAccounts(entities: List<AccountEntity>) {
        pushBatch("accounts", entities.map { it.id to accountToMap(it) })
    }

    fun pushCategory(entity: CategoryEntity) {
        pushSingle("categories", entity.id, categoryToMap(entity))
    }

    fun pushCategories(entities: List<CategoryEntity>) {
        pushBatch("categories", entities.map { it.id to categoryToMap(it) })
    }

    fun pushDebt(entity: DebtEntity) {
        pushSingle("debts", entity.id, debtToMap(entity))
    }

    fun pushDebts(entities: List<DebtEntity>) {
        pushBatch("debts", entities.map { it.id to debtToMap(it) })
    }

    fun pushAsset(entity: AssetEntity) {
        pushSingle("assets", entity.id, assetToMap(entity))
    }

    fun pushAssets(entities: List<AssetEntity>) {
        pushBatch("assets", entities.map { it.id to assetToMap(it) })
    }

    fun pushLabel(entity: LabelEntity) {
        pushSingle("labels", entity.id, labelToMap(entity))
    }

    fun pushLabels(entities: List<LabelEntity>) {
        pushBatch("labels", entities.map { it.id to labelToMap(it) })
    }

    fun pushBudget(entity: BudgetEntity) {
        pushSingle("budgets", entity.id, budgetToMap(entity))
    }

    fun pushBudgets(entities: List<BudgetEntity>) {
        pushBatch("budgets", entities.map { it.id to budgetToMap(it) })
    }

    fun pushRecurring(entity: RecurringEntity) {
        pushSingle("recurringTransactions", entity.id, recurringToMap(entity))
    }

    fun pushRecurrings(entities: List<RecurringEntity>) {
        pushBatch("recurringTransactions", entities.map { it.id to recurringToMap(it) })
    }

    fun pushFinancialPlan(entity: FinancialPlanEntity) {
        pushSingle("financialPlans", entity.id, financialPlanToMap(entity))
    }

    fun pushFinancialPlans(entities: List<FinancialPlanEntity>) {
        pushBatch("financialPlans", entities.map { it.id to financialPlanToMap(it) })
    }

    fun pushEvent(entity: EventEntity) {
        pushSingle("events", entity.id, eventToMap(entity))
    }

    fun pushEvents(entities: List<EventEntity>) {
        pushBatch("events", entities.map { it.id to eventToMap(it) })
    }

    fun pushSavingsGoal(entity: SavingsGoalEntity) {
        pushSingle("savingsGoals", entity.id, savingsGoalToMap(entity))
    }

    fun pushSavingsGoals(entities: List<SavingsGoalEntity>) {
        pushBatch("savingsGoals", entities.map { it.id to savingsGoalToMap(it) })
    }

    fun pushTimelineScenario(entity: TimelineScenarioEntity) {
        pushSingle("timelineScenarios", entity.id, timelineScenarioToMap(entity))
    }

    fun pushTimelineScenarios(entities: List<TimelineScenarioEntity>) {
        pushBatch("timelineScenarios", entities.map { it.id to timelineScenarioToMap(it) })
    }

    fun pushAppSettings(entity: AppSettingsEntity) {
        pushSingle("appSettings", "main", appSettingsToMap(entity))
    }

    fun pushCustomLogo(entity: CustomLogoEntity) {
        pushSingle("customLogos", entity.id, customLogoToMap(entity))
    }

    fun pushCustomLogos(entities: List<CustomLogoEntity>) {
        pushBatch("customLogos", entities.map { it.id to customLogoToMap(it) })
    }

    private fun pushSingle(collectionName: String, docId: String, payload: Map<String, Any>) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val safeId = RoomEntityMappers.sanitizeFirestoreId(docId)
        syncScope.launch {
            try {
                if (auth.currentUser?.uid != uid) return@launch
                if (_isOnline.value) _syncStatus.value = SyncStatus.Syncing
                val docRef = firestore.collection("users").document(uid)
                    .collection(collectionName).document(safeId)
                docRef.set(payload, SetOptions.merge()).await()
                val ts = System.currentTimeMillis()
                _lastSyncedAt.value = ts
                _syncStatus.value = if (_isOnline.value) SyncStatus.Synced(ts) else SyncStatus.Offline
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "users/$uid/$collectionName/$safeId")
                _syncStatus.value = if (_isOnline.value) {
                    SyncStatus.Error(e.localizedMessage ?: "Gagal mengunggah perubahan")
                } else {
                    SyncStatus.Offline
                }
            }
        }
    }

    private fun pushBatch(collectionName: String, items: List<Pair<String, Map<String, Any>>>) {
        if (items.isEmpty()) return
        val user = auth.currentUser ?: return
        val uid = user.uid
        syncScope.launch {
            try {
                if (auth.currentUser?.uid != uid) return@launch
                if (_isOnline.value) _syncStatus.value = SyncStatus.Syncing
                items.chunked(MAX_BATCH_OPS).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { (rawId, payload) ->
                        val safeId = RoomEntityMappers.sanitizeFirestoreId(rawId)
                        val docRef = firestore.collection("users").document(uid)
                            .collection(collectionName).document(safeId)
                        batch.set(docRef, payload, SetOptions.merge())
                    }
                    batch.commit().await()
                }
                val ts = System.currentTimeMillis()
                _lastSyncedAt.value = ts
                _syncStatus.value = if (_isOnline.value) SyncStatus.Synced(ts) else SyncStatus.Offline
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "users/$uid/$collectionName/batch")
                _syncStatus.value = if (_isOnline.value) {
                    SyncStatus.Error(e.localizedMessage ?: "Gagal menyinkronkan data")
                } else {
                    SyncStatus.Offline
                }
            }
        }
    }

    // ==================== FIRESTORE SERIALIZATION & PARSING (1:1 WITH RULES) ====================

    private fun transactionToMap(e: TransactionEntity): Map<String, Any> {
        val safeId = RoomEntityMappers.sanitizeFirestoreId(e.id)
        val safeType = if (e.type in listOf("EXPENSE", "INCOME", "TRANSFER")) e.type else "EXPENSE"
        return mapOf(
            "id" to safeId,
            "title" to e.title.take(200),
            "type" to safeType,
            "amount" to e.amount.coerceAtLeast(0.0),
            "categoryId" to e.categoryId.take(120),
            "subcategory" to e.subcategory.take(120),
            "accountId" to e.accountId.take(128),
            "accountName" to e.accountName.take(120),
            "toAccountId" to e.toAccountId.take(128),
            "toAccountName" to e.toAccountName.take(120),
            "note" to e.note.take(1000),
            "label" to e.label.take(200),
            "recurrence" to e.recurrence.take(120),
            "date" to e.date.coerceAtLeast(0L),
            "dayOfMonth" to e.dayOfMonth.coerceIn(1, 31),
            "monthIndex" to e.monthIndex.coerceIn(1, 12),
            "year" to e.year.coerceIn(1970, 2100),
            "dayOfWeekName" to e.dayOfWeekName.take(30),
            "time" to e.time.take(20),
            "transferFee" to e.transferFee.coerceAtLeast(0.0),
            "targetAssetId" to e.targetAssetId.take(128),
            "targetAssetName" to e.targetAssetName.take(120),
            "targetDebtId" to e.targetDebtId.take(128),
            "targetDebtName" to e.targetDebtName.take(120),
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "isDeleted" to e.isDeleted,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseTransactionDocument(doc: DocumentSnapshot): TransactionEntity? {
        val data = doc.data ?: return null
        val epoch = (data["date"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = epoch }
        return TransactionEntity(
            id = doc.id,
            title = data["title"] as? String ?: "",
            type = data["type"] as? String ?: "EXPENSE",
            amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
            categoryId = data["categoryId"] as? String ?: "Other",
            subcategory = data["subcategory"] as? String ?: "",
            accountId = data["accountId"] as? String ?: "",
            accountName = data["accountName"] as? String ?: (data["accountId"] as? String ?: ""),
            toAccountId = data["toAccountId"] as? String ?: "",
            toAccountName = data["toAccountName"] as? String ?: "",
            note = data["note"] as? String ?: "",
            label = data["label"] as? String ?: "",
            recurrence = data["recurrence"] as? String ?: "Tidak pernah",
            date = epoch,
            dayOfMonth = (data["dayOfMonth"] as? Number)?.toInt() ?: cal.get(Calendar.DAY_OF_MONTH),
            monthIndex = (data["monthIndex"] as? Number)?.toInt() ?: (cal.get(Calendar.MONTH) + 1),
            year = (data["year"] as? Number)?.toInt() ?: cal.get(Calendar.YEAR),
            dayOfWeekName = data["dayOfWeekName"] as? String ?: "Sen",
            time = data["time"] as? String ?: "12:00",
            transferFee = (data["transferFee"] as? Number)?.toDouble() ?: 0.0,
            targetAssetId = data["targetAssetId"] as? String ?: "",
            targetAssetName = data["targetAssetName"] as? String ?: "",
            targetDebtId = data["targetDebtId"] as? String ?: "",
            targetDebtName = data["targetDebtName"] as? String ?: "",
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L,
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }

    private fun accountToMap(e: AccountEntity): Map<String, Any> {
        val safeId = RoomEntityMappers.sanitizeFirestoreId(e.id)
        return mapOf(
            "id" to safeId,
            "name" to e.name.ifBlank { "Dompet" }.take(120),
            "type" to e.type.take(80),
            "initialBalance" to e.initialBalance,
            "currentBalance" to e.currentBalance,
            "icon" to e.icon.take(500),
            "color" to e.color,
            "currency" to e.currency.take(16),
            "lastCategory" to e.lastCategory.take(120),
            "lastAmount" to e.lastAmount.coerceAtLeast(0.0),
            "lastActivityTime" to e.lastActivityTime.take(60),
            "excludeFromNetWorth" to e.excludeFromNetWorth,
            "excludeFromBudget" to e.excludeFromBudget,
            "sortOrder" to e.sortOrder,
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "isDeleted" to e.isDeleted,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseAccountDocument(doc: DocumentSnapshot): AccountEntity? {
        val data = doc.data ?: return null
        val rawIcon = data["icon"] as? String ?: "Bank"
        return AccountEntity(
            id = doc.id,
            name = data["name"] as? String ?: "Dompet",
            type = data["type"] as? String ?: "Bank · IDR",
            initialBalance = (data["initialBalance"] as? Number)?.toDouble() ?: 0.0,
            currentBalance = (data["currentBalance"] as? Number)?.toDouble() ?: 0.0,
            icon = ImageUploadHelper.normalizeCustomIconPathForLocalDevice(appContext, rawIcon),
            color = (data["color"] as? Number)?.toLong() ?: 0xFF3B82F6L,
            currency = data["currency"] as? String ?: "IDR",
            lastCategory = data["lastCategory"] as? String ?: "Other",
            lastAmount = (data["lastAmount"] as? Number)?.toDouble() ?: 0.0,
            lastActivityTime = data["lastActivityTime"] as? String ?: "Baru saja",
            excludeFromNetWorth = data["excludeFromNetWorth"] as? Boolean ?: false,
            excludeFromBudget = data["excludeFromBudget"] as? Boolean ?: false,
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L,
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }

    private fun categoryToMap(e: CategoryEntity): Map<String, Any> {
        val safeId = RoomEntityMappers.sanitizeFirestoreId(e.id)
        val safeType = if (e.type in listOf("EXPENSE", "INCOME", "TRANSFER")) e.type else "EXPENSE"
        return mapOf(
            "id" to safeId,
            "name" to e.name.ifBlank { "Kategori" }.take(120),
            "type" to safeType,
            "icon" to e.icon.take(240),
            "color" to e.color,
            "isVisible" to e.isVisible,
            "subcategoriesJson" to e.subcategoriesJson.take(50000),
            "sortOrder" to e.sortOrder,
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "isDeleted" to e.isDeleted,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseCategoryDocument(doc: DocumentSnapshot): CategoryEntity? {
        val data = doc.data ?: return null
        return CategoryEntity(
            id = doc.id,
            name = data["name"] as? String ?: "Kategori",
            type = data["type"] as? String ?: "EXPENSE",
            icon = data["icon"] as? String ?: "",
            color = (data["color"] as? Number)?.toLong() ?: 0xFF3B82F6L,
            isVisible = data["isVisible"] as? Boolean ?: true,
            subcategoriesJson = data["subcategoriesJson"] as? String ?: "[]",
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L,
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }

    private fun debtToMap(e: DebtEntity): Map<String, Any> {
        val safeId = RoomEntityMappers.sanitizeFirestoreId(e.id)
        val safeType = if (e.type in listOf("BORROWED", "LENT")) e.type else "BORROWED"
        return mapOf(
            "id" to safeId,
            "personName" to e.personName.ifBlank { "Pinjaman" }.take(120),
            "amount" to e.amount.coerceAtLeast(0.0),
            "paidAmount" to e.paidAmount.coerceAtLeast(0.0),
            "type" to safeType,
            "dueDate" to e.dueDate.take(60),
            "isPaid" to e.isPaid,
            "startDate" to e.startDate.take(60),
            "startTime" to e.startTime.take(20),
            "monthlyPayment" to e.monthlyPayment.coerceAtLeast(0.0),
            "interestRate" to e.interestRate.coerceAtLeast(0.0),
            "extraFees" to e.extraFees.coerceAtLeast(0.0),
            "categoryIcon" to e.categoryIcon.take(80),
            "institutionLogo" to e.institutionLogo.take(500),
            "tenureMonths" to e.tenureMonths.coerceAtLeast(0),
            "linkedWalletName" to e.linkedWalletName.take(120),
            "recordsJson" to e.recordsJson.take(50000),
            "sortOrder" to e.sortOrder,
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "isDeleted" to e.isDeleted,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseDebtDocument(doc: DocumentSnapshot): DebtEntity? {
        val data = doc.data ?: return null
        val rawLogo = data["institutionLogo"] as? String ?: ""
        return DebtEntity(
            id = doc.id,
            personName = data["personName"] as? String ?: "Pinjaman",
            amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
            paidAmount = (data["paidAmount"] as? Number)?.toDouble() ?: 0.0,
            type = data["type"] as? String ?: "BORROWED",
            dueDate = data["dueDate"] as? String ?: "",
            isPaid = data["isPaid"] as? Boolean ?: false,
            startDate = data["startDate"] as? String ?: "",
            startTime = data["startTime"] as? String ?: "12:00",
            monthlyPayment = (data["monthlyPayment"] as? Number)?.toDouble() ?: 0.0,
            interestRate = (data["interestRate"] as? Number)?.toDouble() ?: 0.0,
            extraFees = (data["extraFees"] as? Number)?.toDouble() ?: 0.0,
            categoryIcon = data["categoryIcon"] as? String ?: "CreditCard",
            institutionLogo = ImageUploadHelper.normalizeCustomIconPathForLocalDevice(appContext, rawLogo),
            tenureMonths = (data["tenureMonths"] as? Number)?.toInt() ?: 0,
            linkedWalletName = data["linkedWalletName"] as? String ?: "",
            recordsJson = data["recordsJson"] as? String ?: "[]",
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L,
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }

    private fun assetToMap(e: AssetEntity): Map<String, Any> {
        val safeId = RoomEntityMappers.sanitizeFirestoreId(e.id)
        return mapOf(
            "id" to safeId,
            "name" to e.name.ifBlank { "Aset" }.take(120),
            "groupName" to e.groupName.take(80),
            "assetType" to e.assetType.take(80),
            "value" to e.value.coerceAtLeast(0.0),
            "platform" to e.platform.take(120),
            "performancePercent" to e.performancePercent,
            "currency" to e.currency.take(16),
            "createdAtDate" to e.createdAtDate.take(60),
            "createdAtTime" to e.createdAtTime.take(20),
            "valuationMethod" to e.valuationMethod.take(60),
            "recordsJson" to e.recordsJson.take(50000),
            "iconUri" to e.iconUri.take(500),
            "tickerSymbol" to e.tickerSymbol.take(40),
            "lotCount" to e.lotCount.coerceAtLeast(0.0),
            "averagePrice" to e.averagePrice.coerceAtLeast(0.0),
            "currentPrice" to e.currentPrice.coerceAtLeast(0.0),
            "dailyPriceChange" to e.dailyPriceChange,
            "dailyPriceChangePercent" to e.dailyPriceChangePercent,
            "lastPriceUpdate" to e.lastPriceUpdate.coerceAtLeast(0L),
            "sortOrder" to e.sortOrder,
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "isDeleted" to e.isDeleted,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseAssetDocument(doc: DocumentSnapshot): AssetEntity? {
        val data = doc.data ?: return null
        val rawIconUri = data["iconUri"] as? String ?: ""
        return AssetEntity(
            id = doc.id,
            name = data["name"] as? String ?: "Aset",
            groupName = data["groupName"] as? String ?: "Investasi",
            assetType = data["assetType"] as? String ?: "Bank Account",
            value = (data["value"] as? Number)?.toDouble() ?: 0.0,
            platform = data["platform"] as? String ?: "Bibit",
            performancePercent = (data["performancePercent"] as? Number)?.toDouble() ?: 0.0,
            currency = data["currency"] as? String ?: "IDR",
            createdAtDate = data["createdAtDate"] as? String ?: "",
            createdAtTime = data["createdAtTime"] as? String ?: "",
            valuationMethod = data["valuationMethod"] as? String ?: "Manual",
            recordsJson = data["recordsJson"] as? String ?: "[]",
            iconUri = ImageUploadHelper.normalizeCustomIconPathForLocalDevice(appContext, rawIconUri),
            tickerSymbol = data["tickerSymbol"] as? String ?: "",
            lotCount = (data["lotCount"] as? Number)?.toDouble() ?: 0.0,
            averagePrice = (data["averagePrice"] as? Number)?.toDouble() ?: 0.0,
            currentPrice = (data["currentPrice"] as? Number)?.toDouble() ?: 0.0,
            dailyPriceChange = (data["dailyPriceChange"] as? Number)?.toDouble() ?: 0.0,
            dailyPriceChangePercent = (data["dailyPriceChangePercent"] as? Number)?.toDouble() ?: 0.0,
            lastPriceUpdate = (data["lastPriceUpdate"] as? Number)?.toLong() ?: 0L,
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L,
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }

    private fun labelToMap(e: LabelEntity): Map<String, Any> {
        val safeId = RoomEntityMappers.sanitizeFirestoreId(e.id)
        return mapOf(
            "id" to safeId,
            "name" to e.name.ifBlank { "Label" }.take(120),
            "sortOrder" to e.sortOrder,
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "isDeleted" to e.isDeleted,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseLabelDocument(doc: DocumentSnapshot): LabelEntity? {
        val data = doc.data ?: return null
        val name = data["name"] as? String ?: ""
        if (name.isBlank()) return null
        return LabelEntity(
            id = doc.id,
            name = name,
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L,
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }

    private fun budgetToMap(e: BudgetEntity): Map<String, Any> {
        val safeId = RoomEntityMappers.sanitizeFirestoreId(e.id)
        return mapOf(
            "id" to safeId,
            "category" to e.category.ifBlank { "Kategori" }.take(120),
            "allocated" to e.allocated.coerceAtLeast(0.0),
            "spent" to e.spent.coerceAtLeast(0.0),
            "name" to e.name.take(120),
            "categoriesJson" to e.categoriesJson.take(20000),
            "accountNamesJson" to e.accountNamesJson.take(20000),
            "frequency" to e.frequency.take(60),
            "customStartDate" to e.customStartDate.take(40),
            "customEndDate" to e.customEndDate.take(40),
            "sortOrder" to e.sortOrder,
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "isDeleted" to e.isDeleted,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseBudgetDocument(doc: DocumentSnapshot): BudgetEntity? {
        val data = doc.data ?: return null
        return BudgetEntity(
            id = doc.id,
            category = data["category"] as? String ?: "Kategori",
            allocated = (data["allocated"] as? Number)?.toDouble() ?: 0.0,
            spent = (data["spent"] as? Number)?.toDouble() ?: 0.0,
            name = data["name"] as? String ?: "",
            categoriesJson = data["categoriesJson"] as? String ?: "[]",
            accountNamesJson = data["accountNamesJson"] as? String ?: "[]",
            frequency = data["frequency"] as? String ?: "Bulanan",
            customStartDate = data["customStartDate"] as? String ?: "",
            customEndDate = data["customEndDate"] as? String ?: "",
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L,
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }

    private fun recurringToMap(e: RecurringEntity): Map<String, Any> {
        val safeId = RoomEntityMappers.sanitizeFirestoreId(e.id)
        val safeType = if (e.type in listOf("EXPENSE", "INCOME", "TRANSFER")) e.type else "EXPENSE"
        return mapOf(
            "id" to safeId,
            "title" to e.title.ifBlank { "Tagihan" }.take(200),
            "amount" to e.amount.coerceAtLeast(0.0),
            "type" to safeType,
            "category" to e.category.take(120),
            "subcategory" to e.subcategory.take(120),
            "accountName" to e.accountName.take(120),
            "toAccountName" to e.toAccountName.take(120),
            "dayOfMonth" to e.dayOfMonth.coerceIn(1, 31),
            "frequency" to e.frequency.take(80),
            "startDate" to e.startDate.take(60),
            "endDate" to e.endDate.take(60),
            "note" to e.note.take(1000),
            "label" to e.label.take(200),
            "isActive" to e.isActive,
            "autoDeduct" to e.autoDeduct,
            "lastProcessedMonthYear" to e.lastProcessedMonthYear.take(30),
            "endCondition" to e.endCondition.take(40),
            "endAfterCount" to e.endAfterCount.coerceAtLeast(0),
            "logoName" to e.logoName.take(500),
            "skippedCount" to e.skippedCount.coerceAtLeast(0),
            "snoozedUntilDate" to e.snoozedUntilDate.take(60),
            "lastSkippedDateLabel" to e.lastSkippedDateLabel.take(80),
            "reminderEnabled" to e.reminderEnabled,
            "reminderOptionsJson" to e.reminderOptionsJson.take(5000),
            "sortOrder" to e.sortOrder,
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "isDeleted" to e.isDeleted,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseRecurringDocument(doc: DocumentSnapshot): RecurringEntity? {
        val data = doc.data ?: return null
        return RecurringEntity(
            id = doc.id,
            title = data["title"] as? String ?: "Tagihan",
            amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
            type = data["type"] as? String ?: "EXPENSE",
            category = data["category"] as? String ?: "Other",
            subcategory = data["subcategory"] as? String ?: "",
            accountName = data["accountName"] as? String ?: "",
            toAccountName = data["toAccountName"] as? String ?: "",
            dayOfMonth = (data["dayOfMonth"] as? Number)?.toInt() ?: 1,
            frequency = data["frequency"] as? String ?: "Setiap bulan",
            startDate = data["startDate"] as? String ?: "",
            endDate = data["endDate"] as? String ?: "",
            note = data["note"] as? String ?: "",
            label = data["label"] as? String ?: "",
            isActive = data["isActive"] as? Boolean ?: true,
            autoDeduct = data["autoDeduct"] as? Boolean ?: true,
            lastProcessedMonthYear = data["lastProcessedMonthYear"] as? String ?: "",
            endCondition = data["endCondition"] as? String ?: "NEVER",
            endAfterCount = (data["endAfterCount"] as? Number)?.toInt() ?: 0,
            logoName = data["logoName"] as? String ?: "",
            skippedCount = (data["skippedCount"] as? Number)?.toInt() ?: 0,
            snoozedUntilDate = data["snoozedUntilDate"] as? String ?: "",
            lastSkippedDateLabel = data["lastSkippedDateLabel"] as? String ?: "",
            reminderEnabled = data["reminderEnabled"] as? Boolean ?: false,
            reminderOptionsJson = data["reminderOptionsJson"] as? String ?: "[]",
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L,
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }

    private fun financialPlanToMap(e: FinancialPlanEntity): Map<String, Any> {
        val safeId = RoomEntityMappers.sanitizeFirestoreId(e.id)
        return mapOf(
            "id" to safeId,
            "title" to e.title.ifBlank { "Pembagian Gaji" }.take(200),
            "monthIndex" to e.monthIndex.coerceIn(1, 12),
            "year" to e.year.coerceIn(1970, 2100),
            "itemsJson" to e.itemsJson.ifBlank { "[]" }.take(100000),
            "sortOrder" to e.sortOrder,
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "isDeleted" to e.isDeleted,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseFinancialPlanDocument(doc: DocumentSnapshot): FinancialPlanEntity? {
        val data = doc.data ?: return null
        return FinancialPlanEntity(
            id = doc.id,
            title = data["title"] as? String ?: "Pembagian Gaji",
            monthIndex = (data["monthIndex"] as? Number)?.toInt() ?: 10,
            year = (data["year"] as? Number)?.toInt() ?: 2026,
            itemsJson = data["itemsJson"] as? String ?: "[]",
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L,
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }

    private fun eventToMap(e: EventEntity): Map<String, Any> {
        val safeId = RoomEntityMappers.sanitizeFirestoreId(e.id)
        return mapOf(
            "id" to safeId,
            "name" to e.name.ifBlank { "Acara" }.take(150),
            "eventType" to e.eventType.take(80),
            "targetBudget" to e.targetBudget.coerceAtLeast(0.0),
            "monthIndex" to e.monthIndex.coerceIn(1, 12),
            "year" to e.year.coerceIn(1970, 2100),
            "startDate" to e.startDate.take(60),
            "endDate" to e.endDate.take(60),
            "currency" to e.currency.take(16),
            "iconName" to e.iconName.take(120),
            "colorHex" to e.colorHex,
            "note" to e.note.take(1000),
            "plannedExpensesJson" to e.plannedExpensesJson.take(50000),
            "isCompleted" to e.isCompleted,
            "sortOrder" to e.sortOrder,
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "isDeleted" to e.isDeleted,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseEventDocument(doc: DocumentSnapshot): EventEntity? {
        val data = doc.data ?: return null
        return EventEntity(
            id = doc.id,
            name = data["name"] as? String ?: "Acara",
            eventType = data["eventType"] as? String ?: "Perjalanan",
            targetBudget = (data["targetBudget"] as? Number)?.toDouble() ?: 0.0,
            monthIndex = (data["monthIndex"] as? Number)?.toInt() ?: 11,
            year = (data["year"] as? Number)?.toInt() ?: 2026,
            startDate = data["startDate"] as? String ?: "",
            endDate = data["endDate"] as? String ?: "",
            currency = data["currency"] as? String ?: "IDR",
            iconName = data["iconName"] as? String ?: "Celebration",
            colorHex = (data["colorHex"] as? Number)?.toLong() ?: 0xFF3B82F6L,
            note = data["note"] as? String ?: "",
            plannedExpensesJson = data["plannedExpensesJson"] as? String ?: "[]",
            isCompleted = data["isCompleted"] as? Boolean ?: false,
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L,
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }

    private fun savingsGoalToMap(e: SavingsGoalEntity): Map<String, Any> {
        val safeId = RoomEntityMappers.sanitizeFirestoreId(e.id)
        return mapOf(
            "id" to safeId,
            "name" to e.name.ifBlank { "Tabungan" }.take(150),
            "targetAmount" to e.targetAmount.coerceAtLeast(0.0),
            "currentAmount" to e.currentAmount.coerceAtLeast(0.0),
            "targetDate" to e.targetDate.take(60),
            "startDate" to e.startDate.take(60),
            "currency" to e.currency.take(16),
            "iconName" to e.iconName.take(120),
            "note" to e.note.take(1000),
            "colorHex" to e.colorHex,
            "isCompleted" to e.isCompleted,
            "recordsJson" to e.recordsJson.take(50000),
            "sortOrder" to e.sortOrder,
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "isDeleted" to e.isDeleted,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseSavingsGoalDocument(doc: DocumentSnapshot): SavingsGoalEntity? {
        val data = doc.data ?: return null
        return SavingsGoalEntity(
            id = doc.id,
            name = data["name"] as? String ?: "Tabungan",
            targetAmount = (data["targetAmount"] as? Number)?.toDouble() ?: 0.0,
            currentAmount = (data["currentAmount"] as? Number)?.toDouble() ?: 0.0,
            targetDate = data["targetDate"] as? String ?: "",
            startDate = data["startDate"] as? String ?: "",
            currency = data["currency"] as? String ?: "IDR",
            iconName = data["iconName"] as? String ?: "Savings",
            note = data["note"] as? String ?: "",
            colorHex = (data["colorHex"] as? Number)?.toLong() ?: 0xFF3B82F6L,
            isCompleted = data["isCompleted"] as? Boolean ?: false,
            recordsJson = data["recordsJson"] as? String ?: "[]",
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L,
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }

    private fun timelineScenarioToMap(e: TimelineScenarioEntity): Map<String, Any> {
        val safeId = RoomEntityMappers.sanitizeFirestoreId(e.id)
        return mapOf(
            "id" to safeId,
            "amount" to e.amount.coerceAtLeast(0.0),
            "isIncome" to e.isIncome,
            "note" to e.note.take(500),
            "date" to e.date.take(60),
            "recurrence" to e.recurrence.take(80),
            "sortOrder" to e.sortOrder,
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "isDeleted" to e.isDeleted,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseTimelineScenarioDocument(doc: DocumentSnapshot): TimelineScenarioEntity? {
        val data = doc.data ?: return null
        return TimelineScenarioEntity(
            id = doc.id,
            amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
            isIncome = data["isIncome"] as? Boolean ?: false,
            note = data["note"] as? String ?: "",
            date = data["date"] as? String ?: "",
            recurrence = data["recurrence"] as? String ?: "Hanya sekali",
            sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L,
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }

    private fun appSettingsToMap(e: AppSettingsEntity): Map<String, Any> {
        val validJson = if (e.settingsJson.length >= 2) e.settingsJson else "{}"
        return mapOf(
            "id" to "main",
            "settingsJson" to validJson.take(30000),
            "activePlanId" to e.activePlanId.take(128),
            "planTitle" to e.planTitle.take(200),
            "selectedWalletFilter" to e.selectedWalletFilter.take(120),
            "upcomingItemsJson" to e.upcomingItemsJson.take(50000),
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseAppSettingsDocument(doc: DocumentSnapshot): AppSettingsEntity? {
        val data = doc.data ?: return null
        return AppSettingsEntity(
            id = "main",
            settingsJson = data["settingsJson"] as? String ?: "{}",
            activePlanId = data["activePlanId"] as? String ?: "plan_default",
            planTitle = data["planTitle"] as? String ?: "Pembagian Gaji",
            selectedWalletFilter = data["selectedWalletFilter"] as? String ?: "Semua",
            upcomingItemsJson = data["upcomingItemsJson"] as? String ?: "[]",
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L
        )
    }

    private fun customLogoToMap(e: CustomLogoEntity): Map<String, Any> {
        val safeId = RoomEntityMappers.sanitizeFirestoreId(e.id)
        return mapOf(
            "id" to safeId,
            "fileName" to e.fileName.ifBlank { "logo.png" }.take(200),
            "base64Data" to e.base64Data.take(700000),
            "updatedAt" to e.updatedAt.coerceAtLeast(1L),
            "isDeleted" to e.isDeleted,
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun parseCustomLogoDocument(doc: DocumentSnapshot): CustomLogoEntity? {
        val data = doc.data ?: return null
        val fileName = data["fileName"] as? String ?: ""
        if (fileName.isBlank()) return null
        return CustomLogoEntity(
            id = doc.id,
            fileName = fileName,
            base64Data = data["base64Data"] as? String ?: "",
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 1L,
            isDeleted = data["isDeleted"] as? Boolean ?: false
        )
    }

    fun release() {
        auth.removeAuthStateListener(authStateListener)
        listenerJobs.forEach { it.cancel() }
        listenerJobs.clear()
    }
}
