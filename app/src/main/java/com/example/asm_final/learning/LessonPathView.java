package com.example.asm_final.learning;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import com.example.asm_final.R;
import com.example.asm_final.utils.ScreenViews;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.function.Consumer;

/** Numbered learning stages with text states; no decorative icon or reward logic. */
public final class LessonPathView extends LinearLayout {
  public LessonPathView(Context context, LessonPathData path, Consumer<String> open) {
    super(context);
    setOrientation(VERTICAL);
    LinearLayout summary = ScreenViews.card(context);
    TextView progress =
        ScreenViews.text(
            context,
            context.getString(R.string.course_path_count, path.completedLessons, path.totalLessons),
            18,
            true);
    progress.setId(R.id.course_path_summary);
    summary.addView(progress);
    LinearProgressIndicator track = new LinearProgressIndicator(context);
    track.setId(R.id.course_path_progress);
    track.setMax(Math.max(1, path.totalLessons));
    track.setProgress(path.completedLessons);
    track.setTrackThickness(ScreenViews.dp(context, 6));
    track.setTrackCornerRadius(ScreenViews.dp(context, 3));
    summary.addView(track, new LayoutParams(-1, ScreenViews.dp(context, 12)));
    summary.addView(
        ScreenViews.text(context, context.getString(R.string.course_path_note), 13, false));
    addView(summary);
    for (LessonPathData.Unit unit : path.units) addView(stage(context, unit, open));
  }

  private View stage(Context c, LessonPathData.Unit unit, Consumer<String> open) {
    LinearLayout row = new LinearLayout(c);
    row.setOrientation(HORIZONTAL);
    row.setLayoutParams(new LayoutParams(-1, -2));
    FrameLayout marker = new FrameLayout(c);
    View line = new View(c);
    line.setBackgroundColor(ContextCompat.getColor(c, R.color.dividerColor));
    FrameLayout.LayoutParams rail =
        new FrameLayout.LayoutParams(ScreenViews.dp(c, 2), -1, Gravity.CENTER_HORIZONTAL);
    marker.addView(line, rail);
    TextView number =
        ScreenViews.text(
            c,
            unit.review
                ? c.getString(R.string.course_review_badge)
                : Integer.toString(unit.ordinal),
            14,
            true);
    number.setPadding(0, 0, 0, 0);
    number.setGravity(Gravity.CENTER);
    GradientDrawable badge = new GradientDrawable();
    badge.setCornerRadius(ScreenViews.dp(c, 14));
    badge.setColor(
        ContextCompat.getColor(
            c,
            "AVAILABLE".equals(unit.state) ? R.color.primaryContainerColor : R.color.surfaceColor));
    badge.setStroke(
        ScreenViews.dp(c, "AVAILABLE".equals(unit.state) ? 2 : 1),
        ContextCompat.getColor(
            c, "AVAILABLE".equals(unit.state) ? R.color.neumorphic_accent : R.color.dividerColor));
    number.setBackground(badge);
    FrameLayout.LayoutParams badgeSize =
        new FrameLayout.LayoutParams(
            ScreenViews.dp(c, 40), ScreenViews.dp(c, 40), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    badgeSize.topMargin = ScreenViews.dp(c, 24);
    marker.addView(number, badgeSize);
    marker.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    row.addView(marker, new LayoutParams(ScreenViews.dp(c, 48), -1));
    LinearLayout card = ScreenViews.card(c);
    card.setPadding(
        ScreenViews.dp(c, 16), ScreenViews.dp(c, 14), ScreenViews.dp(c, 16), ScreenViews.dp(c, 14));
    LayoutParams content = new LayoutParams(0, -2, 1);
    content.topMargin = ScreenViews.dp(c, 12);
    content.bottomMargin = ScreenViews.dp(c, 4);
    content.setMarginStart(ScreenViews.dp(c, 8));
    row.addView(card, content);
    String title =
        unit.review
            ? c.getString(R.string.course_review_range, Math.max(1, unit.ordinal - 4), unit.ordinal)
            : c.getString(R.string.course_unit, unit.ordinal, unit.title);
    card.addView(ScreenViews.text(c, title, 19, true));
    boolean done = "COMPLETED".equals(unit.state), locked = "LOCKED".equals(unit.state);
    card.addView(
        ScreenViews.text(
            c,
            done
                ? c.getString(R.string.course_path_retake)
                : c.getString(R.string.course_stage_reward, unit.xp),
            13,
            false));
    Button button =
        ScreenViews.button(
            c,
            locked
                ? R.string.course_locked
                : done ? R.string.course_done : R.string.course_available,
            () -> {
              if (!locked) open.accept(unit.id);
            });
    button.setEnabled(!locked);
    card.addView(button);
    if (locked)
      card.addView(ScreenViews.text(c, c.getString(R.string.course_unlock_note), 13, false));
    return row;
  }
}
