package com.example.mentor.geography;

import com.example.mentor.learning.*;
import java.util.*;

/** Entry for geography curriculum; implementation stays inside its subject module. */
public final class GeographySubject implements SubjectModule {
  @Override
  public SubjectId id() {
    return SubjectId.GEOGRAPHY;
  }

  @Override
  public Set<Tool> tools() {
    return Collections.emptySet();
  }
}
