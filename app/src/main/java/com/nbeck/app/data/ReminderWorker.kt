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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nbeck.app.MainActivity
import com.nbeck.app.NbeckApp
import com.nbeck.app.R
import com.nbeck.app.game.Mode
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Reminder for today's daily challenge: every day at 15:00, Friday at 12:00, Saturday at 22:00 —
 * only when today's daily isn't finished yet. Each run schedules the next one.
 */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as NbeckApp
        val settings = app.prefs.settings.first()
        val today = LocalDate.now()
        val played = app.prefs.game(Mode.DAILY, today).first()?.finished == true
        if (settings.dailyReminder && !played) {
            notify(applicationContext, settings.language, app.prefs.stats.first().daily.liveStreak(Mode.DAILY, today))
        }
        schedule(applicationContext, after = LocalDateTime.now().plusMinutes(1))
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "daily"
        private const val WORK = "daily-reminder"

        /** Reminder time for a day of the week. */
        fun timeFor(day: DayOfWeek): LocalTime = when (day) {
            DayOfWeek.FRIDAY -> LocalTime.of(12, 0)
            DayOfWeek.SATURDAY -> LocalTime.of(22, 0)
            else -> LocalTime.of(15, 0)
        }

        /** The first reminder moment strictly after [after]. */
        fun next(after: LocalDateTime): LocalDateTime {
            var d = after.toLocalDate()
            while (true) {
                val at = d.atTime(timeFor(d.dayOfWeek))
                if (at.isAfter(after)) return at
                d = d.plusDays(1)
            }
        }

        fun schedule(context: Context, after: LocalDateTime = LocalDateTime.now()) {
            val delay = Duration.between(LocalDateTime.now(), next(after)).toMillis().coerceAtLeast(0)
            val req = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, req)
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
                context, 1,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val text = when {
                streak > 0 && he -> "הרצף שלך ($streak ימים) מחכה – מי השחקן של היום?"
                streak > 0 -> "Keep your $streak-day streak alive – who's today's player?"
                he -> "עוד לא שיחקת היום. מי השחקן הסודי?"
                else -> "You haven't played today. Who's the secret player?"
            }
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFFFF7A1A.toInt())
                .setContentTitle(if (he) "האתגר היומי מחכה לך 🏀" else "Today's challenge is waiting 🏀")
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(2001, n)
        }
    }
}
