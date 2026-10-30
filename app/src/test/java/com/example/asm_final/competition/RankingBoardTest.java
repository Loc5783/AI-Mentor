package com.example.asm_final.competition;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

public class RankingBoardTest {
  private final CompetitionQuery xp =
      new CompetitionQuery(CompetitionQuery.Metric.DAILY_XP, CompetitionQuery.Scope.GLOBAL);

  private RankingBoard.Entry entry(int rank, long score, String id) {
    return new RankingBoard.Entry(
        rank, score, new PublicLearner(id, "Học viên " + id, "", "2026-10-19", score, 1, false));
  }

  private void invalid(Runnable check) {
    try {
      check.run();
      fail("Invalid board accepted");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void tiesUseCompetitionRanksRatherThanUniquePositions() {
    RankingBoard board =
        new RankingBoard(
            xp,
            "2026-10-19",
            Arrays.asList(entry(1, 400, "1"), entry(1, 400, "2"), entry(3, 200, "3")));
    assertEquals(3, board.entries.get(2).rank);
  }

  @Test
  public void validatesScoresInsteadOfRecalculatingRanksFromXpLevels() {
    invalid(
        () ->
            new RankingBoard(
                xp, "2026-10-19", Arrays.asList(entry(1, 200, "1"), entry(2, 400, "2"))));
    invalid(
        () ->
            new RankingBoard(
                xp, "2026-10-19", Arrays.asList(entry(1, 200, "1"), entry(2, 200, "2"))));
  }

  @Test
  public void rejectsDuplicateLearners() {
    invalid(
        () ->
            new RankingBoard(
                xp, "2026-10-19", Arrays.asList(entry(1, 200, "1"), entry(1, 200, "1"))));
  }

  @Test
  public void rejectsMixedLearningDays() {
    invalid(
        () -> new RankingBoard(xp, "2026-10-18", Collections.singletonList(entry(1, 200, "1"))));
  }

  @Test
  public void rejectsMetricMismatch() {
    RankingBoard.Entry inconsistent =
        new RankingBoard.Entry(
            1, 200, new PublicLearner("1", "Học viên", "", "2026-10-19", 400, 2, false));
    invalid(() -> new RankingBoard(xp, "2026-10-19", Collections.singletonList(inconsistent)));
  }

  @Test
  public void streakRankingUsesDaysEvenWhenXpDiffers() {
    RankingBoard board =
        new RankingBoard(
            new CompetitionQuery(CompetitionQuery.Metric.STREAK, CompetitionQuery.Scope.FRIENDS),
            "2026-10-19",
            Collections.singletonList(
                new RankingBoard.Entry(
                    1, 2, new PublicLearner("1", "Học viên", "", "2026-10-19", 600, 2, false))));
    assertEquals(2, board.entries.get(0).value);
  }

  @Test
  public void holdsAnImmutableCopy() {
    List<RankingBoard.Entry> list = new ArrayList<>();
    list.add(entry(1, 0, "1"));
    RankingBoard board = new RankingBoard(xp, "2026-10-19", list);
    list.clear();
    assertEquals(1, board.entries.size());
    try {
      board.entries.clear();
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  @Test
  public void emptyListIsValidWithoutInventingLearners() {
    assertTrue(new RankingBoard(xp, "2026-10-19", Collections.emptyList()).entries.isEmpty());
  }

  @Test
  public void relevantEventsDifferFromPersonalSummaryPolicy() {
    assertTrue(RankingEventPolicy.affects("PROFILE", "other", "me"));
    assertTrue(RankingEventPolicy.affects("RANKING", "current", "me"));
    assertTrue(RankingEventPolicy.affects("FRIENDSHIP", "request", "me"));
    assertFalse(RankingEventPolicy.affects("ATTEMPT", "question", "me"));
    assertFalse(RankingEventPolicy.affects("MESSAGE", "chat", "me"));
  }

  @Test
  public void publicLearnerAcceptsSupabaseUuidAndLegacyNumericId() {
    assertEquals("42", entry(1, 0, "42").learner.id);
    assertEquals(
        "a1234567-89ab-cdef-0123-456789abcdef",
        entry(1, 0, "a1234567-89ab-cdef-0123-456789abcdef").learner.id);
  }

  @Test
  public void publicLearnerRejectsMalformedRemoteId() {
    invalid(() -> entry(1, 0, "not-an-id"));
  }
}
