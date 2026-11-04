package com.example.asm_final.accounts;

import java.net.URI;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/** Routing only: Supabase validates credentials before any password mutation. */
public final class AuthCallback {
  public enum Kind { NONE, INVALID, SIGNUP, RECOVERY }
  public final Kind kind;
  public final String accessToken;
  private AuthCallback(Kind kind, String token) { this.kind = kind; accessToken = token; }
  public static AuthCallback parse(String raw) {
    if (raw == null) return new AuthCallback(Kind.NONE, "");
    try {
      URI uri = URI.create(raw);
      if (!"aimentor".equals(uri.getScheme()) || !"auth".equals(uri.getHost())
          || !"/callback".equals(uri.getPath())) return new AuthCallback(Kind.NONE, "");
      Map<String, String> values = new HashMap<>();
      read(uri.getRawQuery(), values);
      read(uri.getRawFragment(), values);
      if (values.containsKey("error") || values.containsKey("error_code"))
        return new AuthCallback(Kind.INVALID, "");
      String token = values.getOrDefault("access_token", "");
      if (token.isBlank()) return new AuthCallback(Kind.INVALID, "");
      String type = values.getOrDefault("type", "");
      return new AuthCallback("recovery".equals(type) ? Kind.RECOVERY
          : "signup".equals(type) ? Kind.SIGNUP : Kind.INVALID, token);
    } catch (IllegalArgumentException | UnsupportedEncodingException invalid) {
      return new AuthCallback(Kind.INVALID, "");
    }
  }
  private static void read(String part, Map<String, String> values)
      throws UnsupportedEncodingException {
    if (part == null) return;
    for (String pair : part.split("&")) {
      String[] bits = pair.split("=", 2);
      String key = URLDecoder.decode(bits[0], "UTF-8");
      if (values.containsKey(key)) throw new IllegalArgumentException("Duplicate parameter");
      values.put(key, bits.length == 2 ? URLDecoder.decode(bits[1], "UTF-8") : "");
    }
  }
}
