package com.example.asm_final.learning;

import static org.junit.Assert.*;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.SavedStateHandle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.asm_final.AiMentorApplication;
import com.example.asm_final.AppServices;
import com.example.asm_final.activities.LessonActivity;
import com.example.mentor.data.ActiveUser;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Real device instrumented test suite running directly on Android.
 * Covers Scenarios A through I (logic tests with controllable fake API and real Looper/SavedState)
 * as well as real Activity lifecycle actions (recreation, background/foreground, chat return)
 * and live backend integration over HTTP loopback/reverse proxy.
 */
@RunWith(AndroidJUnit4.class)
public class ConnectedLessonRealDeviceScenariosTest {

  private Context context;
  private Application app;
  private AppServices services;
  private Handler mainHandler;

  @Before
  public void setUp() {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    app = (Application) context.getApplicationContext();
    services = ((AiMentorApplication) app).services();
    mainHandler = new Handler(Looper.getMainLooper());
  }

  private void runOnMain(Runnable action) throws Exception {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      action.run();
    } else {
      CountDownLatch latch = new CountDownLatch(1);
      AtomicReference<Throwable> failure = new AtomicReference<>();
      mainHandler.post(() -> {
        try {
          action.run();
        } catch (Throwable t) {
          failure.set(t);
        } finally {
          latch.countDown();
        }
      });
      assertTrue("Main thread action timed out", latch.await(10, TimeUnit.SECONDS));
      if (failure.get() != null) {
        throw new RuntimeException(failure.get());
      }
    }
  }

  // Controllable fake client to capture exact request payloads & keys
  public static class CapturingApiClient implements LessonViewModel.LessonApiClient {
    public static class Call {
      public final String method;
      public final String path;
      public final JSONObject body;
      public final String key;
      public final Integer revision;

      public Call(String method, String path, JSONObject body, String key, Integer revision) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.key = key;
        this.revision = revision;
      }
    }

    public final List<Call> calls = Collections.synchronizedList(new ArrayList<>());
    public volatile boolean signedIn = true;
    public volatile boolean configured = true;
    public volatile java.util.function.Function<Call, JSONObject> customResponder;

    @Override
    public boolean configured() {
      return configured;
    }

    @Override
    public boolean signedIn() {
      return signedIn;
    }

    @Override
    public void signIn(String identifier, String password) {}

    @Override
    public JSONObject request(
        String method, String path, JSONObject body, String key, Integer revision)
        throws Exception {
      Call call = new Call(method, path, body, key, revision);
      calls.add(call);
      if (customResponder != null) {
        JSONObject res = customResponder.apply(call);
        if (res != null) return res;
      }
      if ("POST".equals(method) && "/enrollments".equals(path)) {
        String subj = body != null ? body.optString("subjectId", "def") : "def";
        return new JSONObject().put("id", "enr_" + subj);
      }
      if ("GET".equals(method) && path.endsWith("/path")) {
        return new JSONObject()
            .put("enrollment", new JSONObject().put("id", "enr_default"))
            .put("curriculumVersion", "v1")
            .put("units", new JSONArray());
      }
      if ("GET".equals(method) && path.startsWith("/attempts/")) {
        return new JSONObject()
            .put("id", path.substring("/attempts/".length()))
            .put("subjectId", "physics")
            .put("grade", 10)
            .put("step", 1)
            .put("revision", 1)
            .put("questions", new JSONArray());
      }
      return new JSONObject();
    }
  }

  // Controlled executor that dispatches to Android Main Looper when handler is provided
  public static class StepExecutor implements Executor {
    public final List<Runnable> queue = Collections.synchronizedList(new ArrayList<>());
    private final Handler handler;

    public StepExecutor() {
      this.handler = null;
    }

    public StepExecutor(Handler handler) {
      this.handler = handler;
    }

    @Override
    public void execute(Runnable r) {
      queue.add(r);
    }

    public boolean runNext() throws Exception {
      if (!queue.isEmpty()) {
        Runnable r = queue.remove(0);
        if (handler != null) {
          CountDownLatch latch = new CountDownLatch(1);
          AtomicReference<Throwable> err = new AtomicReference<>();
          handler.post(() -> {
            try {
              r.run();
            } catch (Throwable t) {
              err.set(t);
            } finally {
              latch.countDown();
            }
          });
          assertTrue("Action timed out on main handler", latch.await(5, TimeUnit.SECONDS));
          if (err.get() != null) {
            throw new RuntimeException(err.get());
          }
        } else {
          r.run();
        }
        return true;
      }
      return false;
    }

    public void runAll() throws Exception {
      while (runNext()) {}
    }
  }

  // =========================================================================
  // SCENARIO A: Chuyển môn/lớp khi đang tải
  // =========================================================================
  @Test
  public void testScenarioA_SubjectGradeSwitchWhileLoading() throws Exception {
    SavedStateHandle saved = new SavedStateHandle();
    StepExecutor worker = new StepExecutor();
    StepExecutor main = new StepExecutor(mainHandler);
    CapturingApiClient client = new CapturingApiClient();

    AtomicInteger activeUserRef = new AtomicInteger(1);
    LessonViewModel vm = new LessonViewModel(app, saved, client, worker, main, activeUserRef::get);

    runOnMain(() -> {
      // 1. Bind Physics 10
      vm.bind(1, "physics", 10);
    });

    String physicsKey = saved.get("1:physics:10:enrollKey");
    assertNotNull("Scope Physics 10 must generate enrollment key", physicsKey);
    assertEquals("Worker must have queued task for Physics", 1, worker.queue.size());

    runOnMain(() -> {
      // 2. Switch to Math 12 before Physics completes
      vm.bind(1, "math", 12);
    });

    String mathKey = saved.get("1:math:12:enrollKey");
    assertNotNull("Scope Math 12 must generate enrollment key", mathKey);
    assertNotEquals("Keys for different scopes must be different", physicsKey, mathKey);
    assertEquals("Worker now has 2 tasks", 2, worker.queue.size());

    // 3. Let old Physics worker run
    boolean ran = worker.runNext();
    assertTrue(ran);

    // Verify: request sent by old worker has Physics payload AND Physics key
    assertTrue("Old worker must have executed calls", client.calls.size() >= 1);
    CapturingApiClient.Call c = client.calls.get(0);
    assertEquals("POST", c.method);
    assertEquals("/enrollments", c.path);
    assertEquals("physics", c.body.optString("subjectId"));
    assertEquals(10, c.body.optInt("grade"));
    assertEquals("Old Physics worker must use its own scoped key", physicsKey, c.key);
    assertNotEquals("Old Physics worker must NOT use Math key", mathKey, c.key);

    // 4. Run old Physics callback on main thread
    main.runAll();

    // Verify: old callback dropped! Math state not poisoned
    assertEquals("math", vm.subject());
    assertEquals(12, vm.grade());
    assertNull("Path of Math 12 must remain null after old callback dropped", vm.path);
    assertNull("Attempt must remain null", vm.attempt);
    assertNull("Error must remain null", vm.error);

    // 5. Now let Math 12 worker run
    worker.runNext();
    main.runAll();

    CapturingApiClient.Call mathCall = client.calls.stream()
        .filter(call -> "math".equals(call.body != null ? call.body.optString("subjectId") : null))
        .findFirst()
        .orElse(null);
    assertNotNull("Math 12 request must have been sent", mathCall);
    assertEquals("math", mathCall.body.optString("subjectId"));
    assertEquals(12, mathCall.body.optInt("grade"));
    assertEquals("Math worker must use Math key", mathKey, mathCall.key);
  }

  // =========================================================================
  // SCENARIO B: Chuyển tài khoản
  // =========================================================================
  @Test
  public void testScenarioB_AccountSwitchIsolation() throws Exception {
    SavedStateHandle saved = new SavedStateHandle();
    StepExecutor worker = new StepExecutor();
    StepExecutor main = new StepExecutor(mainHandler);
    CapturingApiClient client = new CapturingApiClient();

    AtomicInteger activeUserRef = new AtomicInteger(101);
    LessonViewModel vm = new LessonViewModel(app, saved, client, worker, main, activeUserRef::get);

    runOnMain(() -> vm.bind(101, "physics", 10));
    String owner101Key = saved.get("101:physics:10:enrollKey");
    assertNotNull(owner101Key);

    // Active user switches to 102
    activeUserRef.set(102);

    runOnMain(() -> vm.bind(102, "physics", 10));
    String owner102Key = saved.get("102:physics:10:enrollKey");
    assertNotNull(owner102Key);
    assertNotEquals("Different accounts must have different enroll keys", owner101Key, owner102Key);

    // Run old worker of 101 and its callback
    worker.runNext();
    main.runAll();

    // Verify: callback dropped because activeUser is 102 and vm.owner is 102
    assertEquals(102, vm.owner());
    assertNull("Owner 101 response must not bleed into Owner 102", vm.path);

    // Run 102 worker
    worker.runNext();
    main.runAll();
    assertNotNull("Owner 102 receives its own path", vm.path);
  }

  // =========================================================================
  // SCENARIO C: Rebind cùng scope (Idempotency logic)
  // =========================================================================
  @Test
  public void testScenarioC_RebindIdempotency_SameScopeNoDuplicateNetwork() throws Exception {
    SavedStateHandle saved = new SavedStateHandle();
    StepExecutor worker = new StepExecutor();
    StepExecutor main = new StepExecutor(mainHandler);
    CapturingApiClient client = new CapturingApiClient();

    LessonViewModel vm = new LessonViewModel(app, saved, client, worker, main, () -> 1);

    runOnMain(() -> vm.bind(1, "physics", 10));
    worker.runAll();
    main.runAll();
    assertNotNull("Path loaded initially", vm.path);
    int callCountBefore = client.calls.size();

    // Rebind with same scope: ViewModel must recognise existing loaded scope and avoid duplicate calls
    runOnMain(() -> vm.bind(1, "physics", 10));

    assertEquals("Rebind with same scope must NOT make duplicate network requests",
        callCountBefore, client.calls.size());
    assertEquals("Worker queue must be empty on rebind", 0, worker.queue.size());
  }

  // =========================================================================
  // SCENARIO D: Xử lý tác vụ bất đồng bộ (Worker background & callback execution)
  // =========================================================================
  @Test
  public void testScenarioD_AsyncWorkerAndCallbackExecution() throws Exception {
    SavedStateHandle saved = new SavedStateHandle();
    StepExecutor worker = new StepExecutor();
    StepExecutor main = new StepExecutor(mainHandler);
    CapturingApiClient client = new CapturingApiClient();

    LessonViewModel vm = new LessonViewModel(app, saved, client, worker, main, () -> 1);
    runOnMain(() -> vm.bind(1, "physics", 10));

    // Worker finishes in background queue
    worker.runAll();

    // Main looper processes callback results
    main.runAll();

    assertFalse("Must not hang in busy state", vm.busy);
    assertNotNull("Path must be loaded cleanly", vm.path);
    assertNull("No error after async callback execution", vm.error);
  }

  // =========================================================================
  // SCENARIO E: Quay lại từ Chat AI (Khôi phục tiến độ đã lưu từ server)
  // =========================================================================
  @Test
  public void testScenarioE_LeaveScreenAndReturn_ChatHandoff() throws Exception {
    SavedStateHandle saved = new SavedStateHandle();
    StepExecutor worker = new StepExecutor();
    StepExecutor main = new StepExecutor(mainHandler);
    CapturingApiClient client = new CapturingApiClient();

    // Configure fake server to return verified saved progress with step=3, revision=2
    JSONObject serverAttempt = new JSONObject()
        .put("id", "att_step_3")
        .put("subjectId", "physics")
        .put("grade", 10)
        .put("step", 3)
        .put("revision", 2)
        .put("kind", "LESSON")
        .put("questions", new JSONArray()
            .put(new JSONObject().put("id", "q1").put("prompt", "Prompt 1").put("options", new JSONArray().put(new JSONObject().put("id", "o1").put("text", "Opt1"))))
            .put(new JSONObject().put("id", "q2").put("prompt", "Prompt 2").put("options", new JSONArray().put(new JSONObject().put("id", "o2").put("text", "Opt2"))))
            .put(new JSONObject().put("id", "q3").put("prompt", "Prompt 3").put("options", new JSONArray().put(new JSONObject().put("id", "o3").put("text", "Opt3")))))
        .put("answers", new JSONArray()
            .put(new JSONObject().put("questionId", "q1").put("selectedOptionId", "o1").put("correct", true)));

    client.customResponder = call -> {
      if ("GET".equals(call.method) && "/attempts/att_step_3".equals(call.path)) {
        return serverAttempt;
      }
      return null;
    };

    // A restored attempt must exist before bind reads SavedStateHandle. Writing
    // a pointer after bind does not create an active attempt in the controller.
    runOnMain(() -> saved.set("1:physics:10:attemptId", "att_step_3"));
    LessonViewModel vm = new LessonViewModel(app, saved, client, worker, main, () -> 1);
    runOnMain(() -> vm.bind(1, "physics", 10));
    worker.runAll();
    main.runAll();
    assertNotNull("Bind must restore the active attempt before Chat handoff", vm.attempt);

    // Returning from Chat: reload() is invoked
    runOnMain(vm::reload);
    worker.runAll();
    main.runAll();

    CapturingApiClient.Call lastCall = client.calls.get(client.calls.size() - 1);
    assertEquals("GET", lastCall.method);
    assertEquals("/attempts/att_step_3", lastCall.path);

    // Assert attempt progress fields that directly drive LearningUi.PracticeContent:
    assertNotNull("Attempt must not be null after reload", vm.attempt);
    assertEquals("att_step_3", vm.attempt.optString("id"));
    assertEquals("Step must be 3 as confirmed by server", 3, vm.attempt.optInt("step"));
    assertEquals("Revision must be 2 as confirmed by server", 2, vm.attempt.optInt("revision"));

    // Check questions array and UI active question index calculation:
    // In PracticeContent: index = maxOf(0, step - if (lesson) 2 else 0) -> 3 - 2 = 1 (Question 2)
    JSONArray questions = vm.attempt.getJSONArray("questions");
    assertEquals(3, questions.length());
    int activeQuestionIndex = Math.max(0, vm.attempt.optInt("step") - ("LESSON".equals(vm.attempt.optString("kind")) ? 2 : 0));
    assertEquals(1, activeQuestionIndex);
    assertEquals("q2", questions.getJSONObject(activeQuestionIndex).getString("id"));

    // Check previous saved answer is preserved:
    JSONArray answers = vm.attempt.getJSONArray("answers");
    assertEquals(1, answers.length());
    assertEquals("q1", answers.getJSONObject(0).getString("questionId"));
    assertEquals("o1", answers.getJSONObject(0).getString("selectedOptionId"));
  }

  // =========================================================================
  // SCENARIO F: Khôi phục trạng thái từ SavedStateHandle (SavedState recovery logic)
  // =========================================================================
  @Test
  public void testScenarioF_SavedStateHandleRestorationLogic() throws Exception {
    // 1. Prior session writes scoped state
    SavedStateHandle saved1 = new SavedStateHandle();
    saved1.set("1:physics:10:attemptId", "att_saved_42");
    saved1.set("1:physics:10:pendingMethod", "PATCH");
    saved1.set("1:physics:10:pendingPath", "/attempts/att_saved_42");
    saved1.set("1:physics:10:pendingBody", "{\"step\":2}");
    saved1.set("1:physics:10:pendingKey", "key_idempotent_123");
    saved1.set("1:physics:10:pendingRevision", 3);

    // 2. New ViewModel instance created with restored SavedStateHandle
    StepExecutor worker = new StepExecutor();
    StepExecutor main = new StepExecutor(mainHandler);
    CapturingApiClient client = new CapturingApiClient();

    LessonViewModel vm = new LessonViewModel(app, saved1, client, worker, main, () -> 1);

    runOnMain(() -> vm.bind(1, "physics", 10));
    assertTrue("Must detect pending work from restored state after bind", vm.hasPending());

    worker.runAll();
    main.runAll();

    // Verify: bind restored the scoped attempt
    CapturingApiClient.Call firstCall = client.calls.get(0);
    assertEquals("GET", firstCall.method);
    assertEquals("/attempts/att_saved_42", firstCall.path);

    // Verify: retry sends the exact pending request with preserved key and revision
    runOnMain(vm::retry);
    worker.runAll();
    main.runAll();

    CapturingApiClient.Call retryCall = client.calls.get(client.calls.size() - 1);
    assertEquals("PATCH", retryCall.method);
    assertEquals("/attempts/att_saved_42", retryCall.path);
    assertEquals("key_idempotent_123", retryCall.key);
    assertEquals(Integer.valueOf(3), retryCall.revision);
  }

  // =========================================================================
  // SCENARIO G: Lỗi mạng và thử lại (Network retry idempotency)
  // =========================================================================
  @Test
  public void testScenarioG_NetworkErrorAndRetryIdempotency() throws Exception {
    SavedStateHandle saved = new SavedStateHandle();
    StepExecutor worker = new StepExecutor();
    StepExecutor main = new StepExecutor(mainHandler);
    CapturingApiClient client = new CapturingApiClient();

    LessonViewModel vm = new LessonViewModel(app, saved, client, worker, main, () -> 1);
    runOnMain(() -> vm.bind(1, "physics", 10));
    worker.runAll();
    main.runAll();

    vm.attempt = new JSONObject().put("id", "att_net").put("revision", 1);

    // Force network failure on next call
    client.customResponder = call -> {
      if ("PATCH".equals(call.method)) {
        throw new RuntimeException("Simulated network timeout");
      }
      try {
        return new JSONObject().put("id", "att_net").put("revision", 2);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };

    runOnMain(() -> vm.save(1, "q1", "opt_a"));
    String pendingKey1 = saved.get("1:physics:10:pendingKey");
    assertNotNull(pendingKey1);

    // Worker fails
    worker.runAll();
    main.runAll();

    assertEquals("NETWORK_ERROR", vm.error);
    assertFalse(vm.busy);
    assertTrue("Pending must remain preserved after network failure", vm.hasPending());

    // Restore network and click Retry
    client.customResponder = null;
    runOnMain(vm::retry);

    String pendingKey2 = saved.get("1:physics:10:pendingKey");
    assertEquals("Retry MUST preserve the exact same idempotency key", pendingKey1, pendingKey2);

    worker.runAll();
    main.runAll();

    assertNull("Error must be cleared after successful retry", vm.error);
    assertFalse(vm.hasPending());
  }

  // =========================================================================
  // SCENARIO H: Nhấn nhanh / lặp lại (Rapid clicks debouncing)
  // =========================================================================
  @Test
  public void testScenarioH_RapidRepeatedClicksProtection() throws Exception {
    SavedStateHandle saved = new SavedStateHandle();
    StepExecutor worker = new StepExecutor();
    StepExecutor main = new StepExecutor(mainHandler);
    CapturingApiClient client = new CapturingApiClient();

    LessonViewModel vm = new LessonViewModel(app, saved, client, worker, main, () -> 1);
    runOnMain(() -> vm.bind(1, "physics", 10));
    worker.runAll();
    main.runAll();

    vm.attempt = new JSONObject().put("id", "att_rapid").put("revision", 1);

    runOnMain(() -> {
      // First click: initiates save
      vm.save(1, "q1", "opt_a");
      // Rapid follow-up clicks while busy
      vm.save(1, "q1", "opt_b");
      vm.save(2, "q2", "opt_c");
      vm.complete();
    });

    // Exactly 1 worker task must have been queued
    assertEquals("Rapid clicks must NOT queue duplicate worker tasks", 1, worker.queue.size());
    worker.runAll();
    main.runAll();
  }

  // =========================================================================
  // SCENARIO I: Khôi phục bài làm legacy (Candidate store separation)
  // =========================================================================
  @Test
  public void testScenarioI_LegacyAttemptRecoveryAndScopeSeparation() throws Exception {
    SavedStateHandle saved = new SavedStateHandle();
    // Simulate legacy attempt saved without metadata
    saved.set("attemptId", "legacy_chem_11");
    saved.set("savedOwner", 1);

    StepExecutor worker = new StepExecutor();
    StepExecutor main = new StepExecutor(mainHandler);
    CapturingApiClient client = new CapturingApiClient();

    // Server says legacy_chem_11 is actually Chemistry 11
    client.customResponder = call -> {
      if (call.path.equals("/attempts/legacy_chem_11")) {
        try {
          return new JSONObject()
              .put("id", "legacy_chem_11")
              .put("subjectId", "chemistry")
              .put("grade", 11)
              .put("questions", new JSONArray());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
      return null;
    };

    LessonViewModel vm = new LessonViewModel(app, saved, client, worker, main, () -> 1);

    // 1. Open Physics 10 -> Server detects mismatch
    runOnMain(() -> vm.bind(1, "physics", 10));
    worker.runAll();
    main.runAll();

    assertNull("Physics 10 must NOT adopt Chemistry 11 attempt", vm.attempt);
    assertNotNull("Physics 10 gets clean path", vm.path);

    // Verify: Chemistry 11 legacy candidate was saved with metadata in candidate store
    assertEquals("chemistry", saved.get("legacy_candidate_subj:1:legacy_chem_11"));
    assertEquals(Integer.valueOf(11), saved.get("legacy_candidate_grade:1:legacy_chem_11"));

    // 2. Open Chemistry 11 -> Cleanly recovers legacy candidate!
    runOnMain(() -> vm.bind(1, "chemistry", 11));
    worker.runAll();
    main.runAll();

    assertNotNull("Chemistry 11 must recover its matching legacy candidate", vm.attempt);
    assertEquals("legacy_chem_11", vm.attempt.optString("id"));
  }

  // =========================================================================
  // REAL ACTIVITY LIFECYCLE TESTS VIA ActivityScenario
  // =========================================================================
  @Test
  public void testLifecycle_ActivityRecreation_PreservesAttemptProgressAndAnswers() throws Exception {
    int prevUser = ActiveUser.get(context);
    try {
      ActiveUser.set(context, 1);
      Intent intent = new Intent(context, LessonActivity.class);
      intent.putExtra("GRADE", 10);
      intent.putExtra("SUBJECT", "physics");

      try (ActivityScenario<LessonActivity> scenario = ActivityScenario.launch(intent)) {
        scenario.moveToState(Lifecycle.State.RESUMED);

        // Setup an in-progress attempt with step 3, revision 2, questions, and saved answers
        JSONObject activeAttempt = new JSONObject()
            .put("id", "att_recreate_42")
            .put("subjectId", "physics")
            .put("grade", 10)
            .put("step", 3)
            .put("revision", 2)
            .put("kind", "LESSON")
            .put("questions", new JSONArray()
                .put(new JSONObject().put("id", "q1").put("prompt", "Prompt 1").put("options", new JSONArray().put(new JSONObject().put("id", "opt_1a").put("text", "Opt 1"))))
                .put(new JSONObject().put("id", "q2").put("prompt", "Prompt 2").put("options", new JSONArray().put(new JSONObject().put("id", "opt_2b").put("text", "Opt 2"))))
                .put(new JSONObject().put("id", "q3").put("prompt", "Prompt 3").put("options", new JSONArray().put(new JSONObject().put("id", "opt_3c").put("text", "Opt 3")))))
            .put("answers", new JSONArray()
                .put(new JSONObject().put("questionId", "q1").put("selectedOptionId", "opt_1a").put("correct", true)));

        scenario.onActivity(activity -> {
          androidx.lifecycle.ViewModelProvider provider = new androidx.lifecycle.ViewModelProvider(activity);
          LessonViewModel vm = provider.get(LessonViewModel.class);
          vm.attempt = activeAttempt;
        });

        // Recreate activity (simulating configuration change / screen rotation)
        scenario.recreate();

        // Verify: activity is alive, and ViewModel retained attempt, progress step, and saved answer
        scenario.onActivity(activity -> {
          assertNotNull("Recreated activity must exist", activity);
          assertFalse("Recreated activity must not be finishing", activity.isFinishing());

          androidx.lifecycle.ViewModelProvider provider = new androidx.lifecycle.ViewModelProvider(activity);
          LessonViewModel vm = provider.get(LessonViewModel.class);
          assertNotNull("Attempt must be preserved after recreation", vm.attempt);
          assertEquals("att_recreate_42", vm.attempt.optString("id"));
          assertEquals("Step must be preserved at 3", 3, vm.attempt.optInt("step"));
          assertEquals("Revision must be preserved at 2", 2, vm.attempt.optInt("revision"));

          JSONArray questions = vm.attempt.optJSONArray("questions");
          assertNotNull("Questions array must be intact", questions);
          assertEquals(3, questions.length());

          JSONArray answers = vm.attempt.optJSONArray("answers");
          assertNotNull("Answers array must be intact", answers);
          assertEquals(1, answers.length());
          assertEquals("q1", answers.optJSONObject(0).optString("questionId"));
          assertEquals("opt_1a", answers.optJSONObject(0).optString("selectedOptionId"));
        });
      }
    } finally {
      ActiveUser.set(context, prevUser);
    }
  }

  @Test
  public void testLifecycle_BackgroundForegroundTransition_PreservesAttemptAndState() throws Exception {
    int prevUser = ActiveUser.get(context);
    try {
      ActiveUser.set(context, 1);
      Intent intent = new Intent(context, LessonActivity.class);
      intent.putExtra("GRADE", 10);
      intent.putExtra("SUBJECT", "physics");

      try (ActivityScenario<LessonActivity> scenario = ActivityScenario.launch(intent)) {
        scenario.moveToState(Lifecycle.State.RESUMED);

        JSONObject activeAttempt = new JSONObject()
            .put("id", "att_bg_99")
            .put("subjectId", "physics")
            .put("grade", 10)
            .put("step", 3)
            .put("revision", 2)
            .put("kind", "LESSON")
            .put("questions", new JSONArray()
                .put(new JSONObject().put("id", "q1").put("prompt", "P1").put("options", new JSONArray().put(new JSONObject().put("id", "opt_1").put("text", "T1"))))
                .put(new JSONObject().put("id", "q2").put("prompt", "P2").put("options", new JSONArray().put(new JSONObject().put("id", "opt_2").put("text", "T2")))))
            .put("answers", new JSONArray()
                .put(new JSONObject().put("questionId", "q1").put("selectedOptionId", "opt_1").put("correct", true)));

        scenario.onActivity(activity -> {
          LessonViewModel vm = new androidx.lifecycle.ViewModelProvider(activity).get(LessonViewModel.class);
          vm.attempt = activeAttempt;
        });

        // App moved to background (user pressed Home or switched apps)
        scenario.moveToState(Lifecycle.State.CREATED);

        // App returned to foreground
        scenario.moveToState(Lifecycle.State.RESUMED);

        scenario.onActivity(activity -> {
          assertNotNull("Activity must be alive after foreground return", activity);
          assertFalse("Activity must not be finishing", activity.isFinishing());

          LessonViewModel vm = new androidx.lifecycle.ViewModelProvider(activity).get(LessonViewModel.class);
          assertFalse("Must not hang in busy state", vm.busy);
          assertNotNull("Attempt must be preserved after backgrounding", vm.attempt);
          assertEquals("att_bg_99", vm.attempt.optString("id"));
          assertEquals(3, vm.attempt.optInt("step"));
          assertEquals(2, vm.attempt.optInt("revision"));
          assertEquals("opt_1", vm.attempt.optJSONArray("answers").optJSONObject(0).optString("selectedOptionId"));
        });
      }
    } finally {
      ActiveUser.set(context, prevUser);
    }
  }

  @Test
  public void testLifecycle_ChatHandoffAndReturn_PreservesProgressAndAnswers() throws Exception {
    int prevUser = ActiveUser.get(context);
    try {
      ActiveUser.set(context, 1);
      Intent intent = new Intent(context, LessonActivity.class);
      intent.putExtra("GRADE", 10);
      intent.putExtra("SUBJECT", "physics");

      try (ActivityScenario<LessonActivity> scenario = ActivityScenario.launch(intent)) {
        scenario.moveToState(Lifecycle.State.RESUMED);

        JSONObject initialAttempt = new JSONObject()
            .put("id", "att_chat_handoff")
            .put("subjectId", "physics")
            .put("grade", 10)
            .put("step", 3)
            .put("revision", 2)
            .put("kind", "LESSON")
            .put("questions", new JSONArray()
                .put(new JSONObject().put("id", "q1").put("prompt", "P1").put("options", new JSONArray().put(new JSONObject().put("id", "opt_1").put("text", "T1"))))
                .put(new JSONObject().put("id", "q2").put("prompt", "P2").put("options", new JSONArray().put(new JSONObject().put("id", "opt_2").put("text", "T2")))))
            .put("answers", new JSONArray()
                .put(new JSONObject().put("questionId", "q1").put("selectedOptionId", "opt_1").put("correct", true)));

        scenario.onActivity(activity -> {
          LessonViewModel vm = new androidx.lifecycle.ViewModelProvider(activity).get(LessonViewModel.class);
          vm.attempt = initialAttempt;
          try {
            java.lang.reflect.Field returningField = LessonActivity.class.getDeclaredField("returning");
            returningField.setAccessible(true);
            returningField.setBoolean(activity, true);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });

        // Simulating user moving to Chat and returning to LessonActivity
        scenario.moveToState(Lifecycle.State.CREATED);
        scenario.moveToState(Lifecycle.State.RESUMED);

        scenario.onActivity(activity -> {
          LessonViewModel vm = new androidx.lifecycle.ViewModelProvider(activity).get(LessonViewModel.class);
          assertNotNull("Attempt must not be null after returning from Chat", vm.attempt);
          assertEquals("att_chat_handoff", vm.attempt.optString("id"));
          assertEquals("Step must be preserved at 3", 3, vm.attempt.optInt("step"));
          assertEquals("Revision must be preserved at 2", 2, vm.attempt.optInt("revision"));
          assertEquals("Saved answer for q1 must remain intact", "opt_1",
              vm.attempt.optJSONArray("answers").optJSONObject(0).optString("selectedOptionId"));
        });
      }
    } finally {
      ActiveUser.set(context, prevUser);
    }
  }

  // =========================================================================
  // LIVE BACKEND END-TO-END ROUNDTRIP OVER USB REVERSE
  // =========================================================================
  @Test
  public void testLiveBackendIntegration_RealNetworkRoundtrip() throws Exception {
    String endpoint = com.example.asm_final.backend.BackendClient.endpoint(context);
    if (endpoint == null || endpoint.isEmpty()) {
      endpoint = "http://127.0.0.1:18080";
    }
    boolean backendReachable = false;
    try {
      java.net.HttpURLConnection check =
          (java.net.HttpURLConnection) new java.net.URL(endpoint + "/health").openConnection();
      check.setConnectTimeout(3000);
      check.setReadTimeout(3000);
      backendReachable = (check.getResponseCode() == 200);
      check.disconnect();
    } catch (Exception ignored) {
      backendReachable = false;
    }

    // Explicitly SKIP with Assume so JUnit runner does NOT count as PASSED when unreachable
    Assume.assumeTrue("Backend is unreachable at " + endpoint + "; test skipped", backendReachable);

    int prevActiveUser = ActiveUser.get(context);
    String prevTestEndpoint = context.getSharedPreferences("backend_test", 0).getString("endpoint", null);
    int testOwner = 0;
    ExecutorService liveExecutor = Executors.newSingleThreadExecutor();

    try {
      // 1. Create an isolated test user on device SQLite
      String testUser = "devqa_" + System.currentTimeMillis();
      String testEmail = testUser + "@qa.example";
      testOwner = (int) services.registration().register(testUser, "Mentor01!2026", testEmail, "0912345678");
      assertTrue("Test user must be registered in local database", testOwner > 0);
      services.learners().setGrade(testOwner, 10);

      // 2. Sign up on QA backend
      java.net.HttpURLConnection signUp =
          (java.net.HttpURLConnection) new java.net.URL(endpoint + "/auth/sign-up").openConnection();
      signUp.setRequestMethod("POST");
      signUp.setDoOutput(true);
      signUp.setRequestProperty("Content-Type", "application/json");
      JSONObject signUpBody =
          new JSONObject()
              .put("displayName", testUser)
              .put("email", testEmail)
              .put("password", "Mentor01!2026")
              .put("acceptedTermsVersion", "2026-10");
      try (java.io.OutputStream out = signUp.getOutputStream()) {
        out.write(signUpBody.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      int status = signUp.getResponseCode();
      assertEquals("Sign up on QA backend must succeed with 201", 201, status);
      java.io.ByteArrayOutputStream outBytes = new java.io.ByteArrayOutputStream();
      try (java.io.InputStream in = signUp.getInputStream()) {
        byte[] buf = new byte[2048];
        int n;
        while ((n = in.read(buf)) != -1) outBytes.write(buf, 0, n);
      }
      JSONObject signUpResp = new JSONObject(outBytes.toString("UTF-8"));
      String remoteUserId = signUpResp.getString("userId");
      signUp.disconnect();

      try (com.example.asm_final.accounts.RemoteAccountLinks links =
          new com.example.asm_final.accounts.RemoteAccountLinks(context)) {
        links.bind(testOwner, endpoint, remoteUserId);
      }

      // 3. Configure backend session & SharedPreferences test endpoint
      context.getSharedPreferences("backend_test", 0).edit().putString("endpoint", endpoint).commit();
      ActiveUser.set(context, testOwner);

      // 4. Use real BackendClient
      com.example.asm_final.backend.BackendClient realClient =
          new com.example.asm_final.backend.BackendClient(context, testOwner);
      realClient.signIn(testEmail, "Mentor01!2026");
      assertTrue("Real backend client must be signed in", realClient.signedIn());

      // 5. Run LessonViewModel with real BackendClient on device
      SavedStateHandle liveSaved = new SavedStateHandle();
      LessonViewModel liveVm =
          new LessonViewModel(
              app,
              liveSaved,
              realClient,
              liveExecutor,
              mainHandler::post,
              () -> ActiveUser.get(context));

      final int finalTestOwner = testOwner;
      runOnMain(() -> liveVm.bind(finalTestOwner, "physics", 10));

      // Wait for real network call to settle
      long deadline = android.os.SystemClock.uptimeMillis() + 15000;
      while (android.os.SystemClock.uptimeMillis() < deadline) {
        if (!liveVm.busy && liveVm.path != null) break;
        android.os.SystemClock.sleep(100);
      }

      assertNotNull("Real backend must return valid curriculum path", liveVm.path);
      assertNull("No error on real path fetch", liveVm.error);
      assertTrue("Path must have enrollment object", liveVm.path.has("enrollment"));

      // Verify scoped key was used and saved
      String savedKey = liveSaved.get(testOwner + ":physics:10:enrollKey");
      assertNotNull("Scoped enroll key must be saved", savedKey);
    } finally {
      liveExecutor.shutdownNow();
      // Clean up SharedPreferences and active user
      ActiveUser.set(context, prevActiveUser);
      if (prevTestEndpoint != null) {
        context.getSharedPreferences("backend_test", 0).edit().putString("endpoint", prevTestEndpoint).commit();
      } else {
        context.getSharedPreferences("backend_test", 0).edit().remove("endpoint").commit();
      }
    }
  }
}
