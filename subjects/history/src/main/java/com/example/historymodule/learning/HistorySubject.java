package com.example.historymodule.learning;

import com.example.mentor.learning.*;
import java.util.*;

/** Entry for history curriculum; implementation stays inside its subject module. */
public final class HistorySubject implements SubjectModule {
  @Override
  public SubjectId id() {
    return SubjectId.HISTORY;
  }

  @Override
  public Set<Tool> tools() {
    return Collections.emptySet();
  }
}
