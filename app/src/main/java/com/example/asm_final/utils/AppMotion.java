package com.example.asm_final.utils;

import android.animation.AnimatorInflater;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asm_final.R;

/** Small shared transitions and card feedback without replacing gestures or click handlers. */
public final class AppMotion implements Application.ActivityLifecycleCallbacks {
  private AppMotion() {}

  public static void install(Application app) {
    app.registerActivityLifecycleCallbacks(new AppMotion());
  }

  private static boolean enabled(View view) {
    if (Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) return false;
    return Settings.Global.getFloat(
            view.getContext().getContentResolver(), Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        > 0f;
  }

  private static void enter(View view) {
    if (view.getTag(R.id.mentor_motion_entered) != null) return;
    view.setTag(R.id.mentor_motion_entered, true);
    if (!enabled(view)) return;
    view.setAlpha(.94f);
    view.animate()
        .alpha(1f)
        .setDuration(180)
        .setInterpolator(new android.view.animation.DecelerateInterpolator())
        .start();
  }

  private static void decorate(View view) {
    if (view instanceof CardView
        && view.isClickable()
        && view.getTag(R.id.mentor_motion_card) == null) {
      view.setTag(R.id.mentor_motion_card, true);
      if (enabled(view) && view.getStateListAnimator() == null)
        view.setStateListAnimator(
            AnimatorInflater.loadStateListAnimator(
                view.getContext(), com.example.mentor.ui.R.animator.mentor_card_press));
      if (view.getForeground() == null) {
        android.util.TypedValue value = new android.util.TypedValue();
        if (view.getContext()
                .getTheme()
                .resolveAttribute(android.R.attr.selectableItemBackground, value, true)
            && value.resourceId != 0)
          view.setForeground(
              androidx.core.content.ContextCompat.getDrawable(view.getContext(), value.resourceId));
      }
    }
    if (view instanceof RecyclerView && view.getTag(R.id.mentor_motion_list) == null) {
      view.setTag(R.id.mentor_motion_list, true);
      ((RecyclerView) view)
          .addOnChildAttachStateChangeListener(
              new RecyclerView.OnChildAttachStateChangeListener() {
                @Override
                public void onChildViewAttachedToWindow(View child) {
                  decorate(child);
                }

                @Override
                public void onChildViewDetachedFromWindow(View child) {}
              });
    }
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
        decorate(((ViewGroup) view).getChildAt(i));
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle state) {
    if (activity instanceof FragmentActivity)
      ((FragmentActivity) activity)
          .getSupportFragmentManager()
          .registerFragmentLifecycleCallbacks(
              new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentViewCreated(
                    FragmentManager manager, Fragment fragment, View view, Bundle saved) {
                  decorate(view);
                  if (saved == null) enter(view);
                }

                @Override
                public void onFragmentViewDestroyed(FragmentManager manager, Fragment fragment) {
                  View view = fragment.getView();
                  if (view != null) {
                    view.animate().cancel();
                    view.setAlpha(1f);
                  }
                }
              },
              false);
  }

  @Override
  public void onActivityStarted(Activity activity) {
    ViewGroup root = activity.findViewById(android.R.id.content);
    if (root != null) {
      decorate(root);
      if (root.getChildCount() > 0) enter(root.getChildAt(0));
    }
  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    View root = activity.findViewById(android.R.id.content);
    if (root != null) root.animate().cancel();
  }

  @Override
  public void onActivityResumed(Activity activity) {}

  @Override
  public void onActivityPaused(Activity activity) {}

  @Override
  public void onActivityStopped(Activity activity) {}

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle out) {}
}
