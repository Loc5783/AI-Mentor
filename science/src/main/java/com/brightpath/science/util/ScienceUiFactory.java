package com.brightpath.science.util;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.brightpath.science.R;

/** Creates consistent, high-contrast controls used only inside the Science module. */
public final class ScienceUiFactory {
    private ScienceUiFactory() { }

    public static ArrayAdapter<String> spinnerAdapter(Context context, String[] values) {
        String[] safeValues = values == null ? new String[0] : values;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context, R.layout.item_science_spinner_selected, safeValues);
        adapter.setDropDownViewResource(R.layout.item_science_spinner_dropdown);
        return adapter;
    }
}
