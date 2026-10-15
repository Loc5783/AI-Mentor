package com.example.historymodule.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.historymodule.HistoryQuizActivity;
import com.example.historymodule.R;
import com.example.historymodule.adapter.HistoryHistoryAdapter;
import com.example.historymodule.databases.HistoryHistoryItem;
import com.example.historymodule.databases.SqliteDbHelper;
import com.example.historymodule.model.HistoryLevelCalculator;
import com.example.historymodule.util.HistoryUserSession;

import java.util.ArrayList;
import java.util.List;

public final class HistoryQuizFragment extends Fragment {
    private String[] getEducationLevels() {
        return new String[]{
                getString(R.string.history_quiz_edu_ms),
                getString(R.string.history_quiz_edu_hs),
                getString(R.string.history_quiz_edu_uni)
        };
    }

    private String[] getRawEducationLevels() {
        return new String[]{"Trung học cơ sở", "Trung học phổ thông", "Đại học"};
    }

    private final List<HistoryHistoryItem> attempts = new ArrayList<>();
    private HistoryHistoryAdapter adapter;
    private SqliteDbHelper dbHelper;
    private TextView levelView;
    private TextView totalExpView;
    private TextView progressTextView;
    private TextView emptyView;
    private ProgressBar progressBar;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.history_fragment_quiz, container, false);
        userId = HistoryUserSession.getUserId(requireContext());
        dbHelper = new SqliteDbHelper(requireContext());

        levelView = view.findViewById(R.id.tvHistoryQuizLevel);
        totalExpView = view.findViewById(R.id.tvHistoryQuizTotalExp);
        progressTextView = view.findViewById(R.id.tvHistoryQuizProgress);
        progressBar = view.findViewById(R.id.pbHistoryQuizLevel);
        emptyView = view.findViewById(R.id.tvHistoryQuizEmpty);

        view.findViewById(R.id.item_quiz_topic_vn)
                .setOnClickListener(v -> chooseEducationLevel(getString(R.string.history_quiz_topic_vn)));
        view.findViewById(R.id.item_quiz_topic_foreign)
                .setOnClickListener(v -> chooseEducationLevel(getString(R.string.history_quiz_topic_foreign)));
        view.findViewById(R.id.item_quiz_topic_mixed)
                .setOnClickListener(v -> chooseEducationLevel(getString(R.string.history_quiz_topic_mixed)));

        RecyclerView recycler = view.findViewById(R.id.recycler_ai_quizzes);
        adapter = new HistoryHistoryAdapter(attempts);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        recycler.setNestedScrollingEnabled(false);

        refresh();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dbHelper != null && adapter != null) refresh();
    }

    private void chooseEducationLevel(String topic) {
        String[] levels = getEducationLevels();
        String[] rawLevels = getRawEducationLevels();
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.history_quiz_edu_title)
                .setItems(levels, (dialog, which) ->
                        openQuiz(topic, rawLevels[which]))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openQuiz(String topic, String educationLevel) {
        Intent intent = new Intent(requireContext(), HistoryQuizActivity.class);
        intent.putExtra(HistoryQuizActivity.EXTRA_TOPIC, topic);
        intent.putExtra(HistoryQuizActivity.EXTRA_EDUCATION_LEVEL, educationLevel);
        startActivity(intent);
    }

    private void refresh() {
        int totalExp = dbHelper.getTotalQuizExp(userId);
        int level = HistoryLevelCalculator.getLevel(totalExp);
        levelView.setText(getString(R.string.history_quiz_level_format, level, getString(HistoryLevelCalculator.getTitleResId(level))));
        totalExpView.setText(getString(R.string.history_quiz_exp_total, totalExp));
        progressBar.setProgress(HistoryLevelCalculator.getProgressPercent(totalExp));
        if (level >= HistoryLevelCalculator.MAX_LEVEL) {
            progressTextView.setText(R.string.history_quiz_level_max);
        } else {
            int current = HistoryLevelCalculator.getExpInsideCurrentLevel(totalExp);
            int required = HistoryLevelCalculator.getExpToNextLevel(level);
            progressTextView.setText(getString(R.string.history_quiz_exp_detail, current, required, Math.max(0, required - current)));
        }

        attempts.clear();
        attempts.addAll(dbHelper.loadQuizAttempts(userId, 10));
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(attempts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        if (dbHelper != null) dbHelper.close();
        dbHelper = null;
        adapter = null;
        levelView = null;
        totalExpView = null;
        progressTextView = null;
        progressBar = null;
        emptyView = null;
        super.onDestroyView();
    }
}
