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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Elderly
import androidx.compose.material.icons.rounded.FiberNew
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LooksTwo
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nbeck.app.NbeckApp
import com.nbeck.app.game.Achievement
import com.nbeck.app.game.AchievementState
import com.nbeck.app.game.Stats
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.components.ArenaBackground
import com.nbeck.app.ui.components.LedText
import com.nbeck.app.ui.components.TopBar
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb

fun Achievement.icon(): ImageVector = when (this) {
    Achievement.FIRST_WIN -> Icons.Rounded.SportsBasketball
    Achievement.FIRST_TRY -> Icons.Rounded.GpsFixed
    Achievement.THREE_POINTER -> Icons.Rounded.Bolt
    Achievement.STREAK_3, Achievement.STREAK_7, Achievement.STREAK_30 -> Icons.Rounded.LocalFireDepartment
    Achievement.NO_YELLOW -> Icons.Rounded.WorkspacePremium
    Achievement.BUZZER_BEATER -> Icons.Rounded.Timer
    Achievement.WINS_10, Achievement.WINS_50, Achievement.WINS_100 -> Icons.Rounded.EmojiEvents
    Achievement.ALL_TEAMS -> Icons.Rounded.Flag
    Achievement.GLOBETROTTER -> Icons.Rounded.Public
    Achievement.SECOND_TRY -> Icons.Rounded.LooksTwo
    Achievement.HOT_START -> Icons.Rounded.RocketLaunch
    Achievement.FREE_STREAK_5 -> Icons.Rounded.Whatshot
    Achievement.FREE_WINS_25 -> Icons.Rounded.Shuffle
    Achievement.HISTORIAN -> Icons.Rounded.History
    Achievement.BLUE_WHITE -> Icons.Rounded.Star
    Achievement.INTERNATIONAL_10 -> Icons.Rounded.FlightTakeoff
    Achievement.ROOKIE_HUNTER -> Icons.Rounded.FiberNew
    Achievement.OLD_SCHOOL -> Icons.Rounded.Elderly
    Achievement.RING_HUNTER -> Icons.Rounded.Diamond
    Achievement.UNDRAFTED -> Icons.AutoMirrored.Rounded.TrendingUp
    Achievement.NIGHT_OWL -> Icons.Rounded.Bedtime
}

@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as NbeckApp
    val s = LocalStrings.current
    val c = nb
    val state by remember { app.prefs.achievements }.collectAsState(initial = AchievementState())
    val stats by remember { app.prefs.stats }.collectAsState(initial = Stats())
    val unlocked = Achievement.entries.count { state.isUnlocked(it) }

    ArenaBackground(parquet = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(s.achievements, onBack) {
                LedText("$unlocked/${Achievement.entries.size}", style = Type.led.copy(fontSize = Type.title.fontSize), modifier = Modifier.padding(horizontal = Space.m))
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(horizontal = Space.gutter),
                horizontalArrangement = Arrangement.spacedBy(Space.m),
                verticalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                items(Achievement.entries.sortedByDescending { state.isUnlocked(it) }, key = { it.name }) { a ->
                    Badge(a, state.isUnlocked(a), state.progress(a, stats))
                }
                item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(Space.xl)) }
            }
        }
    }
}

@Composable
private fun Badge(a: Achievement, unlocked: Boolean, progress: Int) {
    val c = nb
    val s = LocalStrings.current
    val shape = RoundedCornerShape(Space.rCard)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface1)
            .border(1.dp, if (unlocked) c.accent.copy(alpha = 0.6f) else c.border, shape)
            .padding(Space.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(if (unlocked) c.accent else c.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (unlocked) a.icon() else Icons.Rounded.Lock, contentDescription = if (unlocked) null else s.locked, tint = if (unlocked) c.onAccent else c.text3, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(Space.s))
        Text(s.achievementTitle(a), style = Type.bodyStrong, color = c.text1, textAlign = TextAlign.Center)
        Text(s.achievementDesc(a), style = Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = c.text2, textAlign = TextAlign.Center, minLines = 2)
        if (!unlocked && a.target > 1) {
            Spacer(Modifier.height(Space.s))
            LinearProgressIndicator(
                progress = { progress / a.target.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = c.accent, trackColor = c.surface2, drawStopIndicator = {},
            )
            Text("$progress/${a.target}", style = Type.label.copy(fontSize = Type.label.fontSize * 0.9f), color = c.text3, modifier = Modifier.alpha(0.9f).padding(top = 4.dp))
        }
    }
}

