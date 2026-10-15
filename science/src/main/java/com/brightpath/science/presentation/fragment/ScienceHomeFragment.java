package com.brightpath.science.presentation.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.brightpath.science.R;
import com.brightpath.science.di.ScienceModule;
import com.brightpath.science.domain.model.ScienceSubject;
import com.brightpath.science.presentation.CircuitDiagramActivity;
import com.brightpath.science.presentation.PeriodicTableActivity;
import com.brightpath.science.presentation.ScienceActivity;
import com.brightpath.science.presentation.ScienceMainActivity;
import com.brightpath.science.presentation.ScienceProgressActivity;
import com.brightpath.science.util.ScienceLevelCalculator;
import com.brightpath.science.util.SciencePreferenceStore;

public final class ScienceHomeFragment extends Fragment {
    public ScienceHomeFragment() { super(R.layout.fragment_science_home); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.buttonHomeBack).setOnClickListener(v -> requireActivity().finish());
        view.findViewById(R.id.buttonHomeAction).setOnClickListener(v -> main().selectTab(R.id.navScienceHistory));
        view.findViewById(R.id.cardHomePhysics).setOnClickListener(v -> openChat(ScienceSubject.PHYSICS));
        view.findViewById(R.id.cardHomeChemistry).setOnClickListener(v -> openChat(ScienceSubject.CHEMISTRY));
        view.findViewById(R.id.cardHomeBiology).setOnClickListener(v -> openChat(ScienceSubject.BIOLOGY));
        view.findViewById(R.id.cardHomePeriodicTable).setOnClickListener(v -> startActivity(new Intent(requireContext(), PeriodicTableActivity.class)));
        view.findViewById(R.id.cardHomeCircuit).setOnClickListener(v -> startActivity(new Intent(requireContext(), CircuitDiagramActivity.class)));
        view.findViewById(R.id.cardHomeProgress).setOnClickListener(v -> startActivity(new Intent(requireContext(), ScienceProgressActivity.class)));
        updateExp(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) updateExp(view);
    }

    private void updateExp(@NonNull View view) {
        int userId = new com.brightpath.science.util.ScienceUserSession(requireContext()).getUserId();
        int totalExp;
        try {
            totalExp = ScienceModule.get(requireContext())
                    .quizAttemptRepository().getTotalExp(userId);
        } catch (Exception ignored) {
            totalExp = 0;
        }
        int level = ScienceLevelCalculator.getLevel(totalExp);
        String levelTitle = getString(ScienceLevelCalculator.getTitleResId(level));
        ((TextView) view.findViewById(R.id.tvHomeScienceExp))
                .setText(getString(R.string.science_level_format, level) + " • "
                        + levelTitle
                        + "\n" + getString(R.string.science_total_exp_format, totalExp));
    }

    private void openChat(ScienceSubject subject) {
        SciencePreferenceStore store = new SciencePreferenceStore(requireContext());
        store.save(subject, store.getLevelPosition(), store.getStylePosition());
        Intent intent = new Intent(requireContext(), ScienceActivity.class);
        intent.putExtra(ScienceActivity.EXTRA_SUBJECT, subject.getCode());
        startActivity(intent);
    }

    private ScienceMainActivity main() { return (ScienceMainActivity) requireActivity(); }
}
