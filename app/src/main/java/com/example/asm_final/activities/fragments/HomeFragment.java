package com.example.asm_final.activities.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.example.asm_final.*;
import com.example.asm_final.dashboard.*;
import com.example.asm_final.onboarding.LearnerProfile;
import com.example.asm_final.utils.*;
import com.example.mentor.data.ActiveUser;
import java.util.Calendar;
import java.util.List;

/** Coordinates account-scoped dashboard and study-time loading. */
public final class HomeFragment extends Fragment {
  private final Handler main = new Handler(Looper.getMainLooper());
  private View root;
  private int owner, generation;
  private OverviewUpdates.Subscription updates;
  private com.example.asm_final.ui.OverviewUi subjectsView;
  private boolean needsLogin;
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

  private void openChat() {
    ((com.example.asm_final.activities.HomeActivity) requireActivity()).navigateTo(R.id.nav_chat, false);
  }

  private void openCompetition() {
    ((com.example.asm_final.activities.HomeActivity) requireActivity()).navigateTo(R.id.nav_competition, false);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
    owner = ActiveUser.get(requireContext());
    subjectsView =
        new com.example.asm_final.ui.OverviewUi(
            requireContext(),
            false,
            this::overviewAction,
            () -> {},
            () -> {},
            this::openLessons,
            this::openSchedule,
            this::openChat,
            this::openCompetition);
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
                LearningOverviewRepository.Snapshot snapshot =
                    services.overview().load(owner, grade);
                main.post(
                    () -> {
                      if (root != viewOwner
                          || request != generation
                          || ActiveUser.get(context) != owner) return;
                      needsLogin = snapshot.needsLogin;
                      retryOverview =
                          snapshot.mode != LearningOverviewRepository.Mode.LOCAL
                              && (snapshot.data == null
                                  || snapshot.mode == LearningOverviewRepository.Mode.CACHED);
                      subjectsView.identity(
                          profile == null
                              ? getString(R.string.home_default_user)
                              : profile.displayName,
                          null);
                      subjectsView.render(snapshot, grade);
                    });
                List<com.example.mentor.schedule.domain.TodoTask> todayTasks = null;
                try {
                  int day = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1;
                  var repository = ((AiMentorApplication) context).scheduleRepository();
                  try {
                    todayTasks = repository.getTodos(Integer.toString(owner), day);
                  } finally {
                    ((com.example.mentor.schedule.domain.ScheduleImportRepository) repository).close();
                  }
                } catch (RuntimeException ignored) {
                  // An unavailable schedule should not hide the rest of Home.
                }
                List<com.example.mentor.schedule.domain.TodoTask> finalTodayTasks = todayTasks;
                main.post(
                    () -> {
                      if (root == viewOwner
                          && request == generation
                          && ActiveUser.get(context) == owner) subjectsView.tasks(finalTodayTasks);
                    });
                org.json.JSONObject studySummary = null;
                if (com.example.asm_final.supabase.SupabaseHttp.enabled(context)) {
                  try {
                    studySummary = services.study().summary(owner);
                  } catch (RuntimeException ignored) {
                    // Keep the rest of Home usable when this optional summary is unavailable.
                  }
                }
                org.json.JSONObject finalStudySummary = studySummary;
                main.post(
                    () -> {
                      if (root == viewOwner
                          && request == generation
                          && ActiveUser.get(context) == owner) subjectsView.activity(finalStudySummary);
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
