package com.example.asm_final;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.EditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.asm_final.activities.MainActivity;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OnlineSmokeLoginTest {
  @Test
  public void testAccountCanOpenOnlineApp() {
    Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    String email = InstrumentationRegistry.getArguments().getString("mentorEmail");
    String password = InstrumentationRegistry.getArguments().getString("mentorPassword");
    assertNotNull("Pass mentorEmail as a runner argument", email);
    assertNotNull("Pass mentorPassword as a runner argument", password);

    Activity login = instrumentation.startActivitySync(
        new Intent(instrumentation.getTargetContext(), MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    instrumentation.runOnMainSync(() -> {
      ((EditText) login.findViewById(R.id.edtLoginUsername)).setText(email);
      ((EditText) login.findViewById(R.id.edtLoginPassword)).setText(password);
      ((android.widget.CheckBox) login.findViewById(R.id.checkRemember)).setChecked(true);
      login.findViewById(R.id.btnLogin).performClick();
    });
    long until = SystemClock.uptimeMillis() + 30000;
    while (SystemClock.uptimeMillis() < until) {
      boolean[] loggedIn = {false};
      instrumentation.runOnMainSync(() -> {
        Collection<Activity> resumed = ActivityLifecycleMonitorRegistry.getInstance()
            .getActivitiesInStage(Stage.RESUMED);
        for (Activity activity : resumed) {
          if (!(activity instanceof MainActivity)) loggedIn[0] = true;
        }
      });
      if (loggedIn[0]) return;
      SystemClock.sleep(250);
    }
    fail("Online login did not leave the sign-in screen within 30 seconds");
  }
}
