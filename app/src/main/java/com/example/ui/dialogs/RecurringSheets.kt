package com.example.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AccountWallet
import com.example.data.model.RecurringTransaction
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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale

data class RecurringTemplatePrefill(
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val dayOfMonth: Int,
    val icon: ImageVector
)

private data class BillLogoEntry(
    val name: String,
    val categoryTag: String,
    val tab: String, // "Langganan" or "Akun"
    val colorHex: Long
)

private val BillLogoCatalog = listOf(
    BillLogoEntry("ChatGPT", "AI", "Langganan", 0xFF10A37FL),
    BillLogoEntry("Gemini", "AI", "Langganan", 0xFF4285F4L),
    BillLogoEntry("Claude", "AI", "Langganan", 0xFFD97706L),
    BillLogoEntry("Duolingo", "EDUCATION", "Langganan", 0xFF58CC02L),
    BillLogoEntry("Coursera", "EDUCATION", "Langganan", 0xFF0056D2L),
    BillLogoEntry("Ruangguru", "EDUCATION", "Langganan", 0xFF22D3EEL),
    BillLogoEntry("Strava", "FITNESS", "Langganan", 0xFFFC4C02L),
    BillLogoEntry("Gym", "FITNESS", "Langganan", 0xFFEF4444L),
    BillLogoEntry("PlayStation", "GAMING", "Langganan", 0xFF003791L),
    BillLogoEntry("Steam", "GAMING", "Langganan", 0xFF1B2838L),
    BillLogoEntry("Netflix", "GAMING", "Langganan", 0xFFE50914L),
    BillLogoEntry("Spotify", "GAMING", "Langganan", 0xFF1DB954L),
    BillLogoEntry("YouTube", "GAMING", "Langganan", 0xFFFF0000L),
    BillLogoEntry("BRI", "BANK", "Akun", 0xFF00529CL),
    BillLogoEntry("BCA", "BANK", "Akun", 0xFF005BACL),
    BillLogoEntry("Mandiri", "BANK", "Akun", 0xFF003D79L),
    BillLogoEntry("BNI", "BANK", "Akun", 0xFFF15A22L),
    BillLogoEntry("Bank Jago", "BANK", "Akun", 0xFFFACC15L),
    BillLogoEntry("SeaBank", "BANK", "Akun", 0xFFF97316L),
    BillLogoEntry("GoPay", "E-WALLET", "Akun", 0xFF00AED6L),
    BillLogoEntry("OVO", "E-WALLET", "Akun", 0xFF4C3494L),
    BillLogoEntry("DANA", "E-WALLET", "Akun", 0xFF118EEAL),
    BillLogoEntry("ShopeePay", "E-WALLET", "Akun", 0xFFEE4D2DL)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringSortDisplaySheet(
    currentSort: RecurringSortOption,
    currentStyle: RecurringListStyle,
    onSelectSort: (RecurringSortOption) -> Unit,
    onSelectStyle: (RecurringListStyle) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextSubtle.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Urutkan berdasarkan",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            RecurringSortOption.entries.forEach { option ->
                val isSelected = currentSort == option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectSort(option)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                        .testTag("sort_option_${option.name}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.label,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = TextWhite
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Terpilih",
                            tint = AccentBlueBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Gaya daftar",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            RecurringListStyle.entries.forEach { style ->
                val isSelected = currentStyle == style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSelectStyle(style)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                        .testTag("list_style_${style.name}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = style.label,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = TextWhite
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Terpilih",
                            tint = AccentBlueBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringQuickTemplatesSheet(
    actionableItems: List<RecurringComputedItem>,
    onProcessAllDue: () -> Unit,
    onSelectTemplate: (RecurringTemplatePrefill) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val templates = remember {
        listOf(
            RecurringTemplatePrefill("Gaji Pokok", 5000000.0, TransactionType.INCOME, "Salary", 1, Icons.Default.Payments),
            RecurringTemplatePrefill("Listrik PLN", 250000.0, TransactionType.EXPENSE, "Bills & Fees", 10, Icons.Default.ElectricBolt),
            RecurringTemplatePrefill("Tagihan WiFi", 350000.0, TransactionType.EXPENSE, "Bills & Fees", 20, Icons.Default.Wifi),
            RecurringTemplatePrefill("Langganan Streaming", 150000.0, TransactionType.EXPENSE, "Entertainment", 15, Icons.Default.Subscriptions)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            Text("Aksi Cepat & Template", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Catat tagihan yang jatuh tempo sekaligus atau buat jadwal dari template", fontSize = 12.sp, color = TextMuted)

            if (actionableItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onProcessAllDue,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66B28C)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catat Semua Jatuh Tempo (${actionableItems.size})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text("Template Populer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
            Spacer(modifier = Modifier.height(10.dp))

            templates.forEach { tpl ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTemplate(tpl) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (tpl.type == TransactionType.INCOME) IncomeGreenSoft else ExpenseRedSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tpl.icon,
                                contentDescription = null,
                                tint = if (tpl.type == TransactionType.INCOME) IncomeGreen else ExpenseRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tpl.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                            Text("${tpl.category} · Tgl ${tpl.dayOfMonth}", fontSize = 11.sp, color = TextSubtle)
                        }
                        Text(CurrencyHelper.formatRupiah(tpl.amount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentBlueBright)
                    }
                }
            }
        }
    }
}

@Composable
fun RecurringHelpDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Tentang Transaksi Berulang", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.height(12.dp))
                HelpBulletRow("Mendatang", "Menampilkan jadwal yang jatuh tempo dalam 7 hari ke depan atau belum diproses.")
                HelpBulletRow("Langganan", "Otomatis mendeteksi pola tagihan rutin dari riwayat transaksi Anda.")
                HelpBulletRow("Detail & Riwayat", "Ketuk baris transaksi berulang untuk melihat progres, menunda tanggal, menyetel pengingat, atau menghapus.")
                HelpBulletRow("Catat atau Lewati", "Gunakan tombol ✓ untuk mencatat ke dompet atau ↷ untuk melewati siklus saat ini.")
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mengerti", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HelpBulletRow(title: String, body: String) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccentBlueBright)
        Text(body, fontSize = 12.sp, color = TextMuted)
    }
}

private fun formatCalcNumber(value: String): String {
    if (value.isBlank()) return "0"
    val clean = value.toLongOrNull() ?: return value
    val sym = DecimalFormatSymbols(Locale("id", "ID")).apply { groupingSeparator = '.' }
    return DecimalFormat("#,###", sym).format(clean)
}

@Composable
fun AddEditRecurringTransactionDialog(
    viewModel: FinanceViewModel,
    initialItem: RecurringTransaction? = null,
    prefill: RecurringTemplatePrefill? = null,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val wallets by viewModel.wallets.collectAsState()
    val allCategories by viewModel.categories.collectAsState()
    val selectedWalletFilter by viewModel.selectedWalletFilter.collectAsState()

    var amountText by remember {
        mutableStateOf(
            when {
                initialItem != null && initialItem.amount > 0 -> initialItem.amount.toLong().toString()
                prefill != null && prefill.amount > 0 -> prefill.amount.toLong().toString()
                else -> ""
            }
        )
    }
    var pendingLeftOperand by remember { mutableStateOf<Double?>(null) }
    var pendingOperator by remember { mutableStateOf<String?>(null) }

    var selectedType by remember {
        mutableStateOf(initialItem?.type ?: prefill?.type ?: TransactionType.EXPENSE)
    }
    var selectedCategory by remember {
        mutableStateOf(
            initialItem?.category
                ?: prefill?.category
                ?: "Other"
        )
    }
    var selectedSubcategory by remember { mutableStateOf(initialItem?.subcategory ?: "") }
    var isCategoryChosen by remember {
        mutableStateOf(initialItem != null || prefill != null)
    }

    val defaultWalletName = remember(wallets, initialItem, selectedWalletFilter) {
        if (initialItem != null) {
            initialItem.accountName
        } else if (selectedWalletFilter != "Semua" && selectedWalletFilter != "Semua Dompet" && selectedWalletFilter.isNotBlank()) {
            wallets.firstOrNull { it.name.equals(selectedWalletFilter, ignoreCase = true) }?.name
                ?: wallets.firstOrNull()?.name ?: ""
        } else {
            wallets.firstOrNull()?.name ?: ""
        }
    }
    var selectedWallet by remember(defaultWalletName) { mutableStateOf(defaultWalletName) }
    val defaultToWalletName = remember(wallets, initialItem, selectedWallet) {
        val savedDest = initialItem?.toAccountName?.takeIf { it.isNotBlank() }
        if (savedDest != null) {
            wallets.firstOrNull { it.name.equals(savedDest, ignoreCase = true) }?.name ?: savedDest
        } else {
            wallets.firstOrNull { !it.name.equals(selectedWallet, ignoreCase = true) }?.name ?: ""
        }
    }
    var selectedToWallet by remember(initialItem, wallets) { mutableStateOf(defaultToWalletName) }

    val now = remember { Calendar.getInstance() }
    val todayD = now.get(Calendar.DAY_OF_MONTH)
    val todayM = now.get(Calendar.MONTH) + 1
    val todayY = now.get(Calendar.YEAR)
    val defaultStartDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", todayD, todayM, todayY)

    var startDateText by remember {
        mutableStateOf(initialItem?.startDate?.ifBlank { defaultStartDate } ?: defaultStartDate)
    }
    var selectedDay by remember { mutableIntStateOf(initialItem?.dayOfMonth ?: prefill?.dayOfMonth ?: todayD) }
    var endCondition by remember {
        mutableStateOf(
            initialItem?.endCondition ?: if (!initialItem?.endDate.isNullOrBlank()) "ON_DATE" else "NEVER"
        )
    }
    var endDateText by remember { mutableStateOf(initialItem?.endDate ?: "") }
    var endAfterCount by remember { mutableIntStateOf((initialItem?.endAfterCount ?: 5).coerceAtLeast(1)) }
    var noteText by remember { mutableStateOf(initialItem?.note?.ifBlank { initialItem.title } ?: prefill?.title ?: "") }
    var logoName by remember { mutableStateOf(initialItem?.logoName ?: "") }
    var frequency by remember { mutableStateOf(initialItem?.frequency ?: "Setiap bulan") }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showAccountGridPicker by remember { mutableStateOf(false) }
    var showToAccountGridPicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndConditionSheet by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showLogoPickerSheet by remember { mutableStateOf(false) }
    var showFrequencyMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val headerColor = when (selectedType) {
        TransactionType.EXPENSE -> Color(0xFFB91C1C)
        TransactionType.INCOME -> Color(0xFF059669)
        TransactionType.TRANSFER -> Color(0xFF1D4ED8)
    }

    fun applyCalcKey(key: String) {
        errorMessage = null
        when (key) {
            "⌫" -> {
                if (amountText.isNotEmpty()) amountText = amountText.dropLast(1)
            }
            "+", "-", "×", "÷" -> {
                val cur = amountText.toDoubleOrNull() ?: 0.0
                if (pendingLeftOperand != null && pendingOperator != null && amountText.isNotEmpty()) {
                    val res = when (pendingOperator) {
                        "+" -> pendingLeftOperand!! + cur
                        "-" -> (pendingLeftOperand!! - cur).coerceAtLeast(0.0)
                        "×" -> pendingLeftOperand!! * cur
                        "÷" -> if (cur != 0.0) pendingLeftOperand!! / cur else pendingLeftOperand!!
                        else -> cur
                    }
                    pendingLeftOperand = res
                } else {
                    pendingLeftOperand = cur
                }
                pendingOperator = key
                amountText = ""
            }
            "=" -> {
                val cur = amountText.toDoubleOrNull() ?: 0.0
                if (pendingLeftOperand != null && pendingOperator != null) {
                    val res = when (pendingOperator) {
                        "+" -> pendingLeftOperand!! + cur
                        "-" -> (pendingLeftOperand!! - cur).coerceAtLeast(0.0)
                        "×" -> pendingLeftOperand!! * cur
                        "÷" -> if (cur != 0.0) pendingLeftOperand!! / cur else pendingLeftOperand!!
                        else -> cur
                    }
                    amountText = res.toLong().coerceAtLeast(0L).toString()
                    pendingLeftOperand = null
                    pendingOperator = null
                }
            }
            "000" -> {
                if (amountText.isNotEmpty() && amountText.length <= 9) amountText += "000"
            }
            else -> {
                if (amountText == "0") {
                    amountText = key
                } else if (amountText.length < 12) {
                    amountText += key
                }
            }
        }
    }

    fun resolveFinalAmount(): Double {
        val cur = amountText.toDoubleOrNull() ?: 0.0
        return if (pendingLeftOperand != null && pendingOperator != null && amountText.isNotEmpty()) {
            when (pendingOperator) {
                "+" -> pendingLeftOperand!! + cur
                "-" -> (pendingLeftOperand!! - cur).coerceAtLeast(0.0)
                "×" -> pendingLeftOperand!! * cur
                "÷" -> if (cur != 0.0) pendingLeftOperand!! / cur else pendingLeftOperand!!
                else -> cur
            }
        } else cur
    }

    FullScreenModalContainer(onDismissRequest = onDismiss) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .imePadding()
                .testTag("dialog_add_edit_recurring"),
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                // Dark Calculator Keypad & Primary CTA matching video 01:36
                Surface(
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fullScreenBottomSafePadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = ExpenseRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            )
                        } else if ((amountText.toDoubleOrNull() ?: 0.0) <= 0.0) {
                            Text(
                                text = "Tambahkan Jumlah",
                                color = TextSubtle,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            )
                        }

                        val keyRows = listOf(
                            listOf("÷", "7", "8", "9", "⌫"),
                            listOf("×", "4", "5", "6", "="),
                            listOf("-", "1", "2", "3", "000"),
                            listOf("+", "0")
                        )

                        keyRows.forEachIndexed { rIdx, keys ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.5.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                keys.forEach { key ->
                                    val isOp = key in listOf("÷", "×", "-", "+", "=")
                                    val weightVal = if (rIdx == 3 && key == "0") 4f else 1f
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isOp) DarkSurfaceElevated else Color(0xFF1D2230),
                                        border = BorderStroke(0.8.dp, DarkCardBorder.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .weight(weightVal)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { applyCalcKey(key) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (key == "⌫") {
                                                Icon(Icons.AutoMirrored.Filled.Backspace, "Hapus digit", tint = TextWhite, modifier = Modifier.size(18.dp))
                                            } else {
                                                Text(
                                                    text = key,
                                                    fontSize = 16.sp,
                                                    fontWeight = if (isOp) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isOp) AccentBlueBright else TextWhite
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val ctaText = when {
                            !isCategoryChosen -> "Pilih Kategori"
                            initialItem == null -> "Tambah Transaksi Berulang"
                            else -> "Simpan Perubahan"
                        }
                        Button(
                            onClick = {
                                if (!isCategoryChosen) {
                                    showCategoryPicker = true
                                    return@Button
                                }
                                val amt = resolveFinalAmount()
                                if (amt <= 0.0) {
                                    errorMessage = "Masukkan nominal transaksi berulang lebih dari 0"
                                    return@Button
                                }
                                if (selectedType == TransactionType.TRANSFER &&
                                    (selectedToWallet.isBlank() || selectedToWallet.equals(selectedWallet, ignoreCase = true))
                                ) {
                                    errorMessage = if (wallets.size <= 1) {
                                        "Tambahkan minimal 2 dompet atau akun lain terlebih dahulu"
                                    } else {
                                        "Pilih dompet atau akun tujuan yang berbeda"
                                    }
                                    return@Button
                                }
                                val finalTitle = noteText.trim()
                                    .ifBlank {
                                        if (selectedType == TransactionType.TRANSFER && selectedToWallet.isNotBlank()) {
                                            "$selectedCategory ke $selectedToWallet"
                                        } else {
                                            selectedSubcategory.ifBlank { selectedCategory }
                                        }
                                    }
                                val finalEndDate = when (endCondition) {
                                    "ON_DATE" -> endDateText
                                    "AFTER_COUNT" -> computeProjectedEndDate(startDateText, frequency, endAfterCount)
                                    else -> ""
                                }
                                val newItem = RecurringTransaction(
                                    id = initialItem?.id ?: java.util.UUID.randomUUID().toString(),
                                    title = finalTitle,
                                    amount = amt,
                                    type = selectedType,
                                    category = selectedCategory,
                                    subcategory = selectedSubcategory,
                                    accountName = selectedWallet,
                                    toAccountName = if (selectedType == TransactionType.TRANSFER) selectedToWallet else "",
                                    dayOfMonth = selectedDay,
                                    frequency = frequency,
                                    startDate = startDateText,
                                    endDate = finalEndDate,
                                    note = noteText.trim(),
                                    label = initialItem?.label ?: "",
                                    isActive = initialItem?.isActive ?: true,
                                    autoDeduct = initialItem?.autoDeduct ?: true,
                                    lastProcessedMonthYear = initialItem?.lastProcessedMonthYear ?: "",
                                    endCondition = endCondition,
                                    endAfterCount = if (endCondition == "AFTER_COUNT") endAfterCount else 0,
                                    logoName = logoName,
                                    skippedCount = initialItem?.skippedCount ?: 0,
                                    snoozedUntilDate = initialItem?.snoozedUntilDate ?: "",
                                    lastSkippedDateLabel = initialItem?.lastSkippedDateLabel ?: "",
                                    reminderEnabled = initialItem?.reminderEnabled ?: false,
                                    reminderOptions = initialItem?.reminderOptions ?: listOf("Pada hari jatuh tempo", "1 hari sebelumnya")
                                )
                                if (initialItem == null) {
                                    viewModel.addRecurringTransaction(newItem)
                                } else {
                                    viewModel.updateRecurringTransaction(newItem)
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isCategoryChosen) AccentBlue else Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_save_recurring_transaction")
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(ctaText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Colored Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerColor)
                        .fullScreenTopSafePadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (initialItem == null) "Tambah transaksi berulang" else "Edit transaksi berulang",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        if (initialItem != null) {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showCategoryPicker = true }
                                .padding(end = 8.dp)
                                .testTag("btn_recurring_pick_category")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.getCategoryIcon(selectedCategory),
                                    contentDescription = "Kategori",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isCategoryChosen) selectedSubcategory.ifBlank { selectedCategory } else "Pilih Kategori",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = when (selectedType) {
                                        TransactionType.EXPENSE -> "Pengeluaran"
                                        TransactionType.INCOME -> "Pemasukan"
                                        TransactionType.TRANSFER -> "Transfer"
                                    },
                                    fontSize = 11.5.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val displayAmount = if (pendingOperator != null && pendingLeftOperand != null) {
                                "${formatCalcNumber(pendingLeftOperand!!.toLong().toString())} $pendingOperator ${if (amountText.isEmpty()) "" else formatCalcNumber(amountText)}"
                            } else {
                                formatCalcNumber(amountText)
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = displayAmount,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "IDR",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Form Rows
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    val isTransferMode = selectedType == TransactionType.TRANSFER
                    // 1. Akun / Dari akun
                    val walletIcon = wallets.firstOrNull { it.name.equals(selectedWallet, ignoreCase = true) }?.iconName ?: ""
                    RecurringFormRow(
                        icon = {
                            InstitutionLogoBadge(walletName = selectedWallet, iconName = walletIcon, size = 26.dp)
                        },
                        label = if (isTransferMode) "Dari akun" else "Akun",
                        value = selectedWallet.ifBlank { "Pilih akun" },
                        onClick = { showAccountGridPicker = true },
                        testTag = "row_recurring_account"
                    )
                    FormDivider()

                    // 1b. Ke dompet / akun (ketika kategori Transfer dipilih)
                    if (isTransferMode) {
                        val toWalletIcon = wallets.firstOrNull { it.name.equals(selectedToWallet, ignoreCase = true) }?.iconName ?: ""
                        RecurringFormRow(
                            icon = {
                                if (selectedToWallet.isNotBlank()) {
                                    InstitutionLogoBadge(walletName = selectedToWallet, iconName = toWalletIcon, size = 26.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallReceived,
                                        contentDescription = "Akun Tujuan",
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            label = "Ke dompet / akun",
                            value = selectedToWallet.ifBlank { "Pilih akun tujuan" },
                            onClick = { showToAccountGridPicker = true },
                            testTag = "row_recurring_to_account"
                        )
                        FormDivider()
                    }

                    // 2. Tanggal Mulai
                    RecurringFormRow(
                        icon = {
                            Icon(Icons.Default.CalendarToday, null, tint = AccentBlueBright, modifier = Modifier.size(20.dp))
                        },
                        label = "Tanggal Mulai",
                        value = formatDateForDisplay(startDateText),
                        onClick = { showStartDatePicker = true },
                        testTag = "row_recurring_start_date"
                    )
                    FormDivider()

                    // 3. Kapan berakhirnya?
                    val endValueLabel = when (endCondition) {
                        "ON_DATE" -> if (endDateText.isNotBlank()) "Pada ${formatDateForDisplay(endDateText)}" else "Pada tanggal"
                        "AFTER_COUNT" -> "Setelah $endAfterCount kali"
                        else -> "Tanpa Tanggal Akhir"
                    }
                    RecurringFormRow(
                        icon = {
                            Icon(Icons.Default.EventBusy, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        },
                        label = "Tanggal Akhir",
                        value = endValueLabel,
                        onClick = { showEndConditionSheet = true },
                        testTag = "row_recurring_end_condition"
                    )
                    FormDivider()

                    // 4. Tulis catatan (inline text input)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        BasicTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            textStyle = TextStyle(color = TextWhite, fontSize = 15.sp),
                            cursorBrush = SolidColor(AccentBlue),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_recurring_note"),
                            decorationBox = { inner ->
                                if (noteText.isEmpty()) {
                                    Text("Tulis catatan (judul tagihan)", fontSize = 15.sp, color = TextSubtle)
                                }
                                inner()
                            }
                        )
                    }
                    FormDivider()

                    // 5. Tambah logo
                    RecurringFormRow(
                        icon = {
                            if (logoName.isNotBlank()) {
                                InstitutionLogoBadge(walletName = logoName, iconName = logoName, size = 24.dp)
                            } else {
                                Icon(Icons.Default.Image, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                            }
                        },
                        label = "Tambah logo",
                        value = logoName.ifBlank { "Opsional" },
                        onClick = { showLogoPickerSheet = true },
                        testTag = "row_recurring_logo"
                    )
                    FormDivider()

                    // 6. Pengulangan
                    Box {
                        RecurringFormRow(
                            icon = {
                                Icon(Icons.Default.Repeat, null, tint = AccentBlueBright, modifier = Modifier.size(20.dp))
                            },
                            label = "Pengulangan",
                            value = frequency,
                            onClick = { showFrequencyMenu = true },
                            testTag = "row_recurring_frequency"
                        )
                        DropdownMenu(
                            expanded = showFrequencyMenu,
                            onDismissRequest = { showFrequencyMenu = false },
                            modifier = Modifier.background(DarkSurfaceElevated)
                        ) {
                            listOf("Setiap hari", "Setiap minggu", "Setiap 2 minggu", "Setiap bulan", "Setiap tahun").forEach { freq ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = freq,
                                            color = if (frequency == freq) AccentBlueBright else TextWhite,
                                            fontWeight = if (frequency == freq) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        frequency = freq
                                        showFrequencyMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryPicker) {
        CategoryPickerSubDialog(
            currentCategory = selectedCategory,
            currentSubcategory = selectedSubcategory,
            categories = allCategories,
            onCategorySelected = { cat ->
                selectedCategory = cat
                selectedSubcategory = ""
                selectedType = resolveCategoryType(cat, allCategories)
                if (selectedType == TransactionType.TRANSFER &&
                    (selectedToWallet.isBlank() || selectedToWallet.equals(selectedWallet, ignoreCase = true))
                ) {
                    selectedToWallet = wallets.firstOrNull { !it.name.equals(selectedWallet, ignoreCase = true) }?.name ?: ""
                }
                errorMessage = null
                isCategoryChosen = true
                showCategoryPicker = false
            },
            onCategoryAndSubcategorySelected = { cat, subcat ->
                selectedCategory = cat
                selectedSubcategory = subcat
                selectedType = resolveCategoryType(cat, allCategories)
                if (selectedType == TransactionType.TRANSFER &&
                    (selectedToWallet.isBlank() || selectedToWallet.equals(selectedWallet, ignoreCase = true))
                ) {
                    selectedToWallet = wallets.firstOrNull { !it.name.equals(selectedWallet, ignoreCase = true) }?.name ?: ""
                }
                errorMessage = null
                isCategoryChosen = true
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }

    if (showAccountGridPicker) {
        SelectFromAccountGridDialog(
            wallets = wallets,
            selectedWalletName = selectedWallet,
            onSelectWallet = {
                selectedWallet = it.name
                if (selectedToWallet.equals(it.name, ignoreCase = true)) {
                    selectedToWallet = wallets.firstOrNull { w -> !w.name.equals(it.name, ignoreCase = true) }?.name ?: ""
                }
                errorMessage = null
                showAccountGridPicker = false
            },
            onDismiss = { showAccountGridPicker = false },
            title = if (selectedType == TransactionType.TRANSFER) "Pilih Akun Asal" else "Pilih Akun"
        )
    }

    if (showToAccountGridPicker) {
        SelectFromAccountGridDialog(
            wallets = wallets,
            selectedWalletName = selectedToWallet,
            onSelectWallet = {
                selectedToWallet = it.name
                errorMessage = null
                showToAccountGridPicker = false
            },
            onDismiss = { showToAccountGridPicker = false },
            title = "Pilih Dompet / Akun Tujuan",
            excludedWalletName = selectedWallet
        )
    }

    if (showStartDatePicker) {
        val parts = startDateText.split("/")
        val initD = parts.getOrNull(0)?.toIntOrNull() ?: todayD
        val initM = parts.getOrNull(1)?.toIntOrNull() ?: todayM
        val initY = parts.getOrNull(2)?.toIntOrNull() ?: todayY
        DatePickerSubDialog(
            initialDay = initD,
            initialMonth = initM,
            initialYear = initY,
            onDateSelected = { d, m, y, _, _ ->
                startDateText = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m, y)
                selectedDay = d
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndConditionSheet) {
        EndConditionBottomSheet(
            currentCondition = endCondition,
            currentEndDate = endDateText,
            currentCount = endAfterCount,
            startDate = startDateText,
            frequency = frequency,
            onPickEndDate = { showEndDatePicker = true },
            onSave = { cond, dateStr, count ->
                endCondition = cond
                endDateText = dateStr
                endAfterCount = count
                showEndConditionSheet = false
            },
            onDismiss = { showEndConditionSheet = false }
        )
    }

    if (showEndDatePicker) {
        val parts = endDateText.ifBlank { startDateText }.split("/")
        val initD = parts.getOrNull(0)?.toIntOrNull() ?: todayD
        val initM = parts.getOrNull(1)?.toIntOrNull() ?: todayM
        val initY = parts.getOrNull(2)?.toIntOrNull() ?: (todayY + 1)
        DatePickerSubDialog(
            initialDay = initD,
            initialMonth = initM,
            initialYear = initY,
            onDateSelected = { d, m, y, _, _ ->
                endDateText = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m, y)
                endCondition = "ON_DATE"
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    if (showLogoPickerSheet) {
        BillLogoPickerBottomSheet(
            currentLogo = logoName,
            onSelectLogo = { chosen ->
                logoName = chosen
                showLogoPickerSheet = false
            },
            onDismiss = { showLogoPickerSheet = false }
        )
    }

    if (showDeleteConfirm && initialItem != null) {
        DeleteRecurringConfirmDialog(
            title = initialItem.title,
            recordedCount = 0,
            onConfirm = {
                viewModel.deleteRecurringTransaction(initialItem.id)
                showDeleteConfirm = false
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

private fun resolveCategoryType(catName: String, categories: List<com.example.data.model.MainCategory>): TransactionType {
    if (catName.equals("Transfer", true) || catName.equals("Asset", true) || catName.equals("Pembayaran Utang", true)) {
        return TransactionType.TRANSFER
    }
    categories.firstOrNull { it.name.equals(catName, ignoreCase = true) }?.let { return it.type }
    return if (catName in listOf("Salary", "Extra Income", "Bonus", "Investasi", "Pemberian")) {
        TransactionType.INCOME
    } else TransactionType.EXPENSE
}

private fun formatDateForDisplay(slash: String): String {
    val parts = slash.split("/")
    if (parts.size != 3) return slash
    val d = parts[0].toIntOrNull() ?: return slash
    val m = parts[1].toIntOrNull() ?: return slash
    val y = parts[2].toIntOrNull() ?: return slash
    val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    return "$d ${months.getOrElse(m - 1) { "Nov" }} $y"
}

private fun computeProjectedEndDate(startDate: String, frequency: String, count: Int): String {
    val parts = startDate.split("/")
    val cal = Calendar.getInstance()
    if (parts.size == 3) {
        val d = parts[0].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
        val m = (parts[1].toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
        val y = parts[2].toIntOrNull() ?: cal.get(Calendar.YEAR)
        cal.set(y, m, d)
    }
    val steps = (count - 1).coerceAtLeast(0)
    when {
        frequency.contains("hari", true) -> cal.add(Calendar.DAY_OF_MONTH, steps)
        frequency.contains("2 minggu", true) -> cal.add(Calendar.WEEK_OF_YEAR, steps * 2)
        frequency.contains("minggu", true) -> cal.add(Calendar.WEEK_OF_YEAR, steps)
        frequency.contains("tahun", true) -> cal.add(Calendar.YEAR, steps)
        else -> cal.add(Calendar.MONTH, steps)
    }
    return String.format(
        Locale.getDefault(),
        "%02d/%02d/%04d",
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.YEAR)
    )
}

@Composable
private fun RecurringFormRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, fontSize = 15.sp, color = TextWhite)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 13.5.sp, color = TextMuted)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextSubtle, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FormDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.6.dp,
        color = DarkCardBorder.copy(alpha = 0.45f)
    )
}

@Composable
fun SelectFromAccountGridDialog(
    wallets: List<AccountWallet>,
    selectedWalletName: String,
    onSelectWallet: (AccountWallet) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Pilih Akun",
    excludedWalletName: String? = null
) {
    val displayedWallets = remember(wallets, excludedWalletName) {
        if (excludedWalletName.isNullOrBlank()) wallets
        else wallets.filterNot { it.name.equals(excludedWalletName, ignoreCase = true) }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, "Tutup", tint = TextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (displayedWallets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada dompet atau akun lain.\nTambahkan akun baru di menu Akun terlebih dahulu.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        items(displayedWallets, key = { it.id }) { wallet ->
                            val isSelected = wallet.name.equals(selectedWalletName, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) AccentBlue.copy(alpha = 0.2f) else DarkSurfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) AccentBlue else DarkCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectWallet(wallet) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    InstitutionLogoBadge(
                                        walletName = wallet.name,
                                        iconName = wallet.iconName,
                                        size = 30.dp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = wallet.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = CurrencyHelper.formatRupiah(wallet.balance),
                                        fontSize = 11.5.sp,
                                        color = if (isSelected) AccentBlueBright else TextMuted,
                                        maxLines = 1
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndConditionBottomSheet(
    currentCondition: String,
    currentEndDate: String,
    currentCount: Int,
    startDate: String,
    frequency: String,
    onPickEndDate: () -> Unit,
    onSave: (condition: String, endDate: String, count: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var condition by remember(currentCondition) { mutableStateOf(currentCondition) }
    var count by remember(currentCount) { mutableIntStateOf(currentCount.coerceAtLeast(1)) }

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
            Text("Kapan berakhirnya?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(14.dp))

            val options = listOf(
                Triple("NEVER", "Tidak pernah", "Berulang terus tanpa tanggal akhir"),
                Triple("ON_DATE", "Pada tanggal", if (currentEndDate.isNotBlank()) formatDateForDisplay(currentEndDate) else "Pilih tanggal akhir di kalender"),
                Triple("AFTER_COUNT", "Setelah beberapa kali", "Otomatis berhenti setelah jumlah siklus tertentu")
            )

            options.forEach { (key, title, sub) ->
                val isSelected = condition == key
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) AccentBlue.copy(alpha = 0.16f) else DarkSurfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) AccentBlue else DarkCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            condition = key
                            if (key == "ON_DATE") onPickEndDate()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                            Text(sub, fontSize = 11.5.sp, color = TextMuted)
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AccentBlue else Color.Transparent)
                                .border(1.5.dp, if (isSelected) AccentBlue else TextSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            if (condition == "AFTER_COUNT") {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = DarkSurface,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { if (count > 1) count-- }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Remove, "Kurangi", tint = TextWhite, modifier = Modifier.size(18.dp))
                                }
                            }

                            Text(
                                text = "$count kali",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )

                            Surface(
                                shape = CircleShape,
                                color = DarkSurface,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { count++ }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, "Tambah", tint = TextWhite, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 10, 20).forEach { preset ->
                                val isMatch = count == preset
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isMatch) AccentBlue else DarkSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { count = preset }
                                ) {
                                    Text(
                                        text = "${preset}×",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMatch) Color.White else TextMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        val projected = computeProjectedEndDate(startDate, frequency, count)
                        Text(
                            text = "Berakhir pada $projected",
                            fontSize = 12.sp,
                            color = AccentBlueBright,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSave(condition, currentEndDate, count) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Selesai", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillLogoPickerBottomSheet(
    currentLogo: String,
    onSelectLogo: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeTab by remember { mutableStateOf("Langganan") }
    var activeFilter by remember { mutableStateOf("Semua") }
    var picked by remember(currentLogo) { mutableStateOf(currentLogo) }

    val filterChips = if (activeTab == "Langganan") {
        listOf("Semua", "AI", "EDUCATION", "FITNESS", "GAMING")
    } else {
        listOf("Semua", "BANK", "E-WALLET")
    }
    val filteredLogos = remember(activeTab, activeFilter) {
        BillLogoCatalog.filter {
            it.tab == activeTab && (activeFilter == "Semua" || it.categoryTag == activeFilter)
        }
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
                .padding(bottom = 26.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pilih Logo Tagihan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                if (picked.isNotBlank()) {
                    Text(
                        text = "Hapus logo",
                        fontSize = 12.sp,
                        color = ExpenseRed,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            picked = ""
                            onSelectLogo("")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs: Langganan | Akun
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf("Langganan", "Akun").forEach { tab ->
                        val isSel = activeTab == tab
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (isSel) AccentBlue else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable {
                                    activeTab = tab
                                    activeFilter = "Semua"
                                }
                        ) {
                            Text(
                                text = tab,
                                fontSize = 13.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filterChips.forEach { chip ->
                    val isSel = activeFilter == chip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) AccentBlue.copy(alpha = 0.2f) else DarkSurfaceElevated,
                        border = BorderStroke(1.dp, if (isSel) AccentBlue else DarkCardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { activeFilter = chip }
                    ) {
                        Text(
                            text = chip,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) AccentBlueBright else TextMuted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                items(filteredLogos, key = { it.name }) { entry ->
                    val isSel = picked.equals(entry.name, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSel) AccentBlue.copy(alpha = 0.18f) else DarkSurfaceElevated,
                        border = BorderStroke(1.dp, if (isSel) AccentBlue else DarkCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { picked = entry.name }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(entry.colorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = entry.name.take(2).uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = entry.name,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { onSelectLogo(picked) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Pilih Ikon", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
