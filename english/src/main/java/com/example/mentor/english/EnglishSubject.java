package com.example.mentor.english;

import com.example.mentor.learning.*;
import java.util.*;

/** Entry for english curriculum; implementation stays inside its subject module. */
public final class EnglishSubject implements SubjectModule {
  @Override
  public SubjectId id() {
    return SubjectId.ENGLISH;
  }

  @Override
  public Set<Tool> tools() {
    return Collections.emptySet();
  }
}
