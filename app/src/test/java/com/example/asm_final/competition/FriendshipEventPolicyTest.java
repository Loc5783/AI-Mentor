package com.example.asm_final.competition;

import static org.junit.Assert.*;
import org.junit.Test;

public class FriendshipEventPolicyTest {
  @Test public void friendsOnlyRefreshForFriendshipChanges() {
    assertTrue(FriendshipEventPolicy.affects("FRIENDSHIP", "friend", "owner"));
    assertFalse(FriendshipEventPolicy.affects("COMPLETION", "attempt", "owner"));
    assertFalse(FriendshipEventPolicy.affects("PROFILE", "friend", "owner"));
    assertFalse(FriendshipEventPolicy.affects("SCHEDULE", "todo", "owner"));
  }
}
