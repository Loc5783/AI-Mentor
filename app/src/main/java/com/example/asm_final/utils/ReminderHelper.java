package com.example.asm_final.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public final class ReminderHelper {
  private static final String PREFS_NAME = "settings";
  private static final String KEY_REMINDERS_ENABLED = "reminders_enabled";
  public static final String KEY_HAS_PROMPTED_NOTIFICATION = "has_prompted_notification";
  private static final int REQUEST_CODE = 1001;

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
      scheduleDailyReminder(context);
    } else {
      cancelReminder(context);
    }
  }

  public static void scheduleDailyReminder(Context context) {
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (alarmManager == null) return;

    Intent intent = new Intent(context, ReminderReceiver.class);
    PendingIntent pendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(System.currentTimeMillis());
    calendar.set(Calendar.HOUR_OF_DAY, 20); // 8:00 PM
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);

    // If time is in the past, schedule for tomorrow
    if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
      calendar.add(Calendar.DAY_OF_YEAR, 1);
    }

    alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.getTimeInMillis(),
        AlarmManager.INTERVAL_DAY,
        pendingIntent);
  }

  private static void cancelReminder(Context context) {
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (alarmManager == null) return;

    Intent intent = new Intent(context, ReminderReceiver.class);
    PendingIntent pendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    alarmManager.cancel(pendingIntent);
  }
}
