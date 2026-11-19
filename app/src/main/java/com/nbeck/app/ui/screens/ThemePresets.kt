package com.nbeck.app.ui.screens

import com.nbeck.app.data.CustomTheme

/** Blend two ARGB colors: t = 0 → a, t = 1 → b. */
private fun mix(a: Long, b: Long, t: Float): Long {
    fun ch(c: Long, sh: Int) = ((c shr sh) and 0xFF).toFloat()
    fun one(sh: Int) = (ch(a, sh) + (ch(b, sh) - ch(a, sh)) * t).toInt().coerceIn(0, 255).toLong() shl sh
    return 0xFF000000 or one(16) or one(8) or one(0)
}

/** How bright and colorful a color is (0..1): near-black or near-gray scores low. */
private fun vivid(c: Long): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toInt(), hsv)
    return hsv[1] * 0.6f + hsv[2] * 0.4f - if (hsv[2] < 0.22f) 1f else 0f
}

private fun brightness(c: Long): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toInt(), hsv)
    return hsv[2] * (1f - 0.35f * hsv[1]) + 0.35f * hsv[1] * hsv[2]
}

/**
 * A dark arena theme built from a team's two colors: the floor and cards take a tint of the main
 * color, the brighter color becomes the buttons, the other one lights the scoreboard.
 */
private fun team(name: String, main: Long, second: Long, iconsFrom: Boolean = true): Pair<String, CustomTheme> {
    val (accent, other) = if (vivid(main) >= vivid(second)) main to second else second to main
    // LEDs and icons sit on dark panels: the second color only if it's bright enough to read.
    val led = if (vivid(other) > 0.3f && brightness(other) >= 0.7f) other else accent
    val glowBase = if (vivid(main) > 0.15f) main else accent
    return name to CustomTheme(
        bg = mix(0xFF0B0D12, glowBase, 0.16f),
        surface = mix(0xFF141821, glowBase, 0.24f),
        accent = accent,
        led = led,
        glow = glowBase,
        border = mix(0xFF2A303C, glowBase, 0.35f),
        iconFree = if (iconsFrom) led else null,
        iconChain = if (iconsFrom) accent else null,
        iconHl = if (iconsFrom) led else null,
        iconArchive = if (iconsFrom) accent else null,
    )
}

/** All 30 NBA teams, alphabetically by nickname. */
val TeamPresets: List<Pair<String, CustomTheme>> = listOf(
    team("76ers", 0xFF006BB6, 0xFFED174C),
    team("Bucks", 0xFF00471B, 0xFFEEE1C6),
    team("Bulls", 0xFFCE1141, 0xFF000000),
    team("Cavaliers", 0xFF860038, 0xFFFDBB30),
    team("Celtics", 0xFF007A33, 0xFFBA9653),
    team("Clippers", 0xFFC8102E, 0xFF1D428A),
    team("Grizzlies", 0xFF5D76A9, 0xFFF5B112),
    team("Hawks", 0xFFC8102E, 0xFFC1D32F),
    team("Heat", 0xFF98002E, 0xFFF9A01B),
    team("Hornets", 0xFF1D1160, 0xFF00A3B4),
    team("Jazz", 0xFF002B5C, 0xFFF9A01B),
    team("Kings", 0xFF5A2D81, 0xFFB4BCC3),
    team("Knicks", 0xFF006BB6, 0xFFF58426),
    team("Lakers", 0xFF552583, 0xFFFDB927),
    team("Magic", 0xFF0077C0, 0xFFC4CED4),
    team("Mavericks", 0xFF00538C, 0xFFB8C4CA),
    team("Nets", 0xFF2A2A2A, 0xFFFFFFFF),
    team("Nuggets", 0xFF0E2240, 0xFFFEC524),
    team("Pacers", 0xFF002D62, 0xFFFDBB30),
    team("Pelicans", 0xFF0C2340, 0xFFC8A15A),
    team("Pistons", 0xFFC8102E, 0xFF1D42BA),
    team("Raptors", 0xFFCE1141, 0xFFA1A1A4),
    team("Rockets", 0xFFCE1141, 0xFFC4CED4),
    team("Spurs", 0xFF8A8D8F, 0xFFC4CED4),
    team("Suns", 0xFF1D1160, 0xFFE56020),
    team("Thunder", 0xFF007AC1, 0xFFEF3B24),
    team("Timberwolves", 0xFF0C2340, 0xFF78BE20),
    team("Trail Blazers", 0xFFE03A3E, 0xFFC4CED4),
    team("Warriors", 0xFF1D428A, 0xFFFFC72C),
    team("Wizards", 0xFF002B5C, 0xFFE31837),
)

/** More ideas beyond the teams. */
val IdeaPresets: List<Pair<String, CustomTheme>> = listOf(
    "Arena" to CustomTheme(),
    "Neon" to CustomTheme(bg = 0xFF0A0A14, surface = 0xFF15152A, accent = 0xFFE879F9, led = 0xFF38BDF8, glow = 0xFF7B4DDB, border = 0xFF7B4DDB, parquet = false,
        iconFree = 0xFF38BDF8, iconChain = 0xFFE879F9, iconHl = 0xFF4ADE80, iconArchive = 0xFFFACC15),
    "Sunset" to CustomTheme(bg = 0xFF1A0B1E, surface = 0xFF2A1430, accent = 0xFFFF5E62, led = 0xFFFFC371, glow = 0xFFFF7E5F,
        iconFree = 0xFFFFC371, iconChain = 0xFFFF5E62, iconHl = 0xFFFF9A8B, iconArchive = 0xFFFFC371),
    "Ocean" to CustomTheme(bg = 0xFF061A2B, surface = 0xFF0B2A42, accent = 0xFF00B4D8, led = 0xFF90E0EF, glow = 0xFF0077B6, parquet = false,
        iconFree = 0xFF90E0EF, iconChain = 0xFF48CAE4, iconHl = 0xFF00F5D4, iconArchive = 0xFFCAF0F8),
    "Forest" to CustomTheme(bg = 0xFF0D1A12, surface = 0xFF15281C, accent = 0xFF52B788, led = 0xFFB7E4C7, glow = 0xFF2D6A4F, wood = 0xFF6B4F2A),
    "Lava" to CustomTheme(bg = 0xFF140806, surface = 0xFF241210, accent = 0xFFFF4500, led = 0xFFFFB000, glow = 0xFFFF4500,
        iconFree = 0xFFFFB000, iconChain = 0xFFFF6A3D, iconHl = 0xFFFFD166, iconArchive = 0xFFFF4500),
    "Retro 90s" to CustomTheme(bg = 0xFF1B0F2E, surface = 0xFF2A1A45, accent = 0xFF00E5FF, led = 0xFFFF4FD8, glow = 0xFFFF4FD8, border = 0xFF00E5FF,
        iconFree = 0xFF00E5FF, iconChain = 0xFFFF4FD8, iconHl = 0xFFFFE600, iconArchive = 0xFF7CFF4F),
    "Matrix" to CustomTheme(bg = 0xFF020A02, surface = 0xFF061806, accent = 0xFF00FF41, led = 0xFF00FF41, glow = 0xFF00C832, parquet = false,
        text1 = 0xFFC8FFD0, border = 0xFF0F5A1C, green = 0xFF00C832, iconFree = 0xFF00FF41, iconChain = 0xFF00FF41, iconHl = 0xFF00FF41, iconArchive = 0xFF00FF41),
    "Midnight Gold" to CustomTheme(bg = 0xFF0A0A0A, surface = 0xFF161616, accent = 0xFFD4AF37, led = 0xFFD4AF37, glow = 0xFFD4AF37, border = 0xFF3A3220,
        iconFree = 0xFFD4AF37, iconChain = 0xFFD4AF37, iconHl = 0xFFD4AF37, iconArchive = 0xFFD4AF37, iconStats = 0xFFD4AF37, iconAch = 0xFFD4AF37),
    "Mono" to CustomTheme(bg = 0xFF0E0E0E, surface = 0xFF1A1A1A, accent = 0xFFFFFFFF, led = 0xFFFFFFFF, glow = 0xFF9A9A9A, parquet = false,
        iconFree = 0xFFFFFFFF, iconChain = 0xFFFFFFFF, iconHl = 0xFFFFFFFF, iconArchive = 0xFFFFFFFF, iconStats = 0xFFFFFFFF, iconAch = 0xFFFFFFFF),
    "Blue & White" to CustomTheme(bg = 0xFFF4F8FF, surface = 0xFFFFFFFF, accent = 0xFF0038B8, led = 0xFF4DA3FF, glow = 0xFF0038B8, parquet = false,
        iconFree = 0xFF0038B8, iconChain = 0xFF0038B8, iconHl = 0xFF0038B8, iconArchive = 0xFF0038B8, iconStats = 0xFF0038B8, iconAch = 0xFF0038B8),
    "Hardwood" to CustomTheme(bg = 0xFFF3E3C3, surface = 0xFFFFF6E5, accent = 0xFFC8102E, led = 0xFFFFB021, glow = 0xFFE0A050, wood = 0xFFC8935A,
        iconFree = 0xFF1D428A, iconChain = 0xFF7A3E9D, iconHl = 0xFF1E8C4E, iconArchive = 0xFFB7791F),
    "Beach" to CustomTheme(bg = 0xFFFFF4E8, surface = 0xFFFFFFFF, accent = 0xFF00A6A6, led = 0xFFFFB021, glow = 0xFFFFC72C,
        iconFree = 0xFF00A6A6, iconChain = 0xFFFF6F61, iconHl = 0xFF2EAD5B, iconArchive = 0xFFE09B00),
    "Candy" to CustomTheme(bg = 0xFFFFF0F6, surface = 0xFFFFFFFF, accent = 0xFFFF4F9A, led = 0xFFB15CFF, glow = 0xFFFF9AC9, parquet = false,
        iconFree = 0xFF4FB8FF, iconChain = 0xFFB15CFF, iconHl = 0xFF2FBF71, iconArchive = 0xFFFF9F1C),
    "Ice" to CustomTheme(bg = 0xFFE8F4FA, surface = 0xFFFFFFFF, accent = 0xFF0077B6, led = 0xFF38BDF8, glow = 0xFF90E0EF, parquet = false,
        iconFree = 0xFF0077B6, iconChain = 0xFF5E60CE, iconHl = 0xFF0096C7, iconArchive = 0xFF48CAE4),
    "Snow" to CustomTheme(bg = 0xFFEEF2F8, surface = 0xFFFFFFFF, accent = 0xFF1D8CF2, led = 0xFF38BDF8, parquet = false),
)
