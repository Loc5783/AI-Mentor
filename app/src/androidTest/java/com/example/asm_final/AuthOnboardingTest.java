package com.example.asm_final;

import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.*;
import com.example.asm_final.accounts.*;
import com.example.asm_final.activities.*;
import com.example.asm_final.activities.database.DatabaseHelper;
import com.example.asm_final.onboarding.*;
import com.example.asm_final.utils.*;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import java.util.*;
import java.util.function.Predicate;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AuthOnboardingTest {
  @org.junit.Rule public final LocalBackendIsolation localBackend = new LocalBackendIsolation();
  private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
  private final Context context = ins.getTargetContext();
  private AppServices services;
  private String email;
  private int originalUser;
  private String language;
  private boolean night;
  private Map<String, ?> session;
  private static final String PASSWORD = "Learner01!2026";

  private void main(Runnable task) {
    ins.runOnMainSync(task);
    ins.waitForIdleSync();
  }

  private Activity current() {
    Collection<Activity> values =
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
    for (Activity a : values) if (a.hasWindowFocus()) return a;
    return values.isEmpty() ? null : values.iterator().next();
  }

  private void waitFor(Predicate<Activity> check) {
    long until = android.os.SystemClock.uptimeMillis() + 20000;
    while (android.os.SystemClock.uptimeMillis() < until) {
      boolean[] done = {false};
      main(
          () -> {
            Activity a = current();
            done[0] = a != null && check.test(a);
          });
      if (done[0]) return;
      android.os.SystemClock.sleep(50);
    }
    fail("Auth flow did not reach expected state");
  }

  private void start(Class<?> type, String username) {
    ins.startActivitySync(
        new Intent(context, type)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("USERNAME", username));
  }

  private void await(Class<?> type) {
    waitFor(a -> type.isInstance(a) && a.hasWindowFocus());
  }

  private void edit(int id, String text) {
    ((EditText) current().findViewById(id)).setText(text);
  }

  private void click(int id) {
    current().findViewById(id).performClick();
  }

  private void capture(String name) throws Exception {
    ins.getUiAutomation().waitForIdle(250, 3000);
    android.graphics.Bitmap bitmap = ins.getUiAutomation().takeScreenshot();
    assertNotNull("Screenshot unavailable", bitmap);
    java.io.File dir = context.getExternalFilesDir("auth-ui");
    assertNotNull(dir);
    dir.mkdirs();
    try (java.io.FileOutputStream out =
        new java.io.FileOutputStream(new java.io.File(dir, name + ".png"))) {
      bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    } finally {
      bitmap.recycle();
    }
  }

  @Before
  public void prepare() {
    services = ((AiMentorApplication) context.getApplicationContext()).services();
    email = "auth_" + System.nanoTime() + "@school.edu";
    originalUser = com.example.mentor.data.ActiveUser.get(context);
    language = LocaleHelper.getLanguage(context);
    night = ThemeHelper.isDarkMode(context);
    session = new HashMap<>(context.getSharedPreferences("auth_session", 0).getAll());
    AuthSession.clear(context);
    main(
        () -> {
          LocaleHelper.setLocale(context, "vi");
          ThemeHelper.setDarkMode(context, false);
        });
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
        });
    try (DatabaseHelper h = new DatabaseHelper(context)) {
      h.getWritableDatabase()
          .delete("users", "email=? OR email=?", new String[] {email, "second_" + email});
    }
    AuthSession.clear(context);
    android.content.SharedPreferences.Editor editor =
        context.getSharedPreferences("auth_session", 0).edit().clear();
    for (Map.Entry<String, ?> e : session.entrySet()) {
      if (e.getValue() instanceof Boolean) editor.putBoolean(e.getKey(), (Boolean) e.getValue());
      if (e.getValue() instanceof Integer) editor.putInt(e.getKey(), (Integer) e.getValue());
      if (e.getValue() instanceof String) editor.putString(e.getKey(), (String) e.getValue());
    }
    editor.apply();
    com.example.mentor.data.ActiveUser.set(context, originalUser);
    main(
        () -> {
          ThemeHelper.setDarkMode(context, night);
          LocaleHelper.setLocale(context, language);
        });
  }

  private int learner() {
    long id = services.learnerRegistration().registerLearner("Người học thử", email, PASSWORD);
    assertTrue(id > 0);
    return (int) id;
  }

  private void login(boolean remember) {
    boolean[] alreadyOpen = {false};
    main(() -> alreadyOpen[0] = current() instanceof MainActivity);
    if (!alreadyOpen[0]) start(MainActivity.class, "");
    await(MainActivity.class);
    main(
        () -> {
          edit(R.id.edtLoginUsername, email);
          edit(R.id.edtLoginPassword, PASSWORD);
          ((com.google.android.material.checkbox.MaterialCheckBox)
                  current().findViewById(R.id.checkRemember))
              .setChecked(remember);
          click(R.id.btnLogin);
        });
  }

  private void awaitIntro(int page) {
    waitFor(
        a ->
            a instanceof OnboardingActivity
                && a.findViewById(R.id.onboarding_next).isEnabled()
                && ((TextView) a.findViewById(R.id.onboarding_step))
                    .getText()
                    .toString()
                    .equals(a.getString(R.string.onboard_step, page + 1)));
  }

  private void chooseProfile() {
    waitFor(
        a ->
            a instanceof ProfileSetupActivity
                && a.findViewById(R.id.profile_setup_save).isEnabled()
                && !((EditText) a.findViewById(R.id.profile_display_name))
                    .getText()
                    .toString()
                    .isEmpty());
    main(
        () -> {
          click(R.id.grade_11);
          click(R.id.interest_math);
          click(R.id.interest_biology);
          MaterialAutoCompleteTextView goals = current().findViewById(R.id.profile_goal_choice);
          goals.setText(current().getResources().getStringArray(R.array.learner_goals)[3], false);
          goals.getOnItemClickListener().onItemClick(null, null, 3, 3);
        });
  }

  @Test
  public void newRegistrationRequiresConsentAndStartsUnverifiedLocalOnboarding() throws Exception {
    start(RegisterActivity.class, "");
    await(RegisterActivity.class);
    main(
        () -> {
          edit(R.id.edtRegisterUsername, "Học sinh thử");
          edit(R.id.edtRegisterEmail, email);
          edit(R.id.edtRegisterPassword, PASSWORD);
          edit(R.id.edtRegisterConfirmPassword, PASSWORD);
          edit(R.id.edtRegisterPassword, "Different01!2026");
          assertNotNull(
              AuthFields.container(
                      (EditText) current().findViewById(R.id.edtRegisterConfirmPassword))
                  .getError());
          edit(R.id.edtRegisterPassword, PASSWORD);
          assertNull(
              AuthFields.container(
                      (EditText) current().findViewById(R.id.edtRegisterConfirmPassword))
                  .getError());
          click(R.id.btnRegister);
          assertEquals(
              current().getString(R.string.auth_terms_required),
              ((TextView) current().findViewById(R.id.tvRegisterMessage)).getText().toString());
        });
    assertEquals(0, services.accountLookup().userId(email));
    capture("register-pastel");
    main(
        () -> {
          ((com.google.android.material.checkbox.MaterialCheckBox)
                  current().findViewById(R.id.checkTerms))
              .setChecked(true);
          click(R.id.btnRegister);
        });
    awaitIntro(0);
    int id = services.accountLookup().userId(email);
    assertTrue(id > 0);
    LearnerProfile p = services.learners().load(id);
    assertEquals(0, p.step);
    assertTrue(p.consentedAt > 0);
    try (DatabaseHelper h = new DatabaseHelper(context);
        Cursor c = h.getUserData(email)) {
      assertTrue(c.moveToFirst());
      String hash = c.getString(c.getColumnIndexOrThrow("password"));
      assertTrue(hash.startsWith("$2"));
      assertNotEquals(PASSWORD, hash);
      assertEquals("", c.getString(c.getColumnIndexOrThrow("phone")));
    }
    assertTrue(
        services.authenticator().authenticate(email.toUpperCase(java.util.Locale.ROOT), PASSWORD));
    assertEquals(
        -1,
        services
            .learnerRegistration()
            .registerLearner("Trùng email", email.toUpperCase(java.util.Locale.ROOT), PASSWORD));
    capture("onboarding-welcome");
  }

  @Test
  public void fourPagesResumeAcrossLoginAndCompletedProfileDoesNotRepeat() throws Exception {
    int id = learner();
    login(false);
    awaitIntro(0);
    main(() -> click(R.id.onboarding_next));
    awaitIntro(1);
    assertEquals(1, services.learners().load(id).step);
    main(() -> current().finish());
    AuthSession.clear(context);
    login(false);
    awaitIntro(1);
    main(() -> click(R.id.onboarding_next));
    awaitIntro(2);
    capture("onboarding-learning");
    main(() -> click(R.id.onboarding_next));
    awaitIntro(3);
    main(() -> click(R.id.onboarding_next));
    chooseProfile();
    capture("profile-setup");
    main(() -> click(R.id.profile_setup_save));
    await(HomeActivity.class);
    LearnerProfile p = services.learners().load(id);
    assertEquals(5, p.step);
    assertEquals(11, p.grade);
    assertEquals(3, p.goal);
    assertEquals("MATH,BIOLOGY", p.subjects);
    waitFor(
        a ->
            a instanceof HomeActivity
                && a.findViewById(R.id.tv_learning_context).getVisibility() == View.VISIBLE);
    main(
        () ->
            assertTrue(
                ((TextView) current().findViewById(R.id.tv_learning_context))
                    .getText()
                    .toString()
                    .contains("11")));
    main(() -> ((HomeActivity) current()).logout());
    await(MainActivity.class);
    login(false);
    await(HomeActivity.class);
    main(
        () -> {
          ((HomeActivity) current()).navigateTo(R.id.nav_settings, false);
          ((AppCompatActivity) current()).getSupportFragmentManager().executePendingTransactions();
        });
    waitFor(a -> a.findViewById(R.id.setting_app_guide) != null);
    main(() -> click(R.id.setting_app_guide));
    awaitIntro(0);
    main(() -> click(R.id.onboarding_skip));
    await(HomeActivity.class);
    assertEquals(5, services.learners().load(id).step);
  }

  @Test
  public void skipStillRequiresProfileAndDraftSurvivesRecreation() throws Exception {
    int id = learner();
    login(false);
    awaitIntro(0);
    main(() -> click(R.id.onboarding_skip));
    chooseProfile();
    main(
        () -> {
          edit(R.id.profile_minutes, "25");
          current().recreate();
        });
    waitFor(
        a ->
            a instanceof ProfileSetupActivity
                && a.findViewById(R.id.profile_setup_save).isEnabled()
                && ((EditText) a.findViewById(R.id.profile_minutes))
                    .getText()
                    .toString()
                    .equals("25"));
    main(
        () -> {
          assertTrue(((Chip) current().findViewById(R.id.grade_11)).isChecked());
          assertTrue(((Chip) current().findViewById(R.id.interest_math)).isChecked());
          current().finish();
        });
    assertEquals(4, services.learners().load(id).step);
    start(HomeActivity.class, email);
    await(ProfileSetupActivity.class);
    waitFor(a -> a.findViewById(R.id.profile_setup_save).isEnabled());
    main(() -> click(R.id.profile_setup_save));
    await(HomeActivity.class);
    assertEquals(25, services.learners().load(id).minutes);
  }

  @Test
  public void learnerDataIsIsolatedAndLateDraftCannotUndoCompletion() {
    int first = learner();
    int second =
        (int)
            services
                .learnerRegistration()
                .registerLearner("Bạn thứ hai", "second_" + email, PASSWORD);
    services.learners().advance(first, 2);
    assertEquals(0, services.learners().load(second).step);
    LearnerProfile p =
        new LearnerProfile(
            first, 4, "Hồ sơ đã xong", 12, "HISTORY", 2, 0, System.currentTimeMillis());
    services.learners().complete(p);
    services.learners().advance(first, 0);
    services.learners().saveDraft(new LearnerProfile(first, 1, "Bản cũ", 10, "MATH", 0, 10, 0));
    LearnerProfile done = services.learners().load(first);
    assertEquals(5, done.step);
    assertEquals(12, done.grade);
    assertEquals("HISTORY", done.subjects);
    assertEquals(0, services.learners().load(second).step);
  }

  @Test
  public void passwordDraftSurvivesThemeAndLanguageChanges() throws Exception {
    start(MainActivity.class, "");
    await(MainActivity.class);
    main(
        () -> {
          edit(R.id.edtLoginUsername, email);
          edit(R.id.edtLoginPassword, PASSWORD);
          click(R.id.auth_theme);
        });
    waitFor(
        a ->
            a instanceof MainActivity
                && (a.getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES);
    main(
        () -> {
          assertEquals(
              PASSWORD,
              ((EditText) current().findViewById(R.id.edtLoginPassword)).getText().toString());
          click(R.id.auth_language);
        });
    waitFor(
        a ->
            a instanceof MainActivity
                && a.getResources()
                    .getConfiguration()
                    .getLocales()
                    .get(0)
                    .getLanguage()
                    .equals("en"));
    main(
        () ->
            assertEquals(
                PASSWORD,
                ((EditText) current().findViewById(R.id.edtLoginPassword)).getText().toString()));
    capture("login-dark-english");
  }

  @Test
  public void rememberedSessionRestoresAndLogoutClearsIt() {
    int id = learner();
    services
        .learners()
        .complete(
            new LearnerProfile(
                id, 4, "Người học thử", 10, "MATH", 0, 0, System.currentTimeMillis()));
    login(true);
    await(HomeActivity.class);
    main(() -> current().finish());
    start(MainActivity.class, "");
    await(HomeActivity.class);
    assertEquals(email, AuthSession.remembered(context));
    main(() -> ((HomeActivity) current()).logout());
    await(MainActivity.class);
    assertEquals("", AuthSession.remembered(context));
    assertEquals(0, com.example.mentor.data.ActiveUser.get(context));
  }

  @Test
  public void disconnectedEmailServiceNeverClaimsSuccessOrChangesPassword() throws Exception {
    learner();
    assertFalse(services.recovery().available());
    start(AuthRecoveryActivity.class, "");
    await(AuthRecoveryActivity.class);
    main(
        () -> {
          assertFalse(current().findViewById(R.id.recovery_submit).isEnabled());
          assertEquals(
              current().getString(R.string.auth_recovery_unavailable),
              ((TextView) current().findViewById(R.id.recovery_status)).getText().toString());
          click(R.id.recovery_reset_mode);
          edit(R.id.recovery_token, "invalid-code");
          edit(R.id.recovery_password, "Changed01!2026");
          edit(R.id.recovery_confirm, "Changed01!2026");
          assertFalse(current().findViewById(R.id.recovery_submit).isEnabled());
        });
    capture("reset-service-unavailable");
    assertTrue(services.authenticator().authenticate(email, PASSWORD));
    assertFalse(services.authenticator().authenticate(email, "Changed01!2026"));
    main(
        () -> {
          click(R.id.recovery_verify_mode);
          assertFalse(current().findViewById(R.id.recovery_submit).isEnabled());
          assertFalse(current().findViewById(R.id.recovery_resend).isEnabled());
        });
    capture("verification-service-unavailable");
  }

  @Test
  public void accountAndProfileInsertRollbackTogetherOnWriteFailure() {
    String marker = "Rollback " + System.nanoTime();
    try (DatabaseHelper h = new DatabaseHelper(context)) {
      SQLiteDatabase db = h.getWritableDatabase();
      db.execSQL(
          "CREATE TRIGGER auth_profile_failure BEFORE INSERT ON learner_profiles WHEN"
              + " NEW.display_name='"
              + marker
              + "' BEGIN SELECT RAISE(ABORT,'QA profile failure'); END");
      try {
        try {
          services.learnerRegistration().registerLearner(marker, email, PASSWORD);
          fail("Write must fail");
        } catch (android.database.sqlite.SQLiteException expected) {
        }
        assertEquals(0, services.accountLookup().userId(email));
      } finally {
        db.execSQL("DROP TRIGGER auth_profile_failure");
      }
    }
  }

  @Test
  public void v2MigrationKeepsLegacyPasswordsAndSkipsOnboarding() {
    try (SQLiteDatabase memory = SQLiteDatabase.create(null);
        DatabaseHelper helper = new DatabaseHelper(context)) {
      memory.execSQL(
          "CREATE TABLE users(id INTEGER PRIMARY KEY,username TEXT,password TEXT,full_name"
              + " TEXT,address TEXT,phone TEXT,email TEXT,avatar TEXT)");
      memory.execSQL(
          "INSERT INTO users(id,username,password,full_name)"
              + " VALUES(7,'legacy','unchanged-hash','Existing learner')");
      helper.onUpgrade(memory, 2, 3);
      try (Cursor c =
          memory.rawQuery(
              "SELECT u.password,p.step,p.display_name FROM users u JOIN learner_profiles p ON"
                  + " p.user_id=u.id WHERE u.id=7",
              null)) {
        assertTrue(c.moveToFirst());
        assertEquals("unchanged-hash", c.getString(0));
        assertEquals(5, c.getInt(1));
        assertEquals("Existing learner", c.getString(2));
      }
    }
  }
}
