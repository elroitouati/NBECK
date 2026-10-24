package com.nbeck.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbeck.app.NbeckApp
import com.nbeck.app.audio.Sfx
import com.nbeck.app.data.Player
import com.nbeck.app.game.Arrow
import com.nbeck.app.game.Category
import com.nbeck.app.game.Cell
import com.nbeck.app.game.Mark
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.components.ArenaBackground
import com.nbeck.app.ui.components.Card
import com.nbeck.app.ui.components.FlipTile
import com.nbeck.app.ui.components.NbButton
import com.nbeck.app.ui.components.TopBar
import com.nbeck.app.ui.theme.Motion
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb
import java.time.LocalDate

private val demoCells = listOf(
    Cell(Mark.GRAY), Cell(Mark.YELLOW), Cell(Mark.YELLOW), Cell(Mark.GRAY, Arrow.DOWN), Cell(Mark.YELLOW, Arrow.UP), Cell(Mark.GREEN),
)

private val demoPlayer = Player(
    id = "demo", name = "Jayson Tatum", firstName = "Jayson", lastName = "Tatum", teamId = 2, position = "F",
    birthDate = "1998-03-03", jersey = "0", country = "USA", headshot = "https://a.espncdn.com/i/headshots/nba/players/full/4065648.png",
)

@Composable
fun HowToScreen(onBack: () -> Unit, onPlay: () -> Unit) {
    val app = LocalContext.current.applicationContext as NbeckApp
    val s = LocalStrings.current
    val c = nb
    val players by app.players.players.collectAsState()
    val example = players.firstOrNull { it.name == demoPlayer.name } ?: demoPlayer

    ArenaBackground(parquet = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(s.howToTitle, onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
                Text(s.howToIntro, style = Type.body.copy(fontSize = 17.sp, lineHeight = 27.sp), color = c.text1)
                Spacer(Modifier.height(Space.xl))
                Card(Modifier.fillMaxWidth()) {
                    Text(s.howToExample, style = Type.label, color = c.text2)
                    Spacer(Modifier.height(Space.s))
                    Text(example.name, style = Type.title.copy(textDirection = TextDirection.Ltr), color = c.text1)
                    Spacer(Modifier.height(Space.m))
                    Row(Modifier.fillMaxWidth()) {
                        Category.entries.forEach { cat ->
                            Text(s.category(cat), style = Type.label.copy(fontSize = 11.sp), color = c.text2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Category.entries.forEachIndexed { i, cat ->
                            FlipTile(
                                cat, demoCells[i], example, LocalDate.now(), animate = true, delayMs = 300 + i * Motion.flipStagger,
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp).aspectRatio(0.9f),
                                onLanded = { app.feedback.play(Sfx.FLIP) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Space.xl))
                Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    LegendLine(Mark.GREEN, s.howToGreen)
                    LegendLine(Mark.YELLOW, s.howToYellow)
                    LegendLine(Mark.GRAY, s.howToGray)
                    IconLine(Icons.Rounded.ArrowUpward, s.howToArrows)
                    IconLine(Icons.Rounded.GridView, s.howToBoard)
                    IconLine(Icons.Rounded.People, s.howToModes)
                }
                Spacer(Modifier.height(Space.xl))
            }
            NbButton(s.letsPlay, Modifier.fillMaxWidth().padding(Space.gutter), onClick = onPlay)
        }
    }
}

@Composable
private fun LegendLine(mark: Mark, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 2.dp)) { Legend(mark, "") }
        Text(text, style = Type.body, color = nb.text1, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun IconLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = nb.accent, modifier = Modifier.padding(top = 2.dp).size(22.dp))
        Spacer(Modifier.width(Space.m))
        Text(text, style = Type.body, color = nb.text1, modifier = Modifier.weight(1f))
    }
}
