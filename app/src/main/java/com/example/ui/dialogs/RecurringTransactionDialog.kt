package com.example.ui.dialogs

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountWallet
import com.example.data.model.RecurringTransaction
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.FullScreenModalContainer
import com.example.ui.components.fullScreenBottomSafePadding
import com.example.ui.components.fullScreenTopSafePadding
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CurrencyHelper
import com.example.util.InstitutionLogoBadge
import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class RecurringSortOption(val label: String) {
    NEXT_DUE_DATE("Tanggal jatuh tempo berikutnya"),
    RECENTLY_ADDED("Baru ditambahkan")
}

enum class RecurringListStyle(val label: String) {
    CARD("Kartu"),
    TABLE("Tabel")
}

data class RecurringComputedItem(
    val item: RecurringTransaction,
    val dueDateLabel: String,
    val dueDay: Int,
    val dueMonth: Int,
    val dueYear: Int,
    val daysDiffFromToday: Int,
    val isProcessedThisCycle: Boolean,
    val isOverdue: Boolean,
    val isDueToday: Boolean,
    val isUpcoming7Days: Boolean,
    val needsAction: Boolean,
    val recordedCount: Int
)

private val INDONESIAN_SHORT_MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
)

private val INDONESIAN_FULL_MONTHS = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
)

private fun shortDayInitial(dayOfWeek: Int): String = when (dayOfWeek) {
    Calendar.SUNDAY -> "M"
    Calendar.MONDAY -> "S"
    Calendar.TUESDAY -> "S"
    Calendar.WEDNESDAY -> "R"
    Calendar.THURSDAY -> "K"
    Calendar.FRIDAY -> "J"
    Calendar.SATURDAY -> "S"
    else -> "S"
}

private fun normalizedCalendar(year: Int, month1to12: Int, day: Int): Calendar {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month1to12 - 1)
        set(Calendar.DAY_OF_MONTH, 1)
        val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
        set(Calendar.DAY_OF_MONTH, day.coerceIn(1, maxDay))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private fun computeRecurringItemState(
    item: RecurringTransaction,
    transactions: List<Transaction>,
    todayYear: Int,
    todayMonth: Int,
    todayDay: Int
): RecurringComputedItem {
    val todayCal = normalizedCalendar(todayYear, todayMonth, todayDay)
    val currentMonthYearKey = "$todayMonth/$todayYear"
    val isProcessedThisCycle = item.lastProcessedMonthYear == currentMonthYearKey

    val targetDueCal = when {
        item.frequency == "Setiap hari" -> {
            if (isProcessedThisCycle) {
                (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
            } else {
                todayCal
            }
        }
        item.frequency == "Setiap tahun" -> {
            val parts = item.startDate.split("/")
            val startMonth = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: todayMonth
            val candidate = normalizedCalendar(todayYear, startMonth, item.dayOfMonth)
            if (isProcessedThisCycle) {
                normalizedCalendar(todayYear + 1, startMonth, item.dayOfMonth)
            } else {
                candidate
            }
        }
        else -> {
            // "Setiap bulan" or "Setiap 2 minggu"
            if (isProcessedThisCycle) {
                val nextMonth = if (todayMonth < 12) todayMonth + 1 else 1
                val nextYear = if (todayMonth < 12) todayYear else todayYear + 1
                normalizedCalendar(nextYear, nextMonth, item.dayOfMonth)
            } else {
                normalizedCalendar(todayYear, todayMonth, item.dayOfMonth)
            }
        }
    }

    val diffMillis = targetDueCal.timeInMillis - todayCal.timeInMillis
    val daysDiff = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
    val dueDay = targetDueCal.get(Calendar.DAY_OF_MONTH)
    val dueMonth = targetDueCal.get(Calendar.MONTH) + 1
    val dueYear = targetDueCal.get(Calendar.YEAR)
    val monthShort = INDONESIAN_SHORT_MONTHS.getOrElse(dueMonth - 1) { "Nov" }
    val dueDateLabel = "$dueDay $monthShort"

    val isOverdue = item.isActive && !isProcessedThisCycle && daysDiff < 0
    val isDueToday = item.isActive && !isProcessedThisCycle && daysDiff == 0
    val isUpcoming7Days = item.isActive && !isProcessedThisCycle && (daysDiff in 0..7 || isOverdue)
    val needsAction = item.isActive && !isProcessedThisCycle && daysDiff <= 0

    val matchedTxCount = transactions.count { tx ->
        tx.title.equals(item.title, ignoreCase = true) ||
            (tx.recurrence.isNotBlank() &&
                tx.recurrence != "Tidak pernah" &&
                tx.category.equals(item.category, ignoreCase = true) &&
                tx.accountName.equals(item.accountName, ignoreCase = true) &&
                tx.amount == item.amount)
    }
    val recordedCount = if (isProcessedThisCycle && matchedTxCount == 0) 1 else matchedTxCount

    return RecurringComputedItem(
        item = item,
        dueDateLabel = dueDateLabel,
        dueDay = dueDay,
        dueMonth = dueMonth,
        dueYear = dueYear,
        daysDiffFromToday = daysDiff,
        isProcessedThisCycle = isProcessedThisCycle,
        isOverdue = isOverdue,
        isDueToday = isDueToday,
        isUpcoming7Days = isUpcoming7Days,
        needsAction = needsAction,
        recordedCount = recordedCount
    )
}

@Composable
fun RecurringTransactionDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val recurringList by viewModel.recurringTransactions.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    var selectedTab by remember { mutableIntStateOf(1) } // 0 = Mendatang, 1 = Semua (matches video entry)
    var sortOption by remember { mutableStateOf(RecurringSortOption.NEXT_DUE_DATE) }
    var listStyle by remember { mutableStateOf(RecurringListStyle.CARD) }
    var selectedCalendarDayOffset by remember { mutableStateOf<Int?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var prefillTemplate by remember { mutableStateOf<RecurringTemplatePrefill?>(null) }
    var editingItem by remember { mutableStateOf<RecurringTransaction?>(null) }
    var detailItemId by remember { mutableStateOf<String?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showLanggananDialog by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val todayCal = remember { Calendar.getInstance() }
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
    val todayMonth = todayCal.get(Calendar.MONTH) + 1
    val todayYear = todayCal.get(Calendar.YEAR)

    val computedItems = remember(recurringList, allTransactions, todayDay, todayMonth, todayYear) {
        recurringList.map { item ->
            computeRecurringItemState(
                item = item,
                transactions = allTransactions,
                todayYear = todayYear,
                todayMonth = todayMonth,
                todayDay = todayDay
            )
        }
    }

    val sortedItems = remember(computedItems, sortOption) {
        when (sortOption) {
            RecurringSortOption.NEXT_DUE_DATE -> computedItems.sortedWith(
                compareBy<RecurringComputedItem> { !it.item.isActive }
                    .thenBy { it.isProcessedThisCycle }
                    .thenBy { it.daysDiffFromToday }
                    .thenBy { it.item.title.lowercase() }
            )
            RecurringSortOption.RECENTLY_ADDED -> computedItems.reversed()
        }
    }

    val upcomingItems = remember(sortedItems) {
        sortedItems.filter { it.isUpcoming7Days }
    }

    val displayedUpcomingItems = remember(upcomingItems, selectedCalendarDayOffset) {
        val offset = selectedCalendarDayOffset
        if (offset == null) {
            upcomingItems
        } else {
            upcomingItems.filter { item ->
                if (offset == 0) item.daysDiffFromToday <= 0 else item.daysDiffFromToday == offset
            }
        }
    }

    val totalUpcomingAmount = remember(computedItems) {
        computedItems.filter { it.item.isActive && !it.isProcessedThisCycle }.sumOf { it.item.amount }
    }
    val activeCount = remember(computedItems) {
        computedItems.count { it.item.isActive }
    }
    val overdueCount = remember(computedItems) {
        computedItems.count { it.isOverdue }
    }
    val actionableItems = remember(computedItems) {
        computedItems.filter { it.needsAction }
    }

    FullScreenModalContainer(onDismissRequest = onDismiss) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .testTag("dialog_recurring_transactions"),
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        prefillTemplate = null
                        showAddDialog = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = AccentBlue,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fullScreenBottomSafePadding()
                        .padding(bottom = 8.dp)
                        .size(56.dp)
                        .testTag("btn_add_recurring_transaction")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Transaksi Berulang",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Top App Bar: [←] Berulang      [Templates] [?] [Tune]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fullScreenTopSafePadding()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("btn_close_recurring_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = TextWhite
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Berulang",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        TopActionSquareButton(
                            icon = Icons.Default.Subscriptions,
                            contentDescription = "Langganan Terdeteksi",
                            onClick = { showLanggananDialog = true },
                            testTag = "btn_recurring_langganan"
                        )
                        TopActionSquareButton(
                            icon = Icons.Outlined.HelpOutline,
                            contentDescription = "Bantuan Transaksi Berulang",
                            onClick = { showHelpDialog = true },
                            testTag = "btn_recurring_help"
                        )
                        TopActionSquareButton(
                            icon = Icons.Default.Tune,
                            contentDescription = "Urutkan dan Gaya Daftar",
                            onClick = { showSortSheet = true },
                            testTag = "btn_recurring_sort_style"
                        )
                    }
                }

                // Segmented Tab Switcher: "Mendatang <N>" | "Semua <N>"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
                        modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RecurringSegmentChip(
                                label = "Mendatang",
                                countText = "${upcomingItems.size}",
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                testTag = "tab_recurring_upcoming"
                            )
                            RecurringSegmentChip(
                                label = "Semua",
                                countText = "${sortedItems.size}",
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                testTag = "tab_recurring_all"
                            )
                        }
                    }
                }

                // Main Scrollable Content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 10.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (selectedTab == 0) {
                        item {
                            val upcomingSummaryText = if (upcomingItems.isEmpty()) {
                                "Tidak ada pembayaran yang jatuh tempo dalam 7 hari ke depan"
                            } else {
                                "Anda memiliki ${upcomingItems.size} pembayaran yang jatuh tempo dalam 7 hari ke depan"
                            }
                            Text(
                                text = upcomingSummaryText,
                                fontSize = 13.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }

                        item {
                            UpcomingWeekCalendarCard(
                                todayYear = todayYear,
                                todayMonth = todayMonth,
                                todayDay = todayDay,
                                upcomingItems = upcomingItems,
                                selectedDayOffset = selectedCalendarDayOffset,
                                onSelectDayOffset = { offset ->
                                    selectedCalendarDayOffset =
                                        if (selectedCalendarDayOffset == offset) null else offset
                                }
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Akan datang",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                if (selectedCalendarDayOffset != null) {
                                    Text(
                                        text = "Tampilkan semua",
                                        fontSize = 12.sp,
                                        color = AccentBlueBright,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { selectedCalendarDayOffset = null }
                                    )
                                }
                            }
                        }

                        if (displayedUpcomingItems.isEmpty()) {
                            item {
                                RecurringEmptyCard(
                                    title = "Tidak ada jadwal jatuh tempo",
                                    subtitle = if (selectedCalendarDayOffset != null) {
                                        "Ketuk tanggal lagi untuk melihat seluruh jadwal 7 hari ke depan"
                                    } else {
                                        "Semua transaksi berulang dalam 7 hari ke depan sudah tercatat"
                                    }
                                )
                            }
                        } else {
                            item {
                                if (listStyle == RecurringListStyle.CARD) {
                                    RecurringGroupedCardList(
                                        items = displayedUpcomingItems,
                                        wallets = wallets,
                                        isUpcomingMode = true,
                                        onItemClick = { detailItemId = it.item.id },
                                        onSkip = { viewModel.skipRecurringOccurrence(it.item.id, it.dueDateLabel) },
                                        onConfirm = { viewModel.processRecurringTransaction(it.item.id) }
                                    )
                                } else {
                                    RecurringTableCardList(
                                        items = displayedUpcomingItems,
                                        wallets = wallets,
                                        onItemClick = { detailItemId = it.item.id },
                                        onConfirm = { viewModel.processRecurringTransaction(it.item.id) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Semua Tab: 3-Column Summary Strip
                        item {
                            RecurringSummaryStrip(
                                totalUpcomingAmount = totalUpcomingAmount,
                                activeCount = activeCount,
                                overdueCount = overdueCount
                            )
                        }

                        if (sortedItems.isEmpty()) {
                            item {
                                RecurringEmptyCard(
                                    title = "Belum ada transaksi berulang",
                                    subtitle = "Tekan tombol + di kanan bawah untuk menambahkan jadwal rutin Anda"
                                )
                            }
                        } else {
                            item {
                                if (listStyle == RecurringListStyle.CARD) {
                                    RecurringGroupedCardList(
                                        items = sortedItems,
                                        wallets = wallets,
                                        isUpcomingMode = false,
                                        onItemClick = { detailItemId = it.item.id },
                                        onSkip = { viewModel.skipRecurringOccurrence(it.item.id, it.dueDateLabel) },
                                        onConfirm = { viewModel.processRecurringTransaction(it.item.id) }
                                    )
                                } else {
                                    RecurringTableCardList(
                                        items = sortedItems,
                                        wallets = wallets,
                                        onItemClick = { detailItemId = it.item.id },
                                        onConfirm = { viewModel.processRecurringTransaction(it.item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        RecurringSortDisplaySheet(
            currentSort = sortOption,
            currentStyle = listStyle,
            onSelectSort = { sortOption = it },
            onSelectStyle = { listStyle = it },
            onDismiss = { showSortSheet = false }
        )
    }

    if (showTemplatesSheet) {
        RecurringQuickTemplatesSheet(
            actionableItems = actionableItems,
            onProcessAllDue = {
                actionableItems.forEach { computed ->
                    viewModel.processRecurringTransaction(computed.item.id)
                }
                showTemplatesSheet = false
            },
            onSelectTemplate = { template ->
                showTemplatesSheet = false
                prefillTemplate = template
                showAddDialog = true
            },
            onDismiss = { showTemplatesSheet = false }
        )
    }

    if (showHelpDialog) {
        RecurringHelpDialog(onDismiss = { showHelpDialog = false })
    }

    if (showAddDialog) {
        AddEditRecurringTransactionDialog(
            viewModel = viewModel,
            prefill = prefillTemplate,
            onDismiss = {
                showAddDialog = false
                prefillTemplate = null
            }
        )
    }

    editingItem?.let { item ->
        AddEditRecurringTransactionDialog(
            viewModel = viewModel,
            initialItem = item,
            onDismiss = { editingItem = null }
        )
    }

    detailItemId?.let { id ->
        RecurringDetailScreen(
            recurringId = id,
            viewModel = viewModel,
            onEdit = { editingItem = it },
            onDismiss = { detailItemId = null }
        )
    }

    if (showLanggananDialog) {
        LanggananDiscoveryDialog(
            recurringList = recurringList,
            allTransactions = allTransactions,
            onTrackCandidate = { cand ->
                showLanggananDialog = false
                prefillTemplate = cand
                showAddDialog = true
            },
            onSelectRecurring = { sub ->
                showLanggananDialog = false
                detailItemId = sub.id
            },
            onDismiss = { showLanggananDialog = false }
        )
    }
}

@Composable
private fun TopActionSquareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = TextWhite,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun RecurringSegmentChip(
    label: String,
    countText: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) DarkSurfaceElevated else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) TextWhite else TextMuted
            )
            Text(
                text = countText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) TextWhite.copy(alpha = 0.8f) else TextSubtle
            )
        }
    }
}

@Composable
private fun RecurringSummaryStrip(
    totalUpcomingAmount: Double,
    activeCount: Int,
    overdueCount: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1.25f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = CurrencyHelper.formatRupiah(totalUpcomingAmount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Mendatang",
                    fontSize = 11.sp,
                    color = TextSubtle
                )
            }

            VerticalDivider(
                modifier = Modifier.height(28.dp),
                thickness = 1.dp,
                color = DarkCardBorder.copy(alpha = 0.6f)
            )

            Column(
                modifier = Modifier.weight(0.85f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$activeCount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Aktif",
                    fontSize = 11.sp,
                    color = TextSubtle
                )
            }

            VerticalDivider(
                modifier = Modifier.height(28.dp),
                thickness = 1.dp,
                color = DarkCardBorder.copy(alpha = 0.6f)
            )

            Column(
                modifier = Modifier.weight(0.85f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$overdueCount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (overdueCount > 0) ExpenseRed else TextWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Terlambat",
                    fontSize = 11.sp,
                    color = TextSubtle
                )
            }
        }
    }
}

@Composable
private fun UpcomingWeekCalendarCard(
    todayYear: Int,
    todayMonth: Int,
    todayDay: Int,
    upcomingItems: List<RecurringComputedItem>,
    selectedDayOffset: Int?,
    onSelectDayOffset: (Int) -> Unit
) {
    val monthTitle = INDONESIAN_FULL_MONTHS.getOrElse(todayMonth - 1) { "November" }
    val todayBase = remember(todayYear, todayMonth, todayDay) {
        normalizedCalendar(todayYear, todayMonth, todayDay)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = monthTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (offset in 0..6) {
                    val dayCal = (todayBase.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_MONTH, offset)
                    }
                    val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
                    val initial = shortDayInitial(dayCal.get(Calendar.DAY_OF_WEEK))
                    val hasOverdueOnToday = offset == 0 && upcomingItems.any { it.daysDiffFromToday <= 0 }
                    val hasDueOnDay = upcomingItems.any { it.daysDiffFromToday == offset }
                    val isSelected = selectedDayOffset == offset || (selectedDayOffset == null && offset == 0)

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectDayOffset(offset) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = initial,
                            fontSize = 11.sp,
                            color = TextSubtle,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> AccentBlue
                                        else -> Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$dayNum",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        hasOverdueOnToday -> IncomeGreen
                                        hasDueOnDay -> AccentBlueBright
                                        else -> Color.Transparent
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringGroupedCardList(
    items: List<RecurringComputedItem>,
    wallets: List<AccountWallet>,
    isUpcomingMode: Boolean,
    onItemClick: (RecurringComputedItem) -> Unit,
    onSkip: (RecurringComputedItem) -> Unit,
    onConfirm: (RecurringComputedItem) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, computed ->
                val walletIcon = remember(wallets, computed.item.accountName) {
                    wallets.firstOrNull {
                        it.name.equals(computed.item.accountName, ignoreCase = true)
                    }?.iconName ?: ""
                }
                RecurringGroupedCardRow(
                    computed = computed,
                    walletIcon = walletIcon,
                    isUpcomingMode = isUpcomingMode,
                    onClick = { onItemClick(computed) },
                    onSkip = { onSkip(computed) },
                    onConfirm = { onConfirm(computed) }
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.7.dp,
                        color = DarkCardBorder.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringGroupedCardRow(
    computed: RecurringComputedItem,
    walletIcon: String,
    isUpcomingMode: Boolean,
    onClick: () -> Unit,
    onSkip: () -> Unit,
    onConfirm: () -> Unit
) {
    val item = computed.item
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("recurring_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InstitutionLogoBadge(
            walletName = item.logoName.ifBlank { item.accountName },
            iconName = item.logoName.ifBlank { walletIcon },
            size = 40.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Middle Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (item.isActive) TextWhite else TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (isUpcomingMode) {
                Text(
                    text = computed.dueDateLabel,
                    fontSize = 12.sp,
                    fontWeight = if (computed.isOverdue || computed.isDueToday) FontWeight.Medium else FontWeight.Normal,
                    color = when {
                        computed.isOverdue -> ExpenseRed
                        computed.isDueToday -> Color(0xFFF59E0B)
                        else -> TextSubtle
                    }
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = computed.dueDateLabel,
                        fontSize = 12.sp,
                        color = if (computed.isOverdue) ExpenseRed else TextSubtle
                    )
                    Text(
                        text = " · ",
                        fontSize = 12.sp,
                        color = if (computed.isOverdue) ExpenseRed else TextSubtle
                    )
                    val (statusLabel, statusColor) = when {
                        !item.isActive -> "Dijeda" to TextSubtle
                        computed.isOverdue -> "Terlambat" to ExpenseRed
                        computed.isDueToday -> "Jatuh Tempo" to Color(0xFFF59E0B)
                        computed.isProcessedThisCycle -> "Tercatat" to IncomeGreen
                        else -> "Aktif" to TextMuted
                    }
                    Text(
                        text = statusLabel,
                        fontSize = 12.sp,
                        fontWeight = if (computed.isOverdue || computed.isDueToday) FontWeight.Medium else FontWeight.Normal,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                val recordedSummary = "${computed.recordedCount} tercatat" +
                    if (item.skippedCount > 0) " · ${item.skippedCount} dilewati" else ""
                Text(
                    text = recordedSummary,
                    fontSize = 11.sp,
                    color = TextSubtle
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Amount & Category
        Column(horizontalAlignment = Alignment.End) {
            val formattedAmount = CurrencyHelper.formatRupiah(item.amount)
            Text(
                text = formattedAmount,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    !item.isActive -> TextMuted
                    item.type == TransactionType.INCOME -> TextWhite
                    else -> TextWhite
                },
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (item.type == TransactionType.TRANSFER && item.toAccountName.isNotBlank()) {
                    "${item.accountName} → ${item.toAccountName}"
                } else {
                    item.subcategory.ifBlank { item.category }
                },
                fontSize = 11.sp,
                color = TextSubtle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Inline Skip & Confirm buttons for due/overdue items
        if (computed.needsAction) {
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkCardBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onSkip)
                        .testTag("btn_skip_${item.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Lewati siklus ini",
                        tint = TextWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF66B28C))
                        .clickable(onClick = onConfirm)
                        .testTag("btn_process_now_${item.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Catat transaksi sekarang",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringTableCardList(
    items: List<RecurringComputedItem>,
    wallets: List<AccountWallet>,
    onItemClick: (RecurringComputedItem) -> Unit,
    onConfirm: (RecurringComputedItem) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceElevated.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSubtle,
                    modifier = Modifier.weight(1.4f)
                )
                Text(
                    text = "Jadwal",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSubtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    text = "Nominal",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSubtle,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.1f)
                )
            }

            HorizontalDivider(thickness = 0.8.dp, color = DarkCardBorder.copy(alpha = 0.6f))

            items.forEachIndexed { index, computed ->
                val item = computed.item
                val walletIcon = wallets.firstOrNull {
                    it.name.equals(item.accountName, ignoreCase = true)
                }?.iconName ?: ""

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(computed) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("recurring_table_row_${item.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1.4f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InstitutionLogoBadge(
                            walletName = item.logoName.ifBlank { item.accountName },
                            iconName = item.logoName.ifBlank { walletIcon },
                            size = 28.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = item.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (item.isActive) TextWhite else TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (item.type == TransactionType.TRANSFER && item.toAccountName.isNotBlank()) {
                                    "${item.accountName} → ${item.toAccountName}"
                                } else {
                                    item.subcategory.ifBlank { item.category }
                                },
                                fontSize = 10.sp,
                                color = TextSubtle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(0.8f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = computed.dueDateLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                computed.isOverdue -> ExpenseRed
                                computed.isDueToday -> Color(0xFFF59E0B)
                                else -> TextWhite
                            }
                        )
                        Text(
                            text = when {
                                !item.isActive -> "Dijeda"
                                computed.isOverdue -> "Terlambat"
                                computed.isDueToday -> "Jatuh Tempo"
                                computed.isProcessedThisCycle -> "Tercatat"
                                else -> "Aktif"
                            },
                            fontSize = 10.sp,
                            color = when {
                                computed.isOverdue -> ExpenseRed
                                computed.isDueToday -> Color(0xFFF59E0B)
                                else -> TextSubtle
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.weight(1.1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = CurrencyHelper.formatRupiah(item.amount),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.type == TransactionType.INCOME) IncomeGreen else TextWhite,
                            maxLines = 1
                        )
                        if (computed.needsAction) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color(0xFF66B28C))
                                    .clickable { onConfirm(computed) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Catat",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        thickness = 0.6.dp,
                        color = DarkCardBorder.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringEmptyCard(
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EventRepeat,
                contentDescription = null,
                tint = TextSubtle,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
