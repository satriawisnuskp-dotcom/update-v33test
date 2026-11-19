package com.example.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.components.AcaraTwoToneIcon
import com.example.ui.components.AnggaranTwoToneIcon
import com.example.ui.components.AsetTwoToneIcon
import com.example.ui.components.CariTwoToneIcon
import com.example.ui.components.DaftarTungguTwoToneIcon
import com.example.ui.components.FullScreenModalContainer
import com.example.ui.components.JalanUangTwoToneIcon
import com.example.ui.components.KategoriTwoToneIcon
import com.example.ui.components.LabelTwoToneIcon
import com.example.ui.components.NilaiBersihTwoToneNavIcon
import com.example.ui.components.PerencanaTwoToneIcon
import com.example.ui.components.TabunganTwoToneIcon
import com.example.ui.components.TagihanTwoToneIcon
import com.example.ui.components.UtangTwoToneNavIcon
import com.example.ui.components.fullScreenBottomSafePadding
import com.example.ui.components.fullScreenTopSafePadding
import com.example.ui.components.resolveUniformQuickIconTone
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun MainMenuOrderSheet(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    val currentOrder = remember {
        mutableStateListOf<String>().apply {
            addAll(settings.mainMenuOrder)
        }
    }

    val hiddenItems = remember {
        mutableStateListOf<String>().apply {
            addAll(settings.hiddenMenuItems)
        }
    }

    val density = LocalDensity.current
    val itemSpacingPx = with(density) { 8.dp.toPx() }
    val lazyListState = rememberLazyListState()
    var draggedItemName by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    BackHandler { onDismiss() }

    FullScreenModalContainer(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .fullScreenTopSafePadding()
                    .fullScreenBottomSafePadding()
            ) {
                // Header Top Bar: "Atur menu utama" and "Selesai"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Atur menu utama",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    TextButton(
                        onClick = {
                            viewModel.updateMainMenuOrder(currentOrder.toList(), hiddenItems.toList())
                            onDismiss()
                        },
                        modifier = Modifier.testTag("btn_save_menu_order")
                    ) {
                        Text(
                            text = "Selesai",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlueBright
                        )
                    }
                }

                Text(
                    text = "Geser (=) atau gunakan panah untuk mengatur urutan menu",
                    fontSize = 11.sp,
                    color = TextSubtle,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(currentOrder, key = { _, item -> item }) { index, itemName ->
                        val isHidden = hiddenItems.contains(itemName)
                        val isDraggingThis = draggedItemName == itemName

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDraggingThis) 2f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDraggingThis) dragOffsetY else 0f
                                    scaleX = if (isDraggingThis) 1.015f else 1f
                                    scaleY = if (isDraggingThis) 1.015f else 1f
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isDraggingThis -> Color(0xFF1F293B)
                                    isHidden -> DarkSurfaceElevated.copy(alpha = 0.5f)
                                    else -> DarkSurface
                                }
                            ),
                            border = BorderStroke(
                                width = if (isDraggingThis) 1.5.dp else 1.dp,
                                color = if (isDraggingThis) AccentBlue else DarkCardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Custom item icon
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceElevated,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            MenuItemIcon(itemName = itemName)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = itemName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isHidden) TextMuted else TextWhite
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Eye Toggle Icon
                                    IconButton(
                                        onClick = {
                                            if (isHidden) {
                                                hiddenItems.remove(itemName)
                                            } else {
                                                hiddenItems.add(itemName)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isHidden) "Tampilkan" else "Sembunyikan",
                                            tint = if (isHidden) TextMuted else TextWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Up Reorder
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val item = currentOrder.removeAt(index)
                                                currentOrder.add(index - 1, item)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Naik",
                                            tint = if (index > 0) TextWhite else Color(0x33FFFFFF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Down Reorder
                                    IconButton(
                                        onClick = {
                                            if (index < currentOrder.size - 1) {
                                                val item = currentOrder.removeAt(index)
                                                currentOrder.add(index + 1, item)
                                            }
                                        },
                                        enabled = index < currentOrder.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Turun",
                                            tint = if (index < currentOrder.size - 1) TextWhite else Color(0x33FFFFFF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("drag_handle_menu_$itemName")
                                            .pointerInput(itemName) {
                                                detectVerticalDragGestures(
                                                    onDragStart = {
                                                        draggedItemName = itemName
                                                        dragOffsetY = 0f
                                                    },
                                                    onVerticalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount
                                                        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                                                        val currentIdx = currentOrder.indexOf(itemName)
                                                        val currentInfo = visibleItems.firstOrNull { it.key == itemName }
                                                        if (currentIdx >= 0 && currentInfo != null) {
                                                            if (dragOffsetY > 0f && currentIdx < currentOrder.lastIndex) {
                                                                val nextName = currentOrder[currentIdx + 1]
                                                                val nextInfo = visibleItems.firstOrNull { it.key == nextName }
                                                                val stepPx = ((nextInfo?.size ?: currentInfo.size).toFloat()) + itemSpacingPx
                                                                if (dragOffsetY > stepPx * 0.55f) {
                                                                    val moved = currentOrder.removeAt(currentIdx)
                                                                    currentOrder.add(currentIdx + 1, moved)
                                                                    dragOffsetY -= stepPx
                                                                }
                                                            } else if (dragOffsetY < 0f && currentIdx > 0) {
                                                                val prevName = currentOrder[currentIdx - 1]
                                                                val prevInfo = visibleItems.firstOrNull { it.key == prevName }
                                                                val stepPx = ((prevInfo?.size ?: currentInfo.size).toFloat()) + itemSpacingPx
                                                                if (dragOffsetY < -(stepPx * 0.55f)) {
                                                                    val moved = currentOrder.removeAt(currentIdx)
                                                                    currentOrder.add(currentIdx - 1, moved)
                                                                    dragOffsetY += stepPx
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        if (draggedItemName == itemName) {
                                                            draggedItemName = null
                                                            dragOffsetY = 0f
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        if (draggedItemName == itemName) {
                                                            draggedItemName = null
                                                            dragOffsetY = 0f
                                                        }
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "Geser menu $itemName",
                                            tint = if (isDraggingThis) AccentBlue else TextMuted,
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

@Composable
fun MenuItemIcon(itemName: String, modifier: Modifier = Modifier.size(20.dp)) {
    val uniformTone = resolveUniformQuickIconTone(AccentBlue)
    when (itemName) {
        "Anggaran" -> AnggaranTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Perencana" -> PerencanaTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Acara" -> AcaraTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Aset" -> AsetTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Tabungan" -> TabunganTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Nilai Bersih" -> NilaiBersihTwoToneNavIcon(themeColor = uniformTone, isSelected = true, modifier = modifier)
        "Jalan Uang" -> JalanUangTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Utang" -> UtangTwoToneNavIcon(themeColor = uniformTone, isSelected = true, modifier = modifier)
        "Tagihan" -> TagihanTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Cari" -> CariTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Kategori" -> KategoriTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Label" -> LabelTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Daftar Tunggu" -> DaftarTungguTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        else -> Icon(Icons.Default.ViewAgenda, contentDescription = null, tint = uniformTone, modifier = modifier)
    }
}
