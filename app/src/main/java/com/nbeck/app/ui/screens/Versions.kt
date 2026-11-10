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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nbeck.app.BuildConfig
import com.nbeck.app.data.Release
import com.nbeck.app.data.Changelog
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.Strings
import com.nbeck.app.ui.He
import com.nbeck.app.ui.components.ArenaBackground
import com.nbeck.app.ui.components.LedText
import com.nbeck.app.ui.components.NbButton
import com.nbeck.app.ui.components.TopBar
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb

private fun Release.notes(s: Strings) = if (s === He) he else en
private fun Release.prettyDate() = date.split("-").let { (y, m, d) -> "$d.$m.$y" }

/** Shown once after an update: the notes of every version the player hasn't seen. */
@Composable
fun WhatsNewDialog(releases: List<Release>, onDismiss: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    val shape = RoundedCornerShape(Space.rPanel)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(shape).background(c.surface1).border(1.dp, c.border, shape).padding(Space.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.whatsNewTitle(BuildConfig.VERSION_NAME), style = Type.h2, color = c.text1, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(Space.l))
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.l)) {
                releases.forEach { r ->
                    Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                        if (releases.size > 1) VersionChip(r)
                        r.notes(s).forEach { Bullet(it) }
                    }
                }
            }
            Spacer(Modifier.height(Space.xl))
            NbButton(s.gotIt, Modifier.fillMaxWidth(), onClick = onDismiss)
        }
    }
}

/** Settings > Updates & versions: every release, newest first. */
@Composable
fun VersionsScreen(onBack: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    ArenaBackground(parquet = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(s.versionHistory, onBack)
            LazyColumn(
                Modifier.padding(horizontal = Space.gutter),
                verticalArrangement = Arrangement.spacedBy(Space.m),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Space.xxl),
            ) {
                items(Changelog.releases, key = { it.code }) { r ->
                    val current = r.code == BuildConfig.VERSION_CODE
                    val shape = RoundedCornerShape(Space.rCard)
                    Column(
                        Modifier.fillMaxWidth().clip(shape).background(c.surface1)
                            .border(1.dp, if (current) c.accent.copy(alpha = 0.6f) else c.border, shape)
                            .padding(Space.l),
                        verticalArrangement = Arrangement.spacedBy(Space.s),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VersionChip(r)
                            Spacer(Modifier.weight(1f))
                            if (current) Text(s.installed, style = Type.label, color = c.accent)
                        }
                        r.notes(s).forEach { Bullet(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionChip(r: Release) {
    val c = nb
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(c.board).padding(horizontal = Space.s, vertical = 2.dp)) {
            LedText(r.name, style = Type.led.copy(fontSize = 18.sp, lineHeight = 20.sp))
        }
        Spacer(Modifier.width(Space.s))
        Text(r.prettyDate(), style = Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = c.text2)
    }
}

@Composable
private fun Bullet(text: String) {
    val c = nb
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 9.dp).size(6.dp).clip(CircleShape).background(c.accent))
        Spacer(Modifier.width(Space.m))
        Text(text, style = Type.body, color = c.text1, modifier = Modifier.weight(1f))
    }
}

/** On open, when the repo has a newer version: its notes and "Update now" (downloads and installs in-app). */
@Composable
fun UpdateAvailableDialog(release: com.nbeck.app.data.LatestRelease, onLater: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    val shape = RoundedCornerShape(Space.rPanel)
    val busy = com.nbeck.app.data.Installer.state.collectAsState().value is com.nbeck.app.data.InstallState.Downloading
    Dialog(onDismissRequest = { if (!busy) onLater() }) {
        Column(
            Modifier.fillMaxWidth().clip(shape).background(c.surface1).border(1.dp, c.accent.copy(alpha = 0.6f), shape).padding(Space.xl),
        ) {
            Text(s.updateTitle, style = Type.h2, color = c.text1)
            Spacer(Modifier.height(Space.s))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(c.board).padding(horizontal = Space.s, vertical = 2.dp)) {
                    LedText(release.versionName, style = Type.led.copy(fontSize = 18.sp, lineHeight = 20.sp))
                }
                Spacer(Modifier.width(Space.s))
                Text("${s.currentVersion}: ${BuildConfig.VERSION_NAME}", style = Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = c.text2)
            }
            val notes = if (s === He) release.notesHe else release.notesEn
            if (notes.isNotEmpty()) {
                Spacer(Modifier.height(Space.l))
                Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    notes.forEach { Bullet(it) }
                }
            }
            Spacer(Modifier.height(Space.xl))
            UpdateAction(release)
            if (!busy) {
                Spacer(Modifier.height(Space.s))
                NbButton(s.later, Modifier.fillMaxWidth(), kind = com.nbeck.app.ui.components.ButtonKind.GHOST, onClick = onLater)
            }
        }
    }
}

/** "Update now" button that turns into download progress / install status. Used in the dialog and Settings. */
@Composable
fun UpdateAction(release: com.nbeck.app.data.LatestRelease, autoStart: Boolean = false) {
    val c = nb
    val s = LocalStrings.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val state by com.nbeck.app.data.Installer.state.collectAsState()
    val start = { scope.launch { com.nbeck.app.data.Installer.downloadAndInstall(ctx.applicationContext, release) }; Unit }
    androidx.compose.runtime.LaunchedEffect(autoStart) { if (autoStart && state is com.nbeck.app.data.InstallState.Idle) start() }
    when (val st = state) {
        is com.nbeck.app.data.InstallState.Downloading -> Column {
            Text(s.downloading(st.percent), style = Type.bodyStrong, color = c.text1)
            Spacer(Modifier.height(Space.s))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { st.percent / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = c.accent, trackColor = c.surface2, drawStopIndicator = {},
            )
        }
        com.nbeck.app.data.InstallState.NeedsPermission -> Text(s.installNeedsPermission, style = Type.body, color = c.text1)
        com.nbeck.app.data.InstallState.Installing -> Column {
            Text(s.installOpened, style = Type.body, color = c.text2)
            Spacer(Modifier.height(Space.s))
            NbButton(s.updateNow, Modifier.fillMaxWidth(), onClick = start)
        }
        is com.nbeck.app.data.InstallState.Failed -> Column {
            Text(s.installFailed, style = Type.body, color = Color(0xFFE5484D))
            Spacer(Modifier.height(Space.s))
            NbButton(s.retry, Modifier.fillMaxWidth(), onClick = start)
        }
        com.nbeck.app.data.InstallState.Idle -> NbButton(s.updateNow, Modifier.fillMaxWidth(), onClick = start)
    }
}
