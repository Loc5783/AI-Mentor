package com.example.asm_final.dashboard;

import static org.junit.Assert.*;

import android.content.*;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class OverviewDataTest {
  @Test
  public void weeklySeriesPreservesZeroXpStreakAndRejectsDuplicateDays() throws Exception {
    // Retaking a lesson can light a streak with zero additional XP.
    JSONObject value =
        new JSONObject()
            .put(
                "subjects",
                new JSONArray()
                    .put(subject("biology"))
                    .put(subject("chemistry"))
                    .put(subject("physics")))
            .put("totalXp", 0)
            .put("dailyXp", 0)
            .put("completedLessons", 0)
            .put("totalLessons", 0)
            .put("streakDays", 1)
            .put(
                "practice",
                new JSONObject()
                    .put("firstTryCorrect", 0)
                    .put("questionCount", 0)
                    .put("assisted", 0));
    JSONArray week = new JSONArray();
    for (int i = 1; i <= 7; i++)
      week.put(new JSONObject().put("day", "2026-10-0" + i).put("xp", 0).put("streakLit", i == 7));
    value.put("week", week);
    LearningOverviewData parsed = new LearningOverviewData(value);
    assertEquals(7, parsed.week.size());
    assertTrue(parsed.week.get(6).streakLit);
    assertEquals(0, parsed.week.get(6).xp);
    week.getJSONObject(6).put("day", "2026-10-06");
    try {
      new LearningOverviewData(value);
      fail("Duplicate days");
    } catch (JSONException expected) {
    }
  }

  private JSONObject subject(String id) throws JSONException {
    return new JSONObject()
        .put("subjectId", id)
        .put("curriculumAvailable", true)
        .put("totalXp", 0)
        .put("completedLessons", 0)
        .put("totalLessons", 0);
  }
}
