package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.sync.SyncStatus
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.theme.TrophyYellowLight
import com.example.ui.theme.getTopHeaderGradient
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun SyncStatusBadge(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val (label, accentColor, icon) = when (syncStatus) {
        is SyncStatus.Synced -> Triple(
            if (compact) "Tersinkron" else "Tersinkronisasi",
            IncomeGreen,
            Icons.Default.CloudDone
        )
        is SyncStatus.Syncing -> Triple(
            if (compact) "Sync..." else "Menyinkronkan...",
            AccentBlueBright,
            Icons.Default.CloudSync
        )
        is SyncStatus.Offline -> Triple(
            if (compact) "Offline" else "Offline (Menunggu koneksi)",
            TrophyYellowLight,
            Icons.Default.CloudOff
        )
        is SyncStatus.Error -> Triple(
            if (compact) "Gagal" else "Gagal Sinkronisasi",
            ExpenseRed,
            Icons.Default.ErrorOutline
        )
        is SyncStatus.Guest -> Triple(
            if (compact) "Lokal" else "Mode Tamu · Lokal",
            TextMuted,
            Icons.Default.PhoneAndroid
        )
    }

    val shape = RoundedCornerShape(50)
    Surface(
        modifier = modifier
            .heightIn(min = if (compact) 32.dp else 30.dp)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = accentColor.copy(alpha = 0.14f),
        shape = shape,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.42f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 10.dp,
                vertical = if (compact) 5.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (syncStatus is SyncStatus.Syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.8.dp,
                    color = accentColor
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProfileSyncScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val activityContext = remember(context) { context.findActivity() ?: context }

    val currentUser by viewModel.currentUser.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authErrorMessage by viewModel.authErrorMessage.collectAsState()
    val syncSuccessMessage by viewModel.syncSuccessMessage.collectAsState()

    val transactions by viewModel.allTransactions.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val financialPlans by viewModel.financialPlans.collectAsState()
    val events by viewModel.events.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val labels by viewModel.labels.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    val formattedLastSync = remember(lastSyncedAt) {
        if (lastSyncedAt <= 0L) {
            "Belum pernah tersinkronisasi ke cloud"
        } else {
            val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            "Terakhir sinkron: ${fmt.format(Date(lastSyncedAt))}"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(getTopHeaderGradient(AccentBlue))
            .navigationBarsPadding()
    ) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_back_profile_sync")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextWhite
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Akun & Sinkronisasi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = if (currentUser != null) "Cloud Sync Real-Time Aktif" else "Mode Tamu · Penyimpanan Lokal",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                SyncStatusBadge(
                    syncStatus = syncStatus,
                    compact = true
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Feedback banners (error / success)
            item {
                AnimatedVisibility(visible = !authErrorMessage.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.14f)),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Kesalahan",
                                tint = ExpenseRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = authErrorMessage.orEmpty(),
                                color = TextWhite,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearAuthMessages() }) {
                                Text("Tutup", color = ExpenseRed, fontSize = 12.sp)
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = !syncSuccessMessage.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (!authErrorMessage.isNullOrBlank()) 8.dp else 0.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.14f)),
                        border = BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Berhasil",
                                tint = IncomeGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = syncSuccessMessage.orEmpty(),
                                color = TextWhite,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearAuthMessages() }) {
                                Text("Tutup", color = IncomeGreen, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Account Card (Signed-in vs Guest)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val user = currentUser
                        if (user == null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .background(AccentBlue.copy(alpha = 0.16f))
                                        .border(1.dp, AccentBlue.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Mode Tamu",
                                        tint = AccentBlueBright,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Mode Tamu (Penyimpanan Lokal)",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Data keuangan Anda aman tersimpan di perangkat ini. Masuk dengan Google untuk sinkronisasi otomatis multi-perangkat.",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        lineHeight = 17.sp
                                    )
                                }
                            }

                            HorizontalDivider(color = DarkCardBorder)

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BenefitBulletRow("Cadangan & sinkronisasi real-time antar HP via Cloud Firestore")
                                BenefitBulletRow("Migrasi otomatis transaksi, akun, kategori, label, berulang, perencanaan, anggaran, acara, tabungan, aset, utang, pengaturan & logo kustom")
                                BenefitBulletRow("Tetap cepat & responsif saat offline dengan cache Room Database")
                            }

                            Button(
                                onClick = { viewModel.signInWithGoogle(activityContext) },
                                enabled = !isAuthLoading,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentBlue,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("btn_google_sign_in")
                            ) {
                                if (isAuthLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Menghubungkan ke Google...", fontWeight = FontWeight.SemiBold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Login,
                                        contentDescription = "Masuk dengan Google",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        } else {
                            // Signed-in Profile View
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                val photoUri = user.photoUrl
                                if (photoUri != null) {
                                    AsyncImage(
                                        model = photoUri,
                                        contentDescription = "Foto profil pengguna",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, AccentBlueBright, CircleShape)
                                    )
                                } else {
                                    val initials = (user.displayName ?: user.email ?: "U")
                                        .trim()
                                        .take(1)
                                        .uppercase()
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(AccentBlue.copy(alpha = 0.25f))
                                            .border(2.dp, AccentBlueBright, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            color = TextWhite,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName?.takeIf { it.isNotBlank() } ?: "Pengguna Google",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!user.email.isNullOrBlank()) {
                                        Text(
                                            text = user.email!!,
                                            fontSize = 13.sp,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    SyncStatusBadge(syncStatus = syncStatus, compact = false)
                                }
                            }

                            HorizontalDivider(color = DarkCardBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.manualSync() },
                                    enabled = !isAuthLoading && syncStatus !is SyncStatus.Syncing,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentBlue,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_manual_sync")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sinkronisasi Manual",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sinkronisasi Manual", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = { showLogoutDialog = true },
                                    enabled = !isAuthLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.6f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("btn_logout")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Keluar akun",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Logout", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Sync Status Detail Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_sync_status"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Status Sinkronisasi",
                                tint = AccentBlueBright,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Ringkasan Status Sinkronisasi",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }

                        val statusDescription = when (val st = syncStatus) {
                            is SyncStatus.Synced ->
                                "Semua data Anda di Room Database dan Cloud Firestore berada dalam kondisi terbaru."
                            is SyncStatus.Syncing ->
                                "Sedang menyinkronkan perubahan data dengan server Cloud Firestore..."
                            is SyncStatus.Offline ->
                                "Tidak ada koneksi internet. Perubahan tetap disimpan di Room dan otomatis dikirim saat online."
                            is SyncStatus.Error ->
                                st.message
                            is SyncStatus.Guest ->
                                "Anda belum masuk. Data hanya disimpan secara lokal di perangkat ini."
                        }

                        Text(
                            text = statusDescription,
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 18.sp
                        )

                        Text(
                            text = formattedLastSync,
                            fontSize = 12.sp,
                            color = TextSubtle,
                            fontWeight = FontWeight.Medium
                        )

                        HorizontalDivider(color = DarkCardBorder)

                        // Data counts summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SyncCountMetric("Transaksi", transactions.size, Modifier.weight(1f))
                            SyncCountMetric("Akun", wallets.size, Modifier.weight(1f))
                            SyncCountMetric("Kategori", categories.size, Modifier.weight(1f))
                            SyncCountMetric("Aset", assets.size, Modifier.weight(1f))
                            SyncCountMetric("Utang", debts.size, Modifier.weight(1f))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SyncCountMetric("Anggaran", budgets.size, Modifier.weight(1f))
                            SyncCountMetric("Berulang", recurringTransactions.size, Modifier.weight(1f))
                            SyncCountMetric("Rencana", financialPlans.size, Modifier.weight(1f))
                            SyncCountMetric("Acara", events.size, Modifier.weight(1f))
                            SyncCountMetric("Tabungan", savingsGoals.size, Modifier.weight(1f))
                            SyncCountMetric("Label", labels.size, Modifier.weight(1f))
                        }

                        Text(
                            text = "Pengaturan aplikasi lengkap & logo kustom akun/aset turut tersinkronisasi real-time.",
                            fontSize = 11.5.sp,
                            color = TextSubtle,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Security Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Keamanan Data",
                            tint = IncomeGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Isolasi & Resolusi Konflik Data",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "Setiap data terisolasi per UID pengguna (users/{uid}/...) dengan Firestore Security Rules, menggunakan UUID anti-bentrok, Soft Delete lintas perangkat, serta resolusi konflik Last-Write-Wins berdasarkan timestamp.",
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

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = DarkSurface,
            titleContentColor = TextWhite,
            textContentColor = TextMuted,
            title = {
                Text("Keluar dari Akun Google?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Listener sinkronisasi cloud akan dihentikan dan aplikasi beralih ke mode lokal. Data cache lokal Anda tetap tersimpan di perangkat ini."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.signOut(context)
                    },
                    modifier = Modifier.testTag("btn_confirm_logout")
                ) {
                    Text("Keluar", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal", color = TextWhite)
                }
            }
        )
    }
}

@Composable
private fun BenefitBulletRow(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = IncomeGreen,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(15.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = TextMuted,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun SyncCountMetric(label: String, count: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSubtle,
            maxLines = 1
        )
    }
}
