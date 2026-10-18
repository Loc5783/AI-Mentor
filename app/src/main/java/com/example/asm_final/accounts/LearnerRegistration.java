package com.example.asm_final.accounts;

/** New student registration; compatibility account imports use UserRegistration. */
public interface LearnerRegistration {
  long registerLearner(String displayName, String email, String password);
}
