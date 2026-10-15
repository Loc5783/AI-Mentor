package com.brightpath.science.domain.model;

import com.brightpath.science.R;

public enum EducationLevel {
    MIDDLE_SCHOOL(R.string.science_edu_middle),
    HIGH_SCHOOL(R.string.science_edu_high),
    UNIVERSITY(R.string.science_edu_uni);

    private final int displayNameResId;

    EducationLevel(int displayNameResId) { this.displayNameResId = displayNameResId; }
    public int getDisplayNameResId() { return displayNameResId; }

    public String getDisplayName() {
        if (this == HIGH_SCHOOL) return "High school";
        if (this == UNIVERSITY) return "University";
        return "Middle school";
    }

    public static EducationLevel fromPosition(int position) {
        EducationLevel[] values = values();
        return position >= 0 && position < values.length ? values[position] : MIDDLE_SCHOOL;
    }
}
