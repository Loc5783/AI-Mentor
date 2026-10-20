package com.example.asm_final.dashboard;

import java.util.*;
import org.json.*;

/** Validated presentation data, independent of Activities and transport/session handling. */
public final class LearningOverviewData {
  public static final class Subject {
    public final String id;
    public final long experience;
    public final int completed, total;
    public final boolean curriculumAvailable;

    Subject(JSONObject item) throws JSONException {
      id = item.getString("subjectId");
      curriculumAvailable = item.getBoolean("curriculumAvailable");
      experience = nonnegative(item, "totalXp");
      completed = count(item, "completedLessons");
      total = count(item, "totalLessons");
      if (completed > total) throw new JSONException("Invalid lesson counts");
    }
  }

  public final long totalXp, dailyXp;
  public final int completedLessons,
      totalLessons,
      streakDays,
      firstTryCorrect,
      questionCount,
      assisted;
  public final List<Subject> subjects;

  public LearningOverviewData(JSONObject value) throws JSONException {
    totalXp = nonnegative(value, "totalXp");
    dailyXp = nonnegative(value, "dailyXp");
    completedLessons = count(value, "completedLessons");
    totalLessons = count(value, "totalLessons");
    streakDays = count(value, "streakDays");
    JSONObject practice = value.getJSONObject("practice");
    firstTryCorrect = count(practice, "firstTryCorrect");
    questionCount = count(practice, "questionCount");
    assisted = count(practice, "assisted");
    if (completedLessons > totalLessons
        || firstTryCorrect > questionCount
        || assisted > questionCount
        || dailyXp > totalXp) throw new JSONException("Inconsistent overview");
    List<Subject> parsed = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    JSONArray items = value.getJSONArray("subjects");
    long xp = 0;
    int completed = 0, total = 0;
    for (int i = 0; i < items.length(); i++) {
      Subject subject = new Subject(items.getJSONObject(i));
      if (!Arrays.asList("biology", "chemistry", "physics").contains(subject.id)
          || !ids.add(subject.id)) throw new JSONException("Invalid subject");
      try {
        xp = Math.addExact(xp, subject.experience);
        completed = Math.addExact(completed, subject.completed);
        total = Math.addExact(total, subject.total);
      } catch (ArithmeticException invalid) {
        throw new JSONException("Overview overflow");
      }
      parsed.add(subject);
    }
    if (ids.size() != 3 || xp != totalXp || completed != completedLessons || total != totalLessons)
      throw new JSONException("Inconsistent subjects");
    parsed.sort(java.util.Comparator.comparing(s -> s.id));
    subjects = Collections.unmodifiableList(parsed);
  }

  private static long nonnegative(JSONObject item, String name) throws JSONException {
    long value = item.getLong(name);
    if (value < 0) throw new JSONException("Negative progress");
    return value;
  }

  private static int count(JSONObject item, String name) throws JSONException {
    long value = nonnegative(item, name);
    if (value > Integer.MAX_VALUE) throw new JSONException("Count overflow");
    return (int) value;
  }
}
