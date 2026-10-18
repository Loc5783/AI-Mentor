package com.example.mentor.learning;

/** Stable resume pointer. Chat cannot advance a lesson or award EXP by changing this pointer. */
public final class LessonHandoff {
  public final String enrollmentId, attemptId, lessonId, questionId, curriculumVersion;
  public final int step;

  public LessonHandoff(
      String enrollmentId,
      String attemptId,
      String lessonId,
      String questionId,
      String curriculumVersion,
      int step) {
    this.enrollmentId = required(enrollmentId);
    this.attemptId = required(attemptId);
    this.lessonId = required(lessonId);
    this.questionId = required(questionId);
    this.curriculumVersion = required(curriculumVersion);
    if (step < 0) throw new IllegalArgumentException("Negative lesson step");
    this.step = step;
  }

  private static String required(String value) {
    if (value == null || value.trim().isEmpty())
      throw new IllegalArgumentException("Missing resume identity");
    return value;
  }
}
