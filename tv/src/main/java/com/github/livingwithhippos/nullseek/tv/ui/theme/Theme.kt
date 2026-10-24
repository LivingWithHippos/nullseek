package com.github.livingwithhippos.nullseek.tv.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.github.livingwithhippos.nullseek.data.repository.ThemeColor
import com.github.livingwithhippos.nullseek.data.repository.ThemeMode
import androidx.compose.material3.ColorScheme as M3ColorScheme
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

@Composable
fun shouldUseDarkTheme(themeMode: ThemeMode): Boolean =
    when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

private val M3Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )

/** Maps a standard Material3 scheme to the TV one so both libraries render with the same palette. */
private fun M3ColorScheme.toTvColorScheme(dark: Boolean): ColorScheme {
    val build = if (dark) ::darkTv else ::lightTv
    return build(this)
}

private fun darkTv(c: M3ColorScheme): ColorScheme =
    darkColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.primaryContainer,
        onPrimaryContainer = c.onPrimaryContainer,
        inversePrimary = c.inversePrimary,
        secondary = c.secondary,
        onSecondary = c.onSecondary,
        secondaryContainer = c.secondaryContainer,
        onSecondaryContainer = c.onSecondaryContainer,
        tertiary = c.tertiary,
        onTertiary = c.onTertiary,
        tertiaryContainer = c.tertiaryContainer,
        onTertiaryContainer = c.onTertiaryContainer,
        background = c.background,
        onBackground = c.onBackground,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.surfaceVariant,
        onSurfaceVariant = c.onSurfaceVariant,
        surfaceTint = c.surfaceTint,
        inverseSurface = c.inverseSurface,
        inverseOnSurface = c.inverseOnSurface,
        error = c.error,
        onError = c.onError,
        errorContainer = c.errorContainer,
        onErrorContainer = c.onErrorContainer,
        border = c.outline,
        borderVariant = c.outlineVariant,
        scrim = c.scrim,
    )

private fun lightTv(c: M3ColorScheme): ColorScheme =
    lightColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.primaryContainer,
        onPrimaryContainer = c.onPrimaryContainer,
        inversePrimary = c.inversePrimary,
        secondary = c.secondary,
        onSecondary = c.onSecondary,
        secondaryContainer = c.secondaryContainer,
        onSecondaryContainer = c.onSecondaryContainer,
        tertiary = c.tertiary,
        onTertiary = c.onTertiary,
        tertiaryContainer = c.tertiaryContainer,
        onTertiaryContainer = c.onTertiaryContainer,
        background = c.background,
        onBackground = c.onBackground,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.surfaceVariant,
        onSurfaceVariant = c.onSurfaceVariant,
        surfaceTint = c.surfaceTint,
        inverseSurface = c.inverseSurface,
        inverseOnSurface = c.inverseOnSurface,
        error = c.error,
        onError = c.onError,
        errorContainer = c.errorContainer,
        onErrorContainer = c.onErrorContainer,
        border = c.outline,
        borderVariant = c.outlineVariant,
        scrim = c.scrim,
    )

/**
 * Applies the same palette to `androidx.tv.material3` (focusable surfaces, cards, lists)
 * and to `androidx.compose.material3` (dialogs, text fields, snackbars).
 */
@Composable
fun TvNullSeekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: ThemeColor = ThemeColor.SYSTEM,
    content: @Composable () -> Unit,
) {
    val m3Scheme =
        if (themeColor == ThemeColor.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            themeColor.colorScheme(darkTheme)
        }

    M3MaterialTheme(
        colorScheme = m3Scheme,
        shapes = M3Shapes,
    ) {
        TvMaterialTheme(
            colorScheme = m3Scheme.toTvColorScheme(darkTheme),
            typography = Typography,
            content = content,
        )
    }
}
