package com.example.asm_final.competition;

import android.content.Context;
import com.example.asm_final.supabase.SupabaseAccounts;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Searches only public learner names through the authenticated, row-protected profile view. */
public final class SupabaseLearnerSearch {
  private final Context context;

  public SupabaseLearnerSearch(Context context) {
    this.context = context.getApplicationContext();
  }

  public List<CompetitionViewModel.SearchItem> search(int owner, String input) throws Exception {
    String query = input.replaceAll("[^\\p{L}\\p{N} ]", " ").trim().replaceAll(" +", " ");
    if (query.length() < 2) return Collections.emptyList();
    if (query.length() > 40) query = query.substring(0, 40).trim();
    String filter = URLEncoder.encode("*" + query + "*", "UTF-8");
    String response = new SupabaseAccounts(context).requestFor(owner, "GET",
        "/rest/v1/mentor_profiles?select=id,nickname&nickname=ilike." + filter
            + "&order=nickname.asc&limit=30", null);
    JSONArray rows = new JSONArray(response);
    List<CompetitionViewModel.SearchItem> result = new ArrayList<>();
    for (int i = 0; i < rows.length(); i++) {
      JSONObject row = rows.getJSONObject(i);
      result.add(new CompetitionViewModel.SearchItem(row.getString("id"), row.getString("nickname")));
    }
    return result;
  }
}
