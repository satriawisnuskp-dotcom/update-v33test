package com.example.ui.dialogs

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CategoryIconHelper
import com.example.util.CurrencyHelper
import com.example.util.InstitutionLogoBadge
import java.util.Calendar
import java.util.Locale

private val FullMonthsId = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
)

private val ShortMonthsId = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
)

private val ReminderChoices = listOf(
    "Pada hari jatuh tempo",
    "1 hari sebelumnya",
    "2 hari sebelumnya",
    "3 hari sebelumnya",
    "1 minggu sebelumnya",
    "2 minggu sebelumnya"
)

private fun frequencyBadgeText(freq: String): String = when {
    freq.contains("hari", ignoreCase = true) -> "Harian"
    freq.contains("2 minggu", ignoreCase = true) -> "2 Mingguan"
    freq.contains("minggu", ignoreCase = true) -> "Mingguan"
    freq.contains("tahun", ignoreCase = true) -> "Tahunan"
    else -> "Bulanan"
}

private fun formatReadableSlashDate(slashDate: String): String {
    val parts = slashDate.trim().split("/")
    if (parts.size != 3) return slashDate.ifBlank { "-" }
    val d = parts[0].toIntOrNull() ?: return slashDate
    val m = parts[1].toIntOrNull() ?: return slashDate
    val y = parts[2].toIntOrNull() ?: return slashDate
    val mShort = ShortMonthsId.getOrElse(m - 1) { "Nov" }
    return "$d $mShort $y"
}

private fun findMatchingTransactions(
    item: RecurringTransaction,
    transactions: List<Transaction>
): List<Transaction> {
    return transactions.filter { tx ->
        tx.title.equals(item.title, ignoreCase = true) ||
            (tx.recurrence.isNotBlank() &&
                tx.recurrence != "Tidak pernah" &&
                tx.category.equals(item.category, ignoreCase = true) &&
                tx.accountName.equals(item.accountName, ignoreCase = true) &&
                tx.amount == item.amount)
    }.sortedWith(
        compareByDescending<Transaction> { it.year }
            .thenByDescending { it.monthIndex }
            .thenByDescending { it.dayOfMonth }
    )
}

@Composable
fun RecurringDetailScreen(
    recurringId: String,
    viewModel: FinanceViewModel,
    onEdit: (RecurringTransaction) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val recurringList by viewModel.recurringTransactions.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    val item = recurringList.find { it.id == recurringId }
    if (item == null) {
        onDismiss()
        return
    }

    val todayCal = remember { Calendar.getInstance() }
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
    val todayMonth = todayCal.get(Calendar.MONTH) + 1
    val todayYear = todayCal.get(Calendar.YEAR)

    val matchingTxs = remember(item, allTransactions) {
        findMatchingTransactions(item, allTransactions)
    }
    val recordedTotal = remember(matchingTxs) { matchingTxs.sumOf { it.amount } }
    val currentMonthYearKey = "$todayMonth/$todayYear"
    val isProcessedThisCycle = item.lastProcessedMonthYear == currentMonthYearKey

    val (expectedDay, expectedMonth, expectedYear) = remember(item, isProcessedThisCycle, todayMonth, todayYear) {
        if (item.snoozedUntilDate.isNotBlank()) {
            val parts = item.snoozedUntilDate.split("/")
            val sd = parts.getOrNull(0)?.toIntOrNull() ?: item.dayOfMonth
            val sm = parts.getOrNull(1)?.toIntOrNull() ?: todayMonth
            val sy = parts.getOrNull(2)?.toIntOrNull() ?: todayYear
            Triple(sd, sm, sy)
        } else if (isProcessedThisCycle) {
            val nm = if (todayMonth < 12) todayMonth + 1 else 1
            val ny = if (todayMonth < 12) todayYear else todayYear + 1
            Triple(item.dayOfMonth, nm, ny)
        } else {
            Triple(item.dayOfMonth, todayMonth, todayYear)
        }
    }
    val expectedMonthFull = FullMonthsId.getOrElse(expectedMonth - 1) { "November" }
    val expectedDateText = "$expectedDay $expectedMonthFull"

    var showEditAndRecordTxDialog by remember { mutableStateOf(false) }
    var showSnoozeDateDialog by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    FullScreenModalContainer(onDismissRequest = onDismiss) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .fullScreenBottomSafePadding()
                .testTag("screen_recurring_detail"),
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Top Bar: <-  |  [Edit] [Trash]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fullScreenTopSafePadding()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_back_recurring_detail")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextWhite
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onEdit(item) }
                                .testTag("btn_edit_recurring_detail")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, tint = TextWhite, modifier = Modifier.size(16.dp))
                                Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ExpenseRedSoft,
                            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showDeleteConfirm = true }
                                .testTag("btn_delete_recurring_detail")
                        ) {
                            Box(
                                modifier = Modifier.padding(9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Summary Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (item.logoName.isNotBlank()) {
                                        InstitutionLogoBadge(
                                            walletName = item.logoName,
                                            iconName = item.logoName,
                                            size = 46.dp
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (item.type == TransactionType.INCOME) IncomeGreenSoft else AccentBlue.copy(alpha = 0.16f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getCategoryIcon(item.category),
                                                contentDescription = null,
                                                tint = if (item.type == TransactionType.INCOME) IncomeGreen else AccentBlueBright,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = item.title.ifBlank { item.subcategory.ifBlank { item.category } },
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AccentBlue.copy(alpha = 0.16f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Autorenew, null, tint = AccentBlueBright, modifier = Modifier.size(12.dp))
                                                Text(
                                                    text = frequencyBadgeText(item.frequency),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AccentBlueBright
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyHelper.formatRupiah(item.amount),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = "per periode",
                                        fontSize = 11.sp,
                                        color = TextSubtle
                                    )
                                }
                            }
                        }
                    }

                    // PEMBAYARAN BERIKUTNYA Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "PEMBAYARAN BERIKUTNYA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.7.sp,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val signedAmount = when (item.type) {
                                    TransactionType.EXPENSE -> "-${CurrencyHelper.formatRupiah(item.amount)}"
                                    TransactionType.INCOME -> "+${CurrencyHelper.formatRupiah(item.amount)}"
                                    TransactionType.TRANSFER -> CurrencyHelper.formatRupiah(item.amount)
                                }
                                Text(
                                    text = signedAmount,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (item.type) {
                                        TransactionType.EXPENSE -> ExpenseRed
                                        TransactionType.INCOME -> IncomeGreen
                                        TransactionType.TRANSFER -> AccentBlueBright
                                    }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (item.snoozedUntilDate.isNotBlank()) {
                                        "Ditunda ke $expectedDateText"
                                    } else {
                                        "Diharapkan sekitar $expectedDateText"
                                    },
                                    fontSize = 12.5.sp,
                                    color = if (item.snoozedUntilDate.isNotBlank()) Color(0xFFF59E0B) else TextMuted
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Action Buttons Row: Catat pembayaran | Edit & catat
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.processRecurringTransaction(item.id) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F9D79)),
                                        contentPadding = PaddingValues(vertical = 12.dp),
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .height(48.dp)
                                            .testTag("btn_detail_record_payment")
                                    ) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Catat pembayaran", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = { showEditAndRecordTxDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, DarkCardBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurfaceElevated),
                                        contentPadding = PaddingValues(vertical = 12.dp),
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .height(48.dp)
                                            .testTag("btn_detail_edit_and_record")
                                    ) {
                                        Icon(Icons.Default.Edit, null, tint = TextWhite, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Edit & catat", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite, maxLines = 1)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Sub-actions Row: Tunda | Lewati yang ini
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { showSnoozeDateDialog = true }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("btn_detail_snooze"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.AccessTime, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                        Text("Tunda", fontSize = 13.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { viewModel.skipRecurringOccurrence(item.id, expectedDateText) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("btn_detail_skip_once"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.SkipNext, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                        Text("Lewati yang ini", fontSize = 13.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))

                                // 5-Month Timeline Dots (e.g., Sep 2026 .. Jan 2027)
                                val timelineMonths = remember(expectedMonth, expectedYear, matchingTxs) {
                                    (-2..2).map { offset ->
                                        val c = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, expectedYear)
                                            set(Calendar.MONTH, expectedMonth - 1)
                                            add(Calendar.MONTH, offset)
                                        }
                                        val m = c.get(Calendar.MONTH) + 1
                                        val y = c.get(Calendar.YEAR)
                                        val hasTx = matchingTxs.any { it.monthIndex == m && it.year == y }
                                        Triple(
                                            "${ShortMonthsId.getOrElse(m - 1) { "Nov" }} $y",
                                            offset == 0,
                                            hasTx
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    timelineMonths.forEach { (label, isCurrent, hasTx) ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(if (isCurrent) 14.dp else 11.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            hasTx -> AccentBlueBright
                                                            isCurrent -> AccentBlue.copy(alpha = 0.25f)
                                                            else -> DarkSurfaceElevated
                                                        }
                                                    )
                                                    .then(
                                                        if (isCurrent) {
                                                            Modifier.border(2.dp, AccentBlueBright, CircleShape)
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                            )
                                            Spacer(modifier = Modifier.height(5.dp))
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                color = if (isCurrent) AccentBlueBright else TextSubtle,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                val recordedCountDisplay = maxOf(matchingTxs.size, if (isProcessedThisCycle) 1 else 0)
                                Text(
                                    text = "${CurrencyHelper.formatRupiah(recordedTotal)} dari $recordedCountDisplay pembayaran sejauh ini",
                                    fontSize = 11.5.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 3-Column Stats Cards
                    item {
                        val recordedCountDisplay = maxOf(matchingTxs.size, if (isProcessedThisCycle) 1 else 0)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DetailStatBox(
                                value = "$recordedCountDisplay",
                                label = "Tercatat",
                                valueColor = TextWhite,
                                modifier = Modifier.weight(0.8f)
                            )
                            DetailStatBox(
                                value = CurrencyHelper.formatRupiah(recordedTotal),
                                label = "Total",
                                valueColor = AccentBlueBright,
                                modifier = Modifier.weight(1.3f)
                            )
                            DetailStatBox(
                                value = "${item.skippedCount}",
                                label = "Dilewati",
                                valueColor = if (item.skippedCount > 0) Color(0xFFF59E0B) else TextWhite,
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    }

                    // Details Card (Wallet, Dates, Reminder, Note)
                    item {
                        val walletIcon = wallets.firstOrNull { it.name.equals(item.accountName, ignoreCase = true) }?.iconName ?: ""
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                // Wallet Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        InstitutionLogoBadge(
                                            walletName = item.accountName,
                                            iconName = walletIcon,
                                            size = 28.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (item.type == TransactionType.TRANSFER && item.toAccountName.isNotBlank()) {
                                                "${item.accountName} → ${item.toAccountName}"
                                            } else {
                                                item.accountName
                                            },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite
                                        )
                                    }

                                    val (typeLabel, typeColor, typeBg) = when (item.type) {
                                        TransactionType.EXPENSE -> Triple("Pengeluaran", ExpenseRed, ExpenseRedSoft)
                                        TransactionType.INCOME -> Triple("Pemasukan", IncomeGreen, IncomeGreenSoft)
                                        TransactionType.TRANSFER -> Triple("Transfer", AccentBlueBright, AccentBlue.copy(alpha = 0.16f))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = typeBg
                                    ) {
                                        Text(
                                            text = typeLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = typeColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.45f))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Start & End Dates
                                val endDisplay = when (item.endCondition) {
                                    "ON_DATE" -> formatReadableSlashDate(item.endDate)
                                    "AFTER_COUNT" -> "Setelah ${item.endAfterCount} kali"
                                    else -> if (item.endDate.isNotBlank()) formatReadableSlashDate(item.endDate) else "Tanpa akhir"
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Mulai", fontSize = 11.sp, color = TextSubtle)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = formatReadableSlashDate(item.startDate).ifBlank { "$todayDay ${ShortMonthsId.getOrElse(todayMonth - 1) { "Nov" }} $todayYear" },
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Akhir", fontSize = 11.sp, color = TextSubtle)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = endDisplay,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.45f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Reminder Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showReminderSheet = true }
                                        .padding(vertical = 4.dp)
                                        .testTag("btn_detail_reminder_row"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (item.reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                            contentDescription = null,
                                            tint = if (item.reminderEnabled) AccentBlueBright else TextMuted,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (item.reminderEnabled) "Aktif" else "Tidak aktif",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextWhite
                                            )
                                            Text(
                                                text = if (item.reminderEnabled) {
                                                    "Pengingat · ${item.reminderOptions.size} waktu dipilih"
                                                } else {
                                                    "Pengingat · Sesuai Pengaturan"
                                                },
                                                fontSize = 11.5.sp,
                                                color = TextSubtle
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = TextMuted
                                    )
                                }

                                if (item.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.45f))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Catatan", fontSize = 11.sp, color = TextSubtle)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(item.note, fontSize = 13.sp, color = TextWhite)
                                }
                            }
                        }
                    }

                    // Riwayat Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Riwayat",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceElevated
                                    ) {
                                        Text(
                                            text = "${matchingTxs.size}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMuted,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                if (item.lastSkippedDateLabel.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${item.title} pada ${item.lastSkippedDateLabel} dilewati",
                                                fontSize = 12.sp,
                                                color = Color(0xFFFCD34D),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Batalkan",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentBlueBright,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { viewModel.undoSkipRecurringOccurrence(item.id) }
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    .testTag("btn_undo_skip_recurring")
                                            )
                                        }
                                    }
                                }

                                if (matchingTxs.isEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Belum ada catatan pembayaran yang terekam.",
                                        fontSize = 12.5.sp,
                                        color = TextSubtle,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    matchingTxs.forEachIndexed { idx, tx ->
                                        val monthS = ShortMonthsId.getOrElse(tx.monthIndex - 1) { "Nov" }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tx.title.ifBlank { tx.category },
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextWhite,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${tx.dayOfMonth} $monthS ${tx.year} · ${tx.accountName}",
                                                    fontSize = 11.5.sp,
                                                    color = TextSubtle
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = (if (tx.type == TransactionType.INCOME) "+" else "-") +
                                                        CurrencyHelper.formatRupiah(tx.amount),
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (tx.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                IconButton(
                                                    onClick = { viewModel.deleteTransaction(tx.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Hapus catatan",
                                                        tint = TextSubtle,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (idx < matchingTxs.lastIndex) {
                                            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(36.dp)) }
                }
            }
        }
    }

    if (showEditAndRecordTxDialog) {
        AddTransactionDialog(
            viewModel = viewModel,
            initialCategory = item.category,
            initialWallet = item.accountName,
            initialToWallet = item.toAccountName.ifBlank { null },
            initialLabel = item.label,
            initialAmount = item.amount,
            initialNote = item.title.ifBlank { item.note },
            initialSubcategory = item.subcategory,
            initialRecurrence = item.frequency,
            onTransactionSaved = {
                viewModel.markRecurringProcessedNow(item.id)
            },
            onDismiss = { showEditAndRecordTxDialog = false }
        )
    }

    if (showSnoozeDateDialog) {
        DatePickerSubDialog(
            initialDay = expectedDay,
            initialMonth = expectedMonth,
            initialYear = expectedYear,
            onDateSelected = { d, m, y, _, _ ->
                val dateStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m, y)
                viewModel.snoozeRecurringOccurrence(item.id, dateStr)
                showSnoozeDateDialog = false
            },
            onDismiss = { showSnoozeDateDialog = false }
        )
    }

    if (showReminderSheet) {
        RecurringReminderSheet(
            initialEnabled = item.reminderEnabled,
            initialOptions = item.reminderOptions,
            onSave = { enabled, options ->
                viewModel.updateRecurringReminders(item.id, enabled, options)
                showReminderSheet = false
            },
            onDismiss = { showReminderSheet = false }
        )
    }

    if (showDeleteConfirm) {
        DeleteRecurringConfirmDialog(
            title = item.title,
            recordedCount = matchingTxs.size,
            onConfirm = { deleteHistory ->
                viewModel.deleteRecurringWithTransactions(item.id, deleteHistory)
                showDeleteConfirm = false
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun DetailStatBox(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSubtle,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringReminderSheet(
    initialEnabled: Boolean,
    initialOptions: List<String>,
    onSave: (Boolean, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var enabled by remember { mutableStateOf(initialEnabled) }
    var selectedOptions by remember {
        mutableStateOf(
            if (initialOptions.isEmpty()) setOf("Pada hari jatuh tempo", "1 hari sebelumnya")
            else initialOptions.toSet()
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Pengingat untuk tagihan ini",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Aktifkan pengingat", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                        Text("Terima notifikasi sebelum jatuh tempo", fontSize = 11.5.sp, color = TextMuted)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ReminderChoices.forEach { option ->
                val isChecked = option in selectedOptions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = enabled) {
                            selectedOptions = if (isChecked) selectedOptions - option else selectedOptions + option
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        enabled = enabled,
                        onCheckedChange = { checked ->
                            selectedOptions = if (checked) selectedOptions + option else selectedOptions - option
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentBlue,
                            uncheckedColor = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = option,
                        fontSize = 14.sp,
                        color = if (enabled) TextWhite else TextSubtle
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSave(enabled, selectedOptions.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_recurring_reminder")
            ) {
                Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun DeleteRecurringConfirmDialog(
    title: String,
    recordedCount: Int,
    onConfirm: (deleteHistory: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var deleteHistory by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ExpenseRedSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, null, tint = ExpenseRed, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Hapus Transaksi Berulang?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Jadwal \"$title\" akan dihapus dari daftar berulang.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                if (recordedCount > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { deleteHistory = !deleteHistory }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = deleteHistory,
                                onCheckedChange = { deleteHistory = it },
                                colors = CheckboxDefaults.colors(checkedColor = ExpenseRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(modifier = Modifier.padding(top = 6.dp)) {
                                Text(
                                    text = "Hapus juga $recordedCount catatan yang tercatat",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Saldo akan berubah sesuai jumlah yang dipindahkan transaksi tersebut.",
                                    fontSize = 11.sp,
                                    color = TextSubtle,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Batal", color = TextWhite, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onConfirm(deleteHistory) },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_confirm_delete_recurring")
                    ) {
                        Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class DetectedSubscriptionCandidate(
    val title: String,
    val category: String,
    val subcategory: String,
    val accountName: String,
    val amount: Double,
    val type: TransactionType,
    val dayOfMonth: Int,
    val occurrences: Int
)

@Composable
fun LanggananDiscoveryDialog(
    recurringList: List<RecurringTransaction>,
    allTransactions: List<Transaction>,
    onTrackCandidate: (RecurringTemplatePrefill) -> Unit,
    onSelectRecurring: (RecurringTransaction) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val activeSubs = remember(recurringList) {
        recurringList.filter { it.isActive && it.type == TransactionType.EXPENSE }
    }
    val monthlyTotal = remember(activeSubs) {
        activeSubs.sumOf { item ->
            when {
                item.frequency.contains("hari", ignoreCase = true) -> item.amount * 30
                item.frequency.contains("2 minggu", ignoreCase = true) -> item.amount * 2
                item.frequency.contains("minggu", ignoreCase = true) -> item.amount * 4
                item.frequency.contains("tahun", ignoreCase = true) -> item.amount / 12.0
                else -> item.amount
            }
        }
    }
    val yearlyTotal = monthlyTotal * 12.0

    val detectedCandidates = remember(allTransactions, recurringList) {
        val existingTitles = recurringList.map { it.title.lowercase() }.toSet()
        allTransactions
            .filter { it.type == TransactionType.EXPENSE && it.title.isNotBlank() }
            .groupBy { it.title.trim().lowercase() }
            .mapNotNull { (_, group) ->
                val latest = group.maxByOrNull { it.year * 10000 + it.monthIndex * 100 + it.dayOfMonth } ?: return@mapNotNull null
                if (latest.title.lowercase() in existingTitles) return@mapNotNull null
                val sameAmtCount = group.count { kotlin.math.abs(it.amount - latest.amount) < 1000.0 }
                val isSubLike = group.size >= 2 ||
                    latest.category.contains("Bills", ignoreCase = true) ||
                    latest.subcategory.contains("Langganan", ignoreCase = true) ||
                    latest.subcategory.contains("Internet", ignoreCase = true)
                if (sameAmtCount >= 2 || isSubLike) {
                    DetectedSubscriptionCandidate(
                        title = latest.title,
                        category = latest.category,
                        subcategory = latest.subcategory,
                        accountName = latest.accountName,
                        amount = latest.amount,
                        type = latest.type,
                        dayOfMonth = latest.dayOfMonth,
                        occurrences = group.size
                    )
                } else null
            }
            .sortedByDescending { it.occurrences }
            .take(8)
    }

    FullScreenModalContainer(onDismissRequest = onDismiss) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .fullScreenBottomSafePadding()
                .testTag("screen_langganan"),
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fullScreenTopSafePadding()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Langganan",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Summary Banner
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "${activeSubs.size} PEMBAYARAN BERULANG",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentBlueBright,
                                    letterSpacing = 0.6.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${CurrencyHelper.formatRupiah(monthlyTotal)}/bulan",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "${CurrencyHelper.formatRupiah(yearlyTotal)}/tahun",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = AccentBlue.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "Ditemukan: ${detectedCandidates.size}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentBlueBright,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = IncomeGreenSoft
                                    ) {
                                        Text(
                                            text = "Sudah jadi tagihan: ${activeSubs.size}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = IncomeGreen,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Ditemukan di tempat Anda
                    item {
                        Text(
                            text = "Ditemukan di tempat Anda",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }

                    if (detectedCandidates.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DarkSurface,
                                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Belum ada transaksi berulang baru yang terdeteksi dari riwayat Anda.",
                                    fontSize = 12.5.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(detectedCandidates, key = { it.title }) { cand ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DarkSurface,
                                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(AccentBlue.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getCategoryIcon(cand.category),
                                                contentDescription = null,
                                                tint = AccentBlueBright,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = cand.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextWhite,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${CurrencyHelper.formatRupiah(cand.amount)} · ${cand.occurrences}× tercatat",
                                                fontSize = 11.5.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onTrackCandidate(
                                                RecurringTemplatePrefill(
                                                    title = cand.title,
                                                    amount = cand.amount,
                                                    type = cand.type,
                                                    category = cand.category,
                                                    dayOfMonth = cand.dayOfMonth,
                                                    icon = Icons.Default.Subscriptions
                                                )
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Pantau", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Sudah jadi tagihan
                    item {
                        Text(
                            text = "Sudah jadi tagihan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }

                    if (activeSubs.isEmpty()) {
                        item {
                            Text(
                                text = "Belum ada langganan aktif yang dipantau.",
                                fontSize = 12.5.sp,
                                color = TextSubtle
                            )
                        }
                    } else {
                        items(activeSubs, key = { it.id }) { sub ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DarkSurface,
                                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectRecurring(sub) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        InstitutionLogoBadge(
                                            walletName = sub.logoName.ifBlank { sub.accountName },
                                            iconName = sub.logoName,
                                            size = 36.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = sub.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextWhite,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${frequencyBadgeText(sub.frequency)} · Tgl ${sub.dayOfMonth}",
                                                fontSize = 11.5.sp,
                                                color = TextSubtle
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = CurrencyHelper.formatRupiah(sub.amount),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(IncomeGreenSoft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, null, tint = IncomeGreen, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Pola pembayaran rutin dari riwayat transaksi Anda terdeteksi secara otomatis agar tagihan bulanan lebih mudah dipantau.",
                            fontSize = 11.5.sp,
                            color = TextSubtle,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
