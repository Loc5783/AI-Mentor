package com.example.mathmodule.util;

import java.util.Locale;

/** Difficulty and EXP reward for one mathematics quiz question. */
public enum MathQuizDifficulty {
    EASY("Dễ", 5),
    MEDIUM("Trung bình", 10),
    HARD("Khó", 20);

    private final String displayName;
    private final int expReward;

    MathQuizDifficulty(String displayName, int expReward) {
        this.displayName = displayName;
        this.expReward = expReward;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getExpReward() {
        return expReward;
    }

    public static MathQuizDifficulty fromApiValue(String value) {
        if (value == null) return MEDIUM;
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ạ', 'A')
                .replace('Ả', 'A')
                .replace('Ã', 'A')
                .replace('Ă', 'A')
                .replace('Â', 'A')
                .replace('Ó', 'O')
                .replace('Ò', 'O')
                .replace('Ọ', 'O')
                .replace('Ỏ', 'O')
                .replace('Õ', 'O')
                .replace('Ô', 'O')
                .replace('Ơ', 'O')
                .replace('Í', 'I')
                .replace('Ì', 'I')
                .replace('Ị', 'I')
                .replace('Ỉ', 'I')
                .replace('Ĩ', 'I')
                .replace('Ú', 'U')
                .replace('Ù', 'U')
                .replace('Ụ', 'U')
                .replace('Ủ', 'U')
                .replace('Ũ', 'U')
                .replace('Ư', 'U')
                .replace('É', 'E')
                .replace('È', 'E')
                .replace('Ẹ', 'E')
                .replace('Ẻ', 'E')
                .replace('Ẽ', 'E')
                .replace('Ê', 'E')
                .replace('Đ', 'D');

        if (normalized.contains("EASY") || normalized.contains("DE")) return EASY;
        if (normalized.contains("HARD") || normalized.contains("KHO")) return HARD;
        return MEDIUM;
    }
}
