package com.example.asm_final.activities.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.example.asm_final.*;
import com.example.asm_final.competition.*;
import com.example.asm_final.dashboard.OverviewUpdates;
import com.example.asm_final.utils.ScreenViews;
import com.example.mentor.data.ActiveUser;
import com.google.android.material.chip.*;

/** Renders competition state. Network, score policy and lifecycle jobs live outside the view. */
public final class CompetitionFragment extends Fragment {
  private CompetitionViewModel model;
  private RankingAdapter adapter;
  private TextView own, status, source;
  private Button refresh;
  private OverviewUpdates.Subscription liveUpdates;
  private Chip streakChip, xpChip, friendsChip, globalChip;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle saved) {
    model = new ViewModelProvider(this).get(CompetitionViewModel.class);
    LinearLayout header = ScreenViews.column(requireContext());
    header.addView(
        ScreenViews.text(requireContext(), getString(R.string.competition_title), 28, true));
    header.addView(
        ScreenViews.text(requireContext(), getString(R.string.competition_description), 15, false));
    ChipGroup metrics = group();
    streakChip = chip(metrics, R.string.competition_streak, R.id.ranking_metric_streak);
    xpChip = chip(metrics, R.string.competition_daily, R.id.ranking_metric_xp);
    header.addView(metrics);
    ChipGroup scope = group();
    friendsChip = chip(scope, R.string.competition_friends, R.id.ranking_scope_friends);
    globalChip = chip(scope, R.string.competition_everyone, R.id.ranking_scope_global);
    header.addView(scope);
    LinearLayout card = ScreenViews.card(requireContext());
    card.addView(
        ScreenViews.text(requireContext(), getString(R.string.competition_you), 15, false));
    own = ScreenViews.text(requireContext(), "…", 30, true);
    own.setId(R.id.competition_own_value);
    card.addView(own);
    source = ScreenViews.text(requireContext(), "", 13, false);
    card.addView(source);
    header.addView(card);
    status = ScreenViews.text(requireContext(), "", 15, false);
    status.setId(R.id.ranking_status);
    status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    header.addView(status);
    refresh =
        ScreenViews.button(
            requireContext(),
            R.string.ranking_retry,
            () -> {
              CompetitionViewModel.State current = model.state().getValue();
              if (current != null && current.snapshot != null && current.snapshot.needsLogin)
                ((com.example.asm_final.activities.HomeActivity) requireActivity()).logout();
              else model.refresh();
            });
    refresh.setId(R.id.ranking_refresh);
    header.addView(refresh);
    header.addView(
        ScreenViews.text(requireContext(), getString(R.string.ranking_rules), 13, false));
    header.addView(
        ScreenViews.button(
            requireContext(),
            R.string.competition_learn,
            () ->
                ((com.example.asm_final.activities.HomeActivity) requireActivity())
                    .navigateTo(R.id.nav_category, false)));
    RecyclerView list = new RecyclerView(requireContext());
    list.setId(R.id.ranking_list);
    list.setBackgroundColor(
        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.backgroundColor));
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter =
        new RankingAdapter(
            header,
            learner -> {
              if (getChildFragmentManager().findFragmentByTag("public_learner") == null)
                PublicLearnerDialog.create(learner.id)
                    .show(getChildFragmentManager(), "public_learner");
            });
    list.setAdapter(adapter);
    CompetitionQuery query = model.query();
    (query.metric == CompetitionQuery.Metric.STREAK ? streakChip : xpChip).setChecked(true);
    (query.scope == CompetitionQuery.Scope.FRIENDS ? friendsChip : globalChip).setChecked(true);
    metrics.setOnCheckedStateChangeListener(
        (g, ids) -> {
          if (!ids.isEmpty())
            model.select(
                new CompetitionQuery(
                    ids.contains(R.id.ranking_metric_xp)
                        ? CompetitionQuery.Metric.DAILY_XP
                        : CompetitionQuery.Metric.STREAK,
                    model.query().scope));
        });
    scope.setOnCheckedStateChangeListener(
        (g, ids) -> {
          if (!ids.isEmpty())
            model.select(
                new CompetitionQuery(
                    model.query().metric,
                    ids.contains(R.id.ranking_scope_global)
                        ? CompetitionQuery.Scope.GLOBAL
                        : CompetitionQuery.Scope.FRIENDS));
        });
    return list;
  }

  private ChipGroup group() {
    ChipGroup group = new ChipGroup(requireContext());
    group.setSingleSelection(true);
    group.setSelectionRequired(true);
    return group;
  }

  private Chip chip(ChipGroup group, int label, int id) {
    Chip chip = new Chip(requireContext());
    chip.setId(id);
    chip.setText(label);
    chip.setCheckable(true);
    chip.setEnsureMinTouchTargetSize(true);
    group.addView(chip);
    return chip;
  }

  @Override
  public void onViewCreated(View view, Bundle saved) {
    model.state().observe(getViewLifecycleOwner(), this::render);
  }

  @Override
  public void onResume() {
    super.onResume();
    model.refresh();
    liveUpdates =
        ((AiMentorApplication) requireActivity().getApplication())
            .services()
            .rankingUpdates()
            .observe(ActiveUser.get(requireContext()), model::refresh);
  }

  @Override
  public void onPause() {
    if (liveUpdates != null) {
      liveUpdates.close();
      liveUpdates = null;
    }
    super.onPause();
  }

  private void render(CompetitionViewModel.State state) {
    CompetitionRepository.Snapshot snapshot = state.snapshot;
    refresh.setText(
        snapshot != null && snapshot.needsLogin ? R.string.login_btn : R.string.ranking_retry);
    boolean xp = state.query.metric == CompetitionQuery.Metric.DAILY_XP;
    Long score = null;
    if (snapshot != null) {
      if (xp) score = snapshot.dailyXp;
      else if (snapshot.streak != null) score = snapshot.streak.longValue();
    }
    own.setText(
        score == null
            ? "—"
            : getString(
                xp ? R.string.competition_xp_value : R.string.competition_streak_value, score));
    source.setText(
        snapshot == null
            ? R.string.ranking_loading
            : snapshot.source == CompetitionRepository.Source.LOCAL
                ? R.string.competition_local
                : snapshot.cached
                    ? R.string.overview_cached
                    : snapshot.dailyXp == null
                        ? R.string.overview_error
                        : R.string.overview_current);
    RankingBoard board = snapshot == null ? null : snapshot.board;
    if (state.loading) status.setText(R.string.ranking_loading);
    else if (snapshot == null || snapshot.source == CompetitionRepository.Source.UNAVAILABLE)
      status.setText(
          snapshot != null && snapshot.needsLogin
              ? R.string.ranking_login
              : R.string.ranking_error);
    else if (snapshot.source == CompetitionRepository.Source.LOCAL)
      status.setText(R.string.competition_disconnected);
    else if (board == null || board.entries.isEmpty()) status.setText(R.string.ranking_empty);
    else if (state.query.scope == CompetitionQuery.Scope.FRIENDS && board.entries.size() == 1)
      status.setText(
          getString(R.string.ranking_friends_empty)
              + "\n"
              + getString(R.string.ranking_day, board.learningDay));
    else status.setText(getString(R.string.ranking_day, board.learningDay));
    adapter.submit(board);
  }

  @Override
  public void onDestroyView() {
    adapter = null;
    own = null;
    status = null;
    source = null;
    refresh = null;
    streakChip = null;
    xpChip = null;
    friendsChip = null;
    globalChip = null;
    super.onDestroyView();
  }
}
