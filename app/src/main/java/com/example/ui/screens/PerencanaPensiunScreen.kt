package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CurrencyHelper
import com.example.util.ThousandsSeparatorVisualTransformation
import java.util.Locale
import kotlin.math.pow

@Composable
fun PerencanaPensiunScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val totalAssets by viewModel.totalAssetsValue.collectAsState()
    val totalWalletBalance by viewModel.totalAllWalletsBalance.collectAsState()
    val monthlyExpenseActual by viewModel.monthlyExpense.collectAsState()

    var currentAge by remember { mutableFloatStateOf(30f) }
    var retirementAge by remember { mutableFloatStateOf(60f) }
    var annualReturnRate by remember { mutableFloatStateOf(7.0f) }
    var annualInflationRate by remember { mutableFloatStateOf(3.0f) }

    val initialCombinedWealth = (totalAssets + totalWalletBalance).coerceAtLeast(0.0)
    var hasEditedSavings by remember { mutableStateOf(false) }
    var currentSavingsText by remember {
        mutableStateOf(if (initialCombinedWealth > 0.0) initialCombinedWealth.toLong().toString() else "0")
    }
    LaunchedEffect(initialCombinedWealth) {
        if (!hasEditedSavings && initialCombinedWealth > 0.0) {
            currentSavingsText = initialCombinedWealth.toLong().toString()
        }
    }

    var monthlyContributionText by remember { mutableStateOf("1500000") }
    var hasEditedRetExpense by remember { mutableStateOf(false) }
    var monthlyExpenseRetirementText by remember {
        mutableStateOf(if (monthlyExpenseActual > 0.0) monthlyExpenseActual.toLong().toString() else "5000000")
    }
    LaunchedEffect(monthlyExpenseActual) {
        if (!hasEditedRetExpense && monthlyExpenseActual > 0.0) {
            monthlyExpenseRetirementText = monthlyExpenseActual.toLong().toString()
        }
    }

    val currentSavings = currentSavingsText.toDoubleOrNull() ?: 0.0
    val monthlyContribution = monthlyContributionText.toDoubleOrNull() ?: 0.0
    val monthlyRetExpense = monthlyExpenseRetirementText.toDoubleOrNull() ?: 0.0

    val yearsToRetire = (retirementAge - currentAge).coerceAtLeast(1f)
    val r = (annualReturnRate / 100.0)
    val infl = (annualInflationRate / 100.0)

    // Compound Interest Projection Formula:
    // FV = PV * (1+r)^t + PMT * [((1+r)^t - 1) / r] * 12
    val futureValue = remember(currentSavings, monthlyContribution, yearsToRetire, r) {
        val compoundPrincipal = currentSavings * (1.0 + r).pow(yearsToRetire.toDouble())
        val compoundAnnuity = if (r > 0) {
            (monthlyContribution * 12) * (((1.0 + r).pow(yearsToRetire.toDouble()) - 1.0) / r)
        } else {
            monthlyContribution * 12 * yearsToRetire.toDouble()
        }
        compoundPrincipal + compoundAnnuity
    }

    val presentPurchasingPower = remember(futureValue, yearsToRetire, infl) {
        futureValue / (1.0 + infl).pow(yearsToRetire.toDouble())
    }

    // FIRE 4% rule target in today's purchasing power & inflation-adjusted at retirement
    val fireTargetToday = remember(monthlyRetExpense) {
        monthlyRetExpense * 12.0 * 25.0
    }
    val fireReadinessPct = remember(presentPurchasingPower, fireTargetToday) {
        if (fireTargetToday > 0.0) ((presentPurchasingPower / fireTargetToday) * 100.0).toInt().coerceAtLeast(0) else 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = TextWhite
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Perencana pensiun",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Projected Value Card matching video
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Diproyeksikan pada usia ${retirementAge.toInt()}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyHelper.formatRupiah(futureValue),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sekitar ${CurrencyHelper.formatRupiah(presentPurchasingPower)} dalam nilai uang saat ini, setelah inflasi.",
                            fontSize = 11.5.sp,
                            color = TextSubtle,
                            lineHeight = 16.sp
                        )
                        if (fireTargetToday > 0.0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceElevated,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Target Dana Pensiun (Aturan 4%)", fontSize = 10.sp, color = TextSubtle)
                                        Text(CurrencyHelper.formatRupiah(fireTargetToday), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (fireReadinessPct >= 100) IncomeGreen.copy(alpha = 0.2f) else AccentBlue.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (fireReadinessPct >= 100) "Target Tercapai ($fireReadinessPct%)" else "Kesiapan $fireReadinessPct%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (fireReadinessPct >= 100) IncomeGreen else AccentBlueBright,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Curve Chart "Saldo per tahun"
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Saldo per tahun",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            val w = size.width
                            val h = size.height

                            val path = Path()
                            val fillPath = Path()

                            val pointsCount = 20
                            val points = mutableListOf<Offset>()

                            val denom = (1.0 + r).pow(yearsToRetire.toDouble()) - 1.0
                            for (i in 0 until pointsCount) {
                                val t = i.toFloat() / (pointsCount - 1)
                                val x = t * w
                                // Exponential growth curve with zero-rate guard
                                val expVal = (1.0 + r).pow((t * yearsToRetire).toDouble())
                                val normY = if (denom > 0.0) (expVal - 1.0) / denom else t.toDouble()
                                val y = h - (normY.toFloat() * (h * 0.85f)) - (h * 0.08f)
                                points.add(Offset(x, y.coerceIn(0f, h)))
                            }

                            path.moveTo(points.first().x, points.first().y)
                            fillPath.moveTo(0f, h)
                            fillPath.lineTo(points.first().x, points.first().y)

                            for (i in 1 until points.size) {
                                val p0 = points[i - 1]
                                val p1 = points[i]
                                val cx = (p0.x + p1.x) / 2f
                                path.quadraticTo(p0.x, p0.y, cx, (p0.y + p1.y) / 2f)
                                fillPath.quadraticTo(p0.x, p0.y, cx, (p0.y + p1.y) / 2f)
                            }
                            path.lineTo(points.last().x, points.last().y)
                            fillPath.lineTo(points.last().x, points.last().y)
                            fillPath.lineTo(w, h)
                            fillPath.close()

                            // Draw gradient under curve
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    listOf(AccentBlue.copy(alpha = 0.35f), Color.Transparent)
                                )
                            )

                            // Draw curve line
                            drawPath(
                                path = path,
                                color = AccentBlueBright,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Usia ${currentAge.toInt()}", fontSize = 11.sp, color = TextMuted)
                            Text("Usia ${retirementAge.toInt()}", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }

            // Section "Angka Anda"
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Angka Anda",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        // 1. Usia saat ini
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Usia saat ini", fontSize = 12.sp, color = TextMuted)
                                Text("${currentAge.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentBlueBright)
                            }
                            Slider(
                                value = currentAge,
                                onValueChange = {
                                    currentAge = it
                                    if (retirementAge <= currentAge) retirementAge = currentAge + 5f
                                },
                                valueRange = 18f..65f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentBlue,
                                    activeTrackColor = AccentBlue,
                                    inactiveTrackColor = DarkSurfaceElevated
                                )
                            )
                        }

                        // 2. Usia pensiun
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Usia pensiun", fontSize = 12.sp, color = TextMuted)
                                Text("${retirementAge.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentBlueBright)
                            }
                            Slider(
                                value = retirementAge,
                                onValueChange = { retirementAge = it },
                                valueRange = (currentAge + 1f)..80f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentBlue,
                                    activeTrackColor = AccentBlue,
                                    inactiveTrackColor = DarkSurfaceElevated
                                )
                            )
                        }

                        // 3. Perkiraan imbal hasil tahunan
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Perkiraan imbal hasil tahunan", fontSize = 12.sp, color = TextMuted)
                                Text(String.format(Locale.US, "%.1f%%", annualReturnRate), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                            Slider(
                                value = annualReturnRate,
                                onValueChange = { annualReturnRate = it },
                                valueRange = 1f..15f,
                                colors = SliderDefaults.colors(
                                    thumbColor = IncomeGreen,
                                    activeTrackColor = IncomeGreen,
                                    inactiveTrackColor = DarkSurfaceElevated
                                )
                            )
                        }

                        // 4. Perkiraan inflasi
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Perkiraan inflasi", fontSize = 12.sp, color = TextMuted)
                                Text(String.format(Locale.US, "%.1f%%", annualInflationRate), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                            }
                            Slider(
                                value = annualInflationRate,
                                onValueChange = { annualInflationRate = it },
                                valueRange = 1f..10f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFF97316),
                                    activeTrackColor = Color(0xFFF97316),
                                    inactiveTrackColor = DarkSurfaceElevated
                                )
                            )
                        }

                        // 5. Tabungan saat ini
                        OutlinedTextField(
                            value = currentSavingsText,
                            onValueChange = {
                                hasEditedSavings = true
                                currentSavingsText = it.filter { c -> c.isDigit() }
                            },
                            label = { Text("Tabungan saat ini") },
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
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 6. Kontribusi bulanan
                        OutlinedTextField(
                            value = monthlyContributionText,
                            onValueChange = { monthlyContributionText = it.filter { c -> c.isDigit() } },
                            label = { Text("Kontribusi bulanan (Yang Anda tambahkan setiap bulan)") },
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
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 7. Pengeluaran bulanan saat pensiun
                        OutlinedTextField(
                            value = monthlyExpenseRetirementText,
                            onValueChange = {
                                hasEditedRetExpense = true
                                monthlyExpenseRetirementText = it.filter { c -> c.isDigit() }
                            },
                            label = { Text("Pengeluaran bulanan saat pensiun (nilai uang saat ini)") },
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Footer note
            item {
                Text(
                    text = "Proyeksi sederhana, bukan nasihat keuangan.",
                    fontSize = 11.5.sp,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}
