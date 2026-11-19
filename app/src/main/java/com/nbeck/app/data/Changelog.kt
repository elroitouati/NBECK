package com.nbeck.app.data

/** One released version. Add a new entry at the top for every release (and bump versionCode). */
data class Release(
    val code: Int,
    val name: String,
    val date: String,
    val he: List<String>,
    val en: List<String>,
    /** French notes; older releases fall back to English. */
    val fr: List<String> = emptyList(),
) {
    fun notesFor(l: Language) = when (l) {
        Language.HE -> he
        Language.FR -> fr.ifEmpty { en }
        Language.EN -> en
    }
}

object Changelog {
    val releases: List<Release> = listOf(
        Release(
            23, "2.5.0", "2026-11-19",
            he = listOf(
                "46 ערכות עיצוב מוכנות: כל 30 קבוצות ה-NBA ועוד 16 רעיונות (ניאון, שקיעה, אוקיינוס, מטריקס, כחול-לבן ועוד)",
                "צבעי האייקונים של המשחקים (חופשי, שרשרת, פחות או יותר, אתגרים, סטטיסטיקות, הישגים) חלק מהעיצוב האישי",
                "התראות אמינות: בדיקה ברקע כל רבע שעה גם כשהאפליקציה סגורה – התראה על גרסה חדשה ברגע שהיא זמינה, ותזכורת יומית שלא מתפספסת",
                "בקשת אישור להתראות ולפעילות ברקע, ומסך מצב התראות + התראת בדיקה בהגדרות",
                "פחות או יותר: SKIP מחליף שחקנים וקטגוריה לפני הבחירה הראשונה, ואחריה הופך ל-QUIT",
                "מחיקת המידע מסבירה בדיוק מה נמחק ושואלת אם אתה בטוח",
                "תיקון: כפתורי ההגדרות וההדרכה כבר לא עולים על הלוגו; בהדרכה – דני אבדיה",
            ),
            en = listOf(
                "46 ready-made themes: all 30 NBA teams plus 16 more ideas (Neon, Sunset, Ocean, Matrix, Blue & White and more)",
                "Game icon colors (free play, chain, higher or lower, past challenges, stats, achievements) are part of the custom theme",
                "Reliable notifications: a background check every 15 minutes even with the app closed – new-version alerts as soon as it's out, and a daily reminder that doesn't get lost",
                "Asks to allow notifications and background activity; notification status and a test notification in Settings",
                "Higher or Lower: SKIP swaps the players and category before your first pick, then turns into QUIT",
                "Deleting your data explains exactly what's erased and asks if you're sure",
                "Fix: the settings and help buttons no longer overlap the logo; the how-to now shows Deni Avdija",
            ),
            fr = listOf(
                "46 thèmes prêts : les 30 équipes NBA et 16 autres idées (Neon, Sunset, Ocean, Matrix, Blue & White…)",
                "Les couleurs des icônes des jeux font partie du thème personnalisé",
                "Notifications fiables : vérification toutes les 15 minutes même appli fermée – alerte dès qu'une version sort, et rappel quotidien qui ne se perd pas",
                "Demande l'autorisation des notifications et de l'activité en arrière-plan ; état des notifications et notification test dans les Réglages",
                "Plus ou moins : PASSER change les joueurs et la catégorie avant le premier choix, puis devient QUITTER",
                "La suppression des données explique ce qui est effacé et demande confirmation",
                "Correctif : les boutons réglages et aide ne recouvrent plus le logo ; le tutoriel montre Deni Avdija",
            ),
        ),
        Release(
            22, "2.4.0", "2026-11-19",
            he = listOf(
                "מסך הבית חי יותר: כרטיסים שנכנסים באנימציה, לוגו שזז, אייקונים צבעוניים ושורת רצף/ניצחונות/הישגים",
                "עיצוב מותאם אישית כמעט לכל דבר: רקע, זרקור, פרקט, כרטיסים, מסגרות, טקסט, כפתורים, לוח תוצאות ומשבצות",
                "גלגל צבעים, פלטה של 54 צבעים, קוד צבע וערכות מוכנות",
                "8 הישגים חדשים",
                "התזכורת היומית מגיעה בשעה המדויקת גם בלי לפתוח את האפליקציה, ולא נשלחת אם כבר שיחקת היום",
                "שפה חדשה: צרפתית",
            ),
            en = listOf(
                "A livelier home screen: animated cards, a moving logo, colorful icons and a streak/wins/badges strip",
                "Customize almost everything: background, spotlight, parquet, cards, outlines, text, buttons, scoreboard and tiles",
                "Color wheel, a 54-color palette, hex codes and ready-made presets",
                "8 new achievements",
                "The daily reminder arrives on time even if you don't open the app, and skips days you already played",
                "New language: French",
            ),
            fr = listOf(
                "Un accueil plus vivant : cartes animées, logo qui bouge, icônes colorées et bandeau série/victoires/succès",
                "Personnalise presque tout : fond, projecteur, parquet, cartes, contours, texte, boutons, tableau et cases",
                "Roue chromatique, palette de 54 couleurs, codes hex et thèmes prêts",
                "8 nouveaux succès",
                "Le rappel quotidien arrive à l'heure même sans ouvrir l'appli, et pas si tu as déjà joué",
                "Nouvelle langue : le français",
            ),
        ),
        Release(
            21, "2.3.0", "2026-11-15",
            he = listOf(
                "פחות או יותר: הנתון של השחקן העליון נשאר גלוי, שחקנים שמתאימים לקטגוריה, בלי אפסים, ו\"שווה\" מול 100%",
                "קטגוריה מוצגת בקיצור (RPG) והפירוש מתחתיה",
                "עיצוב מותאם אישית: בוחרים רקע, כרטיסים, צבע ראשי ולוח תוצאות",
                "תזכורת לאתגר היומי: כל יום ב-15:00, שישי ב-12:00, שבת ב-22:00",
                "התראה על גרסה חדשה מגיעה תוך כרבע שעה מהפרסום",
            ),
            en = listOf(
                "Higher or Lower: the top player's number stays visible, category-relevant players, no zeros, and EQUAL against 100%",
                "Category shown as an abbreviation (RPG) with its meaning below",
                "Custom theme: pick background, cards, accent and scoreboard colors",
                "Daily challenge reminder: 15:00 every day, Friday 12:00, Saturday 22:00",
                "New-version notification within about 15 minutes of a release",
            ),
        ),
        Release(
            20, "2.2.0", "2026-11-15",
            he = listOf(
                "משחקון חדש: פחות או יותר – 20 השוואות של נתוני העונה הנוכחית מ-ESPN",
                "נתוני עונה מלאים לכל שחקן: חטיפות, חסימות, איבודים ואחוזי קליעה",
            ),
            en = listOf(
                "New mini-game: Higher or Lower – 20 comparisons of current-season ESPN stats",
                "Full season stat line per player: steals, blocks, turnovers and shooting %",
            ),
        ),
        Release(
            19, "2.1.1", "2026-11-11",
            he = listOf(
                "שרשרת שחקנים: בלי רמז למספר השחקנים, וכפתור דלג לפני הניחוש הראשון",
                "במשחק הרגיל: תמונת השחקן נחשפת רק אחרי שכל המשבצות התהפכו",
            ),
            en = listOf(
                "Players Chain: no hint about how many players, and a Skip button before the first guess",
                "Main game: the player's photo is revealed only after all tiles have flipped",
            ),
        ),
        Release(
            18, "2.1.0", "2026-11-10",
            he = listOf(
                "עדכון ישר מהאפליקציה: \"עדכן עכשיו\" מוריד ופותח את חלון ההתקנה, בלי דפדפן וקבצים",
                "שרשרת שחקנים: כפתור ויתור, ואי אפשר יותר למחוק חוליה",
            ),
            en = listOf(
                "Update right from the app: \"Update now\" downloads and opens the installer – no browser or Files",
                "Players Chain: give-up button; links can no longer be removed",
            ),
        ),
        Release(
            17, "2.0.0", "2026-11-10",
            he = listOf(
                "משחק חדש: שרשרת שחקנים – חבר בין שני שחקנים דרך חברים לקבוצה, ב-5 ניסיונות",
                "היסטוריית הקבוצות של כל שחקן מתעדכנת אוטומטית מ-ESPN",
                "3 הישגים חדשים לשרשרת",
            ),
            en = listOf(
                "New game: Players Chain – connect two players through teammates in 5 attempts",
                "Every player's team history updates automatically from ESPN",
                "3 new Players Chain achievements",
            ),
        ),
        Release(
            16, "1.9.0", "2026-10-30",
            he = listOf("הוסרו החשבונות, הטבלאות והתזכורת היומית – חזרה למשחק כמו בגרסה 1.7.1"),
            en = listOf("Accounts, leaderboards and the daily reminder removed – back to the game as in 1.7.1"),
        ),
        Release(
            12, "1.7.1", "2026-10-29",
            he = listOf("בכל כניסה: חלונית שמודיעה על עדכון חדש עם כפתור \"עדכן עכשיו\""),
            en = listOf("On open: a window announcing a new update, with an \"Update now\" button"),
        ),
        Release(
            11, "1.7.0", "2026-10-29",
            he = listOf(
                "התראה כשיוצאת גרסה חדשה – לחיצה עליה מורידה אותה",
                "בהגדרות: בדיקה אם יש גרסה חדשה",
            ),
            en = listOf(
                "A notification when a new version is out – tap it to download",
                "Settings shows when a newer version is available",
            ),
        ),
        Release(
            10, "1.6.0", "2026-10-29",
            he = listOf(
                "אנימציית פתיחה חדשה של DNBA",
                "חלונית \"מה חדש\" אחרי כל עדכון",
                "היסטוריית גרסאות בהגדרות",
            ),
            en = listOf(
                "New DNBA opening animation",
                "\"What's new\" window after every update",
                "Version history in Settings",
            ),
        ),
        Release(
            9, "1.5.0", "2026-10-27",
            he = listOf(
                "מספרי גופייה לפי המשחק האחרון של כל קבוצה (למשל ג'מאל שד – 1)",
                "תיקון תאריכי לידה ומדינות שגויים",
                "רענון נתונים אוטומטי בכל פתיחה של האפליקציה",
                "12 הישגים חדשים",
            ),
            en = listOf(
                "Jersey numbers from each team's latest game (e.g. Jamal Shead – 1)",
                "Fixed wrong birth dates and countries",
                "Player data refreshes every time you open the app",
                "12 new achievements",
            ),
        ),
        Release(
            8, "1.4.0", "2026-10-26",
            he = listOf(
                "אתגרים קודמים – האתגר היומי של 30 הימים האחרונים",
                "ערכת הצבע עוקבת אחרי הגדרות המכשיר",
                "מחיקת הנתונים שלי בהגדרות",
                "רשימת החיפוש תמיד נפתחת מההתחלה",
            ),
            en = listOf(
                "Past challenges – the daily of the last 30 days",
                "Theme follows the phone's setting",
                "Delete my data, in Settings",
                "Search suggestions always open at the top",
            ),
        ),
        Release(
            7, "1.3.1", "2026-10-25",
            he = listOf("אייקון חד שלא נחתך"),
            en = listOf("Sharp app icon that isn't cut off"),
        ),
        Release(
            6, "1.3.0", "2026-10-25",
            he = listOf("אייקון ומסך בית חדשים", "מדינות לשחקנים שהיו בלי מדינה", "הוסר כפתור השיתוף"),
            en = listOf("New icon and home screen", "Countries for players that had none", "Share button removed"),
        ),
        Release(
            5, "1.2.1", "2026-10-25",
            he = listOf("תוצאות החיפוש לפי סדר ABC, בלי מספר גופייה"),
            en = listOf("Search results A–Z, without jersey numbers"),
        ),
        Release(
            4, "1.2.0", "2026-10-25",
            he = listOf("השם החדש: DNBA", "תיקון משבצת הטבעת שנתקעה בסיבוב", "הוסרו סימוני ✓/✗ מהמשבצות"),
            en = listOf("New name: DNBA", "Fixed the ring tile getting stuck mid-flip", "Removed the ✓/✗ marks from tiles"),
        ),
        Release(
            3, "1.1.0", "2026-10-24",
            he = listOf("קטגוריות חדשות: דראפט וטבעת", "עמדות G, F, C בלבד", "כפתור ויתור", "צלילי סיום חדשים"),
            en = listOf("New categories: draft and ring", "Positions G, F, C only", "Give-up button", "New end sounds"),
        ),
        Release(
            2, "1.0.1", "2026-10-24",
            he = listOf("תיקון טבלת הדיביז'נים שלא נפתחה"),
            en = listOf("Fixed the division board not opening"),
        ),
        Release(
            1, "1.0", "2026-10-24",
            he = listOf("הגרסה הראשונה"),
            en = listOf("First release"),
        ),
    )

    /** Releases the player hasn't seen yet (newest first). */
    fun since(lastSeen: Int): List<Release> = releases.filter { it.code > lastSeen }
}
