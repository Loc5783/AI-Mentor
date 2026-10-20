package com.example.asm_final.backend;

import android.content.Context;
import android.os.Looper;
import com.example.asm_final.BuildConfig;
import com.example.mentor.data.ActiveUser;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/**
 * Bounded transport. Caller owns mutation keys; uncertain network failures are never auto-retried.
 */
public final class BackendClient {
  private static final java.util.concurrent.ConcurrentHashMap<String, Object> LOCKS =
      new java.util.concurrent.ConcurrentHashMap<>();
  private final Object sessionLock;

  public static String endpoint(Context context) {
    String value =
        BuildConfig.DEBUG
            ? context
                .getSharedPreferences("backend_test", 0)
                .getString("endpoint", BuildConfig.MENTOR_BACKEND_URL)
            : BuildConfig.MENTOR_BACKEND_URL;
    return value == null ? "" : value.trim().replaceAll("/+$", "");
  }

  /** Read and remove opaque encrypted credentials immediately; remote revocation can run later. */
  public static Runnable detach(Context context, int owner) {
    BackendClient client = new BackendClient(context, owner);
    final String detached;
    synchronized (client.sessionLock) {
      detached = client.sessions.take();
    }
    return () -> {
      if (detached.isEmpty() || !client.configured()) return;
      try {
        client.transportGuard();
        JSONObject old = client.sessions.decode(detached);
        if (old != null)
          client.raw(
              "POST",
              "/auth/sign-out",
              new JSONObject().put("refreshToken", old.getString("refreshToken")),
              old.getString("accessToken"),
              null,
              null);
      } catch (Exception offline) {
        /* Local credentials were removed even when offline. */
      }
    };
  }

  public static final class Identity {
    public final String endpoint;
    public final JSONObject account, learning;
    private final JSONObject session;

    private Identity(String endpoint, JSONObject session, JSONObject account, JSONObject learning) {
      this.endpoint = endpoint;
      this.session = session;
      this.account = account;
      this.learning = learning;
    }

    public String remoteId() throws org.json.JSONException {
      return account.getString("id");
    }
  }

  public static Identity authenticate(Context context, String identifier, String password)
      throws Exception {
    BackendClient transport = new BackendClient(context, 0);
    transport.transportGuard();
    JSONObject session =
        transport.raw(
            "POST",
            "/auth/sign-in",
            new JSONObject().put("identifier", identifier).put("password", password),
            null,
            null,
            null);
    return transport.identity(session);
  }

  public static Identity register(Context context, String name, String email, String password)
      throws Exception {
    BackendClient transport = new BackendClient(context, 0);
    transport.transportGuard();
    JSONObject session =
        transport.raw(
            "POST",
            "/auth/sign-up",
            new JSONObject()
                .put("displayName", name)
                .put("email", email)
                .put("password", password)
                .put("acceptedTermsVersion", "2026-10"),
            null,
            null,
            null);
    return transport.identity(session);
  }

  private Identity identity(JSONObject session) throws Exception {
    String token = session.getString("accessToken");
    JSONObject account = raw("GET", "/me/account", null, token, null, null);
    if (!session.getString("userId").equals(account.getString("id")))
      throw new Failure("INVALID_SERVER_RESPONSE", 502);
    return new Identity(
        base, session, account, raw("GET", "/me/learning-profile", null, token, null, null));
  }

  public static void install(Context context, int owner, Identity identity) throws Exception {
    BackendClient client = new BackendClient(context, owner, identity.endpoint);
    synchronized (client.sessionLock) {
      client.sessions.save(identity.session);
    }
  }

  public static final class Failure extends IOException {
    public final String code;
    public final int status;

    public Failure(String code, int status) {
      super(code);
      this.code = code;
      this.status = status;
    }
  }

  private final Context context;
  private final int owner;
  private final String base;
  private final BackendSession sessions;

  public BackendClient(Context context, int owner) {
    this(context, owner, endpoint(context));
  }

  /** Local HTTP is restricted to debug tests and Android Emulator's host alias. */
  public BackendClient(Context context, int owner, String endpoint) {
    this.context = context.getApplicationContext();
    this.owner = owner;
    String value = endpoint.replaceAll("/+$", "");
    if (!value.isEmpty() && !BackendEndpointPolicy.allowed(value, BuildConfig.DEBUG))
      throw new IllegalArgumentException("Invalid backend endpoint");
    base = value;
    sessionLock = LOCKS.computeIfAbsent(base + ":" + owner, ignored -> new Object());
    sessions = new BackendSession(context, owner, base);
  }

  public boolean configured() {
    return !base.isEmpty();
  }

  /** Short authentication phase only; the caller owns and closes the long-lived connection. */
  HttpURLConnection events(String cursor) throws Exception {
    HttpURLConnection connection = authenticatedConnection("/events", "text/event-stream");
    connection.setRequestProperty("Last-Event-ID", cursor);
    return connection;
  }

  /** Same-origin authenticated reads for dedicated bounded streaming adapters. */
  HttpURLConnection authenticatedConnection(String path, String accept) throws Exception {
    if (!path.startsWith("/") || path.startsWith("//") || path.contains(".."))
      throw new IllegalArgumentException("Invalid API path");
    request("GET", "/me/account", null, null, null);
    final String token;
    synchronized (sessionLock) {
      guard();
      JSONObject session = sessions.load();
      if (session == null) throw new Failure("AUTHENTICATION_REQUIRED", 401);
      token = session.getString("accessToken");
    }
    HttpURLConnection connection = (HttpURLConnection) new URL(base + path).openConnection();
    connection.setConnectTimeout(12000);
    connection.setReadTimeout(15000);
    connection.setInstanceFollowRedirects(false);
    connection.setRequestProperty("Accept", accept);
    connection.setRequestProperty("Authorization", "Bearer " + token);
    return connection;
  }

  private void guard() throws Failure {
    transportGuard();
    if (owner <= 0 || ActiveUser.get(context) != owner) throw new Failure("ACCOUNT_CHANGED", 401);
  }

  private void transportGuard() throws Failure {
    if (Looper.myLooper() == Looper.getMainLooper())
      throw new IllegalStateException("Network on main thread");
    if (!configured()) throw new Failure("BACKEND_NOT_CONFIGURED", 503);
  }

  public boolean signedIn() throws Exception {
    synchronized (sessionLock) {
      guard();
      return sessions.load() != null;
    }
  }

  public void signIn(String identifier, String password) throws Exception {
    synchronized (sessionLock) {
      guard();
      JSONObject previous = sessions.load();
      Identity identity = authenticate(context, identifier, password);
      try (com.example.asm_final.accounts.RemoteAccountLinks links =
          new com.example.asm_final.accounts.RemoteAccountLinks(context)) {
        String expected = links.remoteId(owner, base);
        if (expected.isEmpty() && previous != null) expected = previous.optString("userId", "");
        if (expected.isEmpty() || !expected.equals(identity.remoteId()))
          throw new Failure("ACCOUNT_LINK_REQUIRED", 409);
        if (!links.bound(owner, base)) links.bind(owner, base, expected);
      }
      guard();
      sessions.save(identity.session);
    }
  }

  public JSONObject request(
      String method, String path, JSONObject body, String key, Integer revision) throws Exception {
    synchronized (sessionLock) {
      guard();
      JSONObject session = sessions.load();
      if (session == null) throw new Failure("AUTHENTICATION_REQUIRED", 401);
      try {
        JSONObject result =
            raw(method, path, body, session.getString("accessToken"), key, revision);
        guard();
        return result;
      } catch (Failure failure) {
        if (failure.status != 401) throw failure;
        guard();
        JSONObject refreshed;
        try {
          refreshed =
              raw(
                  "POST",
                  "/auth/refresh",
                  new JSONObject().put("refreshToken", session.getString("refreshToken")),
                  null,
                  null,
                  null);
        } catch (Failure invalid) {
          if (invalid.status == 401) sessions.clear();
          throw invalid;
        }
        guard();
        sessions.save(refreshed);
        JSONObject result =
            raw(method, path, body, refreshed.getString("accessToken"), key, revision);
        guard();
        return result;
      }
    }
  }

  private JSONObject raw(
      String method, String path, JSONObject body, String token, String key, Integer revision)
      throws Exception {
    if (!path.startsWith("/") || path.startsWith("//") || path.contains(".."))
      throw new IllegalArgumentException("Invalid API path");
    HttpURLConnection connection = (HttpURLConnection) new URL(base + path).openConnection();
    try {
      connection.setRequestMethod(method);
      connection.setConnectTimeout(12000);
      connection.setReadTimeout(20000);
      connection.setInstanceFollowRedirects(false);
      connection.setRequestProperty("Accept", "application/json");
      if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token);
      if (key != null) connection.setRequestProperty("Idempotency-Key", key);
      if (revision != null) connection.setRequestProperty("If-Match", revision.toString());
      if (body != null) {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(bytes.length);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        try (OutputStream out = connection.getOutputStream()) {
          out.write(bytes);
        }
      }
      int status = connection.getResponseCode();
      InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      if (input != null)
        try (InputStream stream = input) {
          byte[] buffer = new byte[8192];
          int count;
          while ((count = stream.read(buffer)) != -1) {
            if (bytes.size() + count > 4 * 1024 * 1024)
              throw new Failure("RESPONSE_TOO_LARGE", 502);
            bytes.write(buffer, 0, count);
          }
        }
      JSONObject value;
      try {
        value = bytes.size() == 0 ? new JSONObject() : new JSONObject(bytes.toString("UTF-8"));
      } catch (org.json.JSONException invalid) {
        throw new Failure("INVALID_SERVER_RESPONSE", status);
      }
      if (status < 200 || status >= 300)
        throw new Failure(value.optString("code", "SERVER_ERROR"), status);
      return value;
    } finally {
      connection.disconnect();
    }
  }
}
