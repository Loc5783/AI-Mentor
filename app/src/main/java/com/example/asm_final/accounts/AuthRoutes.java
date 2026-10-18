package com.example.asm_final.accounts;

import android.app.Activity;
import android.content.Intent;
import com.example.asm_final.activities.*;
import com.example.asm_final.onboarding.*;

public final class AuthRoutes {
  private AuthRoutes() {}

  public static void open(
      Activity activity, String username, LearnerProfile profile, boolean clearTask) {
    if (profile == null) throw new IllegalArgumentException("Missing learning profile");
    Class<?> target =
        profile.complete()
            ? HomeActivity.class
            : profile.step >= OnboardingPolicy.PROFILE
                ? ProfileSetupActivity.class
                : OnboardingActivity.class;
    Intent intent = new Intent(activity, target).putExtra("USERNAME", username);
    if (clearTask) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    activity.startActivity(intent);
    activity.finish();
  }

  public static void login(Activity a) {
    AuthSession.clear(a);
    a.startActivity(
        new Intent(a, MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    a.finish();
  }
}
