package com.example.asm_final.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.asm_final.R;
import com.example.asm_final.activities.MainActivity;
import com.example.mentor.data.ActiveUser;
import com.example.mentor.schedule.data.ScheduleRepositoryImpl;
import com.example.mentor.schedule.domain.ScheduleEntry;

import java.util.Calendar;
import java.util.List;

public class ReminderReceiver extends BroadcastReceiver {
  private static final String CHANNEL_ID = "study_schedule_channel";
  private static final int NOTIFICATION_ID = 101;

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
      if (ReminderHelper.isReminderEnabled(context)) {
        ReminderHelper.scheduleReminders(context);
      }
      return;
    }

    if (!ReminderHelper.isReminderEnabled(context)) return;

    int owner = ActiveUser.get(context);
    if (owner <= 0) return; // Không có tài khoản đăng nhập thì không có thông báo

    String action = intent.getAction();
    if (ReminderHelper.ACTION_EVENING_REMINDER.equals(action)) {
        handleEveningReminder(context, owner);
    } else if (ReminderHelper.ACTION_MORNING_REMINDER.equals(action)) {
        handleMorningReminder(context, owner);
    }
  }

  private void handleEveningReminder(Context context, int owner) {
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_YEAR, 1);
      int tomorrowDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
      if (tomorrowDay == 0) tomorrowDay = 7;

      ScheduleRepositoryImpl repo = new ScheduleRepositoryImpl(context);
      List<ScheduleEntry> entries = repo.getEntries(String.valueOf(owner), tomorrowDay);

      String title, content;
      if (entries.isEmpty()) {
          boolean hasAnySchedule = checkAnyScheduleExists(repo, String.valueOf(owner));
          if (!hasAnySchedule) {
              title = context.getString(R.string.reminder_title_empty);
              content = context.getString(R.string.reminder_body_empty);
          } else {
              title = context.getString(R.string.reminder_evening_title_free);
              content = context.getString(R.string.reminder_evening_body_free);
          }
      } else {
          String dayName = context.getString(com.example.mentor.schedule.R.string.schedule_days).split(",")[tomorrowDay - 1];
          StringBuilder subjects = new StringBuilder();
          for (int i = 0; i < entries.size(); i++) {
              subjects.append(entries.get(i).getTitle());
              if (i < entries.size() - 1) subjects.append(", ");
          }
          title = context.getString(R.string.reminder_evening_title_active, dayName);
          content = context.getString(R.string.reminder_evening_body_active, entries.size(), subjects.toString());
      }
      
      showNotification(context, title, content);
  }

  private void handleMorningReminder(Context context, int owner) {
      Calendar cal = Calendar.getInstance();
      int todayDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
      if (todayDay == 0) todayDay = 7;

      ScheduleRepositoryImpl repo = new ScheduleRepositoryImpl(context);
      List<ScheduleEntry> entries = repo.getEntries(String.valueOf(owner), todayDay);
      
      // Filter morning classes (period 1 to 5)
      java.util.ArrayList<ScheduleEntry> morningEntries = new java.util.ArrayList<>();
      for (ScheduleEntry e : entries) {
          if (e.getPeriod() <= 5) morningEntries.add(e);
      }

      String title, content;
      if (morningEntries.isEmpty()) {
          title = context.getString(R.string.reminder_morning_title_free);
          content = context.getString(R.string.reminder_morning_body_free);
      } else {
          morningEntries.sort((a, b) -> Integer.compare(a.getPeriod(), b.getPeriod()));
          StringBuilder subjects = new StringBuilder();
          for (int i = 0; i < morningEntries.size(); i++) {
              subjects.append(morningEntries.get(i).getTitle());
              if (i < morningEntries.size() - 1) subjects.append(", ");
          }
          String startTime = morningEntries.get(0).getFormattedStartTime();
          title = context.getString(R.string.reminder_morning_title_active);
          content = context.getString(R.string.reminder_morning_body_active, morningEntries.size(), subjects.toString(), startTime);
      }

      showNotification(context, title, content);
  }

  private boolean checkAnyScheduleExists(ScheduleRepositoryImpl repo, String accountId) {
      for (int i = 1; i <= 7; i++) {
          if (!repo.getEntries(accountId, i).isEmpty()) return true;
      }
      return false;
  }

  private void showNotification(Context context, String title, String content) {
    NotificationManager notificationManager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    if (notificationManager == null) return;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel =
          new NotificationChannel(
              CHANNEL_ID,
              context.getString(R.string.reminder_channel_name),
              NotificationManager.IMPORTANCE_DEFAULT);
      notificationManager.createNotificationChannel(channel);
    }

    Intent mainIntent = new Intent(context, MainActivity.class);
    // Add extra flag to tell MainActivity/HomeActivity to navigate to Schedule tab
    mainIntent.putExtra("NAVIGATE_TO_SCHEDULE", true);
    mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    PendingIntent pendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

    notificationManager.notify(NOTIFICATION_ID, builder.build());
  }
}
