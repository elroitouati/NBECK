package com.nbeck.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.nbeck.app.audio.Feedback
import com.nbeck.app.data.PlayerRepository
import com.nbeck.app.data.Prefs
import com.nbeck.app.data.RefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import com.nbeck.app.game.Achievement

class NbeckApp : Application(), ImageLoaderFactory {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var players: PlayerRepository
    lateinit var prefs: Prefs
    lateinit var feedback: Feedback
    /** Newly unlocked achievements, shown as an animated toast over any screen. */
    val achievementEvents = MutableSharedFlow<List<Achievement>>(extraBufferCapacity = 4)
    /** Latest release found in the repo (null until checked). */
    val latestRelease = kotlinx.coroutines.flow.MutableStateFlow<com.nbeck.app.data.LatestRelease?>(null)

    override fun onCreate() {
        super.onCreate()
        players = PlayerRepository(this)
        prefs = Prefs(this)
        feedback = Feedback(this, prefs, appScope)
        // Load the cached/bundled roster right away; MainActivity.onStart refreshes it from ESPN.
        appScope.launch { players.load() }
        runCatching { RefreshWorker.schedule(this) }
        runCatching { com.nbeck.app.data.Updates.schedule(this) }
        // 1.8.x scheduled an evening reminder; that feature is gone, so stop the leftover job.
        runCatching { androidx.work.WorkManager.getInstance(this).cancelUniqueWork("daily-reminder") }
    }

    /** Headshots are cached on disk so they keep working offline. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.2).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("headshots")).maxSizeBytes(80L * 1024 * 1024).build() }
        .respectCacheHeaders(false)
        .crossfade(true)
        .build()
}
