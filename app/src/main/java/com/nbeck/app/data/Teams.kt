package com.nbeck.app.data

import androidx.compose.ui.graphics.Color

enum class Conference { EAST, WEST }

enum class Division(val conference: Conference, val label: String, val short: String) {
    ATLANTIC(Conference.EAST, "Atlantic", "ATLC"),
    CENTRAL(Conference.EAST, "Central", "CENT"),
    SOUTHEAST(Conference.EAST, "Southeast", "SE"),
    NORTHWEST(Conference.WEST, "Northwest", "NW"),
    PACIFIC(Conference.WEST, "Pacific", "PAC"),
    SOUTHWEST(Conference.WEST, "Southwest", "SW"),
}

/** One NBA franchise. [espnId] is the id used by the ESPN API (see EspnSource). */
data class Team(
    val espnId: Int,
    val abbr: String,
    val city: String,
    val name: String,
    val division: Division,
    val primary: Long,
    val secondary: Long,
) {
    val fullName get() = "$city $name"
    val conference get() = division.conference
    val color get() = Color(primary)
    val accent get() = Color(secondary)
    val logoUrl get() = "https://a.espncdn.com/i/teamlogos/nba/500/${espnSlug}.png"
    private val espnSlug get() = when (abbr) {
        "GSW" -> "gs"; "NOP" -> "no"; "NYK" -> "ny"; "SAS" -> "sa"; "UTA" -> "utah"; "WAS" -> "wsh"
        else -> abbr.lowercase()
    }
}

object Teams {
    val all: List<Team> = listOf(
        Team(2, "BOS", "Boston", "Celtics", Division.ATLANTIC, 0xFF007A33, 0xFFBA9653),
        Team(17, "BKN", "Brooklyn", "Nets", Division.ATLANTIC, 0xFF1A1A1A, 0xFFF2F2F2),
        Team(18, "NYK", "New York", "Knicks", Division.ATLANTIC, 0xFF006BB6, 0xFFF58426),
        Team(20, "PHI", "Philadelphia", "76ers", Division.ATLANTIC, 0xFF006BB6, 0xFFED174C),
        Team(28, "TOR", "Toronto", "Raptors", Division.ATLANTIC, 0xFFCE1141, 0xFF1A1A1A),
        Team(4, "CHI", "Chicago", "Bulls", Division.CENTRAL, 0xFFCE1141, 0xFF1A1A1A),
        Team(5, "CLE", "Cleveland", "Cavaliers", Division.CENTRAL, 0xFF860038, 0xFFFDBB30),
        Team(8, "DET", "Detroit", "Pistons", Division.CENTRAL, 0xFFC8102E, 0xFF1D42BA),
        Team(11, "IND", "Indiana", "Pacers", Division.CENTRAL, 0xFF002D62, 0xFFFDBB30),
        Team(15, "MIL", "Milwaukee", "Bucks", Division.CENTRAL, 0xFF00471B, 0xFFEEE1C6),
        Team(1, "ATL", "Atlanta", "Hawks", Division.SOUTHEAST, 0xFFC8102E, 0xFFFDB927),
        Team(30, "CHA", "Charlotte", "Hornets", Division.SOUTHEAST, 0xFF1D1160, 0xFF00788C),
        Team(14, "MIA", "Miami", "Heat", Division.SOUTHEAST, 0xFF98002E, 0xFFF9A01B),
        Team(19, "ORL", "Orlando", "Magic", Division.SOUTHEAST, 0xFF0077C0, 0xFFC4CED4),
        Team(27, "WAS", "Washington", "Wizards", Division.SOUTHEAST, 0xFF002B5C, 0xFFE31837),
        Team(7, "DEN", "Denver", "Nuggets", Division.NORTHWEST, 0xFF0E2240, 0xFFFEC524),
        Team(16, "MIN", "Minnesota", "Timberwolves", Division.NORTHWEST, 0xFF0C2340, 0xFF78BE20),
        Team(25, "OKC", "Oklahoma City", "Thunder", Division.NORTHWEST, 0xFF007AC1, 0xFFEF3B24),
        Team(22, "POR", "Portland", "Trail Blazers", Division.NORTHWEST, 0xFFE03A3E, 0xFF1A1A1A),
        Team(26, "UTA", "Utah", "Jazz", Division.NORTHWEST, 0xFF4B2A85, 0xFFF2F2F2),
        Team(9, "GSW", "Golden State", "Warriors", Division.PACIFIC, 0xFF1D428A, 0xFFFFC72C),
        Team(12, "LAC", "LA", "Clippers", Division.PACIFIC, 0xFFC8102E, 0xFF1D428A),
        Team(13, "LAL", "Los Angeles", "Lakers", Division.PACIFIC, 0xFF552583, 0xFFFDB927),
        Team(21, "PHX", "Phoenix", "Suns", Division.PACIFIC, 0xFF1D1160, 0xFFE56020),
        Team(23, "SAC", "Sacramento", "Kings", Division.PACIFIC, 0xFF5A2D81, 0xFF63727A),
        Team(6, "DAL", "Dallas", "Mavericks", Division.SOUTHWEST, 0xFF00538C, 0xFFB8C4CA),
        Team(10, "HOU", "Houston", "Rockets", Division.SOUTHWEST, 0xFFCE1141, 0xFFC4CED4),
        Team(29, "MEM", "Memphis", "Grizzlies", Division.SOUTHWEST, 0xFF5D76A9, 0xFF12173F),
        Team(3, "NOP", "New Orleans", "Pelicans", Division.SOUTHWEST, 0xFF0C2340, 0xFFC8102E),
        Team(24, "SAS", "San Antonio", "Spurs", Division.SOUTHWEST, 0xFF55595C, 0xFFC4CED4),
    )

    private val byId = all.associateBy { it.espnId }
    operator fun get(espnId: Int): Team = byId.getValue(espnId)
    fun find(espnId: Int): Team? = byId[espnId]
    fun inDivision(d: Division) = all.filter { it.division == d }
}
