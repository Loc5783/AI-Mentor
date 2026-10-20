package com.example.asm_final.dashboard;

import android.content.Context;
import android.view.View;
import android.widget.*;
import androidx.core.content.ContextCompat;
import com.example.asm_final.R;
import com.example.asm_final.utils.*;
import com.example.mentor.learning.SubjectId;
import java.util.*;

/** Renders only chosen school subjects. No legacy module totals or fabricated zero statistics. */
public final class DashboardSubjectsView {
  public interface OpenLesson {
    void open(SubjectId subject, int grade);
  }

  private final View root;
  private final Context context;
  private final OpenLesson open;
  private final LinearLayout cards, legend;
  private static final int[] COLORS = {
    R.color.cat_science_text, R.color.cat_math_text, R.color.cat_history_text
  };

  public DashboardSubjectsView(View root, OpenLesson open) {
    this.root = root;
    this.context = root.getContext();
    this.open = open;
    cards = root.findViewById(R.id.dashboard_subject_grid);
    legend = root.findViewById(R.id.chart_subject_legend);
  }

  private void text(int id, CharSequence value) {
    ((TextView) root.findViewById(id)).setText(value);
  }

  private String string(int id, Object... args) {
    return context.getString(id, args);
  }

  private void chart(boolean visible, int title, CharSequence message, boolean loading) {
    root.findViewById(R.id.chart_data_group).setVisibility(visible ? View.VISIBLE : View.GONE);
    root.findViewById(R.id.chart_placeholder).setVisibility(visible ? View.GONE : View.VISIBLE);
    root.findViewById(R.id.chart_placeholder_icon).setVisibility(View.GONE);
    root.findViewById(R.id.chart_loading_spinner).setVisibility(loading ? View.VISIBLE : View.GONE);
    text(R.id.chart_placeholder_title, string(title));
    text(R.id.tv_chart_summary, message);
  }

  public void loading() {
    cards.removeAllViews();
    legend.removeAllViews();
    text(R.id.tv_total_level_value, "—");
    text(R.id.tv_level_title, string(R.string.auth_processing));
    text(R.id.tv_exp_detail, "");
    text(R.id.text_recent_empty, "");
    ((ProgressBar) root.findViewById(R.id.pb_total_exp)).setProgress(0);
    root.findViewById(R.id.tv_dashboard_status).setVisibility(View.GONE);
    root.findViewById(R.id.button_chart_action).setEnabled(false);
    chart(false, R.string.home_chart_loading, string(R.string.home_chart_loading_hint), true);
  }

  public void failure() {
    loading();
    chart(false, R.string.home_chart_error_title, string(R.string.overview_error), false);
    action(true, R.string.overview_reload);
  }

  private void action(boolean retry, int title) {
    TextView button = root.findViewById(R.id.button_chart_action);
    button.setVisibility(View.VISIBLE);
    button.setEnabled(true);
    button.setTag(retry);
    button.setText(title);
  }

  public void render(
      LearningOverviewRepository.Snapshot snapshot, Set<SubjectId> selected, int grade) {
    cards.removeAllViews();
    legend.removeAllViews();
    LearningOverviewData data = snapshot.data;
    boolean local = snapshot.mode == LearningOverviewRepository.Mode.LOCAL;
    boolean cached = snapshot.mode == LearningOverviewRepository.Mode.CACHED;
    text(R.id.tv_total_level_label, string(R.string.overview_title));
    text(R.id.dashboard_recent_heading, string(R.string.overview_practice_heading));
    int notice =
        snapshot.needsLogin
            ? R.string.overview_expired
            : local
                ? R.string.dashboard_connect_notice
                : cached
                    ? R.string.overview_cached
                    : data == null ? R.string.overview_error : R.string.overview_current;
    text(R.id.tv_dashboard_status, string(notice));
    root.findViewById(R.id.tv_dashboard_status).setVisibility(View.VISIBLE);
    List<SimplePieChartView.Slice> slices = new ArrayList<>();
    int completed = 0, total = 0;
    long selectedXp = 0;
    String language = LocaleHelper.getLanguage(context);
    for (SubjectId id : SubjectId.values()) {
      if (!selected.contains(id)) continue;
      LearningOverviewData.Subject progress = null;
      if (data != null)
        for (LearningOverviewData.Subject s : data.subjects)
          if (id.wireId.equals(s.id)) progress = s;
      int color =
          ContextCompat.getColor(
              context, COLORS[id == SubjectId.BIOLOGY ? 0 : id == SubjectId.PHYSICS ? 1 : 2]);
      String name = id.label(language);
      LinearLayout card = ScreenViews.card(context);
      card.setTag("dashboard-subject:" + id.wireId);
      card.addView(ScreenViews.text(context, name, 20, true));
      if (progress != null) {
        completed += progress.completed;
        total += progress.total;
        selectedXp += progress.experience;
        card.addView(
            ScreenViews.text(
                context,
                string(
                    R.string.overview_subject_progress,
                    progress.completed,
                    progress.total,
                    progress.experience),
                14,
                false));
        slices.add(new SimplePieChartView.Slice(progress.experience, color));
        TextView label =
            ScreenViews.text(
                context, string(R.string.home_chart_subject, name, progress.experience), 14, false);
        label.setTextColor(color);
        legend.addView(label);
      } else
        card.addView(
            ScreenViews.text(context, string(R.string.dashboard_progress_pending), 14, false));
      card.addView(ScreenViews.button(context, R.string.course_open, () -> open.open(id, grade)));
      cards.addView(card);
    }
    if (selected.isEmpty())
      cards.addView(ScreenViews.text(context, string(R.string.lesson_choose_prompt), 16, false));
    text(
        R.id.tv_total_level_value,
        data == null || total == 0 ? "—" : string(R.string.overview_lessons, completed, total));
    text(
        R.id.tv_level_title,
        data == null ? string(notice) : string(R.string.overview_daily_xp, data.dailyXp));
    text(R.id.tv_exp_detail, data == null ? "" : string(R.string.overview_streak, data.streakDays));
    ((ProgressBar) root.findViewById(R.id.pb_total_exp))
        .setProgress(total == 0 ? 0 : (int) (completed * 100L / total));
    SimplePieChartView pie = root.findViewById(R.id.pie_chart_exp);
    pie.setData(slices);
    pie.setContentDescription(string(R.string.home_chart_accessibility, selectedXp));
    text(
        R.id.tv_chart_center,
        selectedXp == 0 ? string(R.string.zero_count) : string(R.string.home_chart_center));
    chart(
        selectedXp > 0,
        data == null && !local ? R.string.home_chart_error_title : R.string.home_chart_empty_title,
        data == null
            ? string(notice)
            : selectedXp == 0
                ? string(R.string.dashboard_chart_empty)
                : string(R.string.home_chart_total, selectedXp)
                    + "\n"
                    + string(
                        cached ? R.string.overview_cached : R.string.dashboard_selected_source),
        false);
    text(
        R.id.text_recent_empty,
        data == null
            ? string(R.string.dashboard_progress_pending)
            : data.questionCount == 0
                ? string(R.string.overview_no_practice)
                : string(R.string.overview_practice, data.firstTryCorrect, data.questionCount)
                    + "\n"
                    + string(R.string.overview_assisted, data.assisted));
    action(
        !local && (data == null || cached || snapshot.needsLogin),
        snapshot.needsLogin
            ? R.string.overview_sign_in
            : !local && (data == null || cached)
                ? R.string.overview_reload
                : R.string.home_chart_start);
  }
}
