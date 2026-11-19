package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.FinancialPlan
import com.example.data.model.MainCategory
import com.example.data.model.NewPlanStartMode
import com.example.data.model.PlanSectionType
import com.example.data.model.PlannedItem
import com.example.data.model.TransactionType
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.util.CategoryIconHelper
import com.example.util.ThousandsSeparatorVisualTransformation

private val SheetShortMonthNames = listOf(
    "JAN", "FEB", "MAR", "APR", "MEI", "JUN",
    "JUL", "AGU", "SEP", "OKT", "NOV", "DES"
)

private val SheetFullMonthNames = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
)

data class PlanMonthChoice(
    val monthIndex: Int,
    val year: Int,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNewPlanSheet(
    monthChoices: List<PlanMonthChoice>,
    activePlanTitle: String,
    activePlanItemCount: Int,
    recurringBillsCount: Int,
    existingPlans: List<FinancialPlan> = emptyList(),
    activePlanId: String = "",
    onCreatePlan: (monthIndex: Int, year: Int, mode: NewPlanStartMode) -> Unit,
    onDeletePlan: ((FinancialPlan) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedChoiceIndex by remember(monthChoices) { mutableIntStateOf(0) }
    var pendingDeletePlan by remember { mutableStateOf<FinancialPlan?>(null) }
    val selectedChoice = monthChoices.getOrElse(selectedChoiceIndex) {
        PlanMonthChoice(12, 2026, "Desember 2026")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            // Header row: Calendar Icon + "Rencana baru" + Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentBlue.copy(alpha = 0.16f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = AccentBlueBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Rencana baru",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = DarkSurfaceElevated,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() }
                        .testTag("btn_close_new_plan_sheet")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Untuk",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                monthChoices.forEachIndexed { index, choice ->
                    val isSelected = index == selectedChoiceIndex
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) AccentBlue else DarkSurfaceElevated,
                        border = if (isSelected) null else BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedChoiceIndex = index }
                            .testTag("chip_plan_month_${choice.monthIndex}_${choice.year}")
                    ) {
                        Text(
                            text = choice.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextMuted,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Mulai dari",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StartFromOptionCard(
                    icon = Icons.Default.ContentCopy,
                    title = "Salin rencana aktif",
                    subtitle = "Salin $activePlanItemCount pos dari $activePlanTitle",
                    onClick = {
                        onCreatePlan(
                            selectedChoice.monthIndex,
                            selectedChoice.year,
                            NewPlanStartMode.COPY_ACTIVE
                        )
                    },
                    testTag = "btn_start_plan_copy"
                )

                StartFromOptionCard(
                    icon = Icons.Default.ReceiptLong,
                    title = "Mulai dari tagihan saja",
                    subtitle = "Muat $recurringBillsCount tagihan & pemasukan berulang",
                    onClick = {
                        onCreatePlan(
                            selectedChoice.monthIndex,
                            selectedChoice.year,
                            NewPlanStartMode.BILLS_ONLY
                        )
                    },
                    testTag = "btn_start_plan_bills"
                )

                StartFromOptionCard(
                    icon = Icons.Default.Add,
                    title = "Mulai kosong",
                    subtitle = "Susun rencana baru dari awal",
                    onClick = {
                        onCreatePlan(
                            selectedChoice.monthIndex,
                            selectedChoice.year,
                            NewPlanStartMode.EMPTY
                        )
                    },
                    testTag = "btn_start_plan_empty"
                )
            }

            if (existingPlans.size > 1 && onDeletePlan != null) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Kelola rencana tersimpan (${existingPlans.size})",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    existingPlans.forEach { plan ->
                        val isCurrent = plan.id == activePlanId
                        val mShort = SheetShortMonthNames.getOrElse(plan.monthIndex - 1) { "BLN" }
                        val yShort = (plan.year % 100).toString().padStart(2, '0')
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isCurrent) AccentBlue.copy(alpha = 0.45f) else DarkCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) AccentBlue.copy(alpha = 0.2f) else DarkBackground,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isCurrent) AccentBlue.copy(alpha = 0.4f) else DarkCardBorder
                                        )
                                    ) {
                                        Text(
                                            text = "$mShort $yShort",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) AccentBlueBright else TextMuted,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = plan.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isCurrent) "Rencana aktif · ${plan.items.size} pos" else "${plan.items.size} pos tersimpan",
                                            fontSize = 11.sp,
                                            color = if (isCurrent) AccentBlueBright else TextSubtle
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { pendingDeletePlan = plan },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("btn_sheet_delete_plan_${plan.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Hapus rencana ${plan.title}",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        pendingDeletePlan?.let { targetPlan ->
            DeletePlanConfirmDialog(
                plan = targetPlan,
                onConfirm = {
                    onDeletePlan?.invoke(targetPlan)
                    pendingDeletePlan = null
                },
                onDismiss = { pendingDeletePlan = null }
            )
        }
    }
}

@Composable
fun DeletePlanConfirmDialog(
    plan: FinancialPlan,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val monthLabel = "${SheetFullMonthNames.getOrElse(plan.monthIndex - 1) { "Bulan" }} ${plan.year}"
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = ExpenseRedSoft,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Hapus Rencana?",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Rencana \"${plan.title}\" ($monthLabel) beserta ${plan.items.size} pos di dalamnya akan dihapus dari daftar rencana.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onDismiss() }
                            .testTag("btn_cancel_delete_plan")
                    ) {
                        Text(
                            text = "Batal",
                            color = TextMuted,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_confirm_delete_plan")
                    ) {
                        Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StartFromOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkBackground.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlannedItemSheet(
    section: PlanSectionType,
    editingItem: PlannedItem?,
    categories: List<MainCategory>,
    onSave: (PlannedItem) -> Unit,
    onDelete: (() -> Unit)? = null,
    onManageCategories: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val targetType = if (section == PlanSectionType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE

    val matchingMainCategories = remember(categories, section) {
        val visible = categories.filter { it.type == targetType && it.isVisible }
        if (visible.isNotEmpty()) {
            visible
        } else if (section == PlanSectionType.INCOME) {
            listOf("Salary", "Extra Income", "Investasi", "Bisnis / Freelance").map {
                MainCategory(name = it, type = TransactionType.INCOME)
            }
        } else {
            listOf("Food & Drink", "Transport", "Bills & Fees", "Shopping", "Entertainment", "Kesehatan", "Pendidikan", "Other").map {
                MainCategory(name = it, type = TransactionType.EXPENSE)
            }
        }
    }

    var name by remember(editingItem) { mutableStateOf(editingItem?.name ?: "") }
    var category by remember(editingItem, section, matchingMainCategories) {
        mutableStateOf(
            editingItem?.category?.takeIf { it.isNotBlank() }
                ?: if (section == PlanSectionType.SAVINGS) "" else matchingMainCategories.firstOrNull()?.name.orEmpty()
        )
    }
    var subcategory by remember(editingItem) { mutableStateOf(editingItem?.subcategory ?: "") }
    var amountText by remember(editingItem) {
        mutableStateOf(editingItem?.amount?.toLong()?.takeIf { it > 0 }?.toString() ?: "")
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val selectedMainCategory = remember(matchingMainCategories, category) {
        matchingMainCategories.find { it.name.equals(category, ignoreCase = true) }
    }
    val visibleSubcategories = remember(selectedMainCategory) {
        selectedMainCategory?.subcategories?.filter { it.isVisible } ?: emptyList()
    }
    val selectedSubItem = remember(visibleSubcategories, subcategory) {
        visibleSubcategories.find { it.name.equals(subcategory, ignoreCase = true) }
    }

    val sheetTitle = if (editingItem != null) {
        "Edit Pos Rencana"
    } else {
        when (section) {
            PlanSectionType.INCOME -> "Tambah Pemasukan"
            PlanSectionType.EXPENSE -> "Tambah Pengeluaran"
            PlanSectionType.SAVINGS -> "Tambah Tabungan"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sheetTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (section != PlanSectionType.SAVINGS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kategori",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (onManageCategories != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onManageCategories() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("btn_planner_manage_categories")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = AccentBlueBright,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Kelola Kategori",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentBlueBright
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Selected Category & Subcategory Trigger Card
                val catColor = CategoryIconHelper.getCategoryColor(category.ifBlank { "Other" }, targetType)
                val catIcon = if (selectedSubItem != null) {
                    CategoryIconHelper.getCategoryIcon(selectedSubItem.name, selectedSubItem.iconName)
                } else {
                    CategoryIconHelper.getCategoryIcon(category, selectedMainCategory?.iconName ?: "")
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showCategoryPicker = true }
                        .testTag("btn_open_planner_category_picker")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                    .clip(CircleShape)
                                    .background(catColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.ifBlank { "Pilih Kategori" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        subcategory.isNotBlank() -> "Subkategori: $subcategory"
                                        visibleSubcategories.isNotEmpty() -> "${visibleSubcategories.size} subkategori tersedia · ketuk untuk pilih"
                                        else -> "Ketuk untuk memilih kategori"
                                    },
                                    fontSize = 11.sp,
                                    color = if (subcategory.isNotBlank()) AccentBlueBright else TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Pilih Kategori",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick horizontal chips synced from existing MainCategories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    matchingMainCategories.forEach { mainCat ->
                        val isSelected = category.equals(mainCat.name, ignoreCase = true)
                        val chipColor = CategoryIconHelper.getCategoryColor(mainCat.name, mainCat.type)
                        val chipIcon = CategoryIconHelper.getCategoryIcon(mainCat.name, mainCat.iconName)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) chipColor.copy(alpha = 0.22f) else DarkSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) chipColor else DarkCardBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    category = mainCat.name
                                    if (!mainCat.subcategories.any { it.isVisible && it.name.equals(subcategory, ignoreCase = true) }) {
                                        subcategory = ""
                                    }
                                    errorMessage = null
                                }
                                .testTag("chip_plan_cat_${mainCat.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = chipIcon,
                                    contentDescription = null,
                                    tint = if (isSelected) chipColor else TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = mainCat.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextWhite else TextMuted
                                )
                            }
                        }
                    }
                }

                // Quick Subcategory chips when the selected MainCategory has visible subcategories
                if (visibleSubcategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Subkategori (${selectedMainCategory?.name ?: category})",
                        fontSize = 11.sp,
                        color = TextSubtle,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visibleSubcategories.forEach { sub ->
                            val isSubSelected = subcategory.equals(sub.name, ignoreCase = true)
                            val subColor = CategoryIconHelper.getSubcategoryColor(
                                selectedMainCategory?.name ?: category,
                                sub.name,
                                targetType
                            )
                            val subIcon = CategoryIconHelper.getCategoryIcon(sub.name, sub.iconName)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSubSelected) AccentBlue.copy(alpha = 0.22f) else DarkSurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSubSelected) AccentBlue else DarkCardBorder.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        subcategory = if (isSubSelected) "" else sub.name
                                    }
                                    .testTag("chip_plan_subcat_${sub.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = subIcon,
                                        contentDescription = null,
                                        tint = if (isSubSelected) AccentBlueBright else subColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = sub.name,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSubSelected) TextWhite else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Text("Nama Pos", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            val nameHint = when (section) {
                PlanSectionType.SAVINGS -> "Contoh: Dana Darurat, Tabungan Liburan..."
                else -> "Contoh: ${subcategory.ifBlank { category.ifBlank { "Gaji Pokok, Belanja Bulanan" } }}..."
            }
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                placeholder = { Text(nameHint, color = TextSubtle) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_planned_item_name"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DarkCardBorder
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("Nominal (Rp)", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    if (digits.length <= 11) {
                        amountText = digits
                        errorMessage = null
                    }
                },
                visualTransformation = ThousandsSeparatorVisualTransformation('.'),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("0", color = TextSubtle) },
                prefix = { Text("Rp", color = TextMuted, fontWeight = FontWeight.SemiBold) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_planned_item_amount"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DarkCardBorder
                ),
                shape = RoundedCornerShape(14.dp)
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = ExpenseRed, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (editingItem != null && onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ExpenseRedSoft,
                            contentColor = ExpenseRed
                        ),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hapus", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (name.isBlank()) {
                            errorMessage = "Nama pos tidak boleh kosong."
                            return@Button
                        }
                        if (amt <= 0.0) {
                            errorMessage = "Nominal harus lebih dari 0."
                            return@Button
                        }
                        val saved = editingItem?.copy(
                            name = name.trim(),
                            category = if (section == PlanSectionType.SAVINGS) "" else category.trim(),
                            subcategory = if (section == PlanSectionType.SAVINGS) "" else subcategory.trim(),
                            amount = amt
                        ) ?: PlannedItem(
                            name = name.trim(),
                            category = if (section == PlanSectionType.SAVINGS) "" else category.trim(),
                            subcategory = if (section == PlanSectionType.SAVINGS) "" else subcategory.trim(),
                            amount = amt,
                            section = section
                        )
                        onSave(saved)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_save_planned_item")
                ) {
                    Text(
                        text = if (editingItem != null) "Simpan Perubahan" else "Tambah ke Rencana",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showCategoryPicker && section != PlanSectionType.SAVINGS) {
        PlannerCategoryPickerDialog(
            section = section,
            categories = matchingMainCategories,
            selectedCategory = category,
            selectedSubcategory = subcategory,
            onSelect = { chosenCat, chosenSub ->
                category = chosenCat
                subcategory = chosenSub
                showCategoryPicker = false
            },
            onManageCategories = onManageCategories?.let { manage ->
                {
                    showCategoryPicker = false
                    manage()
                }
            },
            onDismiss = { showCategoryPicker = false }
        )
    }
}

@Composable
private fun PlannerCategoryPickerDialog(
    section: PlanSectionType,
    categories: List<MainCategory>,
    selectedCategory: String,
    selectedSubcategory: String,
    onSelect: (category: String, subcategory: String) -> Unit,
    onManageCategories: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedIds by remember(selectedCategory, categories) {
        val activeParent = categories.find { it.name.equals(selectedCategory, ignoreCase = true) }
        mutableStateOf(if (activeParent != null && selectedSubcategory.isNotBlank()) setOf(activeParent.id) else emptySet())
    }

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) {
            categories
        } else {
            val q = searchQuery.trim().lowercase()
            categories.filter { cat ->
                cat.name.lowercase().contains(q) ||
                    cat.subcategories.any { it.isVisible && it.name.lowercase().contains(q) }
            }
        }
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
                    Text(
                        text = if (section == PlanSectionType.INCOME) "Kategori Pemasukan" else "Kategori Pengeluaran",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onManageCategories != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AccentBlue.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onManageCategories() }
                            ) {
                                Text(
                                    text = "Kelola",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentBlueBright,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari kategori atau subkategori...", color = TextSubtle, fontSize = 12.5.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCategories, key = { it.id }) { cat ->
                        val isParentSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                        val catColor = CategoryIconHelper.getCategoryColor(cat.name, cat.type)
                        val visibleSubs = cat.subcategories.filter { it.isVisible }.let { subs ->
                            if (searchQuery.isBlank()) subs
                            else {
                                val q = searchQuery.trim().lowercase()
                                if (cat.name.lowercase().contains(q)) subs
                                else subs.filter { it.name.lowercase().contains(q) }
                            }
                        }
                        val hasSubs = visibleSubs.isNotEmpty()
                        val isExpanded = expandedIds.contains(cat.id) || (searchQuery.isNotBlank() && hasSubs)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceElevated, RoundedCornerShape(14.dp))
                                .border(
                                    1.dp,
                                    if (isParentSelected) catColor.copy(alpha = 0.65f) else DarkCardBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelect(cat.name, "") }
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(
                                                if (isParentSelected) catColor else catColor.copy(alpha = 0.2f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getCategoryIcon(cat.name, cat.iconName),
                                            contentDescription = null,
                                            tint = if (isParentSelected) Color.White else catColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = cat.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                        if (hasSubs) {
                                            Text(
                                                text = "${visibleSubs.size} subkategori",
                                                fontSize = 10.5.sp,
                                                color = TextSubtle
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isParentSelected && selectedSubcategory.isBlank()) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = AccentBlueBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    if (hasSubs) {
                                        IconButton(
                                            onClick = {
                                                expandedIds = if (isExpanded) {
                                                    expandedIds - cat.id
                                                } else {
                                                    expandedIds + cat.id
                                                }
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Subkategori",
                                                tint = TextMuted
                                            )
                                        }
                                    }
                                }
                            }

                            if (isExpanded && hasSubs) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    visibleSubs.forEach { sub ->
                                        val isSubSelected = isParentSelected && selectedSubcategory.equals(sub.name, ignoreCase = true)
                                        val subColor = CategoryIconHelper.getSubcategoryColor(cat.name, sub.name, cat.type)
                                        val subIcon = CategoryIconHelper.getCategoryIcon(sub.name, sub.iconName)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSubSelected) AccentBlue.copy(alpha = 0.24f) else Color(0x14FFFFFF),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSubSelected) AccentBlue else Color.Transparent
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { onSelect(cat.name, sub.name) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(subColor.copy(alpha = 0.18f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = subIcon,
                                                            contentDescription = null,
                                                            tint = subColor,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = sub.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = TextWhite
                                                    )
                                                }
                                                if (isSubSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = AccentBlueBright,
                                                        modifier = Modifier.size(16.dp)
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
    }
}

@Composable
fun EditPlanTitleDialog(
    currentTitle: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Ubah Nama Rencana", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onDismiss() }
                    ) {
                        Text("Batal", color = TextMuted, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (title.isNotBlank()) onSave(title.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PlannerHelpDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tentang Perencanaan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Prinsip Zero-Based Budgeting:\n\n" +
                            "1. Masukkan semua sumber pemasukan yang diperkirakan.\n" +
                            "2. Rencanakan pos pengeluaran bulanan Anda.\n" +
                            "3. Alokasikan sisa dana ke tabungan dan investasi.\n\n" +
                            "Targetkan dana 'Belum Dialokasikan' mendekati nol agar setiap rupiah memiliki tujuan yang terencana.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
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
