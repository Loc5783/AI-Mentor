package com.example.mentor.learning;

/** Shared policy only. A completion ledger must authorize and persist each award atomically. */
public final class LearningRewardPolicy {
  public enum Completion {
    LESSON,
    FIVE_LESSON_REVIEW
  }

  private LearningRewardPolicy() {}

  /** alreadyAwarded is looked up by account/subject/grade/stable unit ID, not by attempt ID. */
  public static int experience(Completion kind, boolean alreadyAwarded) {
    if (kind == null) throw new IllegalArgumentException("Completion kind required");
    if (alreadyAwarded) return 0;
    return kind == Completion.LESSON ? 200 : 400;
  }

  /** Completing a genuine retake may light today's streak even though its EXP award is zero. */
  public static boolean lightsStreak(boolean alreadyLitToday) {
    return !alreadyLitToday;
  }
}
