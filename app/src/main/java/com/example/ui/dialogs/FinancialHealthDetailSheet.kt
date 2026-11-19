package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.summary.FinancialHealthScoreGauge
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TrophyYellowLight
import com.example.ui.theme.TrophyYellowSoft
import com.example.util.CurrencyHelper
import com.example.util.FinancialHealthReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialHealthDetailSheet(
    score: Int = 0,
    savingsRatePct: Double = 0.0,
    netSurplusFormatted: String = "+Rp0",
    report: FinancialHealthReport? = null,
    onDismiss: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onAddTransaction: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val effectiveScore = report?.score ?: score
    val effectiveSavingsRate = report?.savingsRatePct ?: savingsRatePct
    val effectiveSurplusStr = if (report != null) {
        val sign = if (report.netSurplus >= 0) "+" else ""
        "$sign${CurrencyHelper.formatRupiah(report.netSurplus)}"
    } else netSurplusFormatted
    val statusLabel = report?.statusLabel ?: when {
        effectiveScore >= 80 -> "Sangat Baik"
        effectiveScore >= 60 -> "Baik"
        effectiveScore >= 40 -> "Cukup"
        else -> "Perlu Perhatian"
    }
    val statusColor = when {
        effectiveScore >= 60 -> IncomeGreen
        effectiveScore >= 40 -> TrophyYellowLight
        else -> ExpenseRed
    }
    val statusBg = when {
        effectiveScore >= 60 -> IncomeGreenSoft
        effectiveScore >= 40 -> TrophyYellowSoft
        else -> ExpenseRedSoft
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(IncomeGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Skor Kesehatan Keuangan",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Evaluasi menyeluruh kebiasaan finansial Anda",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusBg
                        ) {
                            Text(
                                text = "Status: $statusLabel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Surplus: $effectiveSurplusStr",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Tingkat tabungan mencapai ${effectiveSavingsRate.toInt()}% dari pemasukan",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    FinancialHealthScoreGauge(
                        score = effectiveScore,
                        label = "$effectiveScore/100",
                        modifier = Modifier.size(76.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "5 Pilar Kesehatan Finansial",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(10.dp))

            val pillars = report?.pillars?.map { p ->
                val pColor = when (p.statusLevel) {
                    1 -> IncomeGreen
                    0 -> TrophyYellowLight
                    else -> ExpenseRed
                }
                PillarData(p.name, p.description, p.score, p.maxScore, pColor)
            } ?: listOf(
                PillarData("Tingkat Tabungan", "${effectiveSavingsRate.toInt()}% dari pemasukan", (effectiveScore / 5).coerceIn(0, 20), 20, statusColor),
                PillarData("Kontrol Pengeluaran", "Pantauan transaksi periode berjalan", (effectiveScore / 5).coerceIn(0, 20), 20, statusColor),
                PillarData("Konsistensi Pelacakan", "Keaktifan pencatatan harian", (effectiveScore / 5).coerceIn(0, 20), 20, statusColor),
                PillarData("Diversifikasi Pendapatan", "Jumlah pos pemasukan aktif", (effectiveScore / 5).coerceIn(0, 20), 20, statusColor),
                PillarData("Progres Anggaran", "Kepatuhan anggaran & rasio utang", (effectiveScore / 5).coerceIn(0, 20), 20, statusColor)
            )

            pillars.forEach { pillar ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pillar.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                            Text("${pillar.score}/${pillar.maxScore} poin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pillar.color)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (pillar.score.toFloat() / pillar.maxScore.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = pillar.color,
                            trackColor = DarkSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(pillar.desc, fontSize = 10.sp, color = TextSubtle)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actionable Tips Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = IncomeGreenSoft,
                border = androidx.compose.foundation.BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = report?.recommendationTitle ?: "Rekomendasi untuk Meningkatkan Skor",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = report?.recommendationDesc ?: "Catat seluruh transaksi harian Anda dan jaga agar pengeluaran di bawah anggaran.",
                            fontSize = 11.sp,
                            color = TextWhite,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Integration Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onNavigateToBudgets()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                ) {
                    Icon(imageVector = Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Anggaran", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onNavigateToPlanner()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF818CF8))
                ) {
                    Icon(imageVector = Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Perencana", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onDismiss()
                        onAddTransaction()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Catat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class PillarData(
    val name: String,
    val desc: String,
    val score: Int,
    val maxScore: Int,
    val color: Color
)
