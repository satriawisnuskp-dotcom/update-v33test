package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.MainCategory
import com.example.data.model.TransactionTemplate
import com.example.data.model.TransactionType
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.util.CategoryIconHelper
import com.example.util.CurrencyHelper

private fun formatTemplateAccountSummary(
    accountName: String,
    toAccountName: String,
    targetAssetName: String,
    targetDebtName: String,
    type: TransactionType
): String {
    val src = accountName.ifBlank { "Dompet" }
    if (type != TransactionType.TRANSFER) return src
    val dest = targetDebtName.ifBlank { targetAssetName.ifBlank { toAccountName } }
    return if (dest.isNotBlank()) "$src → $dest" else src
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveTransactionTemplateSheet(
    initialName: String,
    category: String,
    subcategory: String,
    amount: Double,
    type: TransactionType,
    accountName: String,
    toAccountName: String = "",
    transferFee: Double = 0.0,
    targetAssetId: String = "",
    targetAssetName: String = "",
    targetDebtId: String = "",
    targetDebtName: String = "",
    note: String = "",
    label: String = "",
    recurrence: String = "Tidak pernah",
    currency: String = "IDR",
    categories: List<MainCategory>,
    onSave: (TransactionTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var templateName by remember(initialName, category) {
        mutableStateOf(initialName.ifBlank { subcategory.ifBlank { category } })
    }
    var rememberAmount by remember { mutableStateOf(true) }

    val matchedCat = remember(categories, category, type) {
        categories.firstOrNull { !it.isDeleted && it.type == type && it.name.equals(category, ignoreCase = true) }
            ?: categories.firstOrNull { !it.isDeleted && it.name.equals(category, ignoreCase = true) }
    }
    val catColor = CategoryIconHelper.resolveCategoryColor(matchedCat, category, type)
    val catIcon = CategoryIconHelper.getCategoryIcon(
        subcategory.ifBlank { category },
        matchedCat?.iconName ?: ""
    )

    val labelCount = remember(label) {
        label.split(",").map { it.trim() }.filter { it.isNotBlank() }.size
    }
    val accountText = formatTemplateAccountSummary(
        accountName = accountName,
        toAccountName = toAccountName,
        targetAssetName = targetAssetName,
        targetDebtName = targetDebtName,
        type = type
    )
    val subtitleSummary = if (labelCount > 0) "$accountText · $labelCount label" else accountText

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Simpan sebagai template",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Isi entri ini dengan satu ketukan lain kali",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = TextMuted
                    )
                }
            }

            // Template Name Input
            OutlinedTextField(
                value = templateName,
                onValueChange = { templateName = it },
                placeholder = { Text("Nama template", color = TextSubtle) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DarkCardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_template_name")
            )

            // Preview Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(catColor.copy(alpha = 0.2f))
                                .border(1.dp, catColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = category,
                                tint = catColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (subcategory.isNotBlank()) "$category · $subcategory" else category,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitleSummary,
                                fontSize = 11.5.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = CurrencyHelper.formatRupiah(amount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rememberAmount) TextWhite else TextSubtle
                    )
                }
            }

            // Toggle: Ingat jumlahnya
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceElevated.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { rememberAmount = !rememberAmount }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ingat jumlahnya",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Text(
                            text = if (rememberAmount) {
                                "Aktif: Jumlah disimpan setiap kali"
                            } else {
                                "Mati: Anda mengetik jumlahnya setiap kali"
                            },
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = rememberAmount,
                        onCheckedChange = { rememberAmount = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentBlue,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurface
                        ),
                        modifier = Modifier.testTag("switch_remember_template_amount")
                    )
                }
            }

            // Primary Save Button
            Button(
                onClick = {
                    val cleanName = templateName.trim().ifBlank { subcategory.ifBlank { category } }
                    onSave(
                        TransactionTemplate(
                            name = cleanName,
                            category = category,
                            subcategory = subcategory,
                            amount = if (rememberAmount) amount else 0.0,
                            rememberAmount = rememberAmount,
                            type = type,
                            accountName = accountName,
                            toAccountName = toAccountName,
                            transferFee = transferFee,
                            targetAssetId = targetAssetId,
                            targetAssetName = targetAssetName,
                            targetDebtId = targetDebtId,
                            targetDebtName = targetDebtName,
                            note = note,
                            label = label,
                            recurrence = recurrence,
                            currency = currency
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_confirm_save_template")
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simpan template",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePickerSheet(
    templates: List<TransactionTemplate>,
    categories: List<MainCategory>,
    isManageMode: Boolean,
    onSelectTemplate: (TransactionTemplate) -> Unit = {},
    onRenameTemplate: (String, String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onReorderTemplates: (List<TransactionTemplate>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingTemplate by remember { mutableStateOf<TransactionTemplate?>(null) }

    val localTemplates = remember {
        mutableStateListOf<TransactionTemplate>().apply { addAll(templates) }
    }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeights = remember { mutableMapOf<String, Int>() }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val spacingPx = with(density) { 10.dp.toPx() }

    LaunchedEffect(templates) {
        if (draggedId == null) {
            localTemplates.clear()
            localTemplates.addAll(templates)
        }
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 22.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Template",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = if (isManageMode) {
                            "Seret untuk mengurutkan · ... untuk ganti nama atau hapus"
                        } else {
                            "Ketuk salah satu untuk mengisi entri"
                        },
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (localTemplates.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AccentBlue.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Belum ada template tersimpan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Text(
                            text = "Simpan entri apa pun sebagai template dengan tombol bookmark di formulir entri.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(localTemplates, key = { _, tpl -> tpl.id }) { _, tpl ->
                        val isDragging = draggedId == tpl.id
                        val matchedCat = categories.firstOrNull {
                            !it.isDeleted && it.type == tpl.type && it.name.equals(tpl.category, ignoreCase = true)
                        } ?: categories.firstOrNull {
                            !it.isDeleted && it.name.equals(tpl.category, ignoreCase = true)
                        }
                        val catColor = CategoryIconHelper.resolveCategoryColor(matchedCat, tpl.category, tpl.type)
                        val catIcon = CategoryIconHelper.getCategoryIcon(
                            tpl.subcategory.ifBlank { tpl.category },
                            matchedCat?.iconName ?: ""
                        )
                        val walletText = formatTemplateAccountSummary(
                            accountName = tpl.accountName,
                            toAccountName = tpl.toAccountName,
                            targetAssetName = tpl.targetAssetName,
                            targetDebtName = tpl.targetDebtName,
                            type = tpl.type
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDragging) Color(0xFF1E293B) else DarkSurfaceElevated
                            ),
                            border = BorderStroke(
                                width = if (isDragging) 1.5.dp else 1.dp,
                                color = if (isDragging) AccentBlue else DarkCardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 2f else 0f)
                                .onSizeChanged { itemHeights[tpl.id] = it.height }
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    scaleX = if (isDragging) 1.015f else 1f
                                    scaleY = if (isDragging) 1.015f else 1f
                                }
                                .clickable(enabled = !isManageMode) {
                                    onSelectTemplate(tpl)
                                }
                                .testTag("template_item_${tpl.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(catColor.copy(alpha = 0.2f))
                                        .border(1.dp, catColor.copy(alpha = 0.45f), RoundedCornerShape(11.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = tpl.category,
                                        tint = catColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tpl.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${tpl.category} · $walletText",
                                        fontSize = 11.5.sp,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (tpl.rememberAmount && tpl.amount > 0.0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = CurrencyHelper.formatRupiah(tpl.amount),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                }

                                IconButton(
                                    onClick = { editingTemplate = tpl },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("btn_template_more_${tpl.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreHoriz,
                                        contentDescription = "Opsi template ${tpl.name}",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (isManageMode) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("drag_handle_template_${tpl.id}")
                                            .pointerInput(tpl.id) {
                                                detectVerticalDragGestures(
                                                    onDragStart = {
                                                        draggedId = tpl.id
                                                        dragOffsetY = 0f
                                                    },
                                                    onVerticalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount
                                                        val idx = localTemplates.indexOfFirst { it.id == tpl.id }
                                                        val curH = itemHeights[tpl.id]?.toFloat() ?: 140f
                                                        if (idx >= 0) {
                                                            if (dragOffsetY > 0f && idx < localTemplates.lastIndex) {
                                                                val nextId = localTemplates[idx + 1].id
                                                                val stepPx = (itemHeights[nextId]?.toFloat() ?: curH) + spacingPx
                                                                if (dragOffsetY > stepPx * 0.55f) {
                                                                    val moved = localTemplates.removeAt(idx)
                                                                    localTemplates.add(idx + 1, moved)
                                                                    dragOffsetY -= stepPx
                                                                }
                                                            } else if (dragOffsetY < 0f && idx > 0) {
                                                                val prevId = localTemplates[idx - 1].id
                                                                val stepPx = (itemHeights[prevId]?.toFloat() ?: curH) + spacingPx
                                                                if (dragOffsetY < -(stepPx * 0.55f)) {
                                                                    val moved = localTemplates.removeAt(idx)
                                                                    localTemplates.add(idx - 1, moved)
                                                                    dragOffsetY += stepPx
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        if (draggedId == tpl.id) {
                                                            draggedId = null
                                                            dragOffsetY = 0f
                                                            onReorderTemplates(localTemplates.toList())
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        if (draggedId == tpl.id) {
                                                            draggedId = null
                                                            dragOffsetY = 0f
                                                            onReorderTemplates(localTemplates.toList())
                                                        }
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "Seret untuk mengurutkan ${tpl.name}",
                                            tint = if (isDragging) AccentBlue else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isManageMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Simpan entri apa pun sebagai template dengan tombol bookmark di formulir entri.",
                        fontSize = 11.5.sp,
                        color = TextSubtle
                    )
                }
            }
        }
    }

    editingTemplate?.let { tpl ->
        EditTemplateActionSheet(
            template = tpl,
            onRename = { newName ->
                onRenameTemplate(tpl.id, newName)
                editingTemplate = null
            },
            onDelete = {
                onDeleteTemplate(tpl.id)
                editingTemplate = null
            },
            onDismiss = { editingTemplate = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTemplateActionSheet(
    template: TransactionTemplate,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var renameText by remember(template.id) { mutableStateOf(template.name) }

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ganti nama atau hapus template ini",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = TextMuted
                    )
                }
            }

            OutlinedTextField(
                value = renameText,
                onValueChange = { renameText = it },
                placeholder = { Text("Nama template", color = TextSubtle) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DarkCardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_rename_template")
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ExpenseRed.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDelete() }
                    .testTag("btn_delete_template")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus template",
                        tint = ExpenseRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Hapus template",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ExpenseRed
                    )
                }
            }

            Button(
                onClick = {
                    if (renameText.isNotBlank()) {
                        onRename(renameText.trim())
                    }
                },
                enabled = renameText.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_rename_template")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simpan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCategoryPickerSheet(
    currentCategory: String,
    currentSubcategory: String = "",
    categories: List<MainCategory>,
    templates: List<TransactionTemplate>,
    showTemplatesAboveCategories: Boolean,
    onCategoryAndSubcategorySelected: (String, String) -> Unit,
    onSelectTemplate: (TransactionTemplate) -> Unit,
    onOpenTemplatesSheet: () -> Unit,
    onOpenManageCategories: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fallbackExpense = remember {
        listOf(
            "Other", "Bills & Fees", "Market", "Food & Dining",
            "Shopping", "Transport", "Kesehatan", "Pendidikan", "Hiburan"
        )
    }
    val fallbackIncome = remember {
        listOf("Salary", "Extra Income", "Bonus", "Investasi", "Pemberian")
    }
    val transferCategories = remember {
        listOf("Transfer", "Asset", "Pembayaran Utang")
    }

    val initialTab = remember(currentCategory, categories) {
        when {
            currentCategory in transferCategories ||
                currentCategory.equals("Aset", ignoreCase = true) ||
                currentCategory.equals("Transfer Antar Bank", ignoreCase = true) ||
                currentCategory.equals("Bayar Utang", ignoreCase = true) ||
                currentCategory.equals("Pembayaran Utang", ignoreCase = true) -> 2
            categories.any { it.type == TransactionType.INCOME && it.name.equals(currentCategory, ignoreCase = true) } ||
                fallbackIncome.any { it.equals(currentCategory, ignoreCase = true) } -> 1
            else -> 0
        }
    }
    var activeTab by remember { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedCategoryIds by remember { mutableStateOf(setOf<String>()) }

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
                .heightIn(min = 380.dp, max = 580.dp)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header: Category Icon + "Pilih kategori" | Bookmark button + Settings gear button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentBlue.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Pilih kategori",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenTemplatesSheet() }
                            .testTag("btn_open_templates_from_category")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "Buka daftar template",
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenManageCategories() }
                            .testTag("btn_open_manage_categories")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan kategori",
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Optional Quick Template Strip when setting is enabled
            if (showTemplatesAboveCategories && templates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Template",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_template_strip")
                ) {
                    items(templates, key = { it.id }) { tpl ->
                        val matchedCat = categories.firstOrNull {
                            !it.isDeleted && it.type == tpl.type && it.name.equals(tpl.category, ignoreCase = true)
                        } ?: categories.firstOrNull {
                            !it.isDeleted && it.name.equals(tpl.category, ignoreCase = true)
                        }
                        val tplColor = CategoryIconHelper.resolveCategoryColor(matchedCat, tpl.category, tpl.type)
                        val tplIcon = CategoryIconHelper.getCategoryIcon(
                            tpl.subcategory.ifBlank { tpl.category },
                            matchedCat?.iconName ?: ""
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectTemplate(tpl) }
                                .testTag("quick_template_chip_${tpl.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(tplColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tplIcon,
                                        contentDescription = tpl.name,
                                        tint = tplColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = tpl.name,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (tpl.rememberAmount && tpl.amount > 0.0) {
                                    Text(
                                        text = CurrencyHelper.formatRupiah(tpl.amount),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar ("Cari kategori")
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Cari kategori", color = TextSubtle, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Bersihkan pencarian",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DarkCardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_category")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Segmented Tabs: Pengeluaran | Pemasukan | Transfer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceElevated, RoundedCornerShape(14.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Pengeluaran", "Pemasukan", "Transfer").forEachIndexed { idx, title ->
                    val isSelected = activeTab == idx
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            when (idx) {
                                0 -> ExpenseRed
                                1 -> IncomeGreen
                                else -> AccentBlue
                            }
                        } else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { activeTab = idx }
                            .testTag("category_tab_$idx")
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextMuted,
                            modifier = Modifier.padding(vertical = 9.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val cleanQuery = searchQuery.trim().lowercase()
            val customCatsForTab = remember(categories, activeTab, cleanQuery) {
                val base = when (activeTab) {
                    0 -> categories.filter { !it.isDeleted && it.type == TransactionType.EXPENSE && it.isVisible }
                    1 -> categories.filter { !it.isDeleted && it.type == TransactionType.INCOME && it.isVisible }
                    else -> emptyList()
                }
                if (cleanQuery.isEmpty()) {
                    base
                } else {
                    base.filter { cat ->
                        cat.name.lowercase().contains(cleanQuery) ||
                            cat.subcategories.any { it.isVisible && it.name.lowercase().contains(cleanQuery) }
                    }
                }
            }

            val filteredTransfers = remember(cleanQuery, transferCategories) {
                if (cleanQuery.isEmpty()) {
                    transferCategories
                } else {
                    transferCategories.filter { cat ->
                        val label = when (cat) {
                            "Asset" -> "transfer aset investasi emas saham"
                            "Pembayaran Utang" -> "pembayaran utang bayar cicilan"
                            else -> "transfer antar akun kirim dompet bank"
                        }
                        cat.lowercase().contains(cleanQuery) || label.contains(cleanQuery)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeTab == 2) {
                    items(filteredTransfers) { cat ->
                        val isSelected = currentCategory.equals(cat, ignoreCase = true) ||
                            (cat == "Transfer" && currentCategory.equals("Transfer Antar Bank", ignoreCase = true)) ||
                            (cat == "Asset" && (currentCategory.equals("Aset", ignoreCase = true) || currentCategory.equals("Transfer Aset", ignoreCase = true))) ||
                            (cat == "Pembayaran Utang" && currentCategory.equals("Bayar Utang", ignoreCase = true))

                        val displayCatName = when (cat) {
                            "Asset" -> "Transfer Aset"
                            "Pembayaran Utang" -> "Pembayaran Utang"
                            else -> "Transfer Antar Akun"
                        }
                        val displaySubtext = when (cat) {
                            "Asset" -> "Investasi, emas, saham, deposito"
                            "Pembayaran Utang" -> "Cicilan, pinjaman, pelunasan utang"
                            else -> "Kirim uang antar dompet / bank"
                        }
                        val catIcon = when (cat) {
                            "Asset" -> Icons.AutoMirrored.Filled.TrendingUp
                            "Pembayaran Utang" -> Icons.Default.Handshake
                            else -> Icons.Default.SwapHoriz
                        }
                        val catColor = when (cat) {
                            "Asset" -> Color(0xFF10B981)
                            "Pembayaran Utang" -> Color(0xFFFF5722)
                            else -> AccentBlue
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) catColor.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) catColor else DarkCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCategoryAndSubcategorySelected(cat, "")
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(if (isSelected) catColor else catColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = catIcon,
                                            contentDescription = displayCatName,
                                            tint = if (isSelected) Color.White else catColor,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = displayCatName,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = TextWhite
                                        )
                                        Text(
                                            text = displaySubtext,
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (customCatsForTab.isNotEmpty()) {
                    items(customCatsForTab, key = { it.id }) { cat ->
                        val isParentSelected = currentCategory.equals(cat.name, ignoreCase = true)
                        val visibleSubs = cat.subcategories.filter { sub ->
                            sub.isVisible && (cleanQuery.isEmpty() ||
                                sub.name.lowercase().contains(cleanQuery) ||
                                cat.name.lowercase().contains(cleanQuery))
                        }
                        val hasSubcategories = cat.subcategories.any { it.isVisible }
                        val hasMatchingSubOnly = cleanQuery.isNotEmpty() &&
                            !cat.name.lowercase().contains(cleanQuery) &&
                            visibleSubs.isNotEmpty()
                        val isExpanded = expandedCategoryIds.contains(cat.id) || hasMatchingSubOnly
                        val itemCatColor = CategoryIconHelper.resolveCategoryColor(cat)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceElevated, RoundedCornerShape(14.dp))
                                .border(
                                    1.dp,
                                    if (isParentSelected) itemCatColor.copy(alpha = 0.7f) else DarkCardBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onCategoryAndSubcategorySelected(cat.name, "")
                                        onDismiss()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(if (isParentSelected) itemCatColor else itemCatColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getCategoryIcon(cat.name, cat.iconName),
                                            contentDescription = cat.name,
                                            tint = if (isParentSelected) Color.White else itemCatColor,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = cat.name,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite
                                        )
                                        if (hasSubcategories) {
                                            Text(
                                                text = "${cat.subcategories.count { it.isVisible }} subkategori",
                                                fontSize = 11.sp,
                                                color = TextSubtle
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isParentSelected && currentSubcategory.isBlank()) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = itemCatColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    if (hasSubcategories) {
                                        IconButton(
                                            onClick = {
                                                expandedCategoryIds = if (expandedCategoryIds.contains(cat.id)) {
                                                    expandedCategoryIds - cat.id
                                                } else {
                                                    expandedCategoryIds + cat.id
                                                }
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (isExpanded) "Sembunyikan subkategori" else "Tampilkan subkategori",
                                                tint = TextMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (visibleSubs.isNotEmpty() && isExpanded) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, bottom = 2.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    visibleSubs.forEach { sub ->
                                        val isSubSelected = isParentSelected && currentSubcategory.equals(sub.name, ignoreCase = true)
                                        val subIcon = CategoryIconHelper.getCategoryIcon(sub.name, sub.iconName)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSubSelected) itemCatColor.copy(alpha = 0.24f) else Color(0x14FFFFFF),
                                            border = BorderStroke(1.dp, if (isSubSelected) itemCatColor else Color.Transparent),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    onCategoryAndSubcategorySelected(cat.name, sub.name)
                                                    onDismiss()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(7.dp))
                                                            .background(itemCatColor.copy(alpha = 0.2f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = subIcon,
                                                            contentDescription = sub.name,
                                                            tint = itemCatColor,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = sub.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSubSelected) TextWhite else TextWhite.copy(alpha = 0.9f)
                                                    )
                                                }
                                                if (isSubSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = itemCatColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val baseFallback = if (activeTab == 0) fallbackExpense else fallbackIncome
                    val filteredFallback = if (cleanQuery.isEmpty()) {
                        baseFallback
                    } else {
                        baseFallback.filter { it.lowercase().contains(cleanQuery) }
                    }
                    if (filteredFallback.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Kategori tidak ditemukan",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    } else {
                        items(filteredFallback) { cat ->
                            val isSelected = currentCategory.equals(cat, ignoreCase = true)
                            val catType = if (activeTab == 1) TransactionType.INCOME else TransactionType.EXPENSE
                            val catColor = CategoryIconHelper.getCategoryColor(cat, catType)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) catColor.copy(alpha = 0.2f) else DarkSurfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) catColor else DarkCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCategoryAndSubcategorySelected(cat, "")
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) catColor else catColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getCategoryIcon(cat),
                                                contentDescription = cat,
                                                tint = if (isSelected) Color.White else catColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = cat,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = TextWhite
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(18.dp)
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
