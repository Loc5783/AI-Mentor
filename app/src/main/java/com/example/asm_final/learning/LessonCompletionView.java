package com.example.asm_final.learning;

import android.content.Context;
import android.widget.LinearLayout;
import com.example.asm_final.R;
import com.example.asm_final.utils.ScreenViews;

/** Displays already-confirmed results; animation never awards points or changes progress. */
public final class LessonCompletionView extends LinearLayout {
  public LessonCompletionView(Context context, String result, String streak) {
    super(context);
    setOrientation(VERTICAL);
    setId(R.id.course_completion_card);
    LinearLayout card = ScreenViews.card(context);
    card.addView(
        ScreenViews.text(context, context.getString(R.string.course_congratulations), 27, true));
    card.addView(ScreenViews.text(context, result, 22, true));
    if (streak != null) card.addView(ScreenViews.text(context, streak, 17, false));
    addView(card);
  }
}
