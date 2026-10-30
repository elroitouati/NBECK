package com.nbeck.app.data

/** One released version. Add a new entry at the top for every release (and bump versionCode). */
data class Release(
    val code: Int,
    val name: String,
    val date: String,
    val he: List<String>,
    val en: List<String>,
)

object Changelog {
    val releases: List<Release> = listOf(
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
