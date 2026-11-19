package com.example.ui.dialogs

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.EventItem
import com.example.data.model.EventPlannedExpense
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CategoryIconHelper
import com.example.util.CurrencyHelper
import com.example.util.ThousandsSeparatorVisualTransformation
import java.util.Calendar
import java.util.Locale
import java.util.UUID

fun eventIconFromName(iconName: String): ImageVector {
    return when (iconName) {
        "Flight" -> Icons.Default.Flight
        "Favorite" -> Icons.Default.Favorite
        "Home" -> Icons.Default.Home
        "School" -> Icons.Default.School
        "CardGiftcard" -> Icons.Default.CardGiftcard
        "ShoppingBag" -> Icons.Default.ShoppingBag
        "Work" -> Icons.Default.Work
        "Event" -> Icons.Default.Event
        else -> Icons.Default.Celebration
    }
}

data class EventTypeTemplate(
    val type: String,
    val iconName: String,
    val defaultColorHex: Long,
    val defaultPlannedExpenses: List<Pair<String, String>> // Pair(name, category)
)

val EventTypeTemplates = listOf(
    EventTypeTemplate(
        type = "Perjalanan",
        iconName = "Flight",
        defaultColorHex = 0xFF3B82F6L,
        defaultPlannedExpenses = listOf(
            "Transportasi" to "Transport",
            "Menginap" to "Bills & Fees",
            "Makanan" to "Food & Dining",
            "Kegiatan" to "Hiburan",
            "Belanja" to "Shopping"
        )
    ),
    EventTypeTemplate(
        type = "Musim liburan",
        iconName = "CardGiftcard",
        defaultColorHex = 0xFF10B981L,
        defaultPlannedExpenses = listOf(
            "Hadiah & Kado" to "Shopping",
            "Makan Bersama" to "Food & Dining",
            "Transportasi" to "Transport",
            "Dekorasi" to "Other"
        )
    ),
    EventTypeTemplate(
        type = "Perayaan",
        iconName = "Celebration",
        defaultColorHex = 0xFFEC4899L,
        defaultPlannedExpenses = listOf(
            "Sewa Tempat" to "Bills & Fees",
            "Katering" to "Food & Dining",
            "Dekorasi" to "Shopping",
            "Dokumentasi" to "Hiburan"
        )
    ),
    EventTypeTemplate(
        type = "Bisnis",
        iconName = "Work",
        defaultColorHex = 0xFF6366F1L,
        defaultPlannedExpenses = listOf(
            "Tiket & Transport" to "Transport",
            "Akomodasi" to "Bills & Fees",
            "Jamuan Klien" to "Food & Dining"
        )
    ),
    EventTypeTemplate(
        type = "Renovasi",
        iconName = "Home",
        defaultColorHex = 0xFFF59E0BL,
        defaultPlannedExpenses = listOf(
            "Material Bangunan" to "Shopping",
            "Jasa Tukang" to "Bills & Fees",
            "Perabotan" to "Other"
        )
    ),
    EventTypeTemplate(
        type = "Belajar",
        iconName = "School",
        defaultColorHex = 0xFF8B5CF6L,
        defaultPlannedExpenses = listOf(
            "Biaya Kursus" to "Pendidikan",
            "Buku & Materi" to "Pendidikan",
            "Sertifikasi" to "Pendidikan"
        )
    ),
    EventTypeTemplate(
        type = "Kustom",
        iconName = "Favorite",
        defaultColorHex = 0xFF06B6D4L,
        defaultPlannedExpenses = emptyList()
    )
)

private val EventColorPalette12 = listOf(
    0xFF3B82F6L, 0xFF2563EBL, 0xFF10B981L, 0xFF059669L,
    0xFFF59E0BL, 0xFFEA580CL, 0xFFEC4899L, 0xFFEF4444L,
    0xFF8B5CF6L, 0xFF6366F1L, 0xFF06B6D4L, 0xFF14B8A6L
)

private val EventShortMonths = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
)

private fun parseSlashDate(dateStr: String): Triple<Int, Int, Int>? {
    val parts = dateStr.trim().split("/")
    if (parts.size != 3) return null
    val d = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val y = parts[2].toIntOrNull() ?: return null
    return Triple(d, m, y)
}

fun formatEventReadableDate(dateStr: String): String {
    val parsed = parseSlashDate(dateStr) ?: return dateStr.ifBlank { "-" }
    val mShort = EventShortMonths.getOrElse(parsed.second - 1) { "Nov" }
    return "${parsed.first} $mShort ${parsed.third}"
}

private fun addDaysToSlashDate(startStr: String, daysToAdd: Int): String {
    val parsed = parseSlashDate(startStr) ?: return ""
    val cal = Calendar.getInstance().apply {
        set(parsed.third, parsed.second - 1, parsed.first, 0, 0, 0)
        add(Calendar.DAY_OF_MONTH, daysToAdd)
    }
    return String.format(
        Locale.getDefault(),
        "%02d/%02d/%04d",
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.YEAR)
    )
}

private data class PlannedExpenseDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "Other",
    val subcategory: String = "",
    val amountText: String = "",
    val isPaid: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTemplatePickerSheet(
    onContinue: (EventTypeTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTemplate by remember { mutableStateOf(EventTypeTemplates.first()) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Acara baru",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Apa yang Anda rencanakan? Kami akan mengisi rincian yang biasa.",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val rows = EventTypeTemplates.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { tpl ->
                            val isSelected = selectedTemplate.type == tpl.type
                            val tplColor = Color(tpl.defaultColorHex)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) tplColor.copy(alpha = 0.2f) else DarkSurfaceElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) tplColor else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(86.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedTemplate = tpl }
                                    .testTag("tpl_event_${tpl.type}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(tplColor.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = eventIconFromName(tpl.iconName),
                                            contentDescription = null,
                                            tint = tplColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = tpl.type,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) TextWhite else TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onContinue(selectedTemplate) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_continue_event_template")
            ) {
                Text(
                    text = "Lanjutkan",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddEditEventFullScreenDialog(
    viewModel: FinanceViewModel,
    editingEvent: EventItem? = null,
    initialTemplate: EventTypeTemplate? = null,
    defaultMonthIndex: Int,
    defaultYear: Int,
    onSave: (EventItem) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val allCategories by viewModel.categories.collectAsState()
    val todayCal = remember { Calendar.getInstance() }
    val todaySlash = remember {
        String.format(
            Locale.getDefault(),
            "%02d/%02d/%04d",
            todayCal.get(Calendar.DAY_OF_MONTH),
            todayCal.get(Calendar.MONTH) + 1,
            todayCal.get(Calendar.YEAR)
        )
    }

    var eventType by remember(editingEvent, initialTemplate) {
        mutableStateOf(editingEvent?.eventType ?: initialTemplate?.type ?: "Perjalanan")
    }
    var name by remember(editingEvent) { mutableStateOf(editingEvent?.name ?: "") }
    var selectedIcon by remember(editingEvent, initialTemplate) {
        mutableStateOf(editingEvent?.iconName ?: initialTemplate?.iconName ?: "Flight")
    }
    var selectedColor by remember(editingEvent, initialTemplate) {
        mutableLongStateOf(editingEvent?.colorHex ?: initialTemplate?.defaultColorHex ?: 0xFF3B82F6L)
    }
    var startDate by remember(editingEvent) {
        mutableStateOf(editingEvent?.startDate?.ifBlank { todaySlash } ?: todaySlash)
    }
    var endDate by remember(editingEvent) {
        mutableStateOf(editingEvent?.endDate ?: "")
    }
    var selectedDurationChip by remember(editingEvent) {
        mutableStateOf(if (editingEvent != null && editingEvent.endDate.isNotBlank()) "Custom" else "Terbuka")
    }
    var budgetText by remember(editingEvent) {
        mutableStateOf(
            if (editingEvent != null && editingEvent.targetBudget > 0.0) {
                editingEvent.targetBudget.toLong().toString()
            } else ""
        )
    }
    var note by remember(editingEvent) { mutableStateOf(editingEvent?.note ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val plannedDrafts = remember(editingEvent, initialTemplate) {
        mutableStateListOf<PlannedExpenseDraft>().apply {
            if (editingEvent != null) {
                editingEvent.plannedExpenses.forEach { pe ->
                    add(
                        PlannedExpenseDraft(
                            id = pe.id,
                            name = pe.name,
                            category = pe.category,
                            subcategory = pe.subcategory,
                            amountText = if (pe.amount > 0) pe.amount.toLong().toString() else "",
                            isPaid = pe.isPaid
                        )
                    )
                }
            } else {
                initialTemplate?.defaultPlannedExpenses?.forEach { (itemName, itemCat) ->
                    add(
                        PlannedExpenseDraft(
                            name = itemName,
                            category = itemCat,
                            amountText = ""
                        )
                    )
                }
            }
        }
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var categoryPickerRowIndex by remember { mutableStateOf<Int?>(null) }

    val plannedTotal = plannedDrafts.sumOf { it.amountText.toDoubleOrNull() ?: 0.0 }
    val themeColor = Color(selectedColor)

    FullScreenModalContainer(onDismissRequest = onDismiss) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .imePadding()
                .testTag("screen_add_edit_event"),
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Surface(
                    color = DarkBackground,
                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fullScreenBottomSafePadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        errorMessage?.let {
                            Text(
                                text = it,
                                color = ExpenseRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Button(
                            onClick = {
                                val trimmedName = name.trim()
                                if (trimmedName.isBlank()) {
                                    errorMessage = "Nama acara tidak boleh kosong."
                                    return@Button
                                }
                                val parsedStart = parseSlashDate(startDate)
                                val mIdx = parsedStart?.second ?: defaultMonthIndex
                                val yr = parsedStart?.third ?: defaultYear
                                val finalPlanned = plannedDrafts.mapNotNull { d ->
                                    val t = d.name.trim()
                                    if (t.isBlank()) null
                                    else EventPlannedExpense(
                                        id = d.id,
                                        name = t,
                                        category = d.category.ifBlank { "Other" },
                                        subcategory = d.subcategory,
                                        amount = d.amountText.toDoubleOrNull() ?: 0.0,
                                        isPaid = d.isPaid
                                    )
                                }
                                val saved = editingEvent?.copy(
                                    name = trimmedName,
                                    eventType = eventType,
                                    targetBudget = budgetText.toDoubleOrNull() ?: 0.0,
                                    monthIndex = mIdx,
                                    year = yr,
                                    startDate = startDate,
                                    endDate = endDate,
                                    iconName = selectedIcon,
                                    colorHex = selectedColor,
                                    note = note.trim(),
                                    plannedExpenses = finalPlanned
                                ) ?: EventItem(
                                    name = trimmedName,
                                    eventType = eventType,
                                    targetBudget = budgetText.toDoubleOrNull() ?: 0.0,
                                    monthIndex = mIdx,
                                    year = yr,
                                    startDate = startDate,
                                    endDate = endDate,
                                    currency = "IDR",
                                    iconName = selectedIcon,
                                    colorHex = selectedColor,
                                    note = note.trim(),
                                    plannedExpenses = finalPlanned,
                                    isCompleted = false
                                )
                                onSave(saved)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_save_event")
                        ) {
                            Text(
                                text = if (editingEvent == null) "Buat Acara" else "Simpan Perubahan",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fullScreenTopSafePadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (editingEvent == null) "Buat Acara" else "Edit Acara",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Live Hero Preview Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(136.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            themeColor.copy(alpha = 0.9f),
                                            themeColor.copy(alpha = 0.5f),
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
                                    .size(136.dp)
                                    .alpha(0.35f)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.35f)
                                    ) {
                                        Text(
                                            text = "HARI 1",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = eventType,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = name.ifBlank { "Nama acara Anda" },
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = buildString {
                                            append("Mulai ${formatEventReadableDate(startDate)}")
                                            if (endDate.isNotBlank()) {
                                                append(" - ${formatEventReadableDate(endDate)}")
                                            }
                                            if (plannedTotal > 0) {
                                                append(" · Rencana ${CurrencyHelper.formatRupiah(plannedTotal)}")
                                            }
                                        },
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // Jenis
                    item {
                        Column {
                            Text("Jenis", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EventTypeTemplates.forEach { tpl ->
                                    val isSelected = eventType == tpl.type
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) themeColor.copy(alpha = 0.22f) else DarkSurface,
                                        border = BorderStroke(1.dp, if (isSelected) themeColor else DarkCardBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                eventType = tpl.type
                                                selectedIcon = tpl.iconName
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = eventIconFromName(tpl.iconName),
                                                contentDescription = null,
                                                tint = if (isSelected) themeColor else TextMuted,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = tpl.type,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) TextWhite else TextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Nama Acara
                    item {
                        Column {
                            Text("Nama Acara", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    errorMessage = null
                                },
                                placeholder = { Text("mis., Perjalanan ke Bali 2026", color = TextSubtle) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = DarkCardBorder
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_event_name")
                            )
                        }
                    }

                    // Tampilan (12 swatches)
                    item {
                        Column {
                            Text("Tampilan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(8.dp))
                            EventColorPalette12.chunked(6).forEachIndexed { rowIdx, rowColors ->
                                if (rowIdx > 0) Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowColors.forEach { hex ->
                                        val isSelected = selectedColor == hex
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(hex),
                                            border = if (isSelected) BorderStroke(2.5.dp, Color.White) else null,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .clickable { selectedColor = hex }
                                        ) {
                                            if (isSelected) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Kapan
                    item {
                        Column {
                            Text("Kapan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = DarkSurface,
                                    border = BorderStroke(1.dp, DarkCardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { showStartDatePicker = true }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Tanggal Mulai", fontSize = 11.sp, color = TextSubtle)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.CalendarToday, null, tint = AccentBlueBright, modifier = Modifier.size(14.dp))
                                            Text(
                                                text = formatEventReadableDate(startDate),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextWhite
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = DarkSurface,
                                    border = BorderStroke(1.dp, DarkCardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { showEndDatePicker = true }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Tanggal Akhir", fontSize = 11.sp, color = TextSubtle)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.CalendarToday, null, tint = AccentBlueBright, modifier = Modifier.size(14.dp))
                                            Text(
                                                text = if (endDate.isBlank()) "Terbuka" else formatEventReadableDate(endDate),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (endDate.isBlank()) TextMuted else TextWhite
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            val durations = listOf(
                                "3 hari" to 3,
                                "5 hari" to 5,
                                "1 minggu" to 7,
                                "2 minggu" to 14,
                                "Terbuka" to 0
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                durations.forEach { (label, days) ->
                                    val isSelected = selectedDurationChip == label
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) AccentBlue else DarkSurface,
                                        border = BorderStroke(1.dp, if (isSelected) AccentBlue else DarkCardBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedDurationChip = label
                                                endDate = if (days == 0) "" else addDaysToSlashDate(startDate, days - 1)
                                            }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextMuted,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Uang
                    item {
                        Column {
                            Text("Uang", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = DarkSurface,
                                    border = BorderStroke(1.dp, DarkCardBorder)
                                ) {
                                    Text(
                                        text = "IDR",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                                    )
                                }
                                OutlinedTextField(
                                    value = budgetText,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        if (digits.length <= 12) budgetText = digits
                                    },
                                    placeholder = { Text("Tetapkan target pengeluaran untuk acara ini", color = TextSubtle, fontSize = 12.5.sp) },
                                    prefix = { Text("Rp ", color = TextMuted, fontWeight = FontWeight.SemiBold) },
                                    visualTransformation = ThousandsSeparatorVisualTransformation('.'),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface,
                                        focusedBorderColor = AccentBlue,
                                        unfocusedBorderColor = DarkCardBorder
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_event_budget")
                                )
                            }

                            if (plannedTotal > 0.0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AccentBlue.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            budgetText = plannedTotal.toLong().toString()
                                        }
                                        .testTag("btn_use_planned_total_budget")
                                ) {
                                    Text(
                                        text = "Gunakan total rencana sebagai anggaran (${CurrencyHelper.formatRupiah(plannedTotal)})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentBlueBright,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Biaya yang direncanakan
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Biaya yang direncanakan",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = CurrencyHelper.formatRupiah(plannedTotal),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentBlueBright
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                plannedDrafts.forEachIndexed { idx, draft ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = DarkSurfaceElevated,
                                            border = BorderStroke(1.dp, DarkCardBorder),
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { categoryPickerRowIndex = idx }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = CategoryIconHelper.getCategoryIcon(draft.category),
                                                    contentDescription = "Pilih kategori",
                                                    tint = AccentBlueBright,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = draft.name,
                                            onValueChange = { newVal ->
                                                plannedDrafts[idx] = draft.copy(name = newVal)
                                            },
                                            placeholder = { Text("Item biaya", color = TextSubtle, fontSize = 12.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextWhite,
                                                unfocusedTextColor = TextWhite,
                                                focusedContainerColor = DarkSurfaceElevated,
                                                unfocusedContainerColor = DarkSurfaceElevated,
                                                focusedBorderColor = AccentBlue,
                                                unfocusedBorderColor = DarkCardBorder
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1.25f)
                                        )

                                        OutlinedTextField(
                                            value = draft.amountText,
                                            onValueChange = { input ->
                                                val digits = input.filter { it.isDigit() }
                                                if (digits.length <= 10) {
                                                    plannedDrafts[idx] = draft.copy(amountText = digits)
                                                }
                                            },
                                            placeholder = { Text("Rp 0", color = TextSubtle, fontSize = 12.sp) },
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
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(0.95f)
                                        )

                                        IconButton(
                                            onClick = { plannedDrafts.removeAt(idx) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Hapus item", tint = TextSubtle, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            plannedDrafts.add(PlannedExpenseDraft(name = "", category = "Other"))
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
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

                    // Catatan
                    item {
                        Column {
                            Text("Catatan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                placeholder = { Text("Catatan opsional untuk acara ini...", color = TextSubtle) },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = DarkCardBorder
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }

    if (showStartDatePicker) {
        val parsed = parseSlashDate(startDate)
        DatePickerSubDialog(
            initialDay = parsed?.first ?: todayCal.get(Calendar.DAY_OF_MONTH),
            initialMonth = parsed?.second ?: (todayCal.get(Calendar.MONTH) + 1),
            initialYear = parsed?.third ?: todayCal.get(Calendar.YEAR),
            onDateSelected = { d, m, y, _, _ ->
                startDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m, y)
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        val parsed = parseSlashDate(endDate) ?: parseSlashDate(startDate)
        DatePickerSubDialog(
            initialDay = parsed?.first ?: todayCal.get(Calendar.DAY_OF_MONTH),
            initialMonth = parsed?.second ?: (todayCal.get(Calendar.MONTH) + 1),
            initialYear = parsed?.third ?: todayCal.get(Calendar.YEAR),
            onDateSelected = { d, m, y, _, _ ->
                endDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m, y)
                selectedDurationChip = "Custom"
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    categoryPickerRowIndex?.let { rowIdx ->
        val currentDraft = plannedDrafts.getOrNull(rowIdx)
        if (currentDraft != null) {
            CategoryPickerSubDialog(
                currentCategory = currentDraft.category,
                currentSubcategory = currentDraft.subcategory,
                categories = allCategories,
                onCategoryAndSubcategorySelected = { cat, sub ->
                    plannedDrafts[rowIdx] = currentDraft.copy(
                        category = cat,
                        subcategory = sub
                    )
                    categoryPickerRowIndex = null
                },
                onDismiss = { categoryPickerRowIndex = null }
            )
        } else {
            categoryPickerRowIndex = null
        }
    }
}

@Composable
fun DeleteEventConfirmDialog(
    eventName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(ExpenseRedSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Hapus Acara",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Acara \"$eventName\" akan dihapus. Transaksi yang pernah dicatat tetap tersimpan di riwayat Anda.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Batal", color = TextWhite, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_confirm_delete_event")
                    ) {
                        Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
