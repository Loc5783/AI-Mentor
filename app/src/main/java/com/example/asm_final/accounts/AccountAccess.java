package com.example.asm_final.accounts;

import android.content.Context;
import com.example.asm_final.activities.database.DatabaseHelper;
import com.example.asm_final.backend.BackendClient;
import com.example.asm_final.onboarding.LearnerProfileRepository;
import java.util.UUID;

/** The login/register forms and lesson screens share one server session per verified identity. */
public final class AccountAccess {
  private final Context context;
  private final LocalAccountRepository local;
  private final LearnerProfileRepository learners;
  private static final Object IMPORT_LOCK = new Object();

  public AccountAccess(
      Context context, LocalAccountRepository local, LearnerProfileRepository learners) {
    this.context = context.getApplicationContext();
    this.local = local;
    this.learners = learners;
  }

  public AuthTicket signIn(String identifier, String password) throws Exception {
    if (com.example.asm_final.supabase.SupabaseHttp.enabled(context)) return new com.example.asm_final.supabase.SupabaseAccounts(context).signIn(identifier, password);
    if (BackendClient.endpoint(context).isEmpty()) {
      if (!local.authenticate(identifier, password)) return null;
      return ticket(local.canonicalUsername(identifier));
    }
    BackendClient.Identity identity = BackendClient.authenticate(context, identifier, password);
    return importIdentity(identity, identifier, password);
  }

  public AuthTicket register(String name, String email, String password) throws Exception {
    if (com.example.asm_final.supabase.SupabaseHttp.enabled(context)) return new com.example.asm_final.supabase.SupabaseAccounts(context).register(name, email, password);
    if (BackendClient.endpoint(context).isEmpty()) {
      long id = local.registerLearner(name, email, password);
      return id > 0 ? ticket(local.canonicalUsername(email)) : null;
    }
    return importIdentity(BackendClient.register(context, name, email, password), email, password);
  }

  private AuthTicket ticket(String username) {
    int owner = local.userId(username);
    return owner > 0 ? new AuthTicket(username, owner, learners.load(owner)) : null;
  }

  public AuthTicket restore(String username) throws Exception {
    if (com.example.asm_final.supabase.SupabaseHttp.enabled(context)) return new com.example.asm_final.supabase.SupabaseAccounts(context).restore(username);
    AuthTicket ticket = ticket(username);
    if (ticket == null || BackendClient.endpoint(context).isEmpty()) return ticket;
    try (RemoteAccountLinks links = new RemoteAccountLinks(context)) {
      String id = links.remoteId(ticket.userId, BackendClient.endpoint(context));
      if (id.isEmpty()) return null;
      BackendClient api = new BackendClient(context, ticket.userId);
      if (!api.signedIn()) return null;
      try {
        if (!id.equals(api.request("GET", "/me/account", null, null, null).getString("id")))
          return null;
      } catch (BackendClient.Failure failure) {
        if (failure.status == 401) return null;
        if (failure.status < 500) throw failure;
      } catch (java.io.IOException offline) {
        // A remembered, encrypted session may read private local data while offline.
      }
      return ticket;
    }
  }

  private AuthTicket importIdentity(
      BackendClient.Identity identity, String identifier, String password) throws Exception {
    synchronized (IMPORT_LOCK) {
      try (RemoteAccountLinks links = new RemoteAccountLinks(context);
          DatabaseHelper db = new DatabaseHelper(context)) {
        int owner = links.owner(identity.endpoint, identity.remoteId());
        String username = "";
        if (owner == 0) {
          // Link legacy data only after proving both the server and the local credentials.
          if (local.authenticate(identifier, password)) {
            username = local.canonicalUsername(identifier);
            int candidate = local.userId(username);
            if (!links.bound(candidate, identity.endpoint)) owner = candidate;
          }
          if (owner == 0) {
            String preferred = identity.account.getString("username");
            username = local.userId(preferred) > 0 ? "remote_" + UUID.randomUUID() : preferred;
            String displayName = identity.account.getJSONObject("profile").getString("nickname");
            owner =
                (int)
                    db.addRemoteAccount(
                        username,
                        identity.account.getString("email"),
                        displayName,
                        identity.learning.optInt("grade", 0));
            if (owner <= 0)
              throw new android.database.sqlite.SQLiteException("Remote cache creation failed");
          }
          links.bind(owner, identity.endpoint, identity.remoteId());
        }
        try (android.database.Cursor c =
            db.getReadableDatabase()
                .rawQuery(
                    "SELECT username FROM users WHERE id=?",
                    new String[] {Integer.toString(owner)})) {
          if (!c.moveToFirst())
            throw new android.database.sqlite.SQLiteException("Missing local account");
          username = c.getString(0);
        }
        BackendClient.install(context, owner, identity);
        return new AuthTicket(username, owner, learners.load(owner));
      }
    }
  }
}
