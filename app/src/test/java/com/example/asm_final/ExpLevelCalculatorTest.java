package com.example.asm_final;

import static org.junit.Assert.*;

import com.example.asm_final.utils.ExpLevelCalculator;
import org.junit.Test;

public class ExpLevelCalculatorTest {
  @Test
  public void homeStartsWithSameThresholdAsQuizzes() {
    assertEquals(1, ExpLevelCalculator.getLevel(0));
    assertEquals(120, ExpLevelCalculator.getExpToNextLevel(0));
    assertEquals(0, ExpLevelCalculator.getProgressPercentage(0));
  }

  @Test
  public void crossingFirstLevelUsesNextVariableThreshold() {
    assertEquals(1, ExpLevelCalculator.getLevel(119));
    assertEquals(1, ExpLevelCalculator.getExpToNextLevel(119));
    assertEquals(2, ExpLevelCalculator.getLevel(120));
    assertEquals(145, ExpLevelCalculator.getExpToNextLevel(120));
    assertEquals(0, ExpLevelCalculator.getProgressPercentage(120));
  }

  @Test
  public void maximumDoesNotRequestAnotherLevel() {
    assertEquals(20, ExpLevelCalculator.getLevel(Integer.MAX_VALUE));
    assertEquals(0, ExpLevelCalculator.getExpToNextLevel(Integer.MAX_VALUE));
    assertEquals(100, ExpLevelCalculator.getProgressPercentage(Integer.MAX_VALUE));
  }

  @Test
  public void negativeLegacyXpIsDisplayedAsZero() {
    assertEquals(1, ExpLevelCalculator.getLevel(-1));
    assertEquals(120, ExpLevelCalculator.getExpToNextLevel(-1));
    assertEquals(0, ExpLevelCalculator.getProgressPercentage(-1));
  }

  @Test
  public void finalLevelTitleIsReachable() {
    assertEquals(R.string.home_level_4, ExpLevelCalculator.getLevelTitleResId(19));
    assertEquals(R.string.home_level_5, ExpLevelCalculator.getLevelTitleResId(20));
  }

  @Test
  public void subjectTotalsDoNotOverflowIntoLevelOne() {
    int total = ExpLevelCalculator.totalExperience(Integer.MAX_VALUE, 1, 400, 500);
    assertEquals(Integer.MAX_VALUE, total);
    assertEquals(20, ExpLevelCalculator.getLevel(total));
  }

  @Test
  public void invalidNegativeSubjectDoesNotCancelEarnedXp() {
    assertEquals(120, ExpLevelCalculator.totalExperience(-50, 60, 60, 0));
  }

  @Test
  public void normalSubjectTotalsArePreserved() {
    assertEquals(400, ExpLevelCalculator.totalExperience(100, 150, 50, 100));
  }
}
