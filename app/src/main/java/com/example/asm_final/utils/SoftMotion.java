package com.example.asm_final.utils;

import android.animation.AnimatorInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import com.example.asm_final.R;

/** Optional presentation only: never installs touch listeners or delays a business action. */
public final class SoftMotion {
  private SoftMotion() {}

  private static void rest(View view) {
    if (view.getStateListAnimator() != null) view.getStateListAnimator().jumpToCurrentState();
    view.animate().cancel();
    view.setAlpha(1f);
    view.setTranslationY(0f);
    view.setScaleX(1f);
    view.setScaleY(1f);
  }

  private static void lifecycle(View view) {
    if (view.getTag(R.id.soft_motion_bound) != null) return;
    view.setTag(R.id.soft_motion_bound, true);
    view.addOnAttachStateChangeListener(
        new View.OnAttachStateChangeListener() {
          @Override
          public void onViewAttachedToWindow(View v) {}

          @Override
          public void onViewDetachedFromWindow(View v) {
            rest(v);
          }
        });
  }

  public static void enter(View view) {
    reveal(view, false);
  }

  public static void complete(View view) {
    reveal(view, true);
  }

  private static void reveal(View view, boolean completion) {
    lifecycle(view);
    rest(view);
    if (!MotionSettings.enabled(view.getContext())) return;
    view.setAlpha(.96f);
    view.setTranslationY(ScreenViews.dp(view.getContext(), 4));
    if (completion) {
      view.setScaleX(.985f);
      view.setScaleY(.985f);
    }
    view.animate()
        .alpha(1f)
        .translationY(0f)
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(completion ? 240 : 160)
        .setInterpolator(new DecelerateInterpolator())
        .start();
  }

  public static void press(View view) {
    lifecycle(view);
    view.setTag(R.id.soft_motion_press, true);
    rest(view);
    view.setStateListAnimator(
        MotionSettings.enabled(view.getContext())
            ? AnimatorInflater.loadStateListAnimator(
                view.getContext(), com.example.mentor.ui.R.animator.mentor_card_press)
            : null);
  }
}
