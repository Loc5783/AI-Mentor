package com.example.mentor.literature;

import com.example.mentor.learning.*;
import java.util.*;

/** Entry for literature curriculum; implementation stays inside its subject module. */
public final class LiteratureSubject implements SubjectModule {
  @Override
  public SubjectId id() {
    return SubjectId.LITERATURE;
  }

  @Override
  public Set<Tool> tools() {
    return Collections.emptySet();
  }
}
