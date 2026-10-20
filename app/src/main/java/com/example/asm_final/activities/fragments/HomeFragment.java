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
  private DashboardSubjectsView subjectsView;
  private boolean needsLogin;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
    root = inflater.inflate(R.layout.fragment_home, parent, false);
    owner = ActiveUser.get(requireContext());
    subjectsView = new DashboardSubjectsView(root, this::openLesson);
    root.findViewById(R.id.button_start_learning).setOnClickListener(v -> openLessons());
    root.findViewById(R.id.button_chart_action)
        .setOnClickListener(
            v -> {
              if (needsLogin)
                ((com.example.asm_final.activities.HomeActivity) requireActivity()).logout();
              else if (Boolean.TRUE.equals(v.getTag())) load(false);
              else openLessons();
            });
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
                      ((TextView) root.findViewById(R.id.txt_home_content))
                          .setText(
                              profile == null
                                  ? getString(R.string.home_default_user)
                                  : profile.displayName);
                      TextView contextLabel = root.findViewById(R.id.tv_learning_context);
                      contextLabel.setVisibility(View.VISIBLE);
                      contextLabel.setText(
                          grade >= 10 && grade <= 12
                              ? getString(R.string.chat_grade, grade)
                              : getString(R.string.overview_choose_grade));
                      subjectsView.render(snapshot, selected, grade);
                    });
              } catch (RuntimeException failure) {
                main.post(
                    () -> {
                      if (root == viewOwner
                          && request == generation
                          && ActiveUser.get(context) == owner) subjectsView.failure();
                    });
              }
            });
  }
}
