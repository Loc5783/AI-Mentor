package com.example.asm_final.learning;

import java.util.*;
import org.json.*;

/** Presentation snapshot; reward/unlock decisions remain authoritative server values. */
public final class LessonPathData {
  public static final class Stage {
    public final int number;
    public final List<Unit> units;

    private Stage(int number, List<Unit> units) {
      this.number = number;
      this.units = Collections.unmodifiableList(new ArrayList<>(units));
    }
  }

  public static final class Unit {
    public final String id, title, state;
    public final boolean review, canOpen;
    public final int ordinal, xp;

    private Unit(JSONObject value, int ordinal) throws JSONException {
      id = value.getString("id");
      title = value.getString("title");
      state = value.getString("state");
      review = "FIVE_LESSON_REVIEW".equals(value.getString("kind"));
      canOpen = value.optBoolean("canOpen", !"LOCKED".equals(value.getString("state")));
      this.ordinal = ordinal;
      xp = value.getInt("xp");
      if (!Arrays.asList("AVAILABLE", "COMPLETED", "LOCKED", "SKIPPED").contains(state) || xp < 0)
        throw new JSONException("Invalid path state");
    }
  }

  public final List<Unit> units;
  public final List<Stage> visibleStages;
  public final int completedLessons, skippedLessons, totalLessons;

  public LessonPathData(JSONArray values) throws JSONException {
    List<Unit> result = new ArrayList<>();
    List<Stage> stages = new ArrayList<>();
    List<Unit> visibleStage = new ArrayList<>();
    int stageNumber = 1;
    int ordinal = 0, completed = 0, skipped = 0;
    for (int i = 0; i < values.length(); i++) {
      JSONObject value = values.getJSONObject(i);
      boolean review = "FIVE_LESSON_REVIEW".equals(value.getString("kind"));
      if (!review) ordinal++;
      Unit unit = new Unit(value, ordinal);
      if (!review && "COMPLETED".equals(unit.state)) completed++;
      if (!review && "SKIPPED".equals(unit.state)) skipped++;
      result.add(unit);
      if (!"SKIPPED".equals(unit.state)) visibleStage.add(unit);
      if (review) {
        if (!visibleStage.isEmpty()) stages.add(new Stage(stageNumber, visibleStage));
        visibleStage.clear();
        stageNumber++;
      }
    }
    if (!visibleStage.isEmpty()) stages.add(new Stage(stageNumber, visibleStage));
    units = Collections.unmodifiableList(result);
    visibleStages = Collections.unmodifiableList(stages);
    totalLessons = ordinal;
    completedLessons = completed;
    skippedLessons = skipped;
  }
}
