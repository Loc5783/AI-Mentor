package com.example.asm_final.competition;

import java.util.*;
import org.json.*;

/** The wire schema is translated at the adapter boundary, outside UI/domain policy. */
public final class CompetitionJson {
  private CompetitionJson() {}

  public static PublicLearner learner(JSONObject value) throws JSONException {
    return new PublicLearner(
        value.getString("id"),
        value.getString("nickname"),
        value.getString("bio"),
        value.getString("learningDay"),
        value.getLong("dailyXp"),
        value.getInt("streakDays"),
        !value.isNull("avatarUrl"));
  }

  public static RankingBoard board(JSONObject value, CompetitionQuery query) throws JSONException {
    if (!query.metric.name().equals(value.getString("metric"))
        || !query.scope.name().equals(value.getString("scope")))
      throw new JSONException("Ranking query mismatch");
    JSONArray items = value.getJSONArray("entries");
    List<RankingBoard.Entry> entries = new ArrayList<>();
    if (items.length() > 100) throw new JSONException("Ranking too large");
    for (int i = 0; i < items.length(); i++) {
      JSONObject item = items.getJSONObject(i);
      entries.add(
          new RankingBoard.Entry(
              item.getInt("rank"), item.getLong("value"), learner(item.getJSONObject("profile"))));
    }
    return new RankingBoard(query, value.getString("learningDay"), entries);
  }
}
