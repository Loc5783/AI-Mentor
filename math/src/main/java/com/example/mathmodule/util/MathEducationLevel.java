package com.example.mathmodule.util;

/** Supported learner levels when generating mathematics quizzes. */
public enum MathEducationLevel {
    MIDDLE_SCHOOL("Middle School", "học sinh THCS, lớp 6 đến lớp 9"),
    HIGH_SCHOOL("High School", "học sinh THPT, lớp 10 đến lớp 12"),
    UNIVERSITY("University", "sinh viên đại học");

    private final String displayName;
    private final String promptDescription;

    MathEducationLevel(String displayName, String promptDescription) {
        this.displayName = displayName;
        this.promptDescription = promptDescription;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPromptDescription() {
        return promptDescription;
    }

    public static MathEducationLevel fromDisplayName(String value) {
        if (value != null) {
            for (MathEducationLevel level : values()) {
                if (level.displayName.equalsIgnoreCase(value.trim())) return level;
            }
        }
        return MIDDLE_SCHOOL;
    }
}
