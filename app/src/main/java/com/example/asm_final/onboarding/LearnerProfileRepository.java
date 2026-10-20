package com.example.asm_final.onboarding;

public interface LearnerProfileRepository {
  LearnerProfile load(int userId);

  void advance(int userId, int step);

  void saveDraft(LearnerProfile profile);

  void complete(LearnerProfile profile);

  void setGrade(int userId, int grade);
}
