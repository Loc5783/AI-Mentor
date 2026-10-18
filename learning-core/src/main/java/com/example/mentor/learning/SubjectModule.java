package com.example.mentor.learning;

import java.util.Set;

/** Subject-owned entry contract. Lessons, quiz UI and rewards belong to shared engines. */
public interface SubjectModule {
  SubjectId id();

  Set<Tool> tools();

  enum Tool {
    CALCULATOR,
    CIRCUIT,
    PERIODIC_TABLE
  }
}
