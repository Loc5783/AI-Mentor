package com.example.asm_final.competition;

/** Only friendship changes invalidate the friends list. */
public final class FriendshipEventPolicy {
  private FriendshipEventPolicy() {}

  public static boolean affects(String type, String id, String owner) {
    return "FRIENDSHIP".equals(type);
  }
}
