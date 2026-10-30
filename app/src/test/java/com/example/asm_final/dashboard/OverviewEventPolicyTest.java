package com.example.asm_final.dashboard;

import static org.junit.Assert.*;

import org.junit.Test;

public class OverviewEventPolicyTest {
  @Test
  public void completionsAndEnrollmentsInvalidateTheSummary() {
    assertTrue(OverviewEventPolicy.affectsSummary("COMPLETION", "attempt", "me"));
    assertTrue(OverviewEventPolicy.affectsSummary("ENROLLMENT", "course", "me"));
  }

  @Test
  public void onlyOwnPublicProfileInvalidatesPrivateSummary() {
    assertTrue(OverviewEventPolicy.affectsSummary("PROFILE", "me", "me"));
    assertFalse(OverviewEventPolicy.affectsSummary("PROFILE", "another", "me"));
    assertFalse(OverviewEventPolicy.affectsSummary("PROFILE", null, null));
  }

  @Test
  public void ownFriendshipChangesRefreshProfileFriends() {
    assertTrue(OverviewEventPolicy.affectsSummary("FRIENDSHIP", "current", "me"));
  }

  @Test
  public void chatTypingQuestionSavesAndGlobalRankingDoNotReloadPersonalStats() {
    for (String type : new String[] {"MESSAGE", "ATTEMPT", "RANKING", "UNKNOWN"})
      assertFalse(OverviewEventPolicy.affectsSummary(type, "current", "me"));
  }
}
