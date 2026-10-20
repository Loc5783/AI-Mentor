package com.example.asm_final.onboarding;

import java.util.*;

public final class OnboardingPolicy {
  public static final int PAGES = 4, PROFILE = 4, COMPLETE = 5;
  public static final Set<String> SUBJECTS =
      Collections.unmodifiableSet(
          new LinkedHashSet<>(
              Arrays.asList(
                  "MATH",
                  "LITERATURE",
                  "ENGLISH",
                  "HISTORY",
                  "GEOGRAPHY",
                  "CHEMISTRY",
                  "PHYSICS",
                  "BIOLOGY",
                  "ECONOMICS_LAW")));

  private OnboardingPolicy() {}

  public static boolean validName(String name) {
    return name != null && name.trim().length() >= 2 && name.trim().length() <= 80;
  }

  public static boolean validSubjects(String subjects) {
    if (subjects == null) return false;
    if (subjects.isEmpty()) return true; // Actual enrollment is chosen in Lesson.
    Set<String> seen = new HashSet<>();
    for (String subject : subjects.split(",", -1))
      if (!SUBJECTS.contains(subject) || !seen.add(subject)) return false;
    return true;
  }

  public static boolean valid(LearnerProfile p) {
    return p != null
        && p.userId > 0
        && validName(p.displayName)
        && p.grade >= 10
        && p.grade <= 12
        && validSubjects(p.subjects)
        && p.goal >= 0
        && p.goal < 4
        && (p.minutes == 0 || p.minutes >= 5 && p.minutes <= 240);
  }
}
