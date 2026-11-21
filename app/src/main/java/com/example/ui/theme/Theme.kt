package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HealthSuiteColorScheme = lightColorScheme(
    primary = PrimaryEmerald,
    onPrimary = Color.White,
    primaryContainer = PrototypeBannerBg,
    onPrimaryContainer = PrototypeBannerText,
    secondary = SidebarRoleCardBg,
    onSecondary = SidebarText,
    background = CanvasBg,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = PillPlannedBg,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = RowDivider,
    error = DangerText,
    onError = Color.White,
    errorContainer = DangerBg,
    onErrorContainer = DangerText
)

@Composable
fun HealthSuite360Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HealthSuiteColorScheme,
        typography = HealthSuiteTypography,
        content = content
    )
}
