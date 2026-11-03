package com.example.asm_final.learning;

import com.example.mentor.learning.SubjectId;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** Converts onboarding subject identifiers into the learning enrollment contract. */
public final class EnrollmentSelection {
  private EnrollmentSelection() {}

  public static Set<SubjectId> fromProfile(String subjects) {
    EnumSet<SubjectId> result = EnumSet.noneOf(SubjectId.class);
    if (subjects == null || subjects.isBlank()) return result;
    for (String value : subjects.split(",", -1)) {
      String wireId = value.trim().toLowerCase(Locale.ROOT);
      if (!wireId.isEmpty()) result.add(SubjectId.fromWireId(wireId));
    }
    return result;
  }
}