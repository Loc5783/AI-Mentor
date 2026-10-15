package com.example.modulelaptrinh.utils;

public final class ProgrammingLevelCalculator {
    public static final int MAX_LEVEL = 20;
    private static final int BASE_EXP_TO_NEXT_LEVEL = 100;
    private static final int EXP_STEP_PER_LEVEL = 20;

    private ProgrammingLevelCalculator() { }

    public static int getLevel(int totalExp) {
        int safeExp = Math.max(0, totalExp);
        int level = 1;
        while (level < MAX_LEVEL && safeExp >= getTotalExpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    public static int getTotalExpForLevel(int level) {
        int safeLevel = Math.max(1, Math.min(level, MAX_LEVEL));
        int completedLevels = safeLevel - 1;
        return BASE_EXP_TO_NEXT_LEVEL * completedLevels
                + EXP_STEP_PER_LEVEL * completedLevels * (completedLevels - 1) / 2;
    }

    public static int getExpToNextLevel(int currentLevel) {
        int safeLevel = Math.max(1, Math.min(currentLevel, MAX_LEVEL));
        if (safeLevel >= MAX_LEVEL) return 0;
        return BASE_EXP_TO_NEXT_LEVEL + EXP_STEP_PER_LEVEL * (safeLevel - 1);
    }

    public static int getExpInsideCurrentLevel(int totalExp) {
        int safeExp = Math.max(0, totalExp);
        int level = getLevel(safeExp);
        return Math.max(0, safeExp - getTotalExpForLevel(level));
    }

    public static int getProgressPercent(int totalExp) {
        int level = getLevel(totalExp);
        if (level >= MAX_LEVEL) return 100;
        int required = getExpToNextLevel(level);
        if (required <= 0) return 100;
        int currentExp = totalExp - getTotalExpForLevel(level);
        return Math.min(100, Math.round(currentExp * 100f / required));
    }

    public static int getTitleResId(int level) {
        int safeLevel = Math.max(1, Math.min(level, MAX_LEVEL));
        if (safeLevel <= 4) return com.example.modulelaptrinh.R.string.prog_level_1;
        if (safeLevel <= 8) return com.example.modulelaptrinh.R.string.prog_level_2;
        if (safeLevel <= 12) return com.example.modulelaptrinh.R.string.prog_level_3;
        if (safeLevel <= 16) return com.example.modulelaptrinh.R.string.prog_level_4;
        return com.example.modulelaptrinh.R.string.prog_level_5;
    }
}
