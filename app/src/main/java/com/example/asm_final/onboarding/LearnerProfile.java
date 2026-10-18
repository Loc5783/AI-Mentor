package com.example.asm_final.onboarding;

/** Account-owned onboarding state and learning preferences; no credentials. */
public final class LearnerProfile {
  public final int userId, step, grade, goal, minutes;
  public final String displayName, subjects;
  public final long consentedAt;

  public LearnerProfile(
      int id,
      int step,
      String name,
      int grade,
      String subjects,
      int goal,
      int minutes,
      long consentedAt) {
    this.userId = id;
    this.step = step;
    this.displayName = name == null ? "" : name;
    this.grade = grade;
    this.subjects = subjects == null ? "" : subjects;
    this.goal = goal;
    this.minutes = minutes;
    this.consentedAt = consentedAt;
  }

  public boolean complete() {
    return step == OnboardingPolicy.COMPLETE;
  }
}
