package com.example.asm_final;

import static org.junit.Assert.*;

import android.content.*;
import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.asm_final.learning.*;
import com.example.asm_final.utils.*;
import java.util.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class LearningPresentationTest {
  private final android.app.Instrumentation ins = InstrumentationRegistry.getInstrumentation();
  private final Context context = ins.getTargetContext();

  private void buttons(View view, List<Button> result) {
    if (view instanceof Button) result.add((Button) view);
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
        buttons(((ViewGroup) view).getChildAt(i), result);
  }

  private void bounded(View view) {
    if (view instanceof ImageView) fail("Decorative image in text learning path");
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        View child = group.getChildAt(i);
        assertTrue("Horizontal overflow", child.getRight() <= group.getWidth());
        assertTrue("Negative horizontal position", child.getLeft() >= 0);
        bounded(child);
      }
    }
  }

  @Test
  public void numberedPathKeepsReviewSeparateAndFitsLargeTextBothThemesAndLanguages()
      throws Exception {
    JSONArray units = new JSONArray();
    for (int i = 1; i <= 6; i++)
      units.put(
          new JSONObject()
              .put("id", "unit-" + i)
              .put("title", "Tên bài học dài để kiểm tra xuống dòng và khả năng đọc")
              .put("kind", i == 6 ? "FIVE_LESSON_REVIEW" : "LESSON")
              .put("xp", i == 6 ? 400 : 200)
              .put("state", i == 1 ? "COMPLETED" : i == 2 ? "AVAILABLE" : "LOCKED"));
    LessonPathData path = new LessonPathData(units);
    assertEquals(5, path.totalLessons);
    assertEquals(1, path.completedLessons);
    assertTrue(path.units.get(5).review);
    assertEquals(5, path.units.get(5).ordinal);
    ins.runOnMainSync(
        () -> {
          for (String language : new String[] {"vi", "en"})
            for (int mode :
                new int[] {Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
              Configuration config = new Configuration(context.getResources().getConfiguration());
              config.fontScale = 1.3f;
              config.setLocale(new Locale(language));
              config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | mode;
              Context themed =
                  new androidx.appcompat.view.ContextThemeWrapper(
                      context.createConfigurationContext(config), R.style.Theme_ASMfinal);
              List<String> opened = new ArrayList<>();
              LessonPathView view = new LessonPathView(themed, path, opened::add);
              int width = ScreenViews.dp(themed, 320);
              view.measure(
                  View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                  View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
              view.layout(0, 0, width, view.getMeasuredHeight());
              bounded(view);
              ProgressBar progress = view.findViewById(R.id.course_path_progress);
              assertEquals(5, progress.getMax());
              assertEquals(1, progress.getProgress());
              List<Button> actions = new ArrayList<>();
              buttons(view, actions);
              assertEquals(6, actions.size());
              for (Button action : actions) action.performClick();
              assertEquals(Arrays.asList("unit-1", "unit-2"), opened);
            }
        });
  }

  @Test
  public void optionalMotionNeverDelaysButtonActionsAndDisabledMotionLeavesFinalState() {
    boolean disabled =
        "off".equals(InstrumentationRegistry.getArguments().getString("motionExpected", "on"));
    ins.runOnMainSync(
        () -> {
          Context themed =
              new androidx.appcompat.view.ContextThemeWrapper(context, R.style.Theme_ASMfinal);
          assertEquals(!disabled, MotionSettings.enabled(themed));
          int[] actions = {0};
          Button button = ScreenViews.button(themed, R.string.course_next, () -> actions[0]++);
          SoftMotion.enter(button);
          SoftMotion.complete(button);
          button.performClick();
          assertEquals(1, actions[0]);
          if (disabled) {
            assertEquals(1f, button.getAlpha(), 0f);
            assertEquals(1f, button.getScaleX(), 0f);
            assertEquals(1f, button.getScaleY(), 0f);
            assertEquals(0f, button.getTranslationY(), 0f);
            assertNull(button.getStateListAnimator());
          }
          button.animate().cancel();
        });
  }
}
