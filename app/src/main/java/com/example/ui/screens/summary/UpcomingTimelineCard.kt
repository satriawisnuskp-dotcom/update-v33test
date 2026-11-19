package com.example.ui.screens.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecurringTransaction
import com.example.data.model.TimelineScenarioItem
import com.example.data.model.TransactionType
import com.example.data.model.UpcomingScheduleItem
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.util.CurrencyHelper
import java.util.Calendar

private data class TimelineDisplayRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateLabel: String,
    val sortDay: Int,
    val amount: Double,
    val isIncome: Boolean,
    val sourceKind: String, // "SCENARIO", "RECURRING", "SCHEDULE"
    val scenarioId: String? = null
)

@Composable
fun UpcomingTimelineCard(
    currentBalance: Double,
    upcomingItems: List<UpcomingScheduleItem>,
    recurringTransactions: List<RecurringTransaction>,
    timelineScenarios: List<TimelineScenarioItem>,
    isBalanceHidden: Boolean = false,
    onAddClick: () -> Unit,
    onDeleteScenario: (String) -> Unit,
    onRecurringClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf("Semua") }
    val curCal = remember { Calendar.getInstance() }
    val currentMonth = curCal.get(Calendar.MONTH) + 1
    val currentYear = curCal.get(Calendar.YEAR)
    val currentMonthKey = "$currentMonth/$currentYear"
    val monthNamesShort = remember {
        listOf("", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    }
    val mShort = monthNamesShort.getOrElse(currentMonth) { "Bln" }

    val mergedRows = remember(upcomingItems, recurringTransactions, timelineScenarios, activeFilter, currentMonthKey) {
        val list = mutableListOf<TimelineDisplayRow>()
        val seenTitles = mutableSetOf<String>()

        timelineScenarios.forEach { sc ->
            val parsedDay = sc.date.split(" ", ",", "/").mapNotNull { it.toIntOrNull() }.firstOrNull() ?: 15
            list.add(
                TimelineDisplayRow(
                    id = "sc_${sc.id}",
                    title = sc.note.ifBlank { if (sc.isIncome) "Skenario Masuk" else "Skenario Keluar" },
                    subtitle = "${sc.recurrence} · Jika-jika",
                    dateLabel = sc.date.ifBlank { "$parsedDay $mShort" }.take(14),
                    sortDay = parsedDay,
                    amount = sc.amount,
                    isIncome = sc.isIncome,
                    sourceKind = "SCENARIO",
                    scenarioId = sc.id
                )
            )
        }

        recurringTransactions.filter { it.isActive && it.lastProcessedMonthYear != currentMonthKey }.forEach { rec ->
            seenTitles.add(rec.title.trim().lowercase())
            list.add(
                TimelineDisplayRow(
                    id = "rec_${rec.id}",
                    title = rec.title,
                    subtitle = "${rec.accountName} · ${rec.frequency}",
                    dateLabel = "${rec.dayOfMonth} $mShort",
                    sortDay = rec.dayOfMonth,
                    amount = rec.amount,
                    isIncome = rec.type == TransactionType.INCOME,
                    sourceKind = "RECURRING"
                )
            )
        }

        upcomingItems.forEach { up ->
            if (up.title.trim().lowercase() !in seenTitles) {
                val parsedDay = up.dateLabel.trim().split(" ").firstOrNull()?.toIntOrNull() ?: 28
                list.add(
                    TimelineDisplayRow(
                        id = "up_${up.id}",
                        title = up.title,
                        subtitle = "${up.accountName} · Terjadwal",
                        dateLabel = up.dateLabel,
                        sortDay = parsedDay,
                        amount = up.amount,
                        isIncome = up.isIncome,
                        sourceKind = "SCHEDULE"
                    )
                )
            }
        }

        val sorted = list.sortedBy { it.sortDay }
        when (activeFilter) {
            "Masuk" -> sorted.filter { it.isIncome }
            "Keluar" -> sorted.filter { !it.isIncome }
            else -> sorted
        }
    }

    val projectedBalance = remember(currentBalance, mergedRows) {
        mergedRows.fold(currentBalance) { acc, item ->
            if (item.isIncome) acc + item.amount else acc - item.amount
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_upcoming_timeline")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Yang akan datang",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = if (isBalanceHidden) "Saldo sekarang ••••••"
                        else "${CurrencyHelper.formatCompactRupiah(currentBalance)} sekarang → ${CurrencyHelper.formatCompactRupiah(projectedBalance)} tersisa",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentBlue.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onAddClick() }
                        .testTag("btn_add_timeline_scenario")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah ke garis waktu",
                            tint = AccentBlueBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Tambah",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlueBright
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Pills: Semua | Masuk | Keluar
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Semua", "Masuk", "Keluar").forEach { tab ->
                    val isSelected = activeFilter == tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AccentBlue else DarkSurfaceElevated,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { activeFilter = tab }
                            .testTag("timeline_filter_$tab")
                    ) {
                        Text(
                            text = tab,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (mergedRows.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Belum ada jadwal ${if (activeFilter == "Semua") "mendatang" else activeFilter.lowercase()}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ketuk + Tambah untuk simulasi skenario arus kas atau atur transaksi berulang.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                var running = currentBalance
                mergedRows.take(6).forEachIndexed { idx, row ->
                    running = if (row.isIncome) running + row.amount else running - row.amount
                    if (idx > 0) {
                        HorizontalDivider(
                            color = DarkCardBorder.copy(alpha = 0.35f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (row.sourceKind == "RECURRING") {
                                    Modifier.clickable { onRecurringClick() }
                                } else Modifier
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Timeline dot + date
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(44.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = when (row.sourceKind) {
                                            "SCENARIO" -> AccentBlue.copy(alpha = 0.18f)
                                            else -> if (row.isIncome) IncomeGreen.copy(alpha = 0.16f) else ExpenseRed.copy(alpha = 0.16f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (row.sourceKind) {
                                        "SCENARIO" -> Icons.Default.AutoAwesome
                                        "RECURRING" -> Icons.Default.EventRepeat
                                        else -> Icons.Default.Schedule
                                    },
                                    contentDescription = null,
                                    tint = when (row.sourceKind) {
                                        "SCENARIO" -> AccentBlueBright
                                        else -> if (row.isIncome) IncomeGreen else ExpenseRed
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = row.dateLabel,
                                fontSize = 9.5.sp,
                                color = TextSubtle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = row.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (row.sourceKind == "RECURRING") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AccentBlue.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Berulang",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentBlueBright,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (row.sourceKind == "SCENARIO") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF8B5CF6).copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "Simulasi",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFA78BFA),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = row.subtitle,
                                fontSize = 11.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isBalanceHidden) "••••••"
                                else (if (row.isIncome) "+" else "-") + CurrencyHelper.formatCompactRupiah(row.amount),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (row.isIncome) IncomeGreen else ExpenseRed
                            )
                            Text(
                                text = if (isBalanceHidden) "••••" else "Saldo: ${CurrencyHelper.formatCompactRupiah(running)}",
                                fontSize = 10.sp,
                                color = TextSubtle
                            )
                        }

                        if (row.scenarioId != null) {
                            IconButton(
                                onClick = { onDeleteScenario(row.scenarioId) },
                                modifier = Modifier
                                    .size(30.dp)
                                    .padding(start = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus skenario",
                                    tint = TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
