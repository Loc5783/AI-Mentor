package com.example.asm_final.accounts;

import org.junit.Test;
import static org.junit.Assert.*;

public class AuthCallbackTest {
  private static final String URL = "aimentor://auth/callback";
  @Test public void recoveryIsNotSignup() {
    AuthCallback result = AuthCallback.parse(URL + "#type=recovery&access_token=test%2Btoken");
    assertEquals(AuthCallback.Kind.RECOVERY, result.kind);
    assertEquals("test+token", result.accessToken);
  }
  @Test public void signupRoutesToLogin() {
    assertEquals(AuthCallback.Kind.SIGNUP,
        AuthCallback.parse(URL + "#type=signup&access_token=test").kind);
  }
  @Test public void errorOverridesToken() {
    assertEquals(AuthCallback.Kind.INVALID,
        AuthCallback.parse(URL + "#type=signup&access_token=test&error_code=otp_expired").kind);
  }
  @Test public void bareCallbackDoesNotClaimSuccess() {
    assertEquals(AuthCallback.Kind.INVALID, AuthCallback.parse(URL).kind);
  }
  @Test public void missingTokenIsInvalid() {
    assertEquals(AuthCallback.Kind.INVALID, AuthCallback.parse(URL + "#type=recovery").kind);
  }
  @Test public void unrelatedLinksAreIgnored() {
    assertEquals(AuthCallback.Kind.NONE, AuthCallback.parse("https://example.com").kind);
    assertEquals(AuthCallback.Kind.NONE, AuthCallback.parse(null).kind);
  }
  @Test public void malformedAndDuplicateParametersAreRejected() {
    assertEquals(AuthCallback.Kind.INVALID, AuthCallback.parse(URL + "#access_token=%ZZ").kind);
    assertEquals(AuthCallback.Kind.INVALID,
        AuthCallback.parse(URL + "?type=signup#type=recovery&access_token=test").kind);
  }
  @Test public void unknownTypesAreRejected() {
    assertEquals(AuthCallback.Kind.INVALID,
        AuthCallback.parse(URL + "#type=other&access_token=test").kind);
  }
}
