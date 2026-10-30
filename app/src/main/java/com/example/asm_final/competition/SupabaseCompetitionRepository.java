package com.example.asm_final.competition;

import android.content.Context;
import com.example.asm_final.backend.BackendClient;
import com.example.asm_final.dashboard.LearningOverviewRepository;
import com.example.asm_final.onboarding.LearnerProfile;
import com.example.asm_final.onboarding.LearnerProfileRepository;
import java.util.Locale;
import org.json.JSONObject;

/** Reads the same ranking contract from the authenticated Supabase social RPC. */
public final class SupabaseCompetitionRepository implements CompetitionRepository {
  private final Context context;
  private final LearningOverviewRepository overview;
  private final LearnerProfileRepository learners;

  public SupabaseCompetitionRepository(
      Context context, LearningOverviewRepository overview, LearnerProfileRepository learners) {
    this.context = context.getApplicationContext();
    this.overview = overview;
    this.learners = learners;
  }

  @Override
  public Snapshot load(int owner, CompetitionQuery query) {
    Long dailyXp = null;
    Integer streak = null;
    try {
      LearnerProfile learner = learners.load(owner);
      LearningOverviewRepository.Snapshot summary = overview.load(owner, learner == null ? 0 : learner.grade);
      if (summary.data != null) {
        dailyXp = summary.data.dailyXp;
        streak = summary.data.streakDays;
      }
      if (summary.mode == LearningOverviewRepository.Mode.LOCAL)
        return new Snapshot(Source.LOCAL, null, dailyXp, streak, false, false);

      JSONObject request = new JSONObject()
          .put("method", "GET")
          .put("path", "/rankings")
          .put("body", new JSONObject())
          .put("query", new JSONObject()
              .put("metric", query.metric.name().toUpperCase(Locale.ROOT))
              .put("scope", query.scope.name().toUpperCase(Locale.ROOT)));
      JSONObject response = new JSONObject(
          new com.example.asm_final.supabase.SupabaseAccounts(context)
              .requestFor(owner, "POST", "/rest/v1/rpc/mentor_social_api", request));
      RankingBoard board = CompetitionJson.board(response, query);
      return new Snapshot(Source.LIVE, board, dailyXp, streak, false, false);
    } catch (Exception failure) {
      boolean needsLogin = failure instanceof BackendClient.Failure
          && ((BackendClient.Failure) failure).status == 401;
      return new Snapshot(Source.UNAVAILABLE, null, dailyXp, streak, false, needsLogin);
    }
  }
}
