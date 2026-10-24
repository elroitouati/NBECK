package com.nbeck.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbeck.app.R
import com.nbeck.app.data.ThemeMode
import com.nbeck.app.game.Mark

/*
 * NBECK design tokens.
 * Direction: "arena at night" — dark navy court, warm parquet, spotlight falloff, an LED scoreboard.
 * One dominant hue (basketball orange) + neutrals; the green/yellow/gray grading colors are
 * semantic and are never used decoratively. Team colors appear only where the spec asks.
 */
@Immutable
data class NbColors(
    val isDark: Boolean,
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val border: Color,
    val borderStrong: Color,
    val text1: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val onAccent: Color,
    val green: Color,
    val onGreen: Color,
    val yellow: Color,
    val onYellow: Color,
    val gray: Color,
    val onGray: Color,
    val spotlight: Color,
    val woodA: Color,
    val woodB: Color,
    // The scoreboard is a physical dark object in both themes.
    val board: Color = Color(0xFF07090D),
    val boardEdge: Color = Color(0xFF232833),
    val led: Color = Color(0xFFFFB021),
    val ledDim: Color = Color(0x22FFB021),
) {
    fun tile(m: Mark) = when (m) { Mark.GREEN -> green; Mark.YELLOW -> yellow; Mark.GRAY -> gray }
    fun onTile(m: Mark) = when (m) { Mark.GREEN -> onGreen; Mark.YELLOW -> onYellow; Mark.GRAY -> onGray }
}

val DarkColors = NbColors(
    isDark = true,
    bg = Color(0xFF0B0D12),
    surface1 = Color(0xFF141821),
    surface2 = Color(0xFF1B202B),
    border = Color(0x1AFFFFFF),
    borderStrong = Color(0x29FFFFFF),
    text1 = Color(0xFFF4F6F8),
    text2 = Color(0xADF4F6F8),
    text3 = Color(0x70F4F6F8),
    accent = Color(0xFFFF7A1A),
    onAccent = Color(0xFF1C0C00),
    green = Color(0xFF34C759),
    onGreen = Color(0xFF052611),
    yellow = Color(0xFFF7C631),
    onYellow = Color(0xFF2A2100),
    gray = Color(0xFF2A303B),
    onGray = Color(0xFFDDE1E7),
    spotlight = Color(0x33FFE2B8),
    woodA = Color(0xFF3A2414),
    woodB = Color(0xFF2B1A0E),
)

val LightColors = NbColors(
    isDark = false,
    bg = Color(0xFFF5F3EE),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFECE8E0),
    border = Color(0x1A14181D),
    borderStrong = Color(0x2E14181D),
    text1 = Color(0xFF14181D),
    text2 = Color(0xA814181D),
    text3 = Color(0x7514181D),
    accent = Color(0xFFDD5A00),
    onAccent = Color(0xFFFFFFFF),
    green = Color(0xFF2BB35A),
    onGreen = Color(0xFF03200C),
    yellow = Color(0xFFF2BC12),
    onYellow = Color(0xFF2A2000),
    gray = Color(0xFFD9DCE1),
    onGray = Color(0xFF3A4049),
    spotlight = Color(0x66FFFFFF),
    woodA = Color(0xFFE9C79A),
    woodB = Color(0xFFDDB685),
)

@OptIn(ExperimentalTextApi::class)
private fun heebo(w: Int) = Font(R.font.heebo, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

val Heebo = FontFamily(heebo(400), heebo(500), heebo(700), heebo(800), heebo(900))
val Bebas = FontFamily(Font(R.font.bebas_neue))
/** Dot-matrix LED face for the scoreboard: counters, countdown, stat numbers. */
@OptIn(ExperimentalTextApi::class)
val Led = FontFamily(
    Font(R.font.doto, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900), FontVariation.Setting("ROND", 100f))),
)

object Type {
    val display = TextStyle(fontFamily = Bebas, fontSize = 56.sp, lineHeight = 56.sp, letterSpacing = 2.sp)
    val led = TextStyle(fontFamily = Led, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 30.sp)
    val h1 = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 38.sp)
    val h2 = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp)
    val title = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp)
    val body = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp)
    val bodyStrong = body.copy(fontWeight = FontWeight.Bold)
    val label = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp)
    val tile = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 16.sp)
}

/** Spacing (4pt grid) and radii. One radius language: 12 controls, 16 tiles/cards, 24 panels. */
object Space {
    val xs = 4.dp; val s = 8.dp; val m = 12.dp; val l = 16.dp; val xl = 20.dp; val xxl = 24.dp; val xxxl = 32.dp
    val gutter = 16.dp
    val rControl = 12.dp; val rTile = 10.dp; val rCard = 16.dp; val rPanel = 24.dp
}

/** Motion tokens (Emil Kowalski curves). */
object Motion {
    val easeOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val easeInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
    val drawer = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    const val press = 140
    const val small = 220
    const val page = 320
    const val flip = 420
    const val flipStagger = 160
}

val LocalNb = staticCompositionLocalOf { DarkColors }

@Composable
fun NbeckTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.text1,
        surface = c.surface1, onSurface = c.text1, surfaceVariant = c.surface2, onSurfaceVariant = c.text2,
        outline = c.borderStrong,
    ) else lightColorScheme(
        primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.text1,
        surface = c.surface1, onSurface = c.text1, surfaceVariant = c.surface2, onSurfaceVariant = c.text2,
        outline = c.borderStrong,
    )
    CompositionLocalProvider(LocalNb provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(bodyLarge = Type.body, bodyMedium = Type.body, labelLarge = Type.label, titleLarge = Type.title),
            content = content,
        )
    }
}

val nb: NbColors @Composable get() = LocalNb.current
