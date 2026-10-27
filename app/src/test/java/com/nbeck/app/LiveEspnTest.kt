package com.nbeck.app

import com.nbeck.app.data.EspnSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Hits the real ESPN API. Run with -Pnetwork; skipped otherwise. */
class LiveEspnTest {
    @Test fun refreshUsesNumberWornInLatestGame() = runBlocking {
        assumeTrue(System.getProperty("network") == "true")
        val bundle = EspnSource().fetchAll(known = emptyMap(), ringIds = emptySet())
        val byName = bundle.players.associateBy { it.name }
        println("live players: ${bundle.players.size}")
        assertTrue(bundle.players.size > 400)
        assertEquals("1", byName.getValue("Jamal Shead").jersey)
    }
}
