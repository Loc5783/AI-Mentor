package com.example.asm_final;

import static org.junit.Assert.*;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
/*
 * Role: Test class for App Shell, used to verify key rules and behaviors after code changes.
 * Purpose: These tests verify key behaviors automatically instead of relying only on manual UI testing.
 */
public class ExampleInstrumentedTest {
  @Test
  public void useAppContext() {
    // Context of the app under test.
    Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    assertEquals("com.example.asm_final", appContext.getPackageName());
  }
}
