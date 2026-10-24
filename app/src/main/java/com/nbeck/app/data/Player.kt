package com.nbeck.app.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.Period

@Serializable
data class Player(
    val id: String,
    val name: String,
    val firstName: String = "",
    val lastName: String = "",
    val teamId: Int,
    /** One of G, F, C, G-F, F-G, F-C, C-F. */
    val position: String,
    /** ISO yyyy-MM-dd. */
    val birthDate: String,
    /** Null when the player has no number yet — shown as "—" and always graded gray. */
    val jersey: String? = null,
    val country: String? = null,
    val headshot: String? = null,
    val ppg: Double? = null,
    val rpg: Double? = null,
    val apg: Double? = null,
    val gp: Int = 0,
) {
    val team: Team get() = Teams[teamId]
    val jerseyNumber: Int? get() = jersey?.toIntOrNull()
    fun ageOn(date: LocalDate): Int = Period.between(LocalDate.parse(birthDate), date).years
    val displayLast: String get() = lastName.ifBlank { name.substringAfterLast(' ') }
}

@Serializable
data class PlayerBundle(
    val source: String = "",
    val fetchedAt: String = "",
    val players: List<Player>,
)
