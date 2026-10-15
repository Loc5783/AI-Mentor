package com.brightpath.science.domain.model;

import com.brightpath.science.R;

public enum ScienceSubject {
    PHYSICS("PHYSICS", R.string.science_subj_physics),
    CHEMISTRY("CHEMISTRY", R.string.science_subj_chemistry),
    BIOLOGY("BIOLOGY", R.string.science_subj_biology);

    private final String code;
    private final int displayNameResId;

    ScienceSubject(String code, int displayNameResId) {
        this.code = code;
        this.displayNameResId = displayNameResId;
    }

    public String getCode() { return code; }
    public int getDisplayNameResId() { return displayNameResId; }

    public String getDisplayName() {
        if (this == CHEMISTRY) return "Chemistry";
        if (this == BIOLOGY) return "Biology";
        return "Physics";
    }

    public static ScienceSubject fromCode(String value) {
        if (value == null) return PHYSICS;
        for (ScienceSubject subject : values()) {
            if (subject.code.equalsIgnoreCase(value)) {
                return subject;
            }
        }
        return PHYSICS;
    }
}
