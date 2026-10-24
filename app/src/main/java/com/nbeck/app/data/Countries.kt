package com.nbeck.app.data

/** Country names as ESPN spells them -> ISO 3166 alpha-2, for flag + short code. */
object Countries {
    private val iso = mapOf(
        "USA" to "US", "Canada" to "CA", "France" to "FR", "Australia" to "AU", "Germany" to "DE",
        "Nigeria" to "NG", "Spain" to "ES", "England" to "GB", "United Kingdom" to "GB", "Bahamas" to "BS",
        "Netherlands" to "NL", "Bosnia & Herzegovina" to "BA", "Bosnia and Herzegovina" to "BA",
        "Switzerland" to "CH", "Cameroon" to "CM", "Senegal" to "SN", "Russia" to "RU", "Serbia" to "RS",
        "Dominican Republic" to "DO", "Georgia" to "GE", "Italy" to "IT", "Lithuania" to "LT",
        "Belgium" to "BE", "Israel" to "IL", "Estonia" to "EE", "Portugal" to "PT", "Croatia" to "HR",
        "Guinea" to "GN", "Latvia" to "LV", "Brazil" to "BR", "New Zealand" to "NZ", "Turkey" to "TR",
        "Türkiye" to "TR", "Congo" to "CG", "Japan" to "JP", "Slovenia" to "SI", "Mexico" to "MX",
        "Greece" to "GR", "Sweden" to "SE", "Jamaica" to "JM", "Democratic Republic of Congo" to "CD",
        "DR Congo" to "CD", "South Sudan" to "SS", "Sudan" to "SD", "Montenegro" to "ME", "Ukraine" to "UA",
        "Finland" to "FI", "Austria" to "AT", "Czech Republic" to "CZ", "Czechia" to "CZ", "Poland" to "PL",
        "Argentina" to "AR", "Puerto Rico" to "PR", "Haiti" to "HT", "Mali" to "ML", "Angola" to "AO",
        "Egypt" to "EG", "Ghana" to "GH", "Gabon" to "GA", "South Korea" to "KR", "China" to "CN",
        "Philippines" to "PH", "Venezuela" to "VE", "Uruguay" to "UY", "Denmark" to "DK", "Norway" to "NO",
        "Iceland" to "IS", "Hungary" to "HU", "Bulgaria" to "BG", "Romania" to "RO", "Ireland" to "IE",
        "Scotland" to "GB", "Wales" to "GB", "St. Lucia" to "LC", "Saint Lucia" to "LC", "Tunisia" to "TN",
        "Cape Verde" to "CV", "Central African Republic" to "CF", "Ivory Coast" to "CI", "Kenya" to "KE",
        "Macedonia" to "MK", "North Macedonia" to "MK", "Lebanon" to "LB", "Iran" to "IR", "Taiwan" to "TW",
        "U.S. Virgin Islands" to "VI", "Virgin Islands" to "VI", "Trinidad and Tobago" to "TT", "Belize" to "BZ",
        "Panama" to "PA", "Colombia" to "CO", "Cuba" to "CU", "Chile" to "CL", "Peru" to "PE", "Albania" to "AL",
        "Kosovo" to "XK", "Belarus" to "BY", "Slovakia" to "SK", "Cyprus" to "CY", "Luxembourg" to "LU",
    )

    fun code(country: String?): String? = country?.let { iso[it] }

    /** Regional-indicator flag emoji, or null for unknown countries. */
    fun flag(country: String?): String? {
        val c = code(country) ?: return null
        if (c == "XK") return null
        return c.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
    }

    /** Three-letter label for the tile. */
    fun short(country: String?): String = when {
        country == null -> "—"
        country == "USA" -> "USA"
        else -> country.filter { it.isLetter() }.take(3).uppercase()
    }
}
