package com.nbeck.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.nbeck.app.data.Language
import com.nbeck.app.data.SavedGame
import com.nbeck.app.data.ThemeMode
import com.nbeck.app.game.Daily
import com.nbeck.app.game.Mode
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.screens.AchievementsScreen
import com.nbeck.app.ui.screens.GameScreen
import com.nbeck.app.ui.screens.HomeScreen
import com.nbeck.app.ui.screens.HowToScreen
import com.nbeck.app.ui.screens.SettingsScreen
import com.nbeck.app.ui.screens.StatsScreen
import com.nbeck.app.ui.stringsFor
import com.nbeck.app.ui.theme.NbeckTheme
import kotlinx.coroutines.runBlocking
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Design review renders (not assertions). Run with -Pscreenshots; output in app/build/outputs/roborazzi. */
private fun androidx.compose.ui.Modifier.androidxBg() = this.background(androidx.compose.ui.graphics.Color(0xFF2B3FB0))

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h851dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<NbeckApp>()

    @Before fun only() = assumeTrue(System.getProperty("screenshots") == "true")

    private fun shot(name: String, lang: Language = Language.HE, theme: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(
                LocalStrings provides stringsFor(lang),
                LocalLayoutDirection provides if (lang == Language.HE) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) { NbeckTheme(theme) { content() } }
        }
        compose.mainClock.advanceTimeBy(3000)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    private fun seedDaily(guessNames: List<String>, finished: Boolean = false, won: Boolean = false) = runBlocking {
        val players = app.players.load()
        val today = LocalDate.now()
        val answer = Daily.pick(players, today)
        val ids = guessNames.mapNotNull { n -> players.firstOrNull { it.name == n }?.id }.let { if (won) it + answer.id else it }
        app.prefs.saveGame(SavedGame(Mode.DAILY, today.toString(), answer.id, ids, finished = finished, won = won, recorded = true))
    }

    private val someGuesses = listOf("LeBron James", "Nikola Jokic", "Jayson Tatum", "Deni Avdija")

    /** Launcher icon under the two common masks: circle and rounded square (72dp visible of 108dp). */
    @Test fun launcher_icon() = shot("00_icon") {
        androidx.compose.foundation.layout.Row(
            androidx.compose.ui.Modifier.androidxBg().padding(24.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
        ) {
            listOf(androidx.compose.foundation.shape.CircleShape, androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).forEach { shape ->
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier.size(144.dp).clip(shape).background(androidx.compose.ui.graphics.Color(0xFF0A0A0B)),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    androidx.compose.foundation.Image(
                        androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground), null,
                        androidx.compose.ui.Modifier.requiredSize(216.dp),
                    )
                }
            }
        }
    }

    @Test fun intro_frames() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides stringsFor(Language.HE), LocalLayoutDirection provides LayoutDirection.Rtl) {
                NbeckTheme(ThemeMode.DARK) { com.nbeck.app.ui.components.IntroOverlay(onLand = {}, onDone = {}) }
            }
        }
        var t = 0L
        for (at in listOf(150L, 380L, 450L, 560L, 700L, 900L, 1100L)) {
            compose.mainClock.advanceTimeBy(at - t); t = at
            compose.onRoot().captureRoboImage("build/screenshots/intro_${at}ms.png")
        }
    }

    @Test fun versions_he() = shot("12_versions_he") { com.nbeck.app.ui.screens.VersionsScreen({}) }

    @OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
    @Test fun whats_new_he() {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides stringsFor(Language.HE), LocalLayoutDirection provides LayoutDirection.Rtl) {
                NbeckTheme(ThemeMode.DARK) {
                    HomeScreen({}, {}, {}, {}, {}, {})
                    com.nbeck.app.ui.screens.WhatsNewDialog(com.nbeck.app.data.Changelog.releases.take(1)) {}
                }
            }
        }
        compose.mainClock.advanceTimeBy(1500); compose.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("build/screenshots/13_whats_new_he.png")
    }

    @OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
    @Test fun update_available_he() {
        compose.setContent {
            CompositionLocalProvider(LocalStrings provides stringsFor(Language.HE), LocalLayoutDirection provides LayoutDirection.Rtl) {
                NbeckTheme(ThemeMode.DARK) {
                    HomeScreen({}, {}, {}, {}, {}, {})
                    com.nbeck.app.ui.screens.UpdateAvailableDialog(
                        com.nbeck.app.data.LatestRelease(99, "1.8.0", "https://example.invalid/DNBA.apk",
                            notesHe = listOf("אנימציה חדשה בניצחון", "תיקוני באגים")),
                        onUpdate = {}, onLater = {},
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(1500); compose.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("build/screenshots/14_update_he.png")
    }

    @Test fun account_signed_out_he() = shot("15_account_he") { com.nbeck.app.ui.screens.AccountScreen({}) }
    @Test fun leaderboards_signed_out_he() = shot("16_leaderboards_he") { com.nbeck.app.ui.screens.LeaderboardScreen({}, {}) }

    @Test fun home_he_dark() = shot("01_home_he_dark") { HomeScreen({}, {}, {}, {}, {}, {}) }
    @Test fun home_en_light() = shot("02_home_en_light", Language.EN, ThemeMode.LIGHT) { HomeScreen({}, {}, {}, {}, {}, {}) }
    @Test fun howto_he() = shot("03_howto_he") { HowToScreen({}, {}) }
    @Test fun game_empty_he() { seedDaily(emptyList()); shot("04_game_empty_he") { GameScreen(Mode.DAILY, {}, {}) } }
    @Test fun game_progress_he() { seedDaily(someGuesses); shot("05_game_progress_he") { GameScreen(Mode.DAILY, {}, {}) } }
    @Test fun game_progress_light_en() { seedDaily(someGuesses); shot("06_game_progress_en_light", Language.EN, ThemeMode.LIGHT) { GameScreen(Mode.DAILY, {}, {}) } }
    @Test fun game_won_he() { seedDaily(someGuesses.take(3), finished = true, won = true); shot("07_end_won_he") { GameScreen(Mode.DAILY, {}, {}) } }
    @Test fun stats_he() = shot("08_stats_he") { StatsScreen({}, {}) }
    @Test fun achievements_he() = shot("09_achievements_he") { AchievementsScreen({}) }
    @Test fun archive_he() {
        seedDaily(listOf("LeBron James", "Nikola Jokic"), finished = true, won = true)
        shot("11_archive_he") { com.nbeck.app.ui.screens.ArchiveScreen({}, {}) }
    }
    @Test fun settings_he() = shot("10_settings_he") { SettingsScreen({}) }
}
