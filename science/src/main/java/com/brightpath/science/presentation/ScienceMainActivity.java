package com.brightpath.science.presentation;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.brightpath.science.R;
import com.brightpath.science.presentation.fragment.ScienceFormulaFragment;
import com.brightpath.science.presentation.fragment.ScienceHistoryFragment;
import com.brightpath.science.presentation.fragment.ScienceHomeFragment;
import com.brightpath.science.presentation.fragment.ScienceQuizFragment;
import com.brightpath.science.presentation.fragment.ScienceSolveFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public final class ScienceMainActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_TAB =
            "com.brightpath.science.extra.SELECTED_TAB";

    private static final String STATE_SELECTED_ITEM =
            "science_selected_item";

    private BottomNavigationView bottomNavigation;
    private int selectedItemId = R.id.navScienceHome;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_science_main);

        bottomNavigation = findViewById(R.id.scienceBottomNavigation);
        applyBottomNavigationInsets();

        if (savedInstanceState != null) {
            selectedItemId = savedInstanceState.getInt(
                    STATE_SELECTED_ITEM,
                    R.id.navScienceHome
            );
        } else {
            selectedItemId = readRequestedTab(getIntent());
        }

        setupBottomNavigation();

        bottomNavigation.setSelectedItemId(selectedItemId);

        if (getSupportFragmentManager()
                .findFragmentById(R.id.scienceFragmentContainer) == null) {
            navigateTo(selectedItemId);
        } else {
            applyNavigationTint(selectedItemId);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        int requestedTab = readRequestedTab(intent);
        bottomNavigation.setSelectedItemId(requestedTab);
    }

    private void applyBottomNavigationInsets() {
        final int baseHeight = getResources().getDimensionPixelSize(
                R.dimen.science_bottom_navigation_height);
        final int baseBottomPadding = bottomNavigation.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (view, insets) -> {
            int navigationInset = insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()).bottom;
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    baseBottomPadding + navigationInset
            );
            android.view.ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params.height != baseHeight + navigationInset) {
                params.height = baseHeight + navigationInset;
                view.setLayoutParams(params);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(bottomNavigation);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(
                item -> navigateTo(item.getItemId())
        );

        bottomNavigation.setOnItemReselectedListener(item -> {
            // Không tạo Fragment mới khi người dùng bấm lại tab hiện tại.
        });
    }

    private int readRequestedTab(@Nullable Intent intent) {
        if (intent == null) {
            return R.id.navScienceHome;
        }

        int requested = intent.getIntExtra(
                EXTRA_SELECTED_TAB,
                R.id.navScienceHome
        );

        return isValidTab(requested)
                ? requested
                : R.id.navScienceHome;
    }

    private boolean isValidTab(@IdRes int itemId) {
        return itemId == R.id.navScienceHome
                || itemId == R.id.navScienceSolve
                || itemId == R.id.navScienceHistory
                || itemId == R.id.navScienceFormula
                || itemId == R.id.navScienceQuiz;
    }

    private boolean navigateTo(@IdRes int itemId) {
        Fragment fragment = createFragment(itemId);
        if (fragment == null) {
            return false;
        }

        selectedItemId = itemId;
        applyNavigationTint(itemId);

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(
                        R.id.scienceFragmentContainer,
                        fragment
                )
                .commit();

        return true;
    }

    @Nullable
    private Fragment createFragment(@IdRes int itemId) {
        if (itemId == R.id.navScienceHome) {
            return new ScienceHomeFragment();
        }
        if (itemId == R.id.navScienceSolve) {
            return new ScienceSolveFragment();
        }
        if (itemId == R.id.navScienceHistory) {
            return new ScienceHistoryFragment();
        }
        if (itemId == R.id.navScienceFormula) {
            return new ScienceFormulaFragment();
        }
        if (itemId == R.id.navScienceQuiz) {
            return new ScienceQuizFragment();
        }
        return null;
    }

    private void applyNavigationTint(@IdRes int itemId) {
        int colorSelector = R.color.science_bottom_nav_home_color;

        if (itemId == R.id.navScienceSolve) {
            colorSelector = R.color.science_bottom_nav_solve_color;
        } else if (itemId == R.id.navScienceHistory) {
            colorSelector = R.color.science_bottom_nav_history_color;
        } else if (itemId == R.id.navScienceFormula) {
            colorSelector = R.color.science_bottom_nav_formula_color;
        } else if (itemId == R.id.navScienceQuiz) {
            colorSelector = R.color.science_bottom_nav_quiz_color;
        }

        bottomNavigation.setItemIconTintList(
                ContextCompat.getColorStateList(this, colorSelector)
        );
        bottomNavigation.setItemTextColor(
                ContextCompat.getColorStateList(this, colorSelector)
        );
    }

    public void selectTab(@IdRes int itemId) {
        if (isValidTab(itemId)) {
            bottomNavigation.setSelectedItemId(itemId);
        }
    }

    @Override
    public void onBackPressed() {
        if (selectedItemId != R.id.navScienceHome) {
            bottomNavigation.setSelectedItemId(R.id.navScienceHome);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(
                STATE_SELECTED_ITEM,
                selectedItemId
        );
        super.onSaveInstanceState(outState);
    }
}
