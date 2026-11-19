package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.EventItem
import com.example.data.model.EventPlannedExpense
import com.example.data.model.MainCategory
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.FullScreenModalContainer
import com.example.ui.components.fullScreenBottomSafePadding
import com.example.ui.components.fullScreenTopSafePadding
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.dialogs.CategoryPickerSubDialog
import com.example.ui.dialogs.DeleteEventConfirmDialog
import com.example.ui.dialogs.formatEventReadableDate
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
import com.example.util.ThousandsSeparatorVisualTransformation
import java.util.Calendar
import java.util.concurrent.TimeUnit

fun computeEventDaysElapsed(event: EventItem): Int {
    val startParts = event.startDate.split("/")
    if (startParts.size == 3) {
        val sd = startParts[0].toIntOrNull()
        val sm = startParts[1].toIntOrNull()
        val sy = startParts[2].toIntOrNull()
        if (sd != null && sm != null && sy != null) {
            val startCal = Calendar.getInstance().apply {
                set(sy, sm - 1, sd, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val refCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (event.endDate.isNotBlank()) {
                val endParts = event.endDate.split("/")
                val ed = endParts.getOrNull(0)?.toIntOrNull()
                val em = endParts.getOrNull(1)?.toIntOrNull()
                val ey = endParts.getOrNull(2)?.toIntOrNull()
                if (ed != null && em != null && ey != null) {
                    val endCal = Calendar.getInstance().apply {
                        set(ey, em - 1, ed, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (event.isCompleted || refCal.after(endCal)) {
                        val diff = TimeUnit.MILLISECONDS.toDays(endCal.timeInMillis - startCal.timeInMillis).toInt() + 1
                        return diff.coerceAtLeast(1)
                    }
                }
            }
            val diff = TimeUnit.MILLISECONDS.toDays(refCal.timeInMillis - startCal.timeInMillis).toInt() + 1
            return diff.coerceAtLeast(1)
        }
    }
    return 1
}

@Composable
fun AcaraDetailScreen(
    eventId: String,
    viewModel: FinanceViewModel,
    onEdit: (EventItem) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val events by viewModel.events.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allCategories by viewModel.categories.collectAsState()

    val event = events.find { it.id == eventId }
    if (event == null) {
        onDismiss()
        return
    }

    val eventColor = Color(event.colorHex)
    val daysElapsed = remember(event) { computeEventDaysElapsed(event) }
    val linkedTxs = remember(allTransactions, event.name) {
        allTransactions.filter { tx ->
            tx.label.split(",").map { it.trim().lowercase() }.contains(event.name.lowercase()) ||
                tx.label.equals(event.name, ignoreCase = true)
        }.sortedWith(
            compareByDescending<Transaction> { it.year }
                .thenByDescending { it.monthIndex }
                .thenByDescending { it.dayOfMonth }
        )
    }

    val totalExpense = remember(linkedTxs) { linkedTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } }
    val totalIncome = remember(linkedTxs) { linkedTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } }
    val netTotal = totalIncome - totalExpense
    val hasBudget = event.targetBudget > 0.0
    val budgetProgress = if (hasBudget) (totalExpense / event.targetBudget).toFloat().coerceIn(0f, 1f) else 0f
    val budgetPercent = if (hasBudget) ((totalExpense / event.targetBudget) * 100).toInt() else 0

    var selectedPlannedForAction by remember { mutableStateOf<EventPlannedExpense?>(null) }
    var editingPlannedExpense by remember { mutableStateOf<EventPlannedExpense?>(null) }
    var showAddPlannedExpenseDialog by remember { mutableStateOf(false) }
    var showLinkTransactionsSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var prefillPlannedForTx by remember { mutableStateOf<EventPlannedExpense?>(null) }

    FullScreenModalContainer(onDismissRequest = onDismiss) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .testTag("screen_acara_detail"),
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        prefillPlannedForTx = null
                        showAddTxDialog = true
                    },
                    containerColor = AccentBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fullScreenBottomSafePadding()
                        .padding(bottom = 8.dp)
                        .size(56.dp)
                        .testTag("fab_detail_add_event_tx")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Catat Transaksi", tint = Color.White)
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Top Bar: <- <Nama>   [Edit] [Trash]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fullScreenTopSafePadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_back_acara_detail")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextWhite)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = event.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onEdit(event) }
                                .testTag("btn_edit_event_detail")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Acara", tint = TextWhite, modifier = Modifier.size(18.dp))
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ExpenseRedSoft,
                            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showDeleteConfirm = true }
                                .testTag("btn_delete_event_detail")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus Acara", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Gradient Hero Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(142.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            eventColor.copy(alpha = 0.92f),
                                            eventColor.copy(alpha = 0.55f),
                                            DarkSurface
                                        )
                                    )
                                )
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_acara_travel),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(142.dp)
                                    .alpha(0.36f)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color.Black.copy(alpha = 0.35f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (event.isCompleted) TextMuted else IncomeGreen)
                                        )
                                        Text(
                                            text = if (event.isCompleted) "SELESAI" else "AKTIF · Hari $daysElapsed",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = event.name,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    val dateRangeText = buildString {
                                        val s = if (event.startDate.isNotBlank()) formatEventReadableDate(event.startDate) else "${event.monthIndex}/${event.year}"
                                        append(s)
                                        append(" - ")
                                        append(if (event.endDate.isNotBlank()) formatEventReadableDate(event.endDate) else "sekarang")
                                    }
                                    Text(
                                        text = dateRangeText,
                                        fontSize = 12.5.sp,
                                        color = Color.White.copy(alpha = 0.88f)
                                    )
                                }
                            }
                        }
                    }

                    // Stats Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "${event.eventType.uppercase()} · Hari $daysElapsed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.6.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Pengeluaran", fontSize = 12.sp, color = TextSubtle)
                                Text(
                                    text = CurrencyHelper.formatRupiah(totalExpense),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalExpense > 0) ExpenseRed else TextWhite
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Pemasukan", fontSize = 11.sp, color = TextSubtle)
                                        Text(
                                            text = CurrencyHelper.formatRupiah(totalIncome),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Bersih", fontSize = 11.sp, color = TextSubtle)
                                        Text(
                                            text = CurrencyHelper.formatRupiah(netTotal),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (netTotal >= 0) IncomeGreen else TextWhite
                                        )
                                    }
                                    Column(modifier = Modifier.weight(0.6f), horizontalAlignment = Alignment.End) {
                                        Text("Entri", fontSize = 11.sp, color = TextSubtle)
                                        Text(
                                            text = "${linkedTxs.size}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                    }
                                }

                                if (hasBudget) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Anggaran ${CurrencyHelper.formatRupiah(event.targetBudget)}",
                                            fontSize = 12.sp,
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "$budgetPercent%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (totalExpense > event.targetBudget) ExpenseRed else AccentBlueBright
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { budgetProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = if (totalExpense > event.targetBudget) ExpenseRed else eventColor,
                                        trackColor = DarkSurfaceElevated,
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }

                    // Biaya yang direncanakan Card
                    item {
                        val paidCount = event.plannedExpenses.count { it.isPaid }
                        val totalPlannedCount = event.plannedExpenses.size
                        val paidProgress = if (totalPlannedCount > 0) paidCount.toFloat() / totalPlannedCount.toFloat() else 0f

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Biaya yang direncanakan",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = "$paidCount dari $totalPlannedCount dibayar",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (paidCount == totalPlannedCount && totalPlannedCount > 0) IncomeGreen else AccentBlueBright
                                    )
                                }

                                if (totalPlannedCount > 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { paidProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = IncomeGreen,
                                        trackColor = DarkSurfaceElevated,
                                        strokeCap = StrokeCap.Round
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    event.plannedExpenses.forEachIndexed { idx, pe ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { selectedPlannedForAction = pe }
                                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                                .testTag("planned_expense_row_${pe.id}"),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (pe.isPaid) IncomeGreenSoft else DarkSurfaceElevated),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (pe.isPaid) Icons.Default.CheckCircle else CategoryIconHelper.getCategoryIcon(pe.category),
                                                        contentDescription = null,
                                                        tint = if (pe.isPaid) IncomeGreen else AccentBlueBright,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = pe.name,
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextWhite,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (pe.isPaid) IncomeGreenSoft else DarkSurfaceElevated
                                                    ) {
                                                        Text(
                                                            text = if (pe.isPaid) "Dibayar" else "Direncanakan",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = if (pe.isPaid) IncomeGreen else TextMuted,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (pe.amount > 0) CurrencyHelper.formatRupiah(pe.amount) else "-",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (pe.isPaid) IncomeGreen else TextWhite
                                            )
                                        }
                                        if (idx < event.plannedExpenses.lastIndex) {
                                            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showAddPlannedExpenseDialog = true }
                                        .padding(vertical = 8.dp)
                                        .testTag("btn_add_planned_expense_detail"),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, null, tint = AccentBlueBright, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tambahkan biaya yang direncanakan",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentBlueBright
                                    )
                                }
                            }
                        }
                    }

                    // Tandai selesai Switch Card
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
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DarkSurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Flag, null, tint = AccentBlueBright, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Tandai selesai",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite
                                        )
                                        Text(
                                            text = "Menghentikan hitungan hari. Anda dapat membukanya kembali.",
                                            fontSize = 11.5.sp,
                                            color = TextSubtle,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = event.isCompleted,
                                    onCheckedChange = { viewModel.toggleEventCompleted(event.id) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = IncomeGreen
                                    ),
                                    modifier = Modifier.testTag("switch_event_completed")
                                )
                            }
                        }
                    }

                    // Transaksi Section
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Transaksi (${linkedTxs.size})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AccentBlue.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showLinkTransactionsSheet = true }
                                        .testTag("btn_link_event_transactions")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(Icons.Default.Link, null, tint = AccentBlueBright, modifier = Modifier.size(15.dp))
                                        Text("Tautkan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccentBlueBright)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (linkedTxs.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = DarkSurface,
                                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Belum ada transaksi yang tertaut ke acara ini. Ketuk + untuk mencatat atau Tautkan untuk memilih transaksi yang sudah ada.",
                                        fontSize = 12.5.sp,
                                        color = TextMuted,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = DarkSurface,
                                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        linkedTxs.forEachIndexed { idx, tx ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(DarkSurfaceElevated),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = CategoryIconHelper.getCategoryIcon(tx.category),
                                                            contentDescription = null,
                                                            tint = AccentBlueBright,
                                                            modifier = Modifier.size(17.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = tx.title.ifBlank { tx.category },
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = TextWhite,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${tx.dayOfMonth}/${tx.monthIndex}/${tx.year} · ${tx.accountName}",
                                                            fontSize = 11.sp,
                                                            color = TextSubtle
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = (if (tx.type == TransactionType.INCOME) "+" else "-") +
                                                        CurrencyHelper.formatRupiah(tx.amount),
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (tx.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                                                )
                                            }
                                            if (idx < linkedTxs.lastIndex) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 14.dp),
                                                    color = DarkCardBorder.copy(alpha = 0.45f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4-Action Sheet on tapping a Planned Expense item
    selectedPlannedForAction?.let { pe ->
        PlannedExpenseActionSheet(
            expense = pe,
            onTogglePaid = {
                viewModel.toggleEventPlannedExpensePaid(event.id, pe.id)
                selectedPlannedForAction = null
            },
            onRecordTransaction = {
                selectedPlannedForAction = null
                prefillPlannedForTx = pe
                showAddTxDialog = true
            },
            onEdit = {
                selectedPlannedForAction = null
                editingPlannedExpense = pe
            },
            onDelete = {
                viewModel.deleteEventPlannedExpense(event.id, pe.id)
                selectedPlannedForAction = null
            },
            onDismiss = { selectedPlannedForAction = null }
        )
    }

    if (showAddPlannedExpenseDialog) {
        AddEditPlannedExpenseDialog(
            allCategories = allCategories,
            initialExpense = null,
            onSave = { newPe ->
                viewModel.addEventPlannedExpense(event.id, newPe)
                showAddPlannedExpenseDialog = false
            },
            onDismiss = { showAddPlannedExpenseDialog = false }
        )
    }

    editingPlannedExpense?.let { pe ->
        AddEditPlannedExpenseDialog(
            allCategories = allCategories,
            initialExpense = pe,
            onSave = { updated ->
                viewModel.updateEventPlannedExpense(event.id, updated)
                editingPlannedExpense = null
            },
            onDismiss = { editingPlannedExpense = null }
        )
    }

    if (showLinkTransactionsSheet) {
        LinkTransactionsToEventSheet(
            eventName = event.name,
            allTransactions = allTransactions,
            onToggleLink = { txId, shouldLink ->
                viewModel.linkTransactionToEvent(txId, event.name, shouldLink)
            },
            onDismiss = { showLinkTransactionsSheet = false }
        )
    }

    if (showAddTxDialog) {
        val pe = prefillPlannedForTx
        AddTransactionDialog(
            viewModel = viewModel,
            initialCategory = pe?.category?.ifBlank { "Other" } ?: "Other",
            initialSubcategory = pe?.subcategory ?: "",
            initialAmount = pe?.amount ?: 0.0,
            initialNote = pe?.name ?: "",
            initialLabel = event.name,
            onTransactionSaved = {
                if (pe != null && !pe.isPaid) {
                    viewModel.toggleEventPlannedExpensePaid(event.id, pe.id)
                }
            },
            onDismiss = {
                showAddTxDialog = false
                prefillPlannedForTx = null
            }
        )
    }

    if (showDeleteConfirm) {
        DeleteEventConfirmDialog(
            eventName = event.name,
            onConfirm = {
                viewModel.deleteEvent(event.id)
                showDeleteConfirm = false
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannedExpenseActionSheet(
    expense: EventPlannedExpense,
    onTogglePaid: () -> Unit,
    onRecordTransaction: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = expense.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            if (expense.amount > 0) {
                Text(
                    text = CurrencyHelper.formatRupiah(expense.amount),
                    fontSize = 13.sp,
                    color = AccentBlueBright
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            PlannedActionRow(
                icon = Icons.Default.Check,
                tint = IncomeGreen,
                label = if (expense.isPaid) "Tandai belum dibayar" else "Tandai sebagai dibayar",
                onClick = onTogglePaid
            )
            PlannedActionRow(
                icon = Icons.Default.Add,
                tint = AccentBlueBright,
                label = "Catat transaksi",
                onClick = onRecordTransaction
            )
            PlannedActionRow(
                icon = Icons.Default.Edit,
                tint = TextWhite,
                label = "Ubah",
                onClick = onEdit
            )
            PlannedActionRow(
                icon = Icons.Default.Delete,
                tint = ExpenseRed,
                label = "Hapus",
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun PlannedActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = tint)
    }
}

@Composable
private fun AddEditPlannedExpenseDialog(
    allCategories: List<MainCategory>,
    initialExpense: EventPlannedExpense?,
    onSave: (EventPlannedExpense) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialExpense?.name ?: "") }
    var amountText by remember {
        mutableStateOf(
            if (initialExpense != null && initialExpense.amount > 0) initialExpense.amount.toLong().toString() else ""
        )
    }
    var category by remember { mutableStateOf(initialExpense?.category ?: "Other") }
    var subcategory by remember { mutableStateOf(initialExpense?.subcategory ?: "") }
    var showCatPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (initialExpense == null) "Tambah Biaya Rencana" else "Ubah Biaya Rencana",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCatPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(CategoryIconHelper.getCategoryIcon(category), null, tint = AccentBlueBright, modifier = Modifier.size(20.dp))
                        Text(
                            text = if (subcategory.isNotBlank()) "$category · $subcategory" else category,
                            fontSize = 13.5.sp,
                            color = TextWhite,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Nama item", color = TextSubtle) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 12) amountText = digits
                    },
                    placeholder = { Text("Perkiraan jumlah", color = TextSubtle) },
                    prefix = { Text("Rp ", color = TextMuted, fontWeight = FontWeight.SemiBold) },
                    visualTransformation = ThousandsSeparatorVisualTransformation('.'),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Batal", color = TextWhite)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val updated = initialExpense?.copy(
                                    name = name.trim(),
                                    category = category,
                                    subcategory = subcategory,
                                    amount = amountText.toDoubleOrNull() ?: 0.0
                                ) ?: EventPlannedExpense(
                                    name = name.trim(),
                                    category = category,
                                    subcategory = subcategory,
                                    amount = amountText.toDoubleOrNull() ?: 0.0
                                )
                                onSave(updated)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCatPicker) {
        CategoryPickerSubDialog(
            currentCategory = category,
            currentSubcategory = subcategory,
            categories = allCategories,
            onCategoryAndSubcategorySelected = { cat, sub ->
                category = cat
                subcategory = sub
                showCatPicker = false
            },
            onDismiss = { showCatPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkTransactionsToEventSheet(
    eventName: String,
    allTransactions: List<Transaction>,
    onToggleLink: (txId: String, link: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sortedTxs = remember(allTransactions) {
        allTransactions.sortedWith(
            compareByDescending<Transaction> { it.year }
                .thenByDescending { it.monthIndex }
                .thenByDescending { it.dayOfMonth }
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
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Tautkan Transaksi ke $eventName",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pilih transaksi yang ingin dihitung dalam acara ini.",
                fontSize = 12.5.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (sortedTxs.isEmpty()) {
                Text(
                    text = "Belum ada transaksi yang tersimpan.",
                    fontSize = 13.sp,
                    color = TextSubtle,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedTxs, key = { it.id }) { tx ->
                        val isLinked = tx.label.split(",").map { it.trim().lowercase() }.contains(eventName.lowercase()) ||
                            tx.label.equals(eventName, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLinked) AccentBlue.copy(alpha = 0.14f) else DarkSurfaceElevated,
                            border = BorderStroke(1.dp, if (isLinked) AccentBlue else DarkCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleLink(tx.id, !isLinked) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(
                                        checked = isLinked,
                                        onCheckedChange = { onToggleLink(tx.id, it) },
                                        colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
                                    )
                                    Column {
                                        Text(
                                            text = tx.title.ifBlank { tx.category },
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${tx.dayOfMonth}/${tx.monthIndex}/${tx.year} · ${tx.accountName}",
                                            fontSize = 11.sp,
                                            color = TextSubtle
                                        )
                                    }
                                }
                                Text(
                                    text = CurrencyHelper.formatRupiah(tx.amount),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Selesai", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
