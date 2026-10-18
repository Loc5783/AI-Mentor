package com.example.mentor.chemistry;

import com.example.mentor.learning.*;
import java.util.*;

/** Entry for chemistry curriculum; implementation stays inside its subject module. */
public final class ChemistrySubject implements SubjectModule {
  @Override
  public SubjectId id() {
    return SubjectId.CHEMISTRY;
  }

  @Override
  public Set<Tool> tools() {
    return Collections.singleton(Tool.PERIODIC_TABLE);
  }
}
