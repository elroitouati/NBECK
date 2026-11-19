package com.nbeck.app.ui.screens

import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
import com.nbeck.app.ui.components.ColorPickerDialog
import com.nbeck.app.ui.components.LedText
import com.nbeck.app.ui.components.NbButton
import com.nbeck.app.ui.components.Scoreboard
import com.nbeck.app.ui.components.TopBar
import com.nbeck.app.ui.components.Wordmark
import com.nbeck.app.ui.components.pressable
import com.nbeck.app.ui.theme.NbColors
import com.nbeck.app.ui.theme.NbeckColors
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.customColors
import com.nbeck.app.ui.theme.nb
import kotlinx.coroutines.launch

/** Which color a row edits. [auto] = it can be reset to the automatic value. */
private class Field(val label: String, val current: (NbColors) -> Color, val explicit: Boolean, val auto: Boolean, val set: (Long?) -> CustomTheme)

/**
 * Designs the one custom theme. The whole screen is drawn in the draft colors, so every change is
 * previewed live. Every color opens a picker with a color wheel and a palette; "Save" stores it as
 * the Custom theme and switches to it.
 */
@Composable
fun ThemeEditorScreen(onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as NbeckApp
    val settings by remember { app.prefs.settings }.collectAsState(initial = Settings())
    var draft by remember { mutableStateOf<CustomTheme?>(null) }
    val d = draft ?: settings.custom ?: CustomTheme()
    var editing by remember { mutableStateOf<Field?>(null) }
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val colors = customColors(d)

    NbeckColors(colors) {
        val c = nb
        ArenaBackground {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                TopBar(s.themeEditor, onDone)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
                    Text(s.themeHint, style = Type.body, color = c.text2)
                    Spacer(Modifier.height(Space.l))
                    Preview()

                    PresetRows(s.tPresetsTeams, TeamPresets) { draft = it }
                    PresetRows(s.tPresets, IdeaPresets) { draft = it }
                    Group(s.tGroupAtmosphere) {
                        ColorRow(Field(s.tBackground, { it.bg }, true, false) { d.copy(bg = it!!) }) { editing = it }
                        ColorRow(Field(s.tGlow, { it.spotlight.copy(alpha = 1f) }, d.glow != null, true) { d.copy(glow = it) }) { editing = it }
                        ToggleRow(s.tParquet, d.parquet) { draft = d.copy(parquet = it) }
                        if (d.parquet) ColorRow(Field(s.tWood, { it.woodA }, d.wood != null, true) { d.copy(wood = it) }) { editing = it }
                    }
                    Group(s.tGroupCards) {
                        ColorRow(Field(s.tCards, { it.surface1 }, true, false) { d.copy(surface = it!!) }) { editing = it }
                        ColorRow(Field(s.tBorder, { it.borderStrong }, d.border != null, true) { d.copy(border = it) }) { editing = it }
                    }
                    Group(s.tGroupText) {
                        ColorRow(Field(s.tText1, { it.text1 }, d.text1 != null, true) { d.copy(text1 = it) }) { editing = it }
                        ColorRow(Field(s.tText2, { it.text2.copy(alpha = 1f) }, d.text2 != null, true) { d.copy(text2 = it) }) { editing = it }
                    }
                    Group(s.tGroupButtons) {
                        ColorRow(Field(s.tAccent, { it.accent }, true, false) { d.copy(accent = it!!) }) { editing = it }
                        ColorRow(Field(s.tOnAccent, { it.onAccent }, d.onAccent != null, true) { d.copy(onAccent = it) }) { editing = it }
                    }
                    Group(s.tGroupBoard) {
                        ColorRow(Field(s.tBoard, { it.board }, d.board != null, true) { d.copy(board = it) }) { editing = it }
                        ColorRow(Field(s.tLed, { it.led }, true, false) { d.copy(led = it!!) }) { editing = it }
                    }
                    Group(s.tGroupTiles) {
                        ColorRow(Field(s.tGreen, { it.green }, d.green != null, true) { d.copy(green = it) }) { editing = it }
                        ColorRow(Field(s.tYellow, { it.yellow }, d.yellow != null, true) { d.copy(yellow = it) }) { editing = it }
                        ColorRow(Field(s.tGray, { it.gray }, d.gray != null, true) { d.copy(gray = it) }) { editing = it }
                    }
                    Group(s.tGroupIcons) {
                        ColorRow(Field(s.free, { it.iconFree }, d.iconFree != null, true) { d.copy(iconFree = it) }) { editing = it }
                        ColorRow(Field(s.chain, { it.iconChain }, d.iconChain != null, true) { d.copy(iconChain = it) }) { editing = it }
                        ColorRow(Field(s.hl, { it.iconHl }, d.iconHl != null, true) { d.copy(iconHl = it) }) { editing = it }
                        ColorRow(Field(s.archive, { it.iconArchive }, d.iconArchive != null, true) { d.copy(iconArchive = it) }) { editing = it }
                        ColorRow(Field(s.stats, { it.iconStats }, d.iconStats != null, true) { d.copy(iconStats = it) }) { editing = it }
                        ColorRow(Field(s.achievements, { it.iconAch }, d.iconAch != null, true) { d.copy(iconAch = it) }) { editing = it }
                    }
                    Spacer(Modifier.height(Space.l))
                    NbButton(s.tReset, Modifier.fillMaxWidth(), kind = ButtonKind.GHOST) { draft = CustomTheme() }
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
            editing?.let { f ->
                ColorPickerDialog(
                    title = f.label, initial = f.current(colors), useLabel = s.tUse,
                    autoLabel = if (f.auto) s.tAuto else null,
                    onPick = { col -> draft = f.set(col.toArgb().toLong() and 0xFFFFFFFFL); editing = null },
                    onAuto = if (f.auto) ({ draft = f.set(null); editing = null }) else null,
                    onDismiss = { editing = null },
                )
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
                .border(1.dp, c.borderStrong, RoundedCornerShape(Space.rCard)).padding(Space.m),
        ) {
            Text(s.previewCard, style = Type.bodyStrong, color = c.text1)
            Text(s.dailySub, style = Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = c.text2)
            Spacer(Modifier.height(Space.s))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(Mark.GREEN, Mark.YELLOW, Mark.GRAY).forEach { m ->
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(c.tile(m)), contentAlignment = Alignment.Center) {
                        Text("23", style = Type.label, color = c.onTile(m))
                    }
                }
                Spacer(Modifier.weight(1f))
                Scoreboard { LedText("4/8", style = Type.led.copy(fontSize = 20.sp, lineHeight = 22.sp)) }
            }
        }
        Spacer(Modifier.height(Space.m))
        NbButton(s.daily, Modifier.fillMaxWidth()) {}
        Spacer(Modifier.height(Space.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            listOf(
                androidx.compose.material.icons.Icons.Rounded.Shuffle to c.iconFree,
                androidx.compose.material.icons.Icons.Rounded.Link to c.iconChain,
                androidx.compose.material.icons.Icons.Rounded.SwapVert to c.iconHl,
                androidx.compose.material.icons.Icons.Rounded.History to c.iconArchive,
                androidx.compose.material.icons.Icons.Rounded.BarChart to c.iconStats,
                androidx.compose.material.icons.Icons.Rounded.EmojiEvents to c.iconAch,
            ).forEach { (icon, tint) ->
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)).border(1.dp, tint.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

/** Presets in a sideways-scrolling grid of three rows. */
@Composable
private fun PresetRows(title: String, presets: List<Pair<String, CustomTheme>>, onPick: (CustomTheme) -> Unit) {
    Spacer(Modifier.height(Space.xl))
    Text(title, style = Type.title, color = nb.text1)
    Spacer(Modifier.height(Space.s))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        presets.chunked(3).forEach { col ->
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                col.forEach { (name, t) -> PresetChip(name, t) { onPick(t) } }
            }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    val c = nb
    Spacer(Modifier.height(Space.xl))
    Text(title, style = Type.title, color = c.text1)
    Spacer(Modifier.height(Space.s))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Space.rCard)).background(c.surface1)
            .border(1.dp, c.border, RoundedCornerShape(Space.rCard)).padding(vertical = 4.dp),
    ) { content() }
}

@Composable
private fun ColorRow(f: Field, onEdit: (Field) -> Unit) {
    val c = nb
    val s = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).pressable { onEdit(f) }.padding(horizontal = Space.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(f.label, style = Type.bodyStrong, color = c.text1, modifier = Modifier.weight(1f))
        if (f.auto && !f.explicit) {
            Text(s.tAuto, style = Type.label, color = c.text2)
            Spacer(Modifier.width(Space.s))
        }
        Box(Modifier.size(34.dp).clip(CircleShape).background(f.current(c)).border(2.dp, c.borderStrong, CircleShape))
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val c = nb
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).pressable(role = Role.Switch) { onChange(!value) }.padding(horizontal = Space.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Type.bodyStrong, color = c.text1, modifier = Modifier.weight(1f))
        Switch(value, onChange, colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent, uncheckedTrackColor = c.surface2, uncheckedBorderColor = c.borderStrong))
    }
}

@Composable
private fun PresetChip(name: String, t: CustomTheme, onClick: () -> Unit) {
    val col = customColors(t)
    val shape = RoundedCornerShape(Space.rControl)
    Column(
        Modifier.width(104.dp).clip(shape).background(col.bg).border(1.dp, col.borderStrong, shape).pressable(onClick = onClick).padding(Space.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(col.surface1, col.accent, col.led).forEach { Box(Modifier.size(16.dp).clip(CircleShape).background(it)) }
        }
        Spacer(Modifier.height(4.dp))
        Text(name, style = Type.label, color = col.text1, maxLines = 1)
    }
}
