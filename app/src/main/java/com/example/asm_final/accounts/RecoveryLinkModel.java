package com.example.asm_final.accounts;

import androidx.lifecycle.ViewModel;

/** Recovery credentials survive rotation only; never saved to disk or instance state. */
public final class RecoveryLinkModel extends ViewModel {
  public String accessToken = "";
  public void clear() { accessToken = ""; }
  @Override protected void onCleared() { clear(); }
}
