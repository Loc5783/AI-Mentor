package com.example.mentor.learning;

import java.util.*;

/** Composition validates the complete curriculum before navigation can consume it. */
public final class SubjectCatalog {
  private final Map<SubjectId, SubjectModule> modules;
  private final Set<SubjectId> lessonSubjects;

  public SubjectCatalog(
      Collection<? extends SubjectModule> supplied, Set<SubjectId> lessonSubjects) {
    EnumMap<SubjectId, SubjectModule> result = new EnumMap<>(SubjectId.class);
    for (SubjectModule module : supplied) {
      if (module == null || module.id() == null || result.put(module.id(), module) != null)
        throw new IllegalArgumentException("Duplicate or invalid subject module");
    }
    if (result.size() != SubjectId.values().length)
      throw new IllegalArgumentException("All nine subject modules are required");
    modules = Collections.unmodifiableMap(result);
    if (lessonSubjects == null
        || lessonSubjects.isEmpty()
        || !result.keySet().containsAll(lessonSubjects))
      throw new IllegalArgumentException("A nonempty subset of lesson subjects is required");
    this.lessonSubjects = Collections.unmodifiableSet(EnumSet.copyOf(lessonSubjects));
  }

  public SubjectModule module(SubjectId id) {
    SubjectModule module = modules.get(id);
    if (module == null) throw new IllegalArgumentException("Unknown subject");
    return module;
  }

  public Collection<SubjectModule> all() {
    return modules.values();
  }

  public boolean hasLessonScope(SubjectId id) {
    return lessonSubjects.contains(id);
  }

  /** Scope selection does not imply that curriculum/content is already published. */
  public Collection<SubjectModule> lessonModules() {
    List<SubjectModule> selected = new ArrayList<>();
    for (SubjectId id : lessonSubjects) selected.add(modules.get(id));
    return Collections.unmodifiableList(selected);
  }
}
