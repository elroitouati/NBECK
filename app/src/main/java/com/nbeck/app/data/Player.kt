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
    /** G, F or C. */
    val position: String,
    /** ISO yyyy-MM-dd. */
    val birthDate: String,
    /** Null when the player has no number yet — shown as "—" and always graded gray. */
    val jersey: String? = null,
    val country: String? = null,
    val headshot: String? = null,
    /** Year the player was drafted; null = undrafted ("UND"). */
    val draftYear: Int? = null,
    /** Won at least one NBA championship. */
    val ring: Boolean = false,
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
    /** Bumped when fields are added; older caches are ignored (v2 added draft year and ring). */
    val schema: Int = 1,
    val source: String = "",
    val fetchedAt: String = "",
    val players: List<Player>,
)

const val PLAYER_SCHEMA = 2
