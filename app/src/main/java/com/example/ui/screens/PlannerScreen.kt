package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FinancialPlan
import com.example.data.model.MainCategory
import com.example.data.model.PlanSectionType
import com.example.data.model.PlannedItem
import com.example.data.model.TransactionType
import com.example.ui.components.PerencanaTwoToneIcon
import com.example.ui.dialogs.AddEditPlannedItemSheet
import com.example.ui.dialogs.CreateNewPlanSheet
import com.example.ui.dialogs.DeletePlanConfirmDialog
import com.example.ui.dialogs.EditPlanTitleDialog
import com.example.ui.dialogs.ManageCategoriesDialog
import com.example.ui.dialogs.PlanMonthChoice
import com.example.ui.dialogs.PlannerHelpDialog
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
import com.example.util.CategoryIconHelper
import com.example.util.CurrencyHelper
import java.util.Calendar
import java.util.Locale

private val PlannerRed = Color(0xFFDD5B67)
private val PlannerGreen = Color(0xFF56B88D)
private val PlannerSpaceColor = Color(0xFF334155)

private val FullMonthNames = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
)

private val ShortMonthNames = listOf(
    "JAN", "FEB", "MAR", "APR", "MEI", "JUN",
    "JUL", "AGU", "SEP", "OKT", "NOV", "DES"
)

@Composable
fun PlannerScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val planTitle by viewModel.planTitle.collectAsState()
    val plannedItems by viewModel.plannedItems.collectAsState()
    val financialPlans by viewModel.financialPlans.collectAsState()
    val activePlanId by viewModel.activePlanId.collectAsState()
    val plannedIncomeTotal by viewModel.plannedIncomeTotal.collectAsState()
    val plannedExpenseTotal by viewModel.plannedExpenseTotal.collectAsState()
    val plannedSavingsTotal by viewModel.plannedSavingsTotal.collectAsState()
    val unallocatedAmount by viewModel.unallocatedAmount.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Rencana aktif, 1 = Semua rencana
    var showEditPlanTitleDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showNewPlanSheet by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<FinancialPlan?>(null) }
    var editingItem by remember { mutableStateOf<PlannedItem?>(null) }
    var addingSection by remember { mutableStateOf<PlanSectionType?>(null) }

    val incomeItems = remember(plannedItems) { plannedItems.filter { it.section == PlanSectionType.INCOME } }
    val expenseItems = remember(plannedItems) { plannedItems.filter { it.section == PlanSectionType.EXPENSE } }
    val savingsItems = remember(plannedItems) { plannedItems.filter { it.section == PlanSectionType.SAVINGS } }

    val savingsRate = remember(plannedIncomeTotal, plannedSavingsTotal) {
        if (plannedIncomeTotal > 0) (plannedSavingsTotal / plannedIncomeTotal) * 100 else 0.0
    }
    val expenseRatio = remember(plannedIncomeTotal, plannedExpenseTotal) {
        if (plannedIncomeTotal > 0) (plannedExpenseTotal / plannedIncomeTotal) * 100 else 0.0
    }

    val activeRecurringCount = remember(recurringTransactions) {
        recurringTransactions.count { it.isActive }
    }

    // Generate upcoming month choices (3 months starting after latest plan or current month)
    val nextMonthChoices = remember(financialPlans) {
        val cal = Calendar.getInstance()
        val latest = financialPlans.maxByOrNull { it.year * 100 + it.monthIndex }
        if (latest != null) {
            cal.set(Calendar.YEAR, latest.year)
            cal.set(Calendar.MONTH, (latest.monthIndex - 1).coerceIn(0, 11))
            cal.add(Calendar.MONTH, 1)
        } else {
            cal.add(Calendar.MONTH, 1)
        }
        (0..2).map {
            val m = cal.get(Calendar.MONTH) + 1
            val y = cal.get(Calendar.YEAR)
            val label = "${FullMonthNames.getOrElse(m - 1) { "Bulan" }} $y"
            cal.add(Calendar.MONTH, 1)
            PlanMonthChoice(monthIndex = m, year = y, label = label)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = DarkBackground,
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showNewPlanSheet = true },
                    containerColor = AccentBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("fab_new_plan")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Rencana baru", tint = Color.White)
                }
            }
        },
        bottomBar = {
            if (selectedTab == 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurface,
                    tonalElevation = 8.dp,
                    border = BorderStroke(0.5.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BELUM DIALOKASIKAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = CurrencyHelper.formatRupiah(unallocatedAmount),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (unallocatedAmount < 0) ExpenseRed else TextWhite,
                            modifier = Modifier.testTag("bottom_unallocated_amount")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top App Bar: [←] Perencana [Share]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_planner_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Perencana",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val shareText = buildString {
                                appendLine("Rencana Keuangan: $planTitle")
                                appendLine("Pemasukan: ${CurrencyHelper.formatRupiah(plannedIncomeTotal)}")
                                appendLine("Pengeluaran: ${CurrencyHelper.formatRupiah(plannedExpenseTotal)}")
                                appendLine("Tabungan: ${CurrencyHelper.formatRupiah(plannedSavingsTotal)}")
                                appendLine("Belum Dialokasikan: ${CurrencyHelper.formatRupiah(unallocatedAmount)}")
                                appendLine("Tingkat Tabungan: ${String.format(Locale.US, "%.1f%%", savingsRate)}")
                            }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Rencana Keuangan"))
                        }
                        .testTag("btn_planner_share")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = "Bagikan",
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Segmented Tab Switcher matching video: "Rencana aktif" | "Semua rencana N"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
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
                        PlannerSegmentChip(
                            label = "Rencana aktif",
                            countText = null,
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            testTag = "tab_active_plan"
                        )
                        PlannerSegmentChip(
                            label = "Semua rencana",
                            countText = "${financialPlans.size.coerceAtLeast(1)}",
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            testTag = "tab_all_plans"
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                ActivePlanContent(
                    planTitle = planTitle,
                    unallocatedAmount = unallocatedAmount,
                    plannedIncomeTotal = plannedIncomeTotal,
                    plannedExpenseTotal = plannedExpenseTotal,
                    plannedSavingsTotal = plannedSavingsTotal,
                    savingsRate = savingsRate,
                    expenseRatio = expenseRatio,
                    incomeItems = incomeItems,
                    expenseItems = expenseItems,
                    savingsItems = savingsItems,
                    categories = categories,
                    canDeleteActivePlan = financialPlans.size > 1,
                    onEditPlanTitle = { showEditPlanTitleDialog = true },
                    onDeleteActivePlan = {
                        financialPlans.find { it.id == activePlanId }?.let { planToDelete = it }
                    },
                    onOpenHelp = { showHelpDialog = true },
                    onEditItem = { editingItem = it },
                    onAddSectionItem = { addingSection = it }
                )
            } else {
                AllPlansTimelineContent(
                    plans = financialPlans,
                    activePlanId = activePlanId,
                    activePlanTitle = planTitle,
                    nextMonthChoice = nextMonthChoices.first(),
                    recurringCount = activeRecurringCount,
                    onSelectPlan = { plan ->
                        viewModel.selectActivePlan(plan.id)
                        selectedTab = 0
                    },
                    onDeletePlan = { plan ->
                        planToDelete = plan
                    },
                    onOpenNewPlanSheet = { showNewPlanSheet = true }
                )
            }
        }
    }

    if (showNewPlanSheet) {
        CreateNewPlanSheet(
            monthChoices = nextMonthChoices,
            activePlanTitle = planTitle,
            activePlanItemCount = plannedItems.size,
            recurringBillsCount = activeRecurringCount,
            existingPlans = financialPlans,
            activePlanId = activePlanId,
            onCreatePlan = { m, y, mode ->
                viewModel.createFinancialPlan(m, y, mode)
                showNewPlanSheet = false
                selectedTab = 0
            },
            onDeletePlan = { plan ->
                viewModel.deleteFinancialPlan(plan.id)
            },
            onDismiss = { showNewPlanSheet = false }
        )
    }

    planToDelete?.let { targetPlan ->
        DeletePlanConfirmDialog(
            plan = targetPlan,
            onConfirm = {
                viewModel.deleteFinancialPlan(targetPlan.id)
                planToDelete = null
            },
            onDismiss = { planToDelete = null }
        )
    }

    editingItem?.let { item ->
        AddEditPlannedItemSheet(
            section = item.section,
            editingItem = item,
            categories = categories,
            onSave = { updated ->
                viewModel.updatePlannedItem(updated)
                editingItem = null
            },
            onDelete = {
                viewModel.deletePlannedItem(item.id)
                editingItem = null
            },
            onManageCategories = { showManageCategoriesDialog = true },
            onDismiss = { editingItem = null }
        )
    }

    addingSection?.let { section ->
        AddEditPlannedItemSheet(
            section = section,
            editingItem = null,
            categories = categories,
            onSave = { newItem ->
                viewModel.addPlannedItem(newItem)
                addingSection = null
            },
            onManageCategories = { showManageCategoriesDialog = true },
            onDismiss = { addingSection = null }
        )
    }

    if (showManageCategoriesDialog) {
        ManageCategoriesDialog(
            viewModel = viewModel,
            onDismiss = { showManageCategoriesDialog = false }
        )
    }

    if (showEditPlanTitleDialog) {
        EditPlanTitleDialog(
            currentTitle = planTitle,
            onSave = { newTitle ->
                viewModel.updatePlanTitle(newTitle)
                showEditPlanTitleDialog = false
            },
            onDismiss = { showEditPlanTitleDialog = false }
        )
    }

    if (showHelpDialog) {
        PlannerHelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
private fun PlannerSegmentChip(
    label: String,
    countText: String?,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) DarkSurfaceElevated else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) TextWhite else TextMuted
            )
            if (countText != null) {
                Text(
                    text = countText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) AccentBlueBright else TextSubtle
                )
            }
        }
    }
}

@Composable
private fun ActivePlanContent(
    planTitle: String,
    unallocatedAmount: Double,
    plannedIncomeTotal: Double,
    plannedExpenseTotal: Double,
    plannedSavingsTotal: Double,
    savingsRate: Double,
    expenseRatio: Double,
    incomeItems: List<PlannedItem>,
    expenseItems: List<PlannedItem>,
    savingsItems: List<PlannedItem>,
    categories: List<MainCategory>,
    canDeleteActivePlan: Boolean,
    onEditPlanTitle: () -> Unit,
    onDeleteActivePlan: () -> Unit,
    onOpenHelp: () -> Unit,
    onEditItem: (PlannedItem) -> Unit,
    onAddSectionItem: (PlanSectionType) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Title row: Pembagian Gaji ✎       [Delete] ?
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEditPlanTitle() }
                        .padding(vertical = 4.dp)
                        .testTag("btn_edit_plan_title")
                ) {
                    Text(
                        text = planTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Ubah Nama Rencana",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (canDeleteActivePlan) {
                        IconButton(
                            onClick = onDeleteActivePlan,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("btn_delete_active_plan")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Hapus Rencana Aktif",
                                tint = ExpenseRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenHelp,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_planner_help")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Bantuan Perencanaan",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Summary Card: BELUM DIALOKASIKAN
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "BELUM DIALOKASIKAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSubtle,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyHelper.formatRupiah(unallocatedAmount),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (unallocatedAmount < 0) ExpenseRed else TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (unallocatedAmount >= 0) {
                            "Belum dialokasikan. Rencana yang seimbang menempatkan setiap jumlah di tempatnya — targetkan nol."
                        } else {
                            "Melebihi pemasukan. Sesuaikan beberapa pos pengeluaran atau tambah pemasukan agar seimbang."
                        },
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SegmentedPlannerProgressBar(
                        expenseTotal = plannedExpenseTotal,
                        savingsTotal = plannedSavingsTotal,
                        unallocatedTotal = unallocatedAmount.coerceAtLeast(0.0),
                        incomeTotal = plannedIncomeTotal
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlannerLegendItem(color = PlannerRed, label = "Pengeluaran")
                        PlannerLegendItem(color = PlannerGreen, label = "Tabungan")
                        PlannerLegendItem(color = PlannerSpaceColor, label = "Ruang gerak")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TINGKAT TABUNGAN",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSubtle,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f%%", savingsRate),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }

                        VerticalDivider(
                            color = DarkCardBorder,
                            thickness = 0.8.dp,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RASIO PENGELUARAN",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSubtle,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f%%", expenseRatio),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }
                    }
                }
            }
        }

        // Section 1: PEMASUKAN
        item {
            PlannerSectionHeader(
                title = "PEMASUKAN",
                badgeColor = PlannerGreen,
                totalAmount = plannedIncomeTotal,
                amountColor = PlannerGreen
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column {
                    incomeItems.forEachIndexed { index, item ->
                        val (icon, bgCol, tintCol) = resolvePlannerItemIconAndColor(
                            item = item,
                            categories = categories,
                            txType = TransactionType.INCOME
                        )
                        PlannedItemRow(
                            item = item,
                            amountColor = PlannerGreen,
                            icon = icon,
                            iconBgColor = bgCol,
                            iconTint = tintCol,
                            onEditClick = { onEditItem(item) }
                        )
                        if (index < incomeItems.size - 1) {
                            HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    if (incomeItems.isNotEmpty()) {
                        HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    AddPlannedItemRowButton(
                        label = "Tambah pemasukan",
                        accentColor = PlannerGreen,
                        onClick = { onAddSectionItem(PlanSectionType.INCOME) }
                    )
                }
            }
        }

        // Section 2: PENGELUARAN
        item {
            PlannerSectionHeader(
                title = "PENGELUARAN",
                badgeColor = PlannerRed,
                totalAmount = plannedExpenseTotal,
                amountColor = PlannerRed
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column {
                    expenseItems.forEachIndexed { index, item ->
                        val (icon, bgCol, tintCol) = resolvePlannerItemIconAndColor(
                            item = item,
                            categories = categories,
                            txType = TransactionType.EXPENSE
                        )
                        PlannedItemRow(
                            item = item,
                            amountColor = PlannerRed,
                            icon = icon,
                            iconBgColor = bgCol,
                            iconTint = tintCol,
                            onEditClick = { onEditItem(item) }
                        )
                        if (index < expenseItems.size - 1) {
                            HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    if (expenseItems.isNotEmpty()) {
                        HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    AddPlannedItemRowButton(
                        label = "Tambah pengeluaran",
                        accentColor = PlannerRed,
                        onClick = { onAddSectionItem(PlanSectionType.EXPENSE) }
                    )
                }
            }
        }

        // Section 3: RENCANA TABUNGAN
        item {
            PlannerSectionHeader(
                title = "RENCANA TABUNGAN",
                badgeColor = PlannerGreen,
                totalAmount = plannedSavingsTotal,
                amountColor = PlannerGreen
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column {
                    savingsItems.forEachIndexed { index, item ->
                        PlannedItemRow(
                            item = item,
                            amountColor = PlannerGreen,
                            icon = null,
                            iconBgColor = Color.Transparent,
                            iconTint = Color.Transparent,
                            onEditClick = { onEditItem(item) }
                        )
                        if (index < savingsItems.size - 1) {
                            HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    if (savingsItems.isNotEmpty()) {
                        HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    AddPlannedItemRowButton(
                        label = "Tambah tabungan",
                        accentColor = PlannerGreen,
                        onClick = { onAddSectionItem(PlanSectionType.SAVINGS) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AllPlansTimelineContent(
    plans: List<FinancialPlan>,
    activePlanId: String,
    activePlanTitle: String,
    nextMonthChoice: PlanMonthChoice,
    recurringCount: Int,
    onSelectPlan: (FinancialPlan) -> Unit,
    onDeletePlan: (FinancialPlan) -> Unit,
    onOpenNewPlanSheet: () -> Unit
) {
    val calNow = remember { Calendar.getInstance() }
    val curMonth = calNow.get(Calendar.MONTH) + 1
    val curYear = calNow.get(Calendar.YEAR)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 92.dp)
    ) {
        items(plans, key = { it.id }) { plan ->
            val isActive = plan.id == activePlanId
            val income = plan.items.filter { it.section == PlanSectionType.INCOME }.sumOf { it.amount }
            val expense = plan.items.filter { it.section == PlanSectionType.EXPENSE }.sumOf { it.amount }
            val savings = plan.items.filter { it.section == PlanSectionType.SAVINGS }.sumOf { it.amount }
            val planned = expense + savings
            val remaining = income - planned

            val periodLabel = when {
                plan.year == curYear && plan.monthIndex == curMonth -> "BULAN INI"
                plan.year * 100 + plan.monthIndex > curYear * 100 + curMonth -> "MENDATANG"
                else -> "BULAN LALU"
            }

            val monthShort = ShortMonthNames.getOrElse(plan.monthIndex - 1) { "BLN" }
            val yearShort = (plan.year % 100).toString().padStart(2, '0')

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                // Left Timeline Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(56.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isActive) Color.Transparent else DarkSurface,
                        border = BorderStroke(1.dp, if (isActive) AccentBlueBright.copy(alpha = 0.55f) else DarkCardBorder),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(
                            modifier = if (isActive) {
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                AccentBlue.copy(alpha = 0.72f),
                                                AccentBlue.copy(alpha = 0.36f)
                                            )
                                        )
                                    )
                            } else {
                                Modifier.fillMaxSize()
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = monthShort,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White.copy(alpha = 0.9f) else TextMuted
                                )
                                Text(
                                    text = yearShort,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color.White else TextWhite
                                )
                            }
                        }
                    }

                    // Vertical connector line down to the next node
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .padding(vertical = 4.dp)
                            .background(DarkCardBorder)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectPlan(plan) }
                        .testTag("card_plan_${plan.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(
                        1.dp,
                        if (isActive) AccentBlue.copy(alpha = 0.45f) else DarkCardBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = periodLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentBlueBright,
                                    letterSpacing = 0.6.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = plan.title,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isActive) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AccentBlue.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "Aktif",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentBlueBright,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (plans.size > 1) {
                                    IconButton(
                                        onClick = { onDeletePlan(plan) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("btn_delete_plan_${plan.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus Rencana",
                                            tint = if (isActive) ExpenseRed else TextMuted,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SegmentedPlannerProgressBar(
                            expenseTotal = expense,
                            savingsTotal = savings,
                            unallocatedTotal = remaining.coerceAtLeast(0.0),
                            incomeTotal = income
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PlanSummaryMetricCol(
                                label = "Pemasukan",
                                value = CurrencyHelper.formatRupiah(income),
                                valueColor = TextWhite,
                                modifier = Modifier.weight(1f)
                            )
                            PlanSummaryMetricCol(
                                label = "Direncanakan",
                                value = CurrencyHelper.formatRupiah(planned),
                                valueColor = TextWhite,
                                modifier = Modifier.weight(1f)
                            )
                            PlanSummaryMetricCol(
                                label = "Sisa",
                                value = CurrencyHelper.formatRupiah(remaining),
                                valueColor = if (remaining >= 0) PlannerGreen else ExpenseRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Next Unplanned Month CTA Row
        item {
            val nextMonthShort = ShortMonthNames.getOrElse(nextMonthChoice.monthIndex - 1) { "BLN" }
            val nextMonthFull = FullMonthNames.getOrElse(nextMonthChoice.monthIndex - 1) { "Bulan Berikutnya" }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenNewPlanSheet() },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(
                                color = DarkCardBorder,
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                                ),
                                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = nextMonthShort,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSubtle
                            )
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(DarkSurface.copy(alpha = 0.65f))
                        .clickable { onOpenNewPlanSheet() }
                        .testTag("card_plan_next_month")
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRoundRect(
                            color = DarkCardBorder,
                            style = Stroke(
                                width = 1.2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                            ),
                            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rencanakan $nextMonthFull",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Salin $activePlanTitle, atau mulai dari $recurringCount tagihannya.",
                                fontSize = 11.5.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        PerencanaTwoToneIcon(
                            themeColor = PlannerGreen,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanSummaryMetricCol(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlannerSectionHeader(
    title: String,
    badgeColor: Color,
    totalAmount: Double,
    amountColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.6.sp
            )
        }

        Text(
            text = CurrencyHelper.formatRupiah(totalAmount),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
fun PlannedItemRow(
    item: PlannedItem,
    amountColor: Color,
    icon: ImageVector?,
    iconBgColor: Color,
    iconTint: Color,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("planned_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                if (item.category.isNotBlank()) {
                    val subtitle = if (item.subcategory.isNotBlank()) {
                        "${item.category} • ${item.subcategory}"
                    } else {
                        item.category
                    }
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = CurrencyHelper.formatRupiah(item.amount),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("btn_edit_planned_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit nominal ${item.name}",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun AddPlannedItemRowButton(
    label: String,
    accentColor: Color = TextMuted,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("btn_add_planned_row_$label"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0x1AFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentColor
        )
    }
}

@Composable
fun SegmentedPlannerProgressBar(
    expenseTotal: Double,
    savingsTotal: Double,
    unallocatedTotal: Double,
    incomeTotal: Double
) {
    val total = if (incomeTotal > 0) incomeTotal else (expenseTotal + savingsTotal + unallocatedTotal).coerceAtLeast(1.0)
    val expenseWeight = (expenseTotal / total).toFloat().coerceIn(0f, 1f)
    val savingsWeight = (savingsTotal / total).toFloat().coerceIn(0f, 1f)
    val unallocatedWeight = ((total - expenseTotal - savingsTotal).coerceAtLeast(0.0) / total).toFloat().coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(DarkSurfaceElevated)
    ) {
        if (expenseWeight > 0f) {
            Box(
                modifier = Modifier
                    .weight(expenseWeight)
                    .height(9.dp)
                    .background(PlannerRed)
            )
        }
        if (savingsWeight > 0f) {
            Box(
                modifier = Modifier
                    .weight(savingsWeight)
                    .height(9.dp)
                    .background(PlannerGreen)
            )
        }
        if (unallocatedWeight > 0f) {
            Box(
                modifier = Modifier
                    .weight(unallocatedWeight)
                    .height(9.dp)
                    .background(PlannerSpaceColor)
            )
        }
    }
}

@Composable
fun PlannerLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = TextMuted
        )
    }
}

fun resolvePlannerItemIconAndColor(
    item: PlannedItem,
    categories: List<MainCategory>,
    txType: TransactionType
): Triple<ImageVector, Color, Color> {
    val mainCat = categories.find {
        it.type == txType && it.name.equals(item.category, ignoreCase = true)
    } ?: categories.find { it.name.equals(item.category, ignoreCase = true) }

    val subItem = if (item.subcategory.isNotBlank()) {
        mainCat?.subcategories?.find { it.name.equals(item.subcategory, ignoreCase = true) }
    } else {
        null
    }

    val lookupKey = when {
        item.subcategory.isNotBlank() -> item.subcategory
        item.category.isNotBlank() -> item.category
        else -> item.name
    }
    val iconKey = subItem?.iconName?.takeIf { it.isNotBlank() }
        ?: mainCat?.iconName?.takeIf { it.isNotBlank() }
        ?: ""
    val icon = CategoryIconHelper.getCategoryIcon(lookupKey, iconKey)
    val color = CategoryIconHelper.getCategoryColor(item.category.ifBlank { item.name }, txType)
    return Triple(icon, color.copy(alpha = 0.2f), color)
}

fun getCategoryIconAndColor(category: String, name: String): Triple<ImageVector, Color, Color> {
    val key = if (category.isNotBlank()) category else name
    val icon = CategoryIconHelper.getCategoryIcon(key)
    val color = CategoryIconHelper.getCategoryColor(key)
    return Triple(icon, color.copy(alpha = 0.2f), color)
}
