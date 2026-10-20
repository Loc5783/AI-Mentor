package com.example.asm_final.activities.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.example.asm_final.R;

public final class ProfileHubFragment extends Fragment {
  private com.example.asm_final.dashboard.OverviewUpdates.Subscription liveUpdates;
  private com.example.asm_final.utils.AsyncActionRunner actions;
  private com.example.asm_final.dashboard.LearningOverviewRepository.Snapshot overview;
  private int overviewRequest;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
    return inflater.inflate(R.layout.fragment_profile_hub, parent, false);
  }

  @Override
  public void onViewCreated(View view, Bundle state) {
    String username = getArguments() == null ? "" : getArguments().getString("USERNAME", "");
    ((TextView) view.findViewById(R.id.hub_name)).setText(username);
    view.findViewById(R.id.button_learning_progress)
        .setOnClickListener(
            v -> {
              if (overview != null
                  && overview.mode
                      != com.example.asm_final.dashboard.LearningOverviewRepository.Mode.LOCAL) {
                showServerProgress();
                return;
              }
              if (!com.example.asm_final.backend.BackendClient.endpoint(requireContext()).isEmpty()
                  && overview == null) {
                Toast.makeText(requireContext(), R.string.auth_processing, Toast.LENGTH_SHORT)
                    .show();
                return;
              }
              if (getChildFragmentManager().findFragmentByTag("learning_progress") != null) return;
              LearningProgressDialog dialog = new LearningProgressDialog();
              Bundle args = new Bundle();
              args.putString("USERNAME", username);
              dialog.setArguments(args);
              dialog.show(getChildFragmentManager(), "learning_progress");
            });
    view.findViewById(R.id.profile_open_settings)
        .setOnClickListener(
            v ->
                ((com.example.asm_final.activities.HomeActivity) requireActivity())
                    .navigateTo(R.id.nav_settings, true));
    view.findViewById(R.id.profile_edit_public)
        .setOnClickListener(
            v ->
                ((com.example.asm_final.activities.HomeActivity) requireActivity())
                    .navigateTo(R.id.nav_edit_profile, true));
    com.example.asm_final.AppServices services =
        ((com.example.asm_final.AiMentorApplication) requireActivity().getApplication()).services();
    actions =
        new com.example.asm_final.utils.AsyncActionRunner(
            services.io(),
            task -> new android.os.Handler(android.os.Looper.getMainLooper()).post(task));
    actions.submit(
        () -> services.profiles().load(username),
        profile -> {
          if (profile == null) return;
          if (profile.fullName != null && !profile.fullName.isEmpty())
            ((TextView) view.findViewById(R.id.hub_name)).setText(profile.fullName);
          if (profile.avatar != null && !profile.avatar.isEmpty())
            try {
              ((ImageView) view.findViewById(R.id.hub_avatar))
                  .setImageURI(Uri.parse(profile.avatar));
            } catch (RuntimeException ignored) {
            }
        },
        error -> {});
  }

  @Override
  public void onResume() {
    super.onResume();
    loadOverview();
    liveUpdates =
        com.example.asm_final.dashboard.OverviewLiveBinding.start(this, this::loadOverview);
  }

  @Override
  public void onPause() {
    if (liveUpdates != null) {
      liveUpdates.close();
      liveUpdates = null;
    }
    super.onPause();
  }

  private void loadOverview() {
    final View root = getView();
    if (root == null) return;
    int request = ++overviewRequest,
        owner = com.example.mentor.data.ActiveUser.get(requireContext());
    com.example.asm_final.AppServices services =
        ((com.example.asm_final.AiMentorApplication) requireActivity().getApplication()).services();
    android.content.Context context = requireContext().getApplicationContext();
    services
        .io()
        .execute(
            () -> {
              try {
                com.example.asm_final.onboarding.LearnerProfile profile =
                    services.learners().load(owner);
                com.example.asm_final.dashboard.LearningOverviewRepository.Snapshot result =
                    services.overview().load(owner, profile == null ? 0 : profile.grade);
                new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(
                        () -> {
                          if (getView() != root
                              || request != overviewRequest
                              || com.example.mentor.data.ActiveUser.get(context) != owner) return;
                          overview = result;
                          TextView stats = root.findViewById(R.id.hub_learning_stats);
                          stats.setVisibility(
                              result.mode
                                      == com.example.asm_final.dashboard.LearningOverviewRepository
                                          .Mode.LOCAL
                                  ? View.GONE
                                  : View.VISIBLE);
                          if (result.data != null)
                            stats.setText(
                                getString(R.string.overview_daily_xp, result.data.dailyXp)
                                    + "\n"
                                    + getString(R.string.overview_streak, result.data.streakDays));
                          else
                            stats.setText(
                                result.needsLogin
                                    ? R.string.overview_expired
                                    : R.string.overview_error);
                        });
              } catch (RuntimeException unavailable) {
                /* The local profile itself remains usable. */
              }
            });
  }

  private void showServerProgress() {
    com.example.asm_final.dashboard.LearningOverviewData data = overview.data;
    String message =
        data == null
            ? getString(overview.needsLogin ? R.string.overview_expired : R.string.overview_error)
            : getString(R.string.overview_daily_xp, data.dailyXp)
                + "\n"
                + getString(R.string.overview_streak, data.streakDays)
                + "\n"
                + getString(R.string.overview_lessons, data.completedLessons, data.totalLessons)
                + "\n\n"
                + (data.questionCount == 0
                    ? getString(R.string.overview_no_practice)
                    : getString(
                        R.string.overview_practice, data.firstTryCorrect, data.questionCount))
                + (overview.mode
                        == com.example.asm_final.dashboard.LearningOverviewRepository.Mode.CACHED
                    ? "\n\n" + getString(R.string.overview_cached)
                    : "");
    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.overview_title)
        .setMessage(message)
        .setNeutralButton(
            R.string.overview_legacy_title,
            (d, w) -> {
              if (getChildFragmentManager().findFragmentByTag("legacy_progress") != null) return;
              LearningProgressDialog legacy = new LearningProgressDialog();
              Bundle args = new Bundle();
              args.putString(
                  "USERNAME",
                  getArguments() == null ? "" : getArguments().getString("USERNAME", ""));
              args.putBoolean("legacy_only", true);
              legacy.setArguments(args);
              legacy.show(getChildFragmentManager(), "legacy_progress");
            })
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }

  @Override
  public void onDestroyView() {
    overviewRequest++;
    overview = null;
    if (actions != null) actions.close();
    super.onDestroyView();
  }
}
