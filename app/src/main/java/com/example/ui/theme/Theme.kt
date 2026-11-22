package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ArsamLightColorScheme = lightColorScheme(
    primary = CrimsonPrimary,
    onPrimary = ClinicalSurface,
    primaryContainer = CrimsonSoftContainer,
    onPrimaryContainer = OnCrimsonContainer,
    secondary = SlateSecondary,
    onSecondary = ClinicalSurface,
    secondaryContainer = SlateContainer,
    onSecondaryContainer = InkPrimary,
    tertiary = VerifiedEmerald,
    onTertiary = ClinicalSurface,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = VerifiedEmerald,
    error = EmergencyCoral,
    onError = ClinicalSurface,
    background = ClinicalBackground,
    onBackground = InkPrimary,
    surface = ClinicalSurface,
    onSurface = InkPrimary,
    surfaceVariant = ClinicalSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = ClinicalOutline
)

private val ArsamShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun ArsamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ArsamLightColorScheme,
        typography = ArsamTypography,
        shapes = ArsamShapes,
        content = content
    )
}
