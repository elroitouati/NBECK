package com.nbeck.app

import androidx.test.core.app.ApplicationProvider
import com.nbeck.app.game.HL_ROUNDS
import com.nbeck.app.game.HlChoice
import com.nbeck.app.ui.screens.HL_REVEAL_MS
import com.nbeck.app.ui.screens.HigherLowerViewModel
import com.nbeck.app.ui.screens.HlPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HigherLowerVmTest {
    private val looper get() = shadowOf(android.os.Looper.getMainLooper())

    private fun ready(): HigherLowerViewModel {
        val vm = HigherLowerViewModel(ApplicationProvider.getApplicationContext<NbeckApp>())
        repeat(100) {
            looper.idle()
            if (vm.ui.value.phase == HlPhase.PLAYING) return vm
            Thread.sleep(30)
        }
        error("game didn't load")
    }

    private fun wrongFor(vm: HigherLowerViewModel): HlChoice {
        val g = vm.ui.value.game!!
        val t = g.value(g.top); val b = g.value(g.bottom)
        return when {
            b > t -> HlChoice.LOWER
            b < t -> if (g.capped) HlChoice.EQUAL else HlChoice.HIGHER
            else -> HlChoice.LOWER
        }
    }

    @Test fun fullGameFlow() {
        val vm = ready()
        val game = vm.ui.value.game!!
        val category = game.category
        repeat(HL_ROUNDS) { i ->
            val top = game.top; val bottom = game.bottom
            vm.answer(wrongFor(vm))
            // Revealed at once, with the verdict…
            assertEquals(HlPhase.REVEAL, vm.ui.value.phase)
            assertEquals(i + 1, vm.ui.value.last!!.number)
            // …and further taps during the reveal do nothing (10: no double answers).
            vm.answer(HlChoice.HIGHER); vm.answer(HlChoice.LOWER)
            assertEquals(i + 1, game.rounds.size)
            // Still revealed just before one second (9)…
            looper.idleFor(Duration.ofMillis(HL_REVEAL_MS - 50))
            assertEquals(HlPhase.REVEAL, vm.ui.value.phase)
            // …then the next comparison, or the summary after the 20th.
            looper.idleFor(Duration.ofMillis(100))
            if (i < HL_ROUNDS - 1) {
                assertEquals(HlPhase.PLAYING, vm.ui.value.phase)   // 7: a wrong answer doesn't end it
                assertEquals(bottom.id, game.top.id)               // 8: bottom became top
                assertNotEquals(top.id, game.top.id)
            }
            assertEquals(category, game.category)                  // 2: same category throughout
        }
        assertEquals(HlPhase.SUMMARY, vm.ui.value.phase)           // 1, 12: summary after exactly 20
        assertEquals(HL_ROUNDS, game.rounds.size)
        assertEquals(0, game.score)
        vm.answer(HlChoice.HIGHER)
        assertEquals(HL_ROUNDS, game.rounds.size)                  // no 21st comparison

        // 13: new game — score reset, new category, fresh players.
        vm.newGame()
        val next = vm.ui.value.game!!
        assertEquals(HlPhase.PLAYING, vm.ui.value.phase)
        assertEquals(0, next.score); assertEquals(0, next.rounds.size)
        assertNotEquals(category, next.category)
        assertTrue((next.players.map { it.id } intersect game.players.map { it.id }.toSet()).isEmpty())
    }
}
