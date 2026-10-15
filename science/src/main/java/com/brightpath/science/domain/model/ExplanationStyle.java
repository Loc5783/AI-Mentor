package com.brightpath.science.domain.model;

import com.brightpath.science.R;

public enum ExplanationStyle {
    SHORT(R.string.science_style_short),
    DETAILED(R.string.science_style_detailed),
    STEP_BY_STEP(R.string.science_style_step_by_step);

    private final int displayNameResId;

    ExplanationStyle(int displayNameResId) { this.displayNameResId = displayNameResId; }
    public int getDisplayNameResId() { return displayNameResId; }

    public String getDisplayName() {
        if (this == SHORT) return "Short";
        if (this == DETAILED) return "Detailed";
        return "Step-by-step";
    }

    public static ExplanationStyle fromPosition(int position) {
        ExplanationStyle[] values = values();
        return position >= 0 && position < values.length ? values[position] : STEP_BY_STEP;
    }
}
