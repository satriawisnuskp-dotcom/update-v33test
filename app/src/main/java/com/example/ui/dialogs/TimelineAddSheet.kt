package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimelineScenarioItem
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.ThousandsSeparatorVisualTransformation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineAddSheet(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isIncome by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedRecurrence by remember { mutableStateOf("Hanya sekali") }

    val sdf = remember { SimpleDateFormat("d/M/yyyy", Locale.getDefault()) }
    val displaySdf = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")) }
    val pickedCalendar = remember { java.util.Calendar.getInstance() }
    var currentDateStr by remember { mutableStateOf(displaySdf.format(pickedCalendar.time)) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val recurrenceOptions = listOf(
        "Hanya sekali",
        "Setiap minggu",
        "Setiap 2 minggu",
        "Setiap bulan",
        "Setiap 3 bulan",
        "Setiap tahun"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextWhite)
                }
                Text(
                    text = "Tambahkan ke garis waktu",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.size(32.dp))
            }

            // Subtitle Description matching video
            Text(
                text = "Skenario jika-jika untuk ramalan Anda saja. Itu tidak pernah dicatat sebagai transaksi dan tidak pernah mengubah saldo.",
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 17.sp
            )

            // Tabs: Masuk / Keluar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Masuk
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isIncome) IncomeGreen.copy(alpha = 0.2f) else DarkSurfaceElevated,
                    border = BorderStroke(1.dp, if (isIncome) IncomeGreen else DarkCardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isIncome = true }
                        .testTag("timeline_type_masuk")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = if (isIncome) IncomeGreen else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Masuk",
                            fontSize = 13.sp,
                            fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                            color = if (isIncome) IncomeGreen else TextMuted
                        )
                    }
                }

                // Keluar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isIncome) ExpenseRed.copy(alpha = 0.2f) else DarkSurfaceElevated,
                    border = BorderStroke(1.dp, if (!isIncome) ExpenseRed else DarkCardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isIncome = false }
                        .testTag("timeline_type_keluar")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (!isIncome) ExpenseRed else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Keluar",
                            fontSize = 13.sp,
                            fontWeight = if (!isIncome) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isIncome) ExpenseRed else TextMuted
                        )
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                label = { Text("Jumlah") },
                prefix = { Text("Rp ", color = TextWhite) },
                visualTransformation = ThousandsSeparatorVisualTransformation('.'),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DarkCardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timeline_amount_input")
            )

            // Note Input with counter
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { if (it.length <= 40) noteText = it },
                    placeholder = { Text("Bonus, pengembalian, perjalanan, pembayaran klien...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timeline_note_input")
                )
                Text(
                    text = "${noteText.length}/40",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp)
                )
            }

            // Interactive Date Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        android.app.DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                pickedCalendar.set(y, m, d)
                                currentDateStr = displaySdf.format(pickedCalendar.time)
                            },
                            pickedCalendar.get(java.util.Calendar.YEAR),
                            pickedCalendar.get(java.util.Calendar.MONTH),
                            pickedCalendar.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .testTag("timeline_date_picker")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = currentDateStr,
                            fontSize = 13.sp,
                            color = TextWhite,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Ubah",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentBlue
                    )
                }
            }

            // Recurrence Section
            Text(
                text = "Ulangi",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recurrenceOptions.forEach { opt ->
                    val isSelected = selectedRecurrence == opt
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AccentBlue else DarkSurfaceElevated,
                        border = BorderStroke(1.dp, if (isSelected) AccentBlue else DarkCardBorder),
                        modifier = Modifier.clickable { selectedRecurrence = opt }
                    ) {
                        Text(
                            text = opt,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0.0) {
                        viewModel.addTimelineScenario(
                            TimelineScenarioItem(
                                amount = amount,
                                isIncome = isIncome,
                                note = noteText.ifBlank { if (isIncome) "Rencana Pemasukan" else "Rencana Pengeluaran" },
                                date = currentDateStr,
                                recurrence = selectedRecurrence
                            )
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Simpan ke garis waktu",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
