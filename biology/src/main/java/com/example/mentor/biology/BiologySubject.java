package com.example.mentor.biology;

import com.example.mentor.learning.*;
import java.util.*;

/** Entry for biology curriculum; implementation stays inside its subject module. */
public final class BiologySubject implements SubjectModule {
  @Override
  public SubjectId id() {
    return SubjectId.BIOLOGY;
  }

  @Override
  public Set<Tool> tools() {
    return Collections.emptySet();
  }
}
