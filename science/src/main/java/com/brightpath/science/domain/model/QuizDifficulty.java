package com.brightpath.science.domain.model;

import com.brightpath.science.R;

/** Difficulty of one quiz question and the EXP awarded for a correct answer. */
public enum QuizDifficulty {
    EASY(R.string.science_quiz_diff_easy, 5),
    MEDIUM(R.string.science_quiz_diff_medium, 10),
    HARD(R.string.science_quiz_diff_hard, 20);

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
        if (type == QuizQuestion.Type.TRUE_FALSE) return EASY;
        if (type == QuizQuestion.Type.MATCHING) return HARD;
        return MEDIUM;
    }
}
