package com.example.asm_final;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.*;
import com.example.asm_final.activities.*;
import com.example.asm_final.utils.ThemeHelper;
import com.google.android.material.textfield.TextInputLayout;
import java.util.*;
import java.util.function.Predicate;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ResearchUxTest {
  private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
  private final Context context = ins.getTargetContext();
  private String username;
  private int userId, originalUser;
  private boolean originalNight;
  private String originalLanguage;
  private Map<String, ?> calculatorState;
  private Activity foreground;

  private Activity resumed() {
    Collection<Activity> activities =
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
    if (foreground != null
        && activities.contains(foreground)
        && !foreground.isFinishing()
        && !foreground.isChangingConfigurations()) return foreground;
    if (foreground != null)
      for (Activity activity : activities) {
        if (activity.getClass() == foreground.getClass() && !activity.isFinishing()) {
          foreground = activity;
          return activity;
        }
      }
    for (Activity activity : activities) if (activity.hasWindowFocus()) return activity;
    return activities.iterator().next();
  }

  private void main(Runnable action) {
    ins.runOnMainSync(action);
    ins.waitForIdleSync();
  }

  private void start(Class<?> type) {
    foreground =
        ins.startActivitySync(
            new Intent(context, type)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("USERNAME", username));
    waitFor(a -> a == foreground && a.hasWindowFocus());
  }

  private void waitFor(Predicate<Activity> predicate) {
    long end = android.os.SystemClock.uptimeMillis() + 15000;
    while (android.os.SystemClock.uptimeMillis() < end) {
      boolean[] ready = {false};
      ins.runOnMainSync(
          () -> {
            Collection<Activity> a =
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            ready[0] = a.stream().anyMatch(predicate);
          });
      if (ready[0]) return;
      android.os.SystemClock.sleep(50);
    }
    fail("UI not ready");
  }

  @Before
  public void prepare() {
    originalUser = com.example.mentor.data.ActiveUser.get(context);
    originalNight = ThemeHelper.isDarkMode(context);
    originalLanguage = com.example.asm_final.utils.LocaleHelper.getLanguage(context);
    String testLanguage =
        InstrumentationRegistry.getArguments().getString("mentorLocale", originalLanguage);
    main(() -> com.example.asm_final.utils.LocaleHelper.setLocale(context, testLanguage));
    calculatorState = new HashMap<>(context.getSharedPreferences("math_calculator", 0).getAll());
    context.getSharedPreferences("math_calculator", 0).edit().clear().apply();
    try (com.example.mathmodule.databases.SqliteDbHelper helper =
        new com.example.mathmodule.databases.SqliteDbHelper(context)) {
      helper.getWritableDatabase();
    }

    username = "ux_" + System.nanoTime();
    AppServices services = ((AiMentorApplication) context.getApplicationContext()).services();
    assertTrue(
        services.registration().register(username, "Mentor01!2026", "ux@gmail.com", "0901234567")
            > 0);
    userId = services.accountLookup().userId(username);
    com.example.mentor.data.ActiveUser.set(context, userId);
  }

  @After
  public void cleanup() {
    main(
        () -> {
          for (Stage stage :
              new Stage[] {Stage.RESUMED, Stage.STARTED, Stage.STOPPED, Stage.CREATED})
            for (Activity a :
                new ArrayList<>(
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage)))
              a.finish();
          ThemeHelper.setDarkMode(context, originalNight);
          com.example.asm_final.utils.LocaleHelper.setLocale(context, originalLanguage);
        });
    if (userId > 0) {
      try (com.example.asm_final.activities.database.DatabaseHelper helper =
          new com.example.asm_final.activities.database.DatabaseHelper(context)) {
        android.database.sqlite.SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("DROP TRIGGER IF EXISTS ux_fail_note_insert");
        db.execSQL("DROP TRIGGER IF EXISTS ux_fail_note_update");
        db.delete("math_history", "user_id=?", new String[] {Integer.toString(userId)});
        try (android.database.Cursor tables =
            db.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name='history_quiz_attempts'",
                null)) {
          if (tables.moveToFirst())
            db.delete(
                "history_quiz_attempts", "user_id=?", new String[] {Integer.toString(userId)});
        }
        db.delete("users", "id=?", new String[] {Integer.toString(userId)});
      }
    }
    com.example.mentor.data.ActiveUser.set(context, originalUser);
    SharedPreferences.Editor editor =
        context.getSharedPreferences("math_calculator", 0).edit().clear();
    for (Map.Entry<String, ?> item : calculatorState.entrySet()) {
      Object value = item.getValue();
      if (value instanceof String) editor.putString(item.getKey(), (String) value);
      else if (value instanceof Boolean) editor.putBoolean(item.getKey(), (Boolean) value);
      else if (value instanceof Float) editor.putFloat(item.getKey(), (Float) value);
    }
    editor.apply();
  }

  private TextInputLayout fieldContainer(EditText field) {
    ViewParent parent = field.getParent();
    while (!(parent instanceof TextInputLayout)) parent = parent.getParent();
    return (TextInputLayout) parent;
  }

  private void capture(String name) {
    try {
      ins.getUiAutomation().waitForIdle(250, 3000);
      android.graphics.Bitmap bitmap = ins.getUiAutomation().takeScreenshot();
      java.io.File folder = context.getExternalFilesDir("verified-ui");
      folder.mkdirs();
      try (java.io.FileOutputStream out =
          new java.io.FileOutputStream(new java.io.File(folder, name + ".png"))) {
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
      } finally {
        bitmap.recycle();
      }
    } catch (Exception error) {
      throw new AssertionError(error);
    }
  }

  @Test
  public void loginErrorsAreInlineAndClearWithoutLosingDraft() {
    start(MainActivity.class);
    main(
        () -> {
          EditText name = resumed().findViewById(R.id.edtLoginUsername);
          assertNull(fieldContainer(name).getError());
          resumed().findViewById(R.id.btnLogin).performClick();
          assertNotNull(fieldContainer(name).getError());
          assertTrue(name.isFocused());
          name.setText("draft_user");
          assertNull(fieldContainer(name).getError());
          assertNotNull(
              fieldContainer((EditText) resumed().findViewById(R.id.edtLoginPassword)).getError());
        });
    capture("ux-login-validation");
  }

  @Test
  public void registrationShowsEmailGuidanceAndTargetsInvalidField() {
    start(RegisterActivity.class);
    main(
        () -> {
          EditText email = resumed().findViewById(R.id.edtRegisterEmail);
          assertNotNull(fieldContainer(email).getHelperText());
          assertNull(fieldContainer(email).getError());
          ((EditText) resumed().findViewById(R.id.edtRegisterUsername)).setText("draft_user");
          email.setText("learner@");
          ((com.google.android.material.checkbox.MaterialCheckBox)
                  resumed().findViewById(R.id.checkTerms))
              .setChecked(true);
          ((EditText) resumed().findViewById(R.id.edtRegisterPassword)).setText("Mentor01!2026");
          ((EditText) resumed().findViewById(R.id.edtRegisterConfirmPassword))
              .setText("Mentor01!2026");
          resumed().findViewById(R.id.btnRegister).performClick();
          assertNotNull(fieldContainer(email).getError());
          assertTrue(email.isFocused());
          assertEquals(
              "", ((TextView) resumed().findViewById(R.id.tvRegisterMessage)).getText().toString());
          assertEquals(
              "draft_user",
              ((EditText) resumed().findViewById(R.id.edtRegisterUsername)).getText().toString());
        });
    capture("ux-register-validation");
  }

  @Test
  public void calculatorResultStaysVisibleWhenKeypadScrolls() {
    start(com.example.mathmodule.MathDashboardActivity.class);
    main(() -> com.example.mathmodule.calculator.MathCalculator.show(resumed()));
    int entered = 0;
    String[] expectedExpression = {"6", "6×", "6×7", "42"};
    for (String key : new String[] {"6", "×", "7", "="}) {
      onView(withText(key))
          .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
          .perform(scrollTo(), click());
      assertEquals(
          "Calculator key " + key,
          expectedExpression[entered++],
          context.getSharedPreferences("math_calculator", 0).getString("expression", ""));
    }
    onView(withTagValue(is((Object) "mentor_calculator_keypad"))).perform(swipeUp());
    onView(withText("42"))
        .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
        .check(matches(isDisplayed()));
    capture("ux-calculator-pinned-result");
    onView(withText(com.example.mathmodule.R.string.math_calculator_collapse))
        .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
        .perform(click());
  }

  @Test
  public void offlineChatRetainsTypedQuestionInEachOnlineOnlySubject() {
    org.junit.Assume.assumeFalse(
        new com.example.mathmodule.api.MathGeminiApiClient().isConfigured());
    org.junit.Assume.assumeFalse(
        new com.example.historymodule.api.HistoryAiService().isConfigured());
    Class<?>[] types = {
      com.example.mathmodule.MathChatActivity.class,
      com.example.historymodule.HistoryChatActivity.class,
    };
    int[] inputs = {
      com.example.mathmodule.R.id.math_edt_question,
      com.example.historymodule.R.id.message_edit_text,
    };
    int[] sends = {
      com.example.mathmodule.R.id.math_btn_send, com.example.historymodule.R.id.send_btn,
    };
    int[] lists = {
      com.example.mathmodule.R.id.math_recycler_view, com.example.historymodule.R.id.recycler_view,
    };
    for (int index = 0; index < types.length; index++) {
      final int i = index;
      foreground =
          ins.startActivitySync(
              new Intent(context, types[i])
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  .putExtra("USER_ID", userId));
      waitFor(a -> a == foreground && a.hasWindowFocus());
      final int[] messageCount = {0};
      main(
          () ->
              messageCount[0] =
                  ((androidx.recyclerview.widget.RecyclerView) resumed().findViewById(lists[i]))
                      .getAdapter()
                      .getItemCount());
      onView(withId(inputs[i])).perform(click(), replaceText("Keep this question"));
      if (Boolean.parseBoolean(
          InstrumentationRegistry.getArguments().getString("mentorRequireIme", "false"))) {
        waitFor(
            a -> {
              androidx.core.view.WindowInsetsCompat insets =
                  androidx.core.view.ViewCompat.getRootWindowInsets(a.getWindow().getDecorView());
              return insets != null
                  && insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime());
            });
      }
      onView(withId(sends[i])).perform(click());
      main(
          () -> {
            EditText input = resumed().findViewById(inputs[i]);
            androidx.recyclerview.widget.RecyclerView list = resumed().findViewById(lists[i]);
            int count = messageCount[0];
            assertEquals("Keep this question", input.getText().toString());
            assertEquals(count, list.getAdapter().getItemCount());
            assertEquals(
                View.VISIBLE,
                resumed()
                    .findViewById(com.example.mentor.ui.R.id.mentor_ai_availability)
                    .getVisibility());
          });
      capture("ux-offline-chat-" + i);
      main(() -> resumed().finish());
    }
  }

  @Test
  public void calculatorRemainsUsableInLandscapeWithPinnedResult() {
    start(com.example.mathmodule.MathDashboardActivity.class);
    final Activity portrait = foreground;
    main(
        () ->
            resumed()
                .setRequestedOrientation(
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
    waitFor(
        a ->
            a != portrait
                && a instanceof com.example.mathmodule.MathDashboardActivity
                && a.getResources().getConfiguration().orientation
                    == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                && a.hasWindowFocus()
                && !a.isChangingConfigurations());
    main(() -> com.example.mathmodule.calculator.MathCalculator.show(resumed()));
    for (String key : new String[] {"6", "×", "7", "="})
      onView(withText(key))
          .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
          .perform(scrollTo(), click());
    onView(withText("42"))
        .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
        .check(matches(isDisplayed()));
    capture("ux-calculator-landscape");
    onView(withText(com.example.mathmodule.R.string.math_calculator_collapse))
        .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
        .perform(click());
  }

  @Test
  public void profileProgressKeepsStreakAndWeeklyXpOffOriginalDashboard() {
    String language = com.example.asm_final.utils.LocaleHelper.getLanguage(context);
    main(
        () -> {
          com.example.asm_final.utils.LocaleHelper.setLocale(
              context, InstrumentationRegistry.getArguments().getString("mentorLocale", "vi"));
          ThemeHelper.setDarkMode(
              context,
              Boolean.parseBoolean(
                  InstrumentationRegistry.getArguments().getString("mentorNight", "false")));
        });
    try {
      start(HomeActivity.class);
      main(
          () -> {
            assertNull(resumed().findViewById(R.id.card_learning_streak));
            ((HomeActivity) resumed()).navigateTo(R.id.nav_profile, false);
            ((AppCompatActivity) resumed())
                .getSupportFragmentManager()
                .executePendingTransactions();
            resumed().findViewById(R.id.button_learning_progress).performClick();
          });
      final View[] progress = {null};
      long until = android.os.SystemClock.uptimeMillis() + 15000;
      while (android.os.SystemClock.uptimeMillis() < until) {
        main(
            () -> {
              androidx.fragment.app.Fragment hub =
                  ((AppCompatActivity) resumed())
                      .getSupportFragmentManager()
                      .findFragmentById(R.id.fragment_container);
              androidx.fragment.app.DialogFragment dialog =
                  (androidx.fragment.app.DialogFragment)
                      hub.getChildFragmentManager().findFragmentByTag("learning_progress");
              if (dialog != null && dialog.getDialog() != null) {
                LinearLayout week = dialog.getDialog().findViewById(R.id.streak_week_days);
                if (week != null && week.getChildCount() == 7)
                  progress[0] = dialog.getDialog().getWindow().getDecorView();
              }
            });
        if (progress[0] != null) break;
        android.os.SystemClock.sleep(50);
      }
      assertNotNull(progress[0]);
      main(
          () -> {
            LinearLayout week = progress[0].findViewById(R.id.streak_week_days);
            for (int i = 0; i < 7; i++) {
              TextView day = (TextView) week.getChildAt(i);
              assertTrue(day.getWidth() > 0);
              assertTrue(day.getRight() <= week.getWidth());
              assertTrue(day.getLineCount() <= 2);
              assertNotNull(day.getContentDescription());
            }
            assertTrue(
                ((TextView) progress[0].findViewById(R.id.weekly_quiz_xp))
                    .getText()
                    .toString()
                    .contains("0"));
          });
      main(
          () -> {
            android.widget.ProgressBar milestone =
                progress[0].findViewById(R.id.streak_milestone_progress);
            assertEquals(3, milestone.getMax());
            assertEquals(0, milestone.getProgress());
          });
      capture("ux-profile-streak-weekly-progress");
      onView(withId(R.id.button_streak_quiz))
          .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
          .perform(scrollTo(), click());
      waitFor(
          a ->
              a instanceof HomeActivity
                  && ((HomeActivity) a)
                          .getSupportFragmentManager()
                          .findFragmentById(R.id.fragment_container)
                      instanceof com.example.asm_final.activities.fragments.LessonHubFragment);
    } finally {
      main(() -> com.example.asm_final.utils.LocaleHelper.setLocale(context, language));
    }
  }
}
