package com.example.ui.dialogs

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.model.defaultDashboardWidgetOrder
import com.example.data.model.defaultStatistikCardOrder
import com.example.ui.components.FullScreenModalContainer
import com.example.ui.components.fullScreenBottomSafePadding
import com.example.ui.components.fullScreenTopSafePadding
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

data class StatCardConfig(
    val id: String,
    val title: String,
    val icon: String = "📊",
    val isVisible: Boolean = true
)

object StatCardCatalog {
    private val metadata = mapOf(
        "TREND_INCOME" to ("Tren Pemasukan" to "📈"),
        "TREND_EXPENSE" to ("Tren Pengeluaran" to "📉"),
        "EXPENSE_BREAKDOWN" to ("Pengeluaran" to "🍩"),
        "EXPENSE_PER_LABEL" to ("Pengeluaran per label" to "🏷️"),
        "UPCOMING_TIMELINE" to ("Yang akan datang" to "🗓️"),
        "CASH_FLOW" to ("Arus Kas" to "💸"),
        "INCOME_BREAKDOWN" to ("Pemasukan" to "💰"),
        "EARNINGS_SUMMARY" to ("Ringkasan Penghasilan" to "💵"),
        "SAVINGS_RATE" to ("Rasio menabung" to "🐷"),
        "DAILY_AVERAGE" to ("Rata-rata harian" to "📅"),
        "PERIOD_COMPARISON" to ("Periode ini vs lalu" to "⚖️"),
        "MOST_CHANGED_CATEGORY" to ("Kategori paling berubah" to "🔀"),
        "DAILY_PATTERN" to ("Pola harian" to "☀️"),
        "LARGEST_EXPENSES" to ("Pengeluaran terbesar" to "🔥"),
        "UPCOMING_BILLS" to ("Tagihan mendatang" to "🧾"),
        "PERIOD_HIGHLIGHT" to ("Sorotan periode" to "✨"),
        "MONEY_FLOW_SANKEY" to ("Aliran uang" to "🌊"),
        "FIXED_COSTS" to ("Biaya tetap" to "🔒"),
        "CASH_FLOW_JOURNEY" to ("Perjalanan arus kas" to "🧭"),
        "EXPENSE_PER_ACCOUNT" to ("Pengeluaran per akun" to "🏦"),
        "MONEY_PATH" to ("Jalan Uang" to "🛤️")
    )

    fun buildConfigs(order: List<String>, hidden: List<String>): List<StatCardConfig> {
        val hiddenSet = hidden.toSet()
        val fullOrder = (order + defaultStatistikCardOrder).distinct()
        return fullOrder.mapNotNull { id ->
            val meta = metadata[id] ?: return@mapNotNull null
            StatCardConfig(
                id = id,
                title = meta.first,
                icon = meta.second,
                isVisible = id !in hiddenSet
            )
        }
    }

    fun getDashboardWidgetMeta(id: String): Pair<String, String> = when (id) {
        "SAFE_TO_SPEND" -> "Aman Dibelanjakan Hari Ini" to "💳"
        "BUDGET" -> "Anggaran" to "📊"
        "UPCOMING" -> "Yang akan datang" to "🗓️"
        "SPENDING_ACTIVITY" -> "Aktivitas Pengeluaran" to "📅"
        else -> id to "📌"
    }
}

@Composable
fun CustomizeStatisticsDialog(
    initialCards: List<StatCardConfig>,
    isSideBySideTrends: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<StatCardConfig>, Boolean) -> Unit
) {
    BackHandler { onDismiss() }

    var cards by remember { mutableStateOf(initialCards) }
    var sideBySide by remember { mutableStateOf(isSideBySideTrends) }
    val visibleCards = remember(cards) { cards.filter { it.isVisible } }
    val hiddenCards = remember(cards) { cards.filter { !it.isVisible } }

    FullScreenModalContainer(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .fullScreenTopSafePadding()
            ) {
                // Header: [X] Sesuaikan Statistik    Atur ulang   [Selesai]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_customize_stats")) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = TextWhite)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Sesuaikan Statistik",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${visibleCards.size} dari ${cards.size} kartu aktif",
                                fontSize = 11.sp,
                                color = TextSubtle
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Atur ulang",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    cards = StatCardCatalog.buildConfigs(defaultStatistikCardOrder, emptyList())
                                    sideBySide = true
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("btn_reset_customize_stats")
                        )

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = AccentBlue,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    onSave(cards, sideBySide)
                                    onDismiss()
                                }
                                .testTag("btn_save_customize_stats")
                        ) {
                            Text(
                                text = "Selesai",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(AccentBlue.copy(alpha = 0.18f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("📊", fontSize = 15.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Tren berdampingan",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                        Text(
                                            text = "Pemasukan & Pengeluaran dalam 1 baris",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Switch(
                                    checked = sideBySide,
                                    onCheckedChange = { sideBySide = it },
                                    modifier = Modifier.testTag("switch_side_by_side_trends"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = IncomeGreen,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = DarkSurfaceElevated
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "KARTU AKTIF (${visibleCards.size})",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSubtle,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    itemsIndexed(visibleCards, key = { _, item -> item.id }) { visibleIdx, card ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = null,
                                        tint = TextSubtle,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = card.icon, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = card.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (visibleIdx > 0) {
                                                val prevVisibleId = visibleCards[visibleIdx - 1].id
                                                val mutable = cards.toMutableList()
                                                val curPos = mutable.indexOfFirst { it.id == card.id }
                                                val prevPos = mutable.indexOfFirst { it.id == prevVisibleId }
                                                if (curPos >= 0 && prevPos >= 0) {
                                                    val item = mutable.removeAt(curPos)
                                                    mutable.add(prevPos, item)
                                                    cards = mutable
                                                }
                                            }
                                        },
                                        enabled = visibleIdx > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Naikkan",
                                            tint = if (visibleIdx > 0) TextMuted else TextSubtle.copy(alpha = 0.35f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (visibleIdx < visibleCards.lastIndex) {
                                                val nextVisibleId = visibleCards[visibleIdx + 1].id
                                                val mutable = cards.toMutableList()
                                                val curPos = mutable.indexOfFirst { it.id == card.id }
                                                val nextPos = mutable.indexOfFirst { it.id == nextVisibleId }
                                                if (curPos >= 0 && nextPos >= 0) {
                                                    val item = mutable.removeAt(curPos)
                                                    mutable.add(nextPos, item)
                                                    cards = mutable
                                                }
                                            }
                                        },
                                        enabled = visibleIdx < visibleCards.lastIndex,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Turunkan",
                                            tint = if (visibleIdx < visibleCards.lastIndex) TextMuted else TextSubtle.copy(alpha = 0.35f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Switch(
                                        checked = card.isVisible,
                                        onCheckedChange = { checked ->
                                            cards = cards.map {
                                                if (it.id == card.id) it.copy(isVisible = checked) else it
                                            }
                                        },
                                        modifier = Modifier.testTag("toggle_stat_card_${card.id}"),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = IncomeGreen,
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = DarkSurfaceElevated
                                        )
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(6.dp)) }
                }

                // Bottom tray: Hidden cards "Kartu lain — ketuk untuk menambah"
                Surface(
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fullScreenBottomSafePadding()
                            .padding(top = 10.dp, bottom = 14.dp)
                    ) {
                        Text(
                            text = if (hiddenCards.isNotEmpty()) {
                                "Kartu lain (${hiddenCards.size}) — ketuk untuk menambah"
                            } else {
                                "Semua kartu statistik aktif ditampilkan"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                        if (hiddenCards.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(hiddenCards, key = { it.id }) { hiddenCard ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = DarkSurfaceElevated,
                                        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable {
                                                val mutable = cards.toMutableList()
                                                mutable.removeAll { it.id == hiddenCard.id }
                                                mutable.add(hiddenCard.copy(isVisible = true))
                                                cards = mutable
                                            }
                                            .testTag("toggle_stat_card_${hiddenCard.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Tambah ${hiddenCard.title}",
                                                tint = AccentBlueBright,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(hiddenCard.icon, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = hiddenCard.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextWhite
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeDashboardWidgetsSheet(
    initialOrder: List<String>,
    initialHidden: List<String>,
    isBalanceHidden: Boolean,
    onToggleBalanceHidden: () -> Unit,
    onSave: (order: List<String>, hidden: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var widgetOrder by remember {
        mutableStateOf((initialOrder + defaultDashboardWidgetOrder).distinct())
    }
    var hiddenSet by remember { mutableStateOf(initialHidden.toSet()) }

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
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sesuaikan Dasbor",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Atur Ulang",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentBlue,
                    modifier = Modifier
                        .clickable {
                            widgetOrder = defaultDashboardWidgetOrder
                            hiddenSet = emptySet()
                        }
                        .padding(6.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sembunyikan Nominal Saldo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Text(
                            text = "Samarkan angka utama di kartu dasbor",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = isBalanceHidden,
                        onCheckedChange = { onToggleBalanceHidden() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentBlue,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurface
                        )
                    )
                }
            }

            widgetOrder.forEachIndexed { idx, widgetId ->
                val (label, emoji) = StatCardCatalog.getDashboardWidgetMeta(widgetId)
                val visible = widgetId !in hiddenSet
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(emoji, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (visible) TextWhite else TextMuted
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (idx > 0) {
                                        val m = widgetOrder.toMutableList()
                                        val w = m.removeAt(idx)
                                        m.add(idx - 1, w)
                                        widgetOrder = m
                                    }
                                },
                                enabled = idx > 0,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Naikkan widget",
                                    tint = if (idx > 0) TextMuted else TextSubtle.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (idx < widgetOrder.lastIndex) {
                                        val m = widgetOrder.toMutableList()
                                        val w = m.removeAt(idx)
                                        m.add(idx + 1, w)
                                        widgetOrder = m
                                    }
                                },
                                enabled = idx < widgetOrder.lastIndex,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Turunkan widget",
                                    tint = if (idx < widgetOrder.lastIndex) TextMuted else TextSubtle.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    hiddenSet = if (visible) hiddenSet + widgetId else hiddenSet - widgetId
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (visible) "Sembunyikan widget" else "Tampilkan widget",
                                    tint = if (visible) IncomeGreen else TextMuted,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(widgetOrder, hiddenSet.toList())
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 8.dp)
                    .height(48.dp)
                    .testTag("btn_save_dashboard_widgets"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Selesai", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
