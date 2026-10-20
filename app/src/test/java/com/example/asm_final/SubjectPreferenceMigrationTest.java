package com.example.asm_final;

import static org.junit.Assert.*;

import com.example.asm_final.onboarding.*;
import com.example.mentor.learning.SubjectId;
import org.junit.Test;

public final class SubjectPreferenceMigrationTest {
  @Test
  public void oldAggregatesAreNotGuessedIntoNewEnrollments() {
    assertEquals(
        "MATH,HISTORY", SubjectPreferenceMigration.clean("MATH,SCIENCE,HISTORY,PROGRAMMING"));
    assertEquals("", SubjectPreferenceMigration.clean("SCIENCE,PROGRAMMING"));
    assertEquals("", SubjectPreferenceMigration.clean(null));
  }

  @Test
  public void actualSubjectsArePreservedAndDeduplicated() {
    assertEquals(
        "BIOLOGY,CHEMISTRY",
        SubjectPreferenceMigration.clean(" biology,UNKNOWN,CHEMISTRY,BIOLOGY"));
  }

  @Test
  public void allNineSubjectsHavePreferencesButOnlyThreePublishLessons() {
    assertEquals(9, OnboardingPolicy.SUBJECTS.size());
    for (SubjectId id : SubjectId.values())
      assertTrue(OnboardingPolicy.SUBJECTS.contains(id.name()));
    assertTrue(OnboardingPolicy.validSubjects(""));
    assertFalse(OnboardingPolicy.validSubjects("SCIENCE"));
    assertFalse(OnboardingPolicy.validSubjects("PROGRAMMING"));
    assertEquals(3, com.example.asm_final.learning.SubjectModules.create().lessonModules().size());
  }
}
