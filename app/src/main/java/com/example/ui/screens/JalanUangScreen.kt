package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
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
import com.example.util.FinancialHealthHelper

data class HabitSubtask(
    val id: String,
    val title: String,
    val subtitle: String
)

data class HabitMilestone(
    val id: Int,
    val title: String,
    val color: Color,
    val subtasks: List<HabitSubtask>,
    val tips: String
)

@Composable
fun JalanUangScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val allTransactions by viewModel.allTransactions.collectAsState()
    val periodTransactions by viewModel.transactions.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val periodIncome by viewModel.monthlyIncome.collectAsState()
    val periodExpense by viewModel.monthlyExpense.collectAsState()

    val milestones = remember {
        listOf(
            HabitMilestone(
                id = 1,
                title = "1 · Arus kas",
                color = Color(0xFF1E3A5F),
                tips = "Arus kas positif adalah fondasi dari seluruh kebebasan finansial Anda. Kunci dari arus kas adalah mengetahui dengan persis ke mana uang Anda pergi setiap harinya.",
                subtasks = listOf(
                    HabitSubtask("1_1", "Catat transaksi pertama", "Entri pertama adalah sentuhan kebiasaan dalam bentuk miniatur."),
                    HabitSubtask("1_2", "Catat tiga hari berturut-turut", "Tiga hari berturut-turut tugas berubah menjadi ritme."),
                    HabitSubtask("1_3", "Catat sepuluh entri", "Sepuluh entri cukup untuk aplikasi menunjukkan pola kepada Anda."),
                    HabitSubtask("1_4", "Tambahkan catatan ke entri", "Sebuah catatan di sebelah angka akan sangat membantu evaluasi bulan depan."),
                    HabitSubtask("1_5", "Atur hari gajian atau auto-save", "Siklus Anda dimulai ketika gaji masuk, bukan hanya tanggal satu.")
                )
            ),
            HabitMilestone(
                id = 2,
                title = "2 · Perlindungan",
                color = Color(0xFF0F766E),
                tips = "Perlindungan memastikan Anda tidak terlempar ke jurang finansial saat musibah atau pengeluaran mendadak datang.",
                subtasks = listOf(
                    HabitSubtask("2_1", "Siapkan dana darurat 3-6 bulan", "Pondasi ketenangan menghadapi ketidakpastian hidup."),
                    HabitSubtask("2_2", "Amankan asuransi & pos kesehatan", "Mencegah biaya rumah sakit menggerus tabungan utama."),
                    HabitSubtask("2_3", "Pisahkan dompet tabungan & operasional", "Uang belanja tidak bercampur dengan uang simpanan."),
                    HabitSubtask("2_4", "Catat pengeluaran & anggaran bulanan", "Mengetahui biaya hidup minimum yang wajib dipenuhi.")
                )
            ),
            HabitMilestone(
                id = 3,
                title = "3 · Utang",
                color = Color(0xFF155E75),
                tips = "Bebas dari bunga utang konsumtif memberi ruang napas bagi tabungan dan investasi jangka panjang Anda.",
                subtasks = listOf(
                    HabitSubtask("3_1", "Daftar semua kewajiban & pinjaman", "Melihat total utang secara jujur dan transparan."),
                    HabitSubtask("3_2", "Tentukan tenggat pelunasan utang", "Lengkapi jatuh tempo agar pembayaran tidak terlambat."),
                    HabitSubtask("3_3", "Jaga arus kas tetap surplus", "Hindari belanja defisit yang memicu utang konsumtif baru."),
                    HabitSubtask("3_4", "Lakukan pembayaran cicilan tercatat", "Menjaga skor kredit dan menghindari denda keterlambatan.")
                )
            ),
            HabitMilestone(
                id = 4,
                title = "4 · Menabung",
                color = Color(0xFF0E7490),
                tips = "Menabung adalah seni menunda kesenangan sesaat demi stabilitas dan ketenangan masa depan.",
                subtasks = listOf(
                    HabitSubtask("4_1", "Sisihkan minimal 10% pendapatan", "Pay yourself first sebelum mulai belanja."),
                    HabitSubtask("4_2", "Tetapkan target tabungan spesifik", "Beri nama setiap rupiah untuk tujuan yang jelas."),
                    HabitSubtask("4_3", "Otomatisasi setoran tabungan gajian", "Auto-save saat gajian mengurangi godaan boros."),
                    HabitSubtask("4_4", "Catat setoran tabungan rutin", "Rayakan progres setiap kali menabung ke tujuan.")
                )
            ),
            HabitMilestone(
                id = 5,
                title = "5 · Berkembang",
                color = Color(0xFF0369A1),
                tips = "Uang yang bekerja untuk Anda adalah jalan utama melipatgandakan kekayaan melampaui inflasi.",
                subtasks = listOf(
                    HabitSubtask("5_1", "Miliki aset investasi pertama", "Mulai membangun portofolio pada aset produktif."),
                    HabitSubtask("5_2", "Diversifikasi instrumen aset", "Sebar risiko ke beberapa jenis aset seperti saham, emas, atau kas."),
                    HabitSubtask("5_3", "Evaluasi nilai pasar portofolio", "Pantau total nilai aset riil secara berkala."),
                    HabitSubtask("5_4", "Kalahkan laju inflasi (portofolio hijau)", "Imbal hasil portofolio tumbuh positif melampaui modal.")
                )
            ),
            HabitMilestone(
                id = 6,
                title = "6 · Kebebasan",
                color = Color(0xFF0284C7),
                tips = "Kebebasan finansial sejati tercapai ketika waktu Anda sepenuhnya menjadi milik Anda sendiri.",
                subtasks = listOf(
                    HabitSubtask("6_1", "Capai rasio tabungan >= 50%", "Kemampuan hidup hemat dengan selisih simpanan besar."),
                    HabitSubtask("6_2", "Hasil investasi 4% tutupi biaya hidup", "Hasil investasi cukup membiayai operasional dasar."),
                    HabitSubtask("6_3", "Dana investasi capai Rp100 Juta+", "Tonggak akumulasi modal untuk pertumbuhan majemuk."),
                    HabitSubtask("6_4", "Kemandirian finansial penuh (25x tahunan)", "Kekayaan bersih mencukupi biaya hidup jangka panjang.")
                )
            )
        )
    }

    val autoDoneMap = remember(
        allTransactions,
        periodTransactions,
        wallets,
        debts,
        savingsGoals,
        assets,
        budgets,
        recurringTransactions,
        settings,
        periodIncome,
        periodExpense
    ) {
        val totalWalletBal = wallets.filterNot { it.excludeFromNetWorth }.sumOf { it.balance }
        val totalAssetVal = assets.sumOf { it.realValue }
        val totalGainLoss = assets.sumOf { it.unrealizedGainLoss }
        val streak = FinancialHealthHelper.calculateConsecutiveStreak(periodTransactions.map { it.dayOfMonth }.toSet())
        val savingsRate = if (periodIncome > 0.0) (periodIncome - periodExpense) / periodIncome else 0.0

        mapOf(
            "1_1" to allTransactions.isNotEmpty(),
            "1_2" to (streak >= 3),
            "1_3" to (allTransactions.size >= 10),
            "1_4" to allTransactions.any { it.note.isNotBlank() },
            "1_5" to (settings.monthlyStartDay != 1 || settings.paydayAutoSaveEnabled),
            "2_1" to ((periodExpense > 0.0 && totalWalletBal >= periodExpense * 3.0) || savingsGoals.any { it.name.contains("darurat", ignoreCase = true) && it.currentAmount > 0.0 }),
            "2_2" to (recurringTransactions.any { it.category.contains("Kesehatan", true) || it.title.contains("Asuransi", true) || it.title.contains("BPJS", true) } ||
                allTransactions.any { it.category.contains("Kesehatan", true) || it.title.contains("Asuransi", true) || it.title.contains("BPJS", true) }),
            "2_3" to (wallets.size >= 2),
            "2_4" to (budgets.isNotEmpty() || periodTransactions.any { it.type == TransactionType.EXPENSE }),
            "3_1" to (debts.isNotEmpty() || recurringTransactions.any { it.category.contains("Utang", true) || it.category.contains("Cicilan", true) }),
            "3_2" to (debts.isNotEmpty() && debts.all { it.dueDate.isNotBlank() }),
            "3_3" to (periodIncome > 0.0 && periodIncome >= periodExpense),
            "3_4" to debts.any { it.paidAmount > 0.0 },
            "4_1" to (savingsRate >= 0.10),
            "4_2" to savingsGoals.isNotEmpty(),
            "4_3" to settings.paydayAutoSaveEnabled,
            "4_4" to savingsGoals.any { it.records.isNotEmpty() },
            "5_1" to assets.isNotEmpty(),
            "5_2" to (assets.map { it.assetType }.distinct().size >= 2),
            "5_3" to (totalAssetVal > 0.0),
            "5_4" to (assets.isNotEmpty() && totalGainLoss > 0.0),
            "6_1" to (savingsRate >= 0.50),
            "6_2" to (periodExpense > 0.0 && (totalAssetVal * 0.04 / 12.0) >= periodExpense),
            "6_3" to (totalAssetVal >= 100_000_000.0),
            "6_4" to (periodExpense > 0.0 && (totalWalletBal + totalAssetVal) >= (periodExpense * 12.0 * 25.0))
        )
    }

    val subtaskOverrides = remember { mutableStateMapOf<String, Boolean>() }
    val isSubtaskDone: (String) -> Boolean = { id ->
        subtaskOverrides[id] ?: (autoDoneMap[id] == true)
    }

    var selectedTipMilestone by remember { mutableStateOf<HabitMilestone?>(null) }
    var expandedMilestoneId by remember { mutableIntStateOf(1) }

    val totalSubtasks = milestones.sumOf { it.subtasks.size }
    val completedSubtasks = milestones.sumOf { it.subtasks.count { st -> isSubtaskDone(st.id) } }

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
                text = "Jalan Uang",
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
            // Subtitle Description
            item {
                Text(
                    text = "Kebiasaan yang mengikuti teori, satu langkah pada satu waktu. Aplikasi merancang setiap langkah dari data Anda sendiri.",
                    fontSize = 12.5.sp,
                    color = TextMuted,
                    lineHeight = 18.sp
                )
            }

            // Interactive Pyramid Graphic matching the video
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // The 6-tier Pyramid
                        val pyramidTiers = listOf(
                            Triple("Kebebasan", Color(0xFF0EA5E9), 0.35f),
                            Triple("Berkembang", Color(0xFF0284C7), 0.48f),
                            Triple("Menabung", Color(0xFF0369A1), 0.62f),
                            Triple("Utang", Color(0xFF0F766E), 0.75f),
                            Triple("Perlindungan", Color(0xFF115E59), 0.88f),
                            Triple("Arus kas", Color(0xFF1E3A5F), 1.0f)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            pyramidTiers.forEachIndexed { idx, (label, color, widthFraction) ->
                                val tierMilestoneId = 6 - idx
                                val tierMilestone = milestones.find { it.id == tierMilestoneId }
                                val isLevelActive = tierMilestone?.subtasks?.any { isSubtaskDone(it.id) } == true
                                Surface(
                                    shape = RoundedCornerShape(topStart = if (idx == 0) 8.dp else 4.dp, topEnd = if (idx == 0) 8.dp else 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                                    color = if (isLevelActive) color else color.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .fillMaxWidth(widthFraction)
                                        .height(30.dp)
                                        .clickable {
                                            expandedMilestoneId = 6 - idx
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress bar matching video: "22 dari 25 selesai"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progres Kebiasaan",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "$completedSubtasks dari $totalSubtasks selesai",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlueBright
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { (completedSubtasks.toFloat() / totalSubtasks).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AccentBlue,
                            trackColor = DarkSurfaceElevated,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }

            // Accordion Sections
            items(milestones, key = { it.id }) { milestone ->
                val isExpanded = expandedMilestoneId == milestone.id
                val doneCount = milestone.subtasks.count { isSubtaskDone(it.id) }
                val isAllDone = doneCount == milestone.subtasks.size

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, if (isExpanded) AccentBlue.copy(alpha = 0.6f) else DarkCardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedMilestoneId = if (isExpanded) 0 else milestone.id
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isAllDone) IncomeGreen.copy(alpha = 0.2f) else DarkSurfaceElevated,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isAllDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = IncomeGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "${milestone.id}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = milestone.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = "$doneCount dari ${milestone.subtasks.size} selesai",
                                        fontSize = 11.sp,
                                        color = if (isAllDone) IncomeGreen else TextMuted
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { selectedTipMilestone = milestone },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = AccentBlueBright,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pelajari", fontSize = 10.5.sp, color = TextWhite)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Collapsible Subtasks
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                milestone.subtasks.forEach { st ->
                                    val checked = isSubtaskDone(st.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                subtaskOverrides[st.id] = !checked
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { isC ->
                                                subtaskOverrides[st.id] = isC
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = AccentBlue,
                                                uncheckedColor = TextMuted,
                                                checkmarkColor = Color.White
                                            ),
                                            modifier = Modifier.size(24.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Text(
                                                text = st.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (checked) TextWhite else TextMuted
                                            )
                                            Text(
                                                text = st.subtitle,
                                                fontSize = 11.5.sp,
                                                color = TextMuted,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Tips Dialog
    selectedTipMilestone?.let { milestone ->
        AlertDialog(
            onDismissRequest = { selectedTipMilestone = null },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = AccentBlueBright,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(milestone.title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = milestone.tips,
                    fontSize = 13.sp,
                    color = TextMuted,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { selectedTipMilestone = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Saya Mengerti", color = Color.White)
                }
            }
        )
    }
}
