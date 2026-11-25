package com.example.asm_final.competition;

import android.content.Context;
import com.example.asm_final.accounts.RemoteAccountLinks;
import com.example.asm_final.backend.BackendClient;
import java.util.*;
import org.json.*;

/** Legacy FastAPI adapter kept while cloud mode is optional. */
public final class BackendFriendshipRepository implements FriendshipRepository {
  private final Context context;

  public BackendFriendshipRepository(Context context) {
    this.context = context.getApplicationContext();
  }

  private BackendClient client(int owner) {
    return new BackendClient(context, owner);
  }

  private String remoteId(int owner) throws Exception {
    String endpoint = BackendClient.endpoint(context);
    try (RemoteAccountLinks links = new RemoteAccountLinks(context)) {
      String id = links.remoteId(owner, endpoint);
      if (id == null || id.isEmpty()) throw new BackendClient.Failure("ACCOUNT_LINK_REQUIRED", 401);
      return id;
    }
  }

  @Override
  public List<Item> list(int owner) throws Exception {
    String me = remoteId(owner);
    JSONArray items = client(owner).request("GET", "/friendships", null, null, null).optJSONArray("items");
    List<Item> result = new ArrayList<>();
    if (items == null) return result;
    Map<String, JSONObject> sharedStreaks = new HashMap<>();
    try {
      JSONArray streaks = client(owner).request("GET", "/friends/streaks", null, null, null)
          .optJSONArray("streaks");
      if (streaks != null) for (int i = 0; i < streaks.length(); i++) {
        JSONObject streak = streaks.optJSONObject(i);
        if (streak != null) sharedStreaks.put(streak.optString("friendId", ""), streak);
      }
    } catch (Exception ignored) {
      // Older backend builds can omit shared streaks without blocking the friends list.
    }
    for (int i = 0; i < items.length(); i++) {
      JSONObject row = items.getJSONObject(i);
      String requester = row.optString("requesterProfileId", "");
      String recipient = row.optString("recipientProfileId", "");
      boolean outgoing = me.equals(requester);
      String friendId = outgoing ? recipient : requester;
      JSONObject shared = sharedStreaks.get(friendId);
      JSONObject profile = row.optJSONObject("profile");
      result.add(new Item(row.getString("id"), friendId,
          row.optString("state", "PENDING"), outgoing ? "OUTGOING" : "INCOMING",
          profile == null ? "" : profile.optString("nickname", ""),
          profile == null ? 0 : profile.optLong("dailyXp", 0),
          profile == null ? 0 : profile.optInt("streakDays", 0),
          shared == null ? 0 : shared.optInt("streakDays", 0),
          shared == null || shared.isNull("lastSharedDay") ? null : shared.optString("lastSharedDay"),
          row.optInt("revision", 1)));
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  public Item request(int owner, String profileId, String requestKey) throws Exception {
    if (profileId == null || !profileId.matches("[1-9][0-9]{0,18}"))
      throw new IllegalArgumentException("Invalid legacy profile ID");
    if (requestKey == null || requestKey.trim().isEmpty())
      throw new IllegalArgumentException("Missing request key");
    JSONObject row = client(owner).request("POST", "/friendships",
        new JSONObject().put("recipientProfileId", profileId), requestKey, null);
    return new Item(row.getString("id"), profileId, row.optString("state", "PENDING"),
        "OUTGOING", "", 0, 0, row.optInt("revision", 1));
  }

  @Override
  public Item decide(int owner, String friendshipId, String decision, int revision) throws Exception {
    if (!"ACCEPT".equals(decision) && !"DECLINE".equals(decision))
      throw new IllegalArgumentException("Invalid friendship decision");
    JSONObject row = client(owner).request("PATCH", "/friendships/" + safeId(friendshipId),
        new JSONObject().put("decision", decision), null, revision);
    return new Item(row.getString("id"), "", row.optString("state", "ACCEPTED"),
        "INCOMING", "", 0, 0, row.optInt("revision", revision + 1));
  }

  @Override
  public void remove(int owner, String friendshipId, int revision) throws Exception {
    client(owner).request("DELETE", "/friendships/" + safeId(friendshipId), null, null, revision);
  }

  private static String safeId(String id) {
    if (id == null || !id.matches("[A-Za-z0-9_-]{1,100}"))
      throw new IllegalArgumentException("Invalid friendship ID");
    return id;
  }
}
