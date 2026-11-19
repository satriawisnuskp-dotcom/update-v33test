package com.example.ui.screens

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsRecord
import com.example.ui.components.FullScreenModalContainer
import com.example.ui.components.fullScreenBottomSafePadding
import com.example.ui.components.fullScreenTopSafePadding
import com.example.ui.dialogs.DeleteSavingsGoalConfirmDialog
import com.example.ui.dialogs.DepositWithdrawGoalSheet
import com.example.ui.dialogs.formatSavingsReadableDate
import com.example.ui.dialogs.savingsIconFromName
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CurrencyHelper

@Composable
fun TabunganDetailScreen(
    goalId: String,
    viewModel: FinanceViewModel,
    onEdit: (SavingsGoal) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val goals by viewModel.savingsGoals.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    val goal = goals.find { it.id == goalId }
    if (goal == null) {
        onDismiss()
        return
    }

    val goalColor = Color(goal.colorHex)
    val orderedRecords = remember(goal.records) { goal.records.reversed() }

    var showAddRecordSheet by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<SavingsRecord?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    FullScreenModalContainer(onDismissRequest = onDismiss) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .testTag("screen_tabungan_detail"),
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editingRecord = null
                        showAddRecordSheet = true
                    },
                    containerColor = AccentBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fullScreenBottomSafePadding()
                        .padding(bottom = 8.dp)
                        .size(56.dp)
                        .testTag("fab_detail_add_savings_record")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah catatan", tint = Color.White)
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Top Bar: <- <Nama>   [Edit] [Trash]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fullScreenTopSafePadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_back_tabungan_detail")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextWhite)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(goalColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = savingsIconFromName(goal.iconName),
                                contentDescription = null,
                                tint = goalColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = goal.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onEdit(goal) }
                                .testTag("btn_edit_goal_detail")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Tabungan", tint = TextWhite, modifier = Modifier.size(18.dp))
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ExpenseRedSoft,
                            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showDeleteConfirm = true }
                                .testTag("btn_delete_goal_detail")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus Tabungan", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // UNTUK DITABUNG Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (goal.remainingAmount <= 0.0) "TARGET TERCAPAI" else "UNTUK DITABUNG",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.7.sp,
                                    color = if (goal.remainingAmount <= 0.0) IncomeGreen else TextMuted
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = CurrencyHelper.formatRupiah(goal.remainingAmount),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${CurrencyHelper.formatRupiah(goal.currentAmount)} terkumpul",
                                    fontSize = 13.sp,
                                    color = IncomeGreen,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                LinearProgressIndicator(
                                    progress = { (goal.progressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = goalColor,
                                    trackColor = DarkSurfaceElevated,
                                    strokeCap = StrokeCap.Round
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("TERKUMPUL", fontSize = 10.sp, color = TextSubtle, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = CurrencyHelper.formatRupiah(goal.currentAmount),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("TARGET", fontSize = 10.sp, color = TextSubtle, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = CurrencyHelper.formatRupiah(goal.targetAmount),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite
                                        )
                                    }
                                    Column(modifier = Modifier.weight(0.6f), horizontalAlignment = Alignment.End) {
                                        Text("PROGRES", fontSize = 10.sp, color = TextSubtle, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${goal.progressPercent}%",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = goalColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // CATATAN Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CATATAN",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.7.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "Terbaru terlebih dahulu",
                                        fontSize = 11.5.sp,
                                        color = TextSubtle
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (orderedRecords.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Belum ada setoran",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Ketuk + untuk mencatat setoran pertama Anda.",
                                            fontSize = 12.sp,
                                            color = TextSubtle,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    orderedRecords.forEachIndexed { idx, rec ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { editingRecord = rec }
                                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                                .testTag("savings_record_${rec.id}"),
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
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (rec.isWithdraw) ExpenseRedSoft else IncomeGreenSoft),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (rec.isWithdraw) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                        contentDescription = null,
                                                        tint = if (rec.isWithdraw) ExpenseRed else IncomeGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = rec.note.ifBlank { if (rec.isWithdraw) "Penarikan" else "Setoran" },
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextWhite,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val sub = buildString {
                                                        append(formatSavingsReadableDate(rec.dateStr))
                                                        if (rec.walletName.isNotBlank()) append(" · ${rec.walletName}")
                                                    }
                                                    Text(
                                                        text = sub,
                                                        fontSize = 11.sp,
                                                        color = TextSubtle
                                                    )
                                                }
                                            }
                                            Text(
                                                text = (if (rec.isWithdraw) "-" else "+") + CurrencyHelper.formatRupiah(rec.amount),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rec.isWithdraw) ExpenseRed else IncomeGreen
                                            )
                                        }
                                        if (idx < orderedRecords.lastIndex) {
                                            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.45f))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            editingRecord = null
                                            showAddRecordSheet = true
                                        }
                                        .padding(vertical = 8.dp)
                                        .testTag("btn_add_savings_record_inline"),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, null, tint = AccentBlueBright, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tambah catatan",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentBlueBright
                                    )
                                }
                            }
                        }
                    }

                    // Tandai selesai Switch Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.65f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DarkSurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Flag, null, tint = AccentBlueBright, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Tandai selesai",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite
                                        )
                                        Text(
                                            text = "Anda dapat menutup tujuan dengan jumlah berapa pun.",
                                            fontSize = 11.5.sp,
                                            color = TextSubtle,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = goal.isCompleted,
                                    onCheckedChange = { viewModel.toggleSavingsGoalCompleted(goal.id) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = IncomeGreen
                                    ),
                                    modifier = Modifier.testTag("switch_goal_completed")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddRecordSheet) {
        DepositWithdrawGoalSheet(
            goal = goal,
            initialIsWithdraw = false,
            editingRecord = null,
            wallets = wallets,
            onSaveRecord = { amount, isWithdraw, note, dateStr, walletName ->
                viewModel.addSavingsRecord(goal.id, amount, isWithdraw, note, dateStr, walletName)
            },
            onDismiss = { showAddRecordSheet = false }
        )
    }

    editingRecord?.let { rec ->
        DepositWithdrawGoalSheet(
            goal = goal,
            initialIsWithdraw = rec.isWithdraw,
            editingRecord = rec,
            wallets = wallets,
            onSaveRecord = { amount, isWithdraw, note, dateStr, _ ->
                viewModel.updateSavingsRecord(goal.id, rec.id, amount, isWithdraw, note, dateStr)
            },
            onDeleteRecord = {
                viewModel.deleteSavingsRecord(goal.id, rec.id)
            },
            onDismiss = { editingRecord = null }
        )
    }

    if (showDeleteConfirm) {
        DeleteSavingsGoalConfirmDialog(
            goalName = goal.name,
            onConfirm = {
                viewModel.deleteSavingsGoal(goal.id)
                showDeleteConfirm = false
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
