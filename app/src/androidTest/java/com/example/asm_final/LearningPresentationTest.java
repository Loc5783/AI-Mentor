package com.example.asm_final;

import static org.junit.Assert.*;

import android.content.*;
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

  @Test
  public void numberedPathKeepsReviewSeparateFromLessons()
      throws Exception {
    JSONArray units = new JSONArray();
    for (int i = 1; i <= 6; i++)
      units.put(
          new JSONObject()
              .put("id", "unit-" + i)
              .put("title", "Tên bài học dài để kiểm tra xuống dòng và khả năng đọc")
              .put("kind", i == 6 ? "FIVE_LESSON_REVIEW" : "LESSON")
              .put("xp", i == 6 ? 400 : 200)
              .put("state", i == 1 ? "COMPLETED" : i == 2 ? "AVAILABLE" : i == 3 ? "SKIPPED" : "LOCKED")
              .put("canOpen", i != 3));
    LessonPathData path = new LessonPathData(units);
    assertEquals(5, path.totalLessons);
    assertEquals(1, path.completedLessons);
    assertEquals(1, path.skippedLessons);
    assertTrue(path.units.get(5).review);
    assertEquals(5, path.units.get(5).ordinal);
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
