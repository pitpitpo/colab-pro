package com.pox.colab.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pox.colab.domain.model.ColabPalette
import com.pox.colab.domain.model.DisplayMode

// Material Design 3 Expressive AMOLED Pure Black Elevation Steps
val AmoledSurfaceLowest = Color(0xFF000000)
val AmoledSurfaceLow = Color(0xFF0C0C0C)
val AmoledSurface = Color(0xFF121212)
val AmoledSurfaceHigh = Color(0xFF1A1A1A)
val AmoledSurfaceHighest = Color(0xFF242424)

// Standard M3 Dark Slate Elevation Steps
val DarkSlateLowest = Color(0xFF0E1014)
val DarkSlateLow = Color(0xFF14171D)
val DarkSlateSurface = Color(0xFF1A1E26)
val DarkSlateHigh = Color(0xFF222732)
val DarkSlateHighest = Color(0xFF2B313E)

// M3 Shape Scale: 12.dp, 16.dp, 20.dp, 28.dp rounded corners
val ColabExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ColabM3ExpressiveTheme(
    palette: ColabPalette = ColabPalette.ColabAmber,
    displayMode: DisplayMode = DisplayMode.AmoledPureBlack,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accentColor = Color(palette.primaryHex)

    val baseScheme: ColorScheme = when {
        displayMode == DisplayMode.DynamicMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(context)
        }
        else -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF120C00),
            primaryContainer = accentColor.copy(alpha = 0.16f),
            onPrimaryContainer = accentColor,
            secondary = accentColor
        )
    }

    val colorScheme = when (displayMode) {
        DisplayMode.AmoledPureBlack -> baseScheme.copy(
            background = AmoledSurfaceLowest,
            surface = AmoledSurfaceLowest,
            surfaceContainerLowest = AmoledSurfaceLowest,
            surfaceContainerLow = AmoledSurfaceLow,
            surfaceContainer = AmoledSurface,
            surfaceContainerHigh = AmoledSurfaceHigh,
            surfaceContainerHighest = AmoledSurfaceHighest,
            primary = accentColor,
            primaryContainer = accentColor.copy(alpha = 0.16f),
            onPrimaryContainer = accentColor
        )
        DisplayMode.DarkMode -> baseScheme.copy(
            background = DarkSlateLowest,
            surface = DarkSlateLowest,
            surfaceContainerLowest = DarkSlateLowest,
            surfaceContainerLow = DarkSlateLow,
            surfaceContainer = DarkSlateSurface,
            surfaceContainerHigh = DarkSlateHigh,
            surfaceContainerHighest = DarkSlateHighest,
            primary = accentColor,
            primaryContainer = accentColor.copy(alpha = 0.16f),
            onPrimaryContainer = accentColor
        )
        DisplayMode.DynamicMaterialYou -> baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ColabExpressiveShapes,
        content = content
    )
}
