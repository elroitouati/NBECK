package com.nbeck.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbeck.app.NbeckApp
import com.nbeck.app.game.Mode
import com.nbeck.app.game.ModeStats
import com.nbeck.app.game.Stats
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.components.ArenaBackground
import com.nbeck.app.ui.components.ButtonKind
import com.nbeck.app.ui.components.LedText
import com.nbeck.app.ui.components.NbButton
import com.nbeck.app.ui.components.Scoreboard
import com.nbeck.app.ui.components.TopBar
import com.nbeck.app.ui.components.pressable
import com.nbeck.app.ui.theme.Motion
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb
import java.time.LocalDate

@Composable
fun StatsScreen(onBack: () -> Unit, onPlay: (Mode) -> Unit) {
    val app = LocalContext.current.applicationContext as NbeckApp
    val s = LocalStrings.current
    val c = nb
    val stats by remember { app.prefs.stats }.collectAsState(initial = Stats())
    var mode by rememberSaveable { mutableStateOf(Mode.DAILY) }
    val m = stats[mode]

    ArenaBackground(parquet = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(s.stats, onBack)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
                Segmented(listOf(s.daily, s.free), if (mode == Mode.DAILY) 0 else 1) { mode = if (it == 0) Mode.DAILY else Mode.FREE }
                Spacer(Modifier.height(Space.l))
                // Scoreboard hero: the numbers people came for.
                Scoreboard(Modifier.fillMaxWidth().heightIn(min = 96.dp)) {
                    Big(m.played.toString(), s.played, Modifier.weight(1f))
                    Big("${m.winPct}", s.winPct, Modifier.weight(1f))
                    Big(m.liveStreak(mode, LocalDate.now()).toString(), s.streak, Modifier.weight(1f))
                    Big(m.maxStreak.toString(), s.maxStreak, Modifier.weight(1f))
                }
                Spacer(Modifier.height(Space.m))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(Space.rCard)).background(c.surface1).border(1.dp, c.border, RoundedCornerShape(Space.rCard)).padding(Space.l),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s.avgGuesses, style = Type.bodyStrong, color = c.text1, modifier = Modifier.weight(1f))
                    Text(if (m.wins == 0) "—" else "%.1f".format(m.avgGuesses), style = Type.h2, color = c.text1)
                }
                Spacer(Modifier.height(Space.xl))
                Text(s.distribution, style = Type.title, color = c.text1)
                Spacer(Modifier.height(Space.m))
                if (m.played == 0) {
                    Column(Modifier.fillMaxWidth().padding(vertical = Space.xl), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.noGamesYet, style = Type.body, color = c.text2)
                        Spacer(Modifier.height(Space.m))
                        NbButton(s.playFirst, kind = ButtonKind.SECONDARY) { onPlay(mode) }
                    }
                } else Distribution(m)
                Spacer(Modifier.height(Space.xxl))
            }
        }
    }
}

@Composable
private fun Big(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LedText(value, style = Type.led.copy(fontSize = 32.sp, lineHeight = 34.sp))
        Text(label, style = Type.label.copy(fontSize = 11.sp), color = androidx.compose.ui.graphics.Color(0xFF8C93A1), textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun Distribution(m: ModeStats) {
    val c = nb
    val max = (m.dist.maxOrNull() ?: 0).coerceAtLeast(1)
    val best = m.dist.indices.maxByOrNull { m.dist[it] }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        m.dist.forEachIndexed { i, v ->
            val frac by animateFloatAsState(v.toFloat() / max, tween(Motion.page + 200, delayMillis = i * 40, easing = Motion.easeOut), label = "bar")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", style = Type.bodyStrong, color = c.text2, modifier = Modifier.width(20.dp))
                Box(Modifier.weight(1f).height(28.dp)) {
                    Box(
                        Modifier.fillMaxHeight().fillMaxWidth(frac.coerceAtLeast(0.001f)).widthIn(min = 32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (i == best && v > 0) c.green else c.surface2),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text("$v", style = Type.label, color = if (i == best && v > 0) c.onGreen else c.text1, modifier = Modifier.padding(horizontal = Space.s))
                    }
                }
            }
        }
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = nb
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Space.rControl)).background(c.surface2).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(Space.rControl - 4.dp))
                    .background(if (on) c.surface1 else androidx.compose.ui.graphics.Color.Transparent)
                    .then(if (on) Modifier.border(1.dp, c.border, RoundedCornerShape(Space.rControl - 4.dp)) else Modifier)
                    .pressable(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Text(label, style = Type.bodyStrong, color = if (on) c.text1 else c.text2) }
        }
    }
}
