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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.data.model.EventItem
import com.example.data.model.TransactionType
import com.example.ui.dialogs.AddEditEventFullScreenDialog
import com.example.ui.dialogs.EventTemplatePickerSheet
import com.example.ui.dialogs.EventTypeTemplate
import com.example.ui.dialogs.MonthYearPickerDialog
import com.example.ui.dialogs.formatEventReadableDate
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CurrencyHelper

private val AcaraShortMonths = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

@Composable
fun AcaraScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    onNavigateToTransactions: (eventLabel: String) -> Unit,
    onAddTransactionWithEvent: (eventLabel: String) -> Unit
) {
    BackHandler { onBack() }

    val monthOptions by viewModel.monthOptions.collectAsState()
    val selectedMonthIndex by viewModel.selectedMonthIndex.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val events by viewModel.events.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    var showMonthYearPicker by remember { mutableStateOf(false) }
    var showTemplatePickerSheet by remember { mutableStateOf(false) }
    var selectedTemplateForCreate by remember { mutableStateOf<EventTypeTemplate?>(null) }
    var showFullCreateEdit by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventItem?>(null) }
    var detailEventId by remember { mutableStateOf<String?>(null) }

    val activeEvents = remember(events) { events.filter { !it.isCompleted } }
    val completedEvents = remember(events) { events.filter { it.isCompleted } }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEvent = null
                    selectedTemplateForCreate = null
                    showTemplatePickerSheet = true
                },
                containerColor = AccentBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .size(56.dp)
                    .testTag("fab_add_event")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Acara", tint = Color.White)
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
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_acara")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextWhite
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Acara",
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
                        .testTag("btn_acara_month_picker")
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

            if (events.isEmpty()) {
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
                            painter = painterResource(id = R.drawable.img_acara_empty),
                            contentDescription = "Ilustrasi Acara",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(152.dp)
                                .clip(RoundedCornerShape(32.dp))
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            text = "Belum Ada Acara",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Perjalanan, pernikahan, renovasi — buat acara dan setiap transaksi yang Anda tautkan dihitung di satu tempat.",
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
                    if (activeEvents.isNotEmpty()) {
                        item {
                            Text(
                                text = "AKTIF ${activeEvents.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        items(activeEvents, key = { it.id }) { event ->
                            AcaraGradientCard(
                                event = event,
                                allTransactions = allTransactions,
                                onClick = { detailEventId = event.id }
                            )
                        }
                    }

                    if (completedEvents.isNotEmpty()) {
                        item {
                            Text(
                                text = "SELESAI ${completedEvents.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                            )
                        }
                        items(completedEvents, key = { it.id }) { event ->
                            AcaraGradientCard(
                                event = event,
                                allTransactions = allTransactions,
                                onClick = { detailEventId = event.id }
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
        EventTemplatePickerSheet(
            onContinue = { template ->
                showTemplatePickerSheet = false
                selectedTemplateForCreate = template
                editingEvent = null
                showFullCreateEdit = true
            },
            onDismiss = { showTemplatePickerSheet = false }
        )
    }

    if (showFullCreateEdit) {
        AddEditEventFullScreenDialog(
            viewModel = viewModel,
            editingEvent = editingEvent,
            initialTemplate = selectedTemplateForCreate,
            defaultMonthIndex = selectedMonthIndex,
            defaultYear = selectedYear,
            onSave = { saved ->
                if (editingEvent == null) {
                    viewModel.addEvent(saved)
                } else {
                    viewModel.updateEvent(saved)
                }
                showFullCreateEdit = false
                editingEvent = null
                selectedTemplateForCreate = null
            },
            onDismiss = {
                showFullCreateEdit = false
                editingEvent = null
                selectedTemplateForCreate = null
            }
        )
    }

    detailEventId?.let { id ->
        AcaraDetailScreen(
            eventId = id,
            viewModel = viewModel,
            onEdit = { target ->
                editingEvent = target
                selectedTemplateForCreate = null
                showFullCreateEdit = true
            },
            onDismiss = { detailEventId = null }
        )
    }
}

@Composable
private fun AcaraGradientCard(
    event: EventItem,
    allTransactions: List<com.example.data.model.Transaction>,
    onClick: () -> Unit
) {
    val txs = remember(allTransactions, event.name) {
        allTransactions.filter { tx ->
            tx.label.split(",").map { it.trim().lowercase() }.contains(event.name.lowercase()) ||
                tx.label.equals(event.name, ignoreCase = true)
        }
    }
    val totalExpense = remember(txs) { txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } }
    val eventColor = Color(event.colorHex)
    val daysElapsed = remember(event) { computeEventDaysElapsed(event) }
    val startLabel = remember(event) {
        if (event.startDate.isNotBlank()) formatEventReadableDate(event.startDate)
        else "${AcaraShortMonths.getOrElse(event.monthIndex - 1) { "Nov" }} ${event.year}"
    }
    val hasBudget = event.targetBudget > 0.0
    val progress = if (hasBudget) (totalExpense / event.targetBudget).toFloat().coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        eventColor.copy(alpha = if (event.isCompleted) 0.45f else 0.88f),
                        eventColor.copy(alpha = if (event.isCompleted) 0.25f else 0.48f),
                        DarkSurface
                    )
                )
            )
            .clickable(onClick = onClick)
            .testTag("card_event_${event.id}")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_acara_travel),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(150.dp)
                .alpha(if (event.isCompleted) 0.18f else 0.34f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.35f)
            ) {
                Text(
                    text = if (event.isCompleted) "SELESAI" else "HARI $daysElapsed",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = event.name,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${event.eventType} · Dari $startLabel · ${txs.size} entri",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = CurrencyHelper.formatRupiah(totalExpense),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (hasBudget) {
                        "dari ${CurrencyHelper.formatRupiah(event.targetBudget)}"
                    } else {
                        "terpakai"
                    },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            if (hasBudget) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (totalExpense > event.targetBudget) ExpenseRed else Color.White,
                    trackColor = Color.Black.copy(alpha = 0.25f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}
