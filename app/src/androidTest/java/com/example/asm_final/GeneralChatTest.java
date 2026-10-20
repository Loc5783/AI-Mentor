package com.example.asm_final;

import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import androidx.core.content.FileProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.*;
import com.example.asm_final.activities.HomeActivity;
import com.example.asm_final.chat.*;
import com.example.asm_final.learning.LearningSetupStore;
import com.example.mentor.chat.*;
import com.example.mentor.data.ActiveUser;
import com.example.mentor.learning.SubjectId;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GeneralChatTest {
  private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
  private final Context context = ins.getTargetContext();
  private AiMentorApplication app;
  private AppServices services;
  private int owner, other, original;
  private String username;
  private ChatStore store;
  private ChatViewModel model;

  private void main(Runnable action) {
    ins.runOnMainSync(action);
    ins.waitForIdleSync();
  }

  private void await(BooleanSupplier condition) {
    long end = SystemClock.uptimeMillis() + 15000;
    while (SystemClock.uptimeMillis() < end) {
      boolean[] ok = {false};
      ins.runOnMainSync(() -> ok[0] = condition.getAsBoolean());
      if (ok[0]) return;
      SystemClock.sleep(40);
    }
    fail("Chat state did not settle");
  }

  @Before
  public void setup() {
    app = (AiMentorApplication) context.getApplicationContext();
    services = app.services();
    original = ActiveUser.get(context);
    username = "general_chat_" + System.nanoTime();
    owner =
        (int)
            services
                .registration()
                .register(username, "Mentor01!2026", "qa@gmail.com", "0901234567");
    other =
        (int)
            services
                .registration()
                .register(username + "_other", "Mentor01!2026", "qa@gmail.com", "0901234567");
    assertTrue(owner > 0 && other > 0);
    services.learners().setGrade(owner, 11);
    ActiveUser.set(context, owner);
    store = new LocalChatStore(context);
  }

  @After
  public void cleanup() {
    main(
        () -> {
          if (model != null) model.close();
          for (Stage stage :
              new Stage[] {
                Stage.RESUMED, Stage.STARTED, Stage.CREATED, Stage.STOPPED, Stage.PAUSED
              })
            for (Activity a :
                new ArrayList<>(
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage)))
              a.finish();
        });
    for (int user : new int[] {owner, other})
      if (user > 0)
        for (ChatRecords.Thread thread : store.threads(user, ""))
          store.deleteThread(user, thread.id);
    try (com.example.asm_final.activities.database.DatabaseHelper h =
        new com.example.asm_final.activities.database.DatabaseHelper(context)) {
      h.getWritableDatabase()
          .delete(
              "users",
              "id IN (?,?)",
              new String[] {Integer.toString(owner), Integer.toString(other)});
    }
    ActiveUser.set(context, original);
  }

  @Test
  public void fiveDestinationsExistAndEachOpensItsOwnScreen() throws Exception {
    HomeActivity home =
        (HomeActivity)
            ins.startActivitySync(
                new Intent(context, HomeActivity.class)
                    .putExtra("USERNAME", username)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    main(
        () -> {
          com.google.android.material.bottomnavigation.BottomNavigationView nav =
              home.findViewById(R.id.bottom_navigation);
          assertEquals(5, nav.getMenu().size());
          int[] expected = {
            R.id.nav_home, R.id.nav_chat, R.id.nav_category, R.id.nav_competition, R.id.nav_profile
          };
          for (int i = 0; i < 5; i++)
            assertEquals(expected[i], nav.getMenu().getItem(i).getItemId());
        });
    for (int tab :
        new int[] {
          R.id.nav_chat, R.id.nav_category, R.id.nav_competition, R.id.nav_profile, R.id.nav_home
        }) {
      main(
          () -> {
            home.navigateTo(tab, false);
            home.getSupportFragmentManager().executePendingTransactions();
          });
      if (tab == R.id.nav_chat)
        await(
            () ->
                home.findViewById(R.id.chat_input) != null
                    && home.findViewById(R.id.chat_input).isEnabled());
      if (tab == R.id.nav_profile)
        main(() -> assertNotNull(home.findViewById(R.id.profile_open_settings)));
      capture("five-tabs-" + tab);
    }
  }

  private void capture(String name) throws Exception {
    ins.waitForIdleSync();
    Bitmap bitmap = null;
    for (int i = 0; i < 3 && bitmap == null; i++) {
      bitmap = ins.getUiAutomation().takeScreenshot();
      if (bitmap == null) SystemClock.sleep(150);
    }
    assertNotNull(bitmap);
    File dir = new File(context.getExternalFilesDir(null), "general-chat-ui");
    assertTrue(dir.isDirectory() || dir.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
    bitmap.recycle();
  }

  @Test
  public void foldersKeepOrDeleteChatsAndCannotCrossAccountBoundaries() {
    String folder = store.createFolder(owner, "Sinh học");
    ChatRecords.Thread thread = store.create(owner, folder), foreign = store.create(other, null);
    store.saveDraft(owner, thread.id, "Câu hỏi đang soạn");
    store.renameThread(owner, thread.id, "Ôn tập");
    try {
      store.moveThread(other, foreign.id, folder);
      fail("Must reject foreign folder");
    } catch (IllegalArgumentException expected) {
    }
    try {
      store.snapshot(other, thread.id);
      fail("Must reject foreign thread");
    } catch (IllegalArgumentException expected) {
    }
    store.deleteFolder(owner, folder, false);
    assertNull(store.thread(owner, thread.id).folderId);
    assertEquals("Câu hỏi đang soạn", store.thread(owner, thread.id).draft);
    String replacement = store.createFolder(owner, "Lý");
    store.moveThread(owner, thread.id, replacement);
    store.deleteFolder(owner, replacement, true);
    assertTrue(store.threads(owner, "").isEmpty());
    assertEquals(1, store.threads(other, "").size());
  }

  @Test
  public void chatDraftSurvivesSwitchingTabsAndActivityRecreation() {
    HomeActivity home =
        (HomeActivity)
            ins.startActivitySync(
                new Intent(context, HomeActivity.class)
                    .putExtra("USERNAME", username)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    main(
        () -> {
          home.navigateTo(R.id.nav_chat, false);
          home.getSupportFragmentManager().executePendingTransactions();
        });
    await(
        () ->
            home.findViewById(R.id.chat_input) != null
                && home.findViewById(R.id.chat_input).isEnabled());
    main(
        () -> {
          ((android.widget.EditText) home.findViewById(R.id.chat_input))
              .setText("Giữ câu hỏi đang soạn");
          home.navigateTo(R.id.nav_home, false);
          home.getSupportFragmentManager().executePendingTransactions();
          home.navigateTo(R.id.nav_chat, false);
          home.getSupportFragmentManager().executePendingTransactions();
        });
    await(
        () ->
            home.findViewById(R.id.chat_input) != null
                && home.findViewById(R.id.chat_input).isEnabled());
    main(
        () ->
            assertEquals(
                "Giữ câu hỏi đang soạn",
                ((android.widget.EditText) home.findViewById(R.id.chat_input))
                    .getText()
                    .toString()));
    main(home::recreate);
    await(
        () -> {
          for (Activity a :
              ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
            if (a instanceof HomeActivity
                && a != home
                && a.findViewById(R.id.chat_input) != null
                && a.findViewById(R.id.chat_input).isEnabled())
              return ((android.widget.EditText) a.findViewById(R.id.chat_input))
                  .getText()
                  .toString()
                  .equals("Giữ câu hỏi đang soạn");
          return false;
        });
  }

  @Test
  public void storedReplyIsIdempotentAndFailedMessagesDoNotCorruptHistory() {
    ChatRecords.Thread thread = store.create(owner, null);
    String first = store.stageMessage(owner, thread.id, "Một câu hỏi");
    assertTrue(
        store.finish(owner, thread.id, first, new TutorReply("Một câu trả lời", false), null));
    assertFalse(store.finish(owner, thread.id, first, new TutorReply("Bị trùng", false), null));
    String failed = store.stageMessage(owner, thread.id, "Câu bị mất mạng");
    store.finish(owner, thread.id, failed, null, "NETWORK");
    String next = store.stageMessage(owner, thread.id, "Câu tiếp theo");
    TutorRequest request =
        store.request(
            owner,
            thread.id,
            next,
            new com.example.mentor.learning.LearningContext(owner, 11, "vi"));
    assertEquals(3, request.turns.size());
    assertEquals("Câu tiếp theo", request.turns.get(2).text);
  }

  @Test
  public void attachmentIsCopiedPrivatelyAndSurvivesPickerSourceDeletion() throws Exception {
    File directory = new File(context.getCacheDir(), "general_chat_camera");
    assertTrue(directory.isDirectory() || directory.mkdirs());
    File source = new File(directory, "qa-" + System.nanoTime() + ".txt");
    try (FileOutputStream out = new FileOutputStream(source)) {
      out.write("Học sinh lớp 11".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    LocalChatFiles files = new LocalChatFiles(context);
    ChatRecords.FileItem item =
        files.importUri(
            owner,
            FileProvider.getUriForFile(
                context, context.getPackageName() + ".fileprovider", source));
    assertTrue(source.delete());
    assertEquals("text/plain", item.mime);
    ChatRecords.Thread thread = store.create(owner, null);
    store.addDraftFile(owner, thread.id, item);
    assertEquals(1, store.snapshot(owner, thread.id).draftFiles.size());
    assertEquals(
        "Học sinh lớp 11",
        new String(files.payload(owner, item).bytes(), java.nio.charset.StandardCharsets.UTF_8));
    try {
      files.resolve(owner, new ChatRecords.FileItem("x", "x", "text/plain", "../other.bin", 1));
      fail("Escaping path");
    } catch (IllegalArgumentException expected) {
    }
    store.deleteThread(owner, thread.id);
    assertFalse(files.resolve(owner, item).exists());
  }

  private void openModel(TutorGateway gateway) {
    main(
        () -> {
          model = new ChatViewModel(app, store, gateway);
          model.bind(owner);
        });
    await(() -> model.snapshot != null && !model.loading);
  }

  @Test
  public void unavailableServiceKeepsDraftAndNeverCreatesAFakeAnswer() {
    openModel(GeminiTutorGateway.fromConfiguration("test-model", ""));
    main(
        () -> {
          model.setDraft("Giải thích giúp em");
          model.send();
        });
    assertEquals("Giải thích giúp em", model.draft);
    assertTrue(model.snapshot.messages.isEmpty());
    assertEquals(R.string.chat_service_unavailable, model.status);
  }

  @Test
  public void modelSendsActualGradeAndPreviousTurnsWithoutRepeatingProviderCalls() {
    List<TutorRequest> calls = new CopyOnWriteArrayList<>();
    openModel(
        new TutorGateway() {
          public boolean configured() {
            return true;
          }

          public TutorReply reply(TutorRequest r) {
            calls.add(r);
            return new TutorReply("Đáp án", false);
          }
        });
    main(
        () -> {
          model.setDraft("Câu 1");
          model.send();
        });
    await(() -> !model.requesting && model.snapshot.messages.size() == 2);
    main(
        () -> {
          model.setDraft("Câu 2");
          model.send();
        });
    await(() -> !model.requesting && model.snapshot.messages.size() == 4);
    assertEquals(2, calls.size());
    assertEquals(11, calls.get(1).context.grade);
    assertEquals(3, calls.get(1).turns.size());
    assertEquals("", model.draft);
  }

  @Test
  public void stoppingRequestRejectsLateProviderAnswer() throws Exception {
    CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
    openModel(
        new TutorGateway() {
          public boolean configured() {
            return true;
          }

          public TutorReply reply(TutorRequest r) {
            started.countDown();
            boolean done = false;
            while (!done)
              try {
                release.await();
                done = true;
              } catch (InterruptedException ignored) {
              }
            return new TutorReply("Trả lời đến muộn", false);
          }
        });
    try {
      main(
          () -> {
            model.setDraft("Một câu hỏi");
            model.send();
          });
      assertTrue(started.await(10, TimeUnit.SECONDS));
      main(() -> model.cancel());
      release.countDown();
      await(
          () ->
              model.snapshot.messages.size() == 1
                  && model.snapshot.messages.get(0).status.equals("CANCELLED"));
      SystemClock.sleep(250);
      assertEquals(1, store.snapshot(owner, model.threadId()).messages.size());
    } finally {
      release.countDown();
    }
  }

  @Test
  public void lessonEnrollmentOnlyEnablesThreeSubjectsAndKeepsGradesSeparate() {
    try (LearningSetupStore setup = new LearningSetupStore(context)) {
      setup.save(owner, 11, EnumSet.of(SubjectId.BIOLOGY, SubjectId.CHEMISTRY));
      assertEquals(2, setup.selected(owner, 11).size());
      assertTrue(setup.selected(owner, 12).isEmpty());
      try {
        setup.save(owner, 11, EnumSet.of(SubjectId.MATH));
        fail("Framework-only subject");
      } catch (IllegalArgumentException expected) {
      }
      assertEquals(2, setup.selected(owner, 11).size());
      assertTrue(setup.selected(other, 11).isEmpty());
    }
  }

  private android.widget.TextView text(android.view.View root, String value) {
    if (root instanceof android.widget.TextView
        && value.contentEquals(((android.widget.TextView) root).getText()))
      return (android.widget.TextView) root;
    if (root instanceof android.view.ViewGroup)
      for (int i = 0; i < ((android.view.ViewGroup) root).getChildCount(); i++) {
        android.widget.TextView found = text(((android.view.ViewGroup) root).getChildAt(i), value);
        if (found != null) return found;
      }
    return null;
  }

  @Test
  public void supplementaryToolsExpandPersistAndStillOpenCircuit() throws Exception {
    services.learners().setGrade(owner, 10);
    try (LearningSetupStore setup = new LearningSetupStore(context)) {
      setup.save(owner, 10, EnumSet.of(SubjectId.PHYSICS, SubjectId.CHEMISTRY));
    }
    HomeActivity first =
        (HomeActivity)
            ins.startActivitySync(
                new Intent(context, HomeActivity.class)
                    .putExtra("USERNAME", username)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    main(() -> first.navigateTo(R.id.nav_category, false));
    await(
        () ->
            text(first.findViewById(android.R.id.content), first.getString(R.string.lesson_circuit))
                != null);
    main(
        () -> {
          android.widget.TextView circuit =
              text(
                  first.findViewById(android.R.id.content),
                  first.getString(R.string.lesson_circuit));
          assertFalse(circuit.isShown());
          android.view.View card = (android.view.View) circuit.getParent().getParent();
          text(card, first.getString(R.string.lesson_tools_more)).performClick();
          assertTrue(circuit.isShown());
          first.recreate();
        });
    HomeActivity[] restored = {null};
    await(
        () -> {
          for (Activity activity :
              ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
            if (activity instanceof HomeActivity && activity != first) {
              restored[0] = (HomeActivity) activity;
              android.widget.TextView circuit =
                  text(
                      activity.findViewById(android.R.id.content),
                      activity.getString(R.string.lesson_circuit));
              return circuit != null && circuit.isShown();
            }
          return false;
        });
    main(
        () -> {
          android.widget.TextView periodic =
              text(
                  restored[0].findViewById(android.R.id.content),
                  restored[0].getString(R.string.lesson_periodic));
          assertFalse(periodic.isShown());
          text(
                  (android.view.View) periodic.getParent().getParent(),
                  restored[0].getString(R.string.lesson_tools_more))
              .performClick();
          assertTrue(periodic.isShown());
          assertTrue(periodic.hasOnClickListeners());
          text(
                  restored[0].findViewById(android.R.id.content),
                  restored[0].getString(R.string.lesson_circuit))
              .performClick();
        });
    await(
        () -> {
          for (Activity activity :
              ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
            if (activity instanceof com.brightpath.science.presentation.CircuitDiagramActivity)
              return true;
          return false;
        });
  }
}
