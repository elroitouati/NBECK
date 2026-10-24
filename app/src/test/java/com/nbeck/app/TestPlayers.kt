package com.nbeck.app

import com.nbeck.app.data.Player

/** Team ids are ESPN ids: 2 BOS (Atlantic/E), 18 NYK (Atlantic/E), 4 CHI (Central/E), 13 LAL (Pacific/W), 9 GSW (Pacific/W). */
fun player(
    id: String = "1",
    name: String = "Test Player",
    team: Int = 2,
    pos: String = "G",
    born: String = "2000-01-01",
    jersey: String? = "10",
    country: String? = "USA",
    ppg: Double? = null,
) = Player(
    id = id, name = name, firstName = name.substringBefore(' '), lastName = name.substringAfter(' '),
    teamId = team, position = pos, birthDate = born, jersey = jersey, country = country, ppg = ppg,
)
