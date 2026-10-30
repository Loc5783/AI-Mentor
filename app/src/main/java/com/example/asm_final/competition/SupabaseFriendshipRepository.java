package com.example.asm_final.competition;

import android.content.Context;
import com.example.asm_final.supabase.SupabaseAccounts;
import java.util.*;
import org.json.*;

/** Maps the existing friendship contract to the authenticated Supabase social RPC. */
public final class SupabaseFriendshipRepository implements FriendshipRepository {
  private static final String RPC = "/rest/v1/rpc/mentor_social_api";
  private final Context context;

  public SupabaseFriendshipRepository(Context context) {
    this.context = context.getApplicationContext();
  }

  private JSONObject call(int owner, String method, String path, JSONObject body, JSONObject query)
      throws Exception {
    JSONObject request = new JSONObject()
        .put("method", method)
        .put("path", path)
        .put("body", body == null ? new JSONObject() : body)
        .put("query", query == null ? new JSONObject() : query);
    return new JSONObject(new SupabaseAccounts(context).requestFor(
        owner, "POST", RPC, request));
  }

  @Override
  public List<Item> list(int owner) throws Exception {
    JSONArray items = call(owner, "GET", "/friendships", null, null).optJSONArray("items");
    List<Item> result = new ArrayList<>();
    if (items == null) return result;
    for (int i = 0; i < items.length(); i++) {
      JSONObject row = items.getJSONObject(i);
      JSONObject profile = row.optJSONObject("profile");
      result.add(new Item(
          row.getString("id"),
          profile == null ? "" : profile.optString("id", ""),
          row.optString("status", row.optString("state", "PENDING")),
          row.optString("direction", ""),
          profile == null ? "" : profile.optString("nickname", ""),
          profile == null ? 0 : profile.optLong("dailyXp", 0),
          profile == null ? 0 : profile.optInt("streakDays", 0),
          row.optInt("revision", 1)));
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  public Item request(int owner, String profileId, String requestKey) throws Exception {
    if (profileId == null || !profileId.matches("(?i)[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"))
      throw new IllegalArgumentException("Invalid Supabase profile ID");
    if (requestKey == null || requestKey.trim().isEmpty() || requestKey.length() > 100)
      throw new IllegalArgumentException("Invalid request key");
    JSONObject row = call(owner, "POST", "/friendships",
        new JSONObject().put("profileId", profileId), new JSONObject().put("requestKey", requestKey));
    return new Item(row.getString("id"), profileId, row.optString("status", "PENDING"),
        "OUTGOING", "", 0, 0, row.optInt("revision", 1));
  }

  @Override
  public Item decide(int owner, String friendshipId, String decision, int revision) throws Exception {
    if (!"ACCEPT".equals(decision) && !"DECLINE".equals(decision))
      throw new IllegalArgumentException("Invalid friendship decision");
    JSONObject row = call(owner, "PATCH", "/friendships/" + safeId(friendshipId),
        new JSONObject().put("decision", decision), new JSONObject().put("expectedRevision", revision));
    return new Item(row.getString("id"), "", row.optString("status", "ACCEPTED"),
        "INCOMING", "", 0, 0, row.optInt("revision", revision + 1));
  }

  @Override
  public void remove(int owner, String friendshipId, int revision) throws Exception {
    call(owner, "DELETE", "/friendships/" + safeId(friendshipId), null,
        new JSONObject().put("expectedRevision", revision));
  }

  private static String safeId(String id) {
    if (id == null || !id.matches("(?i)[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"))
      throw new IllegalArgumentException("Invalid friendship ID");
    return id;
  }
}
