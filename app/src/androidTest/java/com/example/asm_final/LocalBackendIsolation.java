package com.example.asm_final;

import android.content.SharedPreferences;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.rules.ExternalResource;

/** Local UI tests must not create accounts on a developer's configured backend. */
public final class LocalBackendIsolation extends ExternalResource {
  private SharedPreferences settings;
  private boolean present;
  private String previous;

  @Override
  protected void before() {
    settings =
        InstrumentationRegistry.getInstrumentation()
            .getTargetContext()
            .getSharedPreferences("backend_test", 0);
    present = settings.contains("endpoint");
    previous = settings.getString("endpoint", "");
    settings.edit().putString("endpoint", "").commit();
  }

  @Override
  protected void after() {
    SharedPreferences.Editor edit = settings.edit();
    if (present) edit.putString("endpoint", previous);
    else edit.remove("endpoint");
    edit.commit();
  }
}
