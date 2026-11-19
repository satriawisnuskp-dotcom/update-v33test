package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetCategory
import com.example.data.model.TransactionType
import com.example.ui.dialogs.PilihAkunBudgetSheet
import com.example.ui.dialogs.PilihFrekuensiBudgetSheet
import com.example.ui.dialogs.PilihKategoriSubkategoriSheet
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CategoryIconHelper
import com.example.util.ThousandsSeparatorVisualTransformation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditBudgetScreen(
    viewModel: FinanceViewModel,
    initialFrequency: String = "Bulanan",
    editingBudget: BudgetCategory? = null,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val expenseCategories = remember(categories) {
        categories.filter { it.type == TransactionType.EXPENSE && it.isVisible }
    }

    var amountText by remember(editingBudget) {
        mutableStateOf(
            if (editingBudget != null && editingBudget.allocated > 0) {
                editingBudget.allocated.toLong().toString()
            } else ""
        )
    }
    var budgetName by remember(editingBudget) {
        mutableStateOf(editingBudget?.name ?: "")
    }
    var selectedCategories by remember(editingBudget) {
        mutableStateOf(
            editingBudget?.targetCategories ?: listOf(expenseCategories.firstOrNull()?.name ?: "Food & Drink")
        )
    }
    var selectedAccounts by remember(editingBudget) {
        mutableStateOf(editingBudget?.accountNames ?: emptyList())
    }
    var selectedFrequency by remember(editingBudget, initialFrequency) {
        mutableStateOf(editingBudget?.frequency ?: initialFrequency)
    }

    val defaultTodayStr = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }
    var customStartDate by remember(editingBudget) {
        mutableStateOf(editingBudget?.customStartDate?.ifBlank { defaultTodayStr } ?: defaultTodayStr)
    }
    var customEndDate by remember(editingBudget) {
        mutableStateOf(editingBudget?.customEndDate?.ifBlank { defaultTodayStr } ?: defaultTodayStr)
    }

    var showAccountSheet by remember { mutableStateOf(false) }
    var showFrequencySheet by remember { mutableStateOf(false) }
    var showAllCategoriesSheet by remember { mutableStateOf(false) }

    val quickChipCategories = remember(expenseCategories, selectedCategories) {
        val baseNames = expenseCategories.map { it.name }
        (baseNames + selectedCategories).distinct().take(10)
    }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            Toast.makeText(context, "Masukkan jumlah anggaran terlebih dahulu", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (selectedCategories.isEmpty()) {
                            Toast.makeText(context, "Pilih minimal satu kategori", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val primaryCat = selectedCategories.first()
                        val resolvedName = budgetName.trim().ifBlank {
                            if (selectedCategories.size == 1) primaryCat else selectedCategories.take(2).joinToString(", ")
                        }
                        val newBudget = BudgetCategory(
                            id = editingBudget?.id ?: java.util.UUID.randomUUID().toString(),
                            category = primaryCat,
                            allocated = amount,
                            spent = editingBudget?.spent ?: 0.0,
                            name = resolvedName,
                            categories = selectedCategories,
                            accountNames = selectedAccounts,
                            frequency = selectedFrequency,
                            customStartDate = if (selectedFrequency == "Rentang khusus") customStartDate else "",
                            customEndDate = if (selectedFrequency == "Rentang khusus") customEndDate else ""
                        )
                        if (editingBudget == null) {
                            viewModel.addBudget(newBudget)
                        } else {
                            viewModel.updateBudget(newBudget)
                        }
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_save_budget"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text(
                        text = if (editingBudget == null) "Buat Anggaran Baru" else "Simpan Perubahan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextWhite
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (editingBudget == null) "Tambah Anggaran Baru" else "Edit Anggaran",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Jumlah Anggaran
                Column {
                    Text(
                        text = "Jumlah Anggaran",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Rp",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (amountText.isEmpty()) {
                                        Text(
                                            text = "0",
                                            fontSize = 16.sp,
                                            color = TextSubtle
                                        )
                                    }
                                    BasicTextField(
                                        value = amountText,
                                        onValueChange = { input ->
                                            val digits = input.filter { it.isDigit() }.take(13)
                                            amountText = digits
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = TextWhite,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        cursorBrush = SolidColor(AccentBlue),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        visualTransformation = ThousandsSeparatorVisualTransformation('.'),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_budget_amount")
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceElevated,
                                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = settings.primaryCurrency,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Nama Anggaran
                Column {
                    Text(
                        text = "Nama Anggaran",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = budgetName,
                        onValueChange = { budgetName = it },
                        placeholder = {
                            Text(
                                text = "mis. Anggaran Belanja",
                                color = TextSubtle,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkCardBorder.copy(alpha = 0.5f),
                            cursorColor = AccentBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_budget_name")
                    )
                }

                // 3. Anggaran Untuk (Kategori Chips Card)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Anggaran Untuk",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            quickChipCategories.forEach { catName ->
                                val isSelected = selectedCategories.any { it.equals(catName, ignoreCase = true) }
                                val catColor = CategoryIconHelper.getCategoryColor(catName, TransactionType.EXPENSE)
                                val catIconKey = expenseCategories.find { it.name.equals(catName, ignoreCase = true) }?.iconName ?: ""

                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = if (isSelected) AccentBlue.copy(alpha = 0.16f) else DarkSurfaceElevated,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) AccentBlue else DarkCardBorder.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable {
                                            val updated = if (isSelected) {
                                                if (selectedCategories.size > 1) {
                                                    selectedCategories.filterNot { it.equals(catName, ignoreCase = true) }
                                                } else selectedCategories
                                            } else {
                                                selectedCategories + catName
                                            }
                                            selectedCategories = updated
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(catColor.copy(alpha = 0.22f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getCategoryIcon(catName, catIconKey),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = catName,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) TextWhite else TextWhite.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showAllCategoriesSheet = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Semua kategori dan subkategori",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentBlueBright
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = AccentBlueBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 4. Akun & Frekuensi Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Akun row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAccountSheet = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("row_budget_account"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AccentBlue.copy(alpha = 0.14f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = AccentBlueBright,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Akun",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val accSummary = when {
                                    selectedAccounts.isEmpty() -> "Semua Akun"
                                    selectedAccounts.size == 1 -> selectedAccounts.first()
                                    else -> "${selectedAccounts.size} Akun"
                                }
                                Text(
                                    text = accSummary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = DarkCardBorder.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Frekuensi row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFrequencySheet = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("row_budget_frequency"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AccentBlue.copy(alpha = 0.14f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Repeat,
                                            contentDescription = null,
                                            tint = AccentBlueBright,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Frekuensi",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedFrequency,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (selectedFrequency == "Rentang khusus") {
                            HorizontalDivider(
                                color = DarkCardBorder.copy(alpha = 0.35f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = customStartDate,
                                    onValueChange = { customStartDate = it },
                                    label = { Text("Mulai (DD/MM/YYYY)", fontSize = 10.sp, color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedBorderColor = AccentBlue,
                                        unfocusedBorderColor = DarkCardBorder
                                    )
                                )
                                OutlinedTextField(
                                    value = customEndDate,
                                    onValueChange = { customEndDate = it },
                                    label = { Text("Sampai (DD/MM/YYYY)", fontSize = 10.sp, color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedBorderColor = AccentBlue,
                                        unfocusedBorderColor = DarkCardBorder
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAccountSheet) {
        PilihAkunBudgetSheet(
            wallets = wallets,
            initialSelectedAccounts = selectedAccounts,
            onConfirm = { accs ->
                selectedAccounts = accs
                showAccountSheet = false
            },
            onDismiss = { showAccountSheet = false }
        )
    }

    if (showFrequencySheet) {
        PilihFrekuensiBudgetSheet(
            selectedFrequency = selectedFrequency,
            onSelect = { freq ->
                selectedFrequency = freq
                showFrequencySheet = false
            },
            onDismiss = { showFrequencySheet = false }
        )
    }

    if (showAllCategoriesSheet) {
        PilihKategoriSubkategoriSheet(
            categories = categories,
            selectedCategories = selectedCategories,
            onConfirm = { cats ->
                if (cats.isNotEmpty()) {
                    selectedCategories = cats
                }
                showAllCategoriesSheet = false
            },
            onDismiss = { showAllCategoriesSheet = false }
        )
    }
}
