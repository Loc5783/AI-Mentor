package com.example.asm_final.competition;

import android.content.Context;
import com.example.asm_final.BuildConfig;
import com.example.asm_final.accounts.RemoteAccountLinks;
import com.example.asm_final.supabase.SupabaseAccounts;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Searches only public learner names through the authenticated, row-protected profile view. */
public final class SupabaseLearnerSearch implements LearnerDirectory {
  private final Context context;

  public SupabaseLearnerSearch(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public String currentProfileId(int owner) throws Exception {
    try (RemoteAccountLinks links = new RemoteAccountLinks(context)) {
      return links.remoteId(owner, BuildConfig.SUPABASE_URL);
    }
  }

  @Override
  public List<FriendsViewModel.SearchItem> search(int owner, String input) throws Exception {
    String query = input.replaceAll("[^\\p{L}\\p{N} ]", " ").trim().replaceAll(" +", " ");
    if (query.length() < 2) return Collections.emptyList();
    if (query.length() > 40) query = query.substring(0, 40).trim();
    String response = new SupabaseAccounts(context).requestFor(owner, "POST",
        "/rest/v1/rpc/mentor_search_learners", new JSONObject().put("p_query", query));
    JSONArray rows = new JSONArray(response);
    List<FriendsViewModel.SearchItem> result = new ArrayList<>();
    for (int i = 0; i < rows.length(); i++) {
      JSONObject row = rows.getJSONObject(i);
      result.add(new FriendsViewModel.SearchItem(row.getString("id"), row.getString("nickname")));
    }
    return result;
  }
}
