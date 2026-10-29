package com.nbeck.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nbeck.app.MainActivity
import com.nbeck.app.NbeckApp
import com.nbeck.app.R
import com.nbeck.app.game.Mode
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Evening nudge (~19:30): today's daily isn't finished yet — mentions the streak when there is one. */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as NbeckApp
        val settings = app.prefs.settings.first()
        if (!settings.dailyReminder) return Result.success()
        val today = LocalDate.now()
        val game = app.prefs.game(Mode.DAILY, today).first()
        if (game?.finished == true) return Result.success()
        val streak = app.prefs.stats.first().daily.liveStreak(Mode.DAILY, today)
        notify(applicationContext, settings.language, streak)
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "daily"
        private val AT: LocalTime = LocalTime.of(19, 30)

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(AT)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily-reminder", ExistingPeriodicWorkPolicy.KEEP, req)
        }

        private fun notify(context: Context, language: Language, streak: Int) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            val he = language == Language.HE
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, if (he) "תזכורת יומית" else "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT)
            )
            val open = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val title = if (he) "האתגר היומי מחכה לך 🏀" else "Today's challenge is waiting 🏀"
            val text = when {
                streak > 0 && he -> "הרצף שלך ($streak ימים) בסכנה — יש לך עד חצות"
                streak > 0 -> "Your $streak-day streak is on the line — play before midnight"
                he -> "עוד לא שיחקת היום. מי השחקן הסודי?"
                else -> "You haven't played today. Who's the secret player?"
            }
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFFFF7A1A.toInt())
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(2001, n)
        }
    }
}
