package com.example.mentor.economicslaw;

import com.example.mentor.learning.*;
import java.util.*;

/** Entry for economics_law curriculum; implementation stays inside its subject module. */
public final class EconomicsLawSubject implements SubjectModule {
  @Override
  public SubjectId id() {
    return SubjectId.ECONOMICS_LAW;
  }

  @Override
  public Set<Tool> tools() {
    return Collections.emptySet();
  }
}
