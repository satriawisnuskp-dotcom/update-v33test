package com.example.ui.screens.summary

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.util.CategorySlice
import com.example.util.CurrencyHelper
import com.example.util.FinancialHealthHelper
import kotlin.math.abs

@Composable
fun BukuHarianStoryDialog(
    monthLabel: String,
    periodTransactions: List<Transaction>,
    prevPeriodTransactions: List<Transaction>,
    incomeTotal: Double,
    expenseTotal: Double,
    expenseSlices: List<CategorySlice>,
    daysInMonth: Int,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val totalSlides = 7
    var currentSlide by remember { mutableIntStateOf(0) }

    val prevIncome = remember(prevPeriodTransactions) {
        prevPeriodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val prevExpense = remember(prevPeriodTransactions) {
        prevPeriodTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val expenseTxs = remember(periodTransactions) {
        periodTransactions.filter { it.type == TransactionType.EXPENSE }
    }
    val topExpenses = remember(expenseTxs) {
        expenseTxs.sortedByDescending { it.amount }.take(3)
    }
    val activeDays = remember(periodTransactions) {
        periodTransactions.map { it.dayOfMonth }.toSet()
    }
    val streak = remember(activeDays) {
        FinancialHealthHelper.calculateConsecutiveStreak(activeDays)
    }
    val avgPerDay = if (daysInMonth > 0) expenseTotal / daysInMonth else 0.0
    val netFlow = incomeTotal - expenseTotal
    val savingsRate = if (incomeTotal > 0.0) (((incomeTotal - expenseTotal) / incomeTotal) * 100.0).toInt() else 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A1628), Color(0xFF0F223D), Color(0xFF091220))
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Left/Right tap zones for story navigation
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(0.32f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentSlide > 0) currentSlide--
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(0.68f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentSlide < totalSlides - 1) currentSlide++ else onDismiss()
                        }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Story progress segments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    for (i in 0 until totalSlides) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (i <= currentSlide) Color.White else Color.White.copy(alpha = 0.25f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Top story bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Buku harian uang Anda",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = monthLabel,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_story")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup buku harian", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Slide Body
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                    modifier = Modifier.weight(1f),
                    label = "StorySlide"
                ) { slide ->
                    when (slide) {
                        0 -> StorySlideIntro(monthLabel, periodTransactions.size, incomeTotal + expenseTotal)
                        1 -> StorySlideFlow(incomeTotal, expenseTotal, netFlow, savingsRate)
                        2 -> StorySlideWhereItWent(expenseSlices)
                        3 -> StorySlideBigOnes(topExpenses, expenseTotal)
                        4 -> StorySlideRhythm(avgPerDay, activeDays, daysInMonth, streak)
                        5 -> StorySlideThenAndNow(incomeTotal, prevIncome, expenseTotal, prevExpense)
                        else -> StorySlideCarryForward(monthLabel, netFlow, savingsRate, onDismiss)
                    }
                }

                // Bottom hint & page indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentSlide > 0) "‹ Ketuk kiri untuk kembali" else "",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${currentSlide + 1} / $totalSlides",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (currentSlide < totalSlides - 1) "Ketuk kanan untuk lanjut ›" else "",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StorySlideIntro(monthLabel: String, txCount: Int, totalMoved: Double) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Twinkling night stars + crescent moon canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val w = size.width
            val h = size.height
            val moonCenter = Offset(w * 0.5f, h * 0.52f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFDE047).copy(alpha = 0.25f), Color.Transparent),
                    center = moonCenter,
                    radius = 54.dp.toPx()
                ),
                radius = 54.dp.toPx(),
                center = moonCenter
            )
            drawCircle(
                color = Color(0xFFFDE047),
                radius = 22.dp.toPx(),
                center = moonCenter
            )
            drawCircle(
                color = Color(0xFF0C1B33),
                radius = 18.dp.toPx(),
                center = Offset(moonCenter.x + 10.dp.toPx(), moonCenter.y - 6.dp.toPx())
            )
            val stars = listOf(
                Offset(w * 0.18f, h * 0.28f) to 2.5.dp.toPx(),
                Offset(w * 0.29f, h * 0.65f) to 2.dp.toPx(),
                Offset(w * 0.72f, h * 0.25f) to 3.dp.toPx(),
                Offset(w * 0.83f, h * 0.58f) to 2.2.dp.toPx(),
                Offset(w * 0.65f, h * 0.78f) to 1.8.dp.toPx(),
                Offset(w * 0.36f, h * 0.2f) to 2.dp.toPx()
            )
            stars.forEach { (pos, r) ->
                drawCircle(Color.White.copy(alpha = 0.8f), radius = r, center = pos)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF38BDF8).copy(alpha = 0.18f),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
        ) {
            Text(
                text = monthLabel.uppercase(),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7DD3FC),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Bulan Anda,\ndibuka.",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 38.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Sepanjang $monthLabel, Anda mencatat $txCount transaksi dengan perputaran dana sebesar ${CurrencyHelper.formatRupiah(totalMoved)}.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun StorySlideFlow(income: Double, expense: Double, net: Double, savingsRate: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        // Warm horizon sun rays illustration
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
        ) {
            val sunCenter = Offset(size.width * 0.5f, size.height * 0.85f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFBBF24).copy(alpha = 0.45f), Color.Transparent),
                    center = sunCenter,
                    radius = 65.dp.toPx()
                ),
                radius = 65.dp.toPx(),
                center = sunCenter
            )
            drawArc(
                color = Color(0xFFFDE047),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(sunCenter.x - 28.dp.toPx(), sunCenter.y - 28.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(56.dp.toPx(), 56.dp.toPx())
            )
            drawLine(
                color = Color(0xFF38BDF8).copy(alpha = 0.6f),
                start = Offset(0f, sunCenter.y),
                end = Offset(size.width, sunCenter.y),
                strokeWidth = 2.dp.toPx()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Alirannya", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (net >= 0) "Arus kas Anda positif dan menyisakan tabungan bersih." else "Pengeluaran bulan ini melampaui pemasukan Anda.",
            fontSize = 13.5.sp,
            color = Color.White.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(20.dp))

        StoryMetricTile("Uang Masuk", CurrencyHelper.formatRupiah(income), Color(0xFF34D399))
        Spacer(modifier = Modifier.height(10.dp))
        StoryMetricTile("Uang Keluar", CurrencyHelper.formatRupiah(expense), Color(0xFFFB7185))
        Spacer(modifier = Modifier.height(10.dp))
        StoryMetricTile(
            label = "Selisih Bersih (${savingsRate}% ditabung)",
            value = (if (net >= 0) "+" else "") + CurrencyHelper.formatRupiah(net),
            color = if (net >= 0) Color(0xFF38BDF8) else Color(0xFFFBBF24)
        )
    }
}

@Composable
private fun StorySlideWhereItWent(slices: List<CategorySlice>) {
    val top4 = slices.take(4)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Kemana perginya", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Pos pengeluaran paling dominan di taman keuangan Anda bulan ini.",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(22.dp))

        if (top4.isEmpty()) {
            Text("Belum ada pengeluaran tercatat.", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
        } else {
            val maxSlice = (top4.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(1.0)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                val count = top4.size
                val slot = size.width / (count + 1)
                top4.forEachIndexed { idx, slice ->
                    val cx = slot * (idx + 1)
                    val ratio = (slice.amount / maxSlice).toFloat().coerceIn(0.25f, 1f)
                    val topY = size.height - (ratio * (size.height - 34.dp.toPx()))
                    drawLine(
                        color = Color(0xFF34D399).copy(alpha = 0.78f),
                        start = Offset(cx, size.height),
                        end = Offset(cx, topY),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Leaves along stem
                    val midY = (size.height + topY) / 2f
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = 0.75f),
                        radius = 5.5.dp.toPx(),
                        center = Offset(cx + 8.dp.toPx(), midY)
                    )
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = 0.75f),
                        radius = 5.dp.toPx(),
                        center = Offset(cx - 8.dp.toPx(), midY + 12.dp.toPx())
                    )
                    // Petals + center
                    val r = 15.dp.toPx()
                    for (angleDeg in listOf(0f, 60f, 120f, 180f, 240f, 300f)) {
                        val rad = Math.toRadians(angleDeg.toDouble())
                        val px = cx + (Math.cos(rad) * r * 0.65).toFloat()
                        val py = topY + (Math.sin(rad) * r * 0.65).toFloat()
                        drawCircle(
                            color = slice.color.copy(alpha = 0.88f),
                            radius = 9.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                    drawCircle(
                        color = Color(0xFFFDE047),
                        radius = 6.dp.toPx(),
                        center = Offset(cx, topY)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            top4.forEach { s ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(s.color, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(s.category, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Text(
                        text = "${CurrencyHelper.formatCompactRupiah(s.amount)} (${s.percentage.toInt()}%)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StorySlideBigOnes(topExpenses: List<Transaction>, totalExpense: Double) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        // Floating hot-air balloon canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
        ) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.42f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFB7185).copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = 48.dp.toPx()
                ),
                radius = 48.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(color = Color(0xFFFB7185), radius = 22.dp.toPx(), center = Offset(cx, cy))
            drawCircle(color = Color(0xFFFDA4AF).copy(alpha = 0.55f), radius = 12.dp.toPx(), center = Offset(cx - 5.dp.toPx(), cy - 4.dp.toPx()))
            // Strings + basket
            drawLine(Color.White.copy(alpha = 0.6f), Offset(cx - 10.dp.toPx(), cy + 18.dp.toPx()), Offset(cx - 5.dp.toPx(), size.height - 14.dp.toPx()), 1.5.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.6f), Offset(cx + 10.dp.toPx(), cy + 18.dp.toPx()), Offset(cx + 5.dp.toPx(), size.height - 14.dp.toPx()), 1.5.dp.toPx())
            drawRoundRect(
                color = Color(0xFFFBBF24),
                topLeft = Offset(cx - 8.dp.toPx(), size.height - 14.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 10.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Yang besar", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Transaksi pengeluaran tunggal paling menonjol di bulan ini.",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (topExpenses.isEmpty()) {
            Text("Belum ada transaksi pengeluaran.", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
        } else {
            topExpenses.forEachIndexed { idx, tx ->
                val share = if (totalExpense > 0.0) ((tx.amount / totalExpense) * 100).toInt() else 0
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = if (idx == 0) 0.14f else 0.07f),
                    border = if (idx == 0) BorderStroke(1.dp, Color(0xFFFB7185).copy(alpha = 0.6f)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.title.ifBlank { tx.category },
                                fontSize = if (idx == 0) 15.5.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${tx.category} · Tgl ${tx.dayOfMonth} · $share% dari total",
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.68f)
                            )
                        }
                        Text(
                            text = CurrencyHelper.formatCompactRupiah(tx.amount),
                            fontSize = if (idx == 0) 15.5.sp else 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFB7185)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorySlideRhythm(avgPerDay: Double, activeDays: Set<Int>, daysInMonth: Int, streak: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        // Night-sky constellation connecting active tracked days
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
        ) {
            val sortedDays = activeDays.sorted().take(12)
            val step = size.width / (sortedDays.size.coerceAtLeast(2) + 1)
            val points = sortedDays.mapIndexed { idx, d ->
                val yOffset = ((d * 37) % 50) / 50f
                Offset(step * (idx + 1), 16.dp.toPx() + yOffset * (size.height - 32.dp.toPx()))
            }
            for (i in 0 until points.lastIndex) {
                drawLine(
                    color = Color(0xFF38BDF8).copy(alpha = 0.45f),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 1.8.dp.toPx()
                )
            }
            points.forEach { pt ->
                drawCircle(color = Color(0xFF7DD3FC).copy(alpha = 0.35f), radius = 6.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = pt)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("Ritme Anda", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Konsistensi pencatatan dan kecepatan belanja harian Anda.",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        StoryMetricTile("Rata-rata Pengeluaran / Hari", CurrencyHelper.formatRupiah(avgPerDay), Color(0xFF7DD3FC))
        Spacer(modifier = Modifier.height(10.dp))
        StoryMetricTile("Hari Aktif Tercatat", "${activeDays.size} dari $daysInMonth hari", Color(0xFF34D399))
        Spacer(modifier = Modifier.height(10.dp))
        StoryMetricTile("Rentetan Pencatatan Terpanjang", "$streak hari berturut-turut", Color(0xFFFBBF24))
    }
}

@Composable
private fun StorySlideThenAndNow(curIncome: Double, prevIncome: Double, curExpense: Double, prevExpense: Double) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        // Twin peaks mountain skyline + connecting dotted arc
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
        ) {
            val h = size.height
            val w = size.width
            val peak1 = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.1f, h)
                lineTo(w * 0.35f, h * 0.35f)
                lineTo(w * 0.6f, h)
                close()
            }
            val peak2 = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.42f, h)
                lineTo(w * 0.68f, h * 0.2f)
                lineTo(w * 0.92f, h)
                close()
            }
            drawPath(peak1, color = Color(0xFF38BDF8).copy(alpha = 0.28f))
            drawPath(peak2, color = Color(0xFF34D399).copy(alpha = 0.34f))
            drawLine(
                color = Color(0xFFFDE047).copy(alpha = 0.7f),
                start = Offset(w * 0.35f, h * 0.35f),
                end = Offset(w * 0.68f, h * 0.2f),
                strokeWidth = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )
            drawCircle(Color(0xFF38BDF8), radius = 4.dp.toPx(), center = Offset(w * 0.35f, h * 0.35f))
            drawCircle(Color(0xFF34D399), radius = 4.5.dp.toPx(), center = Offset(w * 0.68f, h * 0.2f))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("Dulu dan\nsekarang", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 34.sp)
        Spacer(modifier = Modifier.height(16.dp))

        val incDiffPct = if (prevIncome > 0) (((curIncome - prevIncome) / prevIncome) * 100).toInt() else 0
        val expDiffPct = if (prevExpense > 0) (((curExpense - prevExpense) / prevExpense) * 100).toInt() else 0

        StoryMetricTile(
            label = "Pemasukan vs Bulan Lalu (${if (incDiffPct >= 0) "+" else ""}$incDiffPct%)",
            value = "${CurrencyHelper.formatCompactRupiah(prevIncome)} → ${CurrencyHelper.formatCompactRupiah(curIncome)}",
            color = Color(0xFF34D399)
        )
        Spacer(modifier = Modifier.height(12.dp))
        StoryMetricTile(
            label = "Pengeluaran vs Bulan Lalu (${if (expDiffPct >= 0) "+" else ""}$expDiffPct%)",
            value = "${CurrencyHelper.formatCompactRupiah(prevExpense)} → ${CurrencyHelper.formatCompactRupiah(curExpense)}",
            color = if (curExpense <= prevExpense) Color(0xFF34D399) else Color(0xFFFB7185)
        )
    }
}

@Composable
private fun StorySlideCarryForward(monthLabel: String, netFlow: Double, savingsRate: Int, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating sky lanterns (Lampion) canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val lanterns = listOf(
                Triple(size.width * 0.25f, size.height * 0.62f, 12.dp.toPx()),
                Triple(size.width * 0.5f, size.height * 0.38f, 16.dp.toPx()),
                Triple(size.width * 0.76f, size.height * 0.55f, 13.dp.toPx())
            )
            lanterns.forEach { (lx, ly, r) ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFBBF24).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(lx, ly),
                        radius = r * 2.2f
                    ),
                    radius = r * 2.2f,
                    center = Offset(lx, ly)
                )
                drawRoundRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(lx - r * 0.7f, ly - r),
                    size = androidx.compose.ui.geometry.Size(r * 1.4f, r * 1.8f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFFFEF08A),
                    radius = r * 0.38f,
                    center = Offset(lx, ly + r * 0.3f)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Bawanya maju",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (netFlow >= 0) {
                "Selamat! Pada $monthLabel Anda mempertahankan rasio menabung $savingsRate% dan menjaga arus kas tetap sehat."
            } else {
                "Gunakan rincian kategori di $monthLabel ini untuk merapikan anggaran bulan berikutnya agar kembali surplus."
            },
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDone,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_story_finish")
        ) {
            Text(
                text = "Tutup buku harian",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0A1628)
            )
        }
    }
}

@Composable
private fun StoryMetricTile(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.72f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}
