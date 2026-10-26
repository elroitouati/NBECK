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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbeck.app.NbeckApp
import com.nbeck.app.data.SavedGame
import com.nbeck.app.game.MAX_GUESSES
import com.nbeck.app.game.Mark
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.components.ArenaBackground
import com.nbeck.app.ui.components.LedText
import com.nbeck.app.ui.components.TopBar
import com.nbeck.app.ui.components.pressable
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb
import java.time.LocalDate
import java.time.format.TextStyle

/** How far back the daily archive goes. */
const val ARCHIVE_DAYS = 30L

/** The daily challenge of each of the last 30 days, today first. */
@Composable
fun ArchiveScreen(onBack: () -> Unit, onPlay: (LocalDate) -> Unit) {
    val app = LocalContext.current.applicationContext as NbeckApp
    val s = LocalStrings.current
    val games by remember { app.prefs.dailyGames }.collectAsState(initial = emptyMap())
    val today = LocalDate.now()
    val days = remember(today) { (0..ARCHIVE_DAYS).map { today.minusDays(it) } }

    ArenaBackground(parquet = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(s.archive, onBack)
            Text(s.archiveSub, style = Type.body, color = nb.text2, modifier = Modifier.padding(horizontal = Space.gutter))
            LazyColumn(
                Modifier.padding(horizontal = Space.gutter),
                verticalArrangement = Arrangement.spacedBy(Space.s),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Space.m),
            ) {
                items(days, key = { it.toString() }) { d -> DayRow(d, d == today, games[d.toString()]) { onPlay(d) } }
            }
        }
    }
}

@Composable
private fun DayRow(date: LocalDate, isToday: Boolean, game: SavedGame?, onClick: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    val shape = RoundedCornerShape(Space.rCard)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape).background(c.surface1).border(1.dp, c.border, shape)
            .pressable(onClick = onClick).padding(horizontal = Space.l, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LedText("%02d.%02d".format(date.dayOfMonth, date.monthValue), style = Type.led.copy(fontSize = 22.sp, lineHeight = 24.sp))
        Spacer(Modifier.width(Space.l))
        Column(Modifier.weight(1f)) {
            Text(
                if (isToday) s.today else date.dayOfWeek.getDisplayName(TextStyle.FULL, s.locale),
                style = Type.bodyStrong, color = c.text1,
            )
            if (game == null || game.guessIds.isEmpty() && !game.finished) {
                Text(s.notPlayed, style = Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = c.text2)
            }
        }
        Status(game)
    }
}

@Composable
private fun Status(game: SavedGame?) {
    val c = nb
    val s = LocalStrings.current
    val shape = RoundedCornerShape(Space.rControl)
    val n = game?.guessIds?.size ?: 0
    val (label, bg, fg) = when {
        game == null || (n == 0 && !game.finished) -> Triple(s.play, c.accent, c.onAccent)
        game.finished && game.won -> Triple("$n/$MAX_GUESSES", c.tile(Mark.GREEN), c.onTile(Mark.GREEN))
        game.finished -> Triple("X/$MAX_GUESSES", c.tile(Mark.GRAY), c.onTile(Mark.GRAY))
        else -> Triple("$n/$MAX_GUESSES", c.surface2, c.text1)
    }
    Box(Modifier.clip(shape).background(bg).padding(horizontal = Space.m, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(label, style = Type.label.copy(fontSize = 14.sp), color = fg)
    }
}
