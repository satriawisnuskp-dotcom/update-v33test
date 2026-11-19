package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetCategory
import com.example.data.model.TransactionType
import com.example.ui.dialogs.BudgetDetailSheet
import com.example.ui.dialogs.MonthYearPickerDialog
import com.example.ui.dialogs.PengaturanTampilanAnggaranSheet
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TrophyYellow
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.BudgetMathHelper
import com.example.util.CategoryIconHelper
import com.example.util.ComputedBudgetItem
import com.example.util.CurrencyHelper
import com.example.util.ThousandsSeparatorVisualTransformation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val EmeraldBudgetGreen = Color(0xFF56B88D)

@Composable
fun AnggaranScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val budgets by viewModel.budgets.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val monthOptions by viewModel.monthOptions.collectAsState()
    val selectedMonth by viewModel.selectedMonthIndex.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()

    var selectedFrequency by remember { mutableStateOf("Bulanan") }
    var isSummaryExpanded by remember { mutableStateOf(true) }
    var showDisplaySettingsSheet by remember { mutableStateOf(false) }
    var showMonthYearPicker by remember { mutableStateOf(false) }
    var showAddEditScreen by remember { mutableStateOf(false) }
    var showQuickStartInline by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetCategory?>(null) }
    var detailBudgetItem by remember { mutableStateOf<ComputedBudgetItem?>(null) }

    if (showAddEditScreen) {
        AddEditBudgetScreen(
            viewModel = viewModel,
            initialFrequency = selectedFrequency,
            editingBudget = editingBudget,
            onBack = {
                showAddEditScreen = false
                editingBudget = null
            }
        )
        return
    }

    BackHandler { onBack() }

    val filteredBudgets = remember(budgets, selectedFrequency) {
        budgets.filter { it.frequency == selectedFrequency }
    }

    val computedItems = remember(
        filteredBudgets,
        allTransactions,
        wallets,
        categories,
        selectedMonth,
        selectedYear
    ) {
        filteredBudgets.map { b ->
            BudgetMathHelper.computeBudgetItem(
                budget = b,
                allTransactions = allTransactions,
                wallets = wallets,
                allCategories = categories,
                selectedMonth = selectedMonth,
                selectedYear = selectedYear
            )
        }
    }

    val periodInfo = remember(selectedFrequency, selectedMonth, selectedYear) {
        BudgetMathHelper.calculatePeriodInfo(selectedFrequency, selectedMonth, selectedYear)
    }

    val totalAllocated = remember(computedItems) { computedItems.sumOf { it.budget.allocated } }
    val totalSpent = remember(computedItems) { computedItems.sumOf { it.spent } }
    val totalRemaining = (totalAllocated - totalSpent).coerceAtLeast(0.0)
    val totalOver = (totalSpent - totalAllocated).coerceAtLeast(0.0)
    val isTotalOver = totalAllocated > 0 && totalSpent > totalAllocated
    val totalRatio = if (totalAllocated > 0) (totalSpent / totalAllocated).toFloat().coerceIn(0f, 1f) else 0f
    val totalPercentInt = if (totalAllocated > 0) ((totalSpent / totalAllocated) * 100.0).roundToInt().coerceAtLeast(0) else 0
    val remainingPerDay = totalRemaining / periodInfo.daysRemaining.coerceAtLeast(1)

    val summaryStatusColor = when {
        isTotalOver -> ExpenseRed
        totalPercentInt >= settings.budgetWarningPercent && totalAllocated > 0 -> TrophyYellow
        else -> EmeraldBudgetGreen
    }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            Column(
                modifier = Modifier.navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Upper Sparkle "Mulai cepat" FAB matching video 00:52
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0EA5E9), Color(0xFF14B8A6))
                            )
                        )
                        .clickable { showQuickStartInline = !showQuickStartInline }
                        .testTag("btn_budget_quick_start_top")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Mulai cepat anggaran",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        editingBudget = null
                        showAddEditScreen = true
                    },
                    containerColor = AccentBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("fab_add_budget")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Anggaran", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_anggaran")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Anggaran",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDisplaySettingsSheet = true }
                            .testTag("btn_budget_display_settings")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Pengaturan tampilan",
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val summaryText = buildString {
                                    appendLine("Ringkasan Anggaran ($selectedFrequency)")
                                    appendLine("Terpakai: ${CurrencyHelper.formatRupiah(totalSpent)} ($totalPercentInt%)")
                                    appendLine("Total Anggaran: ${CurrencyHelper.formatRupiah(totalAllocated)}")
                                    appendLine("Sisa: ${CurrencyHelper.formatRupiah(totalRemaining)}")
                                    computedItems.forEach { item ->
                                        appendLine("• ${item.budget.displayName}: ${CurrencyHelper.formatRupiah(item.spent)} / ${CurrencyHelper.formatRupiah(item.budget.allocated)} (${item.rawPercentInt}%)")
                                    }
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, summaryText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Bagikan Ringkasan Anggaran"))
                            }
                            .testTag("btn_budget_share")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = "Bagikan Anggaran",
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Month Selector Row (Calendar Picker + Scrollable Month Pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentBlue.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .padding(start = 14.dp, end = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showMonthYearPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pilih Bulan dan Tahun",
                            tint = AccentBlueBright,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = AccentBlueBright,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                val monthScrollState = rememberScrollState()
                LaunchedEffect(monthOptions) {
                    monthScrollState.scrollTo(monthScrollState.maxValue)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(monthScrollState)
                        .padding(end = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    monthOptions.forEach { opt ->
                        val isSelected = selectedMonth == opt.monthIndex && selectedYear == opt.year
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) AccentBlue else DarkSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.selectMonthAndYear(opt.monthIndex, opt.year) }
                        ) {
                            Text(
                                text = opt.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // Frequency Tab Selector
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BudgetMathHelper.frequencyOptions.forEach { freq ->
                        val isSelected = selectedFrequency == freq
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (isSelected) AccentBlue else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { selectedFrequency = freq }
                        ) {
                            Text(
                                text = freq,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextMuted,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSummaryExpanded = !isSummaryExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${periodInfo.daysRemaining} hari tersisa",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                                Icon(
                                    imageVector = if (isSummaryExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Ubah Ringkasan",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            AnimatedVisibility(visible = isSummaryExpanded) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (settings.budgetSummaryStyle == "Bilah") {
                                        // Bilah mode
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Column {
                                                Text("Terpakai sejauh ini", fontSize = 11.5.sp, color = TextMuted)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = CurrencyHelper.formatRupiah(totalSpent),
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = summaryStatusColor
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Total anggaran", fontSize = 11.5.sp, color = TextMuted)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = CurrencyHelper.formatRupiah(totalAllocated),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextWhite
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            LinearProgressIndicator(
                                                progress = { totalRatio },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(12.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                                color = summaryStatusColor,
                                                trackColor = DarkSurfaceVariant,
                                                strokeCap = StrokeCap.Round
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "$totalPercentInt% terpakai",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextWhite
                                            )
                                            Text(
                                                text = "Laju hari ini ${(periodInfo.paceProgress * 100).roundToInt()}%",
                                                fontSize = 11.sp,
                                                color = TextSubtle
                                            )
                                        }
                                    } else {
                                        // Sirkular mode (Default matching video f_02.png)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(116.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    val strokePx = 13.dp.toPx()
                                                    val inset = strokePx / 2f
                                                    val arcSize = Size(size.width - strokePx, size.height - strokePx)

                                                    // Track circle
                                                    drawArc(
                                                        color = Color(0xFF2B2F3A),
                                                        startAngle = -90f,
                                                        sweepAngle = 360f,
                                                        useCenter = false,
                                                        topLeft = Offset(inset, inset),
                                                        size = arcSize,
                                                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                                    )

                                                    // Pace tick mark (elapsed period indicator)
                                                    val paceAngleRad = ((-90f + (periodInfo.paceProgress * 360f)) * PI / 180.0)
                                                    val center = Offset(size.width / 2f, size.height / 2f)
                                                    val rInner = (size.width / 2f) - strokePx * 0.9f
                                                    val rOuter = (size.width / 2f) - strokePx * 0.1f
                                                    drawLine(
                                                        color = TextMuted.copy(alpha = 0.65f),
                                                        start = Offset(
                                                            x = center.x + (rInner * cos(paceAngleRad)).toFloat(),
                                                            y = center.y + (rInner * sin(paceAngleRad)).toFloat()
                                                        ),
                                                        end = Offset(
                                                            x = center.x + (rOuter * cos(paceAngleRad)).toFloat(),
                                                            y = center.y + (rOuter * sin(paceAngleRad)).toFloat()
                                                        ),
                                                        strokeWidth = 2.dp.toPx(),
                                                        cap = StrokeCap.Round
                                                    )

                                                    // Active spent arc
                                                    if (totalRatio > 0f) {
                                                        drawArc(
                                                            color = summaryStatusColor,
                                                            startAngle = -90f,
                                                            sweepAngle = (360f * totalRatio).coerceAtLeast(6f),
                                                            useCenter = false,
                                                            topLeft = Offset(inset, inset),
                                                            size = arcSize,
                                                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                                        )
                                                    }
                                                }

                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "$totalPercentInt%",
                                                        fontSize = 19.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextWhite
                                                    )
                                                    Text(
                                                        text = "Terpakai sejauh ini",
                                                        fontSize = 8.5.sp,
                                                        color = TextMuted,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(18.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Terpakai sejauh ini",
                                                    fontSize = 12.sp,
                                                    color = TextMuted
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = CurrencyHelper.formatRupiah(totalSpent),
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = summaryStatusColor
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // 3 Column Metrics
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Total anggaran", fontSize = 10.5.sp, color = TextMuted)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = CurrencyHelper.formatRupiah(totalAllocated),
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isTotalOver) "Kelebihan" else "Sisa",
                                                fontSize = 10.5.sp,
                                                color = TextMuted
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = CurrencyHelper.formatRupiah(if (isTotalOver) totalOver else totalRemaining),
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isTotalOver) ExpenseRed else TextWhite
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Sisa per hari", fontSize = 10.5.sp, color = TextMuted)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = CurrencyHelper.formatRupiah(if (isTotalOver) 0.0 else remainingPerDay),
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Status Motivation Text
                                    val statusMessage = buildAnnotatedString {
                                        when {
                                            totalAllocated <= 0.0 -> {
                                                withStyle(SpanStyle(color = TextMuted)) {
                                                    append("Tambahkan anggaran $selectedFrequency untuk mulai memantau pengeluaran.")
                                                }
                                            }
                                            isTotalOver -> {
                                                withStyle(SpanStyle(color = TextWhite)) {
                                                    append("Melebihi anggaran sebesar ")
                                                }
                                                withStyle(SpanStyle(color = ExpenseRed, fontWeight = FontWeight.SemiBold)) {
                                                    append("${CurrencyHelper.formatRupiah(totalOver)}.")
                                                }
                                            }
                                            totalPercentInt >= settings.budgetWarningPercent -> {
                                                withStyle(SpanStyle(color = TextWhite)) {
                                                    append("Mendekati batas! Tersisa ")
                                                }
                                                withStyle(SpanStyle(color = TrophyYellow, fontWeight = FontWeight.SemiBold)) {
                                                    append("${CurrencyHelper.formatRupiah(totalRemaining)}.")
                                                }
                                            }
                                            else -> {
                                                withStyle(SpanStyle(color = TextWhite)) {
                                                    append("Bagus! Anda memiliki ")
                                                }
                                                withStyle(SpanStyle(color = EmeraldBudgetGreen, fontWeight = FontWeight.SemiBold)) {
                                                    append("${CurrencyHelper.formatRupiah(totalRemaining)} tersisa.")
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = statusMessage,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Inline Expandable Blue-Gradient "Mulai cepat" Card (Video 00:52–01:01)
                if (showQuickStartInline) {
                    item(key = "INLINE_QUICK_START") {
                        InlineMulaiCepatCard(
                            frequency = selectedFrequency,
                            availableCategories = categories.filter { it.type == TransactionType.EXPENSE && it.isVisible }.map { it.name },
                            onApply = { createdBudgets ->
                                createdBudgets.forEach { viewModel.addBudget(it) }
                                showQuickStartInline = false
                            },
                            onClose = { showQuickStartInline = false }
                        )
                    }
                }

                // Budget List / Empty State
                if (computedItems.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 34.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AccentBlue.copy(alpha = 0.14f),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PieChart,
                                            contentDescription = null,
                                            tint = AccentBlueBright,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Belum Ada Anggaran $selectedFrequency",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tetapkan batas pengeluaran per kategori agar arus kas tetap terkendali.",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showQuickStartInline = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_budget_quick_start")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mulai Cepat (Preset Otomatis)", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        editingBudget = null
                                        showAddEditScreen = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, DarkCardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_budget_create_manual")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextWhite)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Buat Anggaran Baru", fontWeight = FontWeight.Bold, color = TextWhite)
                                }
                            }
                        }
                    }
                } else if (settings.budgetListStyle == "Kisi") {
                    // Kisi (Spacious cards mode)
                    itemsIndexed(computedItems, key = { _, it -> it.budget.id }) { _, item ->
                        BudgetGridCard(
                            item = item,
                            warningPercent = settings.budgetWarningPercent,
                            onClick = { detailBudgetItem = item }
                        )
                    }
                } else {
                    // Tabel (Compact spreadsheet rows card matching f_02.png)
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Table Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Nama",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        modifier = Modifier.weight(1.35f)
                                    )
                                    Text(
                                        text = "Terpakai",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.95f)
                                    )
                                    Text(
                                        text = "Jumlah",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1.05f)
                                    )
                                    Text(
                                        text = "%",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(46.dp)
                                    )
                                }

                                computedItems.forEachIndexed { idx, item ->
                                    if (idx > 0) {
                                        HorizontalDivider(
                                            color = DarkCardBorder.copy(alpha = 0.35f),
                                            modifier = Modifier.padding(horizontal = 14.dp)
                                        )
                                    }
                                    BudgetTableRow(
                                        item = item,
                                        paceProgress = periodInfo.paceProgress,
                                        warningPercent = settings.budgetWarningPercent,
                                        onClick = { detailBudgetItem = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDisplaySettingsSheet) {
        PengaturanTampilanAnggaranSheet(
            currentSummaryStyle = settings.budgetSummaryStyle,
            currentListStyle = settings.budgetListStyle,
            onSelectSummaryStyle = { style ->
                viewModel.updateBudgetDisplayStyle(style, settings.budgetListStyle)
            },
            onSelectListStyle = { style ->
                viewModel.updateBudgetDisplayStyle(settings.budgetSummaryStyle, style)
            },
            onDismiss = { showDisplaySettingsSheet = false }
        )
    }

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            currentMonth = selectedMonth,
            currentYear = selectedYear,
            onDismiss = { showMonthYearPicker = false },
            onSelectMonthYear = { month, year ->
                viewModel.selectMonthAndYear(month, year)
                showMonthYearPicker = false
            }
        )
    }

    detailBudgetItem?.let { currentItem ->
        BudgetDetailSheet(
            item = currentItem,
            daysRemaining = periodInfo.daysRemaining,
            warningPercent = settings.budgetWarningPercent,
            onEdit = {
                editingBudget = currentItem.budget
                detailBudgetItem = null
                showAddEditScreen = true
            },
            onDelete = {
                viewModel.deleteBudget(currentItem.budget.id)
                detailBudgetItem = null
            },
            onDismiss = { detailBudgetItem = null }
        )
    }
}

private data class BudgetQuickPreset(
    val id: String,
    val title: String,
    val billsPct: Int,
    val foodPct: Int,
    val transportPct: Int,
    val lifestylePct: Int
) {
    val savingsPct: Int get() = (100 - billsPct - foodPct - transportPct - lifestylePct).coerceAtLeast(0)
}

@Composable
private fun InlineMulaiCepatCard(
    frequency: String,
    availableCategories: List<String>,
    onApply: (List<BudgetCategory>) -> Unit,
    onClose: () -> Unit
) {
    var incomeText by remember { mutableStateOf("5000000") }
    val presets = remember {
        listOf(
            BudgetQuickPreset(
                id = "saver",
                title = "Penabung",
                billsPct = 28,
                foodPct = 15,
                transportPct = 9,
                lifestylePct = 8
            ),
            BudgetQuickPreset(
                id = "balanced",
                title = "Seimbang",
                billsPct = 30,
                foodPct = 18,
                transportPct = 10,
                lifestylePct = 12
            ),
            BudgetQuickPreset(
                id = "flexible",
                title = "Fleksibel",
                billsPct = 28,
                foodPct = 22,
                transportPct = 12,
                lifestylePct = 18
            )
        )
    }
    var selectedPresetId by remember { mutableStateOf("balanced") }
    val selectedPreset = presets.first { it.id == selectedPresetId }
    val parsedBaseAmount = incomeText.filter { it.isDigit() }.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val resolveCat: (List<String>, String) -> String = { candidates, fallback ->
        availableCategories.firstOrNull { c -> candidates.any { it.equals(c, ignoreCase = true) } } ?: fallback
    }
    val billsCat = remember(availableCategories) { resolveCat(listOf("Bills & Fees", "Tagihan & Utilitas"), "Bills & Fees") }
    val foodCat = remember(availableCategories) { resolveCat(listOf("Food & Drink", "Food & Dining", "Makanan & Minuman"), "Food & Drink") }
    val transportCat = remember(availableCategories) { resolveCat(listOf("Transport", "Transportasi"), "Transport") }
    val lifestyleCat = remember(availableCategories) { resolveCat(listOf("Shopping", "Entertainment", "Hiburan", "Belanja"), "Shopping") }

    val billsAmount = parsedBaseAmount * (selectedPreset.billsPct / 100.0)
    val foodAmount = parsedBaseAmount * (selectedPreset.foodPct / 100.0)
    val transportAmount = parsedBaseAmount * (selectedPreset.transportPct / 100.0)
    val lifestyleAmount = parsedBaseAmount * (selectedPreset.lifestylePct / 100.0)
    val savedForGoals = parsedBaseAmount * (selectedPreset.savingsPct / 100.0)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_inline_quick_start")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0E2A47), Color(0xFF0B1F36))
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF7DD3FC), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Mulai cepat", fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Beri tahu pemasukan bulananmu untuk merancang alokasi", fontSize = 11.sp, color = Color(0xFFBAE6FD))
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup Mulai Cepat", tint = Color(0xFFBAE6FD), modifier = Modifier.size(18.dp))
                }
            }

            OutlinedTextField(
                value = incomeText,
                onValueChange = { raw -> incomeText = raw.filter { it.isDigit() }.take(12) },
                label = { Text("Pendapatan Bulanan", color = Color(0xFFBAE6FD)) },
                prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = Color.White) },
                singleLine = true,
                visualTransformation = ThousandsSeparatorVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF38BDF8).copy(alpha = 0.4f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF091A2E),
                    unfocusedContainerColor = Color(0xFF091A2E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_quickstart_income")
            )

            // Segmented style selector: [Penabung | Seimbang | Fleksibel]
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF091A2E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presets.forEach { preset ->
                        val sel = preset.id == selectedPresetId
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (sel) AccentBlue else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { selectedPresetId = preset.id }
                                .testTag("preset_option_${preset.id}")
                        ) {
                            Text(
                                text = preset.title,
                                fontSize = 12.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                color = if (sel) Color.White else Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Live Category Rows
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF091A2E).copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickStartCategoryLine(billsCat, billsAmount)
                    QuickStartCategoryLine(foodCat, foodAmount)
                    QuickStartCategoryLine(transportCat, transportAmount)
                    QuickStartCategoryLine(lifestyleCat, lifestyleAmount)
                }
            }

            // Green sparkle savings callout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EmeraldBudgetGreen.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldBudgetGreen, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Menyisihkan ${CurrencyHelper.formatRupiah(savedForGoals)} (${selectedPreset.savingsPct}%) untuk menabung & tujuan – sesuaikan apa saja.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFA7F3D0),
                    lineHeight = 15.sp
                )
            }

            Button(
                onClick = {
                    val created = listOf(
                        BudgetCategory(
                            category = billsCat,
                            name = billsCat,
                            allocated = billsAmount,
                            categories = listOf(billsCat),
                            frequency = frequency
                        ),
                        BudgetCategory(
                            category = foodCat,
                            name = foodCat,
                            allocated = foodAmount,
                            categories = listOf(foodCat),
                            frequency = frequency
                        ),
                        BudgetCategory(
                            category = transportCat,
                            name = transportCat,
                            allocated = transportAmount,
                            categories = listOf(transportCat),
                            frequency = frequency
                        ),
                        BudgetCategory(
                            category = lifestyleCat,
                            name = lifestyleCat,
                            allocated = lifestyleAmount,
                            categories = listOf(lifestyleCat),
                            frequency = frequency
                        )
                    )
                    onApply(created)
                },
                enabled = parsedBaseAmount > 0.0,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_apply_quickstart_budget")
            ) {
                Text("Buat anggaran ini", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp), tint = Color.White)
            }
        }
    }
}

@Composable
private fun QuickStartCategoryLine(categoryName: String, amount: Double) {
    val catColor = CategoryIconHelper.getCategoryColor(categoryName, TransactionType.EXPENSE)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(catColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconHelper.getCategoryIcon(categoryName),
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(categoryName, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
        }
        Text(CurrencyHelper.formatRupiah(amount), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextWhite)
    }
}

@Composable
private fun BudgetTableRow(
    item: ComputedBudgetItem,
    paceProgress: Float,
    warningPercent: Int,
    onClick: () -> Unit
) {
    val budget = item.budget
    val primaryCat = budget.targetCategories.firstOrNull() ?: budget.category
    val catColor = CategoryIconHelper.getCategoryColor(primaryCat, TransactionType.EXPENSE)
    val isOver = budget.allocated > 0 && item.spent > budget.allocated
    val isWarning = !isOver && item.rawPercentInt >= warningPercent
    val ringColor = when {
        isOver -> ExpenseRed
        isWarning -> TrophyYellow
        else -> EmeraldBudgetGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1.35f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = catColor.copy(alpha = 0.16f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = CategoryIconHelper.getCategoryIcon(primaryCat),
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = budget.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = if (item.spent > 0) CurrencyHelper.formatRupiah(item.spent) else "–",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isOver) ExpenseRed else if (item.spent > 0) TextWhite else TextMuted,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(0.95f)
        )

        Text(
            text = CurrencyHelper.formatRupiah(budget.allocated),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextWhite,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(1.05f)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 3.5.dp.toPx()
                val inset = strokePx / 2f
                val arcSize = Size(size.width - strokePx, size.height - strokePx)

                drawArc(
                    color = Color(0xFF2B2F3A),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                val paceAngleRad = ((-90f + (paceProgress * 360f)) * PI / 180.0)
                val center = Offset(size.width / 2f, size.height / 2f)
                val rInner = (size.width / 2f) - strokePx * 1.1f
                val rOuter = (size.width / 2f)
                drawLine(
                    color = TextMuted.copy(alpha = 0.5f),
                    start = Offset(
                        x = center.x + (rInner * cos(paceAngleRad)).toFloat(),
                        y = center.y + (rInner * sin(paceAngleRad)).toFloat()
                    ),
                    end = Offset(
                        x = center.x + (rOuter * cos(paceAngleRad)).toFloat(),
                        y = center.y + (rOuter * sin(paceAngleRad)).toFloat()
                    ),
                    strokeWidth = 1.5.dp.toPx()
                )

                if (item.percentage > 0f) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = (360f * item.percentage).coerceAtLeast(8f),
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                text = "${item.rawPercentInt}",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isOver) ExpenseRed else if (item.rawPercentInt > 0) EmeraldBudgetGreen else TextMuted
            )
        }
    }
}

@Composable
private fun BudgetGridCard(
    item: ComputedBudgetItem,
    warningPercent: Int,
    onClick: () -> Unit
) {
    val budget = item.budget
    val primaryCat = budget.targetCategories.firstOrNull() ?: budget.category
    val catColor = CategoryIconHelper.getCategoryColor(primaryCat, TransactionType.EXPENSE)
    val isOver = budget.allocated > 0 && item.spent > budget.allocated
    val isWarning = !isOver && item.rawPercentInt >= warningPercent
    val barColor = when {
        isOver -> ExpenseRed
        isWarning -> TrophyYellow
        else -> EmeraldBudgetGreen
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = catColor.copy(alpha = 0.16f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = CategoryIconHelper.getCategoryIcon(primaryCat),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = budget.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val txCount = item.matchingTransactions.size
                        val subtitle = if (txCount > 0) {
                            if (isOver) "Melebihi ${CurrencyHelper.formatRupiah(item.overAmount)} · $txCount transaksi"
                            else "${CurrencyHelper.formatRupiah(item.remaining)} tersisa · $txCount transaksi"
                        } else {
                            "${CurrencyHelper.formatRupiah(item.remaining)} tersisa"
                        }
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = if (isOver) ExpenseRed else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyHelper.formatRupiah(item.spent),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOver) ExpenseRed else TextWhite
                    )
                    Text(
                        text = "Dari ${CurrencyHelper.formatRupiah(budget.allocated)}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { item.percentage },
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = barColor,
                    trackColor = DarkSurfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${item.rawPercentInt}%",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.rawPercentInt > 0) barColor else TextMuted
                )
            }
        }
    }
}
