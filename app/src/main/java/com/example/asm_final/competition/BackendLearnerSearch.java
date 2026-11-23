package com.example.asm_final.competition;

import android.content.Context;
import com.example.asm_final.accounts.RemoteAccountLinks;
import com.example.asm_final.backend.BackendClient;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Public learner search for the optional FastAPI transport. */
public final class BackendLearnerSearch implements LearnerDirectory {
  private final Context context;

  public BackendLearnerSearch(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public String currentProfileId(int owner) throws Exception {
    try (RemoteAccountLinks links = new RemoteAccountLinks(context)) {
      return links.remoteId(owner, BackendClient.endpoint(context));
    }
  }

  @Override
  public List<FriendsViewModel.SearchItem> search(int owner, String query) throws Exception {
    JSONObject response = new BackendClient(context, owner).request("GET",
        "/profiles/search?q=" + URLEncoder.encode(query, "UTF-8"), null, null, null);
    JSONArray rows = response.optJSONArray("items");
    List<FriendsViewModel.SearchItem> result = new ArrayList<>();
    if (rows != null) for (int i = 0; i < rows.length(); i++) {
      JSONObject row = rows.getJSONObject(i);
      result.add(new FriendsViewModel.SearchItem(row.getString("id"), row.getString("nickname")));
    }
    return result;
  }
}
