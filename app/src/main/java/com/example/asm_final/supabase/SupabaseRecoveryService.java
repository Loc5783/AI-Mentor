package com.example.asm_final.supabase;

import android.content.Context;
import com.example.asm_final.accounts.RecoveryService;
import com.example.asm_final.backend.BackendClient;
import org.json.JSONObject;

/** Supabase email OTP flows; reset verification and password update are one atomic UI action. */
public final class SupabaseRecoveryService implements RecoveryService {
  private final Context context;
  private final SupabaseTransport http;

  public SupabaseRecoveryService(Context context) {
    this(context, new SupabaseHttp());
  }

  SupabaseRecoveryService(Context context, SupabaseTransport http) {
    this.context = context.getApplicationContext();
    this.http = http;
  }

  @Override
  public boolean available() {
    return SupabaseHttp.enabled(context);
  }

  @Override
  public Result requestReset(String email) {
    return request("POST", "/auth/v1/recover", json("email", email.trim()), null);
  }

  @Override
  public Result resendVerification(String email) {
    return request("POST", "/auth/v1/resend", json("type", "signup", "email", email.trim()), null);
  }

  @Override
  public Result verify(String email, String token) {
    return request("POST", "/auth/v1/verify", json("type", "signup", "email", email.trim(), "token", token.trim()), null);
  }

  @Override
  public Result reset(String email, String token, String password) {
    try {
      String response = http.request("POST", "/auth/v1/verify",
          json("type", "recovery", "email", email.trim(), "token", token.trim()), null);
      String accessToken = new JSONObject(response).getString("access_token");
      if (accessToken.isBlank()) return Result.INVALID_TOKEN;
      http.request("PUT", "/auth/v1/user", json("password", password), accessToken);
      return Result.PASSWORD_RESET;
    } catch (Exception failure) {
      return failure instanceof BackendClient.Failure && isInvalidToken((BackendClient.Failure) failure)
          ? Result.INVALID_TOKEN : Result.FAILED;
    }
  }

  private Result request(String method, String path, JSONObject body, String token) {
    try {
      http.request(method, path, body, token);
      return path.endsWith("/verify") ? Result.VERIFIED : Result.REQUEST_ACCEPTED;
    } catch (Exception failure) {
      return failure instanceof BackendClient.Failure && isInvalidToken((BackendClient.Failure) failure)
          ? Result.INVALID_TOKEN : Result.FAILED;
    }
  }

  private static JSONObject json(String... pairs) {
    JSONObject value = new JSONObject();
    try {
      for (int i = 0; i < pairs.length; i += 2) value.put(pairs[i], pairs[i + 1]);
      return value;
    } catch (org.json.JSONException invalid) {
      throw new IllegalArgumentException("Invalid recovery request", invalid);
    }
  }

  private static boolean isInvalidToken(BackendClient.Failure failure) {
    return failure.status == 400 || failure.status == 401 || failure.status == 422;
  }
}
