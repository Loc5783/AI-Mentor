package com.example.mentor.learning;

import java.util.*;

/** A review is inserted AFTER five lessons; it never counts as another lesson in grouping. */
public final class ReviewCheckpointPolicy {
  public static final int LESSONS_PER_REVIEW = 5;

  private ReviewCheckpointPolicy() {}

  public static List<String> precedingLessons(
      List<String> orderedLessonIds, int completedPosition) {
    if (completedPosition <= 0
        || completedPosition % LESSONS_PER_REVIEW != 0
        || completedPosition > orderedLessonIds.size())
      throw new IllegalArgumentException("Reviews require a complete five-lesson block");
    Set<String> unique = new HashSet<>();
    for (String id : orderedLessonIds) {
      if (id == null || id.trim().isEmpty() || !unique.add(id))
        throw new IllegalArgumentException("Lesson identifiers must be unique and nonempty");
    }
    return Collections.unmodifiableList(
        new ArrayList<>(
            orderedLessonIds.subList(completedPosition - LESSONS_PER_REVIEW, completedPosition)));
  }

  public static int questions(boolean extendedCoverage) {
    return extendedCoverage ? 15 : 10;
  }

  public static int questionsPerLesson(int total) {
    if (total != 10 && total != 15)
      throw new IllegalArgumentException("A review has 10 or 15 questions");
    return total / LESSONS_PER_REVIEW;
  }
}
