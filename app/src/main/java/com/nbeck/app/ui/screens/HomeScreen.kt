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
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.SwapVert
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
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.nbeck.app.game.Achievement
import com.nbeck.app.game.AchievementState
import com.nbeck.app.game.Stats

@Composable
fun HomeScreen(
    onPlay: (Mode) -> Unit,
    onStats: () -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit,
    onHowTo: () -> Unit,
    onArchive: () -> Unit,
    onChain: () -> Unit = {},
    onHigherLower: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as NbeckApp
    val s = LocalStrings.current
    val c = nb
    val dailyFlow = remember { app.prefs.game(Mode.DAILY) }
    val daily by dailyFlow.collectAsState(initial = null)
    val today = LocalDate.now().toString()
    val playedToday = daily?.let { it.date == today && it.finished } == true
    val inProgress = daily?.let { it.date == today && !it.finished && it.guessIds.isNotEmpty() } == true
    val stats by remember { app.prefs.stats }.collectAsState(initial = Stats())
    val ach by remember { app.prefs.achievements }.collectAsState(initial = AchievementState())
    val bob = rememberInfiniteTransition(label = "logo")
    val lift by bob.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "lift")

    ArenaBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        // Fixed top bar: drawn above the scrolling content so the logo's glow never covers it.
        Row(Modifier.fillMaxWidth().zIndex(1f).padding(horizontal = Space.gutter).padding(top = Space.s), horizontalArrangement = Arrangement.End) {
            RoundIcon(Icons.AutoMirrored.Rounded.HelpOutline, s.howTo, onHowTo)
            Spacer(Modifier.width(Space.s))
            RoundIcon(Icons.Rounded.Settings, s.settings, onSettings)
        }
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(horizontal = Space.gutter),
        ) {
            Spacer(Modifier.height(Space.xl))
            Spacer(Modifier.weight(0.4f))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.graphicsLayer {
                    translationY = -6.dp.toPx() * lift
                    val k = 1f + 0.02f * lift
                    scaleX = k; scaleY = k
                }) { Wordmark(height = 104.dp) }
                Spacer(Modifier.height(Space.m))
                Text(s.tagline, style = Type.title, color = c.text2)
                Spacer(Modifier.height(Space.l))
                Row(Modifier.entrance(0), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    StatChip(Icons.Rounded.LocalFireDepartment, Color(0xFFFF6B3D), stats.daily.liveStreak(Mode.DAILY, LocalDate.now()).toString(), s.homeStreak)
                    StatChip(Icons.Rounded.SportsBasketball, Color(0xFF2FBF71), (stats.daily.wins + stats.free.wins).toString(), s.homeWins)
                    StatChip(Icons.Rounded.MilitaryTech, Color(0xFFFFC72C), "${ach.unlocked.size}/${Achievement.entries.size}", s.homeBadges)
                }
            }
            Spacer(Modifier.height(Space.xxl))

            // Primary action: the daily. Biggest, warmest thing on the screen.
            ModeCard(
                title = s.daily,
                subtitle = when {
                    playedToday -> s.dailyDone
                    else -> s.dailySub
                },
                icon = if (playedToday) Icons.Rounded.Check else Icons.Rounded.CalendarToday,
                primary = true, tint = Color.Unspecified, index = 1,
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
                title = s.free, subtitle = s.freeSub, tint = c.iconFree, index = 2, icon = Icons.Rounded.Shuffle, primary = false, badge = null,
                onClick = { onPlay(Mode.FREE) },
            )
            Spacer(Modifier.height(Space.m))
            ModeCard(
                title = s.chain, subtitle = s.chainSub, tint = c.iconChain, index = 3, icon = Icons.Rounded.Link, primary = false, badge = null,
                onClick = onChain,
            )
            Spacer(Modifier.height(Space.m))
            ModeCard(
                title = s.hl, subtitle = s.hlSub, tint = c.iconHl, index = 4, icon = Icons.Rounded.SwapVert, primary = false, badge = null,
                onClick = onHigherLower,
            )
            Spacer(Modifier.height(Space.m))
            ModeCard(
                title = s.archive, subtitle = s.archiveSub, tint = c.iconArchive, index = 5, icon = Icons.Rounded.History, primary = false, badge = null,
                onClick = onArchive,
            )
            Spacer(Modifier.height(Space.xl))
            Row(Modifier.fillMaxWidth().entrance(6), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                SmallTile(Icons.Rounded.BarChart, s.stats, c.iconStats, Modifier.weight(1f), onStats)
                SmallTile(Icons.Rounded.EmojiEvents, s.achievements, c.iconAch, Modifier.weight(1f), onAchievements)
            }
            Spacer(Modifier.weight(1.4f))
        }
        }
        }
    }
}

@Composable
private fun ModeCard(title: String, subtitle: String, icon: ImageVector, primary: Boolean, badge: String?, tint: Color = Color.Unspecified, index: Int = 0, onClick: () -> Unit) {
    val c = nb
    val shape = RoundedCornerShape(Space.rPanel)
    val bg = if (primary) Brush.linearGradient(listOf(c.accent, c.accent.copy(alpha = 0.82f))) else Brush.linearGradient(listOf(c.surface1, c.surface1))
    val fg = if (primary) c.onAccent else c.text1
    Row(
        Modifier
            .entrance(index)
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
            Modifier.size(52.dp).clip(CircleShape)
                .background(if (primary || tint == Color.Unspecified) Color(0x26000000) else tint.copy(alpha = 0.18f))
                .then(if (!primary && tint != Color.Unspecified) Modifier.border(1.dp, tint.copy(alpha = 0.45f), CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (primary || tint == Color.Unspecified) fg else tint, modifier = Modifier.size(26.dp)) }
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
private fun SmallTile(icon: ImageVector, label: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    val c = nb
    val shape = RoundedCornerShape(Space.rCard)
    Row(
        modifier.heightIn(min = 64.dp).clip(shape).background(c.surface1).border(1.dp, c.border, shape)
            .pressable(onClick = onClick).padding(horizontal = Space.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint)
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

/** Cards slide up and fade in one after another when the home screen opens. */
@Composable
private fun Modifier.entrance(index: Int): Modifier {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60L * index)
        a.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 28.dp.toPx()
    }
}

@Composable
private fun StatChip(icon: ImageVector, tint: Color, value: String, label: String) {
    val c = nb
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(c.surface1).border(1.dp, c.border, RoundedCornerShape(50))
            .padding(horizontal = Space.m, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(value, style = Type.bodyStrong, color = c.text1)
        Spacer(Modifier.width(4.dp))
        Text(label, style = Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = c.text2)
    }
}
