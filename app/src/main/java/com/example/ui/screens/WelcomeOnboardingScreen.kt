package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.OnboardingBillInput
import com.example.data.model.OnboardingBudgetInput
import com.example.ui.components.CompletionCheckmarkBadge
import com.example.ui.components.Step3BillsIllustration
import com.example.ui.components.Step4PieChartIllustration
import com.example.ui.components.Step5CoinJarIllustration
import com.example.ui.components.WelcomeHeroIllustration
import com.example.ui.dialogs.BackupRestoreDialog
import com.example.ui.dialogs.OnboardingAccountIconPickerSheet
import com.example.ui.dialogs.OnboardingCurrencyPickerSheet
import com.example.ui.dialogs.OnboardingCustomBillDialog
import com.example.ui.dialogs.OnboardingDueDayDialog
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CurrencyDataHelper
import com.example.util.InstitutionLogoBadge
import com.example.util.ThousandsSeparatorVisualTransformation
import java.util.Calendar

private val OnboardingBgTop = Color(0xFF0D172E)
private val OnboardingBgMid = Color(0xFF080E1C)
private val OnboardingBgBottom = Color(0xFF060913)
private val OnboardingCardBg = Color(0xFF10182B)
private val OnboardingCardAltBg = Color(0xFF131D33)
private val OnboardingCardBorder = Color(0xFF1F2B47)
private val OnboardingBlue = Color(0xFF2D6BFF)
private val OnboardingBlueBright = Color(0xFF60A5FA)
private val OnboardingTextPrimary = Color(0xFFF8FAFC)
private val OnboardingTextMuted = Color(0xFF94A3B8)
private val OnboardingGreen = Color(0xFF10B981)

private data class BillPreset(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val defaultDay: Int = 1,
    val category: String = "Bills & Fees"
)

private data class BudgetPreset(
    val categoryName: String,
    val matchCategories: List<String>,
    val icon: ImageVector,
    val iconTint: Color,
    val suggestedPercent: Double
)

private val DefaultBillPresets = listOf(
    BillPreset("sewa", "Sewa", Icons.Default.Home, Color(0xFF3B82F6), 1),
    BillPreset("listrik", "Listrik", Icons.Default.Bolt, Color(0xFF14B8A6), 1),
    BillPreset("air", "Air", Icons.Default.WaterDrop, Color(0xFF06B6D4), 1),
    BillPreset("internet", "Internet", Icons.Default.Wifi, Color(0xFF10B981), 1),
    BillPreset("ponsel", "Paket ponsel", Icons.Default.Smartphone, Color(0xFFA855F7), 1),
    BillPreset("streaming", "Streaming", Icons.Default.PlayArrow, Color(0xFFF97316), 1),
    BillPreset("asuransi", "Asuransi", Icons.Default.Shield, Color(0xFFEAB308), 1),
    BillPreset("gym", "Gym", Icons.Default.FitnessCenter, Color(0xFFF43F5E), 1),
    BillPreset("sekolah", "Biaya sekolah", Icons.Default.School, Color(0xFF84CC16), 1)
)

private val DefaultBudgetPresets = listOf(
    BudgetPreset("Food & Drink", listOf("Food & Drink", "Food & Dining"), Icons.Default.Restaurant, Color(0xFFF97316), 0.25),
    BudgetPreset("Transport", listOf("Transport"), Icons.Default.DirectionsBus, Color(0xFF3B82F6), 0.12),
    BudgetPreset("Groceries", listOf("Shopping", "Groceries", "Market"), Icons.Default.ShoppingCart, Color(0xFF14B8A6), 0.15),
    BudgetPreset("Shopping", listOf("Shopping"), Icons.Default.ShoppingBag, Color(0xFFA855F7), 0.10),
    BudgetPreset("Entertainment", listOf("Entertainment"), Icons.Default.SportsEsports, Color(0xFF22C55E), 0.08)
)

@Composable
fun WelcomeOnboardingScreen(
    viewModel: FinanceViewModel
) {
    val context = LocalContext.current

    // Steps: 0 = Welcome, 1..5 = Setup Steps, 6 = Completion Summary
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }

    // Step 1 state (Account & Currency)
    var selectedCurrencyCode by rememberSaveable { mutableStateOf<String?>(null) }
    var accountType by rememberSaveable { mutableStateOf("Tunai") }
    var hasSelectedAccountType by rememberSaveable { mutableStateOf(false) }
    var accountName by rememberSaveable { mutableStateOf("Tunai") }
    var hasEditedAccountName by rememberSaveable { mutableStateOf(false) }
    var accountIconName by rememberSaveable { mutableStateOf("cash") }
    var hasPickedCustomIcon by rememberSaveable { mutableStateOf(false) }
    var initialBalanceText by rememberSaveable { mutableStateOf("") }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showIconPickerSheet by remember { mutableStateOf(false) }

    // Step 2 state (Payday & Monthly Income)
    var paydayDay by rememberSaveable { mutableIntStateOf(1) } // 1..30, or 31 for last day
    var paydaySkipped by rememberSaveable { mutableStateOf(false) }
    var monthlyIncomeText by rememberSaveable { mutableStateOf("") }

    // Step 3 state (Monthly Recurring Bills)
    val selectedBillIds = remember { mutableStateListOf<String>() }
    val customBillPresets = remember { mutableStateListOf<BillPreset>() }
    val billAmountMap = remember { mutableStateMapOf<String, String>() }
    val billDayMap = remember { mutableStateMapOf<String, Int>() }
    var billReminderEnabled by rememberSaveable { mutableStateOf(false) }
    var billsSkipped by rememberSaveable { mutableStateOf(false) }
    var editingDueDayForBill by remember { mutableStateOf<BillPreset?>(null) }
    var showCustomBillDialog by remember { mutableStateOf(false) }

    // Step 4 state (Monthly Budgets)
    val enabledBudgets = remember { mutableStateMapOf<String, Boolean>() }
    val budgetAmountMap = remember { mutableStateMapOf<String, String>() }
    var budgetsSkipped by rememberSaveable { mutableStateOf(false) }

    // Step 5 state (Monthly Plan)
    var planCreated by rememberSaveable { mutableStateOf(true) }

    val activeCurrency = CurrencyDataHelper.findByCode(selectedCurrencyCode ?: "IDR")
    val currencySymbol = activeCurrency.symbol
    val initialBalance = initialBalanceText.toDoubleOrNull() ?: 0.0
    val monthlyIncome = monthlyIncomeText.toDoubleOrNull() ?: 0.0

    val allBillPresets = remember(customBillPresets.size) {
        DefaultBillPresets + customBillPresets
    }

    val activeBills = remember(selectedBillIds.toList(), billAmountMap.toMap(), billDayMap.toMap(), billReminderEnabled, allBillPresets) {
        selectedBillIds.mapNotNull { id ->
            val preset = allBillPresets.firstOrNull { it.id == id } ?: return@mapNotNull null
            OnboardingBillInput(
                id = preset.id,
                title = preset.title,
                amount = billAmountMap[id]?.toDoubleOrNull() ?: 0.0,
                dayOfMonth = billDayMap[id] ?: preset.defaultDay,
                reminderEnabled = billReminderEnabled,
                category = preset.category
            )
        }
    }

    val activeBudgets = remember(enabledBudgets.toMap(), budgetAmountMap.toMap()) {
        DefaultBudgetPresets.mapNotNull { preset ->
            val isEnabled = enabledBudgets[preset.categoryName] == true
            val amt = budgetAmountMap[preset.categoryName]?.toDoubleOrNull() ?: 0.0
            if (isEnabled && amt > 0.0) {
                OnboardingBudgetInput(
                    categoryName = preset.categoryName,
                    matchCategories = preset.matchCategories,
                    amount = amt
                )
            } else {
                null
            }
        }
    }

    val totalBillsAmount = activeBills.sumOf { it.amount }
    val totalBudgetsAmount = activeBudgets.sumOf { it.amount }
    val cycleLabel = remember(paydayDay) { computeCycleRangeLabel(paydayDay) }

    fun finalizeOnboarding() {
        val code = selectedCurrencyCode ?: "IDR"
        viewModel.completeOnboarding(
            currencyCode = code,
            accountName = accountName,
            accountType = accountType,
            accountIconName = accountIconName,
            initialBalance = initialBalance,
            paydayDay = paydayDay,
            monthlyIncome = monthlyIncome,
            bills = if (billsSkipped) emptyList() else activeBills,
            budgets = if (budgetsSkipped) emptyList() else activeBudgets,
            createMonthlyPlan = planCreated
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        finalizeOnboarding()
    }

    BackHandler(enabled = currentStep > 0) {
        currentStep = (currentStep - 1).coerceAtLeast(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(OnboardingBgTop, OnboardingBgMid, OnboardingBgBottom)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
        ) {
            // Top Progress Header on Steps 1..6
            if (currentStep in 1..6) {
                OnboardingTopProgressBar(
                    currentStep = currentStep,
                    showSkip = currentStep in 2..5,
                    onBack = { currentStep = (currentStep - 1).coerceAtLeast(0) },
                    onSkip = {
                        when (currentStep) {
                            2 -> {
                                paydaySkipped = true
                                paydayDay = 1
                                monthlyIncomeText = ""
                                currentStep = 3
                            }
                            3 -> {
                                billsSkipped = true
                                selectedBillIds.clear()
                                currentStep = 4
                            }
                            4 -> {
                                budgetsSkipped = true
                                enabledBudgets.clear()
                                currentStep = 5
                            }
                            5 -> {
                                planCreated = false
                                currentStep = 6
                            }
                        }
                    }
                )
            }

            // Main Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                },
                label = "onboarding_step_transition",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    0 -> WelcomeHeroStep(
                        onStartClick = { currentStep = 1 },
                        onRestoreClick = { showBackupRestoreDialog = true }
                    )

                    1 -> Step1AccountSetup(
                        selectedCurrencyCode = selectedCurrencyCode,
                        accountType = accountType,
                        showAccountDetails = selectedCurrencyCode != null || hasSelectedAccountType,
                        accountName = accountName,
                        accountIconName = accountIconName,
                        initialBalanceText = initialBalanceText,
                        onOpenCurrencyPicker = { showCurrencySheet = true },
                        onSelectAccountType = { type ->
                            hasSelectedAccountType = true
                            accountType = type
                            if (!hasEditedAccountName || accountName in listOf("Tunai", "Bank", "Dompet digital")) {
                                accountName = type
                            }
                            if (!hasPickedCustomIcon) {
                                accountIconName = when (type) {
                                    "Bank" -> "Bank"
                                    "Dompet digital" -> "Dompet digital"
                                    else -> "cash"
                                }
                            }
                        },
                        onAccountNameChange = {
                            accountName = it
                            hasEditedAccountName = true
                        },
                        onOpenIconPicker = { showIconPickerSheet = true },
                        onInitialBalanceChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 13) initialBalanceText = digits
                        },
                        onNextClick = {
                            if (selectedCurrencyCode == null) {
                                showCurrencySheet = true
                            } else {
                                currentStep = 2
                            }
                        }
                    )

                    2 -> Step2PaydayIncome(
                        paydayDay = paydayDay,
                        cycleLabel = cycleLabel,
                        currencySymbol = currencySymbol,
                        monthlyIncomeText = monthlyIncomeText,
                        onSelectDay = { day ->
                            paydayDay = day
                            paydaySkipped = false
                        },
                        onIncomeChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 13) {
                                monthlyIncomeText = digits
                                paydaySkipped = false
                            }
                        },
                        onNextClick = {
                            paydaySkipped = false
                            currentStep = 3
                        }
                    )

                    3 -> Step3BillsSetup(
                        presets = allBillPresets,
                        selectedBillIds = selectedBillIds,
                        billAmountMap = billAmountMap,
                        billDayMap = billDayMap,
                        billReminderEnabled = billReminderEnabled,
                        currencySymbol = currencySymbol,
                        onToggleBill = { preset ->
                            billsSkipped = false
                            if (preset.id in selectedBillIds) {
                                selectedBillIds.remove(preset.id)
                            } else {
                                selectedBillIds.add(preset.id)
                                if (!billDayMap.containsKey(preset.id)) {
                                    billDayMap[preset.id] = preset.defaultDay
                                }
                            }
                        },
                        onAddOtherClick = { showCustomBillDialog = true },
                        onAmountChange = { billId, input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 12) billAmountMap[billId] = digits
                        },
                        onDueDateClick = { preset -> editingDueDayForBill = preset },
                        onToggleReminder = { billReminderEnabled = it },
                        onNextClick = {
                            billsSkipped = selectedBillIds.isEmpty()
                            currentStep = 4
                        }
                    )

                    4 -> Step4BudgetsSetup(
                        presets = DefaultBudgetPresets,
                        hasBills = selectedBillIds.isNotEmpty() && !billsSkipped,
                        monthlyIncome = monthlyIncome,
                        currencyCode = activeCurrency.code,
                        currencySymbol = currencySymbol,
                        enabledBudgets = enabledBudgets,
                        budgetAmountMap = budgetAmountMap,
                        totalBudgetsAmount = totalBudgetsAmount,
                        onGoToIncome = { currentStep = 2 },
                        onAutoFillFromIncome = {
                            val remainingBase = (monthlyIncome - totalBillsAmount).coerceAtLeast(monthlyIncome * 0.5)
                            DefaultBudgetPresets.forEach { preset ->
                                val suggested = ((remainingBase * preset.suggestedPercent) / 10000.0).toLong() * 10000L
                                if (suggested > 0) {
                                    enabledBudgets[preset.categoryName] = true
                                    budgetAmountMap[preset.categoryName] = suggested.toString()
                                }
                            }
                        },
                        onToggleCategory = { name ->
                            val next = !(enabledBudgets[name] ?: false)
                            enabledBudgets[name] = next
                            budgetsSkipped = false
                        },
                        onBudgetAmountChange = { name, input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 12) {
                                budgetAmountMap[name] = digits
                                if (digits.isNotEmpty() && (digits.toLongOrNull() ?: 0L) > 0L) {
                                    enabledBudgets[name] = true
                                    budgetsSkipped = false
                                }
                            }
                        },
                        onNextClick = {
                            budgetsSkipped = activeBudgets.isEmpty()
                            currentStep = 5
                        }
                    )

                    5 -> Step5MonthlyPlanPreview(
                        cycleLabel = cycleLabel,
                        currencyCode = activeCurrency.code,
                        monthlyIncome = monthlyIncome,
                        billCount = if (billsSkipped) 0 else selectedBillIds.size,
                        totalBills = totalBillsAmount,
                        budgetCount = if (budgetsSkipped) 0 else activeBudgets.size,
                        totalBudgets = totalBudgetsAmount,
                        onGoToIncome = { currentStep = 2 },
                        onCompleteSetupClick = {
                            planCreated = true
                            currentStep = 6
                        }
                    )

                    else -> Step6CompletionSummary(
                        accountSummary = "${accountName.ifBlank { accountType }} · ${CurrencyDataHelper.formatAmount(initialBalance, activeCurrency.code)}",
                        paydaySummary = when {
                            paydaySkipped && monthlyIncome <= 0 -> "Dilewati"
                            paydayDay == 31 -> "Hari terakhir bulan"
                            else -> "Setiap tanggal $paydayDay"
                        },
                        billsSummary = if (billsSkipped || activeBills.isEmpty()) {
                            "Dilewati"
                        } else {
                            "${activeBills.size} tagihan · ${CurrencyDataHelper.formatAmount(totalBillsAmount, activeCurrency.code)}"
                        },
                        budgetsSummary = if (budgetsSkipped || activeBudgets.isEmpty()) {
                            "Dilewati"
                        } else {
                            "${activeBudgets.size} kategori · ${CurrencyDataHelper.formatAmount(totalBudgetsAmount, activeCurrency.code)}"
                        },
                        planSummary = if (!planCreated) "Tidak dibuat" else "Dibuat",
                        onFinishClick = {
                            val needsNotificationPermission = billReminderEnabled &&
                                activeBills.isNotEmpty() &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED

                            if (needsNotificationPermission) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                finalizeOnboarding()
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal Sheets & Dialogs
    if (showCurrencySheet) {
        OnboardingCurrencyPickerSheet(
            selectedCurrencyCode = selectedCurrencyCode,
            onSelectCurrency = { option ->
                selectedCurrencyCode = option.code
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false }
        )
    }

    if (showIconPickerSheet) {
        OnboardingAccountIconPickerSheet(
            currentIconName = accountIconName,
            accountName = accountName,
            onSelectIcon = { chosen ->
                accountIconName = chosen
                hasPickedCustomIcon = true
                showIconPickerSheet = false
            },
            onDismiss = { showIconPickerSheet = false }
        )
    }

    editingDueDayForBill?.let { targetBill ->
        val currentDay = billDayMap[targetBill.id] ?: targetBill.defaultDay
        OnboardingDueDayDialog(
            billName = targetBill.title,
            currentDay = currentDay,
            onSelectDay = { day ->
                billDayMap[targetBill.id] = day
                editingDueDayForBill = null
            },
            onDismiss = { editingDueDayForBill = null }
        )
    }

    if (showCustomBillDialog) {
        OnboardingCustomBillDialog(
            onAddBill = { title ->
                val id = "custom_${System.currentTimeMillis()}"
                val newPreset = BillPreset(
                    id = id,
                    title = title,
                    icon = Icons.Default.ReceiptLong,
                    iconColor = Color(0xFF38BDF8),
                    defaultDay = 1
                )
                customBillPresets.add(newPreset)
                selectedBillIds.add(id)
                billDayMap[id] = 1
                billsSkipped = false
                showCustomBillDialog = false
            },
            onDismiss = { showCustomBillDialog = false }
        )
    }

    if (showBackupRestoreDialog) {
        BackupRestoreDialog(
            viewModel = viewModel,
            onDismiss = { showBackupRestoreDialog = false }
        )
    }
}

@Composable
private fun OnboardingTopProgressBar(
    currentStep: Int,
    showSkip: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(OnboardingCardBg)
                .border(1.dp, OnboardingCardBorder, CircleShape)
                .testTag("btn_onboarding_back")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = OnboardingTextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (seg in 1..6) {
                val active = seg <= currentStep
                val alpha by animateFloatAsState(
                    targetValue = if (active) 1f else 0.25f,
                    label = "seg_$seg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (active) OnboardingBlue.copy(alpha = alpha)
                            else Color(0xFF28344E)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (showSkip) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.testTag("btn_onboarding_skip")
            ) {
                Text(
                    text = "Lewati",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnboardingTextMuted
                )
            }
        } else {
            Spacer(modifier = Modifier.width(44.dp))
        }
    }
}

@Composable
private fun WelcomeHeroStep(
    onStartClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.55f))

        WelcomeHeroIllustration(modifier = Modifier.size(168.dp))

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Rencanakan bulan Anda sebelum dimulai",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 33.sp,
            color = OnboardingTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tambahkan tagihan yang sudah Anda bayar, atur beberapa batas, dan lihat sisa untuk ditabung. Hanya sekitar dua menit.",
            fontSize = 14.5.sp,
            lineHeight = 22.sp,
            color = OnboardingTextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(26.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WelcomeFeatureTile(
                label = "Akun",
                icon = Icons.Default.AccountBalanceWallet,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
            WelcomeFeatureTile(
                label = "Tagihan",
                icon = Icons.Default.ReceiptLong,
                tint = Color(0xFFA855F7),
                modifier = Modifier.weight(1f)
            )
            WelcomeFeatureTile(
                label = "Anggaran",
                icon = Icons.Default.PieChart,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            WelcomeFeatureTile(
                label = "Rencana",
                icon = Icons.Default.Savings,
                tint = Color(0xFF22D3EE),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = OnboardingTextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Pribadi & aman · data Anda tetap di perangkat",
                fontSize = 12.sp,
                color = OnboardingTextMuted
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OnboardingPrimaryButton(
            label = "Mulai",
            showArrow = true,
            onClick = onStartClick,
            testTag = "btn_onboarding_start"
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = OnboardingTextMuted)) {
                    append("Sudah terdaftar? ")
                }
                withStyle(SpanStyle(color = OnboardingBlueBright, fontWeight = FontWeight.SemiBold)) {
                    append("Pulihkan data")
                }
            },
            fontSize = 13.5.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRestoreClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("btn_onboarding_restore")
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun WelcomeFeatureTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = OnboardingCardBg,
            border = BorderStroke(1.dp, OnboardingCardBorder),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = OnboardingTextMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun Step1AccountSetup(
    selectedCurrencyCode: String?,
    accountType: String,
    showAccountDetails: Boolean,
    accountName: String,
    accountIconName: String,
    initialBalanceText: String,
    onOpenCurrencyPicker: () -> Unit,
    onSelectAccountType: (String) -> Unit,
    onAccountNameChange: (String) -> Unit,
    onOpenIconPicker: () -> Unit,
    onInitialBalanceChange: (String) -> Unit,
    onNextClick: () -> Unit
) {
    val chosenCurrency = selectedCurrencyCode?.let { CurrencyDataHelper.findByCode(it) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "LANGKAH 1 DARI 5",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = OnboardingBlueBright
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Di mana uang Anda saat ini?",
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 32.sp,
                color = OnboardingTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Mulai dengan satu akun. Akun lainnya bisa ditambahkan nanti.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = OnboardingTextMuted
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Currency Selector Dropdown
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = OnboardingCardBg,
                border = BorderStroke(
                    1.dp,
                    if (chosenCurrency != null) OnboardingBlue.copy(alpha = 0.65f) else OnboardingCardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenCurrencyPicker)
                    .testTag("btn_onboarding_currency")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (chosenCurrency != null) {
                        Text(text = chosenCurrency.flagEmoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = chosenCurrency.displayDropdownLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnboardingTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = OnboardingTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Pilih Mata Uang",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnboardingTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Pilih mata uang",
                        tint = OnboardingTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Jenis Akun",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnboardingTextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    Triple("Tunai", Icons.Default.Payments, "tunai"),
                    Triple("Bank", Icons.Default.AccountBalance, "bank"),
                    Triple("Dompet digital", Icons.Default.Smartphone, "ewallet")
                ).forEach { (label, icon, tag) ->
                    val isSelected = showAccountDetails && accountType == label
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) OnboardingBlue.copy(alpha = 0.16f) else OnboardingCardBg,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) OnboardingBlue else OnboardingCardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(82.dp)
                            .clickable { onSelectAccountType(label) }
                            .testTag("btn_account_type_$tag")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) OnboardingBlueBright else OnboardingTextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OnboardingTextPrimary else OnboardingTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showAccountDetails) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Nama Akun",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnboardingTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clickable(onClick = onOpenIconPicker)
                                .testTag("btn_onboarding_pick_account_icon"),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = OnboardingCardBg,
                                border = BorderStroke(1.dp, OnboardingCardBorder),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    InstitutionLogoBadge(
                                        walletName = accountName,
                                        iconName = accountIconName,
                                        size = 34.dp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(OnboardingBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Ubah ikon",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = OnboardingCardBg,
                            border = BorderStroke(1.dp, OnboardingBlue.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = accountName,
                                    onValueChange = onAccountNameChange,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = OnboardingTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(OnboardingBlue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_onboarding_account_name")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Saldo Awal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnboardingTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = OnboardingCardBg,
                        border = BorderStroke(1.dp, OnboardingCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chosenCurrency?.symbol ?: "Rp",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnboardingTextMuted
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = initialBalanceText,
                                onValueChange = onInitialBalanceChange,
                                singleLine = true,
                                visualTransformation = ThousandsSeparatorVisualTransformation('.'),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    color = OnboardingTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                cursorBrush = SolidColor(OnboardingBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_onboarding_initial_balance"),
                                decorationBox = { inner ->
                                    if (initialBalanceText.isEmpty()) {
                                        Text(
                                            text = "0",
                                            fontSize = 16.sp,
                                            color = OnboardingTextMuted
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = OnboardingTextMuted,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ketik saldo yang tampil di aplikasi bank Anda. Tidak ada yang terhubung ke bank Anda.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = OnboardingTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OnboardingPrimaryButton(
                label = "Berikutnya",
                showArrow = true,
                onClick = onNextClick,
                testTag = "btn_onboarding_next_1"
            )
        }
    }
}

@Composable
private fun Step2PaydayIncome(
    paydayDay: Int,
    cycleLabel: String,
    currencySymbol: String,
    monthlyIncomeText: String,
    onSelectDay: (Int) -> Unit,
    onIncomeChange: (String) -> Unit,
    onNextClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "LANGKAH 2 DARI 5",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = OnboardingBlueBright
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Kapan Anda Menerima Gaji?",
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 32.sp,
                color = OnboardingTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Laporan bulanan diriset dari hari ini.",
                fontSize = 14.sp,
                color = OnboardingTextMuted
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Payday Grid Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = OnboardingCardBg,
                border = BorderStroke(1.dp, OnboardingCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = (1..30).toList().chunked(7)
                    rows.forEachIndexed { rowIdx, week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            week.forEach { day ->
                                val isSel = paydayDay == day
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) OnboardingBlue else OnboardingCardAltBg,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clickable { onSelectDay(day) }
                                        .testTag("payday_day_$day")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = day.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else OnboardingTextPrimary
                                        )
                                    }
                                }
                            }
                            if (rowIdx == rows.lastIndex) {
                                val isLastDay = paydayDay == 31
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isLastDay) OnboardingBlue else OnboardingCardAltBg,
                                    modifier = Modifier
                                        .weight(5f)
                                        .height(40.dp)
                                        .clickable { onSelectDay(31) }
                                        .testTag("payday_last_day")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Hari terakhir bulan",
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isLastDay) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isLastDay) Color.White else OnboardingTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cycle preview pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OnboardingBlue.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, OnboardingBlue.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = OnboardingBlueBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = OnboardingTextMuted)) {
                                append("Bulan ini berjalan ")
                            }
                            withStyle(SpanStyle(color = OnboardingTextPrimary, fontWeight = FontWeight.Bold)) {
                                append(cycleLabel)
                            }
                        },
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Pendapatan Bulanan",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnboardingTextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = OnboardingCardBg,
                border = BorderStroke(
                    1.dp,
                    if (monthlyIncomeText.isNotEmpty()) OnboardingBlue else OnboardingCardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currencySymbol,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnboardingTextMuted
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = monthlyIncomeText,
                        onValueChange = onIncomeChange,
                        singleLine = true,
                        visualTransformation = ThousandsSeparatorVisualTransformation('.'),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            color = OnboardingTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(OnboardingBlue),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_onboarding_monthly_income"),
                        decorationBox = { inner ->
                            if (monthlyIncomeText.isEmpty()) {
                                Text(
                                    text = "0",
                                    fontSize = 18.sp,
                                    color = OnboardingTextMuted
                                )
                            }
                            inner()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Angka perkiraan sudah cukup. Angka ini mengisi anggaran dan rencana Anda. Kosongkan jika sering berubah.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = OnboardingTextMuted
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OnboardingPrimaryButton(
                label = "Berikutnya",
                showArrow = true,
                onClick = onNextClick,
                testTag = "btn_onboarding_next_2"
            )
        }
    }
}

@Composable
private fun Step3BillsSetup(
    presets: List<BillPreset>,
    selectedBillIds: List<String>,
    billAmountMap: Map<String, String>,
    billDayMap: Map<String, Int>,
    billReminderEnabled: Boolean,
    currencySymbol: String,
    onToggleBill: (BillPreset) -> Unit,
    onAddOtherClick: () -> Unit,
    onAmountChange: (String, String) -> Unit,
    onDueDateClick: (BillPreset) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onNextClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LANGKAH 3 DARI 5",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = OnboardingBlueBright
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Apa saja yang Anda bayar setiap bulan?",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 30.sp,
                        color = OnboardingTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Step3BillsIllustration(modifier = Modifier.size(66.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ketuk tagihan yang Anda punya, lalu isi jumlah dan tanggal jatuh temponya.",
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = OnboardingTextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3-column grid of presets + "+ Lainnya"
            val totalCells = presets.map<BillPreset, BillPreset?> { it } + listOf(null)
            val gridRows = totalCells.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                gridRows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { preset ->
                            if (preset == null) {
                                // "+ Lainnya" card
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = OnboardingCardBg,
                                    border = BorderStroke(1.dp, OnboardingCardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(86.dp)
                                        .clickable(onClick = onAddOtherClick)
                                        .testTag("btn_onboarding_add_custom_bill")
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Lainnya",
                                            tint = OnboardingTextMuted,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Lainnya",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = OnboardingTextMuted
                                        )
                                    }
                                }
                            } else {
                                val isSel = preset.id in selectedBillIds
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSel) OnboardingBlue.copy(alpha = 0.16f) else OnboardingCardBg,
                                    border = BorderStroke(
                                        width = if (isSel) 1.5.dp else 1.dp,
                                        color = if (isSel) OnboardingBlue else OnboardingCardBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(86.dp)
                                        .clickable { onToggleBill(preset) }
                                        .testTag("bill_preset_${preset.id}")
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                        if (isSel) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(OnboardingBlue),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Dipilih",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(preset.iconColor.copy(alpha = 0.18f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = preset.icon,
                                                    contentDescription = preset.title,
                                                    tint = preset.iconColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = preset.title,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) OnboardingTextPrimary else OnboardingTextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedBillIds.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = OnboardingCardBg.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, OnboardingCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = OnboardingTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Bukan ada tagihan. Anda bisa menambahkannya nanti di Tagihan.",
                            fontSize = 12.5.sp,
                            color = OnboardingTextMuted
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    selectedBillIds.forEach { billId ->
                        val preset = presets.firstOrNull { it.id == billId } ?: return@forEach
                        val day = billDayMap[billId] ?: preset.defaultDay
                        val amtText = billAmountMap[billId] ?: ""

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = OnboardingCardBg,
                            border = BorderStroke(1.dp, OnboardingCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(preset.iconColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = preset.icon,
                                        contentDescription = null,
                                        tint = preset.iconColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnboardingTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Jatuh tempo tgl $day",
                                        fontSize = 11.5.sp,
                                        color = OnboardingBlueBright,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onDueDateClick(preset) }
                                            .padding(vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = OnboardingCardAltBg,
                                    border = BorderStroke(1.dp, OnboardingCardBorder),
                                    modifier = Modifier.widthIn(min = 105.dp, max = 140.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = currencySymbol,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OnboardingTextMuted
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        BasicTextField(
                                            value = amtText,
                                            onValueChange = { onAmountChange(billId, it) },
                                            singleLine = true,
                                            visualTransformation = ThousandsSeparatorVisualTransformation('.'),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            textStyle = TextStyle(
                                                color = OnboardingTextPrimary,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.End
                                            ),
                                            cursorBrush = SolidColor(OnboardingBlue),
                                            modifier = Modifier.weight(1f),
                                            decorationBox = { inner ->
                                                if (amtText.isEmpty()) {
                                                    Text(
                                                        text = "0",
                                                        fontSize = 13.5.sp,
                                                        color = OnboardingTextMuted,
                                                        textAlign = TextAlign.End,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                                inner()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "Isi jumlah untuk setiap tagihan",
                        fontSize = 11.5.sp,
                        color = OnboardingTextMuted,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = OnboardingCardBg,
                        border = BorderStroke(1.dp, OnboardingCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleReminder(!billReminderEnabled) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = OnboardingBlueBright,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ingatkan sehari sebelumnya",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnboardingTextPrimary
                                )
                                Text(
                                    text = "Kami akan meminta izin setelah Anda selesai.",
                                    fontSize = 11.sp,
                                    color = OnboardingTextMuted
                                )
                            }
                            Switch(
                                checked = billReminderEnabled,
                                onCheckedChange = onToggleReminder,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = OnboardingBlue,
                                    uncheckedThumbColor = OnboardingTextMuted,
                                    uncheckedTrackColor = OnboardingCardAltBg
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OnboardingPrimaryButton(
                label = "Berikutnya",
                showArrow = true,
                onClick = onNextClick,
                testTag = "btn_onboarding_next_3"
            )
        }
    }
}

@Composable
private fun Step4BudgetsSetup(
    presets: List<BudgetPreset>,
    hasBills: Boolean,
    monthlyIncome: Double,
    currencyCode: String,
    currencySymbol: String,
    enabledBudgets: Map<String, Boolean>,
    budgetAmountMap: Map<String, String>,
    totalBudgetsAmount: Double,
    onGoToIncome: () -> Unit,
    onAutoFillFromIncome: () -> Unit,
    onToggleCategory: (String) -> Unit,
    onBudgetAmountChange: (String, String) -> Unit,
    onNextClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LANGKAH 4 DARI 5",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = OnboardingBlueBright
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tetapkan Batas Anggaran Bulanan",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 30.sp,
                        color = OnboardingTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Step4PieChartIllustration(modifier = Modifier.size(64.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasBills) {
                    "Tagihan Anda sudah tercakup. Batas ini untuk pengeluaran sehari-hari."
                } else {
                    "Atur batas kategori untuk membantu menjaga pengeluaran sehari-hari tetap terkendali."
                },
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = OnboardingTextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Income suggestion card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = OnboardingCardBg,
                border = BorderStroke(1.dp, OnboardingCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    if (monthlyIncome <= 0.0) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Isi pemasukan Anda di langkah 2 dan angka ini akan terisi otomatis.",
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = OnboardingTextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tambah pemasukan",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnboardingBlueBright,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onGoToIncome)
                                    .padding(vertical = 2.dp)
                            )
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pemasukan bulanan: ${CurrencyDataHelper.formatAmount(monthlyIncome, currencyCode)}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnboardingTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Isi batas otomatis dari pemasukan",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnboardingBlueBright,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onAutoFillFromIncome)
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Budget Category Rows
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                presets.forEach { preset ->
                    val isChecked = enabledBudgets[preset.categoryName] == true
                    val amtText = budgetAmountMap[preset.categoryName] ?: ""
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = OnboardingCardBg,
                        border = BorderStroke(
                            1.dp,
                            if (isChecked) OnboardingBlue.copy(alpha = 0.55f) else OnboardingCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleCategory(preset.categoryName) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Custom rounded checkbox
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isChecked) OnboardingBlue else Color.Transparent)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isChecked) OnboardingBlue else OnboardingTextMuted,
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(preset.iconTint.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = preset.icon,
                                    contentDescription = null,
                                    tint = preset.iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = preset.categoryName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnboardingTextPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = OnboardingCardAltBg,
                                border = BorderStroke(1.dp, OnboardingCardBorder),
                                modifier = Modifier.widthIn(min = 105.dp, max = 140.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currencySymbol,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnboardingTextMuted
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    BasicTextField(
                                        value = amtText,
                                        onValueChange = { onBudgetAmountChange(preset.categoryName, it) },
                                        singleLine = true,
                                        visualTransformation = ThousandsSeparatorVisualTransformation('.'),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(
                                            color = OnboardingTextPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.End
                                        ),
                                        cursorBrush = SolidColor(OnboardingBlue),
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { inner ->
                                            if (amtText.isEmpty()) {
                                                Text(
                                                    text = "0",
                                                    fontSize = 13.5.sp,
                                                    color = OnboardingTextMuted,
                                                    textAlign = TextAlign.End,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            inner()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total anggaran bulanan",
                    fontSize = 13.sp,
                    color = OnboardingTextMuted
                )
                Text(
                    text = CurrencyDataHelper.formatAmount(totalBudgetsAmount, currencyCode),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnboardingTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OnboardingPrimaryButton(
                label = "Berikutnya",
                showArrow = true,
                onClick = onNextClick,
                testTag = "btn_onboarding_next_4"
            )
        }
    }
}

@Composable
private fun Step5MonthlyPlanPreview(
    cycleLabel: String,
    currencyCode: String,
    monthlyIncome: Double,
    billCount: Int,
    totalBills: Double,
    budgetCount: Int,
    totalBudgets: Double,
    onGoToIncome: () -> Unit,
    onCompleteSetupClick: () -> Unit
) {
    val leftToSave = monthlyIncome - totalBills - totalBudgets

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "LANGKAH 5 DARI 5",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = OnboardingBlueBright
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Inilah bulan Anda",
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OnboardingTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Dari gajian ke gajian: $cycleLabel. Ubah baris mana pun nanti di Perencana.",
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = OnboardingTextMuted
            )

            Spacer(modifier = Modifier.height(22.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = OnboardingCardBg,
                border = BorderStroke(1.dp, OnboardingCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Step5CoinJarIllustration(modifier = Modifier.size(104.dp))

                    Spacer(modifier = Modifier.height(14.dp))

                    if (monthlyIncome <= 0.0 && totalBills <= 0.0 && totalBudgets <= 0.0) {
                        Text(
                            text = "Isi pemasukan Anda untuk melihat sisa setelah tagihan dan anggaran.",
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            color = OnboardingTextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tambah pemasukan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnboardingBlueBright,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onGoToIncome)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PlanSummaryRow(
                                label = "Pendapatan bulanan",
                                value = CurrencyDataHelper.formatAmount(monthlyIncome, currencyCode),
                                valueColor = OnboardingGreen
                            )
                            PlanSummaryRow(
                                label = if (billCount > 0) "Tagihan ($billCount)" else "Tagihan",
                                value = "-${CurrencyDataHelper.formatAmount(totalBills, currencyCode)}",
                                valueColor = Color(0xFFF59E0B)
                            )
                            PlanSummaryRow(
                                label = if (budgetCount > 0) "Anggaran ($budgetCount)" else "Anggaran",
                                value = "-${CurrencyDataHelper.formatAmount(totalBudgets, currencyCode)}",
                                valueColor = OnboardingBlueBright
                            )
                            HorizontalDivider(color = OnboardingCardBorder)
                            PlanSummaryRow(
                                label = "Sisa untuk ditabung",
                                value = CurrencyDataHelper.formatAmount(leftToSave, currencyCode),
                                valueColor = if (leftToSave >= 0) OnboardingGreen else Color(0xFFF43F5E),
                                bold = true
                            )
                        }
                        if (monthlyIncome <= 0.0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tambah pemasukan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnboardingBlueBright,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onGoToIncome)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OnboardingPrimaryButton(
                label = "Selesaikan pengaturan",
                showCheck = true,
                onClick = onCompleteSetupClick,
                testTag = "btn_onboarding_next_5"
            )
        }
    }
}

@Composable
private fun PlanSummaryRow(
    label: String,
    value: String,
    valueColor: Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (bold) 14.5.sp else 13.5.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) OnboardingTextPrimary else OnboardingTextMuted
        )
        Text(
            text = value,
            fontSize = if (bold) 15.5.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun Step6CompletionSummary(
    accountSummary: String,
    paydaySummary: String,
    billsSummary: String,
    budgetsSummary: String,
    planSummary: String,
    onFinishClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            CompletionCheckmarkBadge(modifier = Modifier.size(116.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Anda sudah siap!",
                fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OnboardingTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kebiasaan dimulai hari ini – inilah pengaturan Anda sekilas.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = OnboardingTextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = OnboardingCardBg,
                border = BorderStroke(1.dp, OnboardingCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    CompletionSummaryRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = Color(0xFF3B82F6),
                        label = "Akun",
                        value = accountSummary
                    )
                    HorizontalDivider(color = OnboardingCardBorder.copy(alpha = 0.7f))
                    CompletionSummaryRow(
                        icon = Icons.Default.CalendarToday,
                        iconTint = Color(0xFF60A5FA),
                        label = "Hari gajian",
                        value = paydaySummary
                    )
                    HorizontalDivider(color = OnboardingCardBorder.copy(alpha = 0.7f))
                    CompletionSummaryRow(
                        icon = Icons.Default.ReceiptLong,
                        iconTint = Color(0xFFA855F7),
                        label = "Tagihan",
                        value = billsSummary
                    )
                    HorizontalDivider(color = OnboardingCardBorder.copy(alpha = 0.7f))
                    CompletionSummaryRow(
                        icon = Icons.Default.PieChart,
                        iconTint = Color(0xFFF59E0B),
                        label = "Anggaran bulanan",
                        value = budgetsSummary
                    )
                    HorizontalDivider(color = OnboardingCardBorder.copy(alpha = 0.7f))
                    CompletionSummaryRow(
                        icon = Icons.Default.Savings,
                        iconTint = Color(0xFF22D3EE),
                        label = "Rencana bulanan",
                        value = planSummary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OnboardingPrimaryButton(
                label = "Ke Dasbor",
                showArrow = true,
                onClick = onFinishClick,
                testTag = "btn_onboarding_finish"
            )
        }
    }
}

@Composable
private fun CompletionSummaryRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 13.5.sp,
            color = OnboardingTextMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnboardingTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OnboardingPrimaryButton(
    label: String,
    onClick: () -> Unit,
    testTag: String,
    showArrow: Boolean = false,
    showCheck: Boolean = false
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = OnboardingBlue),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (showArrow) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else if (showCheck) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun computeCycleRangeLabel(paydayDay: Int): String {
    val shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    val now = Calendar.getInstance()
    val todayDay = now.get(Calendar.DAY_OF_MONTH)
    val maxThisMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)

    val normalizedStartDay = if (paydayDay == 31) maxThisMonth else paydayDay.coerceIn(1, maxThisMonth)
    val startCal = (now.clone() as Calendar).apply {
        if (todayDay < normalizedStartDay && paydayDay != 1) {
            add(Calendar.MONTH, -1)
        }
        val mMax = getActualMaximum(Calendar.DAY_OF_MONTH)
        val actualStart = if (paydayDay == 31) mMax else paydayDay.coerceIn(1, mMax)
        set(Calendar.DAY_OF_MONTH, actualStart)
    }

    val endCal = (startCal.clone() as Calendar).apply {
        if (paydayDay == 1) {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        } else {
            add(Calendar.MONTH, 1)
            val mMaxNext = getActualMaximum(Calendar.DAY_OF_MONTH)
            val nextStart = if (paydayDay == 31) mMaxNext else paydayDay.coerceIn(1, mMaxNext)
            set(Calendar.DAY_OF_MONTH, nextStart)
            add(Calendar.DAY_OF_MONTH, -1)
        }
    }

    val sDay = startCal.get(Calendar.DAY_OF_MONTH)
    val sMonth = shortMonths[startCal.get(Calendar.MONTH)]
    val eDay = endCal.get(Calendar.DAY_OF_MONTH)
    val eMonth = shortMonths[endCal.get(Calendar.MONTH)]
    return "$sDay $sMonth – $eDay $eMonth"
}
