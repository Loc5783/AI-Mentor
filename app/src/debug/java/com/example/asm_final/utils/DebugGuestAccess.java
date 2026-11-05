package com.example.asm_final.utils;

import android.content.Context;
import com.example.asm_final.AppServices;
import com.example.asm_final.BuildConfig;
import com.example.asm_final.accounts.AuthTicket;

/** Signs into the isolated USB QA backend with its seeded demo identity. */
public final class DebugGuestAccess {
  private DebugGuestAccess() {}

  public static AuthTicket authenticate(Context context, AppServices services) throws Exception {
    if (!BuildConfig.DEBUG_GUEST_MODE ||
        com.example.asm_final.backend.BackendClient.endpoint(context).isEmpty()) return null;
    return services.accountAccess().signIn("student 01", "123123123");
  }
}
