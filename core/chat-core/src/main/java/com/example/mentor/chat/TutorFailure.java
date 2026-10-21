package com.example.mentor.chat;

/**
 * Safe error categories for localized UI; raw provider responses and keys never reach the screen.
 */
public final class TutorFailure extends Exception {
  public enum Code {
    NOT_CONFIGURED,
    INVALID_CONFIGURATION,
    NETWORK,
    RATE_LIMITED,
    UNAUTHORIZED,
    PROVIDER,
    INVALID_REQUEST,
    EMPTY_RESPONSE,
    CANCELLED
  }

  public final Code code;

  public TutorFailure(Code code) {
    super(code.name());
    this.code = code;
  }
}
