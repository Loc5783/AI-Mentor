package com.example.mentor.physics;

import com.example.mentor.learning.*;
import java.util.*;

/** Entry for physics curriculum; implementation stays inside its subject module. */
public final class PhysicsSubject implements SubjectModule {
  @Override
  public SubjectId id() {
    return SubjectId.PHYSICS;
  }

  @Override
  public Set<Tool> tools() {
    return Collections.singleton(Tool.CIRCUIT);
  }
}
