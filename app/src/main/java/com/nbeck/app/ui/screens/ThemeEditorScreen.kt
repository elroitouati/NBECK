package com.nbeck.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbeck.app.NbeckApp
import com.nbeck.app.data.CustomTheme
import com.nbeck.app.data.Settings
import com.nbeck.app.data.ThemeMode
import com.nbeck.app.game.Mark
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.components.ArenaBackground
import com.nbeck.app.ui.components.ButtonKind
import com.nbeck.app.ui.components.LedText
import com.nbeck.app.ui.components.NbButton
import com.nbeck.app.ui.components.Scoreboard
import com.nbeck.app.ui.components.TopBar
import com.nbeck.app.ui.components.Wordmark
import com.nbeck.app.ui.components.pressable
import com.nbeck.app.ui.theme.NbeckColors
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.customColors
import com.nbeck.app.ui.theme.lum
import com.nbeck.app.ui.theme.nb
import kotlinx.coroutines.launch

private val Backgrounds = listOf(0xFF0B0D12, 0xFF0F1626, 0xFF161022, 0xFF0D1A14, 0xFF1A120D, 0xFF121212, 0xFFF5F3EE, 0xFFEEF2F8, 0xFFFFF4E8, 0xFFF1F5F0)
private val DarkCards = listOf(0xFF141821, 0xFF1A2236, 0xFF221A30, 0xFF16261E, 0xFF261A13, 0xFF1E1E1E)
private val LightCards = listOf(0xFFFFFFFF, 0xFFF7F9FC, 0xFFFFFBF4, 0xFFF5FAF4, 0xFFECE8E0)
private val Accents = listOf(0xFFFF7A1A, 0xFFE03A3E, 0xFF1D8CF2, 0xFF7B4DDB, 0xFFFDB927, 0xFF2BB35A, 0xFF00A6A6, 0xFFEC4899, 0xFF552583, 0xFF007A33)
private val Leds = listOf(0xFFFFB021, 0xFFFF4D4D, 0xFF4ADE80, 0xFF38BDF8, 0xFFF4F6F8, 0xFFE879F9)

/**
 * Designs the one custom theme. The whole screen is drawn in the draft colors, so every pick is
 * previewed live; "Save" stores it as the Custom theme and switches to it.
 */
@Composable
fun ThemeEditorScreen(onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as NbeckApp
    val settings by remember { app.prefs.settings }.collectAsState(initial = Settings())
    var draft by remember { mutableStateOf<CustomTheme?>(null) }
    val d = draft ?: settings.custom ?: CustomTheme()
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    NbeckColors(customColors(d)) {
        val c = nb
        ArenaBackground(parquet = false) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                TopBar(s.themeEditor, onDone)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
                    Text(s.themeHint, style = Type.body, color = c.text2)
                    Spacer(Modifier.height(Space.l))
                    Preview()
                    Section(s.tBackground)
                    Swatches(Backgrounds, d.bg) { bg ->
                        // Cards follow the background's brightness so text stays readable on both.
                        val dark = Color(bg).lum() < 0.5f
                        val cards = if (dark) DarkCards else LightCards
                        draft = d.copy(bg = bg, surface = if (d.surface in cards) d.surface else cards.first())
                    }
                    Section(s.tCards)
                    Swatches(if (Color(d.bg).lum() < 0.5f) DarkCards else LightCards, d.surface) { draft = d.copy(surface = it) }
                    Section(s.tAccent)
                    Swatches(Accents, d.accent) { draft = d.copy(accent = it) }
                    Section(s.tLed)
                    Swatches(Leds, d.led) { draft = d.copy(led = it) }
                    Spacer(Modifier.height(Space.xl))
                }
                Column(Modifier.padding(Space.gutter)) {
                    NbButton(s.saveTheme, Modifier.fillMaxWidth()) {
                        scope.launch {
                            app.prefs.updateSettings { it.copy(custom = d, theme = ThemeMode.CUSTOM, themeChosen = true) }
                            onDone()
                        }
                    }
                }
            }
        }
    }
}

/** A small slice of the app in the draft colors: logo, a card, a button, tiles and the scoreboard. */
@Composable
private fun Preview() {
    val c = nb
    val s = LocalStrings.current
    val shape = RoundedCornerShape(Space.rPanel)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.bg).border(1.dp, c.borderStrong, shape).padding(Space.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Wordmark(height = 44.dp)
        Spacer(Modifier.height(Space.m))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Space.rCard)).background(c.surface1)
                .border(1.dp, c.border, RoundedCornerShape(Space.rCard)).padding(Space.m),
        ) {
            Text(s.previewCard, style = Type.bodyStrong, color = c.text1)
            Text(s.dailySub, style = Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = c.text2)
            Spacer(Modifier.height(Space.s))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Mark.GREEN, Mark.YELLOW, Mark.GRAY).forEach { m ->
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(c.tile(m)))
                }
                Spacer(Modifier.weight(1f))
                Scoreboard { LedText("4/8", style = Type.led.copy(fontSize = 20.sp, lineHeight = 22.sp)) }
            }
        }
        Spacer(Modifier.height(Space.m))
        NbButton(s.daily, Modifier.fillMaxWidth()) {}
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(Space.xl))
    Text(title, style = Type.title, color = nb.text1)
    Spacer(Modifier.height(Space.s))
}

@Composable
private fun Swatches(colors: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    val c = nb
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        colors.forEach { argb ->
            val color = Color(argb)
            val on = argb == selected
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(color)
                    .border(if (on) 3.dp else 1.dp, if (on) c.accent else c.borderStrong, CircleShape)
                    .pressable { onPick(argb) },
                contentAlignment = Alignment.Center,
            ) {
                if (on) Icon(Icons.Rounded.Check, null, tint = if (color.lum() > 0.55f) Color(0xFF14181D) else Color(0xFFF7F7F7))
            }
        }
    }
}

