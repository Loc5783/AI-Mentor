package com.example.asm_final.accounts;

/** This local build has no email/token server. It never claims to have sent mail. */
public final class UnavailableRecoveryService implements RecoveryService {
  public boolean available() {
    return false;
  }

  public Result requestReset(String email) {
    return Result.UNAVAILABLE;
  }

  public Result resendVerification(String email) {
    return Result.UNAVAILABLE;
  }

  public Result verify(String email, String token) {
    return Result.UNAVAILABLE;
  }

  public Result reset(String email, String token, String password) {
    return Result.UNAVAILABLE;
  }
}
