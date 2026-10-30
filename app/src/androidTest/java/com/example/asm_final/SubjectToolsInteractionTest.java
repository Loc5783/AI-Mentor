package com.example.asm_final;

import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mentor.chemistry.presentation.PeriodicTableActivity;
import com.example.mentor.physics.presentation.CircuitDiagramActivity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public final class SubjectToolsInteractionTest {
  @Test
  public void circuitSeriesParallelSwitchAndInvalidValues() {
    try (ActivityScenario<CircuitDiagramActivity> scenario =
        ActivityScenario.launch(CircuitDiagramActivity.class)) {
      scenario.onActivity(
          activity -> {
            ((EditText) activity.findViewById(com.example.mentor.physics.R.id.etCircuitVoltage))
                .setText("6");
            ((EditText) activity.findViewById(com.example.mentor.physics.R.id.etCircuitR1))
                .setText("10");
            ((EditText) activity.findViewById(com.example.mentor.physics.R.id.etCircuitR2))
                .setText("20");
            ((CheckBox) activity.findViewById(com.example.mentor.physics.R.id.cbCircuitSwitch))
                .setChecked(true);
          });
      scenario.onActivity(
          activity -> {
            assertEquals(
                activity.getString(
                    com.example.mentor.physics.R.string.science_circuit_result,
                    30d,
                    .2d,
                    .2d,
                    .2d,
                    2d,
                    4d,
                    1.2d),
                ((TextView)
                        activity.findViewById(com.example.mentor.physics.R.id.tvCircuitResult))
                    .getText()
                    .toString());
            ((Spinner) activity.findViewById(com.example.mentor.physics.R.id.spCircuitType))
                .setSelection(1);
          });
      androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(
          activity -> {
            assertEquals(
                activity.getString(
                    com.example.mentor.physics.R.string.science_circuit_result,
                    20d / 3,
                    .9d,
                    .6d,
                    .3d,
                    6d,
                    6d,
                    5.4d),
                ((TextView)
                        activity.findViewById(com.example.mentor.physics.R.id.tvCircuitResult))
                    .getText()
                    .toString());
            activity.findViewById(com.example.mentor.physics.R.id.circuitDiagramView).performClick();
            assertFalse(
                ((CheckBox) activity.findViewById(com.example.mentor.physics.R.id.cbCircuitSwitch))
                    .isChecked());
            assertEquals(
                activity.getString(
                    com.example.mentor.physics.R.string.science_circuit_result,
                    20d / 3,
                    0d,
                    0d,
                    0d,
                    0d,
                    0d,
                    0d),
                ((TextView)
                        activity.findViewById(com.example.mentor.physics.R.id.tvCircuitResult))
                    .getText()
                    .toString());
            assertEquals(
                null,
                activity
                    .findViewById(android.R.id.content)
                    .findViewWithTag(com.example.mathmodule.calculator.MathCalculator.TAG));
          });
      scenario.recreate();
      scenario.onActivity(
          activity -> {
            assertEquals(
                "6",
                ((EditText) activity.findViewById(com.example.mentor.physics.R.id.etCircuitVoltage))
                    .getText()
                    .toString());
            assertFalse(
                ((CheckBox) activity.findViewById(com.example.mentor.physics.R.id.cbCircuitSwitch))
                    .isChecked());
            ((EditText) activity.findViewById(com.example.mentor.physics.R.id.etCircuitR1))
                .setText("0");
            assertEquals(
                activity.getString(com.example.mentor.physics.R.string.science_circuit_invalid),
                ((TextView)
                        activity.findViewById(com.example.mentor.physics.R.id.tvCircuitResult))
                    .getText()
                    .toString());
          });
      Espresso.onView(ViewMatchers.withId(com.example.mentor.physics.R.id.btnCircuitBack))
          .perform(ViewActions.scrollTo(), ViewActions.click());
    }
  }

  @Test
  public void periodicSearchDetailsEmptyAndRecreation() {
    try (ActivityScenario<PeriodicTableActivity> scenario =
        ActivityScenario.launch(PeriodicTableActivity.class)) {
      scenario.onActivity(
          activity ->
              assertEquals(
                  118,
                  ((RecyclerView)
                              activity.findViewById(
                                  com.example.mentor.chemistry.R.id.rvPeriodicTable))
                          .getAdapter()
                          .getItemCount()));
      Espresso.onView(ViewMatchers.withId(com.example.mentor.chemistry.R.id.etElementSearch))
          .perform(ViewActions.replaceText("26"), ViewActions.closeSoftKeyboard());
      Espresso.onView(ViewMatchers.withId(com.example.mentor.chemistry.R.id.btnElementSearch))
          .perform(ViewActions.click());
      Espresso.onView(ViewMatchers.withText("Fe")).perform(ViewActions.click());
      Espresso.onView(ViewMatchers.withId(android.R.id.button1))
          .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
          .perform(ViewActions.click());

      scenario.recreate();
      scenario.onActivity(
          activity -> {
            assertEquals(
                "26",
                ((EditText)
                        activity.findViewById(com.example.mentor.chemistry.R.id.etElementSearch))
                    .getText()
                    .toString());
            assertEquals(
                1,
                ((RecyclerView)
                            activity.findViewById(
                                com.example.mentor.chemistry.R.id.rvPeriodicTable))
                        .getAdapter()
                        .getItemCount());
          });

      // An unsubmitted draft must not replace the active result set after recreation.
      Espresso.onView(ViewMatchers.withId(com.example.mentor.chemistry.R.id.etElementSearch))
          .perform(ViewActions.replaceText("no-such-element"), ViewActions.closeSoftKeyboard());
      scenario.recreate();
      scenario.onActivity(
          activity -> {
            assertEquals(
                "no-such-element",
                ((EditText)
                        activity.findViewById(com.example.mentor.chemistry.R.id.etElementSearch))
                    .getText()
                    .toString());
            assertEquals(
                1,
                ((RecyclerView)
                            activity.findViewById(
                                com.example.mentor.chemistry.R.id.rvPeriodicTable))
                        .getAdapter()
                        .getItemCount());
          });

      Espresso.onView(ViewMatchers.withId(com.example.mentor.chemistry.R.id.etElementSearch))
          .perform(ViewActions.replaceText("no-such-element"), ViewActions.closeSoftKeyboard());
      Espresso.onView(ViewMatchers.withId(com.example.mentor.chemistry.R.id.btnElementSearch))
          .perform(ViewActions.click());
      scenario.onActivity(
          activity ->
              assertEquals(
                  0,
                  ((RecyclerView)
                              activity.findViewById(
                                  com.example.mentor.chemistry.R.id.rvPeriodicTable))
                          .getAdapter()
                          .getItemCount()));
      Espresso.onView(ViewMatchers.withId(com.example.mentor.chemistry.R.id.btnPeriodicBack))
          .perform(ViewActions.click());
    }
  }
}
