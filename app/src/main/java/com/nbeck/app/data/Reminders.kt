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
import com.nbeck.app.MainActivity
import com.nbeck.app.NbeckApp
import com.nbeck.app.R
import com.nbeck.app.game.Mode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Reminder for today's daily challenge: every day at 15:00, Friday at 12:00, Saturday at 22:00 —
 * only when today's daily isn't finished yet. Uses an exact alarm, so it fires on time whether or
 * not the app was opened (and is set again after a reboot or an app update).
 */
object Reminders {
    private const val CHANNEL = "daily"
    const val ACTION = "com.nbeck.app.DAILY_REMINDER"

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

    private fun pending(context: Context) = PendingIntent.getBroadcast(
        context, 3, Intent(context, ReminderReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Arms the alarm for the next reminder moment (replaces any earlier one). */
    fun schedule(context: Context, after: LocalDateTime = LocalDateTime.now()) {
        val at = next(after).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService(android.app.AlarmManager::class.java)
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, at, pending(context))
        else am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, at, pending(context))
        // The WorkManager reminder of 2.3.0 is replaced by this alarm.
        runCatching { androidx.work.WorkManager.getInstance(context).cancelUniqueWork("daily-reminder") }
    }

    /** Alarm went off: remind if today's daily is still open, then arm the next one. */
    suspend fun fire(context: Context) {
        val app = context.applicationContext as NbeckApp
        val settings = app.prefs.settings.first()
        val today = LocalDate.now()
        val played = app.prefs.game(Mode.DAILY, today).first()?.finished == true
        if (settings.dailyReminder && !played) {
            notify(context, settings.language, app.prefs.stats.first().daily.liveStreak(Mode.DAILY, today))
        }
        schedule(context, after = LocalDateTime.now().plusMinutes(1))
    }

    private fun notify(context: Context, language: Language, streak: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val t = notificationText(language, streak)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, t.channel, NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFFF7A1A.toInt())
            .setContentTitle(t.title)
            .setContentText(t.text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(2001, n)
    }

    private class Text(val channel: String, val title: String, val text: String)

    private fun notificationText(language: Language, streak: Int) = when (language) {
        Language.HE -> Text(
            "תזכורת יומית", "האתגר היומי מחכה לך 🏀",
            if (streak > 0) "הרצף שלך ($streak ימים) מחכה – מי השחקן של היום?" else "עוד לא שיחקת היום. מי השחקן הסודי?",
        )
        Language.FR -> Text(
            "Rappel quotidien", "Le défi du jour t'attend 🏀",
            if (streak > 0) "Garde ta série de $streak jours – qui est le joueur du jour ?" else "Tu n'as pas encore joué aujourd'hui. Qui est le joueur mystère ?",
        )
        Language.EN -> Text(
            "Daily reminder", "Today's challenge is waiting 🏀",
            if (streak > 0) "Keep your $streak-day streak alive – who's today's player?" else "You haven't played today. Who's the secret player?",
        )
    }
}

/** Receives the reminder alarm, and re-arms it after a reboot or an app update. */
class ReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Reminders.ACTION -> {
                val done = goAsync()
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                    try { Reminders.fire(context) } finally { done.finish() }
                }
            }
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> Reminders.schedule(context)
        }
    }
}
