package com.example.asm_final.competition;

import android.content.Context;
import com.example.asm_final.*;
import com.example.asm_final.backend.BackendClient;
import com.example.asm_final.dashboard.*;
import java.util.*;

/** Adapts authoritative server ranks while keeping legacy/local progress explicitly separate. */
public final class BackendCompetitionRepository implements CompetitionRepository {
  private final Context context;
  private final LearningOverviewRepository overview;
  private final com.example.asm_final.onboarding.LearnerProfileRepository learners;

  public BackendCompetitionRepository(
      Context context,
      LearningOverviewRepository overview,
      com.example.asm_final.onboarding.LearnerProfileRepository learners) {
    this.context = context.getApplicationContext();
    this.overview = overview;
    this.learners = learners;
  }

  @Override
  public Snapshot load(int owner, CompetitionQuery query) {
    Long xp = null;
    Integer streak = null;
    boolean cached = false, needsLogin = false;
    try {
      com.example.asm_final.onboarding.LearnerProfile learner = learners.load(owner);
      LearningOverviewRepository.Snapshot summary =
          overview.load(owner, learner == null ? 0 : learner.grade);
      if (summary.mode == LearningOverviewRepository.Mode.LOCAL) {
        return new Snapshot(Source.LOCAL, null, null, null, false, false);
      }
      cached = summary.mode == LearningOverviewRepository.Mode.CACHED;
      needsLogin = summary.needsLogin;
      if (summary.data != null) {
        xp = summary.data.dailyXp;
        streak = summary.data.streakDays;
      }
      RankingBoard board =
          CompetitionJson.board(
              new BackendClient(context, owner)
                  .request(
                      "GET",
                      "/rankings?metric=" + query.metric.name() + "&scope=" + query.scope.name(),
                      null,
                      null,
                      null),
              query);
      try (com.example.asm_final.accounts.RemoteAccountLinks links =
          new com.example.asm_final.accounts.RemoteAccountLinks(context)) {
        String ownId = links.remoteId(owner, BackendClient.endpoint(context));
        for (RankingBoard.Entry entry : board.entries)
          if (entry.learner.id.equals(ownId)) {
            xp = entry.learner.dailyXp;
            streak = entry.learner.streakDays;
            cached = false;
            break;
          }
      }
      return new Snapshot(Source.LIVE, board, xp, streak, cached, false);
    } catch (Exception failure) {
      if (failure instanceof BackendClient.Failure)
        needsLogin |= ((BackendClient.Failure) failure).status == 401;
      return new Snapshot(Source.UNAVAILABLE, null, xp, streak, cached, needsLogin);
    }
  }
}
