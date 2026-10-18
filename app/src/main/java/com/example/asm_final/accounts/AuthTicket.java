package com.example.asm_final.accounts;

import com.example.asm_final.onboarding.LearnerProfile;

public final class AuthTicket {
  public final String username;
  public final int userId;
  public final LearnerProfile profile;

  public AuthTicket(String name, int id, LearnerProfile profile) {
    this.username = name;
    this.userId = id;
    this.profile = profile;
  }
}
