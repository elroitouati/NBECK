package com.nbeck.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbeck.app.data.Countries
import com.nbeck.app.data.Player
import com.nbeck.app.game.Daily
import com.nbeck.app.game.Mode
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.components.ButtonKind
import com.nbeck.app.ui.components.Confetti
import com.nbeck.app.ui.components.IconBox
import com.nbeck.app.ui.components.JerseyShape
import com.nbeck.app.ui.components.LedText
import com.nbeck.app.ui.components.MiniSquare
import com.nbeck.app.ui.components.NbButton
import com.nbeck.app.ui.components.PlayerPhoto
import com.nbeck.app.ui.components.Scoreboard
import com.nbeck.app.ui.theme.Bebas
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun EndScreen(ui: GameUi, onClose: () -> Unit, onPlayAgain: () -> Unit, onHome: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    val answer = ui.answer ?: return
    val team = answer.team
    Box(
        Modifier.fillMaxSize().background(
            if (ui.won) Brush.verticalGradient(listOf(team.color.copy(alpha = 0.92f), c.bg.copy(alpha = 0.98f), c.bg))
            else Brush.verticalGradient(listOf(c.bg.copy(alpha = 0.97f), c.bg))
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                IconBox(onClick = onClose, label = s.close) { Icon(Icons.Rounded.Close, s.close, tint = if (ui.won) Color(0xFFF7F7F7) else c.text1) }
            }
            Text(if (ui.won) s.win else if (ui.gaveUp) s.gaveUp else s.loss, style = Type.h1.copy(fontSize = 40.sp, lineHeight = 46.sp), color = if (ui.won) Color(0xFFF7F7F7) else c.text1)
            Text(
                if (ui.won) s.winSub(ui.guessCount) else s.lossSub,
                style = Type.title, color = if (ui.won) Color(0xE6F7F7F7) else c.text2,
            )
            Spacer(Modifier.height(Space.l))
            // The card is an English artifact (names, team, numbers): laid out LTR in both languages.
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                PlayerCard(answer, ui.date)
            }
            Spacer(Modifier.height(Space.xl))
            ResultGrid(ui)
            Spacer(Modifier.height(Space.xl))
            if (ui.mode == Mode.FREE) {
                NbButton(s.playAgain, Modifier.fillMaxWidth(), onClick = onPlayAgain)
            } else {
                Countdown()
            }
            Spacer(Modifier.height(Space.s))
            NbButton(s.backHome, Modifier.fillMaxWidth(), kind = ButtonKind.GHOST, onClick = onHome)
            Spacer(Modifier.height(Space.xxl))
        }
        if (ui.won && ui.endedLive) {
            Confetti(listOf(team.color, team.accent, Color(0xFFF7F7F7)), key = ui.gameKey)
        }
    }
}

/** Collectible trading card in the player's team colors. */
@Composable
fun PlayerCard(p: Player, date: LocalDate) {
    val c = nb
    val s = LocalStrings.current
    val t = p.team
    val shape = RoundedCornerShape(Space.rPanel)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(t.color, t.color.copy(alpha = 0.85f).compositeOn(Color(0xFF0B0D12)))))
            .border(3.dp, t.accent, shape)
            .padding(Space.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t.fullName.uppercase(), style = Type.label.copy(fontFamily = Bebas, fontSize = 18.sp, letterSpacing = 1.5.sp, textDirection = TextDirection.Ltr), color = Color(0xD9F7F7F7), modifier = Modifier.weight(1f))
            coil.compose.AsyncImage(t.logoUrl, null, Modifier.size(36.dp))
        }
        Box(
            Modifier.fillMaxWidth().height(210.dp).padding(vertical = Space.s)
                .clip(RoundedCornerShape(Space.rCard))
                .background(Brush.radialGradient(listOf(Color(0x55FFFFFF), Color.Transparent))),
            contentAlignment = Alignment.BottomCenter,
        ) {
            PlayerPhoto(p, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            Box(
                Modifier.align(Alignment.TopStart).size(width = 58.dp, height = 54.dp).clip(JerseyShape).background(t.accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(p.jersey ?: "—", style = Type.display.copy(fontSize = 28.sp, lineHeight = 28.sp, textDirection = TextDirection.Ltr), color = t.color, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Text(p.name, style = Type.display.copy(fontSize = 40.sp, lineHeight = 42.sp, textDirection = TextDirection.Ltr), color = Color(0xFFF7F7F7), maxLines = 2)
        Spacer(Modifier.height(Space.s))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Fact(s.pos, p.position, Modifier.weight(1f))
            Fact(s.age, p.ageOn(date).toString(), Modifier.weight(1f))
            Fact(s.country, listOfNotNull(Countries.flag(p.country), Countries.short(p.country)).joinToString(" "), Modifier.weight(1f))
        }
        Spacer(Modifier.height(Space.s))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Fact(s.draft, p.draftYear?.toString() ?: s.undrafted, Modifier.weight(1f))
            Fact(s.ring, if (p.ring) "✓ ${s.champion}" else "✗ ${s.noTitle}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(Space.s))
        Scoreboard(Modifier.fillMaxWidth()) {
            if (p.ppg == null) {
                Text(s.noSeasonStats, style = Type.label, color = Color(0xFF8C93A1), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            } else {
                StatCell(s.ppg, p.ppg, Modifier.weight(1f))
                StatCell(s.rpg, p.rpg, Modifier.weight(1f))
                StatCell(s.apg, p.apg, Modifier.weight(1f))
            }
        }
    }
}

private fun Color.compositeOn(bg: Color): Color = Color(
    red = red * alpha + bg.red * (1 - alpha), green = green * alpha + bg.green * (1 - alpha), blue = blue * alpha + bg.blue * (1 - alpha), alpha = 1f,
)

@Composable
private fun Fact(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(Space.rControl)).background(Color(0x33000000)).padding(vertical = Space.s), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = Type.label.copy(fontSize = 11.sp), color = Color(0xB3F7F7F7))
        Text(value, style = Type.title.copy(textDirection = TextDirection.Ltr), color = Color(0xFFF7F7F7), maxLines = 1)
    }
}

@Composable
private fun StatCell(label: String, v: Double?, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LedText(v?.let { "%.1f".format(it) } ?: "—", style = Type.led.copy(fontSize = 26.sp, lineHeight = 28.sp))
        Text(label, style = Type.label.copy(fontSize = 11.sp), color = Color(0xFF8C93A1))
    }
}

@Composable
private fun ResultGrid(ui: GameUi) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ui.results.forEach { r ->
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { r.cells.forEach { MiniSquare(it.mark) } }
            }
        }
    }
}

@Composable
fun Countdown(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    var left by remember { mutableStateOf(Daily.untilNext(LocalDateTime.now())) }
    LaunchedEffect(Unit) {
        while (true) {
            left = Daily.untilNext(LocalDateTime.now())
            delay(1000L - System.currentTimeMillis() % 1000L)
        }
    }
    Scoreboard(modifier.fillMaxWidth()) {
        Text(s.nextPlayerIn, style = Type.label, color = Color(0xFF8C93A1), modifier = Modifier.weight(1f))
        val sec = left.seconds
        LedText("%02d:%02d:%02d".format(sec / 3600, (sec % 3600) / 60, sec % 60), style = Type.led.copy(fontSize = 26.sp, lineHeight = 28.sp))
    }
}
