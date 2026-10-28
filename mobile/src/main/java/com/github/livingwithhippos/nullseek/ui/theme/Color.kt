package com.github.livingwithhippos.nullseek.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.github.livingwithhippos.nullseek.data.repository.ThemeColor

// Purple (Default static fallback)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

internal val PurpleDarkColorScheme =
    darkColorScheme(
        primary = Purple80,
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = PurpleGrey80,
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = Pink80,
        onTertiary = Color(0xFF492532),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD8E4),
    )

internal val PurpleLightColorScheme =
    lightColorScheme(
        primary = Purple40,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = PurpleGrey40,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1D192B),
        tertiary = Pink40,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFD8E4),
        onTertiaryContainer = Color(0xFF31111D),
    )

// Blue
internal val BlueDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFA8C7FA),
        onPrimary = Color(0xFF062E6F),
        primaryContainer = Color(0xFF0842A0),
        onPrimaryContainer = Color(0xFFD3E3FD),
        secondary = Color(0xFFBEC6DC),
        onSecondary = Color(0xFF283141),
        secondaryContainer = Color(0xFF3E4759),
        onSecondaryContainer = Color(0xFFDAE2F9),
        tertiary = Color(0xFFDEBCDF),
        onTertiary = Color(0xFF402843),
        tertiaryContainer = Color(0xFF583E5B),
        onTertiaryContainer = Color(0xFFFBD7FC),
    )

internal val BlueLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF0B57D0),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD3E3FD),
        onPrimaryContainer = Color(0xFF041E49),
        secondary = Color(0xFF565F71),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDAE2F9),
        onSecondaryContainer = Color(0xFF131C2B),
        tertiary = Color(0xFF705575),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFBD7FC),
        onTertiaryContainer = Color(0xFF29132E),
    )

// Teal
internal val TealDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF80D5D4),
        onPrimary = Color(0xFF003737),
        primaryContainer = Color(0xFF004F4F),
        onPrimaryContainer = Color(0xFF9CF1F0),
        secondary = Color(0xFFB0CCCB),
        onSecondary = Color(0xFF1B3534),
        secondaryContainer = Color(0xFF324B4B),
        onSecondaryContainer = Color(0xFFCCE8E7),
        tertiary = Color(0xFFB3C8E8),
        onTertiary = Color(0xFF1C314B),
        tertiaryContainer = Color(0xFF334863),
        onTertiaryContainer = Color(0xFFD3E4FF),
    )

internal val TealLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF006A6A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF9CF1F0),
        onPrimaryContainer = Color(0xFF002020),
        secondary = Color(0xFF4A6363),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCCE8E7),
        onSecondaryContainer = Color(0xFF051F1F),
        tertiary = Color(0xFF4B607C),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD3E4FF),
        onTertiaryContainer = Color(0xFF041C35),
    )

// Green
internal val GreenDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF9CD67D),
        onPrimary = Color(0xFF0C3900),
        primaryContainer = Color(0xFF205107),
        onPrimaryContainer = Color(0xFFB8F397),
        secondary = Color(0xFFBDCBB0),
        onSecondary = Color(0xFF273420),
        secondaryContainer = Color(0xFF3E4A35),
        onSecondaryContainer = Color(0xFFD9E7CB),
        tertiary = Color(0xFFA0CFD0),
        onTertiary = Color(0xFF003738),
        tertiaryContainer = Color(0xFF1E4E4F),
        onTertiaryContainer = Color(0xFFBCEBEC),
    )

internal val GreenLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF386A20),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB8F397),
        onPrimaryContainer = Color(0xFF042100),
        secondary = Color(0xFF55624C),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD9E7CB),
        onSecondaryContainer = Color(0xFF131F0D),
        tertiary = Color(0xFF386667),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFBCEBEC),
        onTertiaryContainer = Color(0xFF002021),
    )

// Orange
internal val OrangeDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFFFB77C),
        onPrimary = Color(0xFF4D2700),
        primaryContainer = Color(0xFF6D3900),
        onPrimaryContainer = Color(0xFFFFDCC2),
        secondary = Color(0xFFE3C0A6),
        onSecondary = Color(0xFF422C19),
        secondaryContainer = Color(0xFF5A422E),
        onSecondaryContainer = Color(0xFFFFDCC2),
        tertiary = Color(0xFFC4CA92),
        onTertiary = Color(0xFF2E3309),
        tertiaryContainer = Color(0xFF444A1E),
        onTertiaryContainer = Color(0xFFE0E6AC),
    )

internal val OrangeLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF904D00),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDCC2),
        onPrimaryContainer = Color(0xFF2E1500),
        secondary = Color(0xFF745944),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFDCC2),
        onSecondaryContainer = Color(0xFF2A1707),
        tertiary = Color(0xFF5C6233),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE0E6AC),
        onTertiaryContainer = Color(0xFF191E00),
    )

// Red
internal val RedDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFFFB4AB),
        onPrimary = Color(0xFF5F1412),
        primaryContainer = Color(0xFF7D2A26),
        onPrimaryContainer = Color(0xFFFFDAD5),
        secondary = Color(0xFFE7BDB7),
        onSecondary = Color(0xFF442926),
        secondaryContainer = Color(0xFF5D3F3B),
        onSecondaryContainer = Color(0xFFFFDAD5),
        tertiary = Color(0xFFE0C38C),
        onTertiary = Color(0xFF3F2E04),
        tertiaryContainer = Color(0xFF574419),
        onTertiaryContainer = Color(0xFFFEDF9E),
    )

internal val RedLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF9C413C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDAD5),
        onPrimaryContainer = Color(0xFF410002),
        secondary = Color(0xFF775652),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFDAD5),
        onSecondaryContainer = Color(0xFF2C1512),
        tertiary = Color(0xFF715B2E),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFEDF9E),
        onTertiaryContainer = Color(0xFF261900),
    )

// TorBox
val TorBoxGreen = Color(0xFF04BF8A)

internal val TorBoxDarkColorScheme =
    darkColorScheme(
        primary = TorBoxGreen,
        onPrimary = Color(0xFF00382A),
        primaryContainer = Color(0xFF00513C),
        onPrimaryContainer = Color(0xFF8AF7CB),
        secondary = Color(0xFFB3CCC0),
        onSecondary = Color(0xFF1F352C),
        secondaryContainer = Color(0xFF354B42),
        onSecondaryContainer = Color(0xFFCFE9DC),
        tertiary = Color(0xFFA6CCE0),
        onTertiary = Color(0xFF0A3445),
        tertiaryContainer = Color(0xFF254B5C),
        onTertiaryContainer = Color(0xFFC2E8FD),
    )

internal val TorBoxLightColorScheme =
    lightColorScheme(
        primary = TorBoxGreen,
        onPrimary = Color(0xFF00382A),
        primaryContainer = Color(0xFF8AF7CB),
        onPrimaryContainer = Color(0xFF002116),
        secondary = Color(0xFF4C6358),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCFE9DC),
        onSecondaryContainer = Color(0xFF092017),
        tertiary = Color(0xFF3E6374),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFC2E8FD),
        onTertiaryContainer = Color(0xFF001F2A),
    )

fun ThemeColor.colorScheme(darkTheme: Boolean): ColorScheme =
    when (this) {
        ThemeColor.SYSTEM, ThemeColor.PURPLE -> if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
        ThemeColor.BLUE -> if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
        ThemeColor.TEAL -> if (darkTheme) TealDarkColorScheme else TealLightColorScheme
        ThemeColor.GREEN -> if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme
        ThemeColor.ORANGE -> if (darkTheme) OrangeDarkColorScheme else OrangeLightColorScheme
        ThemeColor.RED -> if (darkTheme) RedDarkColorScheme else RedLightColorScheme
        ThemeColor.TORBOX -> if (darkTheme) TorBoxDarkColorScheme else TorBoxLightColorScheme
    }

fun ThemeColor.swatchColor(darkTheme: Boolean): Color? =
    when (this) {
        ThemeColor.SYSTEM -> null
        else -> colorScheme(darkTheme).primary
    }
