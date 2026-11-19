package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SavingsGoal
import com.example.ui.dialogs.AddEditSavingsGoalFullScreenDialog
import com.example.ui.dialogs.MonthYearPickerDialog
import com.example.ui.dialogs.SavingsGoalTemplate
import com.example.ui.dialogs.SavingsTemplatePickerSheet
import com.example.ui.dialogs.formatSavingsReadableDate
import com.example.ui.dialogs.savingsIconFromName
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CurrencyHelper

private fun nextMilestoneLabel(goal: SavingsGoal): String {
    val pct = goal.progressPercent
    return when {
        pct >= 100 || goal.currentAmount >= goal.targetAmount -> "Target tercapai!"
        pct >= 75 -> "Pencapaian berikutnya di 100%"
        pct >= 50 -> "Pencapaian berikutnya di 75%"
        pct >= 25 -> "Pencapaian berikutnya di 50%"
        else -> "Pencapaian berikutnya di 25%"
    }
}

@Composable
fun TabunganScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val monthOptions by viewModel.monthOptions.collectAsState()
    val selectedMonthIndex by viewModel.selectedMonthIndex.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showMonthYearPicker by remember { mutableStateOf(false) }
    var showTemplatePickerSheet by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<SavingsGoalTemplate?>(null) }
    var showFullCreateEdit by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var detailGoalId by remember { mutableStateOf<String?>(null) }

    val totalSaved = remember(savingsGoals) { savingsGoals.sumOf { it.currentAmount } }
    val totalTarget = remember(savingsGoals) { savingsGoals.sumOf { it.targetAmount } }
    val activeGoals = remember(savingsGoals) { savingsGoals.filter { !it.isCompleted } }
    val completedGoals = remember(savingsGoals) { savingsGoals.filter { it.isCompleted } }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingGoal = null
                    selectedTemplate = null
                    showTemplatePickerSheet = true
                },
                containerColor = AccentBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("fab_add_savings_goal")
                    .size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Tujuan", tint = Color.White)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_tabungan")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextWhite
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Tabungan",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            // Month Selector Row
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
                        .testTag("btn_tabungan_month_picker")
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
                        val isSelected = opt.monthIndex == selectedMonthIndex && opt.year == selectedYear
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

            Spacer(modifier = Modifier.height(10.dp))

            if (savingsGoals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_tabungan_empty),
                            contentDescription = "Ilustrasi Tabungan",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(152.dp)
                                .clip(RoundedCornerShape(32.dp))
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            text = "Belum Ada Tujuan",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Namai jumlah yang Anda tabung — dana darurat, rumah, perjalanan — lalu catat setiap setoran saat terjadi.",
                            fontSize = 13.5.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    // TOTAL DITABUNG Hero Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF0F3D2E),
                                            Color(0xFF0A2920),
                                            DarkSurface
                                        )
                                    )
                                )
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_tabungan_piggy),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(134.dp)
                                    .alpha(0.35f)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Text(
                                    text = "TOTAL DITABUNG",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen,
                                    letterSpacing = 0.7.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = CurrencyHelper.formatRupiah(totalSaved),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "dari ${CurrencyHelper.formatRupiah(totalTarget)} untuk ${savingsGoals.size} tujuan",
                                    fontSize = 12.5.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Menabung dari saat gajian Card
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
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(IncomeGreenSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Savings, null, tint = IncomeGreen, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Menabung dari saat gajian",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Saat gaji Anda masuk, tentukan untuk menyisihkan ${settings.paydayAutoSavePercent}% ke tujuan sebelum Anda membelanjakannya.",
                                            fontSize = 11.5.sp,
                                            color = TextMuted,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = settings.paydayAutoSaveEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.updatePaydayAutoSave(enabled, settings.paydayAutoSavePercent)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = IncomeGreen
                                    ),
                                    modifier = Modifier.testTag("switch_payday_autosave")
                                )
                            }
                        }
                    }

                    if (activeGoals.isNotEmpty()) {
                        item {
                            Text(
                                text = "AKTIF ${activeGoals.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        items(activeGoals, key = { it.id }) { goal ->
                            TabunganGoalCard(
                                goal = goal,
                                onClick = { detailGoalId = goal.id }
                            )
                        }
                    }

                    if (completedGoals.isNotEmpty()) {
                        item {
                            Text(
                                text = "SELESAI ${completedGoals.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                            )
                        }
                        items(completedGoals, key = { it.id }) { goal ->
                            TabunganGoalCard(
                                goal = goal,
                                onClick = { detailGoalId = goal.id }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            currentMonth = selectedMonthIndex,
            currentYear = selectedYear,
            onDismiss = { showMonthYearPicker = false },
            onSelectMonthYear = { m, y ->
                viewModel.selectMonthAndYear(m, y)
                showMonthYearPicker = false
            }
        )
    }

    if (showTemplatePickerSheet) {
        SavingsTemplatePickerSheet(
            onContinue = { tpl ->
                showTemplatePickerSheet = false
                selectedTemplate = tpl
                editingGoal = null
                showFullCreateEdit = true
            },
            onDismiss = { showTemplatePickerSheet = false }
        )
    }

    if (showFullCreateEdit) {
        AddEditSavingsGoalFullScreenDialog(
            editingGoal = editingGoal,
            initialTemplate = selectedTemplate,
            onSave = { saved ->
                if (editingGoal == null) {
                    viewModel.addSavingsGoal(saved)
                } else {
                    viewModel.updateSavingsGoal(saved)
                }
                showFullCreateEdit = false
                editingGoal = null
                selectedTemplate = null
            },
            onDismiss = {
                showFullCreateEdit = false
                editingGoal = null
                selectedTemplate = null
            }
        )
    }

    detailGoalId?.let { id ->
        TabunganDetailScreen(
            goalId = id,
            viewModel = viewModel,
            onEdit = { target ->
                editingGoal = target
                selectedTemplate = null
                showFullCreateEdit = true
            },
            onDismiss = { detailGoalId = null }
        )
    }
}

@Composable
private fun TabunganGoalCard(
    goal: SavingsGoal,
    onClick: () -> Unit
) {
    val goalColor = Color(goal.colorHex)
    val subtitleText = remember(goal.records, goal.currentAmount) {
        when {
            goal.records.isEmpty() && goal.currentAmount <= 0.0 -> "Belum ada yang ditabung"
            goal.records.isNotEmpty() -> {
                val last = goal.records.last()
                "${goal.records.size} catatan · Terakhir ${formatSavingsReadableDate(last.dateStr)}"
            }
            else -> "Terkumpul ${CurrencyHelper.formatRupiah(goal.currentAmount)}"
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("card_savings_goal_${goal.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = goalColor.copy(alpha = 0.16f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = savingsIconFromName(goal.iconName),
                                contentDescription = null,
                                tint = goalColor,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = goal.name,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitleText,
                            fontSize = 11.5.sp,
                            color = TextSubtle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${goal.progressPercent}%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = goalColor
                    )
                    if (goal.targetDate.isNotBlank()) {
                        Text(
                            text = formatSavingsReadableDate(goal.targetDate),
                            fontSize = 11.sp,
                            color = TextSubtle
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (goal.progressPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = goalColor,
                trackColor = DarkSurfaceElevated,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nextMilestoneLabel(goal),
                    fontSize = 11.5.sp,
                    color = if (goal.progressPercent >= 100) IncomeGreen else TextMuted,
                    fontWeight = if (goal.progressPercent >= 100) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = "${CurrencyHelper.formatRupiah(goal.currentAmount)} dari ${CurrencyHelper.formatRupiah(goal.targetAmount)}",
                    fontSize = 11.5.sp,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
