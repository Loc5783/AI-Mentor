package com.example.modulelaptrinh;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.modulelaptrinh.fragments.ChatHistoryFragment;
import com.example.modulelaptrinh.fragments.HomeFragment;
import com.example.modulelaptrinh.fragments.NoteFragment;
import com.example.modulelaptrinh.fragments.QuizFragment;
import com.example.modulelaptrinh.fragments.QuizSelectionFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "programming_user_id";

    private BottomNavigationView bottomNav;
    private Toolbar toolbar;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_programming_main);

        currentUserId = Math.max(0, getIntent().getIntExtra(EXTRA_USER_ID, 0));

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        bottomNav = findViewById(R.id.bottom_nav);

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            showRootFragment(new HomeFragment(), getString(R.string.prog_tab_home));
        } else {
            updateTitleForSelectedItem(bottomNav.getSelectedItemId());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return showRootFragment(new HomeFragment(), getString(R.string.prog_tab_home));
            }
            if (itemId == R.id.nav_history) {
                return showRootFragment(new ChatHistoryFragment(), getString(R.string.prog_tab_history));
            }
            if (itemId == R.id.nav_notes) {
                return showRootFragment(new NoteFragment(), getString(R.string.prog_tab_notes));
            }
            if (itemId == R.id.nav_quiz) {
                return showRootFragment(new QuizSelectionFragment(), getString(R.string.prog_tab_quiz));
            }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    bottomNav.setSelectedItemId(R.id.nav_quiz);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(R.string.prog_tab_quiz);
                    }
                    return;
                }
                finish();
            }
        });
    }

    public int getCurrentUserId() {
        return currentUserId;
    }

    private boolean showRootFragment(Fragment fragment, String title) {
        if (fragment == null) {
            return false;
        }

        getSupportFragmentManager().popBackStackImmediate();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
        return true;
    }

    public void openQuiz(String subject, String topic, String level, int questionCount) {
        QuizFragment fragment = QuizFragment.newInstance(subject, topic, level, questionCount);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack("programming_quiz")
                .commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.quiz_title_format, topic));
        }
    }

    private void updateTitleForSelectedItem(int itemId) {
        if (getSupportActionBar() == null) {
            return;
        }
        if (itemId == R.id.nav_history) {
            getSupportActionBar().setTitle(R.string.prog_tab_history);
        } else if (itemId == R.id.nav_notes) {
            getSupportActionBar().setTitle(R.string.prog_tab_notes);
        } else if (itemId == R.id.nav_quiz) {
            getSupportActionBar().setTitle(R.string.prog_tab_quiz);
        } else {
            getSupportActionBar().setTitle(R.string.prog_tab_home);
        }
    }
}
