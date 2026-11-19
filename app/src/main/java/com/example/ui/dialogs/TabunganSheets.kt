package com.example.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AccountWallet
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsRecord
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
import com.example.util.CurrencyHelper
import com.example.util.InstitutionLogoBadge
import com.example.util.ThousandsSeparatorVisualTransformation
import java.util.Calendar
import java.util.Locale

fun savingsIconFromName(iconName: String): ImageVector {
    return when (iconName) {
        "Shield" -> Icons.Default.Shield
        "Home" -> Icons.Default.Home
        "Flight" -> Icons.Default.Flight
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "School" -> Icons.Default.School
        "LaptopMac" -> Icons.Default.LaptopMac
        "Favorite" -> Icons.Default.Favorite
        "Star" -> Icons.Default.Star
        "CardGiftcard" -> Icons.Default.CardGiftcard
        else -> Icons.Default.Savings
    }
}

data class SavingsGoalTemplate(
    val name: String,
    val iconName: String,
    val colorHex: Long
)

val SavingsGoalTemplates = listOf(
    SavingsGoalTemplate("Dana darurat", "Shield", 0xFF10B981L),
    SavingsGoalTemplate("Rumah", "Home", 0xFF3B82F6L),
    SavingsGoalTemplate("Perjalanan", "Flight", 0xFF06B6D4L),
    SavingsGoalTemplate("Gadget", "LaptopMac", 0xFFEC4899L),
    SavingsGoalTemplate("Mobil", "DirectionsCar", 0xFFF59E0BL),
    SavingsGoalTemplate("Pendidikan", "School", 0xFF8B5CF6L),
    SavingsGoalTemplate("Pernikahan", "Favorite", 0xFFEF4444L),
    SavingsGoalTemplate("Lainnya", "Savings", 0xFF14B8A6L),
    SavingsGoalTemplate("Impian besar", "Star", 0xFF6366F1L)
)

private val GoalIconChoices = listOf(
    "Savings", "Shield", "Home", "Flight", "DirectionsCar", "School", "LaptopMac", "Favorite", "Star"
)

private val GoalColorPalette12 = listOf(
    0xFF10B981L, 0xFF059669L, 0xFF3B82F6L, 0xFF2563EBL,
    0xFF06B6D4L, 0xFF14B8A6L, 0xFFF59E0BL, 0xFFEA580CL,
    0xFF8B5CF6L, 0xFF6366F1L, 0xFFEC4899L, 0xFFEF4444L
)

private val SavingsShortMonths = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
)

private fun parseSavingsSlashDate(dateStr: String): Triple<Int, Int, Int>? {
    val parts = dateStr.trim().split("/")
    if (parts.size != 3) return null
    val d = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val y = parts[2].toIntOrNull() ?: return null
    return Triple(d, m, y)
}

fun formatSavingsReadableDate(dateStr: String): String {
    val parsed = parseSavingsSlashDate(dateStr) ?: return dateStr.ifBlank { "-" }
    val mShort = SavingsShortMonths.getOrElse(parsed.second - 1) { "Nov" }
    return "${parsed.first} $mShort ${parsed.third}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsTemplatePickerSheet(
    onContinue: (SavingsGoalTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTemplate by remember { mutableStateOf(SavingsGoalTemplates.first()) }

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
                        text = "Tujuan tabungan baru",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pilih tujuan umum atau mulai dengan nama buatan Anda sendiri.",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val rows = SavingsGoalTemplates.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { tpl ->
                            val isSelected = selectedTemplate.name == tpl.name
                            val tplColor = Color(tpl.colorHex)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) tplColor.copy(alpha = 0.2f) else DarkSurfaceElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) tplColor else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(84.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedTemplate = tpl }
                                    .testTag("tpl_savings_${tpl.name}")
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
                                            imageVector = savingsIconFromName(tpl.iconName),
                                            contentDescription = null,
                                            tint = tplColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = tpl.name,
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
                    .testTag("btn_continue_savings_template")
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
fun AddEditSavingsGoalFullScreenDialog(
    editingGoal: SavingsGoal? = null,
    initialTemplate: SavingsGoalTemplate? = null,
    onSave: (SavingsGoal) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

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

    var name by remember(editingGoal, initialTemplate) {
        mutableStateOf(
            editingGoal?.name ?: if (initialTemplate != null && initialTemplate.name != "Lainnya" && initialTemplate.name != "Impian besar") {
                initialTemplate.name
            } else ""
        )
    }
    var selectedIcon by remember(editingGoal, initialTemplate) {
        mutableStateOf(editingGoal?.iconName ?: initialTemplate?.iconName ?: "Savings")
    }
    var selectedColor by remember(editingGoal, initialTemplate) {
        mutableLongStateOf(editingGoal?.colorHex ?: initialTemplate?.colorHex ?: 0xFF10B981L)
    }
    var targetText by remember(editingGoal) {
        mutableStateOf(editingGoal?.targetAmount?.toLong()?.takeIf { it > 0 }?.toString() ?: "")
    }
    var startDate by remember(editingGoal) {
        mutableStateOf(editingGoal?.startDate?.ifBlank { todaySlash } ?: todaySlash)
    }
    var targetDate by remember(editingGoal) {
        mutableStateOf(editingGoal?.targetDate ?: "")
    }
    var note by remember(editingGoal) { mutableStateOf(editingGoal?.note ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showTargetDatePicker by remember { mutableStateOf(false) }

    val themeColor = Color(selectedColor)

    FullScreenModalContainer(onDismissRequest = onDismiss) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .imePadding()
                .testTag("screen_add_edit_goal"),
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
                            Text(text = it, color = ExpenseRed, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Button(
                            onClick = {
                                val targetAmount = targetText.toDoubleOrNull() ?: 0.0
                                if (name.isBlank()) {
                                    errorMessage = "Nama tabungan tidak boleh kosong."
                                    return@Button
                                }
                                if (targetAmount <= 0.0) {
                                    errorMessage = "Jumlah target harus lebih dari 0."
                                    return@Button
                                }
                                val saved = editingGoal?.copy(
                                    name = name.trim(),
                                    targetAmount = targetAmount,
                                    startDate = startDate,
                                    targetDate = targetDate.trim(),
                                    iconName = selectedIcon,
                                    note = note.trim(),
                                    colorHex = selectedColor
                                ) ?: SavingsGoal(
                                    name = name.trim(),
                                    targetAmount = targetAmount,
                                    currentAmount = 0.0,
                                    startDate = startDate,
                                    targetDate = targetDate.trim(),
                                    currency = "IDR",
                                    iconName = selectedIcon,
                                    note = note.trim(),
                                    colorHex = selectedColor,
                                    isCompleted = false,
                                    records = emptyList()
                                )
                                onSave(saved)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_save_goal")
                        ) {
                            Text(
                                text = if (editingGoal == null) "Buat Tabungan" else "Simpan Perubahan",
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (editingGoal == null) "Buat Tabungan" else "Edit Tabungan",
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
                    // Nama Tabungan
                    item {
                        Column {
                            Text("Nama Tabungan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    errorMessage = null
                                },
                                placeholder = { Text("mis., Dana Liburan", color = TextSubtle) },
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
                                    .testTag("input_goal_name")
                            )
                        }
                    }

                    // Ikon
                    item {
                        Column {
                            Text("Ikon", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GoalIconChoices.forEach { iconName ->
                                    val isSelected = selectedIcon == iconName
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) themeColor.copy(alpha = 0.22f) else DarkSurface,
                                        border = BorderStroke(1.dp, if (isSelected) themeColor else DarkCardBorder),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { selectedIcon = iconName }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = savingsIconFromName(iconName),
                                                contentDescription = iconName,
                                                tint = if (isSelected) themeColor else TextMuted,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Warna (12 swatches)
                    item {
                        Column {
                            Text("Warna", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(8.dp))
                            GoalColorPalette12.chunked(6).forEachIndexed { rowIdx, rowColors ->
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

                    // Mata Uang
                    item {
                        Column {
                            Text("Mata Uang", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DarkSurface,
                                border = BorderStroke(1.dp, DarkCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "IDR - Indonesian Rupiah",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite
                                    )
                                    Text("Utama", fontSize = 12.sp, color = AccentBlueBright, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Jumlah Target
                    item {
                        Column {
                            Text("Jumlah Target", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = targetText,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }
                                    if (digits.length <= 12) {
                                        targetText = digits
                                        errorMessage = null
                                    }
                                },
                                placeholder = { Text("0", color = TextSubtle) },
                                prefix = { Text("Rp ", color = TextMuted, fontWeight = FontWeight.Bold) },
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
                                    .fillMaxWidth()
                                    .testTag("input_goal_target")
                            )
                        }
                    }

                    // Tanggal Mulai & Tanggal Waktu
                    item {
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
                                            text = formatSavingsReadableDate(startDate),
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
                                    .clickable { showTargetDatePicker = true }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Tanggal Waktu", fontSize = 11.sp, color = TextSubtle)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarToday, null, tint = AccentBlueBright, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = if (targetDate.isBlank()) "Opsional" else formatSavingsReadableDate(targetDate),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (targetDate.isBlank()) TextMuted else TextWhite
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Catatan
                    item {
                        Column {
                            Text("Catatan (Opsional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                placeholder = { Text("Motivasi atau catatan tujuan...", color = TextSubtle) },
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
        val parsed = parseSavingsSlashDate(startDate)
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

    if (showTargetDatePicker) {
        val parsed = parseSavingsSlashDate(targetDate) ?: parseSavingsSlashDate(startDate)
        DatePickerSubDialog(
            initialDay = parsed?.first ?: todayCal.get(Calendar.DAY_OF_MONTH),
            initialMonth = parsed?.second ?: (todayCal.get(Calendar.MONTH) + 1),
            initialYear = parsed?.third ?: todayCal.get(Calendar.YEAR),
            onDateSelected = { d, m, y, _, _ ->
                targetDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m, y)
                showTargetDatePicker = false
            },
            onDismiss = { showTargetDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositWithdrawGoalSheet(
    goal: SavingsGoal,
    initialIsWithdraw: Boolean = false,
    editingRecord: SavingsRecord? = null,
    wallets: List<AccountWallet>,
    onSaveRecord: (amount: Double, isWithdraw: Boolean, note: String, dateStr: String, walletName: String) -> Boolean,
    onDeleteRecord: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

    var isWithdraw by remember(editingRecord, initialIsWithdraw) {
        mutableStateOf(editingRecord?.isWithdraw ?: initialIsWithdraw)
    }
    var amountText by remember(editingRecord) {
        mutableStateOf(editingRecord?.amount?.toLong()?.takeIf { it > 0 }?.toString() ?: "")
    }
    var noteText by remember(editingRecord) {
        mutableStateOf(editingRecord?.note ?: "")
    }
    var dateStr by remember(editingRecord) {
        mutableStateOf(editingRecord?.dateStr?.ifBlank { todaySlash } ?: todaySlash)
    }
    var selectedWallet by remember(editingRecord) {
        mutableStateOf(editingRecord?.walletName ?: "") // default "" = "Tidak ada akun" (catatan kertas)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val shortDatePill = remember(dateStr) {
        val p = parseSavingsSlashDate(dateStr)
        if (p != null) String.format(Locale.getDefault(), "%02d/%02d", p.first, p.second) else dateStr
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
            // Header + 📅 DD/MM pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            editingRecord != null -> "Edit catatan"
                            isWithdraw -> "Ambil dari tujuan"
                            else -> "Tambah ke tujuan"
                        },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = goal.name,
                        fontSize = 12.5.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showDatePicker = true }
                        .testTag("btn_savings_record_date")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, null, tint = AccentBlueBright, modifier = Modifier.size(14.dp))
                        Text(shortDatePill, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode switcher: ↓ Setoran | ↑ Tarik
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isWithdraw) IncomeGreenSoft else Color.Transparent,
                        border = if (!isWithdraw) BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.5f)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                isWithdraw = false
                                errorMessage = null
                            }
                            .testTag("tab_savings_setoran")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (!isWithdraw) IncomeGreen else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Setoran",
                                fontSize = 13.5.sp,
                                fontWeight = if (!isWithdraw) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isWithdraw) IncomeGreen else TextMuted
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isWithdraw) ExpenseRedSoft else Color.Transparent,
                        border = if (isWithdraw) BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.5f)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                isWithdraw = true
                                errorMessage = null
                            }
                            .testTag("tab_savings_tarik")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isWithdraw) ExpenseRed else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tarik",
                                fontSize = 13.5.sp,
                                fontWeight = if (isWithdraw) FontWeight.Bold else FontWeight.Medium,
                                color = if (isWithdraw) ExpenseRed else TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("JUMLAH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.6.sp)
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    if (digits.length <= 12) {
                        amountText = digits
                        errorMessage = null
                    }
                },
                placeholder = { Text("0", color = TextSubtle) },
                prefix = {
                    Text(
                        text = if (isWithdraw) "-Rp " else "+Rp ",
                        color = if (isWithdraw) ExpenseRed else IncomeGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                visualTransformation = ThousandsSeparatorVisualTransformation('.'),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (isWithdraw) ExpenseRed else IncomeGreen,
                    unfocusedTextColor = if (isWithdraw) ExpenseRed else IncomeGreen,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedBorderColor = if (isWithdraw) ExpenseRed else IncomeGreen,
                    unfocusedBorderColor = DarkCardBorder
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_goal_deposit_amount")
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("CATATAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.6.sp)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text("Untuk apa ini?", color = TextSubtle) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DarkCardBorder
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (editingRecord == null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text("DOMPET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.6.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isPaperSelected = selectedWallet.isBlank()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPaperSelected) AccentBlue.copy(alpha = 0.2f) else DarkSurfaceElevated,
                        border = BorderStroke(1.dp, if (isPaperSelected) AccentBlue else DarkCardBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedWallet = ""
                                errorMessage = null
                            }
                            .testTag("chip_savings_no_account")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                text = "Tidak ada akun",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPaperSelected) TextWhite else TextMuted
                            )
                            Text(
                                text = "Hanya catatan",
                                fontSize = 10.5.sp,
                                color = if (isPaperSelected) AccentBlueBright else TextSubtle
                            )
                        }
                    }

                    wallets.forEach { w ->
                        val isSelected = selectedWallet.equals(w.name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) AccentBlue.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) AccentBlue else DarkCardBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedWallet = w.name
                                    errorMessage = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InstitutionLogoBadge(
                                    walletName = w.name,
                                    iconName = w.iconName,
                                    size = 24.dp
                                )
                                Column {
                                    Text(
                                        text = w.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = CurrencyHelper.formatRupiah(w.balance),
                                        fontSize = 10.5.sp,
                                        color = if (isSelected) AccentBlueBright else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (selectedWallet.isBlank()) {
                        "Tanpa akun, ini hanya catatan kertas — tidak ada yang keluar dari akun Anda."
                    } else {
                        "Dengan akun dipilih, uang sungguhan bergerak dan transaksi dibuat bersama catatan ini."
                    },
                    fontSize = 11.5.sp,
                    color = TextSubtle,
                    lineHeight = 16.sp
                )
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = ExpenseRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (editingRecord != null && onDeleteRecord != null) {
                    OutlinedButton(
                        onClick = {
                            onDeleteRecord()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = ExpenseRedSoft),
                        modifier = Modifier
                            .weight(0.45f)
                            .height(48.dp)
                            .testTag("btn_delete_savings_record")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus", color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) {
                            errorMessage = "Masukkan nominal lebih dari 0."
                            return@Button
                        }
                        if (isWithdraw && editingRecord == null && amt > goal.currentAmount) {
                            errorMessage = "Saldo tabungan tidak mencukupi (tersedia ${CurrencyHelper.formatRupiah(goal.currentAmount)})."
                            return@Button
                        }
                        if (!isWithdraw && selectedWallet.isNotBlank() && editingRecord == null) {
                            val walletBal = wallets.find { it.name.equals(selectedWallet, ignoreCase = true) }?.balance ?: 0.0
                            if (amt > walletBal) {
                                errorMessage = "Saldo dompet $selectedWallet tidak mencukupi."
                                return@Button
                            }
                        }
                        val ok = onSaveRecord(amt, isWithdraw, noteText.trim(), dateStr, selectedWallet)
                        if (ok) {
                            onDismiss()
                        } else {
                            errorMessage = "Gagal menyimpan catatan. Periksa nominal atau saldo tersedia."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_confirm_goal_transfer")
                ) {
                    Text(
                        text = if (editingRecord != null) "Simpan" else if (isWithdraw) "Catat Penarikan" else "Tambah ke Tujuan",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val parsed = parseSavingsSlashDate(dateStr)
        DatePickerSubDialog(
            initialDay = parsed?.first ?: todayCal.get(Calendar.DAY_OF_MONTH),
            initialMonth = parsed?.second ?: (todayCal.get(Calendar.MONTH) + 1),
            initialYear = parsed?.third ?: todayCal.get(Calendar.YEAR),
            onDateSelected = { d, m, y, _, _ ->
                dateStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m, y)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun DeleteSavingsGoalConfirmDialog(
    goalName: String,
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
                    text = "Hapus Tabungan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tujuan tabungan \"$goalName\" akan dihapus beserta catatan paper-nya.",
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
                            .testTag("btn_confirm_delete_goal")
                    ) {
                        Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
