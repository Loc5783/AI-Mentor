package com.example.mentor.learning;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

public class SubjectCatalogTest {
  private static final Set<SubjectId> LESSONS =
      EnumSet.of(SubjectId.BIOLOGY, SubjectId.CHEMISTRY, SubjectId.PHYSICS);

  private SubjectModule module(SubjectId subject) {
    return new SubjectModule() {
      public SubjectId id() {
        return subject;
      }

      public Set<Tool> tools() {
        return Collections.emptySet();
      }
    };
  }

  @Test
  public void nineSubjectsHaveStableDistinctIdentifiers() {
    Set<String> ids = new HashSet<>();
    List<SubjectModule> modules = new ArrayList<>();
    for (SubjectId id : SubjectId.values()) {
      assertTrue(ids.add(id.wireId));
      assertEquals(id, SubjectId.fromWireId(id.wireId));
      assertFalse(id.label("vi").isEmpty());
      assertFalse(id.label("en").isEmpty());
      modules.add(module(id));
    }
    SubjectCatalog catalog = new SubjectCatalog(modules, LESSONS);
    assertEquals(9, catalog.all().size());
    assertEquals(3, catalog.lessonModules().size());
    for (SubjectId subject : SubjectId.values())
      assertEquals(LESSONS.contains(subject), catalog.hasLessonScope(subject));
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingModuleFailsComposition() {
    new SubjectCatalog(Collections.singletonList(module(SubjectId.MATH)), LESSONS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void duplicateModuleFailsComposition() {
    List<SubjectModule> modules = new ArrayList<>();
    for (SubjectId id : SubjectId.values()) modules.add(module(id));
    modules.add(module(SubjectId.MATH));
    new SubjectCatalog(modules, LESSONS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void legacyScienceIsNotOneOfNineSubjects() {
    SubjectId.fromWireId("SCIENCE");
  }

  @Test
  public void gradeContextAcceptsOnlyExplicitSupportedGradeAndLanguage() {
    for (int grade = 10; grade <= 12; grade++)
      assertEquals(grade, new LearningContext(2, grade, "vi").grade);
    for (int grade : new int[] {0, 9, 13}) {
      try {
        new LearningContext(2, grade, "vi");
        fail("Unsupported grade");
      } catch (IllegalArgumentException expected) {
      }
    }
    try {
      new LearningContext(0, 10, "vi");
      fail("Missing account");
    } catch (IllegalArgumentException expected) {
    }
    try {
      new LearningContext(2, 10, "xx");
      fail("Unsupported language");
    } catch (IllegalArgumentException expected) {
    }
  }
}
