package com.example.asm_final.dashboard;

/** Public events about other learners must not refresh the current learner's private summary. */
public final class OverviewEventPolicy {
  private OverviewEventPolicy() {}

  public static boolean affectsSummary(String type, String id, String owner) {
    return "COMPLETION".equals(type)
        || "ENROLLMENT".equals(type)
        || "LEARNING_PROFILE".equals(type)
        || ("PROFILE".equals(type) && owner != null && owner.equals(id));
  }
}
