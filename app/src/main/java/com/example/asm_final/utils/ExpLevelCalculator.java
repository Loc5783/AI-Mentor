package com.example.asm_final.utils;

import com.example.asm_final.R;

/*
 * Role: Utility class for App Shell, grouping reusable logic such as user sessions, levels, formatting, or data conversion.
 * Purpose: Moving shared logic into helpers reduces duplication and keeps Activities and Fragments focused on screen coordination.
 */
public class ExpLevelCalculator {

  /** Sum imported subject totals without overflowing back to level one. */
  public static int totalExperience(int... subjectExperience) {
    long total = 0;
    for (int experience : subjectExperience) total += Math.max(0, experience);
    return (int) Math.min(Integer.MAX_VALUE, total);
  }

  public static int getLevel(int totalExp) {
    return com.example.mentor.data.QuizPolicy.level(totalExp);
  }

  /*
   * Responsibility: Reads or loads data for `get progress percentage` from the appropriate data source.
   * Purpose: Centralizing read operations makes null handling, error handling, and data sources clearer.
   */
  public static int getProgressPercentage(int totalExp) {
    return com.example.mentor.data.QuizPolicy.progress(totalExp);
  }

  public static int getLevelTitleResId(int level) {
    if (level < 5) return R.string.home_level_1;
    if (level < 10) return R.string.home_level_2;
    if (level < 15) return R.string.home_level_3;
    if (level < 20) return R.string.home_level_4;
    return R.string.home_level_5;
  }

  /*
   * Responsibility: Reads or loads data for `get exp to next level` from the appropriate data source.
   * Purpose: Centralizing read operations makes null handling, error handling, and data sources clearer.
   */
  public static int getExpToNextLevel(int totalExp) {
    return com.example.mentor.data.QuizPolicy.remaining(totalExp);
  }
}
