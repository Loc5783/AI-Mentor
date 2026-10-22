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

  public static final class Day {
    public final String day;
    public final long xp;
    public final boolean streakLit;

    Day(JSONObject value) throws JSONException {
      day = value.getString("day");
      java.text.SimpleDateFormat format =
          new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT);
      format.setLenient(false);
      java.text.ParsePosition position = new java.text.ParsePosition(0);
      if (!day.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
          || format.parse(day, position) == null
          || position.getIndex() != day.length()) throw new JSONException("Invalid day");
      xp = nonnegative(value, "xp");
      streakLit = value.getBoolean("streakLit");
    }
  }

  public final List<Day> week;
  public final long totalXp, dailyXp;
  public final int completedLessons,
      totalLessons,
      streakDays,
      firstTryCorrect,
      questionCount,
      assisted;
  public final String rankTier;
  public final List<Subject> subjects;

  public LearningOverviewData(JSONObject value) throws JSONException {
    totalXp = nonnegative(value, "totalXp");
    dailyXp = nonnegative(value, "dailyXp");
    completedLessons = count(value, "completedLessons");
    totalLessons = count(value, "totalLessons");
    streakDays = count(value, "streakDays");
    rankTier = value.optString("rankTier", "BRONZE");
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
      boolean isValidSubject = true;
      try {
        com.example.mentor.learning.SubjectId.fromWireId(subject.id);
      } catch (Exception e) {
        isValidSubject = false;
      }
      if (!isValidSubject || !ids.add(subject.id)) throw new JSONException("Invalid subject");
      try {
        xp = Math.addExact(xp, subject.experience);
        completed = Math.addExact(completed, subject.completed);
        total = Math.addExact(total, subject.total);
      } catch (ArithmeticException invalid) {
        throw new JSONException("Overview overflow");
      }
      parsed.add(subject);
    }
    if (ids.size() != items.length() || xp != totalXp || completed != completedLessons || total != totalLessons)
      throw new JSONException("Inconsistent subjects");
    parsed.sort(java.util.Comparator.comparing(s -> s.id));
    subjects = Collections.unmodifiableList(parsed);
    List<Day> days = new ArrayList<>();
    JSONArray series = value.optJSONArray("week");
    if (series != null) {
      if (series.length() != 7) throw new JSONException("Invalid week length");
      String previous = "";
      for (int i = 0; i < series.length(); i++) {
        Day day = new Day(series.getJSONObject(i));
        if (day.day.compareTo(previous) <= 0) throw new JSONException("Unordered week");
        days.add(day);
        previous = day.day;
      }
    }
    week = Collections.unmodifiableList(days);
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
