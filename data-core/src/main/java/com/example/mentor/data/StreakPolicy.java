package com.example.mentor.data;

import java.util.*;

/** Local calendar days, not rolling 24-hour windows; handles DST and year boundaries. */
public final class StreakPolicy {
  private StreakPolicy() {}

  /** AIMentor milestones; presentation goals do not grant XP or change streak eligibility. */
  public static int nextMilestone(int current) {
    int safe = Math.max(0, current);
    for (int milestone : new int[] {3, 7, 14, 30, 50, 100, 365})
      if (safe < milestone) return milestone;
    return (int) Math.min(Integer.MAX_VALUE, ((long) safe / 365 + 1) * 365);
  }

  public static long dayId(long time, TimeZone zone) {
    Calendar local = Calendar.getInstance(zone);
    local.setTimeInMillis(time);
    Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    utc.clear();
    utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
    return utc.getTimeInMillis() / 86400000L;
  }

  public static StreakSnapshot calculate(Collection<Long> recordedDays, long now, TimeZone zone) {
    long today = dayId(now, zone);
    TreeSet<Long> days = new TreeSet<>();
    for (Long day : recordedDays) if (day != null && day >= 0 && day <= today) days.add(day);
    int best = 0, run = 0;
    long previous = Long.MIN_VALUE;
    for (long day : days) {
      run = day == previous + 1 ? run + 1 : 1;
      best = Math.max(best, run);
      previous = day;
    }
    long anchor = days.contains(today) ? today : today - 1;
    int current = 0;
    while (days.contains(anchor - current)) current++;
    return new StreakSnapshot(current, best, today, days);
  }
}
