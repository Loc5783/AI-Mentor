package com.example.asm_final;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.asm_final.activities.HomeActivity;
import com.example.asm_final.activities.MainActivity;
import com.example.asm_final.utils.DebugAccounts;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CanonicalSmokeTest {
  @org.junit.Rule public final LocalBackendIsolation localBackend = new LocalBackendIsolation();

  @org.junit.Rule
  public final androidx.compose.ui.test.junit4.ComposeTestRule compose = ComposeProbe.rule();

  private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

  private Activity resumed() {
    return ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED)
        .iterator()
        .next();
  }

  private void main(Runnable action) {
    instrumentation.runOnMainSync(action);
    instrumentation.waitForIdleSync();
  }

  private void start(Class<?> type) {
    instrumentation.startActivitySync(
        new Intent(instrumentation.getTargetContext(), type)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  }

  private void awaitActivity(String expected) {
    long deadline = android.os.SystemClock.uptimeMillis() + 10000;
    while (android.os.SystemClock.uptimeMillis() < deadline) {
      final boolean[] ready = {false};
      instrumentation.runOnMainSync(
          () -> {
            for (Activity activity :
                ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)) {
              if (expected == null || expected.equals(activity.getClass().getName()))
                ready[0] = true;
            }
          });
      if (ready[0]) return;
      android.os.SystemClock.sleep(50);
    }
    fail("Activity did not resume: " + expected);
  }

  private void capture(String name) throws Exception {
    awaitActivity(null);
    if (android.os.Build.VERSION.SDK_INT >= 29) {
      CountDownLatch drawn = new CountDownLatch(1);
      main(
          () -> {
            View decor = resumed().getWindow().getDecorView();
            decor.getViewTreeObserver().registerFrameCommitCallback(drawn::countDown);
            decor.invalidate();
          });
      assertTrue(drawn.await(5, TimeUnit.SECONDS));
    }
    instrumentation.getUiAutomation().waitForIdle(250, 3000);
    Bitmap screen = instrumentation.getUiAutomation().takeScreenshot();
    if (screen == null) return;
    File folder = instrumentation.getTargetContext().getExternalFilesDir("verified-ui");
    try (FileOutputStream output = new FileOutputStream(new File(folder, name + ".png"))) {
      screen.compress(Bitmap.CompressFormat.PNG, 100, output);
    } finally {
      screen.recycle();
    }
  }

  @org.junit.Before
  public void selectTheme() {
    boolean night =
        Boolean.parseBoolean(
            InstrumentationRegistry.getArguments().getString("mentorNight", "false"));
    main(
        () ->
            com.example.asm_final.utils.ThemeHelper.setDarkMode(
                instrumentation.getTargetContext(), night));
  }

  private BottomNavigationView navigation(View view) {
    if (view instanceof BottomNavigationView) return (BottomNavigationView) view;
    if (view instanceof android.view.ViewGroup) {
      android.view.ViewGroup group = (android.view.ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        BottomNavigationView found = navigation(group.getChildAt(i));
        if (found != null) return found;
      }
    }
    return null;
  }

  private void checkSafeBounds(View navigation) {
    View content = resumed().findViewById(android.R.id.content);
    int[] origin = new int[2], position = new int[2];
    content.getLocationOnScreen(origin);
    navigation.getLocationOnScreen(position);
    assertTrue(
        "Navigation must remain above the system controls",
        position[1] + navigation.getHeight()
            <= origin[1] + content.getHeight() - content.getPaddingBottom());
    assertTrue("Navigation must be visible", navigation.getHeight() > 0);
  }

  @Test
  public void loginRegistrationAndFiveMainTabsOpen() throws Exception {
    DebugAccounts.seed(instrumentation.getTargetContext());
    start(MainActivity.class);
    main(() -> resumed().findViewById(R.id.tvGoToRegister).performClick());
    awaitActivity("com.example.asm_final.activities.RegisterActivity");
    main(() -> resumed().findViewById(R.id.tvGoToLogin).performClick());
    awaitActivity(MainActivity.class.getName());
    main(
        () -> {
          ((EditText) resumed().findViewById(R.id.edtLoginUsername)).setText("student 01");
          ((EditText) resumed().findViewById(R.id.edtLoginPassword)).setText("123123123");
          resumed().findViewById(R.id.btnLogin).performClick();
        });
    awaitActivity(HomeActivity.class.getName());
    for (int tab :
        new int[] {
          R.id.nav_home, R.id.nav_chat, R.id.nav_category, R.id.nav_competition, R.id.nav_profile
        }) {
      main(
          () -> {
            ((HomeActivity) resumed()).navigateTo(tab, false);
            ((AppCompatActivity) resumed())
                .getSupportFragmentManager()
                .executePendingTransactions();
          });
      ComposeProbe.text(
          compose, instrumentation.getTargetContext().getString(R.string.design_tab_home));
      ComposeProbe.text(
          compose, instrumentation.getTargetContext().getString(R.string.nav_profile_label));
    }
    main(() -> ((HomeActivity) resumed()).logout());
    awaitActivity(MainActivity.class.getName());
  }

  @Test
  public void systemInsetsDoNotAccumulateOrDoubleCountKeyboard() {
    start(MainActivity.class);
    try {
      main(
          () -> {
            View content = resumed().findViewById(android.R.id.content);
            WindowInsetsCompat bars =
                new WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 48))
                    .build();
            ViewCompat.dispatchApplyWindowInsets(content, bars);
            ViewCompat.dispatchApplyWindowInsets(content, bars);
            assertEquals(24, content.getPaddingTop());
            assertEquals(48, content.getPaddingBottom());
            WindowInsetsCompat keyboard =
                new WindowInsetsCompat.Builder(bars)
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 320))
                    .build();
            ViewCompat.dispatchApplyWindowInsets(content, keyboard);
            assertEquals(320, content.getPaddingBottom());
          });
    } finally {
      main(() -> resumed().finish());
    }
  }

  private void awaitEnabled(int id) {
    long limit = android.os.SystemClock.uptimeMillis() + 10000;
    while (android.os.SystemClock.uptimeMillis() < limit) {
      final boolean[] ready = {false};
      main(
          () -> {
            View view = resumed().findViewById(id);
            ready[0] = view != null && view.isEnabled();
          });
      if (ready[0]) return;
      android.os.SystemClock.sleep(50);
    }
    fail("Control not ready: " + id);
  }

  @Test
  public void registerLoginUpdatePasswordAndLogout() {
    String username = "solid_" + System.nanoTime() + "@school.edu";
    try {
      start(MainActivity.class);
      main(() -> resumed().findViewById(R.id.tvGoToRegister).performClick());
      awaitActivity("com.example.asm_final.activities.RegisterActivity");
      main(
          () -> {
            ((EditText) resumed().findViewById(R.id.edtRegisterUsername)).setText("SOLID learner");
            ((EditText) resumed().findViewById(R.id.edtRegisterEmail)).setText(username);
            ((com.google.android.material.checkbox.MaterialCheckBox)
                    resumed().findViewById(R.id.checkTerms))
                .setChecked(true);
            ((EditText) resumed().findViewById(R.id.edtRegisterPassword)).setText("Solid01!2026");
            ((EditText) resumed().findViewById(R.id.edtRegisterConfirmPassword))
                .setText("Solid01!2026");
            resumed().findViewById(R.id.btnRegister).performClick();
          });
      awaitActivity(com.example.asm_final.activities.OnboardingActivity.class.getName());
      awaitEnabled(R.id.onboarding_next);
      main(() -> resumed().findViewById(R.id.onboarding_skip).performClick());
      awaitActivity(com.example.asm_final.activities.ProfileSetupActivity.class.getName());
      awaitEnabled(R.id.profile_setup_save);
      main(
          () -> {
            resumed().findViewById(R.id.grade_10).performClick();
            resumed().findViewById(R.id.interest_math).performClick();
            com.google.android.material.textfield.MaterialAutoCompleteTextView goal =
                resumed().findViewById(R.id.profile_goal_choice);
            goal.setText(resumed().getResources().getStringArray(R.array.learner_goals)[0], false);
            goal.getOnItemClickListener().onItemClick(null, null, 0, 0);
            resumed().findViewById(R.id.profile_setup_save).performClick();
          });
      awaitActivity(HomeActivity.class.getName());
      main(() -> ((HomeActivity) resumed()).logout());
      awaitActivity(MainActivity.class.getName());
      main(
          () -> {
            ((EditText) resumed().findViewById(R.id.edtLoginUsername)).setText(username);
            ((EditText) resumed().findViewById(R.id.edtLoginPassword)).setText("Solid01!2026");
            resumed().findViewById(R.id.btnLogin).performClick();
          });
      awaitActivity(HomeActivity.class.getName());
      main(
          () -> {
            ((HomeActivity) resumed()).navigateTo(R.id.nav_edit_profile, false);
            ((AppCompatActivity) resumed())
                .getSupportFragmentManager()
                .executePendingTransactions();
          });
      // Advance the Compose navigation transition before polling retained native Views.
      compose.waitForIdle();
      awaitEnabled(R.id.btnUpdateProfile);
      main(
          () -> {
            ((EditText) resumed().findViewById(R.id.edtProfileFullName))
                .setText("SOLID regression");
            ((EditText) resumed().findViewById(R.id.edtProfilePassword)).setText(" Solid02!2026 ");
            resumed().findViewById(R.id.btnUpdateProfile).performClick();
          });
      awaitEnabled(R.id.btnUpdateProfile);
      com.example.asm_final.accounts.LocalAccountRepository accounts =
          new com.example.asm_final.accounts.LocalAccountRepository(
              instrumentation.getTargetContext());
      assertTrue(accounts.authenticate(username, " Solid02!2026 "));
      assertFalse(accounts.authenticate(username, "Solid01!2026"));
      assertEquals(
          "SOLID regression",
          new com.example.asm_final.accounts.LocalUserProfileRepository(
                  instrumentation.getTargetContext())
              .load(username)
              .fullName);
      main(() -> ((HomeActivity) resumed()).logout());
      awaitActivity(MainActivity.class.getName());
      main(() -> resumed().finish());
    } finally {
      try (com.example.asm_final.activities.database.DatabaseHelper db =
          new com.example.asm_final.activities.database.DatabaseHelper(
              instrumentation.getTargetContext())) {
        db.getWritableDatabase().delete("users", "username=?", new String[] {username});
      }
    }
  }
}
