package com.example.asm_final.activities.fragments;

import android.os.*;
import android.view.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.asm_final.*;
import com.example.asm_final.competition.CompetitionViewModel;
import com.example.asm_final.competition.PublicLearnerDialog;
import com.example.asm_final.activities.HomeActivity;
import com.example.asm_final.dashboard.*;
import com.example.asm_final.learning.LearningSetupStore;
import com.example.asm_final.ui.ProfileUi;
import com.example.mentor.data.ActiveUser;
import com.example.mentor.learning.SubjectId;
import java.util.Collections;
import java.util.Set;

/** Owns account-scoped loading; the Profile UI only renders repository results. */
public final class ProfileHubFragment extends Fragment {
  private OverviewUpdates.Subscription updates;
  private ProfileUi presenter;
  private CompetitionViewModel friendsModel;
  private int generation;
  private boolean needsLogin;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
    friendsModel = new ViewModelProvider(this).get(CompetitionViewModel.class);
    presenter =
        new ProfileUi(
            requireContext(),
            () -> {
              if (needsLogin) shell().logout();
              else load();
            },
            () -> shell().navigateTo(R.id.nav_edit_profile, true),
            () -> shell().navigateTo(R.id.nav_settings, true),
            () -> shell().navigateTo(R.id.nav_category, false),
            friendsModel,
            id -> {
              if (getChildFragmentManager().findFragmentByTag("public_learner") == null)
                PublicLearnerDialog.create(id).show(getChildFragmentManager(), "public_learner");
            });
    String username = getArguments() == null ? "" : getArguments().getString("USERNAME", "");
    return presenter.view();
  }

  private HomeActivity shell() {
    return (HomeActivity) requireActivity();
  }

  @Override
  public void onResume() {
    super.onResume();
    load();
    friendsModel.refreshFriends();
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
                Set<SubjectId> subjects = Collections.emptySet();
                if (grade >= 10 && grade <= 12) {
                  try (LearningSetupStore enrollment = new LearningSetupStore(context)) {
                    subjects = enrollment.selected(owner, grade);
                  }
                }
                Set<SubjectId> chosen = subjects;
                var result = services.overview().load(owner, grade);
                org.json.JSONObject studySummary = null;
                org.json.JSONObject badgeSummary = null;
                if (com.example.asm_final.supabase.SupabaseHttp.enabled(context)) {
                  try {
                    studySummary = services.study().summary(owner);
                  } catch (RuntimeException ignored) {
                    // Keep the rest of the profile usable when the optional cloud summary is down.
                  }
                  try {
                    badgeSummary = services.study().badges(owner);
                  } catch (RuntimeException ignored) {
                    // The UI presents an explicit unavailable state instead of inventing badge data.
                  }
                }
                org.json.JSONObject finalStudySummary = studySummary;
                org.json.JSONObject finalBadgeSummary = badgeSummary;
                new Handler(Looper.getMainLooper())
                    .post(
                        () -> {
                          if (getView() != viewOwner
                              || request != generation
                              || ActiveUser.get(context) != owner) return;
                          needsLogin = result.needsLogin;
                          presenter.identity(
                              userVisibleName(profile, username),
                              profile == null ? null : profile.avatar,
                              profile == null ? "" : profile.bio);
                          presenter.learning(grade, learning == null ? -1 : learning.goal, chosen);
                          presenter.render(result);
                          presenter.activity(finalStudySummary, finalBadgeSummary);
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

  /** A login handle or email is not a display name and must never appear on the profile. */
  private static String userVisibleName(
      com.example.asm_final.accounts.UserProfile profile, String loginName) {
    if (profile == null || profile.fullName == null || profile.fullName.isBlank()) return "";
    String name = profile.fullName.trim();
    if (name.equalsIgnoreCase(loginName == null ? "" : loginName.trim())
        || name.equalsIgnoreCase(profile.email == null ? "" : profile.email.trim())) return "";
    return name;
  }

  @Override
  public void onDestroyView() {
    generation++;
    presenter = null;
    super.onDestroyView();
  }
}
