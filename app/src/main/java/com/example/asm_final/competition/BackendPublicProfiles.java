package com.example.asm_final.competition;

import android.content.Context;
import com.example.asm_final.backend.*;
import com.example.mentor.data.ActiveUser;

public final class BackendPublicProfiles implements PublicProfileRepository {
  private final Context context;
  private final java.util.concurrent.ConcurrentMap<String, String> supabaseAvatarPaths =
      new java.util.concurrent.ConcurrentHashMap<>();

  public BackendPublicProfiles(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public PublicLearner load(int owner, String id) throws Exception {
    if (id == null || !(id.matches("[1-9][0-9]{0,18}") || (com.example.asm_final.supabase.SupabaseHttp.enabled(context) && id.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))))
      throw new IllegalArgumentException("Invalid profile ID");
    if (com.example.asm_final.supabase.SupabaseHttp.enabled(context)) {
      org.json.JSONObject response = new org.json.JSONObject(
          new com.example.asm_final.supabase.SupabaseAccounts(context).requestFor(
              owner,
              "POST",
              "/rest/v1/rpc/mentor_social_api",
              new org.json.JSONObject().put("method", "GET")
                  .put("path", "/profiles/" + id)
                  .put("body", new org.json.JSONObject())
                  .put("query", new org.json.JSONObject())));
      String avatarPath = response.optString("avatarUrl", "");
      if (avatarPath.isBlank()) supabaseAvatarPaths.remove(id);
      else supabaseAvatarPaths.put(id, avatarPath);
      PublicLearner result = CompetitionJson.learner(response);
      if (!id.equals(result.id)) throw new BackendClient.Failure("INVALID_SERVER_RESPONSE", 502);
      return result;
    }
    PublicLearner result =
        CompetitionJson.learner(
            new BackendClient(context, owner).request("GET", "/profiles/" + id, null, null, null));
    if (!id.equals(result.id)) throw new BackendClient.Failure("INVALID_SERVER_RESPONSE", 502);
    return result;
  }

  @Override
  public byte[] avatar(int owner, String id) throws Exception {
    if (com.example.asm_final.supabase.SupabaseHttp.enabled(context)) {
      String path = supabaseAvatarPaths.get(id);
      if (path == null || path.isBlank()) throw new java.io.FileNotFoundException("Avatar unavailable");
      return new com.example.asm_final.supabase.SupabaseStorage(context).loadPublicAvatar(owner, id, path);
    }
    byte[] image = BackendAvatar.load(new BackendClient(context, owner), id);
    if (ActiveUser.get(context) != owner) throw new BackendClient.Failure("ACCOUNT_CHANGED", 401);
    return image;
  }
}
