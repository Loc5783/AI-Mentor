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
import com.example.asm_final.activities.HomeActivity;
import com.example.asm_final.accounts.AuthSession;
import com.example.asm_final.dashboard.LearningOverviewData;
import com.example.asm_final.dashboard.LearningOverviewRepository;
import com.example.asm_final.onboarding.LearnerProfile;
import com.example.mentor.data.ActiveUser;
import com.example.mentor.schedule.domain.ScheduleEntry;
import com.example.mentor.schedule.domain.ScheduleRepository;
import com.example.mentor.schedule.domain.TodoTask;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {
  private static final String CHANNEL_ID = "study_schedule_channel";
  private static final int NOTIFICATION_ID = 101;
  private static final int STREAK_NOTIFICATION_ID = 102;

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
    if (!ReminderHelper.ACTION_EVENING_REMINDER.equals(action)
        && !ReminderHelper.ACTION_MORNING_REMINDER.equals(action)
        && !ReminderHelper.ACTION_STREAK_REMINDER.equals(action)
        && !ReminderHelper.ACTION_EXAM_REMINDER.equals(action)) return;

    // Schedule data may live in Supabase. Keep network/database work off the receiver's
    // main thread and read it from the same repository the schedule screen uses.
    final PendingResult pending = goAsync();
    com.example.asm_final.AiMentorApplication app =
        (com.example.asm_final.AiMentorApplication) context.getApplicationContext();
    app.services().io().execute(() -> {
      ScheduleRepository repository = null;
      try {
        if (!ReminderHelper.isReminderEnabled(context) || ActiveUser.get(context) != owner) return;
        if (ReminderHelper.ACTION_STREAK_REMINDER.equals(action)) {
          handleStreakReminder(context, owner, app);
        } else if (ReminderHelper.ACTION_EVENING_REMINDER.equals(action)) {
          repository = app.scheduleRepository();
          handleEveningReminder(context, owner, repository);
        } else if (ReminderHelper.ACTION_EXAM_REMINDER.equals(action)) {
          repository = app.scheduleRepository();
          handleExamReminder(context, owner, repository);
        } else {
          repository = app.scheduleRepository();
          handleMorningReminder(context, owner, repository);
        }
      } catch (Exception failure) {
        android.util.Log.w("ReminderReceiver", "Could not load schedule for reminder", failure);
      } finally {
        try {
          if (repository instanceof com.example.mentor.schedule.domain.ScheduleImportRepository) {
            ((com.example.mentor.schedule.domain.ScheduleImportRepository) repository).close();
          }
        } catch (RuntimeException closeFailure) {
          android.util.Log.w("ReminderReceiver", "Could not close schedule repository", closeFailure);
        } finally {
          pending.finish();
        }
      }
    });
  }

  private void handleExamReminder(Context context, int owner, ScheduleRepository repo) {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    int day = cal.get(Calendar.DAY_OF_WEEK) - 1;
    if (day == 0) day = 7;
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    format.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    String today = format.format(cal.getTime());
    java.util.ArrayList<String> titles = new java.util.ArrayList<>();
    for (TodoTask task : repo.getTodos(String.valueOf(owner), day)) {
      if (!task.isCompleted() && today.equals(task.getDueDate()) &&
          (task.getTitle().contains(" · Ôn thi · ") ||
              task.getTitle().contains(" · Exam revision · "))) titles.add(task.getTitle());
    }
    if (!titles.isEmpty() && ActiveUser.get(context) == owner) {
      showNotification(context, context.getString(R.string.exam_plan_reminder_title),
          context.getString(R.string.exam_plan_reminder_body, android.text.TextUtils.join(", ", titles)), 103);
    }
  }

  private void handleStreakReminder(Context context, int owner,
      com.example.asm_final.AiMentorApplication app) {
    TimeZone zone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
    Calendar now = Calendar.getInstance(zone);
    if (now.get(Calendar.HOUR_OF_DAY) < 22) return;
    SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    dayFormat.setTimeZone(zone);
    String today = dayFormat.format(now.getTime());
    now.add(Calendar.DAY_OF_YEAR, -1);
    String yesterday = dayFormat.format(now.getTime());
    LearnerProfile profile = app.services().learners().load(owner);
    if (profile == null || profile.grade < 10 || profile.grade > 12) return;
    LearningOverviewRepository.Snapshot snapshot = app.services().overview().load(owner, profile.grade);
    if (snapshot.mode != LearningOverviewRepository.Mode.LIVE || snapshot.data == null
        || snapshot.data.week.size() < 2 || ActiveUser.get(context) != owner) return;
    List<LearningOverviewData.Day> week = snapshot.data.week;
    LearningOverviewData.Day last = week.get(week.size() - 1);
    LearningOverviewData.Day previous = week.get(week.size() - 2);
    if (StreakReminderPolicy.shouldWarn(today, yesterday, last.day, previous.day,
        last.streakLit, previous.streakLit, snapshot.data.streakDays)) {
      showNotification(context, context.getString(R.string.reminder_streak_title),
          context.getString(R.string.reminder_streak_body), STREAK_NOTIFICATION_ID);
    }
  }

  private void handleEveningReminder(Context context, int owner, ScheduleRepository repo) {
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_YEAR, 1);
      int tomorrowDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
      if (tomorrowDay == 0) tomorrowDay = 7;

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

  private void handleMorningReminder(Context context, int owner, ScheduleRepository repo) {
      Calendar cal = Calendar.getInstance();
      int todayDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
      if (todayDay == 0) todayDay = 7;

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

  private boolean checkAnyScheduleExists(ScheduleRepository repo, String accountId) {
      for (int i = 1; i <= 7; i++) {
          if (!repo.getEntries(accountId, i).isEmpty()) return true;
      }
      return false;
  }

  private void showNotification(Context context, String title, String content) {
    showNotification(context, title, content, NOTIFICATION_ID);
  }

  private void showNotification(Context context, String title, String content, int notificationId) {
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

    String username = AuthSession.currentUsername(context);
    Intent mainIntent = new Intent(context,
        username.isEmpty() ? MainActivity.class : HomeActivity.class);
    if (!username.isEmpty()) mainIntent.putExtra("USERNAME", username);
    mainIntent.putExtra(HomeActivity.EXTRA_START_DESTINATION,
        notificationId == STREAK_NOTIFICATION_ID ? R.id.nav_home : R.id.nav_schedule);
    mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    PendingIntent pendingIntent =
        PendingIntent.getActivity(
            context,
            notificationId,
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

    notificationManager.notify(notificationId, builder.build());
  }
}
