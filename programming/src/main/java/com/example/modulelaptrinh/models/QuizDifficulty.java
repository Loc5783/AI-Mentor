package com.example.modulelaptrinh.models;

import com.example.modulelaptrinh.R;

public enum QuizDifficulty {
    EASY(R.string.quiz_diff_easy, 5),
    MEDIUM(R.string.quiz_diff_medium, 10),
    HARD(R.string.quiz_diff_hard, 20);

    private final int displayNameResId;
    private final int expReward;

    QuizDifficulty(int displayNameResId, int expReward) {
        this.displayNameResId = displayNameResId;
        this.expReward = expReward;
    }

    public int getDisplayNameResId() {
        return displayNameResId;
    }

    public int getExpReward() {
        return expReward;
    }

    public static QuizDifficulty fromApiValue(String value) {
        if (value == null) return MEDIUM;
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        if ("EASY".equals(normalized) || "DỄ".equals(normalized)) return EASY;
        if ("HARD".equals(normalized) || "KHÓ".equals(normalized)) return HARD;
        return MEDIUM;
    }

    public static QuizDifficulty inferFromType(QuizQuestion.Type type) {
        if (type == null) return MEDIUM;
        if (type == QuizQuestion.Type.TRUE_FALSE) return EASY;
        if (type == QuizQuestion.Type.FILL_BLANK) return HARD;
        return MEDIUM;
    }
}
