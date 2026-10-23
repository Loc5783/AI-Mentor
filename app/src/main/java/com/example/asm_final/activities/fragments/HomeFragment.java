package com.example.asm_final.activities.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.example.asm_final.*;
import com.example.asm_final.dashboard.*;
import com.example.asm_final.learning.LearningSetupStore;
import com.example.asm_final.onboarding.LearnerProfile;
import com.example.asm_final.utils.*;
import com.example.mentor.data.ActiveUser;
import com.example.mentor.learning.SubjectId;
import java.util.*;

/** Coordinates account-scoped loading. Subject cards and chart binding live in their own view. */
public final class HomeFragment extends Fragment {
  private final Handler main = new Handler(Looper.getMainLooper());
  private View root;
  private int owner, generation;
  private OverviewUpdates.Subscription updates;
  private com.example.asm_final.ui.OverviewUi subjectsView;
  private boolean needsLogin;
  private int selectedGrade;
  private boolean retryOverview;

  private void overviewAction() {
    if (needsLogin) ((com.example.asm_final.activities.HomeActivity) requireActivity()).logout();
    else if (retryOverview) load(false);
    else openLessons();
  }

  private void openSchedule() {
    if (requireActivity() instanceof com.example.asm_final.activities.HomeActivity) {
      ((com.example.asm_final.activities.HomeActivity) requireActivity()).navigateTo(0, false);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
    owner = ActiveUser.get(requireContext());
    subjectsView =
        new com.example.asm_final.ui.OverviewUi(
            requireContext(),
            false,
            subject -> openLesson(subject, selectedGrade),
            this::overviewAction,
            () -> {},
            () -> {},
            this::openLessons,
            this::openSchedule);
    root = subjectsView.view();
    return root;
  }

  @Override
  public void onResume() {
    super.onResume();
    load(true);
    updates = OverviewLiveBinding.start(this, () -> load(false));
  }

  @Override
  public void onPause() {
    if (updates != null) {
      updates.close();
      updates = null;
    }
    super.onPause();
  }

  @Override
  public void onDestroyView() {
    generation++;
    subjectsView = null;
    root = null;
    super.onDestroyView();
  }

  private void openLessons() {
    ((com.example.asm_final.activities.HomeActivity) requireActivity())
        .navigateTo(R.id.nav_category, false);
  }

  private void openLesson(SubjectId subject, int grade) {
    startActivity(
        new android.content.Intent(
                requireContext(), com.example.asm_final.activities.LessonActivity.class)
            .putExtra("SUBJECT", subject.wireId)
            .putExtra("GRADE", grade)
            .putExtra(
                "USERNAME",
                getArguments() == null ? "" : getArguments().getString("USERNAME", "")));
  }

  private void load(boolean loading) {
    if (root == null) return;
    int request = ++generation;
    View viewOwner = root;
    android.content.Context context = requireContext().getApplicationContext();
    AppServices services = ((AiMentorApplication) context).services();
    if (loading) subjectsView.loading();
    services
        .io()
        .execute(
            () -> {
              try {
                LearnerProfile profile = services.learners().load(owner);
                int grade = profile == null ? 0 : profile.grade;
                Set<SubjectId> selected;
                try (LearningSetupStore store = new LearningSetupStore(context)) {
                  selected = store.selected(owner, grade);
                }
                LearningOverviewRepository.Snapshot snapshot =
                    services.overview().load(owner, grade);
                main.post(
                    () -> {
                      if (root != viewOwner
                          || request != generation
                          || ActiveUser.get(context) != owner) return;
                      needsLogin = snapshot.needsLogin;
                      selectedGrade = grade;
                      retryOverview =
                          snapshot.mode != LearningOverviewRepository.Mode.LOCAL
                              && (snapshot.data == null
                                  || snapshot.mode == LearningOverviewRepository.Mode.CACHED);
                      subjectsView.identity(
                          profile == null
                              ? getString(R.string.home_default_user)
                              : profile.displayName,
                          null);
                      subjectsView.render(snapshot, selected, grade);
                    });
              } catch (RuntimeException failure) {
                main.post(
                    () -> {
                      if (root == viewOwner
                          && request == generation
                          && ActiveUser.get(context) == owner) {
                        retryOverview = true;
                        subjectsView.failure();
                      }
                    });
              }
            });
  }
}
