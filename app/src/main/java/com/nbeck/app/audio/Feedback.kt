package com.nbeck.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.nbeck.app.R
import com.nbeck.app.data.Prefs
import com.nbeck.app.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

enum class Sfx { FLIP, WIN, LOSE, CROWD }

/** Sounds (organ fanfare on win, trombone slide on loss, tick per tile flip, crowd on achievement) and haptics. */
class Feedback(context: Context, prefs: Prefs, scope: CoroutineScope) {
    private val settings = prefs.settings.stateIn(scope, SharingStarted.Eagerly, Settings())

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    private val ids = mapOf(
        Sfx.FLIP to pool.load(context, R.raw.snd_flip, 1),
        Sfx.WIN to pool.load(context, R.raw.snd_win, 1),
        Sfx.LOSE to pool.load(context, R.raw.snd_lose, 1),
        Sfx.CROWD to pool.load(context, R.raw.snd_crowd, 1),
    )

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun play(s: Sfx) {
        if (!settings.value.sound) return
        val vol = if (s == Sfx.FLIP) 0.45f else 0.9f
        pool.play(ids.getValue(s), vol, vol, 1, 0, 1f)
    }

    /** Light tick — used when a tile lands green. */
    fun tick() {
        if (!settings.value.haptics) return
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= 29) v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        else v.vibrate(VibrationEffect.createOneShot(18, 120))
    }

    fun heavy() {
        if (!settings.value.haptics) return
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= 29) v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        else v.vibrate(VibrationEffect.createOneShot(40, 200))
    }
}
