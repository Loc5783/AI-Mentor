package com.example.asm_final.accounts;

/**
 * Server boundary: a production provider must verify tokens and enforce server-side resend limits.
 */
public interface RecoveryService {
  enum Result {
    UNAVAILABLE,
    REQUEST_ACCEPTED,
    VERIFIED,
    PASSWORD_RESET,
    INVALID_TOKEN,
    FAILED
  }

  boolean available();

  Result requestReset(String email);

  Result resendVerification(String email);

  Result verify(String token);

  Result reset(String token, String password);
}
