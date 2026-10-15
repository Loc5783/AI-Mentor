package com.example.modulelaptrinh.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.modulelaptrinh.MainActivity;
import com.example.modulelaptrinh.R;
import com.example.modulelaptrinh.database.ProgrammingDbHelper;
import com.example.modulelaptrinh.utils.ProgrammingLevelCalculator;

public class ProgressDetailFragment extends Fragment {

    private TextView tvLevel, tvTitle, tvTotalExp, tvExpProgress, tvProgressPercent;
    private TextView tvQuizCount, tvAccuracy, tvTotalExpAll;
    private ImageView ivBack;
    private View progressBar;

    private ProgrammingDbHelper dbHelper;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress_detail, container, false);
        userId = ((MainActivity) requireActivity()).getCurrentUserId();

        dbHelper = new ProgrammingDbHelper(requireContext());

        initViews(view);
        loadProgressData();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        ivBack = view.findViewById(R.id.iv_back);
        tvLevel = view.findViewById(R.id.tv_level);
        tvTitle = view.findViewById(R.id.tv_title);
        tvTotalExp = view.findViewById(R.id.tv_total_exp);
        tvExpProgress = view.findViewById(R.id.tv_exp_progress);
        tvProgressPercent = view.findViewById(R.id.tv_progress_percent);
        progressBar = view.findViewById(R.id.progress_bar_exp);

        tvQuizCount = view.findViewById(R.id.tv_quiz_count);
        tvAccuracy = view.findViewById(R.id.tv_accuracy);
        tvTotalExpAll = view.findViewById(R.id.tv_total_exp_all);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).getSupportActionBar().setTitle("AI Mentor");
            }
        });
    }

    private void loadProgressData() {
        int totalExp = dbHelper.getTotalExpAllSubjects(userId);
        int totalExpAll = dbHelper.getTotalExpAllSubjects(userId);

        int level = ProgrammingLevelCalculator.getLevel(totalExp);
        String title = getString(ProgrammingLevelCalculator.getTitleResId(level));
        int progressPercent = ProgrammingLevelCalculator.getProgressPercent(totalExp);
        int expToNext = ProgrammingLevelCalculator.getExpToNextLevel(level);
        int expInLevel = ProgrammingLevelCalculator.getExpInsideCurrentLevel(totalExp);

        int quizCount = dbHelper.getQuizAttemptCount(userId, "Lập trình");
        double accuracy = dbHelper.getAverageAccuracy(userId, "Lập trình");

        tvLevel.setText(String.valueOf(level));
        tvTitle.setText(title);
        tvTotalExp.setText(getString(R.string.prog_exp_format, totalExp));
        tvExpProgress.setText(getString(R.string.prog_exp_detail_format, expInLevel, expToNext));
        tvProgressPercent.setText(getString(R.string.prog_percent_format, progressPercent));

        tvQuizCount.setText(String.valueOf(quizCount));
        tvAccuracy.setText(accuracy > 0 ? getString(R.string.prog_percent_format, (int)accuracy) : "0%");
        tvTotalExpAll.setText(String.valueOf(totalExpAll));
    }
}