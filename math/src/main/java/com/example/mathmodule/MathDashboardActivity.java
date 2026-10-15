package com.example.mathmodule;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.example.mathmodule.fragments.MathFormulaFragment;
import com.example.mathmodule.fragments.MathHistoryFragment;
import com.example.mathmodule.fragments.MathQuizFragment;
import com.example.mathmodule.fragments.MathSolveFragment;

public class MathDashboardActivity extends AppCompatActivity {
    private static final String STATE_TAB = "math_current_tab";

    private int currentTab = 0;
    private LinearLayout[] navItems;
    private ImageView[] navIcons;
    private TextView[] navLabels;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_math_dashboard);

        navItems = new LinearLayout[]{
                findViewById(R.id.nav_home),
                findViewById(R.id.nav_history),
                findViewById(R.id.nav_formula),
                findViewById(R.id.nav_quiz)
        };
        navIcons = new ImageView[]{
                findViewById(R.id.icon_nav_home),
                findViewById(R.id.icon_nav_history),
                findViewById(R.id.icon_nav_formula),
                findViewById(R.id.icon_nav_quiz)
        };
        navLabels = new TextView[]{
                findViewById(R.id.text_nav_home),
                findViewById(R.id.text_nav_history),
                findViewById(R.id.text_nav_formula),
                findViewById(R.id.text_nav_quiz)
        };

        currentTab = savedInstanceState == null ? 0 : savedInstanceState.getInt(STATE_TAB, 0);
        for (int i = 0; i < navItems.length; i++) {
            final int tab = i;
            navItems[i].setOnClickListener(v -> selectTab(tab));
        }

        updateNavigationHighlight(currentTab);
        if (savedInstanceState == null
                || getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            loadFragment(createFragment(currentTab));
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentTab != 0) {
                    selectTab(0);
                } else {
                    finish();
                }
            }
        });
    }

    private void selectTab(int tab) {
        if (tab < 0 || tab >= navItems.length) tab = 0;
        if (currentTab == tab
                && getSupportFragmentManager().findFragmentById(R.id.fragment_container) != null) {
            updateNavigationHighlight(tab);
            return;
        }
        currentTab = tab;
        updateNavigationHighlight(tab);
        loadFragment(createFragment(tab));
    }

    private Fragment createFragment(int tab) {
        switch (tab) {
            case 1: return new MathHistoryFragment();
            case 2: return new MathFormulaFragment();
            case 3: return new MathQuizFragment();
            default: return new MathSolveFragment();
        }
    }

    private void updateNavigationHighlight(int activeTab) {
        int[] activeColorRes = {
                R.color.math_nav_solve,
                R.color.math_nav_history,
                R.color.math_nav_notes,
                R.color.math_nav_quiz
        };
        int inactiveColor = ContextCompat.getColor(this, R.color.math_inactive);

        for (int i = 0; i < navItems.length; i++) {
            int color = i == activeTab ? ContextCompat.getColor(this, activeColorRes[i]) : inactiveColor;
            ImageViewCompat.setImageTintList(navIcons[i],
                    android.content.res.ColorStateList.valueOf(color));
            navLabels[i].setTextColor(color);
            navLabels[i].setTypeface(null, i == activeTab ? Typeface.BOLD : Typeface.NORMAL);
            navItems[i].setSelected(i == activeTab);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_TAB, currentTab);
        super.onSaveInstanceState(outState);
    }
}
