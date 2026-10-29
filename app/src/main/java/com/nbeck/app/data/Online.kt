package com.nbeck.app.data

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.nbeck.app.R
import com.nbeck.app.game.AchievementState
import com.nbeck.app.game.MAX_GUESSES
import com.nbeck.app.game.Stats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/** The signed-in player's public profile (users/{uid}). */
data class Profile(
    val uid: String,
    val nickname: String,
    val team: Int?,
    val photo: String?,
    val friendCode: String,
    val stats: PublicStats = PublicStats(),
    val achievements: List<String> = emptyList(),
)

data class PublicStats(
    val played: Int = 0,
    val wins: Int = 0,
    val winPct: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val avgGuesses: Double = 0.0,
)

/** One row of a leaderboard. [value] = guesses (daily) or points (league). */
data class BoardRow(
    val uid: String,
    val nickname: String,
    val team: Int?,
    val photo: String?,
    val won: Boolean = true,
    val guesses: Int = 0,
    val seconds: Int = 0,
    val points: Int = 0,
    val played: Int = 0,
)

/**
 * Accounts and leaderboards on Firebase (project "dnba-c115d"). Playing never needs an account;
 * signing in adds a public profile, the daily table and the monthly league.
 *
 * Firestore layout (security rules: firestore.rules at the repo root):
 *   users/{uid}                     profile + public stats
 *   nicknames/{lowercase nickname}  { uid } — claims a nickname so it's unique
 *   daily/{yyyy-MM-dd}/results/{uid} one result per player per day (create-only)
 *   league/{yyyy-MM}/players/{uid}  monthly points; rules allow +points only together with a new daily result
 */
class Online(private val context: Context) {

    private val auth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()
    private val db: FirebaseFirestore? = runCatching { Firebase.firestore }.getOrNull()

    private val _profile = MutableStateFlow<Profile?>(null)
    /** Null = signed out, or signed in but no profile yet (see [needsProfile]). */
    val profile: StateFlow<Profile?> = _profile

    private val _needsProfile = MutableStateFlow(false)
    val needsProfile: StateFlow<Boolean> = _needsProfile

    val uid: String? get() = auth?.currentUser?.uid
    val googlePhoto: String? get() = auth?.currentUser?.photoUrl?.toString()
    val googleName: String? get() = auth?.currentUser?.displayName

    /** Loads the profile of an already signed-in user (app start). */
    suspend fun restore() {
        if (uid == null) return
        runCatching { loadProfile() }
    }

    /** Google sign-in via Credential Manager, then Firebase. Throws on cancel/failure. */
    suspend fun signIn(activity: Activity) {
        val a = auth ?: error("Firebase unavailable")
        val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val cred = CredentialManager.create(activity).getCredential(activity, request).credential
        require(cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { "Unexpected credential" }
        val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
        a.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        loadProfile()
    }

    suspend fun signOut(activity: Activity) {
        auth?.signOut()
        runCatching { CredentialManager.create(activity).clearCredentialState(androidx.credentials.ClearCredentialStateRequest()) }
        _profile.value = null
        _needsProfile.value = false
    }

    private suspend fun loadProfile() {
        val id = uid ?: return
        val doc = db!!.collection("users").document(id).get().await()
        if (!doc.exists()) {
            _profile.value = null
            _needsProfile.value = true
            return
        }
        @Suppress("UNCHECKED_CAST")
        val st = doc.get("stats") as? Map<String, Any?> ?: emptyMap()
        _profile.value = Profile(
            uid = id,
            nickname = doc.getString("nickname").orEmpty(),
            team = doc.getLong("team")?.toInt(),
            photo = doc.getString("photo"),
            friendCode = doc.getString("friendCode").orEmpty(),
            stats = st.toStats(),
            achievements = (doc.get("achievements") as? List<*>)?.filterIsInstance<String>().orEmpty(),
        )
        _needsProfile.value = false
    }

    enum class NickResult { OK, TAKEN, INVALID }

    /**
     * Creates or edits the profile. The nickname is claimed in nicknames/{lowercase} inside a
     * transaction, so two players can't end up with the same one.
     */
    suspend fun saveProfile(nickname: String, team: Int?, showPhoto: Boolean): NickResult {
        val id = uid ?: return NickResult.INVALID
        val nick = nickname.trim().replace(Regex("\\s+"), " ")
        if (!isValidNickname(nick)) return NickResult.INVALID
        val key = nick.lowercase()
        val old = _profile.value
        val photo = if (showPhoto) googlePhoto else null
        val d = db!!
        val ok = d.runTransaction { tx ->
            val claim = d.collection("nicknames").document(key)
            val snap = tx.get(claim)
            if (snap.exists() && snap.getString("uid") != id) return@runTransaction false
            if (old != null && old.nickname.lowercase() != key) tx.delete(d.collection("nicknames").document(old.nickname.lowercase()))
            tx.set(claim, mapOf("uid" to id))
            tx.set(
                d.collection("users").document(id),
                mapOf(
                    "nickname" to nick,
                    "nicknameLower" to key,
                    "team" to team,
                    "photo" to photo,
                    "friendCode" to (old?.friendCode?.takeIf { it.isNotEmpty() } ?: friendCodeFor(id)),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            true
        }.await()
        if (!ok) return NickResult.TAKEN
        // Keep this month's league row and today's result showing the current name/team/photo.
        val ident = mapOf("nickname" to nick, "team" to team, "photo" to photo)
        runCatching { d.collection("league").document(monthKey(LocalDate.now())).collection("players").document(id).update(ident).await() }
        runCatching { d.collection("daily").document(LocalDate.now().toString()).collection("results").document(id).update(ident).await() }
        loadProfile()
        return NickResult.OK
    }

    /** Mirrors the local statistics and unlocked achievements onto the public profile. */
    suspend fun syncStats(stats: Stats, achievements: AchievementState) {
        val id = uid ?: return
        if (_profile.value == null) return
        val all = stats.daily.played + stats.free.played
        val wins = stats.totalWins
        val guessTotal = stats.daily.winGuessTotal + stats.free.winGuessTotal
        val map = mapOf(
            "stats" to mapOf(
                "played" to all,
                "wins" to wins,
                "winPct" to if (all == 0) 0 else Math.round(wins * 100f / all),
                "currentStreak" to stats.daily.liveStreak(com.nbeck.app.game.Mode.DAILY, LocalDate.now()),
                "maxStreak" to stats.daily.maxStreak,
                "avgGuesses" to if (wins == 0) 0.0 else guessTotal.toDouble() / wins,
            ),
            "achievements" to achievements.unlocked.keys.toList(),
        )
        runCatching { db!!.collection("users").document(id).set(map, SetOptions.merge()).await() }
    }

    /**
     * Today's daily, once finished: one result (create-only) plus this month's league points, in a
     * single batch. If today's result already exists the whole batch is rejected — no double points.
     */
    suspend fun submitDaily(date: LocalDate, won: Boolean, guesses: Int, seconds: Int) {
        val p = _profile.value ?: return
        val d = db ?: return
        val points = leaguePoints(won, guesses)
        val ident = mapOf("nickname" to p.nickname, "team" to p.team, "photo" to p.photo)
        val result = d.collection("daily").document(date.toString()).collection("results").document(p.uid)
        val league = d.collection("league").document(monthKey(date)).collection("players").document(p.uid)
        val batch = d.batch()
        batch.set(
            result,
            ident + mapOf(
                "won" to won,
                "guesses" to guesses.coerceIn(1, MAX_GUESSES),
                "seconds" to seconds.coerceIn(0, 99_999),
                "points" to points,
                "rank" to rankKey(won, guesses, seconds),
                "at" to FieldValue.serverTimestamp(),
            ),
        )
        batch.set(
            league,
            ident + mapOf(
                "points" to FieldValue.increment(points.toLong()),
                "played" to FieldValue.increment(1),
                "wins" to FieldValue.increment(if (won) 1 else 0),
                "lastDate" to date.toString(),
            ),
            SetOptions.merge(),
        )
        runCatching { batch.commit().await() }
    }

    suspend fun dailyBoard(date: LocalDate, limit: Long = 100): List<BoardRow> {
        val d = db ?: return emptyList()
        val snap = d.collection("daily").document(date.toString()).collection("results")
            .orderBy("rank").limit(limit).get().await()
        return snap.documents.map { doc ->
            BoardRow(
                uid = doc.id,
                nickname = doc.getString("nickname").orEmpty(),
                team = doc.getLong("team")?.toInt(),
                photo = doc.getString("photo"),
                won = doc.getBoolean("won") ?: false,
                guesses = doc.getLong("guesses")?.toInt() ?: 0,
                seconds = doc.getLong("seconds")?.toInt() ?: 0,
                points = doc.getLong("points")?.toInt() ?: 0,
            )
        }
    }

    suspend fun leagueBoard(month: String, limit: Long = 100): List<BoardRow> {
        val d = db ?: return emptyList()
        val snap = d.collection("league").document(month).collection("players")
            .orderBy("points", Query.Direction.DESCENDING).limit(limit).get().await()
        return snap.documents.map { doc ->
            BoardRow(
                uid = doc.id,
                nickname = doc.getString("nickname").orEmpty(),
                team = doc.getLong("team")?.toInt(),
                photo = doc.getString("photo"),
                points = doc.getLong("points")?.toInt() ?: 0,
                played = doc.getLong("played")?.toInt() ?: 0,
            )
        }
    }

    /** Another player's public profile (tap on a leaderboard row). */
    suspend fun publicProfile(uid: String): Profile? {
        val doc = db?.collection("users")?.document(uid)?.get()?.await() ?: return null
        if (!doc.exists()) return null
        @Suppress("UNCHECKED_CAST")
        val st = doc.get("stats") as? Map<String, Any?> ?: emptyMap()
        return Profile(
            uid = uid,
            nickname = doc.getString("nickname").orEmpty(),
            team = doc.getLong("team")?.toInt(),
            photo = doc.getString("photo"),
            friendCode = doc.getString("friendCode").orEmpty(),
            stats = st.toStats(),
            achievements = (doc.get("achievements") as? List<*>)?.filterIsInstance<String>().orEmpty(),
        )
    }

    private fun Map<String, Any?>.toStats() = PublicStats(
        played = (this["played"] as? Number)?.toInt() ?: 0,
        wins = (this["wins"] as? Number)?.toInt() ?: 0,
        winPct = (this["winPct"] as? Number)?.toInt() ?: 0,
        currentStreak = (this["currentStreak"] as? Number)?.toInt() ?: 0,
        maxStreak = (this["maxStreak"] as? Number)?.toInt() ?: 0,
        avgGuesses = (this["avgGuesses"] as? Number)?.toDouble() ?: 0.0,
    )

    companion object {
        /** 2–16 letters (Hebrew or English), digits, spaces, _ . - */
        fun isValidNickname(s: String) = s.length in 2..16 && s.all { it.isLetterOrDigit() || it in " _.-" } && s.any { it.isLetterOrDigit() }

        /** League points for a daily: win on guess 1 = 10 … win on guess 8 = 3, loss = 0. */
        fun leaguePoints(won: Boolean, guesses: Int) = if (won) 11 - guesses.coerceIn(1, MAX_GUESSES) else 0

        /** Sort key for the daily table: fewer guesses first, then faster; losses last. */
        fun rankKey(won: Boolean, guesses: Int, seconds: Int): Long =
            (if (won) guesses.toLong() else 9L) * 1_000_000L + seconds.coerceIn(0, 999_999)

        fun monthKey(date: LocalDate) = "%04d-%02d".format(date.year, date.monthValue)

        /** Short, readable code for adding friends (phase 2), stable per account. */
        fun friendCodeFor(uid: String): String {
            val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            var h = com.nbeck.app.game.Daily.score("friend|$uid")
            return buildString { repeat(6) { append(alphabet[(h and 31).toInt()]); h = h ushr 5 } }
        }
    }
}
