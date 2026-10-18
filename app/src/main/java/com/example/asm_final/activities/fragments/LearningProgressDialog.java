package com.example.asm_final.activities.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import com.example.asm_final.R;
import com.example.asm_final.dashboard.WeeklyQuizProgress;
import com.example.mentor.data.StreakSnapshot;

/** Account-scoped learning progress; keeps the original home dashboard uncluttered. */
public final class LearningProgressDialog extends DialogFragment {
  private View content;
  private int loadVersion;
  private android.content.Context timeContext;
  private final android.content.BroadcastReceiver timeChanges =
      new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context c, android.content.Intent i) {
          if (isAdded() && content != null) load();
        }
      };

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle state) {
    content =
        LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_learning_progress, null, false);
    content
        .findViewById(R.id.button_streak_quiz)
        .setOnClickListener(
            v -> {
              com.example.asm_final.activities.HomeActivity host =
                  (com.example.asm_final.activities.HomeActivity) requireActivity();
              dismiss();
              host.navigateTo(R.id.nav_category, false);
            });
    return new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.learning_progress_title)
        .setView(content)
        .setPositiveButton(android.R.string.ok, null)
        .create();
  }

  @Override
  public void onStart() {
    super.onStart();
    android.view.Window window = requireDialog().getWindow();
    if (window != null)
      window.setLayout(-1, (int) (getResources().getDisplayMetrics().heightPixels * .82f));
  }

  @Override
  public void onResume() {
    super.onResume();
    load();
    timeContext = requireContext().getApplicationContext();
    android.content.IntentFilter f = new android.content.IntentFilter();
    f.addAction(android.content.Intent.ACTION_DATE_CHANGED);
    f.addAction(android.content.Intent.ACTION_TIME_CHANGED);
    f.addAction(android.content.Intent.ACTION_TIMEZONE_CHANGED);
    ContextCompat.registerReceiver(
        timeContext, timeChanges, f, ContextCompat.RECEIVER_NOT_EXPORTED);
  }

  @Override
  public void onPause() {
    if (timeContext != null) {
      timeContext.unregisterReceiver(timeChanges);
      timeContext = null;
    }
    super.onPause();
  }

  @Override
  public void onDestroyView() {
    loadVersion++;
    content = null;
    super.onDestroyView();
  }

  private void load() {
    final View owner = content;
    final int version = ++loadVersion;
    if (owner == null) return;
    String username = getArguments() == null ? "" : getArguments().getString("USERNAME", "");
    com.example.asm_final.AppServices services =
        ((com.example.asm_final.AiMentorApplication) requireContext().getApplicationContext())
            .services();
    services
        .io()
        .execute(
            () -> {
              StreakSnapshot streak = null;
              WeeklyQuizProgress weekly = null;
              int userId = 0;
              try {
                userId = services.accountLookup().userId(username);
              } catch (RuntimeException ignored) {
              }
              if (userId > 0) {
                try {
                  streak = services.streaks().load(userId);
                } catch (RuntimeException ignored) {
                }
                try {
                  weekly = services.weeklyQuizzes().load(userId);
                } catch (RuntimeException ignored) {
                }
              }
              final StreakSnapshot loadedStreak = streak;
              final WeeklyQuizProgress loadedWeekly = weekly;
              new android.os.Handler(android.os.Looper.getMainLooper())
                  .post(
                      () -> {
                        if (isAdded() && content == owner && loadVersion == version) {
                          renderStreak(loadedStreak);
                          renderWeekly(loadedWeekly);
                        }
                      });
            });
  }

  private void renderWeekly(com.example.asm_final.dashboard.WeeklyQuizProgress weekly) {
    TextView xp = content.findViewById(R.id.weekly_quiz_xp);
    TextView period = content.findViewById(R.id.weekly_quiz_period);
    if (weekly == null) {
      xp.setText(R.string.weekly_quiz_error);
      period.setText("");
      xp.setOnClickListener(v -> load());
      return;
    }
    xp.setOnClickListener(null);
    xp.setText(getString(R.string.weekly_quiz_xp, weekly.experience));
    java.text.DateFormat format =
        java.text.DateFormat.getDateInstance(
            java.text.DateFormat.MEDIUM, getResources().getConfiguration().getLocales().get(0));
    period.setText(
        getString(
            R.string.weekly_quiz_period,
            format.format(new java.util.Date(weekly.startsAt)),
            format.format(new java.util.Date(weekly.endsAt - 1)),
            weekly.completedQuizzes));
  }

  private void renderStreak(com.example.mentor.data.StreakSnapshot streak) {
    View root = content;
    TextView current = root.findViewById(R.id.streak_current);
    TextView best = root.findViewById(R.id.streak_best);
    TextView status = root.findViewById(R.id.streak_status);
    android.widget.LinearLayout week = root.findViewById(R.id.streak_week_days);
    week.removeAllViews();
    if (streak == null) {
      root.findViewById(R.id.streak_milestone_progress).setVisibility(View.GONE);
      ((TextView) root.findViewById(R.id.streak_milestone)).setText("");
      current.setText(R.string.streak_error);
      best.setText("");
      status.setText("");
      current.setOnClickListener(v -> load());
      return;
    }
    current.setOnClickListener(null);
    current.setText(
        getResources().getQuantityString(R.plurals.streak_days, streak.current, streak.current));
    best.setText(getString(R.string.streak_best, streak.best));
    int target = com.example.mentor.data.StreakPolicy.nextMilestone(streak.current);
    ((TextView) root.findViewById(R.id.streak_milestone))
        .setText(getString(R.string.streak_milestone, target));
    android.widget.ProgressBar milestone = root.findViewById(R.id.streak_milestone_progress);
    milestone.setVisibility(View.VISIBLE);
    milestone.setMax(target);
    milestone.setProgress(streak.current);
    milestone.setContentDescription(
        getString(R.string.streak_milestone_progress, streak.current, target));
    android.widget.ImageView flame = root.findViewById(R.id.streak_flame);
    flame.setImageTintList(
        android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(),
                streak.current > 0 ? R.color.streak_flame_color : R.color.textColorSecondary)));
    if (streak.completedToday
        && flame.getTag() == null
        && (android.os.Build.VERSION.SDK_INT < 26
            || android.animation.ValueAnimator.areAnimatorsEnabled())) {
      flame.setTag(true);
      flame.setScaleX(.94f);
      flame.setScaleY(.94f);
      flame.animate().scaleX(1f).scaleY(1f).setDuration(240).start();
    }

    status.setText(
        streak.completedToday
            ? R.string.streak_completed
            : streak.current > 0
                ? R.string.streak_pending
                : streak.best > 0 ? R.string.streak_restart : R.string.streak_start);
    float density = getResources().getDisplayMetrics().density;
    java.util.Locale locale = getResources().getConfiguration().getLocales().get(0);
    java.text.DateFormat dateLabel =
        java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL, locale);
    dateLabel.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
    java.util.Calendar calendar =
        java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
    for (int offset = 6; offset >= 0; offset--) {
      long day = streak.today - offset;
      boolean learned = streak.days.contains(day);
      calendar.setTimeInMillis(day * 86400000L);
      TextView cell = new TextView(requireContext());
      cell.setText(
          String.format(
              locale,
              "%d\n%s",
              calendar.get(java.util.Calendar.DAY_OF_MONTH),
              getString(learned ? R.string.streak_done_glyph : R.string.streak_missing_glyph)));
      cell.setTextSize(13);
      cell.setGravity(android.view.Gravity.CENTER);
      cell.setPadding(0, (int) (6 * density), 0, (int) (6 * density));
      cell.setMinHeight((int) (52 * density));
      cell.setTextColor(
          ContextCompat.getColor(
              requireContext(), learned ? R.color.streak_flame_color : R.color.textColorSecondary));
      if (offset == 0) cell.setTypeface(cell.getTypeface(), android.graphics.Typeface.BOLD);
      android.graphics.drawable.GradientDrawable background =
          new android.graphics.drawable.GradientDrawable();
      background.setCornerRadius(10 * density);
      background.setColor(
          ContextCompat.getColor(
              requireContext(),
              learned ? R.color.streak_flame_container : R.color.surfaceVariantColor));
      if (offset == 0)
        background.setStroke(
            (int) Math.max(1, density),
            ContextCompat.getColor(requireContext(), R.color.streak_flame_color));
      cell.setBackground(background);
      cell.setContentDescription(
          getString(
              learned ? R.string.streak_day_done : R.string.streak_day_missing,
              dateLabel.format(calendar.getTime())));
      android.widget.LinearLayout.LayoutParams params =
          new android.widget.LinearLayout.LayoutParams(0, -2, 1);
      if (offset > 0) params.setMarginEnd((int) (4 * density));
      week.addView(cell, params);
    }
  }
}
