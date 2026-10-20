package com.example.asm_final.competition;

/** Unlike a personal summary, a board must react to other learners and accepted friendships. */
public final class RankingEventPolicy {
  private RankingEventPolicy() {}

  public static boolean affects(String type, String id, String owner) {
    return "RANKING".equals(type)
        || "PROFILE".equals(type)
        || "FRIENDSHIP".equals(type)
        || "COMPLETION".equals(type);
  }
}
