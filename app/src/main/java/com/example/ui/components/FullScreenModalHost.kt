package com.example.ui.components

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.example.ui.theme.DarkBackground
import java.util.UUID

class FullScreenOverlayEntry(
    val id: String,
    initialOnDismiss: () -> Unit,
    initialContent: @Composable () -> Unit
) {
    var onDismiss by mutableStateOf(initialOnDismiss)
    var content by mutableStateOf(initialContent)
}

class FullScreenOverlayController {
    val entries = mutableStateListOf<FullScreenOverlayEntry>()

    fun register(entry: FullScreenOverlayEntry) {
        if (entries.none { it.id == entry.id }) {
            entries.add(entry)
        }
    }

    fun unregister(id: String) {
        entries.removeAll { it.id == id }
    }
}

val LocalFullScreenOverlayController = staticCompositionLocalOf<FullScreenOverlayController?> { null }

@Composable
fun FullScreenOverlayHost(
    controller: FullScreenOverlayController
) {
    controller.entries.forEach { entry ->
        key(entry.id) {
            BackHandler { entry.onDismiss() }
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(entry.id) {
                        detectTapGestures { /* Consume background touches */ }
                    },
                color = DarkBackground
            ) {
                entry.content()
            }
        }
    }
}

/**
 * Hosts a full-screen modal inside the Activity window (when available) so edge-to-edge
 * WindowInsets (status bar, display cutout, navigation bar, and IME keyboard) are 100%
 * accurate on all screen sizes including 6.7-inch displays. Falls back to an edge-to-edge
 * full-screen Dialog in isolated test environments.
 */
@Composable
fun FullScreenModalContainer(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val controller = LocalFullScreenOverlayController.current
    if (controller != null) {
        val entry = remember {
            FullScreenOverlayEntry(
                id = UUID.randomUUID().toString(),
                initialOnDismiss = onDismissRequest,
                initialContent = content
            )
        }
        SideEffect {
            entry.onDismiss = onDismissRequest
            entry.content = content
        }
        DisposableEffect(controller, entry.id) {
            controller.register(entry)
            onDispose {
                controller.unregister(entry.id)
            }
        }
    } else {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val view = LocalView.current
            val dialogWindow = (view.parent as? DialogWindowProvider)?.window
            SideEffect {
                dialogWindow?.let { window ->
                    window.setLayout(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    window.setGravity(Gravity.FILL)
                    window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                    window.setDimAmount(0f)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    window.addFlags(
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    )
                    @Suppress("DEPRECATION")
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                        window.isStatusBarContrastEnforced = false
                    }
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

fun Modifier.fullScreenTopSafePadding(): Modifier = composed {
    windowInsetsPadding(
        WindowInsets.statusBars
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    )
}

fun Modifier.fullScreenBottomSafePadding(): Modifier = composed {
    windowInsetsPadding(
        WindowInsets.navigationBars
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
    )
}
