package com.example.asm_final;

import static org.junit.Assert.*;

import com.example.asm_final.accounts.PasswordPolicy;
import com.example.asm_final.onboarding.*;
import org.junit.Test;

public class OnboardingPolicyTest {
  private LearnerProfile profile(int grade, String subjects, int goal, int minutes) {
    return new LearnerProfile(42, 4, "Hữu Lộc", grade, subjects, goal, minutes, 123);
  }

  @Test
  public void validHighSchoolPreferencesNeedNoPhoneOrDailyTime() {
    assertTrue(OnboardingPolicy.valid(profile(10, "MATH,BIOLOGY", 0, 0)));
    assertTrue(OnboardingPolicy.valid(profile(12, "HISTORY", 3, 240)));
  }

  @Test
  public void gradesAndGoalsStayInSupportedRange() {
    assertFalse(OnboardingPolicy.valid(profile(9, "MATH", 0, 0)));
    assertFalse(OnboardingPolicy.valid(profile(13, "MATH", 0, 0)));
    assertFalse(OnboardingPolicy.valid(profile(11, "MATH", -1, 0)));
    assertFalse(OnboardingPolicy.valid(profile(11, "MATH", 4, 0)));
  }

  @Test
  public void subjectChoicesRejectRemovedUnknownOrDuplicatedSubjects() {
    for (String value : new String[] {"PROGRAMMING", "SCIENCE", "MATH,MATH", "UNKNOWN", "MATH,"})
      assertFalse(OnboardingPolicy.valid(profile(11, value, 1, 0)));
  }

  @Test
  public void optionalTimeIsNotAQuizTimerAndCannotBeNegative() {
    assertFalse(OnboardingPolicy.valid(profile(11, "BIOLOGY", 0, -1)));
    assertFalse(OnboardingPolicy.valid(profile(11, "BIOLOGY", 0, 4)));
    assertFalse(OnboardingPolicy.valid(profile(11, "BIOLOGY", 0, 241)));
    assertTrue(OnboardingPolicy.valid(profile(11, "BIOLOGY", 0, 5)));
  }

  @Test
  public void namesAcceptVietnameseAndRejectBlankOrExcessiveData() {
    assertTrue(OnboardingPolicy.validName("Hữu Lộc"));
    assertFalse(OnboardingPolicy.validName("  "));
    assertFalse(OnboardingPolicy.validName("a"));
    assertFalse(OnboardingPolicy.validName(new String(new char[81]).replace('\0', 'a')));
  }

  @Test
  public void bcryptByteLimitDoesNotSilentlyTruncatePasswords() {
    assertTrue(PasswordPolicy.isValid("A1" + new String(new char[70]).replace('\0', 'a')));
    assertFalse(PasswordPolicy.isValid("A1" + new String(new char[71]).replace('\0', 'a')));
    assertFalse(PasswordPolicy.isValid("A1" + new String(new char[36]).replace('\0', 'Đ')));
  }
}
