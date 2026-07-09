package com.openwrtmanager.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val OpenWrtBlue = Color(0xFF0288D1)
private val OpenWrtBlueDark = Color(0xFF4FC3F7)

private val LightColors = lightColorScheme(
    primary = OpenWrtBlue,
    secondary = Color(0xFF00695C),
    tertiary = Color(0xFFEF6C00)
)

private val DarkColors = darkColorScheme(
    primary = OpenWrtBlueDark,
    secondary = Color(0xFF4DB6AC),
    tertiary = Color(0xFFFFB74D)
)

@Composable
fun OpenWrtManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
