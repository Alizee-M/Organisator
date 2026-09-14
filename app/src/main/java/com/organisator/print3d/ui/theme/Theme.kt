package com.organisator.print3d.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.organisator.print3d.data.JobStatus
import com.organisator.print3d.data.ProjectStatus

// Palette volontairement neutre : gris profonds, un seul accent indigo.
private val Indigo = Color(0xFF4F46E5)
private val IndigoSoft = Color(0xFF9E8BFF)

private val LightScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E3FB),
    onPrimaryContainer = Color(0xFF1E1A5C),
    secondary = Color(0xFF4A5061),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E8EF),
    onSecondaryContainer = Color(0xFF1B1E25),
    background = Color(0xFFF6F6F8),
    onBackground = Color(0xFF14161C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14161C),
    surfaceVariant = Color(0xFFECEDF1),
    onSurfaceVariant = Color(0xFF5A6070),
    outline = Color(0xFFD3D6DE),
    outlineVariant = Color(0xFFE6E8EE),
    error = Color(0xFFD92D20),
    onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = IndigoSoft,
    onPrimary = Color(0xFF1A1440),
    primaryContainer = Color(0xFF2D2860),
    onPrimaryContainer = Color(0xFFE2DEFF),
    secondary = Color(0xFFB6BCCB),
    onSecondary = Color(0xFF222631),
    secondaryContainer = Color(0xFF272C37),
    onSecondaryContainer = Color(0xFFDDE1EA),
    background = Color(0xFF0E0F13),
    onBackground = Color(0xFFE9EBF0),
    surface = Color(0xFF161920),
    onSurface = Color(0xFFE9EBF0),
    surfaceVariant = Color(0xFF1E222B),
    onSurfaceVariant = Color(0xFF9BA2B2),
    outline = Color(0xFF2E3441),
    outlineVariant = Color(0xFF232833),
    error = Color(0xFFFF6B60),
    onError = Color(0xFF3B0A06)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
)

@Composable
fun OrganisatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

/** Couleurs de statut, déclinées pour rester lisibles dans les deux thèmes. */
object StatusPalette {
    fun color(status: JobStatus, dark: Boolean): Color = when (status) {
        JobStatus.A_FAIRE -> if (dark) Color(0xFF8E97A8) else Color(0xFF6B7280)
        JobStatus.A_IMPRIMER -> if (dark) Color(0xFF5FA8FF) else Color(0xFF2563EB)
        JobStatus.EN_COURS -> if (dark) Color(0xFFF5B547) else Color(0xFFB45309)
        JobStatus.TERMINE -> if (dark) Color(0xFF44D39A) else Color(0xFF047857)
        JobStatus.A_REFAIRE -> if (dark) Color(0xFFFF7A70) else Color(0xFFB42318)
        JobStatus.ANNULE -> if (dark) Color(0xFF5D6472) else Color(0xFF9AA0AC)
    }

    fun color(status: ProjectStatus, dark: Boolean): Color = when (status) {
        ProjectStatus.EN_COURS -> if (dark) Color(0xFF5FA8FF) else Color(0xFF2563EB)
        ProjectStatus.EN_PAUSE -> if (dark) Color(0xFFF5B547) else Color(0xFFB45309)
        ProjectStatus.TERMINE -> if (dark) Color(0xFF44D39A) else Color(0xFF047857)
        ProjectStatus.ARCHIVE -> if (dark) Color(0xFF5D6472) else Color(0xFF9AA0AC)
    }

    /** Teintes de séries pour les graphiques, dans l'ordre d'utilisation. */
    fun chartSeries(dark: Boolean): List<Color> = if (dark) listOf(
        Color(0xFF9E8BFF), Color(0xFF5FA8FF), Color(0xFF44D39A),
        Color(0xFFF5B547), Color(0xFFFF7A70), Color(0xFF8E97A8)
    ) else listOf(
        Color(0xFF4F46E5), Color(0xFF2563EB), Color(0xFF047857),
        Color(0xFFB45309), Color(0xFFB42318), Color(0xFF6B7280)
    )
}

@Composable
fun statusColor(status: JobStatus): Color =
    StatusPalette.color(status, isSystemInDarkTheme())

@Composable
fun statusColor(status: ProjectStatus): Color =
    StatusPalette.color(status, isSystemInDarkTheme())
