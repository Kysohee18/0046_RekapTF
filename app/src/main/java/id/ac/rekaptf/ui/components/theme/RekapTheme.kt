package id.ac.rekaptf.ui.components.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Token warna RekapTF sesuai PRD desain v1.0 bagian 5.2.
 *
 * Semua layar memakai token di sini, bukan nilai heksadesimal lepas. Palet ini
 * adalah identitas desain, jadi tema gelap sistem tidak diterapkan.
 */
internal object RekapColor {
    val Background = Color(0xFFF5F7F4)
    val Ink = Color(0xFF00262B)
    val Accent = Color(0xFFABFFAE)
    val InfoSurface = Color(0xFFCDE3E6)
    val Surface = Color(0xFFFFFFFF)
    val MutedText = Color(0xFF50696C)
    val Outline = Color(0xFFCED9D6)
    val Danger = Color(0xFFB42318)
    val DangerSurface = Color(0xFFFFF0E9)
    val SuccessSurface = Color(0xFFE3F1E9)
    val Success = Color(0xFF0B664A)
    val LocalListInk = Color(0xFF1D2922)
    val LocalListMuted = Color(0xFF5F6B64)
    val LocalListStatus = Color(0xFF684515)
}

/**
 * Bentuk. Tombol, input, panel besar, dan kartu umum memakai sudut tegas.
 * Hanya komponen daftar lokal yang membulat (PRD 5.4).
 */
internal object RekapShape {
    val Square = RoundedCornerShape(0.dp)
    val TransactionCard = RoundedCornerShape(12.dp)
    val DraftCard = RoundedCornerShape(14.dp)
    val Badge = RoundedCornerShape(20.dp)
}

internal object RekapSpacing {
    val Page = 24.dp
    val Section = 20.dp
    val Auth = 22.dp
    val Card = 16.dp
    val Tight = 12.dp
    val Small = 8.dp
    val Micro = 4.dp
}

internal object RekapSize {
    val Touch = 48.dp
    val Action = 54.dp
    val Input = 50.dp
    val TransactionRow = 68.dp
    val DraftRow = 72.dp
    val ContentWidth = 345.dp
    val MaxContentWidth = 600.dp
}

/**
 * Inter belum tersedia sebagai aset di proyek. Fallback sistem dipakai sampai
 * berkas font resmi disetujui, dan tidak diunduh saat runtime (PRD 5.3).
 */
internal object RekapFont {
    val Inter: FontFamily = FontFamily.SansSerif
    val LocalList: FontFamily = FontFamily.SansSerif
}

private val LightColors = lightColorScheme(
    primary = RekapColor.Accent,
    onPrimary = RekapColor.Ink,
    primaryContainer = RekapColor.Accent,
    onPrimaryContainer = RekapColor.Ink,
    secondary = Color(0xFF315C50),
    onSecondary = Color.White,
    secondaryContainer = RekapColor.InfoSurface,
    onSecondaryContainer = RekapColor.Ink,
    tertiary = RekapColor.Success,
    onTertiary = Color.White,
    tertiaryContainer = RekapColor.SuccessSurface,
    onTertiaryContainer = RekapColor.Success,
    surface = RekapColor.Surface,
    onSurface = RekapColor.Ink,
    surfaceVariant = Color(0xFFE3EAE6),
    onSurfaceVariant = RekapColor.MutedText,
    background = RekapColor.Background,
    onBackground = RekapColor.Ink,
    surfaceContainerLowest = RekapColor.Surface,
    surfaceContainerLow = RekapColor.Surface,
    surfaceContainer = RekapColor.Background,
    surfaceContainerHigh = RekapColor.Surface,
    outline = RekapColor.Outline,
    outlineVariant = RekapColor.Outline,
    inverseSurface = RekapColor.Ink,
    inverseOnSurface = RekapColor.Background,
    error = RekapColor.Danger,
    onError = Color.White,
    errorContainer = RekapColor.DangerSurface,
    onErrorContainer = Color(0xFF741A13),
)

private val RekapShapes = Shapes(
    extraSmall = RekapShape.Square,
    small = RekapShape.Square,
    medium = RekapShape.Square,
    large = RekapShape.Square,
)

private val Display = type(FontWeight.Bold, 32, 40)
private val CardTitle = type(FontWeight.Bold, 24, 32)
private val Section = type(FontWeight.SemiBold, 17, 24)
private val Body = type(FontWeight.Normal, 14, 21)
private val Label = type(FontWeight.SemiBold, 14, 20)
private val Caption = type(FontWeight.Normal, 12, 18)
private val Micro = type(FontWeight.Medium, 11, 16)

private val RekapTypography = Typography(
    displaySmall = Display,
    headlineMedium = Display,
    headlineSmall = CardTitle,
    titleLarge = CardTitle,
    titleMedium = Section,
    titleSmall = Label,
    bodyLarge = Body,
    bodyMedium = Body,
    bodySmall = Caption,
    labelLarge = Label,
    labelMedium = Caption,
    labelSmall = Micro,
)

private fun type(weight: FontWeight, size: Int, lineHeight: Int): TextStyle = TextStyle(
    fontFamily = RekapFont.Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

@Composable
internal fun RekapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = RekapShapes,
        typography = RekapTypography,
        content = content,
    )
}
