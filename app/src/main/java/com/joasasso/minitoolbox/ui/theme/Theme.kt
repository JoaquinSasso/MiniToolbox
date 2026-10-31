package com.joasasso.minitoolbox.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Md3PrimaryDark,
    onPrimary = Md3OnPrimaryDark,
    primaryContainer = Md3PrimaryContainerDark,
    onPrimaryContainer = Md3OnPrimaryContainerDark,
    secondary = Md3SecondaryDark,
    onSecondary = Md3OnSecondaryDark,
    secondaryContainer = Md3SecondaryContainerDark,
    onSecondaryContainer = Md3OnSecondaryContainerDark,
    tertiary = Md3TertiaryDark,
    onTertiary = Md3OnTertiaryDark,
    tertiaryContainer = Md3TertiaryContainerDark,
    onTertiaryContainer = Md3OnTertiaryContainerDark,
    error = Md3ErrorDark,
    onError = Md3OnErrorDark,
    errorContainer = Md3ErrorContainerDark,
    onErrorContainer = Md3OnErrorContainerDark,
    background = Md3SurfaceDark,
    onBackground = Md3OnSurfaceDark,
    surface = Md3SurfaceDark,
    onSurface = Md3OnSurfaceDark,
    surfaceVariant = Md3SurfaceContainerHighestDark,
    onSurfaceVariant = Md3OnSurfaceVariantDark,
    surfaceDim = Md3SurfaceDimDark,
    surfaceBright = Md3SurfaceBrightDark,
    surfaceContainerLowest = Md3SurfaceContainerLowestDark,
    surfaceContainerLow = Md3SurfaceContainerLowDark,
    surfaceContainer = Md3SurfaceContainerDark,
    surfaceContainerHigh = Md3SurfaceContainerHighDark,
    surfaceContainerHighest = Md3SurfaceContainerHighestDark,
    outline = Md3OutlineDark,
    outlineVariant = Md3OutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = Md3PrimaryLight,
    onPrimary = Md3OnPrimaryLight,
    primaryContainer = Md3PrimaryContainerLight,
    onPrimaryContainer = Md3OnPrimaryContainerLight,
    secondary = Md3SecondaryLight,
    onSecondary = Md3OnSecondaryLight,
    secondaryContainer = Md3SecondaryContainerLight,
    onSecondaryContainer = Md3OnSecondaryContainerLight,
    tertiary = Md3TertiaryLight,
    onTertiary = Md3OnTertiaryLight,
    tertiaryContainer = Md3TertiaryContainerLight,
    onTertiaryContainer = Md3OnTertiaryContainerLight,
    error = Md3ErrorLight,
    onError = Md3OnErrorLight,
    errorContainer = Md3ErrorContainerLight,
    onErrorContainer = Md3OnErrorContainerLight,
    background = Md3SurfaceLight,
    onBackground = Md3OnSurfaceLight,
    surface = Md3SurfaceLight,
    onSurface = Md3OnSurfaceLight,
    surfaceVariant = Md3SurfaceContainerHighestLight,
    onSurfaceVariant = Md3OnSurfaceVariantLight,
    surfaceDim = Md3SurfaceDimLight,
    surfaceBright = Md3SurfaceBrightLight,
    surfaceContainerLowest = Md3SurfaceContainerLowestLight,
    surfaceContainerLow = Md3SurfaceContainerLowLight,
    surfaceContainer = Md3SurfaceContainerLight,
    surfaceContainerHigh = Md3SurfaceContainerHighLight,
    surfaceContainerHighest = Md3SurfaceContainerHighestLight,
    outline = Md3OutlineLight,
    outlineVariant = Md3OutlineVariantLight
)

@Composable
fun MiniToolboxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
