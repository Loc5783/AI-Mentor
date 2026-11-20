package com.example.asm_final.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class StreakReminderPolicyTest {
  @Test public void warnsOnlyForYesterdayStreakNotYetContinued() {
    assertTrue(StreakReminderPolicy.shouldWarn("2026-11-19", "2026-11-18",
        "2026-11-19", "2026-11-18", false, true, 7));
    assertFalse(StreakReminderPolicy.shouldWarn("2026-11-19", "2026-11-18",
        "2026-11-19", "2026-11-18", true, true, 8));
    assertFalse(StreakReminderPolicy.shouldWarn("2026-11-19", "2026-11-18",
        "2026-11-19", "2026-11-18", false, false, 0));
    assertFalse(StreakReminderPolicy.shouldWarn("2026-11-19", "2026-11-18",
        "2026-11-18", "2026-11-17", false, true, 7));
  }
}
