package com.nbeck.app.ui.screens

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nbeck.app.NbeckApp
import com.nbeck.app.audio.Sfx
import com.nbeck.app.data.Player
import com.nbeck.app.game.Category
import com.nbeck.app.game.DivisionBoard
import com.nbeck.app.game.GuessResult
import com.nbeck.app.game.MAX_GUESSES
import com.nbeck.app.game.Mark
import com.nbeck.app.game.Mode
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.components.ArenaBackground
import com.nbeck.app.ui.components.BlankTile
import com.nbeck.app.ui.components.FlipTile
import com.nbeck.app.ui.components.IconBox
import com.nbeck.app.ui.components.LedText
import com.nbeck.app.ui.components.PlayerPhoto
import com.nbeck.app.ui.components.Scoreboard
import com.nbeck.app.ui.components.TopBar
import com.nbeck.app.ui.components.pressable
import com.nbeck.app.ui.theme.Motion
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Total time for one row to finish flipping. */
private val ROW_FLIP_MS = Motion.flipStagger * (Category.entries.size - 1) + Motion.flip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameScreen(mode: Mode, onBack: () -> Unit, onHome: () -> Unit, date: java.time.LocalDate = java.time.LocalDate.now()) {
    val context = LocalContext.current
    val app = context.applicationContext as NbeckApp
    val vm: GameViewModel = viewModel(key = "${mode.name}-$date", factory = GameViewModel.Factory(context.applicationContext as Application, mode, date))
    val ui by vm.ui.collectAsState()
    val s = LocalStrings.current
    val c = nb

    var boardExpanded by rememberSaveable { mutableStateOf(true) }
    // Collapse the board only while the keyboard is actually on screen (the field can keep focus
    // after the keyboard is dismissed with Back, which used to lock the board closed).
    val imeVisible = WindowInsets.isImeVisible
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var showEnd by remember { mutableStateOf(false) }
    val reveal = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // End-of-game choreography: wait for the last row to flip, sound, reveal silhouette, then the end sheet.
    LaunchedEffect(ui.finished, ui.gameKey) {
        if (!ui.finished) { showEnd = false; reveal.snapTo(0f); return@LaunchedEffect }
        if (ui.endedLive) {
            if (!ui.gaveUp) delay(ROW_FLIP_MS.toLong() + 120)
            app.feedback.play(if (ui.won) Sfx.WIN else Sfx.LOSE)
            if (ui.won) app.feedback.heavy()
            reveal.animateTo(1f, tween(700, easing = Motion.easeOut))
            delay(250)
        } else reveal.snapTo(1f)
        showEnd = true
    }

    // Once a row has flipped, mark it done so it never flips again — opening/closing the keyboard
    // re-lays out the list and used to replay the whole reveal.
    LaunchedEffect(ui.animateRow, ui.gameKey) {
        val row = ui.animateRow
        if (row >= 0) {
            delay(ROW_FLIP_MS.toLong() + 100)
            vm.rowAnimationDone(row)
        }
    }

    var confirmGiveUp by remember { mutableStateOf(false) }
    if (confirmGiveUp) {
        GiveUpDialog(onConfirm = { confirmGiveUp = false; vm.giveUp() }, onDismiss = { confirmGiveUp = false })
    }

    val boardStates = remember(ui.results) { DivisionBoard.states(ui.pairs) }

    ArenaBackground(parquet = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            TopBar(
                title = when {
                    mode == Mode.FREE -> s.free
                    date == java.time.LocalDate.now() -> s.daily
                    else -> "${s.daily} · %02d.%02d".format(date.dayOfMonth, date.monthValue)
                },
                onBack = onBack,
            ) {
                if (!ui.finished && !ui.loading) {
                    GiveUpButton { keyboard?.hide(); confirmGiveUp = true }
                }
                if (ui.finished && !showEnd) {
                    IconBox(onClick = { showEnd = true }, label = s.win) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = c.accent)
                    }
                }
            }
            Header(ui.answer, reveal.value, ui.guessCount, ui.finished)
            Spacer(Modifier.height(Space.s))
            GuessTable(ui, Modifier.weight(1f), onFlip = { mark ->
                app.feedback.play(Sfx.FLIP)
                if (mark == Mark.GREEN) app.feedback.tick()
            })
            DivisionBoardView(
                states = boardStates,
                expanded = boardExpanded && !imeVisible,
                onToggle = {
                    if (imeVisible) {
                        keyboard?.hide()
                        focusManager.clearFocus()
                        boardExpanded = true
                    } else {
                        boardExpanded = !boardExpanded
                    }
                },
            )
            if (!ui.finished) {
                SearchBar(
                    query = ui.query,
                    suggestions = ui.suggestions,
                    onQuery = vm::onQuery,
                    onPick = vm::guess,
                )
            }
        }
        AnimatedVisibility(
            visible = showEnd && ui.answer != null,
            enter = slideInVertically(tween(Motion.page + 80, easing = Motion.drawer)) { it / 3 } + fadeIn(tween(Motion.small)),
            exit = slideOutVertically(tween(Motion.small, easing = Motion.easeOut)) { it / 3 } + fadeOut(tween(Motion.press)),
        ) {
            EndScreen(
                ui = ui,
                onClose = { showEnd = false },
                onPlayAgain = { scope.launch { showEnd = false; vm.newGame() } },
                onHome = onHome,
            )
        }
    }
}

@Composable
private fun Header(answer: Player?, reveal: Float, count: Int, finished: Boolean) {
    val c = nb
    val s = LocalStrings.current
    Row(Modifier.fillMaxWidth().padding(horizontal = Space.gutter), verticalAlignment = Alignment.CenterVertically) {
        // The mystery player under a spotlight.
        Box(
            Modifier
                .size(width = 112.dp, height = 84.dp)
                .clip(RoundedCornerShape(Space.rCard))
                .background(Brush.radialGradient(listOf(c.spotlight.copy(alpha = 0.55f), Color.Transparent)))
                .border(1.dp, c.border, RoundedCornerShape(Space.rCard)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Until the game ends, a generic figure — the real headshot's outline would be a hint.
            if (finished) {
                PlayerPhoto(answer, Modifier.fillMaxSize().padding(top = 4.dp).alpha(reveal))
            } else {
                androidx.compose.foundation.Image(
                    androidx.compose.ui.res.painterResource(com.nbeck.app.R.drawable.ic_silhouette), null,
                    Modifier.fillMaxSize().padding(top = 10.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(if (c.isDark) Color(0xFF2A303B) else Color(0xFFC9CDD4)),
                )
                Text("?", style = Type.display.copy(fontSize = 44.sp, lineHeight = 44.sp), color = c.led, modifier = Modifier.align(Alignment.Center).padding(bottom = 8.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Scoreboard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.guessOf, style = Type.label.copy(fontSize = 11.sp), color = Color(0xFF8C93A1))
                LedText("$count/$MAX_GUESSES", style = Type.led.copy(fontSize = 34.sp, lineHeight = 36.sp))
            }
        }
    }
}

@Composable
private fun GuessTable(ui: GameUi, modifier: Modifier, onFlip: (Mark) -> Unit) {
    val c = nb
    val s = LocalStrings.current
    val listState = rememberLazyListState()
    LaunchedEffect(ui.guessCount) { if (ui.guessCount > 0) listState.animateScrollToItem(ui.guessCount - 1) }
    Column(modifier.fillMaxWidth().padding(horizontal = Space.s)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(NAME_COL))
            Category.entries.forEach { cat ->
                Text(
                    s.category(cat), style = Type.label.copy(fontSize = 10.sp), color = c.text2, maxLines = 1,
                    overflow = TextOverflow.Clip, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        if (ui.guesses.isEmpty() && !ui.loading) {
            EmptyHint(Modifier.weight(1f))
        } else {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                itemsIndexed(ui.guesses, key = { _, p -> "${ui.gameKey}-${p.id}" }) { i, p ->
                    GuessRow(p, ui.results[i], ui, animate = i == ui.animateRow, onFlip = onFlip)
                }
                if (!ui.finished) item(key = "next-${ui.gameKey}") { GhostRow(ui.guessCount + 1) }
            }
        }
    }
}

private val NAME_COL = 50.dp

@Composable
private fun GuessRow(p: Player, r: GuessResult, ui: GameUi, animate: Boolean, onFlip: (Mark) -> Unit) {
    val c = nb
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(NAME_COL).padding(end = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(c.surface2)) {
                PlayerPhoto(p, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            }
            Text(
                p.displayLast, style = Type.label.copy(fontSize = 10.sp, lineHeight = 12.sp, textDirection = TextDirection.Ltr),
                color = c.text1, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Category.entries.forEachIndexed { i, cat ->
            FlipTile(
                category = cat,
                cell = r.cells[i],
                guess = p,
                date = ui.date,
                animate = animate,
                delayMs = i * Motion.flipStagger,
                modifier = Modifier.weight(1f).padding(horizontal = 1.5.dp).aspectRatio(0.82f),
                onLanded = onFlip,
            )
        }
    }
}

@Composable
private fun GhostRow(n: Int) {
    val c = nb
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(NAME_COL), contentAlignment = Alignment.Center) {
            LedText("$n", style = Type.led.copy(fontSize = 18.sp, lineHeight = 20.sp), color = c.text3)
        }
        Category.entries.forEach { cat ->
            BlankTile(cat, Modifier.weight(1f).padding(horizontal = 1.5.dp).aspectRatio(0.82f))
        }
    }
}

@Composable
private fun EmptyHint(modifier: Modifier) {
    val c = nb
    val s = LocalStrings.current
    Column(modifier.fillMaxWidth().padding(Space.l), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(s.howToIntro, style = Type.body, color = c.text2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(Space.l))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.l)) {
            Legend(Mark.GREEN, s.legendExact); Legend(Mark.YELLOW, s.legendClose); Legend(Mark.GRAY, s.legendFar)
        }
    }
}

@Composable
fun Legend(mark: Mark, label: String) {
    val c = nb
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(c.tile(mark)))
        Spacer(Modifier.width(6.dp))
        Text(label, style = Type.label, color = c.text1)
    }
}

@Composable
private fun SearchBar(
    query: String,
    suggestions: List<Player>,
    onQuery: (String) -> Unit,
    onPick: (Player) -> Unit,
) {
    val c = nb
    val s = LocalStrings.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Column(Modifier.fillMaxWidth().background(c.surface1)) {
        // Suggestions open upward, right above the field and the keyboard.
        AnimatedVisibility(
            visible = query.isNotBlank(),
            enter = fadeIn(tween(Motion.press)) + slideInVertically(tween(Motion.small, easing = Motion.easeOut)) { it / 6 },
            exit = fadeOut(tween(Motion.press)),
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 300.dp).padding(top = Space.xs)) {
                if (suggestions.isEmpty()) {
                    Text(s.noMatches, style = Type.body, color = c.text2, modifier = Modifier.padding(Space.l))
                } else {
                    // Every new query starts at the top of the list — the list used to keep the scroll
                    // position from the previous search, hiding the best (first) matches.
                    val listState = rememberLazyListState()
                    LaunchedEffect(suggestions) { listState.scrollToItem(0) }
                    LazyColumn(state = listState) {
                        itemsIndexed(suggestions, key = { _, p -> p.id }) { _, p ->
                            SuggestionRow(p) {
                                onPick(p)
                                keyboard?.hide()
                                focusManager.clearFocus()
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.m, vertical = Space.s)
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(Space.rControl))
                .background(c.surface2)
                .border(1.dp, if (query.isNotEmpty()) c.accent else c.borderStrong, RoundedCornerShape(Space.rControl))
                .pressable { focus.requestFocus(); keyboard?.show() }
                .padding(horizontal = Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = c.text2)
            Spacer(Modifier.width(Space.s))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text(s.searchHint, style = Type.body, color = c.text3)
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = Type.body.copy(color = c.text1, textDirection = TextDirection.Ltr),
                    cursorBrush = SolidColor(c.accent),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, autoCorrectEnabled = false, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (suggestions.size == 1) onPick(suggestions.first()) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            if (query.isNotEmpty()) {
                IconBox(onClick = { onQuery("") }, label = s.close, size = 40.dp) { Icon(Icons.Rounded.Close, null, tint = c.text2) }
            }
        }
    }
}

@Composable
private fun SuggestionRow(p: Player, onClick: () -> Unit) {
    val c = nb
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).pressable(onClick = onClick).padding(horizontal = Space.l, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.surface2)) {
            PlayerPhoto(p, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        }
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = Type.bodyStrong.copy(textDirection = TextDirection.Ltr), color = c.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(p.team.fullName, style = Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, textDirection = TextDirection.Ltr), color = c.text2, maxLines = 1)
        }
        // Team only — the jersey number is a category, so showing it here would give it away.
        Text(p.team.abbr, style = Type.label.copy(textDirection = TextDirection.Ltr), color = c.text2)
    }
}

@Composable
private fun GiveUpButton(onClick: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    Row(
        Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(Space.rControl)).border(1.dp, c.borderStrong, RoundedCornerShape(Space.rControl))
            .pressable(onClick = onClick).padding(horizontal = Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Flag, null, tint = c.text2, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(s.giveUp, style = Type.label.copy(fontSize = 14.sp), color = c.text1)
    }
    Spacer(Modifier.width(Space.s))
}

@Composable
private fun GiveUpDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface1,
        title = { Text(s.giveUpTitle, style = Type.title, color = c.text1) },
        text = { Text(s.giveUpBody, style = Type.body, color = c.text2) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text(s.giveUp, style = Type.bodyStrong, color = Color(0xFFE5484D))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(s.keepPlaying, style = Type.bodyStrong, color = c.text1)
            }
        },
    )
}
