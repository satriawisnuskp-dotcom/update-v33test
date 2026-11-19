package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.util.CurrencyHelper

/**
 * Reusable, modern historical chart detail card.
 * Displays the selected data point's value, label, previous historical point,
 * and historical delta/comparison with clean badges, borders, and typography.
 */
@Composable
fun ChartHistoricalDetailCard(
    title: String,
    currentLabel: String,
    currentValue: Double,
    previousLabel: String? = null,
    previousValue: Double? = null,
    isReverseGood: Boolean = false, // True for expense or debt where lower is better
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val delta = if (previousValue != null) currentValue - previousValue else 0.0
    val pctChange = if (previousValue != null && previousValue != 0.0) {
        (delta / Math.abs(previousValue)) * 100.0
    } else if (previousValue != null && previousValue == 0.0 && currentValue > 0.0) {
        100.0
    } else 0.0

    // Determine status: is this change positive/favorable?
    val isPositiveTrend = if (isReverseGood) delta < -1.0 else delta > 1.0
    val isNegativeTrend = if (isReverseGood) delta > 1.0 else delta < -1.0

    val trendColor = when {
        isPositiveTrend -> IncomeGreen
        isNegativeTrend -> ExpenseRed
        else -> TextMuted
    }

    val trendBg = when {
        isPositiveTrend -> IncomeGreenSoft
        isNegativeTrend -> ExpenseRedSoft
        else -> DarkSurfaceVariant
    }

    val trendBorder = when {
        isPositiveTrend -> IncomeGreen.copy(alpha = 0.4f)
        isNegativeTrend -> ExpenseRed.copy(alpha = 0.4f)
        else -> DarkCardBorder
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Category/Title badge + Dismiss button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = AccentBlueBright,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        maxLines = 1
                    )
                }

                if (onDismiss != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { onDismiss() }
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main value row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "NILAI TITIK TERPILIH",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = TextSubtle
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyHelper.formatRupiah(currentValue),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentValue < 0.0) ExpenseRed else TextWhite
                    )
                }

                // Trend badge
                if (previousValue != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = trendBg,
                        border = BorderStroke(1.dp, trendBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    delta > 0 -> Icons.AutoMirrored.Filled.TrendingUp
                                    delta < 0 -> Icons.AutoMirrored.Filled.TrendingDown
                                    else -> Icons.Default.Remove
                                },
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val pctStr = String.format(java.util.Locale.US, "%.1f", Math.abs(pctChange))
                            val sign = if (delta > 0) "+" else if (delta < 0) "-" else ""
                            Text(
                                text = "$sign$pctStr%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = trendColor
                            )
                        }
                    }
                }
            }

            // Historical comparison section
            if (previousValue != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DATA SEBELUMNYA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = TextSubtle
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyHelper.formatRupiah(previousValue),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (previousValue < 0.0) ExpenseRed else TextWhite
                        )
                        if (!previousLabel.isNullOrBlank()) {
                            Text(
                                text = previousLabel,
                                fontSize = 10.sp,
                                color = TextSubtle
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "SELISIH HISTORIS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = TextSubtle
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val deltaSign = if (delta > 0) "+" else if (delta < 0) "-" else ""
                        Text(
                            text = "$deltaSign${CurrencyHelper.formatRupiah(Math.abs(delta))}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = trendColor
                        )
                        Text(
                            text = when {
                                delta > 0 -> "Naik dibanding sebelumnya"
                                delta < 0 -> "Turun dibanding sebelumnya"
                                else -> "Sama dengan sebelumnya"
                            },
                            fontSize = 10.sp,
                            color = trendColor
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Titik awal pemantauan data historis",
                    fontSize = 10.sp,
                    color = TextSubtle
                )
            }
        }
    }
}
