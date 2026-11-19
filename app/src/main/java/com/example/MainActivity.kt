package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.ui.components.FullScreenOverlayController
import com.example.ui.components.FullScreenOverlayHost
import com.example.ui.components.LocalFullScreenOverlayController
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.WelcomeOnboardingScreen
import com.example.ui.theme.AppThemeState
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.getThemeColorByName
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()
    private val pendingWidgetAction = mutableStateOf<Pair<String, Long>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.data.repository.FinanceRepository.setApplicationContext(applicationContext)
        viewModel.initStorage(applicationContext)
        extractWidgetAction(intent)

        // Enable true immersive fullscreen edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            val settings by viewModel.settings.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.trySilentSignIn(this@MainActivity)
            }

            // Dynamically synchronize system bar icons and scrims with dark mode
            LaunchedEffect(settings.isDarkMode) {
                AppThemeState.isDarkMode = settings.isDarkMode
                val barStyle = if (settings.isDarkMode) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }

            // Keep AppThemeState in sync with current settings
            LaunchedEffect(settings.themeColorName) {
                val color = getThemeColorByName(settings.themeColorName)
                AppThemeState.currentAccentColor = color
                AppThemeState.currentThemeName = settings.themeColorName
            }

            // Track lock status across orientation changes
            var isAppUnlocked by rememberSaveable { mutableStateOf(!settings.passcodeEnabled) }

            // If passcode gets disabled in settings, unlock immediately
            LaunchedEffect(settings.passcodeEnabled) {
                if (!settings.passcodeEnabled) {
                    isAppUnlocked = true
                }
            }

            // Global Text Size Scaling across the whole app
            val currentDensity = LocalDensity.current
            val fontScaleMultiplier = (settings.textSizePercent / 100f).coerceIn(0.75f, 1.5f)
            val fullScreenOverlayController = remember { FullScreenOverlayController() }

            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = currentDensity.fontScale * fontScaleMultiplier
                ),
                LocalFullScreenOverlayController provides fullScreenOverlayController
            ) {
                MyApplicationTheme(
                    darkTheme = settings.isDarkMode,
                    themeColor = getThemeColorByName(settings.themeColorName)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = DarkBackground
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (settings.passcodeEnabled && !isAppUnlocked) {
                                AppLockScreen(
                                    expectedPasscode = settings.appPasscode,
                                    onUnlockSuccess = { isAppUnlocked = true }
                                )
                            } else if (!settings.hasCompletedOnboarding) {
                                WelcomeOnboardingScreen(viewModel = viewModel)
                            } else {
                                val currentWidgetAction = pendingWidgetAction.value
                                MainScreen(
                                    viewModel = viewModel,
                                    widgetAction = currentWidgetAction?.first,
                                    widgetActionId = currentWidgetAction?.second ?: 0L,
                                    onConsumeWidgetAction = { pendingWidgetAction.value = null }
                                )
                                FullScreenOverlayHost(controller = fullScreenOverlayController)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractWidgetAction(intent)
    }

    private fun extractWidgetAction(intent: Intent?) {
        val action = intent?.getStringExtra(com.example.widget.WidgetNavigation.EXTRA_WIDGET_ACTION)
        if (!action.isNullOrBlank()) {
            pendingWidgetAction.value = action to System.nanoTime()
            intent.removeExtra(com.example.widget.WidgetNavigation.EXTRA_WIDGET_ACTION)
        }
    }
}
