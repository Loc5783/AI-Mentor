package com.example.historymodule;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.historymodule.fragments.HistoryFormulaFragment;
import com.example.historymodule.fragments.HistoryHistoryFragment;
import com.example.historymodule.fragments.HistoryQuizFragment;
import com.example.historymodule.fragments.HistorySolveFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public final class HistoryDashboardActivity extends AppCompatActivity {
    private BottomNavigationView navigation;
    private TextView titleView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_activity_dashboard);

        titleView = findViewById(R.id.tvHistoryDashboardTitle);
        navigation = findViewById(R.id.bottom_navigation);
        findViewById(R.id.btnHistoryDashboardBack).setOnClickListener(v -> finish());

        navigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_solver) {
                showPage(new HistorySolveFragment(), getString(R.string.history_tab_solver));
            } else if (id == R.id.nav_history) {
                showPage(new HistoryHistoryFragment(), getString(R.string.history_tab_history));
            } else if (id == R.id.nav_events) {
                showPage(new HistoryFormulaFragment(), getString(R.string.history_tab_notes));
            } else if (id == R.id.nav_quiz) {
                showPage(new HistoryQuizFragment(), getString(R.string.history_tab_quiz));
            } else {
                return false;
            }
            return true;
        });

        if (savedInstanceState == null) {
            navigation.setSelectedItemId(R.id.nav_solver);
        } else {
            updateTitle(navigation.getSelectedItemId());
        }
    }

    private void showPage(Fragment fragment, String title) {
        titleView.setText(title);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void updateTitle(int selectedId) {
        if (selectedId == R.id.nav_history) titleView.setText(R.string.history_tab_history);
        else if (selectedId == R.id.nav_events) titleView.setText(R.string.history_tab_notes);
        else if (selectedId == R.id.nav_quiz) titleView.setText(R.string.history_tab_quiz);
        else titleView.setText(R.string.history_tab_solver);
    }
}
