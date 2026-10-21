package com.example.asm_final;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.os.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.*;
import com.example.asm_final.activities.*;
import com.example.asm_final.backend.*;
import com.example.asm_final.learning.LessonViewModel;
import com.example.mentor.data.ActiveUser;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;

/** Opt-in local backend test. Does not use any AI provider or paid API key. */
@RunWith(AndroidJUnit4.class)
public final class ConnectedLearningTest {
  @Rule public final androidx.compose.ui.test.junit4.ComposeTestRule compose = ComposeProbe.rule();
  private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
  private final Context context = ins.getTargetContext();
  private int owner, original;
  private String username, endpoint, email;
  private final List<Integer> extraOwners = new ArrayList<>();
  private BackendSession session;
  private LessonViewModel model;
  private final Map<String, String> answers = new HashMap<>();
  private int curriculumLessonCount;

  private void main(Runnable action) {
    ins.runOnMainSync(action);
    ins.waitForIdleSync();
  }

  private void await(BooleanSupplier condition) {
    long end = SystemClock.uptimeMillis() + 25000;
    while (SystemClock.uptimeMillis() < end) {
      boolean[] ready = {false};
      ins.runOnMainSync(() -> ready[0] = condition.getAsBoolean());
      if (ready[0]) return;
      SystemClock.sleep(40);
    }
    fail("Learning state did not settle: " + (model == null ? "no model" : model.error));
  }

  private Activity current() {
    for (Activity activity :
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
      return activity;
    return null;
  }

  private void settled() {
    await(() -> !model.busy);
    main(() -> assertNull(model.error));
  }

  private void drawn() throws Exception {
    java.util.concurrent.CountDownLatch frames = new java.util.concurrent.CountDownLatch(1);
    ins.runOnMainSync(
        () -> {
          android.view.View root = current().getWindow().getDecorView();
          root.postOnAnimation(() -> root.postOnAnimation(frames::countDown));
        });
    assertTrue("Screen frame not rendered", frames.await(5, java.util.concurrent.TimeUnit.SECONDS));
    ins.waitForIdleSync();
  }

  @Before
  public void setup() throws Exception {
    endpoint = InstrumentationRegistry.getArguments().getString("mentorBackendUrl", "");
    Assume.assumeTrue("Requires a local QA backend", !endpoint.isEmpty());
    assertTrue(endpoint.matches("http://127\\.0\\.0\\.1:[0-9]+"));
    original = ActiveUser.get(context);
    AppServices app = ((AiMentorApplication) context.getApplicationContext()).services();
    username = "connected_" + System.nanoTime();
    email = username + "@school.example";
    owner = (int) app.registration().register(username, "Mentor01!2026", email, "0901234567");
    assertTrue(owner > 0);
    app.learners().setGrade(owner, 10);
    ActiveUser.set(context, owner);
    context.getSharedPreferences("backend_test", 0).edit().putString("endpoint", endpoint).commit();
    HttpURLConnection connection =
        (HttpURLConnection) new URL(endpoint + "/auth/sign-up").openConnection();
    try {
      connection.setRequestMethod("POST");
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(15000);
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/json");
      JSONObject body =
          new JSONObject()
              .put("displayName", username)
              .put("email", email)
              .put("password", "Mentor01!2026")
              .put("acceptedTermsVersion", "2026-10");
      try (OutputStream out = connection.getOutputStream()) {
        out.write(body.toString().getBytes(StandardCharsets.UTF_8));
      }
      assertEquals(201, connection.getResponseCode());
    } finally {
      connection.disconnect();
    }
    MainActivity login =
        (MainActivity)
            ins.startActivitySync(
                new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    main(
        () -> {
          ((android.widget.EditText) login.findViewById(R.id.edtLoginUsername)).setText(email);
          ((android.widget.EditText) login.findViewById(R.id.edtLoginPassword))
              .setText("Mentor01!2026");
          login.findViewById(R.id.btnLogin).performClick();
        });
    await(() -> current() instanceof HomeActivity);
    ComposeProbe.text(compose, context.getString(R.string.design_today));
    assertEquals(owner, ActiveUser.get(context));
    try (com.example.asm_final.accounts.RemoteAccountLinks links =
        new com.example.asm_final.accounts.RemoteAccountLinks(context)) {
      assertTrue(links.bound(owner, endpoint));
    }
    session = new BackendSession(context, owner, endpoint);
    assertNotNull(session.load());
    String encrypted =
        context.getSharedPreferences("backend_sessions", 0).getString("owner_" + owner, "");
    assertFalse(encrypted.contains("accessToken"));
    try (InputStream in = ins.getContext().getAssets().open("physics-10.json")) {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int n;
      while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
      JSONArray lessons = new JSONObject(bytes.toString("UTF-8")).getJSONArray("lessons");
      curriculumLessonCount = lessons.length();
      for (int i = 0; i < lessons.length(); i++) {
        JSONArray questions = lessons.getJSONObject(i).getJSONArray("questions");
        for (int q = 0; q < questions.length(); q++)
          answers.put(
              questions.getJSONObject(q).getString("id"),
              questions.getJSONObject(q).getString("correctOptionId"));
      }
    }
  }

  @After
  public void cleanup() {
    if (owner <= 0) return;
    main(
        () -> {
          for (Stage stage :
              new Stage[] {
                Stage.RESUMED, Stage.STARTED, Stage.CREATED, Stage.STOPPED, Stage.PAUSED
              })
            for (Activity activity :
                new ArrayList<>(
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage)))
              activity.finish();
        });
    if (session != null) session.clear();
    context.getSharedPreferences("backend_test", 0).edit().remove("endpoint").commit();
    try (com.example.asm_final.activities.database.DatabaseHelper helper =
        new com.example.asm_final.activities.database.DatabaseHelper(context)) {
      helper.getWritableDatabase().delete("users", "id=?", new String[] {Integer.toString(owner)});
      for (int extra : extraOwners) {
        new BackendSession(context, extra, endpoint).clear();
        helper
            .getWritableDatabase()
            .delete("users", "id=?", new String[] {Integer.toString(extra)});
      }
    }
    ActiveUser.set(context, original);
  }

  @Test
  public void dashboardReceivesServerChangesWithoutLeavingTheScreen() throws Exception {
    try (com.example.asm_final.learning.LearningSetupStore store =
        new com.example.asm_final.learning.LearningSetupStore(context)) {
      store.save(
          owner,
          10,
          java.util.EnumSet.of(
              com.example.mentor.learning.SubjectId.PHYSICS,
              com.example.mentor.learning.SubjectId.CHEMISTRY));
    }
    BackendClient otherClient = new BackendClient(context, owner);
    for (String subject : new String[] {"physics", "chemistry"}) {
      otherClient.request(
          "POST",
          "/enrollments",
          new JSONObject().put("subjectId", subject).put("grade", 10),
          UUID.randomUUID().toString(),
          null);
      JSONObject summary = otherClient.request("GET", "/me/dashboard?grade=10", null, null, null);
      int total = summary.getInt("totalLessons");
      assertTrue(total > 0);
      ComposeProbe.text(compose, context.getString(R.string.overview_lessons, 0, total));
    }
  }

  private JSONObject competitionHttp(
      JSONObject identity, String method, String path, JSONObject body, String revision)
      throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + path).openConnection();
    try {
      connection.setRequestMethod(method);
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(15000);
      if (identity != null)
        connection.setRequestProperty(
            "Authorization", "Bearer " + identity.getString("accessToken"));
      if (revision != null) connection.setRequestProperty("If-Match", revision);
      if (body != null) {
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        try (OutputStream out = connection.getOutputStream()) {
          out.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
      }
      int status = connection.getResponseCode();
      assertTrue("QA HTTP status " + status, status == 200 || status == 201);
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (InputStream input = connection.getInputStream()) {
        byte[] block = new byte[4096];
        int n;
        while ((n = input.read(block)) != -1) bytes.write(block, 0, n);
      }
      return new JSONObject(bytes.toString("UTF-8"));
    } finally {
      connection.disconnect();
    }
  }

  private com.example.asm_final.competition.CompetitionViewModel competitionModel() {
    androidx.fragment.app.Fragment screen =
        ((HomeActivity) current())
            .getSupportFragmentManager().getFragments().stream()
                .filter(
                    f ->
                        f instanceof com.example.asm_final.activities.fragments.CompetitionFragment
                            && f.isResumed())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Competition screen not active"));
    return new ViewModelProvider(screen)
        .get(com.example.asm_final.competition.CompetitionViewModel.class);
  }

  private String competitionAvatar(JSONObject identity) throws Exception {
    String boundary = "mentor" + System.nanoTime();
    ByteArrayOutputStream multipart = new ByteArrayOutputStream();
    multipart.write(
        ("--"
                + boundary
                + "\r\nContent-Disposition: form-data; name=\"purpose\"\r\n\r\nAVATAR\r\n"
                + "--"
                + boundary
                + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"avatar.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n")
            .getBytes(StandardCharsets.UTF_8));
    android.graphics.Bitmap image =
        android.graphics.Bitmap.createBitmap(16, 16, android.graphics.Bitmap.Config.ARGB_8888);
    image.eraseColor(0xff73305a);
    image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, multipart);
    image.recycle();
    multipart.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
    HttpURLConnection connection =
        (HttpURLConnection) new URL(endpoint + "/attachments").openConnection();
    try {
      connection.setRequestMethod("POST");
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(15000);
      connection.setRequestProperty("Authorization", "Bearer " + identity.getString("accessToken"));
      connection.setRequestProperty("Idempotency-Key", UUID.randomUUID().toString());
      connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
      connection.setDoOutput(true);
      try (OutputStream output = connection.getOutputStream()) {
        multipart.writeTo(output);
      }
      assertEquals(201, connection.getResponseCode());
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (InputStream input = connection.getInputStream()) {
        byte[] block = new byte[4096];
        int n;
        while ((n = input.read(block)) != -1) bytes.write(block, 0, n);
      }
      return new JSONObject(bytes.toString("UTF-8")).getString("id");
    } finally {
      connection.disconnect();
    }
  }

  private com.example.asm_final.competition.PublicLearnerDialog learnerDialog() {
    androidx.fragment.app.Fragment screen =
        ((HomeActivity) current())
            .getSupportFragmentManager().getFragments().stream()
                .filter(
                    f ->
                        f instanceof com.example.asm_final.activities.fragments.CompetitionFragment
                            && f.isResumed())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Competition screen not active"));
    return (com.example.asm_final.competition.PublicLearnerDialog)
        screen.getChildFragmentManager().findFragmentByTag("public_learner");
  }

  @Test
  public void rankingsFilterAcceptedFriendsAndPublicProfilesUpdateLive() throws Exception {
    String nickname = "Classmate_" + System.nanoTime();
    JSONObject other =
        competitionHttp(
            null,
            "POST",
            "/auth/sign-up",
            new JSONObject()
                .put("displayName", nickname)
                .put("email", nickname.toLowerCase(Locale.ROOT) + "@school.example")
                .put("password", "Mentor01!2026")
                .put("acceptedTermsVersion", "2026-10"),
            null);
    BackendClient own = new BackendClient(context, owner);
    JSONObject invitation =
        own.request(
            "POST",
            "/friendships",
            new JSONObject().put("recipientProfileId", other.getString("userId")),
            UUID.randomUUID().toString(),
            null);
    main(() -> ((HomeActivity) current()).navigateTo(R.id.nav_competition, false));
    ComposeProbe.text(compose, context.getString(R.string.competition_title));
    await(
        () -> {
          com.example.asm_final.competition.CompetitionViewModel.State state =
              competitionModel().state().getValue();
          return state != null
              && !state.loading
              && state.snapshot != null
              && state.snapshot.board != null
              && state.snapshot.board.entries.size() == 1;
        });
    ComposeProbe.click(compose, context.getString(R.string.competition_everyone));
    await(
        () -> {
          com.example.asm_final.competition.CompetitionViewModel.State state =
              competitionModel().state().getValue();
          return !state.loading
              && state.snapshot != null
              && state.snapshot.board != null
              && state.query.scope
                  == com.example.asm_final.competition.CompetitionQuery.Scope.GLOBAL;
        });
    ComposeProbe.click(compose, context.getString(R.string.competition_daily));
    ComposeProbe.click(compose, context.getString(R.string.competition_friends));
    await(
        () -> {
          com.example.asm_final.competition.CompetitionViewModel.State state =
              competitionModel().state().getValue();
          return !state.loading
              && state.snapshot != null
              && state.snapshot.board != null
              && state.snapshot.board.entries.size() == 1
              && state.query.metric
                  == com.example.asm_final.competition.CompetitionQuery.Metric.DAILY_XP
              && state.query.scope
                  == com.example.asm_final.competition.CompetitionQuery.Scope.FRIENDS;
        });
    competitionHttp(
        other,
        "PATCH",
        "/friendships/" + invitation.getString("id"),
        new JSONObject().put("decision", "ACCEPT"),
        "1");
    await(
        () -> {
          com.example.asm_final.competition.CompetitionViewModel.State state =
              competitionModel().state().getValue();
          return !state.loading
              && state.snapshot != null
              && state.snapshot.board != null
              && state.snapshot.board.entries.size() == 2;
        });
    ComposeProbe.listClick(compose, "ranking-list", "ranking:" + other.getString("userId"));
    await(
        () ->
            learnerDialog() != null
                && learnerDialog().getDialog() != null
                && nickname.contentEquals(
                    ((android.widget.TextView)
                            learnerDialog().getDialog().findViewById(R.id.public_profile_name))
                        .getText()));
    String changed = "Bạn học vừa đổi tên", bio = "Mình đang học Hóa học.";
    competitionHttp(
        other,
        "PATCH",
        "/me/public-profile",
        new JSONObject().put("nickname", changed).put("bio", bio),
        "1");
    await(
        () ->
            learnerDialog() != null
                && changed.contentEquals(
                    ((android.widget.TextView)
                            learnerDialog().getDialog().findViewById(R.id.public_profile_name))
                        .getText())
                && bio.contentEquals(
                    ((android.widget.TextView)
                            learnerDialog().getDialog().findViewById(R.id.public_profile_bio))
                        .getText()));
    String avatarId = competitionAvatar(other);
    competitionHttp(
        other,
        "PATCH",
        "/me/public-profile",
        new JSONObject().put("avatarAttachmentId", avatarId),
        "2");
    await(
        () ->
            learnerDialog() != null
                && new ViewModelProvider(learnerDialog())
                        .get(com.example.asm_final.competition.PublicProfileViewModel.class)
                        .state()
                        .getValue()
                        .avatar
                    != null);
    try (FileOutputStream out =
        new FileOutputStream(
            new File(context.getExternalFilesDir(null), "rankings-public-profile.png"))) {
      ins.getUiAutomation()
          .takeScreenshot()
          .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    }
    onView(withText(R.string.ranking_close)).perform(click());
    main(() -> current().recreate());
    await(
        () ->
            current() instanceof HomeActivity
                && competitionModel().query().metric
                    == com.example.asm_final.competition.CompetitionQuery.Metric.DAILY_XP
                && competitionModel().query().scope
                    == com.example.asm_final.competition.CompetitionQuery.Scope.FRIENDS
                && competitionModel().state().getValue().snapshot != null
                && competitionModel().state().getValue().snapshot.board != null);
    session.clear();
    main(() -> competitionModel().refresh());
    await(
        () ->
            competitionModel().state().getValue().snapshot != null
                && competitionModel().state().getValue().snapshot.needsLogin);
    ComposeProbe.click(compose, context.getString(R.string.login_btn));
    await(() -> current() instanceof MainActivity);
  }

  @Test
  public void overviewObserverRetriesCoalescesAndCancelsQueuedUpdates() throws Exception {
    java.util.concurrent.atomic.AtomicInteger attempts =
        new java.util.concurrent.atomic.AtomicInteger();
    java.util.concurrent.atomic.AtomicInteger delivered =
        new java.util.concurrent.atomic.AtomicInteger();
    java.util.concurrent.atomic.AtomicReference<EventStream.Listener> callback =
        new java.util.concurrent.atomic.AtomicReference<>();
    java.util.concurrent.CountDownLatch stopped = new java.util.concurrent.CountDownLatch(1);
    com.example.asm_final.dashboard.OverviewUpdates updates =
        new com.example.asm_final.dashboard.LiveOverviewUpdates(
            context,
            user ->
                new EventStream() {
                  @Override
                  public void run(Listener listener) throws Exception {
                    if (attempts.incrementAndGet() == 1)
                      throw new IOException("Temporary disconnect");
                    callback.set(listener);
                    listener.ready();
                    for (int i = 0; i < 100; i++) listener.changed("COMPLETION", "test-unit");
                    stopped.await();
                  }

                  @Override
                  public void close() {
                    stopped.countDown();
                  }
                });
    com.example.asm_final.dashboard.OverviewUpdates.Subscription subscription =
        updates.observe(owner, delivered::incrementAndGet);
    try {
      await(() -> delivered.get() == 1);
      assertEquals(2, attempts.get());
      main(
          () -> {
            callback.get().changed("COMPLETION", "queued-before-close");
            subscription.close();
            callback.get().changed("COMPLETION", "late-after-close");
          });
      SystemClock.sleep(750);
      assertEquals(1, delivered.get());
    } finally {
      subscription.close();
    }
  }

  @Test
  public void rankingRefreshBurstShowsResultsAndLoadsOnlyOneFollowup() throws Exception {
    java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.atomic.AtomicInteger calls =
        new java.util.concurrent.atomic.AtomicInteger();
    List<Long> shown = new ArrayList<>();
    com.example.asm_final.competition.CompetitionViewModel[] viewModel = {null};
    androidx.lifecycle.ViewModelStore store = new androidx.lifecycle.ViewModelStore();
    main(
        () -> {
          viewModel[0] =
              new com.example.asm_final.competition.CompetitionViewModel(
                  (Application) context.getApplicationContext(),
                  new androidx.lifecycle.SavedStateHandle(),
                  (user, query) -> {
                    int count = calls.incrementAndGet();
                    if (count == 1) {
                      entered.countDown();
                      try {
                        release.await();
                      } catch (InterruptedException stopped) {
                        Thread.currentThread().interrupt();
                      }
                    }
                    return new com.example.asm_final.competition.CompetitionRepository.Snapshot(
                        com.example.asm_final.competition.CompetitionRepository.Source.LOCAL,
                        null,
                        (long) count,
                        0,
                        false,
                        false);
                  });
          store.put("ranking-test", viewModel[0]);
          viewModel[0]
              .state()
              .observeForever(
                  state -> {
                    if (state.snapshot != null && state.snapshot.dailyXp != null)
                      shown.add(state.snapshot.dailyXp);
                  });
          viewModel[0].refresh();
        });
    try {
      assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
      main(
          () -> {
            for (int i = 0; i < 100; i++) viewModel[0].refresh();
          });
      release.countDown();
      await(
          () ->
              viewModel[0].state().getValue().snapshot != null
                  && Long.valueOf(2).equals(viewModel[0].state().getValue().snapshot.dailyXp)
                  && !viewModel[0].state().getValue().loading);
      assertEquals(2, calls.get());
      main(
          () -> {
            assertTrue(shown.contains(1L));
            assertTrue(shown.contains(2L));
          });
    } finally {
      release.countDown();
      main(store::clear);
    }
  }

  @Test
  public void realPlacementLessonHelpChatReturnRotationAndRewards() throws Exception {
    try (com.example.asm_final.learning.LearningSetupStore store =
        new com.example.asm_final.learning.LearningSetupStore(context)) {
      store.save(owner, 10, java.util.EnumSet.of(com.example.mentor.learning.SubjectId.PHYSICS));
    }
    LessonActivity activity =
        (LessonActivity)
            ins.startActivitySync(
                new Intent(context, LessonActivity.class)
                    .putExtra("USERNAME", username)
                    .putExtra("SUBJECT", "physics")
                    .putExtra("GRADE", 10)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    main(() -> model = new ViewModelProvider(activity).get(LessonViewModel.class));
    await(() -> !model.busy && model.path != null);
    assertNull(model.error);
    main(() -> model.start("placement", true));
    settled();
    JSONArray placement = model.attempt.getJSONArray("questions");
    for (int i = 0; i < placement.length(); i++) {
      String id = placement.getJSONObject(i).getString("id");
      int step = i;
      main(() -> model.save(step, id, answers.get(id)));
      settled();
    }
    main(model::complete);
    settled();
    assertEquals(0, model.completion.getInt("xpAwarded"));
    assertFalse(model.completion.getBoolean("streakLit"));
    main(model::backToPath);
    settled();
    assertEquals(
        curriculumLessonCount,
        new com.example.asm_final.learning.LessonPathData(model.path.getJSONArray("units"))
            .totalLessons);
    ComposeProbe.text(
        compose, context.getString(R.string.course_path_count, 0, curriculumLessonCount));
    String unit = model.path.getJSONArray("units").getJSONObject(0).getString("id");
    main(() -> model.start(unit, false));
    settled();
    main(() -> model.save(1, null, null));
    settled();
    main(() -> model.save(2, null, null));
    settled();
    String qid = model.attempt.getJSONArray("questions").getJSONObject(0).getString("id");
    String wrong = "a".equals(answers.get(qid)) ? "b" : "a";
    for (int i = 0; i < 2; i++) {
      main(() -> model.save(2, qid, wrong));
      settled();
    }
    assertEquals(2, model.attempt.getJSONArray("answers").getJSONObject(0).getInt("wrongCount"));
    assertTrue(model.attempt.getJSONArray("questions").getJSONObject(0).has("explanation"));
    main(activity::recreate);
    await(() -> current() instanceof LessonActivity && current() != activity);
    main(
        () -> model = new ViewModelProvider((LessonActivity) current()).get(LessonViewModel.class));
    assertEquals(2, model.attempt.getInt("step"));
    ComposeProbe.click(compose, context.getString(R.string.course_ask_ai));
    await(() -> current() instanceof HomeActivity);
    ComposeProbe.tag(compose, "chat-input");
    ComposeProbe.click(compose, context.getString(R.string.design_understood));
    await(() -> current() instanceof LessonActivity && !model.busy);
    assertEquals(2, model.attempt.getInt("step"));
    JSONArray practice = model.attempt.getJSONArray("questions");
    for (int i = 0; i < practice.length(); i++) {
      String id = practice.getJSONObject(i).getString("id");
      int step = i + 2;
      main(() -> model.save(step, id, answers.get(id)));
      settled();
    }
    main(model::complete);
    settled();
    assertEquals(200, model.completion.getInt("xpAwarded"));
    assertTrue(model.completion.getBoolean("streakLit"));
    LessonActivity[] completedActivity = {null};
    main(
        () -> {
          completedActivity[0] = (LessonActivity) current();
          completedActivity[0].recreate();
        });
    await(() -> current() instanceof LessonActivity && current() != completedActivity[0]);
    main(
        () -> {
          model = new ViewModelProvider((LessonActivity) current()).get(LessonViewModel.class);
          assertFalse(model.animateCompletion());
        });
    drawn();
    try (FileOutputStream out =
        new FileOutputStream(
            new File(context.getExternalFilesDir(null), "soft-learning-completion.png"))) {
      ins.getUiAutomation()
          .takeScreenshot()
          .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    }
    exportScreenshot("soft-learning-completion.png");
    String completion = model.completion.getString("id");
    main(model::complete);
    settled();
    assertEquals(completion, model.completion.getString("id"));
    JSONObject profile =
        new BackendClient(context, owner).request("GET", "/me/public-profile", null, null, null);
    assertEquals(200, profile.getInt("dailyXp"));
    assertEquals(1, profile.getInt("streakDays"));
    main(model::backToPath);
    settled();
    ComposeProbe.text(
        compose, context.getString(R.string.course_path_count, 1, curriculumLessonCount));
    drawn();
    try (FileOutputStream out =
        new FileOutputStream(
            new File(context.getExternalFilesDir(null), "soft-learning-path.png"))) {
      ins.getUiAutomation()
          .takeScreenshot()
          .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    }
    exportScreenshot("soft-learning-path.png");
    main(() -> current().finish());
    await(() -> current() instanceof HomeActivity);
    ComposeProbe.text(compose, context.getString(R.string.overview_daily_xp, 200L));
    ComposeProbe.text(
        compose, context.getString(R.string.overview_lessons, 1, curriculumLessonCount));
    main(() -> ((HomeActivity) current()).navigateTo(R.id.nav_profile, false));
    ComposeProbe.text(compose, context.getString(R.string.overview_daily_xp, 200L));
    main(() -> ((HomeActivity) current()).navigateTo(R.id.nav_competition, false));
    ComposeProbe.text(compose, context.getString(R.string.competition_streak_value, 1L));
    ComposeProbe.click(compose, context.getString(R.string.competition_daily));
    ComposeProbe.text(compose, context.getString(R.string.competition_xp_value, 200L));
    try (FileOutputStream out =
        new FileOutputStream(
            new File(context.getExternalFilesDir(null), "connected-learning-completion.png"))) {
      ins.getUiAutomation()
          .takeScreenshot()
          .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    }
  }

  private void exportScreenshot(String name) throws Exception {
    if (!name.matches("[a-z-]+\\.png")) throw new IllegalArgumentException("Invalid QA screenshot");
    File source = new File(context.getExternalFilesDir(null), name);
    try (InputStream result = new android.os.ParcelFileDescriptor.AutoCloseInputStream(
        ins.getUiAutomation().executeShellCommand("cp " + source.getAbsolutePath() + " /data/local/tmp/" + name))) {
      while (result.read() != -1) {}
    }
  }

  @Test
  public void emailAloneNeverMergesLegacyData() throws Exception {
    AppServices app = ((AiMentorApplication) context.getApplicationContext()).services();
    String legacyEmail = username + "_legacy@school.example";
    int legacy =
        (int) app.registration().register(username + "_legacy", "LocalOnly01!", legacyEmail, "");
    extraOwners.add(legacy);
    com.example.asm_final.chat.ChatRecords.Thread thread = app.chats().create(legacy, null);
    app.chats().saveDraft(legacy, thread.id, "Keep this legacy draft");
    com.example.asm_final.accounts.AuthTicket ticket =
        app.accountAccess().register("Separate account", legacyEmail, "RemoteOnly01!");
    extraOwners.add(ticket.userId);
    assertNotEquals(legacy, ticket.userId);
    try {
      new BackendClient(context, owner).signIn(legacyEmail, "RemoteOnly01!");
      fail("A lesson reauthentication must not switch remote identities");
    } catch (BackendClient.Failure mismatch) {
      assertEquals("ACCOUNT_LINK_REQUIRED", mismatch.code);
    }
    assertEquals("Keep this legacy draft", app.chats().thread(legacy, thread.id).draft);
    try (com.example.asm_final.accounts.RemoteAccountLinks links =
        new com.example.asm_final.accounts.RemoteAccountLinks(context)) {
      assertFalse(links.bound(legacy, endpoint));
      assertTrue(links.bound(ticket.userId, endpoint));
    }
  }

  @Test
  public void concurrentRefreshAndDelayedLogoutKeepTheNewSession() throws Exception {
    JSONObject stored = session.load();
    stored.put("accessToken", "expired-for-qa");
    session.save(stored);
    java.util.concurrent.ExecutorService pool =
        java.util.concurrent.Executors.newFixedThreadPool(4);
    try {
      List<java.util.concurrent.Future<JSONObject>> calls = new ArrayList<>();
      for (int i = 0; i < 4; i++)
        calls.add(
            pool.submit(
                () ->
                    new BackendClient(context, owner)
                        .request("GET", "/me/dashboard?grade=10", null, null, null)));
      assertEquals(owner, ActiveUser.get(context));
      for (java.util.concurrent.Future<JSONObject> call : calls)
        assertTrue(call.get(25, java.util.concurrent.TimeUnit.SECONDS).has("profileId"));
    } finally {
      pool.shutdownNow();
    }
    Runnable revokeOld = BackendClient.detach(context, owner);
    assertNull(session.load());
    ((AiMentorApplication) context.getApplicationContext())
        .services()
        .accountAccess()
        .signIn(email, "Mentor01!2026");
    revokeOld.run();
    assertNotNull(
        new BackendClient(context, owner).request("GET", "/me/account", null, null, null));
    ActiveUser.set(context, owner + 1000000);
    try {
      new BackendClient(context, owner).request("GET", "/me/account", null, null, null);
      fail("A client must not read the previous account");
    } catch (BackendClient.Failure expected) {
      assertEquals("ACCOUNT_CHANGED", expected.code);
    } finally {
      ActiveUser.set(context, owner);
    }
    JSONObject progress =
        new BackendClient(context, owner)
            .request("GET", "/me/dashboard?grade=10", null, null, null);
    String remoteId = progress.getString("profileId");
    try (com.example.asm_final.dashboard.LearningOverviewStore cache =
        new com.example.asm_final.dashboard.LearningOverviewStore(context)) {
      cache.save(owner, endpoint, remoteId, 10, progress);
      assertNotNull(cache.load(owner, endpoint, remoteId, 10));
      assertNull(cache.load(owner, endpoint, remoteId, 11));
      assertNull(cache.load(owner, endpoint + "/other", remoteId, 10));
      assertNull(cache.load(owner, endpoint, "other-remote-account", 10));
      assertNull(cache.load(owner + 1000000, endpoint, remoteId, 10));
    }
  }
}
