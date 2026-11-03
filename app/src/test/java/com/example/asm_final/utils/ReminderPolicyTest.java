package com.example.asm_final.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class ReminderPolicyTest {

  @Test
  public void shouldPromptPolicyOnlyWhenNotPromptedAndNotEnabled() {
    // If not prompted yet and reminder is not enabled -> SHOULD prompt
    assertTrue(ReminderHelper.shouldPromptPolicy(false, false));

    // If already prompted -> SHOULD NOT prompt again
    assertFalse(ReminderHelper.shouldPromptPolicy(true, false));
    assertFalse(ReminderHelper.shouldPromptPolicy(true, true));

    // If already enabled -> SHOULD NOT prompt
    assertFalse(ReminderHelper.shouldPromptPolicy(false, true));
  }
}
