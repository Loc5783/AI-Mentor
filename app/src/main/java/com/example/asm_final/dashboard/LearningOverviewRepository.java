package com.example.asm_final.dashboard;

import android.content.Context;
import com.example.asm_final.accounts.RemoteAccountLinks;
import com.example.asm_final.backend.BackendClient;
import com.example.mentor.data.ActiveUser;
import org.json.JSONObject;

/** Screens share this summary; legacy EXP is never added to server rewards a second time. */
public final class LearningOverviewRepository {
  public enum Mode {
    LOCAL,
    LIVE,
    CACHED,
    UNAVAILABLE,
    NO_GRADE
  }

  public static final class Snapshot {
    public final Mode mode;
    public final LearningOverviewData data;
    public final boolean needsLogin;

    Snapshot(Mode mode, LearningOverviewData data, boolean needsLogin) {
      this.mode = mode;
      this.data = data;
      this.needsLogin = needsLogin;
    }
  }

  private final Context context;

  public LearningOverviewRepository(Context context) {
    this.context = context.getApplicationContext();
  }

  public Snapshot load(int owner, int grade) {
    if (com.example.asm_final.supabase.SupabaseHttp.enabled(context)) {
      return loadSupabase(owner, grade);
    }
    String endpoint = BackendClient.endpoint(context);
    if (endpoint.isEmpty()) return new Snapshot(Mode.LOCAL, null, false);
    String remoteId;
    try (RemoteAccountLinks links = new RemoteAccountLinks(context)) {
      remoteId = links.remoteId(owner, endpoint);
    }
    if (remoteId.isEmpty()) return new Snapshot(Mode.UNAVAILABLE, null, true);
    if (grade < 10 || grade > 12) return new Snapshot(Mode.NO_GRADE, null, false);
    try {
      JSONObject value =
          new BackendClient(context, owner)
              .request("GET", "/me/dashboard?grade=" + grade, null, null, null);
      if (!remoteId.equals(value.getString("profileId")) || grade != value.getInt("grade"))
        throw new BackendClient.Failure("INVALID_SERVER_RESPONSE", 502);
      LearningOverviewData parsed = new LearningOverviewData(value);
      if (ActiveUser.get(context) != owner) return new Snapshot(Mode.UNAVAILABLE, null, false);
      try (LearningOverviewStore cache = new LearningOverviewStore(context)) {
        cache.save(owner, endpoint, remoteId, grade, value);
      } catch (RuntimeException storage) {
        /* A successful server response remains usable without a cache. */
      }
      return new Snapshot(Mode.LIVE, parsed, false);
    } catch (Exception failure) {
      boolean expired =
          failure instanceof BackendClient.Failure
              && ((BackendClient.Failure) failure).status == 401;
      if (ActiveUser.get(context) != owner) return new Snapshot(Mode.UNAVAILABLE, null, false);
      try (LearningOverviewStore cache = new LearningOverviewStore(context)) {
        JSONObject cached = cache.load(owner, endpoint, remoteId, grade);
        return new Snapshot(
            cached == null ? Mode.UNAVAILABLE : Mode.CACHED,
            cached == null ? null : new LearningOverviewData(cached),
            expired);
      } catch (Exception storage) {
        return new Snapshot(Mode.UNAVAILABLE, null, expired);
      }
    }
  }

  private Snapshot loadSupabase(int owner, int grade) {
    String endpoint = com.example.asm_final.BuildConfig.SUPABASE_URL;
    if (grade < 10 || grade > 12) return new Snapshot(Mode.NO_GRADE, null, false);
    String remoteId;
    try (RemoteAccountLinks links = new RemoteAccountLinks(context)) {
      remoteId = links.remoteId(owner, endpoint);
    }
    if (remoteId.isEmpty()) return new Snapshot(Mode.UNAVAILABLE, null, true);
    try {
      JSONObject value = new JSONObject(
          new com.example.asm_final.supabase.SupabaseAccounts(context)
              .requestFor(owner, "POST", "/rest/v1/rpc/mentor_overview", new JSONObject().put("g", grade)));
      if (!remoteId.equals(value.getString("profileId")) || grade != value.getInt("grade"))
        throw new BackendClient.Failure("INVALID_SERVER_RESPONSE", 502);
      LearningOverviewData parsed = new LearningOverviewData(value);
      if (ActiveUser.get(context) != owner) return new Snapshot(Mode.UNAVAILABLE, null, false);
      try (LearningOverviewStore cache = new LearningOverviewStore(context)) {
        cache.save(owner, endpoint, remoteId, grade, value);
      } catch (RuntimeException ignored) { /* Live response remains usable without a cache. */ }
      return new Snapshot(Mode.LIVE, parsed, false);
    } catch (Exception failure) {
      boolean expired = failure instanceof BackendClient.Failure
          && ((BackendClient.Failure) failure).status == 401;
      if (ActiveUser.get(context) != owner) return new Snapshot(Mode.UNAVAILABLE, null, false);
      try (LearningOverviewStore cache = new LearningOverviewStore(context)) {
        JSONObject cached = cache.load(owner, endpoint, remoteId, grade);
        return new Snapshot(cached == null ? Mode.UNAVAILABLE : Mode.CACHED,
            cached == null ? null : new LearningOverviewData(cached), expired);
      } catch (Exception ignored) {
        return new Snapshot(Mode.UNAVAILABLE, null, expired);
      }
    }
  }
}
