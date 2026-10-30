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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nbeck.app.NbeckApp
import com.nbeck.app.game.MAX_GUESSES
import com.nbeck.app.game.Mode
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.components.ArenaBackground
import com.nbeck.app.ui.components.LedText
import com.nbeck.app.ui.components.Wordmark
import com.nbeck.app.ui.components.pressable
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb
import java.time.LocalDate

@Composable
fun HomeScreen(
    onPlay: (Mode) -> Unit,
    onStats: () -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit,
    onHowTo: () -> Unit,
    onArchive: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as NbeckApp
    val s = LocalStrings.current
    val c = nb
    val dailyFlow = remember { app.prefs.game(Mode.DAILY) }
    val daily by dailyFlow.collectAsState(initial = null)
    val today = LocalDate.now().toString()
    val playedToday = daily?.let { it.date == today && it.finished } == true
    val inProgress = daily?.let { it.date == today && !it.finished && it.guessIds.isNotEmpty() } == true

    ArenaBackground {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(horizontal = Space.gutter),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = Space.s), horizontalArrangement = Arrangement.End) {
                RoundIcon(Icons.AutoMirrored.Rounded.HelpOutline, s.howTo, onHowTo)
                Spacer(Modifier.width(Space.s))
                RoundIcon(Icons.Rounded.Settings, s.settings, onSettings)
            }
            Spacer(Modifier.weight(0.55f))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Wordmark(height = 104.dp)
                Spacer(Modifier.height(Space.m))
                Text(s.tagline, style = Type.title, color = c.text2)
            }
            Spacer(Modifier.height(Space.xxxl))

            // Primary action: the daily. Biggest, warmest thing on the screen.
            ModeCard(
                title = s.daily,
                subtitle = when {
                    playedToday -> s.dailyDone
                    else -> s.dailySub
                },
                icon = if (playedToday) Icons.Rounded.Check else Icons.Rounded.CalendarToday,
                primary = true,
                badge = when {
                    playedToday -> if (daily?.won == true) "${daily?.guessIds?.size}/$MAX_GUESSES" else "X/$MAX_GUESSES"
                    inProgress -> "${daily?.guessIds?.size}/$MAX_GUESSES"
                    else -> null
                },
                onClick = { onPlay(Mode.DAILY) },
            )
            if (playedToday) {
                Spacer(Modifier.height(Space.s))
                Countdown()
            }
            Spacer(Modifier.height(Space.m))
            ModeCard(
                title = s.free, subtitle = s.freeSub, icon = Icons.Rounded.Shuffle, primary = false, badge = null,
                onClick = { onPlay(Mode.FREE) },
            )
            Spacer(Modifier.height(Space.m))
            ModeCard(
                title = s.archive, subtitle = s.archiveSub, icon = Icons.Rounded.History, primary = false, badge = null,
                onClick = onArchive,
            )
            Spacer(Modifier.height(Space.xl))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                SmallTile(Icons.Rounded.BarChart, s.stats, Modifier.weight(1f), onStats)
                SmallTile(Icons.Rounded.EmojiEvents, s.achievements, Modifier.weight(1f), onAchievements)
            }
            Spacer(Modifier.weight(1.4f))
        }
        }
    }
}

@Composable
private fun ModeCard(title: String, subtitle: String, icon: ImageVector, primary: Boolean, badge: String?, onClick: () -> Unit) {
    val c = nb
    val shape = RoundedCornerShape(Space.rPanel)
    val bg = if (primary) Brush.linearGradient(listOf(c.accent, c.accent.copy(alpha = 0.82f))) else Brush.linearGradient(listOf(c.surface1, c.surface1))
    val fg = if (primary) c.onAccent else c.text1
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = if (primary) 104.dp else 84.dp)
            .then(
                if (primary) Modifier.shadow(28.dp, shape, ambientColor = c.accent, spotColor = c.accent)
                else Modifier
            )
            .clip(shape)
            .background(bg)
            .then(if (!primary) Modifier.border(1.dp, c.border, shape) else Modifier)
            .pressable(onClick = onClick)
            .padding(horizontal = Space.xl, vertical = Space.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(if (primary) Color(0x26000000) else c.surface2),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = fg, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.width(Space.l))
        Column(Modifier.weight(1f)) {
            Text(title, style = if (primary) Type.h2 else Type.title, color = fg)
            Text(subtitle, style = Type.body.copy(fontSize = Type.label.fontSize * 1.1f), color = fg.copy(alpha = 0.78f))
        }
        if (badge != null) {
            Box(Modifier.clip(RoundedCornerShape(Space.rControl)).background(c.board).padding(horizontal = Space.m, vertical = 6.dp)) {
                LedText(badge, style = Type.led.copy(fontSize = Type.title.fontSize, lineHeight = Type.title.lineHeight))
            }
        }
    }
}

@Composable
private fun SmallTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val c = nb
    val shape = RoundedCornerShape(Space.rCard)
    Row(
        modifier.heightIn(min = 64.dp).clip(shape).background(c.surface1).border(1.dp, c.border, shape)
            .pressable(onClick = onClick).padding(horizontal = Space.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.accent)
        Spacer(Modifier.width(Space.m))
        Text(label, style = Type.bodyStrong, color = c.text1)
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = nb
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(c.surface1).border(1.dp, c.border, CircleShape).pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = c.text1) }
}
