package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountWallet
import com.example.data.model.MainCategory
import com.example.data.model.TransactionType
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TrophyYellow
import com.example.util.CategoryIconHelper
import com.example.util.ComputedBudgetItem
import com.example.util.CurrencyHelper
import com.example.util.InstitutionLogoBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaturanTampilanAnggaranSheet(
    currentSummaryStyle: String,
    currentListStyle: String,
    onSelectSummaryStyle: (String) -> Unit,
    onSelectListStyle: (String) -> Unit,
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
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Pengaturan tampilan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Kartu ringkasan",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DisplayOptionCard(
                    title = "Bilah",
                    subtitle = "Jumlah besar dengan bilah kemajuan",
                    isSelected = currentSummaryStyle == "Bilah",
                    onClick = { onSelectSummaryStyle("Bilah") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("option_summary_bilah"),
                    iconContent = { tint ->
                        Canvas(modifier = Modifier.size(26.dp)) {
                            val h = size.height / 5f
                            drawRoundRect(
                                color = tint,
                                topLeft = Offset(0f, h * 0.4f),
                                size = Size(size.width * 0.45f, h * 1.1f),
                                cornerRadius = CornerRadius(h / 2f, h / 2f)
                            )
                            drawRoundRect(
                                color = tint,
                                topLeft = Offset(0f, h * 2.1f),
                                size = Size(size.width * 0.95f, h * 1.1f),
                                cornerRadius = CornerRadius(h / 2f, h / 2f)
                            )
                            drawRoundRect(
                                color = tint.copy(alpha = 0.45f),
                                topLeft = Offset(size.width * 0.35f, h * 3.7f),
                                size = Size(size.width * 0.6f, h * 0.9f),
                                cornerRadius = CornerRadius(h / 2f, h / 2f)
                            )
                        }
                    }
                )

                DisplayOptionCard(
                    title = "Sirkular",
                    subtitle = "Pengeluaran ditampilkan sebagai cincin",
                    isSelected = currentSummaryStyle == "Sirkular",
                    onClick = { onSelectSummaryStyle("Sirkular") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("option_summary_sirkular"),
                    iconContent = { tint ->
                        Canvas(modifier = Modifier.size(26.dp)) {
                            drawCircle(
                                color = tint,
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Daftar anggaran",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DisplayOptionCard(
                    title = "Kisi",
                    subtitle = "Kartu luas, satu per anggaran",
                    isSelected = currentListStyle == "Kisi",
                    onClick = { onSelectListStyle("Kisi") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("option_list_kisi"),
                    iconContent = { tint ->
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Kisi",
                            tint = tint,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                )

                DisplayOptionCard(
                    title = "Tabel",
                    subtitle = "Baris spreadsheet kompak",
                    isSelected = currentListStyle == "Tabel",
                    onClick = { onSelectListStyle("Tabel") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("option_list_tabel"),
                    iconContent = { tint ->
                        Canvas(modifier = Modifier.size(26.dp)) {
                            val barH = size.height / 5.5f
                            for (row in 0 until 3) {
                                val y = row * (barH * 1.7f) + barH * 0.4f
                                drawRoundRect(
                                    color = tint,
                                    topLeft = Offset(0f, y),
                                    size = Size(size.width, barH),
                                    cornerRadius = CornerRadius(barH / 2f, barH / 2f)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DisplayOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable (Color) -> Unit
) {
    val activeBorder = if (isSelected) AccentBlue else DarkCardBorder.copy(alpha = 0.6f)
    val bg = if (isSelected) AccentBlue.copy(alpha = 0.08f) else DarkSurfaceElevated
    val iconColor = if (isSelected) AccentBlueBright else TextSubtle

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, activeBorder),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                iconContent(iconColor)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) AccentBlueBright else TextWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = TextMuted,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilihAkunBudgetSheet(
    wallets: List<AccountWallet>,
    initialSelectedAccounts: List<String>, // empty = Semua Akun
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectAll by remember { mutableStateOf(initialSelectedAccounts.isEmpty()) }
    var selectedSet by remember {
        mutableStateOf(
            if (initialSelectedAccounts.isEmpty()) wallets.map { it.name }.toSet()
            else initialSelectedAccounts.toSet()
        )
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentBlue.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = AccentBlueBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Pilih Akun",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = DarkSurfaceElevated,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    val allChecked = selectAll || (wallets.isNotEmpty() && selectedSet.size == wallets.size)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (allChecked) AccentBlue.copy(alpha = 0.08f) else DarkSurfaceElevated,
                        border = BorderStroke(
                            width = if (allChecked) 1.5.dp else 1.dp,
                            color = if (allChecked) AccentBlue else DarkCardBorder.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (allChecked) {
                                    selectAll = false
                                    selectedSet = emptySet()
                                } else {
                                    selectAll = true
                                    selectedSet = wallets.map { it.name }.toSet()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DarkSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = null,
                                            tint = AccentBlueBright,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Semua Akun",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = "Pilih Semua",
                                        fontSize = 11.5.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            SelectionCheckCircle(checked = allChecked)
                        }
                    }
                }

                items(wallets) { wallet ->
                    val isChecked = selectAll || wallet.name in selectedSet
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isChecked) AccentBlue.copy(alpha = 0.08f) else DarkSurfaceElevated,
                        border = BorderStroke(
                            width = if (isChecked) 1.5.dp else 1.dp,
                            color = if (isChecked) AccentBlue else DarkCardBorder.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectAll = false
                                selectedSet = if (wallet.name in selectedSet) {
                                    selectedSet - wallet.name
                                } else {
                                    selectedSet + wallet.name
                                }
                                if (wallets.isNotEmpty() && selectedSet.size == wallets.size) {
                                    selectAll = true
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        InstitutionLogoBadge(
                                            walletName = wallet.name,
                                            iconName = wallet.iconName,
                                            size = 34.dp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = wallet.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = CurrencyHelper.formatRupiah(wallet.balance),
                                        fontSize = 11.5.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            SelectionCheckCircle(checked = isChecked)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val result = if (selectAll || selectedSet.isEmpty() || selectedSet.size == wallets.size) {
                        emptyList()
                    } else {
                        selectedSet.toList()
                    }
                    onConfirm(result)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Selesai",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SelectionCheckCircle(checked: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (checked) AccentBlue else Color.Transparent,
        border = if (checked) null else BorderStroke(1.5.dp, TextSubtle),
        modifier = Modifier.size(24.dp)
    ) {
        if (checked) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Dipilih",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

private data class FrequencyMeta(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilihFrekuensiBudgetSheet(
    selectedFrequency: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val items = remember {
        listOf(
            FrequencyMeta("Harian", "Lacak pengeluaran setiap hari", Icons.Default.CalendarToday),
            FrequencyMeta("Mingguan", "Lacak pengeluaran setiap minggu", Icons.Default.ViewWeek),
            FrequencyMeta("Bulanan", "Lacak pengeluaran setiap bulan", Icons.Default.CalendarMonth),
            FrequencyMeta("Triwulanan", "Pantau pengeluaran tiap kuartal", Icons.Default.GridView),
            FrequencyMeta("Tahunan", "Pantau pengeluaran sepanjang tahun", Icons.Default.EventRepeat),
            FrequencyMeta("Rentang khusus", "Anggaran sekali pakai untuk tanggal yang kamu pilih", Icons.Default.DateRange)
        )
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 26.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentBlue.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = AccentBlueBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Pilih Frekuensi",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = DarkSurfaceElevated,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { freq ->
                    val isSelected = selectedFrequency == freq.title
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) AccentBlue.copy(alpha = 0.10f) else DarkSurfaceElevated,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AccentBlue else DarkCardBorder.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelect(freq.title) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) AccentBlue.copy(alpha = 0.18f) else DarkSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = freq.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) AccentBlueBright else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = freq.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = freq.subtitle,
                                        fontSize = 11.5.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            if (isSelected) {
                                SelectionCheckCircle(checked = true)
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
fun PilihKategoriSubkategoriSheet(
    categories: List<MainCategory>,
    selectedCategories: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentSelection by remember { mutableStateOf(selectedCategories.toSet()) }
    var expandedMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    val expenseCategories = remember(categories) {
        categories.filter { it.type == TransactionType.EXPENSE && it.isVisible }
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Semua kategori dan subkategori",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenseCategories) { mainCat ->
                    val catColor = CategoryIconHelper.getCategoryColor(mainCat.name, TransactionType.EXPENSE)
                    val isMainSelected = mainCat.name in currentSelection
                    val isExpanded = expandedMap[mainCat.id] ?: false

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (isMainSelected) AccentBlue else DarkCardBorder.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentSelection = if (isMainSelected) {
                                            currentSelection - mainCat.name
                                        } else {
                                            currentSelection + mainCat.name
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = catColor.copy(alpha = 0.16f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getCategoryIcon(mainCat.name, mainCat.iconName),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = mainCat.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (mainCat.subcategories.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                expandedMap = expandedMap.toMutableMap().apply {
                                                    put(mainCat.id, !isExpanded)
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Subkategori",
                                                tint = TextMuted
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SelectionCheckCircle(checked = isMainSelected)
                                }
                            }

                            if (isExpanded && mainCat.subcategories.isNotEmpty()) {
                                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                                Column(modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 8.dp)) {
                                    mainCat.subcategories.filter { it.isVisible }.forEach { sub ->
                                        val isSubSelected = sub.name in currentSelection
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    currentSelection = if (isSubSelected) {
                                                        currentSelection - sub.name
                                                    } else {
                                                        currentSelection + sub.name
                                                    }
                                                }
                                                .padding(vertical = 8.dp, horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = CategoryIconHelper.getCategoryIcon(sub.name, sub.iconName),
                                                    contentDescription = null,
                                                    tint = catColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = sub.name,
                                                    fontSize = 13.sp,
                                                    color = TextWhite
                                                )
                                            }
                                            SelectionCheckCircle(checked = isSubSelected)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { onConfirm(currentSelection.toList()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Terapkan (${currentSelection.size})", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailSheet(
    item: ComputedBudgetItem,
    daysRemaining: Int,
    warningPercent: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val budget = item.budget
    val primaryCat = budget.targetCategories.firstOrNull() ?: budget.category
    val catColor = CategoryIconHelper.getCategoryColor(primaryCat, TransactionType.EXPENSE)
    val isOver = item.spent > budget.allocated && budget.allocated > 0
    val isWarning = !isOver && item.rawPercentInt >= warningPercent
    val progressColor = when {
        isOver -> ExpenseRed
        isWarning -> TrophyYellow
        else -> Color(0xFF56B88D)
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
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = catColor.copy(alpha = 0.16f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = CategoryIconHelper.getCategoryIcon(primaryCat),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = budget.displayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val accLabel = if (budget.accountNames.isEmpty()) "Semua Akun" else budget.accountNames.joinToString(", ")
                        Text(
                            text = "${budget.frequency} · $accLabel",
                            fontSize = 11.5.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkBackground,
                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Terpakai", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = CurrencyHelper.formatRupiah(item.spent),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = progressColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Batas Anggaran", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = CurrencyHelper.formatRupiah(budget.allocated),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { item.percentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = DarkSurfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isOver) "Melebihi ${CurrencyHelper.formatRupiah(item.overAmount)}" else "Sisa ${CurrencyHelper.formatRupiah(item.remaining)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isOver) ExpenseRed else IncomeGreen
                        )
                        val dailyAllow = item.remaining / daysRemaining.coerceAtLeast(1)
                        Text(
                            text = if (!isOver) "${CurrencyHelper.formatRupiah(dailyAllow)}/hari" else "${item.rawPercentInt}%",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Transaksi Terkait (${item.matchingTransactions.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (item.matchingTransactions.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada pengeluaran tercatat di kategori ini pada periode terpilih.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(item.matchingTransactions) { tx ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurfaceElevated
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.title.ifBlank { tx.category },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${tx.dayOfMonth}/${tx.monthIndex}/${tx.year} · ${tx.accountName}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                                Text(
                                    text = "-${CurrencyHelper.formatRupiah(tx.amount)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hapus", color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ubah", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DarkSurface,
            title = { Text("Hapus Anggaran?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Anggaran \"${budget.displayName}\" akan dihapus. Riwayat transaksi Anda tetap aman.",
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Hapus", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", color = TextMuted)
                }
            }
        )
    }
}
