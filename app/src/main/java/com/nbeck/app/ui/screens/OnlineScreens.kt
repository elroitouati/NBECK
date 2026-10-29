package com.nbeck.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.nbeck.app.NbeckApp
import com.nbeck.app.data.BoardRow
import com.nbeck.app.data.Online
import com.nbeck.app.data.Profile
import com.nbeck.app.data.Teams
import com.nbeck.app.game.Achievement
import com.nbeck.app.game.MAX_GUESSES
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.components.ArenaBackground
import com.nbeck.app.ui.components.ButtonKind
import com.nbeck.app.ui.components.LedText
import com.nbeck.app.ui.components.NbButton
import com.nbeck.app.ui.components.Scoreboard
import com.nbeck.app.ui.components.TopBar
import com.nbeck.app.ui.components.pressable
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Profile photo when shared, else the favourite team's logo, else the nickname's first letter. */
@Composable
fun Avatar(nickname: String, team: Int?, photo: String?, size: Dp = 40.dp) {
    val c = nb
    val t = team?.let { Teams.find(it) }
    Box(
        Modifier.size(size).clip(CircleShape).background(t?.color ?: c.surface2),
        contentAlignment = Alignment.Center,
    ) {
        when {
            photo != null -> AsyncImage(photo, null, Modifier.fillMaxSize())
            t != null -> AsyncImage(t.logoUrl, null, Modifier.size(size * 0.72f))
            else -> Text(nickname.take(1).uppercase(), style = Type.bodyStrong.copy(fontSize = (size.value * 0.42f).sp), color = c.text1)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Account: sign in → set up profile → profile summary

@Composable
fun AccountScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as NbeckApp
    val online = app.online
    val s = LocalStrings.current
    val profile by online.profile.collectAsState()
    val needsProfile by online.needsProfile.collectAsState()
    val signedIn by online.signedIn.collectAsState()
    val profileError by online.profileError.collectAsState()
    var editing by rememberSaveable { mutableStateOf(false) }

    ArenaBackground(parquet = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(s.account, onBack)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
                when {
                    signedIn == null -> SignInCard()
                    needsProfile || editing -> ProfileForm(profile) { editing = false }
                    profile != null -> MyProfile(profile!!, onEdit = { editing = true })
                    profileError != null -> ProfileErrorCard(profileError!!)
                    else -> Text(s.loading, style = Type.body, color = nb.text2)
                }
                Spacer(Modifier.height(Space.xxl))
            }
        }
    }
}

/** Signed in with Google, but the profile couldn't be loaded — say so and offer a retry. */
@Composable
private fun ProfileErrorCard(detail: String) {
    val c = nb
    val s = LocalStrings.current
    val app = LocalContext.current.applicationContext as NbeckApp
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(Space.rCard)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface1).border(1.dp, c.border, shape).padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(s.profileLoadFailed, style = Type.title, color = c.text1, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.s))
        Text(detail, style = Type.label, color = Color(0xFFE5484D), textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.l))
        NbButton(s.retry, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY) { scope.launch { app.online.reloadProfile() } }
    }
}

@Composable
fun SignInCard(modifier: Modifier = Modifier) {
    val c = nb
    val s = LocalStrings.current
    val ctx = LocalContext.current
    val app = ctx.applicationContext as NbeckApp
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val shape = RoundedCornerShape(Space.rCard)
    Column(
        modifier.fillMaxWidth().clip(shape).background(c.surface1).border(1.dp, c.border, shape).padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(s.signInTitle, style = Type.h2, color = c.text1, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.s))
        Text(s.signInBody, style = Type.body, color = c.text2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.xl))
        NbButton(if (busy) s.loading else s.signInGoogle, Modifier.fillMaxWidth(), enabled = !busy) {
            val activity = ctx.findActivity() ?: return@NbButton
            busy = true; error = null
            scope.launch {
                runCatching { app.online.signIn(activity) }.onFailure { e ->
                    android.util.Log.w("DNBA", "sign-in failed", e)
                    // Technical detail under the friendly message, so a screenshot tells us what broke.
                    error = s.signInFailed + "\n" + (e::class.simpleName ?: "Error") + ": " + (e.message ?: "").take(160)
                }
                busy = false
            }
        }
        if (error != null) {
            Spacer(Modifier.height(Space.s))
            Text(error!!, style = Type.label, color = Color(0xFFE5484D), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ProfileForm(current: Profile?, onDone: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    val app = LocalContext.current.applicationContext as NbeckApp
    val scope = rememberCoroutineScope()
    var nick by rememberSaveable { mutableStateOf(current?.nickname ?: app.online.googleName?.take(16).orEmpty()) }
    var team by rememberSaveable { mutableStateOf(current?.team) }
    var showPhoto by rememberSaveable { mutableStateOf(current?.photo != null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Text(s.profileTitle, style = Type.h2, color = c.text1)
    Spacer(Modifier.height(Space.l))
    Text(s.nickname, style = Type.bodyStrong, color = c.text1)
    Spacer(Modifier.height(Space.xs))
    val shape = RoundedCornerShape(Space.rControl)
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(c.surface2)
            .border(1.dp, if (error != null) Color(0xFFE5484D) else c.borderStrong, shape).padding(horizontal = Space.m),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = nick, onValueChange = { nick = it.take(16); error = null }, singleLine = true,
            textStyle = Type.body.copy(color = c.text1), cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), modifier = Modifier.fillMaxWidth(),
        )
    }
    Text(error ?: s.nicknameHint, style = Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
        color = if (error != null) Color(0xFFE5484D) else c.text2, modifier = Modifier.padding(top = 4.dp))

    Spacer(Modifier.height(Space.xl))
    Text(s.favoriteTeam, style = Type.bodyStrong, color = c.text1)
    Spacer(Modifier.height(Space.s))
    TeamPicker(team) { team = it }

    Spacer(Modifier.height(Space.xl))
    val cardShape = RoundedCornerShape(Space.rCard)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(cardShape).background(c.surface1).border(1.dp, c.border, cardShape)
            .pressable(role = Role.Switch) { showPhoto = !showPhoto }.padding(horizontal = Space.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(nick.ifBlank { "?" }, team, if (showPhoto) app.online.googlePhoto else null, 40.dp)
        Spacer(Modifier.width(Space.m))
        Text(s.showPhoto, style = Type.body, color = c.text1, modifier = Modifier.weight(1f))
        Switch(showPhoto, { showPhoto = it }, colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent, uncheckedTrackColor = c.surface2, uncheckedBorderColor = c.borderStrong))
    }

    Spacer(Modifier.height(Space.xl))
    NbButton(if (busy) s.loading else s.save, Modifier.fillMaxWidth(), enabled = !busy) {
        busy = true
        scope.launch {
            val r = runCatching { app.online.saveProfile(nick, team, showPhoto) }.getOrElse { Online.NickResult.INVALID }
            busy = false
            when (r) {
                Online.NickResult.OK -> {
                    app.online.syncStats(app.prefs.stats.collectFirst(), app.prefs.achievements.collectFirst())
                    onDone()
                }
                Online.NickResult.TAKEN -> error = s.nicknameTaken
                Online.NickResult.INVALID -> error = s.nicknameInvalid
            }
        }
    }
}

private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.collectFirst(): T = first()

@Composable
private fun TeamPicker(selected: Int?, onPick: (Int?) -> Unit) {
    val c = nb
    val s = LocalStrings.current
    val all = listOf<Int?>(null) + Teams.all.sortedBy { it.abbr }.map { it.espnId }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        all.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { id ->
                    val on = id == selected
                    val t = id?.let { Teams[it] }
                    val shape = RoundedCornerShape(10.dp)
                    Column(
                        Modifier.weight(1f).clip(shape).background(if (on) (t?.color ?: c.accent) else c.surface1)
                            .border(if (on) 2.dp else 1.dp, if (on) c.accent else c.border, shape)
                            .pressable { onPick(id) }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (t != null) AsyncImage(t.logoUrl, t.fullName, Modifier.size(24.dp)) else Spacer(Modifier.height(24.dp))
                        Text(t?.abbr ?: s.noTeam, style = Type.label.copy(fontSize = 10.sp, textDirection = TextDirection.Ltr),
                            color = if (on) Color(0xFFF7F7F7) else c.text2, maxLines = 1)
                    }
                }
                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MyProfile(p: Profile, onEdit: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    val ctx = LocalContext.current
    val app = ctx.applicationContext as NbeckApp
    val scope = rememberCoroutineScope()
    ProfileHeader(p)
    Spacer(Modifier.height(Space.l))
    PublicStatsBoard(p)
    Spacer(Modifier.height(Space.l))
    val shape = RoundedCornerShape(Space.rCard)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(c.surface1).border(1.dp, c.border, shape).padding(Space.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(s.friendCode, style = Type.bodyStrong, color = c.text1, modifier = Modifier.weight(1f))
        LedText(p.friendCode, style = Type.led.copy(fontSize = 22.sp, lineHeight = 24.sp))
    }
    Spacer(Modifier.height(Space.xl))
    NbButton(s.editProfile, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, onClick = onEdit)
    Spacer(Modifier.height(Space.s))
    NbButton(s.signOut, Modifier.fillMaxWidth(), kind = ButtonKind.GHOST) {
        ctx.findActivity()?.let { a -> scope.launch { app.online.signOut(a) } }
    }
}

@Composable
private fun ProfileHeader(p: Profile) {
    val c = nb
    val t = p.team?.let { Teams.find(it) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(p.nickname, p.team, p.photo, 64.dp)
        Spacer(Modifier.width(Space.l))
        Column {
            Text(p.nickname, style = Type.h2, color = c.text1)
            if (t != null) Text(t.fullName, style = Type.body.copy(textDirection = TextDirection.Ltr), color = c.text2)
        }
    }
}

@Composable
private fun PublicStatsBoard(p: Profile) {
    val s = LocalStrings.current
    Scoreboard(Modifier.fillMaxWidth()) {
        StatBig(p.stats.played.toString(), s.gamesPlayed, Modifier.weight(1f))
        StatBig("${p.stats.winPct}", s.winPct, Modifier.weight(1f))
        StatBig(p.stats.currentStreak.toString(), s.streak, Modifier.weight(1f))
        StatBig(p.stats.maxStreak.toString(), s.maxStreak, Modifier.weight(1f))
    }
}

@Composable
private fun StatBig(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LedText(value, style = Type.led.copy(fontSize = 26.sp, lineHeight = 28.sp))
        Text(label, style = Type.label.copy(fontSize = 11.sp), color = Color(0xFF8C93A1), textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** Another player's public profile, opened from a leaderboard row. */
@Composable
fun ProfileDialog(uid: String, onDismiss: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    val app = LocalContext.current.applicationContext as NbeckApp
    var p by remember { mutableStateOf<Profile?>(null) }
    LaunchedEffect(uid) { p = runCatching { app.online.publicProfile(uid) }.getOrNull() }
    val shape = RoundedCornerShape(Space.rPanel)
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(shape).background(c.surface1).border(1.dp, c.border, shape).padding(Space.xl)) {
            val prof = p
            if (prof == null) {
                Text(s.loading, style = Type.body, color = c.text2)
            } else {
                ProfileHeader(prof)
                Spacer(Modifier.height(Space.l))
                PublicStatsBoard(prof)
                val earned = prof.achievements.mapNotNull { n -> Achievement.entries.firstOrNull { it.name == n } }
                if (earned.isNotEmpty()) {
                    Spacer(Modifier.height(Space.l))
                    Text("${s.achievements} · ${earned.size}/${Achievement.entries.size}", style = Type.bodyStrong, color = c.text1)
                    Spacer(Modifier.height(Space.s))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScrollCompat()) {
                        earned.forEach { a ->
                            Box(Modifier.size(36.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                                androidx.compose.material3.Icon(a.icon(), s.achievementTitle(a), tint = c.onAccent, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Space.xl))
            NbButton(s.close, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, onClick = onDismiss)
        }
    }
}

@Composable
private fun Modifier.horizontalScrollCompat() = this.then(Modifier.horizontalScroll(rememberScrollState()))

// ---------------------------------------------------------------------------------------------
// Leaderboards: today's daily + this month's league

@Composable
fun LeaderboardScreen(onBack: () -> Unit, onAccount: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    val app = LocalContext.current.applicationContext as NbeckApp
    val profile by app.online.profile.collectAsState()
    val signedIn by app.online.signedIn.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    var rows by remember { mutableStateOf<List<BoardRow>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    var openUid by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now()
    val month = YearMonth.from(today)

    LaunchedEffect(tab, reload, profile?.uid) {
        if (app.online.uid == null) return@LaunchedEffect
        rows = null; failed = false
        runCatching {
            if (tab == 0) app.online.dailyBoard(today) else app.online.leagueBoard(Online.monthKey(today))
        }.onSuccess { rows = it }.onFailure { failed = true }
    }

    ArenaBackground(parquet = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(s.leaderboards, onBack)
            Column(Modifier.padding(horizontal = Space.gutter)) {
                Segmented(listOf(s.todayTab, s.leagueMonth(month)), tab) { tab = it }
                Spacer(Modifier.height(Space.m))
            }
            when {
                signedIn == null || profile == null -> Column(Modifier.padding(horizontal = Space.gutter)) {
                    if (signedIn == null) SignInCard() else NbButton(s.profileTitle, Modifier.fillMaxWidth(), onClick = onAccount)
                }
                failed -> Column(Modifier.fillMaxWidth().padding(Space.xl), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.loadFailed, style = Type.body, color = c.text2, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(Space.m))
                    NbButton(s.retry, kind = ButtonKind.SECONDARY) { reload++ }
                }
                rows == null -> Text(s.loading, style = Type.body, color = c.text2, modifier = Modifier.padding(Space.gutter))
                rows!!.isEmpty() -> Text(if (tab == 0) s.noResultsToday else s.noLeagueYet, style = Type.body, color = c.text2,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(Space.xl))
                else -> LazyColumn(
                    Modifier.padding(horizontal = Space.gutter),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = Space.xxl),
                ) {
                    if (tab == 1) item { Text(s.leagueRules, style = Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = c.text2, modifier = Modifier.padding(bottom = Space.s)) }
                    itemsIndexed(rows!!, key = { _, r -> r.uid }) { i, r ->
                        BoardRowView(i + 1, r, daily = tab == 0, me = r.uid == profile?.uid) { openUid = r.uid }
                    }
                }
            }
        }
    }
    openUid?.let { ProfileDialog(it) { openUid = null } }
}

@Composable
private fun BoardRowView(rank: Int, r: BoardRow, daily: Boolean, me: Boolean, onClick: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    val shape = RoundedCornerShape(Space.rCard)
    val medal = when (rank) { 1 -> Color(0xFFFFC94A); 2 -> Color(0xFFC9D1DB); 3 -> Color(0xFFD08A4E); else -> null }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape).background(if (me) c.accent.copy(alpha = 0.16f) else c.surface1)
            .border(1.dp, if (me) c.accent else c.border, shape).pressable(onClick = onClick).padding(horizontal = Space.m, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
            LedText("$rank", style = Type.led.copy(fontSize = 20.sp, lineHeight = 22.sp), color = medal ?: c.text2)
        }
        Avatar(r.nickname, r.team, r.photo, 38.dp)
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text(if (me) "${r.nickname} · ${s.you}" else r.nickname, style = Type.bodyStrong, color = c.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            r.team?.let { Teams.find(it) }?.let { Text(it.abbr, style = Type.label.copy(textDirection = TextDirection.Ltr), color = c.text2) }
        }
        if (daily) {
            Column(horizontalAlignment = Alignment.End) {
                LedText(if (r.won) "${r.guesses}/$MAX_GUESSES" else "X/$MAX_GUESSES", style = Type.led.copy(fontSize = 20.sp, lineHeight = 22.sp),
                    color = if (r.won) c.led else c.text3)
                Text("%d:%02d".format(r.seconds / 60, r.seconds % 60), style = Type.label.copy(textDirection = TextDirection.Ltr), color = c.text2)
            }
        } else {
            Column(horizontalAlignment = Alignment.End) {
                LedText("${r.points}", style = Type.led.copy(fontSize = 22.sp, lineHeight = 24.sp))
                Text("${r.played} ${s.gamesPlayed}", style = Type.label.copy(fontSize = 11.sp), color = c.text2)
            }
        }
    }
}
