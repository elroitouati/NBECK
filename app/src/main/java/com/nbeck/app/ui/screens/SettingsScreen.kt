package com.nbeck.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nbeck.app.NbeckApp
import com.nbeck.app.data.Language
import com.nbeck.app.data.Settings
import com.nbeck.app.data.ThemeMode
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.components.ArenaBackground
import com.nbeck.app.ui.components.ButtonKind
import com.nbeck.app.ui.components.NbButton
import com.nbeck.app.ui.components.TopBar
import com.nbeck.app.ui.components.pressable
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as NbeckApp
    val s = LocalStrings.current
    val c = nb
    val settings by remember { app.prefs.settings }.collectAsState(initial = Settings())
    val players by app.players.players.collectAsState()
    val scope = rememberCoroutineScope()
    var refreshState by remember { mutableStateOf<String?>(null) }
    fun update(f: (Settings) -> Settings) { scope.launch { app.prefs.updateSettings(f) } }

    ArenaBackground(parquet = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(s.settings, onBack)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
                Section(s.language)
                Segmented(listOf("עברית", "English"), if (settings.language == Language.HE) 0 else 1) { i ->
                    update { it.copy(language = if (i == 0) Language.HE else Language.EN) }
                }
                Section(s.theme)
                val themes = listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM)
                Segmented(listOf(s.themeDark, s.themeLight, s.themeSystem), themes.indexOf(settings.theme)) { i ->
                    update { it.copy(theme = themes[i]) }
                }
                Spacer(Modifier.height(Space.xl))
                Toggle(s.sounds, settings.sound) { v -> update { it.copy(sound = v) } }
                Spacer(Modifier.height(Space.s))
                Toggle(s.haptics, settings.haptics) { v -> update { it.copy(haptics = v) } }
                Section(s.dataSection)
                Text(s.dataInfo(players.size), style = Type.body, color = c.text2)
                Spacer(Modifier.height(Space.m))
                NbButton(refreshState ?: s.refreshNow, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, enabled = refreshState != s.refreshing) {
                    scope.launch {
                        refreshState = s.refreshing
                        val ok = app.players.refresh(force = true)
                        refreshState = if (ok) s.refreshed else s.refreshFailed
                    }
                }
                Spacer(Modifier.height(Space.xxl))
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(Space.xl))
    Text(title, style = Type.title, color = nb.text1)
    Spacer(Modifier.height(Space.s))
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val c = nb
    val shape = RoundedCornerShape(Space.rCard)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape).background(c.surface1).border(1.dp, c.border, shape)
            .pressable(role = Role.Switch) { onChange(!value) }.padding(horizontal = Space.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Type.bodyStrong, color = c.text1, modifier = Modifier.weight(1f))
        Switch(
            checked = value, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent, uncheckedTrackColor = c.surface2, uncheckedBorderColor = c.borderStrong),
        )
    }
}
