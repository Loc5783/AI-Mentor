package com.example.asm_final;

import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.asm_final.accounts.AuthTicket;
import com.example.asm_final.activities.HomeActivity;
import com.example.asm_final.backend.BackendClient;
import com.example.asm_final.learning.LearningSetupStore;
import com.example.mentor.data.ActiveUser;
import com.example.mentor.learning.SubjectId;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;

/** Opt-in smoke test of the compiled emulator URL, using demo 03 without awarding fake XP. */
@RunWith(AndroidJUnit4.class)
public final class LocalBackendConnectionTest {
  @Test
  public void configuredEmulatorCanSignInLoadSixtyLessonsAndShowLiveDashboard() throws Exception {
    Assume.assumeTrue(
        Boolean.parseBoolean(
            InstrumentationRegistry.getArguments().getString("mentorLocalBackend", "false")));
    assertEquals("http://10.0.2.2:8000", BuildConfig.MENTOR_BACKEND_URL);
    Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    Context context = ins.getTargetContext();
    SharedPreferences settings = context.getSharedPreferences("backend_test", 0);
    boolean contained = settings.contains("endpoint");
    String previous = settings.getString("endpoint", "");
    int oldOwner = ActiveUser.get(context);
    HomeActivity activity = null;
    settings.edit().remove("endpoint").commit();
    try {
      AppServices services = ((AiMentorApplication) context.getApplicationContext()).services();
      AuthTicket ticket = services.accountAccess().signIn("student 03", "123123123");
      assertNotNull(ticket);
      ActiveUser.set(context, ticket.userId);
      services.learners().setGrade(ticket.userId, 10);
      BackendClient api = new BackendClient(context, ticket.userId);
      assertTrue(api.configured());
      try (LearningSetupStore store = new LearningSetupStore(context)) {
        store.save(
            ticket.userId,
            10,
            EnumSet.of(SubjectId.PHYSICS, SubjectId.CHEMISTRY, SubjectId.BIOLOGY));
      }
      for (String subject : new String[] {"physics", "chemistry", "biology"}) {
        JSONObject enrollment =
            api.request(
                "POST",
                "/enrollments",
                new JSONObject().put("subjectId", subject).put("grade", 10),
                UUID.randomUUID().toString(),
                null);
        JSONObject path =
            api.request(
                "GET", "/enrollments/" + enrollment.getString("id") + "/path", null, null, null);
        JSONArray units = path.getJSONArray("units");
        assertEquals(24, units.length());
        for (int i = 5; i < units.length(); i += 6)
          assertEquals("FIVE_LESSON_REVIEW", units.getJSONObject(i).getString("kind"));
      }
      assertEquals(
          60,
          api.request("GET", "/me/dashboard?grade=10", null, null, null).getInt("totalLessons"));
      activity =
          (HomeActivity)
              ins.startActivitySync(
                  new Intent(context, HomeActivity.class)
                      .putExtra("USERNAME", ticket.username)
                      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      final HomeActivity home = activity;
      boolean ready = false;
      long end = SystemClock.uptimeMillis() + 20000;
      while (SystemClock.uptimeMillis() < end) {
        boolean[] loaded = {false};
        ins.runOnMainSync(
            () -> {
              TextView status = home.findViewById(R.id.tv_dashboard_status);
              loaded[0] =
                  status != null
                      && status
                          .getText()
                          .toString()
                          .equals(home.getString(R.string.overview_current));
            });
        if (loaded[0]) {
          ready = true;
          break;
        }
        SystemClock.sleep(50);
      }
      assertTrue("Configured emulator did not load live backend data", ready);
      ins.runOnMainSync(
          () ->
              assertEquals(
                  3,
                  ((android.view.ViewGroup) home.findViewById(R.id.dashboard_subject_grid))
                      .getChildCount()));
      CountDownLatch drawn = new CountDownLatch(1);
      ins.runOnMainSync(
          () ->
              home.getWindow()
                  .getDecorView()
                  .postOnAnimation(
                      () -> home.getWindow().getDecorView().postOnAnimation(drawn::countDown)));
      assertTrue(drawn.await(5, TimeUnit.SECONDS));
      try (FileOutputStream out =
          new FileOutputStream(
              new File(context.getExternalFilesDir(null), "school-live-dashboard.png"))) {
        ins.getUiAutomation()
            .takeScreenshot()
            .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
      }
    } finally {
      if (activity != null) {
        final HomeActivity home = activity;
        ins.runOnMainSync(home::finish);
      }
      ActiveUser.set(context, oldOwner);
      SharedPreferences.Editor edit = settings.edit();
      if (contained) edit.putString("endpoint", previous);
      else edit.remove("endpoint");
      edit.commit();
    }
  }
}
