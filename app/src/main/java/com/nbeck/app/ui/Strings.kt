package com.nbeck.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.nbeck.app.data.Language
import com.nbeck.app.game.Achievement
import com.nbeck.app.game.Category

/** All UI copy, Hebrew and English. Player/team names and abbreviations stay English in both. */
interface Strings {
    val tagline: String
    val daily: String
    val dailySub: String
    val dailyDone: String
    val free: String
    val freeSub: String
    val stats: String
    val achievements: String
    val settings: String
    val howTo: String
    val nextPlayerIn: String
    val loading: String
    val searchHint: String
    val noMatches: String
    val guessOf: String
    val divisionsBoard: String
    val east: String
    val west: String
    val win: String
    val loss: String
    val lossSub: String
    fun winSub(n: Int): String
    val playAgain: String
    val backHome: String
    val played: String
    val winPct: String
    val streak: String
    val maxStreak: String
    val avgGuesses: String
    val distribution: String
    val noGamesYet: String
    val playFirst: String
    val language: String
    val theme: String
    val themeDark: String
    val themeLight: String
    val themeSystem: String
    val sounds: String
    val haptics: String
    val dataSection: String
    fun dataInfo(count: Int): String
    val refreshNow: String
    val refreshing: String
    val refreshed: String
    val refreshFailed: String
    val achievementUnlocked: String
    fun category(c: Category): String
    fun achievementTitle(a: Achievement): String
    fun achievementDesc(a: Achievement): String
    val howToTitle: String
    val howToIntro: String
    val howToExample: String
    val howToGreen: String
    val howToYellow: String
    val howToGray: String
    val howToArrows: String
    val howToBoard: String
    val howToModes: String
    val letsPlay: String
    val ppg: String
    val rpg: String
    val apg: String
    val age: String
    val pos: String
    val number: String
    val country: String
    val seasonStats: String
    val noSeasonStats: String
    val legendExact: String
    val legendClose: String
    val legendFar: String
    val back: String
    val close: String
    val expand: String
    val collapse: String
    val locked: String
    val giveUp: String
    val giveUpTitle: String
    val giveUpBody: String
    val keepPlaying: String
    val gaveUp: String
    val draft: String
    val ring: String
    val undrafted: String
    val champion: String
    val noTitle: String
}

object He : Strings {
    override val tagline = "נחש את שחקן ה-NBA"
    override val daily = "האתגר היומי"
    override val dailySub = "שחקן אחד לכל החברים, פעם ביום"
    override val dailyDone = "שיחקת היום"
    override val free = "משחק חופשי"
    override val freeSub = "שחקן אקראי, בלי הגבלה"
    override val stats = "סטטיסטיקות"
    override val achievements = "הישגים"
    override val settings = "הגדרות"
    override val howTo = "איך משחקים"
    override val nextPlayerIn = "השחקן הבא בעוד"
    override val loading = "טוען שחקנים…"
    override val searchHint = "הקלד שם של שחקן"
    override val noMatches = "אין שחקן בשם הזה. נסה שם פרטי או שם משפחה"
    override val guessOf = "ניחוש"
    override val divisionsBoard = "טבלת דיביז'נים"
    override val east = "מזרח"
    override val west = "מערב"
    override val win = "קלעת!"
    override val loss = "נגמר הזמן"
    override val lossSub = "השחקן הסודי היה"
    override fun winSub(n: Int) = if (n == 1) "בניחוש הראשון!" else "ב-$n ניחושים"
    override val playAgain = "שחק שוב"
    override val backHome = "חזרה למסך הבית"
    override val played = "משחקים"
    override val winPct = "% ניצחונות"
    override val streak = "רצף נוכחי"
    override val maxStreak = "רצף שיא"
    override val avgGuesses = "ממוצע ניחושים"
    override val distribution = "בכמה ניחושים ניצחתי"
    override val noGamesYet = "עוד לא שיחקת במצב הזה"
    override val playFirst = "שחק עכשיו"
    override val language = "שפה"
    override val theme = "ערכת צבע"
    override val themeDark = "זירה בלילה"
    override val themeLight = "בהיר"
    override val themeSystem = "לפי המכשיר"
    override val sounds = "צלילים"
    override val haptics = "רטט"
    override val dataSection = "נתוני שחקנים"
    override fun dataInfo(count: Int) = "$count שחקנים מהעונה הנוכחית · מקור: ESPN · מתעדכן פעם ביום"
    override val refreshNow = "רענן עכשיו"
    override val refreshing = "מרענן…"
    override val refreshed = "הנתונים עודכנו"
    override val refreshFailed = "אין חיבור. המשחק ממשיך עם הנתונים השמורים"
    override val achievementUnlocked = "הישג חדש"
    override fun category(c: Category) = when (c) {
        Category.TEAM -> "קבוצה"
        Category.DIVISION -> "דיביז'ן"
        Category.POSITION -> "עמדה"
        Category.AGE -> "גיל"
        Category.NUMBER -> "מספר"
        Category.COUNTRY -> "מדינה"
        Category.DRAFT -> "דראפט"
        Category.RING -> "טבעת"
    }
    override fun achievementTitle(a: Achievement) = when (a) {
        Achievement.FIRST_WIN -> "סל ראשון"
        Achievement.FIRST_TRY -> "ניחוש ראשון"
        Achievement.THREE_POINTER -> "שלשה"
        Achievement.STREAK_3 -> "רצף 3"
        Achievement.STREAK_7 -> "רצף 7"
        Achievement.STREAK_30 -> "רצף 30"
        Achievement.NO_YELLOW -> "בלי צהוב"
        Achievement.BUZZER_BEATER -> "על הבאזר"
        Achievement.WINS_10 -> "10 ניצחונות"
        Achievement.WINS_50 -> "50 ניצחונות"
        Achievement.WINS_100 -> "100 ניצחונות"
        Achievement.ALL_TEAMS -> "כל הליגה"
        Achievement.GLOBETROTTER -> "גלובטרוטר"
    }
    override fun achievementDesc(a: Achievement) = when (a) {
        Achievement.FIRST_WIN -> "נצח במשחק הראשון שלך"
        Achievement.FIRST_TRY -> "נצח בניחוש הראשון"
        Achievement.THREE_POINTER -> "נצח ב-3 ניחושים או פחות"
        Achievement.STREAK_3 -> "3 ימים ברצף של ניצחון ביומי"
        Achievement.STREAK_7 -> "7 ימים ברצף של ניצחון ביומי"
        Achievement.STREAK_30 -> "30 ימים ברצף של ניצחון ביומי"
        Achievement.NO_YELLOW -> "נצח בלי אף משבצת צהובה"
        Achievement.BUZZER_BEATER -> "נצח בניחוש האחרון"
        Achievement.WINS_10 -> "10 ניצחונות בסך הכול"
        Achievement.WINS_50 -> "50 ניצחונות בסך הכול"
        Achievement.WINS_100 -> "100 ניצחונות בסך הכול"
        Achievement.ALL_TEAMS -> "נחש שחקן מכל 30 הקבוצות"
        Achievement.GLOBETROTTER -> "נחש שחקנים מ-15 מדינות שונות"
    }
    override val howToTitle = "איך משחקים"
    override val howToIntro = "יש לך 8 ניחושים למצוא את שחקן ה-NBA הסודי. הקלד שם, בחר מהרשימה, וכל ניחוש מראה כמה קרוב היית."
    override val howToExample = "לדוגמה, ניחשת את השחקן הזה:"
    override val howToGreen = "מדויק"
    override val howToYellow = "קרוב — דיביז'ן אחר באותו כנס, או הפרש של עד 3 בגיל, במספר או בשנת הדראפט"
    override val howToGray = "רחוק"
    override val howToArrows = "החץ בגיל, במספר ובדראפט מראה אם התשובה גבוהה או נמוכה יותר. טבעת: ✓ זכה באליפות, ✗ לא זכה. UND = לא נבחר בדראפט"
    override val howToBoard = "טבלת הדיביז'נים בתחתית מעמעמת קבוצות שכבר לא אפשריות"
    override val howToModes = "ביומי — אותו שחקן לכל החברים. בחופשי — כמה שבא לך."
    override val letsPlay = "יאללה, למשחק"
    override val ppg = "נק'"
    override val rpg = "ריב'"
    override val apg = "אס'"
    override val age = "גיל"
    override val pos = "עמדה"
    override val number = "מספר"
    override val country = "מדינה"
    override val seasonStats = "ממוצעי עונה"
    override val noSeasonStats = "עוד לא שיחק העונה"
    override val legendExact = "מדויק"
    override val legendClose = "קרוב"
    override val legendFar = "רחוק"
    override val back = "חזרה"
    override val close = "סגור"
    override val expand = "הצג טבלת דיביז'נים"
    override val collapse = "הסתר טבלת דיביז'נים"
    override val locked = "נעול"
    override val giveUp = "ויתור"
    override val giveUpTitle = "לוותר על המשחק?"
    override val giveUpBody = "השחקן הסודי ייחשף והמשחק ייחשב כהפסד."
    override val keepPlaying = "המשך לשחק"
    override val gaveUp = "ויתרת"
    override val draft = "דראפט"
    override val ring = "טבעת"
    override val undrafted = "UND"
    override val champion = "אלוף"
    override val noTitle = "אין"
}

object En : Strings {
    override val tagline = "Guess the NBA player"
    override val daily = "Daily challenge"
    override val dailySub = "One player for all your friends, once a day"
    override val dailyDone = "Played today"
    override val free = "Free play"
    override val freeSub = "Random player, no limit"
    override val stats = "Statistics"
    override val achievements = "Achievements"
    override val settings = "Settings"
    override val howTo = "How to play"
    override val nextPlayerIn = "Next player in"
    override val loading = "Loading players…"
    override val searchHint = "Type a player's name"
    override val noMatches = "No player by that name. Try a first or last name"
    override val guessOf = "Guess"
    override val divisionsBoard = "Divisions"
    override val east = "East"
    override val west = "West"
    override val win = "Bucket!"
    override val loss = "Out of time"
    override val lossSub = "The secret player was"
    override fun winSub(n: Int) = if (n == 1) "First guess!" else "In $n guesses"
    override val playAgain = "Play again"
    override val backHome = "Back to home"
    override val played = "Played"
    override val winPct = "Win %"
    override val streak = "Current streak"
    override val maxStreak = "Best streak"
    override val avgGuesses = "Avg. guesses"
    override val distribution = "Guess distribution"
    override val noGamesYet = "No games in this mode yet"
    override val playFirst = "Play now"
    override val language = "Language"
    override val theme = "Theme"
    override val themeDark = "Arena night"
    override val themeLight = "Light"
    override val themeSystem = "System"
    override val sounds = "Sounds"
    override val haptics = "Vibration"
    override val dataSection = "Player data"
    override fun dataInfo(count: Int) = "$count current-season players · Source: ESPN · Updated daily"
    override val refreshNow = "Refresh now"
    override val refreshing = "Refreshing…"
    override val refreshed = "Data updated"
    override val refreshFailed = "No connection. Playing on with saved data"
    override val achievementUnlocked = "Achievement unlocked"
    override fun category(c: Category) = when (c) {
        Category.TEAM -> "Team"
        Category.DIVISION -> "Div"
        Category.POSITION -> "Pos"
        Category.AGE -> "Age"
        Category.NUMBER -> "No."
        Category.COUNTRY -> "Ctry"
        Category.DRAFT -> "Draft"
        Category.RING -> "Ring"
    }
    override fun achievementTitle(a: Achievement) = when (a) {
        Achievement.FIRST_WIN -> "First bucket"
        Achievement.FIRST_TRY -> "First try"
        Achievement.THREE_POINTER -> "Three-pointer"
        Achievement.STREAK_3 -> "3-day streak"
        Achievement.STREAK_7 -> "7-day streak"
        Achievement.STREAK_30 -> "30-day streak"
        Achievement.NO_YELLOW -> "No yellow"
        Achievement.BUZZER_BEATER -> "Buzzer beater"
        Achievement.WINS_10 -> "10 wins"
        Achievement.WINS_50 -> "50 wins"
        Achievement.WINS_100 -> "100 wins"
        Achievement.ALL_TEAMS -> "Whole league"
        Achievement.GLOBETROTTER -> "Globetrotter"
    }
    override fun achievementDesc(a: Achievement) = when (a) {
        Achievement.FIRST_WIN -> "Win your first game"
        Achievement.FIRST_TRY -> "Win on the first guess"
        Achievement.THREE_POINTER -> "Win in 3 guesses or fewer"
        Achievement.STREAK_3 -> "Win the daily 3 days in a row"
        Achievement.STREAK_7 -> "Win the daily 7 days in a row"
        Achievement.STREAK_30 -> "Win the daily 30 days in a row"
        Achievement.NO_YELLOW -> "Win without a single yellow tile"
        Achievement.BUZZER_BEATER -> "Win on the last guess"
        Achievement.WINS_10 -> "10 wins in total"
        Achievement.WINS_50 -> "50 wins in total"
        Achievement.WINS_100 -> "100 wins in total"
        Achievement.ALL_TEAMS -> "Guess a player from all 30 teams"
        Achievement.GLOBETROTTER -> "Guess players from 15 countries"
    }
    override val howToTitle = "How to play"
    override val howToIntro = "You have 8 guesses to find the secret NBA player. Type a name, pick from the list, and each guess shows how close you are."
    override val howToExample = "Say you guessed this player:"
    override val howToGreen = "Exact"
    override val howToYellow = "Close — same conference, or within 3 in age, number or draft year"
    override val howToGray = "Far"
    override val howToArrows = "Arrows on age, number and draft show whether the answer is higher or lower. Ring: ✓ won a title, ✗ didn't. UND = undrafted"
    override val howToBoard = "The division board at the bottom dims teams that are ruled out"
    override val howToModes = "Daily — the same player for all your friends. Free — as many as you like."
    override val letsPlay = "Let's play"
    override val ppg = "PTS"
    override val rpg = "REB"
    override val apg = "AST"
    override val age = "Age"
    override val pos = "Pos"
    override val number = "No."
    override val country = "Country"
    override val seasonStats = "Season averages"
    override val noSeasonStats = "Hasn't played this season yet"
    override val legendExact = "Exact"
    override val legendClose = "Close"
    override val legendFar = "Far"
    override val back = "Back"
    override val close = "Close"
    override val expand = "Show division board"
    override val collapse = "Hide division board"
    override val locked = "Locked"
    override val giveUp = "Give up"
    override val giveUpTitle = "Give up this game?"
    override val giveUpBody = "The secret player will be revealed and the game counts as a loss."
    override val keepPlaying = "Keep playing"
    override val gaveUp = "You gave up"
    override val draft = "Draft"
    override val ring = "Ring"
    override val undrafted = "UND"
    override val champion = "Champion"
    override val noTitle = "None"
}

fun stringsFor(l: Language): Strings = if (l == Language.HE) He else En

val LocalStrings = staticCompositionLocalOf<Strings> { He }
