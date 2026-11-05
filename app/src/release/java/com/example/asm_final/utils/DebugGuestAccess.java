package com.example.asm_final.utils;

import android.content.Context;
import com.example.asm_final.AppServices;
import com.example.asm_final.accounts.AuthTicket;

/** Release builds never enter without server authentication. */
public final class DebugGuestAccess {
  private DebugGuestAccess() {}

  public static AuthTicket authenticate(Context context, AppServices services) {
    return null;
  }
}
