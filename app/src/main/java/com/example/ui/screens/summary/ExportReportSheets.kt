package com.example.ui.screens.summary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.CategorySlice
import com.example.util.CurrencyHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EksporLaporanSheet(
    periodLabel: String,
    transactionCount: Int,
    onSelectPdfPreview: () -> Unit,
    onExportExcelCsv: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(AccentBlue.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = AccentBlueBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(20.dp)
                                .background(ExpenseRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Ekspor Laporan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "$periodLabel · $transactionCount transaksi",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                }
            }

            ExportOptionRow(
                icon = Icons.Default.PictureAsPdf,
                iconTint = ExpenseRed,
                title = "Dokumen PDF",
                subtitle = "Laporan keuangan multi-halaman dengan pratinjau cetak",
                tag = "btn_export_option_pdf",
                onClick = {
                    onDismiss()
                    onSelectPdfPreview()
                }
            )

            ExportOptionRow(
                icon = Icons.Default.TableChart,
                iconTint = IncomeGreen,
                title = "Spreadsheet Excel",
                subtitle = "Siap dibuka di Microsoft Excel & Google Sheets",
                tag = "btn_export_option_excel",
                onClick = {
                    onDismiss()
                    onExportExcelCsv()
                }
            )

            ExportOptionRow(
                icon = Icons.Default.Description,
                iconTint = AccentBlueBright,
                title = "File CSV",
                subtitle = "Format data koma-terpisah standar untuk arsip pribadi",
                tag = "btn_export_option_csv",
                onClick = {
                    onDismiss()
                    onExportExcelCsv()
                }
            )

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun ExportOptionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconTint.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Text(subtitle, fontSize = 11.5.sp, color = TextMuted)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted
            )
        }
    }
}

@Composable
fun PdfPreviewScreenDialog(
    periodLabel: String,
    walletName: String,
    incomeTotal: Double,
    expenseTotal: Double,
    netCashFlow: Double,
    savingsRate: Int,
    expenseBreakdown: List<CategorySlice>,
    incomeBreakdown: List<CategorySlice>,
    transactions: List<Transaction>,
    onGenerateAndSavePdf: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup pratinjau", tint = TextWhite)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Laporan Keuangan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Pratinjau Dokumen PDF · $periodLabel",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = onGenerateAndSavePdf) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan PDF", tint = AccentBlueBright)
                    }
                }

                // Multi-page white paper preview inside dark viewer
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Page 1: Executive Summary + Category Breakdown
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                        .padding(14.dp)
                                ) {
                                    Column {
                                        Text("LAPORAN KEUANGAN", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Periode: $periodLabel · Akun: $walletName", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PdfSummaryMetricBox("PEMASUKAN", CurrencyHelper.formatCompactRupiah(incomeTotal), Color(0xFF15803D), Color(0xFFF0FDF4), Modifier.weight(1f))
                                    PdfSummaryMetricBox("PENGELUARAN", CurrencyHelper.formatCompactRupiah(expenseTotal), Color(0xFFB91C1C), Color(0xFFFEF2F2), Modifier.weight(1f))
                                    PdfSummaryMetricBox("SURPLUS ($savingsRate%)", CurrencyHelper.formatCompactRupiah(netCashFlow), Color(0xFF1D4ED8), Color(0xFFEFF6FF), Modifier.weight(1f))
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("RINCIAN PENGELUARAN", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.height(8.dp))

                                if (expenseBreakdown.isEmpty()) {
                                    Text("Tidak ada pengeluaran pada periode ini.", fontSize = 11.sp, color = Color(0xFF64748B))
                                } else {
                                    expenseBreakdown.take(6).forEach { slice ->
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(slice.category, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                                Text(
                                                    "${CurrencyHelper.formatRupiah(slice.amount)} (${slice.percentage.toInt()}%)",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            LinearProgressIndicator(
                                                progress = { (slice.percentage / 100f).coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = slice.color,
                                                trackColor = Color(0xFFF1F5F9)
                                            )
                                        }
                                    }
                                }

                                if (incomeBreakdown.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("SUMBER PEMASUKAN", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    incomeBreakdown.take(4).forEach { s ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(s.category, fontSize = 11.5.sp, color = Color(0xFF334155))
                                            Text(CurrencyHelper.formatRupiah(s.amount), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Halaman 1", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }

                    // Page 2: Transaction Table
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("DAFTAR TRANSAKSI (${transactions.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF1F5F9))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("TGL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.width(38.dp))
                                    Text("KETERANGAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.weight(1f))
                                    Text("NOMINAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                }

                                transactions.take(25).forEachIndexed { idx, tx ->
                                    if (idx > 0) HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${tx.dayOfMonth}/${tx.monthIndex}", fontSize = 10.5.sp, color = Color(0xFF64748B), modifier = Modifier.width(38.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                tx.title.ifBlank { tx.category },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1E293B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text("${tx.category} · ${tx.accountName}", fontSize = 9.5.sp, color = Color(0xFF64748B))
                                        }
                                        val amtColor = when (tx.type) {
                                            TransactionType.INCOME -> Color(0xFF15803D)
                                            TransactionType.EXPENSE -> Color(0xFFB91C1C)
                                            TransactionType.TRANSFER -> Color(0xFF1D4ED8)
                                        }
                                        Text(
                                            CurrencyHelper.formatCompactRupiah(tx.amount),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = amtColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Halaman 2", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }

                // Bottom Bar Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Tutup", color = TextWhite)
                    }
                    Button(
                        onClick = onGenerateAndSavePdf,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        modifier = Modifier
                            .weight(1.6f)
                            .height(48.dp)
                            .testTag("btn_confirm_generate_pdf")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan & Cetak PDF", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfSummaryMetricBox(
    label: String,
    value: String,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(label, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(modifier = Modifier.height(3.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
