package com.example.mentor.learning;

/** Account-owned trusted profile context. Do not infer grade from the learner's message. */
public final class LearningContext {
  public final int userId, grade;
  public final String language;

  public LearningContext(int userId, int grade, String language) {
    if (userId <= 0 || grade < 10 || grade > 12)
      throw new IllegalArgumentException("A learner and grade 10–12 are required");
    if (!"vi".equals(language) && !"en".equals(language))
      throw new IllegalArgumentException("Unsupported language");
    this.userId = userId;
    this.grade = grade;
    this.language = language;
  }
}
