package com.github.livingwithhippos.nullseek.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.livingwithhippos.nullseek.data.repository.ThemeColor
import com.github.livingwithhippos.nullseek.data.repository.ThemeMode

private val SquareShapes =
    Shapes(
        extraSmall = RoundedCornerShape(0.dp),
        small = RoundedCornerShape(0.dp),
        medium = RoundedCornerShape(0.dp),
        large = RoundedCornerShape(0.dp),
        extraLarge = RoundedCornerShape(0.dp),
    )

@Composable
fun shouldUseDarkTheme(themeMode: ThemeMode): Boolean =
    when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

@Composable
fun NullSeekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: ThemeColor = ThemeColor.SYSTEM,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = themeColor == ThemeColor.SYSTEM,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && themeColor == ThemeColor.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            else -> themeColor.colorScheme(darkTheme)
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SquareShapes,
        content = content,
    )
}
