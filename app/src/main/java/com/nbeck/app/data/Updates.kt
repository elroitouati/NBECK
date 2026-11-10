package com.nbeck.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nbeck.app.BuildConfig
import com.nbeck.app.NbeckApp
import com.nbeck.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** release/latest.json in the (public) GitHub repo — rewritten for every release. */
@Serializable
data class LatestRelease(
    val versionCode: Int,
    val versionName: String,
    val apk: String,
    val notesHe: List<String> = emptyList(),
    val notesEn: List<String> = emptyList(),
)

/**
 * New-version check. There is no server: each release publishes the APK and release/latest.json to
 * the GitHub repo, the app reads that file (on open and every few hours in the background) and,
 * when it lists a higher versionCode, posts a notification whose tap downloads the APK.
 */
object Updates {
    private const val MANIFEST = "https://raw.githubusercontent.com/elroitouati/NBECK/HEAD/release/latest.json"
    private const val CHANNEL = "updates"
    private const val NOTIFICATION_ID = 1001
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

    suspend fun fetch(): LatestRelease? = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url(MANIFEST).header("Cache-Control", "no-cache").build()).execute().use { r ->
                if (!r.isSuccessful) null else json.decodeFromString(LatestRelease.serializer(), r.body!!.string())
            }
        }.getOrNull()
    }

    fun isNewer(r: LatestRelease?) = r != null && r.versionCode > BuildConfig.VERSION_CODE

    /** Fetch; remember the result for Settings; notify once per new version. */
    suspend fun check(context: Context) {
        val app = context.applicationContext as NbeckApp
        val latest = fetch() ?: return
        app.latestRelease.value = latest
        if (!isNewer(latest)) return
        val settings = app.prefs.settings.first()
        if (settings.notifiedVersion >= latest.versionCode) return
        if (notify(context, latest, settings.language)) app.prefs.updateSettings { it.copy(notifiedVersion = latest.versionCode) }
    }

    /** Notification tap: open the app, which downloads and installs the update itself. */
    const val EXTRA_INSTALL = "dnba.install_update"
    private fun openAppIntent(context: Context) =
        Intent(context, com.nbeck.app.MainActivity::class.java)
            .putExtra(EXTRA_INSTALL, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    private fun notify(context: Context, r: LatestRelease, language: Language): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        val he = language == Language.HE
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, if (he) "עדכוני גרסה" else "App updates", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val tap = PendingIntent.getActivity(context, 0, openAppIntent(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notes = (if (he) r.notesHe else r.notesEn).joinToString("\n") { "• $it" }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFFF7A1A.toInt())
            .setContentTitle(if (he) "גרסה חדשה של DNBA: ${r.versionName}" else "New DNBA version: ${r.versionName}")
            .setContentText(if (he) "לחץ כדי לעדכן" else "Tap to update")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                (if (he) "לחץ כדי לעדכן\n" else "Tap to update\n") + notes
            ))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
        return true
    }

    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<UpdateWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("update-check", ExistingPeriodicWorkPolicy.KEEP, req)
    }
}

class UpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Updates.check(applicationContext)
        return Result.success()
    }
}

sealed interface InstallState {
    data object Idle : InstallState
    data class Downloading(val percent: Int) : InstallState
    /** Android needs "Install unknown apps" allowed for DNBA once; we wait for the user to come back. */
    data object NeedsPermission : InstallState
    /** The system installer is open. */
    data object Installing : InstallState
    data class Failed(val reason: String) : InstallState
}

/**
 * Downloads the new APK into the app's cache and opens the system installer on it — no browser,
 * no Files app. Android still shows its own "Install / Update" confirmation; that can't be skipped
 * for an app outside the Play Store.
 */
object Installer {
    val state = kotlinx.coroutines.flow.MutableStateFlow<InstallState>(InstallState.Idle)
    /** Set from a notification tap: start the update as soon as the app is up. */
    val requested = kotlinx.coroutines.flow.MutableStateFlow(false)
    private var pending: java.io.File? = null
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()

    suspend fun downloadAndInstall(context: Context, r: LatestRelease) {
        if (state.value is InstallState.Downloading) return
        state.value = InstallState.Downloading(0)
        val dir = java.io.File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = java.io.File(dir, "DNBA-${r.versionCode}.apk")
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(r.apk).header("Cache-Control", "no-cache").build()).execute().use { res ->
                    check(res.isSuccessful) { "HTTP ${res.code}" }
                    val body = res.body!!
                    val total = body.contentLength().takeIf { it > 0 }
                    body.byteStream().use { input ->
                        file.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024)
                            var done = 0L
                            var last = -1
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                done += n
                                val pct = if (total != null) (done * 100 / total).toInt() else 0
                                if (pct != last) { last = pct; state.value = InstallState.Downloading(pct) }
                            }
                        }
                    }
                }
            }
        }
        ok.exceptionOrNull()?.let { state.value = InstallState.Failed(it.message ?: "download failed"); return }
        install(context, file)
    }

    fun install(context: Context, file: java.io.File) {
        val pm = context.packageManager
        if (!pm.canRequestPackageInstalls()) {
            pending = file
            state.value = InstallState.NeedsPermission
            context.startActivity(
                Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        state.value = InstallState.Installing
    }

    /** Back from the "Install unknown apps" screen: continue if it was allowed. */
    fun resumeIfAllowed(context: Context) {
        val f = pending ?: return
        if (state.value == InstallState.NeedsPermission && context.packageManager.canRequestPackageInstalls()) {
            pending = null
            install(context, f)
        }
    }

    fun reset() { if (state.value !is InstallState.Downloading) state.value = InstallState.Idle }
}
