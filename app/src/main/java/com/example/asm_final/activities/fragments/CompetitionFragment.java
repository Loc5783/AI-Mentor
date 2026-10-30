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
import com.example.mentor.data.ActiveUser;
import com.google.android.material.chip.*;

/** Renders competition state. Network, score policy and lifecycle jobs live outside the view. */
public final class CompetitionFragment extends Fragment {
  private CompetitionViewModel model;
  private OverviewUpdates.Subscription liveUpdates;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle saved) {
    model = new ViewModelProvider(this).get(CompetitionViewModel.class);
    return com.example.asm_final.ui.CompetitionUi.view(
        requireContext(),
        model,
        () -> {
          CompetitionViewModel.State current = model.state().getValue();
          if (current != null && current.snapshot != null && current.snapshot.needsLogin)
            ((com.example.asm_final.activities.HomeActivity) requireActivity()).logout();
          else model.refresh();
        },
        () ->
            ((com.example.asm_final.activities.HomeActivity) requireActivity())
                .navigateTo(R.id.nav_category, false),
        id -> {
          if (getChildFragmentManager().findFragmentByTag("public_learner") == null)
            PublicLearnerDialog.create(id).show(getChildFragmentManager(), "public_learner");
        });
  }

  @Override
  public void onResume() {
    super.onResume();
    model.refresh();
    model.refreshFriends();
    liveUpdates =
        ((AiMentorApplication) requireActivity().getApplication())
            .services()
            .rankingUpdates()
            .observe(
                ActiveUser.get(requireContext()),
                () -> {
                  model.refresh();
                  model.refreshFriends();
                });
  }

  @Override
  public void onPause() {
    if (liveUpdates != null) {
      liveUpdates.close();
      liveUpdates = null;
    }
    super.onPause();
  }
}
