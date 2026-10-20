package com.example.asm_final.activities.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asm_final.R;
import com.example.asm_final.utils.ExpLevelCalculator;
import com.example.asm_final.utils.RecentActivity;
import com.example.asm_final.utils.RecentActivityAdapter;
import com.example.asm_final.utils.SimplePieChartView;
import java.util.ArrayList;
import java.util.List;

/*
 * Role: Fragment for App Shell, responsible for a specific functional area of the screen and its user interactions.
 * Purpose: Using Fragments gives each tab or feature a focused responsibility and makes the screen flow easier to explain.
 */
public class HomeFragment extends Fragment {
  private com.example.asm_final.dashboard.OverviewUpdates.Subscription liveUpdates;

  private TextView txtUsername, tvTotalLevel, tvLevelTitle, tvExpDetail;
  private ProgressBar pbTotal;

  // Subject mini stats
  private View miniMath, miniScience, miniHistory, miniProg;

  // Advanced stats
  private SimplePieChartView pieChart;
  private ProgressBar pbExpMath, pbExpScience, pbExpHistory, pbExpProg;
  private RecyclerView rvRecent;

  private String username;
  private int userId;
  private int loadVersion;
  private boolean remoteOverview, remoteNeedsLogin;
  private int learningGrade;
  private boolean recentExpanded;
  private RecentActivity.Subject recentSubject;
  private List<RecentActivity> recentActivities = java.util.Collections.emptyList();

  /*
   * Responsibility: Initializes the Android component, inflates and binds the layout, and prepares initial data and listeners.
   * Purpose: This is the screen entry point, so the initial state must be prepared before user interaction begins.
   */
  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null)
      recentExpanded = savedInstanceState.getBoolean("recent_expanded");
    if (getArguments() != null) {
      username = getArguments().getString("USERNAME");
    }
  }

  /*
   * Responsibility: Handles the Android callback or event `onCreateView` and updates the related state.
   * Purpose: Keeping callbacks separate makes event flow clearer and follows the Android lifecycle cleanly.
   */
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_home, container, false);

    txtUsername = view.findViewById(R.id.txt_home_content);
    tvTotalLevel = view.findViewById(R.id.tv_total_level_value);
    tvLevelTitle = view.findViewById(R.id.tv_level_title);
    tvExpDetail = view.findViewById(R.id.tv_exp_detail);
    pbTotal = view.findViewById(R.id.pb_total_exp);

    miniMath = view.findViewById(R.id.mini_math);
    miniScience = view.findViewById(R.id.mini_science);
    miniHistory = view.findViewById(R.id.mini_history);
    miniProg = view.findViewById(R.id.mini_prog);

    pieChart = view.findViewById(R.id.pie_chart_exp);
    pbExpMath = view.findViewById(R.id.pb_time_math);
    pbExpScience = view.findViewById(R.id.pb_time_science);
    pbExpHistory = view.findViewById(R.id.pb_time_history);
    pbExpProg = view.findViewById(R.id.pb_time_prog);

    rvRecent = view.findViewById(R.id.rv_recent_activity);
    rvRecent.setLayoutManager(new LinearLayoutManager(requireContext()));

    View[] subjects = {miniMath, miniScience, miniHistory, miniProg};
    RecentActivity.Subject[] names = {
      RecentActivity.Subject.MATH,
      RecentActivity.Subject.SCIENCE,
      RecentActivity.Subject.HISTORY,
      RecentActivity.Subject.PROGRAMMING
    };
    for (int i = 0; i < subjects.length; i++) {
      final RecentActivity.Subject subject = names[i];
      final int position = i;
      subjects[i].setOnClickListener(
          v -> {
            if (remoteOverview) {
              openRemoteSubject(position);
              return;
            }
            if (userId > 0)
              com.example.asm_final.utils.SubjectNavigator.open(requireContext(), subject, userId);
          });
      subjects[i].setFocusable(true);
    }
    view.findViewById(R.id.button_start_learning)
        .setOnClickListener(
            v -> {
              if (remoteOverview) {
                ((com.example.asm_final.activities.HomeActivity) requireActivity())
                    .navigateTo(R.id.nav_category, false);
              } else if (recentSubject != null && userId > 0)
                com.example.asm_final.utils.SubjectNavigator.open(
                    requireContext(), recentSubject, userId);
              else
                ((com.example.asm_final.activities.HomeActivity) requireActivity())
                    .navigateTo(R.id.nav_category, false);
            });
    view.findViewById(R.id.button_chart_action)
        .setOnClickListener(
            v -> {
              if (remoteNeedsLogin)
                ((com.example.asm_final.activities.HomeActivity) requireActivity()).logout();
              else if (Boolean.TRUE.equals(v.getTag())) setupData();
              else
                ((com.example.asm_final.activities.HomeActivity) requireActivity())
                    .navigateTo(R.id.nav_category, false);
            });
    view.findViewById(R.id.button_recent_more)
        .setOnClickListener(
            v -> {
              recentExpanded = !recentExpanded;
              renderRecent();
            });
    return view;
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    out.putBoolean("recent_expanded", recentExpanded);
    super.onSaveInstanceState(out);
  }

  @Override
  public void onResume() {
    super.onResume();
    setupData();
    liveUpdates =
        com.example.asm_final.dashboard.OverviewLiveBinding.start(this, () -> setupData(false));
  }

  @Override
  public void onPause() {
    if (liveUpdates != null) {
      liveUpdates.close();
      liveUpdates = null;
    }
    super.onPause();
  }

  private void setupData() {
    setupData(true);
  }

  private void setupData(boolean showLoading) {
    if (getView() == null) return;
    if (showLoading)
      renderChartState(com.example.asm_final.dashboard.DashboardChartState.loading(), 0);
    if (showLoading
        && !com.example.asm_final.backend.BackendClient.endpoint(requireContext()).isEmpty()) {
      tvTotalLevel.setText("—");
      tvLevelTitle.setText(R.string.auth_processing);
      tvExpDetail.setVisibility(View.GONE);
      pbTotal.setProgress(0);
      getView().findViewById(R.id.dashboard_subject_grid).setVisibility(View.GONE);
      rvRecent.setVisibility(View.GONE);
      getView().findViewById(R.id.text_recent_empty).setVisibility(View.GONE);
      getView().findViewById(R.id.button_recent_more).setVisibility(View.GONE);
      getView().findViewById(R.id.tv_dashboard_status).setVisibility(View.GONE);
    }
    if (username == null || username.isEmpty()) {
      txtUsername.setText(R.string.home_default_user);
      renderChartState(
          com.example.asm_final.dashboard.DashboardChartState.from(
              new com.example.asm_final.dashboard.DashboardSnapshot(
                  new java.util.EnumMap<>(RecentActivity.Subject.class),
                  java.util.Collections.emptyList(),
                  java.util.EnumSet.allOf(RecentActivity.Subject.class))),
          0);
      return;
    }

    txtUsername.setText(username);
    final int version = ++loadVersion;
    final View owner = getView();
    final android.content.Context context = requireContext().getApplicationContext();
    com.example.asm_final.AppServices services =
        ((com.example.asm_final.AiMentorApplication) context).services();
    services
        .io()
        .execute(
            () -> {
              com.example.asm_final.dashboard.DashboardSnapshot loaded;
              com.example.asm_final.dashboard.LearningOverviewRepository.Snapshot overview = null;
              com.example.asm_final.onboarding.LearnerProfile learner = null;
              int resolvedId = 0;
              try {
                resolvedId = services.accountLookup().userId(username);
                learner = services.learners().load(resolvedId);
                overview =
                    services.overview().load(resolvedId, learner == null ? 0 : learner.grade);
                loaded =
                    overview.mode
                            == com.example.asm_final.dashboard.LearningOverviewRepository.Mode.LOCAL
                        ? services.dashboard().load(resolvedId)
                        : null;
              } catch (RuntimeException error) {
                loaded =
                    new com.example.asm_final.dashboard.DashboardSnapshot(
                        new java.util.EnumMap<>(RecentActivity.Subject.class),
                        java.util.Collections.emptyList(),
                        java.util.EnumSet.allOf(RecentActivity.Subject.class));
              }
              final com.example.asm_final.dashboard.LearningOverviewRepository.Snapshot remote =
                  overview;
              final com.example.asm_final.onboarding.LearnerProfile learningProfile = learner;
              final int loadedUserId = resolvedId;
              final com.example.asm_final.dashboard.DashboardSnapshot snapshot = loaded;
              new android.os.Handler(android.os.Looper.getMainLooper())
                  .post(
                      () -> {
                        if (isAdded()
                            && getView() == owner
                            && loadVersion == version
                            && (loadedUserId <= 0
                                || com.example.mentor.data.ActiveUser.get(context)
                                    == loadedUserId)) {
                          userId = loadedUserId;
                          learningGrade = learningProfile == null ? 0 : learningProfile.grade;
                          if (learningProfile != null && !learningProfile.displayName.isEmpty())
                            txtUsername.setText(learningProfile.displayName);
                          TextView contextLabel = getView().findViewById(R.id.tv_learning_context);
                          boolean hasContext =
                              learningProfile != null
                                  && learningProfile.grade >= 10
                                  && learningProfile.grade <= 12
                                  && learningProfile.goal >= 0
                                  && learningProfile.goal < 4;
                          contextLabel.setVisibility(hasContext ? View.VISIBLE : View.GONE);
                          if (hasContext)
                            contextLabel.setText(
                                getString(
                                    R.string.profile_context,
                                    learningProfile.grade,
                                    getResources()
                                        .getStringArray(R.array.learner_goals)[
                                        learningProfile.goal]));
                          if (remote != null
                              && remote.mode
                                  != com.example.asm_final.dashboard.LearningOverviewRepository.Mode
                                      .LOCAL) renderRemote(remote);
                          else render(snapshot);
                        }
                      });
            });
  }

  private void render(com.example.asm_final.dashboard.DashboardSnapshot snapshot) {
    remoteOverview = false;
    remoteNeedsLogin = false;
    getView().findViewById(R.id.dashboard_subject_grid).setVisibility(View.VISIBLE);
    miniProg.setVisibility(View.VISIBLE);
    rvRecent.setVisibility(View.VISIBLE);
    tvExpDetail.setVisibility(View.VISIBLE);
    ((TextView) getView().findViewById(R.id.tv_total_level_label))
        .setText(R.string.home_overall_level);
    ((TextView) getView().findViewById(R.id.dashboard_recent_heading))
        .setText(R.string.home_recent_activity);
    getView().findViewById(R.id.chart_exp_prog).setVisibility(View.VISIBLE);
    pbExpProg.setVisibility(View.VISIBLE);
    int mathExp = snapshot.progress(RecentActivity.Subject.MATH).experience;
    int scienceExp = snapshot.progress(RecentActivity.Subject.SCIENCE).experience;
    int historyExp = snapshot.progress(RecentActivity.Subject.HISTORY).experience;
    int programmingExp = snapshot.progress(RecentActivity.Subject.PROGRAMMING).experience;
    int totalExp =
        ExpLevelCalculator.totalExperience(mathExp, historyExp, programmingExp, scienceExp);

    // --- UPDATE UI TOTAL ---
    int level = ExpLevelCalculator.getLevel(totalExp);
    tvTotalLevel.setText(getString(R.string.home_level_number, level));
    tvLevelTitle.setText(ExpLevelCalculator.getLevelTitleResId(level));
    pbTotal.setProgress(ExpLevelCalculator.getProgressPercentage(totalExp));
    tvExpDetail.setText(
        level >= com.example.mentor.data.QuizPolicy.MAX_LEVEL
            ? getString(R.string.home_max_level_reached)
            : getString(
                R.string.home_exp_remaining, ExpLevelCalculator.getExpToNextLevel(totalExp)));

    // --- UPDATE MINI CARDS ---
    updateMiniStat(miniMath, getString(R.string.cat_math), R.drawable.ic_cat_math, mathExp);
    updateMiniStat(
        miniScience, getString(R.string.cat_science), R.drawable.ic_cat_science, scienceExp);
    updateMiniStat(
        miniHistory, getString(R.string.cat_history), R.drawable.ic_cat_history, historyExp);
    updateMiniStat(
        miniProg, getString(R.string.cat_it), R.drawable.ic_cat_programming, programmingExp);

    // --- UPDATE PIE CHART ---
    List<SimplePieChartView.Slice> slices = new ArrayList<>();
    slices.add(
        new SimplePieChartView.Slice(
            mathExp, ContextCompat.getColor(requireContext(), R.color.cat_math_text)));
    slices.add(
        new SimplePieChartView.Slice(
            scienceExp, ContextCompat.getColor(requireContext(), R.color.cat_science_text)));
    slices.add(
        new SimplePieChartView.Slice(
            historyExp, ContextCompat.getColor(requireContext(), R.color.cat_history_text)));
    slices.add(
        new SimplePieChartView.Slice(
            programmingExp, ContextCompat.getColor(requireContext(), R.color.cat_it_text)));
    pieChart.setData(slices);

    int maximum =
        Math.max(1, Math.max(mathExp, Math.max(historyExp, Math.max(programmingExp, scienceExp))));
    pbExpMath.setProgress((int) (mathExp * 100L / maximum));
    pbExpScience.setProgress((int) (scienceExp * 100L / maximum));
    pbExpHistory.setProgress((int) (historyExp * 100L / maximum));
    pbExpProg.setProgress((int) (programmingExp * 100L / maximum));
    recentActivities = snapshot.activities;
    renderRecent();
    RecentActivity latest = null;
    for (RecentActivity activity : snapshot.activities)
      if (activity.getTimestamp() > 0
          && (latest == null || activity.getTimestamp() > latest.getTimestamp())) latest = activity;
    recentSubject = latest == null ? null : latest.getSubject();
    int label =
        recentSubject == RecentActivity.Subject.MATH
            ? R.string.cat_math
            : recentSubject == RecentActivity.Subject.SCIENCE
                ? R.string.cat_science
                : recentSubject == RecentActivity.Subject.HISTORY
                    ? R.string.cat_history
                    : R.string.cat_it;
    ((TextView) getView().findViewById(R.id.button_start_learning))
        .setText(
            recentSubject == null
                ? getString(R.string.ux_choose_subject)
                : getString(R.string.ux_reopen_subject, getString(label)));
    View view = getView();
    view.findViewById(R.id.text_recent_empty)
        .setVisibility(
            snapshot.activities.isEmpty() && snapshot.unavailable.isEmpty()
                ? View.VISIBLE
                : View.GONE);
    boolean incomplete = !snapshot.unavailable.isEmpty();
    ((TextView) view.findViewById(R.id.tv_chart_center))
        .setText(
            totalExp == 0
                ? (incomplete ? "—" : getString(R.string.zero_count))
                : getString(R.string.home_chart_center));
    pieChart.setContentDescription(
        incomplete
            ? getString(R.string.home_chart_partial)
            : getString(R.string.home_chart_accessibility, totalExp));
    int[] legendIds = {
      R.id.chart_exp_math, R.id.chart_exp_science, R.id.chart_exp_history, R.id.chart_exp_prog
    };
    int[] subjectLabels = {
      R.string.cat_math, R.string.cat_science, R.string.cat_history, R.string.cat_it
    };
    RecentActivity.Subject[] subjectNames = {
      RecentActivity.Subject.MATH,
      RecentActivity.Subject.SCIENCE,
      RecentActivity.Subject.HISTORY,
      RecentActivity.Subject.PROGRAMMING
    };
    for (int i = 0; i < legendIds.length; i++) {
      String labelText = getString(subjectLabels[i]);
      ((TextView) view.findViewById(legendIds[i]))
          .setText(
              snapshot.unavailable.contains(subjectNames[i])
                  ? getString(R.string.home_chart_subject_unavailable, labelText)
                  : getString(
                      R.string.home_chart_subject,
                      labelText,
                      snapshot.progress(subjectNames[i]).experience));
    }
    renderChartState(com.example.asm_final.dashboard.DashboardChartState.from(snapshot), totalExp);
    TextView status = getView().findViewById(R.id.tv_dashboard_status);
    status.setVisibility(snapshot.unavailable.isEmpty() ? View.GONE : View.VISIBLE);
    status.setOnClickListener(v -> setupData());
  }

  private void openRemoteSubject(int index) {
    if (index < 0 || index >= 3 || learningGrade < 10 || learningGrade > 12) {
      ((com.example.asm_final.activities.HomeActivity) requireActivity())
          .navigateTo(R.id.nav_category, false);
      return;
    }
    String[] ids = {"biology", "chemistry", "physics"};
    startActivity(
        new android.content.Intent(
                requireContext(), com.example.asm_final.activities.LessonActivity.class)
            .putExtra("SUBJECT", ids[index])
            .putExtra("GRADE", learningGrade)
            .putExtra("USERNAME", username));
  }

  private void renderRemote(
      com.example.asm_final.dashboard.LearningOverviewRepository.Snapshot snapshot) {
    remoteOverview = true;
    remoteNeedsLogin = snapshot.needsLogin;
    recentSubject = null;
    View view = getView();
    com.example.asm_final.dashboard.LearningOverviewData data = snapshot.data;
    ((TextView) view.findViewById(R.id.tv_total_level_label)).setText(R.string.overview_title);
    ((TextView) view.findViewById(R.id.button_start_learning)).setText(R.string.ux_choose_subject);
    ((TextView) view.findViewById(R.id.dashboard_recent_heading))
        .setText(R.string.overview_practice_heading);
    rvRecent.setVisibility(View.GONE);
    view.findViewById(R.id.button_recent_more).setVisibility(View.GONE);
    miniProg.setVisibility(View.GONE);
    view.findViewById(R.id.chart_exp_prog).setVisibility(View.GONE);
    pbExpProg.setVisibility(View.GONE);
    TextView status = view.findViewById(R.id.tv_dashboard_status);
    status.setVisibility(View.VISIBLE);
    status.setOnClickListener(v -> setupData());
    int notice =
        snapshot.needsLogin
            ? R.string.overview_expired
            : snapshot.mode
                    == com.example.asm_final.dashboard.LearningOverviewRepository.Mode.CACHED
                ? R.string.overview_cached
                : snapshot.mode
                        == com.example.asm_final.dashboard.LearningOverviewRepository.Mode.NO_GRADE
                    ? R.string.overview_choose_grade
                    : data == null ? R.string.overview_error : R.string.overview_current;
    status.setText(notice);
    view.findViewById(R.id.dashboard_subject_grid)
        .setVisibility(data == null ? View.GONE : View.VISIBLE);
    TextView practice = view.findViewById(R.id.text_recent_empty);
    practice.setVisibility(View.VISIBLE);
    if (data == null) {
      tvTotalLevel.setText("—");
      tvLevelTitle.setText(R.string.overview_title);
      tvExpDetail.setVisibility(View.GONE);
      pbTotal.setProgress(0);
      renderChartState(com.example.asm_final.dashboard.DashboardChartState.unavailable(), 0);
      ((TextView) view.findViewById(R.id.chart_placeholder_title))
          .setText(R.string.home_chart_error_title);
      ((TextView) view.findViewById(R.id.tv_chart_summary)).setText(notice);
      practice.setText(
          snapshot.mode == com.example.asm_final.dashboard.LearningOverviewRepository.Mode.NO_GRADE
              ? R.string.overview_choose_grade
              : R.string.overview_error);
    } else {
      tvExpDetail.setVisibility(View.VISIBLE);
      tvTotalLevel.setText(
          data.totalLessons == 0
              ? "—"
              : getString(R.string.overview_lessons, data.completedLessons, data.totalLessons));
      boolean hasCurriculum = false;
      for (com.example.asm_final.dashboard.LearningOverviewData.Subject subject : data.subjects)
        hasCurriculum |= subject.curriculumAvailable;
      tvLevelTitle.setText(
          data.totalLessons > 0
              ? getString(R.string.overview_daily_xp, data.dailyXp)
              : getString(
                  hasCurriculum
                      ? R.string.overview_no_enrollment
                      : R.string.overview_no_curriculum));
      tvExpDetail.setText(getString(R.string.overview_streak, data.streakDays));
      pbTotal.setProgress(
          data.totalLessons == 0 ? 0 : (int) (data.completedLessons * 100L / data.totalLessons));
      List<SimplePieChartView.Slice> slices = new ArrayList<>();
      View[] cards = {miniMath, miniScience, miniHistory};
      int[] colors = {R.color.cat_science_text, R.color.cat_math_text, R.color.cat_history_text};
      int[] legends = {R.id.chart_exp_math, R.id.chart_exp_science, R.id.chart_exp_history};
      ProgressBar[] bars = {pbExpMath, pbExpScience, pbExpHistory};
      long maximum = 1;
      for (com.example.asm_final.dashboard.LearningOverviewData.Subject subject : data.subjects)
        maximum = Math.max(maximum, subject.experience);
      for (int i = 0; i < data.subjects.size(); i++) {
        com.example.asm_final.dashboard.LearningOverviewData.Subject subject = data.subjects.get(i);
        String name =
            com.example.mentor.learning.SubjectId.valueOf(
                    subject.id.toUpperCase(java.util.Locale.ROOT))
                .label(com.example.asm_final.utils.LocaleHelper.getLanguage(requireContext()));
        int color = ContextCompat.getColor(requireContext(), colors[i]);
        slices.add(new SimplePieChartView.Slice(subject.experience, color));
        ((TextView) cards[i].findViewById(R.id.tv_mini_name)).setText(name);
        ((ImageView) cards[i].findViewById(R.id.iv_mini_icon))
            .setImageResource(R.drawable.ic_cat_science);
        String detail =
            getString(
                R.string.overview_subject_progress,
                subject.completed,
                subject.total,
                subject.experience);
        ((TextView) cards[i].findViewById(R.id.tv_mini_exp)).setText(detail);
        cards[i].setContentDescription(name + ", " + detail);
        ((ProgressBar) cards[i].findViewById(R.id.pb_mini_stat))
            .setProgress(subject.total == 0 ? 0 : (int) (subject.completed * 100L / subject.total));
        ((TextView) view.findViewById(legends[i]))
            .setText(getString(R.string.home_chart_subject, name, subject.experience));
        bars[i].setProgress((int) (subject.experience / (double) maximum * 100));
        bars[i].setProgressTintList(android.content.res.ColorStateList.valueOf(color));
      }
      pieChart.setData(slices);
      pieChart.setContentDescription(getString(R.string.home_chart_accessibility, data.totalXp));
      ((TextView) view.findViewById(R.id.tv_chart_center))
          .setText(
              data.totalXp == 0
                  ? getString(R.string.zero_count)
                  : getString(R.string.home_chart_center));
      renderChartState(
          com.example.asm_final.dashboard.DashboardChartState.recorded(
              data.totalXp > 0,
              snapshot.mode
                  != com.example.asm_final.dashboard.LearningOverviewRepository.Mode.LIVE),
          data.totalXp);
      if (snapshot.mode != com.example.asm_final.dashboard.LearningOverviewRepository.Mode.LIVE)
        ((TextView) view.findViewById(R.id.tv_chart_summary)).setText(notice);
      else if (data.totalXp > 0)
        ((TextView) view.findViewById(R.id.tv_chart_summary))
            .setText(
                getString(R.string.home_chart_total, data.totalXp)
                    + "\n"
                    + getString(R.string.overview_source));
      practice.setText(
          data.questionCount == 0
              ? getString(R.string.overview_no_practice)
              : getString(R.string.overview_practice, data.firstTryCorrect, data.questionCount)
                  + "\n"
                  + getString(R.string.overview_assisted, data.assisted));
    }
    com.google.android.material.button.MaterialButton action =
        view.findViewById(R.id.button_chart_action);
    action.setVisibility(View.VISIBLE);
    action.setEnabled(true);
    boolean chooseGrade =
        snapshot.mode == com.example.asm_final.dashboard.LearningOverviewRepository.Mode.NO_GRADE;
    boolean start =
        !snapshot.needsLogin
            && (chooseGrade
                || (data != null
                    && data.totalXp == 0
                    && snapshot.mode
                        == com.example.asm_final.dashboard.LearningOverviewRepository.Mode.LIVE));
    action.setTag(!start);
    action.setText(
        snapshot.needsLogin
            ? R.string.overview_sign_in
            : start ? R.string.home_chart_start : R.string.overview_reload);
  }

  private void renderChartState(
      com.example.asm_final.dashboard.DashboardChartState state, long totalExp) {
    View view = getView();
    if (view == null) return;
    boolean loading =
        state.phase == com.example.asm_final.dashboard.DashboardChartState.Phase.LOADING;
    view.findViewById(R.id.chart_data_group)
        .setVisibility(state.showChart ? View.VISIBLE : View.GONE);
    view.findViewById(R.id.chart_placeholder)
        .setVisibility(state.showChart ? View.GONE : View.VISIBLE);
    view.findViewById(R.id.chart_loading_spinner).setVisibility(loading ? View.VISIBLE : View.GONE);
    view.findViewById(R.id.chart_placeholder_icon)
        .setVisibility(loading ? View.GONE : View.VISIBLE);
    int title, message;
    switch (state.phase) {
      case LOADING:
        title = R.string.home_chart_loading;
        message = R.string.home_chart_loading_hint;
        break;
      case ERROR:
        title = R.string.home_chart_error_title;
        message = R.string.home_chart_error;
        break;
      case PARTIAL:
        title = R.string.home_chart_partial_title;
        message = R.string.home_chart_partial;
        break;
      case EMPTY:
        title = R.string.home_chart_empty_title;
        message = R.string.home_chart_empty;
        break;
      default:
        title = R.string.home_completion_ratio;
        message = R.string.home_chart_total;
    }
    ((TextView) view.findViewById(R.id.chart_placeholder_title)).setText(title);
    TextView summary = view.findViewById(R.id.tv_chart_summary);
    summary.setText(
        message == R.string.home_chart_total ? getString(message, totalExp) : getString(message));
    summary.setTextAlignment(
        state.showChart ? View.TEXT_ALIGNMENT_VIEW_START : View.TEXT_ALIGNMENT_CENTER);
    com.google.android.material.button.MaterialButton action =
        view.findViewById(R.id.button_chart_action);
    action.setVisibility(
        !loading && (state.retry() || !state.showChart) ? View.VISIBLE : View.GONE);
    action.setEnabled(!loading);
    action.setTag(state.retry());
    action.setText(state.retry() ? R.string.home_chart_retry : R.string.home_chart_start);
  }

  /*
   * Responsibility: Updates or synchronizes `update mini stat` with the latest data or state.
   * Purpose: Keeps the UI and data synchronized after user actions.
   */
  private void renderRecent() {
    int size = recentExpanded ? recentActivities.size() : Math.min(3, recentActivities.size());
    rvRecent.setAdapter(
        new RecentActivityAdapter(new ArrayList<>(recentActivities.subList(0, size))));
    TextView more = getView().findViewById(R.id.button_recent_more);
    more.setVisibility(recentActivities.size() > 3 ? View.VISIBLE : View.GONE);
    more.setText(recentExpanded ? R.string.home_less_activity : R.string.home_more_activity);
  }

  private void updateMiniStat(View root, String name, int icon, int exp) {
    ((ImageView) root.findViewById(R.id.iv_mini_icon)).setImageResource(icon);
    ((TextView) root.findViewById(R.id.tv_mini_name)).setText(name);
    root.setContentDescription(
        getString(
            R.string.home_subject_progress_description,
            name,
            ExpLevelCalculator.getLevel(exp),
            exp));
    ((TextView) root.findViewById(R.id.tv_mini_exp))
        .setText(getString(R.string.home_subject_level, ExpLevelCalculator.getLevel(exp), exp));
    ((ProgressBar) root.findViewById(R.id.pb_mini_stat))
        .setProgress(com.example.mentor.data.QuizPolicy.progress(exp));
  }
}
