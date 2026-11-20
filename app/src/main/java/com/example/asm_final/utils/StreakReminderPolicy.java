package com.example.asm_final.utils;

/** Send a warning only for an active streak that has not been continued today. */
public final class StreakReminderPolicy {
  private StreakReminderPolicy() {}

  public static boolean shouldWarn(String today, String yesterday,
      String latestDay, String previousDay, boolean todayLit, boolean yesterdayLit,
      int streakDays) {
    return streakDays > 0 && today.equals(latestDay) && yesterday.equals(previousDay)
        && !todayLit && yesterdayLit;
  }
}
