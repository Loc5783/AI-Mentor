package com.example.asm_final;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.mentor.chat.*;
import com.example.mentor.learning.*;
import java.io.IOException;
import java.util.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Fake transport tests exercise real JSON/rotation without paid API calls. */
@RunWith(AndroidJUnit4.class)
public class SharedTutorContractTest {
  private TutorRequest request() {
    return new TutorRequest(
        new LearningContext(7, 11, "vi"),
        Arrays.asList(
            new TutorTurn(7, TutorTurn.Role.USER, "Câu trước", Collections.emptyList()),
            new TutorTurn(7, TutorTurn.Role.ASSISTANT, "Giải thích trước", Collections.emptyList()),
            new TutorTurn(
                7,
                TutorTurn.Role.USER,
                "Giải thích rõ bước này",
                Arrays.asList(new TutorAttachment("bai.png", "image/png", new byte[] {1, 2, 3})))),
        new LessonHandoff("e", "a", "l", "q", "v", 4));
  }

  private String answer(String finish) {
    return "{\"candidates\":[{\"finishReason\":\""
        + finish
        + "\",\"content\":{\"parts\":[{\"text\":\"Giải thích phù hợp lớp 11\"}]}}]}";
  }

  @Test
  public void sendsRealGradeMultiTurnHistoryAndAttachmentAsSeparateParts() throws Exception {
    String[] captured = {null};
    GeminiTutorGateway gateway =
        new GeminiTutorGateway(
            new GeminiKeyPool("sample-a"),
            "test-model",
            (key, model, json) -> {
              captured[0] = json;
              return new GeminiTutorGateway.Response(200, answer("STOP"));
            });
    assertFalse(gateway.reply(request()).truncated);
    JSONObject payload = new JSONObject(captured[0]);
    assertTrue(payload.getJSONObject("systemInstruction").toString().contains("grade 11"));
    JSONArray turns = payload.getJSONArray("contents");
    assertEquals(3, turns.length());
    assertEquals("model", turns.getJSONObject(1).getString("role"));
    assertEquals(
        "image/png",
        turns
            .getJSONObject(2)
            .getJSONArray("parts")
            .getJSONObject(1)
            .getJSONObject("inlineData")
            .getString("mimeType"));
    assertFalse(captured[0].contains("sample-a"));
    assertFalse(captured[0].contains("enrollmentId"));
  }

  @Test
  public void unauthorizedKeyRotatesToBackup() throws Exception {
    int[] calls = {0};
    GeminiTutorGateway gateway =
        new GeminiTutorGateway(
            new GeminiKeyPool("first", "second"),
            "test-model",
            (key, model, json) -> {
              calls[0]++;
              return new GeminiTutorGateway.Response(
                  key.equals("first") ? 403 : 200, key.equals("first") ? "" : answer("STOP"));
            });
    assertFalse(gateway.reply(request()).text.isEmpty());
    assertEquals(2, calls[0]);
  }

  @Test
  public void exhaustedKeysReportRateLimitWithoutFakeAnswer() throws Exception {
    int[] calls = {0};
    GeminiTutorGateway gateway =
        new GeminiTutorGateway(
            new GeminiKeyPool("first", "second"),
            "test-model",
            (key, model, json) -> {
              calls[0]++;
              return new GeminiTutorGateway.Response(429, "");
            });
    try {
      gateway.reply(request());
      fail("Must fail");
    } catch (TutorFailure failure) {
      assertEquals(TutorFailure.Code.RATE_LIMITED, failure.code);
    }
    assertEquals(2, calls[0]);
  }

  @Test
  public void uncertainNetworkFailureDoesNotAutomaticallyRepeatPaidGeneration() throws Exception {
    int[] calls = {0};
    GeminiTutorGateway gateway =
        new GeminiTutorGateway(
            new GeminiKeyPool("first"),
            "test-model",
            (key, model, json) -> {
              calls[0]++;
              throw new IOException("offline");
            });
    try {
      gateway.reply(request());
      fail("Must fail");
    } catch (TutorFailure failure) {
      assertEquals(TutorFailure.Code.NETWORK, failure.code);
    }
    assertEquals(1, calls[0]);
  }

  @Test
  public void truncatedAnswerHasExplicitFlag() throws Exception {
    GeminiTutorGateway gateway =
        new GeminiTutorGateway(
            new GeminiKeyPool("sample"),
            "test-model",
            (key, model, json) -> new GeminiTutorGateway.Response(200, answer("MAX_TOKENS")));
    assertTrue(gateway.reply(request()).truncated);
  }

  @Test
  public void blockedAnswerCannotBePresentedAsSuccessful() throws Exception {
    GeminiTutorGateway gateway =
        new GeminiTutorGateway(
            new GeminiKeyPool("sample"),
            "test-model",
            (key, model, json) -> new GeminiTutorGateway.Response(200, answer("SAFETY")));
    try {
      gateway.reply(request());
      fail("Must fail");
    } catch (TutorFailure failure) {
      assertEquals(TutorFailure.Code.EMPTY_RESPONSE, failure.code);
    }
  }

  @Test
  public void appComposesAllNineIndependentlyOwnedSubjects() {
    AiMentorApplication app =
        (AiMentorApplication)
            InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
    SubjectCatalog catalog = app.services().subjects();
    assertEquals(9, catalog.all().size());
    assertEquals(3, catalog.lessonModules().size());
    assertTrue(catalog.hasLessonScope(SubjectId.BIOLOGY));
    assertTrue(catalog.hasLessonScope(SubjectId.CHEMISTRY));
    assertTrue(catalog.hasLessonScope(SubjectId.PHYSICS));
    assertFalse(catalog.hasLessonScope(SubjectId.MATH));
    assertFalse(catalog.hasLessonScope(SubjectId.ENGLISH));
    assertTrue(catalog.module(SubjectId.MATH).tools().contains(SubjectModule.Tool.CALCULATOR));
    assertFalse(catalog.module(SubjectId.HISTORY).tools().contains(SubjectModule.Tool.CALCULATOR));
    assertTrue(catalog.module(SubjectId.PHYSICS).tools().contains(SubjectModule.Tool.CIRCUIT));
    assertTrue(
        catalog.module(SubjectId.CHEMISTRY).tools().contains(SubjectModule.Tool.PERIODIC_TABLE));
  }
}
