package com.example.asm_final.competition;

import android.content.Context;
import com.example.asm_final.backend.*;
import com.example.mentor.data.ActiveUser;

public final class BackendPublicProfiles implements PublicProfileRepository {
  private final Context context;

  public BackendPublicProfiles(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public PublicLearner load(int owner, String id) throws Exception {
    if (id == null || !id.matches("[1-9][0-9]{0,18}"))
      throw new IllegalArgumentException("Invalid profile ID");
    PublicLearner result =
        CompetitionJson.learner(
            new BackendClient(context, owner).request("GET", "/profiles/" + id, null, null, null));
    if (!id.equals(result.id)) throw new BackendClient.Failure("INVALID_SERVER_RESPONSE", 502);
    return result;
  }

  @Override
  public byte[] avatar(int owner, String id) throws Exception {
    byte[] image = BackendAvatar.load(new BackendClient(context, owner), id);
    if (ActiveUser.get(context) != owner) throw new BackendClient.Failure("ACCOUNT_CHANGED", 401);
    return image;
  }
}
