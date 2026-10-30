package com.nbeck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nbeck.app.audio.Sfx
import com.nbeck.app.data.Language
import com.nbeck.app.data.effectiveTheme
import com.nbeck.app.game.Achievement
import com.nbeck.app.game.Mode
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.screens.AchievementsScreen
import com.nbeck.app.ui.screens.ArchiveScreen
import com.nbeck.app.ui.screens.GameScreen
import com.nbeck.app.ui.screens.HomeScreen
import com.nbeck.app.ui.screens.HowToScreen
import com.nbeck.app.ui.screens.SettingsScreen
import com.nbeck.app.ui.screens.StatsScreen
import com.nbeck.app.ui.screens.icon
import com.nbeck.app.ui.stringsFor
import com.nbeck.app.ui.theme.DarkColors
import com.nbeck.app.ui.theme.Motion
import com.nbeck.app.ui.theme.NbeckTheme
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Android 13+: notifications need runtime permission; asked once, after the first intro. */
    private val notificationPermission =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) (application as NbeckApp).appScope.launch { com.nbeck.app.data.Updates.check(this@MainActivity) }
        }

    private fun animationsEnabled() =
        android.provider.Settings.Global.getFloat(contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

    /** Every time the app is opened (or comes back to the foreground): pull fresh rosters from ESPN. */
    override fun onStart() {
        super.onStart()
        val app = application as NbeckApp
        app.appScope.launch { app.players.refreshOnOpen() }
        app.appScope.launch { com.nbeck.app.data.Updates.check(app) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val app = application as NbeckApp
        setContent {
            val settings by app.prefs.settings.collectAsState(initial = null)
            val st = settings
            if (st == null) {
                Box(Modifier.fillMaxSize().background(DarkColors.bg))
                return@setContent
            }
            val dir = if (st.language == Language.HE) LayoutDirection.Rtl else LayoutDirection.Ltr
            // Intro only on a fresh launch — not when coming back from the recent-apps list or after
            // a rotation (savedInstanceState != null), and not when system animations are off.
            var showIntro by rememberSaveable { mutableStateOf(savedInstanceState == null && animationsEnabled()) }
            var whatsNew by remember { mutableStateOf<List<com.nbeck.app.data.Release>>(emptyList()) }
            LaunchedEffect(Unit) {
                val current = BuildConfig.VERSION_CODE
                if (st.lastSeenVersion < current) {
                    // A brand-new player gets the how-to, not a changelog.
                    if (st.seenHowTo) whatsNew = com.nbeck.app.data.Changelog.since(st.lastSeenVersion).ifEmpty { com.nbeck.app.data.Changelog.releases.take(1) }
                    app.prefs.updateSettings { it.copy(lastSeenVersion = current) }
                }
            }
            LaunchedEffect(showIntro) {
                if (!showIntro && android.os.Build.VERSION.SDK_INT >= 33 && !st.askedNotifications) {
                    app.prefs.updateSettings { it.copy(askedNotifications = true) }
                    notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            CompositionLocalProvider(LocalStrings provides stringsFor(st.language), LocalLayoutDirection provides dir) {
                NbeckTheme(st.effectiveTheme) {
                    AppNav(showHowToFirst = !st.seenHowTo)
                    AchievementToastHost()
                    if (whatsNew.isNotEmpty() && !showIntro) {
                        com.nbeck.app.ui.screens.WhatsNewDialog(whatsNew.take(3)) { whatsNew = emptyList() }
                    }
                    // Newer version in the repo: offer it on every open (after the intro and any
                    // "what's new"), until installed. "Later" hides it for this session only.
                    val latest by app.latestRelease.collectAsState()
                    var updateDismissed by rememberSaveable { mutableStateOf(false) }
                    val update = latest?.takeIf { com.nbeck.app.data.Updates.isNewer(it) }
                    if (update != null && !updateDismissed && !showIntro && whatsNew.isEmpty()) {
                        com.nbeck.app.ui.screens.UpdateAvailableDialog(
                            update,
                            onUpdate = { updateDismissed = true; startActivity(com.nbeck.app.data.Updates.downloadIntent(update)) },
                            onLater = { updateDismissed = true },
                        )
                    }
                    if (showIntro) {
                        com.nbeck.app.ui.components.IntroOverlay(onLand = { app.feedback.tick() }, onDone = { showIntro = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNav(showHowToFirst: Boolean) {
    val nav = rememberNavController()
    val app = (androidx.compose.ui.platform.LocalContext.current.applicationContext as NbeckApp)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(Unit) {
        if (showHowToFirst) {
            nav.navigate("howto")
            app.prefs.updateSettings { it.copy(seenHowTo = true) }
        }
    }
    val enter = tween<androidx.compose.ui.unit.IntOffset>(Motion.page, easing = Motion.easeOut)
    NavHost(
        nav, startDestination = "home",
        modifier = Modifier.fillMaxSize().background(nb.bg),
        enterTransition = { slideIntoContainer(SlideDirection.Start, enter) { it / 5 } + fadeIn(tween(Motion.small)) },
        exitTransition = { fadeOut(tween(Motion.small)) },
        popEnterTransition = { fadeIn(tween(Motion.small)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, enter) { it / 5 } + fadeOut(tween(Motion.small)) },
    ) {
        composable("home") {
            HomeScreen(
                onPlay = { m -> nav.navigate("game/${m.name}") },
                onStats = { nav.navigate("stats") },
                onAchievements = { nav.navigate("achievements") },
                onSettings = { nav.navigate("settings") },
                onHowTo = { nav.navigate("howto") },
                onArchive = { nav.navigate("archive") },
            )
        }
        composable(
            "game/{mode}?date={date}",
            arguments = listOf(androidx.navigation.navArgument("date") { type = androidx.navigation.NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            val mode = Mode.valueOf(entry.arguments?.getString("mode") ?: "DAILY")
            val date = entry.arguments?.getString("date")?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                ?: java.time.LocalDate.now()
            GameScreen(mode, onBack = { nav.popBackStack() }, onHome = { nav.popBackStack("home", inclusive = false) }, date = date)
        }
        composable("archive") {
            ArchiveScreen(onBack = { nav.popBackStack() }, onPlay = { d -> nav.navigate("game/DAILY?date=$d") })
        }
        composable("howto") {
            HowToScreen(onBack = { nav.popBackStack() }, onPlay = {
                scope.launch { nav.navigate("game/DAILY") { popUpTo("home") } }
            })
        }
        composable("stats") { StatsScreen(onBack = { nav.popBackStack() }, onPlay = { m -> nav.navigate("game/${m.name}") { popUpTo("home") } }) }
        composable("achievements") { AchievementsScreen(onBack = { nav.popBackStack() }) }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }, onVersions = { nav.navigate("versions") }) }
        composable("versions") { com.nbeck.app.ui.screens.VersionsScreen(onBack = { nav.popBackStack() }) }
    }
}

/** Slides a "new achievement" banner in from the top, with the crowd sound. */
@Composable
private fun AchievementToastHost() {
    val app = (androidx.compose.ui.platform.LocalContext.current.applicationContext as NbeckApp)
    val s = LocalStrings.current
    val c = nb
    var current by remember { mutableStateOf<Achievement?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        app.achievementEvents.collect { list ->
            delay(1800) // let the board and end sheet land first
            for (a in list) {
                current = a
                visible = true
                app.feedback.play(Sfx.CROWD)
                delay(2800)
                visible = false
                delay(400)
            }
        }
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(Space.gutter), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(Motion.page, easing = Motion.drawer)) { -it } + fadeIn(tween(Motion.small)) + scaleIn(tween(Motion.page), initialScale = 0.94f),
            exit = slideOutVertically(tween(Motion.small, easing = Motion.easeOut)) { -it } + fadeOut(tween(Motion.press)),
        ) {
            val a = current ?: return@AnimatedVisibility
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Space.rCard)).background(c.board)
                    .border(1.dp, c.led.copy(alpha = 0.5f), RoundedCornerShape(Space.rCard)).padding(Space.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                    Icon(a.icon(), null, tint = c.onAccent)
                }
                Spacer(Modifier.width(Space.m))
                Column {
                    Text(s.achievementUnlocked, style = Type.label, color = c.led)
                    Text(s.achievementTitle(a), style = Type.title, color = Color(0xFFF4F6F8))
                    Text(s.achievementDesc(a), style = Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = Color(0xADF4F6F8))
                }
            }
        }
    }
}
