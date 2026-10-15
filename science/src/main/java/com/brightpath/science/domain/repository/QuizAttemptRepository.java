package com.brightpath.science.domain.repository;

import com.brightpath.science.domain.model.EducationLevel;
import com.brightpath.science.domain.model.QuizChallenge;
import com.brightpath.science.domain.model.QuizPerformance;
import com.brightpath.science.domain.model.ScienceSubject;

public interface QuizAttemptRepository {
    long saveAttempt(int userId, ScienceSubject subject, EducationLevel level,
                     String topic, QuizChallenge challenge,
                     int totalQuestions, int correctAnswers, int expEarned);

    QuizPerformance getRecentPerformance(int userId, ScienceSubject subject,
                                         EducationLevel level, int attemptLimit);

    /**
     * Returns the complete Quiz performance of one subject across every education level.
     * This is used by the Quiz dashboard and must never mix Physics, Chemistry and Biology.
     */
    QuizPerformance getSubjectPerformance(int userId, ScienceSubject subject);

    int countAttempts(int userId);
    double calculateAccuracy(int userId);
    int getTotalExp(int userId);
}
