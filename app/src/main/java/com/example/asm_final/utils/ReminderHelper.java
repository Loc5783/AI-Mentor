package com.example.asm_final.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;
import java.util.TimeZone;

public final class ReminderHelper {
  private static final String PREFS_NAME = "settings";
  private static final String KEY_REMINDERS_ENABLED = "reminders_enabled";
  public static final String KEY_HAS_PROMPTED_NOTIFICATION = "has_prompted_notification";
  
  public static final int REQUEST_CODE_EVENING = 1001;
  public static final int REQUEST_CODE_MORNING = 1002;
  public static final int REQUEST_CODE_STREAK = 1003;
  public static final int REQUEST_CODE_EXAM = 1004;
  
  public static final String ACTION_EVENING_REMINDER = "com.example.asm_final.ACTION_EVENING_REMINDER";
  public static final String ACTION_MORNING_REMINDER = "com.example.asm_final.ACTION_MORNING_REMINDER";
  public static final String ACTION_STREAK_REMINDER = "com.example.asm_final.ACTION_STREAK_REMINDER";
  public static final String ACTION_EXAM_REMINDER = "com.example.asm_final.ACTION_EXAM_REMINDER";

  public static void setExamPlanHour(Context context, int hour) {
    if (hour < 0 || hour > 23) throw new IllegalArgumentException("hour");
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putInt("exam_plan_hour", hour).apply();
    reschedule(context);
  }

  private ReminderHelper() {}

  public static boolean hasPromptedNotification(Context context) {
    return context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_HAS_PROMPTED_NOTIFICATION, false);
  }

  public static void setHasPromptedNotification(Context context, boolean prompted) {
    context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_HAS_PROMPTED_NOTIFICATION, prompted)
        .apply();
  }

  public static boolean shouldPromptPolicy(boolean hasPrompted, boolean isReminderEnabled) {
    if (hasPrompted) {
      return false;
    }
    return !isReminderEnabled;
  }

  public static boolean shouldPromptNotification(Context context) {
    return shouldPromptPolicy(hasPromptedNotification(context), isReminderEnabled(context));
  }

  public static boolean isReminderEnabled(Context context) {
    return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_REMINDERS_ENABLED, false)
        && androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled();
  }

  public static void setReminderEnabled(Context context, boolean enabled) {
    context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_REMINDERS_ENABLED, enabled)
        .putBoolean(KEY_HAS_PROMPTED_NOTIFICATION, true)
        .apply();

    if (enabled) {
      scheduleReminders(context);
    } else {
      cancelReminders(context);
    }
  }
  
  public static void reschedule(Context context) {
      if (isReminderEnabled(context)) {
          scheduleReminders(context);
      }
  }

  public static void scheduleReminders(Context context) {
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (alarmManager == null) return;

    // Evening Reminder (20:00)
    Intent eveningIntent = new Intent(context, ReminderReceiver.class);
    eveningIntent.setAction(ACTION_EVENING_REMINDER);
    PendingIntent eveningPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_EVENING,
            eveningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    Calendar eveningCal = Calendar.getInstance();
    eveningCal.setTimeInMillis(System.currentTimeMillis());
    eveningCal.set(Calendar.HOUR_OF_DAY, 20); // 20:00
    eveningCal.set(Calendar.MINUTE, 0);
    eveningCal.set(Calendar.SECOND, 0);

    if (eveningCal.getTimeInMillis() <= System.currentTimeMillis()) {
      eveningCal.add(Calendar.DAY_OF_YEAR, 1);
    }

    alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        eveningCal.getTimeInMillis(),
        AlarmManager.INTERVAL_DAY,
        eveningPending);

    // Morning Reminder (06:45)
    Intent morningIntent = new Intent(context, ReminderReceiver.class);
    morningIntent.setAction(ACTION_MORNING_REMINDER);
    PendingIntent morningPending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MORNING,
            morningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    Calendar morningCal = Calendar.getInstance();
    morningCal.setTimeInMillis(System.currentTimeMillis());
    morningCal.set(Calendar.HOUR_OF_DAY, 6); // 06:45
    morningCal.set(Calendar.MINUTE, 45);
    morningCal.set(Calendar.SECOND, 0);

    if (morningCal.getTimeInMillis() <= System.currentTimeMillis()) {
      morningCal.add(Calendar.DAY_OF_YEAR, 1);
    }

    alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        morningCal.getTimeInMillis(),
        AlarmManager.INTERVAL_DAY,
        morningPending);

    // Streak days are counted by Supabase in Vietnam time, independent of device timezone.
    Intent streakIntent = new Intent(context, ReminderReceiver.class);
    streakIntent.setAction(ACTION_STREAK_REMINDER);
    PendingIntent streakPending = PendingIntent.getBroadcast(context, REQUEST_CODE_STREAK,
        streakIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Calendar streakCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    streakCal.set(Calendar.HOUR_OF_DAY, 22);
    streakCal.set(Calendar.MINUTE, 0);
    streakCal.set(Calendar.SECOND, 0);
    streakCal.set(Calendar.MILLISECOND, 0);
    if (streakCal.getTimeInMillis() <= System.currentTimeMillis())
      streakCal.add(Calendar.DAY_OF_YEAR, 1);
    alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
        streakCal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, streakPending);

    if (context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).contains("exam_plan_hour")) {
      Intent examIntent = new Intent(context, ReminderReceiver.class).setAction(ACTION_EXAM_REMINDER);
      PendingIntent examPending = PendingIntent.getBroadcast(context, REQUEST_CODE_EXAM, examIntent,
          PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      Calendar examCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
      examCal.set(Calendar.HOUR_OF_DAY, context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
          .getInt("exam_plan_hour", 19));
      examCal.set(Calendar.MINUTE, 0); examCal.set(Calendar.SECOND, 0);
      if (examCal.getTimeInMillis() <= System.currentTimeMillis()) examCal.add(Calendar.DAY_OF_YEAR, 1);
      alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, examCal.getTimeInMillis(),
          AlarmManager.INTERVAL_DAY, examPending);
    }
  }

  private static void cancelReminders(Context context) {
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (alarmManager == null) return;
    alarmManager.cancel(PendingIntent.getBroadcast(context, REQUEST_CODE_EXAM,
        new Intent(context, ReminderReceiver.class).setAction(ACTION_EXAM_REMINDER),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

    Intent eveningIntent = new Intent(context, ReminderReceiver.class);
    eveningIntent.setAction(ACTION_EVENING_REMINDER);
    PendingIntent eveningPending = PendingIntent.getBroadcast(
            context, REQUEST_CODE_EVENING, eveningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    alarmManager.cancel(eveningPending);

    Intent morningIntent = new Intent(context, ReminderReceiver.class);
    morningIntent.setAction(ACTION_MORNING_REMINDER);
    PendingIntent morningPending = PendingIntent.getBroadcast(
            context, REQUEST_CODE_MORNING, morningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    alarmManager.cancel(morningPending);

    Intent streakIntent = new Intent(context, ReminderReceiver.class);
    streakIntent.setAction(ACTION_STREAK_REMINDER);
    PendingIntent streakPending = PendingIntent.getBroadcast(context, REQUEST_CODE_STREAK,
        streakIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    alarmManager.cancel(streakPending);
  }
}
