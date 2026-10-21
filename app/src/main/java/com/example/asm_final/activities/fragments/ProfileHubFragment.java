package com.example.asm_final.activities.fragments;

import android.os.*;
import android.view.*;
import androidx.fragment.app.Fragment;
import com.example.asm_final.*;
import com.example.asm_final.activities.HomeActivity;
import com.example.asm_final.dashboard.*;
import com.example.asm_final.ui.OverviewUi;
import com.example.mentor.data.ActiveUser;
import java.util.Collections;

/** Owns lifecycle and account-scoped loading; Compose renders repository results only. */
public final class ProfileHubFragment extends Fragment {
  private OverviewUpdates.Subscription updates;
  private OverviewUi presenter;
  private int generation;
  private boolean needsLogin;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
    presenter =
        new OverviewUi(
            requireContext(),
            true,
            subject -> {},
            () -> {
              if (needsLogin) shell().logout();
              else load();
            },
            () -> shell().navigateTo(R.id.nav_edit_profile, true),
            () -> shell().navigateTo(R.id.nav_settings, true),
            () -> shell().navigateTo(R.id.nav_category, false));
    presenter.identity(
        getArguments() == null ? "" : getArguments().getString("USERNAME", ""), null);
    return presenter.view();
  }

  private HomeActivity shell() {
    return (HomeActivity) requireActivity();
  }

  @Override
  public void onResume() {
    super.onResume();
    load();
    updates = OverviewLiveBinding.start(this, this::load);
  }

  @Override
  public void onPause() {
    if (updates != null) {
      updates.close();
      updates = null;
    }
    super.onPause();
  }

  private void load() {
    if (getView() == null || presenter == null) return;
    View viewOwner = getView();
    int owner = ActiveUser.get(requireContext()), request = ++generation;
    android.content.Context context = requireContext().getApplicationContext();
    AppServices services = ((AiMentorApplication) context).services();
    String username = getArguments() == null ? "" : getArguments().getString("USERNAME", "");
    services
        .io()
        .execute(
            () -> {
              try {
                var learning = services.learners().load(owner);
                var profile = services.profiles().load(username);
                int grade = learning == null ? 0 : learning.grade;
                var result = services.overview().load(owner, grade);
                new Handler(Looper.getMainLooper())
                    .post(
                        () -> {
                          if (getView() != viewOwner
                              || request != generation
                              || ActiveUser.get(context) != owner) return;
                          needsLogin = result.needsLogin;
                          presenter.identity(
                              profile != null
                                      && profile.fullName != null
                                      && !profile.fullName.isEmpty()
                                  ? profile.fullName
                                  : username,
                              profile == null ? null : profile.avatar);
                          presenter.render(result, Collections.emptySet(), grade);
                        });
              } catch (RuntimeException unavailable) {
                new Handler(Looper.getMainLooper())
                    .post(
                        () -> {
                          if (getView() == viewOwner
                              && request == generation
                              && ActiveUser.get(context) == owner) presenter.failure();
                        });
              }
            });
  }

  @Override
  public void onDestroyView() {
    generation++;
    presenter = null;
    super.onDestroyView();
  }
}
