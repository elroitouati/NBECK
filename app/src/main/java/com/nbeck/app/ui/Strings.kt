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
    val archive: String
    val archiveSub: String
    val today: String
    val notPlayed: String
    val play: String
    val deleteData: String
    val resetSection: String
    val deleteTitle: String
    val deleteBody: String
    val delete: String
    val cancel: String
    val deleted: String
    val locale: java.util.Locale
    fun whatsNewTitle(version: String): String
    val gotIt: String
    val updates: String
    val currentVersion: String
    val versionHistory: String
    val installed: String
    fun updateAvailable(version: String): String
    val downloadUpdate: String
    val upToDate: String
    val updateTitle: String
    val updateNow: String
    val later: String
    val account: String
    val signInTitle: String
    val signInBody: String
    val signInGoogle: String
    val signInFailed: String
    val signOut: String
    val profileTitle: String
    val nickname: String
    val nicknameHint: String
    val nicknameTaken: String
    val nicknameInvalid: String
    val favoriteTeam: String
    val noTeam: String
    val showPhoto: String
    val save: String
    val editProfile: String
    val friendCode: String
    val leaderboards: String
    val leaderboardsSub: String
    val todayTab: String
    val leagueTab: String
    val you: String
    val noResultsToday: String
    val noLeagueYet: String
    val loadFailed: String
    val retry: String
    val pointsShort: String
    val leagueRules: String
    val dailyReminder: String
    val dailyReminderSub: String
    val gamesPlayed: String
    val profileLoadFailed: String
    fun leagueMonth(month: java.time.YearMonth): String
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
        Achievement.SECOND_TRY -> "ריבאונד התקפה"
        Achievement.HOT_START -> "פתיחה חמה"
        Achievement.FREE_STREAK_5 -> "יד חמה"
        Achievement.FREE_WINS_25 -> "מכור למשחק"
        Achievement.HISTORIAN -> "היסטוריון"
        Achievement.BLUE_WHITE -> "כחול-לבן"
        Achievement.INTERNATIONAL_10 -> "שגריר"
        Achievement.ROOKIE_HUNTER -> "צייד רוקים"
        Achievement.OLD_SCHOOL -> "אולד סקול"
        Achievement.RING_HUNTER -> "צייד טבעות"
        Achievement.UNDRAFTED -> "אנדרדוג"
        Achievement.NIGHT_OWL -> "ינשוף לילה"
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
        Achievement.SECOND_TRY -> "נצח בדיוק בניחוש השני"
        Achievement.HOT_START -> "5 משבצות ירוקות או יותר כבר בניחוש הראשון, ואז ניצחון"
        Achievement.FREE_STREAK_5 -> "5 ניצחונות ברצף במשחק החופשי"
        Achievement.FREE_WINS_25 -> "25 ניצחונות במשחק החופשי"
        Achievement.HISTORIAN -> "סיים 5 אתגרים קודמים"
        Achievement.BLUE_WHITE -> "נצח כשהשחקן הסודי ישראלי"
        Achievement.INTERNATIONAL_10 -> "10 ניצחונות על שחקנים סודיים מחוץ לארה\"ב"
        Achievement.ROOKIE_HUNTER -> "נצח כשהשחקן הסודי רוקי מהדראפט האחרון"
        Achievement.OLD_SCHOOL -> "נצח כשהשחקן הסודי בן 35 ומעלה"
        Achievement.RING_HUNTER -> "נצח כשלשחקן הסודי יש טבעת"
        Achievement.UNDRAFTED -> "נצח כשהשחקן הסודי לא נבחר בדראפט"
        Achievement.NIGHT_OWL -> "נצח בין חצות לחמש בבוקר"
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
    override val archive = "אתגרים קודמים"
    override val archiveSub = "האתגר היומי של 30 הימים האחרונים"
    override val today = "היום"
    override val notPlayed = "עוד לא שיחקת"
    override val play = "שחק"
    override val deleteData = "מחק את הנתונים שלי"
    override val resetSection = "איפוס"
    override val deleteTitle = "למחוק את כל הנתונים?"
    override val deleteBody = "כל הסטטיסטיקות, ההישגים והמשחקים יימחקו ויתאפסו. אי אפשר לשחזר אותם."
    override val delete = "מחק הכול"
    override val cancel = "ביטול"
    override val deleted = "הנתונים נמחקו"
    override val locale: java.util.Locale = java.util.Locale.forLanguageTag("he")
    override fun whatsNewTitle(version: String) = "מה חדש בגרסה $version"
    override val gotIt = "יאללה"
    override val updates = "עדכונים וגרסאות"
    override val currentVersion = "הגרסה המותקנת"
    override val versionHistory = "היסטוריית גרסאות"
    override val installed = "מותקנת"
    override fun updateAvailable(version: String) = "גרסה חדשה זמינה: $version"
    override val downloadUpdate = "הורד והתקן"
    override val upToDate = "יש לך את הגרסה האחרונה"
    override val updateTitle = "יש עדכון חדש!"
    override val updateNow = "עדכן עכשיו"
    override val later = "אחר כך"
    override val account = "חשבון"
    override val signInTitle = "התחבר כדי להתחרות"
    override val signInBody = "עם חשבון אתה מופיע בטבלת האתגר היומי ובליגה החודשית מול כל השחקנים. המשחק עצמו ממשיך לעבוד גם בלי חשבון."
    override val signInGoogle = "התחבר עם Google"
    override val signInFailed = "ההתחברות לא הצליחה. נסה שוב"
    override val signOut = "התנתק"
    override val profileTitle = "הפרופיל שלך"
    override val nickname = "כינוי"
    override val nicknameHint = "2–16 תווים, כך יראו אותך בטבלאות"
    override val nicknameTaken = "הכינוי הזה תפוס. נסה אחר"
    override val nicknameInvalid = "כינוי של 2–16 אותיות, ספרות או רווחים"
    override val favoriteTeam = "קבוצה אהובה"
    override val noTeam = "בלי"
    override val showPhoto = "הצג את תמונת הפרופיל שלי מ-Google"
    override val save = "שמור"
    override val editProfile = "ערוך פרופיל"
    override val friendCode = "קוד חבר"
    override val leaderboards = "טבלאות"
    override val leaderboardsSub = "טבלת היום וליגה חודשית"
    override val todayTab = "היום"
    override val leagueTab = "ליגה"
    override val you = "אתה"
    override val noResultsToday = "עוד אף אחד לא סיים את האתגר של היום. תהיה הראשון!"
    override val noLeagueYet = "הליגה של החודש עוד ריקה. כל אתגר יומי שתסיים מכניס נקודות"
    override val loadFailed = "לא הצלחנו לטעון את הטבלה. בדוק חיבור לאינטרנט"
    override val retry = "נסה שוב"
    override val pointsShort = "נק'"
    override val leagueRules = "ניצחון בניחוש 1 = 10 נק', כל ניחוש נוסף פחות 1, הפסד = 0. הטבלה מתאפסת בתחילת כל חודש"
    override val dailyReminder = "תזכורת יומית"
    override val dailyReminderSub = "ב-19:30, אם עוד לא שיחקת את היומי"
    override val gamesPlayed = "משחקים"
    override val profileLoadFailed = "התחברת, אבל לא הצלחנו לטעון את הפרופיל"
    override fun leagueMonth(month: java.time.YearMonth) =
        "ליגת " + month.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, locale)
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
        Achievement.SECOND_TRY -> "Putback"
        Achievement.HOT_START -> "Hot start"
        Achievement.FREE_STREAK_5 -> "Heat check"
        Achievement.FREE_WINS_25 -> "Gym rat"
        Achievement.HISTORIAN -> "Historian"
        Achievement.BLUE_WHITE -> "Blue and white"
        Achievement.INTERNATIONAL_10 -> "Ambassador"
        Achievement.ROOKIE_HUNTER -> "Rookie hunter"
        Achievement.OLD_SCHOOL -> "Old school"
        Achievement.RING_HUNTER -> "Ring chaser"
        Achievement.UNDRAFTED -> "Underdog"
        Achievement.NIGHT_OWL -> "Night owl"
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
        Achievement.SECOND_TRY -> "Win on exactly the second guess"
        Achievement.HOT_START -> "5+ green tiles on your first guess, then win"
        Achievement.FREE_STREAK_5 -> "Win 5 free-play games in a row"
        Achievement.FREE_WINS_25 -> "Win 25 free-play games"
        Achievement.HISTORIAN -> "Finish 5 past challenges"
        Achievement.BLUE_WHITE -> "Win when the secret player is Israeli"
        Achievement.INTERNATIONAL_10 -> "Win 10 times with a secret player from outside the USA"
        Achievement.ROOKIE_HUNTER -> "Win when the secret player is a rookie from the latest draft"
        Achievement.OLD_SCHOOL -> "Win when the secret player is 35 or older"
        Achievement.RING_HUNTER -> "Win when the secret player has a ring"
        Achievement.UNDRAFTED -> "Win when the secret player went undrafted"
        Achievement.NIGHT_OWL -> "Win between midnight and 5 a.m."
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
    override val archive = "Past challenges"
    override val archiveSub = "The daily challenge of the last 30 days"
    override val today = "Today"
    override val notPlayed = "Not played yet"
    override val play = "Play"
    override val deleteData = "Delete my data"
    override val resetSection = "Reset"
    override val deleteTitle = "Delete all your data?"
    override val deleteBody = "All statistics, achievements and games will be erased and reset. This can't be undone."
    override val delete = "Delete everything"
    override val cancel = "Cancel"
    override val deleted = "Your data was deleted"
    override val locale: java.util.Locale = java.util.Locale.ENGLISH
    override fun whatsNewTitle(version: String) = "What's new in $version"
    override val gotIt = "Let's go"
    override val updates = "Updates & versions"
    override val currentVersion = "Installed version"
    override val versionHistory = "Version history"
    override val installed = "Installed"
    override fun updateAvailable(version: String) = "New version available: $version"
    override val downloadUpdate = "Download and install"
    override val upToDate = "You have the latest version"
    override val updateTitle = "A new update is out!"
    override val updateNow = "Update now"
    override val later = "Later"
    override val account = "Account"
    override val signInTitle = "Sign in to compete"
    override val signInBody = "With an account you appear in the daily table and the monthly league against everyone. The game itself keeps working without one."
    override val signInGoogle = "Sign in with Google"
    override val signInFailed = "Sign-in failed. Try again"
    override val signOut = "Sign out"
    override val profileTitle = "Your profile"
    override val nickname = "Nickname"
    override val nicknameHint = "2–16 characters, shown on the leaderboards"
    override val nicknameTaken = "That nickname is taken. Try another"
    override val nicknameInvalid = "2–16 letters, digits or spaces"
    override val favoriteTeam = "Favorite team"
    override val noTeam = "None"
    override val showPhoto = "Show my Google profile photo"
    override val save = "Save"
    override val editProfile = "Edit profile"
    override val friendCode = "Friend code"
    override val leaderboards = "Leaderboards"
    override val leaderboardsSub = "Today's table and the monthly league"
    override val todayTab = "Today"
    override val leagueTab = "League"
    override val you = "You"
    override val noResultsToday = "Nobody has finished today's challenge yet. Be the first!"
    override val noLeagueYet = "This month's league is still empty. Every daily you finish scores points"
    override val loadFailed = "Couldn't load the table. Check your connection"
    override val retry = "Try again"
    override val pointsShort = "pts"
    override val leagueRules = "Win on guess 1 = 10 pts, one less per extra guess, loss = 0. Resets every month"
    override val dailyReminder = "Daily reminder"
    override val dailyReminderSub = "At 19:30 if you haven't played the daily"
    override val gamesPlayed = "Played"
    override val profileLoadFailed = "You're signed in, but the profile couldn't be loaded"
    override fun leagueMonth(month: java.time.YearMonth) =
        month.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, locale) + " league"
}

fun stringsFor(l: Language): Strings = if (l == Language.HE) He else En

val LocalStrings = staticCompositionLocalOf<Strings> { He }
