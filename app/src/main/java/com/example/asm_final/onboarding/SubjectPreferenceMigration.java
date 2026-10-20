package com.example.asm_final.onboarding;

import java.util.*;

/** Cleans old interests without guessing which science subject a person meant to enroll in. */
public final class SubjectPreferenceMigration {
  private SubjectPreferenceMigration() {}

  public static String clean(String stored) {
    Set<String> result = new LinkedHashSet<>();
    if (stored != null)
      for (String value : stored.split(",")) {
        String id = value.trim().toUpperCase(Locale.ROOT);
        if (OnboardingPolicy.SUBJECTS.contains(id)) result.add(id);
      }
    return String.join(",", result);
  }
}
