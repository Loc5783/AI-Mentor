package com.example.asm_final.dashboard;

import static org.junit.Assert.*;

import android.content.*;
import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.asm_final.R;
import com.example.asm_final.utils.ScreenViews;
import com.example.mentor.learning.SubjectId;
import java.util.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class DashboardSubjectsViewTest {
  private final android.app.Instrumentation ins = InstrumentationRegistry.getInstrumentation();

  private LearningOverviewData data() throws Exception {
    JSONArray subjects = new JSONArray();
    for (String id : new String[] {"biology", "chemistry", "physics"})
      subjects.put(
          new JSONObject()
              .put("subjectId", id)
              .put("curriculumAvailable", true)
              .put("totalXp", 200)
              .put("completedLessons", 1)
              .put("totalLessons", 20));
    return new LearningOverviewData(
        new JSONObject()
            .put("subjects", subjects)
            .put("totalXp", 600)
            .put("dailyXp", 600)
            .put("completedLessons", 3)
            .put("totalLessons", 60)
            .put("streakDays", 1)
            .put(
                "practice",
                new JSONObject()
                    .put("firstTryCorrect", 1)
                    .put("questionCount", 3)
                    .put("assisted", 1)));
  }

  private void bounded(View view) {
    if (view.getVisibility() == View.GONE) return;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        View child = group.getChildAt(i);
        if (child.getVisibility() == View.GONE) continue;
        assertTrue("Right overflow: " + child, child.getRight() <= group.getWidth());
        assertTrue("Left overflow: " + child, child.getLeft() >= 0);
        bounded(child);
      }
    }
  }

  @Test
  public void onlyChosenSubjectsAppearAndRemovedSubjectsNeverLeaveChartEntries() throws Exception {
    LearningOverviewData data = data();
    ins.runOnMainSync(
        () -> {
          Context base = ins.getTargetContext();
          for (String language : new String[] {"vi", "en"})
            for (int night :
                new int[] {Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
              Configuration config = new Configuration(base.getResources().getConfiguration());
              config.fontScale = 1.3f;
              config.setLocale(new Locale(language));
              config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
              Context context =
                  new androidx.appcompat.view.ContextThemeWrapper(
                      base.createConfigurationContext(config), R.style.Theme_ASMfinal);
              View root = LayoutInflater.from(context).inflate(R.layout.fragment_home, null, false);
              List<SubjectId> opened = new ArrayList<>();
              DashboardSubjectsView view =
                  new DashboardSubjectsView(
                      root,
                      (id, grade) -> {
                        assertEquals(10, grade);
                        opened.add(id);
                      });
              view.render(
                  new LearningOverviewRepository.Snapshot(
                      LearningOverviewRepository.Mode.LIVE, data, false),
                  EnumSet.of(SubjectId.BIOLOGY),
                  10);
              LinearLayout cards = root.findViewById(R.id.dashboard_subject_grid);
              assertEquals(1, cards.getChildCount());
              assertNotNull(cards.findViewWithTag("dashboard-subject:biology"));
              assertNull(cards.findViewWithTag("dashboard-subject:chemistry"));
              assertEquals(
                  context.getString(R.string.overview_lessons, 1, 20),
                  ((TextView) root.findViewById(R.id.tv_total_level_value)).getText().toString());
              LinearLayout legend = root.findViewById(R.id.chart_subject_legend);
              assertEquals(1, legend.getChildCount());
              assertTrue(((TextView) legend.getChildAt(0)).getText().toString().contains("200"));
              LinearLayout card = (LinearLayout) cards.getChildAt(0);
              card.getChildAt(card.getChildCount() - 1).performClick();
              assertEquals(Collections.singletonList(SubjectId.BIOLOGY), opened);
              int width = ScreenViews.dp(context, 320);
              root.measure(
                  View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                  View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
              root.layout(0, 0, width, root.getMeasuredHeight());
              bounded(root);
              view.render(
                  new LearningOverviewRepository.Snapshot(
                      LearningOverviewRepository.Mode.CACHED, data, false),
                  EnumSet.of(SubjectId.PHYSICS),
                  10);
              assertNull(cards.findViewWithTag("dashboard-subject:biology"));
              assertNotNull(cards.findViewWithTag("dashboard-subject:physics"));
              assertEquals(1, legend.getChildCount());
              assertEquals(
                  context.getString(R.string.overview_cached),
                  ((TextView) root.findViewById(R.id.tv_dashboard_status)).getText().toString());
              view.render(
                  new LearningOverviewRepository.Snapshot(
                      LearningOverviewRepository.Mode.LIVE, data, false),
                  EnumSet.noneOf(SubjectId.class),
                  10);
              assertEquals(0, legend.getChildCount());
              assertEquals(View.GONE, root.findViewById(R.id.chart_data_group).getVisibility());
              assertEquals(View.VISIBLE, root.findViewById(R.id.chart_placeholder).getVisibility());
            }
        });
  }

  @Test
  public void missingServerShowsAnExplanationInsteadOfFabricatedStatistics() {
    ins.runOnMainSync(
        () -> {
          Context context =
              new androidx.appcompat.view.ContextThemeWrapper(
                  ins.getTargetContext(), R.style.Theme_ASMfinal);
          View root = LayoutInflater.from(context).inflate(R.layout.fragment_home, null, false);
          DashboardSubjectsView view = new DashboardSubjectsView(root, (id, grade) -> {});
          view.render(
              new LearningOverviewRepository.Snapshot(
                  LearningOverviewRepository.Mode.LOCAL, null, false),
              EnumSet.of(SubjectId.CHEMISTRY),
              10);
          assertEquals(
              "—", ((TextView) root.findViewById(R.id.tv_total_level_value)).getText().toString());
          assertEquals(
              context.getString(R.string.dashboard_connect_notice),
              ((TextView) root.findViewById(R.id.tv_chart_summary)).getText().toString());
          assertNotNull(root.findViewWithTag("dashboard-subject:chemistry"));
          assertEquals(View.GONE, root.findViewById(R.id.chart_data_group).getVisibility());
          view.loading();
          assertEquals(
              0, ((LinearLayout) root.findViewById(R.id.dashboard_subject_grid)).getChildCount());
          view.failure();
          assertTrue(root.findViewById(R.id.button_chart_action).isEnabled());
          assertEquals(Boolean.TRUE, root.findViewById(R.id.button_chart_action).getTag());
        });
  }
}
