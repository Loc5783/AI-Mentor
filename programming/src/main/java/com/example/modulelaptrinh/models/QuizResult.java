package com.example.modulelaptrinh.models;

import java.util.List;

public class QuizResult {
    private final QuizRequest request;
    private final List<QuizQuestion> questions;
    private final int totalQuestions;
    private final int correctAnswers;
    private final int expEarned;

    public QuizResult(QuizRequest request, List<QuizQuestion> questions,
                      int correctAnswers, int expEarned) {
        this.request = request;
        this.questions = questions;
        this.totalQuestions = questions != null ? questions.size() : 0;
        this.correctAnswers = Math.max(0, Math.min(correctAnswers, totalQuestions));
        this.expEarned = Math.max(0, expEarned);
    }

    public QuizRequest getRequest() { return request; }
    public List<QuizQuestion> getQuestions() { return questions; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getExpEarned() { return expEarned; }
    public double getAccuracyPercent() {
        return totalQuestions == 0 ? 0 : (correctAnswers * 100.0) / totalQuestions;
    }
}