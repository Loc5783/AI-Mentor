package com.example.asm_final.learning;

import com.example.mentor.learning.SubjectCatalog;
import com.example.mentor.learning.SubjectId;
import java.util.Arrays;
import java.util.EnumSet;

/** Sole composition point: individual subjects never depend on app or on each other. */
public final class SubjectModules {
  private SubjectModules() {}

  public static SubjectCatalog create() {
    return new SubjectCatalog(
        Arrays.asList(
            new com.example.mathmodule.learning.MathSubject(),
            new com.example.mentor.literature.LiteratureSubject(),
            new com.example.mentor.english.EnglishSubject(),
            new com.example.historymodule.learning.HistorySubject(),
            new com.example.mentor.geography.GeographySubject(),
            new com.example.mentor.chemistry.ChemistrySubject(),
            new com.example.mentor.physics.PhysicsSubject(),
            new com.example.mentor.biology.BiologySubject(),
            new com.example.mentor.economicslaw.EconomicsLawSubject()),
        EnumSet.of(SubjectId.BIOLOGY, SubjectId.CHEMISTRY, SubjectId.PHYSICS));
  }
}
