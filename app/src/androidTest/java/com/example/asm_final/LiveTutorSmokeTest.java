package com.example.asm_final;

import static org.junit.Assert.*;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.mentor.chat.*;
import com.example.mentor.learning.LearningContext;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;

/** Opt-in real provider tests. Private credentials are runtime input, never baked into an APK. */
@RunWith(AndroidJUnit4.class)
public final class LiveTutorSmokeTest {
  private TutorGateway gateway;
  private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

  @Before
  public void prepare() throws Exception {
    Assume.assumeTrue(
        "true".equals(InstrumentationRegistry.getArguments().getString("mentorLiveAi")));
    File file = new File(context.getExternalFilesDir("private-qa"), "old-gemini.json");
    assertTrue("Private QA credentials are required for an opted-in live run", file.isFile());
    assertTrue("Unexpected QA configuration size", file.length() < 32768);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (InputStream input = new FileInputStream(file)) {
      byte[] block = new byte[4096];
      int n;
      while ((n = input.read(block)) != -1) {
        if (bytes.size() + n > 32768) throw new IOException("QA configuration too large");
        bytes.write(block, 0, n);
      }
    }
    JSONObject config = new JSONObject(bytes.toString("UTF-8"));
    JSONArray values = config.getJSONArray("keys");
    String[] keys = new String[values.length()];
    for (int i = 0; i < keys.length; i++) keys[i] = values.getString(i);
    gateway = GeminiTutorGateway.fromConfiguration(config.getString("model"), keys);
    assertTrue("Private QA configuration is not usable", gateway.configured());
  }

  private void probe(String subject, int grade, String question, String expected) throws Exception {
    TutorRequest request =
        new TutorRequest(
            new LearningContext(1, grade, "vi"),
            Collections.singletonList(
                new TutorTurn(1, TutorTurn.Role.USER, question, Collections.emptyList())),
            null);
    TutorReply reply = gateway.reply(request);
    assertFalse("Provider returned a truncated answer", reply.truncated);
    assertTrue("Provider answer is empty or too short", reply.text.trim().length() >= 30);
    assertTrue(
        "Expected basic concept or result missing",
        reply.text.toLowerCase(Locale.ROOT).contains(expected));
    JSONObject report =
        new JSONObject()
            .put("subject", subject)
            .put("grade", grade)
            .put("question", question)
            .put("answer", reply.text)
            .put("truncated", reply.truncated);
    File folder = context.getExternalFilesDir("live-tutor");
    assertTrue(folder.isDirectory() || folder.mkdirs());
    try (OutputStream output = new FileOutputStream(new File(folder, subject + ".json"))) {
      output.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
    }
  }

  @Test
  public void physicsGrade10() throws Exception {
    probe(
        "physics",
        10,
        "Vật chuyển động đều với tốc độ 5 m/s trong 4 giây đi được bao nhiêu mét? Giải thích ngắn"
            + " theo trình độ hiện tại của mình.",
        "20");
  }

  @Test
  public void chemistryGrade11() throws Exception {
    probe(
        "chemistry",
        11,
        "Ôn lại kiến thức nền: nguyên tử trung hòa có số hiệu nguyên tử Z = 8 thì có bao nhiêu"
            + " proton và electron? Giải thích ngắn, vừa sức.",
        "8");
  }

  @Test
  public void biologyGrade12() throws Exception {
    probe(
        "biology",
        12,
        "Giải thích ngắn vai trò của màng sinh chất và tính thấm chọn lọc, theo trình độ hiện tại"
            + " của mình.",
        "màng");
  }
}
