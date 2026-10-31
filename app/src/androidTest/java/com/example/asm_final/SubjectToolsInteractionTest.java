package com.example.asm_final;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import org.junit.Test;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import com.example.mentor.physics.presentation.CircuitDiagramActivity;
import com.example.mentor.chemistry.presentation.PeriodicTableActivity;
public class SubjectToolsInteractionTest {
 @Test public void circuitSeriesParallelSwitchAndInvalidValues() {
  try(ActivityScenario<CircuitDiagramActivity> scenario=ActivityScenario.launch(CircuitDiagramActivity.class)){
   scenario.onActivity(a->{
    ((EditText)a.findViewById(com.example.mentor.physics.R.id.etCircuitVoltage)).setText("6");
    ((EditText)a.findViewById(com.example.mentor.physics.R.id.etCircuitR1)).setText("10");
    ((EditText)a.findViewById(com.example.mentor.physics.R.id.etCircuitR2)).setText("20");
    ((CheckBox)a.findViewById(com.example.mentor.physics.R.id.cbCircuitSwitch)).setChecked(true);
   });
   scenario.onActivity(a->{
    assertEquals(a.getString(com.example.mentor.physics.R.string.science_circuit_result,30d,.2d,.2d,.2d,2d,4d,1.2d),
     ((TextView)a.findViewById(com.example.mentor.physics.R.id.tvCircuitResult)).getText().toString());
    ((Spinner)a.findViewById(com.example.mentor.physics.R.id.spCircuitType)).setSelection(1);
   });
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.onActivity(a->{
    assertEquals(a.getString(com.example.mentor.physics.R.string.science_circuit_result,20d/3,.9d,.6d,.3d,6d,6d,5.4d),
     ((TextView)a.findViewById(com.example.mentor.physics.R.id.tvCircuitResult)).getText().toString());
    a.findViewById(com.example.mentor.physics.R.id.circuitDiagramView).performClick();
    assertFalse(((CheckBox)a.findViewById(com.example.mentor.physics.R.id.cbCircuitSwitch)).isChecked());
    assertEquals(a.getString(com.example.mentor.physics.R.string.science_circuit_result,20d/3,0d,0d,0d,0d,0d,0d),
     ((TextView)a.findViewById(com.example.mentor.physics.R.id.tvCircuitResult)).getText().toString());
    assertNull(a.findViewById(android.R.id.content).findViewWithTag(com.example.mathmodule.calculator.MathCalculator.TAG));
   });
   scenario.recreate();
   scenario.onActivity(a->{
    assertEquals("6",((EditText)a.findViewById(com.example.mentor.physics.R.id.etCircuitVoltage)).getText().toString());
    assertFalse(((CheckBox)a.findViewById(com.example.mentor.physics.R.id.cbCircuitSwitch)).isChecked());
    ((EditText)a.findViewById(com.example.mentor.physics.R.id.etCircuitR1)).setText("0");
    assertEquals(a.getString(com.example.mentor.physics.R.string.science_circuit_invalid),
     ((TextView)a.findViewById(com.example.mentor.physics.R.id.tvCircuitResult)).getText().toString());
   });
   onView(withId(com.example.mentor.physics.R.id.btnCircuitBack)).perform(scrollTo(),click());
  }
 }
 @Test public void periodicSearchDetailsEmptyAndRecreation() {
  try(ActivityScenario<PeriodicTableActivity> scenario=ActivityScenario.launch(PeriodicTableActivity.class)){
   scenario.onActivity(a->{
    RecyclerView list=a.findViewById(com.example.mentor.chemistry.R.id.rvPeriodicTable);
    assertEquals(118,list.getAdapter().getItemCount());
    assertNull(a.findViewById(android.R.id.content).findViewWithTag(com.example.mathmodule.calculator.MathCalculator.TAG));
   });
   onView(withId(com.example.mentor.chemistry.R.id.etElementSearch)).perform(replaceText("26"),closeSoftKeyboard());
   onView(withId(com.example.mentor.chemistry.R.id.btnElementSearch)).perform(click());
   onView(withText("Fe")).perform(click());
   onView(withId(android.R.id.button1)).check(matches(isDisplayed())).perform(click());
   scenario.recreate();
   scenario.onActivity(a->{
    assertEquals("26",((EditText)a.findViewById(com.example.mentor.chemistry.R.id.etElementSearch)).getText().toString());
    assertEquals(1,((RecyclerView)a.findViewById(com.example.mentor.chemistry.R.id.rvPeriodicTable)).getAdapter().getItemCount());
   });
   onView(withId(com.example.mentor.chemistry.R.id.etElementSearch)).perform(replaceText("no-such-element"),closeSoftKeyboard());
   onView(withId(com.example.mentor.chemistry.R.id.btnElementSearch)).perform(click());
   scenario.onActivity(a->assertEquals(0,((RecyclerView)a.findViewById(com.example.mentor.chemistry.R.id.rvPeriodicTable)).getAdapter().getItemCount()));
   onView(withId(com.example.mentor.chemistry.R.id.btnPeriodicBack)).perform(click());
  }
 }
}
