package com.cldnpl.receipty

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import com.cldnpl.receipty.app.DemoSeed
import com.cldnpl.receipty.localization.I18n
import com.cldnpl.receipty.models.AppSettings
import com.cldnpl.receipty.models.Appearance
import com.cldnpl.receipty.theme.Haptics
import com.cldnpl.receipty.theme.LocalDark
import com.cldnpl.receipty.theme.Palette
import com.cldnpl.receipty.views.RootView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        DemoSeed.screen = intent?.getStringExtra("screen")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dark = when (AppSettings.appearance) {
                Appearance.SYSTEM -> isSystemInDarkTheme()
                Appearance.LIGHT -> false
                Appearance.DARK -> true
            }
            // Le icone della barra di stato seguono il tema dell'app, non quello del sistema.
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            val view = LocalView.current
            LaunchedEffect(view) { Haptics.view = view }

            // Come `dynamicTypeSize(...xxLarge)` su iOS: il testo cresce, ma fino a un certo punto.
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDark provides dark,
                LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f)),
            ) {
                val colors = if (dark) {
                    darkColorScheme(primary = Palette.pink, surface = Palette.card, onSurface = Palette.ink)
                } else {
                    lightColorScheme(primary = Palette.raspberry, surface = Palette.card, onSurface = Palette.ink)
                }
                MaterialTheme(colorScheme = colors) {
                    CompositionLocalProvider(
                        LocalTextSelectionColors provides TextSelectionColors(Palette.pink, Palette.pink.copy(alpha = 0.3f)),
                    ) {
                        RootView()
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // La lingua dell'app si può cambiare anche dalle impostazioni di sistema (Android 13+).
        I18n.syncWithSystem(this)
    }
}
