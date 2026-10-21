package com.example.mentor.learning;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

public class LearningRulesTest {
  @Test
  public void aRetakeAwardsNoExtraExperienceButCanLightANewDaysStreak() {
    for (LearningRewardPolicy.Completion kind : LearningRewardPolicy.Completion.values()) {
      assertEquals(0, LearningRewardPolicy.experience(kind, true));
      assertTrue(LearningRewardPolicy.experience(kind, false) > 0);
    }
    assertTrue(LearningRewardPolicy.lightsStreak(false));
    assertFalse(LearningRewardPolicy.lightsStreak(true));
  }

  private final List<String> lessons =
      Arrays.asList("l1", "l2", "l3", "l4", "l5", "l6", "l7", "l8", "l9", "l10", "l11");

  @Test
  public void firstReviewUsesExactlyTheFirstFiveLessons() {
    assertEquals(
        Arrays.asList("l1", "l2", "l3", "l4", "l5"),
        ReviewCheckpointPolicy.precedingLessons(lessons, 5));
  }

  @Test
  public void secondReviewUsesFiveImmediatelyPrecedingLessons() {
    assertEquals(
        Arrays.asList("l6", "l7", "l8", "l9", "l10"),
        ReviewCheckpointPolicy.precedingLessons(lessons, 10));
  }

  @Test
  public void reviewDoesNotAppearAfterIncompleteBlockOrPastCourseEnd() {
    for (int at : new int[] {-5, 0, 4, 6, 11, 15}) {
      try {
        ReviewCheckpointPolicy.precedingLessons(lessons, at);
        fail("Invalid position");
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void duplicateContentCannotBeReviewedTwice() {
    ReviewCheckpointPolicy.precedingLessons(Arrays.asList("a", "b", "c", "d", "a"), 5);
  }

  @Test
  public void tenOrFifteenQuestionsCoverEachLessonEqually() {
    assertEquals(10, ReviewCheckpointPolicy.questions(false));
    assertEquals(15, ReviewCheckpointPolicy.questions(true));
    assertEquals(2, ReviewCheckpointPolicy.questionsPerLesson(10));
    assertEquals(3, ReviewCheckpointPolicy.questionsPerLesson(15));
  }

  @Test(expected = IllegalArgumentException.class)
  public void fiveQuestionReviewIsRejected() {
    ReviewCheckpointPolicy.questionsPerLesson(5);
  }

  @Test
  public void bothActivitiesLightStreakButReviewDoublesExperience() {
    assertEquals(
        200, LearningRewardPolicy.experience(LearningRewardPolicy.Completion.LESSON, false));
    assertEquals(
        400,
        LearningRewardPolicy.experience(LearningRewardPolicy.Completion.FIVE_LESSON_REVIEW, false));
    assertTrue(LearningRewardPolicy.lightsStreak(false));
    assertFalse(LearningRewardPolicy.lightsStreak(true));
  }

  @Test
  public void handoffRetainsExactAttemptQuestionAndCurriculum() {
    LessonHandoff link = new LessonHandoff("e1", "a2", "l3", "q4", "v5", 6);
    assertEquals("a2", link.attemptId);
    assertEquals("q4", link.questionId);
    assertEquals("v5", link.curriculumVersion);
    assertEquals(6, link.step);
  }
}
