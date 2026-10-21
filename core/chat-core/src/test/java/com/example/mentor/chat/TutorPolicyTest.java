package com.example.mentor.chat;

import static org.junit.Assert.*;

import com.example.mentor.learning.*;
import java.util.*;
import org.junit.Test;

public class TutorPolicyTest {
  @Test
  public void invalidChatConfigCannotCrashTheApplicationComposition() throws Exception {
    TutorGateway gateway = GeminiTutorGateway.fromConfiguration("bad/model", "sample");
    assertFalse(gateway.configured());
    try {
      gateway.reply(null);
      fail("Must report configuration error");
    } catch (TutorFailure failure) {
      assertEquals(TutorFailure.Code.INVALID_CONFIGURATION, failure.code);
    }
    assertTrue(GeminiTutorGateway.fromConfiguration(" test-model ", "sample").configured());
  }

  private TutorTurn turn(int owner, TutorTurn.Role role, String text) {
    return new TutorTurn(owner, role, text, Collections.emptyList());
  }

  @Test
  public void promptUsesActualGradeAndAllNineSubjectsInBothLanguages() {
    for (int grade = 10; grade <= 12; grade++)
      for (String language : new String[] {"vi", "en"}) {
        String prompt = TutorInstructions.forLearner(new LearningContext(3, grade, language));
        assertTrue(prompt.contains("grade " + grade));
        for (SubjectId id : SubjectId.values()) assertTrue(prompt.contains(id.label(language)));
      }
  }

  @Test(expected = IllegalArgumentException.class)
  public void anotherAccountsHistoryCannotBeSent() {
    new TutorRequest(
        new LearningContext(1, 10, "vi"),
        Arrays.asList(turn(2, TutorTurn.Role.USER, "Question")),
        null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void historyMustIncludeLatestUserMessage() {
    new TutorRequest(
        new LearningContext(1, 10, "vi"),
        Arrays.asList(
            turn(1, TutorTurn.Role.USER, "Question"), turn(1, TutorTurn.Role.ASSISTANT, "Answer")),
        null);
  }

  @Test
  public void completeHistoryAndResumePointerAreImmutable() {
    List<TutorTurn> original =
        new ArrayList<>(Arrays.asList(turn(1, TutorTurn.Role.USER, "Explain")));
    LessonHandoff handoff = new LessonHandoff("e", "a", "l", "q", "v", 2);
    TutorRequest request = new TutorRequest(new LearningContext(1, 11, "en"), original, handoff);
    original.clear();
    assertEquals(1, request.turns.size());
    assertSame(handoff, request.handoff);
    try {
      request.turns.clear();
      fail("Must be immutable");
    } catch (UnsupportedOperationException expected) {
    }
  }

  @Test
  public void attachmentBytesCannotChangeAfterValidation() {
    byte[] bytes = {1, 2};
    TutorAttachment attachment = new TutorAttachment("p.png", "image/png", bytes);
    bytes[0] = 9;
    byte[] returned = attachment.bytes();
    returned[0] = 8;
    assertEquals(1, attachment.bytes()[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void executableUploadRejected() {
    new TutorAttachment("file.exe", "application/octet-stream", new byte[] {1});
  }

  @Test(expected = IllegalArgumentException.class)
  public void oversizedAttachmentRejected() {
    new TutorAttachment("large.pdf", "application/pdf", new byte[TutorAttachment.MAX_BYTES + 1]);
  }

  @Test
  public void keysRotateOnRejectionAndRecoverAfterCooldownWithoutLeaking() {
    long[] now = {1000};
    GeminiKeyPool pool = new GeminiKeyPool(() -> now[0], " first ", "second", "first", "");
    assertTrue(pool.configured());
    assertEquals(0, pool.next());
    pool.reject(0, 429);
    assertEquals(1, pool.next());
    pool.reject(1, 403);
    assertEquals(-1, pool.next());
    now[0] += 60001;
    assertEquals(0, pool.next());
    assertFalse(pool.toString().contains("first"));
    assertFalse(pool.toString().contains("second"));
  }

  @Test
  public void blankKeysDisableNetworkBeforeTransportRuns() throws Exception {
    GeminiTutorGateway gateway =
        new GeminiTutorGateway(
            new GeminiKeyPool("", " "),
            "test-model",
            (key, model, payload) -> {
              fail("No transport expected");
              return null;
            });
    try {
      gateway.reply(null);
      fail("Must fail");
    } catch (TutorFailure failure) {
      assertEquals(TutorFailure.Code.NOT_CONFIGURED, failure.code);
    }
  }
}
