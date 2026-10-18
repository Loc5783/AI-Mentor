package com.example.mathmodule.learning;

import com.example.mentor.learning.*;
import java.util.*;

/** Entry for math curriculum; implementation stays inside its subject module. */
public final class MathSubject implements SubjectModule {
  @Override
  public SubjectId id() {
    return SubjectId.MATH;
  }

  @Override
  public Set<Tool> tools() {
    return Collections.singleton(Tool.CALCULATOR);
  }
}
