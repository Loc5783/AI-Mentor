package com.example.asm_final.competition;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.asm_final.*;
import com.example.asm_final.dashboard.OverviewUpdates;
import com.example.asm_final.utils.ScreenViews;
import com.example.mentor.data.ActiveUser;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Public fields in a scrollable dialog; private account metadata is never bound here. */
public final class PublicLearnerDialog extends DialogFragment {
  private PublicProfileViewModel model;
  private OverviewUpdates.Subscription live;
  private TextView name, bio, stats, status;
  private ImageView avatar;

  public static PublicLearnerDialog create(String id) {
    PublicLearnerDialog dialog = new PublicLearnerDialog();
    Bundle args = new Bundle();
    args.putString("PROFILE_ID", id);
    dialog.setArguments(args);
    return dialog;
  }

  @Override
  public Dialog onCreateDialog(Bundle saved) {
    model = new ViewModelProvider(this).get(PublicProfileViewModel.class);
    LinearLayout root = ScreenViews.column(requireContext());
    avatar = new ImageView(requireContext());
    avatar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
    LinearLayout.LayoutParams size =
        new LinearLayout.LayoutParams(
            ScreenViews.dp(requireContext(), 72), ScreenViews.dp(requireContext(), 72));
    size.gravity = android.view.Gravity.CENTER_HORIZONTAL;
    root.addView(avatar, size);
    avatar.setVisibility(View.GONE);
    name = ScreenViews.text(requireContext(), "", 24, true);
    name.setId(R.id.public_profile_name);
    root.addView(name);
    bio = ScreenViews.text(requireContext(), "", 16, false);
    bio.setId(R.id.public_profile_bio);
    root.addView(bio);
    stats = ScreenViews.text(requireContext(), "", 18, true);
    stats.setId(R.id.public_profile_stats);
    root.addView(stats);
    status = ScreenViews.text(requireContext(), getString(R.string.ranking_loading), 14, false);
    root.addView(status);
    root.addView(ScreenViews.button(requireContext(), R.string.ranking_retry, model::refresh));
    model
        .state()
        .observe(
            this,
            state -> {
              PublicLearner learner = state.learner;
              name.setText(learner == null ? "" : learner.nickname);
              bio.setText(
                  learner == null
                      ? ""
                      : learner.bio.trim().isEmpty()
                          ? getString(R.string.ranking_bio_empty)
                          : learner.bio);
              stats.setText(
                  learner == null
                      ? ""
                      : getString(R.string.competition_streak_value, (long) learner.streakDays)
                          + "\n"
                          + getString(R.string.competition_xp_value, learner.dailyXp));
              status.setText(
                  state.loading
                      ? getString(R.string.ranking_loading)
                      : state.failed
                          ? getString(R.string.ranking_profile_error)
                          : getString(R.string.ranking_day, learner.learningDay)
                              + (state.avatarFailed
                                  ? "\n" + getString(R.string.ranking_avatar_error)
                                  : ""));
              avatar.setVisibility(state.avatar == null ? View.GONE : View.VISIBLE);
              avatar.setImageBitmap(state.avatar);
            });
    return new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.ranking_profile)
        .setView(ScreenViews.scroll(requireContext(), root))
        .setPositiveButton(R.string.ranking_close, null)
        .create();
  }

  @Override
  public void onResume() {
    super.onResume();
    model.refresh();
    live =
        ((AiMentorApplication) requireActivity().getApplication())
            .services()
            .publicProfileUpdates(requireContext(), model.profileId)
            .observe(ActiveUser.get(requireContext()), model::refresh);
  }

  @Override
  public void onPause() {
    if (live != null) {
      live.close();
      live = null;
    }
    super.onPause();
  }
}
