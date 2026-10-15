package com.example.historymodule.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HistoryQuizQuestion {
    private final String difficulty;
    private final String question;
    private final List<String> options;
    private final int correctIndex;
    private final String explanation;

    public HistoryQuizQuestion(String difficulty,
                               String question,
                               List<String> options,
                               int correctIndex,
                               String explanation) {
        this.difficulty = normalizeDifficulty(difficulty);
        this.question = question == null ? "" : question.trim();
        this.options = Collections.unmodifiableList(
                options == null ? new ArrayList<>() : new ArrayList<>(options));
        this.correctIndex = Math.max(0, Math.min(3, correctIndex));
        this.explanation = explanation == null ? "" : explanation.trim();
    }

    private static String normalizeDifficulty(String value) {
        if (value == null) return "MEDIUM";
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        if ("EASY".equals(normalized) || "HARD".equals(normalized)) {
            return normalized;
        }
        return "MEDIUM";
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getDifficultyDisplayName() {
        if ("EASY".equals(difficulty)) return "Dễ";
        if ("HARD".equals(difficulty)) return "Khó";
        return "Trung bình";
    }

    public int getExpReward() {
        if ("EASY".equals(difficulty)) return 5;
        if ("HARD".equals(difficulty)) return 20;
        return 10;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public String getExplanation() {
        return explanation;
    }
}
